package org.uma.jmetal.algorithm.multiobjective.mypso.v35;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.uma.jmetal.util.pseudorandom.PseudoRandomGenerator;

/**
 * Small, production-isolated Test-and-Apply controller for the v3.5 line.
 * It owns only macro-neighborhood state; candidate construction/evaluation is
 * deliberately delegated to the algorithm layer.
 */
public final class V35CaTaLiteController implements Serializable {
  private static final long serialVersionUID = 1L;

  public static final class Decision implements Serializable {
    private static final long serialVersionUID = 1L;
    private final boolean test;
    private final boolean exploratory;
    private final List<V35MacroNeighborhood> actions;
    private final long epoch;
    private final long callOrdinal;
    private final int remainingApplyCalls;
    private final String reason;

    private Decision(boolean test, boolean exploratory,
        List<V35MacroNeighborhood> actions, long epoch, long callOrdinal,
        int remainingApplyCalls, String reason) {
      this.test = test;
      this.exploratory = exploratory;
      this.actions = Collections.unmodifiableList(new ArrayList<>(actions));
      this.epoch = epoch;
      this.callOrdinal = callOrdinal;
      this.remainingApplyCalls = remainingApplyCalls;
      this.reason = reason;
    }
    public boolean isTest() { return test; }
    public boolean isExploratory() { return exploratory; }
    public List<V35MacroNeighborhood> getActions() { return actions; }
    public long getEpoch() { return epoch; }
    public long getCallOrdinal() { return callOrdinal; }
    public int getRemainingApplyCalls() { return remainingApplyCalls; }
    public String getReason() { return reason; }
  }

  private static final class Stat implements Serializable {
    private static final long serialVersionUID = 1L;
    long calls;
    long successes;
    double gain;
    long evaluations;
    long elapsedNanos;
    long workUnits;
    long failures;
    long consecutiveApplyFailures;
  }

  private static final class State implements Serializable {
    private static final long serialVersionUID = 1L;
    String mask;
    long epoch;
    int remainingApply;
    long ordinal;
    /** V35-FC-3: the top-2 tie probe fires at most once per epoch. */
    boolean probedThisEpoch;
    /** V35-FC-3: cumulative Test/Apply evaluation tallies per context. */
    long testEvaluations;
    long applyEvaluations;
    final Map<V35MacroNeighborhood, Long> testBaseline =
        new EnumMap<>(V35MacroNeighborhood.class);
  }

  private final V35CaTaLiteConfiguration configuration;
  private final Map<String, State> states = new HashMap<>();
  private final Map<String, Map<V35MacroNeighborhood, Stat>> statistics = new HashMap<>();

  public V35CaTaLiteController(V35CaTaLiteConfiguration configuration) {
    if (configuration == null) throw new IllegalArgumentException("configuration");
    this.configuration = configuration;
  }

  public Decision decide(V35CaTaContext context, List<V35MacroNeighborhood> valid,
      PseudoRandomGenerator random) {
    if (context == null || random == null) throw new IllegalArgumentException("context/random");
    List<V35MacroNeighborhood> mask = stable(valid, context);
    String key = context.getRole() + "|" + context.getBottleneck();
    State state = states.get(key);
    if (state == null) { state = new State(); states.put(key, state); }
    String signature = mask.toString();
    if (!signature.equals(state.mask)) {
      state.mask = signature;
      beginTestEpoch(key, state, mask);
    }
    if (mask.isEmpty()) return new Decision(true, false, mask, state.epoch, 0L, 0,
        "NO_VALID_NEIGHBORHOOD");
    V35MacroNeighborhood best = best(key, mask);
    boolean complete = true;
    for (V35MacroNeighborhood action : mask) {
      if (stat(key, action).calls - value(state.testBaseline, action) < configuration.getNTest()) {
        complete = false;
        break;
      }
    }
    if (!complete) {
      return new Decision(true, false, mask, state.epoch, 0L, state.remainingApply,
          "TEST");
    }
    // V35-FC-3: when the top two macros tie on the primary credit key, spend
    // one extra probe FE on each instead of committing blindly.
    if (configuration.isTop2ProbeEnabled() && !state.probedThisEpoch && mask.size() >= 2) {
      List<V35MacroNeighborhood> top2 = topTwo(key, mask);
      if (top2 != null) {
        state.probedThisEpoch = true;
        return new Decision(true, false, top2, state.epoch, 0L, state.remainingApply,
            "TOP2_PROBE");
      }
    }
    boolean retestBudgetAvailable = !testShareExhausted(state);
    if (best != null && stat(key, best).consecutiveApplyFailures
        >= configuration.getStagnationThreshold()) {
      if (!retestBudgetAvailable) {
        return applyCurrentBest(key, state, mask, best,
            "RETEST_SUPPRESSED_TEST_SHARE_CAP");
      }
      beginTestEpoch(key, state, mask);
      return new Decision(true, false, mask, state.epoch, 0L, state.remainingApply,
          "CONSECUTIVE_APPLY_FAILURE_RETEST");
    }
    if (state.remainingApply <= 0) {
      if (!retestBudgetAvailable) {
        return applyCurrentBest(key, state, mask, best,
            "RETEST_SUPPRESSED_TEST_SHARE_CAP");
      }
      beginTestEpoch(key, state, mask);
      return new Decision(true, false, mask, state.epoch, 0L, state.remainingApply,
          "APPLY_HORIZON_COMPLETE_TEST");
    }
    boolean explore = random.nextDouble() < configuration.getApplyExploreProbability();
    V35MacroNeighborhood selected = explore
        ? mask.get(random.nextInt(0, mask.size() - 1)) : best;
    if (selected == null) selected = mask.get(0);
    state.remainingApply--;
    return new Decision(false, explore, Collections.singletonList(selected), state.epoch,
        state.ordinal++, state.remainingApply, explore ? "APPLY_EXPLORE" : "APPLY_BEST");
  }

  /**
   * V35-FC-3: hard Test-share gate, {@code FE_Test <= cap * FE_local}.  With
   * the default cap of 1.0 this is never true, preserving the archived
   * behaviour byte-for-byte.
   */
  private boolean testShareExhausted(State state) {
    long total = state.testEvaluations + state.applyEvaluations;
    if (state.testEvaluations <= 0L || total <= 0L) return false;
    return (double) state.testEvaluations > configuration.getTestFeShareCap() * (double) total;
  }

  /** Keeps applying the incumbent winner instead of an expensive Re-test. */
  private Decision applyCurrentBest(String key, State state, List<V35MacroNeighborhood> mask,
      V35MacroNeighborhood best, String reason) {
    if (best == null) best = mask.get(0);
    state.remainingApply = Math.max(1, state.remainingApply);
    state.remainingApply--;
    return new Decision(false, false, Collections.singletonList(best), state.epoch,
        state.ordinal++, state.remainingApply, reason);
  }

  /**
   * V35-FC-3: the strongest two macros of the mask when they are
   * indistinguishable on the primary credit key (equal successes), or
   * {@code null} when a clear winner exists.
   */
  private List<V35MacroNeighborhood> topTwo(String key, List<V35MacroNeighborhood> mask) {
    V35MacroNeighborhood first = null;
    V35MacroNeighborhood second = null;
    for (V35MacroNeighborhood action : mask) {
      if (first == null || compare(stat(key, action), action, stat(key, first), first,
          0.0, 0.0) < 0) {
        second = first;
        first = action;
      } else if (second == null || compare(stat(key, action), action, stat(key, second),
          second, 0.0, 0.0) < 0) {
        second = action;
      }
    }
    if (first == null || second == null) return null;
    if (stat(key, first).successes != stat(key, second).successes) return null;
    List<V35MacroNeighborhood> pair = new ArrayList<>();
    pair.add(first);
    pair.add(second);
    return pair;
  }

  public void record(V35CaTaContext context, V35MacroNeighborhood action, boolean success,
      double directionGain, int completeEvaluations) {
    record(context, action, success, directionGain, completeEvaluations, 0L, false);
  }

  /** Records one evaluated macro action. Test observations do not count as Apply stagnation. */
  public void record(V35CaTaContext context, V35MacroNeighborhood action, boolean success,
      double directionGain, int completeEvaluations, boolean testPhase) {
    record(context, action, success, directionGain, completeEvaluations, 0L, testPhase);
  }

  public void record(V35CaTaContext context, V35MacroNeighborhood action, boolean success,
      double directionGain, int completeEvaluations, long elapsedNanos, boolean testPhase) {
    record(context, action, success, directionGain, completeEvaluations,
        completeEvaluations, elapsedNanos, testPhase);
  }

  /** Deterministic work units drive action selection; elapsed nanos are diagnostics only. */
  public void record(V35CaTaContext context, V35MacroNeighborhood action, boolean success,
      double directionGain, int completeEvaluations, long workUnits,
      long elapsedNanos, boolean testPhase) {
    if (context == null || action == null) throw new IllegalArgumentException("context/action");
    if (completeEvaluations < 0 || workUnits < 0L || elapsedNanos < 0L
        || !Double.isFinite(directionGain)) {
      throw new IllegalArgumentException("invalid CA-TA-Lite observation");
    }
    String key = context.getRole() + "|" + context.getBottleneck();
    Stat value = stat(key, action);
    State state = states.get(key);
    if (state == null) { state = new State(); states.put(key, state); }
    if (testPhase) state.testEvaluations += completeEvaluations;
    else state.applyEvaluations += completeEvaluations;
    value.calls++;
    value.evaluations += completeEvaluations;
    value.workUnits += workUnits;
    value.elapsedNanos += elapsedNanos;
    if (success) value.successes++;
    else value.failures++;
    if (!testPhase) {
      value.consecutiveApplyFailures = success ? 0L : value.consecutiveApplyFailures + 1L;
    }
    value.gain += directionGain;
  }

  public long getEpoch(V35CaTaContext context) {
    State state = states.get(context.getRole() + "|" + context.getBottleneck());
    return state == null ? 0L : state.epoch;
  }

  public int getRemainingApplyCalls(V35CaTaContext context) {
    State state = states.get(context.getRole() + "|" + context.getBottleneck());
    return state == null ? 0 : state.remainingApply;
  }

  public String canonicalStatistics() {
    List<String> rows = new ArrayList<>();
    for (String key : statistics.keySet()) {
      for (Map.Entry<V35MacroNeighborhood, Stat> entry : statistics.get(key).entrySet()) {
        Stat s = entry.getValue();
        rows.add(key + "|" + entry.getKey() + "|calls=" + s.calls
            + "|successes=" + s.successes + "|gain=" + s.gain
            + "|evaluations=" + s.evaluations + "|failures=" + s.failures
            + "|workUnits=" + s.workUnits
            + "|elapsedNanos=" + s.elapsedNanos
            + "|consecutiveApplyFailures=" + s.consecutiveApplyFailures);
      }
    }
    Collections.sort(rows);
    return String.join("\n", rows);
  }

  private Stat stat(String key, V35MacroNeighborhood action) {
    Map<V35MacroNeighborhood, Stat> bucket = statistics.get(key);
    if (bucket == null) { bucket = new EnumMap<>(V35MacroNeighborhood.class); statistics.put(key, bucket); }
    Stat value = bucket.get(action);
    if (value == null) { value = new Stat(); bucket.put(action, value); }
    return value;
  }

  private static long value(Map<V35MacroNeighborhood, Long> values, V35MacroNeighborhood key) {
    Long result = values.get(key); return result == null ? 0L : result;
  }

  private void beginTestEpoch(String key, State state, List<V35MacroNeighborhood> mask) {
    state.epoch++;
    state.remainingApply = mask.size() * configuration.getNTest()
        * configuration.getApplyMultiplier();
    state.ordinal = 0L;
    state.probedThisEpoch = false;
    state.testBaseline.clear();
    for (V35MacroNeighborhood action : mask) {
      state.testBaseline.put(action, stat(key, action).calls);
      stat(key, action).consecutiveApplyFailures = 0L;
    }
  }

  private V35MacroNeighborhood best(String key, List<V35MacroNeighborhood> mask) {
    final double medianWorkUnits = medianAverageWorkUnits(key, mask);
    final double medianEvaluations = medianAverageEvaluations(key, mask);
    V35MacroNeighborhood result = null;
    for (V35MacroNeighborhood action : mask) {
      if (result == null || compare(stat(key, action), action, stat(key, result), result,
          medianWorkUnits, medianEvaluations) < 0) result = action;
    }
    return result;
  }

  private static int compare(Stat a, V35MacroNeighborhood aa, Stat b, V35MacroNeighborhood bb,
      double medianWorkUnits, double medianEvaluations) {
    int result = Long.compare(b.successes, a.successes);
    if (result == 0) {
      double averageA = a.gain / Math.max(1L, a.calls);
      double averageB = b.gain / Math.max(1L, b.calls);
      result = Double.compare(averageB, averageA);
    }
    if (result == 0) {
      result = Double.compare(cost(a, medianWorkUnits, medianEvaluations),
          cost(b, medianWorkUnits, medianEvaluations));
    }
    if (result == 0) result = Long.compare(a.calls, b.calls);
    if (result == 0) result = Integer.compare(aa.ordinal(), bb.ordinal());
    return result;
  }

  private double medianAverageWorkUnits(String key, List<V35MacroNeighborhood> mask) {
    List<Double> values = new ArrayList<>();
    for (V35MacroNeighborhood action : mask) {
      Stat s = stat(key, action);
      values.add(s.calls == 0L ? 0.0 : (double) s.workUnits / s.calls);
    }
    return median(values);
  }

  private double medianAverageEvaluations(String key, List<V35MacroNeighborhood> mask) {
    List<Double> values = new ArrayList<>();
    for (V35MacroNeighborhood action : mask) {
      Stat s = stat(key, action);
      values.add(s.calls == 0L ? 0.0 : (double) s.evaluations / s.calls);
    }
    return median(values);
  }

  private static double median(List<Double> values) {
    List<Double> sorted = new ArrayList<>(values);
    Collections.sort(sorted);
    return sorted.isEmpty() ? 0.0 : sorted.get((sorted.size() - 1) / 2);
  }

  private static double cost(Stat value, double medianWorkUnits, double medianEvaluations) {
    double averageWorkUnits = value.calls == 0L ? 0.0 : (double) value.workUnits / value.calls;
    double averageEvaluations = value.calls == 0L ? 0.0 : (double) value.evaluations / value.calls;
    return 0.5 * averageWorkUnits / (medianWorkUnits + 1.0e-12)
        + 0.5 * averageEvaluations / (medianEvaluations + 1.0e-12);
  }

  private static List<V35MacroNeighborhood> stable(List<V35MacroNeighborhood> values,
      V35CaTaContext context) {
    List<V35MacroNeighborhood> result = new ArrayList<>();
    if (values != null) for (V35MacroNeighborhood value : values)
      if (value != null && context.allows(value) && !result.contains(value)) result.add(value);
    Collections.sort(result, Comparator.comparingInt(Enum::ordinal));
    return result;
  }
}
