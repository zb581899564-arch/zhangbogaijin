package org.uma.jmetal.algorithm.multiobjective.mypso.zhangbo;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Deterministic capacity-limited personal nondominated archive. */
public final class ZhangBoPersonalArchive {
  public static final class Update {
    private final List<ZhangBoArchiveEntry> entries;
    private final boolean insertedEntrySurvived;
    private final int dominatedRemoved;
    private final int duplicatesRemoved;
    private final int truncatedRemoved;

    private Update(List<ZhangBoArchiveEntry> entries, boolean insertedEntrySurvived,
                   int dominatedRemoved, int duplicatesRemoved, int truncatedRemoved) {
      this.entries = Collections.unmodifiableList(new ArrayList<>(entries));
      this.insertedEntrySurvived = insertedEntrySurvived;
      this.dominatedRemoved = dominatedRemoved;
      this.duplicatesRemoved = duplicatesRemoved;
      this.truncatedRemoved = truncatedRemoved;
    }

    public List<ZhangBoArchiveEntry> getEntries() { return new ArrayList<>(entries); }
    public boolean isInsertedEntrySurvived() { return insertedEntrySurvived; }
    public int getDominatedRemoved() { return dominatedRemoved; }
    public int getDuplicatesRemoved() { return duplicatesRemoved; }
    public int getTruncatedRemoved() { return truncatedRemoved; }
  }

  private final ZhangBoPersonalArchiveConfiguration configuration;

  public ZhangBoPersonalArchive(ZhangBoPersonalArchiveConfiguration configuration) {
    if (configuration == null || !configuration.isEnabled()) {
      throw new IllegalArgumentException("Enabled personal archive configuration required");
    }
    this.configuration = configuration;
  }

  public Update update(
      List<ZhangBoArchiveEntry> previous,
      ZhangBoArchiveEntry inserted,
      ZhangBoSubSwarm group,
      ZhangBoArchiveBounds bounds) {
    if (previous == null || inserted == null || group == null || bounds == null) {
      throw new IllegalArgumentException("Archive update arguments cannot be null");
    }
    List<ZhangBoArchiveEntry> union = new ArrayList<>(previous.size() + 1);
    union.addAll(previous);
    union.add(inserted);
    List<ZhangBoArchiveEntry> nondominated = strictNondominated(union);
    int dominatedRemoved = union.size() - nondominated.size();
    List<ZhangBoArchiveEntry> unique = removeNearDuplicates(nondominated, bounds);
    int duplicatesRemoved = nondominated.size() - unique.size();
    List<ZhangBoArchiveEntry> result = unique;
    int truncatedRemoved = 0;
    if (unique.size() > configuration.getCapacity()) {
      result = truncate(unique, group, bounds);
      truncatedRemoved = unique.size() - result.size();
    }
    Collections.sort(result, Comparator.comparing(ZhangBoArchiveEntry::getFingerprint));
    boolean survived = containsFingerprint(result, inserted.getFingerprint());
    return new Update(result, survived, dominatedRemoved, duplicatesRemoved, truncatedRemoved);
  }

  private static List<ZhangBoArchiveEntry> strictNondominated(
      List<ZhangBoArchiveEntry> values) {
    List<ZhangBoArchiveEntry> sorted = new ArrayList<>(values);
    Collections.sort(sorted, Comparator
        .comparing(ZhangBoArchiveEntry::getFingerprint)
        .thenComparingLong(ZhangBoArchiveEntry::getEvaluationOrdinal));
    List<ZhangBoArchiveEntry> result = new ArrayList<>();
    for (int index = 0; index < sorted.size(); index++) {
      ZhangBoArchiveEntry candidate = sorted.get(index);
      boolean dominated = false;
      for (int other = 0; other < sorted.size(); other++) {
        if (index != other && dominates(sorted.get(other), candidate)) {
          dominated = true;
          break;
        }
      }
      if (!dominated) result.add(candidate);
    }
    return result;
  }

  private static boolean dominates(ZhangBoArchiveEntry left, ZhangBoArchiveEntry right) {
    boolean strict = false;
    for (int objective = 0; objective < 3; objective++) {
      if (left.getObjective(objective) > right.getObjective(objective)) return false;
      if (left.getObjective(objective) < right.getObjective(objective)) strict = true;
    }
    return strict;
  }

  private List<ZhangBoArchiveEntry> removeNearDuplicates(
      List<ZhangBoArchiveEntry> values, ZhangBoArchiveBounds bounds) {
    int[] parent = new int[values.size()];
    for (int index = 0; index < parent.length; index++) parent[index] = index;
    for (int left = 0; left < values.size(); left++) {
      for (int right = left + 1; right < values.size(); right++) {
        if (bounds.objectiveDistance(values.get(left), values.get(right))
            < configuration.getDuplicateEpsilon()) {
          union(parent, left, right);
        }
      }
    }
    Map<Integer, List<ZhangBoArchiveEntry>> components = new LinkedHashMap<>();
    for (int index = 0; index < values.size(); index++) {
      int root = find(parent, index);
      List<ZhangBoArchiveEntry> component = components.get(root);
      if (component == null) {
        component = new ArrayList<>();
        components.put(root, component);
      }
      component.add(values.get(index));
    }
    Map<String, Double> fitness = epsilonFitness(values, bounds);
    List<ZhangBoArchiveEntry> result = new ArrayList<>();
    for (List<ZhangBoArchiveEntry> component : components.values()) {
      Collections.sort(component, Comparator.comparing(ZhangBoArchiveEntry::getFingerprint));
      ZhangBoArchiveEntry best = component.get(0);
      for (int index = 1; index < component.size(); index++) {
        best = betterSimilar(best, component.get(index), fitness, bounds);
      }
      result.add(best);
    }
    return result;
  }

  private ZhangBoArchiveEntry betterSimilar(
      ZhangBoArchiveEntry left, ZhangBoArchiveEntry right,
      Map<String, Double> fitness, ZhangBoArchiveBounds bounds) {
    double leftRisk = bounds.fatigueRisk(left, configuration);
    double rightRisk = bounds.fatigueRisk(right, configuration);
    int risk = Double.compare(leftRisk, rightRisk);
    if (risk < 0) return left;
    if (risk > 0) return right;
    int indicator = Double.compare(fitness.get(left.getFingerprint()),
        fitness.get(right.getFingerprint()));
    if (indicator < 0) return left;
    if (indicator > 0) return right;
    int ordinal = Long.compare(left.getEvaluationOrdinal(), right.getEvaluationOrdinal());
    if (ordinal < 0) return left;
    if (ordinal > 0) return right;
    return left.getFingerprint().compareTo(right.getFingerprint()) <= 0 ? left : right;
  }

  private List<ZhangBoArchiveEntry> truncate(
      List<ZhangBoArchiveEntry> values, ZhangBoSubSwarm group,
      ZhangBoArchiveBounds bounds) {
    List<ZhangBoArchiveEntry> selected = new ArrayList<>();
    addDistinct(selected, directionalAnchor(values, group, bounds));
    if (selected.size() < configuration.getCapacity()) {
      addDistinct(selected, epsilonAnchor(values, bounds));
    }
    while (selected.size() < configuration.getCapacity()) {
      ZhangBoArchiveEntry next = farthest(values, selected, bounds);
      if (next == null) break;
      addDistinct(selected, next);
    }
    return selected;
  }

  private ZhangBoArchiveEntry directionalAnchor(
      List<ZhangBoArchiveEntry> values, ZhangBoSubSwarm group,
      ZhangBoArchiveBounds bounds) {
    ZhangBoArchiveEntry best = values.get(0);
    for (int index = 1; index < values.size(); index++) {
      ZhangBoArchiveEntry candidate = values.get(index);
      double candidateScore = direction(candidate, group, bounds);
      double bestScore = direction(best, group, bounds);
      if (candidateScore < bestScore) best = candidate;
      else if (Double.compare(candidateScore, bestScore) == 0) {
        best = stableTie(best, candidate, bounds);
      }
    }
    return fatigueNeighborhoodChoice(best, values, bounds,
        new Metric() { public double value(ZhangBoArchiveEntry entry) {
          return direction(entry, group, bounds);
        }});
  }

  private double direction(
      ZhangBoArchiveEntry entry, ZhangBoSubSwarm group,
      ZhangBoArchiveBounds bounds) {
    return ZhangBoSubSwarmSemantics.archivePhi(entry, group, bounds);
  }

  private ZhangBoArchiveEntry epsilonAnchor(
      List<ZhangBoArchiveEntry> values, ZhangBoArchiveBounds bounds) {
    Map<String, Double> fitness = epsilonFitness(values, bounds);
    ZhangBoArchiveEntry best = values.get(0);
    for (int index = 1; index < values.size(); index++) {
      ZhangBoArchiveEntry candidate = values.get(index);
      int compare = Double.compare(fitness.get(candidate.getFingerprint()),
          fitness.get(best.getFingerprint()));
      if (compare < 0) best = candidate;
      else if (compare == 0) best = stableTie(best, candidate, bounds);
    }
    final Map<String, Double> frozenFitness = fitness;
    return fatigueNeighborhoodChoice(best, values, bounds,
        new Metric() { public double value(ZhangBoArchiveEntry entry) {
          return frozenFitness.get(entry.getFingerprint());
        }});
  }

  private Map<String, Double> epsilonFitness(
      List<ZhangBoArchiveEntry> values, ZhangBoArchiveBounds bounds) {
    Map<String, Double> result = new HashMap<>();
    if (values.size() == 1) {
      result.put(values.get(0).getFingerprint(), 0.0);
      return result;
    }
    double scale = configuration.getNormalizationEpsilon();
    for (ZhangBoArchiveEntry left : values) {
      for (ZhangBoArchiveEntry right : values) {
        if (left != right) scale = Math.max(scale, Math.abs(indicator(left, right, bounds)));
      }
    }
    for (ZhangBoArchiveEntry candidate : values) {
      double sum = 0.0;
      for (ZhangBoArchiveEntry other : values) {
        if (candidate != other) {
          sum += Math.exp(-indicator(other, candidate, bounds)
              / (scale * configuration.getIndicatorKappa()));
        }
      }
      result.put(candidate.getFingerprint(), -sum);
    }
    return result;
  }

  Map<String, Double> epsilonFitnessValues(
      List<ZhangBoArchiveEntry> values, ZhangBoArchiveBounds bounds) {
    return new HashMap<>(epsilonFitness(values, bounds));
  }

  private static double indicator(
      ZhangBoArchiveEntry left, ZhangBoArchiveEntry right,
      ZhangBoArchiveBounds bounds) {
    double result = Double.NEGATIVE_INFINITY;
    for (int objective = 0; objective < 3; objective++) {
      result = Math.max(result,
          bounds.objective(left, objective) - bounds.objective(right, objective));
    }
    return result;
  }

  private ZhangBoArchiveEntry farthest(
      List<ZhangBoArchiveEntry> values, List<ZhangBoArchiveEntry> selected,
      ZhangBoArchiveBounds bounds) {
    ZhangBoArchiveEntry best = null;
    double bestDistance = Double.NEGATIVE_INFINITY;
    for (ZhangBoArchiveEntry candidate : values) {
      if (containsFingerprint(selected, candidate.getFingerprint())) continue;
      double nearest = Double.POSITIVE_INFINITY;
      for (ZhangBoArchiveEntry anchor : selected) {
        nearest = Math.min(nearest, bounds.objectiveDistance(candidate, anchor));
      }
      if (nearest > bestDistance) {
        best = candidate;
        bestDistance = nearest;
      } else if (Double.compare(nearest, bestDistance) == 0 && best != null) {
        best = stableTie(best, candidate, bounds);
      }
    }
    if (best == null) return null;
    List<ZhangBoArchiveEntry> available = new ArrayList<>();
    for (ZhangBoArchiveEntry candidate : values) {
      if (!containsFingerprint(selected, candidate.getFingerprint())) available.add(candidate);
    }
    final List<ZhangBoArchiveEntry> anchors = selected;
    return fatigueNeighborhoodChoice(best, available, bounds,
        new Metric() { public double value(ZhangBoArchiveEntry entry) {
          double nearest = Double.POSITIVE_INFINITY;
          for (ZhangBoArchiveEntry anchor : anchors) {
            nearest = Math.min(nearest, bounds.objectiveDistance(entry, anchor));
          }
          return -nearest;
        }});
  }

  private ZhangBoArchiveEntry fatigueNeighborhoodChoice(
      ZhangBoArchiveEntry primaryBest, List<ZhangBoArchiveEntry> values,
      ZhangBoArchiveBounds bounds, Metric metric) {
    ZhangBoArchiveEntry result = primaryBest;
    for (ZhangBoArchiveEntry candidate : values) {
      if (bounds.objectiveDistance(primaryBest, candidate)
          <= configuration.getSimilarityEpsilon()) {
        double candidateRisk = bounds.fatigueRisk(candidate, configuration);
        double resultRisk = bounds.fatigueRisk(result, configuration);
        if (candidateRisk < resultRisk) result = candidate;
        else if (Double.compare(candidateRisk, resultRisk) == 0) {
          int score = Double.compare(metric.value(candidate), metric.value(result));
          if (score < 0 || (score == 0 && candidate.getFingerprint()
              .compareTo(result.getFingerprint()) < 0)) result = candidate;
        }
      }
    }
    return result;
  }

  private ZhangBoArchiveEntry stableTie(
      ZhangBoArchiveEntry left, ZhangBoArchiveEntry right,
      ZhangBoArchiveBounds bounds) {
    if (bounds.objectiveDistance(left, right) <= configuration.getSimilarityEpsilon()) {
      int risk = Double.compare(bounds.fatigueRisk(left, configuration),
          bounds.fatigueRisk(right, configuration));
      if (risk < 0) return left;
      if (risk > 0) return right;
    }
    return left.getFingerprint().compareTo(right.getFingerprint()) <= 0 ? left : right;
  }

  private static boolean containsFingerprint(
      List<ZhangBoArchiveEntry> values, String fingerprint) {
    for (ZhangBoArchiveEntry value : values) {
      if (value.getFingerprint().equals(fingerprint)) return true;
    }
    return false;
  }

  private static void addDistinct(
      List<ZhangBoArchiveEntry> values, ZhangBoArchiveEntry candidate) {
    if (candidate != null && !containsFingerprint(values, candidate.getFingerprint())) {
      values.add(candidate);
    }
  }

  private static int find(int[] parent, int value) {
    while (parent[value] != value) {
      parent[value] = parent[parent[value]];
      value = parent[value];
    }
    return value;
  }

  private static void union(int[] parent, int left, int right) {
    int leftRoot = find(parent, left);
    int rightRoot = find(parent, right);
    if (leftRoot != rightRoot) {
      if (leftRoot < rightRoot) parent[rightRoot] = leftRoot;
      else parent[leftRoot] = rightRoot;
    }
  }

  private interface Metric { double value(ZhangBoArchiveEntry entry); }
}
