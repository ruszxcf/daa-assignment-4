"""Writes tests/student/cases.jsonl (inputs), expected.jsonl (HAND-DERIVED expected answers) and CASES.md
(what each case checks and which defect it would reveal).

Expected records contain only what is unique:
  lcs     "length"                       (the sequence is validated by meaning: subsequence of both, right length)
  merge   "cost"                         (the plan is validated by simulating it)
  tree    "weight"                       (the selection is validated: no repeated/adjacent vertices, sum = weight)
  naive/kmp/rk  "positions"              exact
  suffix  "sa", "lcp", "repeated_length" (sa and lcp are unique; the repeated string is validated by meaning)
  aho     "counts"                       exact

Run from the repository root:  python tests/student/make_cases.py
"""
import json
import pathlib

HERE = pathlib.Path(__file__).resolve().parent
B = 1_000_000_000
CASES = []


def case(name, request, expected, defect):
    CASES.append({"name": name, "input": request, "expected": expected, "defect": defect})


# ------------------------------------------------------------------ LCS
case("lcs: assignment example", {"task": "lcs", "a": "tea_time", "b": "team"}, {"length": 4},
     "Treating the common part as a substring instead of a subsequence ('te' or 'tea' would be found, not 'team').")
case("lcs: both strings empty", {"task": "lcs", "a": "", "b": ""}, {"length": 0},
     "Missing base case: indexing an empty string.")
case("lcs: one string empty", {"task": "lcs", "a": "abc", "b": ""}, {"length": 0},
     "Row/column 0 not initialised to 0; wrong shape of the table for an empty string.")
case("lcs: non-contiguous", {"task": "lcs", "a": "axbxc", "b": "abc"}, {"length": 3},
     "Resetting the length on a mismatch (substring DP) would give 1.")
case("lcs: two different optima", {"task": "lcs", "a": "ab", "b": "ba"}, {"length": 1},
     "Rejecting a valid alternative: both 'a' and 'b' are accepted; a wrong length is not.")

# ------------------------------------------------------------------ merge
case("merge: assignment example", {"task": "merge", "sizes": [2, 3, 4]}, {"cost": 14},
     "Merging in the wrong order (7+9 = 16) or forgetting that a merge costs the combined size.")
case("merge: zero blocks", {"task": "merge", "sizes": []}, {"cost": 0},
     "Crash for n=0; the plan must be empty.")
case("merge: one block", {"task": "merge", "sizes": [5]}, {"cost": 0},
     "A single block needs no merge (cost 0, empty plan), not a cost of 5.")
case("merge: adjacency matters", {"task": "merge", "sizes": [1, 100, 1]}, {"cost": 203},
     "Greedy 'merge the two smallest blocks' would join the two 1s (not adjacent): 2+102 = 104, which is not a valid plan.")
case("merge: zeros", {"task": "merge", "sizes": [0, 0, 0]}, {"cost": 0},
     "Using 0 as 'not computed yet' in the table; many optimal plans must all be accepted.")
case("merge: large costs", {"task": "merge", "sizes": [B, B, B, B]}, {"cost": 8 * B},
     "32-bit overflow: the optimum 2e9 + 2e9 + 4e9 = 8e9 does not fit into int.")

# ------------------------------------------------------------------ tree
case("tree: assignment example", {"task": "tree", "n": 3, "weights": [4, 9, 6], "edges": [[0, 1], [1, 2]]}, {"weight": 10},
     "Greedy by weight would pick the middle vertex 9; the two ends give 10.")
case("tree: empty tree", {"task": "tree", "n": 0, "weights": [], "edges": []}, {"weight": 0},
     "Crash on n=0 (no root).")
case("tree: all negative", {"task": "tree", "n": 3, "weights": [-1, -2, -3], "edges": [[0, 1], [1, 2]]}, {"weight": 0},
     "Forcing at least one vertex: the empty set (weight 0) is allowed and optimal.")
case("tree: star, heavy centre", {"task": "tree", "n": 4, "weights": [10, 1, 1, 1], "edges": [[0, 1], [0, 2], [0, 3]]}, {"weight": 10},
     "Taking the three leaves (3) instead of the centre (10).")
case("tree: star, heavy leaves", {"task": "tree", "n": 4, "weights": [5, 3, 3, 3], "edges": [[0, 1], [0, 2], [0, 3]]}, {"weight": 9},
     "Taking the centre (5) instead of the three leaves (9).")
case("tree: large sum", {"task": "tree", "n": 3, "weights": [B, B, B], "edges": [[0, 1], [1, 2]]}, {"weight": 2 * B},
     "32-bit overflow of the total weight.")

# ------------------------------------------------------------------ searches (three commands, same fixtures)
SEARCH = [
    ("overlapping matches", "aaaaa", "aaa", [0, 1, 2], "Skipping overlaps (reporting only start 0) or missing the last start 2."),
    ("overlap via KMP fallback", "abababa", "aba", [0, 2, 4], "No fallback after a full match: starts 2 and 4 would be lost."),
    ("empty pattern", "abc", "", [0, 1, 2, 3], "Forgetting the final boundary n, or indexing the empty pattern."),
    ("empty text and pattern", "", "", [0], "n+1 = 1 start for two empty strings."),
    ("pattern longer than text", "abc", "abcd", [], "Out-of-range comparison at the end of the text."),
    ("case and punctuation", "Ab, ab; AB", "ab", [4], "Case folding or treating ',' and ' ' specially."),
    ("last valid start", "xxabc", "abc", [2], "Off-by-one loop bound (start n-m is valid)."),
]
for name, text, pat, positions, defect in SEARCH:
    for task in ("naive", "kmp", "rk"):
        case(f"{task}: {name}", {"task": task, "text": text, "pattern": pat}, {"positions": positions}, defect)
case("rk: collision window is not reported", {"task": "rk", "text": "abdj", "pattern": "dj"}, {"positions": [2]},
     "'ab' and 'dj' both hash to 75 (base 31, mod 101); without character verification 'ab' (start 0) would be reported too.")
case("rk: collision, two real matches", {"task": "rk", "text": "abdjab", "pattern": "ab"}, {"positions": [0, 4]},
     "'dj' at start 2 has the same hash as 'ab' and must be rejected; starts 0 and 4 must stay.")
case("rk: negative remainder", {"task": "rk", "text": "zzzzzzzzzzaaaaaaaaaa", "pattern": "zzzaa"}, {"positions": [7]},
     "Without normalising a negative remainder after removing the leftmost character the rolling hash drifts and the match is missed.")

# ------------------------------------------------------------------ suffix
case("suffix: banana", {"task": "suffix", "text": "banana"},
     {"sa": [5, 3, 1, 0, 4, 2], "lcp": [0, 1, 3, 0, 0, 2], "repeated_length": 3},
     "Wrong tie-break in doubling; lcp[i] must compare sa[i-1] and sa[i] (not i-1 and i); overlapping repeat 'ana' must be allowed.")
case("suffix: empty text", {"task": "suffix", "text": ""}, {"sa": [], "lcp": [], "repeated_length": 0},
     "Crash on n=0.")
case("suffix: all equal", {"task": "suffix", "text": "aaa"}, {"sa": [2, 1, 0], "lcp": [0, 1, 2], "repeated_length": 2},
     "A missing second half must sort FIRST ('a' < 'aa' < 'aaa'); repeated is 'aa' (starts 0 and 1, overlapping).")
case("suffix: all distinct", {"task": "suffix", "text": "abc"}, {"sa": [0, 1, 2], "lcp": [0, 0, 0], "repeated_length": 0},
     "Inventing a repeat when max lcp is 0: repeated must be the empty string.")
case("suffix: single character", {"task": "suffix", "text": "z"}, {"sa": [0], "lcp": [0], "repeated_length": 0},
     "Doubling loop that never stops or reads past the end for n=1.")

# ------------------------------------------------------------------ aho
case("aho: assignment example", {"task": "aho", "patterns": ["he", "she", "he"], "texts": ["sheshe", "sheshe", ""]},
     {"counts": [[2, 2, 2], [2, 2, 2], [0, 0, 0]]},
     "'he' is a suffix of 'she' (needs failure-link propagation); duplicates get equal counts; a repeated text starts from fresh counts.")
case("aho: empty dictionary", {"task": "aho", "patterns": [], "texts": ["abc", ""]}, {"counts": [[], []]},
     "One EMPTY row per text, not an empty result and not a crash.")
case("aho: no texts", {"task": "aho", "patterns": ["a"], "texts": []}, {"counts": []},
     "Output must be an empty list when there are no texts.")
case("aho: overlapping patterns", {"task": "aho", "patterns": ["a", "aa"], "texts": ["aaaa"]}, {"counts": [[4, 3]]},
     "Counting non-overlapping matches (2 for 'aa') or ignoring that 'a' is a suffix of 'aa'.")
case("aho: nested suffix patterns", {"task": "aho", "patterns": ["abc", "bc", "c"], "texts": ["abc"]}, {"counts": [[1, 1, 1]]},
     "Only the longest pattern is counted when failure links are not followed.")
case("aho: independent texts", {"task": "aho", "patterns": ["ab"], "texts": ["ab", "ab", "b"]}, {"counts": [[1], [1], [0]]},
     "Counts or automaton state carried over from the previous text (would give 2 and 3).")

with open(HERE / "cases.jsonl", "w", encoding="utf-8") as f_in, open(HERE / "expected.jsonl", "w", encoding="utf-8") as f_out:
    for c in CASES:
        f_in.write(json.dumps(c["input"], separators=(",", ":")) + "\n")
        f_out.write(json.dumps({"name": c["name"], **c["expected"]}, separators=(",", ":")) + "\n")

with open(HERE / "CASES.md", "w", encoding="utf-8") as f:
    f.write("# Hand-written cases\n\nGenerated by `python tests/student/make_cases.py`. Line *i* of `cases.jsonl` (input) belongs to line *i* of\n")
    f.write("`expected.jsonl` (hand-derived expected answer). `python tests/student/check_cases.py` runs the program, writes the real\n")
    f.write("answers to `actual.jsonl` and checks each answer by meaning.\n\n")
    f.write("| # | Case | Defect it detects |\n|---:|---|---|\n")
    for i, c in enumerate(CASES, 1):
        f.write(f"| {i} | {c['name']} | {c['defect']} |\n")
print(f"wrote {len(CASES)} cases")
