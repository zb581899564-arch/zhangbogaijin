package org.uma.jmetal.algorithm.multiobjective.mypso.v35;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/** Dominance-Safe Cache Refresh: it only replaces a cached teacher when a known strict dominator exists. */
public final class V35DscrSanitizer {
  private V35DscrSanitizer() { }

  public static V35SocialTeacher sanitize(V35SubSwarmRole role,
      V35SocialTeacher cached, V35SocialKnowledgeSnapshot snapshot) {
    if (role == null || cached == null || snapshot == null) {
      throw new IllegalArgumentException("DSCR input cannot be null");
    }
    List<V35SocialTeacher> dominators = new ArrayList<>();
    for (V35SocialTeacher candidate : snapshot.getTeachers()) {
      if (strictlyDominates(candidate.getObjectives(), cached.getObjectives())) dominators.add(candidate);
    }
    if (dominators.isEmpty()) return cached;
    // FC-TIME-2-A3: the G4 direction score's min/max range is a pure function of the
    // frozen snapshot and was recomputed for every dominator (O(T) per comparison,
    // O(T^2) overall). Compute it once with the same iteration order — the values
    // are bit-identical, so the sort order is unchanged.
    double[] minimum = null;
    double[] maximum = null;
    if (role == V35SubSwarmRole.G4_BALANCED) {
      minimum = new double[] {Double.POSITIVE_INFINITY, Double.POSITIVE_INFINITY, Double.POSITIVE_INFINITY};
      maximum = new double[] {Double.NEGATIVE_INFINITY, Double.NEGATIVE_INFINITY, Double.NEGATIVE_INFINITY};
      for (V35SocialTeacher teacher : snapshot.getTeachers()) {
        double[] values = teacher.getObjectives();
        for (int i = 0; i < 3; i++) {
          minimum[i] = Math.min(minimum[i], values[i]);
          maximum[i] = Math.max(maximum[i], values[i]);
        }
      }
    }
    final double[] mn = minimum;
    final double[] mx = maximum;
    dominators.sort(new Comparator<V35SocialTeacher>() {
      @Override public int compare(V35SocialTeacher a, V35SocialTeacher b) {
        int result = Double.compare(directionScore(role, a.getObjectives(), mn, mx),
            directionScore(role, b.getObjectives(), mn, mx));
        return result != 0 ? result : a.getFingerprint().compareTo(b.getFingerprint());
      }
    });
    return dominators.get(0);
  }

  public static boolean strictlyDominates(double[] left, double[] right) {
    boolean strict = false;
    for (int i = 0; i < 3; i++) {
      if (left[i] > right[i]) return false;
      if (left[i] < right[i]) strict = true;
    }
    return strict;
  }

  public static double directionScore(V35SubSwarmRole role, double[] objectives,
      V35SocialKnowledgeSnapshot snapshot) {
    if (role == V35SubSwarmRole.G1_CMAX) return objectives[0];
    if (role == V35SubSwarmRole.G2_TEC) return objectives[1];
    if (role == V35SubSwarmRole.G3_TWC) return objectives[2];
    double[] minimum = {Double.POSITIVE_INFINITY, Double.POSITIVE_INFINITY, Double.POSITIVE_INFINITY};
    double[] maximum = {Double.NEGATIVE_INFINITY, Double.NEGATIVE_INFINITY, Double.NEGATIVE_INFINITY};
    for (V35SocialTeacher teacher : snapshot.getTeachers()) {
      double[] values = teacher.getObjectives();
      for (int i = 0; i < 3; i++) {
        minimum[i] = Math.min(minimum[i], values[i]);
        maximum[i] = Math.max(maximum[i], values[i]);
      }
    }
    return directionScore(role, objectives, minimum, maximum);
  }

  /** FC-TIME-2-A3: direction score with a precomputed snapshot range (O(1)). */
  public static double directionScore(V35SubSwarmRole role, double[] objectives,
      double[] minimum, double[] maximum) {
    if (role == V35SubSwarmRole.G1_CMAX) return objectives[0];
    if (role == V35SubSwarmRole.G2_TEC) return objectives[1];
    if (role == V35SubSwarmRole.G3_TWC) return objectives[2];
    double score = Double.NEGATIVE_INFINITY;
    for (int i = 0; i < 3; i++) {
      double range = Math.max(1.0e-12, maximum[i] - minimum[i]);
      score = Math.max(score, (objectives[i] - minimum[i]) / range);
    }
    return score;
  }
}
