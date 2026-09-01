package org.uma.jmetal.runner.lc_psode;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import org.uma.jmetal.algorithm.multiobjective.mypso.p8.P8InitialPopulationProvider;
import org.uma.jmetal.algorithm.multiobjective.mypso.v35.V35FairRunner;
import org.uma.jmetal.problem.multiobjective.dfsp.decoder.ProductionDecodeMode;
import org.uma.jmetal.problem.multiobjective.dfsp.fatigue.ZhangBoCanonicalProblemLoader;
import org.uma.jmetal.problem.multiobjective.dfsp.fatigue.ZhangBoCanonicalProductionProblem;
import org.uma.jmetal.problem.multiobjective.dfsp.fatigue.shift.ZhangBoShiftConfiguration;
import org.uma.jmetal.problem.multiobjective.dfsp.model.DhhfspFourVectorSolution;
import org.uma.jmetal.solution.PermutationSolution;

/**
 * V35-GAP-LOCAL-FE-PACING-REPAIR-V1 zero-FE snapshot materializer.
 *
 * <p>Deterministic four-vector initial populations for the repair gate seeds,
 * byte-format identical to the formal freeze runner schema
 * ({@code v35-formal-initial-population-v1}: header + per-particle
 * JS/FA/MA/WA lines). Performs zero decoder evaluations; the reload
 * verification path is the same readSnapshot used by the runner.</p>
 *
 * <p>CLI: {@code --project-root <java-jmetal58 dir> --instance <name>
 * --seed <long> --output <snapshot file>}</p>
 */
public final class V35RepairSnapshotMaterializer {

  private V35RepairSnapshotMaterializer() { }

  public static void main(String[] args) throws Exception {
    String instance = null;
    Long seed = null;
    Path projectRoot = null;
    Path output = null;
    for (int index = 0; index < args.length; index += 2) {
      if (index + 1 >= args.length) throw usage();
      if ("--project-root".equals(args[index])) projectRoot = Paths.get(args[index + 1]);
      else if ("--instance".equals(args[index])) instance = args[index + 1];
      else if ("--seed".equals(args[index])) seed = Long.parseLong(args[index + 1]);
      else if ("--output".equals(args[index])) output = Paths.get(args[index + 1]);
      else throw usage();
    }
    if (instance == null || seed == null || projectRoot == null || output == null) {
      throw usage();
    }
    Path javaProject = projectRoot.toAbsolutePath().normalize();
    Path instanceFile = javaProject.resolve("EADHFSP").resolve(instance + ".txt");
    Path setupDir = javaProject.resolve("instance-extensions/v1");
    Path fatigueDir = javaProject.resolve("fatigue-parameters/v1");
    if (!Files.isRegularFile(instanceFile)
        || !Files.isRegularFile(setupDir.resolve(instance + ".setup.txt"))
        || !Files.isRegularFile(fatigueDir.resolve(instance + ".fatigue.txt"))) {
      throw new IllegalArgumentException("missing instance/extension/fatigue for " + instance);
    }
    ZhangBoCanonicalProductionProblem problem = ZhangBoCanonicalProblemLoader.load(
        instanceFile, ProductionDecodeMode.FM3, seed, setupDir, fatigueDir,
        ZhangBoShiftConfiguration.none());
    List<PermutationSolution<Integer>> population = new ArrayList<>();
    for (int i = 0; i < 100; i++) population.add(problem.createSolution());
    if (problem.getEvaluationCounter().getSuccessfulEvaluations() != 0L) {
      throw new IllegalStateException("materialization consumed FE");
    }
    String v35 = V35FairRunner.initialHash(population);
    String p8 = P8InitialPopulationProvider.sha256(population);
    String problemHash = problemConfigurationHash(instance, problem);
    Path target = output.toAbsolutePath().normalize();
    Files.createDirectories(target.getParent());
    if (Files.exists(target)) throw new IllegalStateException("refusing overwrite: " + target);

    StringBuilder text = new StringBuilder();
    text.append("schema=v35-formal-initial-population-v1\n")
        .append("instanceId=").append(instance).append('\n')
        .append("instanceSHA256=").append(problem.getInstance().getInstanceSha256()).append('\n')
        .append("SUTSHA256=").append(problem.getInstance().getInstanceExtensionSha256()).append('\n')
        .append("fatigueParameterSHA256=").append(problem.getParameters().getConfigurationSha256())
        .append('\n')
        .append("problemConfigurationSHA256=").append(problemHash).append('\n')
        .append("seed=").append(seed).append('\n')
        .append("population=").append(population.size()).append('\n')
        .append("decoderMode=FM3\nfamilyMode=DEGENERATE_SINGLE_FAMILY\n")
        .append("setupMode=SEQUENCE_INDEPENDENT\nshiftMode=NONE\n")
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
    byte[] bytes = text.toString().getBytes(StandardCharsets.UTF_8);
    Files.write(target, bytes);

    List<PermutationSolution<Integer>> reloaded =
        ZhangBoV35FormalInitialPopulationFreezeRunner.readSnapshot(target, problem);
    if (!v35.equals(V35FairRunner.initialHash(reloaded))
        || !p8.equals(P8InitialPopulationProvider.sha256(reloaded))) {
      throw new IllegalStateException("reload hash mismatch: " + target);
    }
    if (problem.getEvaluationCounter().getSuccessfulEvaluations() != 0L) {
      throw new IllegalStateException("reload verification consumed FE");
    }
    System.out.println("materialized|" + instance + "|" + seed + "|"
        + sha256Hex(bytes) + "|" + v35 + "|" + p8);
  }

  private static void append(StringBuilder text, String label, List<Integer> vector) {
    text.append(label).append('=');
    for (int index = 0; index < vector.size(); index++) {
      if (index > 0) text.append(',');
      text.append(vector.get(index));
    }
    text.append('\n');
  }

  private static String problemConfigurationHash(String instanceId,
      ZhangBoCanonicalProductionProblem problem) throws Exception {
    String text = "schema=v35-formal-problem-configuration-v1\n"
        + "instanceId=" + instanceId + "\n"
        + "instanceSHA256=" + problem.getInstance().getInstanceSha256() + "\n"
        + "SUTSHA256=" + problem.getInstance().getInstanceExtensionSha256() + "\n"
        + "fatigueParameterSHA256=" + problem.getParameters().getConfigurationSha256() + "\n"
        + "decoderMode=FM3\nfamilyMode=DEGENERATE_SINGLE_FAMILY\n"
        + "setupMode=SEQUENCE_INDEPENDENT\nshiftMode=NONE\nobjectives=0,1,6\n";
    return sha256Hex(text.getBytes(StandardCharsets.UTF_8));
  }

  private static String sha256Hex(byte[] bytes) throws Exception {
    byte[] digest = java.security.MessageDigest.getInstance("SHA-256").digest(bytes);
    StringBuilder out = new StringBuilder();
    for (byte b : digest) out.append(String.format("%02x", b & 0xff));
    return out.toString();
  }

  private static IllegalArgumentException usage() {
    return new IllegalArgumentException(
        "usage: --project-root <dir> --instance <name> --seed <long> --output <file>");
  }
}
