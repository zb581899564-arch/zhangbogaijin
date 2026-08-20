package org.uma.jmetal.runner.lc_psode;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.uma.jmetal.algorithm.multiobjective.mypso.p8.P8InitialPopulationProvider;
import org.uma.jmetal.algorithm.multiobjective.mypso.v35.p25e.V35P25EAlgorithmResult;
import org.uma.jmetal.algorithm.multiobjective.mypso.v35.p25e.V35P25EPaperAuthorEngine;
import org.uma.jmetal.problem.multiobjective.dfsp.decoder.ProductionDecodeMode;
import org.uma.jmetal.problem.multiobjective.dfsp.fatigue.V35ComparisonProblemAdapter;
import org.uma.jmetal.problem.multiobjective.dfsp.fatigue.V35P25ERepairAudit;
import org.uma.jmetal.problem.multiobjective.dfsp.fatigue.ZhangBoCanonicalProblemLoader;
import org.uma.jmetal.problem.multiobjective.dfsp.fatigue.ZhangBoCanonicalProductionProblem;
import org.uma.jmetal.problem.multiobjective.dfsp.fatigue.shift.ZhangBoShiftConfiguration;
import org.uma.jmetal.solution.PermutationSolution;

/**
 * Diagnostic-only audit of the P25E paper-author representation repairs.
 *
 * <p>Runs the four paper-author adapters with a small FE budget, installs the
 * pass-through {@link V35P25ERepairAudit} around each run and writes per-event
 * repair details plus per-algorithm summaries. The audit is observational:
 * front, FE, random streams and mechanism events are identical with or without
 * it. Results are never part of any reference front.</p>
 */
public final class ZhangBoV35P25ERepairAuditRunner {
  public static final long DEFAULT_SEED = 20260822L;
  public static final int DEFAULT_MAX_FES = 2000;

  private static final V35P25EPaperAuthorEngine.AlgorithmKind[] AUTHOR_KINDS = {
      V35P25EPaperAuthorEngine.AlgorithmKind.HMOPSO_QLS_F,
      V35P25EPaperAuthorEngine.AlgorithmKind.MOPSO_F,
      V35P25EPaperAuthorEngine.AlgorithmKind.MOPSODS_DE_F,
      V35P25EPaperAuthorEngine.AlgorithmKind.MOHEADE_F};

  private ZhangBoV35P25ERepairAuditRunner() { }

  public static void main(String[] args) throws Exception {
    Arguments value = Arguments.parse(args);
    Path project = value.projectRoot.toAbsolutePath().normalize();
    Path javaProject = Files.isDirectory(project.resolve("EADHFSP"))
        ? project : project.resolve("java-jmetal58");
    Path instance = javaProject.resolve("EADHFSP/20_2_3_1.txt");
    Path extension = javaProject.resolve("instance-extensions/v1");
    Path fatigue = javaProject.resolve("fatigue-parameters/v1");
    Path target = value.output.toAbsolutePath().normalize();
    Files.createDirectories(target);

    for (V35P25EPaperAuthorEngine.AlgorithmKind kind : AUTHOR_KINDS) {
      ZhangBoCanonicalProductionProblem problem = ZhangBoCanonicalProblemLoader.load(
          instance, ProductionDecodeMode.FM3, value.seed, extension, fatigue,
          ZhangBoShiftConfiguration.none());
      List<PermutationSolution<Integer>> initial = new ArrayList<>();
      for (int index = 0; index < 100; index++) initial.add(problem.createSolution());
      V35ComparisonProblemAdapter adapter = new V35ComparisonProblemAdapter(problem,
          P8InitialPopulationProvider.copy(initial),
          V35ComparisonProblemAdapter.ObjectiveView.AUTHOR_SEVEN_SLOT, value.maxFEs);
      V35P25ERepairAudit.install();
      try {
        V35P25EAlgorithmResult record = V35P25EPaperAuthorEngine.run(kind, adapter,
            100, value.maxFEs, value.seed);
        List<V35P25ERepairAudit.Event> events = new ArrayList<>(V35P25ERepairAudit.events());
        writeCsv(target, kind, events);
        writeSummary(target, kind, events, record);
        writeFront(target, kind, record.getFront());
        System.out.println("AUDIT_COMPLETED algorithm=" + kind + " FE="
            + record.getEvaluations() + " repairs=" + events.size()
            + " front=" + record.getFront().size());
      } finally {
        V35P25ERepairAudit.clear();
      }
    }
  }

  private static void writeCsv(Path target, V35P25EPaperAuthorEngine.AlgorithmKind kind,
      List<V35P25ERepairAudit.Event> events) throws Exception {
    StringBuilder out = new StringBuilder("vector,position,oldValue,newValue,evaluationIndex\n");
    for (V35P25ERepairAudit.Event event : events) {
      out.append(event.vector).append(',').append(event.position).append(',')
          .append(event.oldValue).append(',').append(event.newValue).append(',')
          .append(event.evaluationIndex).append('\n');
    }
    Files.write(target.resolve("repair-audit-" + kind + ".csv"),
        out.toString().getBytes(StandardCharsets.UTF_8));
  }

  private static void writeFront(Path target, V35P25EPaperAuthorEngine.AlgorithmKind kind,
      List<double[]> front) throws Exception {
    StringBuilder out = new StringBuilder("Cmax,TEC,TWC\n");
    for (double[] point : front) {
      out.append(point[0]).append(',').append(point[1]).append(',').append(point[2]).append('\n');
    }
    Files.write(target.resolve("front-" + kind + ".csv"),
        out.toString().getBytes(StandardCharsets.UTF_8));
  }

  private static void writeSummary(Path target, V35P25EPaperAuthorEngine.AlgorithmKind kind,
      List<V35P25ERepairAudit.Event> events, V35P25EAlgorithmResult record) throws Exception {
    Map<String, Integer> byVector = new LinkedHashMap<>();
    Map<String, Integer> byPosition = new LinkedHashMap<>();
    Map<String, Integer> oldNewPairs = new LinkedHashMap<>();
    List<Integer> perEvaluation = new ArrayList<>();
    int lastIndex = -1;
    int countForCurrent = 0;
    for (V35P25ERepairAudit.Event event : events) {
      byVector.merge(event.vector, 1, Integer::sum);
      byPosition.merge("p" + event.position, 1, Integer::sum);
      oldNewPairs.merge(event.oldValue + "->" + event.newValue, 1, Integer::sum);
      if (event.evaluationIndex != lastIndex) {
        if (lastIndex >= 0) perEvaluation.add(countForCurrent);
        lastIndex = event.evaluationIndex;
        countForCurrent = 1;
      } else {
        countForCurrent++;
      }
    }
    if (lastIndex >= 0) perEvaluation.add(countForCurrent);
    Collections.sort(perEvaluation);

    StringBuilder out = new StringBuilder();
    out.append("algorithm=").append(kind).append('\n');
    out.append("FE=").append(record.getEvaluations()).append('\n');
    out.append("repairsTotal=").append(events.size()).append('\n');
    out.append("repairsPerEvaluationMean=")
        .append(events.isEmpty() ? 0.0 : String.format("%.4f",
            (double) events.size() / record.getEvaluations())).append('\n');
    out.append("evaluationsWithRepair=").append(perEvaluation.size()).append(" / ")
        .append(record.getEvaluations()).append('\n');
    if (!perEvaluation.isEmpty()) {
      out.append("maxRepairsInOneEvaluation=").append(perEvaluation.get(perEvaluation.size() - 1))
          .append('\n');
      out.append("repairsP50/P90/P99=")
          .append(percentile(perEvaluation, 50)).append('/')
          .append(percentile(perEvaluation, 90)).append('/')
          .append(percentile(perEvaluation, 99)).append('\n');
    }
    out.append("byVector=").append(byVector).append('\n');
    out.append("byPosition=").append(byPosition).append('\n');
    out.append("oldToNewPairs=").append(oldNewPairs).append('\n');
    out.append("frontSize=").append(record.getFront().size()).append('\n');
    Files.write(target.resolve("repair-summary-" + kind + ".txt"),
        out.toString().getBytes(StandardCharsets.UTF_8));
    System.out.println(out);
  }

  private static int percentile(List<Integer> sorted, double p) {
    if (sorted.isEmpty()) return 0;
    int index = (int) Math.ceil(p / 100.0 * sorted.size()) - 1;
    return sorted.get(Math.max(0, Math.min(sorted.size() - 1, index)));
  }

  private static final class Arguments {
    private Path projectRoot;
    private Path output;
    private long seed = DEFAULT_SEED;
    private int maxFEs = DEFAULT_MAX_FES;

    private static Arguments parse(String[] args) {
      Arguments value = new Arguments();
      for (int index = 0; index < args.length; index += 2) {
        if (index + 1 >= args.length) throw usage();
        if ("--project-root".equals(args[index])) value.projectRoot = Paths.get(args[index + 1]);
        else if ("--output".equals(args[index])) value.output = Paths.get(args[index + 1]);
        else if ("--seed".equals(args[index])) value.seed = Long.parseLong(args[index + 1]);
        else if ("--max-fes".equals(args[index])) value.maxFEs = Integer.parseInt(args[index + 1]);
        else throw usage();
      }
      if (value.projectRoot == null || value.output == null) throw usage();
      return value;
    }

    private static IllegalArgumentException usage() {
      return new IllegalArgumentException("Usage: --project-root <path> --output <path> "
          + "[--seed <long>] [--max-fes <int>]");
    }
  }
}
