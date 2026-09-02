package org.uma.jmetal.algorithm.multiobjective.mypso.zhangbo;

import org.uma.jmetal.solution.PermutationSolution;
import org.uma.jmetal.util.pseudorandom.PseudoRandomGenerator;

import java.io.Serializable;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.uma.jmetal.algorithm.multiobjective.mypso.zhangbo.qpv2.V35QpTopKCandidateSelector;
import org.uma.jmetal.algorithm.multiobjective.mypso.zhangbo.qpv2.V35QpTopKConfiguration;
import org.uma.jmetal.algorithm.multiobjective.mypso.zhangbo.qpv2.V35QpV2TelemetrySink;

/** P6.3 subgroup-shared Q-pbest controller with frozen-batch TD updates. */
public final class ZhangBoQpController implements Serializable {
  private static final long serialVersionUID = 1L;
  private static final int STATES = 16;
  private static final int ACTIONS = 4;

  public enum SelectionMode { EPSILON_GREEDY, GREEDY_FROZEN }
  public enum SettlementMode { LEARN, OBSERVE_ONLY, SOFT_LEARN }

  private final ZhangBoQpConfiguration configuration;
  private final ZhangBoPersonalArchiveConfiguration archiveConfiguration;
  private final ZhangBoQpCandidateSelector selector;
  private final ZhangBoPersonalArchive archive;
  private final PseudoRandomGenerator random;
  private final long derivedSeed;
  private final boolean allowMissingFatigueAsZero;
  private final V35QpTopKConfiguration qpTopKConfiguration;
  private final V35QpTopKCandidateSelector topKSelector;
  private V35QpV2TelemetrySink telemetrySink;
  private int currentOuterCycle = 0;
  private int currentQRound = 0;
  private long currentActualFE = 0L;
  private final Map<ZhangBoSubSwarm, double[][]> tables =
      new EnumMap<>(ZhangBoSubSwarm.class);
  private final Map<ZhangBoSubSwarm, double[][]> frozenTables =
      new EnumMap<>(ZhangBoSubSwarm.class);
  private final Map<ZhangBoSubSwarm, GroupStats> previousParentStats =
      new EnumMap<>(ZhangBoSubSwarm.class);
  private final ZhangBoEventLog events = new ZhangBoEventLog();
  private final long[] actionCounts = new long[ACTIONS];
  private final double[] actionRewards = new double[ACTIONS];
  private long sequence;
  private long pbestSwitches;
  private long trainedTransitionCount;
  private long frozenObservationCount;

  public ZhangBoQpController(
      ZhangBoQpConfiguration configuration,
      ZhangBoPersonalArchiveConfiguration archiveConfiguration,
      PseudoRandomGenerator random,
      long derivedSeed) {
    this(configuration, archiveConfiguration, random, derivedSeed, false);
  }

  public ZhangBoQpController(
      ZhangBoQpConfiguration configuration,
      ZhangBoPersonalArchiveConfiguration archiveConfiguration,
      PseudoRandomGenerator random,
      long derivedSeed,
      boolean allowMissingFatigueAsZero) {
    this(configuration, archiveConfiguration, random, derivedSeed, allowMissingFatigueAsZero, V35QpTopKConfiguration.CANONICAL_A4);
  }

  public ZhangBoQpController(
      ZhangBoQpConfiguration configuration,
      ZhangBoPersonalArchiveConfiguration archiveConfiguration,
      PseudoRandomGenerator random,
      long derivedSeed,
      boolean allowMissingFatigueAsZero,
      V35QpTopKConfiguration qpTopKConfiguration) {
    if (configuration == null || !configuration.isEnabled()
        || archiveConfiguration == null || !archiveConfiguration.isEnabled()
        || random == null) {
      throw new IllegalArgumentException("Enabled Qp/archive configuration and random are required");
    }
    this.configuration = configuration;
    this.archiveConfiguration = archiveConfiguration;
    this.selector = new ZhangBoQpCandidateSelector(configuration, archiveConfiguration);
    this.archive = new ZhangBoPersonalArchive(archiveConfiguration);
    this.random = random;
    this.derivedSeed = derivedSeed;
    this.allowMissingFatigueAsZero = allowMissingFatigueAsZero;
    this.qpTopKConfiguration = qpTopKConfiguration != null ? qpTopKConfiguration : V35QpTopKConfiguration.CANONICAL_A4;
    this.topKSelector = this.qpTopKConfiguration.isEnabled()
        ? new V35QpTopKCandidateSelector(this.selector, this.archive, this.configuration, this.archiveConfiguration)
        : null;
    for (ZhangBoSubSwarm group : ZhangBoSubSwarmSemantics.roles()) {
      tables.put(group, new double[STATES][ACTIONS]);
    }
  }

  public void setTelemetrySink(V35QpV2TelemetrySink sink) {
    this.telemetrySink = sink;
  }

  public void setObservationContext(int outerCycle, int qRound, long actualFE) {
    this.currentOuterCycle = outerCycle;
    this.currentQRound = qRound;
    this.currentActualFE = actualFE;
  }

  public List<Selection> selectGroup(
      ZhangBoSubSwarm group,
      List<PermutationSolution<Integer>> particles,
      PermutationSolution<Integer> gbest,
      Map<Long, ZhangBoLineageMemory> memories,
      ZhangBoArchiveBounds bounds,
      long evaluationCount,
      long maximumEvaluations,
      long firstBranchId) {
    return selectGroup(group, particles, gbest, memories, bounds, evaluationCount,
        maximumEvaluations, firstBranchId, SelectionMode.EPSILON_GREEDY);
  }

  public List<Selection> selectGroup(
      ZhangBoSubSwarm group,
      List<PermutationSolution<Integer>> particles,
      PermutationSolution<Integer> gbest,
      Map<Long, ZhangBoLineageMemory> memories,
      ZhangBoArchiveBounds bounds,
      long evaluationCount,
      long maximumEvaluations,
      long firstBranchId,
      SelectionMode selectionMode) {
    if (group == null || particles == null || particles.isEmpty() || gbest == null
        || memories == null || bounds == null || maximumEvaluations <= 0L
        || selectionMode == null) {
      throw new IllegalArgumentException("Invalid Qp group selection inputs");
    }
    double[][] frozen = copyTable(tables.get(group));
    frozenTables.put(group, frozen);
    List<Prepared> prepared = new ArrayList<>();
    ZhangBoArchiveEntry gbestEntry = entry(gbest,
        ZhangBoEvaluatedPddrSelector.Source.PARENT, 0L);
    for (int particleIndex = 0; particleIndex < particles.size(); particleIndex++) {
      PermutationSolution<Integer> particle = particles.get(particleIndex);
      ZhangBoLineageTag tag = lineageTag(particle);
      ZhangBoLineageMemory memory = memories.get(tag.getLineageId());
      if (memory == null) throw new IllegalStateException("Missing lineage " + tag.getLineageId());
      ZhangBoArchiveEntry current = entry(particle,
          ZhangBoEvaluatedPddrSelector.Source.PARENT, 0L);
      String requested = selectedFingerprint(particle);
      ZhangBoQpCandidateSelector.Candidates candidates = selector.build(memory.getEntries(),
          requested, group, current, gbestEntry, bounds);
      String resolved = candidates.getResolvedKeepFingerprint();
      particle.setAttribute(ZhangBoQpLineageState.class,
          new ZhangBoQpLineageState(resolved));
      particle.setAttribute(ZhangBoQpBranchTag.class,
          new ZhangBoQpBranchTag(firstBranchId + particleIndex, tag.getLineageId()));
      ZhangBoArchiveEntry currentPbest = ZhangBoQpCandidateSelector.find(
          memory.getEntries(), resolved);
      double rho = ZhangBoQpCandidateSelector.guidanceCosine(
          current, currentPbest, gbestEntry, bounds);
      prepared.add(new Prepared(firstBranchId + particleIndex, particle, memory, current, gbestEntry,
          candidates, requested, resolved, rho));
    }

    List<Double> validRhos = new ArrayList<>();
    for (Prepared value : prepared) if (Double.isFinite(value.rho)) validRhos.add(value.rho);
    double redundancyThreshold = Math.max(configuration.getRedundancyFloor(), median(validRhos));
    GroupStats currentStats = stats(group, entries(particles), bounds);
    GroupStats previous = previousParentStats.get(group);
    int evolutionNeed = previous == null ? 3 : evolutionNeed(previous, currentStats,
        configuration.getConvergenceTolerance(), configuration.getDiversityTolerance());
    previousParentStats.put(group, currentStats);
    double exploration = explorationProbability(evaluationCount, maximumEvaluations);
    List<Selection> result = new ArrayList<>();
    for (Prepared value : prepared) {
      int stagnation = value.memory.getNoArchiveUpdateCount()
          >= configuration.getStagnationGenerations() ? 1 : 0;
      int redundancy = Double.isFinite(value.rho)
          && value.rho >= redundancyThreshold ? 1 : 0;
      int state = stateIndex(evolutionNeed, stagnation, redundancy);
      int actionIndex = selectionMode == SelectionMode.GREEDY_FROZEN
          ? selectGreedyAction(frozen[state], value.candidates.getMask())
          : selectAction(frozen[state], value.candidates.getMask(), exploration,
              value.memory.getLineageId(), group);
      ZhangBoQpAction action = ZhangBoQpAction.values()[actionIndex];
      ZhangBoArchiveEntry canonicalSelected = value.candidates.get(action);
      if (canonicalSelected == null) throw new IllegalStateException("Selected masked Qp action has no candidate");

      ZhangBoArchiveEntry selected = canonicalSelected;
      int qpPoolSize = 1;
      int qpPoolIndex = 0;
      boolean qpSelectedIsCanonical = true;
      boolean drewExtraRng = false;

      if (topKSelector != null && qpTopKConfiguration.isEnabled()) {
        V35QpTopKCandidateSelector.PoolsAndCanonical poolPackage =
            topKSelector.buildPools(value.memory.getEntries(), value.requestedFingerprint,
                group, value.current, value.gbest, bounds, qpTopKConfiguration.getK());
        List<ZhangBoArchiveEntry> pool = poolPackage.getPool(action);
        V35QpTopKCandidateSelector.PoolSelectionResult poolResult =
            topKSelector.selectLeader(action, pool, random);
        selected = poolResult.getSelected();
        qpPoolSize = poolResult.getPoolSize();
        qpPoolIndex = poolResult.getSelectedIndex();
        qpSelectedIsCanonical = poolResult.isCanonical();
        drewExtraRng = poolResult.isDrewRng();
      }

      if (telemetrySink != null) {
        telemetrySink.recordEvent(new V35QpV2TelemetrySink.QpPoolSelectionEvent(
            currentActualFE, currentOuterCycle, currentQRound,
            value.memory.getLineageId(), group.name(), action.name(),
            maskText(value.candidates.getMask()), value.memory.getEntries().size(),
            qpTopKConfiguration.getK(), qpPoolSize, qpPoolIndex,
            qpSelectedIsCanonical, drewExtraRng,
            selected.getFingerprint(), canonicalSelected.getFingerprint()));
      }
      if (!selected.getFingerprint().equals(value.resolvedFingerprint)) pbestSwitches++;
      actionCounts[actionIndex]++;
      value.particle.setAttribute(ZhangBoQpLineageState.class,
          new ZhangBoQpLineageState(selected.getFingerprint()));
      Selection selection = new Selection(value.branchId, value.memory.getLineageId(), group, state, action,
          value.candidates.getMask(), value.current, value.gbest, selected,
          value.memory.getEntries(), value.memory.getNoArchiveUpdateCount(), currentStats,
          exploration, value.rho, redundancyThreshold);
      result.add(selection);
      log(group, "select", "lineage=" + value.memory.getLineageId() + ",state=" + state
          + ",E=" + evolutionNeed + ",H=" + stagnation + ",R=" + redundancy
          + ",mask=" + maskText(value.candidates.getMask()) + ",action=" + action
          + ",epsilon=" + exploration + ",rho=" + value.rho + ",rhoThreshold="
          + redundancyThreshold + ",pbest=" + selected.getFingerprint()
          + (selectionMode == SelectionMode.GREEDY_FROZEN
              ? ",selectionMode=GREEDY_FROZEN" : ""));
    }
    Collections.sort(result, Comparator.comparingLong(Selection::getBranchId));
    return result;
  }

  public void settle(
      List<PermutationSolution<Integer>> evaluatedChildren,
      Map<Long, Selection> selections,
      ZhangBoArchiveBounds bounds,
      long firstEvaluationOrdinal) {
    settle(evaluatedChildren, selections, bounds, firstEvaluationOrdinal,
        SettlementMode.LEARN);
  }

  public void settle(
      List<PermutationSolution<Integer>> evaluatedChildren,
      Map<Long, Selection> selections,
      ZhangBoArchiveBounds bounds,
      long firstEvaluationOrdinal,
      SettlementMode settlementMode) {
    settle(evaluatedChildren, selections, bounds, firstEvaluationOrdinal,
        settlementMode, 1.0, null);
  }

  /**
   * V35-FC-4: settlement with a contribution-gated soft freeze.  In
   * {@code SOFT_LEARN} only branches whose offspring actually executed at
   * least one pbest-derived CFVF action (members of
   * {@code contributingBranchIds}) earn TD transitions, applied at learning
   * rate {@code alpha * alphaScale}; the rest keep the frozen observation.
   * {@code LEARN} ignores both extra arguments and stays byte-identical.
   */
  public void settle(
      List<PermutationSolution<Integer>> evaluatedChildren,
      Map<Long, Selection> selections,
      ZhangBoArchiveBounds bounds,
      long firstEvaluationOrdinal,
      SettlementMode settlementMode,
      double alphaScale,
      java.util.Set<Long> contributingBranchIds) {
    if (evaluatedChildren == null || selections == null || bounds == null
        || evaluatedChildren.size() != selections.size() || settlementMode == null) {
      throw new IllegalArgumentException("Incomplete Qp settlement batch");
    }
    if (!(alphaScale > 0.0) || alphaScale > 1.0) {
      throw new IllegalArgumentException("alphaScale must be in (0,1]");
    }
    Map<ZhangBoSubSwarm, List<Preview>> grouped =
        new EnumMap<>(ZhangBoSubSwarm.class);
    for (ZhangBoSubSwarm group : ZhangBoSubSwarmSemantics.roles()) grouped.put(group, new ArrayList<Preview>());
    for (int index = 0; index < evaluatedChildren.size(); index++) {
      PermutationSolution<Integer> child = evaluatedChildren.get(index);
      ZhangBoQpBranchTag branch = branchTag(child);
      Selection selection = selections.get(branch.getBranchId());
      if (selection == null) throw new IllegalStateException("No Qp selection for branch " + branch.getBranchId());
      ZhangBoArchiveEntry childEntry = entry(child,
          ZhangBoEvaluatedPddrSelector.Source.GLOBAL_OFFSPRING,
          firstEvaluationOrdinal + index);
      ZhangBoPersonalArchive.Update update = archive.update(selection.previousArchive,
          childEntry, selection.group, bounds);
      List<ZhangBoArchiveEntry> nextArchive = update.getEntries();
      ZhangBoArchiveEntry nextPbest = ZhangBoQpCandidateSelector.find(nextArchive,
          selection.selectedPbest.getFingerprint());
      if (nextPbest == null) nextPbest = selector.directional(nextArchive, selection.group, bounds);
      child.setAttribute(ZhangBoQpLineageState.class,
          new ZhangBoQpLineageState(nextPbest.getFingerprint()));
      // V35-SOURCE-ATTRIBUTION-PATCH: personal-archive observation (pure
      // observation; no-op unless armed; no RNG/FE/behavior change).
      org.uma.jmetal.algorithm.multiobjective.mypso.v35.V35SourceAttributionObserver
          .onPersonalArchiveUpdate(childEntry.getFingerprint(),
              nextPbest.getFingerprint(), update.isInsertedEntrySurvived(),
              selection.getAction() == null ? -1 : selection.getAction().ordinal());
      // V35-SOURCE-ATTRIBUTION-PATCH: personal-archive observation (pure
      // observation; no-op unless armed; no RNG/FE/behavior change).
      org.uma.jmetal.algorithm.multiobjective.mypso.v35.V35SourceAttributionObserver
          .onPersonalArchiveUpdate(childEntry.getFingerprint(),
              nextPbest.getFingerprint(), update.isInsertedEntrySurvived(),
              selection.getAction() == null ? -1 : selection.getAction().ordinal());
      int nextNoUpdate = update.isInsertedEntrySurvived()
          ? 0 : selection.previousNoUpdate + 1;
      grouped.get(selection.group).add(new Preview(selection, child, childEntry,
          nextArchive, nextPbest, update.isInsertedEntrySurvived(), nextNoUpdate));
    }

    for (ZhangBoSubSwarm group : ZhangBoSubSwarmSemantics.roles()) {
      List<Preview> values = grouped.get(group);
      if (values.isEmpty()) continue;
      List<ZhangBoArchiveEntry> childEntries = new ArrayList<>();
      for (Preview value : values) childEntries.add(value.childEntry);
      GroupStats nextStats = stats(group, childEntries, bounds);
      int nextEvolutionNeed = evolutionNeed(values.get(0).selection.groupBefore,
          nextStats, configuration.getConvergenceTolerance(),
          configuration.getDiversityTolerance());
      List<Double> nextRhos = new ArrayList<>();
      for (Preview value : values) {
        value.nextRho = ZhangBoQpCandidateSelector.guidanceCosine(value.childEntry,
            value.nextPbest, value.selection.gbest, bounds);
        if (Double.isFinite(value.nextRho)) nextRhos.add(value.nextRho);
      }
      double nextRedundancyThreshold = Math.max(configuration.getRedundancyFloor(), median(nextRhos));
      List<Transition> transitions = new ArrayList<>();
      for (Preview value : values) {
        int nextH = value.nextNoUpdate >= configuration.getStagnationGenerations() ? 1 : 0;
        int nextR = Double.isFinite(value.nextRho)
            && value.nextRho >= nextRedundancyThreshold ? 1 : 0;
        int nextState = stateIndex(nextEvolutionNeed, nextH, nextR);
        ZhangBoQpCandidateSelector.Candidates nextCandidates = selector.build(
            value.nextArchive, value.nextPbest.getFingerprint(), group,
            value.childEntry, value.selection.gbest, bounds);
        boolean contributing = settlementMode == SettlementMode.LEARN
            || (settlementMode == SettlementMode.SOFT_LEARN
                && contributingBranchIds != null
                && contributingBranchIds.contains(value.selection.branchId));
        if (contributing) {
          Reward reward = reward(value.selection.current, value.childEntry, group,
              value.archiveSurvived, bounds);
          actionRewards[value.selection.action.ordinal()] += reward.total;
          transitions.add(new Transition(value.selection.branchId, value.selection.state,
              value.selection.action.ordinal(), reward.total, nextState,
              nextCandidates.getMask()));
          trainedTransitionCount++;
          log(group, "reward", "lineage=" + value.selection.lineageId + ",action="
              + value.selection.action + ",dom=" + reward.dominance + ",direction="
              + reward.direction + ",archive=" + reward.archive + ",risk="
              + reward.fatigue + ",total=" + reward.total + ",nextState=" + nextState
              + ",nextMask=" + maskText(nextCandidates.getMask()) + ",archiveSurvived="
              + value.archiveSurvived);
        } else {
          frozenObservationCount++;
          log(group, "observeFrozen", "lineage=" + value.selection.lineageId
              + ",action=" + value.selection.action + ",nextState=" + nextState
              + ",nextMask=" + maskText(nextCandidates.getMask())
              + ",archiveSurvived=" + value.archiveSurvived);
        }
      }
      if (settlementMode == SettlementMode.LEARN) {
        batchUpdate(group, transitions);
      } else if (settlementMode == SettlementMode.SOFT_LEARN && !transitions.isEmpty()) {
        batchUpdate(group, transitions, alphaScale);
      }
    }
  }

  /** Builds deterministic directional pbest decisions without reading Qp tables or RNG. */
  public List<ZhangBoPersonalLeaderDecision> selectDirectionalWarmupGroup(
      ZhangBoSubSwarm group,
      List<PermutationSolution<Integer>> particles,
      Map<Long, ZhangBoLineageMemory> memories,
      ZhangBoArchiveBounds bounds,
      long firstBranchId) {
    if (group == null || particles == null || particles.isEmpty()
        || memories == null || bounds == null) {
      throw new IllegalArgumentException("Invalid directional warmup inputs");
    }
    List<ZhangBoPersonalLeaderDecision> result = new ArrayList<>();
    for (int index = 0; index < particles.size(); index++) {
      PermutationSolution<Integer> particle = particles.get(index);
      ZhangBoLineageTag tag = lineageTag(particle);
      ZhangBoLineageMemory memory = memories.get(tag.getLineageId());
      if (memory == null) throw new IllegalStateException("Missing lineage " + tag.getLineageId());
      ZhangBoArchiveEntry selected = selector.directional(memory.getEntries(), group, bounds);
      long branchId = firstBranchId + index;
      particle.setAttribute(ZhangBoQpLineageState.class,
          new ZhangBoQpLineageState(selected.getFingerprint()));
      particle.setAttribute(ZhangBoQpBranchTag.class,
          new ZhangBoQpBranchTag(branchId, tag.getLineageId()));
      result.add(ZhangBoPersonalLeaderDecision.warmupDirectional(branchId, group, selected));
    }
    return result;
  }

  public void reconcilePopulation(
      List<PermutationSolution<Integer>> selected,
      Map<Long, ZhangBoLineageMemory> memories,
      ZhangBoArchiveBounds bounds) {
    for (PermutationSolution<Integer> solution : selected) {
      ZhangBoLineageTag tag = lineageTag(solution);
      ZhangBoLineageMemory memory = memories.get(tag.getLineageId());
      if (memory == null) throw new IllegalStateException("Missing rebuilt lineage " + tag.getLineageId());
      String requested = selectedFingerprint(solution);
      ZhangBoArchiveEntry resolved = ZhangBoQpCandidateSelector.find(memory.getEntries(), requested);
      if (resolved == null) resolved = selector.directional(memory.getEntries(),
          memory.getSubSwarm(), bounds);
      solution.setAttribute(ZhangBoQpLineageState.class,
          new ZhangBoQpLineageState(resolved.getFingerprint()));
      log(memory.getSubSwarm(), "lineage", "lineage=" + tag.getLineageId()
          + ",requested=" + requested + ",resolved=" + resolved.getFingerprint());
    }
  }

  private Reward reward(
      ZhangBoArchiveEntry parent, ZhangBoArchiveEntry child,
      ZhangBoSubSwarm group, boolean archiveSurvived,
      ZhangBoArchiveBounds bounds) {
    double dominance;
    if (dominates(child, parent)) dominance = 1.0;
    else if (dominates(parent, child)) dominance = -1.0;
    else if (sameObjectives(parent, child)) dominance = 0.0;
    else dominance = 0.2;
    double oldPhi = ZhangBoQpCandidateSelector.phi(parent, group, bounds);
    double newPhi = ZhangBoQpCandidateSelector.phi(child, group, bounds);
    double direction = (oldPhi - newPhi)
        / (Math.abs(oldPhi) + archiveConfiguration.getNormalizationEpsilon());
    double archiveContribution = archiveSurvived ? 1.0 : 0.0;
    double fatigue = bounds.fatigueRisk(parent, archiveConfiguration)
        - bounds.fatigueRisk(child, archiveConfiguration);
    double total = configuration.getDominanceWeight() * dominance
        + configuration.getDirectionWeight() * direction
        + configuration.getArchiveWeight() * archiveContribution
        + configuration.getFatigueWeight() * fatigue;
    return new Reward(dominance, direction, archiveContribution, fatigue, total);
  }

  Reward rewardForTest(
      ZhangBoArchiveEntry parent, ZhangBoArchiveEntry child,
      ZhangBoSubSwarm group, boolean archiveSurvived,
      ZhangBoArchiveBounds bounds) {
    return reward(parent, child, group, archiveSurvived, bounds);
  }

  private void batchUpdate(ZhangBoSubSwarm group, List<Transition> transitions) {
    batchUpdate(group, transitions, 1.0);
  }

  private void batchUpdate(ZhangBoSubSwarm group, List<Transition> transitions,
      double alphaScale) {
    Collections.sort(transitions, Comparator.comparingLong(Transition::getLineageId));
    double[][] frozen = frozenTables.get(group);
    double[][] updated = copyTable(tables.get(group));
    Map<Integer, Aggregate> aggregates = new LinkedHashMap<>();
    for (Transition transition : transitions) {
      double maxNext = maxValid(frozen[transition.nextState], transition.nextMask);
      double target = transition.reward + configuration.getGamma() * maxNext;
      int key = transition.state * ACTIONS + transition.action;
      Aggregate aggregate = aggregates.get(key);
      if (aggregate == null) {
        aggregate = new Aggregate();
        aggregates.put(key, aggregate);
      }
      aggregate.sum += target;
      aggregate.count++;
    }
    List<Integer> keys = new ArrayList<>(aggregates.keySet());
    Collections.sort(keys);
    for (Integer key : keys) {
      int state = key / ACTIONS;
      int action = key % ACTIONS;
      Aggregate aggregate = aggregates.get(key);
      double mean = aggregate.sum / aggregate.count;
      double old = frozen[state][action];
      double effectiveAlpha = configuration.getAlpha() * alphaScale;
      updated[state][action] = (1.0 - effectiveAlpha) * old
          + effectiveAlpha * mean;
      log(group, alphaScale == 1.0 ? "update" : "softUpdate",
          "state=" + state + ",action=" + action + ",count="
          + aggregate.count + ",old=" + old + ",targetMean=" + mean
          + ",new=" + updated[state][action]
          + (alphaScale == 1.0 ? "" : ",alphaScale=" + alphaScale));
    }
    tables.put(group, updated);
  }

  void batchUpdateForTest(ZhangBoSubSwarm group, List<Transition> transitions) {
    frozenTables.put(group, copyTable(tables.get(group)));
    batchUpdate(group, new ArrayList<>(transitions));
  }

  void setTableValueForTest(ZhangBoSubSwarm group, int state, int action, double value) {
    tables.get(group)[state][action] = value;
  }

  int selectActionForTest(
      double[] q, boolean[] mask, double exploration,
      long lineageId, ZhangBoSubSwarm group) {
    return selectAction(q, mask, exploration, lineageId, group);
  }

  int selectGreedyActionForTest(double[] q, boolean[] mask) {
    return selectGreedyAction(q, mask);
  }

  double explorationProbabilityForTest(long evaluations, long maximumEvaluations) {
    return explorationProbability(evaluations, maximumEvaluations);
  }

  private int selectAction(
      double[] q, boolean[] mask, double exploration,
      long lineageId, ZhangBoSubSwarm group) {
    double draw = random.nextDouble();
    List<Integer> valid = validActions(mask);
    int selected;
    if (draw < exploration) {
      selected = valid.get(random.nextInt(0, valid.size() - 1));
      log(group, "random", "lineage=" + lineageId + ",draw=" + draw
          + ",epsilon=" + exploration + ",mode=explore,action=" + selected);
    } else {
      selected = valid.get(0);
      for (int action : valid) {
        if (q[action] > q[selected]) selected = action;
      }
      log(group, "random", "lineage=" + lineageId + ",draw=" + draw
          + ",epsilon=" + exploration + ",mode=greedy,action=" + selected);
    }
    return selected;
  }

  private static int selectGreedyAction(double[] q, boolean[] mask) {
    List<Integer> valid = validActions(mask);
    int selected = valid.get(0);
    for (int action : valid) if (q[action] > q[selected]) selected = action;
    return selected;
  }

  private double explorationProbability(long evaluations, long maximumEvaluations) {
    double progress = Math.max(0.0, Math.min(1.0,
        evaluations / (double) maximumEvaluations));
    return configuration.getEpsilonEnd()
        + (configuration.getEpsilonStart() - configuration.getEpsilonEnd())
        * (1.0 - progress);
  }

  static int stateIndex(int evolutionNeed, int stagnation, int redundancy) {
    if (evolutionNeed < 0 || evolutionNeed > 3
        || stagnation < 0 || stagnation > 1 || redundancy < 0 || redundancy > 1) {
      throw new IllegalArgumentException("Invalid Qp state component");
    }
    return 4 * (2 * stagnation + redundancy) + evolutionNeed;
  }

  static int evolutionNeed(
      GroupStats previous, GroupStats current,
      double convergenceTolerance, double diversityTolerance) {
    double deltaC = (previous.convergence - current.convergence)
        / (Math.abs(previous.convergence)
        + ZhangBoPersonalArchiveConfiguration.DEFAULT_NORMALIZATION_EPSILON);
    double deltaD = (current.diversity - previous.diversity)
        / (Math.abs(previous.diversity)
        + ZhangBoPersonalArchiveConfiguration.DEFAULT_NORMALIZATION_EPSILON);
    if (deltaC > convergenceTolerance && deltaD > -diversityTolerance) return 0;
    if (deltaC > convergenceTolerance && deltaD <= -diversityTolerance) return 1;
    if (deltaC <= convergenceTolerance && deltaD > diversityTolerance) return 2;
    return 3;
  }

  static GroupStats stats(
      ZhangBoSubSwarm group, List<ZhangBoArchiveEntry> entries,
      ZhangBoArchiveBounds bounds) {
    if (entries.isEmpty()) throw new IllegalArgumentException("Empty subgroup");
    double convergence = 0.0;
    for (ZhangBoArchiveEntry entry : entries) {
      if (group == ZhangBoSubSwarm.G4_BALANCED) {
        convergence += (bounds.objective(entry, 0) + bounds.objective(entry, 1)
            + bounds.objective(entry, 2)) / 3.0;
      } else {
        convergence += ZhangBoQpCandidateSelector.phi(entry, group, bounds);
      }
    }
    convergence /= entries.size();
    double diversity = 0.0;
    if (entries.size() > 1) {
      for (int left = 0; left < entries.size(); left++) {
        double nearest = Double.POSITIVE_INFINITY;
        for (int right = 0; right < entries.size(); right++) {
          if (left != right) nearest = Math.min(nearest,
              bounds.objectiveDistance(entries.get(left), entries.get(right)));
        }
        diversity += nearest;
      }
      diversity /= entries.size();
    }
    return new GroupStats(convergence, diversity);
  }

  private static boolean dominates(ZhangBoArchiveEntry left, ZhangBoArchiveEntry right) {
    boolean strict = false;
    for (int objective = 0; objective < 3; objective++) {
      if (left.getObjective(objective) > right.getObjective(objective)) return false;
      if (left.getObjective(objective) < right.getObjective(objective)) strict = true;
    }
    return strict;
  }

  private static boolean sameObjectives(ZhangBoArchiveEntry left, ZhangBoArchiveEntry right) {
    return Arrays.equals(left.getObjectives(), right.getObjectives());
  }

  private static double maxValid(double[] q, boolean[] mask) {
    double result = Double.NEGATIVE_INFINITY;
    for (int action = 0; action < mask.length; action++) {
      if (mask[action]) result = Math.max(result, q[action]);
    }
    if (!Double.isFinite(result)) throw new IllegalStateException("No valid next Qp action");
    return result;
  }

  private static List<Integer> validActions(boolean[] mask) {
    List<Integer> result = new ArrayList<>();
    for (int action = 0; action < mask.length; action++) if (mask[action]) result.add(action);
    if (result.isEmpty()) throw new IllegalStateException("Qp action mask is empty");
    return result;
  }

  private static double median(List<Double> values) {
    if (values.isEmpty()) return Double.NEGATIVE_INFINITY;
    List<Double> sorted = new ArrayList<>(values);
    Collections.sort(sorted);
    int middle = sorted.size() / 2;
    return sorted.size() % 2 == 1 ? sorted.get(middle)
        : (sorted.get(middle - 1) + sorted.get(middle)) / 2.0;
  }

  private static ZhangBoLineageTag lineageTag(PermutationSolution<Integer> solution) {
    Object value = solution.getAttribute(ZhangBoLineageTag.class);
    if (!(value instanceof ZhangBoLineageTag)) {
      throw new IllegalStateException("Qp candidate has no lineage tag");
    }
    return (ZhangBoLineageTag) value;
  }

  private static ZhangBoQpBranchTag branchTag(PermutationSolution<Integer> solution) {
    Object value = solution.getAttribute(ZhangBoQpBranchTag.class);
    if (!(value instanceof ZhangBoQpBranchTag)) {
      throw new IllegalStateException("Qp child has no branch tag");
    }
    return (ZhangBoQpBranchTag) value;
  }

  private static String selectedFingerprint(PermutationSolution<Integer> solution) {
    Object value = solution.getAttribute(ZhangBoQpLineageState.class);
    return value instanceof ZhangBoQpLineageState
        ? ((ZhangBoQpLineageState) value).getSelectedPbestFingerprint() : null;
  }

  private ZhangBoArchiveEntry entry(
      PermutationSolution<Integer> solution,
      ZhangBoEvaluatedPddrSelector.Source source,
      long evaluationOrdinal) {
    return ZhangBoArchiveEntry.fromSolution(solution, source, 0, evaluationOrdinal,
        allowMissingFatigueAsZero);
  }

  private List<ZhangBoArchiveEntry> entries(
      List<PermutationSolution<Integer>> solutions) {
    List<ZhangBoArchiveEntry> result = new ArrayList<>();
    long ordinal = 0L;
    for (PermutationSolution<Integer> solution : solutions) {
      result.add(entry(solution, ZhangBoEvaluatedPddrSelector.Source.PARENT, ++ordinal));
    }
    return result;
  }

  private static double[][] copyTable(double[][] source) {
    double[][] result = new double[source.length][];
    for (int index = 0; index < source.length; index++) result[index] = source[index].clone();
    return result;
  }

  private static String maskText(boolean[] mask) {
    StringBuilder out = new StringBuilder();
    for (boolean value : mask) out.append(value ? '1' : '0');
    return out.toString();
  }

  private void log(ZhangBoSubSwarm group, String type, String detail) {
    events.add("event=" + (++sequence) + ",group=" + group + ",type=" + type + ',' + detail);
  }

  public double[][] getTable(ZhangBoSubSwarm group) { return copyTable(tables.get(group)); }
  public List<String> getEvents() { return events.snapshot(); }
  public long getEventCount() { return events.getTotalCount(); }
  public String getEventStreamHash() { return events.rollingSha256(); }
  public long getPbestSwitches() { return pbestSwitches; }
  public long getActionCount(ZhangBoQpAction action) { return actionCounts[action.ordinal()]; }
  public double getAverageReward(ZhangBoQpAction action) {
    long count = actionCounts[action.ordinal()];
    return count == 0L ? 0.0 : actionRewards[action.ordinal()] / count;
  }
  public long getTrainedTransitionCount() { return trainedTransitionCount; }
  public long getFrozenObservationCount() { return frozenObservationCount; }
  public long getExecutedActionCount() {
    long total = 0L;
    for (long value : actionCounts) total += value;
    return total;
  }

  public String tableHash() {
    StringBuilder out = new StringBuilder("subSwarmSemanticsVersion=")
        .append(ZhangBoSubSwarmSemantics.VERSION).append('\n')
        .append("subSwarmRoleMappingSha256=")
        .append(ZhangBoSubSwarmSemantics.mappingHash()).append('\n');
    for (ZhangBoSubSwarm group : ZhangBoSubSwarmSemantics.roles()) {
      double[][] q = tables.get(group);
      for (int state = 0; state < q.length; state++) {
        out.append(group).append(':').append(state).append(':')
            .append(Arrays.toString(q[state])).append('\n');
      }
    }
    return sha256(out.toString());
  }

  public String toCanonicalText() {
    StringBuilder out = new StringBuilder("subSwarmSemanticsVersion=")
        .append(ZhangBoSubSwarmSemantics.VERSION).append('\n')
        .append("subSwarmRoleMappingSha256=")
        .append(ZhangBoSubSwarmSemantics.mappingHash()).append('\n');
    out.append("derivedSeed=").append(derivedSeed).append('\n');
    for (ZhangBoSubSwarm group : ZhangBoSubSwarmSemantics.roles()) {
      double[][] q = tables.get(group);
      for (int state = 0; state < q.length; state++) {
        out.append(group).append(".q").append(state).append('=')
            .append(q[state][0]).append(',').append(q[state][1]).append(',')
            .append(q[state][2]).append(',').append(q[state][3]).append('\n');
      }
    }
    for (String event : events) out.append(event).append('\n');
    return out.toString();
  }

  public static final class GroupStats implements Serializable {
    private static final long serialVersionUID = 1L;
    private final double convergence;
    private final double diversity;
    public GroupStats(double convergence, double diversity) {
      this.convergence = convergence;
      this.diversity = diversity;
    }
    public double getConvergence() { return convergence; }
    public double getDiversity() { return diversity; }
  }

  public static final class Selection implements Serializable {
    private static final long serialVersionUID = 1L;
    private final long branchId;
    private final long lineageId;
    private final ZhangBoSubSwarm group;
    private final int state;
    private final ZhangBoQpAction action;
    private final boolean[] mask;
    private final ZhangBoArchiveEntry current;
    private final ZhangBoArchiveEntry gbest;
    private final ZhangBoArchiveEntry selectedPbest;
    private final List<ZhangBoArchiveEntry> previousArchive;
    private final int previousNoUpdate;
    private final GroupStats groupBefore;
    private final double explorationProbability;
    private final double redundancyCosine;
    private final double redundancyThreshold;

    private Selection(
        long branchId, long lineageId, ZhangBoSubSwarm group, int state, ZhangBoQpAction action,
        boolean[] mask, ZhangBoArchiveEntry current, ZhangBoArchiveEntry gbest,
        ZhangBoArchiveEntry selectedPbest, List<ZhangBoArchiveEntry> previousArchive,
        int previousNoUpdate, GroupStats groupBefore, double explorationProbability,
        double redundancyCosine, double redundancyThreshold) {
      this.branchId = branchId;
      this.lineageId = lineageId;
      this.group = group;
      this.state = state;
      this.action = action;
      this.mask = mask.clone();
      this.current = current;
      this.gbest = gbest;
      this.selectedPbest = selectedPbest;
      this.previousArchive = new ArrayList<>(previousArchive);
      this.previousNoUpdate = previousNoUpdate;
      this.groupBefore = groupBefore;
      this.explorationProbability = explorationProbability;
      this.redundancyCosine = redundancyCosine;
      this.redundancyThreshold = redundancyThreshold;
    }

    public long getBranchId() { return branchId; }
    public long getLineageId() { return lineageId; }
    public ZhangBoSubSwarm getGroup() { return group; }
    public int getState() { return state; }
    public ZhangBoQpAction getAction() { return action; }
    public boolean[] getMask() { return mask.clone(); }
    public String getSelectedPbestFingerprint() { return selectedPbest.getFingerprint(); }
    public PermutationSolution<Integer> pbestSolution(PermutationSolution<Integer> template) {
      return selectedPbest.toSolution(template);
    }
    public double getExplorationProbability() { return explorationProbability; }
    public double getRedundancyCosine() { return redundancyCosine; }
    public double getRedundancyThreshold() { return redundancyThreshold; }
  }

  public static final class Transition {
    private final long lineageId;
    private final int state;
    private final int action;
    private final double reward;
    private final int nextState;
    private final boolean[] nextMask;
    public Transition(long lineageId, int state, int action, double reward,
                      int nextState, boolean[] nextMask) {
      this.lineageId = lineageId;
      this.state = state;
      this.action = action;
      this.reward = reward;
      this.nextState = nextState;
      this.nextMask = nextMask.clone();
    }
    public long getLineageId() { return lineageId; }
  }

  private static final class Prepared {
    private final long branchId;
    private final PermutationSolution<Integer> particle;
    private final ZhangBoLineageMemory memory;
    private final ZhangBoArchiveEntry current;
    private final ZhangBoArchiveEntry gbest;
    private final ZhangBoQpCandidateSelector.Candidates candidates;
    private final String requestedFingerprint;
    private final String resolvedFingerprint;
    private final double rho;
    private Prepared(long branchId, PermutationSolution<Integer> particle, ZhangBoLineageMemory memory,
                     ZhangBoArchiveEntry current, ZhangBoArchiveEntry gbest,
                     ZhangBoQpCandidateSelector.Candidates candidates,
                     String requestedFingerprint, String resolvedFingerprint, double rho) {
      this.branchId = branchId;
      this.particle = particle;
      this.memory = memory;
      this.current = current;
      this.gbest = gbest;
      this.candidates = candidates;
      this.requestedFingerprint = requestedFingerprint;
      this.resolvedFingerprint = resolvedFingerprint;
      this.rho = rho;
    }
  }

  private static final class Preview {
    private final Selection selection;
    private final PermutationSolution<Integer> child;
    private final ZhangBoArchiveEntry childEntry;
    private final List<ZhangBoArchiveEntry> nextArchive;
    private final ZhangBoArchiveEntry nextPbest;
    private final boolean archiveSurvived;
    private final int nextNoUpdate;
    private double nextRho;
    private Preview(Selection selection, PermutationSolution<Integer> child,
                    ZhangBoArchiveEntry childEntry, List<ZhangBoArchiveEntry> nextArchive,
                    ZhangBoArchiveEntry nextPbest, boolean archiveSurvived, int nextNoUpdate) {
      this.selection = selection;
      this.child = child;
      this.childEntry = childEntry;
      this.nextArchive = nextArchive;
      this.nextPbest = nextPbest;
      this.archiveSurvived = archiveSurvived;
      this.nextNoUpdate = nextNoUpdate;
    }
  }

  static final class Reward {
    private final double dominance;
    private final double direction;
    private final double archive;
    private final double fatigue;
    private final double total;
    private Reward(double dominance, double direction, double archive,
                   double fatigue, double total) {
      this.dominance = dominance;
      this.direction = direction;
      this.archive = archive;
      this.fatigue = fatigue;
      this.total = total;
    }
    double getDominance() { return dominance; }
    double getDirection() { return direction; }
    double getArchive() { return archive; }
    double getFatigue() { return fatigue; }
    double getTotal() { return total; }
  }

  private static final class Aggregate { private double sum; private int count; }

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
}
