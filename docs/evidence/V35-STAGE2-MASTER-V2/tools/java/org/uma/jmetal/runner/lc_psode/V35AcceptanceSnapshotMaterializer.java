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

/** Creates the one non-formal acceptance snapshot, then verifies it through the frozen reader. */
public final class V35AcceptanceSnapshotMaterializer {
  private static final String INSTANCE = "20_2_3_1";
  private static final long SEED = 20260828L;
  private static final int POPULATION = 100;

  private V35AcceptanceSnapshotMaterializer() { }

  public static void main(String[] args) throws Exception {
    if (args.length != 4 || !"--project-root".equals(args[0]) || !"--output".equals(args[2])) {
      throw new IllegalArgumentException("usage: --project-root <java-project> --output <snapshot>");
    }
    Path root = Paths.get(args[1]).toAbsolutePath().normalize();
    if (!Files.isDirectory(root.resolve("EADHFSP"))) root = root.resolve("java-jmetal58");
    Path output = Paths.get(args[3]).toAbsolutePath().normalize();
    if (Files.exists(output)) throw new IllegalStateException("refusing overwrite: " + output);
    Files.createDirectories(output.getParent());
    Path instance = root.resolve("EADHFSP/" + INSTANCE + ".txt");
    Path setup = root.resolve("instance-extensions/v1");
    Path fatigue = root.resolve("fatigue-parameters/v1");
    ZhangBoCanonicalProductionProblem problem = ZhangBoCanonicalProblemLoader.load(instance,
        ProductionDecodeMode.FM3, SEED, setup, fatigue, ZhangBoShiftConfiguration.none());
    List<PermutationSolution<Integer>> population = new ArrayList<PermutationSolution<Integer>>();
    for (int i = 0; i < POPULATION; i++) population.add(problem.createSolution());
    if (problem.getEvaluationCounter().getSuccessfulEvaluations() != 0L) {
      throw new IllegalStateException("snapshot materialization consumed FE");
    }
    String v35 = V35FairRunner.initialHash(population);
    String p8 = P8InitialPopulationProvider.sha256(population);
    String problemHash = problemHash(problem);
    StringBuilder text = new StringBuilder()
        .append("schema=").append(ZhangBoV35FormalInitialPopulationFreezeRunner.SCHEMA).append('\n')
        .append("instanceId=").append(INSTANCE).append('\n')
        .append("instanceSHA256=").append(problem.getInstance().getInstanceSha256()).append('\n')
        .append("SUTSHA256=").append(problem.getInstance().getInstanceExtensionSha256()).append('\n')
        .append("fatigueParameterSHA256=").append(problem.getParameters().getConfigurationSha256()).append('\n')
        .append("problemConfigurationSHA256=").append(problemHash).append('\n')
        .append("seed=").append(SEED).append('\n').append("population=").append(POPULATION).append('\n')
        .append("decoderMode=FM3\nfamilyMode=DEGENERATE_SINGLE_FAMILY\n")
        .append("setupMode=SEQUENCE_INDEPENDENT\nshiftMode=NONE\n")
        .append("semanticTag=").append(problem.getMode().getSemanticTag()).append('\n')
        .append("initialPopulationSHA256=").append(v35).append('\n')
        .append("initialPopulationP8SHA256=").append(p8).append('\n');
    for (int i = 0; i < population.size(); i++) {
      DhhfspFourVectorSolution solution = (DhhfspFourVectorSolution) population.get(i);
      text.append("particle=").append(i).append('\n');
      vector(text, "JS", solution.getJobSequence());
      vector(text, "FA", solution.getFactoryAssignments());
      vector(text, "MA", solution.getMachineAssignments());
      vector(text, "WA", solution.getWorkerAssignments());
    }
    Files.write(output, text.toString().getBytes(StandardCharsets.UTF_8));
    List<PermutationSolution<Integer>> reloaded =
        ZhangBoV35FormalInitialPopulationFreezeRunner.readSnapshot(output, problem);
    if (!v35.equals(V35FairRunner.initialHash(reloaded))
        || !p8.equals(P8InitialPopulationProvider.sha256(reloaded))) {
      throw new IllegalStateException("acceptance snapshot round-trip mismatch");
    }
    Path manifest = output.resolveSibling("ACCEPTANCE_SNAPSHOT.properties");
    String evidence = "schema=v35-launcher-acceptance-snapshot-v1\nlauncherAcceptanceOnly=true\n"
        + "includedInFormalStatistics=false\nincludedInReferenceFront=false\ninstance=" + INSTANCE
        + "\nseed=" + SEED + "\npopulation=" + POPULATION + "\nsnapshotSha256=" + sha256(output)
        + "\ninitialPopulationHashV35=" + v35 + "\ninitialPopulationHashP8=" + p8
        + "\nproblemConfigurationSha256=" + problemHash + "\n";
    Files.write(manifest, evidence.getBytes(StandardCharsets.UTF_8));
    System.out.println("V35_ACCEPTANCE_SNAPSHOT_COMPLETED snapshot=" + output);
  }

  private static void vector(StringBuilder text, String label, List<Integer> values) {
    text.append(label).append('=');
    for (int i = 0; i < values.size(); i++) { if (i > 0) text.append(','); text.append(values.get(i)); }
    text.append('\n');
  }
  private static String problemHash(ZhangBoCanonicalProductionProblem problem) throws Exception {
    String text = "schema=v35-formal-problem-configuration-v1\ninstanceId=" + INSTANCE
        + "\ninstanceSHA256=" + problem.getInstance().getInstanceSha256()
        + "\nSUTSHA256=" + problem.getInstance().getInstanceExtensionSha256()
        + "\nfatigueParameterSHA256=" + problem.getParameters().getConfigurationSha256()
        + "\ndecoderMode=FM3\nfamilyMode=DEGENERATE_SINGLE_FAMILY\n"
        + "setupMode=SEQUENCE_INDEPENDENT\nshiftMode=NONE\nobjectives=0,1,6\n";
    return sha256(text.getBytes(StandardCharsets.UTF_8));
  }
  private static String sha256(Path path) throws Exception { return sha256(Files.readAllBytes(path)); }
  private static String sha256(byte[] data) throws Exception {
    byte[] digest = MessageDigest.getInstance("SHA-256").digest(data);
    StringBuilder out = new StringBuilder(); for (byte b : digest) out.append(String.format(Locale.ROOT, "%02x", b & 0xff)); return out.toString();
  }
}
