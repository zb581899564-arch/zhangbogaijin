package org.uma.jmetal.algorithm.multiobjective.mypso.zhangbo.cata;

import org.uma.jmetal.algorithm.multiobjective.mypso.zhangbo.ZhangBoArchiveBounds;
import org.uma.jmetal.algorithm.multiobjective.mypso.zhangbo.ZhangBoSubSwarm;
import org.uma.jmetal.algorithm.multiobjective.mypso.zhangbo.ZhangBoSubSwarmSemantics;
import org.uma.jmetal.solution.PermutationSolution;

/** Author-compatible local acceptance and the one documented CA-TA direction quality metric. */
public final class ZhangBoLocalSearchAcceptance {
  private static final double EPSILON = 1.0e-12;

  private ZhangBoLocalSearchAcceptance() { }

  public static boolean accepts(
      PermutationSolution<Integer> parent,
      PermutationSolution<Integer> candidate,
      ZhangBoSubSwarm role) {
    if (parent == null || candidate == null || role == null) {
      throw new IllegalArgumentException("parent, candidate and role are required");
    }
    switch (role) {
      case G1_CMAX:
        return candidate.getObjective(0) < parent.getObjective(0);
      case G2_TEC:
        return candidate.getObjective(1) < parent.getObjective(1);
      case G3_TWC:
        return candidate.getObjective(6) < parent.getObjective(6);
      case G4_BALANCED:
        // Exact author balanced-group rule: any of the three tracked objectives improves.
        return candidate.getObjective(0) < parent.getObjective(0)
            || candidate.getObjective(1) < parent.getObjective(1)
            || candidate.getObjective(6) < parent.getObjective(6);
      default:
        throw new IllegalStateException("Unhandled subgroup=" + role);
    }
  }

  public static double qualityGain(
      PermutationSolution<Integer> parent,
      PermutationSolution<Integer> candidate,
      ZhangBoSubSwarm role,
      ZhangBoArchiveBounds bounds) {
    if (bounds == null) throw new IllegalArgumentException("bounds");
    double phiParent = ZhangBoSubSwarmSemantics.phi(parent, role,
        bounds.getObjectiveMinimums(), bounds.getObjectiveMaximums());
    double phiCandidate = ZhangBoSubSwarmSemantics.phi(candidate, role,
        bounds.getObjectiveMinimums(), bounds.getObjectiveMaximums());
    return clip((phiParent - phiCandidate) / (Math.abs(phiParent) + EPSILON));
  }

  public static double clip(double value) {
    return Math.max(-1.0, Math.min(1.0, value));
  }
}
