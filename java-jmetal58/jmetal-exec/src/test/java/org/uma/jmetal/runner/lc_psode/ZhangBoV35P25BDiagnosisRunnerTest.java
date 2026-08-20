package org.uma.jmetal.runner.lc_psode;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import org.junit.Test;
import org.uma.jmetal.algorithm.multiobjective.mypso.v35.V35BottleneckDiagnosisConfiguration;

public class ZhangBoV35P25BDiagnosisRunnerTest {
  @Test(timeout = 180000)
  public void shortCalibrationWritesSeparatedMainAndShadowEvidence() throws Exception {
    Path project = Paths.get("").toAbsolutePath().normalize();
    if (project.getFileName() != null && "jmetal-exec".equals(project.getFileName().toString())) {
      project = project.getParent();
    }
    Path output = Files.createTempDirectory("v35-p25b-smoke-");
    Path run = ZhangBoV35P25BDiagnosisRunner.runForTest(
        ZhangBoV35P25BDiagnosisRunner.Phase.CALIBRATION,
        ZhangBoV35P25BDiagnosisRunner.Instance.E20,
        ZhangBoV35P25BDiagnosisRunner.Arm.A4,
        20260814L, project, output, 10, 1000,
        V35BottleneckDiagnosisConfiguration.calibrationAudit());
    String record = new String(Files.readAllBytes(run.resolve("run-record.csv")),
        StandardCharsets.UTF_8);
    assertTrue(record.contains("CALIBRATION,20_2_3_1,A4,20260814,COMPLETED"));
    assertTrue(Files.size(run.resolve("bottleneck-pressure-events.csv")) > 100L);
    assertTrue(Files.size(run.resolve("shadow-probes.csv")) > 100L);
    String status = new String(Files.readAllBytes(run.resolve("status.properties")),
        StandardCharsets.UTF_8);
    assertTrue(status.contains("fullEvaluations="));
    assertEquals(true, Files.isRegularFile(run.resolve("evidence-sha256.tsv")));
  }
}
