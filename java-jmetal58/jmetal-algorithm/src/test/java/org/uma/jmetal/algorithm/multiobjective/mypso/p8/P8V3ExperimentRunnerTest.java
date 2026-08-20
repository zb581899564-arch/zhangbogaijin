package org.uma.jmetal.algorithm.multiobjective.mypso.p8;

import static org.junit.Assert.assertEquals;
import org.junit.Test;

public class P8V3ExperimentRunnerTest {
  @Test
  public void fixedScopeProduces204LabelSlots() {
    assertEquals(204, P8V3ExperimentRunner.expectedRecordCount(2, 3));
    assertEquals(34, P8V3ExperimentRunner.expectedRecordCount(1, 1));
  }

  @Test(expected = IllegalArgumentException.class)
  public void runnerRejectsLegacySingleInstanceScope() {
    P8V3ExperimentRunner.run(java.util.Collections.<P8V3ExperimentRunner.InstanceBinding>emptyList(),
        P8V3ExperimentRunner.DEFAULT_SEEDS);
  }
}
