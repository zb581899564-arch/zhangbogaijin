package org.uma.jmetal.problem.multiobjective.dfsp.fatigue;

import org.junit.Test;
import org.uma.jmetal.problem.multiobjective.dfsp.ZhangBoEDHHFSPW;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;

public class ZhangBoFatigueParameterCodecTest {
  @Test
  public void allFortyFiveManifestsShouldBeStrictStableAndInRange() throws Exception {
    Path project = Paths.get("..").toAbsolutePath().normalize();
    Path data = project.resolve("EADHFSP");
    Path parameters = project.resolve("fatigue-parameters/v1");
    String previous = System.getProperty("dhfsp.data.dir");
    System.setProperty("dhfsp.data.dir", data.toString());
    try {
      List<Path> instances = new ArrayList<>();
      Files.newDirectoryStream(data, "*.txt").forEach(instances::add);
      Collections.sort(instances);
      assertEquals(45, instances.size());
      for (Path instancePath : instances) {
        String[] parts = instancePath.getFileName().toString().replace(".txt", "").split("_");
        int jobs = Integer.parseInt(parts[0]);
        int stages = Integer.parseInt(parts[1]);
        int factories = Integer.parseInt(parts[2]);
        int problem = Integer.parseInt(parts[3]);
        ZhangBoEDHHFSPW source = new ZhangBoEDHHFSPW(jobs, stages, factories, problem);
        ZhangBoFatigueInstanceData instance = source.getFatigueInstanceData();
        Path file = ZhangBoFatigueParameterCodec.fileFor(parameters, jobs, stages, factories, problem);
        byte[] before = Files.readAllBytes(file);
        ZhangBoFatigueParameters loaded = ZhangBoFatigueParameterCodec.read(file, instance);
        assertEquals(instance.getInstanceSha256(), loaded.getInstanceSha256());
        for (int f = 0; f < factories; f++) {
          for (int k = 0; k < stages; k++) {
            assertEquals(instance.getWorkerCount(f), loaded.getWorkers(f, k));
            assertEquals(0.30, loaded.getMaximumIncrease(k), 0.0);
            for (int w = 0; w < instance.getWorkerCount(f); w++) {
              assertTrue(loaded.getLambda(f, w, k) >= 0.01 && loaded.getLambda(f, w, k) <= 0.03);
              assertTrue(loaded.getMu(f, w, k) >= 0.03 && loaded.getMu(f, w, k) <= 0.07);
              assertEquals(0.30 / (loaded.getLambda(f, w, k) * Math.log(2.0)),
                  loaded.getDelta(f, w, k), 1.0e-12);
            }
          }
        }
        assertArrayEquals(before, Files.readAllBytes(file));
      }
    } finally {
      if (previous == null) System.clearProperty("dhfsp.data.dir");
      else System.setProperty("dhfsp.data.dir", previous);
    }
  }

  @Test
  public void keyedSamplingShouldIgnoreTraversalOrderAndReactToInstanceHash() {
    double forward = ZhangBoFatigueParameterGenerator.keyedUnit(
        repeat('A', 64), "lambda", 1, 2, 3);
    ZhangBoFatigueParameterGenerator.keyedUnit(repeat('A', 64), "mu", 0, 0, 0);
    double afterOtherKey = ZhangBoFatigueParameterGenerator.keyedUnit(
        repeat('A', 64), "lambda", 1, 2, 3);
    double changedInstance = ZhangBoFatigueParameterGenerator.keyedUnit(
        repeat('B', 64), "lambda", 1, 2, 3);
    assertEquals(forward, afterOtherKey, 0.0);
    assertNotEquals(forward, changedInstance, 0.0);
  }

  @Test
  public void duplicateFieldWithAValidFileHashShouldStillBeRejected() throws Exception {
    ZhangBoFatigueInstanceData instance = new ZhangBoFatigueInstanceData(repeat('D', 64),
        1, 1, 1, new int[][] {{1}}, new double[][][] {{{1.0}}},
        new int[][][] {{{1}}}, new int[][] {{1}}, new int[] {1},
        new double[][] {{1.0}}, new int[][] {{1}});
    Path file = Files.createTempFile("p5-duplicate-", ".txt");
    try {
      ZhangBoFatigueParameterCodec.write(file, instance,
          ZhangBoFatigueParameterGenerator.generate(instance));
      byte[] firstEncoding = Files.readAllBytes(file);
      ZhangBoFatigueParameterCodec.write(file, instance,
          ZhangBoFatigueParameterGenerator.generate(instance));
      assertArrayEquals(firstEncoding, Files.readAllBytes(file));
      String original = new String(Files.readAllBytes(file), StandardCharsets.UTF_8);
      String payload = original.substring(original.indexOf('\n') + 1)
          + "warningThreshold=0.80000000000000000\n";
      String tampered = "configurationSha256="
          + ZhangBoFatigueParameterCodec.sha256(payload.getBytes(StandardCharsets.UTF_8))
          + "\n" + payload;
      Files.write(file, tampered.getBytes(StandardCharsets.UTF_8));
      boolean rejected = false;
      try {
        ZhangBoFatigueParameterCodec.read(file, instance);
      } catch (IllegalArgumentException expected) {
        rejected = expected.getMessage().contains("Duplicate field");
      }
      assertTrue(rejected);
    } finally {
      Files.deleteIfExists(file);
    }
  }

  private static String repeat(char value, int count) {
    StringBuilder result = new StringBuilder(count);
    for (int i = 0; i < count; i++) result.append(value);
    return result.toString();
  }
}
