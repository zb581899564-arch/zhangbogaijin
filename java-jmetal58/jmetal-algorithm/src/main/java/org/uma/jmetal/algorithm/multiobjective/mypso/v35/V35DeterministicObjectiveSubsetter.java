package org.uma.jmetal.algorithm.multiobjective.mypso.v35;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.uma.jmetal.algorithm.multiobjective.mypso.zhangbo.ZhangBoQgController;
import org.uma.jmetal.solution.PermutationSolution;

/**
 * Deterministic three-objective maximin selector used by reports and dormant
 * archive experiments.  It preserves one stable extreme per objective, uses
 * no randomness and is independent of input order.
 */
public final class V35DeterministicObjectiveSubsetter {
  private static final int[] OBJECTIVES = {0, 1, 6};
  private static final double EPSILON = 1.0e-12;

  private V35DeterministicObjectiveSubsetter() { }

  @SuppressWarnings("unchecked")
  public static List<PermutationSolution<Integer>> selectSolutions(
      List<PermutationSolution<Integer>> source, int capacity) {
    if (source == null) throw new IllegalArgumentException("source");
    if (capacity < 3) throw new IllegalArgumentException("capacity must preserve three extremes");
    Map<String, PermutationSolution<Integer>> unique = new LinkedHashMap<>();
    List<PermutationSolution<Integer>> ordered = new ArrayList<>(source);
    ordered.sort(solutionComparator());
    for (PermutationSolution<Integer> solution : ordered) {
      if (solution == null) throw new IllegalArgumentException("source contains null");
      String key = objectiveKey(solution);
      if (!unique.containsKey(key)) unique.put(key, solution);
    }
    List<PermutationSolution<Integer>> values = new ArrayList<>(unique.values());
    if (values.size() <= capacity) return defensiveSortedCopy(values);

    double[][] bounds = bounds(values);
    List<PermutationSolution<Integer>> selected = new ArrayList<>();
    for (int objective : OBJECTIVES) {
      PermutationSolution<Integer> extreme = Collections.min(values,
          objectiveComparator(objective));
      if (!containsObjectivePoint(selected, extreme)) selected.add(extreme);
    }
    while (selected.size() < capacity) {
      PermutationSolution<Integer> best = null;
      double bestDistance = -1.0;
      for (PermutationSolution<Integer> candidate : values) {
        if (containsObjectivePoint(selected, candidate)) continue;
        double distance = minDistance(candidate, selected, bounds);
        if (best == null || distance > bestDistance + EPSILON
            || (Math.abs(distance - bestDistance) <= EPSILON
                && solutionComparator().compare(candidate, best) < 0)) {
          best = candidate;
          bestDistance = distance;
        }
      }
      if (best == null) break;
      selected.add(best);
    }
    return defensiveSortedCopy(selected);
  }

  public static List<double[]> selectPoints(List<double[]> source, int capacity) {
    if (source == null) throw new IllegalArgumentException("source");
    if (capacity < 3) throw new IllegalArgumentException("capacity must preserve three extremes");
    List<Point> values = new ArrayList<>();
    for (double[] point : source) {
      if (point == null || point.length != 3) throw new IllegalArgumentException("three objectives required");
      values.add(new Point(point));
    }
    Collections.sort(values);
    Map<String, Point> unique = new LinkedHashMap<>();
    for (Point point : values) if (!unique.containsKey(point.key())) unique.put(point.key(), point);
    values = new ArrayList<>(unique.values());
    if (values.size() <= capacity) return pointCopies(values);
    double[][] bounds = pointBounds(values);
    List<Point> selected = new ArrayList<>();
    for (int objective = 0; objective < 3; objective++) {
      Point extreme = values.get(0);
      for (Point value : values) {
        if (value.values[objective] < extreme.values[objective]
            || (value.values[objective] == extreme.values[objective]
                && value.compareTo(extreme) < 0)) extreme = value;
      }
      if (!selected.contains(extreme)) selected.add(extreme);
    }
    while (selected.size() < capacity) {
      Point best = null;
      double bestDistance = -1.0;
      for (Point candidate : values) {
        if (selected.contains(candidate)) continue;
        double distance = minPointDistance(candidate, selected, bounds);
        if (best == null || distance > bestDistance + EPSILON
            || (Math.abs(distance - bestDistance) <= EPSILON && candidate.compareTo(best) < 0)) {
          best = candidate;
          bestDistance = distance;
        }
      }
      if (best == null) break;
      selected.add(best);
    }
    Collections.sort(selected);
    return pointCopies(selected);
  }

  public static String pointsToCsv(List<double[]> points) {
    StringBuilder out = new StringBuilder("Cmax,TEC,TWC\n");
    for (double[] point : points) {
      out.append(point[0]).append(',').append(point[1]).append(',').append(point[2]).append('\n');
    }
    return out.toString();
  }

  private static Comparator<PermutationSolution<Integer>> solutionComparator() {
    return new Comparator<PermutationSolution<Integer>>() {
      @Override public int compare(PermutationSolution<Integer> left,
          PermutationSolution<Integer> right) {
        for (int objective : OBJECTIVES) {
          int comparison = Double.compare(left.getObjective(objective), right.getObjective(objective));
          if (comparison != 0) return comparison;
        }
        return ZhangBoQgController.fingerprint(left).compareTo(ZhangBoQgController.fingerprint(right));
      }
    };
  }

  private static Comparator<PermutationSolution<Integer>> objectiveComparator(final int objective) {
    return new Comparator<PermutationSolution<Integer>>() {
      @Override public int compare(PermutationSolution<Integer> left,
          PermutationSolution<Integer> right) {
        int comparison = Double.compare(left.getObjective(objective), right.getObjective(objective));
        return comparison != 0 ? comparison : solutionComparator().compare(left, right);
      }
    };
  }

  @SuppressWarnings("unchecked")
  private static List<PermutationSolution<Integer>> defensiveSortedCopy(
      List<PermutationSolution<Integer>> values) {
    List<PermutationSolution<Integer>> copy = new ArrayList<>();
    for (PermutationSolution<Integer> value : values) {
      copy.add((PermutationSolution<Integer>) value.copy());
    }
    copy.sort(solutionComparator());
    return Collections.unmodifiableList(copy);
  }

  private static boolean containsObjectivePoint(List<PermutationSolution<Integer>> values,
      PermutationSolution<Integer> candidate) {
    String key = objectiveKey(candidate);
    for (PermutationSolution<Integer> value : values) if (objectiveKey(value).equals(key)) return true;
    return false;
  }

  private static String objectiveKey(PermutationSolution<Integer> value) {
    return Double.toHexString(value.getObjective(0)) + '|'
        + Double.toHexString(value.getObjective(1)) + '|'
        + Double.toHexString(value.getObjective(6));
  }

  private static double[][] bounds(List<PermutationSolution<Integer>> values) {
    double[][] result = {{Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY},
        {Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY},
        {Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY}};
    for (PermutationSolution<Integer> value : values) {
      for (int i = 0; i < 3; i++) {
        double objective = value.getObjective(OBJECTIVES[i]);
        result[i][0] = Math.min(result[i][0], objective);
        result[i][1] = Math.max(result[i][1], objective);
      }
    }
    return result;
  }

  private static double minDistance(PermutationSolution<Integer> candidate,
      List<PermutationSolution<Integer>> selected, double[][] bounds) {
    double minimum = Double.POSITIVE_INFINITY;
    for (PermutationSolution<Integer> value : selected) {
      double sum = 0.0;
      for (int i = 0; i < 3; i++) {
        double range = Math.max(bounds[i][1] - bounds[i][0], EPSILON);
        double delta = (candidate.getObjective(OBJECTIVES[i])
            - value.getObjective(OBJECTIVES[i])) / range;
        sum += delta * delta;
      }
      minimum = Math.min(minimum, Math.sqrt(sum));
    }
    return minimum;
  }

  private static double[][] pointBounds(List<Point> values) {
    double[][] result = {{Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY},
        {Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY},
        {Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY}};
    for (Point value : values) for (int i = 0; i < 3; i++) {
      result[i][0] = Math.min(result[i][0], value.values[i]);
      result[i][1] = Math.max(result[i][1], value.values[i]);
    }
    return result;
  }

  private static double minPointDistance(Point candidate, List<Point> selected, double[][] bounds) {
    double minimum = Double.POSITIVE_INFINITY;
    for (Point value : selected) {
      double sum = 0.0;
      for (int i = 0; i < 3; i++) {
        double range = Math.max(bounds[i][1] - bounds[i][0], EPSILON);
        double delta = (candidate.values[i] - value.values[i]) / range;
        sum += delta * delta;
      }
      minimum = Math.min(minimum, Math.sqrt(sum));
    }
    return minimum;
  }

  private static List<double[]> pointCopies(List<Point> points) {
    List<double[]> result = new ArrayList<>();
    for (Point point : points) result.add(point.values.clone());
    return Collections.unmodifiableList(result);
  }

  private static final class Point implements Comparable<Point> {
    private final double[] values;
    private Point(double[] values) { this.values = values.clone(); }
    private String key() {
      return Double.toHexString(values[0]) + '|' + Double.toHexString(values[1]) + '|'
          + Double.toHexString(values[2]);
    }
    @Override public int compareTo(Point other) {
      for (int i = 0; i < 3; i++) {
        int comparison = Double.compare(values[i], other.values[i]);
        if (comparison != 0) return comparison;
      }
      return 0;
    }
    @Override public boolean equals(Object value) {
      return value instanceof Point && key().equals(((Point) value).key());
    }
    @Override public int hashCode() { return key().hashCode(); }
  }
}
