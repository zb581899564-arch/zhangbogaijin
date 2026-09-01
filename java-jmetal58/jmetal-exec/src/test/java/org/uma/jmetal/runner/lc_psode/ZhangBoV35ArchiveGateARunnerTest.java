package org.uma.jmetal.runner.lc_psode;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Properties;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

public class ZhangBoV35ArchiveGateARunnerTest {
  @Rule public TemporaryFolder temporaryFolder = new TemporaryFolder();

  @Test(timeout = 300000)
  public void i1GateAWritesClosedPureObservationEvidence() throws Exception {
    Path root = Paths.get("").toAbsolutePath().normalize();
    if (root.getFileName() != null && "jmetal-exec".equals(root.getFileName().toString())) {
      root = root.getParent();
    }
    Path output = temporaryFolder.getRoot().toPath().resolve("i1-gate-a");
    ZhangBoV35ArchiveGateARunner.runForTest(root, output, "I1", 20260808L);
    Properties result = new Properties();
    try (java.io.InputStream input = Files.newInputStream(
        output.resolve("gate-a-result.properties"))) {
      result.load(input);
    }
    assertEquals("BLOCKED", result.getProperty("archiveGateA"));
    assertEquals("false", result.getProperty("decisionEqualsObservedAfterExactDedup"));
    assertTrue(Files.isRegularFile(output.resolve("behavior-hashes.properties")));
    assertTrue(Files.isRegularFile(output.resolve("evidence-sha256.tsv")));
  }
}
