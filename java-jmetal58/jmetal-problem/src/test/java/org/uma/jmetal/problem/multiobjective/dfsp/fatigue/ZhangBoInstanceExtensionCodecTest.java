package org.uma.jmetal.problem.multiobjective.dfsp.fatigue;

import org.junit.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

public class ZhangBoInstanceExtensionCodecTest {
  @Test
  public void generationIsOrderIndependentAndCodecIsStrict() throws Exception {
    String sha = repeat('A', 64);
    ZhangBoInstanceExtension first = ZhangBoInstanceExtensionGenerator.generate(sha, 7, 3);
    ZhangBoInstanceExtension second = ZhangBoInstanceExtensionGenerator.generate(sha, 7, 3);
    for (int job = 0; job < 7; job++) {
      assertArrayEquals(first.copyStandardSetupTimes()[job],
          second.copyStandardSetupTimes()[job]);
      for (int stage = 0; stage < 3; stage++) {
        int value = first.getStandardSetupTime(job, stage);
        assertFalse(value < 1 || value > 9);
      }
    }
    Path directory = Files.createTempDirectory("zhangbo-sut-");
    Path file = directory.resolve("fixture.setup.txt");
    ZhangBoInstanceExtension persisted = ZhangBoInstanceExtensionCodec.write(file, first);
    byte[] before = Files.readAllBytes(file);
    ZhangBoInstanceExtension loaded = ZhangBoInstanceExtensionCodec.read(file, sha, 7, 3);
    assertEquals(persisted.getConfigurationSha256(), loaded.getConfigurationSha256());
    assertArrayEquals(before, Files.readAllBytes(file));
  }

  @Test
  public void instanceHashChangesTheMatrix() {
    ZhangBoInstanceExtension first = ZhangBoInstanceExtensionGenerator.generate(repeat('A', 64), 20, 2);
    ZhangBoInstanceExtension second = ZhangBoInstanceExtensionGenerator.generate(repeat('B', 64), 20, 2);
    assertFalse(Arrays.deepEquals(first.copyStandardSetupTimes(), second.copyStandardSetupTimes()));
  }

  @Test
  public void allFortyFiveMaterializedExtensionsAreBoundAndInRange() throws Exception {
    Path data = Paths.get("EADHFSP");
    if (!Files.isDirectory(data)) data = Paths.get("..", "EADHFSP");
    Path extensions = ZhangBoInstanceExtensionCodec.configuredDirectory();
    List<Path> instances;
    try (java.util.stream.Stream<Path> stream = Files.list(data)) {
      instances = stream.filter(path -> path.getFileName().toString().endsWith(".txt"))
          .sorted().collect(Collectors.toList());
    }
    assertEquals(45, instances.size());
    for (Path instance : instances) {
      String[] dimensions = instance.getFileName().toString().replace(".txt", "").split("_");
      int jobs = Integer.parseInt(dimensions[0]);
      int stages = Integer.parseInt(dimensions[1]);
      int factories = Integer.parseInt(dimensions[2]);
      int id = Integer.parseInt(dimensions[3]);
      String sha = ZhangBoFatigueParameterCodec.sha256(Files.readAllBytes(instance));
      Path file = ZhangBoInstanceExtensionCodec.fileFor(extensions, jobs, stages, factories, id);
      byte[] before = Files.readAllBytes(file);
      ZhangBoInstanceExtension loaded = ZhangBoInstanceExtensionCodec.read(file, sha, jobs, stages);
      for (int job = 0; job < jobs; job++) {
        for (int stage = 0; stage < stages; stage++) {
          int value = loaded.getStandardSetupTime(job, stage);
          assertFalse(value < 1 || value > 9);
        }
      }
      assertArrayEquals(before, Files.readAllBytes(file));
    }
  }

  private static String repeat(char value, int count) {
    StringBuilder out = new StringBuilder();
    for (int i = 0; i < count; i++) out.append(value);
    return out.toString();
  }
}
