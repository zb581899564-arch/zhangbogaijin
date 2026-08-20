package org.uma.jmetal.runner.lc_psode;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import org.uma.jmetal.algorithm.multiobjective.mypso.ZhangBoMOHPSOQ;
import org.uma.jmetal.algorithm.multiobjective.mypso.ZhangBoMOHPSOQBuilder;
import org.uma.jmetal.algorithm.multiobjective.mypso.p8.P8InitialPopulationProvider;
import org.uma.jmetal.algorithm.multiobjective.mypso.v35.V35BottleneckDiagnosisConfiguration;
import org.uma.jmetal.algorithm.multiobjective.mypso.v35.V35CaTaLiteConfiguration;
import org.uma.jmetal.algorithm.multiobjective.mypso.v35.V35ProductionConfiguration;
import org.uma.jmetal.algorithm.multiobjective.mypso.zhangbo.ZhangBoFormalHmopsoQgsConfiguration;
import org.uma.jmetal.problem.multiobjective.dfsp.decoder.ProductionDecodeMode;
import org.uma.jmetal.problem.multiobjective.dfsp.fatigue.ZhangBoCanonicalProblemLoader;
import org.uma.jmetal.problem.multiobjective.dfsp.fatigue.ZhangBoCanonicalProductionProblem;
import org.uma.jmetal.problem.multiobjective.dfsp.fatigue.shift.ZhangBoShiftConfiguration;
import org.uma.jmetal.solution.PermutationSolution;


/**
 * Diagnostic-only budget-pacing single-variable experiment for V35 A4.
 *
 * <p>Runs the full A4 chain (DSCR+CFVF+Qp+Qg+CA-TA-Lite, directional pool
 * off) with a custom inherited-local-search budget: {@code localSearchTimes}
 * is the only variable changed against the Table-9 default of 30. Total FE,
 * seed, initial population and all other mechanisms stay fixed. The run is
 * observational: its fronts are NOT part of any formal reference and the
 * results are marked {@code diagnostic_budget_pacing_only=true}.</p>
 */
public final class ZhangBoV35P25EBudgetDiagnosticRunner {
  public static final long DEFAULT_SEED = 20260822L;
  public static final int DEFAULT_MAX_FES = 50000;

  private ZhangBoV35P25EBudgetDiagnosticRunner() { }

  public static void main(String[] args) throws Exception {
    Arguments value = Arguments.parse(args);
    Path project = value.projectRoot.toAbsolutePath().normalize();
    Path javaProject = Files.isDirectory(project.resolve("EADHFSP"))
        ? project : project.resolve("java-jmetal58");
    boolean pilot = !Files.isRegularFile(
        javaProject.resolve("EADHFSP/" + value.instance + ".txt"));
    Path base = pilot ? javaProject.resolve("EADHFSP-pilot") : javaProject;
    Path instance = pilot
        ? base.resolve("EADHFSP/" + value.instance + ".txt")
        : base.resolve("EADHFSP/" + value.instance + ".txt");
    Path extension = base.resolve("instance-extensions/v1");
    Path fatigue = base.resolve("fatigue-parameters/v1");
    if (!Files.isRegularFile(instance) || !Files.isRegularFile(
        extension.resolve(value.instance + ".setup.txt"))
        || !Files.isRegularFile(fatigue.resolve(value.instance + ".fatigue.txt"))) {
      throw new IllegalArgumentException("missing instance/extension/fatigue files for "
          + value.instance + " (pilot=" + pilot + ")");
    }
    Path target = value.output.toAbsolutePath().normalize();
    Files.createDirectories(target);

    ZhangBoCanonicalProductionProblem problem = ZhangBoCanonicalProblemLoader.load(
        instance, ProductionDecodeMode.FM3, value.seed, extension, fatigue,
        ZhangBoShiftConfiguration.none());
    List<PermutationSolution<Integer>> initial = new ArrayList<>();
    for (int index = 0; index < 100; index++) initial.add(problem.createSolution());
    String initialHash = P8InitialPopulationProvider.sha256(initial);

    org.uma.jmetal.algorithm.multiobjective.mypso.zhangbo.ZhangBoDualQCoordinationConfiguration
        dualQ = value.softFreezeRho > 0.0
        ? org.uma.jmetal.algorithm.multiobjective.mypso.zhangbo.ZhangBoDualQCoordinationConfiguration
            .blockFrozenSoftFreeze(0.10, 5, value.gBlockLength, value.softFreezeRho)
        : org.uma.jmetal.algorithm.multiobjective.mypso.zhangbo.ZhangBoDualQCoordinationConfiguration
            .blockFrozen(0.10, 5, value.gBlockLength);
    V35ProductionConfiguration.Builder builder = V35ProductionConfiguration.builder()
        .seed(value.seed).populationSize(100).maxEvaluations(value.maxFEs)
        .decoderMode(ProductionDecodeMode.FM3)
        .dscr(true).cfvf(true).qg(true).qp(true).caTaLite(true)
        .bottleneckDiagnosis(V35BottleneckDiagnosisConfiguration.fullMaskNoShadow())
        .directionalTeacherPool(false).teacherPoolSize(10)
        .dualQCoordination(dualQ);
    if (value.localFeBudget != null) {
      builder.localFeBudget(value.localFeBudget);
    }
    if (value.cheapTest) {
      builder.caTaLiteConfiguration(V35CaTaLiteConfiguration.cheapTest());
    }
    V35ProductionConfiguration config = builder.build();
    // Table-9 baseline with diagnostic overrides: local-search-times and the
    // G-block length are the only variables changed against formal A4.
    ZhangBoFormalHmopsoQgsConfiguration formal = ZhangBoFormalHmopsoQgsConfiguration.of(
        0.6, 0.2, 0.5, 0.5, 0.08, 0.15, 0.25, 50, value.localSearchTimes, 0.8, 0.8);
    ZhangBoMOHPSOQ algorithm = new ZhangBoMOHPSOQBuilder(problem, 100,
        problem.getNumberOfFactories(), 0.6, 0.5, 0.5, 50)
        .setV35Configuration(config)
        .setFormalBaselineConfiguration(formal)
        .setMaxIterations(value.maxFEs)
        .setInitialSwarmOverride(P8InitialPopulationProvider.copy(initial))
        .build();
    algorithm.setAllowTerminalPartialFormalQPhase(false);

    // FC-TIME-1: pure-observation module profiling; off by default.
    org.uma.jmetal.algorithm.multiobjective.mypso.v35.V35ModuleTimer
        .setEnabled(value.profileModules);
    org.uma.jmetal.algorithm.multiobjective.mypso.v35.V35ModuleTimer.reset();
    // FC-5.1: global best fully-evaluated Cmax observer (pure observation).
    org.uma.jmetal.problem.multiobjective.dfsp.fatigue.V35CmaxBestEver.setEnabled(true);
    org.uma.jmetal.problem.multiobjective.dfsp.fatigue.V35CmaxBestEver.reset();
    // FC-5.2: evaluated-candidate lifecycle audit (pure observation).
    org.uma.jmetal.algorithm.multiobjective.mypso.v35.V35EvaluationSourceContext.setEnabled(true);
    org.uma.jmetal.algorithm.multiobjective.mypso.v35.V35Fc52LifecycleAudit.setEnabled(true);
    org.uma.jmetal.algorithm.multiobjective.mypso.v35.V35Fc52LifecycleAudit.reset();
    // FC-6A-POST / Build-C2: BP-PDDR 稳定性诊断审计（纯观察；唯一新增数据源）。
    org.uma.jmetal.algorithm.multiobjective.mypso.v35.V35Fc6BpPddrDiagnosticAudit.setEnabled(true);
    org.uma.jmetal.algorithm.multiobjective.mypso.v35.V35Fc6BpPddrDiagnosticAudit.setSeed(value.seed);
    // FC-6A.2: 174.44 反事实探针（20-job seed22；FC-5.2 record 655 三元组）。
    if ("20_2_3_1".equals(value.instance) && value.seed == 20260822L) {
      org.uma.jmetal.algorithm.multiobjective.mypso.v35.V35Fc6BpPddrDiagnosticAudit
          .setRegionProbe(174.43665028596877, 11123.472680537456, 15044.462631959621);
    }
    long start = System.nanoTime();
    algorithm.run();
    long runNanos = System.nanoTime() - start;
    int evaluations = (int) problem.getEvaluationCounter().getSuccessfulEvaluations();

    List<double[]> front = new ArrayList<>();
    for (PermutationSolution<Integer> solution : algorithm.getResult()) {
      front.add(new double[]{solution.getObjective(0), solution.getObjective(1),
          solution.getObjective(6)});
    }
    front = org.uma.jmetal.algorithm.multiobjective.mypso.p8.P8MetricCalculator.nondominated(front);
    org.uma.jmetal.algorithm.multiobjective.mypso.v35.V35Fc52LifecycleAudit fc52 =
        org.uma.jmetal.algorithm.multiobjective.mypso.v35.V35Fc52LifecycleAudit.current();
    if (fc52 != null) {
      fc52.finish(algorithm.getResult(), front, evaluations);
    }
    org.uma.jmetal.algorithm.multiobjective.mypso.v35.V35Fc6BpPddrDiagnosticAudit fc6Diag =
        org.uma.jmetal.algorithm.multiobjective.mypso.v35.V35Fc6BpPddrDiagnosticAudit.current();
    if (fc6Diag != null) {
      fc6Diag.finish(front);
    }
    long formalLocalFE = algorithm.getFormalCriticalFactorySwapEvaluations()
        + algorithm.getFormalCriticalFactoryInsertEvaluations()
        + algorithm.getFormalOriginalNeighborhoodEvaluations();

    StringBuilder summary = new StringBuilder();
    summary.append("diagnosticBudgetPacingOnly=true\n")
        .append("algorithm=ZHANGBO_A4\n")
        .append("seed=").append(value.seed).append('\n')
        .append("instance=").append(value.instance).append("\npopulation=100\nmaxFEs=")
        .append(value.maxFEs).append('\n')
        .append("fullEvaluations=").append(evaluations).append('\n')
        .append("initialPopulationHash=").append(initialHash).append('\n')
        .append("localSearchTimes=").append(value.localSearchTimes)
        .append(" (Table9 default=30)\n")
        .append("gBlockLength=").append(value.gBlockLength)
        .append(" (default=5)\n")
        .append("formalBaselineSha256=").append(formal.sha256()).append('\n')
        .append("formalOuterCycles=").append(algorithm.getFormalBaselineOuterCycles()).append('\n')
        .append("formalQgRounds=").append(algorithm.getFormalBaselineQgRounds()).append('\n')
        .append("dualQWarmup=").append(algorithm.getDualQPhaseCount(
            org.uma.jmetal.algorithm.multiobjective.mypso.zhangbo.ZhangBoDualQCoordinator.Phase.WARMUP)).append('\n')
        .append("dualQP=").append(algorithm.getDualQPhaseCount(
            org.uma.jmetal.algorithm.multiobjective.mypso.zhangbo.ZhangBoDualQCoordinator.Phase.P_BLOCK)).append('\n')
        .append("dualQG=").append(algorithm.getDualQPhaseCount(
            org.uma.jmetal.algorithm.multiobjective.mypso.zhangbo.ZhangBoDualQCoordinator.Phase.G_BLOCK)).append('\n')
        .append("formalOuterCycles=").append(algorithm.getFormalBaselineOuterCycles()).append('\n')
        .append("formalQgRounds=").append(algorithm.getFormalBaselineQgRounds()).append('\n')
        .append("pddrEvents=").append(algorithm.getZhangBoPddrEventCount()).append('\n')
        .append("qgSelections=").append(algorithm.getQgSelectionCount()).append('\n')
        .append("qgTdUpdates=").append(algorithm.getQgTdUpdateCount()).append('\n')
        .append("qpActions=").append(algorithm.getQpExecutedActionCount()).append('\n')
        .append("qpTransitions=").append(algorithm.getQpTrainedTransitionCount()).append('\n')
        .append("cfvfOffspring=").append(algorithm.getCfvfOffspringCount()).append('\n')
        .append("cfvfRepairs=").append(algorithm.getCfvfRepairCount()).append('\n')
        .append("archiveInsertions=").append(algorithm.getZhangBoArchiveInsertionCount()).append('\n')
        .append("caTaLiteTest=").append(algorithm.getCaTaTestCalls()).append('\n')
        .append("caTaLiteApply=").append(algorithm.getCaTaApplyCalls()).append('\n')
        .append("caTaLiteFE=").append(algorithm.getCaTaTestCalls() + algorithm.getCaTaApplyCalls()).append('\n')
        .append("cfvfGirSummaryBegin\n")
        .append(algorithm.v35CfvfGirAuditSummary())
        .append("cfvfGirSummaryEnd\n")
        .append("formalLocalFE=").append(formalLocalFE).append('\n')
        .append("formalLocalFraction=").append(evaluations == 0 ? 0.0
            : (double) formalLocalFE / evaluations).append('\n')
        .append("fm3StructurePreviews=").append(algorithm.getV35Fm3StructurePreviews()).append('\n')
        .append("proxyStructurePreviews=").append(algorithm.getV35ProxyStructurePreviews()).append('\n')
        .append("frontSize=").append(front.size()).append('\n')
        .append("runNanos=").append(runNanos).append('\n')
        .append("cmaxLifecycleAudit\n")
        .append(algorithm.v35CmaxLifecycleAuditSummary())
        .append("cmaxLifecycleAuditEnd\n")
        .append(algorithm.getV35CmaxLifecycleAudit().fc51SummaryText());
    if (fc52 != null) {
      summary.append(fc52.fc52SummaryText());
    }
    if (fc6Diag != null) {
      summary.append(fc6Diag.fc6DiagText());
    }
    if (value.profileModules) {
      summary.append("moduleTimingBegin\n")
          .append(org.uma.jmetal.algorithm.multiobjective.mypso.v35.V35ModuleTimer.summaryText())
          .append("moduleTimingEnd\n")
          .append("perCycleTimingBegin\n");
      for (String line : algorithm.v35ModulePerCycleLines()) {
        summary.append(line).append('\n');
      }
      summary.append("perCycleTimingEnd\n");
    }
    Files.write(target.resolve("mechanism-summary.txt"),
        summary.toString().getBytes(StandardCharsets.UTF_8));
    StringBuilder out = new StringBuilder("Cmax,TEC,TWC\n");
    for (double[] point : front) out.append(point[0]).append(',').append(point[1])
        .append(',').append(point[2]).append('\n');
    Files.write(target.resolve("front.csv"), out.toString().getBytes(StandardCharsets.UTF_8));
    System.out.print(summary);
    System.out.println("BUDGET_DIAGNOSTIC_COMPLETED ls=" + value.localSearchTimes
        + " gBlock=" + value.gBlockLength + " FE=" + evaluations + " front=" + front.size());
  }

  private static final class Arguments {
    private Path projectRoot;
    private Path output;
    private int localSearchTimes;
    private int gBlockLength = 5;
    private String instance = "20_2_3_1";
    private long seed = DEFAULT_SEED;
    private int maxFEs = DEFAULT_MAX_FES;
    private org.uma.jmetal.algorithm.multiobjective.mypso.v35.V35LocalFeBudgetConfiguration
        localFeBudget;
    private double softFreezeRho;
    private boolean cheapTest;
    private boolean profileModules;

    private static Arguments parse(String[] args) {
      Arguments value = new Arguments();
      for (int index = 0; index < args.length; index += 2) {
        if (index + 1 >= args.length) throw usage();
        if ("--project-root".equals(args[index])) value.projectRoot = Paths.get(args[index + 1]);
        else if ("--output".equals(args[index])) value.output = Paths.get(args[index + 1]);
        else if ("--local-search-times".equals(args[index])) {
          value.localSearchTimes = Integer.parseInt(args[index + 1]);
        } else if ("--g-block-length".equals(args[index])) {
          value.gBlockLength = Integer.parseInt(args[index + 1]);
        } else if ("--instance".equals(args[index])) {
          value.instance = args[index + 1];
        } else if ("--seed".equals(args[index])) value.seed = Long.parseLong(args[index + 1]);
        else if ("--max-fes".equals(args[index])) value.maxFEs = Integer.parseInt(args[index + 1]);
        else if ("--local-fe-budget".equals(args[index])) {
          String[] parts = args[index + 1].split(":");
          value.localFeBudget = org.uma.jmetal.algorithm.multiobjective.mypso.v35
              .V35LocalFeBudgetConfiguration.of(
                  Double.parseDouble(parts[0]), Double.parseDouble(parts[1]));
        } else if ("--soft-freeze-rho".equals(args[index])) {
          value.softFreezeRho = Double.parseDouble(args[index + 1]);
        } else if ("--cheap-test".equals(args[index])) {
          value.cheapTest = Boolean.parseBoolean(args[index + 1]);
        } else if ("--profile-modules".equals(args[index])) {
          value.profileModules = Boolean.parseBoolean(args[index + 1]);
        } else throw usage();
      }
      if (value.projectRoot == null || value.output == null || value.localSearchTimes <= 0
          || value.gBlockLength <= 0) {
        throw usage();
      }
      return value;
    }

    private static IllegalArgumentException usage() {
      return new IllegalArgumentException("Usage: --project-root <path> --output <path> "
          + "--local-search-times <int> [--g-block-length <int>] [--instance <name>] "
          + "[--seed <long>] [--max-fes <int>] "
          + "[--local-fe-budget <betaMin:betaMax>] [--soft-freeze-rho <double>] "
          + "[--cheap-test <bool>] [--profile-modules <bool>]");
    }
  }
}
