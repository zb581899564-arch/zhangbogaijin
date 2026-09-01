package org.uma.jmetal.runner.lc_psode;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.uma.jmetal.algorithm.multiobjective.mypso.v35.V35SubSwarmMixture;
import org.uma.jmetal.algorithm.multiobjective.mypso.v35.V35SubSwarmMixtureDesign;

/**
 * Held-out confirmation entry point.  RUN executes exactly one physical run;
 * the deployment script must start a fresh JVM for every arm/instance/seed.
 */
public final class ZhangBoV35Doe1ConfirmationRunner {
  private static final String[] INSTANCES = {"20_5_4_1", "50_5_4_1", "100_5_4_1"};
  private static final long[] SEEDS = {20260901L, 20260902L, 20260903L, 20260904L, 20260905L};
  private static final Map<String, Arm> ARMS = arms();

  private ZhangBoV35Doe1ConfirmationRunner() { }

  public static void main(String[] args) throws Exception {
    Arguments arguments = Arguments.parse(args);
    Path project = arguments.projectRoot.toAbsolutePath().normalize();
    Path javaProject = Files.isDirectory(project.resolve("EADHFSP")) ? project : project.resolve("java-jmetal58");
    Path output = arguments.output.toAbsolutePath().normalize();
    if ("REGISTRY".equals(arguments.phase)) {
      Files.createDirectories(output);
      writeRegistry(output);
      System.out.println("V35_DOE1_HELDOUT_REGISTRY_WRITTEN runs=60");
      return;
    }
    if (!"RUN".equals(arguments.phase)) throw new IllegalArgumentException("--phase REGISTRY|RUN");
    Arm arm = ARMS.get(arguments.arm);
    if (arm == null) throw new IllegalArgumentException("--arm BASE|T1|T2|T3 required");
    if (!Arrays.asList(INSTANCES).contains(arguments.instance)) throw new IllegalArgumentException("held-out instance required");
    if (!containsSeed(arguments.seed)) throw new IllegalArgumentException("held-out seed required");
    Files.createDirectories(output);
    ZhangBoV35Doe1MixtureRunner.RunResult result = ZhangBoV35Doe1MixtureRunner.runHeldout(
        javaProject, output, arm.name, arm.treatmentIndex, arm.mixture, arguments.instance, arguments.seed);
    if (result.record.getFullEvaluations() != ZhangBoV35Doe1MixtureRunner.FORMAL_FES
        || result.record.getDecoderCalls() != result.record.getFullEvaluations()
        || result.record.getIllegalSolutions() != 0 || result.record.getDuplicateEvaluations() != 0) {
      throw new IllegalStateException("held-out hard gate failed: " + result.csvRow());
    }
    System.out.println("V35_DOE1_HELDOUT_RUN_COMPLETED arm=" + arm.name + " " + result.csvRow());
  }

  private static Map<String, Arm> arms() {
    List<V35SubSwarmMixture> selected = V35SubSwarmMixtureDesign.select15().getTreatments();
    Map<String, Arm> values = new LinkedHashMap<>();
    values.put("BASE", arm("BASE", selected, 20, 40, 20, 20));
    values.put("T1", arm("T1", selected, 30, 50, 10, 10));
    values.put("T2", arm("T2", selected, 25, 25, 25, 25));
    values.put("T3", arm("T3", selected, 20, 40, 30, 10));
    return Collections.unmodifiableMap(values);
  }

  private static Arm arm(String name, List<V35SubSwarmMixture> selected, int g1, int g4, int g2, int g3) {
    V35SubSwarmMixture mixture = V35SubSwarmMixture.of(g1, g4, g2, g3);
    int index = selected.indexOf(mixture);
    if (index < 0) throw new IllegalStateException("registered treatment missing: " + mixture);
    return new Arm(name, index, mixture);
  }

  private static boolean containsSeed(long value) {
    for (long seed : SEEDS) if (seed == value) return true;
    return false;
  }

  private static void writeRegistry(Path output) throws Exception {
    List<String> rows = new ArrayList<>();
    rows.add("runId,arm,treatmentIndex,mixture,instance,seed,population,maxFEs,requiredPddr,requiredOrder,status");
    int ordinal = 1;
    for (Arm arm : ARMS.values()) for (String instance : INSTANCES) for (long seed : SEEDS) {
      rows.add(String.format("H%03d,%s,%d,%s,%s,%d,100,500000,GLOBAL_ORIGINAL,CATA_THEN_INHERITED,PLANNED",
          ordinal++, arm.name, arm.treatmentIndex, arm.mixture, instance, seed));
    }
    Files.write(output.resolve("run-registry.csv"), (String.join("\n", rows) + "\n").getBytes(StandardCharsets.UTF_8));
    Files.write(output.resolve("CONFIRMATION_PROTOCOL.md"), (
        "# V35-DOE-1 Held-out Confirmation\n\n"
        + "固定臂：BASE=20/40/20/20；T1=30/50/10/10；T2=25/25/25/25；T3=20/40/30/10。\n\n"
        + "每次 RUN 只允许一个 arm、实例与 seed；部署端必须为每条物理运行创建独立 JVM。\n"
        + "所有臂固定 FM3、单族、序列无关设置、ShiftMode=NONE、GLOBAL_ORIGINAL、"
        + "CA-TA-Lite → inherited LS、A4-Pacing、P=G=5、rho=0、方向教师池关闭。\n")
        .getBytes(StandardCharsets.UTF_8));
  }

  private static final class Arm {
    final String name; final int treatmentIndex; final V35SubSwarmMixture mixture;
    Arm(String name, int treatmentIndex, V35SubSwarmMixture mixture) {
      this.name = name; this.treatmentIndex = treatmentIndex; this.mixture = mixture;
    }
  }

  private static final class Arguments {
    String phase = "REGISTRY"; String arm; String instance; long seed = Long.MIN_VALUE;
    Path projectRoot = Paths.get(".");
    Path output = Paths.get("docs/evidence/V35-DOE1-subgroup-mixture/06-heldout-confirmation");
    static Arguments parse(String[] args) {
      Arguments parsed = new Arguments();
      for (int index = 0; index < args.length; index++) {
        String argument = args[index];
        if ("--phase".equals(argument)) parsed.phase = args[++index].toUpperCase();
        else if ("--arm".equals(argument)) parsed.arm = args[++index].toUpperCase();
        else if ("--instance".equals(argument)) parsed.instance = args[++index];
        else if ("--seed".equals(argument)) parsed.seed = Long.parseLong(args[++index]);
        else if ("--project-root".equals(argument)) parsed.projectRoot = Paths.get(args[++index]);
        else if ("--output".equals(argument)) parsed.output = Paths.get(args[++index]);
        else throw new IllegalArgumentException("unknown arg " + argument);
      }
      return parsed;
    }
  }
}
