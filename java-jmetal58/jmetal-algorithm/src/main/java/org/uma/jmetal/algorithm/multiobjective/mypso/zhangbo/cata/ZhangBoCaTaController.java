package org.uma.jmetal.algorithm.multiobjective.mypso.zhangbo.cata;

import java.io.Serializable;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.uma.jmetal.algorithm.multiobjective.mypso.zhangbo.ZhangBoSubSwarm;
import org.uma.jmetal.algorithm.multiobjective.mypso.zhangbo.neighborhood.ZhangBoNeighborhoodId;
import org.uma.jmetal.util.pseudorandom.PseudoRandomGenerator;

/** CA-TA Test-and-Apply controller; invalid actions are never recorded here. */
public final class ZhangBoCaTaController implements Serializable {
  private static final long serialVersionUID = 1L;

  public static final class Decision {
    private final boolean testPhase;
    private final boolean exploratory;
    private final List<ZhangBoNeighborhoodId> neighborhoods;
    private final int repetitions;
    private final long contextEpoch;
    private final long callOrdinal;
    private final int remainingApplyCalls;
    private final String reason;

    private Decision(boolean testPhase, boolean exploratory,
        List<ZhangBoNeighborhoodId> neighborhoods, int repetitions,
        long contextEpoch, long callOrdinal, int remainingApplyCalls, String reason) {
      this.testPhase = testPhase;
      this.exploratory = exploratory;
      this.neighborhoods = Collections.unmodifiableList(new ArrayList<>(neighborhoods));
      this.repetitions = repetitions;
      this.contextEpoch = contextEpoch;
      this.callOrdinal = callOrdinal;
      this.remainingApplyCalls = remainingApplyCalls;
      this.reason = reason;
    }

    public boolean isTestPhase() { return testPhase; }
    public boolean isExploratory() { return exploratory; }
    public List<ZhangBoNeighborhoodId> getNeighborhoods() { return neighborhoods; }
    public int getRepetitions() { return repetitions; }
    public long getContextEpoch() { return contextEpoch; }
    public long getCallOrdinal() { return callOrdinal; }
    public int getRemainingApplyCalls() { return remainingApplyCalls; }
    public String getReason() { return reason; }
  }

  private final ZhangBoCaTaConfiguration configuration;
  private final boolean costCreditEnabled;
  private final ZhangBoCaTaStatistics statistics = new ZhangBoCaTaStatistics();
  private final Map<String, Deque<Boolean>> recentSuccesses = new HashMap<>();
  private final Map<String, String> activeMaskSignatures = new HashMap<>();
  private final Map<String, Map<ZhangBoNeighborhoodId, Long>> testEpochBaselines =
      new HashMap<>();
  private final Map<String, ApplyState> applyStates = new HashMap<>();
  private final Map<String, Long> contextEpochs = new HashMap<>();

  public ZhangBoCaTaController(ZhangBoCaTaConfiguration configuration) {
    this(configuration, true);
  }

  public ZhangBoCaTaController(
      ZhangBoCaTaConfiguration configuration, boolean costCreditEnabled) {
    if (configuration == null || !configuration.isEnabled()) {
      throw new IllegalArgumentException("An enabled CA-TA configuration is required");
    }
    this.configuration = configuration;
    this.costCreditEnabled = costCreditEnabled;
  }

  public ZhangBoCaTaStatistics getStatistics() { return statistics; }

  public long getContextEpoch(ZhangBoCaTaContext context) {
    if (context == null) throw new IllegalArgumentException("context");
    return epoch(context.toCanonicalKey());
  }

  public boolean isStagnated(
      ZhangBoSubSwarm role, ZhangBoCaTaPhase phase, ZhangBoBottleneck bottleneck) {
    Deque<Boolean> values = recentSuccesses.get(baseKey(role, phase, bottleneck));
    if (values == null || values.size() < configuration.getStagnationThreshold()) return false;
    for (Boolean value : values) if (Boolean.TRUE.equals(value)) return false;
    return true;
  }

  public Decision decide(
      ZhangBoCaTaContext context, List<ZhangBoNeighborhoodId> valid,
      PseudoRandomGenerator random) {
    if (context == null || random == null) throw new IllegalArgumentException("context and random");
    List<ZhangBoNeighborhoodId> stable = stable(valid);
    if (stable.isEmpty()) {
      return new Decision(true, false, stable, 0, 0L, 0L, 0,
          "NO_VALID_NEIGHBORHOOD");
    }
    String contextKey = context.toCanonicalKey();
    String maskSignature = maskSignature(stable);
    boolean maskChanged = !maskSignature.equals(activeMaskSignatures.get(contextKey));
    if (maskChanged) {
      activeMaskSignatures.put(contextKey, maskSignature);
      beginTestEpoch(contextKey, context, stable);
    }
    Map<ZhangBoNeighborhoodId, Long> baseline = testEpochBaselines.get(contextKey);
    ZhangBoNeighborhoodId best = statistics.best(context, stable, costCreditEnabled);
    boolean untested = !statistics.hasCompleteTest(
        context, stable, baseline, configuration.getNTest());
    boolean exhausted = best != null && statistics.snapshot(context, best)
        .getConsecutiveFailures() >= configuration.getStagnationThreshold();
    if (untested || exhausted) {
      if (exhausted && !untested) beginTestEpoch(contextKey, context, stable);
      return new Decision(true, false, stable, configuration.getNTest(),
          epoch(contextKey), 0L, 0,
          maskChanged ? "MASK_CHANGED_TEST"
              : (untested ? "INCOMPLETE_TEST" : "CONSECUTIVE_FAILURE_RETEST"));
    }
    ApplyState state = applyStates.get(contextKey);
    if (state != null && state.remainingCalls == 0) {
      beginTestEpoch(contextKey, context, stable);
      return new Decision(true, false, stable, configuration.getNTest(),
          epoch(contextKey), 0L, 0, "APPLY_HORIZON_COMPLETE_TEST");
    }
    if (state == null) {
      state = new ApplyState(stable.size() * configuration.getNTest()
          * configuration.getApplyMultiplier());
      applyStates.put(contextKey, state);
    }
    boolean explore = random.nextDouble() < configuration.getApplyExploreProbability();
    ZhangBoNeighborhoodId selected = explore
        ? stable.get(random.nextInt(0, stable.size() - 1)) : best;
    long callOrdinal = state.nextCallOrdinal++;
    state.remainingCalls--;
    return new Decision(false, explore, Collections.singletonList(selected),
        1, epoch(contextKey), callOrdinal, state.remainingCalls,
        explore ? "APPLY_EXPLORATION" : "APPLY_BEST");
  }

  public void record(
      ZhangBoCaTaContext context, ZhangBoNeighborhoodId id,
      boolean success, double directionGain, long wallClockNanos, long fullEvaluations) {
    statistics.record(context, id, success, directionGain, wallClockNanos, fullEvaluations);
    String key = baseKey(context.getSubSwarm(), context.getPhase(), context.getBottleneck());
    Deque<Boolean> values = recentSuccesses.get(key);
    if (values == null) {
      values = new ArrayDeque<>();
      recentSuccesses.put(key, values);
    }
    values.addLast(success);
    while (values.size() > configuration.getStagnationThreshold()) values.removeFirst();
  }

  private static List<ZhangBoNeighborhoodId> stable(List<ZhangBoNeighborhoodId> values) {
    if (values == null) return Collections.emptyList();
    List<ZhangBoNeighborhoodId> result = new ArrayList<>();
    for (ZhangBoNeighborhoodId value : values) if (value != null && !result.contains(value)) result.add(value);
    Collections.sort(result, Comparator.comparingInt(ZhangBoNeighborhoodId::getNumber));
    return result;
  }

  private void beginTestEpoch(String contextKey, ZhangBoCaTaContext context,
      List<ZhangBoNeighborhoodId> stable) {
    Map<ZhangBoNeighborhoodId, Long> baseline =
        new java.util.EnumMap<>(ZhangBoNeighborhoodId.class);
    for (ZhangBoNeighborhoodId id : stable) {
      baseline.put(id, statistics.snapshot(context, id).getCalls());
    }
    testEpochBaselines.put(contextKey, baseline);
    applyStates.remove(contextKey);
    contextEpochs.put(contextKey, epoch(contextKey) + 1L);
  }

  private long epoch(String contextKey) {
    Long value = contextEpochs.get(contextKey);
    return value == null ? 0L : value;
  }

  private static final class ApplyState implements Serializable {
    private static final long serialVersionUID = 1L;
    private int remainingCalls;
    private long nextCallOrdinal;

    private ApplyState(int remainingCalls) {
      this.remainingCalls = remainingCalls;
    }
  }

  private static String maskSignature(List<ZhangBoNeighborhoodId> stable) {
    StringBuilder value = new StringBuilder();
    for (ZhangBoNeighborhoodId id : stable) {
      if (value.length() > 0) value.append(',');
      value.append(id.getNumber());
    }
    return value.toString();
  }

  private static String baseKey(
      ZhangBoSubSwarm role, ZhangBoCaTaPhase phase, ZhangBoBottleneck bottleneck) {
    return role + "|" + phase + "|" + bottleneck;
  }
}
