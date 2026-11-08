"""Process-level maximum-size check through the real launcher (java -Xmx384m, one JSONL batch, 20 s limit).

    python tests/student/check_large.py

One batch holds one worst-case request per command. Answers are checked with independent, simple oracles
(or by meaning where several answers are valid). Prints wall time of the whole batch.
"""
import json
import pathlib
import random
import sys
import time

ROOT = pathlib.Path(__file__).resolve().parents[2]
sys.path.insert(0, str(ROOT / "tools"))
import run  # noqa: E402

rnd = random.Random(20261108)
ALPHA = "".join(chr(c) for c in range(32, 127))


def rs(n, alphabet=ALPHA):
    return "".join(rnd.choice(alphabet) for _ in range(n))


def build_requests():
    reqs = []
    reqs.append({"task": "lcs", "a": rs(1000, "ab"), "b": rs(1000, "ab")})                     # 0  1000x1000
    reqs.append({"task": "merge", "sizes": [rnd.randint(0, 10**9) for _ in range(200)]})      # 1  n=200
    n = 100000
    reqs.append({"task": "tree", "n": n, "weights": [rnd.randint(-10**9, 10**9) for _ in range(n)],
                 "edges": [[i, i + 1] for i in range(n - 1)]})                                # 2  chain of 100000 (deep)
    reqs.append({"task": "naive", "text": "a" * 3000, "pattern": "a" * 1500})                  # 3  worst case n*m/2
    reqs.append({"task": "kmp", "text": "ab" * 100000, "pattern": "ab" * 50000})               # 4  200000 / 100000
    reqs.append({"task": "rk", "text": "a" * 10000, "pattern": "a" * 2000})                    # 5  every window verified
    reqs.append({"task": "suffix", "text": "a" * 50000})                                       # 6  many doubling rounds
    pats = ["".join(rnd.choice("ab") for _ in range(10)) for _ in range(10000)]                # L = 100000
    texts = ["ab" * 1000] * 100                                                                # 100 texts, 200000 chars
    reqs.append({"task": "aho", "patterns": pats, "texts": texts})                             # 7
    return reqs


def is_subseq(s, t):
    it = iter(t)
    return all(c in it for c in s)


def lcs_len(a, b):
    prev = [0] * (len(b) + 1)
    for x in a:
        cur = [0]
        for j, y in enumerate(b, 1):
            cur.append(prev[j - 1] + 1 if x == y else max(prev[j], cur[j - 1]))
        prev = cur
    return prev[-1]


def merge_cost(sizes):
    n = len(sizes)
    pre = [0]
    for s in sizes:
        pre.append(pre[-1] + s)
    INF = float("inf")
    c = [[0] * n for _ in range(n)]
    for length in range(2, n + 1):
        for l in range(n - length + 1):
            r = l + length - 1
            c[l][r] = min(c[l][k] + c[k + 1][r] for k in range(l, r)) + pre[r + 1] - pre[l]
    return c[0][n - 1] if n else 0


def tree_weight(req):
    w = req["weights"]
    take, skip = 0, 0       # chain: process from the end
    for x in reversed(w):
        take, skip = skip + x, max(take, skip)
    return max(take, skip)


def occurrences(text, pat):
    out, i = [], text.find(pat)
    while i != -1:
        out.append(i)
        i = text.find(pat, i + 1)
    return out


def verify(reqs, ans):
    problems = []
    a = ans[0]
    ok = a["length"] == lcs_len(reqs[0]["a"], reqs[0]["b"]) == len(a["sequence"]) and \
        is_subseq(a["sequence"], reqs[0]["a"]) and is_subseq(a["sequence"], reqs[0]["b"])
    problems += [] if ok else ["lcs"]
    m = ans[1]
    ok = m["cost"] == merge_cost(reqs[1]["sizes"]) and len(m["plan"]) == 199
    problems += [] if ok else ["merge"]
    t = ans[2]
    sel = set(t["selected"])
    ok = t["weight"] == tree_weight(reqs[2]) and len(sel) == len(t["selected"]) and \
        all(not (i in sel and i + 1 in sel) for i in range(99999)) and \
        sum(reqs[2]["weights"][v] for v in sel) == t["weight"]
    problems += [] if ok else ["tree"]
    for idx, name in ((3, "naive"), (4, "kmp"), (5, "rk")):
        want = [s for s in range(len(reqs[idx]["text"]) - len(reqs[idx]["pattern"]) + 1)
                if reqs[idx]["text"].startswith(reqs[idx]["pattern"], s)]
        problems += [] if ans[idx]["positions"] == want else [name]
    s = ans[6]
    n = 50000
    ok = s["sa"] == list(range(n - 1, -1, -1)) and s["lcp"] == list(range(n)) and s["repeated"] == "a" * (n - 1)
    problems += [] if ok else ["suffix"]
    pats, texts = reqs[7]["patterns"], reqs[7]["texts"]
    row = [len(occurrences(texts[0], p)) for p in pats]
    ok = len(ans[7]["counts"]) == 100 and all(r == row for r in ans[7]["counts"])
    problems += [] if ok else ["aho"]
    return problems


def main():
    reqs = build_requests()
    command = run.build(run.select_language(None))
    t0 = time.perf_counter()
    ans = run.execute(command, reqs)     # raises on timeout (20 s) or a malformed line
    elapsed = time.perf_counter() - t0
    problems = verify(reqs, ans)
    print(f"one batch with 8 maximum-size requests took {elapsed:.2f} s (limit 20 s), "
          f"{'all answers correct' if not problems else 'WRONG: ' + ', '.join(problems)}")
    return 1 if problems else 0


if __name__ == "__main__":
    sys.exit(main())
