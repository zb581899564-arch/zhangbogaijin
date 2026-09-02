package org.uma.jmetal.algorithm.multiobjective.mypso.zhangbo.qpv2;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.uma.jmetal.algorithm.multiobjective.mypso.zhangbo.ZhangBoArchiveBounds;
import org.uma.jmetal.algorithm.multiobjective.mypso.zhangbo.ZhangBoArchiveEntry;
import org.uma.jmetal.algorithm.multiobjective.mypso.zhangbo.ZhangBoPersonalArchive;
import org.uma.jmetal.algorithm.multiobjective.mypso.zhangbo.ZhangBoPersonalArchiveConfiguration;
import org.uma.jmetal.algorithm.multiobjective.mypso.zhangbo.ZhangBoQpAction;
import org.uma.jmetal.algorithm.multiobjective.mypso.zhangbo.ZhangBoQpCandidateSelector;
import org.uma.jmetal.algorithm.multiobjective.mypso.zhangbo.ZhangBoQpConfiguration;
import org.uma.jmetal.algorithm.multiobjective.mypso.zhangbo.ZhangBoSubSwarm;
import org.uma.jmetal.algorithm.multiobjective.mypso.zhangbo.ZhangBoSubSwarmSemantics;
import org.uma.jmetal.util.pseudorandom.PseudoRandomGenerator;

/**
 * Implements Qp-v2 Candidate A (Action-consistent Top-K Candidate Pool + Uniform Random Exploration).
 *
 * <p>Strict contract rules:
 * <ul>
 *   <li>Canonical candidates and mask are built via {@link ZhangBoQpCandidateSelector#build}
 *       and are NEVER altered by K.
 *   <li>KEEP pool is always the canonical KEEP singleton.
 *   <li>DIRECTIONAL / EPSILON / COMPLEMENTARY pools are ordered by their canonical comparators
 *       with tie-breaking on fingerprint ascending, truncated to {@code min(K, validCount)}.
 *   <li>When {@code poolSize == 1}, {@link #selectLeader} directly returns {@code pool.get(0)}
 *       with ZERO calls to the {@link PseudoRandomGenerator}.
 *   <li>When {@code poolSize >= 2}, {@link #selectLeader} calls {@code random.nextInt(0, poolSize - 1)}.
 * </ul>
 */
public final class V35QpTopKCandidateSelector {

  private final ZhangBoQpCandidateSelector canonicalSelector;
  private final ZhangBoPersonalArchive archive;
  private final ZhangBoQpConfiguration qpConfiguration;
  private final ZhangBoPersonalArchiveConfiguration archiveConfiguration;

  public static final class PoolSelectionResult {
    private final ZhangBoArchiveEntry selected;
    private final int poolSize;
    private final int selectedIndex;
    private final boolean canonical;
    private final boolean drewRng;

    public PoolSelectionResult(
        ZhangBoArchiveEntry selected, int poolSize, int selectedIndex, boolean drewRng) {
      this.selected = selected;
      this.poolSize = poolSize;
      this.selectedIndex = selectedIndex;
      this.canonical = (selectedIndex == 0);
      this.drewRng = drewRng;
    }

    public ZhangBoArchiveEntry getSelected() { return selected; }
    public int getPoolSize() { return poolSize; }
    public int getSelectedIndex() { return selectedIndex; }
    public boolean isCanonical() { return canonical; }
    public boolean isDrewRng() { return drewRng; }
  }

  public static final class PoolsAndCanonical {
    private final ZhangBoQpCandidateSelector.Candidates canonical;
    private final Map<ZhangBoQpAction, List<ZhangBoArchiveEntry>> pools;

    public PoolsAndCanonical(
        ZhangBoQpCandidateSelector.Candidates canonical,
        Map<ZhangBoQpAction, List<ZhangBoArchiveEntry>> pools) {
      this.canonical = canonical;
      this.pools = Collections.unmodifiableMap(pools);
    }

    public ZhangBoQpCandidateSelector.Candidates getCanonical() { return canonical; }
    public Map<ZhangBoQpAction, List<ZhangBoArchiveEntry>> getPools() { return pools; }
    public List<ZhangBoArchiveEntry> getPool(ZhangBoQpAction action) {
      List<ZhangBoArchiveEntry> list = pools.get(action);
      return list != null ? list : Collections.<ZhangBoArchiveEntry>emptyList();
    }
  }

  public V35QpTopKCandidateSelector(
      ZhangBoQpCandidateSelector canonicalSelector,
      ZhangBoPersonalArchive archive,
      ZhangBoQpConfiguration qpConfiguration,
      ZhangBoPersonalArchiveConfiguration archiveConfiguration) {
    if (canonicalSelector == null || archive == null || qpConfiguration == null || archiveConfiguration == null) {
      throw new IllegalArgumentException("V35QpTopKCandidateSelector inputs cannot be null");
    }
    this.canonicalSelector = canonicalSelector;
    this.archive = archive;
    this.qpConfiguration = qpConfiguration;
    this.archiveConfiguration = archiveConfiguration;
  }

  /**
   * Builds the canonical candidate set (and mask) along with the action-consistent top-K pools.
   */
  public PoolsAndCanonical buildPools(
      List<ZhangBoArchiveEntry> entries,
      String requestedFingerprint,
      ZhangBoSubSwarm group,
      ZhangBoArchiveEntry current,
      ZhangBoArchiveEntry gbest,
      ZhangBoArchiveBounds bounds,
      int k) {
    if (k < 1 || k > 4) {
      throw new IllegalArgumentException("K must be in {1, 2, 3, 4}, got: " + k);
    }

    // 1. Always build the canonical candidates and mask
    ZhangBoQpCandidateSelector.Candidates canonical =
        canonicalSelector.build(entries, requestedFingerprint, group, current, gbest, bounds);

    Map<ZhangBoQpAction, List<ZhangBoArchiveEntry>> pools =
        new EnumMap<ZhangBoQpAction, List<ZhangBoArchiveEntry>>(ZhangBoQpAction.class);

    // 2. KEEP: singleton always
    ZhangBoArchiveEntry keepEntry = canonical.get(ZhangBoQpAction.KEEP);
    if (keepEntry != null) {
      pools.put(ZhangBoQpAction.KEEP, Collections.singletonList(keepEntry));
    } else {
      pools.put(ZhangBoQpAction.KEEP, Collections.<ZhangBoArchiveEntry>emptyList());
    }

    List<ZhangBoArchiveEntry> sorted = new ArrayList<ZhangBoArchiveEntry>(entries);
    Collections.sort(sorted, new Comparator<ZhangBoArchiveEntry>() {
      @Override
      public int compare(ZhangBoArchiveEntry o1, ZhangBoArchiveEntry o2) {
        return o1.getFingerprint().compareTo(o2.getFingerprint());
      }
    });

    if (sorted.size() > 1) {
      // 3. DIRECTIONAL pool: (phi asc, fingerprint asc)
      final ZhangBoSubSwarm fGroup = group;
      final ZhangBoArchiveBounds fBounds = bounds;
      List<ZhangBoArchiveEntry> dirList = new ArrayList<ZhangBoArchiveEntry>(sorted);
      Collections.sort(dirList, new Comparator<ZhangBoArchiveEntry>() {
        @Override
        public int compare(ZhangBoArchiveEntry o1, ZhangBoArchiveEntry o2) {
          int c = Double.compare(
              ZhangBoSubSwarmSemantics.archivePhi(o1, fGroup, fBounds),
              ZhangBoSubSwarmSemantics.archivePhi(o2, fGroup, fBounds));
          return c != 0 ? c : o1.getFingerprint().compareTo(o2.getFingerprint());
        }
      });
      int dirLimit = Math.min(k, dirList.size());
      pools.put(ZhangBoQpAction.DIRECTIONAL,
          Collections.unmodifiableList(new ArrayList<ZhangBoArchiveEntry>(dirList.subList(0, dirLimit))));

      // 4. EPSILON pool: (epsilonFitness asc, fingerprint asc)
      final Map<String, Double> epsFit = computeEpsilonFitness(sorted, bounds);
      List<ZhangBoArchiveEntry> epsList = new ArrayList<ZhangBoArchiveEntry>(sorted);
      Collections.sort(epsList, new Comparator<ZhangBoArchiveEntry>() {
        @Override
        public int compare(ZhangBoArchiveEntry o1, ZhangBoArchiveEntry o2) {
          Double f1 = epsFit.get(o1.getFingerprint());
          Double f2 = epsFit.get(o2.getFingerprint());
          double v1 = f1 != null ? f1.doubleValue() : Double.POSITIVE_INFINITY;
          double v2 = f2 != null ? f2.doubleValue() : Double.POSITIVE_INFINITY;
          int c = Double.compare(v1, v2);
          return c != 0 ? c : o1.getFingerprint().compareTo(o2.getFingerprint());
        }
      });
      int epsLimit = Math.min(k, epsList.size());
      pools.put(ZhangBoQpAction.EPSILON,
          Collections.unmodifiableList(new ArrayList<ZhangBoArchiveEntry>(epsList.subList(0, epsLimit))));

      // 5. COMPLEMENTARY pool: within quality set, (cos asc, spacing desc, fingerprint asc)
      double bestPhi = Double.POSITIVE_INFINITY;
      for (ZhangBoArchiveEntry entry : sorted) {
        bestPhi = Math.min(bestPhi, ZhangBoSubSwarmSemantics.archivePhi(entry, group, bounds));
      }
      double[] social = computeDirection(current, gbest, bounds);
      double socialNorm = computeNorm(social);

      List<ZhangBoArchiveEntry> quality = new ArrayList<ZhangBoArchiveEntry>();
      for (ZhangBoArchiveEntry entry : sorted) {
        if (ZhangBoSubSwarmSemantics.archivePhi(entry, group, bounds)
            <= bestPhi + qpConfiguration.getQualityTolerance()) {
          quality.add(entry);
        }
      }

      if (quality.size() >= 2
          && socialNorm > ZhangBoPersonalArchiveConfiguration.DEFAULT_NORMALIZATION_EPSILON) {
        final Map<String, Double> cosMap = new HashMap<String, Double>();
        final Map<String, Double> spacingMap = new HashMap<String, Double>();
        List<ZhangBoArchiveEntry> compList = new ArrayList<ZhangBoArchiveEntry>();

        for (ZhangBoArchiveEntry candidate : quality) {
          double[] personal = computeDirection(current, candidate, bounds);
          if (computeNorm(personal)
              > ZhangBoPersonalArchiveConfiguration.DEFAULT_NORMALIZATION_EPSILON) {
            compList.add(candidate);
            cosMap.put(candidate.getFingerprint(),
                Double.valueOf(computeCosine(personal, social)));
            spacingMap.put(candidate.getFingerprint(),
                Double.valueOf(computeNearestDistance(candidate, sorted, bounds)));
          }
        }

        if (!compList.isEmpty()) {
          Collections.sort(compList, new Comparator<ZhangBoArchiveEntry>() {
            @Override
            public int compare(ZhangBoArchiveEntry o1, ZhangBoArchiveEntry o2) {
              Double c1 = cosMap.get(o1.getFingerprint());
              Double c2 = cosMap.get(o2.getFingerprint());
              int cmpCos = Double.compare(
                  c1 != null ? c1.doubleValue() : Double.POSITIVE_INFINITY,
                  c2 != null ? c2.doubleValue() : Double.POSITIVE_INFINITY);
              if (cmpCos != 0) return cmpCos;

              Double s1 = spacingMap.get(o1.getFingerprint());
              Double s2 = spacingMap.get(o2.getFingerprint());
              // Spacing descending: larger spacing preferred
              int cmpSpacing = Double.compare(
                  s2 != null ? s2.doubleValue() : Double.NEGATIVE_INFINITY,
                  s1 != null ? s1.doubleValue() : Double.NEGATIVE_INFINITY);
              if (cmpSpacing != 0) return cmpSpacing;

              return o1.getFingerprint().compareTo(o2.getFingerprint());
            }
          });
          int compLimit = Math.min(k, compList.size());
          pools.put(ZhangBoQpAction.COMPLEMENTARY,
              Collections.unmodifiableList(new ArrayList<ZhangBoArchiveEntry>(compList.subList(0, compLimit))));
        } else {
          pools.put(ZhangBoQpAction.COMPLEMENTARY, Collections.<ZhangBoArchiveEntry>emptyList());
        }
      } else {
        pools.put(ZhangBoQpAction.COMPLEMENTARY, Collections.<ZhangBoArchiveEntry>emptyList());
      }
    } else {
      pools.put(ZhangBoQpAction.DIRECTIONAL, Collections.<ZhangBoArchiveEntry>emptyList());
      pools.put(ZhangBoQpAction.EPSILON, Collections.<ZhangBoArchiveEntry>emptyList());
      pools.put(ZhangBoQpAction.COMPLEMENTARY, Collections.<ZhangBoArchiveEntry>emptyList());
    }

    return new PoolsAndCanonical(canonical, pools);
  }

  /**
   * Selects a leader from the given pool according to Candidate A rules.
   */
  public PoolSelectionResult selectLeader(
      ZhangBoQpAction action,
      List<ZhangBoArchiveEntry> pool,
      PseudoRandomGenerator random) {
    if (pool == null || pool.isEmpty()) {
      throw new IllegalStateException("Selected masked Qp action " + action + " has empty candidate pool");
    }

    int poolSize = pool.size();
    if (poolSize == 1) {
      // Strict contract: NEVER invoke random when poolSize == 1
      return new PoolSelectionResult(pool.get(0), 1, 0, false);
    }

    // poolSize >= 2: uniform exploration within top-K
    if (random == null) {
      throw new IllegalArgumentException("PseudoRandomGenerator cannot be null when poolSize >= 2");
    }
    int selectedIndex = random.nextInt(0, poolSize - 1);
    return new PoolSelectionResult(pool.get(selectedIndex), poolSize, selectedIndex, true);
  }

  private Map<String, Double> computeEpsilonFitness(
      List<ZhangBoArchiveEntry> values, ZhangBoArchiveBounds bounds) {
    Map<String, Double> result = new HashMap<String, Double>();
    if (values.size() == 1) {
      result.put(values.get(0).getFingerprint(), 0.0);
      return result;
    }
    double scale = archiveConfiguration.getNormalizationEpsilon();
    for (ZhangBoArchiveEntry left : values) {
      for (ZhangBoArchiveEntry right : values) {
        if (left != right) {
          scale = Math.max(scale, Math.abs(computeIndicator(left, right, bounds)));
        }
      }
    }
    for (ZhangBoArchiveEntry candidate : values) {
      double sum = 0.0;
      for (ZhangBoArchiveEntry other : values) {
        if (candidate != other) {
          sum += Math.exp(-computeIndicator(other, candidate, bounds)
              / (scale * archiveConfiguration.getIndicatorKappa()));
        }
      }
      result.put(candidate.getFingerprint(), -sum);
    }
    return result;
  }

  private static double computeIndicator(
      ZhangBoArchiveEntry left, ZhangBoArchiveEntry right,
      ZhangBoArchiveBounds bounds) {
    double result = Double.NEGATIVE_INFINITY;
    for (int objective = 0; objective < 3; objective++) {
      result = Math.max(result,
          bounds.objective(left, objective) - bounds.objective(right, objective));
    }
    return result;
  }

  private static double[] computeDirection(
      ZhangBoArchiveEntry from, ZhangBoArchiveEntry to, ZhangBoArchiveBounds bounds) {
    return new double[]{
        bounds.objective(to, 0) - bounds.objective(from, 0),
        bounds.objective(to, 1) - bounds.objective(from, 1),
        bounds.objective(to, 2) - bounds.objective(from, 2)
    };
  }

  private static double computeNorm(double[] value) {
    double sum = 0.0;
    for (double item : value) sum += item * item;
    return Math.sqrt(sum);
  }

  private static double computeCosine(double[] left, double[] right) {
    double dot = 0.0;
    for (int i = 0; i < left.length; i++) dot += left[i] * right[i];
    return dot / (computeNorm(left) * computeNorm(right)
        + ZhangBoPersonalArchiveConfiguration.DEFAULT_NORMALIZATION_EPSILON);
  }

  private static double computeNearestDistance(
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
}
