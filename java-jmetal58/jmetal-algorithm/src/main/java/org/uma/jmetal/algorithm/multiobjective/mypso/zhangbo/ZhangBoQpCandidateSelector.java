package org.uma.jmetal.algorithm.multiobjective.mypso.zhangbo;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/** Deterministic four-action mapping and action-mask construction. */
public final class ZhangBoQpCandidateSelector {
  private final ZhangBoQpConfiguration configuration;
  private final ZhangBoPersonalArchive archive;

  public ZhangBoQpCandidateSelector(
      ZhangBoQpConfiguration configuration,
      ZhangBoPersonalArchiveConfiguration archiveConfiguration) {
    if (configuration == null || !configuration.isEnabled()
        || archiveConfiguration == null || !archiveConfiguration.isEnabled()) {
      throw new IllegalArgumentException("Enabled Qp and archive configurations are required");
    }
    this.configuration = configuration;
    this.archive = new ZhangBoPersonalArchive(archiveConfiguration);
  }

  public Candidates build(
      List<ZhangBoArchiveEntry> entries,
      String selectedFingerprint,
      ZhangBoSubSwarm group,
      ZhangBoArchiveEntry current,
      ZhangBoArchiveEntry gbest,
      ZhangBoArchiveBounds bounds) {
    require(entries, group, current, gbest, bounds);
    List<ZhangBoArchiveEntry> sorted = new ArrayList<>(entries);
    Collections.sort(sorted, (left, right) -> left.getFingerprint()
        .compareTo(right.getFingerprint()));
    ZhangBoArchiveEntry directional = directional(sorted, group, bounds);
    ZhangBoArchiveEntry keep = find(sorted, selectedFingerprint);
    if (keep == null) keep = directional;
    Map<ZhangBoQpAction, ZhangBoArchiveEntry> candidates =
        new EnumMap<>(ZhangBoQpAction.class);
    candidates.put(ZhangBoQpAction.KEEP, keep);
    if (sorted.size() > 1) {
      candidates.put(ZhangBoQpAction.DIRECTIONAL, directional);
      candidates.put(ZhangBoQpAction.EPSILON, epsilon(sorted, bounds));
      ZhangBoArchiveEntry complementary = complementary(sorted, group, current, gbest, bounds);
      if (complementary != null) candidates.put(ZhangBoQpAction.COMPLEMENTARY, complementary);
    }
    boolean[] mask = new boolean[ZhangBoQpAction.values().length];
    List<String> used = new ArrayList<>();
    for (ZhangBoQpAction action : ZhangBoQpAction.values()) {
      ZhangBoArchiveEntry candidate = candidates.get(action);
      if (candidate != null && !used.contains(candidate.getFingerprint())) {
        mask[action.ordinal()] = true;
        used.add(candidate.getFingerprint());
      }
    }
    if (!mask[ZhangBoQpAction.KEEP.ordinal()]) {
      throw new IllegalStateException("KEEP must always be valid");
    }
    return new Candidates(candidates, mask, keep.getFingerprint());
  }

  public ZhangBoArchiveEntry directional(
      List<ZhangBoArchiveEntry> entries, ZhangBoSubSwarm group,
      ZhangBoArchiveBounds bounds) {
    ZhangBoArchiveEntry best = entries.get(0);
    for (int index = 1; index < entries.size(); index++) {
      ZhangBoArchiveEntry candidate = entries.get(index);
      int comparison = Double.compare(phi(candidate, group, bounds), phi(best, group, bounds));
      if (comparison < 0 || (comparison == 0 && candidate.getFingerprint()
          .compareTo(best.getFingerprint()) < 0)) best = candidate;
    }
    return best;
  }

  private ZhangBoArchiveEntry epsilon(
      List<ZhangBoArchiveEntry> entries, ZhangBoArchiveBounds bounds) {
    Map<String, Double> fitness = archive.epsilonFitnessValues(entries, bounds);
    ZhangBoArchiveEntry best = entries.get(0);
    for (int index = 1; index < entries.size(); index++) {
      ZhangBoArchiveEntry candidate = entries.get(index);
      int comparison = Double.compare(fitness.get(candidate.getFingerprint()),
          fitness.get(best.getFingerprint()));
      if (comparison < 0 || (comparison == 0 && candidate.getFingerprint()
          .compareTo(best.getFingerprint()) < 0)) best = candidate;
    }
    return best;
  }

  private ZhangBoArchiveEntry complementary(
      List<ZhangBoArchiveEntry> entries, ZhangBoSubSwarm group,
      ZhangBoArchiveEntry current, ZhangBoArchiveEntry gbest,
      ZhangBoArchiveBounds bounds) {
    double bestPhi = Double.POSITIVE_INFINITY;
    for (ZhangBoArchiveEntry entry : entries) {
      bestPhi = Math.min(bestPhi, phi(entry, group, bounds));
    }
    double[] social = direction(current, gbest, bounds);
    if (norm(social) <= ZhangBoPersonalArchiveConfiguration.DEFAULT_NORMALIZATION_EPSILON) {
      return null;
    }
    List<ZhangBoArchiveEntry> quality = new ArrayList<>();
    for (ZhangBoArchiveEntry entry : entries) {
      if (phi(entry, group, bounds) <= bestPhi + configuration.getQualityTolerance()) {
        quality.add(entry);
      }
    }
    if (quality.size() < 2) return null;
    ZhangBoArchiveEntry best = null;
    double bestCosine = Double.POSITIVE_INFINITY;
    double bestSpacing = Double.NEGATIVE_INFINITY;
    for (ZhangBoArchiveEntry candidate : quality) {
      double[] personal = direction(current, candidate, bounds);
      if (norm(personal) <= ZhangBoPersonalArchiveConfiguration.DEFAULT_NORMALIZATION_EPSILON) {
        continue;
      }
      double cosine = cosine(personal, social);
      double spacing = nearestDistance(candidate, entries, bounds);
      if (best == null || cosine < bestCosine
          || (Double.compare(cosine, bestCosine) == 0 && spacing > bestSpacing)
          || (Double.compare(cosine, bestCosine) == 0
          && Double.compare(spacing, bestSpacing) == 0
          && candidate.getFingerprint().compareTo(best.getFingerprint()) < 0)) {
        best = candidate;
        bestCosine = cosine;
        bestSpacing = spacing;
      }
    }
    return best;
  }

  static double phi(
      ZhangBoArchiveEntry entry, ZhangBoSubSwarm group,
      ZhangBoArchiveBounds bounds) {
    return ZhangBoSubSwarmSemantics.archivePhi(entry, group, bounds);
  }

  static double guidanceCosine(
      ZhangBoArchiveEntry current, ZhangBoArchiveEntry pbest,
      ZhangBoArchiveEntry gbest, ZhangBoArchiveBounds bounds) {
    double[] personal = direction(current, pbest, bounds);
    double[] social = direction(current, gbest, bounds);
    if (norm(personal) <= ZhangBoPersonalArchiveConfiguration.DEFAULT_NORMALIZATION_EPSILON
        || norm(social) <= ZhangBoPersonalArchiveConfiguration.DEFAULT_NORMALIZATION_EPSILON) {
      return Double.NaN;
    }
    return cosine(personal, social);
  }

  private static double[] direction(
      ZhangBoArchiveEntry from, ZhangBoArchiveEntry to, ZhangBoArchiveBounds bounds) {
    return new double[]{bounds.objective(to, 0) - bounds.objective(from, 0),
        bounds.objective(to, 1) - bounds.objective(from, 1),
        bounds.objective(to, 2) - bounds.objective(from, 2)};
  }

  private static double cosine(double[] left, double[] right) {
    double dot = 0.0;
    for (int index = 0; index < left.length; index++) dot += left[index] * right[index];
    return dot / (norm(left) * norm(right)
        + ZhangBoPersonalArchiveConfiguration.DEFAULT_NORMALIZATION_EPSILON);
  }

  private static double norm(double[] value) {
    double sum = 0.0;
    for (double item : value) sum += item * item;
    return Math.sqrt(sum);
  }

  private static double nearestDistance(
      ZhangBoArchiveEntry candidate, List<ZhangBoArchiveEntry> entries,
      ZhangBoArchiveBounds bounds) {
    double nearest = Double.POSITIVE_INFINITY;
    for (ZhangBoArchiveEntry other : entries) {
      if (!candidate.getFingerprint().equals(other.getFingerprint())) {
        nearest = Math.min(nearest, bounds.objectiveDistance(candidate, other));
      }
    }
    return nearest;
  }

  static ZhangBoArchiveEntry find(List<ZhangBoArchiveEntry> entries, String fingerprint) {
    if (fingerprint != null) {
      for (ZhangBoArchiveEntry entry : entries) {
        if (fingerprint.equals(entry.getFingerprint())) return entry;
      }
    }
    return null;
  }

  private static void require(
      List<ZhangBoArchiveEntry> entries, ZhangBoSubSwarm group,
      ZhangBoArchiveEntry current, ZhangBoArchiveEntry gbest,
      ZhangBoArchiveBounds bounds) {
    if (entries == null || entries.isEmpty() || group == null || current == null
        || gbest == null || bounds == null) {
      throw new IllegalArgumentException("Qp candidate inputs cannot be null or empty");
    }
  }

  public static final class Candidates {
    private final Map<ZhangBoQpAction, ZhangBoArchiveEntry> candidates;
    private final boolean[] mask;
    private final String resolvedKeepFingerprint;

    private Candidates(Map<ZhangBoQpAction, ZhangBoArchiveEntry> candidates,
                       boolean[] mask, String resolvedKeepFingerprint) {
      this.candidates = new EnumMap<>(candidates);
      this.mask = mask.clone();
      this.resolvedKeepFingerprint = resolvedKeepFingerprint;
    }

    public ZhangBoArchiveEntry get(ZhangBoQpAction action) { return candidates.get(action); }
    public boolean isValid(ZhangBoQpAction action) { return mask[action.ordinal()]; }
    public boolean[] getMask() { return mask.clone(); }
    public String getResolvedKeepFingerprint() { return resolvedKeepFingerprint; }
  }
}
