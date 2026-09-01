package org.uma.jmetal.runner.lc_psode;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import org.junit.Test;
import static org.junit.Assert.*;

/** No-FE regression: the shipped template must fail closed before any launch. */
public class ZhangBoV35FormalComparisonRunnerTest {
  @Test
  public void baselineDoesNotCarryTheA4DualQCoordinationObject() {
    assertNull(ZhangBoV35FormalComparisonRunner.runtimeConfigurationForTest(
        "HMOPSO_QGS_F", 20260822L, 100, 500000).getDualQCoordination());
    assertNotNull(ZhangBoV35FormalComparisonRunner.runtimeConfigurationForTest(
        "V35_MAIN", 20260822L, 100, 500000).getDualQCoordination());
  }

  @Test
  public void shippedPlanIsBlockedUntilFc8AndFreezeArtifactsExist() throws Exception {
    Path project = Paths.get("").toAbsolutePath().normalize();
    while (project.getParent() != null && !Files.isRegularFile(project.resolve("AGENTS.md"))) project = project.getParent();
    Path plan = project.resolve("docs/evidence/V35-FORMAL-EXPERIMENTS/00_protocol/formal-comparison-plan.properties");
    ZhangBoV35FormalComparisonRunner.Readiness readiness = ZhangBoV35FormalComparisonRunner.validate(plan);
    assertFalse(readiness.isReady());
    assertTrue(readiness.getBlockers().toString(), readiness.getBlockers().toString().contains("gate.fc8.champion"));
    assertTrue(readiness.getBlockers().toString(), readiness.getBlockers().toString().contains("formal seed list"));
  }
}
