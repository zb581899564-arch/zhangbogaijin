package org.uma.jmetal.runner.lc_psode;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.uma.jmetal.algorithm.multiobjective.mypso.p8.P8AblationProfile;
import org.uma.jmetal.algorithm.multiobjective.mypso.p8.P8EvidenceWriter;
import org.uma.jmetal.algorithm.multiobjective.mypso.p8.P8ExperimentRegistry;
import org.uma.jmetal.algorithm.multiobjective.mypso.p8.P8ExperimentSpec;
import org.uma.jmetal.algorithm.multiobjective.mypso.p8.P8RunRecord;
import org.uma.jmetal.algorithm.multiobjective.mypso.p8.P8V3ExperimentRunner;
import org.uma.jmetal.problem.PermutationProblem;
import org.uma.jmetal.problem.multiobjective.dfsp.decoder.ProductionDecodeMode;
import org.uma.jmetal.problem.multiobjective.dfsp.fatigue.ZhangBoCanonicalProblemLoader;
import org.uma.jmetal.problem.multiobjective.dfsp.fatigue.ZhangBoCanonicalProductionProblem;
import org.uma.jmetal.problem.multiobjective.dfsp.model.P8GoldenAuthorCompatibilityBridge;
import org.uma.jmetal.solution.PermutationSolution;

/**
 * Executes the P8.4-shift canonical engineering matrix.
 *
 * <p>The author diagnostic implementation is deliberately absent from this runner.  It runs
 * exactly 34 formal labels over two instances and three seeds, and never starts P9's 500000-FE
 * experiments.</p>
 */
public final class ZhangBoP8EngineeringRunner {
  private ZhangBoP8EngineeringRunner() { }

  public static void main(String[] args) throws Exception {
    Path project = Paths.get(".").toAbsolutePath().normalize();
    if (!Files.isDirectory(project.resolve("EADHFSP"))) {
      throw new IllegalStateException("Run P8 from the java-jmetal58 directory: " + project);
    }

    List<P8ExperimentSpec> specs = P8ExperimentRegistry.currentMatrix();
    P8ExperimentRegistry.assertCurrentMatrix(specs);
    Path evidence = project.getParent().resolve("docs/evidence/P8-v4-shift");
    Files.createDirectories(evidence);

    Path bridgeRoot = project.resolve("p8-bridge/v1");
    P8GoldenAuthorCompatibilityBridge.Manifest bridge =
        P8GoldenAuthorCompatibilityBridge.materialize(bridgeRoot);
    Files.copy(bridge.root.resolve("bridge-manifest.txt"),
        evidence.resolve("golden-bridge-manifest.txt"),
        StandardCopyOption.REPLACE_EXISTING);

    List<P8V3ExperimentRunner.InstanceBinding> bindings = Arrays.asList(
        new CanonicalBinding("chapter4-golden-author-bridge",
            bridge.root.resolve("EADHFSP/10_2_2_1.txt"),
            bridge.root.resolve("instance-extensions/v1"),
            bridge.root.resolve("fatigue-parameters/v1"), bridge.instanceSha256),
        new CanonicalBinding("20_2_3_1", project.resolve("EADHFSP/20_2_3_1.txt"),
            project.resolve("instance-extensions/v1"),
            project.resolve("fatigue-parameters/v1"),
            sha256(Files.readAllBytes(project.resolve("EADHFSP/20_2_3_1.txt")))));

    List<P8RunRecord> records = P8V3ExperimentRunner.run(
        bindings, P8V3ExperimentRunner.DEFAULT_SEEDS);
    P8EvidenceWriter.write(evidence, specs, records);
    System.out.println("P8_V3_EVIDENCE " + evidence + " records=" + records.size());
  }

  private static ProductionDecodeMode productionMode(P8AblationProfile.DecoderMode mode) {
    switch (mode) {
      case DETERMINISTIC_CANONICAL:
      case CORRECTED_NO_FATIGUE:
        return ProductionDecodeMode.CANONICAL_NO_FATIGUE;
      case ACCUMULATION_ONLY:
        return ProductionDecodeMode.FM1;
      case ACCUMULATION_RECOVERY:
        return ProductionDecodeMode.FM2;
      case FATIGUE_AWARE_SELECTION:
        return ProductionDecodeMode.FM3;
      case AUTHOR_DIAGNOSTIC:
      case AUTHOR_ACTUAL:
      default:
        throw new IllegalArgumentException(
            "Author diagnostic decoder is forbidden in the formal shift runner: " + mode);
    }
  }

  private static String sha256(byte[] value) {
    try {
      byte[] bytes = MessageDigest.getInstance("SHA-256").digest(value);
      StringBuilder result = new StringBuilder();
      for (byte item : bytes) result.append(String.format("%02x", item & 0xff));
      return result.toString();
    } catch (NoSuchAlgorithmException exception) {
      throw new IllegalStateException("SHA-256 unavailable", exception);
    }
  }

  private static final class CanonicalBinding implements P8V3ExperimentRunner.InstanceBinding {
    private final String name;
    private final Path instance;
    private final Path extensionDirectory;
    private final Path fatigueDirectory;
    private final String instanceSha256;

    private CanonicalBinding(String name, Path instance, Path extensionDirectory,
        Path fatigueDirectory, String instanceSha256) {
      this.name = name;
      this.instance = instance;
      this.extensionDirectory = extensionDirectory;
      this.fatigueDirectory = fatigueDirectory;
      this.instanceSha256 = instanceSha256;
    }

    @Override public String getName() { return name; }
    @Override public String getInstanceSha256() { return instanceSha256; }

    @Override
    public PermutationProblem<PermutationSolution<Integer>> createProblem(
        P8ExperimentSpec spec, long seed) {
      try {
        return ZhangBoCanonicalProblemLoader.load(instance,
            productionMode(spec.getAblationProfile().getDecoderMode()), seed,
            extensionDirectory, fatigueDirectory,
            spec.getAblationProfile().getShiftConfiguration());
      } catch (Exception exception) {
        throw new IllegalStateException("Cannot bind canonical P8 problem " + name
            + " for " + spec.getLabel(), exception);
      }
    }

    @Override
    public List<PermutationSolution<Integer>> createInitialPopulation(
        P8ExperimentSpec spec, long seed) {
      ZhangBoCanonicalProductionProblem problem = (ZhangBoCanonicalProductionProblem)
          createProblem(spec, seed);
      List<PermutationSolution<Integer>> result = new ArrayList<>(
          P8ExperimentRegistry.DEFAULT_POPULATION);
      for (int index = 0; index < P8ExperimentRegistry.DEFAULT_POPULATION; index++) {
        result.add(problem.createSolution());
      }
      return result;
    }
  }
}
