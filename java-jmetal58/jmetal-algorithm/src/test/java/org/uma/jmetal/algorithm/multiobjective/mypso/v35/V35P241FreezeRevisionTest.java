package org.uma.jmetal.algorithm.multiobjective.mypso.v35;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.util.Map;
import java.util.TreeMap;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/** V35-P24.1: corrects the historical A3 wording and freezes the JDK 17 command. */
public class V35P241FreezeRevisionTest {

  @Test(timeout = 300000)
  public void rebuildsFreezeTwiceWithIdenticalDiskBytes() throws Exception {
    Path root = projectRoot();
    String roadmap = utf8(root.resolve("docs/ROADMAP.md"));
    assertTrue("D-070 must label the old A3 value as historical",
        roadmap.contains("A2→A3 +0.059 / Cmax 184.40`) 是**预评价重复计费修复前的历史值**")
            || roadmap.contains("`A2→A3 +0.059 / Cmax 184.40` 是**预评价重复计费修复前的历史值**"));
    assertTrue("D-070 must expose the current corrected A3 value",
        roadmap.contains("A2→A3 **+0.049**（Cmax 206.98→**198.32**"));

    // Rebuild the source inventory and report from the current source tree first.
    new V35P24FreezeCaptureTest().captureFreezeManifest();
    Path evidence = root.resolve("docs/evidence/V35-P24.1");
    Files.createDirectories(evidence);
    writeArtifacts(root, evidence);
    Map<String, String> first = snapshot(evidence);
    writeArtifacts(root, evidence);
    Map<String, String> second = snapshot(evidence);
    assertEquals("P24.1 disk artifacts must be byte-identical on the second build", first, second);
  }

  private static void writeArtifacts(Path root, Path evidence) throws Exception {
    Path p24 = root.resolve("docs/evidence/V35-P24");
    String p24Manifest = utf8(p24.resolve("FREEZE_MANIFEST.txt"));
    int firstBreak = p24Manifest.indexOf('\n');
    if (firstBreak < 0 || !p24Manifest.startsWith("generatedAt=")) {
      throw new IllegalStateException("unexpected V35-P24 manifest header");
    }
    String body = p24Manifest.substring(firstBreak + 1);
    stableWrite(evidence.resolve("FREEZE_MANIFEST.txt"),
        ("revisionTag=v35-p24.1\n" + body).getBytes(StandardCharsets.UTF_8));
    stableWrite(evidence.resolve("source-sha256.csv"),
        Files.readAllBytes(p24.resolve("source-sha256.csv")));
    stableWrite(evidence.resolve("environment.txt"),
        Files.readAllBytes(p24.resolve("environment.txt")));
    stableWrite(evidence.resolve("java-version.txt"),
        Files.readAllBytes(p24.resolve("java-version.txt")));
    stableWrite(evidence.resolve("jdk17-regression-command.ps1"),
        Files.readAllBytes(p24.resolve("jdk17-regression-command.ps1")));

    String report = "# V35-P24.1 冻结修订\n\n"
        + "本修订不改变生产算法机制，只关闭两个冻结证据歧义：\n\n"
        + "- D-070 中 `A3=184.40 / +0.059` 明确为预评价重复计费修复前历史值；"
        + "当前证据统一为 `A3=198.32 / +0.049`。\n"
        + "- 保存 JDK 17 完整回归命令，固定包含 "
        + "`--add-opens=java.base/java.lang=ALL-UNNAMED`。\n"
        + "- `source-sha256.csv` 已从修订后的当前源码树重建，包含更新后的 ROADMAP 哈希。\n"
        + "- 本测试在同一次验收中连续写盘两次，并要求目录内全部冻结物字节级一致。\n\n"
        + "P25A 仅在本修订、205 项回归和六模块打包通过后启动。\n";
    stableWrite(evidence.resolve("V35_P24_1_REPORT.md"), report.getBytes(StandardCharsets.UTF_8));

    StringBuilder hashes = new StringBuilder("path\tsha256\n");
    Map<String, String> current = snapshotWithoutManifest(evidence);
    for (Map.Entry<String, String> entry : current.entrySet()) {
      hashes.append(entry.getKey()).append('\t').append(entry.getValue()).append('\n');
    }
    stableWrite(evidence.resolve("evidence-sha256.tsv"),
        hashes.toString().getBytes(StandardCharsets.UTF_8));
  }

  private static void stableWrite(Path path, byte[] content) throws Exception {
    if (Files.exists(path)) {
      assertEquals("frozen disk artifact drifted: " + path,
          sha256(Files.readAllBytes(path)), sha256(content));
    }
    Files.write(path, content);
  }

  private static Map<String, String> snapshot(Path directory) throws Exception {
    Map<String, String> result = new TreeMap<>();
    java.util.stream.Stream<Path> walk = Files.walk(directory);
    try {
      walk.filter(Files::isRegularFile).forEach(path -> {
        try {
          result.put(directory.relativize(path).toString().replace('\\', '/'),
              sha256(Files.readAllBytes(path)));
        } catch (Exception error) {
          throw new RuntimeException(error);
        }
      });
    } finally {
      walk.close();
    }
    return result;
  }

  private static Map<String, String> snapshotWithoutManifest(Path directory) throws Exception {
    Map<String, String> result = snapshot(directory);
    result.remove("evidence-sha256.tsv");
    return result;
  }

  private static Path projectRoot() {
    Path current = Paths.get("").toAbsolutePath().normalize();
    while (current.getParent() != null && !Files.exists(current.resolve("AGENTS.md"))) {
      current = current.getParent();
    }
    if (!Files.exists(current.resolve("AGENTS.md"))) {
      throw new IllegalStateException("project root not found");
    }
    return current;
  }

  private static String utf8(Path path) throws Exception {
    return new String(Files.readAllBytes(path), StandardCharsets.UTF_8);
  }

  private static String sha256(byte[] bytes) {
    try {
      byte[] digest = MessageDigest.getInstance("SHA-256").digest(bytes);
      StringBuilder out = new StringBuilder();
      for (byte value : digest) out.append(String.format("%02X", value & 0xff));
      return out.toString();
    } catch (Exception error) {
      throw new IllegalStateException(error);
    }
  }
}
