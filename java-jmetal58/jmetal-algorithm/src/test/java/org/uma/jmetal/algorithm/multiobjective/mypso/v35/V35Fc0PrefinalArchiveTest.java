package org.uma.jmetal.algorithm.multiobjective.mypso.v35;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;
import org.junit.Test;
import org.uma.jmetal.algorithm.multiobjective.mypso.ZhangBoMOHPSOQ;
import org.uma.jmetal.algorithm.multiobjective.mypso.ZhangBoMOHPSOQBuilder;
import org.uma.jmetal.algorithm.multiobjective.mypso.p8.P8InitialPopulationProvider;
import org.uma.jmetal.algorithm.multiobjective.mypso.p8.P8MetricCalculator;
import org.uma.jmetal.algorithm.multiobjective.mypso.zhangbo.ZhangBoDualQCoordinationConfiguration;
import org.uma.jmetal.algorithm.multiobjective.mypso.zhangbo.ZhangBoFormalHmopsoQgsConfiguration;
import org.uma.jmetal.problem.multiobjective.dfsp.decoder.ProductionDecodeMode;
import org.uma.jmetal.problem.multiobjective.dfsp.fatigue.ZhangBoCanonicalProblemLoader;
import org.uma.jmetal.problem.multiobjective.dfsp.fatigue.ZhangBoCanonicalProductionProblem;
import org.uma.jmetal.problem.multiobjective.dfsp.fatigue.shift.ZhangBoShiftConfiguration;
import org.uma.jmetal.solution.PermutationSolution;
import static org.junit.Assert.*;

/**
 * V35-FC-0: the A4-PREFINAL archive.  Freezes the current formal A4
 * configuration (DSCR+CFVF+Qp+Qg+CA-TA-Lite, directional pool OFF, dualQ
 * blockFrozen(0.10, 5, 5), Table-9 LS=30, no local-FE budget scheduler, no
 * soft freeze) as the rollback anchor for the whole Final-Candidate pipeline
 * (roadmap D-082).  From this point on, no mechanism change may silently
 * mutate the production default; every change goes through an FC work
 * package with an explicit compatibility gate.
 *
 * Acceptance: (a) the archive manifest/source inventory is reproducible and,
 * if present on disk, must match byte-for-byte modulo the generatedAt line;
 * (b) a 20000 FE A4 replay on 20_2_3_1 with a fixed seed reproduces the exact
 * same front hash across three consecutive replays from the same frozen
 * initial population.
 */
public class V35Fc0PrefinalArchiveTest {
  private static final long SEED = 20260808L;
  private static final int POPULATION = 100;
  private static final int BUDGET = 500000;
  private static final int REPLAY_FES = 20000;
  private static final int REPLAY_COUNT = 3;

  @Test(timeout = 600000)
  public void archiveA4Prefinal() throws Exception {
    Path project = Paths.get("").toAbsolutePath().normalize();
    if (project.getFileName() != null
        && "jmetal-algorithm".equals(project.getFileName().toString())) {
      project = project.getParent();
    }
    while (project.getParent() != null && !Files.exists(project.resolve("AGENTS.md"))) {
      project = project.getParent();
    }
    final Path root = project;
    Path evidence = root.resolve("docs/evidence/V35-P26/00_prefinal-archive");
    Files.createDirectories(evidence);

    V35ProductionConfiguration formal = V35ProductionConfiguration.builder()
        .seed(SEED).populationSize(POPULATION).maxEvaluations(BUDGET)
        .decoderMode(ProductionDecodeMode.FM3)
        .dscr(true).cfvf(true).qg(true).qp(true).caTaLite(true)
        .bottleneckDiagnosis(V35BottleneckDiagnosisConfiguration.fullMaskNoShadow())
        .directionalTeacherPool(false).teacherPoolSize(10).build();
    ZhangBoFormalHmopsoQgsConfiguration table9 = ZhangBoFormalHmopsoQgsConfiguration.table9();
    ZhangBoDualQCoordinationConfiguration dualQ =
        ZhangBoDualQCoordinationConfiguration.blockFrozen();
    V35CaTaLiteConfiguration caTaLite = V35CaTaLiteConfiguration.standard();

    String body = buildBody(root, formal, table9, dualQ, caTaLite);
    assertEquals("archive manifest body must be reproducible in-memory",
        sha256(body.getBytes(StandardCharsets.UTF_8)),
        sha256(buildBody(root, formal, table9, dualQ, caTaLite)
            .getBytes(StandardCharsets.UTF_8)));
    Path manifestPath = evidence.resolve("FREEZE_MANIFEST.txt");
    if (Files.exists(manifestPath)) {
      String frozen = new String(Files.readAllBytes(manifestPath), StandardCharsets.UTF_8);
      assertEquals("on-disk A4-PREFINAL archive must match the rebuilt body "
          + "(drift means the rollback anchor changed)", body, stripGeneratedAt(frozen));
    }
    String generatedAt = LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
    Files.write(manifestPath, ("generatedAt=" + generatedAt + '\n' + body)
        .getBytes(StandardCharsets.UTF_8));

    // Source inventory over the exact same production tree as the P24 freeze,
    // so a later diff between the two CSVs isolates precisely the FC changes.
    List<Path> roots = new ArrayList<>();
    roots.add(root.resolve("java-jmetal58/jmetal-algorithm/src/main/java"
        + "/org/uma/jmetal/algorithm/multiobjective/mypso/v35"));
    roots.add(root.resolve("java-jmetal58/jmetal-algorithm/src/main/java"
        + "/org/uma/jmetal/algorithm/multiobjective/mypso/zhangbo"));
    roots.add(root.resolve("java-jmetal58/jmetal-problem/src/main/java"
        + "/org/uma/jmetal/problem/multiobjective/dfsp"));
    List<Path> singles = new ArrayList<>();
    singles.add(root.resolve("java-jmetal58/jmetal-algorithm/src/main/java"
        + "/org/uma/jmetal/algorithm/multiobjective/mypso/ZhangBoMOHPSOQ.java"));
    singles.add(root.resolve("java-jmetal58/jmetal-algorithm/src/main/java"
        + "/org/uma/jmetal/algorithm/multiobjective/mypso/ZhangBoMOHPSOQBuilder.java"));
    singles.add(root.resolve("AGENTS.md"));
    singles.add(root.resolve("docs/ROADMAP.md"));
    Map<String, String> hashes = new TreeMap<>();
    for (Path directory : roots) {
      java.util.stream.Stream<Path> walk = Files.walk(directory);
      walk.filter(path -> Files.isRegularFile(path) && path.toString().endsWith(".java"))
          .forEach(path -> hashes.put(relativize(root, path), shaHex(path)));
      walk.close();
    }
    for (Path file : singles) {
      if (Files.exists(file)) hashes.put(relativize(root, file), shaHex(file));
    }
    assertTrue("source inventory must cover the mechanism tree: " + hashes.size(),
        hashes.size() > 50);
    // The archive inventory is written once: it is the rollback baseline, and
    // later FC packages deliberately change the sources.  Their drift is
    // isolated by diffing against this frozen copy, so overwriting it would
    // destroy the anchor.
    Path inventoryPath = evidence.resolve("source-sha256.csv");
    if (!Files.exists(inventoryPath)) {
      StringBuilder csv = new StringBuilder("\"path\",\"bytes\",\"sha256\"\n");
      for (Map.Entry<String, String> entry : hashes.entrySet()) {
        Path path = root.resolve(entry.getKey());
        csv.append('"').append(entry.getKey()).append("\",\"")
            .append(Files.size(path)).append("\",\"").append(entry.getValue()).append("\"\n");
      }
      Files.write(inventoryPath, csv.toString().getBytes(StandardCharsets.UTF_8));
    }

    StringBuilder environment = new StringBuilder();
    environment.append("javaVersion=").append(System.getProperty("java.version")).append('\n')
        .append("javaVendor=").append(System.getProperty("java.vendor")).append('\n')
        .append("javaHome=").append(System.getProperty("java.home")).append('\n')
        .append("osName=").append(System.getProperty("os.name")).append('\n')
        .append("osVersion=").append(System.getProperty("os.version")).append('\n')
        .append("osArch=").append(System.getProperty("os.arch")).append('\n')
        .append("fileEncoding=").append(System.getProperty("file.encoding")).append('\n')
        .append("userTimezone=").append(System.getProperty("user.timezone")).append('\n');
    Files.write(evidence.resolve("environment.txt"),
        environment.toString().getBytes(StandardCharsets.UTF_8));

    // Replay gate: three consecutive 20000 FE A4 runs from one frozen initial
    // population must produce byte-identical fronts.
    String replayHash = replayA4FrontHash(root);
    for (int index = 1; index < REPLAY_COUNT; index++) {
      assertEquals("A4-PREFINAL replay " + index + " must match replay 0",
          replayHash, replayA4FrontHash(root));
    }

    writeReport(evidence, hashes.size(), formal.configurationHash(), table9.sha256(),
        replayHash);
  }

  /** Assembles the archived A4 exactly as the formal comparison does and
   *  returns the SHA-256 of its final nondominated front CSV. */
  private static String replayA4FrontHash(Path root) throws Exception {
    ZhangBoCanonicalProductionProblem problem = ZhangBoCanonicalProblemLoader.load(
        root.resolve("java-jmetal58/EADHFSP/20_2_3_1.txt"),
        ProductionDecodeMode.FM3, SEED,
        root.resolve("java-jmetal58/instance-extensions/v1"),
        root.resolve("java-jmetal58/fatigue-parameters/v1"),
        ZhangBoShiftConfiguration.none());
    List<PermutationSolution<Integer>> initial = new ArrayList<>();
    for (int index = 0; index < POPULATION; index++) initial.add(problem.createSolution());
    V35ProductionConfiguration config = V35ProductionConfiguration.builder()
        .seed(SEED).populationSize(POPULATION).maxEvaluations(REPLAY_FES)
        .decoderMode(ProductionDecodeMode.FM3)
        .dscr(true).cfvf(true).qg(true).qp(true).caTaLite(true)
        .bottleneckDiagnosis(V35BottleneckDiagnosisConfiguration.fullMaskNoShadow())
        .directionalTeacherPool(false).teacherPoolSize(10).build();
    ZhangBoFormalHmopsoQgsConfiguration formal =
        ZhangBoFormalHmopsoQgsConfiguration.table9();
    ZhangBoMOHPSOQ algorithm = new ZhangBoMOHPSOQBuilder(problem, POPULATION,
        problem.getNumberOfFactories(), 0.6, 0.5, 0.5, 50)
        .setV35Configuration(config)
        .setFormalBaselineConfiguration(formal)
        .setMaxIterations(REPLAY_FES)
        .setInitialSwarmOverride(P8InitialPopulationProvider.copy(initial))
        .build();
    algorithm.run();
    List<double[]> front = new ArrayList<>();
    for (PermutationSolution<Integer> solution : algorithm.getResult()) {
      front.add(new double[] {solution.getObjective(0), solution.getObjective(1),
          solution.getObjective(6)});
    }
    front = P8MetricCalculator.nondominated(front);
    StringBuilder csv = new StringBuilder("Cmax,TEC,TWC\n");
    for (double[] point : front) {
      csv.append(point[0]).append(',').append(point[1]).append(',')
          .append(point[2]).append('\n');
    }
    return sha256(csv.toString().getBytes(StandardCharsets.UTF_8));
  }

  private static String buildBody(Path root, V35ProductionConfiguration formal,
      ZhangBoFormalHmopsoQgsConfiguration table9,
      ZhangBoDualQCoordinationConfiguration dualQ, V35CaTaLiteConfiguration caTaLite) {
    StringBuilder body = new StringBuilder();
    body.append("snapshotTag=v35-fc0-a4-prefinal-archive\n")
        .append("projectRoot=").append(root).append('\n')
        .append("pipeline=v3.5-Final-Candidate (D-082)\n")
        .append("rollbackAnchorFor=V35-FC-0..FC-9\n")
        .append("productionConfigVersion=").append(V35ProductionConfiguration.VERSION).append('\n')
        .append("algorithmSemanticsVersion=")
        .append(V35ProductionConfiguration.ALGORITHM_SEMANTICS_VERSION).append('\n')
        .append("seed=").append(SEED).append('\n')
        .append("populationSize=").append(POPULATION).append('\n')
        .append("maxEvaluations=").append(BUDGET).append('\n')
        .append("decoderMode=").append(ProductionDecodeMode.FM3).append('\n')
        .append("familyMode=DEGENERATE_SINGLE_FAMILY\n")
        .append("setupMode=SEQUENCE_INDEPENDENT\n")
        .append("shiftMode=NONE\n")
        .append("directionalTeacherPool=false\n")
        .append("bottleneckDiagnosis=BAL_FULL_OPEN_NO_SHADOW\n")
        .append("localFeBudgetScheduler=absent\n")
        .append("softFreezeRho=absent\n")
        .append("gBlockLength=5 (equal to blockLength; gb10/15/20 path permanently closed)\n")
        .append("caTaLiteNTest=").append(caTaLite.getNTest()).append('\n')
        .append("caTaLiteApplyMultiplier=").append(caTaLite.getApplyMultiplier()).append('\n')
        .append("caTaLiteStagnationThreshold=").append(caTaLite.getStagnationThreshold())
        .append('\n')
        .append("replayGate=20000FE_x3_front_sha256_identical\n")
        .append("replayInstance=20_2_3_1\n")
        .append("formalMatrixStarted=false\n")
        .append("formalConfigurationHash=").append(formal.configurationHash()).append('\n')
        .append("formalConfigurationBegin\n")
        .append(formal.canonicalText())
        .append("formalConfigurationEnd\n")
        .append("formalBaselineSha256=").append(table9.sha256()).append('\n')
        .append("formalBaselineCanonicalBegin\n")
        .append(table9.canonicalText())
        .append("formalBaselineCanonicalEnd\n")
        .append("dualQCoordinationBegin\n")
        .append(dualQ.toCanonicalText()).append('\n')
        .append("dualQCoordinationEnd\n");
    return body.toString();
  }

  private static String stripGeneratedAt(String manifest) {
    int firstBreak = manifest.indexOf('\n');
    if (firstBreak < 0 || !manifest.startsWith("generatedAt=")) {
      throw new IllegalStateException("unexpected archive manifest header");
    }
    return manifest.substring(firstBreak + 1);
  }

  private static void writeReport(Path evidence, int sourceCount, String formalHash,
      String table9Hash, String replayHash) throws Exception {
    StringBuilder report = new StringBuilder();
    report.append("# V35-FC-0 A4-PREFINAL 存档\n\n");
    report.append("日期：2026-08-17。依据 D-082（v3.5-Final Candidate）：本存档是整个 FC 流水线")
        .append("（FC-1..FC-9）的回退锚点。存档语义 = 当前正式 A4：DSCR+CFVF+Qp+Qg+CA-TA-Lite、")
        .append("方向教师池关闭、BAL 全开放无 shadow、dualQ blockFrozen(0.10, 5, 5)、")
        .append("Table-9 LS_Times=30；无 local-FE 预算调度、无软冻结（两者分别由 FC-2/FC-4 引入）。\n\n");
    report.append("- `FREEZE_MANIFEST.txt`：语义版本 + 正式配置 canonicalText（哈希 ")
        .append(formalHash).append("）+ Table 9（哈希 ").append(table9Hash)
        .append("）+ dualQ canonicalText；幂等契约：磁盘比对，漂移即失败\n");
    report.append("- `source-sha256.csv`：与 V35-P24 冻结相同的生产源码树（").append(sourceCount)
        .append(" 文件）——后续 FC 包引起的源码变化可用两份 CSV 的 diff 精确隔离\n");
    report.append("- `environment.txt`：运行环境\n");
    report.append("- 重放门：`20_2_3_1`、seed 20260808、20000 FE × 3 次连续重放，")
        .append("front SHA-256 = `").append(replayHash).append("`，三次逐位一致\n\n");
    report.append("## 门\n\nFC-0 之后任何机制改动不得直接改生产默认；必须通过对应 FC 工作包并保持")
        .append("向后兼容门（配置缺省 = 存档语义）。FC 流水线全部失败时回退到本存档")
        .append("（其 500k 已知表现：HV 均势、IGD/TEC 4/4 领先）。\n");
    Files.write(evidence.resolve("FC0_REPORT.md"),
        report.toString().getBytes(StandardCharsets.UTF_8));
  }

  private static String relativize(Path root, Path path) {
    try {
      return root.relativize(path).toString().replace('\\', '/');
    } catch (Exception error) {
      return path.toString().replace('\\', '/');
    }
  }

  private static String shaHex(Path path) {
    try {
      return sha256(Files.readAllBytes(path));
    } catch (Exception error) {
      throw new RuntimeException(error);
    }
  }

  private static String sha256(byte[] bytes) {
    try {
      byte[] digest = MessageDigest.getInstance("SHA-256").digest(bytes);
      StringBuilder out = new StringBuilder();
      for (byte value : digest) out.append(String.format("%02x", value & 0xff));
      return out.toString();
    } catch (java.security.NoSuchAlgorithmException error) {
      throw new IllegalStateException(error);
    }
  }
}
