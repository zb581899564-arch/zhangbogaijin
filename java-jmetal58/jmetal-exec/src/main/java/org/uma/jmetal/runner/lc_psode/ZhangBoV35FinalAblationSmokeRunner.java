package org.uma.jmetal.runner.lc_psode;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;
import org.uma.jmetal.algorithm.multiobjective.mypso.p8.P8InitialPopulationProvider;
import org.uma.jmetal.algorithm.multiobjective.mypso.v35.V35BottleneckDiagnosisConfiguration;
import org.uma.jmetal.algorithm.multiobjective.mypso.v35.V35FairRunner;
import org.uma.jmetal.algorithm.multiobjective.mypso.v35.V35FinalAblationProfile;
import org.uma.jmetal.algorithm.multiobjective.mypso.v35.V35ProductionConfiguration;
import org.uma.jmetal.problem.multiobjective.dfsp.decoder.ProductionDecodeMode;
import org.uma.jmetal.problem.multiobjective.dfsp.fatigue.ZhangBoCanonicalProblemLoader;
import org.uma.jmetal.problem.multiobjective.dfsp.fatigue.ZhangBoCanonicalProductionProblem;
import org.uma.jmetal.problem.multiobjective.dfsp.fatigue.shift.ZhangBoShiftConfiguration;
import org.uma.jmetal.solution.PermutationSolution;

/**
 * Fail-closed 2,000-FE semantic smoke for the approved A0--A4 final ladder.
 * It deliberately has no switch for formal budgets and refuses any budget other
 * than 2,000 FE, so it cannot become a back door to a 500k campaign.
 */
public final class ZhangBoV35FinalAblationSmokeRunner {
  public static final String VERSION = "v35-final-a0-a4-2000fe-smoke-v1";
  public static final int POPULATION = 10;
  public static final int MAX_FES = 2000;
  public static final long SEED = 20260822L;

  private ZhangBoV35FinalAblationSmokeRunner() { }

  public static void main(String[] args) throws Exception {
    Arguments value = Arguments.parse(args);
    Path completed = run(value.projectRoot, value.output, POPULATION, MAX_FES, SEED);
    System.out.println("V35_FINAL_ABLATION_SMOKE_COMPLETED output=" + completed);
  }

  static Path runForTest(Path projectRoot, Path output, int population, int maxFes, long seed)
      throws Exception {
    return run(projectRoot, output, population, maxFes, seed);
  }

  @SuppressWarnings({"rawtypes", "unchecked"})
  private static Path run(Path projectRoot, Path output, int population, int maxFes, long seed)
      throws Exception {
    if (population != POPULATION || maxFes != MAX_FES) {
      throw new IllegalArgumentException("this runner is fixed to population=" + POPULATION
          + " and maxFes=" + MAX_FES + "; formal runs are intentionally refused");
    }
    Path root = projectRoot.toAbsolutePath().normalize();
    Path javaProject = Files.isDirectory(root.resolve("EADHFSP")) ? root : root.resolve("java-jmetal58");
    Path instance = javaProject.resolve("EADHFSP/20_2_3_1.txt");
    Path extension = javaProject.resolve("instance-extensions/v1");
    Path fatigue = javaProject.resolve("fatigue-parameters/v1");
    requireFile(instance); requireFile(extension.resolve("20_2_3_1.setup.txt"));
    requireFile(fatigue.resolve("20_2_3_1.fatigue.txt"));

    Path completed = output.toAbsolutePath().normalize();
    if (Files.exists(completed)) throw new IllegalStateException("refusing overwrite: " + completed);
    Files.createDirectories(completed.getParent());
    Path partial = completed.resolveSibling(".partial-" + completed.getFileName() + "-"
        + System.nanoTime());
    Files.createDirectories(partial);

    String oldData = System.getProperty("dhfsp.data.dir");
    String oldFatigue = System.getProperty("dhfsp.fatigue.dir");
    String oldExtension = System.getProperty("dhfsp.instance.extension.dir");
    try {
      System.setProperty("dhfsp.data.dir", javaProject.resolve("EADHFSP").toString());
      System.setProperty("dhfsp.fatigue.dir", fatigue.toString());
      System.setProperty("dhfsp.instance.extension.dir", extension.toString());

      ZhangBoCanonicalProductionProblem seedProblem = load(instance, extension, fatigue, seed);
      List<PermutationSolution<Integer>> initial = new ArrayList<>();
      for (int index = 0; index < population; index++) initial.add(seedProblem.createSolution());
      // FairRunner owns the formal four-vector fingerprint.  Use that exact
      // representation for both the generated manifest and the run records.
      String initialHash = V35FairRunner.initialHash(initial);
      Files.write(partial.resolve("initial-population.sha256"),
          (initialHash + "  common-initial-four-vector-population\n").getBytes(StandardCharsets.UTF_8));

      StringBuilder csv = new StringBuilder("arm,status,FE,decoderCalls,illegalSolutions,"
          + "duplicateEvaluations,passiveObserved,qgSelections,pddrEvents,baselineUpdateEvents,"
          + "fixedNeighborhoodEvents,formalLocalFE,dscrTeacherUses,dscrValidityChecks,"
          + "dscrReplacements,dscrDominatedTeacherUses,cfvfOffspring,cfvfRepairs,qpActions,"
          + "qpTransitions,archiveInsertions,dualQP,dualQG,caTaLiteTest,caTaLiteApply,"
          + "directionalPoolRequests,directionalPoolFiltered,frontSize,initialPopulationHash,"
          + "profileSha256\n");
      StringBuilder checks = new StringBuilder("# A0--A4 2000 FE semantic smoke checks\n\n")
          .append("- version: `").append(VERSION).append("`\n")
          .append("- instance: `20_2_3_1`; seed: `").append(seed).append("`; population: `")
          .append(population).append("`; maxFEs: `").append(maxFes).append("`\n")
          .append("- common initial population SHA-256: `").append(initialHash).append("`\n\n")
          .append("| Arm | Result | Notes |\n|---|---|---|\n");

      for (V35FinalAblationProfile.Arm arm : V35FinalAblationProfile.ARMS) {
        V35ProductionConfiguration configuration = V35FinalAblationProfile.configurationFor(
            arm, seed, population, maxFes);
        V35FinalAblationProfile.validate(arm, configuration);
        String profileText = V35FinalAblationProfile.canonicalTextFor(arm, seed, population, maxFes);
        String profileHash = V35FinalAblationProfile.configurationHashFor(arm, seed, population, maxFes);
        ZhangBoCanonicalProductionProblem problem = load(instance, extension, fatigue, seed);
        V35FairRunner.RunRecord record = V35FairRunner.run(arm.getMode(), problem,
            P8InitialPopulationProvider.copy(initial), maxFes, seed, true,
            V35BottleneckDiagnosisConfiguration.fullMaskNoShadow(), false, configuration);
        verify(arm, record, initialHash, configuration, maxFes);
        Path armOutput = partial.resolve("arms").resolve(arm.getLabel());
        String evidenceConfiguration = "smokeVersion=" + VERSION + "\n"
            + "arm=" + arm.getLabel() + "\ninstance=20_2_3_1\nseed=" + seed + "\n"
            + "population=" + population + "\nmaxFEs=" + maxFes + "\n"
            + "initialPopulationHash=" + initialHash + "\nprofileSha256=" + profileHash + "\n"
            + "profileCanonicalBegin\n" + profileText + "profileCanonicalEnd\n";
        V35FairRunner.writeRecord(record, armOutput, evidenceConfiguration);
        Files.write(armOutput.resolve("profile.sha256"),
            (profileHash + "  canonical-profile\n").getBytes(StandardCharsets.UTF_8));
        csv.append(csvRow(arm, record, initialHash, profileHash));
        checks.append("| ").append(arm.getLabel()).append(" | PASS | ")
            .append(checkNote(arm, record)).append(" |\n");
      }
      Files.write(partial.resolve("smoke-summary.csv"), csv.toString().getBytes(StandardCharsets.UTF_8));
      checks.append("\nAll assertions are runtime gates.  PASS does not authorize any formal run or"
          + " claim an empirical performance effect.\n");
      Files.write(partial.resolve("SMOKE_CHECKS.md"), checks.toString().getBytes(StandardCharsets.UTF_8));
      Files.write(partial.resolve("RUN_SCOPE.txt"), (
          "SMOKE_ONLY=true\nformal500kAuthorized=false\nformalMatrixStarted=false\n"
          + "singleInstance=20_2_3_1\ncommonSeed=" + seed + "\ncommonInitialPopulationHash="
          + initialHash + "\n").getBytes(StandardCharsets.UTF_8));
      writeHashes(partial);
      move(partial, completed);
      return completed;
    } catch (Exception error) {
      if (Files.exists(partial)) {
        Files.write(partial.resolve("failure.txt"),
            (error.getClass().getName() + ": " + error.getMessage() + "\n")
                .getBytes(StandardCharsets.UTF_8));
        writeHashes(partial);
      }
      throw error;
    } finally {
      restore("dhfsp.data.dir", oldData);
      restore("dhfsp.fatigue.dir", oldFatigue);
      restore("dhfsp.instance.extension.dir", oldExtension);
    }
  }

  private static ZhangBoCanonicalProductionProblem load(Path instance, Path extension, Path fatigue,
      long seed) throws Exception {
    return ZhangBoCanonicalProblemLoader.load(instance, ProductionDecodeMode.FM3, seed, extension,
        fatigue, ZhangBoShiftConfiguration.none());
  }

  private static void verify(V35FinalAblationProfile.Arm arm, V35FairRunner.RunRecord record,
      String initialHash, V35ProductionConfiguration configuration, int maxFes) {
    require("COMPLETED".equals(record.getStatus()), arm, "run status=" + record.getStatus());
    require(initialHash.equals(record.getInitialPopulationHash()), arm, "initial population drift");
    require(record.getFullEvaluations() > 0 && record.getFullEvaluations() <= maxFes, arm,
        "FE outside (0," + maxFes + "]=" + record.getFullEvaluations());
    require(record.getDecoderCalls() == record.getFullEvaluations(), arm,
        "decoder/FE mismatch=" + record.getDecoderCalls() + "/" + record.getFullEvaluations());
    require(record.getIllegalSolutions() == 0 && record.getDuplicateEvaluations() == 0, arm,
        "illegal/duplicate=" + record.getIllegalSolutions() + "/" + record.getDuplicateEvaluations());
    require(record.getPassiveObservedCount() == record.getFullEvaluations(), arm,
        "passive archive missed an evaluation");
    require(!record.getFront().isEmpty(), arm, "empty front");
    String summary = record.getMechanismSummary();
    requirePositive(summary, arm, "qgSelections");
    requirePositive(summary, arm, "formalQgRounds");
    requirePositive(summary, arm, "pddrEvents");
    requirePositive(summary, arm, "fixedNeighborhoodEvents");
    requirePositive(summary, arm, "formalLocalFE");
    requireZero(summary, arm, "cfvfRepairs");
    requireZero(summary, arm, "directionalPoolRequests");
    requireZero(summary, arm, "directionalPoolFiltered");
    requireZero(summary, arm, "shadowSamples");
    requireZero(summary, arm, "shadowEvaluations");

    if (arm.isDscrEnabled()) {
      require(dscrValue(summary, "teacherUses") > 0L, arm, "teacherUses must be > 0");
      require(dscrValue(summary, "validityChecks") > 0L, arm, "validityChecks must be > 0");
      require(dscrValue(summary, "dominatedTeacherUses") == 0L, arm,
          "dominatedTeacherUses must be 0");
    } else {
      // Disabled DSCR serializes either zero counters or the explicit
      // `disabled` marker.  Normalize that marker to zero for the evidence.
      require(dscrValue(summary, "teacherUses") == 0L, arm, "teacherUses must be 0");
      require(dscrValue(summary, "validityChecks") == 0L, arm, "validityChecks must be 0");
      require(dscrValue(summary, "replacements") == 0L, arm, "replacements must be 0");
    }
    // CFVF deliberately replaces the structured baseline update from A2; a
    // non-zero baseline counter there would be a hidden mixed-mechanism arm.
    requireExpected(summary, arm, "baselineUpdateEvents", !arm.isCfvfEnabled());
    requireExpected(summary, arm, "cfvfOffspring", arm.isCfvfEnabled());
    requireExpected(summary, arm, "qpActions", arm.isQpEnabled());
    requireExpected(summary, arm, "qpTransitions", arm.isQpEnabled());
    requireExpected(summary, arm, "archiveInsertions", arm.isQpEnabled());
    requireExpected(summary, arm, "caTaLiteTest", arm.isCaTaLiteEnabled());
    requireExpected(summary, arm, "caTaLiteApply", arm.isCaTaLiteEnabled());
    if (arm.isQpEnabled()) {
      requirePositive(summary, arm, "dualQP");
      requirePositive(summary, arm, "dualQG");
    } else {
      requireZero(summary, arm, "qpActions");
      requireZero(summary, arm, "qpTransitions");
      requireZero(summary, arm, "archiveInsertions");
    }
    boolean expectedPacing = arm.isDynamicLocalFeBudgetEnabled();
    require((configuration.getLocalFeBudget() != null) == expectedPacing, arm,
        "dynamic local-FE configuration drift");
    if (expectedPacing) {
      require(configuration.getLocalFeBudget().getBetaMin() == V35FinalAblationProfile.LOCAL_FE_BETA_MIN
          && configuration.getLocalFeBudget().getBetaMax() == V35FinalAblationProfile.LOCAL_FE_BETA_MAX,
          arm, "unexpected local-FE beta parameters");
    }
  }

  private static String checkNote(V35FinalAblationProfile.Arm arm,
      V35FairRunner.RunRecord record) {
    String summary = record.getMechanismSummary();
    return "FE=" + record.getFullEvaluations() + "; Qg=" + value(summary, "qgSelections")
        + "; PDDR=" + value(summary, "pddrEvents") + "; localFE="
        + value(summary, "formalLocalFE") + "; DSCR=" + dscrValue(summary, "teacherUses")
        + "; CFVF=" + value(summary, "cfvfOffspring") + "; Qp/PA_i="
        + value(summary, "archiveInsertions") + "; CA-TA=" + value(summary, "caTaLiteTest")
        + "/" + value(summary, "caTaLiteApply");
  }

  private static String csvRow(V35FinalAblationProfile.Arm arm, V35FairRunner.RunRecord record,
      String initialHash, String profileHash) {
    String summary = record.getMechanismSummary();
    return String.format(Locale.ROOT,
        "%s,%s,%d,%d,%d,%d,%d,%d,%d,%d,%d,%d,%d,%d,%d,%d,%d,%d,%d,%d,%d,%d,%d,%d,%d,%d,%d,%d,%s,%s\n",
        arm.getLabel(), record.getStatus(), record.getFullEvaluations(), record.getDecoderCalls(),
        record.getIllegalSolutions(), record.getDuplicateEvaluations(), record.getPassiveObservedCount(),
        value(summary, "qgSelections"), value(summary, "pddrEvents"),
        value(summary, "baselineUpdateEvents"), value(summary, "fixedNeighborhoodEvents"),
        value(summary, "formalLocalFE"), dscrValue(summary, "teacherUses"),
        dscrValue(summary, "validityChecks"), dscrValue(summary, "replacements"),
        dscrValue(summary, "dominatedTeacherUses"), value(summary, "cfvfOffspring"),
        value(summary, "cfvfRepairs"), value(summary, "qpActions"), value(summary, "qpTransitions"),
        value(summary, "archiveInsertions"), value(summary, "dualQP"), value(summary, "dualQG"),
        value(summary, "caTaLiteTest"), value(summary, "caTaLiteApply"),
        value(summary, "directionalPoolRequests"), value(summary, "directionalPoolFiltered"),
        record.getFront().size(), initialHash, profileHash);
  }

  private static void requirePositive(String summary, V35FinalAblationProfile.Arm arm, String key) {
    require(value(summary, key) > 0L, arm, key + " must be > 0, got " + value(summary, key));
  }

  private static void requireZero(String summary, V35FinalAblationProfile.Arm arm, String key) {
    require(value(summary, key) == 0L, arm, key + " must be 0, got " + value(summary, key));
  }

  private static void requireExpected(String summary, V35FinalAblationProfile.Arm arm, String key,
      boolean enabled) {
    if (enabled) requirePositive(summary, arm, key); else requireZero(summary, arm, key);
  }

  private static void require(boolean condition, V35FinalAblationProfile.Arm arm, String message) {
    if (!condition) throw new IllegalStateException(arm.getLabel() + ": " + message);
  }

  static long value(String summary, String key) {
    String marker = key + "=";
    int index = summary == null ? -1 : summary.indexOf(marker);
    if (index < 0) return Long.MIN_VALUE;
    int start = index + marker.length();
    int end = start;
    if (end < summary.length() && summary.charAt(end) == '-') end++;
    while (end < summary.length() && Character.isDigit(summary.charAt(end))) {
      end++;
    }
    if (end == start || (end == start + 1 && summary.charAt(start) == '-')) {
      return Long.MIN_VALUE;
    }
    return Long.parseLong(summary.substring(start, end));
  }

  private static long dscrValue(String summary, String key) {
    long result = value(summary, key);
    return result == Long.MIN_VALUE ? 0L : result;
  }

  private static void requireFile(Path path) {
    if (!Files.isRegularFile(path)) throw new IllegalArgumentException("missing input: " + path);
  }

  private static void restore(String key, String value) {
    if (value == null) System.clearProperty(key); else System.setProperty(key, value);
  }

  private static void writeHashes(Path directory) throws Exception {
    Files.deleteIfExists(directory.resolve("evidence-sha256.tsv"));
    Map<String, String> hashes = new TreeMap<>();
    try (java.util.stream.Stream<Path> walk = Files.walk(directory)) {
      walk.filter(Files::isRegularFile).forEach(path -> {
        try {
          hashes.put(directory.relativize(path).toString().replace('\\', '/'), sha256(path));
        } catch (Exception error) {
          throw new RuntimeException(error);
        }
      });
    }
    StringBuilder out = new StringBuilder("path\tsha256\n");
    for (Map.Entry<String, String> entry : hashes.entrySet()) {
      out.append(entry.getKey()).append('\t').append(entry.getValue()).append('\n');
    }
    Files.write(directory.resolve("evidence-sha256.tsv"), out.toString().getBytes(StandardCharsets.UTF_8));
  }

  private static String sha256(Path path) throws Exception {
    byte[] digest = MessageDigest.getInstance("SHA-256").digest(Files.readAllBytes(path));
    StringBuilder out = new StringBuilder();
    for (byte value : digest) out.append(String.format("%02X", value & 0xff));
    return out.toString();
  }

  private static void move(Path source, Path target) throws IOException {
    try {
      Files.move(source, target, StandardCopyOption.ATOMIC_MOVE);
    } catch (AtomicMoveNotSupportedException error) {
      Files.move(source, target);
    }
  }

  private static final class Arguments {
    private Path projectRoot;
    private Path output;

    private static Arguments parse(String[] args) {
      Arguments value = new Arguments();
      for (int index = 0; index < args.length; index += 2) {
        if (index + 1 >= args.length) throw usage();
        if ("--project-root".equals(args[index])) value.projectRoot = Paths.get(args[index + 1]);
        else if ("--output".equals(args[index])) value.output = Paths.get(args[index + 1]);
        else throw usage();
      }
      if (value.projectRoot == null || value.output == null) throw usage();
      return value;
    }

    private static IllegalArgumentException usage() {
      return new IllegalArgumentException("Usage: --project-root <project-or-java-root>"
          + " --output <new-output-directory>; fixed at 2000 FE");
    }
  }
}
