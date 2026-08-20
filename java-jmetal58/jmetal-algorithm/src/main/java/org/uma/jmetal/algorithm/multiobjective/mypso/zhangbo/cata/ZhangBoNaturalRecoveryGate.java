package org.uma.jmetal.algorithm.multiobjective.mypso.zhangbo.cata;

import org.uma.jmetal.algorithm.multiobjective.mypso.zhangbo.neighborhood.ZhangBoNeighborhoodId;
import org.uma.jmetal.problem.multiobjective.dfsp.fatigue.ZhangBoFatigueEvaluationResult;

/** Shared hard gate for the O13 natural-recovery-only neighborhood. */
public final class ZhangBoNaturalRecoveryGate {
  private static final double EPSILON = 1.0e-12;

  private ZhangBoNaturalRecoveryGate() { }

  public static boolean allows(
      ZhangBoNeighborhoodId id,
      ZhangBoFatigueEvaluationResult parent,
      ZhangBoFatigueEvaluationResult candidate) {
    if (id == null || parent == null) return false;
    if (id != ZhangBoNeighborhoodId.O13_NATURAL_RECOVERY_WINDOW) return true;
    return candidate != null
        && candidate.getMetrics().totalNaturalRecovery
        > parent.getMetrics().totalNaturalRecovery + EPSILON;
  }
}
