package org.uma.jmetal.algorithm.multiobjective.mypso.zhangbo;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.uma.jmetal.solution.PermutationSolution;

/**
 * Single source of truth for subgroup roles.  Physical author slots are kept
 * stable, while every role-aware component consumes this mapping.
 */
public final class ZhangBoSubSwarmSemantics {
  public static final String VERSION = "P6.5-subswarm-semantics-v1";
  private static final int[] PHYSICAL_ROLE_IDS = new int[]{0, 3, 1, 2};
  private static final double EPSILON = 1.0e-12;

  private ZhangBoSubSwarmSemantics() { }

  public static List<ZhangBoSubSwarm> roles() {
    List<ZhangBoSubSwarm> result = new ArrayList<>();
    result.add(ZhangBoSubSwarm.G1_CMAX);
    result.add(ZhangBoSubSwarm.G2_TEC);
    result.add(ZhangBoSubSwarm.G3_TWC);
    result.add(ZhangBoSubSwarm.G4_BALANCED);
    return Collections.unmodifiableList(result);
  }

  /** Maps the unchanged author physical slot (1..4) to the canonical role. */
  public static ZhangBoSubSwarm roleForPhysicalSlot(int slot) {
    if (slot < 1 || slot > PHYSICAL_ROLE_IDS.length) {
      throw new IllegalArgumentException("Physical subgroup slot must be 1..4: " + slot);
    }
    return ZhangBoSubSwarm.fromSemanticId(PHYSICAL_ROLE_IDS[slot - 1]);
  }

  public static int physicalSlotForRole(ZhangBoSubSwarm role) {
    if (role == null) throw new IllegalArgumentException("role");
    for (int slot = 0; slot < PHYSICAL_ROLE_IDS.length; slot++) {
      if (PHYSICAL_ROLE_IDS[slot] == role.getSemanticId()) return slot + 1;
    }
    throw new IllegalArgumentException("Unmapped role=" + role);
  }

  public static double[] needWeights(ZhangBoSubSwarm role) {
    if (role == null) throw new IllegalArgumentException("role");
    switch (role) {
      case G1_CMAX: return new double[]{2, 1, 1, 1, 1, 1, 1};
      case G2_TEC: return new double[]{1, 2, 1, 1, 1, 1, 1};
      case G3_TWC: return new double[]{1, 1, 2, 1, 1, 1, 1};
      case G4_BALANCED: return new double[]{1, 1, 1, 1, 1, 1, 1};
      default: throw new IllegalStateException("Unhandled role=" + role);
    }
  }

  /** Objective slots in the author seven-slot vector, or -1 for balanced/PDDR. */
  public static int objectiveIndex(ZhangBoSubSwarm role) {
    if (role == null) throw new IllegalArgumentException("role");
    switch (role) {
      case G1_CMAX: return 0;
      case G2_TEC: return 1;
      case G3_TWC: return 6;
      case G4_BALANCED: return -1;
      default: throw new IllegalStateException("Unhandled role=" + role);
    }
  }

  public static boolean isBoundary(ZhangBoSubSwarm role) {
    return objectiveIndex(role) >= 0;
  }

  /** O12 component order: actual duration, energy, worker cost, fatigue. */
  public static double[] neighborhoodPredictionWeights(ZhangBoSubSwarm role) {
    if (role == null) throw new IllegalArgumentException("role");
    switch (role) {
      case G1_CMAX: return new double[]{0.55, 0.15, 0.15, 0.15};
      case G2_TEC: return new double[]{0.15, 0.55, 0.15, 0.15};
      case G3_TWC: return new double[]{0.15, 0.15, 0.55, 0.15};
      case G4_BALANCED: return new double[]{0.25, 0.25, 0.25, 0.25};
      default: throw new IllegalStateException("Unhandled role=" + role);
    }
  }

  /** Frozen archive-space direction, where archive objective 2 represents production slot 6. */
  public static double archivePhi(
      ZhangBoArchiveEntry entry, ZhangBoSubSwarm role, ZhangBoArchiveBounds bounds) {
    if (entry == null || role == null || bounds == null) {
      throw new IllegalArgumentException("entry, role and bounds are required");
    }
    switch (role) {
      case G1_CMAX: return bounds.objective(entry, 0);
      case G2_TEC: return bounds.objective(entry, 1);
      case G3_TWC: return bounds.objective(entry, 2);
      case G4_BALANCED:
        return Math.max(bounds.objective(entry, 0),
            Math.max(bounds.objective(entry, 1), bounds.objective(entry, 2)));
      default: throw new IllegalStateException("Unhandled role=" + role);
    }
  }

  public static double phi(
      PermutationSolution<Integer> solution, ZhangBoSubSwarm role,
      double[] min, double[] max) {
    if (solution == null || role == null || min == null || max == null
        || min.length != 3 || max.length != 3) {
      throw new IllegalArgumentException("solution, role and three-objective bounds are required");
    }
    if (isBoundary(role)) {
      int objective = objectiveIndex(role);
      int compact = objective == 6 ? 2 : objective;
      return normalized(solution.getObjective(objective), min[compact], max[compact]);
    }
    return balancedPhi(solution, min, max);
  }

  public static double normalized(double value, double min, double max) {
    return (value - min) / (max - min + EPSILON);
  }

  /** Balanced direction is the normalized three-objective Chebyshev value. */
  public static double balancedPhi(
      PermutationSolution<Integer> solution, double[] min, double[] max) {
    if (min == null || max == null || min.length != 3 || max.length != 3) {
      throw new IllegalArgumentException("Balanced bounds must have length 3");
    }
    double result = 0.0;
    int[] objectives = new int[]{0, 1, 6};
    for (int i = 0; i < objectives.length; i++) {
      result = Math.max(result, normalized(solution.getObjective(objectives[i]), min[i], max[i]));
    }
    return result;
  }

  public static String mappingText() {
    return VERSION + "|slot1=G1_CMAX|slot2=G4_BALANCED|slot3=G2_TEC|slot4=G3_TWC";
  }

  public static String mappingHash() {
    try {
      byte[] digest = MessageDigest.getInstance("SHA-256")
          .digest(mappingText().getBytes(StandardCharsets.UTF_8));
      StringBuilder result = new StringBuilder();
      for (byte value : digest) result.append(String.format("%02X", value & 0xff));
      return result.toString();
    } catch (NoSuchAlgorithmException exception) {
      throw new IllegalStateException("SHA-256 unavailable", exception);
    }
  }
}
