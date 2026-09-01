package org.uma.jmetal.runner.lc_psode;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;
import org.uma.jmetal.algorithm.multiobjective.mypso.p8.P8MetricCalculator;

/** Freezes the FC-6 paired engineering reference only after every required run is present. */
public final class ZhangBoV35Fc6ReportRunner {
  private static final double EPS = 1.0e-12;
  private static final long[] SEEDS = {20260822L, 20260823L, 20260824L};

  public enum Kind { ORDER, REGION_CURRENT, REGION_SWAP }
  public enum Decision { ORDER_SWAP, CURRENT_RETAINED, REGION_AWARE_ACCEPTED, STOP_REVIEW }

  private ZhangBoV35Fc6ReportRunner() { }

  public static void main(String[] arguments) throws Exception {
    Arguments values = Arguments.parse(arguments);
    generate(values.kind, values.instance, values.runsRoot, values.output);
  }

  static Decision generate(Kind kind, String instance, Path runsRoot, Path output) throws Exception {
    List<Run> runs = load(kind, instance, runsRoot.toAbsolutePath().normalize());
    List<double[]> combined = new ArrayList<double[]>();
    for (Run run : runs) combined.addAll(run.front);
    List<double[]> reference = P8MetricCalculator.nondominated(combined);
    for (Run run : runs) run.metrics = P8MetricCalculator.calculate(run.front, reference);

    Map<Long, Map<ZhangBoV35Fc6Runner.Phase, Run>> paired = pair(kind, runs);
    Verdict verdict = decide(kind, paired);
    Files.createDirectories(output);
    writeReference(output.resolve("reference-front.csv"), reference);
    writeMetrics(output.resolve("per-seed-metrics.csv"), paired, kind);
    writeLedgerSummary(output.resolve("local-search-ledger-summary.csv"), paired, kind);
    Files.write(output.resolve("decision.properties"), verdict.properties(kind)
        .getBytes(StandardCharsets.UTF_8));
    Files.write(output.resolve(kind == Kind.ORDER ? "FC6A4_ORDER_REPORT.md"
        : "FC6B_REGION_REPORT.md"), verdict.report(kind, instance, reference.size())
            .getBytes(StandardCharsets.UTF_8));
    writeEvidenceAudit(output.resolve("run-evidence-audit.csv"), runs);
    writeHashes(output);
    System.out.println("FC6_REPORT_COMPLETED kind=" + kind + " decision=" + verdict.decision
        + " referenceSize=" + reference.size() + " output=" + output);
    return verdict.decision;
  }

  private static List<Run> load(Kind kind, String instance, Path root) throws Exception {
    if (instance == null || instance.trim().isEmpty()) throw new IllegalArgumentException("instance");
    ZhangBoV35Fc6Runner.Phase[] phases = phases(kind);
    List<Run> result = new ArrayList<Run>();
    for (long seed : SEEDS) {
      String sameStart = null;
      for (ZhangBoV35Fc6Runner.Phase phase : phases) {
        Path directory = root.resolve("runs").resolve(phase.name().toLowerCase(Locale.ROOT)
            + "-" + instance + "-seed-" + seed);
        if (!Files.isDirectory(directory)) throw new IllegalStateException("missing FC-6 run: " + directory);
        verifyManifest(directory);
        Map<String, String> status = properties(directory.resolve("status.properties"));
        if (!"COMPLETED".equals(status.get("status"))) {
          throw new IllegalStateException("incomplete FC-6 run: " + directory);
        }
        Map<String, String> configuration = properties(directory.resolve("configuration.txt"));
        requireConfiguration(kind, phase, instance, seed, configuration);
        Run run = new Run();
        run.phase = phase; run.seed = seed; run.directory = directory;
        run.fe = Integer.parseInt(status.get("fullEvaluations"));
        run.nanos = Long.parseLong(status.get("algorithmRunNanos"));
        run.initialHash = properties(directory.resolve("initial-population.sha256")).get("__first__");
        run.front = readFront(directory.resolve("front.csv"));
        run.ledger = Ledger.read(directory.resolve("local-candidate-ledger.csv"));
        if (sameStart == null) sameStart = run.initialHash;
        else if (!sameStart.equals(run.initialHash)) {
          throw new IllegalStateException("initial population mismatch for seed=" + seed);
        }
        result.add(run);
      }
    }
    return result;
  }

  private static void requireConfiguration(Kind kind, ZhangBoV35Fc6Runner.Phase phase,
      String instance, long seed, Map<String, String> config) {
    if (!instance.equals(config.get("instance")) || !Long.toString(seed).equals(config.get("seed"))
        || !"FM3".equals(config.get("decoderMode")) || !"NONE".equals(config.get("shiftMode"))
        || !"DEGENERATE_SINGLE_FAMILY".equals(config.get("familyMode"))
        || !"SEQUENCE_INDEPENDENT".equals(config.get("setupMode"))
        || !"0,1,6".equals(config.get("objectiveAdapter"))) {
      throw new IllegalStateException("FC-6 shared fairness configuration mismatch for " + phase);
    }
    if (kind == Kind.ORDER) {
      if (!"GLOBAL_ORIGINAL".equals(config.get("pddrSelectionMode"))) {
        throw new IllegalStateException("order arm must use GLOBAL_ORIGINAL PDDR");
      }
      String expected = phase == ZhangBoV35Fc6Runner.Phase.ORDER_CURRENT
          ? "CATA_THEN_INHERITED" : "INHERITED_THEN_CATA";
      if (!expected.equals(config.get("localSearchOrder"))) {
        throw new IllegalStateException("order arm configuration mismatch: " + phase);
      }
    } else {
      boolean swap = kind == Kind.REGION_SWAP;
      boolean global = phase == ZhangBoV35Fc6Runner.Phase.REGION_GLOBAL
          || phase == ZhangBoV35Fc6Runner.Phase.REGION_GLOBAL_SWAP;
      String expected = global ? "GLOBAL_ORIGINAL" : "REGION_AWARE";
      String expectedOrder = swap ? "INHERITED_THEN_CATA" : "CATA_THEN_INHERITED";
      if (!expected.equals(config.get("pddrSelectionMode"))
          || !expectedOrder.equals(config.get("localSearchOrder"))) {
        throw new IllegalStateException("region arm configuration mismatch: " + phase);
      }
    }
  }

  private static Map<Long, Map<ZhangBoV35Fc6Runner.Phase, Run>> pair(Kind kind,
      List<Run> runs) {
    Map<Long, Map<ZhangBoV35Fc6Runner.Phase, Run>> result =
        new TreeMap<Long, Map<ZhangBoV35Fc6Runner.Phase, Run>>();
    for (Run run : runs) {
      Map<ZhangBoV35Fc6Runner.Phase, Run> values = result.get(run.seed);
      if (values == null) { values = new HashMap<ZhangBoV35Fc6Runner.Phase, Run>(); result.put(run.seed, values); }
      if (values.put(run.phase, run) != null) throw new IllegalStateException("duplicate run " + run.seed);
    }
    for (long seed : SEEDS) {
      Map<ZhangBoV35Fc6Runner.Phase, Run> values = result.get(seed);
      if (values == null || values.size() != 2) throw new IllegalStateException("incomplete seed=" + seed);
      for (ZhangBoV35Fc6Runner.Phase phase : phases(kind)) if (!values.containsKey(phase)) {
        throw new IllegalStateException("missing phase=" + phase + " seed=" + seed);
      }
    }
    return result;
  }

  private static Verdict decide(Kind kind,
      Map<Long, Map<ZhangBoV35Fc6Runner.Phase, Run>> paired) {
    ZhangBoV35Fc6Runner.Phase control = kind == Kind.ORDER
        ? ZhangBoV35Fc6Runner.Phase.ORDER_CURRENT
        : (kind == Kind.REGION_CURRENT ? ZhangBoV35Fc6Runner.Phase.REGION_GLOBAL
            : ZhangBoV35Fc6Runner.Phase.REGION_GLOBAL_SWAP);
    ZhangBoV35Fc6Runner.Phase candidate = kind == Kind.ORDER
        ? ZhangBoV35Fc6Runner.Phase.ORDER_SWAP
        : (kind == Kind.REGION_CURRENT ? ZhangBoV35Fc6Runner.Phase.REGION_AWARE
            : ZhangBoV35Fc6Runner.Phase.REGION_AWARE_SWAP);
    List<Double> cmaxGain = new ArrayList<Double>();
    List<Double> hvDelta = new ArrayList<Double>();
    List<Double> igdDelta = new ArrayList<Double>();
    boolean catastrophic = false;
    for (Map<ZhangBoV35Fc6Runner.Phase, Run> pair : paired.values()) {
      Run base = pair.get(control); Run proposed = pair.get(candidate);
      double cmax = percentDecrease(minimum(base.front, 0), minimum(proposed.front, 0));
      double hv = percentChange(base.metrics.hv, proposed.metrics.hv);
      double igd = percentChange(base.metrics.igd, proposed.metrics.igd);
      cmaxGain.add(cmax); hvDelta.add(hv); igdDelta.add(igd);
      if (hv < -5.0 && igd > 20.0) catastrophic = true;
    }
    Verdict result = new Verdict();
    result.medianCmaxGain = median(cmaxGain);
    result.medianHvDelta = median(hvDelta);
    result.medianIgdDelta = median(igdDelta);
    result.catastrophic = catastrophic;
    boolean passes = result.medianCmaxGain >= 2.0 - EPS && result.medianHvDelta >= -2.0 - EPS
        && result.medianIgdDelta <= 10.0 + EPS && !catastrophic;
    result.decision = kind == Kind.ORDER
        ? (passes ? Decision.ORDER_SWAP : Decision.CURRENT_RETAINED)
        : (passes ? Decision.REGION_AWARE_ACCEPTED : Decision.STOP_REVIEW);
    return result;
  }

  private static void writeMetrics(Path file,
      Map<Long, Map<ZhangBoV35Fc6Runner.Phase, Run>> paired, Kind kind) throws Exception {
    StringBuilder text = new StringBuilder("seed,phase,FE,frontSize,HV,IGD,Spacing,Cforward,Creverse,"
        + "minCmax,minTEC,minTWC,algorithmSeconds\n");
    for (Map.Entry<Long, Map<ZhangBoV35Fc6Runner.Phase, Run>> item : paired.entrySet()) {
      for (ZhangBoV35Fc6Runner.Phase phase : phases(kind)) {
        Run run = item.getValue().get(phase);
        text.append(String.format(Locale.ROOT,
            "%d,%s,%d,%d,%.12f,%.12f,%.12f,%.12f,%.12f,%.12f,%.12f,%.12f,%.6f\n",
            item.getKey(), phase, run.fe, run.front.size(), run.metrics.hv, run.metrics.igd,
            run.metrics.spacing, run.metrics.cForward, run.metrics.cReverse,
            minimum(run.front, 0), minimum(run.front, 1), minimum(run.front, 2), run.nanos / 1.0e9));
      }
    }
    Files.write(file, text.toString().getBytes(StandardCharsets.UTF_8));
  }

  private static void writeLedgerSummary(Path file,
      Map<Long, Map<ZhangBoV35Fc6Runner.Phase, Run>> paired, Kind kind) throws Exception {
    StringBuilder text = new StringBuilder("seed,phase,FE_CATA,FE_inherited,bestCmax_CATA,"
        + "bestCmax_inherited,enteredPddr_CATA,enteredPddr_inherited,"
        + "pddrSelected_CATA,pddrSelected_inherited\n");
    for (Map.Entry<Long, Map<ZhangBoV35Fc6Runner.Phase, Run>> item : paired.entrySet()) {
      for (ZhangBoV35Fc6Runner.Phase phase : phases(kind)) {
        Ledger ledger = item.getValue().get(phase).ledger;
        text.append(item.getKey()).append(',').append(phase).append(',')
            .append(ledger.cataFe()).append(',').append(ledger.inheritedFe()).append(',')
            .append(ledger.cataBestCmax()).append(',').append(ledger.inheritedBestCmax()).append(',')
            .append(ledger.cataEntered()).append(',').append(ledger.inheritedEntered()).append(',')
            .append(ledger.cataSelected()).append(',').append(ledger.inheritedSelected()).append('\n');
      }
    }
    Files.write(file, text.toString().getBytes(StandardCharsets.UTF_8));
  }

  private static void writeReference(Path file, List<double[]> values) throws Exception {
    StringBuilder text = new StringBuilder("Cmax,TEC,TWC\n");
    for (double[] value : values) text.append(value[0]).append(',').append(value[1]).append(',')
        .append(value[2]).append('\n');
    Files.write(file, text.toString().getBytes(StandardCharsets.UTF_8));
  }

  private static void writeEvidenceAudit(Path file, List<Run> runs) throws Exception {
    StringBuilder text = new StringBuilder("phase,seed,directory,manifestSha256\n");
    for (Run run : runs) text.append(run.phase).append(',').append(run.seed).append(',')
        .append(run.directory).append(',').append(sha256(run.directory.resolve("evidence-sha256.tsv")))
        .append('\n');
    Files.write(file, text.toString().getBytes(StandardCharsets.UTF_8));
  }

  private static void verifyManifest(Path directory) throws Exception {
    for (String line : Files.readAllLines(directory.resolve("evidence-sha256.tsv"), StandardCharsets.UTF_8)) {
      if (line.startsWith("sha256\t") || line.trim().isEmpty()) continue;
      String[] fields = line.split("\t", 2);
      if (fields.length != 2 || !fields[0].equals(sha256(directory.resolve(fields[1])))) {
        throw new IllegalStateException("evidence hash mismatch in " + directory + ": " + line);
      }
    }
  }

  private static Map<String, String> properties(Path file) throws Exception {
    Map<String, String> result = new HashMap<String, String>();
    List<String> lines = Files.readAllLines(file, StandardCharsets.UTF_8);
    if (!lines.isEmpty() && !lines.get(0).contains("=")) result.put("__first__", lines.get(0).trim().split("\\s+")[0]);
    for (String line : lines) {
      int at = line.indexOf('=');
      if (at > 0) result.put(line.substring(0, at), line.substring(at + 1));
    }
    return result;
  }

  private static List<double[]> readFront(Path file) throws Exception {
    List<double[]> result = new ArrayList<double[]>();
    List<String> lines = Files.readAllLines(file, StandardCharsets.UTF_8);
    for (int index = 1; index < lines.size(); index++) {
      if (lines.get(index).trim().isEmpty()) continue;
      String[] values = lines.get(index).split(",");
      result.add(new double[] {Double.parseDouble(values[0]), Double.parseDouble(values[1]),
          Double.parseDouble(values[2])});
    }
    if (result.isEmpty()) throw new IllegalStateException("empty front: " + file);
    return result;
  }

  private static double minimum(List<double[]> values, int objective) {
    double result = Double.POSITIVE_INFINITY;
    for (double[] value : values) result = Math.min(result, value[objective]);
    return result;
  }

  private static double percentDecrease(double oldValue, double newValue) {
    return 100.0 * (oldValue - newValue) / Math.max(EPS, Math.abs(oldValue));
  }

  private static double percentChange(double oldValue, double newValue) {
    return 100.0 * (newValue - oldValue) / Math.max(EPS, Math.abs(oldValue));
  }

  private static double median(List<Double> values) {
    List<Double> sorted = new ArrayList<Double>(values); Collections.sort(sorted);
    int middle = sorted.size() / 2;
    return sorted.size() % 2 == 0 ? (sorted.get(middle - 1) + sorted.get(middle)) / 2.0
        : sorted.get(middle);
  }

  private static ZhangBoV35Fc6Runner.Phase[] phases(Kind kind) {
    if (kind == Kind.ORDER) {
      return new ZhangBoV35Fc6Runner.Phase[] {ZhangBoV35Fc6Runner.Phase.ORDER_CURRENT,
          ZhangBoV35Fc6Runner.Phase.ORDER_SWAP};
    }
    return kind == Kind.REGION_CURRENT
        ? new ZhangBoV35Fc6Runner.Phase[] {ZhangBoV35Fc6Runner.Phase.REGION_GLOBAL,
            ZhangBoV35Fc6Runner.Phase.REGION_AWARE}
        : new ZhangBoV35Fc6Runner.Phase[] {ZhangBoV35Fc6Runner.Phase.REGION_GLOBAL_SWAP,
            ZhangBoV35Fc6Runner.Phase.REGION_AWARE_SWAP};
  }

  private static void writeHashes(Path directory) throws Exception {
    Map<String, String> values = new TreeMap<String, String>();
    java.util.stream.Stream<Path> paths = Files.walk(directory);
    try {
      paths.filter(Files::isRegularFile).filter(path -> !path.getFileName().toString()
          .equals("evidence-sha256.tsv")).forEach(path -> {
            try { values.put(directory.relativize(path).toString().replace('\\', '/'), sha256(path)); }
            catch (Exception error) { throw new IllegalStateException(error); }
          });
    } finally { paths.close(); }
    StringBuilder text = new StringBuilder("path\tsha256\n");
    for (Map.Entry<String, String> item : values.entrySet()) text.append(item.getKey()).append('\t')
        .append(item.getValue()).append('\n');
    Files.write(directory.resolve("evidence-sha256.tsv"), text.toString().getBytes(StandardCharsets.UTF_8));
  }

  private static String sha256(Path file) throws Exception {
    byte[] digest = MessageDigest.getInstance("SHA-256").digest(Files.readAllBytes(file));
    StringBuilder text = new StringBuilder();
    for (byte value : digest) text.append(String.format("%02x", value & 0xff));
    return text.toString();
  }

  private static final class Run {
    long seed; int fe; long nanos; String initialHash; Path directory;
    ZhangBoV35Fc6Runner.Phase phase; List<double[]> front; Ledger ledger;
    P8MetricCalculator.Metrics metrics;
  }

  private static final class Ledger {
    private final Map<String, Totals> bySource = new HashMap<String, Totals>();
    static Ledger read(Path file) throws Exception {
      Ledger result = new Ledger();
      List<String> lines = Files.readAllLines(file, StandardCharsets.UTF_8);
      for (int index = 1; index < lines.size(); index++) {
        if (lines.get(index).trim().isEmpty()) continue;
        String[] value = lines.get(index).split(",", 12);
        Totals totals = result.totals(value[1]);
        totals.fe += Long.parseLong(value[8]);
        totals.entered += Long.parseLong(value[5]);
        totals.selected += Long.parseLong(value[6]);
        int finalComma = lines.get(index).lastIndexOf(',');
        totals.bestCmax = Math.min(totals.bestCmax,
            Double.parseDouble(lines.get(index).substring(finalComma + 1)));
      }
      return result;
    }
    private Totals totals(String source) {
      Totals value = bySource.get(source);
      if (value == null) { value = new Totals(); bySource.put(source, value); }
      return value;
    }
    long cataFe() { return totals("CATA_TEST").fe + totals("CATA_APPLY").fe; }
    long inheritedFe() { return totals("CRITICAL_SWAP").fe + totals("CRITICAL_INSERT").fe + totals("O1_O9").fe; }
    double cataBestCmax() { return Math.min(totals("CATA_TEST").bestCmax, totals("CATA_APPLY").bestCmax); }
    double inheritedBestCmax() { return Math.min(totals("CRITICAL_SWAP").bestCmax,
        Math.min(totals("CRITICAL_INSERT").bestCmax, totals("O1_O9").bestCmax)); }
    long cataEntered() { return totals("CATA_TEST").entered + totals("CATA_APPLY").entered; }
    long inheritedEntered() { return totals("CRITICAL_SWAP").entered + totals("CRITICAL_INSERT").entered + totals("O1_O9").entered; }
    long cataSelected() { return totals("CATA_TEST").selected + totals("CATA_APPLY").selected; }
    long inheritedSelected() { return totals("CRITICAL_SWAP").selected + totals("CRITICAL_INSERT").selected + totals("O1_O9").selected; }
  }

  private static final class Totals {
    long fe; long entered; long selected; double bestCmax = Double.POSITIVE_INFINITY;
  }

  private static final class Verdict {
    Decision decision; double medianCmaxGain; double medianHvDelta; double medianIgdDelta;
    boolean catastrophic;
    String properties(Kind kind) {
      return "kind=" + kind + "\ndecision=" + decision + "\nmedianCmaxImprovementPercent="
          + medianCmaxGain + "\nmedianHvChangePercent=" + medianHvDelta
          + "\nmedianIgdChangePercent=" + medianIgdDelta + "\ncatastrophicSeed=" + catastrophic
          + "\nformalMatrixStarted=false\nsampledReproductionAccepted=false\n"
          + "fullReproductionAccepted=false\n";
    }
    String report(Kind kind, String instance, int referenceSize) {
      return "# FC-6 " + kind + " engineering report\n\n"
          + "Instance: `" + instance + "`; the reference is frozen only after all six paired runs. "
          + "Reference-front size: **" + referenceSize + "**.\n\n"
          + "## Decision\n\n**" + decision + "**\n\n"
          + "- Median minimum-Cmax improvement: `" + medianCmaxGain + "%`\n"
          + "- Median HV change: `" + medianHvDelta + "%`\n"
          + "- Median IGD change: `" + medianIgdDelta + "%`\n"
          + "- Catastrophic-seed gate: `" + catastrophic + "`\n\n"
          + "This is an engineering causal gate, not a statistical-significance or paper-superiority conclusion.\n";
    }
  }

  private static final class Arguments {
    final Kind kind; final String instance; final Path runsRoot; final Path output;
    private Arguments(Kind kind, String instance, Path runsRoot, Path output) {
      this.kind = kind; this.instance = instance; this.runsRoot = runsRoot; this.output = output;
    }
    static Arguments parse(String[] values) {
      Kind kind = null; String instance = null; Path runs = null, output = null;
      for (int index = 0; index < values.length; index += 2) {
        if (index + 1 >= values.length) throw usage();
        if ("--kind".equals(values[index])) kind = Kind.valueOf(values[index + 1]);
        else if ("--instance".equals(values[index])) instance = values[index + 1];
        else if ("--runs-root".equals(values[index])) runs = Paths.get(values[index + 1]);
        else if ("--output".equals(values[index])) output = Paths.get(values[index + 1]);
        else throw usage();
      }
      if (kind == null || instance == null || runs == null || output == null) throw usage();
      return new Arguments(kind, instance, runs, output);
    }
    static IllegalArgumentException usage() {
      return new IllegalArgumentException("Usage: --kind ORDER|REGION_CURRENT|REGION_SWAP --instance <name> "
          + "--runs-root <path> --output <path>");
    }
  }
}
