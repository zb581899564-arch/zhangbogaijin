package org.uma.jmetal.runner.lc_psode;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import org.uma.jmetal.algorithm.multiobjective.mypso.p8.P8MetricCalculator;
import org.uma.jmetal.algorithm.multiobjective.mypso.v35.V35Doe1Analysis;
import org.uma.jmetal.algorithm.multiobjective.mypso.v35.V35SubSwarmMixture;

/** Offline, fail-closed report builder for the pre-registered 60 held-out runs. */
public final class ZhangBoV35Doe1ConfirmationReportRunner {
  private static final String[] INSTANCES = {"20_5_4_1", "50_5_4_1", "100_5_4_1"};
  private static final long[] SEEDS = {20260901L, 20260902L, 20260903L, 20260904L, 20260905L};
  private static final Map<String, V35SubSwarmMixture> ARMS = arms();

  private ZhangBoV35Doe1ConfirmationReportRunner() { }

  public static void main(String[] args) throws Exception {
    Path input = Paths.get("docs/evidence/V35-DOE1-subgroup-mixture/06-heldout-confirmation");
    Path output = input;
    for (int index = 0; index < args.length; index++) {
      if ("--input".equals(args[index])) input = Paths.get(args[++index]);
      else if ("--output".equals(args[index])) output = Paths.get(args[++index]);
      else throw new IllegalArgumentException("--input/--output only");
    }
    List<Run> runs = readAndValidate(input);
    Files.createDirectories(output.resolve("reference-fronts"));
    Map<String, List<double[]>> references = buildReferences(output, runs);
    for (Run run : runs) run.metrics = P8MetricCalculator.calculate(run.front, references.get(run.instance));
    writeMetrics(output.resolve("confirmation-metrics.csv"), runs);
    List<Pair> pairs = paired(runs);
    writePairs(output.resolve("paired-results.csv"), pairs);
    Decision decision = decide(pairs);
    writeDecision(output.resolve("FINAL_MIXTURE_DECISION.md"), output.resolve("FINAL_MIXTURE_DECISION.properties"), decision);
    writeManifest(output.resolve("evidence-sha256.tsv"), output);
    System.out.println("V35_DOE1_HELDOUT_REPORT_COMPLETED decision=" + decision.code);
  }

  private static Map<String, V35SubSwarmMixture> arms() {
    Map<String, V35SubSwarmMixture> values = new LinkedHashMap<>();
    values.put("BASE", V35SubSwarmMixture.of(20, 40, 20, 20));
    values.put("T1", V35SubSwarmMixture.of(30, 50, 10, 10));
    values.put("T2", V35SubSwarmMixture.of(25, 25, 25, 25));
    values.put("T3", V35SubSwarmMixture.of(20, 40, 30, 10));
    return Collections.unmodifiableMap(values);
  }

  private static List<Run> readAndValidate(Path root) throws Exception {
    List<Run> values = new ArrayList<>();
    for (Map.Entry<String, V35SubSwarmMixture> arm : ARMS.entrySet()) for (String instance : INSTANCES) for (long seed : SEEDS) {
      Path directory = root.resolve("runs").resolve(arm.getKey() + "-" + arm.getValue().toString().replace('/', '_'))
          .resolve(instance).resolve("seed-" + seed);
      if (!Files.isRegularFile(directory.resolve("front.csv")) || !Files.isRegularFile(directory.resolve("status.properties"))
          || !Files.isRegularFile(directory.resolve("configuration.txt"))) {
        throw new IllegalStateException("missing held-out evidence: " + directory);
      }
      Run run = new Run(arm.getKey(), arm.getValue(), instance, seed, directory);
      run.status = readProperties(directory.resolve("status.properties"));
      run.config = readConfiguration(directory.resolve("configuration.txt"));
      run.front = readFront(directory.resolve("front.csv"));
      validateRun(run);
      values.add(run);
    }
    if (values.size() != 60) throw new IllegalStateException("expected 60 held-out runs");
    Map<String, String> initialHashes = new HashMap<>();
    for (Run run : values) {
      String key = run.instance + "/" + run.seed;
      String prior = initialHashes.put(key, run.status.getProperty("initialPopulationHash"));
      if (prior != null && !prior.equals(run.status.getProperty("initialPopulationHash"))) {
        throw new IllegalStateException("initial population hash mismatch: " + key);
      }
    }
    return values;
  }

  private static Properties readProperties(Path file) throws Exception {
    Properties properties = new Properties();
    try (InputStream input = Files.newInputStream(file)) { properties.load(input); }
    return properties;
  }

  private static Map<String, String> readConfiguration(Path file) throws Exception {
    Map<String, String> values = new HashMap<>();
    for (String line : Files.readAllLines(file, StandardCharsets.UTF_8)) {
      int split = line.indexOf('=');
      if (split > 0) values.put(line.substring(0, split), line.substring(split + 1));
    }
    return values;
  }

  private static List<double[]> readFront(Path file) throws Exception {
    List<double[]> values = new ArrayList<>(); List<String> lines = Files.readAllLines(file, StandardCharsets.UTF_8);
    for (int index = 1; index < lines.size(); index++) {
      String[] point = lines.get(index).split(",");
      if (point.length >= 3) values.add(new double[] {Double.parseDouble(point[0]), Double.parseDouble(point[1]), Double.parseDouble(point[2])});
    }
    return values;
  }

  private static void validateRun(Run run) {
    required("COMPLETED".equals(run.status.getProperty("status")), run, "status");
    required("500000".equals(run.status.getProperty("fullEvaluations")), run, "exact FE");
    required("500000".equals(run.status.getProperty("decoderCalls")), run, "decoder FE closure");
    required("0".equals(run.status.getProperty("illegalSolutions")), run, "illegalSolutions");
    required("0".equals(run.status.getProperty("duplicateEvaluations")), run, "duplicateEvaluations");
    required(!run.front.isEmpty(), run, "front");
    required("HELDOUT_CONFIRMATION".equals(run.config.get("campaignPhase")), run, "campaign phase");
    required(run.mixture.toString().equals(run.config.get("mixture")), run, "mixture");
    required(run.instance.equals(run.config.get("instance")), run, "instance");
    required(Long.toString(run.seed).equals(run.config.get("seed")), run, "seed");
    required("GLOBAL_ORIGINAL".equals(run.config.get("selector")), run, "PDDR selector");
    required("CATA_THEN_INHERITED".equals(run.config.get("localSearchOrder")), run, "local-search order");
    required("FM3".equals(run.config.get("decoderMode")), run, "decoder");
    required("NONE".equals(run.config.get("shiftMode")), run, "shift");
    required("DEGENERATE_SINGLE_FAMILY".equals(run.config.get("familyMode")), run, "family mode");
    required("SEQUENCE_INDEPENDENT".equals(run.config.get("setupMode")), run, "setup mode");
    required("false".equals(run.config.get("directionalTeacherPool")), run, "teacher pool");
    required("0".equals(run.config.get("softFreezeRho")), run, "rho");
    required("BAL_FULL_OPEN".equals(run.config.get("pressureMask")), run, "pressure mode");
    required(run.status.getProperty("runtimeSubSwarmSizes", "").equals(runtimeSizes(run.mixture)), run, "runtime subgroup sizes");
    String mechanism = run.status.getProperty("mechanismSummary", "");
    required(number(mechanism, "cfvfOffspring") > 0, run, "CFVF trigger");
    required(number(mechanism, "qgSelections") > 0, run, "Qg trigger");
    required(number(mechanism, "qpActions") > 0, run, "Qp trigger");
    required(number(mechanism, "archiveInsertions") > 0, run, "archive trigger");
    required(number(mechanism, "caTaLiteTest") > 0 && number(mechanism, "caTaLiteApply") > 0, run, "CA-TA Test/Apply trigger");
    required(number(mechanism, "directionalPoolRequests") == 0, run, "teacher-pool isolation");
    required(number(mechanism, "cfvfRepairs") == 0, run, "unexpected repair");
    required(mechanism.contains("dominatedTeacherUses=0"), run, "DSCR DTUR gate");
  }

  private static long number(String text, String key) {
    String marker = key + "="; int start = text.indexOf(marker); if (start < 0) return -1L;
    start += marker.length(); int end = text.indexOf(',', start); if (end < 0) end = text.length();
    try { return Long.parseLong(text.substring(start, end)); } catch (NumberFormatException error) { return -1L; }
  }

  private static void required(boolean condition, Run run, String reason) {
    if (!condition) throw new IllegalStateException("invalid held-out run " + run.directory + ": " + reason);
  }

  private static String runtimeSizes(V35SubSwarmMixture mixture) {
    return "G1_CMAX=" + mixture.getG1Cmax() + ";G4_BALANCED=" + mixture.getG4Balanced()
        + ";G2_TEC=" + mixture.getG2Tec() + ";G3_TWC=" + mixture.getG3Twc();
  }

  private static Map<String, List<double[]>> buildReferences(Path output, List<Run> runs) throws Exception {
    Map<String, List<double[]>> result = new HashMap<>();
    for (String instance : INSTANCES) {
      List<double[]> pooled = new ArrayList<>(); for (Run run : runs) if (instance.equals(run.instance)) pooled.addAll(run.front);
      List<double[]> unique = exactDeduplicate(pooled); List<double[]> reference = P8MetricCalculator.nondominated(unique);
      if (reference.isEmpty()) throw new IllegalStateException("empty reference: " + instance);
      result.put(instance, reference);
      StringBuilder csv = new StringBuilder("Cmax,TEC,TWC\n"); for (double[] point : reference) csv.append(point[0]).append(',').append(point[1]).append(',').append(point[2]).append('\n');
      Files.write(output.resolve("reference-fronts").resolve(instance + ".csv"), csv.toString().getBytes(StandardCharsets.UTF_8));
      double[] bounds = bounds(reference);
      Files.write(output.resolve("reference-fronts").resolve(instance + ".properties"), (
          "rawPointCount=" + pooled.size() + "\nexactDeduplicatedPointCount=" + unique.size() + "\nreferencePointCount=" + reference.size()
          + "\nminCmax=" + bounds[0] + "\nmaxCmax=" + bounds[1] + "\nminTEC=" + bounds[2] + "\nmaxTEC=" + bounds[3]
          + "\nminTWC=" + bounds[4] + "\nmaxTWC=" + bounds[5] + "\nhvReferencePoint=1.1,1.1,1.1\n").getBytes(StandardCharsets.UTF_8));
    }
    return result;
  }

  private static List<double[]> exactDeduplicate(List<double[]> values) {
    List<double[]> unique = new ArrayList<>(); Set<String> seen = new HashSet<>();
    for (double[] value : values) {
      String id = Long.toHexString(Double.doubleToLongBits(value[0])) + ":" + Long.toHexString(Double.doubleToLongBits(value[1])) + ":" + Long.toHexString(Double.doubleToLongBits(value[2]));
      if (seen.add(id)) unique.add(value);
    }
    return unique;
  }

  private static double[] bounds(List<double[]> values) {
    double[] result = {Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY};
    for (double[] point : values) for (int index = 0; index < 3; index++) {
      result[index * 2] = Math.min(result[index * 2], point[index]); result[index * 2 + 1] = Math.max(result[index * 2 + 1], point[index]);
    }
    return result;
  }

  private static void writeMetrics(Path output, List<Run> runs) throws Exception {
    StringBuilder csv = new StringBuilder("arm,mixture,instance,seed,HV,IGD,Spacing,frontSize,minCmax,minTEC,minTWC,algorithmRunNanos\n");
    Collections.sort(runs, runComparator());
    for (Run run : runs) csv.append(run.arm).append(',').append(run.mixture).append(',').append(run.instance).append(',').append(run.seed).append(',')
        .append(run.metrics.hv).append(',').append(run.metrics.igd).append(',').append(run.metrics.spacing).append(',').append(run.metrics.nondominatedCount).append(',')
        .append(run.min(0)).append(',').append(run.min(1)).append(',').append(run.min(2)).append(',').append(run.status.getProperty("algorithmRunNanos", "")).append('\n');
    Files.write(output, csv.toString().getBytes(StandardCharsets.UTF_8));
  }

  private static List<Pair> paired(List<Run> runs) {
    Map<String, Run> base = new HashMap<>(); for (Run run : runs) if ("BASE".equals(run.arm)) base.put(run.instance + "/" + run.seed, run);
    List<Pair> values = new ArrayList<>();
    for (Run run : runs) if (!"BASE".equals(run.arm)) {
      Run b = base.get(run.instance + "/" + run.seed); if (b == null) throw new IllegalStateException("base missing");
      values.add(new Pair(run.arm, run.mixture, run.instance, run.seed,
          relative(b.min(0), run.min(0)), relative(b.min(1), run.min(1)), relative(b.min(2), run.min(2)),
          (run.metrics.hv - b.metrics.hv) / b.metrics.hv, (run.metrics.igd - b.metrics.igd) / b.metrics.igd,
          coverage(run.front, b.front), coverage(b.front, run.front)));
    }
    return values;
  }

  private static double relative(double baseline, double value) { return (baseline - value) / baseline; }
  private static double coverage(List<double[]> left, List<double[]> right) {
    int covered = 0; for (double[] target : right) { for (double[] source : left) if (P8MetricCalculator.dominates(source, target) || equal(source, target)) { covered++; break; } }
    return right.isEmpty() ? 0.0 : (double) covered / right.size();
  }
  private static boolean equal(double[] left, double[] right) { return Math.abs(left[0]-right[0])<=1e-12 && Math.abs(left[1]-right[1])<=1e-12 && Math.abs(left[2]-right[2])<=1e-12; }

  private static void writePairs(Path output, List<Pair> pairs) throws Exception {
    StringBuilder csv = new StringBuilder("arm,mixture,instance,seed,deltaCmax,deltaTEC,deltaTWC,deltaHV,deltaIGD,C_treatment_base,C_base_treatment\n");
    Collections.sort(pairs, new Comparator<Pair>() { @Override public int compare(Pair left, Pair right) { int c=left.arm.compareTo(right.arm);if(c!=0)return c;c=left.instance.compareTo(right.instance);return c!=0?c:Long.compare(left.seed,right.seed); }});
    for (Pair pair : pairs) csv.append(pair.arm).append(',').append(pair.mixture).append(',').append(pair.instance).append(',').append(pair.seed).append(',')
        .append(pair.dCmax).append(',').append(pair.dTec).append(',').append(pair.dTwc).append(',').append(pair.dHv).append(',').append(pair.dIgd).append(',').append(pair.coverTreatmentBase).append(',').append(pair.coverBaseTreatment).append('\n');
    Files.write(output, csv.toString().getBytes(StandardCharsets.UTF_8));
  }

  private static Decision decide(List<Pair> pairs) {
    List<Candidate> candidates = new ArrayList<>();
    for (Map.Entry<String, V35SubSwarmMixture> arm : ARMS.entrySet()) if (!"BASE".equals(arm.getKey())) {
      List<Pair> own = new ArrayList<>(); for (Pair pair : pairs) if (arm.getKey().equals(pair.arm)) own.add(pair);
      candidates.add(new Candidate(arm.getKey(), arm.getValue(), own));
    }
    List<Candidate> passed = new ArrayList<>(); for (Candidate candidate : candidates) if (candidate.passes()) passed.add(candidate);
    if (passed.isEmpty()) return new Decision("DOE1_HELDOUT_RETAIN_BASELINE_20_40_20_20", null, candidates);
    Collections.sort(passed, new Comparator<Candidate>() { @Override public int compare(Candidate left, Candidate right) {
      int c=Double.compare(right.dCmax,left.dCmax);if(c!=0)return c;c=Double.compare(right.dHv,left.dHv);if(c!=0)return c;c=Double.compare(left.dIgd,right.dIgd);return left.mixture.compareTo(right.mixture);
    }});
    return new Decision("DOE1_HELDOUT_ACCEPTED_NEW_MIXTURE", passed.get(0), candidates);
  }

  private static void writeDecision(Path markdown, Path properties, Decision decision) throws Exception {
    StringBuilder report = new StringBuilder("# V35-DOE-1 Held-out Confirmation\n\n");
    report.append("本报告只裁决已预注册的 BASE、T1、T2、T3；开发 reference 与 confirmation reference 严格分离。\n\n");
    report.append("| Arm | Mixture | median ΔCmax | median ΔHV | median ΔIGD | Gate A-E |\n|---|---|---:|---:|---:|---|\n");
    for (Candidate candidate : decision.all) report.append('|').append(candidate.arm).append('|').append(candidate.mixture).append('|').append(percent(candidate.dCmax)).append('|').append(percent(candidate.dHv)).append('|').append(percent(candidate.dIgd)).append('|').append(candidate.gateText()).append("|\n");
    report.append("\n## 唯一结论\n\n` ").append(decision.code).append(" `\n\n");
    if (decision.winner == null) report.append("没有新的 mixture 同时满足预注册的确认门；正式搜索容量冻结为 `20/40/20/20`。\n");
    else report.append("通过全部确认门的最优 mixture 为 `").append(decision.winner.mixture).append("`；选择顺序为 median ΔCmax、ΔHV、ΔIGD、字典序。\n");
    report.append("\n该结论不构成论文显著性结论，也不启动正式多矩阵实验。\n");
    Files.write(markdown, report.toString().getBytes(StandardCharsets.UTF_8));
    Files.write(properties, ("decision=" + decision.code + "\nfinalSearchMixture=" + (decision.winner == null ? "20/40/20/20" : decision.winner.mixture)
        + "\nheldoutRunCount=60\nreferencePolicy=per-instance ND(4 arms x 5 seeds)\n").getBytes(StandardCharsets.UTF_8));
  }

  private static String percent(double value) { return String.format(java.util.Locale.ROOT, "%.4f%%", value * 100.0); }

  private static void writeManifest(Path manifest, Path root) throws Exception {
    List<Path> files = new ArrayList<>(); Files.walk(root).filter(Files::isRegularFile).filter(path -> !path.equals(manifest)).forEach(files::add);
    Collections.sort(files); StringBuilder text = new StringBuilder("sha256\tpath\n");
    for (Path file : files) text.append(sha256(file)).append('\t').append(root.relativize(file).toString().replace('\\','/')).append('\n');
    Files.write(manifest, text.toString().getBytes(StandardCharsets.UTF_8));
  }

  private static String sha256(Path file) throws Exception { byte[] digest=MessageDigest.getInstance("SHA-256").digest(Files.readAllBytes(file));StringBuilder out=new StringBuilder();for(byte value:digest)out.append(String.format("%02x",value&0xff));return out.toString(); }
  private static Comparator<Run> runComparator() { return new Comparator<Run>() { @Override public int compare(Run left, Run right) {int c=left.arm.compareTo(right.arm);if(c!=0)return c;c=left.instance.compareTo(right.instance);return c!=0?c:Long.compare(left.seed,right.seed);}}; }

  private static final class Run {
    final String arm; final V35SubSwarmMixture mixture; final String instance; final long seed; final Path directory;
    Properties status; Map<String,String> config; List<double[]> front; P8MetricCalculator.Metrics metrics;
    Run(String arm,V35SubSwarmMixture mixture,String instance,long seed,Path directory){this.arm=arm;this.mixture=mixture;this.instance=instance;this.seed=seed;this.directory=directory;}
    double min(int objective){double result=Double.POSITIVE_INFINITY;for(double[] point:front)result=Math.min(result,point[objective]);return result;}
  }
  private static final class Pair {
    final String arm;final V35SubSwarmMixture mixture;final String instance;final long seed;final double dCmax,dTec,dTwc,dHv,dIgd,coverTreatmentBase,coverBaseTreatment;
    Pair(String arm,V35SubSwarmMixture mixture,String instance,long seed,double dCmax,double dTec,double dTwc,double dHv,double dIgd,double coverTreatmentBase,double coverBaseTreatment){this.arm=arm;this.mixture=mixture;this.instance=instance;this.seed=seed;this.dCmax=dCmax;this.dTec=dTec;this.dTwc=dTwc;this.dHv=dHv;this.dIgd=dIgd;this.coverTreatmentBase=coverTreatmentBase;this.coverBaseTreatment=coverBaseTreatment;}
  }
  private static final class Candidate {
    final String arm;final V35SubSwarmMixture mixture;final List<Pair> pairs;final double dCmax,dTec,dTwc,dHv,dIgd;
    Candidate(String arm,V35SubSwarmMixture mixture,List<Pair> pairs){this.arm=arm;this.mixture=mixture;this.pairs=pairs;this.dCmax=median(pairs,0);this.dTec=median(pairs,1);this.dTwc=median(pairs,2);this.dHv=median(pairs,3);this.dIgd=median(pairs,4);}
    boolean passes(){return dCmax>=.02&&dHv>=-.02&&dIgd<=.10&&!catastrophe()&&!systematic(1)&&!systematic(2);}
    boolean catastrophe(){for(String instance:INSTANCES){List<Pair> own=byInstance(instance);if(median(own,3)<-.05&&median(own,4)>.20)return true;}return false;}
    boolean systematic(int response){for(String instance:INSTANCES)if(!(median(byInstance(instance),response)<-.02))return false;return true;}
    List<Pair> byInstance(String instance){List<Pair> values=new ArrayList<>();for(Pair pair:pairs)if(instance.equals(pair.instance))values.add(pair);return values;}
    String gateText(){return "A="+(dCmax>=.02)+",B="+(dHv>=-.02)+",C="+(dIgd<=.10)+",D="+(!catastrophe())+",E="+(!systematic(1)&&!systematic(2));}
  }
  private static final class Decision {final String code;final Candidate winner;final List<Candidate> all;Decision(String code,Candidate winner,List<Candidate>all){this.code=code;this.winner=winner;this.all=all;}}
  private static double median(List<Pair> values,int response){List<Double> items=new ArrayList<>();for(Pair pair:values){switch(response){case 0:items.add(pair.dCmax);break;case 1:items.add(pair.dTec);break;case 2:items.add(pair.dTwc);break;case 3:items.add(pair.dHv);break;case 4:items.add(pair.dIgd);break;default:throw new IllegalArgumentException();}}Collections.sort(items);int size=items.size();return size%2==1?items.get(size/2):(items.get(size/2-1)+items.get(size/2))/2.0;}
}
