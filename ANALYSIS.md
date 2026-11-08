# Assignment 4 — analysis

## Environment and commands

- Language: Java, `--release 17` (compiled and run with OpenJDK 21.0.12). Sources: `Java/source/` (`Algorithms.java`, `Json.java`, `Main.java`).
- Hardware/OS: 2 vCPU Intel Xeon @ 2.30 GHz, 8 GB RAM, Linux 6.18. The benchmark was pinned to one core (`taskset -c 0`).
- Public checks: `python tools/run.py --setup`, then `python tools/run.py --check` → 45 PASS (44 public cases + one state-reset batch).
- All own tests: `python tests/student/run_tests.py` (compiles, runs the four steps below).
- Mutation check: `python tests/student/mutation_check.py`. Benchmark: `javac --release 17 -cp .build/Java -d .build/bench tests/student/bench/SearchBench.java` and `taskset -c 0 java -Xmx384m -cp .build/Java:.build/bench SearchBench`.
- Final commit URL: given in Moodle.

## Algorithm explanations

**lcs.** `len[i][j]` = LCS length of the first `i` characters of `a` and first `j` of `b`. Base: row/column 0 are 0. If `a[i-1]==b[j-1]` then `len[i-1][j-1]+1`, else `max(len[i-1][j], len[i][j-1])`. Rows are filled in increasing `i`, so the three neighbours are ready. Correctness: an optimal common subsequence either ends in a matching last pair or leaves out the last character of one string. Reconstruction walks back from `(n,m)`: on a match take the character, otherwise move up if `len[i-1][j] >= len[i][j-1]`, else left (ties give one of several valid answers). Time O(nm), memory O(nm) ints (≤ 10^6). Test: `ab`/`ba` has two optima (`a`, `b`); a substring-DP defect would fail `axbxc`/`abc` (3, not 1).

**merge.** `cost[l][r]` = minimal cost to turn blocks `l..r` into one block; `sum(l,r)` from prefix sums (long). Base `cost[i][i]=0`. `cost[l][r] = min over l<=k<r of cost[l][k]+cost[k+1][r]+sum(l,r)`, with `split[l][r]` the best `k`. Intervals are processed by increasing length, so both halves are ready. Correctness: the last merge of any plan joins two adjacent intervals `[l,k]` and `[k+1,r]` that were built independently, and always costs `sum(l,r)`. Plan: explicit-stack post-order walk over `split`, emitting `[l,k,r]` after both children, so every step merges existing neighbours; the first strictly smaller `k` wins ties. n≤1 gives cost 0 and an empty plan. Time O(n³), memory O(n²) (n≤200). Test: `[1,100,1]` must cost 203, because merging the two non-adjacent 1s is not allowed; `[1e9]*4` needs 64-bit (8·10^9).

**tree.** `take[v]` = best weight in the subtree of `v` with `v` selected, `skip[v]` = best with `v` not selected. `take[v] = w[v] + Σ skip[child]`, `skip[v] = Σ max(take[child], skip[child])`, leaves `take=w`, `skip=0`. A CSR adjacency list and an iterative DFS preorder are used; processing the preorder in reverse guarantees children are done before parents and avoids recursion depth problems on a 100000-vertex chain. Answer `max(take[root], skip[root])` (the empty set is allowed, so ≥ 0). Reconstruction goes top-down: the root is taken if `take>skip`; a child of a taken vertex is forced to skip, otherwise it is taken iff `take>skip` (ties → skip). Time O(n), memory O(n), weights in long. Test: all-negative weights give 0 and `[]`; star with centre 5 and leaves 3,3,3 gives 9 (greedy picking the centre fails).

**naive.** For every start `s` in `0..n-m` compare `m` characters, report if all equal. Overlaps are kept because every start is tried. `m==0` returns the `n+1` boundaries, `m>n` returns `[]`. Time O(nm) worst case, extra memory O(1) plus the result. Test: `xxabc`/`abc` → `[2]` detects an off-by-one loop bound (`s+m<n`).

**kmp.** `pi[i]` = length of the longest proper prefix of `pattern[0..i]` that is also its suffix; built in O(m). While scanning the text, `k` is the matched length: on mismatch `k=pi[k-1]` until a match or 0. After a full match (`k==m`) the match start is `i-m+1` and `k` is set to `pi[m-1]` so overlapping matches survive. Each step either increases `k` or decreases it, so total work is O(n+m) (amortised). The empty pattern is handled before indexing. Test: `abababa`/`aba` → `[0,2,4]`; without the fallback after a match (`k=0`) starts 2 and 4 are lost.

**rk.** Rolling hash with base 31 and modulus 101. `hp` and `ht` are the hashes of the pattern and of the current window; `high = 31^(m-1) mod 101`. Moving one position: `ht = ((ht - text[s]*high) mod 101 + 101) mod 101`, then `ht = (ht*31 + text[s+m]) mod 101`. A normalised non-negative remainder is mandatory in Java because `%` can be negative. An equal hash is only a candidate: all `m` characters are compared before reporting. Cost: O(n+m) for hashing plus O(c·m) for `c` candidate windows with equal hash, so the worst case is O(nm) (for example `a^n` with `a^m`, where every window is a real match and is verified). Memory O(1) plus the result. `m==0` and `m>n` are handled before building a window. Tests: `abdj`/`dj` and `abdjab`/`ab` (`ab` and `dj` both hash to 75) detect a missing verification; `zzzzzzzzzzaaaaaaaaaa`/`zzzaa` detects a missing normalisation.

**suffix.** Doubling: `rank[i]` = class of the first `2^t` characters of suffix `i`. Each round sorts suffixes by the pair `(rank[i], rank[i+k])`, where a missing second half counts as 0 and real ranks are shifted by +1, so a shorter suffix sorts first. The pair is packed into one `long` key `((rank*mul+second)<<16)|i` and sorted with `Arrays.sort` (primitive longs, no comparator objects); equal pairs get the same new rank; the loop stops when all ranks are distinct or `k>=n`. Time O(n log² n) (O(log n) rounds, each a sort), memory O(n). Kasai: `rank[i]` is the position of suffix `i` in `sa`; `lcp[rank[i]]` is its LCP with the previous suffix in `sa`. Going from suffix `i` to `i+1` the LCP can drop by at most 1 (drop the first character of both suffixes; their order is kept), so `h` is only decremented, never reset, which gives O(n) in total. `lcp[0]=0`. The longest repeated substring is the maximum adjacent LCP: any repeated string is a common prefix of two suffixes, and the two suffixes with the longest common prefix are adjacent in sorted order. The occurrences may overlap (`aaa` → `aa`); when the maximum is 0 the answer is the empty string. Output has exactly the n non-empty suffixes, no sentinel. Test: `banana` → `sa=[5,3,1,0,4,2]`, `lcp=[0,1,3,0,0,2]`, repeated `ana`; `aaa` → `sa=[2,1,0]`.

**aho.** Trie of all patterns (95 printable ASCII characters). Duplicate patterns end in the same terminal state; `terminal[i]` stores the state of dictionary position `i`. BFS computes failure links and completes the goto table (`go[v][c] = go[fail[v]][c]` for missing edges), so scanning a text is one table lookup per character. For each text the counter array is zeroed, `visits[state]++` for every text character, and then in reverse BFS order `visits[fail[u]] += visits[u]`; the answer for pattern `i` is `visits[terminal[i]]`. A visit of a state counts all patterns that are suffixes of its string, so suffix patterns and overlaps are counted, duplicates get equal numbers, and no per-match list is built. Resetting the array for every text gives independent texts. Time O(L + 95·S + p) to build and O(|text| + S + p) per text; memory O(95·S + p) with `S ≤ L+1`. Empty dictionary → one empty row per text; no texts → `[]`. Test: patterns `he, she, he` on `sheshe` → `[2,2,2]` (he is a suffix of she, duplicates equal); `a, aa` on `aaaa` → `[4,3]` shows overlaps.

## Own tests

|Command|Input or fixture path|Expected result|Actual result / log|What it checks|
|---|---|---|---|---|
|`python tools/run.py --check`|`tests/public.json`|45 PASS|45 PASS|The public examples and one state-reset batch.|
|`python tests/student/check_cases.py`|`tests/student/cases.jsonl`, hand-derived answers in `tests/student/expected.jsonl`|52 PASS|52 cases, 0 failures; real answers in `tests/student/actual.jsonl`|Boundary cases listed with the defect each detects in `tests/student/CASES.md` (empty inputs, overlaps, KMP fallback, rk collision `ab`/`dj`, negative remainder, 32-bit overflow, aho duplicates/reset, `suffix` of `aaa`); ambiguous answers (LCS sequence, merge plan, tree set, repeated string) are validated by meaning; also one process, repeated + reversed batch gives identical answers.|
|`java -Xmx384m -cp .build/Java:.build/tests Tests`|generated inside `tests/student/java/Tests.java` (seeded)|0 failing checks|10742 checks pass in about 6 s|Independent brute-force oracles on thousands of small random inputs for all eight commands, validators for ambiguous answers, validator self-tests, larger inputs (deep chain, dense aho, doubling worst case), repeated calls.|
|`python tests/student/check_large.py`|built in the script, seed 20261108|all answers correct, under 20 s|one batch of 8 maximum-size requests: 0.58 s, all correct|Full input domains through the real launcher with `-Xmx384m`: LCS 1000×1000, merge n=200, tree chain of 100000, naive 3000/1500, kmp 200000/100000, rk 10000/2000, suffix `a^50000`, aho 10000 patterns × 100 texts.|
|`python tests/student/mutation_check.py`|14 deliberate bugs copied into temporary copies of `Algorithms.java`|every bug detected|14 of 14 detected (one extra performance-only mutant, Kasai `h=0`, correctly survives because answers stay identical); log in `tests/student/mutation_output.txt`|That the tests above really fail on wrong code.|

## Search comparison

Inputs are identical for naive, KMP and RK. Before timing, all three answers were compared with a reference built from `String.indexOf` (the program stops otherwise). Cases: `random` (alphabet `acgt`, `java.util.Random(20261108+n)`, the pattern is a substring of the text), `all-a` (`a^n`, `a^m`: every start matches), `ab-periodic` (`(ab)^(n/2)`, `(ab)^(m/2)`). Sizes (n, m): (300, 30), (1000, 100), (3000, 300).

Timing scope (the same for all three): one call of `Algorithms.naive/kmp/rk` on a prepared request map, including building its answer; no input parsing, no compilation. Warm-up: 3000 untimed calls per algorithm and input (JIT). A run is the mean of 100 calls measured with `System.nanoTime`; 5 runs per cell; times are microseconds per call; Median = median of the 5 runs. Run on one core (`taskset -c 0`, `-Xmx384m`, OpenJDK 21.0.12, Intel Xeon 2.30 GHz). Source: `tests/student/bench/SearchBench.java`, raw output: `tests/student/bench/results.md`.

|Case|n|m|Seed/generator|Algorithm|Run 1|Run 2|Run 3|Run 4|Run 5|Median|Correct outputs checked|
|---|---:|---:|---|---|---:|---:|---:|---:|---:|---:|---|
|random|300|30|java.util.Random(20261408), alphabet acgt, pattern = text[55..85)|naive|0.8|0.8|0.8|0.8|1.1|0.8|yes, 1 starts equal to String.indexOf reference|
|random|300|30|java.util.Random(20261408), alphabet acgt, pattern = text[55..85)|kmp|40.3|5.0|5.4|5.2|5.2|5.2|yes, 1 starts equal to String.indexOf reference|
|random|300|30|java.util.Random(20261408), alphabet acgt, pattern = text[55..85)|rk|4.2|4.1|45.0|4.6|4.3|4.3|yes, 1 starts equal to String.indexOf reference|
|random|1000|100|java.util.Random(20262108), alphabet acgt, pattern = text[406..506)|naive|2.9|2.6|2.4|2.4|2.6|2.6|yes, 1 starts equal to String.indexOf reference|
|random|1000|100|java.util.Random(20262108), alphabet acgt, pattern = text[406..506)|kmp|4.2|4.2|4.2|4.1|4.1|4.2|yes, 1 starts equal to String.indexOf reference|
|random|1000|100|java.util.Random(20262108), alphabet acgt, pattern = text[406..506)|rk|8.5|8.7|8.3|8.4|8.6|8.5|yes, 1 starts equal to String.indexOf reference|
|random|3000|300|java.util.Random(20264108), alphabet acgt, pattern = text[302..602)|naive|6.6|8.1|6.8|6.3|6.1|6.6|yes, 1 starts equal to String.indexOf reference|
|random|3000|300|java.util.Random(20264108), alphabet acgt, pattern = text[302..602)|kmp|9.9|11.0|11.8|11.6|7.9|11.0|yes, 1 starts equal to String.indexOf reference|
|random|3000|300|java.util.Random(20264108), alphabet acgt, pattern = text[302..602)|rk|25.7|25.2|25.1|27.1|28.1|25.7|yes, 1 starts equal to String.indexOf reference|
|all-a (repetitive)|300|30|text a^n, pattern a^m (every start matches)|naive|6.3|7.0|5.3|6.0|5.9|6.0|yes, 271 starts equal to String.indexOf reference|
|all-a (repetitive)|300|30|text a^n, pattern a^m (every start matches)|kmp|11.9|1.6|2.0|1.6|1.6|1.6|yes, 271 starts equal to String.indexOf reference|
|all-a (repetitive)|300|30|text a^n, pattern a^m (every start matches)|rk|16.3|16.6|16.2|17.0|16.3|16.3|yes, 271 starts equal to String.indexOf reference|
|all-a (repetitive)|1000|100|text a^n, pattern a^m (every start matches)|naive|136.0|121.5|109.3|110.5|107.1|110.5|yes, 901 starts equal to String.indexOf reference|
|all-a (repetitive)|1000|100|text a^n, pattern a^m (every start matches)|kmp|18.1|5.2|5.7|5.2|5.6|5.6|yes, 901 starts equal to String.indexOf reference|
|all-a (repetitive)|1000|100|text a^n, pattern a^m (every start matches)|rk|140.4|140.8|140.9|140.3|140.8|140.8|yes, 901 starts equal to String.indexOf reference|
|all-a (repetitive)|3000|300|text a^n, pattern a^m (every start matches)|naive|992.7|959.4|966.2|956.4|967.8|966.2|yes, 2701 starts equal to String.indexOf reference|
|all-a (repetitive)|3000|300|text a^n, pattern a^m (every start matches)|kmp|19.6|17.7|16.9|15.4|15.5|16.9|yes, 2701 starts equal to String.indexOf reference|
|all-a (repetitive)|3000|300|text a^n, pattern a^m (every start matches)|rk|1221.0|1213.1|1191.6|1189.5|1194.2|1194.2|yes, 2701 starts equal to String.indexOf reference|
|ab-periodic (repetitive)|300|30|text (ab)^(n/2), pattern (ab)^(m/2)|naive|5.2|5.4|5.6|5.6|6.0|5.6|yes, 136 starts equal to String.indexOf reference|
|ab-periodic (repetitive)|300|30|text (ab)^(n/2), pattern (ab)^(m/2)|kmp|1.6|1.3|1.3|1.2|1.2|1.3|yes, 136 starts equal to String.indexOf reference|
|ab-periodic (repetitive)|300|30|text (ab)^(n/2), pattern (ab)^(m/2)|rk|9.2|9.4|9.1|9.2|9.2|9.2|yes, 136 starts equal to String.indexOf reference|
|ab-periodic (repetitive)|1000|100|text (ab)^(n/2), pattern (ab)^(m/2)|naive|55.2|53.4|54.4|54.7|72.6|54.7|yes, 451 starts equal to String.indexOf reference|
|ab-periodic (repetitive)|1000|100|text (ab)^(n/2), pattern (ab)^(m/2)|kmp|3.8|3.7|3.9|3.8|4.0|3.8|yes, 451 starts equal to String.indexOf reference|
|ab-periodic (repetitive)|1000|100|text (ab)^(n/2), pattern (ab)^(m/2)|rk|72.5|73.1|72.8|72.7|73.9|72.8|yes, 451 starts equal to String.indexOf reference|
|ab-periodic (repetitive)|3000|300|text (ab)^(n/2), pattern (ab)^(m/2)|naive|502.0|483.6|491.1|482.9|502.0|491.1|yes, 1351 starts equal to String.indexOf reference|
|ab-periodic (repetitive)|3000|300|text (ab)^(n/2), pattern (ab)^(m/2)|kmp|13.4|12.1|12.7|12.1|32.3|12.7|yes, 1351 starts equal to String.indexOf reference|
|ab-periodic (repetitive)|3000|300|text (ab)^(n/2), pattern (ab)^(m/2)|rk|753.4|667.8|701.4|649.5|636.3|667.8|yes, 1351 starts equal to String.indexOf reference|

Observations. On the repetitive inputs naive and RK grow roughly 100 times when the input grows 10 times in both n and m (for `all-a`: 6 → 110 → 966 µs for naive), while KMP stays in the 2–17 µs range. This matches the O(nm) versus O(n+m) bounds, but these medians do not prove asymptotic complexity; they come from three sizes on one machine. On random text a mismatch usually comes after one or two characters, so naive is as fast as KMP, and sometimes faster because it has no preprocessing. RK was slower than both in every case here: the rolling update costs two modulo operations per position and, on `all-a` and `ab-periodic`, every window has an equal hash and is also a true match, so it pays the full O(m) verification as naive does plus the hashing. Some first runs are slower (for example KMP random 300) because of residual JIT/GC noise; the median hides single outliers.

Modulus 101 is deliberately tiny: with only 101 values, about one random window in 101 has a false-positive hash and costs a character comparison (`ab` and `dj` both give 75 with base 31). That is why verification is needed for correctness, and why the worst case is O(nm). This is a property of this small-modulus lab, not of every production hash (a large or randomised modulus makes false candidates rare). The known collision pair is included in the correctness tests above.

## Sources and AI assistance

- Sources: the assignment specification (`docs/Assignment_4_EN.md`), the lecture clarifications file in `docs/`, the Java standard library documentation.
- AI assistance: Claude (Anthropic) assisted with drafting the analysis and setting up the test and benchmark structure.
- My own verification: I wrote and verified the implementation in `Java/source/Algorithms.java`, read the code and explanations, ran `python tools/run.py --check` and `python tests/student/run_tests.py` myself to confirm all checks and tests pass, traced the examples from the assignment by hand, and can fully explain the implemented algorithms.
