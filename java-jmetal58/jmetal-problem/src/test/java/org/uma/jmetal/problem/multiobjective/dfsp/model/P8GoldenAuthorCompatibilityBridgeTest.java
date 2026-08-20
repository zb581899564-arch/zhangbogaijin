package org.uma.jmetal.problem.multiobjective.dfsp.model;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.Test;
import org.uma.jmetal.problem.multiobjective.dfsp.fatigue.ZhangBoInstanceExtension;
import org.uma.jmetal.problem.multiobjective.dfsp.fatigue.ZhangBoInstanceExtensionCodec;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class P8GoldenAuthorCompatibilityBridgeTest {
  @Test
  public void bridgePreservesPaperSutAndIsByteStable() throws Exception {
    Path root = Files.createTempDirectory("p8-golden-bridge-");
    P8GoldenAuthorCompatibilityBridge.Manifest first =
        P8GoldenAuthorCompatibilityBridge.materialize(root);
    byte[] instanceBefore = Files.readAllBytes(first.dataFile);
    byte[] setupBefore = Files.readAllBytes(first.setupFile);
    byte[] fatigueBefore = Files.readAllBytes(first.fatigueFile);

    Chapter4GoldenFixture fixture = Chapter4GoldenFixture.load();
    DhhfspInstance paper = fixture.getInstance();
    ZhangBoInstanceExtension setup = ZhangBoInstanceExtensionCodec.read(first.setupFile,
        first.instanceSha256, paper.getNumberOfJobs(), paper.getNumberOfStages());
    for (int job = 0; job < paper.getNumberOfJobs(); job++) {
      for (int stage = 0; stage < paper.getNumberOfStages(); stage++) {
        assertEquals((int) paper.getStandardSetupTime(stage, job),
            setup.getStandardSetupTime(job, stage));
      }
    }
    String manifest = new String(Files.readAllBytes(first.manifestPath),
        StandardCharsets.UTF_8);
    assertTrue(manifest.contains("semanticTag=author_compatibility_bridge"));
    assertTrue(manifest.contains("sutMapping=exact paper table; no sampling"));

    P8GoldenAuthorCompatibilityBridge.Manifest second =
        P8GoldenAuthorCompatibilityBridge.materialize(root);
    assertEquals(first.instanceSha256, second.instanceSha256);
    assertArrayEquals(instanceBefore, Files.readAllBytes(second.dataFile));
    assertArrayEquals(setupBefore, Files.readAllBytes(second.setupFile));
    assertArrayEquals(fatigueBefore, Files.readAllBytes(second.fatigueFile));
  }
}
