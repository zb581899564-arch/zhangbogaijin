package org.uma.jmetal.algorithm.multiobjective.mypso.p8;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/** Small dependency-free three-objective metric implementation for P8 evidence. */
public final class P8MetricCalculator {
  private static final double EPS = 1e-12;

  private P8MetricCalculator() { }

  public static final class Metrics {
    public final double hv;
    public final double igd;
    public final double spacing;
    public final double cForward;
    public final double cReverse;
    public final int nondominatedCount;

    private Metrics(double hv, double igd, double spacing, double cForward,
        double cReverse, int nondominatedCount) {
      this.hv = hv;
      this.igd = igd;
      this.spacing = spacing;
      this.cForward = cForward;
      this.cReverse = cReverse;
      this.nondominatedCount = nondominatedCount;
    }
  }

  public static List<double[]> nondominated(List<double[]> values) {
    List<double[]> result = new ArrayList<>();
    for (double[] candidate : values) {
      if (candidate == null || candidate.length != 3) {
        throw new IllegalArgumentException("P8 metric vector must have three values");
      }
      boolean dominated = false;
      for (double[] other : values) {
        if (other != candidate && dominates(other, candidate)) {
          dominated = true;
          break;
        }
      }
      if (!dominated) result.add(Arrays.copyOf(candidate, 3));
    }
    Collections.sort(result, vectorComparator());
    return unique(result);
  }

  public static Metrics calculate(List<double[]> approximation, List<double[]> reference) {
    if (approximation == null || approximation.isEmpty() || reference == null || reference.isEmpty()) {
      throw new IllegalArgumentException("P8 metrics require non-empty approximation and reference");
    }
    List<double[]> a = nondominated(normalize(approximation, reference));
    List<double[]> r = nondominated(normalize(reference, reference));
    double hv = hypervolume(a);
    double igd = igd(a, r);
    double spacing = spacing(a);
    return new Metrics(hv, igd, spacing, coverage(a, r), coverage(r, a), a.size());
  }

  public static boolean dominates(double[] left, double[] right) {
    boolean strict = false;
    for (int i = 0; i < 3; i++) {
      if (left[i] > right[i] + EPS) return false;
      if (left[i] + EPS < right[i]) strict = true;
    }
    return strict;
  }

  private static List<double[]> normalize(List<double[]> values, List<double[]> reference) {
    double[] min = {Double.POSITIVE_INFINITY, Double.POSITIVE_INFINITY, Double.POSITIVE_INFINITY};
    double[] max = {Double.NEGATIVE_INFINITY, Double.NEGATIVE_INFINITY, Double.NEGATIVE_INFINITY};
    for (double[] point : reference) {
      for (int i = 0; i < 3; i++) {
        min[i] = Math.min(min[i], point[i]);
        max[i] = Math.max(max[i], point[i]);
      }
    }
    List<double[]> result = new ArrayList<>();
    for (double[] point : values) {
      double[] normalized = new double[3];
      for (int i = 0; i < 3; i++) {
        normalized[i] = clamp((point[i] - min[i]) / Math.max(EPS, max[i] - min[i]));
      }
      result.add(normalized);
    }
    return result;
  }

  private static double hypervolume(List<double[]> points) {
    if (points.isEmpty()) return 0.0;
    final double rx = 1.1, ry = 1.1, rz = 1.1;
    List<double[]> sorted = new ArrayList<>(points);
    Collections.sort(sorted, new Comparator<double[]>() {
      @Override public int compare(double[] a, double[] b) { return Double.compare(a[0], b[0]); }
    });
    double volume = 0.0;
    List<double[]> active = new ArrayList<>();
    int index = 0;
    while (index < sorted.size()) {
      double x = Math.max(0.0, Math.min(rx, sorted.get(index)[0]));
      while (index < sorted.size() && sorted.get(index)[0] <= x + EPS) {
        active.add(sorted.get(index++));
      }
      double nextX = index < sorted.size()
          ? Math.max(x, Math.min(rx, sorted.get(index)[0])) : rx;
      volume += Math.max(0.0, nextX - x) * unionYZ(active, ry, rz);
    }
    return Math.max(0.0, volume);
  }

  private static double unionYZ(List<double[]> points, double ry, double rz) {
    List<double[]> sorted = new ArrayList<>(points);
    Collections.sort(sorted, new Comparator<double[]>() {
      @Override public int compare(double[] a, double[] b) { return Double.compare(a[1], b[1]); }
    });
    double area = 0.0;
    double minZ = rz;
    int index = 0;
    while (index < sorted.size()) {
      double y = Math.max(0.0, Math.min(ry, sorted.get(index)[1]));
      while (index < sorted.size() && sorted.get(index)[1] <= y + EPS) {
        minZ = Math.min(minZ, Math.max(0.0, Math.min(rz, sorted.get(index)[2])));
        index++;
      }
      double nextY = index < sorted.size()
          ? Math.max(y, Math.min(ry, sorted.get(index)[1])) : ry;
      area += Math.max(0.0, nextY - y) * Math.max(0.0, rz - minZ);
    }
    return area;
  }

  private static double igd(List<double[]> approximation, List<double[]> reference) {
    double sum = 0.0;
    for (double[] target : reference) {
      double best = Double.POSITIVE_INFINITY;
      for (double[] point : approximation) best = Math.min(best, distance(target, point));
      sum += best;
    }
    return sum / reference.size();
  }

  private static double spacing(List<double[]> points) {
    if (points.size() < 2) return 0.0;
    double[] distances = new double[points.size()];
    double mean = 0.0;
    for (int i = 0; i < points.size(); i++) {
      double best = Double.POSITIVE_INFINITY;
      for (int j = 0; j < points.size(); j++) if (i != j) best = Math.min(best, distance(points.get(i), points.get(j)));
      distances[i] = best;
      mean += best;
    }
    mean /= distances.length;
    double variance = 0.0;
    for (double value : distances) variance += (value - mean) * (value - mean);
    return Math.sqrt(variance / distances.length);
  }

  private static double coverage(List<double[]> left, List<double[]> right) {
    if (right.isEmpty()) return 0.0;
    int covered = 0;
    for (double[] target : right) {
      for (double[] candidate : left) {
        if (dominates(candidate, target) || equal(candidate, target)) { covered++; break; }
      }
    }
    return ((double) covered) / right.size();
  }

  private static double distance(double[] a, double[] b) {
    double sum = 0.0;
    for (int i = 0; i < 3; i++) sum += (a[i] - b[i]) * (a[i] - b[i]);
    return Math.sqrt(sum);
  }

  private static boolean equal(double[] a, double[] b) {
    for (int i = 0; i < 3; i++) if (Math.abs(a[i] - b[i]) > EPS) return false;
    return true;
  }

  private static double clamp(double value) { return Math.max(0.0, Math.min(1.0, value)); }

  private static List<double[]> unique(List<double[]> values) {
    List<double[]> result = new ArrayList<>();
    for (double[] value : values) if (result.isEmpty() || !equal(result.get(result.size() - 1), value)) result.add(value);
    return result;
  }

  private static Comparator<double[]> vectorComparator() {
    return new Comparator<double[]>() {
      @Override public int compare(double[] a, double[] b) {
        for (int i = 0; i < 3; i++) {
          int c = Double.compare(a[i], b[i]);
          if (c != 0) return c;
        }
        return 0;
      }
    };
  }
}
