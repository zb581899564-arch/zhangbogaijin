package org.uma.jmetal.runner.lc_psode;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import org.uma.jmetal.algorithm.multiobjective.mypso.p8.P8InitialPopulationProvider;
import org.uma.jmetal.algorithm.multiobjective.mypso.v35.V35FairRunner;
import org.uma.jmetal.problem.multiobjective.dfsp.decoder.ProductionDecodeMode;
import org.uma.jmetal.problem.multiobjective.dfsp.fatigue.ZhangBoCanonicalProblemLoader;
import org.uma.jmetal.problem.multiobjective.dfsp.fatigue.ZhangBoCanonicalProductionProblem;
import org.uma.jmetal.problem.multiobjective.dfsp.fatigue.shift.ZhangBoShiftConfiguration;
import org.uma.jmetal.problem.multiobjective.dfsp.model.DhhfspFourVectorSolution;
import org.uma.jmetal.solution.PermutationSolution;

/** Zero-FE snapshot materializer restricted to the pre-registered A0/A2 candidate roster. */
public final class V35A2FinalCandidateSnapshotMaterializer {
  private static final int POPULATION = 100;

  private V35A2FinalCandidateSnapshotMaterializer() { }

  public static void main(String[] args) throws Exception {
    if (args.length != 8 || !"--project-root".equals(args[0]) || !"--instance".equals(args[2])
        || !"--seed".equals(args[4]) || !"--output".equals(args[6])) {
      throw new IllegalArgumentException("usage: --project-root <root> --instance <id> --seed <seed> --output <file>");
    }
    materialize(Paths.get(args[1]), args[3], Long.parseLong(args[5]), Paths.get(args[7]));
  }

  @SuppressWarnings({"rawtypes", "unchecked"})
  static void materialize(Path projectRoot, String instanceId, long seed, Path output) throws Exception {
    if (!allowedInstance(instanceId) || seed < 20260911L || seed > 20260915L) {
      throw new IllegalArgumentException("outside pre-registered A2 final-candidate roster");
    }
    Path javaProject = javaProject(projectRoot);
    Path instance = javaProject.resolve("EADHFSP").resolve(instanceId + ".txt");
    Path setupDirectory = javaProject.resolve("instance-extensions").resolve("v1");
    Path fatigueDirectory = javaProject.resolve("fatigue-parameters").resolve("v1");
    if (!Files.isRegularFile(instance) || !Files.isRegularFile(setupDirectory.resolve(instanceId + ".setup.txt"))
        || !Files.isRegularFile(fatigueDirectory.resolve(instanceId + ".fatigue.txt"))) {
      throw new IllegalStateException("missing frozen input for " + instanceId);
    }
    Path target = output.toAbsolutePath().normalize();
    if (Files.exists(target)) throw new IllegalStateException("refusing overwrite: " + target);
    Files.createDirectories(target.getParent());
    ZhangBoCanonicalProductionProblem problem = ZhangBoCanonicalProblemLoader.load(instance,
        ProductionDecodeMode.FM3, seed, setupDirectory, fatigueDirectory, ZhangBoShiftConfiguration.none());
    List<PermutationSolution<Integer>> population = new ArrayList<PermutationSolution<Integer>>();
    for (int index = 0; index < POPULATION; index++) population.add(problem.createSolution());
    if (problem.getEvaluationCounter().getSuccessfulEvaluations() != 0L) {
      throw new IllegalStateException("snapshot materialization consumed FE");
    }
    String v35 = V35FairRunner.initialHash(population);
    String p8 = P8InitialPopulationProvider.sha256(population);
    String problemHash = problemHash(instanceId, problem);
    writeSnapshot(target, instanceId, seed, problem, population, v35, p8, problemHash);
    List<PermutationSolution<Integer>> reloaded =
        ZhangBoV35FormalInitialPopulationFreezeRunner.readSnapshot(target, problem);
    if (!v35.equals(V35FairRunner.initialHash(reloaded))
        || !p8.equals(P8InitialPopulationProvider.sha256(reloaded))
        || problem.getEvaluationCounter().getSuccessfulEvaluations() != 0L) {
      throw new IllegalStateException("snapshot round trip failed");
    }
    String receipt = "schema=v35-a2-final-candidate-snapshot-v1\ninstance=" + instanceId
        + "\nseed=" + seed + "\npopulation=" + POPULATION + "\nsnapshotSha256=" + sha256(target)
        + "\ninitialPopulationHashV35=" + v35 + "\ninitialPopulationHashP8=" + p8
        + "\ninstanceSha256=" + problem.getInstance().getInstanceSha256()
        + "\nsetupConfigurationSha256=" + problem.getInstance().getInstanceExtensionSha256()
        + "\nfatigueConfigurationSha256=" + problem.getParameters().getConfigurationSha256()
        + "\nproblemConfigurationSha256=" + problemHash + "\nevaluationsDuringMaterialization=0\n";
    Files.write(target.resolveSibling(target.getFileName() + ".receipt.properties"),
        receipt.getBytes(StandardCharsets.UTF_8));
    System.out.println("V35_A2_FINAL_CANDIDATE_SNAPSHOT_COMPLETED instance=" + instanceId + " seed=" + seed);
  }

  private static void writeSnapshot(Path target, String instanceId, long seed,
      ZhangBoCanonicalProductionProblem problem, List<PermutationSolution<Integer>> population,
      String v35, String p8, String problemHash) throws Exception {
    StringBuilder text = new StringBuilder()
        .append("schema=").append(ZhangBoV35FormalInitialPopulationFreezeRunner.SCHEMA).append('\n')
        .append("instanceId=").append(instanceId).append('\n')
        .append("instanceSHA256=").append(problem.getInstance().getInstanceSha256()).append('\n')
        .append("SUTSHA256=").append(problem.getInstance().getInstanceExtensionSha256()).append('\n')
        .append("fatigueParameterSHA256=").append(problem.getParameters().getConfigurationSha256()).append('\n')
        .append("problemConfigurationSHA256=").append(problemHash).append('\n')
        .append("seed=").append(seed).append('\n').append("population=").append(population.size()).append('\n')
        .append("decoderMode=FM3\nfamilyMode=DEGENERATE_SINGLE_FAMILY\n")
        .append("setupMode=SEQUENCE_INDEPENDENT\nshiftMode=NONE\n")
        .append("semanticTag=").append(problem.getMode().getSemanticTag()).append('\n')
        .append("initialPopulationSHA256=").append(v35).append('\n')
        .append("initialPopulationP8SHA256=").append(p8).append('\n');
    for (int particle = 0; particle < population.size(); particle++) {
      DhhfspFourVectorSolution solution = (DhhfspFourVectorSolution) population.get(particle);
      text.append("particle=").append(particle).append('\n');
      vector(text, "JS", solution.getJobSequence());
      vector(text, "FA", solution.getFactoryAssignments());
      vector(text, "MA", solution.getMachineAssignments());
      vector(text, "WA", solution.getWorkerAssignments());
    }
    Files.write(target, text.toString().getBytes(StandardCharsets.UTF_8));
  }

  private static void vector(StringBuilder text, String label, List<Integer> values) {
    text.append(label).append('=');
    for (int index = 0; index < values.size(); index++) {
      if (index > 0) text.append(',');
      text.append(values.get(index));
    }
    text.append('\n');
  }

  private static String problemHash(String instanceId, ZhangBoCanonicalProductionProblem problem) throws Exception {
    String text = "schema=v35-formal-problem-configuration-v1\ninstanceId=" + instanceId
        + "\ninstanceSHA256=" + problem.getInstance().getInstanceSha256()
        + "\nSUTSHA256=" + problem.getInstance().getInstanceExtensionSha256()
        + "\nfatigueParameterSHA256=" + problem.getParameters().getConfigurationSha256()
        + "\ndecoderMode=FM3\nfamilyMode=DEGENERATE_SINGLE_FAMILY\n"
        + "setupMode=SEQUENCE_INDEPENDENT\nshiftMode=NONE\nobjectives=0,1,6\n";
    return sha256(text.getBytes(StandardCharsets.UTF_8));
  }

  private static Path javaProject(Path root) {
    Path normalized = root.toAbsolutePath().normalize();
    if (Files.isDirectory(normalized.resolve("EADHFSP"))) return normalized;
    Path nested = normalized.resolve("java-jmetal58");
    if (Files.isDirectory(nested.resolve("EADHFSP"))) return nested;
    throw new IllegalArgumentException("cannot locate java-jmetal58/EADHFSP from " + root);
  }

  private static boolean allowedInstance(String value) {
    return "20_2_5_1".equals(value) || "20_8_3_1".equals(value)
        || "50_2_5_1".equals(value) || "50_8_3_1".equals(value)
        || "100_2_5_1".equals(value) || "100_8_3_1".equals(value);
  }

  private static String sha256(Path path) throws Exception { return sha256(Files.readAllBytes(path)); }
  private static String sha256(byte[] bytes) throws Exception {
    byte[] digest = MessageDigest.getInstance("SHA-256").digest(bytes);
    StringBuilder out = new StringBuilder();
    for (byte value : digest) out.append(String.format(Locale.ROOT, "%02x", value & 0xff));
    return out.toString();
  }
}
