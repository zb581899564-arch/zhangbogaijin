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
import org.uma.jmetal.algorithm.multiobjective.mypso.zhangbo.ZhangBoFormalHmopsoQgsConfiguration;
import org.uma.jmetal.algorithm.multiobjective.mypso.zhangbo.ZhangBoQgController;
import org.uma.jmetal.algorithm.multiobjective.mypso.zhangbo.ZhangBoSubSwarmSemantics;
import org.uma.jmetal.algorithm.multiobjective.mypso.zhangbo.audit.ZhangBoCmaxAudit;
import org.uma.jmetal.problem.multiobjective.dfsp.decoder.ProductionDecodeMode;
import org.uma.jmetal.problem.multiobjective.dfsp.fatigue.ZhangBoFatigueParameterGenerator;
import static org.junit.Assert.*;

/**
 * V35-P24 closure: the final parameter freeze.  Programmatically captures the
 * frozen semantic versions, seed/budget protocol, decoder/setup/shift boundary,
 * teacher pool, CA-TA-Lite parameters, the Table 9 formal baseline (hash and
 * canonical text), the four acceptance flags (all explicitly false), a source
 * SHA-256 inventory over the v35/zhangbo/audit production sources, and the
 * environment record.
 *
 * Idempotence contract (acceptance review P2): if a freeze manifest already
 * exists on disk, this test must reproduce it byte-for-byte modulo the
 * generatedAt timestamp line; any drift fails the test instead of silently
 * overwriting the frozen artifact.  Produces no production-code semantic change.
 */
public class V35P24FreezeCaptureTest {
  private static final long SEED = 20260808L;
  private static final int POPULATION = 100;
  private static final int BUDGET = 500000;

  @Test(timeout = 300000)
  public void captureFreezeManifest() throws Exception {
    Path project = Paths.get("").toAbsolutePath().normalize();
    if (project.getFileName() != null
        && "jmetal-algorithm".equals(project.getFileName().toString())) {
      project = project.getParent();
    }
    while (project.getParent() != null && !Files.exists(project.resolve("AGENTS.md"))) {
      project = project.getParent();
    }
    final Path root = project;
    Path evidence = root.resolve("docs/evidence/V35-P24");
    Files.createDirectories(evidence);

    V35ProductionConfiguration formal = V35ProductionConfiguration.builder()
        .seed(SEED).populationSize(POPULATION).maxEvaluations(BUDGET)
        .decoderMode(ProductionDecodeMode.FM3)
        .dscr(true).cfvf(true).qg(true).qp(true).caTaLite(true)
        .directionalTeacherPool(true).teacherPoolSize(10).build();
    V35CaTaLiteConfiguration caTaLite = V35CaTaLiteConfiguration.standard();
    ZhangBoFormalHmopsoQgsConfiguration table9 = ZhangBoFormalHmopsoQgsConfiguration.table9();

    // Build the manifest body twice and require identical bytes (in-memory
    // determinism), then require it to match the on-disk freeze if one exists.
    String body = buildBody(root, formal, caTaLite, table9);
    assertEquals("freeze manifest body must be reproducible in-memory",
        sha256(body.getBytes(StandardCharsets.UTF_8)),
        sha256(buildBody(root, formal, caTaLite, table9).getBytes(StandardCharsets.UTF_8)));

    Path manifestPath = evidence.resolve("FREEZE_MANIFEST.txt");
    if (Files.exists(manifestPath)) {
      String frozen = new String(Files.readAllBytes(manifestPath), StandardCharsets.UTF_8);
      String frozenBody = stripGeneratedAt(frozen);
      assertEquals("on-disk freeze manifest body must match the rebuilt body "
          + "(any drift means the frozen artifact changed)", body, frozenBody);
    }

    String generatedAt = LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
    Files.write(manifestPath, ("generatedAt=" + generatedAt + '\n' + body)
        .getBytes(StandardCharsets.UTF_8));

    // Source SHA-256 inventory over the production mechanism sources.
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
    StringBuilder csv = new StringBuilder("\"path\",\"bytes\",\"sha256\"\n");
    for (Map.Entry<String, String> entry : hashes.entrySet()) {
      Path path = root.resolve(entry.getKey());
      csv.append('"').append(entry.getKey()).append("\",\"")
          .append(Files.size(path)).append("\",\"").append(entry.getValue()).append("\"\n");
    }
    Files.write(evidence.resolve("source-sha256.csv"),
        csv.toString().getBytes(StandardCharsets.UTF_8));

    // Environment record.
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
    Files.write(evidence.resolve("java-version.txt"),
        (System.getProperty("java.version") + "\n").getBytes(StandardCharsets.UTF_8));
    String regressionCommand = "mvn.cmd -q -pl jmetal-algorithm `\n"
        + "  \"-Djacoco.skip=true\" `\n"
        + "  \"-DfailIfNoTests=false\" `\n"
        + "  \"-DargLine=--add-opens=java.base/java.lang=ALL-UNNAMED\" `\n"
        + "  test\n";
    Files.write(evidence.resolve("jdk17-regression-command.ps1"),
        regressionCommand.getBytes(StandardCharsets.UTF_8));

    writeReport(evidence, hashes.size(), formal.configurationHash(), table9.sha256());
  }

  private static String buildBody(Path root, V35ProductionConfiguration formal,
      V35CaTaLiteConfiguration caTaLite, ZhangBoFormalHmopsoQgsConfiguration table9) {
    StringBuilder body = new StringBuilder();
    body.append("snapshotTag=v35-p24-final-freeze\n")
        .append("projectRoot=").append(root).append('\n')
        .append("excluded=.git,.idea,target,results\n")
        .append("semanticMainline=v3.5\n")
        .append("productionConfigVersion=").append(V35ProductionConfiguration.VERSION).append('\n')
        .append("algorithmSemanticsVersion=")
        .append(V35ProductionConfiguration.ALGORITHM_SEMANTICS_VERSION).append('\n')
        .append("cmaxAuditVersion=").append(ZhangBoCmaxAudit.VERSION).append('\n')
        .append("dscrMetricsSchema=").append(V35DscrTeacherCache.VERSION).append('\n')
        .append("subSwarmSemanticsVersion=").append(ZhangBoSubSwarmSemantics.VERSION).append('\n')
        .append("qgControllerRequires=").append(ZhangBoQgController.class.getSimpleName()).append('\n')
        .append("seed=").append(SEED).append('\n')
        .append("populationSize=").append(POPULATION).append('\n')
        .append("maxEvaluations=").append(BUDGET).append('\n')
        .append("decoderMode=").append(ProductionDecodeMode.FM3).append('\n')
        .append("familyMode=DEGENERATE_SINGLE_FAMILY\n")
        .append("setupMode=SEQUENCE_INDEPENDENT\n")
        .append("shiftMode=NONE\n")
        .append("teacherPoolSize=").append(formal.getTeacherPoolSize()).append('\n')
        .append("caTaLiteNTest=").append(caTaLite.getNTest()).append('\n')
        .append("caTaLiteApplyMultiplier=").append(caTaLite.getApplyMultiplier()).append('\n')
        .append("caTaLiteApplyExploreProbability=")
        .append(String.format(Locale.ROOT, "%.2f", caTaLite.getApplyExploreProbability()))
        .append('\n')
        .append("caTaLiteStagnationThreshold=").append(caTaLite.getStagnationThreshold())
        .append('\n')
        .append("caTaLiteContexts=").append(caTaLite.contextCount()).append('\n')
        .append("caTaLiteMacroNeighborhoods=").append(
            java.util.Arrays.toString(V35MacroNeighborhood.values())).append('\n')
        .append("fatigueSampler=").append(ZhangBoFatigueParameterGenerator.SAMPLER_ID).append('\n')
        .append("controlledStart=same-seed-identical-initial-population-sha256\n")
        .append("fairBoundary=FM3+single-family+shift-free+single-seed\n")
        .append("pfSdstAccepted=false\n")
        .append("multiSeedStatisticsStarted=false\n")
        .append("formalMatrixStarted=false\n")
        .append("thesisNumbersUpdated=false\n")
        .append("formalConfigurationHash=").append(formal.configurationHash()).append('\n')
        .append("formalConfigurationBegin\n")
        .append(formal.canonicalText())
        .append("formalConfigurationEnd\n")
        .append("formalBaselineSha256=").append(table9.sha256()).append('\n')
        .append("formalBaselineCanonicalBegin\n")
        .append(table9.canonicalText())
        .append("formalBaselineCanonicalEnd\n");
    return body.toString();
  }

  private static String stripGeneratedAt(String manifest) {
    // The file is "generatedAt=<timestamp>\n" + body; the body ends with a single
    // trailing newline.  Split/rejoin would fabricate an extra blank line, so cut
    // at the first line break instead.
    int firstBreak = manifest.indexOf('\n');
    if (firstBreak < 0 || !manifest.startsWith("generatedAt=")) {
      throw new IllegalStateException("unexpected freeze manifest header");
    }
    return manifest.substring(firstBreak + 1);
  }

  private static void writeReport(Path evidence, int sourceCount, String formalHash,
      String table9Hash) throws Exception {
    StringBuilder report = new StringBuilder();
    report.append("# V35-P24 最终参数冻结\n\n");
    report.append("冻结范围：v3.5 生产机制栈（DSCR / CFVF / Q-pbest 双Q / CA-TA-Lite / 方向教师池）、")
        .append("解码边界（FM3、单族退化、序列无关、无班次）、公平协议（seed=20260808、")
        .append("同初始种群 SHA-256、单 seed 500k FE）、Table 9 正式基线（哈希 ").append(table9Hash)
        .append("）。\n\n");
    report.append("- `FREEZE_MANIFEST.txt`：全部冻结语义版本与参数 + 正式配置 canonicalText + 哈希 ")
        .append(formalHash).append(" + Table 9 canonicalText；幂等契约：")
        .append("与磁盘既有冻结物逐字节一致（除 generatedAt 时间戳行），漂移即失败\n");
    report.append("- `source-sha256.csv`：v35/zhangbo/audit 机制源码清单（").append(sourceCount)
        .append(" 文件）+ AGENTS/ROADMAP\n");
    report.append("- `environment.txt` / `java-version.txt`：运行环境\n");
    report.append("- `jdk17-regression-command.ps1`：JDK 17 完整回归命令，固定包含 "
        + "`--add-opens=java.base/java.lang=ALL-UNNAMED`\n\n");
    report.append("## 四个验收标志（全部为 false）\n\n");
    report.append("- PF-SDST 真实启用：**未批准**\n");
    report.append("- 多 seed 统计：**未开始**\n");
    report.append("- 正式实验矩阵：**未开始**\n");
    report.append("- 论文数字更新：**未发生**（本包不产生任何生产代码语义变更）\n\n");
    report.append("## 门\n\nP25（多 seed 正式矩阵）及之后的一切工作需要另行批准；")
        .append("冻结后任何机制/参数变更必须先更新本清单并重新全量回归。\n");
    Files.write(evidence.resolve("V35_P24_REPORT.md"),
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
