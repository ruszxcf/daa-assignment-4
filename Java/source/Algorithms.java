import java.util.*;

/**
 * Assignment 4: LCS, interval merge DP, tree DP, naive/KMP/Rabin-Karp search,
 * suffix array + Kasai LCP and Aho-Corasick.
 *
 * Every method is a pure function of its request: all arrays are created inside the call,
 * nothing is stored in static fields, so consecutive requests cannot influence each other.
 * No built-in substring search (indexOf, contains, regex, ...) is used by any algorithm.
 */
public final class Algorithms {
    private Algorithms() {
    }

    // ------------------------------------------------------------------ input helpers

    private static String str(Map<String, Object> input, String key) {
        return (String) input.get(key);
    }

    private static int toInt(Object o) {
        return Math.toIntExact(((Number) o).longValue());
    }

    private static long toLong(Object o) {
        return ((Number) o).longValue();
    }

    private static List<?> list(Map<String, Object> input, String key) {
        return (List<?>) input.get(key);
    }

    /** Growable int list without boxing (used for match positions). */
    private static final class IntBuf {
        int[] a = new int[16];
        int size;

        void add(int v) {
            if (size == a.length) {
                a = Arrays.copyOf(a, size * 2);
            }
            a[size++] = v;
        }

        int[] toArray() {
            return Arrays.copyOf(a, size);
        }
    }

    private static Map<String, Object> positions(int[] p) {
        Map<String, Object> r = new LinkedHashMap<>();
        r.put("positions", p);
        return r;
    }

    /** Empty pattern matches every boundary 0..n, including the final one. */
    private static int[] allBoundaries(int n) {
        int[] r = new int[n + 1];
        for (int i = 0; i <= n; i++) {
            r[i] = i;
        }
        return r;
    }

    // ------------------------------------------------------------------ Part 1: LCS

    /** Prefix DP: L[i][j] = LCS length of a[0..i) and b[0..j); one optimal sequence is rebuilt backwards. */
    static Map<String, Object> lcs(Map<String, Object> input) {
        String a = str(input, "a");
        String b = str(input, "b");
        int n = a.length();
        int m = b.length();

        int[][] len = new int[n + 1][m + 1]; // row 0 and column 0 stay 0: an empty prefix has LCS 0
        for (int i = 1; i <= n; i++) {
            char ca = a.charAt(i - 1);
            for (int j = 1; j <= m; j++) {
                if (ca == b.charAt(j - 1)) {
                    len[i][j] = len[i - 1][j - 1] + 1;
                } else {
                    len[i][j] = Math.max(len[i - 1][j], len[i][j - 1]);
                }
            }
        }

        StringBuilder sb = new StringBuilder();
        int i = n;
        int j = m;
        while (i > 0 && j > 0) {
            if (a.charAt(i - 1) == b.charAt(j - 1)) { // equal characters: part of an optimal sequence
                sb.append(a.charAt(i - 1));
                i--;
                j--;
            } else if (len[i - 1][j] >= len[i][j - 1]) { // follow the cell that holds the optimum
                i--;
            } else {
                j--;
            }
        }

        Map<String, Object> r = new LinkedHashMap<>();
        r.put("length", len[n][m]);
        r.put("sequence", sb.reverse().toString());
        return r;
    }

    // ------------------------------------------------------------------ Part 2: ordered merge (interval DP)

    /** cost[l][r] = min over the last split k of cost[l][k] + cost[k+1][r] + sum(l..r). */
    static Map<String, Object> merge(Map<String, Object> input) {
        List<?> raw = list(input, "sizes");
        int n = raw.size();
        Map<String, Object> r = new LinkedHashMap<>();
        if (n <= 1) {
            r.put("cost", 0L);
            r.put("plan", new int[0][]);
            return r;
        }
        long[] prefix = new long[n + 1];
        for (int i = 0; i < n; i++) {
            prefix[i + 1] = prefix[i] + toLong(raw.get(i));
        }

        long[][] cost = new long[n][n]; // single blocks cost 0
        int[][] split = new int[n][n];
        for (int length = 2; length <= n; length++) { // shorter intervals first: both halves are ready
            for (int l = 0; l + length - 1 < n; l++) {
                int rr = l + length - 1;
                long total = prefix[rr + 1] - prefix[l];
                long best = Long.MAX_VALUE;
                int bestK = l;
                for (int k = l; k < rr; k++) {
                    long c = cost[l][k] + cost[k + 1][rr] + total;
                    if (c < best) {
                        best = c;
                        bestK = k;
                    }
                }
                cost[l][rr] = best;
                split[l][rr] = bestK;
            }
        }

        // Post-order walk (explicit stack): both child merges are emitted before the parent merge.
        int[][] plan = new int[n - 1][];
        int count = 0;
        int[] stackL = new int[2 * n];
        int[] stackR = new int[2 * n];
        boolean[] expanded = new boolean[2 * n];
        int top = 0;
        stackL[top] = 0;
        stackR[top] = n - 1;
        expanded[top++] = false;
        while (top > 0) {
            int l = stackL[top - 1];
            int rr = stackR[top - 1];
            if (l == rr) {
                top--;
                continue;
            }
            int k = split[l][rr];
            if (expanded[top - 1]) {
                plan[count++] = new int[] {l, k, rr};
                top--;
            } else {
                expanded[top - 1] = true;
                stackL[top] = k + 1; // right child
                stackR[top] = rr;
                expanded[top++] = false;
                stackL[top] = l; // left child (processed first)
                stackR[top] = k;
                expanded[top++] = false;
            }
        }

        r.put("cost", cost[0][n - 1]);
        r.put("plan", plan);
        return r;
    }

    // ------------------------------------------------------------------ Part 3: tree DP

    /**
     * take[v] = w[v] + sum skip[c]; skip[v] = sum max(take[c], skip[c]) over children c.
     * Iterative DFS order (no recursion), DP in reverse order, reconstruction top-down.
     */
    static Map<String, Object> tree(Map<String, Object> input) {
        int n = toInt(input.get("n"));
        Map<String, Object> r = new LinkedHashMap<>();
        if (n == 0) {
            r.put("weight", 0L);
            r.put("selected", new int[0]);
            return r;
        }
        List<?> w = list(input, "weights");
        List<?> e = list(input, "edges");
        long[] weight = new long[n];
        for (int i = 0; i < n; i++) {
            weight[i] = toLong(w.get(i));
        }
        int[] start = new int[n + 1];
        int m = e.size();
        int[] eu = new int[m];
        int[] ev = new int[m];
        for (int i = 0; i < m; i++) {
            List<?> pair = (List<?>) e.get(i);
            eu[i] = toInt(pair.get(0));
            ev[i] = toInt(pair.get(1));
            start[eu[i] + 1]++;
            start[ev[i] + 1]++;
        }
        for (int v = 0; v < n; v++) {
            start[v + 1] += start[v];
        }
        int[] fill = Arrays.copyOf(start, n);
        int[] to = new int[2 * m];
        for (int i = 0; i < m; i++) {
            to[fill[eu[i]]++] = ev[i];
            to[fill[ev[i]]++] = eu[i];
        }

        int[] parent = new int[n];
        int[] order = new int[n]; // vertices in DFS preorder: a parent always precedes its children
        Arrays.fill(parent, -1);
        int[] stack = new int[n];
        int sp = 0;
        int cnt = 0;
        stack[sp++] = 0;
        parent[0] = 0; // the root is its own parent only while marking visited
        while (sp > 0) {
            int v = stack[--sp];
            order[cnt++] = v;
            for (int k = start[v]; k < start[v + 1]; k++) {
                int c = to[k];
                if (parent[c] == -1) {
                    parent[c] = v;
                    stack[sp++] = c;
                }
            }
        }
        parent[0] = -1;

        long[] take = new long[n];
        long[] skip = new long[n];
        for (int v = 0; v < n; v++) {
            take[v] = weight[v];
        }
        for (int i = n - 1; i >= 0; i--) { // children are processed before their parent
            int v = order[i];
            int p = parent[v];
            if (p >= 0) {
                take[p] += skip[v];
                skip[p] += Math.max(take[v], skip[v]);
            }
        }

        boolean[] taken = new boolean[n];
        int chosen = 0;
        for (int i = 0; i < n; i++) { // parents first: a taken parent forces its children to be skipped
            int v = order[i];
            int p = parent[v];
            boolean t;
            if (p < 0) {
                t = take[v] > skip[v];
            } else if (taken[p]) {
                t = false;
            } else {
                t = take[v] > skip[v];
            }
            taken[v] = t;
            if (t) {
                chosen++;
            }
        }
        int[] selected = new int[chosen];
        int idx = 0;
        for (int v = 0; v < n; v++) {
            if (taken[v]) {
                selected[idx++] = v;
            }
        }
        r.put("weight", Math.max(take[0], skip[0]));
        r.put("selected", selected);
        return r;
    }

    // ------------------------------------------------------------------ Part 4: exact search

    /** Naive search: try every start 0..n-m and compare characters. */
    static Map<String, Object> naive(Map<String, Object> input) {
        String text = str(input, "text");
        String pat = str(input, "pattern");
        int n = text.length();
        int m = pat.length();
        if (m == 0) {
            return positions(allBoundaries(n));
        }
        IntBuf out = new IntBuf();
        for (int s = 0; s + m <= n; s++) { // s = n-m is the last valid start
            int j = 0;
            while (j < m && text.charAt(s + j) == pat.charAt(j)) {
                j++;
            }
            if (j == m) {
                out.add(s);
            }
        }
        return positions(out.toArray());
    }

    /** KMP: pi[i] = length of the longest proper prefix of pat[0..i] that is also its suffix. */
    static Map<String, Object> kmp(Map<String, Object> input) {
        String text = str(input, "text");
        String pat = str(input, "pattern");
        int n = text.length();
        int m = pat.length();
        if (m == 0) { // before any pattern indexing
            return positions(allBoundaries(n));
        }
        int[] pi = new int[m];
        for (int i = 1, k = 0; i < m; i++) {
            while (k > 0 && pat.charAt(i) != pat.charAt(k)) {
                k = pi[k - 1];
            }
            if (pat.charAt(i) == pat.charAt(k)) {
                k++;
            }
            pi[i] = k;
        }
        IntBuf out = new IntBuf();
        for (int i = 0, k = 0; i < n; i++) {
            while (k > 0 && text.charAt(i) != pat.charAt(k)) { // mismatch: fall back, never move i back
                k = pi[k - 1];
            }
            if (text.charAt(i) == pat.charAt(k)) {
                k++;
            }
            if (k == m) {
                out.add(i - m + 1);
                k = pi[k - 1]; // fall back so that overlapping matches are kept
            }
        }
        return positions(out.toArray());
    }

    private static final int RK_BASE = 31;
    private static final int RK_MOD = 101;

    /** Rabin-Karp with base 31 / mod 101; equal hashes are only candidates and are verified character by character. */
    static Map<String, Object> rk(Map<String, Object> input) {
        String text = str(input, "text");
        String pat = str(input, "pattern");
        int n = text.length();
        int m = pat.length();
        if (m == 0) {
            return positions(allBoundaries(n));
        }
        if (m > n) {
            return positions(new int[0]);
        }
        int high = 1; // BASE^(m-1) mod MOD: weight of the leftmost character
        for (int i = 1; i < m; i++) {
            high = high * RK_BASE % RK_MOD;
        }
        int hp = 0;
        int ht = 0;
        for (int i = 0; i < m; i++) {
            hp = (hp * RK_BASE + pat.charAt(i)) % RK_MOD;
            ht = (ht * RK_BASE + text.charAt(i)) % RK_MOD;
        }
        IntBuf out = new IntBuf();
        for (int s = 0; ; s++) {
            if (hp == ht) { // candidate window: verify the real characters
                int j = 0;
                while (j < m && text.charAt(s + j) == pat.charAt(j)) {
                    j++;
                }
                if (j == m) {
                    out.add(s);
                }
            }
            if (s + m >= n) {
                break;
            }
            // roll: remove text[s], shift, add text[s+m]; normalise a negative remainder
            ht = (ht - text.charAt(s) * high % RK_MOD + RK_MOD) % RK_MOD;
            ht = (ht * RK_BASE + text.charAt(s + m)) % RK_MOD;
        }
        return positions(out.toArray());
    }

    // ------------------------------------------------------------------ Part 5: suffix array + Kasai

    /**
     * Prefix doubling: after round k the rank of suffix i orders it by its first 2k characters.
     * Each round sorts packed long keys (rank pair, index) with a primitive sort: O(n log^2 n) in total.
     * Kasai then fills lcp in O(n) using lcp[rank[i+1]] >= lcp[rank[i]] - 1.
     */
    static Map<String, Object> suffix(Map<String, Object> input) {
        String text = str(input, "text");
        int n = text.length();
        Map<String, Object> r = new LinkedHashMap<>();
        if (n == 0) {
            r.put("sa", new int[0]);
            r.put("lcp", new int[0]);
            r.put("repeated", "");
            return r;
        }
        long mul = n + 128L; // larger than every rank and than "second rank + 1"
        int[] rank = new int[n];
        int[] sa = new int[n];
        for (int i = 0; i < n; i++) {
            rank[i] = text.charAt(i); // round 0: rank by the first character
        }
        long[] keys = new long[n];
        int[] next = new int[n];
        for (int k = 1; ; k <<= 1) {
            for (int i = 0; i < n; i++) {
                long second = i + k < n ? rank[i + k] + 1 : 0; // a missing second half sorts first
                keys[i] = ((rank[i] * mul + second) << 16) | i;
            }
            Arrays.sort(keys);
            int classes = 0;
            long previous = -1;
            for (int idx = 0; idx < n; idx++) {
                int i = (int) (keys[idx] & 0xFFFFL);
                long pair = keys[idx] >>> 16;
                if (pair != previous) {
                    classes++;
                    previous = pair;
                }
                sa[idx] = i;
                next[i] = classes - 1;
            }
            int[] tmp = rank;
            rank = next;
            next = tmp;
            if (classes == n || k >= n) {
                break;
            }
        }
        // rank[] is now the inverse permutation of sa[] (all suffixes are distinct)
        int[] lcp = new int[n];
        int h = 0;
        int best = 0;
        int bestStart = 0;
        for (int i = 0; i < n; i++) {
            if (rank[i] > 0) {
                int j = sa[rank[i] - 1];
                while (i + h < n && j + h < n && text.charAt(i + h) == text.charAt(j + h)) {
                    h++;
                }
                lcp[rank[i]] = h;
                if (h > best) {
                    best = h;
                    bestStart = i;
                }
                if (h > 0) {
                    h--;
                }
            } else {
                h = 0;
            }
        }
        r.put("sa", sa);
        r.put("lcp", lcp);
        r.put("repeated", text.substring(bestStart, bestStart + best));
        return r;
    }

    // ------------------------------------------------------------------ Part 6: Aho-Corasick

    private static final int ALPHABET = 95; // printable ASCII 32..126

    /**
     * Trie with failure links turned into a full goto automaton (BFS). For every text each visited state
     * is counted once; the counts are then pushed along failure links in reverse BFS order, so a pattern
     * that is a suffix of another one is counted too. Duplicate patterns share one terminal state.
     */
    static Map<String, Object> aho(Map<String, Object> input) {
        List<?> patterns = list(input, "patterns");
        List<?> texts = list(input, "texts");
        int p = patterns.size();
        int q = texts.size();

        long[][] counts = new long[q][];
        if (p == 0) { // an empty dictionary: one empty row per text
            for (int t = 0; t < q; t++) {
                counts[t] = new long[0];
            }
            Map<String, Object> r = new LinkedHashMap<>();
            r.put("counts", counts);
            return r;
        }

        int total = 1;
        for (Object o : patterns) {
            total += ((String) o).length();
        }
        int[] go = new int[total * ALPHABET]; // 0 = no edge yet (the root is never a child)
        int states = 1;
        int[] terminal = new int[p];
        for (int i = 0; i < p; i++) {
            String s = (String) patterns.get(i);
            int v = 0;
            for (int j = 0; j < s.length(); j++) {
                int c = s.charAt(j) - 32;
                int slot = v * ALPHABET + c;
                if (go[slot] == 0) {
                    go[slot] = states++;
                }
                v = go[slot];
            }
            terminal[i] = v;
        }

        int[] fail = new int[states];
        int[] bfs = new int[states];
        int head = 0;
        int tail = 0;
        for (int c = 0; c < ALPHABET; c++) { // depth-1 states fail to the root
            int u = go[c];
            if (u != 0) {
                bfs[tail++] = u;
            }
        }
        while (head < tail) {
            int v = bfs[head++];
            for (int c = 0; c < ALPHABET; c++) {
                int u = go[v * ALPHABET + c];
                if (u != 0) {
                    fail[u] = go[fail[v] * ALPHABET + c];
                    bfs[tail++] = u;
                } else {
                    go[v * ALPHABET + c] = go[fail[v] * ALPHABET + c]; // complete the automaton
                }
            }
        }

        long[] visits = new long[states];
        for (int t = 0; t < q; t++) {
            Arrays.fill(visits, 0); // every text starts from fresh counts
            String s = (String) texts.get(t);
            int v = 0;
            for (int j = 0; j < s.length(); j++) {
                v = go[v * ALPHABET + (s.charAt(j) - 32)];
                visits[v]++;
            }
            for (int i = tail - 1; i >= 0; i--) { // deeper states first: push counts to suffix states
                int u = bfs[i];
                visits[fail[u]] += visits[u];
            }
            long[] row = new long[p];
            for (int i = 0; i < p; i++) {
                row[i] = visits[terminal[i]];
            }
            counts[t] = row;
        }
        Map<String, Object> r = new LinkedHashMap<>();
        r.put("counts", counts);
        return r;
    }
}
