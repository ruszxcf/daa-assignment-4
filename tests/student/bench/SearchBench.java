import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.function.Function;

/**
 * Benchmark naive / KMP / Rabin-Karp on identical inputs (sizes within n, m <= 3000).
 *
 * Timing scope (identical for all three): one call of Algorithms.naive / kmp / rk on an already built request map,
 * including construction of its answer; reading the input and compilation are NOT measured.
 * Warm-up: 3000 untimed calls of each algorithm per input (JIT). A measured run = mean time of REPS (=100) calls (nanoTime).
 * 5 measured runs per algorithm/size/case, raw run times in microseconds per call and their median are printed.
 * Correctness first: all three answers must equal a reference built with String.indexOf; otherwise the program stops.
 */
public class SearchBench {
    static final int RUNS = 5, REPS = 100, WARMUP = 3000;
    static final long SEED = 20261108L;

    static String repeat(String s, int times) {
        return s.repeat(times);
    }

    public static void main(String[] args) {
        int[][] sizes = {{300, 30}, {1000, 100}, {3000, 300}};
        String[] cases = {"random", "all-a (repetitive)", "ab-periodic (repetitive)"};
        System.out.println("|Case|n|m|Seed/generator|Algorithm|Run 1|Run 2|Run 3|Run 4|Run 5|Median|Correct outputs checked|");
        System.out.println("|---|---:|---:|---|---|---:|---:|---:|---:|---:|---:|---|");
        for (String c : cases) {
            for (int[] nm : sizes) {
                int n = nm[0], m = nm[1];
                String text, pat, gen;
                if (c.startsWith("random")) {
                    Random r = new Random(SEED + n);
                    StringBuilder sb = new StringBuilder();
                    for (int i = 0; i < n; i++) sb.append("acgt".charAt(r.nextInt(4)));
                    text = sb.toString();
                    int at = r.nextInt(n - m + 1);
                    pat = text.substring(at, at + m);
                    gen = "java.util.Random(" + (SEED + n) + "), alphabet acgt, pattern = text[" + at + ".." + (at + m) + ")";
                } else if (c.startsWith("all-a")) {
                    text = repeat("a", n);
                    pat = repeat("a", m);
                    gen = "text a^n, pattern a^m (every start matches)";
                } else {
                    text = repeat("ab", n / 2);
                    pat = repeat("ab", m / 2);
                    gen = "text (ab)^(n/2), pattern (ab)^(m/2)";
                }
                Map<String, Object> req = new HashMap<>();
                req.put("text", text);
                req.put("pattern", pat);
                List<Integer> ref = new ArrayList<>();
                for (int i = text.indexOf(pat); i >= 0; i = text.indexOf(pat, i + 1)) ref.add(i);
                int[] want = ref.stream().mapToInt(Integer::intValue).toArray();
                String[] names = {"naive", "kmp", "rk"};
                List<Function<Map<String, Object>, Map<String, Object>>> fs = List.of(Algorithms::naive, Algorithms::kmp, Algorithms::rk);
                for (int a = 0; a < 3; a++) {
                    int[] got = (int[]) fs.get(a).apply(req).get("positions");
                    if (!Arrays.equals(got, want)) throw new IllegalStateException(names[a] + " wrong on " + c + " n=" + n);
                }
                for (int a = 0; a < 3; a++) {
                    Function<Map<String, Object>, Map<String, Object>> f = fs.get(a);
                    long sink = 0;
                    for (int i = 0; i < WARMUP; i++) sink += ((int[]) f.apply(req).get("positions")).length;
                    double[] us = new double[RUNS];
                    for (int run = 0; run < RUNS; run++) {
                        long t0 = System.nanoTime();
                        for (int i = 0; i < REPS; i++) sink += ((int[]) f.apply(req).get("positions")).length;
                        us[run] = (System.nanoTime() - t0) / 1000.0 / REPS;
                    }
                    double[] sorted = us.clone();
                    Arrays.sort(sorted);
                    StringBuilder row = new StringBuilder("|" + c + "|" + n + "|" + m + "|" + gen + "|" + names[a] + "|");
                    for (double v : us) row.append(String.format("%.1f|", v));
                    row.append(String.format("%.1f|", sorted[RUNS / 2]));
                    row.append("yes, " + want.length + " starts equal to String.indexOf reference|");
                    System.out.println(row + (sink == -1 ? " " : ""));
                }
            }
        }
    }
}
