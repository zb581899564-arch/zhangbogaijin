package org.uma.jmetal.runner.lc_psode;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

public class ZhangBoCanonicalExampleRunnerTest {
  @Rule public TemporaryFolder temporary = new TemporaryFolder();

  @Test
  public void oneParticleRunnerWritesTwentyOperationFm0AndFm3Traces() throws Exception {
    Path project = Paths.get("../..").toAbsolutePath().normalize();
    Path output = temporary.newFolder("I1").toPath();
    ZhangBoCanonicalExampleRunner.run(project, output);
    assertEquals(21, Files.readAllLines(
        output.resolve("02_decoder_fm3/program_trace.csv"), StandardCharsets.UTF_8).size());
    assertEquals(21, Files.readAllLines(
        output.resolve("04_fm0_regression/program_trace.csv"), StandardCharsets.UTF_8).size());
    List<String> scope = Files.readAllLines(
        output.resolve("04_fm0_regression/p3_oracle_scope.txt"), StandardCharsets.UTF_8);
    assertTrue(scope.contains("fineTuneCompared=false"));
    assertTrue(scope.contains("rightShiftCompared=false"));
  }
}
