package org.uma.jmetal.runner.lc_psode;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class ZhangBoShiftValidationRunnerTest {
  @Rule public TemporaryFolder temporary = new TemporaryFolder();

  @Test
  public void i1EvidenceIsTruthfulAndHashLocked() throws Exception {
    Path output = temporary.newFolder("shift-evidence").toPath();
    ZhangBoShiftValidationRunner.run(projectRoot(), output);
    String manifest = new String(Files.readAllBytes(output.resolve("manifest.properties")),
        StandardCharsets.UTF_8);
    assertTrue(manifest.contains("algorithmSemanticsVersion=fatigue-shift-v2-common-gap"));
    assertTrue(manifest.contains("shiftMode=LEFT_RIGHT"));
    assertTrue(Integer.parseInt(value(manifest, "leftAccepted")) > 0);
    assertTrue(Integer.parseInt(value(manifest, "rightAccepted")) > 0);
    assertEquals("true", value(manifest, "illustrationGate"));
    assertTrue(Integer.parseInt(value(manifest, "internalPropagations")) > 0);
    String objectives = new String(Files.readAllBytes(
        output.resolve("objectives_and_fatigue.csv")), StandardCharsets.UTF_8);
    assertTrue(objectives.startsWith("metric,S0,S1,S2\n"));
    assertTrue(Files.size(output.resolve("figure12_S0_base.svg")) > 0L);
    assertTrue(Files.size(output.resolve("figure13_S1_after_FCLS.svg")) > 0L);
    assertTrue(Files.size(output.resolve("figure14_S2_after_FCRS.svg")) > 0L);
    assertTrue(Files.size(output.resolve("evidence-sha256.tsv")) > 0L);
  }

  private static Path projectRoot() {
    Path current = Paths.get("").toAbsolutePath().normalize();
    while (current != null && !Files.isDirectory(current.resolve("EADHFSP"))) {
      current = current.getParent();
    }
    if (current == null) throw new IllegalStateException("Cannot locate java-jmetal58 root");
    return current.getParent();
  }

  private static String value(String text, String key) {
    String prefix = key + "=";
    for (String line : text.split("\\r?\\n")) {
      if (line.startsWith(prefix)) return line.substring(prefix.length());
    }
    throw new AssertionError("Missing key " + key);
  }
}
