package org.uma.jmetal.runner.lc_psode;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.List;
import org.uma.jmetal.algorithm.multiobjective.mypso.p8.P8InitialPopulationProvider;
import org.uma.jmetal.algorithm.multiobjective.mypso.v35.V35BottleneckDiagnosisConfiguration;
import org.uma.jmetal.algorithm.multiobjective.mypso.v35.V35FairRunner;
import org.uma.jmetal.algorithm.multiobjective.mypso.v35.V35Fc6LocalCandidateAudit;
import org.uma.jmetal.algorithm.multiobjective.mypso.v35.V35LocalFeBudgetConfiguration;
import org.uma.jmetal.algorithm.multiobjective.mypso.v35.V35LocalSearchOrder;
import org.uma.jmetal.algorithm.multiobjective.mypso.v35.V35ProductionConfiguration;
import org.uma.jmetal.algorithm.multiobjective.mypso.zhangbo.PddrSelectionMode;
import org.uma.jmetal.algorithm.multiobjective.mypso.zhangbo.ZhangBoEvaluatedPddrSelector;
import org.uma.jmetal.problem.multiobjective.dfsp.decoder.ProductionDecodeMode;
import org.uma.jmetal.problem.multiobjective.dfsp.fatigue.ZhangBoCanonicalProblemLoader;
import org.uma.jmetal.problem.multiobjective.dfsp.fatigue.ZhangBoCanonicalProductionProblem;
import org.uma.jmetal.problem.multiobjective.dfsp.fatigue.shift.ZhangBoShiftConfiguration;
import org.uma.jmetal.solution.PermutationSolution;

/**
 * FC-6 single-run executor.  The command line selects a pre-registered arm,
 * never a scientific parameter.  Consequently a historical BP-PDDR run
 * cannot be mistaken for either the local-search-order or region experiment.
 */
public final class ZhangBoV35Fc6Runner {
  public static final String VERSION = "v35-fc6-a3-a4-b-v1";
  public static final int POPULATION = 100;
  public static final int MAX_FES = 500000;
  private static final long[] APPROVED_SEEDS = {20260822L, 20260823L, 20260824L};

  public enum Phase {
    ORDER_CURRENT(PddrSelectionMode.GLOBAL_ORIGINAL,
        V35LocalSearchOrder.CATA_THEN_INHERITED),
    ORDER_SWAP(PddrSelectionMode.GLOBAL_ORIGINAL,
        V35LocalSearchOrder.INHERITED_THEN_CATA),
    REGION_GLOBAL(PddrSelectionMode.GLOBAL_ORIGINAL,
        V35LocalSearchOrder.CATA_THEN_INHERITED),
    REGION_AWARE(PddrSelectionMode.REGION_AWARE,
        V35LocalSearchOrder.CATA_THEN_INHERITED),
    REGION_GLOBAL_SWAP(PddrSelectionMode.GLOBAL_ORIGINAL,
        V35LocalSearchOrder.INHERITED_THEN_CATA),
    REGION_AWARE_SWAP(PddrSelectionMode.REGION_AWARE,
        V35LocalSearchOrder.INHERITED_THEN_CATA);

    private final PddrSelectionMode pddrMode;
    private final V35LocalSearchOrder localOrder;
    Phase(PddrSelectionMode pddrMode, V35LocalSearchOrder localOrder) {
      this.pddrMode = pddrMode;
      this.localOrder = localOrder;
    }
  }

  private ZhangBoV35Fc6Runner() { }

  public static void main(String[] arguments) throws Exception {
    Arguments values = Arguments.parse(arguments);
    run(values.phase, values.instance, values.seed, values.projectRoot, values.output,
        POPULATION, MAX_FES);
  }

  /** Package-visible short-budget hook. The public CLI remains fixed at 500k FE. */
  static Path runForTest(Phase phase, String instanceName, long seed, Path projectRoot,
      Path output, int population, int maxFes) throws Exception {
    return run(phase, instanceName, seed, projectRoot, output, population, maxFes);
  }

  private static Path run(Phase phase, String instanceName, long seed, Path projectRoot,
      Path output, int population, int maxFes) throws Exception {
    requireScope(phase, instanceName, seed, population, maxFes);
    Path project = projectRoot.toAbsolutePath().normalize();
    Path javaProject = Files.isDirectory(project.resolve("EADHFSP"))
        ? project : project.resolve("java-jmetal58");
    Path instance = javaProject.resolve("EADHFSP").resolve(instanceName + ".txt");
    Path extensions = javaProject.resolve("instance-extensions/v1");
    Path fatigue = javaProject.resolve("fatigue-parameters/v1");
    requireFile(instance, "instance");
    requireFile(extensions.resolve(instanceName + ".setup.txt"), "SUT extension");
    requireFile(fatigue.resolve(instanceName + ".fatigue.txt"), "fatigue parameters");

    String runId = phase.name().toLowerCase() + "-" + instanceName + "-seed-" + seed;
    Path finalDirectory = output.toAbsolutePath().normalize().resolve("runs").resolve(runId);
    if (Files.exists(finalDirectory)) {
      throw new IllegalStateException("result exists; refusing overwrite: " + finalDirectory);
    }
    Files.createDirectories(finalDirectory.getParent());
    Path partial = finalDirectory.getParent().resolve(".partial-" + runId + "-" + System.nanoTime());
    Files.createDirectory(partial);

    V35Fc6LocalCandidateAudit.setEnabled(true);
    V35Fc6LocalCandidateAudit.reset();
    try {
      ZhangBoCanonicalProductionProblem problem = ZhangBoCanonicalProblemLoader.load(
          instance, ProductionDecodeMode.FM3, seed, extensions, fatigue,
          ZhangBoShiftConfiguration.none());
      List<PermutationSolution<Integer>> initial = new ArrayList<>();
      for (int index = 0; index < population; index++) initial.add(problem.createSolution());
      String initialHash = P8InitialPopulationProvider.sha256(initial);
      V35ProductionConfiguration configuration = configurationFor(phase, seed, population, maxFes);
      rejectHistoricalBp(configuration);
      String configurationText = configurationText(phase, runId, instanceName, seed, initialHash,
          instance, extensions.resolve(instanceName + ".setup.txt"),
          fatigue.resolve(instanceName + ".fatigue.txt"), javaProject, configuration);

      V35FairRunner.RunRecord record = V35FairRunner.run(V35FairRunner.Mode.V35_FULL_POOL_OFF,
          problem, P8InitialPopulationProvider.copy(initial), maxFes, seed, false,
          V35BottleneckDiagnosisConfiguration.fullMaskNoShadow(),
          maxFes < population * 51, configuration);
      V35FairRunner.writeRecord(record, partial, configurationText);
      Files.write(partial.resolve("initial-population.sha256"),
          (initialHash + "  initial-four-vector-population\n").getBytes(StandardCharsets.UTF_8));
      V35Fc6LocalCandidateAudit audit = V35Fc6LocalCandidateAudit.current();
      if (audit == null) throw new IllegalStateException("FC-6 local candidate audit unavailable");
      audit.writeCsv(partial.resolve("local-candidate-ledger.csv"));
      audit.writeCsv(partial.resolve("merge-pool-ledger.csv"));
      Files.write(partial.resolve("local-candidate-audit-summary.txt"),
          audit.summaryText().getBytes(StandardCharsets.UTF_8));
      audit.writeCrossRegionTeachersCsv(partial.resolve("cross-region-teachers.csv"));
      Files.write(partial.resolve("cross-region-teacher-summary.properties"),
          audit.crossRegionTeacherSummary().getBytes(StandardCharsets.UTF_8));
      Files.write(partial.resolve("selector-call-chain.txt"),
          audit.callChainText().getBytes(StandardCharsets.UTF_8));
      Files.write(partial.resolve("mechanism-summary.txt"),
          (record.getMechanismSummary() + "\nfc6LocalCandidateAuditBegin\n"
              + audit.summaryText() + "fc6LocalCandidateAuditEnd\n")
              .getBytes(StandardCharsets.UTF_8));
      Files.write(partial.resolve("run-record.csv"), runRecordCsv(runId, phase, instanceName,
          seed, initialHash, record).getBytes(StandardCharsets.UTF_8));
      String hardGate = hardGate(phase, record, audit, maxFes);
      if (hardGate != null) {
        Files.write(partial.resolve("FAILED.txt"), hardGate.getBytes(StandardCharsets.UTF_8));
        writeHashes(partial);
        Path failed = finalDirectory.getParent().resolve("failed-" + runId);
        move(partial, failed);
        throw new IllegalStateException(hardGate + "; evidence=" + failed);
      }
      writeHashes(partial);
      move(partial, finalDirectory);
      System.out.println("FC6_RUN_COMPLETED phase=" + phase + " seed=" + seed
          + " instance=" + instanceName + " FE=" + record.getFullEvaluations()
          + " output=" + finalDirectory);
      return finalDirectory;
    } catch (Exception error) {
      if (Files.exists(partial)) {
        Files.write(partial.resolve("EXCEPTION.txt"),
            (error.getClass().getName() + ": " + String.valueOf(error.getMessage()) + "\n")
                .getBytes(StandardCharsets.UTF_8));
        writeHashes(partial);
      }
      throw error;
    } finally {
      V35Fc6LocalCandidateAudit.setEnabled(false);
    }
  }

  static V35ProductionConfiguration configurationFor(Phase phase, long seed,
      int population, int maxFes) {
    if (phase == null) throw new IllegalArgumentException("phase");
    return V35ProductionConfiguration.builder()
        .seed(seed).populationSize(population).maxEvaluations(maxFes)
        .decoderMode(ProductionDecodeMode.FM3)
        .dscr(true).cfvf(true).qg(true).qp(true).caTaLite(true)
        .directionalTeacherPool(false).teacherPoolSize(10)
        .bottleneckDiagnosis(V35BottleneckDiagnosisConfiguration.fullMaskNoShadow())
        .localFeBudget(V35LocalFeBudgetConfiguration.of(0.25, 0.65))
        .pddrSelectionMode(phase.pddrMode)
        .localSearchOrder(phase.localOrder)
        .build();
  }

  private static void requireScope(Phase phase, String instance, long seed, int population,
      int maxFes) {
    if (phase == null || instance == null || population != POPULATION || maxFes < population) {
      throw new IllegalArgumentException("invalid FC-6 phase/instance/population/budget");
    }
    boolean approved = false;
    for (long value : APPROVED_SEEDS) approved |= value == seed;
    if (!approved) throw new IllegalArgumentException("FC-6 requires seed 20260822/23/24");
    if ((phase == Phase.ORDER_CURRENT || phase == Phase.ORDER_SWAP)
        && !"20_2_3_1".equals(instance)) {
      throw new IllegalArgumentException("FC-6A.4 order test requires 20_2_3_1");
    }
    if ((phase == Phase.REGION_GLOBAL || phase == Phase.REGION_AWARE
        || phase == Phase.REGION_GLOBAL_SWAP || phase == Phase.REGION_AWARE_SWAP)
        && !("20_2_3_1".equals(instance) || "100_2_3_1".equals(instance))) {
      throw new IllegalArgumentException("FC-6B requires 20_2_3_1 or 100_2_3_1");
    }
  }

  private static void rejectHistoricalBp(V35ProductionConfiguration configuration) {
    if (configuration.getPddrSelectionMode() == PddrSelectionMode.BP_RESERVED_LEGACY) {
      throw new IllegalArgumentException("BP_RESERVED_LEGACY is historical-only and forbidden in FC-6");
    }
  }

  private static String hardGate(Phase phase, V35FairRunner.RunRecord record,
      V35Fc6LocalCandidateAudit audit, int maxFes) {
    if (!"COMPLETED".equals(record.getStatus())) return "RUN_FAILED=" + record.getStopReason();
    if (record.getFullEvaluations() < POPULATION || record.getFullEvaluations() > maxFes) {
      return "FE_OUT_OF_RANGE=" + record.getFullEvaluations();
    }
    if (record.getFront().isEmpty()) return "EMPTY_FRONT";
    if (record.getDecoderTiming().getSuccessfulDecoderCalls() != record.getFullEvaluations()) {
      return "DECODER_FE_MISMATCH";
    }
    if (record.getDecoderTiming().getLeftShiftNanos() != 0L
        || record.getDecoderTiming().getRightShiftNanos() != 0L) return "SHIFT_NOT_FROZEN";
    String summary = record.getMechanismSummary();
    if (numeric(summary, "cfvfRepairs") != 0L) return "CFVF_REPAIR_NONZERO";
    if (numeric(summary, "cfvfOffspring") <= 0L || numeric(summary, "qpActions") <= 0L
        || numeric(summary, "archiveInsertions") <= 0L || numeric(summary, "qgSelections") <= 0L
        || numeric(summary, "pddrEvents") <= 0L) {
      return "FULL_CHAIN_MISSING";
    }
    // FC-6A.4 deliberately gives CA-TA-Lite and inherited LS a single hard
    // window.  In ORDER_SWAP the inherited path may legally consume that
    // window before CA-TA gets a call; forcing CA-TA>0 here would invalidate
    // the causal experiment.  The ledger records that outcome explicitly.
    if (audit.getRecordedCycleCount() <= 0) return "FC6_CANDIDATE_AUDIT_EMPTY";
    if ((phase == Phase.REGION_AWARE || phase == Phase.REGION_AWARE_SWAP)
        && !audit.hasRegionAwareEvidence()) {
      return "REGION_AWARE_EVIDENCE_MISSING";
    }
    return null;
  }

  private static long numeric(String text, String name) {
    java.util.regex.Matcher match = java.util.regex.Pattern.compile("(?:^|[,|])"
        + java.util.regex.Pattern.quote(name) + "=(-?\\d+)(?:$|[,|])")
        .matcher(text == null ? "" : text);
    return match.find() ? Long.parseLong(match.group(1)) : Long.MIN_VALUE;
  }

  private static String configurationText(Phase phase, String runId, String instanceName, long seed,
      String initialHash, Path instance, Path extension, Path fatigue, Path javaProject,
      V35ProductionConfiguration configuration) throws Exception {
    Path selectorSource = javaProject.resolve("jmetal-algorithm/src/main/java/org/uma/jmetal/algorithm/"
        + "multiobjective/mypso/zhangbo/ZhangBoEvaluatedPddrSelector.java");
    return "fc6Version=" + VERSION + "\nrunId=" + runId + "\nphase=" + phase
        + "\ninstance=" + instanceName + "\nseed=" + seed + "\npopulation=" + POPULATION
        + "\nmaxFEs=" + configuration.getMaxEvaluations() + "\nobjectiveAdapter=0,1,6\n"
        + "decoderMode=FM3\nfamilyMode=DEGENERATE_SINGLE_FAMILY\n"
        + "setupMode=SEQUENCE_INDEPENDENT\nshiftMode=NONE\n"
        + "initialPopulationHash=" + initialHash + "\n"
        + "instanceFileSha256=" + sha256(instance) + "\n"
        + "sutExtensionFileSha256=" + sha256(extension) + "\n"
        + "fatigueParameterFileSha256=" + sha256(fatigue) + "\n"
        + "pddrSelectionMode=" + configuration.getPddrSelectionMode() + "\n"
        + "localSearchOrder=" + configuration.getLocalSearchOrder() + "\n"
        + "selectorSourceSha256=" + sha256(selectorSource) + "\n"
        + "selectorBytecodeSha256=" + classResourceSha256(ZhangBoEvaluatedPddrSelector.class) + "\n"
        + "v35ConfigurationHash=" + configuration.configurationHash() + "\n"
        + "v35ConfigurationBegin\n" + configuration.canonicalText() + "v35ConfigurationEnd\n";
  }

  private static String runRecordCsv(String runId, Phase phase, String instance, long seed,
      String initialHash, V35FairRunner.RunRecord record) {
    return "runId,phase,instance,seed,status,FE,frontSize,initialPopulationHash,algorithmRunNanos\n"
        + runId + ',' + phase + ',' + instance + ',' + seed + ',' + record.getStatus() + ','
        + record.getFullEvaluations() + ',' + record.getFront().size() + ',' + initialHash + ','
        + record.getAlgorithmRunNanos() + '\n';
  }

  private static void requireFile(Path file, String label) {
    if (!Files.isRegularFile(file)) throw new IllegalArgumentException(label + " missing: " + file);
  }

  private static String sha256(Path file) throws Exception {
    byte[] hash = MessageDigest.getInstance("SHA-256").digest(Files.readAllBytes(file));
    return hex(hash);
  }

  private static String classResourceSha256(Class<?> type) throws Exception {
    String resource = '/' + type.getName().replace('.', '/') + ".class";
    InputStream stream = type.getResourceAsStream(resource);
    if (stream == null) throw new IllegalStateException("class resource missing: " + resource);
    try {
      MessageDigest digest = MessageDigest.getInstance("SHA-256");
      byte[] buffer = new byte[8192];
      for (int read; (read = stream.read(buffer)) >= 0;) digest.update(buffer, 0, read);
      return hex(digest.digest());
    } finally {
      stream.close();
    }
  }

  private static String hex(byte[] bytes) {
    StringBuilder text = new StringBuilder(bytes.length * 2);
    for (byte value : bytes) text.append(String.format("%02x", value & 0xff));
    return text.toString();
  }

  private static void writeHashes(Path directory) throws Exception {
    StringBuilder text = new StringBuilder("sha256\tpath\n");
    java.util.stream.Stream<Path> paths = Files.walk(directory);
    try {
      paths.filter(Files::isRegularFile)
          .filter(path -> !path.getFileName().toString().equals("evidence-sha256.tsv"))
          .sorted()
          .forEach(path -> {
            try {
              text.append(sha256(path)).append('\t')
                  .append(directory.relativize(path).toString().replace('\\', '/')).append('\n');
            } catch (Exception error) { throw new IllegalStateException(error); }
          });
    } finally {
      paths.close();
    }
    Files.write(directory.resolve("evidence-sha256.tsv"), text.toString().getBytes(StandardCharsets.UTF_8));
  }

  private static void move(Path from, Path to) throws Exception {
    try {
      Files.move(from, to, StandardCopyOption.ATOMIC_MOVE);
    } catch (AtomicMoveNotSupportedException ignored) {
      Files.move(from, to);
    }
  }

  private static final class Arguments {
    private Phase phase;
    private String instance;
    private long seed;
    private Path projectRoot;
    private Path output;

    private static Arguments parse(String[] arguments) {
      Arguments values = new Arguments();
      for (int index = 0; index < arguments.length; index += 2) {
        if (index + 1 >= arguments.length) throw usage();
        String key = arguments[index];
        String value = arguments[index + 1];
        if ("--phase".equals(key)) values.phase = Phase.valueOf(value);
        else if ("--instance".equals(key)) values.instance = value;
        else if ("--seed".equals(key)) values.seed = Long.parseLong(value);
        else if ("--project-root".equals(key)) values.projectRoot = Paths.get(value);
        else if ("--output".equals(key)) values.output = Paths.get(value);
        else throw usage();
      }
      if (values.phase == null || values.instance == null || values.projectRoot == null
          || values.output == null) throw usage();
      return values;
    }
    private static IllegalArgumentException usage() {
      return new IllegalArgumentException("Usage: --phase ORDER_CURRENT|ORDER_SWAP|REGION_GLOBAL|REGION_AWARE|"
          + "REGION_GLOBAL_SWAP|REGION_AWARE_SWAP "
          + "--instance 20_2_3_1|100_2_3_1 --seed 20260822|20260823|20260824 "
          + "--project-root <path> --output <path>");
    }
  }
}
