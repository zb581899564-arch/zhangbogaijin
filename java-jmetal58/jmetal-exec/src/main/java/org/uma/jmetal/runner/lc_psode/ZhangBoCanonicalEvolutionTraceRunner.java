package org.uma.jmetal.runner.lc_psode;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import org.uma.jmetal.algorithm.multiobjective.mypso.ZhangBoMOHPSOQ;
import org.uma.jmetal.algorithm.multiobjective.mypso.ZhangBoMOHPSOQBuilder;
import org.uma.jmetal.algorithm.multiobjective.mypso.p8.P8ExperimentRegistry;
import org.uma.jmetal.algorithm.multiobjective.mypso.p8.P8ExperimentSpec;
import org.uma.jmetal.algorithm.multiobjective.mypso.p8.P8InitialPopulationProvider;
import org.uma.jmetal.algorithm.multiobjective.mypso.zhangbo.ZhangBoArchiveEntry;
import org.uma.jmetal.algorithm.multiobjective.mypso.zhangbo.ZhangBoEventLog;
import org.uma.jmetal.algorithm.multiobjective.mypso.zhangbo.ZhangBoGlobalSearchConfiguration;
import org.uma.jmetal.algorithm.multiobjective.mypso.zhangbo.ZhangBoLineageMemory;
import org.uma.jmetal.algorithm.multiobjective.mypso.zhangbo.ZhangBoLineageTag;
import org.uma.jmetal.algorithm.multiobjective.mypso.zhangbo.ZhangBoQgController;
import org.uma.jmetal.algorithm.multiobjective.mypso.zhangbo.cata.ZhangBoNeighborhoodCandidateGateway;
import org.uma.jmetal.problem.multiobjective.dfsp.decoder.ProductionDecodeMode;
import org.uma.jmetal.problem.multiobjective.dfsp.fatigue.ZhangBoCanonicalProblemLoader;
import org.uma.jmetal.problem.multiobjective.dfsp.fatigue.ZhangBoCanonicalProductionProblem;
import org.uma.jmetal.problem.multiobjective.dfsp.fatigue.ZhangBoCanonicalSolutionFactory;
import org.uma.jmetal.problem.multiobjective.dfsp.model.Chapter4GoldenFixture;
import org.uma.jmetal.problem.multiobjective.dfsp.model.DhhfspFourVectorSolution;
import org.uma.jmetal.solution.PermutationSolution;
import org.uma.jmetal.util.pseudorandom.JMetalRandom;

/** Fixed 10-particle, 5000-FE explanatory trace for the I1/X0 lineage. */
public final class ZhangBoCanonicalEvolutionTraceRunner {
  private static final long SEED = 20260808L;
  private static final int POPULATION = 10;
  private static final int MAX_FES = 5000;
  private static final long X0_INITIAL_LINEAGE = 2L;

  private ZhangBoCanonicalEvolutionTraceRunner() { }

  public static void main(String[] args) throws Exception {
    Arguments parsed = Arguments.parse(args);
    String previous = System.getProperty(ZhangBoEventLog.FULL_CAPTURE_PROPERTY);
    System.setProperty(ZhangBoEventLog.FULL_CAPTURE_PROPERTY, "true");
    try {
      run(parsed.projectRoot.toAbsolutePath().normalize(),
          parsed.evidenceRoot.toAbsolutePath().normalize());
    } finally {
      if (previous == null) System.clearProperty(ZhangBoEventLog.FULL_CAPTURE_PROPERTY);
      else System.setProperty(ZhangBoEventLog.FULL_CAPTURE_PROPERTY, previous);
    }
  }

  static void run(Path projectRoot, Path evidenceRoot) throws Exception {
    Path evolution = evidenceRoot.resolve("05_one_particle_evolution");
    Path localSearch = evidenceRoot.resolve("06_local_search");
    Path environment = evidenceRoot.resolve("07_environment_selection");
    requireEmpty(evolution); requireEmpty(localSearch); requireEmpty(environment);

    Path bridge = projectRoot.resolve("java-jmetal58/p8-bridge/v1");
    P8ExperimentSpec full = P8ExperimentRegistry.find("FULL");
    ZhangBoCanonicalProductionProblem problem = ZhangBoCanonicalProblemLoader.load(
        bridge.resolve("EADHFSP/10_2_2_1.txt"), ProductionDecodeMode.FM3, SEED,
        bridge.resolve("instance-extensions/v1"), bridge.resolve("fatigue-parameters/v1"),
        full.getAblationProfile().getShiftConfiguration());
    ZhangBoCanonicalSolutionFactory factory = new ZhangBoCanonicalSolutionFactory(
        problem.getInstance(), ProductionDecodeMode.FM3, SEED);
    List<PermutationSolution<Integer>> initial = new ArrayList<>();
    for (int ordinal = 0; ordinal < POPULATION; ordinal++) initial.add(factory.create(ordinal));
    Chapter4GoldenFixture fixture = Chapter4GoldenFixture.load();
    DhhfspFourVectorSolution golden = fixture.createSolution();
    initial.set(2, new DhhfspFourVectorSolution(golden.getJobSequence(),
        golden.getFactoryAssignments(), golden.getMachineAssignments(),
        golden.getWorkerAssignments(), ProductionDecodeMode.FM3.getSemanticTag(),
        ZhangBoCanonicalProductionProblem.NUMBER_OF_OBJECTIVES));
    String initialHash = P8InitialPopulationProvider.sha256(initial);
    writePopulation(initial, evolution.resolve("initial_population.csv"));

    ZhangBoGlobalSearchConfiguration global = P8ExperimentRegistry.configurationFor(full, SEED);
    ZhangBoMOHPSOQ algorithm = new ZhangBoMOHPSOQBuilder(problem, POPULATION,
        problem.getNumberOfFactories(), 0.0, 0.8, 0.8, 50)
        .setMaxIterations(MAX_FES).setSwarmSize(POPULATION).setRand_k(0.6)
        .setCrossoverRate(0.2).setCrossoverRates4machine(0.5)
        .setCrossoverRates4worker(0.5).setMutationRate(0.08)
        .setMutationRate4machine(0.15).setMutationRate4worker(0.25)
        .setLocalSearch(30).setPhysicalSubswarmSizes(2, 4, 2, 2)
        .setGlobalSearchConfiguration(global)
        .setInitialSwarmOverride(P8InitialPopulationProvider.copy(initial)).build();
    algorithm.setCaTaNanoClock(new ZhangBoNeighborhoodCandidateGateway.NanoClock() {
      private long value;
      @Override public long nanoTime() { value += 1000L; return value; }
    });
    JMetalRandom.getInstance().setSeed(SEED);
    algorithm.run();

    List<String> lineageEvents = algorithm.getZhangBoLineageEvents();
    Set<Long> descendants = descendants(lineageEvents, X0_INITIAL_LINEAGE);
    List<String> cfvf = filterLineage(algorithm.getZhangBoP6Events(), descendants);
    List<String> qp = filterLineage(algorithm.getQpEvents(), descendants);
    List<String> cata = filterLineage(algorithm.getCaTaEvents(), descendants);
    List<String> qg = algorithm.getQgEvents();
    List<String> pddr = algorithm.getZhangBoPddrEvents();
    writeLines(evolution.resolve("qg_events.log"), qg);
    writeLines(evolution.resolve("qp_tracked_lineage_events.log"), qp);
    writeLines(evolution.resolve("cfvf_tracked_lineage_events.log"), cfvf);
    writeLines(evolution.resolve("dual_q_events.log"), algorithm.getDualQCoordinationEvents());
    writeLines(localSearch.resolve("cata_tracked_lineage_events.log"), cata);
    Files.write(localSearch.resolve("cata_statistics.txt"),
        algorithm.getCaTaStatisticsCanonicalText().getBytes(StandardCharsets.UTF_8));
    writeLines(environment.resolve("pddr_events.log"), pddr);
    writeLines(environment.resolve("lineage_events.log"), lineageEvents);
    Files.write(environment.resolve("lineage_final_state.txt"),
        algorithm.getZhangBoLineageCanonicalText().getBytes(StandardCharsets.UTF_8));
    writePopulation(algorithm.getResult(), environment.resolve("final_population.csv"));

    boolean qgObserved = !qg.isEmpty() && algorithm.getQgSelectionCount() > 0;
    boolean qpObserved = !qp.isEmpty() && algorithm.getQpExecutedActionCount() > 0;
    boolean cfvfObserved = contains(cfvf, "offspringFingerprint=")
        && algorithm.getCfvfOffspringCount() > 0;
    boolean archiveObserved = algorithm.getZhangBoArchiveInsertionCount() > 0;
    boolean caTaDecisionObserved = contains(cata, ",decision=");
    boolean caTaAcceptedObserved = contains(cata, ",accepted=true");
    boolean pddrObserved = !pddr.isEmpty() && algorithm.getEvaluatedPddrSelections() > 0;
    boolean trackedLineagePddrOutcome = containsAnyLineageOutcome(lineageEvents, descendants);
    boolean survivingDescendant = false;
    long stableDescendant = Long.MAX_VALUE;
    for (Long id : algorithm.getZhangBoLineageMemories().keySet()) {
      if (descendants.contains(id)) {
        survivingDescendant = true;
        stableDescendant = Math.min(stableDescendant, id);
      }
    }
    boolean complete = qgObserved && qpObserved && cfvfObserved && archiveObserved
        && caTaDecisionObserved && caTaAcceptedObserved && pddrObserved
        && trackedLineagePddrOutcome;
    String summary = "schema=zhangbo-i1-evolution-trace-v1\n"
        + "seed=" + SEED + "\npopulation=" + POPULATION + "\nmaxFEs=" + MAX_FES + "\n"
        + "physicalSubswarmSizes=2,4,2,2\ninitialX0Slot=2\ninitialLineage=2\n"
        + "initialPopulationSha256=" + initialHash + "\n"
        + "fullEvaluations=" + algorithm.getFullEvaluationCount() + "\n"
        + "trackedDescendants=" + sorted(descendants) + "\n"
        + "stableMinimumSurvivingDescendant="
        + (survivingDescendant ? stableDescendant : "NONE") + "\n"
        + "qgObserved=" + qgObserved + "\nqpObserved=" + qpObserved
        + "\ncfvfObserved=" + cfvfObserved + "\narchiveObserved=" + archiveObserved
        + "\ncaTaDecisionObserved=" + caTaDecisionObserved
        + "\ncaTaAcceptedObserved=" + caTaAcceptedObserved
        + "\npddrObserved=" + pddrObserved
        + "\ntrackedLineagePddrOutcome=" + trackedLineagePddrOutcome
        + "\nlineageTerminalOutcome=" + (survivingDescendant ? "SURVIVED" : "DELETED_AFTER_TRACE")
        + "\nsurvivingDescendant=" + survivingDescendant
        + "\nsingle_lineage_evolution_trace_validated=" + complete + "\n"
        + "qgSelections=" + algorithm.getQgSelectionCount()
        + "\nqpActions=" + algorithm.getQpExecutedActionCount()
        + "\ncfvfOffspring=" + algorithm.getCfvfOffspringCount()
        + "\narchiveInsertions=" + algorithm.getZhangBoArchiveInsertionCount()
        + "\ncaTaTestCalls=" + algorithm.getCaTaTestCalls()
        + "\ncaTaApplyCalls=" + algorithm.getCaTaApplyCalls()
        + "\ncaTaFullEvaluations=" + algorithm.getCaTaFullEvaluations()
        + "\npddrSelections=" + algorithm.getEvaluatedPddrSelections() + "\n"
        + "qgTableHash=" + algorithm.getQgTableHash()
        + "\nqpTableHash=" + algorithm.getQpTableHash() + "\n";
    Files.write(evolution.resolve("trace_summary.properties"), summary.getBytes(StandardCharsets.UTF_8));
    if (!complete) throw new IllegalStateException("TRACE_EVENT_NOT_OBSERVED\n" + summary);
    System.out.println("CANONICAL_EVOLUTION_TRACE_COMPLETED\n" + summary);
  }

  private static Set<Long> descendants(List<String> events, long root) {
    Set<Long> result = new HashSet<>(); result.add(root);
    boolean changed;
    do {
      changed = false;
      for (String event : events) {
        if (!event.startsWith("split:old=")) continue;
        long oldId = numberAfter(event, "old=");
        long newId = numberAfter(event, "new=");
        if (result.contains(oldId) && result.add(newId)) changed = true;
      }
    } while (changed);
    return result;
  }

  private static long numberAfter(String text, String token) {
    int start = text.indexOf(token);
    if (start < 0) return -1L;
    start += token.length(); int end = start;
    while (end < text.length() && Character.isDigit(text.charAt(end))) end++;
    return Long.parseLong(text.substring(start, end));
  }

  private static List<String> filterLineage(List<String> events, Set<Long> ids) {
    List<String> result = new ArrayList<>();
    for (String event : events) {
      for (Long id : ids) {
        if (event.contains("lineage=" + id)
            || event.contains("lineage:" + id) || event.contains("lineage=" + id + ",")) {
          result.add(event); break;
        }
      }
    }
    return result;
  }

  private static boolean contains(List<String> values, String token) {
    for (String value : values) if (value.contains(token)) return true;
    return false;
  }

  private static boolean containsAnyLineageOutcome(List<String> events, Set<Long> ids) {
    for (String event : events) {
      for (Long id : ids) {
        if ((event.startsWith("evolve:lineage=" + id + ",")
            || event.startsWith("delete:lineage=" + id + ",")
            || event.startsWith("split:old=" + id + ","))) return true;
      }
    }
    return false;
  }

  private static List<Long> sorted(Set<Long> values) {
    List<Long> result = new ArrayList<>(values); Collections.sort(result); return result;
  }

  private static void writeLines(Path path, List<String> values) throws IOException {
    Files.createDirectories(path.getParent());
    Files.write(path, (String.join("\n", values) + (values.isEmpty() ? "" : "\n"))
        .getBytes(StandardCharsets.UTF_8));
  }

  private static void writePopulation(
      List<PermutationSolution<Integer>> values, Path path) throws IOException {
    StringBuilder out = new StringBuilder(
        "slot,lineage,JS,FA,MA,WA,Cmax,TEC,TWC,fingerprint\n");
    for (int index = 0; index < values.size(); index++) {
      PermutationSolution<Integer> solution = values.get(index);
      long lineage = -1L;
      Object tag = solution.getAttribute(ZhangBoLineageTag.class);
      if (tag instanceof ZhangBoLineageTag) lineage = ((ZhangBoLineageTag) tag).getLineageId();
      List<Integer> machine = solution instanceof DhhfspFourVectorSolution
          ? ((DhhfspFourVectorSolution) solution).getMachineAssignments()
          : Collections.<Integer>emptyList();
      out.append(index).append(',').append(lineage).append(",\"")
          .append(join(solution.getVariables())).append("\",\"")
          .append(join(solution.getVariablesid())).append("\",\"")
          .append(join(machine)).append("\",\"")
          .append(join(solution.getVariablesworker())).append("\",")
          .append(number(solution.getObjective(0))).append(',')
          .append(number(solution.getObjective(1))).append(',')
          .append(number(solution.getObjective(6))).append(",\"")
          .append(ZhangBoQgController.fingerprint(solution).replace("\"", "\"\""))
          .append("\"\n");
    }
    Files.write(path, out.toString().getBytes(StandardCharsets.UTF_8));
  }

  private static String join(List<Integer> values) {
    StringBuilder out = new StringBuilder();
    for (int i = 0; i < values.size(); i++) {
      if (i > 0) out.append(','); out.append(values.get(i));
    }
    return out.toString();
  }

  private static String number(double value) {
    return String.format(Locale.ROOT, "%.17g", value);
  }

  private static void requireEmpty(Path directory) throws IOException {
    Files.createDirectories(directory);
    try (DirectoryStream<Path> stream = Files.newDirectoryStream(directory)) {
      if (stream.iterator().hasNext()) throw new IllegalArgumentException("Directory is not empty: " + directory);
    }
  }

  private static final class Arguments {
    private final Path projectRoot; private final Path evidenceRoot;
    private Arguments(Path projectRoot, Path evidenceRoot) {
      this.projectRoot = projectRoot; this.evidenceRoot = evidenceRoot;
    }
    static Arguments parse(String[] args) {
      Path project = null, evidence = null;
      for (int i = 0; i < args.length; i += 2) {
        if (i + 1 >= args.length) throw usage();
        if ("--project-root".equals(args[i])) project = Paths.get(args[i + 1]);
        else if ("--evidence-root".equals(args[i])) evidence = Paths.get(args[i + 1]);
        else throw usage();
      }
      if (project == null || evidence == null) throw usage();
      return new Arguments(project, evidence);
    }
    static IllegalArgumentException usage() {
      return new IllegalArgumentException("Usage: --project-root <path> --evidence-root <I1 path>");
    }
  }
}
