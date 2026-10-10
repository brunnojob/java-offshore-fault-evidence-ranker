import java.io.*;
import java.util.*;
import java.util.stream.*;

public class Main {
    static double numeric(String value) {
        double v = Double.parseDouble(value);
        if (!Double.isFinite(v)) throw new IllegalArgumentException("finite number required");
        return v;
    }
    static void require(boolean condition, String message) {
        if (!condition) throw new IllegalArgumentException(message);
    }
    static List<String[]> input(int columns) throws IOException {
        var rows = new ArrayList<String[]>();
        var reader = new BufferedReader(new InputStreamReader(System.in));
        String line;
        while ((line = reader.readLine()) != null) {
            require(rows.size() < 100000, "input limit exceeded");
            require(line.length() <= 8192, "input record too large");
            var row = line.split(",", -1);
            require(row.length == columns && Arrays.stream(row).noneMatch(String::isBlank), "invalid CSV row");
            rows.add(row);
        }
        require(!rows.isEmpty(), "missing input");
        return rows;
    }
    public static void main(String[] args) {
        try {
            require(args.length == 0 || (args.length == 1 && args[0].equals("--self-test")), "unsupported command arguments");
            if (args.length == 1 && args[0].equals("--self-test")) { test(); System.out.println("tests passed"); }
            else run();
        } catch (Exception e) { System.err.println(e.getMessage()); System.exit(2); }
    }

    record Hypothesis(String name, double prior, double[] likelihood) {}
    static Map<String, Double> rank(List<Hypothesis> hypotheses, boolean[] observed) {
        var logs = new LinkedHashMap<String, Double>();
        double totalPrior = 0;
        for (var h : hypotheses) {
            require(h.prior > 0 && h.prior <= 1 && h.likelihood.length == observed.length, "invalid hypothesis");
            require(!logs.containsKey(h.name), "duplicate hypothesis"); totalPrior += h.prior;
            double score = Math.log(h.prior);
            for (int i = 0; i < observed.length; i++) {
                double p = h.likelihood[i]; require(p > 0 && p < 1, "likelihood must lie in (0,1)");
                score += Math.log(observed[i] ? p : 1 - p);
            }
            logs.put(h.name, score);
        }
        require(Math.abs(totalPrior - 1) < 1e-6, "priors must sum to one");
        double max = logs.values().stream().mapToDouble(Double::doubleValue).max().orElseThrow();
        double denominator = logs.values().stream().mapToDouble(x -> Math.exp(x - max)).sum();
        var output = new LinkedHashMap<String, Double>();
        logs.entrySet().stream().sorted(Map.Entry.<String, Double>comparingByValue().reversed()).forEach(e -> output.put(e.getKey(), Math.exp(e.getValue() - max) / denominator));
        return output;
    }
    static void test() {
        var result = rank(List.of(new Hypothesis("bearing", .5, new double[]{.9}), new Hypothesis("sensor", .5, new double[]{.1})), new boolean[]{true});
        require(Math.abs(result.get("bearing") - .9) < 1e-10, "posterior mismatch");
        var inverted = rank(List.of(new Hypothesis("bearing", .5, new double[]{.9}), new Hypothesis("sensor", .5, new double[]{.1})), new boolean[]{false});
        require(Math.abs(inverted.get("sensor") - .9) < 1e-10, "negative evidence mismatch");
    }
    static void run() throws IOException {
        var rows = input(5); var hypotheses = new ArrayList<Hypothesis>();
        for (var r : rows) hypotheses.add(new Hypothesis(r[0], numeric(r[1]), new double[]{numeric(r[2]), numeric(r[3]), numeric(r[4])}));
        rank(hypotheses, new boolean[]{true, true, false}).forEach((key, value) -> System.out.printf(Locale.ROOT, "%s,%.8f%n", key, value));
    }

}
