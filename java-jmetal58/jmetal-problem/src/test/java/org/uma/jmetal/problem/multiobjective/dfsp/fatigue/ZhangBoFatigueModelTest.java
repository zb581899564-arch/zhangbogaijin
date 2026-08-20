package org.uma.jmetal.problem.multiobjective.dfsp.fatigue;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class ZhangBoFatigueModelTest {
  private static final double EPS = 1.0e-12;

  @Test
  public void equationsShouldCoverRecoveryAccumulationAndMaximumIncrease() {
    assertEquals(0.0, ZhangBoFatigueModel.recover(0.0, 0.05, 100.0), 0.0);
    assertEquals(0.5 * Math.exp(-0.5), ZhangBoFatigueModel.recover(0.5, 0.05, 10.0), EPS);
    assertEquals(0.0, ZhangBoFatigueModel.recover(0.5, 0.05, 1.0e6), 0.0);

    double accumulated = ZhangBoFatigueModel.accumulate(0.0, 0.02, 10.0);
    assertEquals(1.0 - Math.exp(-0.2), accumulated, EPS);
    assertEquals(1.0, ZhangBoFatigueModel.durationMultiplier(0.0, 0.30), 0.0);
    assertTrue(ZhangBoFatigueModel.durationMultiplier(Math.nextAfter(1.0, 0.0), 0.30) <= 1.30);
    assertEquals(1.0, ZhangBoFatigueModel.durationMultiplier(0.75, 0.0), 0.0);
  }

  @Test
  public void analyticalIntegralsShouldMatchSimpleNumericalBounds() {
    double work = ZhangBoFatigueModel.excessIntegralDuringWork(0.9, 0.02, 10.0, 0.8);
    double recovery = ZhangBoFatigueModel.excessIntegralDuringRecovery(0.9, 0.05, 10.0, 0.8);
    assertTrue(work > 0.0 && work < 2.0);
    assertTrue(recovery > 0.0 && recovery < 1.0);
    assertEquals(10.0, ZhangBoFatigueModel.timeAboveDuringWork(0.9, 0.02, 10.0, 0.8), EPS);
    assertEquals(Math.log(0.9 / 0.8) / 0.05,
        ZhangBoFatigueModel.timeAboveDuringRecovery(0.9, 0.05, 10.0, 0.8), EPS);
  }

  @Test(expected = IllegalArgumentException.class)
  public void fatigueOneShouldBeRejected() {
    ZhangBoFatigueModel.accumulate(1.0, 0.02, 1.0);
  }
}
