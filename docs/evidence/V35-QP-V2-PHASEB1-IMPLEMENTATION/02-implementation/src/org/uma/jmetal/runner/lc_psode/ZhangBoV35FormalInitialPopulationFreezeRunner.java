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
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import org.uma.jmetal.algorithm.multiobjective.mypso.p8.P8InitialPopulationProvider;
import org.uma.jmetal.algorithm.multiobjective.mypso.v35.V35FairRunner;
import org.uma.jmetal.problem.multiobjective.dfsp.decoder.ProductionDecodeMode;
import org.uma.jmetal.problem.multiobjective.dfsp.fatigue.ZhangBoCanonicalProblemLoader;
import org.uma.jmetal.problem.multiobjective.dfsp.fatigue.ZhangBoCanonicalProductionProblem;
import org.uma.jmetal.problem.multiobjective.dfsp.fatigue.ZhangBoFatigueInstanceData;
import org.uma.jmetal.problem.multiobjective.dfsp.model.DhhfspFourVectorSolution;
import org.uma.jmetal.problem.multiobjective.dfsp.fatigue.shift.ZhangBoShiftConfiguration;
import org.uma.jmetal.solution.PermutationSolution;

/**
 * Materializes the immutable 45-instance x 20-seed formal initial populations.
 *
 * <p>This is deliberately not an experiment runner: it performs zero decoder
 * evaluations.  It writes each four-vector population once, then reloads and
 * verifies it before accepting the bundle.  Future A0--A4 arms must consume a
 * snapshot through {@link #readSnapshot(Path, ZhangBoCanonicalProductionProblem)}
 * instead of independently calling {@code problem.createSolution()}.</p>
 */
public final class ZhangBoV35FormalInitialPopulationFreezeRunner {
  public static final String VERSION = "v35-formal-initial-population-freeze-v1";
  public static final String SCHEMA = "v35-formal-initial-population-v1";
  public static final int POPULATION = 100;
  private static final int[] JOBS = {20, 50, 100, 150, 200};
  private static final int[] STAGES = {2, 5, 8};
  private static final int[] FACTORIES = {3, 4, 5};
  private static final List<Long> SEEDS = frozenSeeds();

  private ZhangBoV35FormalInitialPopulationFreezeRunner() { }

  public static void main(String[] args) throws Exception {
    Arguments arguments = Arguments.parse(args);
    if ("MATERIALIZE".equals(arguments.phase)) {
      materialize(arguments.projectRoot, arguments.output);
      System.out.println("V35_FORMAL_INITIAL_POPULATIONS_MATERIALIZED rows="
          + (instanceIds().size() * SEEDS.size()));
    } else if ("VERIFY".equals(arguments.phase)) {
      Verification verification = verify(arguments.projectRoot, arguments.output);
      System.out.println("V35_FORMAL_INITIAL_POPULATIONS_VERIFIED rows=" + verification.rows);
    } else {
      throw new IllegalArgumentException("--phase must be MATERIALIZE or VERIFY");
    }
  }

  /** Returns the immutable 20-seed roster pre-registered in docs/ROADMAP.md. */
  public static List<Long> formalSeeds() { return SEEDS; }

  /** Returns the canonical 5 x 3 x 3 EADHFSP formal matrix in lexical dimension order. */
  public static List<String> instanceIds() {
    List<String> result = new ArrayList<>();
    for (int jobs : JOBS) for (int stages : STAGES) for (int factories : FACTORIES) {
      result.add(jobs + "_" + stages + "_" + factories + "_1");
    }
    return Collections.unmodifiableList(result);
  }

  /**
   * Creates a new, complete bundle atomically.  Existing evidence is never
   * overwritten because that would silently alter the fairness baseline.
   */
  public static void materialize(Path projectRoot, Path output) throws Exception {
    Path project = projectRoot.toAbsolutePath().normalize();
    Path javaProject = javaProject(project);
    Path target = output.toAbsolutePath().normalize();
    if (Files.exists(target)) throw new IllegalStateException("refusing overwrite: " + target);
    Files.createDirectories(target.getParent());
    Path partial = target.getParent().resolve(".partial-" + target.getFileName() + '-' + System.nanoTime());
    Files.createDirectory(partial);
    try {
      writeBundle(javaProject, partial);
      Verification verification = verify(javaProject, partial);
      if (verification.rows != instanceIds().size() * SEEDS.size()) {
        throw new IllegalStateException("materialized row count mismatch=" + verification.rows);
      }
      writeEvidenceManifest(partial);
      move(partial, target);
    } catch (Exception error) {
      Files.write(partial.resolve("FAILURE.txt"), (error.getClass().getName() + ": "
          + error.getMessage() + "\n").getBytes(StandardCharsets.UTF_8));
      writeEvidenceManifest(partial);
      throw error;
    }
  }

  /** Verifies every frozen population without evaluating a solution. */
  public static Verification verify(Path projectRoot, Path bundle) throws Exception {
    Path javaProject = javaProject(projectRoot.toAbsolutePath().normalize());
    Path root = bundle.toAbsolutePath().normalize();
    Path manifest = root.resolve("FORMAL_INITIAL_POPULATION_MANIFEST.csv");
    if (!Files.isRegularFile(manifest)) throw new IllegalArgumentException("missing population manifest: " + manifest);
    List<Map<String, String>> rows = csv(manifest);
    int expected = instanceIds().size() * SEEDS.size();
    if (rows.size() != expected) throw new IllegalStateException("population manifest rows=" + rows.size()
        + " expected=" + expected);
    Set<String> keys = new HashSet<>();
    for (Map<String, String> row : rows) {
      String instanceId = required(row, "instanceId");
      long seed = Long.parseLong(required(row, "seed"));
      if (!instanceIds().contains(instanceId) || !SEEDS.contains(seed)) {
        throw new IllegalStateException("unexpected population row=" + instanceId + '/' + seed);
      }
      if (!keys.add(instanceId + '/' + seed)) throw new IllegalStateException("duplicate population row=" + instanceId + '/' + seed);
      Path snapshot = root.resolve(required(row, "snapshotPath")).normalize();
      Path instance = javaProject.resolve("EADHFSP/" + instanceId + ".txt");
      Path extensionDirectory = javaProject.resolve("instance-extensions/v1");
      Path fatigueDirectory = javaProject.resolve("fatigue-parameters/v1");
      ZhangBoCanonicalProductionProblem problem = ZhangBoCanonicalProblemLoader.load(instance,
          ProductionDecodeMode.FM3, seed, extensionDirectory, fatigueDirectory,
          ZhangBoShiftConfiguration.none());
      List<PermutationSolution<Integer>> population = readSnapshot(snapshot, problem);
      if (population.size() != POPULATION) throw new IllegalStateException("population size drift=" + snapshot);
      String v35 = V35FairRunner.initialHash(population);
      String p8 = P8InitialPopulationProvider.sha256(population);
      requireEqual("initialPopulationSHA256", required(row, "initialPopulationSHA256"), v35);
      requireEqual("initialPopulationP8SHA256", required(row, "initialPopulationP8SHA256"), p8);
      requireEqual("snapshotSHA256", required(row, "snapshotSHA256"), sha256(snapshot));
      requireEqual("instanceSHA256", required(row, "instanceSHA256"), problem.getInstance().getInstanceSha256());
      requireEqual("SUTSHA256", required(row, "SUTSHA256"), problem.getInstance().getInstanceExtensionSha256());
      requireEqual("fatigueParameterSHA256", required(row, "fatigueParameterSHA256"),
          problem.getParameters().getConfigurationSha256());
      requireEqual("problemConfigurationSHA256", required(row, "problemConfigurationSHA256"),
          problemConfigurationHash(instanceId, problem));
      if (problem.getEvaluationCounter().getSuccessfulEvaluations() != 0L) {
        throw new IllegalStateException("snapshot verification evaluated a solution");
      }
    }
    return new Verification(rows.size());
  }

  /**
   * Loads a physical snapshot for a formal arm.  The input solution objects are
   * fresh and mutable copies; the immutable on-disk snapshot is not modified.
   */
  public static List<PermutationSolution<Integer>> readSnapshot(Path snapshot,
      ZhangBoCanonicalProductionProblem problem) throws IOException {
    if (snapshot == null || problem == null) throw new IllegalArgumentException("snapshot/problem");
    List<String> lines = Files.readAllLines(snapshot, StandardCharsets.UTF_8);
    Map<String, String> header = new HashMap<>();
    List<PermutationSolution<Integer>> population = new ArrayList<>();
    int cursor = 0;
    while (cursor < lines.size() && !lines.get(cursor).startsWith("particle=")) {
      String line = lines.get(cursor++);
      int split = line.indexOf('=');
      if (split <= 0) throw new IllegalArgumentException("invalid snapshot header: " + snapshot);
      header.put(line.substring(0, split), line.substring(split + 1));
    }
    requireEqual("snapshot.schema", SCHEMA, required(header, "schema"));
    requireEqual("snapshot.instanceSha256", problem.getInstance().getInstanceSha256(), required(header, "instanceSHA256"));
    requireEqual("snapshot.SUTSHA256", problem.getInstance().getInstanceExtensionSha256(), required(header, "SUTSHA256"));
    requireEqual("snapshot.fatigueParameterSHA256", problem.getParameters().getConfigurationSha256(), required(header, "fatigueParameterSHA256"));
    requireEqual("snapshot.decoderMode", "FM3", required(header, "decoderMode"));
    requireEqual("snapshot.familyMode", "DEGENERATE_SINGLE_FAMILY", required(header, "familyMode"));
    requireEqual("snapshot.setupMode", "SEQUENCE_INDEPENDENT", required(header, "setupMode"));
    requireEqual("snapshot.shiftMode", "NONE", required(header, "shiftMode"));
    requireEqual("snapshot.semanticTag", problem.getMode().getSemanticTag(), required(header, "semanticTag"));
    int declaredPopulation = Integer.parseInt(required(header, "population"));
    int jobs = problem.getNumberOfVariables();
    while (cursor < lines.size()) {
      String particle = lines.get(cursor++);
      if (!particle.startsWith("particle=")) throw new IllegalArgumentException("expected particle line: " + snapshot);
      int ordinal = Integer.parseInt(particle.substring("particle=".length()));
      if (ordinal != population.size()) throw new IllegalArgumentException("non-contiguous particle ordinal: " + snapshot);
      if (cursor + 3 >= lines.size()) throw new IllegalArgumentException("truncated particle: " + snapshot);
      List<Integer> js = vector(lines.get(cursor++), "JS", jobs);
      List<Integer> fa = vector(lines.get(cursor++), "FA", jobs);
      List<Integer> ma = vector(lines.get(cursor++), "MA", jobs);
      List<Integer> wa = vector(lines.get(cursor++), "WA", jobs);
      DhhfspFourVectorSolution solution = new DhhfspFourVectorSolution(js, fa, ma, wa,
          problem.getMode().getSemanticTag(), ZhangBoCanonicalProductionProblem.NUMBER_OF_OBJECTIVES);
      validate(solution, problem.getInstance());
      population.add(solution);
    }
    if (population.size() != declaredPopulation || population.size() != POPULATION) {
      throw new IllegalArgumentException("snapshot population count mismatch: " + snapshot);
    }
    String v35 = V35FairRunner.initialHash(population);
    String p8 = P8InitialPopulationProvider.sha256(population);
    requireEqual("snapshot.initialPopulationSHA256", required(header, "initialPopulationSHA256"), v35);
    requireEqual("snapshot.initialPopulationP8SHA256", required(header, "initialPopulationP8SHA256"), p8);
    return population;
  }

  private static void writeBundle(Path javaProject, Path root) throws Exception {
    List<String> instanceRows = new ArrayList<>();
    instanceRows.add("instanceId,instancePath,instanceSHA256,scale,SUTSHA256,fatigueParameterSHA256,problemConfigurationSHA256");
    List<String> populationRows = new ArrayList<>();
    populationRows.add("instanceId,seed,populationSize,snapshotPath,snapshotSHA256,initialPopulationSHA256,initialPopulationP8SHA256,instanceSHA256,SUTSHA256,fatigueParameterSHA256,problemConfigurationSHA256");
    for (String instanceId : instanceIds()) {
      Path instance = javaProject.resolve("EADHFSP/" + instanceId + ".txt");
      Path extensionDirectory = javaProject.resolve("instance-extensions/v1");
      Path fatigueDirectory = javaProject.resolve("fatigue-parameters/v1");
      requireFile(instance); requireFile(extensionDirectory.resolve(instanceId + ".setup.txt"));
      requireFile(fatigueDirectory.resolve(instanceId + ".fatigue.txt"));
      ZhangBoCanonicalProductionProblem provenance = ZhangBoCanonicalProblemLoader.load(instance,
          ProductionDecodeMode.FM3, SEEDS.get(0), extensionDirectory, fatigueDirectory,
          ZhangBoShiftConfiguration.none());
      String problemHash = problemConfigurationHash(instanceId, provenance);
      instanceRows.add(instanceId + ",java-jmetal58/EADHFSP/" + instanceId + ".txt,"
          + provenance.getInstance().getInstanceSha256() + ',' + scale(instanceId) + ','
          + provenance.getInstance().getInstanceExtensionSha256() + ','
          + provenance.getParameters().getConfigurationSha256() + ',' + problemHash);
      for (long seed : SEEDS) {
        ZhangBoCanonicalProductionProblem problem = ZhangBoCanonicalProblemLoader.load(instance,
            ProductionDecodeMode.FM3, seed, extensionDirectory, fatigueDirectory,
            ZhangBoShiftConfiguration.none());
        List<PermutationSolution<Integer>> population = new ArrayList<>();
        for (int particle = 0; particle < POPULATION; particle++) population.add(problem.createSolution());
        String v35 = V35FairRunner.initialHash(population);
        String p8 = P8InitialPopulationProvider.sha256(population);
        Path relative = Paths.get("initial-populations", instanceId, "seed-" + seed + ".fourvec");
        Path snapshot = root.resolve(relative);
        Files.createDirectories(snapshot.getParent());
        writeSnapshot(snapshot, instanceId, seed, problem, population, v35, p8, problemHash);
        List<PermutationSolution<Integer>> reloaded = readSnapshot(snapshot, problem);
        requireEqual("reloaded V35 hash", v35, V35FairRunner.initialHash(reloaded));
        requireEqual("reloaded P8 hash", p8, P8InitialPopulationProvider.sha256(reloaded));
        if (problem.getEvaluationCounter().getSuccessfulEvaluations() != 0L) {
          throw new IllegalStateException("materialization evaluated a solution");
        }
        populationRows.add(instanceId + ',' + seed + ',' + POPULATION + ','
            + relative.toString().replace('\\', '/') + ',' + sha256(snapshot) + ',' + v35 + ',' + p8 + ','
            + problem.getInstance().getInstanceSha256() + ','
            + problem.getInstance().getInstanceExtensionSha256() + ','
            + problem.getParameters().getConfigurationSha256() + ',' + problemHash);
      }
    }
    Files.write(root.resolve("FORMAL_INSTANCE_MANIFEST.csv"),
        (String.join("\n", instanceRows) + "\n").getBytes(StandardCharsets.UTF_8));
    StringBuilder seeds = new StringBuilder("# V35 formal independent seed roster\n")
        .append("# Pre-registered non-performance rule: inclusive integer range 20260808..20260827.\n")
        .append("# Source: docs/ROADMAP.md P9 formal-matrix decision; no outcome-based seed selection.\n");
    for (long seed : SEEDS) seeds.append(seed).append('\n');
    Files.write(root.resolve("FORMAL_SEEDS.txt"), seeds.toString().getBytes(StandardCharsets.UTF_8));
    Files.write(root.resolve("FORMAL_INITIAL_POPULATION_MANIFEST.csv"),
        (String.join("\n", populationRows) + "\n").getBytes(StandardCharsets.UTF_8));
    String seedHash = sha256(root.resolve("FORMAL_SEEDS.txt"));
    String instanceHash = sha256(root.resolve("FORMAL_INSTANCE_MANIFEST.csv"));
    String populationHash = sha256(root.resolve("FORMAL_INITIAL_POPULATION_MANIFEST.csv"));
    String report = "# V35 Formal Manifest and Fairness Freeze\n\n"
        + "```text\nFORMAL_MANIFEST_FREEZE=ACCEPTED\n"
        + "schema=" + SCHEMA + "\ninstances=45\nseeds=20\ninitialPopulationSnapshots=900\n"
        + "populationSize=100\nformalRunsCovered=4500 (A0-A4 x 45 x 20; no run started)\n"
        + "evaluationsPerformedDuringFreeze=0\n```\n\n"
        + "## Frozen semantics\n\n"
        + "FM3; `DEGENERATE_SINGLE_FAMILY`; `SEQUENCE_INDEPENDENT`; `ShiftMode=NONE`; "
        + "`GLOBAL_ORIGINAL`; `CA-TA-Lite -> inherited LS`; A4-Pacing; dual-Q P=5/G=5; "
        + "Qg/Qp/DSCR/CFVF/PA_i enabled only according to the legal A0-A4 rung; `rho=0`; "
        + "directional teacher pool disabled; population 100; `MaxFEs=500000`; mixture 20/40/20/20.\n\n"
        + "## Fairness contract\n\n"
        + "For every `(instanceId, seed)`, each A0-A4 arm must invoke `readSnapshot(...)` on the "
        + "single listed `.fourvec` file, deep-copy the returned population for its private JVM, and "
        + "record both logical hashes.  Calling `problem.createSolution()` in an arm is forbidden. "
        + "The bridge validates all input SHA-256 values, semantic mode, vector lengths, JS permutation, "
        + "factory/machine/worker domains, and both V35/P8 logical population hashes before returning.\n\n"
        + "## Inputs and manifests\n\n"
        + "- instance manifest SHA-256: `" + instanceHash + "`\n"
        + "- formal seed list SHA-256: `" + seedHash + "`\n"
        + "- initial-population manifest SHA-256: `" + populationHash + "`\n"
        + "- one physical snapshot per instance/seed: `initial-populations/<instance>/seed-<seed>.fourvec`\n"
        + "- materialization/verification use zero decoder evaluations and do not start a 500k run.\n\n"
        + "## Matrix accounting\n\n"
        + "The unique 45-instance matrix is `jobs={20,50,100,150,200}` x `stages={2,5,8}` x "
        + "`factories={3,4,5}`, each with problem id 1.  The 20 seeds are the contiguous pre-registered "
        + "range 20260808--20260827.  The manifest covers 45 x 20 = 900 shared starts, or 4,500 possible "
        + "A0--A4 formal arms; this is a provenance/fairness freeze, not authorization to run them.\n";
    Files.write(root.resolve("FORMAL_FAIRNESS_FREEZE.md"), report.getBytes(StandardCharsets.UTF_8));
  }

  private static void writeSnapshot(Path snapshot, String instanceId, long seed,
      ZhangBoCanonicalProductionProblem problem, List<PermutationSolution<Integer>> population,
      String v35, String p8, String problemHash) throws IOException {
    StringBuilder text = new StringBuilder();
    text.append("schema=").append(SCHEMA).append('\n')
        .append("instanceId=").append(instanceId).append('\n')
        .append("instanceSHA256=").append(problem.getInstance().getInstanceSha256()).append('\n')
        .append("SUTSHA256=").append(problem.getInstance().getInstanceExtensionSha256()).append('\n')
        .append("fatigueParameterSHA256=").append(problem.getParameters().getConfigurationSha256()).append('\n')
        .append("problemConfigurationSHA256=").append(problemHash).append('\n')
        .append("seed=").append(seed).append('\n')
        .append("population=").append(population.size()).append('\n')
        .append("decoderMode=FM3\nfamilyMode=DEGENERATE_SINGLE_FAMILY\nsetupMode=SEQUENCE_INDEPENDENT\nshiftMode=NONE\n")
        .append("semanticTag=").append(problem.getMode().getSemanticTag()).append('\n')
        .append("initialPopulationSHA256=").append(v35).append('\n')
        .append("initialPopulationP8SHA256=").append(p8).append('\n');
    for (int particle = 0; particle < population.size(); particle++) {
      DhhfspFourVectorSolution solution = (DhhfspFourVectorSolution) population.get(particle);
      text.append("particle=").append(particle).append('\n');
      append(text, "JS", solution.getJobSequence());
      append(text, "FA", solution.getFactoryAssignments());
      append(text, "MA", solution.getMachineAssignments());
      append(text, "WA", solution.getWorkerAssignments());
    }
    Files.write(snapshot, text.toString().getBytes(StandardCharsets.UTF_8));
  }

  private static void append(StringBuilder text, String label, List<Integer> vector) {
    text.append(label).append('=');
    for (int index = 0; index < vector.size(); index++) {
      if (index > 0) text.append(',');
      text.append(vector.get(index));
    }
    text.append('\n');
  }

  private static List<Integer> vector(String line, String label, int expected) {
    String prefix = label + '=';
    if (!line.startsWith(prefix)) throw new IllegalArgumentException("expected " + label + " vector");
    String[] tokens = line.substring(prefix.length()).split(",", -1);
    if (tokens.length != expected) throw new IllegalArgumentException(label + " length mismatch");
    List<Integer> result = new ArrayList<>(expected);
    for (String token : tokens) result.add(Integer.parseInt(token));
    return result;
  }

  private static void validate(DhhfspFourVectorSolution solution, ZhangBoFatigueInstanceData instance) {
    boolean[] seen = new boolean[instance.getJobs()];
    for (int position = 0; position < instance.getJobs(); position++) {
      int job = solution.getVariableValue(position);
      int factory = solution.getVariableValueid(position);
      int machine = solution.getMachineAssignment(position);
      int worker = solution.getVariableValueworker(position);
      if (job < 0 || job >= seen.length || seen[job]) throw new IllegalArgumentException("invalid JS");
      seen[job] = true;
      if (factory < 0 || factory >= instance.getFactories()
          || machine < 0 || machine >= instance.getMachineCount(factory, 0)
          || !instance.isWorkerEligible(factory, 0, worker)) {
        throw new IllegalArgumentException("invalid first-stage resource");
      }
    }
  }

  private static String problemConfigurationHash(String instanceId,
      ZhangBoCanonicalProductionProblem problem) {
    String text = "schema=v35-formal-problem-configuration-v1\n"
        + "instanceId=" + instanceId + "\n"
        + "instanceSHA256=" + problem.getInstance().getInstanceSha256() + "\n"
        + "SUTSHA256=" + problem.getInstance().getInstanceExtensionSha256() + "\n"
        + "fatigueParameterSHA256=" + problem.getParameters().getConfigurationSha256() + "\n"
        + "decoderMode=FM3\nfamilyMode=DEGENERATE_SINGLE_FAMILY\n"
        + "setupMode=SEQUENCE_INDEPENDENT\nshiftMode=NONE\nobjectives=0,1,6\n";
    return sha256(text.getBytes(StandardCharsets.UTF_8));
  }

  private static String scale(String instanceId) {
    String[] fields = instanceId.split("_");
    return fields[0] + "x" + fields[1] + "x" + fields[2];
  }

  private static Path javaProject(Path root) {
    if (Files.isDirectory(root.resolve("EADHFSP"))) return root;
    Path nested = root.resolve("java-jmetal58");
    if (Files.isDirectory(nested.resolve("EADHFSP"))) return nested;
    throw new IllegalArgumentException("cannot locate java-jmetal58/EADHFSP from " + root);
  }

  private static List<Long> frozenSeeds() {
    List<Long> result = new ArrayList<>();
    for (long value = 20260808L; value <= 20260827L; value++) result.add(value);
    return Collections.unmodifiableList(result);
  }

  private static void requireFile(Path path) {
    if (!Files.isRegularFile(path)) throw new IllegalStateException("missing frozen input: " + path);
  }

  private static String required(Map<String, String> values, String key) {
    String value = values.get(key);
    if (value == null || value.isEmpty()) throw new IllegalArgumentException("missing " + key);
    return value;
  }

  private static void requireEqual(String label, String expected, String actual) {
    if (!expected.equalsIgnoreCase(actual)) {
      throw new IllegalStateException(label + " mismatch expected=" + expected + " actual=" + actual);
    }
  }

  private static String sha256(Path path) throws IOException { return sha256(Files.readAllBytes(path)); }

  private static String sha256(byte[] bytes) {
    try {
      byte[] digest = MessageDigest.getInstance("SHA-256").digest(bytes);
      StringBuilder result = new StringBuilder();
      for (byte value : digest) result.append(String.format("%02x", value & 0xff));
      return result.toString();
    } catch (Exception error) { throw new IllegalStateException(error); }
  }

  private static void writeEvidenceManifest(Path root) throws Exception {
    TreeMap<String, String> hashes = new TreeMap<>();
    try (java.util.stream.Stream<Path> walk = Files.walk(root)) {
      walk.filter(Files::isRegularFile)
          .filter(path -> !"evidence-sha256.tsv".equals(path.getFileName().toString()))
          .forEach(path -> {
            try { hashes.put(root.relativize(path).toString().replace('\\', '/'), sha256(path)); }
            catch (IOException error) { throw new RuntimeException(error); }
          });
    }
    StringBuilder content = new StringBuilder("path\tsha256\n");
    for (Map.Entry<String, String> entry : hashes.entrySet()) {
      content.append(entry.getKey()).append('\t').append(entry.getValue()).append('\n');
    }
    Files.write(root.resolve("evidence-sha256.tsv"), content.toString().getBytes(StandardCharsets.UTF_8));
  }

  private static void move(Path source, Path target) throws IOException {
    try { Files.move(source, target, StandardCopyOption.ATOMIC_MOVE); }
    catch (AtomicMoveNotSupportedException error) { Files.move(source, target); }
  }

  private static List<Map<String, String>> csv(Path path) throws IOException {
    List<String> lines = Files.readAllLines(path, StandardCharsets.UTF_8);
    if (lines.isEmpty()) throw new IllegalArgumentException("empty csv=" + path);
    String[] headers = lines.get(0).split(",", -1);
    List<Map<String, String>> result = new ArrayList<>();
    for (int index = 1; index < lines.size(); index++) {
      if (lines.get(index).isEmpty()) continue;
      String[] values = lines.get(index).split(",", -1);
      if (values.length != headers.length) throw new IllegalArgumentException("csv field mismatch=" + path);
      Map<String, String> row = new HashMap<>();
      for (int column = 0; column < headers.length; column++) row.put(headers[column], values[column]);
      result.add(row);
    }
    return result;
  }

  public static final class Verification {
    public final int rows;
    private Verification(int rows) { this.rows = rows; }
  }

  private static final class Arguments {
    private String phase = "VERIFY";
    private Path projectRoot;
    private Path output;
    private static Arguments parse(String[] args) {
      Arguments result = new Arguments();
      for (int index = 0; index < args.length; index++) {
        if ("--phase".equals(args[index]) && index + 1 < args.length) result.phase = args[++index].toUpperCase();
        else if ("--project-root".equals(args[index]) && index + 1 < args.length) result.projectRoot = Paths.get(args[++index]);
        else if ("--output".equals(args[index]) && index + 1 < args.length) result.output = Paths.get(args[++index]);
        else throw new IllegalArgumentException("Usage: --phase MATERIALIZE|VERIFY --project-root <path> --output <bundle>");
      }
      if (result.projectRoot == null || result.output == null) {
        throw new IllegalArgumentException("Usage: --phase MATERIALIZE|VERIFY --project-root <path> --output <bundle>");
      }
      return result;
    }
  }
}
