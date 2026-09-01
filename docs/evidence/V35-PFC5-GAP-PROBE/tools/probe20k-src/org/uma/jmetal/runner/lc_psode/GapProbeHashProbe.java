package org.uma.jmetal.runner.lc_psode;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import org.uma.jmetal.algorithm.multiobjective.mypso.p8.P8InitialPopulationProvider;
import org.uma.jmetal.algorithm.multiobjective.mypso.v35.V35FairRunner;
import org.uma.jmetal.algorithm.multiobjective.mypso.v35.V35FinalAblationProfile;
import org.uma.jmetal.problem.multiobjective.dfsp.decoder.ProductionDecodeMode;
import org.uma.jmetal.problem.multiobjective.dfsp.fatigue.ZhangBoCanonicalProblemLoader;
import org.uma.jmetal.problem.multiobjective.dfsp.fatigue.ZhangBoCanonicalProductionProblem;
import org.uma.jmetal.problem.multiobjective.dfsp.fatigue.shift.ZhangBoShiftConfiguration;
import org.uma.jmetal.problem.multiobjective.dfsp.model.DhhfspFourVectorSolution;
import org.uma.jmetal.solution.PermutationSolution;

/** Zero-FE probe: deterministic arm-profile hashes, snapshot hashes, and
 * gap-probe-local snapshot materialization (same schema as the formal freeze). */
public final class GapProbeHashProbe {
  private static final long[] SEEDS = {20260827L, 20260906L};

  public static void main(String[] args) throws Exception {
    String mode = args[0];
    if ("profiles".equals(mode)) {
      for (String armName : new String[] {"A0", "A4"}) {
        V35FinalAblationProfile.Arm arm = arm(armName);
        for (long seed : SEEDS) {
          for (int fes : new int[] {20000}) {
            System.out.println("armProfile|" + armName + "|" + seed + "|" + fes + "|"
                + V35FinalAblationProfile.configurationHashFor(arm, seed, 100, fes));
            System.out.println("runtimeConfig|" + armName + "|" + seed + "|" + fes + "|"
                + V35FinalAblationProfile.configurationFor(arm, seed, 100, fes)
                    .configurationHash());
          }
        }
      }
      return;
    }
    String project = args[1];
    if ("snapshots".equals(mode)) {
      for (String instance : new String[] {"50_2_3_1", "100_5_3_1"}) {
        for (long seed : SEEDS) {
          ZhangBoCanonicalProductionProblem problem = load(project, instance, seed);
          List<PermutationSolution<Integer>> initial = read(project, instance, seed, problem);
          System.out.println("snapshotHash|" + instance + "|" + seed + "|"
              + V35FairRunner.initialHash(initial) + "|"
              + P8InitialPopulationProvider.sha256(initial) + "|"
              + problem.getInstance().getInstanceSha256() + "|"
              + problem.getInstance().getInstanceExtensionSha256() + "|"
              + problem.getParameters().getConfigurationSha256());
        }
      }
      return;
    }
    if ("materialize".equals(mode)) {
      Path outDir = Paths.get(args[2]).toAbsolutePath().normalize();
      Files.createDirectories(outDir);
      for (String instance : new String[] {"50_2_3_1", "100_5_3_1"}) {
        long seed = 20260906L;
        ZhangBoCanonicalProductionProblem problem = load(project, instance, seed);
        List<PermutationSolution<Integer>> population = new ArrayList<>();
        for (int i = 0; i < 100; i++) population.add(problem.createSolution());
        String v35 = V35FairRunner.initialHash(population);
        String p8 = P8InitialPopulationProvider.sha256(population);
        String problemHash = problemConfigurationHash(instance, problem);
        Path target = outDir.resolve(instance + "-seed-20260906.fourvec");
        writeSnapshot(target, instance, seed, problem, population, v35, p8, problemHash);
        List<PermutationSolution<Integer>> reloaded = ZhangBoV35FormalInitialPopulationFreezeRunner
            .readSnapshot(target, problem);
        if (!v35.equals(V35FairRunner.initialHash(reloaded))
            || !p8.equals(P8InitialPopulationProvider.sha256(reloaded))) {
          throw new IllegalStateException("reload hash mismatch: " + target);
        }
        System.out.println("materialized|" + instance + "|" + sha256(target) + "|" + v35);
      }
      return;
    }
    throw new IllegalArgumentException("mode must be profiles|snapshots|materialize");
  }

  private static void writeSnapshot(Path snapshot, String instanceId, long seed,
      ZhangBoCanonicalProductionProblem problem, List<PermutationSolution<Integer>> population,
      String v35, String p8, String problemHash) throws Exception {
    StringBuilder text = new StringBuilder();
    text.append("schema=v35-formal-initial-population-v1\n")
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

  private static String problemConfigurationHash(String instanceId,
      ZhangBoCanonicalProductionProblem problem) throws Exception {
    String text = "schema=v35-formal-problem-configuration-v1\n"
        + "instanceId=" + instanceId + "\n"
        + "instanceSHA256=" + problem.getInstance().getInstanceSha256() + "\n"
        + "SUTSHA256=" + problem.getInstance().getInstanceExtensionSha256() + "\n"
        + "fatigueParameterSHA256=" + problem.getParameters().getConfigurationSha256() + "\n"
        + "decoderMode=FM3\nfamilyMode=DEGENERATE_SINGLE_FAMILY\n"
        + "setupMode=SEQUENCE_INDEPENDENT\nshiftMode=NONE\nobjectives=0,1,6\n";
    return sha256(text.getBytes(StandardCharsets.UTF_8));
  }

  private static ZhangBoCanonicalProductionProblem load(String project, String instance,
      long seed) throws Exception {
    return ZhangBoCanonicalProblemLoader.load(
        Paths.get(project, "EADHFSP", instance + ".txt"), ProductionDecodeMode.FM3, seed,
        Paths.get(project, "instance-extensions", "v1"),
        Paths.get(project, "fatigue-parameters", "v1"),
        ZhangBoShiftConfiguration.none());
  }

  private static List<PermutationSolution<Integer>> read(String project, String instance,
      long seed, ZhangBoCanonicalProductionProblem problem) throws Exception {
    Path snapshot = Paths.get(project, "..", "docs", "evidence", "V35-FORMAL-MANIFEST",
        "initial-populations", instance, "seed-" + seed + ".fourvec").normalize();
    if (!Files.exists(snapshot)) {
      snapshot = Paths.get(project, "..", "docs", "evidence", "V35-PFC5-GAP-PROBE",
          "tools", "snapshots-local", instance + "-seed-" + seed + ".fourvec").normalize();
    }
    return ZhangBoV35FormalInitialPopulationFreezeRunner.readSnapshot(snapshot, problem);
  }

  private static String sha256(Path path) throws Exception {
    return sha256(Files.readAllBytes(path));
  }

  private static String sha256(byte[] bytes) throws Exception {
    java.security.MessageDigest digest = java.security.MessageDigest.getInstance("SHA-256");
    StringBuilder builder = new StringBuilder();
    for (byte value : digest.digest(bytes)) {
      builder.append(String.format("%02x", value));
    }
    return builder.toString();
  }

  static V35FinalAblationProfile.Arm arm(String label) {
    for (V35FinalAblationProfile.Arm value : V35FinalAblationProfile.Arm.values()) {
      if (value.getLabel().equals(label)) return value;
    }
    throw new IllegalArgumentException("arm must be A0,A1,A2,A3,A4: " + label);
  }
}
