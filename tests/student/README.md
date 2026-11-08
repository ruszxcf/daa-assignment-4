# Own tests

Run everything from the repository root: `python tests/student/run_tests.py`

| File | Purpose |
|---|---|
| `make_cases.py` → `cases.jsonl`, `expected.jsonl`, `CASES.md` | 52 hand-written boundary cases, hand-derived expected answers, defect each case detects |
| `check_cases.py` → `actual.jsonl` | runs the cases through the real launcher and checks the answers |
| `java/Tests.java` | seeded brute-force oracle tests, validators, large inputs |
| `check_large.py` | one batch of maximum-size requests through the launcher with timing |
| `mutation_check.py` → `mutation_output.txt` | deliberate bugs that the tests must detect |
| `bench/SearchBench.java` → `bench/results.md` | naive/KMP/RK benchmark used in ANALYSIS.md |

ANALYSIS.md ("Own tests") explains the commands and expected/actual results.
