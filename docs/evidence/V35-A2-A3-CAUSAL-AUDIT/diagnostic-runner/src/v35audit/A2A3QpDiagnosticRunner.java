package v35audit;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import org.uma.jmetal.algorithm.multiobjective.mypso.ZhangBoMOHPSOQ;
import org.uma.jmetal.algorithm.multiobjective.mypso.ZhangBoMOHPSOQBuilder;
import org.uma.jmetal.algorithm.multiobjective.mypso.p8.P8InitialPopulationProvider;
import org.uma.jmetal.algorithm.multiobjective.mypso.v35.V35FairRunner;
import org.uma.jmetal.algorithm.multiobjective.mypso.v35.V35FinalAblationProfile;
import org.uma.jmetal.algorithm.multiobjective.mypso.v35.V35PassiveEvaluationArchive;
import org.uma.jmetal.algorithm.multiobjective.mypso.v35.V35ProductionConfiguration;
import org.uma.jmetal.algorithm.multiobjective.mypso.zhangbo.ZhangBoDualQCoordinator;
import org.uma.jmetal.algorithm.multiobjective.mypso.zhangbo.ZhangBoQpAction;
import org.uma.jmetal.algorithm.multiobjective.mypso.zhangbo.audit.ZhangBoCmaxAudit;
import org.uma.jmetal.problem.multiobjective.dfsp.decoder.ProductionDecodeMode;
import org.uma.jmetal.problem.multiobjective.dfsp.fatigue.ZhangBoCanonicalProblemLoader;
import org.uma.jmetal.problem.multiobjective.dfsp.fatigue.ZhangBoCanonicalProductionProblem;
import org.uma.jmetal.problem.multiobjective.dfsp.fatigue.shift.ZhangBoShiftConfiguration;
import org.uma.jmetal.solution.PermutationSolution;
import org.uma.jmetal.util.pseudorandom.JMetalRandom;

/**
 * Run-end-only A2/A3 evidence exporter.
 *
 * <p>This class deliberately lives under the evidence directory rather than
 * the repository source tree.  It constructs the already-built V35
 * algorithm using the same frozen configuration and initial-population APIs
 * as {@link V35FairRunner}; it does not add listeners, read a second random
 * stream, or change a controller.  The only new operation is serializing
 * getters after {@code algorithm.run()} returns.</p>
 */
public final class A2A3QpDiagnosticRunner {
  private static final int POPULATION = 100;
  private static final int MAX_FES = 50_000;
  private static final String INSTANCE = "100_2_3_1";
  private static final long[] ALLOWED_SEEDS = {20260822L, 20260823L, 20260824L};

  private A2A3QpDiagnosticRunner() { }

  public static void main(String[] args) throws Exception {
    Arguments value = Arguments.parse(args);
    Path completed = run(value);
    System.out.println("A2_A3_QP_DIAGNOSTIC_COMPLETED output=" + completed);
  }

  private static Path run(Arguments value) throws Exception {
    Path root = value.projectRoot.toAbsolutePath().normalize();
    Path javaProject = root.resolve("java-jmetal58");
    Path instance = javaProject.resolve("EADHFSP").resolve(value.instance + ".txt");
    Path extension = javaProject.resolve("instance-extensions/v1");
    Path fatigue = javaProject.resolve("fatigue-parameters/v1");
    requireFile(instance);
    requireFile(extension.resolve(value.instance + ".setup.txt"));
    requireFile(fatigue.resolve(value.instance + ".fatigue.txt"));
    requireFile(value.snapshot);
    if (Files.exists(value.output)) {
      throw new IllegalStateException("refusing overwrite: " + value.output);
    }
    if (value.output.getParent() == null) {
      throw new IllegalArgumentException("output must have a parent directory");
    }
    Files.createDirectories(value.output.getParent());
    Files.createDirectories(value.output);

    String oldData = System.getProperty("dhfsp.data.dir");
    String oldFatigue = System.getProperty("dhfsp.fatigue.dir");
    String oldExtension = System.getProperty("dhfsp.instance.extension.dir");
    try {
      System.setProperty("dhfsp.data.dir", javaProject.resolve("EADHFSP").toString());
      System.setProperty("dhfsp.fatigue.dir", fatigue.toString());
      System.setProperty("dhfsp.instance.extension.dir", extension.toString());

      ZhangBoCanonicalProductionProblem snapshotProblem = load(instance, extension, fatigue,
          value.seed);
      List<PermutationSolution<Integer>> initial =
          org.uma.jmetal.runner.lc_psode.ZhangBoV35FormalInitialPopulationFreezeRunner
              .readSnapshot(value.snapshot, snapshotProblem);
      if (initial.size() != POPULATION) {
        throw new IllegalStateException("initial population size=" + initial.size());
      }
      String initialHash = V35FairRunner.initialHash(initial);
      String p8InitialHash = P8InitialPopulationProvider.sha256(initial);

      ZhangBoCanonicalProductionProblem problem = load(instance, extension, fatigue, value.seed);
      V35FinalAblationProfile.Arm arm = value.arm;
      V35ProductionConfiguration configuration = V35FinalAblationProfile.configurationFor(
          arm, value.seed, POPULATION, MAX_FES);
      V35FinalAblationProfile.validate(arm, configuration);

      // Match V35FairRunner's run boundary: the singleton is reset once,
      // immediately before construction/run, and no diagnostic code consumes it.
      JMetalRandom.getInstance().setSeed(value.seed);
      ZhangBoMOHPSOQ algorithm = new ZhangBoMOHPSOQBuilder(problem, initial.size(),
          problem.getNumberOfFactories(), 0.6, 0.5, 0.5, 50)
          .setV35Configuration(configuration)
          .setMaxIterations(MAX_FES)
          .setInitialSwarmOverride(P8InitialPopulationProvider.copy(initial))
          .build();
      algorithm.setAllowTerminalPartialFormalQPhase(false);
      ZhangBoCmaxAudit cmaxAudit = new ZhangBoCmaxAudit(1000L);
      algorithm.setCmaxAudit(cmaxAudit);
      V35PassiveEvaluationArchive passiveArchive = new V35PassiveEvaluationArchive();
      algorithm.setPassiveEvaluationArchive(passiveArchive);

      long start = System.nanoTime();
      algorithm.run();
      long runNanos = System.nanoTime() - start;
      List<PermutationSolution<Integer>> result = algorithm.getResult();
      int evaluations = (int) problem.getEvaluationCounter().getSuccessfulEvaluations();
      cmaxAudit.finish(evaluations, result, result);

      writeEvidence(value, root, instance, extension, fatigue, problem, algorithm, result,
          cmaxAudit, passiveArchive, configuration, initialHash, p8InitialHash, runNanos,
          evaluations);
      return value.output;
    } catch (Exception error) {
      try {
        Files.write(value.output.resolve("failure.txt"),
            (error.getClass().getName() + ": " + error.getMessage() + "\n")
                .getBytes(StandardCharsets.UTF_8));
      } catch (IOException ignored) {
        // Preserve the original failure; the output directory is diagnostic only.
      }
      throw error;
    } finally {
      restore("dhfsp.data.dir", oldData);
      restore("dhfsp.fatigue.dir", oldFatigue);
      restore("dhfsp.instance.extension.dir", oldExtension);
    }
  }

  private static ZhangBoCanonicalProductionProblem load(Path instance, Path extension,
      Path fatigue, long seed) throws Exception {
    return ZhangBoCanonicalProblemLoader.load(instance, ProductionDecodeMode.FM3, seed,
        extension, fatigue, ZhangBoShiftConfiguration.none());
  }

  private static void writeEvidence(Arguments value, Path root, Path instance, Path extension,
      Path fatigue, ZhangBoCanonicalProductionProblem problem, ZhangBoMOHPSOQ algorithm,
      List<PermutationSolution<Integer>> result, ZhangBoCmaxAudit cmaxAudit,
      V35PassiveEvaluationArchive passiveArchive, V35ProductionConfiguration configuration,
      String initialHash, String p8InitialHash, long runNanos, int evaluations) throws IOException {
    StringBuilder status = new StringBuilder()
        .append("status=COMPLETED\n")
        .append("runnerVersion=a2-a3-run-end-diagnostic-v1\n")
        .append("arm=").append(value.arm.getLabel()).append('\n')
        .append("instance=").append(value.instance).append('\n')
        .append("seed=").append(value.seed).append('\n')
        .append("population=").append(POPULATION).append('\n')
        .append("maxFEs=").append(MAX_FES).append('\n')
        .append("fullEvaluations=").append(evaluations).append('\n')
        .append("algorithmFullEvaluationCount=").append(algorithm.getFullEvaluationCount()).append('\n')
        .append("decoderCalls=").append(problem.getDecoderTimingSnapshot().getSuccessfulDecoderCalls()).append('\n')
        .append("illegalSolutions=").append(problem.getEvaluationObservation().getIllegalSolutions()).append('\n')
        .append("duplicateEvaluations=").append(problem.getEvaluationObservation().getDuplicateEvaluations()).append('\n')
        .append("initialPopulationHash=").append(initialHash).append('\n')
        .append("initialPopulationP8Sha256=").append(p8InitialHash).append('\n')
        .append("evaluationTraceHash=").append(problem.getEvaluationObservation().getEvaluationTraceHash()).append('\n')
        .append("runtimeSubSwarmSizes=").append(algorithm.getRuntimeSubSwarmSizes()).append('\n')
        .append("algorithmRunNanos=").append(runNanos).append('\n')
        .append("configurationHash=").append(configuration.configurationHash()).append('\n');
    write(value.output.resolve("status.properties"), status.toString());

    StringBuilder provenance = new StringBuilder()
        .append("projectRoot=").append(root).append('\n')
        .append("instancePath=").append(instance).append('\n')
        .append("extensionDirectory=").append(extension).append('\n')
        .append("fatigueDirectory=").append(fatigue).append('\n')
        .append("instanceSha256=").append(problem.getInstance().getInstanceSha256()).append('\n')
        .append("instanceExtensionSha256=").append(problem.getInstance().getInstanceExtensionSha256()).append('\n')
        .append("fatigueConfigurationSha256=").append(problem.getParameters().getConfigurationSha256()).append('\n')
        .append("snapshotPath=").append(value.snapshot).append('\n')
        .append("configurationHash=").append(configuration.configurationHash()).append('\n')
        .append("configurationCanonicalBegin\n").append(configuration.canonicalText())
        .append("configurationCanonicalEnd\n")
        .append("profileCanonicalBegin\n")
        .append(V35FinalAblationProfile.canonicalTextFor(value.arm, value.seed, POPULATION, MAX_FES))
        .append("profileCanonicalEnd\n");
    write(value.output.resolve("provenance.properties"), provenance.toString());

    StringBuilder mechanism = new StringBuilder()
        .append("pddrSelectionMode=GLOBAL_ORIGINAL\n")
        .append("qpEnabled=").append(value.arm.isQpEnabled()).append('\n')
        .append("qpEventCountTotal=").append(algorithm.getQpEventCount()).append('\n')
        .append("qpEventsRetained=").append(algorithm.getQpEvents().size()).append('\n')
        .append("qpEventStreamHash=").append(algorithm.getQpEventStreamHash()).append('\n')
        .append("qpEventCapacity=4096\n")
        .append("qpFullCapture=false\n")
        .append("qpExecutedActions=").append(algorithm.getQpExecutedActionCount()).append('\n')
        .append("qpTrainedTransitions=").append(algorithm.getQpTrainedTransitionCount()).append('\n')
        .append("qpPbestSwitches=").append(algorithm.getQpPbestSwitches()).append('\n')
        .append("qpTableHash=").append(algorithm.getQpTableHash()).append('\n')
        .append("qgSelections=").append(algorithm.getQgSelectionCount()).append('\n')
        .append("qgTdUpdates=").append(algorithm.getQgTdUpdateCount()).append('\n')
        .append("qgEventCountTotal=").append(algorithm.getQgEventCount()).append('\n')
        .append("qgEventsRetained=").append(algorithm.getQgEvents().size()).append('\n')
        .append("qgEventStreamHash=").append(algorithm.getQgEventStreamHash()).append('\n')
        .append("pddrEventCountTotal=").append(algorithm.getZhangBoPddrEventCount()).append('\n')
        .append("pddrEventsRetained=").append(algorithm.getZhangBoPddrEvents().size()).append('\n')
        .append("pddrEventStreamHash=").append(algorithm.getZhangBoPddrEventStreamHash()).append('\n')
        .append("lineageEventCountTotal=").append(algorithm.getZhangBoLineageEventCount()).append('\n')
        .append("lineageEventsRetained=").append(algorithm.getZhangBoLineageEvents().size()).append('\n')
        .append("lineageEventStreamHash=").append(algorithm.getZhangBoLineageEventStreamHash()).append('\n')
        .append("lineageMemoryCount=").append(algorithm.getZhangBoLineageMemories().size()).append('\n')
        .append("lineageSplits=").append(algorithm.getZhangBoLineageSplitCount()).append('\n')
        .append("lineageDeletions=").append(algorithm.getZhangBoLineageDeletionCount()).append('\n')
        .append("lineageMigrations=").append(algorithm.getZhangBoLineageMigrationCount()).append('\n')
        .append("lineageInsertions=").append(algorithm.getZhangBoArchiveInsertionCount()).append('\n')
        .append("lineageDominatedRemovals=").append(algorithm.getZhangBoArchiveDominatedRemovalCount()).append('\n')
        .append("lineageDuplicateRemovals=").append(algorithm.getZhangBoArchiveDuplicateRemovalCount()).append('\n')
        .append("lineageTruncations=").append(algorithm.getZhangBoArchiveTruncationCount()).append('\n')
        .append("dualQWarmup=").append(algorithm.getDualQPhaseCount(ZhangBoDualQCoordinator.Phase.WARMUP)).append('\n')
        .append("dualQP=").append(algorithm.getDualQPhaseCount(ZhangBoDualQCoordinator.Phase.P_BLOCK)).append('\n')
        .append("dualQG=").append(algorithm.getDualQPhaseCount(ZhangBoDualQCoordinator.Phase.G_BLOCK)).append('\n')
        .append("archiveInsertions=").append(algorithm.getZhangBoArchiveInsertionCount()).append('\n')
        .append("formalOuterCycles=").append(algorithm.getFormalBaselineOuterCycles()).append('\n')
        .append("formalQgRounds=").append(algorithm.getFormalBaselineQgRounds()).append('\n')
        .append("formalLocalFE=").append(algorithm.getFormalCriticalFactorySwapEvaluations()
            + algorithm.getFormalCriticalFactoryInsertEvaluations()
            + algorithm.getFormalOriginalNeighborhoodEvaluations()).append('\n')
        .append("fixedNeighborhoodEvents=").append(algorithm.getFixedNeighborhoodEventCount()).append('\n')
        .append("cfvfOffspring=").append(algorithm.getCfvfOffspringCount()).append('\n')
        .append("cfvfRepairs=").append(algorithm.getCfvfRepairCount()).append('\n')
        .append("p6EventCountTotal=").append(algorithm.getZhangBoP6EventCount()).append('\n')
        .append("p6EventsRetained=").append(algorithm.getZhangBoP6Events().size()).append('\n')
        .append("p6EventStreamHash=").append(algorithm.getZhangBoP6EventStreamHash()).append('\n');
    for (ZhangBoQpAction action : ZhangBoQpAction.values()) {
      mechanism.append("qpActionCount.").append(action.name()).append('=')
          .append(algorithm.getQpActionCount(action)).append('\n')
          .append("qpAverageReward.").append(action.name()).append('=')
          .append(String.format(Locale.ROOT, "%.17g", algorithm.getQpAverageReward(action)))
          .append('\n');
    }
    write(value.output.resolve("mechanism-summary.properties"), mechanism.toString());

    writeLines(value.output.resolve("qp-events-retained.log"), algorithm.getQpEvents());
    writeLines(value.output.resolve("lineage-events-retained.log"), algorithm.getZhangBoLineageEvents());
    writeLines(value.output.resolve("qg-events-retained.log"), algorithm.getQgEvents());
    writeLines(value.output.resolve("pddr-events-retained.log"), algorithm.getZhangBoPddrEvents());
    write(value.output.resolve("qp-canonical.txt"), algorithm.getQpCanonicalText());
    write(value.output.resolve("lineage-canonical.txt"), algorithm.getZhangBoLineageCanonicalText());
    write(value.output.resolve("qg-canonical.txt"), algorithm.getQgCanonicalText());

    StringBuilder front = new StringBuilder("Cmax,TEC,TWC\n");
    for (double[] values : front(result)) {
      front.append(values[0]).append(',').append(values[1]).append(',').append(values[2]).append('\n');
    }
    write(value.output.resolve("front.csv"), front.toString());
    write(value.output.resolve("cmax-audit-curves.csv"), cmaxAudit.curvesCsv());
    write(value.output.resolve("cmax-audit-records.csv"), cmaxAudit.recordsCsv());
    write(value.output.resolve("cmax-audit-summary.txt"), cmaxAudit.summaryText());
    write(value.output.resolve("passive-archive.csv"), passiveArchive.toCsv());
    write(value.output.resolve("passive-summary.properties"),
        "observedCount=" + passiveArchive.getObservedCount() + "\n"
            + "archiveSize=" + passiveArchive.size() + "\n"
            + "retentionRate=" + (passiveArchive.getObservedCount() == 0L ? Double.NaN
                : (double) passiveArchive.size() / passiveArchive.getObservedCount()) + "\n");
    write(value.output.resolve("run-scope.txt"),
        "remoteRun=false\nsourceTreeModified=false\npostRunOnlyExport=true\n"
            + "randomStreamResetOnceAtRunBoundary=true\nfullCapture=false\n"
            + "eventCapacity=4096\nalgorithmDecisionsUnchanged=true\n");
  }

  private static List<double[]> front(List<PermutationSolution<Integer>> result) {
    List<double[]> values = new ArrayList<>();
    for (PermutationSolution<Integer> solution : result) {
      values.add(new double[]{solution.getObjective(0), solution.getObjective(1), solution.getObjective(6)});
    }
    return values;
  }

  private static void write(Path path, String text) throws IOException {
    Files.write(path, (text == null ? "" : text).getBytes(StandardCharsets.UTF_8));
  }

  private static void writeLines(Path path, List<String> lines) throws IOException {
    StringBuilder out = new StringBuilder();
    for (String line : lines) out.append(line).append('\n');
    write(path, out.toString());
  }

  private static void requireFile(Path path) {
    if (!Files.isRegularFile(path)) throw new IllegalArgumentException("missing file: " + path);
  }

  private static void restore(String key, String value) {
    if (value == null) System.clearProperty(key); else System.setProperty(key, value);
  }

  private static final class Arguments {
    private final Path projectRoot;
    private final String instance;
    private final long seed;
    private final V35FinalAblationProfile.Arm arm;
    private final Path snapshot;
    private final Path output;

    private Arguments(Path projectRoot, String instance, long seed,
        V35FinalAblationProfile.Arm arm, Path snapshot, Path output) {
      this.projectRoot = projectRoot;
      this.instance = instance;
      this.seed = seed;
      this.arm = arm;
      this.snapshot = snapshot;
      this.output = output;
    }

    private static Arguments parse(String[] args) {
      String project = null;
      String instance = INSTANCE;
      String armText = null;
      String snapshotText = null;
      String outputText = null;
      Long seed = null;
      for (int i = 0; i < args.length; i++) {
        String key = args[i];
        if (i + 1 >= args.length) throw new IllegalArgumentException("missing value for " + key);
        String value = args[++i];
        switch (key) {
          case "--project-root": project = value; break;
          case "--instance": instance = value; break;
          case "--arm": armText = value; break;
          case "--seed": seed = Long.valueOf(value); break;
          case "--snapshot": snapshotText = value; break;
          case "--output": outputText = value; break;
          default: throw new IllegalArgumentException("unknown option: " + key);
        }
      }
      if (project == null || armText == null || seed == null || snapshotText == null
          || outputText == null) {
        throw new IllegalArgumentException("required: --project-root --arm --seed --snapshot --output");
      }
      if (!INSTANCE.equals(instance)) throw new IllegalArgumentException("instance is fixed to " + INSTANCE);
      if (seed < ALLOWED_SEEDS[0] || seed > ALLOWED_SEEDS[ALLOWED_SEEDS.length - 1]) {
        throw new IllegalArgumentException("seed is fixed to 20260822..20260824");
      }
      boolean allowed = false;
      for (long item : ALLOWED_SEEDS) allowed |= item == seed;
      if (!allowed) throw new IllegalArgumentException("seed is not in the approved three-seed set");
      V35FinalAblationProfile.Arm arm;
      if ("A2".equals(armText)) arm = V35FinalAblationProfile.Arm.A2_CFVF;
      else if ("A3".equals(armText)) arm = V35FinalAblationProfile.Arm.A3_QP_PERSONAL_ARCHIVE;
      else throw new IllegalArgumentException("arm is fixed to A2 or A3");
      Path projectRoot = Paths.get(project).toAbsolutePath().normalize();
      Path snapshot = Paths.get(snapshotText);
      if (!snapshot.isAbsolute()) snapshot = projectRoot.resolve(snapshot);
      Path output = Paths.get(outputText);
      if (!output.isAbsolute()) output = projectRoot.resolve(output);
      return new Arguments(projectRoot, instance, seed, arm, snapshot.normalize(), output.normalize());
    }
  }
}
