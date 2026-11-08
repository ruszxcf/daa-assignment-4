import java.util.*;

/**
 * Self-checking tests for Java/source/Algorithms.java (default package, no framework).
 *
 * Small inputs are checked against independently written oracles (brute force over all subsets,
 * all merge orders, all subsequences, String.indexOf in the TEST only, sorted suffix strings, ...).
 * Ambiguous answers (LCS sequence, merge plan, tree selection) are validated by meaning.
 * Large inputs use different algorithms or closed forms as the oracle.
 *
 * Run from the repository root: python tests/student/run_tests.py
 */
public final class Tests {
    private static int passed;
    private static int failed;

    private static void check(boolean ok, String name) {
        if (ok) {
            passed++;
        } else {
            failed++;
            System.out.println("FAIL: " + name);
        }
    }

    // ---------------------------------------------------------------- plumbing: JSON in, JSON out

    @SuppressWarnings("unchecked")
    private static Map<String, Object> solve(String json) {
        Map<String, Object> request = (Map<String, Object>) Json.parse(json);
        Map<String, Object> result;
        switch ((String) request.get("task")) {
            case "lcs": result = Algorithms.lcs(request); break;
            case "merge": result = Algorithms.merge(request); break;
            case "tree": result = Algorithms.tree(request); break;
            case "naive": result = Algorithms.naive(request); break;
            case "kmp": result = Algorithms.kmp(request); break;
            case "rk": result = Algorithms.rk(request); break;
            case "suffix": result = Algorithms.suffix(request); break;
            case "aho": result = Algorithms.aho(request); break;
            default: throw new IllegalArgumentException("task");
        }
        return (Map<String, Object>) Json.parse(Json.write(result)); // what the grader reads
    }

    private static String q(String s) {
        return Json.write(s);
    }

    private static String intsJson(int[] a) {
        return Arrays.toString(a).replace(" ", "");
    }

    private static String longsJson(long[] a) {
        return Arrays.toString(a).replace(" ", "");
    }

    private static int[] ints(Object o) {
        List<?> l = (List<?>) o;
        int[] r = new int[l.size()];
        for (int i = 0; i < r.length; i++) {
            r[i] = ((Number) l.get(i)).intValue();
        }
        return r;
    }

    private static long num(Object o) {
        return ((Number) o).longValue();
    }

    private static String search(String task, String text, String pat) {
        return "{\"task\":\"" + task + "\",\"text\":" + q(text) + ",\"pattern\":" + q(pat) + "}";
    }

    private static final Random RND = new Random(20261108L);

    private static String randomString(int len, int alphabet) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < len; i++) {
            sb.append((char) ('a' + RND.nextInt(alphabet)));
        }
        return sb.toString();
    }

    // ---------------------------------------------------------------- LCS

    private static boolean isSubsequence(String s, String of) {
        int j = 0;
        for (int i = 0; i < of.length() && j < s.length(); i++) {
            if (of.charAt(i) == s.charAt(j)) {
                j++;
            }
        }
        return j == s.length();
    }

    /** Brute force: try every subsequence of a (|a| <= 12). */
    private static int bruteLcs(String a, String b) {
        int best = 0;
        for (int mask = 0; mask < (1 << a.length()); mask++) {
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < a.length(); i++) {
                if ((mask >> i & 1) != 0) {
                    sb.append(a.charAt(i));
                }
            }
            if (sb.length() > best && isSubsequence(sb.toString(), b)) {
                best = sb.length();
            }
        }
        return best;
    }

    private static boolean lcsValid(String a, String b, Map<String, Object> r, int expectedLength) {
        String seq = (String) r.get("sequence");
        return num(r.get("length")) == expectedLength && seq.length() == expectedLength
                && isSubsequence(seq, a) && isSubsequence(seq, b);
    }

    private static void lcsTests() {
        Map<String, Object> r = solve("{\"task\":\"lcs\",\"a\":\"tea_time\",\"b\":\"team\"}");
        check(lcsValid("tea_time", "team", r, 4) && "team".equals(r.get("sequence")), "lcs spec example");
        check(lcsValid("", "abc", solve("{\"task\":\"lcs\",\"a\":\"\",\"b\":\"abc\"}"), 0), "lcs empty a");
        check(lcsValid("abc", "", solve("{\"task\":\"lcs\",\"a\":\"abc\",\"b\":\"\"}"), 0), "lcs empty b");
        check(lcsValid("", "", solve("{\"task\":\"lcs\",\"a\":\"\",\"b\":\"\"}"), 0), "lcs both empty");
        check(lcsValid("abc", "xyz", solve("{\"task\":\"lcs\",\"a\":\"abc\",\"b\":\"xyz\"}"), 0), "lcs nothing in common");
        check(lcsValid("aaaa", "aa", solve("{\"task\":\"lcs\",\"a\":\"aaaa\",\"b\":\"aa\"}"), 2), "lcs repeated characters");
        // subsequence, not substring: LCS of "axbxc" and "abc" is 3 although "abc" is not contiguous
        check(lcsValid("axbxc", "abc", solve("{\"task\":\"lcs\",\"a\":\"axbxc\",\"b\":\"abc\"}"), 3), "lcs non-contiguous");
        // several optimal sequences: "ab" and "ba" are both optimal for "ab" / "ba"; any one is accepted
        check(lcsValid("ab", "ba", solve("{\"task\":\"lcs\",\"a\":\"ab\",\"b\":\"ba\"}"), 1), "lcs alternative optima");
        check(lcsValid("A a,B", "a A,b", solve("{\"task\":\"lcs\",\"a\":\"A a,B\",\"b\":\"a A,b\"}"), bruteLcs("A a,B", "a A,b")),
                "lcs case, space and punctuation");
        for (int t = 0; t < 600; t++) {
            String a = randomString(RND.nextInt(11), 1 + RND.nextInt(3));
            String b = randomString(RND.nextInt(11), 1 + RND.nextInt(3));
            check(lcsValid(a, b, solve("{\"task\":\"lcs\",\"a\":" + q(a) + ",\"b\":" + q(b) + "}"), bruteLcs(a, b)),
                    "lcs random " + a + "/" + b);
        }
        // validator self-test: a non-subsequence or wrong length must be rejected
        Map<String, Object> bad = new HashMap<>();
        bad.put("length", 4L);
        bad.put("sequence", "mate");
        check(!lcsValid("tea_time", "team", bad, 4), "lcs validator rejects a non-subsequence");
        bad.put("length", 3L);
        bad.put("sequence", "tea");
        check(!lcsValid("tea_time", "team", bad, 4), "lcs validator rejects a too short sequence");
    }

    // ---------------------------------------------------------------- merge

    /** Brute force over ALL merge orders of adjacent blocks (n <= 7). */
    private static long bruteMerge(long[] blocks) {
        if (blocks.length <= 1) {
            return 0;
        }
        long best = Long.MAX_VALUE;
        for (int i = 0; i + 1 < blocks.length; i++) {
            long[] next = new long[blocks.length - 1];
            for (int j = 0, k = 0; j < blocks.length; j++) {
                if (j == i) {
                    next[k++] = blocks[i] + blocks[i + 1];
                    j++;
                } else {
                    next[k++] = blocks[j];
                }
            }
            best = Math.min(best, blocks[i] + blocks[i + 1] + bruteMerge(next));
        }
        return best;
    }

    /** Simulates the plan: every step must merge two CURRENT adjacent blocks; returns the paid cost or -1. */
    private static long simulatePlan(long[] sizes, int[][] plan) {
        int n = sizes.length;
        if (plan.length != Math.max(n - 1, 0)) {
            return -1;
        }
        long[] prefix = new long[n + 1];
        for (int i = 0; i < n; i++) {
            prefix[i + 1] = prefix[i] + sizes[i];
        }
        boolean[][] block = new boolean[n][n]; // block[l][r] = [l,r] is a current block
        for (int i = 0; i < n; i++) {
            block[i][i] = true;
        }
        long cost = 0;
        for (int[] step : plan) {
            int l = step[0];
            int k = step[1];
            int r = step[2];
            if (!(l <= k && k < r && r < n) || !block[l][k] || !block[k + 1][r]) {
                return -1;
            }
            block[l][k] = false;
            block[k + 1][r] = false;
            block[l][r] = true;
            cost += prefix[r + 1] - prefix[l];
        }
        return n == 0 || block[0][n - 1] ? cost : -1;
    }

    private static int[][] rows(Object o) {
        List<?> l = (List<?>) o;
        int[][] r = new int[l.size()][];
        for (int i = 0; i < r.length; i++) {
            r[i] = ints(l.get(i));
        }
        return r;
    }

    private static boolean mergeValid(long[] sizes, Map<String, Object> r, long expectedCost) {
        int[][] plan = rows(r.get("plan"));
        return num(r.get("cost")) == expectedCost && simulatePlan(sizes, plan) == expectedCost;
    }

    private static String mergeReq(long[] sizes) {
        return "{\"task\":\"merge\",\"sizes\":" + longsJson(sizes) + "}";
    }

    private static void mergeTests() {
        Map<String, Object> r = solve(mergeReq(new long[] {2, 3, 4}));
        check(mergeValid(new long[] {2, 3, 4}, r, 14), "merge spec example");
        check(mergeValid(new long[0], solve(mergeReq(new long[0])), 0), "merge n=0");
        check(mergeValid(new long[] {5}, solve(mergeReq(new long[] {5})), 0), "merge n=1");
        check(mergeValid(new long[] {0, 0, 0}, solve(mergeReq(new long[] {0, 0, 0})), 0), "merge zeros");
        check(mergeValid(new long[] {1, 1, 1, 1}, solve(mergeReq(new long[] {1, 1, 1, 1})), 8), "merge equal sizes (many optimal plans)");
        // adjacency matters: greedy "two smallest" (1 and 1 at the ends) would be wrong
        long[] adj = {1, 100, 1};
        check(mergeValid(adj, solve(mergeReq(adj)), bruteMerge(adj)), "merge adjacency: [1,100,1]");
        long big = 1_000_000_000L;
        long[] huge = new long[200];
        Arrays.fill(huge, big);
        long fixed = bruteMergeEqualCost(200, big);
        check(mergeValid(huge, solve(mergeReq(huge)), fixed), "merge 200 blocks of 1e9: cost beyond 32 bit");
        for (int t = 0; t < 800; t++) {
            int n = RND.nextInt(8);
            long[] sizes = new long[n];
            for (int i = 0; i < n; i++) {
                sizes[i] = RND.nextInt(4) == 0 ? 0 : RND.nextInt(30);
            }
            check(mergeValid(sizes, solve(mergeReq(sizes)), bruteMerge(sizes)), "merge random " + Arrays.toString(sizes));
        }
        // validator self-tests: a plan that merges non-adjacent blocks, or in the wrong order, is rejected
        check(simulatePlan(new long[] {2, 3, 4}, new int[][] {{0, 1, 2}, {0, 0, 1}}) == -1, "merge validator: parent before children");
        check(simulatePlan(new long[] {2, 3, 4}, new int[][] {{0, 0, 1}}) == -1, "merge validator: too few merges");
        check(simulatePlan(new long[] {2, 3, 4}, new int[][] {{0, 0, 2}, {0, 1, 2}}) == -1, "merge validator: non-adjacent pair");
    }

    /** For n equal blocks the optimum is a balanced merge tree: cost = size * sum over leaves of their depth. */
    private static long bruteMergeEqualCost(int n, long size) {
        // Huffman-like on equal weights: repeatedly merging the two smallest current blocks is optimal for equal sizes
        // and keeps them adjacent (merge neighbours level by level), so the same value is obtained in O(n log n).
        PriorityQueue<Long> pq = new PriorityQueue<>();
        for (int i = 0; i < n; i++) {
            pq.add(size);
        }
        long cost = 0;
        while (pq.size() > 1) {
            long s = pq.poll() + pq.poll();
            cost += s;
            pq.add(s);
        }
        return cost;
    }

    // ---------------------------------------------------------------- tree

    private static String treeReq(int n, long[] w, int[][] edges) {
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < edges.length; i++) {
            sb.append(i > 0 ? "," : "").append("[").append(edges[i][0]).append(",").append(edges[i][1]).append("]");
        }
        return "{\"task\":\"tree\",\"n\":" + n + ",\"weights\":" + longsJson(w) + ",\"edges\":" + sb.append("]") + "}";
    }

    private static boolean treeValid(int n, long[] w, int[][] edges, Map<String, Object> r, long expected) {
        int[] sel = ints(r.get("selected"));
        boolean[] in = new boolean[n];
        long sum = 0;
        for (int v : sel) {
            if (v < 0 || v >= n || in[v]) {
                return false;
            }
            in[v] = true;
            sum += w[v];
        }
        for (int[] e : edges) {
            if (in[e[0]] && in[e[1]]) {
                return false;
            }
        }
        return sum == expected && num(r.get("weight")) == expected;
    }

    private static long bruteTree(int n, long[] w, int[][] edges) {
        long best = 0;
        for (int mask = 0; mask < (1 << n); mask++) {
            boolean ok = true;
            for (int[] e : edges) {
                if ((mask >> e[0] & 1) != 0 && (mask >> e[1] & 1) != 0) {
                    ok = false;
                    break;
                }
            }
            if (ok) {
                long s = 0;
                for (int i = 0; i < n; i++) {
                    if ((mask >> i & 1) != 0) {
                        s += w[i];
                    }
                }
                best = Math.max(best, s);
            }
        }
        return best;
    }

    private static int[][] randomTree(int n, int shape) {
        int[][] e = new int[Math.max(n - 1, 0)][];
        for (int v = 1; v < n; v++) {
            int parent = shape == 0 ? RND.nextInt(v) : shape == 1 ? v - 1 : 0; // random / chain / star
            e[v - 1] = RND.nextBoolean() ? new int[] {parent, v} : new int[] {v, parent};
        }
        // relabel vertices randomly so that the root 0 is not special
        int[] perm = new int[n];
        for (int i = 0; i < n; i++) {
            perm[i] = i;
        }
        for (int i = n - 1; i > 0; i--) {
            int j = RND.nextInt(i + 1);
            int t = perm[i];
            perm[i] = perm[j];
            perm[j] = t;
        }
        for (int[] x : e) {
            x[0] = perm[x[0]];
            x[1] = perm[x[1]];
        }
        return e;
    }

    private static void treeTests() {
        check(treeValid(3, new long[] {4, 9, 6}, new int[][] {{0, 1}, {1, 2}},
                solve(treeReq(3, new long[] {4, 9, 6}, new int[][] {{0, 1}, {1, 2}})), 10), "tree spec example");
        check(treeValid(0, new long[0], new int[0][], solve(treeReq(0, new long[0], new int[0][])), 0), "tree n=0");
        check(treeValid(1, new long[] {7}, new int[0][], solve(treeReq(1, new long[] {7}, new int[0][])), 7), "tree n=1 positive");
        check(treeValid(1, new long[] {-7}, new int[0][], solve(treeReq(1, new long[] {-7}, new int[0][])), 0), "tree n=1 negative: empty set");
        long[] neg = {-1, -2, -3};
        check(treeValid(3, neg, new int[][] {{0, 1}, {1, 2}}, solve(treeReq(3, neg, new int[][] {{0, 1}, {1, 2}})), 0), "tree all negative");
        long[] zero = {0, 0, 0, 0};
        int[][] star = {{0, 1}, {0, 2}, {0, 3}};
        check(treeValid(4, zero, star, solve(treeReq(4, zero, star)), 0), "tree zeros");
        long[] tie = {5, 5};
        check(treeValid(2, tie, new int[][] {{0, 1}}, solve(treeReq(2, tie, new int[][] {{0, 1}})), 5), "tree tie between the two ends");
        long[] big = {1_000_000_000L, 1_000_000_000L, 1_000_000_000L};
        check(treeValid(3, big, new int[][] {{0, 1}, {1, 2}}, solve(treeReq(3, big, new int[][] {{0, 1}, {1, 2}})), 2_000_000_000L),
                "tree sum beyond 32 bit");
        for (int t = 0; t < 1500; t++) {
            int n = RND.nextInt(13);
            int[][] edges = randomTree(n, RND.nextInt(3));
            long[] w = new long[n];
            for (int i = 0; i < n; i++) {
                w[i] = RND.nextInt(5) == 0 ? 0 : RND.nextInt(21) - 8;
            }
            check(treeValid(n, w, edges, solve(treeReq(n, w, edges)), bruteTree(n, w, edges)), "tree random #" + t);
        }
        // validator self-tests
        Map<String, Object> bad = new HashMap<>();
        bad.put("weight", 13L);
        bad.put("selected", List.of(0L, 1L)); // adjacent
        check(!treeValid(3, new long[] {4, 9, 6}, new int[][] {{0, 1}, {1, 2}}, bad, 13), "tree validator rejects adjacent vertices");
        bad.put("weight", 8L);
        bad.put("selected", List.of(0L, 0L));
        check(!treeValid(3, new long[] {4, 9, 6}, new int[][] {{0, 1}, {1, 2}}, bad, 8), "tree validator rejects a repeated vertex");
    }

    // ---------------------------------------------------------------- naive / kmp / rk

    /** Oracle (test only): all overlapping starts with indexOf. */
    private static int[] oracleSearch(String text, String pat) {
        List<Integer> out = new ArrayList<>();
        if (pat.isEmpty()) {
            for (int i = 0; i <= text.length(); i++) {
                out.add(i);
            }
        } else {
            for (int i = text.indexOf(pat); i >= 0; i = text.indexOf(pat, i + 1)) {
                out.add(i);
            }
        }
        int[] r = new int[out.size()];
        for (int i = 0; i < r.length; i++) {
            r[i] = out.get(i);
        }
        return r;
    }

    private static void searchCase(String text, String pat, String name) {
        int[] expected = oracleSearch(text, pat);
        for (String task : new String[] {"naive", "kmp", "rk"}) {
            check(Arrays.equals(ints(solve(search(task, text, pat)).get("positions")), expected), task + ": " + name);
        }
    }

    private static void searchTests() {
        searchCase("aaaaa", "aaa", "overlaps (spec example)");
        searchCase("abababa", "aba", "overlaps via fallback (spec example)");
        searchCase("abdj", "ab", "ab/dj collision (spec example)");
        searchCase("", "", "empty text and pattern");
        searchCase("", "a", "empty text");
        searchCase("abc", "", "empty pattern: n+1 starts");
        searchCase("abc", "abcd", "pattern longer than text");
        searchCase("abc", "abc", "pattern equals text");
        searchCase("abc", "c", "last valid start");
        searchCase("Hello, World! hello", "hello", "case sensitivity");
        searchCase("a.b*c a.b*c", ".b*", "punctuation is ordinary");
        searchCase("  a  a ", "  ", "spaces are ordinary characters");
        searchCase("aaaa", "aaaa", "all equal, full length");
        searchCase("abcabcabc", "abcabc", "periodic overlap");
        for (int t = 0; t < 1500; t++) {
            String text = randomString(RND.nextInt(25), 1 + RND.nextInt(3));
            String pat = randomString(RND.nextInt(6), 1 + RND.nextInt(3));
            searchCase(text, pat, "random '" + text + "' / '" + pat + "'");
        }
        // Rabin-Karp: base 31, mod 101 collisions. "ab" and "dj" both hash to 75, so "dj" is only a candidate.
        check(rkHash("ab") == 75 && rkHash("dj") == 75, "rk hash of ab and dj is 75 (base 31, mod 101)");
        check(Arrays.equals(ints(solve(search("rk", "abdjabdj", "ab")).get("positions")), new int[] {0, 4}),
                "rk collision windows are verified, not reported");
        // negative remainder: the outgoing character weighs more than the whole rest of the hash
        searchCase("zzzzzzzzzzaaaaaaaaaa", "zzzaa", "rk negative remainder normalisation");
        searchCase("~~~~ ~~~~~ ~~~", "~~ ~", "rk negative remainder, high characters");
        // many collisions at once: find a second colliding pair by search and use it as extra check
        List<String> same = new ArrayList<>();
        for (char a = 32; a < 127 && same.size() < 4; a++) {
            for (char b = 32; b < 127; b++) {
                if (rkHash("" + a + b) == rkHash("ab") && !("" + a + b).equals("ab")) {
                    same.add("" + a + b);
                }
            }
        }
        StringBuilder text = new StringBuilder();
        for (String s : same) {
            text.append(s);
        }
        text.append("ab");
        searchCase(text.toString(), "ab", "rk several colliding two-character windows");
    }

    private static int rkHash(String s) {
        int h = 0;
        for (int i = 0; i < s.length(); i++) {
            h = (h * 31 + s.charAt(i)) % 101;
        }
        return h;
    }

    // ---------------------------------------------------------------- suffix array

    private static void suffixCase(String text, String name) {
        Map<String, Object> r = solve("{\"task\":\"suffix\",\"text\":" + q(text) + "}");
        int n = text.length();
        Integer[] order = new Integer[n];
        for (int i = 0; i < n; i++) {
            order[i] = i;
        }
        Arrays.sort(order, (x, y) -> text.substring(x).compareTo(text.substring(y))); // oracle: sorted suffix strings
        int[] sa = ints(r.get("sa"));
        int[] lcp = ints(r.get("lcp"));
        boolean ok = sa.length == n && lcp.length == n;
        for (int i = 0; ok && i < n; i++) {
            ok = sa[i] == order[i];
        }
        int maxLcp = 0;
        for (int i = 0; ok && i < n; i++) {
            int expected = 0;
            if (i > 0) {
                String a = text.substring(sa[i - 1]);
                String b = text.substring(sa[i]);
                while (expected < a.length() && expected < b.length() && a.charAt(expected) == b.charAt(expected)) {
                    expected++;
                }
            }
            ok = lcp[i] == expected;
            maxLcp = Math.max(maxLcp, expected);
        }
        check(ok, "suffix sa/lcp: " + name);
        String rep = (String) r.get("repeated");
        boolean repOk = rep.length() == maxLcp;
        if (repOk && maxLcp > 0) {
            repOk = oracleSearch(text, rep).length >= 2; // occurs at two distinct starts (overlaps allowed)
        }
        // no longer substring may occur twice: the longest repeat is exactly max(lcp)
        if (repOk && n > 0 && maxLcp + 1 <= n) {
            boolean longer = false;
            for (int i = 0; i + maxLcp + 1 <= n && !longer; i++) {
                longer = oracleSearch(text, text.substring(i, i + maxLcp + 1)).length >= 2;
            }
            repOk = !longer;
        }
        check(repOk, "suffix repeated: " + name);
    }

    private static void suffixTests() {
        Map<String, Object> r = solve("{\"task\":\"suffix\",\"text\":\"banana\"}");
        check(Arrays.equals(ints(r.get("sa")), new int[] {5, 3, 1, 0, 4, 2})
                && Arrays.equals(ints(r.get("lcp")), new int[] {0, 1, 3, 0, 0, 2}) && "ana".equals(r.get("repeated")),
                "suffix spec example (banana)");
        suffixCase("", "empty text");
        suffixCase("a", "single character");
        suffixCase("aaaaaaaa", "all equal");
        suffixCase("abcdefg", "all distinct");
        suffixCase("gfedcba", "all distinct, descending");
        suffixCase("abababab", "periodic");
        suffixCase("mississippi", "mississippi");
        suffixCase("a b a b", "spaces");
        suffixCase("Aa Aa", "case and space");
        for (int t = 0; t < 1200; t++) {
            suffixCase(randomString(RND.nextInt(30), 1 + RND.nextInt(3)), "random #" + t);
        }
        Map<String, Object> e = solve("{\"task\":\"suffix\",\"text\":\"\"}");
        check(ints(e.get("sa")).length == 0 && ints(e.get("lcp")).length == 0 && "".equals(e.get("repeated")), "suffix empty output shape");
        Map<String, Object> d = solve("{\"task\":\"suffix\",\"text\":\"abc\"}");
        check("".equals(d.get("repeated")), "suffix: no repeated substring gives the empty string");
    }

    // ---------------------------------------------------------------- Aho-Corasick

    private static long[][] ahoOracle(List<String> patterns, List<String> texts) {
        long[][] counts = new long[texts.size()][patterns.size()];
        for (int t = 0; t < texts.size(); t++) {
            for (int p = 0; p < patterns.size(); p++) {
                counts[t][p] = oracleSearch(texts.get(t), patterns.get(p)).length;
            }
        }
        return counts;
    }

    private static String ahoReq(List<String> patterns, List<String> texts) {
        StringJoiner pj = new StringJoiner(",", "[", "]");
        for (String s : patterns) {
            pj.add(q(s));
        }
        StringJoiner tj = new StringJoiner(",", "[", "]");
        for (String s : texts) {
            tj.add(q(s));
        }
        return "{\"task\":\"aho\",\"patterns\":" + pj + ",\"texts\":" + tj + "}";
    }

    private static boolean ahoMatches(List<String> patterns, List<String> texts, boolean checkOracle) {
        Map<String, Object> r = solve(ahoReq(patterns, texts));
        List<?> rowsList = (List<?>) r.get("counts");
        if (rowsList.size() != texts.size()) {
            return false;
        }
        long[][] expected = checkOracle ? ahoOracle(patterns, texts) : null;
        for (int t = 0; t < texts.size(); t++) {
            List<?> row = (List<?>) rowsList.get(t);
            if (row.size() != patterns.size()) {
                return false;
            }
            for (int p = 0; checkOracle && p < patterns.size(); p++) {
                if (num(row.get(p)) != expected[t][p]) {
                    return false;
                }
            }
        }
        return true;
    }

    private static void ahoTests() {
        Map<String, Object> r = solve("{\"task\":\"aho\",\"patterns\":[\"he\",\"she\",\"he\"],\"texts\":[\"sheshe\",\"sheshe\",\"\"]}");
        check(r.get("counts").equals(List.of(List.of(2L, 2L, 2L), List.of(2L, 2L, 2L), List.of(0L, 0L, 0L))), "aho spec example");
        check(ahoMatches(List.of(), List.of("abc", ""), true) && ((List<?>) solve(ahoReq(List.of(), List.of("abc", ""))).get("counts")).size() == 2,
                "aho empty dictionary: one empty row per text");
        check(((List<?>) solve(ahoReq(List.of("a"), List.of())).get("counts")).isEmpty(), "aho empty texts array");
        check(ahoMatches(List.of("a", "aa", "aaa"), List.of("aaaa"), true), "aho overlaps (a, aa, aaa in aaaa)");
        check(ahoMatches(List.of("a", "ab", "bab", "bc", "bca", "c", "caa"), List.of("abccab", "bcaa"), true), "aho classic dictionary");
        check(ahoMatches(List.of("x", "x", "x"), List.of("xxx", "x", ""), true), "aho duplicates share counts");
        check(ahoMatches(List.of("abc", "bc", "c"), List.of("abc"), true), "aho suffix patterns (abc, bc, c)");
        check(ahoMatches(List.of("A", "a", " ", "A a"), List.of("A a A a"), true), "aho case sensitive, spaces");
        for (int t = 0; t < 800; t++) {
            List<String> pats = new ArrayList<>();
            int np = RND.nextInt(6);
            for (int i = 0; i < np; i++) {
                pats.add(randomString(1 + RND.nextInt(4), 1 + RND.nextInt(3)));
            }
            List<String> texts = new ArrayList<>();
            int nt = RND.nextInt(4);
            for (int i = 0; i < nt; i++) {
                texts.add(randomString(RND.nextInt(20), 1 + RND.nextInt(3)));
            }
            check(ahoMatches(pats, texts, true), "aho random " + pats + " in " + texts);
        }
        // consecutive requests with different dictionaries: nothing may leak from the first one
        solve(ahoReq(List.of("zzz", "q"), List.of("zzzq")));
        check(ahoMatches(List.of("a"), List.of("aaa"), true), "aho state reset after another dictionary");
        // validator self-test: the oracle really differs when a suffix pattern is ignored
        check(ahoOracle(List.of("she", "he"), List.of("she"))[0][1] == 1, "aho oracle counts the suffix pattern he inside she");
    }

    // ---------------------------------------------------------------- consecutive mixed requests

    private static void resetTest() {
        String[] batch = {
                "{\"task\":\"lcs\",\"a\":\"tea_time\",\"b\":\"team\"}",
                mergeReq(new long[] {2, 3, 4}),
                treeReq(3, new long[] {4, 9, 6}, new int[][] {{0, 1}, {1, 2}}),
                search("naive", "aaaaa", "aaa"), search("kmp", "abababa", "aba"), search("rk", "abdj", "ab"),
                "{\"task\":\"suffix\",\"text\":\"banana\"}",
                ahoReq(List.of("he", "she", "he"), List.of("sheshe", "sheshe", "")),
                "{\"task\":\"suffix\",\"text\":\"\"}", mergeReq(new long[0]), treeReq(0, new long[0], new int[0][]),
                ahoReq(List.of(), List.of("x")),
        };
        List<String> first = new ArrayList<>();
        for (String b : batch) {
            first.add(Json.write(solve(b)));
        }
        boolean same = true;
        for (int pass = 0; pass < 3; pass++) {
            for (int i = batch.length - 1; i >= 0; i--) {
                same &= first.get(i).equals(Json.write(solve(batch[i])));
            }
        }
        check(same, "state reset: repeated and reordered mixed requests give identical results");
    }

    // ---------------------------------------------------------------- large inputs (full domains)

    private static void largeTests() {
        long t0 = System.nanoTime();

        // LCS 1000 x 1000: length from an independent rolling-row DP, sequence validated by meaning
        for (int alphabet : new int[] {2, 26}) {
            String a = randomString(1000, alphabet);
            String b = randomString(1000, alphabet);
            int[] prev = new int[1001];
            int[] cur = new int[1001];
            for (int i = 1; i <= 1000; i++) {
                for (int j = 1; j <= 1000; j++) {
                    cur[j] = a.charAt(i - 1) == b.charAt(j - 1) ? prev[j - 1] + 1 : Math.max(prev[j], cur[j - 1]);
                }
                int[] tmp = prev;
                prev = cur;
                cur = tmp;
            }
            check(lcsValid(a, b, solve("{\"task\":\"lcs\",\"a\":" + q(a) + ",\"b\":" + q(b) + "}"), prev[1000]), "lcs 1000x1000 alphabet " + alphabet);
        }
        String ones = "a".repeat(1000);
        check(lcsValid(ones, ones, solve("{\"task\":\"lcs\",\"a\":" + q(ones) + ",\"b\":" + q(ones) + "}"), 1000), "lcs 1000 equal characters");
        String long2000 = randomString(1, 3).repeat(1);
        check(lcsValid("a".repeat(1000), "", solve("{\"task\":\"lcs\",\"a\":" + q("a".repeat(1000)) + ",\"b\":\"\"}"), 0) && long2000.length() == 1,
                "lcs 1000 vs empty");

        // merge n = 200 with sizes up to 1e9: independent top-down memoised recursion
        long[] sizes = new long[200];
        for (int i = 0; i < 200; i++) {
            sizes[i] = RND.nextInt(1_000_000_001);
        }
        check(mergeValid(sizes, solve(mergeReq(sizes)), topDownMerge(sizes)), "merge n=200 random sizes up to 1e9");
        long[] mixed = new long[200];
        for (int i = 0; i < 200; i++) {
            mixed[i] = i % 2 == 0 ? 0 : 1_000_000_000L;
        }
        check(mergeValid(mixed, solve(mergeReq(mixed)), topDownMerge(mixed)), "merge n=200 zeros and 1e9 alternating");

        // tree n = 100000: chain, star, random; independent leaf-peeling DP
        int n = 100_000;
        for (int shape = 0; shape < 3; shape++) {
            int[][] edges = randomTree(n, shape);
            long[] w = new long[n];
            for (int i = 0; i < n; i++) {
                w[i] = RND.nextInt(2_000_000_001) - 1_000_000_000L;
            }
            check(treeValid(n, w, edges, solve(treeReq(n, w, edges)), leafPeelingTree(n, w, edges)), "tree n=100000 shape " + shape);
        }
        int[][] chain = new int[n - 1][];
        long[] ones1e9 = new long[n];
        for (int i = 0; i + 1 < n; i++) {
            chain[i] = new int[] {i, i + 1};
        }
        Arrays.fill(ones1e9, 1_000_000_000L);
        check(treeValid(n, ones1e9, chain, solve(treeReq(n, ones1e9, chain)), 1_000_000_000L * (n / 2)), "tree chain 100000: total 5e13 beyond 32 bit");

        // search: random and periodic worst cases inside every algorithm's own domain
        String text = randomString(200_000, 2);
        String pat = randomString(8, 2);
        check(Arrays.equals(ints(solve(search("kmp", text, pat)).get("positions")), oracleSearch(text, pat)), "kmp random 200000 / 8");
        String as = "a".repeat(200_000);
        String half = "a".repeat(100_000);
        check(Arrays.equals(ints(solve(search("kmp", as, half)).get("positions")), oracleSearch(as, half)), "kmp 200000 a / 100000 a (100001 matches)");
        check(Arrays.equals(ints(solve(search("kmp", as, "")).get("positions")), oracleSearch(as, "")), "kmp empty pattern on 200000 characters");
        String t3 = "a".repeat(3000);
        check(Arrays.equals(ints(solve(search("naive", t3, "a".repeat(1500))).get("positions")), oracleSearch(t3, "a".repeat(1500))),
                "naive 3000 a / 1500 a");
        check(Arrays.equals(ints(solve(search("naive", t3, t3)).get("positions")), new int[] {0}), "naive 3000/3000");
        String t10 = "a".repeat(10_000);
        String p2000 = "a".repeat(2000);
        check(Arrays.equals(ints(solve(search("rk", t10, p2000)).get("positions")), oracleSearch(t10, p2000)), "rk 10000 a / 2000 a (all windows are candidates)");
        String rkText = randomString(10_000, 2);
        String rkPat = randomString(2000, 2);
        check(Arrays.equals(ints(solve(search("rk", rkText, rkPat)).get("positions")), oracleSearch(rkText, rkPat)), "rk random 10000 / 2000");
        String ab = "ab".repeat(5000);
        check(Arrays.equals(ints(solve(search("rk", ab, "ab".repeat(1000))).get("positions")), oracleSearch(ab, "ab".repeat(1000))), "rk periodic 10000 / 2000");

        // suffix n = 50000: validate sa/lcp/repeated with direct character comparisons
        String[] texts = {"a".repeat(50_000), randomString(50_000, 2), randomString(50_000, 26), "ab".repeat(25_000), "abc".repeat(16_666) + "ab"};
        for (int i = 0; i < texts.length; i++) {
            check(suffixValidLarge(texts[i], solve("{\"task\":\"suffix\",\"text\":" + q(texts[i]) + "}")), "suffix n=50000 text #" + i);
        }

        // aho: 10000 patterns (total length 100000), dense matches, 100 texts
        List<String> pats = new ArrayList<>();
        int totalLen = 0;
        while (pats.size() < 10_000) {
            String s = randomString(1 + RND.nextInt(19), 3);
            if (totalLen + s.length() > 100_000) {
                break;
            }
            pats.add(s);
            totalLen += s.length();
        }
        List<String> tx = new ArrayList<>();
        for (int i = 0; i < 100; i++) {
            tx.add(randomString(2000, 3));
        }
        check(ahoMatchesFast(pats, tx), "aho 10000 random patterns, 100 texts of 2000 characters");
        // dense: patterns a, aa, ..., a^446 (total length 99681) and one text of 200000 a: count of a^j is N-j+1
        List<String> dense = new ArrayList<>();
        int sum = 0;
        for (int j = 1; sum + j <= 100_000; j++) {
            dense.add("a".repeat(j));
            sum += j;
        }
        Map<String, Object> r = solve(ahoReq(dense, List.of("a".repeat(200_000))));
        List<?> row = (List<?>) ((List<?>) r.get("counts")).get(0);
        boolean denseOk = row.size() == dense.size();
        for (int j = 1; denseOk && j <= dense.size(); j++) {
            denseOk = num(row.get(j - 1)) == 200_000L - j + 1;
        }
        check(denseOk, "aho dense matches: a^j counted N-j+1 times in 200000 a");
        List<String> dup = new ArrayList<>();
        for (int i = 0; i < 10_000; i++) {
            dup.add("ab");
        }
        r = solve(ahoReq(dup, List.of("ab".repeat(100_000))));
        row = (List<?>) ((List<?>) r.get("counts")).get(0);
        boolean dupOk = row.size() == 10_000;
        for (Object c : row) {
            dupOk &= num(c) == 100_000L;
        }
        check(dupOk, "aho 10000 duplicates of ab in a 200000-character text: every column 100000");

        System.out.printf("  large tests finished in %.1f s%n", (System.nanoTime() - t0) / 1e9);
    }

    /** For the big aho random test: spot-check 30 random patterns with the indexOf oracle (all columns are cheap to size-check). */
    private static boolean ahoMatchesFast(List<String> pats, List<String> texts) {
        Map<String, Object> r = solve(ahoReq(pats, texts));
        List<?> rowsList = (List<?>) r.get("counts");
        if (rowsList.size() != texts.size()) {
            return false;
        }
        for (int t = 0; t < texts.size(); t++) {
            List<?> row = (List<?>) rowsList.get(t);
            if (row.size() != pats.size()) {
                return false;
            }
            for (int s = 0; s < 40; s++) {
                int p = RND.nextInt(pats.size());
                if (num(row.get(p)) != oracleSearch(texts.get(t), pats.get(p)).length) {
                    return false;
                }
            }
        }
        // a full comparison on the first three texts
        for (int t = 0; t < 3; t++) {
            List<?> row = (List<?>) rowsList.get(t);
            for (int p = 0; p < pats.size(); p++) {
                if (num(row.get(p)) != oracleSearch(texts.get(t), pats.get(p)).length) {
                    return false;
                }
            }
        }
        return true;
    }

    private static long topDownMerge(long[] sizes) {
        int n = sizes.length;
        long[] prefix = new long[n + 1];
        for (int i = 0; i < n; i++) {
            prefix[i + 1] = prefix[i] + sizes[i];
        }
        long[][] memo = new long[n][n];
        for (long[] row : memo) {
            Arrays.fill(row, -1);
        }
        return topDown(0, n - 1, prefix, memo);
    }

    private static long topDown(int l, int r, long[] prefix, long[][] memo) {
        if (l >= r) {
            return 0;
        }
        if (memo[l][r] >= 0) {
            return memo[l][r];
        }
        long best = Long.MAX_VALUE;
        for (int k = l; k < r; k++) {
            best = Math.min(best, topDown(l, k, prefix, memo) + topDown(k + 1, r, prefix, memo));
        }
        return memo[l][r] = best + prefix[r + 1] - prefix[l];
    }

    /** Independent tree oracle: leaf peeling (Kahn style) instead of DFS order. */
    private static long leafPeelingTree(int n, long[] w, int[][] edges) {
        int[] deg = new int[n];
        List<List<Integer>> adj = new ArrayList<>();
        for (int i = 0; i < n; i++) {
            adj.add(new ArrayList<>());
        }
        for (int[] e : edges) {
            adj.get(e[0]).add(e[1]);
            adj.get(e[1]).add(e[0]);
            deg[e[0]]++;
            deg[e[1]]++;
        }
        long[] take = w.clone();
        long[] skip = new long[n];
        boolean[] removed = new boolean[n];
        ArrayDeque<Integer> leaves = new ArrayDeque<>();
        for (int i = 0; i < n; i++) {
            if (deg[i] <= 1) {
                leaves.add(i);
            }
        }
        long answer = 0;
        int processed = 0;
        while (!leaves.isEmpty()) {
            int v = leaves.poll();
            if (removed[v]) {
                continue;
            }
            removed[v] = true;
            processed++;
            int parent = -1;
            for (int u : adj.get(v)) {
                if (!removed[u]) {
                    parent = u;
                }
            }
            if (parent < 0 || processed == n) {
                answer = Math.max(take[v], skip[v]);
                continue;
            }
            take[parent] += skip[v];
            skip[parent] += Math.max(take[v], skip[v]);
            if (--deg[parent] == 1) {
                leaves.add(parent);
            }
        }
        return answer;
    }

    private static boolean suffixValidLarge(String text, Map<String, Object> r) {
        int n = text.length();
        int[] sa = ints(r.get("sa"));
        int[] lcp = ints(r.get("lcp"));
        if (sa.length != n || lcp.length != n || lcp[0] != 0) {
            return false;
        }
        boolean[] seen = new boolean[n];
        for (int s : sa) {
            if (s < 0 || s >= n || seen[s]) {
                return false;
            }
            seen[s] = true;
        }
        int max = 0;
        for (int i = 1; i < n; i++) {
            int a = sa[i - 1];
            int b = sa[i];
            int h = 0;
            while (a + h < n && b + h < n && text.charAt(a + h) == text.charAt(b + h)) {
                h++;
            }
            if (h != lcp[i]) {
                return false;
            }
            // order: the suffix after the common prefix must be smaller (or the first one ended)
            boolean less = a + h == n || (b + h < n && text.charAt(a + h) < text.charAt(b + h));
            if (!less) {
                return false;
            }
            max = Math.max(max, h);
        }
        String rep = (String) r.get("repeated");
        if (rep.length() != max) {
            return false;
        }
        if (max > 0) {
            int first = text.indexOf(rep);
            if (first < 0 || text.indexOf(rep, first + 1) < 0) {
                return false;
            }
        }
        return true;
    }

    // ---------------------------------------------------------------- main

    public static void main(String[] args) {
        long start = System.nanoTime();
        System.out.println("lcs");
        lcsTests();
        System.out.println("merge");
        mergeTests();
        System.out.println("tree");
        treeTests();
        System.out.println("naive / kmp / rk");
        searchTests();
        System.out.println("suffix");
        suffixTests();
        System.out.println("aho");
        ahoTests();
        System.out.println("state reset");
        resetTest();
        System.out.println("large inputs");
        largeTests();
        System.out.printf("%d checks passed, %d failed (%.1f s)%n", passed, failed, (System.nanoTime() - start) / 1e9);
        if (failed > 0) {
            System.exit(1);
        }
    }
}
