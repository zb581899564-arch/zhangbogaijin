package org.uma.jmetal.algorithm.multiobjective.mypso.zhangbo;

import org.uma.jmetal.solution.PermutationSolution;
import org.uma.jmetal.util.pseudorandom.PseudoRandomGenerator;
import org.uma.jmetal.algorithm.multiobjective.mypso.v35.V35DscrSanitizer;
import org.uma.jmetal.algorithm.multiobjective.mypso.v35.V35DscrTeacherCache;
import org.uma.jmetal.algorithm.multiobjective.mypso.v35.V35SocialKnowledgeSnapshot;
import org.uma.jmetal.algorithm.multiobjective.mypso.v35.V35SocialTeacher;
import org.uma.jmetal.algorithm.multiobjective.mypso.v35.V35SubSwarmRole;

import java.io.Serializable;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/** Three-action, two-state original Q-gbest controller adapted to the author solution type. */
public final class ZhangBoQgController implements Serializable {
  private static final long serialVersionUID = 1L;
  private final Map<ZhangBoSubSwarm, double[][]> tables = new EnumMap<>(ZhangBoSubSwarm.class);
  private final Map<ZhangBoSubSwarm, Integer> states = new EnumMap<>(ZhangBoSubSwarm.class);
  private final Map<ZhangBoSubSwarm, PermutationSolution<Integer>> previous = new EnumMap<>(ZhangBoSubSwarm.class);
  private final Map<ZhangBoSubSwarm, PermutationSolution<Integer>> historical = new EnumMap<>(ZhangBoSubSwarm.class);
  private final Map<ZhangBoSubSwarm, Long> previousExposure = new EnumMap<>(ZhangBoSubSwarm.class);
  private final Map<ZhangBoSubSwarm, Long> historicalExposure = new EnumMap<>(ZhangBoSubSwarm.class);
  private final PseudoRandomGenerator random;
  private final double epsilon;
  private final double alpha;
  private final double gamma;
  private final ZhangBoEventLog events = new ZhangBoEventLog();
  private long sequence;
  private long selectionCount;
  private long tdUpdateCount;
  private long frozenObservationCount;
  private long softTdUpdateCount;
  private boolean directionalTeacherPool;
  private int teacherPoolSize = 10;
  /** Observation-only counters; they never participate in action selection. */
  private long directionalPoolRequestCount;
  private long directionalPoolFilteredCount;

  public ZhangBoQgController(
      PseudoRandomGenerator random, double epsilon, double alpha, double gamma) {
    if (random == null) throw new IllegalArgumentException("random");
    requireProbability(epsilon, "epsilon");
    requireProbability(alpha, "alpha");
    requireProbability(gamma, "gamma");
    this.random = random;
    this.epsilon = epsilon;
    this.alpha = alpha;
    this.gamma = gamma;
    for (ZhangBoSubSwarm group : ZhangBoSubSwarmSemantics.roles()) {
      tables.put(group, new double[2][3]);
      states.put(group, 0);
    }
  }

  public Selection select(
      ZhangBoSubSwarm group, List<PermutationSolution<Integer>> candidates) {
    requireCandidates(candidates);
    if (!previous.containsKey(group)) {
      PermutationSolution<Integer> initial = tournament(group, candidates, "initialize");
      previous.put(group, copy(initial));
      historical.put(group, copy(initial));
    }
    int state = states.get(group);
    double draw = random.nextDouble();
    int action;
    if (draw < epsilon) {
      action = bestAction(tables.get(group)[state]);
      log(group, "action", "mode=exploit,state=" + state + ",draw=" + draw + ",action=" + action);
    } else {
      action = random.nextInt(0, 2);
      log(group, "action", "mode=explore,state=" + state + ",draw=" + draw + ",action=" + action);
    }
    PermutationSolution<Integer> leader;
    if (action == 0) {
      leader = copy(previous.get(group));
      increment(previousExposure, group);
    } else if (action == 1) {
      leader = copy(historical.get(group));
      increment(historicalExposure, group);
    } else {
      leader = tournament(group, candidates, "action2");
    }
    previous.put(group, copy(leader));
    PermutationSolution<Integer> best = historical.get(group);
    if (best == null || compare(group, leader, best, candidates) < 0) {
      historical.put(group, copy(leader));
    }
    log(group, "leader", "action=" + action + ",fingerprint=" + fingerprint(leader));
    selectionCount++;
    return new Selection(group, state, action, leader);
  }

  /** Selects greedily from the current table while still executing the selected action semantics. */
  public Selection selectGreedy(
      ZhangBoSubSwarm group, List<PermutationSolution<Integer>> candidates) {
    requireCandidates(candidates);
    if (!previous.containsKey(group)) {
      PermutationSolution<Integer> initial = tournament(group, candidates, "initialize");
      previous.put(group, copy(initial));
      historical.put(group, copy(initial));
    }
    int state = states.get(group);
    int action = bestAction(tables.get(group)[state]);
    log(group, "action", "mode=greedyFrozen,state=" + state + ",action=" + action);
    PermutationSolution<Integer> leader;
    if (action == 0) {
      leader = copy(previous.get(group));
      increment(previousExposure, group);
    } else if (action == 1) {
      leader = copy(historical.get(group));
      increment(historicalExposure, group);
    }
    else leader = tournament(group, candidates, "action2Frozen");
    previous.put(group, copy(leader));
    PermutationSolution<Integer> best = historical.get(group);
    if (best == null || compare(group, leader, best, candidates) < 0) {
      historical.put(group, copy(leader));
    }
    log(group, "leader", "mode=greedyFrozen,action=" + action
        + ",fingerprint=" + fingerprint(leader));
    selectionCount++;
    return new Selection(group, state, action, leader);
  }

  public double settle(
      Selection selection,
      List<PermutationSolution<Integer>> before,
      List<PermutationSolution<Integer>> after) {
    if (selection == null) throw new IllegalArgumentException("selection");
    double reward = reward(selection.getGroup(), before, after);
    int nextState = stateFor(reward);
    double[][] q = tables.get(selection.getGroup());
    double maximum = Math.max(q[nextState][0], Math.max(q[nextState][1], q[nextState][2]));
    int state = selection.getState();
    int action = selection.getAction();
    double old = q[state][action];
    q[state][action] = old + alpha * (reward + gamma * maximum - old);
    states.put(selection.getGroup(), nextState);
    log(selection.getGroup(), "update", "state=" + state + ",action=" + action
        + ",reward=" + reward + ",nextState=" + nextState + ",old=" + old
        + ",new=" + q[state][action]);
    tdUpdateCount++;
    return reward;
  }

  /** Refreshes the observable environment state without changing or crediting the Q table. */
  public double observeWithoutUpdate(
      Selection selection,
      List<PermutationSolution<Integer>> before,
      List<PermutationSolution<Integer>> after) {
    if (selection == null) throw new IllegalArgumentException("selection");
    double delta = reward(selection.getGroup(), before, after);
    int nextState = stateFor(delta);
    states.put(selection.getGroup(), nextState);
    frozenObservationCount++;
    log(selection.getGroup(), "observeFrozen", "state=" + selection.getState()
        + ",action=" + selection.getAction() + ",delta=" + delta
        + ",nextState=" + nextState + ",tableHash=" + tableHash());
    return delta;
  }

  /**
   * V35-FC-4: contribution-gated soft TD update.  Identical to
   * {@link #settle} apart from the learning-rate scale
   * {@code alpha * alphaScale}; used by the frozen controller in a P-block
   * when at least one offspring actually executed a gbest-derived CFVF
   * action.  {@code alphaScale} must lie in (0, 1].
   */
  public double settleWithScaledAlpha(
      Selection selection,
      List<PermutationSolution<Integer>> before,
      List<PermutationSolution<Integer>> after,
      double alphaScale) {
    if (selection == null) throw new IllegalArgumentException("selection");
    if (!(alphaScale > 0.0) || alphaScale > 1.0) {
      throw new IllegalArgumentException("alphaScale must be in (0,1]");
    }
    double reward = reward(selection.getGroup(), before, after);
    int nextState = stateFor(reward);
    double[][] q = tables.get(selection.getGroup());
    double maximum = Math.max(q[nextState][0], Math.max(q[nextState][1], q[nextState][2]));
    int state = selection.getState();
    int action = selection.getAction();
    double old = q[state][action];
    q[state][action] = old + alpha * alphaScale * (reward + gamma * maximum - old);
    states.put(selection.getGroup(), nextState);
    log(selection.getGroup(), "softUpdate", "state=" + state + ",action=" + action
        + ",reward=" + reward + ",nextState=" + nextState + ",old=" + old
        + ",new=" + q[state][action] + ",alphaScale=" + alphaScale);
    tdUpdateCount++;
    softTdUpdateCount++;
    return reward;
  }

  /** V35-FC-4: number of contribution-gated soft TD updates. */
  public long getSoftTdUpdateCount() { return softTdUpdateCount; }

  public static int stateFor(double delta) { return delta >= 0.0 ? 0 : 1; }

  public static double reward(
      ZhangBoSubSwarm group,
      List<PermutationSolution<Integer>> before,
      List<PermutationSolution<Integer>> after) {
    requireNonEmpty(before, "before");
    requireNonEmpty(after, "after");
    if (ZhangBoSubSwarmSemantics.isBoundary(group)) {
      int objective = ZhangBoSubSwarmSemantics.objectiveIndex(group);
      return average(before, objective) - average(after, objective);
    }
    double total = 0.0;
    int[] objectives = new int[] {0, 1, 6};
    for (int objective : objectives) {
      double base = average(before, objective);
      double next = average(after, objective);
      total += (base - next) / Math.max(Math.abs(base), 1.0e-12);
    }
    return total;
  }

  public double[][] getTable(ZhangBoSubSwarm group) {
    double[][] source = tables.get(group);
    return new double[][] {source[0].clone(), source[1].clone()};
  }

  public List<String> getEvents() { return events.snapshot(); }
  public long getEventCount() { return events.getTotalCount(); }
  public String getEventStreamHash() { return events.rollingSha256(); }
  public long getSelectionCount() { return selectionCount; }
  public long getTdUpdateCount() { return tdUpdateCount; }
  public long getFrozenObservationCount() { return frozenObservationCount; }

  /**
   * Sanitizes the actual Qg action-0/action-1 caches. This consumes no FE,
   * no random event, and does not modify Q values or controller states.
   */
  public List<V35DscrTeacherCache.Refresh> sanitizeTeacherCaches(
      ZhangBoSubSwarm group, V35SocialKnowledgeSnapshot snapshot,
      V35DscrTeacherCache ledger, long decisionCycle, long generation, long fe) {
    if (group == null || snapshot == null || ledger == null) {
      throw new IllegalArgumentException("DSCR cache sanitation arguments");
    }
    List<V35DscrTeacherCache.Refresh> events = new ArrayList<>();
    sanitizeOne(group, previous, V35DscrTeacherCache.CacheType.PREVIOUS,
        snapshot, ledger, decisionCycle, generation, fe, events, previousExposure);
    sanitizeOne(group, historical, V35DscrTeacherCache.CacheType.HISTORICAL,
        snapshot, ledger, decisionCycle, generation, fe, events, historicalExposure);
    return events;
  }

  public PermutationSolution<Integer> cachedTeacher(
      ZhangBoSubSwarm group, V35DscrTeacherCache.CacheType cacheType) {
    PermutationSolution<Integer> value = cacheType == V35DscrTeacherCache.CacheType.PREVIOUS
        ? previous.get(group) : historical.get(group);
    return value == null ? null : copy(value);
  }

  public String tableHash() {
    StringBuilder builder = new StringBuilder("subSwarmSemanticsVersion=")
        .append(ZhangBoSubSwarmSemantics.VERSION).append('\n')
        .append("subSwarmRoleMappingSha256=")
        .append(ZhangBoSubSwarmSemantics.mappingHash()).append('\n');
    for (ZhangBoSubSwarm group : ZhangBoSubSwarmSemantics.roles()) {
      double[][] q = tables.get(group);
      builder.append(group).append(':')
          .append(java.util.Arrays.toString(q[0])).append(':')
          .append(java.util.Arrays.toString(q[1])).append('\n');
    }
    return sha256(builder.toString());
  }

  public String toCanonicalText() {
    StringBuilder builder = new StringBuilder("subSwarmSemanticsVersion=")
        .append(ZhangBoSubSwarmSemantics.VERSION).append('\n')
        .append("subSwarmRoleMappingSha256=")
        .append(ZhangBoSubSwarmSemantics.mappingHash()).append('\n');
    for (ZhangBoSubSwarm group : ZhangBoSubSwarmSemantics.roles()) {
      double[][] q = tables.get(group);
      builder.append(group).append(".state=").append(states.get(group)).append('\n');
      builder.append(group).append(".q0=").append(java.util.Arrays.toString(q[0])).append('\n');
      builder.append(group).append(".q1=").append(java.util.Arrays.toString(q[1])).append('\n');
    }
    for (String event : events) builder.append("event=").append(event).append('\n');
    return builder.toString();
  }

  private PermutationSolution<Integer> tournament(
      ZhangBoSubSwarm group, List<PermutationSolution<Integer>> candidates, String reason) {
    List<PermutationSolution<Integer>> pool = pool(group, candidates);
    int leftIndex = random.nextInt(0, pool.size() - 1);
    int rightIndex = random.nextInt(0, pool.size() - 1);
    PermutationSolution<Integer> left = pool.get(leftIndex);
    PermutationSolution<Integer> right = pool.get(rightIndex);
    PermutationSolution<Integer> selected = compare(group, left, right, candidates) <= 0 ? left : right;
    log(group, "tournament", "reason=" + reason + ",left=" + leftIndex + ",right=" + rightIndex
        + ",selected=" + fingerprint(selected));
    return copy(selected);
  }

  /**
   * Directional top-k teacher pool (V35-P10.1).  For boundary subgroups the
   * action-2 tournament draws from the k best candidates in the subgroup's
   * direction instead of the whole nondominated set, so freshly generated
   * direction records have a realistic chance of being selected as teachers.
   * Disabled by default; disabled behaviour is bit-identical to the legacy
   * tournament because the pool is the untouched candidate list.
   */
  public void setDirectionalTeacherPool(boolean enabled, int poolSize) {
    if (enabled && poolSize < 2) {
      throw new IllegalArgumentException("directional teacher pool size must be >= 2");
    }
    if (poolSize < 1) {
      throw new IllegalArgumentException("directional teacher pool size must be >= 1");
    }
    this.directionalTeacherPool = enabled;
    this.teacherPoolSize = poolSize;
  }

  public boolean isDirectionalTeacherPoolEnabled() { return directionalTeacherPool; }
  public int getTeacherPoolSize() { return teacherPoolSize; }
  public long getDirectionalPoolRequestCount() { return directionalPoolRequestCount; }
  public long getDirectionalPoolFilteredCount() { return directionalPoolFilteredCount; }

  private List<PermutationSolution<Integer>> pool(
      ZhangBoSubSwarm group, List<PermutationSolution<Integer>> candidates) {
    if (!directionalTeacherPool || !ZhangBoSubSwarmSemantics.isBoundary(group)) {
      return candidates;
    }
    directionalPoolRequestCount++;
    if (teacherPoolSize >= candidates.size()) return candidates;
    directionalPoolFilteredCount++;
    final int objective = ZhangBoSubSwarmSemantics.objectiveIndex(group);
    List<PermutationSolution<Integer>> sorted = new ArrayList<>(candidates);
    sorted.sort(new java.util.Comparator<PermutationSolution<Integer>>() {
      @Override public int compare(PermutationSolution<Integer> left,
          PermutationSolution<Integer> right) {
        int comparison = Double.compare(left.getObjective(objective), right.getObjective(objective));
        return comparison != 0 ? comparison : fingerprint(left).compareTo(fingerprint(right));
      }
    });
    return new ArrayList<>(sorted.subList(0, Math.min(teacherPoolSize, sorted.size())));
  }

  private void sanitizeOne(ZhangBoSubSwarm group,
      Map<ZhangBoSubSwarm, PermutationSolution<Integer>> cache,
      V35DscrTeacherCache.CacheType cacheType,
      V35SocialKnowledgeSnapshot snapshot, V35DscrTeacherCache ledger,
      long decisionCycle, long generation, long fe,
      List<V35DscrTeacherCache.Refresh> events,
      Map<ZhangBoSubSwarm, Long> exposure) {
    PermutationSolution<Integer> cached = cache.get(group);
    if (cached == null) return;
    V35SubSwarmRole role = role(group);
    V35SocialTeacher before = teacher(cached);
    V35SocialTeacher after = V35DscrSanitizer.sanitize(role, before, snapshot);
    if (!after.getFingerprint().equals(before.getFingerprint())) {
      PermutationSolution<Integer> replacement = snapshot.solutionFor(after.getFingerprint());
      if (replacement == null) {
        throw new IllegalStateException("DSCR replacement solution is missing: "
            + after.getFingerprint());
      }
      cache.put(group, copy(replacement));
    }
    events.add(ledger.recordRefresh(decisionCycle, generation, fe, role, cacheType,
        before, after, snapshot, exposure.containsKey(group) ? exposure.get(group) : 0L));
  }

  private static void increment(Map<ZhangBoSubSwarm, Long> values, ZhangBoSubSwarm group) {
    values.put(group, values.containsKey(group) ? values.get(group) + 1L : 1L);
  }

  private static V35SocialTeacher teacher(PermutationSolution<Integer> solution) {
    return new V35SocialTeacher(new double[] {solution.getObjective(0),
        solution.getObjective(1), solution.getObjective(6)}, fingerprint(solution));
  }

  private static V35SubSwarmRole role(ZhangBoSubSwarm group) {
    switch (group) {
      case G1_CMAX: return V35SubSwarmRole.G1_CMAX;
      case G2_TEC: return V35SubSwarmRole.G2_TEC;
      case G3_TWC: return V35SubSwarmRole.G3_TWC;
      case G4_BALANCED: default: return V35SubSwarmRole.G4_BALANCED;
    }
  }

  private static int compare(
      ZhangBoSubSwarm group,
      PermutationSolution<Integer> left,
      PermutationSolution<Integer> right,
      List<PermutationSolution<Integer>> reference) {
    int comparison;
    if (ZhangBoSubSwarmSemantics.isBoundary(group)) {
      comparison = Double.compare(
          left.getObjective(ZhangBoSubSwarmSemantics.objectiveIndex(group)),
          right.getObjective(ZhangBoSubSwarmSemantics.objectiveIndex(group)));
    } else {
      comparison = Double.compare(pddr(left, reference), pddr(right, reference));
    }
    if (comparison == 0) comparison = fingerprint(left).compareTo(fingerprint(right));
    return comparison;
  }

  private static double pddr(
      PermutationSolution<Integer> solution, List<PermutationSolution<Integer>> reference) {
    org.uma.jmetal.algorithm.multiobjective.mypso.v35.V35ModuleTimer.increment(
        org.uma.jmetal.algorithm.multiobjective.mypso.v35.V35ModuleTimer.C_PDDR_CALLS, 1L);
    org.uma.jmetal.algorithm.multiobjective.mypso.v35.V35ModuleTimer.increment(
        org.uma.jmetal.algorithm.multiobjective.mypso.v35.V35ModuleTimer.C_PDDR_ITEMS,
        reference.size());
    int dominates = 0;
    int dominatedBy = 0;
    for (PermutationSolution<Integer> other : reference) {
      if (other == solution) continue;
      if (dominates(solution, other)) dominates++;
      if (dominates(other, solution)) dominatedBy++;
    }
    return dominatedBy + 1.0 / (dominates + 1.0);
  }

  private static boolean dominates(
      PermutationSolution<Integer> left, PermutationSolution<Integer> right) {
    org.uma.jmetal.algorithm.multiobjective.mypso.v35.V35ModuleTimer.increment(
        org.uma.jmetal.algorithm.multiobjective.mypso.v35.V35ModuleTimer.C_DOMINATES_CALLS, 1L);
    int[] objectives = new int[] {0, 1, 6};
    boolean strict = false;
    for (int objective : objectives) {
      if (left.getObjective(objective) > right.getObjective(objective)) return false;
      if (left.getObjective(objective) < right.getObjective(objective)) strict = true;
    }
    return strict;
  }

  @SuppressWarnings("unchecked")
  private static PermutationSolution<Integer> copy(PermutationSolution<Integer> solution) {
    return (PermutationSolution<Integer>) solution.copy();
  }

  @SuppressWarnings("unchecked")
  public static String fingerprint(PermutationSolution<Integer> solution) {
    return solution.getVariables().toString() + '|' + solution.getVariablesid().toString() + '|'
        + ZhangBoMachineVectorSupport.copy(solution, solution.getNumberOfVariables()).toString()
        + '|' + solution.getVariablesworker().toString();
  }

  private static int bestAction(double[] values) {
    int best = 0;
    for (int action = 1; action < values.length; action++) {
      if (values[action] > values[best]) best = action;
    }
    return best;
  }

  private static double average(List<PermutationSolution<Integer>> values, int objective) {
    double total = 0.0;
    for (PermutationSolution<Integer> value : values) total += value.getObjective(objective);
    return total / values.size();
  }

  private static void requireCandidates(List<PermutationSolution<Integer>> candidates) {
    requireNonEmpty(candidates, "candidates");
  }

  private static void requireNonEmpty(List<?> values, String name) {
    if (values == null || values.isEmpty()) throw new IllegalArgumentException(name + " must be non-empty");
  }

  private static void requireProbability(double value, String name) {
    if (value < 0.0 || value > 1.0 || !Double.isFinite(value)) {
      throw new IllegalArgumentException(name + " must be finite and in [0,1]");
    }
  }

  private static String sha256(String value) {
    try {
      byte[] digest = MessageDigest.getInstance("SHA-256")
          .digest(value.getBytes(StandardCharsets.UTF_8));
      StringBuilder out = new StringBuilder();
      for (byte item : digest) out.append(String.format("%02X", item & 0xff));
      return out.toString();
    } catch (NoSuchAlgorithmException exception) {
      throw new IllegalStateException("SHA-256 unavailable", exception);
    }
  }

  private void log(ZhangBoSubSwarm group, String type, String detail) {
    events.add((sequence++) + ":" + group + ":" + type + ":" + detail);
  }

  public static final class Selection implements Serializable {
    private static final long serialVersionUID = 1L;
    private final ZhangBoSubSwarm group;
    private final int state;
    private final int action;
    private final PermutationSolution<Integer> leader;

    private Selection(
        ZhangBoSubSwarm group, int state, int action, PermutationSolution<Integer> leader) {
      this.group = group;
      this.state = state;
      this.action = action;
      this.leader = leader;
    }

    public ZhangBoSubSwarm getGroup() { return group; }
    public int getState() { return state; }
    public int getAction() { return action; }
    public PermutationSolution<Integer> getLeader() { return copy(leader); }
  }
}
