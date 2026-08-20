package org.uma.jmetal.algorithm.multiobjective.mypso.zhangbo.cata;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.uma.jmetal.algorithm.multiobjective.mypso.zhangbo.neighborhood.ZhangBoNeighborhoodId;

/** Per-(context, neighborhood) Test-and-Apply credit record. */
public final class ZhangBoCaTaStatistics implements Serializable {
  private static final long serialVersionUID = 1L;

  public static final class Snapshot {
    private final long calls;
    private final long successes;
    private final double averageDirectionGain;
    private final double averageWallClockNanos;
    private final double averageFullEvaluations;
    private final long consecutiveFailures;

    private Snapshot(Bucket bucket) {
      this.calls = bucket.calls;
      this.successes = bucket.successes;
      this.averageDirectionGain = bucket.calls == 0L ? 0.0 : bucket.directionGainSum / bucket.calls;
      this.averageWallClockNanos = bucket.calls == 0L
          ? 0.0 : (double) bucket.wallClockNanosSum / bucket.calls;
      this.averageFullEvaluations = bucket.calls == 0L
          ? 0.0 : (double) bucket.fullEvaluationsSum / bucket.calls;
      this.consecutiveFailures = bucket.consecutiveFailures;
    }

    public long getCalls() { return calls; }
    public long getSuccesses() { return successes; }
    public double getAverageDirectionGain() { return averageDirectionGain; }
    public double getAverageWallClockNanos() { return averageWallClockNanos; }
    public double getAverageFullEvaluations() { return averageFullEvaluations; }
    public long getConsecutiveFailures() { return consecutiveFailures; }
  }

  private final Map<ZhangBoCaTaContext, Map<ZhangBoNeighborhoodId, Bucket>> values =
      new HashMap<>();

  public void record(ZhangBoCaTaContext context, ZhangBoNeighborhoodId id,
      boolean success, double directionGain, long wallClockNanos, long fullEvaluations) {
    if (context == null || id == null) throw new IllegalArgumentException("context and id");
    if (!Double.isFinite(directionGain) || wallClockNanos < 0L || fullEvaluations < 0L) {
      throw new IllegalArgumentException("Invalid CA-TA observation");
    }
    Bucket bucket = bucket(context, id);
    bucket.calls++;
    if (success) {
      bucket.successes++;
      bucket.consecutiveFailures = 0L;
    } else {
      bucket.consecutiveFailures++;
    }
    bucket.directionGainSum += directionGain;
    bucket.wallClockNanosSum += wallClockNanos;
    bucket.fullEvaluationsSum += fullEvaluations;
  }

  public Snapshot snapshot(ZhangBoCaTaContext context, ZhangBoNeighborhoodId id) {
    if (context == null || id == null) throw new IllegalArgumentException("context and id");
    return new Snapshot(bucket(context, id));
  }

  public boolean hasObservation(ZhangBoCaTaContext context, List<ZhangBoNeighborhoodId> ids) {
    if (ids == null) throw new IllegalArgumentException("ids");
    for (ZhangBoNeighborhoodId id : ids) if (snapshot(context, id).getCalls() > 0L) return true;
    return false;
  }

  /** True only when every currently legal neighborhood has completed the requested Test quota. */
  public boolean hasCompleteTest(
      ZhangBoCaTaContext context, List<ZhangBoNeighborhoodId> ids,
      Map<ZhangBoNeighborhoodId, Long> baselineCalls, int nTest) {
    if (ids == null || baselineCalls == null || nTest <= 0) {
      throw new IllegalArgumentException("ids, baselineCalls and nTest");
    }
    if (ids.isEmpty()) return false;
    for (ZhangBoNeighborhoodId id : ids) {
      long baseline = baselineCalls.containsKey(id) ? baselineCalls.get(id) : 0L;
      if (snapshot(context, id).getCalls() - baseline < nTest) return false;
    }
    return true;
  }

  /** Required lexicographic credit order: successes, quality, cost, calls, id. */
  public ZhangBoNeighborhoodId best(
      ZhangBoCaTaContext context, List<ZhangBoNeighborhoodId> valid) {
    return best(context, valid, true);
  }

  public ZhangBoNeighborhoodId best(
      ZhangBoCaTaContext context, List<ZhangBoNeighborhoodId> valid,
      final boolean useCostCredit) {
    if (valid == null || valid.isEmpty()) return null;
    final Map<ZhangBoNeighborhoodId, Snapshot> snapshots =
        new EnumMap<>(ZhangBoNeighborhoodId.class);
    List<Double> wallClocks = new ArrayList<>();
    List<Double> fullEvaluations = new ArrayList<>();
    for (ZhangBoNeighborhoodId id : valid) {
      Snapshot value = snapshot(context, id);
      snapshots.put(id, value);
      wallClocks.add(value.getAverageWallClockNanos());
      fullEvaluations.add(value.getAverageFullEvaluations());
    }
    final double wallClockScale = medianDouble(wallClocks);
    final double evaluationScale = medianDouble(fullEvaluations);
    List<ZhangBoNeighborhoodId> sorted = new ArrayList<>(valid);
    Collections.sort(sorted, new Comparator<ZhangBoNeighborhoodId>() {
      @Override public int compare(ZhangBoNeighborhoodId left, ZhangBoNeighborhoodId right) {
        Snapshot a = snapshots.get(left);
        Snapshot b = snapshots.get(right);
        int value = Long.compare(b.getSuccesses(), a.getSuccesses());
        if (value == 0) value = Double.compare(b.getAverageDirectionGain(), a.getAverageDirectionGain());
        if (useCostCredit && value == 0) {
          value = Double.compare(v2Cost(a, wallClockScale, evaluationScale),
              v2Cost(b, wallClockScale, evaluationScale));
        }
        if (value == 0) value = Long.compare(a.getCalls(), b.getCalls());
        if (value == 0) value = Integer.compare(left.getNumber(), right.getNumber());
        return value;
      }
    });
    return sorted.get(0);
  }

  public String toCanonicalText() {
    List<ZhangBoCaTaContext> contexts = new ArrayList<>(values.keySet());
    Collections.sort(contexts, Comparator.comparing(ZhangBoCaTaContext::toCanonicalKey));
    StringBuilder out = new StringBuilder();
    for (ZhangBoCaTaContext context : contexts) {
      for (ZhangBoNeighborhoodId id : ZhangBoNeighborhoodId.values()) {
        Snapshot value = snapshot(context, id);
        if (value.getCalls() == 0L) continue;
        out.append(context.toCanonicalKey()).append('|').append(id).append(".calls=")
            .append(value.getCalls()).append(",successes=").append(value.getSuccesses())
            .append(",averageDirectionGain=").append(value.getAverageDirectionGain())
            .append(",averageWallClockNanos=").append(value.getAverageWallClockNanos())
            .append(",averageFullEvaluations=").append(value.getAverageFullEvaluations())
            .append(",consecutiveFailures=").append(value.getConsecutiveFailures())
            .append('\n');
      }
    }
    return out.toString();
  }

  private Bucket bucket(ZhangBoCaTaContext context, ZhangBoNeighborhoodId id) {
    Map<ZhangBoNeighborhoodId, Bucket> byId = values.get(context);
    if (byId == null) {
      byId = new EnumMap<>(ZhangBoNeighborhoodId.class);
      values.put(context, byId);
    }
    Bucket result = byId.get(id);
    if (result == null) {
      result = new Bucket();
      byId.put(id, result);
    }
    return result;
  }

  private static double medianDouble(List<Double> values) {
    if (values.isEmpty()) return 0.0;
    List<Double> sorted = new ArrayList<>(values);
    Collections.sort(sorted);
    return sorted.get((sorted.size() - 1) / 2);
  }

  private static double v2Cost(
      Snapshot value, double wallClockScale, double evaluationScale) {
    final double epsilon = 1.0e-12;
    return 0.5 * value.getAverageWallClockNanos() / (wallClockScale + epsilon)
        + 0.5 * value.getAverageFullEvaluations() / (evaluationScale + epsilon);
  }

  private static final class Bucket implements Serializable {
    private static final long serialVersionUID = 1L;
    long calls;
    long successes;
    long consecutiveFailures;
    double directionGainSum;
    long wallClockNanosSum;
    long fullEvaluationsSum;
  }
}
