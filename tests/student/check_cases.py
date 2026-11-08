"""Runs tests/student/cases.jsonl through the selected program, saves actual.jsonl and checks every answer.

    python tests/student/check_cases.py

Unique answers are compared exactly; ambiguous ones (LCS sequence, merge plan, tree selection, repeated substring)
are validated by meaning with checks written independently of the solution.
"""
import json
import pathlib
import sys

ROOT = pathlib.Path(__file__).resolve().parents[2]
sys.path.insert(0, str(ROOT / "tools"))
import run  # noqa: E402  (the assignment's launcher: build() and execute())

HERE = pathlib.Path(__file__).resolve().parent


def is_subsequence(s, of):
    it = iter(of)
    return all(c in it for c in s)


def check_lcs(req, ans, exp):
    seq = ans["sequence"]
    if ans["length"] != exp["length"] or len(seq) != exp["length"]:
        return f"length {ans['length']} / sequence {seq!r}"
    return None if is_subsequence(seq, req["a"]) and is_subsequence(seq, req["b"]) else "not a common subsequence"


def check_merge(req, ans, exp):
    sizes, plan = req["sizes"], ans["plan"]
    n = len(sizes)
    if ans["cost"] != exp["cost"] or len(plan) != max(n - 1, 0):
        return f"cost {ans['cost']} with {len(plan)} merges"
    blocks = {(i, i) for i in range(n)}
    paid = 0
    for l, k, r in plan:  # simulate: both children must currently exist and be adjacent
        if not (l <= k < r < n) or (l, k) not in blocks or (k + 1, r) not in blocks:
            return f"invalid step {[l, k, r]}"
        blocks -= {(l, k), (k + 1, r)}
        blocks.add((l, r))
        paid += sum(sizes[l:r + 1])
    if n > 1 and blocks != {(0, n - 1)}:
        return "plan does not end with one block"
    return None if paid == exp["cost"] else f"plan pays {paid}"


def check_tree(req, ans, exp):
    sel = ans["selected"]
    if ans["weight"] != exp["weight"] or len(set(sel)) != len(sel):
        return f"weight {ans['weight']} / repeated vertex"
    chosen = set(sel)
    if any(u in chosen and v in chosen for u, v in req["edges"]):
        return "two adjacent vertices selected"
    return None if sum(req["weights"][v] for v in sel) == exp["weight"] else "selected weights do not sum to weight"


def check_search(req, ans, exp):
    return None if ans["positions"] == exp["positions"] else f"positions {ans['positions']}"


def check_suffix(req, ans, exp):
    if ans["sa"] != exp["sa"] or ans["lcp"] != exp["lcp"]:
        return f"sa {ans['sa']} lcp {ans['lcp']}"
    rep, text = ans["repeated"], req["text"]
    if len(rep) != exp["repeated_length"]:
        return f"repeated {rep!r}"
    if rep:
        starts = [i for i in range(len(text) - len(rep) + 1) if text[i:i + len(rep)] == rep]
        if len(starts) < 2:
            return "repeated does not occur twice"
    return None


def check_aho(req, ans, exp):
    return None if ans["counts"] == exp["counts"] else f"counts {ans['counts']}"


CHECKERS = {"lcs": check_lcs, "merge": check_merge, "tree": check_tree, "naive": check_search, "kmp": check_search,
            "rk": check_search, "suffix": check_suffix, "aho": check_aho}


def main():
    requests = [json.loads(x) for x in (HERE / "cases.jsonl").read_text(encoding="utf-8").splitlines()]
    expected = [json.loads(x) for x in (HERE / "expected.jsonl").read_text(encoding="utf-8").splitlines()]
    command = run.build(run.select_language(None))
    answers = run.execute(command, requests)
    with open(HERE / "actual.jsonl", "w", encoding="utf-8") as f:
        for a in answers:
            f.write(json.dumps(a, separators=(",", ":")) + "\n")
    failures = 0
    for i, (req, ans, exp) in enumerate(zip(requests, answers, expected), 1):
        problem = CHECKERS[req["task"]](req, ans, exp)
        failures += problem is not None
        print(("PASS" if problem is None else f"FAIL ({problem})") + f"  #{i} {exp['name']}")
    # state reset: the same requests twice and in reverse order inside ONE process
    again = run.execute(command, requests + requests[::-1])
    same = again[:len(requests)] == answers and again[len(requests):][::-1] == answers
    print(("PASS" if same else "FAIL") + "  state reset: repeated + reversed batch in one process gives identical answers")
    failures += not same
    print(f"{len(requests)} cases, {failures} failures")
    return 1 if failures else 0


if __name__ == "__main__":
    sys.exit(main())
