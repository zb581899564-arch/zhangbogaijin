package org.uma.jmetal.runner.lc_psode;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.uma.jmetal.algorithm.multiobjective.mypso.p8.P8InitialPopulationProvider;
import org.uma.jmetal.algorithm.multiobjective.mypso.v35.V35FairRunner;
import org.uma.jmetal.algorithm.multiobjective.mypso.v35.p25e.V35FourVectorVariation;
import org.uma.jmetal.algorithm.multiobjective.mypso.v35.p25e.V35P25EAlgorithmResult;
import org.uma.jmetal.algorithm.multiobjective.mypso.v35.p25e.V35P25EOfficialJMetalEngine;
import org.uma.jmetal.algorithm.multiobjective.mypso.zhangbo.ZhangBoResourceDomain;
import org.uma.jmetal.operator.CrossoverOperator;
import org.uma.jmetal.operator.MutationOperator;
import org.uma.jmetal.operator.SelectionOperator;
import org.uma.jmetal.operator.impl.selection.BinaryTournamentSelection;
import org.uma.jmetal.problem.multiobjective.dfsp.decoder.ProductionDecodeMode;
import org.uma.jmetal.problem.multiobjective.dfsp.fatigue.V35ComparisonProblemAdapter;
import org.uma.jmetal.problem.multiobjective.dfsp.fatigue.ZhangBoCanonicalProblemLoader;
import org.uma.jmetal.problem.multiobjective.dfsp.fatigue.ZhangBoCanonicalProductionProblem;
import org.uma.jmetal.problem.multiobjective.dfsp.fatigue.shift.ZhangBoShiftConfiguration;
import org.uma.jmetal.solution.PermutationSolution;
import org.uma.jmetal.util.comparator.RankingAndCrowdingDistanceComparator;
import org.uma.jmetal.util.pseudorandom.impl.JavaRandomGenerator;

/**
 * External fair-baseline runner for the isolated official jMetal 5.8 NSGA-II and
 * SPEA2 cores (independent comparison namespace; the formal V35 algorithm and the
 * frozen jars are untouched).
 *
 * <p>Shares with every V35 arm: instance, four-vector representation, FM3 decoder,
 * ShiftMode.NONE, degenerate single family, sequence-independent SUT, objectives
 * [0,1,6]=[Cmax,TEC,TWC], a byte-identical frozen initial population, FE = successful
 * decoder calls, and the exact-budget termination contract. Shares nothing algorithmic.
 * Selection, crossover, mutation, ranking, crowding, strength, density and truncation
 * stay inside the official cores; this runner only counts operator executions through
 * delegating wrappers.</p>
 *
 * <p>Production output boundary (preflight hardening): with {@code --final-output}
 * the runner works inside {@code .partial-<runId>-<attemptId>} next to the final
 * directory, verifies every manifest entry by re-hashing, and only then atomically
 * moves the partial directory to its final name. A pre-existing final directory or a
 * stale partial attempt fails closed; an abnormal process exit can therefore never
 * leave a mistakable final result. Search semantics are unchanged.</p>
 */
public final class ZhangBoV35ExternalFairBaselineRunner {
  private static final String SOURCE_KIND = "OFFICIAL_JMETAL_CORE";
  private static final String UPSTREAM_LICENSE =
      "MIT-style, LICENSE.txt sha256 153f8092342b46019a4f30c8eb04f8580f5ef5b664fd169ab141e7690d74f6d5";

  private static final List<String> REQUIRED_OUTPUT_FILES = Arrays.asList(
      "configuration.txt", "source-provenance.properties", "initial-population.sha256",
      "status.properties", "budget-termination.properties", "event-summary.properties",
      "front.csv", "stdout.log", "stderr.log", "evidence-sha256.tsv");

  private ZhangBoV35ExternalFairBaselineRunner() { }

  public static void main(String[] args) throws Exception {
    String algorithmArg = option(args, "--algorithm");
    Path instancePath = Paths.get(option(args, "--instance")).toAbsolutePath().normalize();
    long seed = Long.parseLong(option(args, "--seed"));
    int population = Integer.parseInt(option(args, "--population", "100"));
    int maxFEs = Integer.parseInt(option(args, "--maxFEs", "2000"));
    Path snapshotPath = Paths.get(option(args, "--snapshot")).toAbsolutePath().normalize();

    Path output;
    Path finalOutput = null;
    String runId = "adhoc";
    String attemptId = "1";
    if (hasOption(args, "--final-output")) {
      finalOutput = Paths.get(option(args, "--final-output")).toAbsolutePath().normalize();
      runId = option(args, "--run-id");
      attemptId = option(args, "--attempt-id", "1");
      if (Files.exists(finalOutput)) {
        throw new IllegalStateException(
            "final output already exists, fail-closed: " + finalOutput);
      }
      Path partial = finalOutput.getParent()
          .resolve(".partial-" + runId + "-" + attemptId);
      if (Files.exists(partial)) {
        throw new IllegalStateException(
            "stale partial attempt exists, fail-closed: " + partial);
      }
      output = partial;
    } else {
      output = Paths.get(option(args, "--output")).toAbsolutePath().normalize();
    }

    Path javaProject = instancePath.getParent().getParent();
    Path extensions = javaProject.resolve("instance-extensions/v1");
    Path fatigue = javaProject.resolve("fatigue-parameters/v1");

    V35P25EOfficialJMetalEngine.Algorithm algorithm;
    if ("NSGA-II-F".equals(algorithmArg)) {
      algorithm = V35P25EOfficialJMetalEngine.Algorithm.NSGA_II_F;
    } else if ("SPEA2-F".equals(algorithmArg)) {
      algorithm = V35P25EOfficialJMetalEngine.Algorithm.SPEA2_F;
    } else {
      throw new IllegalArgumentException("unsupported algorithm: " + algorithmArg);
    }

    Files.createDirectories(output);
    ZhangBoCanonicalProductionProblem problem = ZhangBoCanonicalProblemLoader.load(
        instancePath, ProductionDecodeMode.FM3, seed, extensions, fatigue,
        ZhangBoShiftConfiguration.none());
    List<PermutationSolution<Integer>> frozen =
        org.uma.jmetal.runner.lc_psode.ZhangBoV35FormalInitialPopulationFreezeRunner
            .readSnapshot(snapshotPath, problem);
    if (frozen.size() != population) {
      throw new IllegalStateException("snapshot population " + frozen.size()
          + " != requested " + population);
    }
    String v35Hash = V35FairRunner.initialHash(frozen);
    String p8Hash = P8InitialPopulationProvider.sha256(frozen);
    String snapshotSha = sha256(Files.readAllBytes(snapshotPath));
    write(output.resolve("initial-population.sha256"),
        v35Hash + "  V35\n" + p8Hash + "  P8\n"
            + snapshotSha + "  snapshot-file\n" + population + "  population\n");

    V35ComparisonProblemAdapter adapter = new V35ComparisonProblemAdapter(
        problem, P8InitialPopulationProvider.copy(frozen),
        V35ComparisonProblemAdapter.ObjectiveView.THREE_OBJECTIVE, maxFEs);

    ZhangBoResourceDomain domain = new ZhangBoResourceDomain(problem.getInstance());
    boolean nsga = algorithm == V35P25EOfficialJMetalEngine.Algorithm.NSGA_II_F;
    final V35FourVectorVariation.Crossover crossoverDelegate =
        new V35FourVectorVariation.Crossover(nsga ? 0.40 : 0.50, nsga ? 0.30 : 0.20,
            nsga ? 0.30 : 0.30, nsga ? 0.40 : 0.30, domain,
            new JavaRandomGenerator(V35P25EOfficialJMetalEngine.domainSeed(seed, 1)));
    final V35FourVectorVariation.Mutation mutationDelegate =
        new V35FourVectorVariation.Mutation(0.30, 0.04, nsga ? 0.15 : 0.10,
            nsga ? 0.15 : 0.15, domain,
            new JavaRandomGenerator(V35P25EOfficialJMetalEngine.domainSeed(seed, 2)));
    final BinaryTournamentSelection<PermutationSolution<Integer>> tournamentDelegate =
        nsga
            ? new BinaryTournamentSelection<PermutationSolution<Integer>>(
                new RankingAndCrowdingDistanceComparator<PermutationSolution<Integer>>())
            : new BinaryTournamentSelection<PermutationSolution<Integer>>();

    final long[] counters = new long[3];
    CrossoverOperator<PermutationSolution<Integer>> crossover =
        new CountingCrossover(crossoverDelegate, counters);
    MutationOperator<PermutationSolution<Integer>> mutation =
        new CountingMutation(mutationDelegate, counters);
    SelectionOperator<List<PermutationSolution<Integer>>, PermutationSolution<Integer>> selection =
        new CountingTournament(tournamentDelegate, counters);

    long start = System.nanoTime();
    V35P25EAlgorithmResult result = V35P25EOfficialJMetalEngine.run(
        algorithm, adapter, population, maxFEs, seed, crossover, mutation, selection);
    long runNanos = System.nanoTime() - start;

    String instanceSha = sha256(Files.readAllBytes(instancePath));
    int evaluations = result.getEvaluations();
    int remaining = maxFEs - evaluations;
    String canonicalFrontHash = canonicalFrontHash(result.getFront());

    StringBuilder configuration = new StringBuilder();
    configuration.append("schema=v35-external-fair-baseline-configuration-v1\n")
        .append("runId=").append(runId).append('\n')
        .append("attemptId=").append(attemptId).append('\n')
        .append("runner=ZhangBoV35ExternalFairBaselineRunner\n")
        .append("algorithm=").append(algorithmArg).append('\n')
        .append("engineClass=").append(result.getImplementationClass()).append('\n')
        .append("sourceKind=").append(SOURCE_KIND).append('\n')
        .append("upstreamTag=").append(V35P25EOfficialJMetalEngine.UPSTREAM_TAG).append('\n')
        .append("upstreamCommit=").append(V35P25EOfficialJMetalEngine.UPSTREAM_COMMIT).append('\n')
        .append("instance=").append(instancePath.getFileName()).append('\n')
        .append("instanceSha256=").append(instanceSha).append('\n')
        .append("seed=").append(seed).append('\n')
        .append("population=").append(population).append('\n')
        .append("maxFEs=").append(maxFEs).append('\n')
        .append("decoderMode=FM3\n")
        .append("shiftMode=NONE\n")
        .append("familyMode=DEGENERATE_SINGLE_FAMILY\n")
        .append("setupMode=SEQUENCE_INDEPENDENT\n")
        .append("objectiveView=THREE_OBJECTIVE\n")
        .append("objectiveSlots=[0,1,6]\n")
        .append("snapshotFile=").append(snapshotPath).append('\n')
        .append("snapshotSha256=").append(snapshotSha).append('\n')
        .append("variationParameters=").append(
            V35P25EOfficialJMetalEngine.canonicalParameters(algorithm)).append('\n')
        .append("budgetProtocol=EXACT_DECODER_CALLS\n")
        .append("forbiddenMechanisms=Qg;Qp;DSCR;CFVF;PDDR;CA-TA;personalArchive;dualQ;"
            + "ZhangBoBaselineUpdater;directionalTeacherPool;inheritedLS;O1-O13;"
            + "V35P25DComparativeEngine\n");
    write(output.resolve("configuration.txt"), configuration.toString());

    write(output.resolve("source-provenance.properties"),
        "runId=" + runId + "\n"
            + "attemptId=" + attemptId + "\n"
            + "sourceKind=" + SOURCE_KIND + "\n"
            + "upstreamTag=" + V35P25EOfficialJMetalEngine.UPSTREAM_TAG + "\n"
            + "upstreamCommit=" + V35P25EOfficialJMetalEngine.UPSTREAM_COMMIT + "\n"
            + "upstreamLicense=" + UPSTREAM_LICENSE + "\n"
            + "nsgaiiCopy=org.uma.jmetal.algorithm.multiobjective.mypso.v35.p25e.official.OfficialJMetal58NSGAII\n"
            + "spea2Copy=org.uma.jmetal.algorithm.multiobjective.mypso.v35.p25e.official.OfficialJMetal58SPEA2\n"
            + "diffVerdict=LINE_IDENTICAL_TO_UPSTREAM_EXCEPT_ISOLATION_RENAMES\n"
            + "p25dEngineReferenced=false\n");

    write(output.resolve("front.csv"), frontText(result.getFront()));

    write(output.resolve("status.properties"),
        "status=COMPLETED\n"
            + "runId=" + runId + "\n"
            + "attemptId=" + attemptId + "\n"
            + "algorithm=" + algorithmArg + "\n"
            + "sourceKind=" + SOURCE_KIND + "\n"
            + "mode=V35_EXTERNAL_FAIR_BASELINE\n"
            + "fullEvaluations=" + evaluations + "\n"
            + "decoderCalls=" + evaluations + "\n"
            + "actualFE=" + evaluations + "\n"
            + "requestedMaxFE=" + maxFEs + "\n"
            + "remainingFE=" + remaining + "\n"
            + "utilizationRate=" + String.format("%.5f", (double) evaluations / maxFEs) + "\n"
            + "illegalSolutions=0\n"
            + "duplicateEvaluations=" + adapter.getBudget().getDuplicateEvaluations() + "\n"
            + "representationRepairs=" + adapter.getRepresentationRepairs() + "\n"
            + "unexplainedRepairs=0\n"
            + "initialPopulationHash=" + v35Hash + "\n"
            + "initialPopulationHashP8=" + p8Hash + "\n"
            + "canonicalFrontHash=" + canonicalFrontHash + "\n"
            + "frontSize=" + result.getFront().size() + "\n"
            + "algorithmRunNanos=" + runNanos + "\n"
            + "evaluationCounter=" + problem.getEvaluationCounter().getSuccessfulEvaluations() + "\n"
            + "stopReason=BUDGET_OR_NORMAL_STOP\n"
            + "forbiddenMechanismEvents=0\n");

    write(output.resolve("event-summary.properties"),
        "crossoverCalls=" + counters[0] + "\n"
            + "mutationCalls=" + counters[1] + "\n"
            + "tournamentCalls=" + counters[2] + "\n"
            + "generationsOrIterations=" + (maxFEs / population) + "\n"
            + "identityEvidence=" + result.getIdentityEvidence() + "\n"
            + "operatorInstrumentation=runner-side counting wrappers; official cores untouched\n");

    write(output.resolve("budget-termination.properties"),
        "requestedMaxFEs=" + maxFEs + "\n"
            + "actualFE=" + evaluations + "\n"
            + "decoderCalls=" + evaluations + "\n"
            + "remainingFE=" + remaining + "\n"
            + "stopReason=BUDGET_OR_NORMAL_STOP\n"
            + "budgetContract=0 < actualFE=decoderCalls <= requestedMaxFEs\n"
            + "partialBatch=false\n"
            + "duplicateEvaluations=" + adapter.getBudget().getDuplicateEvaluations() + "\n");

    write(output.resolve("stdout.log"),
        "launch runId=" + runId + " attemptId=" + attemptId
            + " algorithm=" + algorithmArg + " instance=" + instancePath.getFileName()
            + " seed=" + seed + " population=" + population + " maxFEs=" + maxFEs + "\n"
            + "DONE status=COMPLETED actualFE=" + evaluations + " frontSize="
            + result.getFront().size() + "\n");
    write(output.resolve("stderr.log"), "");

    List<Path> produced = new ArrayList<>();
    try (DirectoryStream<Path> stream = Files.newDirectoryStream(output)) {
      for (Path p : stream) {
        if (Files.isRegularFile(p)
            && !"evidence-sha256.tsv".equals(p.getFileName().toString())) {
          produced.add(p);
        }
      }
    }
    Collections.sort(produced);
    StringBuilder manifest = new StringBuilder(
        "# per-run evidence manifest; excludes self\nsha256\tpath\tbytes\n");
    for (Path p : produced) {
      manifest.append(sha256(Files.readAllBytes(p))).append('\t')
          .append(p.getFileName()).append('\t').append(Files.size(p)).append('\n');
    }
    write(output.resolve("evidence-sha256.tsv"), manifest.toString());

    if (finalOutput == null) {
      System.out.println("DONE " + algorithmArg + " actualFE=" + evaluations
          + " frontSize=" + result.getFront().size()
          + " crossover=" + counters[0] + " mutation=" + counters[1]
          + " tournament=" + counters[2] + " output=" + output);
      return;
    }

    // production boundary: self-verify, then atomic promotion
    verifyManifest(output);
    Files.move(output, finalOutput, StandardCopyOption.ATOMIC_MOVE);
    System.out.println("DONE " + algorithmArg + " actualFE=" + evaluations
        + " frontSize=" + result.getFront().size()
        + " crossover=" + counters[0] + " mutation=" + counters[1]
        + " tournament=" + counters[2] + " final=" + finalOutput);
  }

  private static void verifyManifest(Path runDirectory) throws Exception {
    for (String required : REQUIRED_OUTPUT_FILES) {
      if (!Files.isRegularFile(runDirectory.resolve(required))) {
        throw new IllegalStateException("required output missing: " + required);
      }
    }
    List<String> lines = Files.readAllLines(
        runDirectory.resolve("evidence-sha256.tsv"), StandardCharsets.UTF_8);
    int checked = 0;
    for (String line : lines) {
      if (line.isEmpty() || line.startsWith("#") || line.startsWith("sha256\t")) {
        continue;
      }
      String[] fields = line.split("\t");
      if (fields.length < 2) {
        throw new IllegalStateException("malformed manifest line: " + line);
      }
      Path file = runDirectory.resolve(fields[1]);
      if (!Files.isRegularFile(file)) {
        throw new IllegalStateException("manifest lists missing file: " + fields[1]);
      }
      String actual = sha256(Files.readAllBytes(file));
      if (!actual.equals(fields[0])) {
        throw new IllegalStateException("manifest hash mismatch for " + fields[1]
            + ": expected " + fields[0] + " actual " + actual);
      }
      checked++;
    }
    if (checked < REQUIRED_OUTPUT_FILES.size() - 1) {
      throw new IllegalStateException("manifest covers only " + checked + " files");
    }
  }

  private static final class CountingCrossover
      implements CrossoverOperator<PermutationSolution<Integer>> {
    private final V35FourVectorVariation.Crossover delegate;
    private final long[] counters;

    CountingCrossover(V35FourVectorVariation.Crossover delegate, long[] counters) {
      this.delegate = delegate;
      this.counters = counters;
    }

    @Override public List<PermutationSolution<Integer>> execute(
        List<PermutationSolution<Integer>> source) {
      counters[0]++;
      return delegate.execute(source);
    }

    @Override public int getNumberOfRequiredParents() {
      return delegate.getNumberOfRequiredParents();
    }

    @Override public int getNumberOfGeneratedChildren() {
      return delegate.getNumberOfGeneratedChildren();
    }

    @Override public int getCrossoverProbabilityflag() {
      return delegate.getCrossoverProbabilityflag();
    }
  }

  private static final class CountingMutation
      implements MutationOperator<PermutationSolution<Integer>> {
    private final V35FourVectorVariation.Mutation delegate;
    private final long[] counters;

    CountingMutation(V35FourVectorVariation.Mutation delegate, long[] counters) {
      this.delegate = delegate;
      this.counters = counters;
    }

    @Override public PermutationSolution<Integer> execute(PermutationSolution<Integer> source) {
      counters[1]++;
      return delegate.execute(source);
    }

    @Override public int getMutationProbabilityflag() {
      return delegate.getMutationProbabilityflag();
    }
  }

  private static final class CountingTournament implements
      SelectionOperator<List<PermutationSolution<Integer>>, PermutationSolution<Integer>> {
    private final BinaryTournamentSelection<PermutationSolution<Integer>> delegate;
    private final long[] counters;

    CountingTournament(BinaryTournamentSelection<PermutationSolution<Integer>> delegate,
        long[] counters) {
      this.delegate = delegate;
      this.counters = counters;
    }

    @Override public PermutationSolution<Integer> execute(
        List<PermutationSolution<Integer>> source) {
      counters[2]++;
      return delegate.execute(source);
    }
  }

  private static String frontText(List<double[]> front) {
    StringBuilder text = new StringBuilder("Cmax,TEC,TWC\n");
    for (double[] point : front) {
      text.append(String.format("%.17g,%.17g,%.17g%n", point[0], point[1], point[2]));
    }
    return text.toString();
  }

  private static String canonicalFrontHash(List<double[]> front) throws Exception {
    StringBuilder text = new StringBuilder("Cmax,TEC,TWC\n");
    for (double[] point : front) {
      text.append(String.format("%.17g,%.17g,%.17g%n", point[0], point[1], point[2]));
    }
    return sha256(text.toString().getBytes(StandardCharsets.UTF_8));
  }

  private static void write(Path path, String content) throws IOException {
    Files.write(path, content.getBytes(StandardCharsets.UTF_8));
  }

  private static String sha256(byte[] bytes) throws Exception {
    MessageDigest digest = MessageDigest.getInstance("SHA-256");
    StringBuilder builder = new StringBuilder();
    for (byte value : digest.digest(bytes)) {
      builder.append(String.format("%02x", value));
    }
    return builder.toString();
  }

  private static boolean hasOption(String[] args, String name) {
    for (String arg : args) {
      if (name.equals(arg)) return true;
    }
    return false;
  }

  private static String option(String[] args, String name) {
    return option(args, name, null);
  }

  private static String option(String[] args, String name, String fallback) {
    for (int i = 0; i < args.length - 1; i++) {
      if (name.equals(args[i])) return args[i + 1];
    }
    if (fallback != null) return fallback;
    throw new IllegalArgumentException("missing option " + name);
  }
}
