package org.uma.jmetal.problem.multiobjective.dfsp.model;

import org.junit.Test;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.Files;
import org.uma.jmetal.problem.multiobjective.dfsp.fatigue.ZhangBoFatigueParameterCodec;
import org.uma.jmetal.problem.multiobjective.dfsp.fatigue.ZhangBoInstanceExtension;
import org.uma.jmetal.problem.multiobjective.dfsp.fatigue.ZhangBoInstanceExtensionCodec;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;

public class CanonicalEadhfspInstanceLoaderTest {
  @Test
  public void sourceValuesAndKeyedSupplementationAreDeterministic() throws Exception {
    Path path = Paths.get(System.getProperty("dhfsp.data.dir", "EADHFSP"), "20_2_3_1.txt");
    if (!Files.exists(path)) path = Paths.get("..", "EADHFSP", "20_2_3_1.txt");
    CanonicalEadhfspInstanceLoader loader = new CanonicalEadhfspInstanceLoader(20260808L);
    CanonicalEadhfspInstanceLoader.Loaded first = loader.load(path);
    CanonicalEadhfspInstanceLoader.Loaded second = loader.load(path);
    DhhfspInstance instance = first.getInstance();
    assertEquals(20, instance.getNumberOfJobs());
    assertEquals(2, instance.getNumberOfStages());
    assertEquals(3, instance.getNumberOfFactories());
    assertArrayEquals(new double[] {1.3, 1.0, 1.3, 1.4},
        instance.getMachineSpeeds(0, 0), 0.0);
    assertEquals(35.0, instance.getStandardProcessingTime(0, 0), 0.0);
    assertEquals(10.0, instance.getStandardProcessingTime(1, 0), 0.0);
    assertEquals(3, instance.getWorkerCount(0, 0));
    assertEquals(first.getSupplementationManifest(), second.getSupplementationManifest());
    String sha = ZhangBoFatigueParameterCodec.sha256(Files.readAllBytes(path));
    ZhangBoInstanceExtension extension = ZhangBoInstanceExtensionCodec.read(
        ZhangBoInstanceExtensionCodec.fileFor(
            ZhangBoInstanceExtensionCodec.configuredDirectory(), 20, 2, 3, 1),
        sha, 20, 2);
    for (int job = 0; job < 20; job++) {
      for (int stage = 0; stage < 2; stage++) {
        assertEquals(extension.getStandardSetupTime(job, stage),
            instance.getStandardSetupTime(stage, job), 0.0);
      }
    }
  }
}
