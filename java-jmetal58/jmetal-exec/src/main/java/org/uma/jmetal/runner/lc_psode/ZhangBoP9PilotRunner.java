package org.uma.jmetal.runner.lc_psode;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.uma.jmetal.algorithm.multiobjective.mypso.p8.P8AblationProfile;
import org.uma.jmetal.algorithm.multiobjective.mypso.p8.P8ExperimentExecutor;
import org.uma.jmetal.algorithm.multiobjective.mypso.p8.P8ExperimentRegistry;
import org.uma.jmetal.algorithm.multiobjective.mypso.p8.P8ExperimentSpec;
import org.uma.jmetal.algorithm.multiobjective.mypso.p8.P8InitialPopulationProvider;
import org.uma.jmetal.algorithm.multiobjective.mypso.p8.P8RunRecord;
import org.uma.jmetal.algorithm.multiobjective.mypso.p8.P8RunStatus;
import org.uma.jmetal.problem.PermutationProblem;
import org.uma.jmetal.problem.multiobjective.dfsp.decoder.ProductionDecodeMode;
import org.uma.jmetal.problem.multiobjective.dfsp.fatigue.ZhangBoCanonicalProblemLoader;
import org.uma.jmetal.problem.multiobjective.dfsp.fatigue.ZhangBoCanonicalProductionProblem;
import org.uma.jmetal.problem.multiobjective.dfsp.model.P8GoldenAuthorCompatibilityBridge;
import org.uma.jmetal.solution.PermutationSolution;

/**
 * Small, scope-locked pilot used before P9's formal 500000-FE matrix.
 *
 * <p>It executes B0, B1, B5 and FULL on two instances and three fixed seeds, using
 * 100 particles and 20000 complete evaluations.  The output is engineering evidence only;
 * it is not a formal reproduction result.</p>
 */
public final class ZhangBoP9PilotRunner {
  private static final String[] LABELS = {"B0", "B1", "B5", "FULL"};
  private static final long[] SEEDS = {20260808L, 20260809L, 20260810L};
  private static final int POPULATION = 100;
  private static final int MAX_FES = 20000;

  private ZhangBoP9PilotRunner() { }

  public static void main(String[] args) throws Exception {
    Path project = Paths.get(".").toAbsolutePath().normalize();
    if (!Files.isDirectory(project.resolve("EADHFSP"))) {
      throw new IllegalStateException("Run the pilot from java-jmetal58: " + project);
    }
    Path output = args.length == 0
        ? project.getParent().resolve("docs/evidence/P9-pilot")
        : Paths.get(args[0]).toAbsolutePath().normalize();
    Files.createDirectories(output);
    Files.createDirectories(output.resolve("fronts"));

    List<P8ExperimentSpec> selected = selectedSpecs();
    Path bridgeRoot = project.resolve("p8-bridge/v1");
    P8GoldenAuthorCompatibilityBridge.Manifest bridge =
        P8GoldenAuthorCompatibilityBridge.materialize(bridgeRoot);
    Files.copy(bridge.root.resolve("bridge-manifest.txt"),
        output.resolve("golden-bridge-manifest.txt"), StandardCopyOption.REPLACE_EXISTING);

    List<Binding> bindings = Arrays.asList(
        new Binding("chapter4-golden-author-bridge",
            bridge.root.resolve("EADHFSP/10_2_2_1.txt"),
            bridge.root.resolve("instance-extensions/v1"),
            bridge.root.resolve("fatigue-parameters/v1"), bridge.instanceSha256),
        new Binding("20_2_3_1", project.resolve("EADHFSP/20_2_3_1.txt"),
            project.resolve("instance-extensions/v1"),
            project.resolve("fatigue-parameters/v1"),
            sha256(Files.readAllBytes(project.resolve("EADHFSP/20_2_3_1.txt")))));

    List<P8RunRecord> records = new ArrayList<>();
    for (Binding binding : bindings) {
      for (long seed : SEEDS) {
        String expectedInitialHash = null;
        for (P8ExperimentSpec spec : selected) {
          List<PermutationSolution<Integer>> initial = binding.createInitialPopulation(spec, seed);
          String initialHash = P8InitialPopulationProvider.sha256(initial);
          if (expectedInitialHash == null) expectedInitialHash = initialHash;
          if (!expectedInitialHash.equals(initialHash)) {
            throw new IllegalStateException("Pilot initial population drift: instance="
                + binding.name + ", seed=" + seed + ", label=" + spec.getLabel());
          }
          P8RunRecord record = P8ExperimentExecutor.execute(spec, binding.name,
              binding.instanceSha256, seed, binding.createProblem(spec, seed),
              P8InitialPopulationProvider.copy(initial));
          records.add(record);
          writeFront(output, record);
          System.out.println("P9_PILOT_RUN instance=" + binding.name + " label="
              + spec.getLabel() + " seed=" + seed + " status=" + record.getStatus()
              + " fe=" + record.getFullEvaluations() + " caTaFE="
              + record.getCaTaEvaluations() + " repairs=" + record.getCfvfRepairs()
              + " illegal=" + record.getIllegalSolutions() + " wallMs="
              + record.getWallClockMillis());
        }
      }
    }

    validate(records);
    Files.write(output.resolve("run-records.csv"), recordsCsv(records).getBytes(StandardCharsets.UTF_8));
    Files.write(output.resolve("pilot-summary.csv"), summaryCsv(records).getBytes(StandardCharsets.UTF_8));
    Files.write(output.resolve("P9_PILOT_REPORT.md"), report(records).getBytes(StandardCharsets.UTF_8));
    System.out.println("P9_PILOT_COMPLETED output=" + output + " records=" + records.size());
  }

  private static List<P8ExperimentSpec> selectedSpecs() {
    Map<String, P8ExperimentSpec> byLabel = new LinkedHashMap<>();
    for (P8ExperimentSpec source : P8ExperimentRegistry.currentMatrix()) {
      byLabel.put(source.getLabel(), source);
    }
    List<P8ExperimentSpec> result = new ArrayList<>();
    for (String label : LABELS) {
      P8ExperimentSpec source = byLabel.get(label);
      if (source == null) throw new IllegalStateException("Missing formal P8 profile: " + label);
      result.add(new P8ExperimentSpec(source.getMatrix(), source.getLabel(),
          source.getMechanism(), source.getConfigurationKey() + "-p9-pilot-20k",
          source.getAblationProfile(), P8RunStatus.COMPLETED,
          "P9 pilot only; not formal reproduction", POPULATION, MAX_FES,
          source.getPhysicalSubswarmSizes()));
    }
    return result;
  }

  private static ProductionDecodeMode productionMode(P8AblationProfile.DecoderMode mode) {
    switch (mode) {
      case DETERMINISTIC_CANONICAL:
      case CORRECTED_NO_FATIGUE:
        return ProductionDecodeMode.CANONICAL_NO_FATIGUE;
      case ACCUMULATION_ONLY:
        return ProductionDecodeMode.FM1;
      case ACCUMULATION_RECOVERY:
        return ProductionDecodeMode.FM2;
      case FATIGUE_AWARE_SELECTION:
        return ProductionDecodeMode.FM3;
      default:
        throw new IllegalArgumentException("Diagnostic decoder is forbidden in P9 pilot: " + mode);
    }
  }

  private static void validate(List<P8RunRecord> records) {
    int expected = LABELS.length * SEEDS.length * 2;
    if (records.size() != expected) {
      throw new IllegalStateException("Pilot record count mismatch: " + records.size()
          + " != " + expected);
    }
    for (P8RunRecord record : records) {
      if (record.getStatus() != P8RunStatus.COMPLETED) {
        throw new IllegalStateException("Pilot failed: " + record.getRunId() + " "
            + record.getReason());
      }
      if (record.getFullEvaluations() > MAX_FES || record.getFullEvaluations() < POPULATION) {
        throw new IllegalStateException("Pilot FE boundary failure: " + record.getRunId()
            + " fe=" + record.getFullEvaluations());
      }
      if (record.getIllegalSolutions() != 0 || record.getCfvfRepairs() != 0) {
        throw new IllegalStateException("Pilot legality failure: " + record.getRunId()
            + " illegal=" + record.getIllegalSolutions() + " repair="
            + record.getCfvfRepairs());
      }
      if ("FULL".equals(record.getLabel()) && record.getCaTaEvaluations() <= 0) {
        throw new IllegalStateException("FULL did not execute CA-TA: " + record.getRunId());
      }
    }
  }

  private static void writeFront(Path output, P8RunRecord record) throws Exception {
    Path directory = output.resolve("fronts").resolve(record.getInstance());
    Files.createDirectories(directory);
    StringBuilder csv = new StringBuilder("Cmax,TEC,TWC\n");
    for (double[] point : record.getFront()) {
      csv.append(number(point[0])).append(',').append(number(point[1])).append(',')
          .append(number(point[2])).append('\n');
    }
    Files.write(directory.resolve(record.getLabel() + "-" + record.getSeed() + ".csv"),
        csv.toString().getBytes(StandardCharsets.UTF_8));
  }

  private static String recordsCsv(List<P8RunRecord> records) {
    StringBuilder csv = new StringBuilder("instance,label,seed,status,fullEvaluations,wallClockMillis,")
        .append("caTaEvaluations,cfvfRepairs,illegalSolutions,frontSize,minCmax,minTEC,minTWC,")
        .append("Fmax,Favg,fatigueExcess,highFatigueRatio,totalNaturalRecovery,")
        .append("configurationSha256,initialPopulationSha256,reason\n");
    for (P8RunRecord record : records) {
      double[] minimum = minima(record.getFront());
      csv.append(csv(record.getInstance())).append(',').append(record.getLabel()).append(',')
          .append(record.getSeed()).append(',').append(record.getStatus()).append(',')
          .append(record.getFullEvaluations()).append(',').append(record.getWallClockMillis())
          .append(',').append(record.getCaTaEvaluations()).append(',')
          .append(record.getCfvfRepairs()).append(',').append(record.getIllegalSolutions())
          .append(',').append(record.getFront().size()).append(',').append(number(minimum[0]))
          .append(',').append(number(minimum[1])).append(',').append(number(minimum[2]))
          .append(',').append(number(record.getFmax())).append(',')
          .append(number(record.getFavg())).append(',').append(number(record.getFatigueExcess()))
          .append(',').append(number(record.getHighFatigueRatio())).append(',')
          .append(number(record.getTotalNaturalRecovery())).append(',')
          .append(record.getConfigurationSha256()).append(',')
          .append(record.getInitialPopulationSha256()).append(',')
          .append(csv(record.getReason())).append('\n');
    }
    return csv.toString();
  }

  private static String summaryCsv(List<P8RunRecord> records) {
    StringBuilder csv = new StringBuilder("instance,label,runs,meanFE,meanWallMs,meanFrontSize,")
        .append("meanMinCmax,meanMinTEC,meanMinTWC,meanFmax,meanFatigueExcess,meanNaturalRecovery\n");
    for (String instance : Arrays.asList("chapter4-golden-author-bridge", "20_2_3_1")) {
      for (String label : LABELS) {
        int count = 0;
        double fe = 0.0, wall = 0.0, size = 0.0, cmax = 0.0, tec = 0.0, twc = 0.0;
        double fmax = 0.0, excess = 0.0, recovery = 0.0;
        for (P8RunRecord record : records) {
          if (!instance.equals(record.getInstance()) || !label.equals(record.getLabel())) continue;
          double[] minimum = minima(record.getFront());
          count++;
          fe += record.getFullEvaluations();
          wall += record.getWallClockMillis();
          size += record.getFront().size();
          cmax += minimum[0]; tec += minimum[1]; twc += minimum[2];
          fmax += record.getFmax(); excess += record.getFatigueExcess();
          recovery += record.getTotalNaturalRecovery();
        }
        csv.append(instance).append(',').append(label).append(',').append(count).append(',')
            .append(number(fe / count)).append(',').append(number(wall / count)).append(',')
            .append(number(size / count)).append(',').append(number(cmax / count)).append(',')
            .append(number(tec / count)).append(',').append(number(twc / count)).append(',')
            .append(number(fmax / count)).append(',').append(number(excess / count)).append(',')
            .append(number(recovery / count)).append('\n');
      }
    }
    return csv.toString();
  }

  private static String report(List<P8RunRecord> records) {
    long totalFe = 0L, caTaFe = 0L, wall = 0L;
    for (P8RunRecord record : records) {
      totalFe += record.getFullEvaluations();
      caTaFe += record.getCaTaEvaluations();
      wall += record.getWallClockMillis();
    }
    return "# P9小规模机制贯通先导实验\n\n"
        + "- 性质：工程先导，不是正式论文复现或显著性实验。\n"
        + "- 配置：`B0/B1/B5/FULL`。\n"
        + "- 实例：第四章10工件桥实例、`20_2_3_1`。\n"
        + "- 种子：`20260808/20260809/20260810`。\n"
        + "- 种群：`100`；单运行预算上限：`20000 FE`。\n"
        + "- 完成记录：`" + records.size() + "/24`。\n"
        + "- 完整评价总数：`" + totalFe + "`；CA-TA局部评价：`" + caTaFe + "`。\n"
        + "- 累计运行wall-clock：`" + wall + " ms`。\n"
        + "- 硬门：非法解0、CFVF异常repair 0、FULL的CA-TA评价大于0。\n\n"
        + "本实验只回答编解码和三个创新点能否在同一生产链路中稳定运行，"
        + "不用于宣称算法优越性。`sampled_reproduction_accepted`与"
        + "`full_reproduction_accepted`均保持`false`。\n";
  }

  private static double[] minima(List<double[]> front) {
    double[] result = {Double.NaN, Double.NaN, Double.NaN};
    for (double[] point : front) {
      for (int i = 0; i < 3; i++) {
        if (Double.isNaN(result[i]) || point[i] < result[i]) result[i] = point[i];
      }
    }
    return result;
  }

  private static String number(double value) {
    return Double.isNaN(value) ? "NaN" : String.format(Locale.ROOT, "%.12g", value);
  }

  private static String csv(String value) {
    String normalized = value == null ? "" : value.replace("\"", "\"\"");
    return '"' + normalized + '"';
  }

  private static String sha256(byte[] value) {
    try {
      byte[] bytes = MessageDigest.getInstance("SHA-256").digest(value);
      StringBuilder result = new StringBuilder();
      for (byte item : bytes) result.append(String.format("%02x", item & 0xff));
      return result.toString();
    } catch (NoSuchAlgorithmException exception) {
      throw new IllegalStateException("SHA-256 unavailable", exception);
    }
  }

  private static final class Binding {
    private final String name;
    private final Path instance;
    private final Path extensionDirectory;
    private final Path fatigueDirectory;
    private final String instanceSha256;

    private Binding(String name, Path instance, Path extensionDirectory,
        Path fatigueDirectory, String instanceSha256) {
      this.name = name;
      this.instance = instance;
      this.extensionDirectory = extensionDirectory;
      this.fatigueDirectory = fatigueDirectory;
      this.instanceSha256 = instanceSha256;
    }

    private PermutationProblem<PermutationSolution<Integer>> createProblem(
        P8ExperimentSpec spec, long seed) {
      try {
        return ZhangBoCanonicalProblemLoader.load(instance,
            productionMode(spec.getAblationProfile().getDecoderMode()), seed,
            extensionDirectory, fatigueDirectory,
            spec.getAblationProfile().getShiftConfiguration());
      } catch (Exception exception) {
        throw new IllegalStateException("Cannot load pilot instance " + name, exception);
      }
    }

    private List<PermutationSolution<Integer>> createInitialPopulation(
        P8ExperimentSpec spec, long seed) {
      ZhangBoCanonicalProductionProblem problem =
          (ZhangBoCanonicalProductionProblem) createProblem(spec, seed);
      List<PermutationSolution<Integer>> result = new ArrayList<>(POPULATION);
      for (int index = 0; index < POPULATION; index++) result.add(problem.createSolution());
      return result;
    }
  }
}
