"""Do my tests notice deliberate bugs?   python tests/student/mutation_check.py

Each mutant is a copy of Java/source/Algorithms.java with ONE deliberate bug. tests/student/java/Tests.java is
compiled against it and run; it must report failing checks. The real source tree is never modified.
"""
import os
import pathlib
import shutil
import subprocess
import sys
import tempfile

ROOT = pathlib.Path(__file__).resolve().parents[2]

MUTANTS = [
    ("lcs: the DP ignores the left neighbour", "Math.max(len[i - 1][j], len[i][j - 1])", "len[i - 1][j]"),
    ("merge: the plan uses a wrong split point", "int k = split[l][rr];\n            if (expanded", "int k = l;\n            if (expanded"),
    ("merge: the window total is not added to the cost", "long c = cost[l][k] + cost[k + 1][rr] + total;", "long c = cost[l][k] + cost[k + 1][rr];"),
    ("tree: a child of a taken vertex may also be taken", "} else if (taken[p]) {\n                t = false;", "} else if (taken[p]) {\n                t = take[v] > skip[v];"),
    ("tree: negative vertices are forced into the set", "t = take[v] > skip[v];\n            } else if (taken[p])", "t = true;\n            } else if (taken[p])"),
    ("naive: the last valid start is missed", "for (int s = 0; s + m <= n; s++) {", "for (int s = 0; s + m < n; s++) {"),
    ("kmp: no fallback after a full match (overlaps lost)", "k = pi[k - 1]; // fall back so that overlapping matches are kept", "k = 0;"),
    ("kmp: empty pattern returns n instead of n+1 starts", "if (m == 0) { // before any pattern indexing\n            return positions(allBoundaries(n));",
     "if (m == 0) { // before any pattern indexing\n            return positions(allBoundaries(n - 1));"),
    ("rk: equal hashes are reported without verification", "if (j == m) {\n                    out.add(s);\n                }\n            }\n            if (s + m >= n)",
     "if (true) {\n                    out.add(s);\n                }\n            }\n            if (s + m >= n)"),
    ("rk: negative remainder is not normalised", "ht = (ht - text.charAt(s) * high % RK_MOD + RK_MOD) % RK_MOD;", "ht = (ht - text.charAt(s) * high % RK_MOD) % RK_MOD;"),
    ("suffix: a missing second half sorts last instead of first", "long second = i + k < n ? rank[i + k] + 1 : 0;", "long second = i + k < n ? rank[i + k] + 1 : 100000;"),
    ("aho: counts are not pushed along failure links", "visits[fail[u]] += visits[u];", "visits[fail[u]] += 0;"),
    ("aho: counts of the previous text are not reset", "Arrays.fill(visits, 0); // every text starts from fresh counts", ""),
    ("aho: missing goto edges are not completed", "go[v * ALPHABET + c] = go[fail[v] * ALPHABET + c]; // complete the automaton", ""),
]


# Output-equivalent: the answers stay correct, only the O(n) reuse of Kasai is lost (slower), so no answer test can see it.
EXPECTED_SURVIVORS = [
    ("suffix: Kasai h is reset instead of decremented (performance only)",
     "if (h > 0) {\n                    h--;\n                }", "h = 0;"),
]


def run(cmd):
    return subprocess.run(cmd, capture_output=True, text=True, encoding="utf-8")


def main():
    source = (ROOT / "Java" / "source" / "Algorithms.java").read_text(encoding="utf-8")
    sep = os.pathsep
    missed = 0
    for name, old, new in MUTANTS + EXPECTED_SURVIVORS:
        expected_survivor = (name, old, new) in EXPECTED_SURVIVORS
        assert source.count(old) == 1, f"pattern of '{name}' not found exactly once"
        with tempfile.TemporaryDirectory() as tmp:
            tmp = pathlib.Path(tmp)
            src, out = tmp / "src", tmp / "out"
            src.mkdir()
            out.mkdir()
            for f in (ROOT / "Java" / "source").glob("*.java"):
                shutil.copy(f, src / f.name)
            (src / "Algorithms.java").write_text(source.replace(old, new), encoding="utf-8")
            built = run(["javac", "--release", "17", "-d", str(out), *map(str, src.glob("*.java"))])
            if not built.returncode:
                built = run(["javac", "--release", "17", "-cp", str(out), "-d", str(out),
                             str(ROOT / "tests" / "student" / "java" / "Tests.java")])
            if built.returncode:
                print(f"?  {name}: does not compile\n{built.stderr}")
                continue
            result = run(["java", "-Xmx384m", "-cp", str(out) + sep + str(out), "Tests"])
            failing = sum(1 for line in result.stdout.splitlines() if line.startswith("FAIL"))
            caught = failing > 0 or result.returncode != 0
            if expected_survivor:
                print(f"{'caught  ' if caught else 'survived'} ({failing:5d} failing checks)  {name}  [expected: output-equivalent]")
                continue
            missed += not caught
            print(f"{'caught  ' if caught else 'MISSED  '} ({failing:5d} failing checks)  {name}")
    print(f"{len(MUTANTS) - missed} of {len(MUTANTS)} deliberate bugs were detected")
    return 1 if missed else 0


if __name__ == "__main__":
    sys.exit(main())
