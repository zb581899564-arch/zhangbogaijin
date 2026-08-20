package org.uma.jmetal.algorithm.multiobjective.mypso.zhangbo;

import java.io.Serializable;

/** Deterministic full-generation warmup and P/G-block scheduler. */
public final class ZhangBoDualQCoordinator implements Serializable {
  private static final long serialVersionUID = 1L;

  public enum Phase { SYNCHRONOUS, WARMUP, P_BLOCK, G_BLOCK }

  private final ZhangBoDualQCoordinationConfiguration configuration;

  public ZhangBoDualQCoordinator(ZhangBoDualQCoordinationConfiguration configuration) {
    if (configuration == null) throw new IllegalArgumentException("configuration");
    this.configuration = configuration;
  }

  public Decision decide(
      long evaluationsBeforeGeneration, long maximumEvaluations, int swarmSize) {
    return decide(evaluationsBeforeGeneration, maximumEvaluations, swarmSize,
        generationFromEvaluationCount(evaluationsBeforeGeneration, swarmSize));
  }

  /**
   * Schedules with an explicit completed-generation counter.  The FE count
   * still controls the 10% warmup boundary, but once warmup has ended
   * local-search evaluations are intentionally ignored for B-block progress.
   *
   * <p>{@code completedOuterGenerations} must be incremented once per global
   * offspring round/generation (i.e. once per {@code updatePosition} call), so
   * that {@code blockLength} is measured in rounds/generations as specified by
   * the v3.5 P/G-block contract.  The Q_Times-sized formal outer cycle is not
   * a valid progress unit: it would compress 50 rounds into one step and the
   * G-block would never be reached inside a 20k/100k budget.
   */
  public Decision decide(
      long evaluationsBeforeGeneration, long maximumEvaluations, int swarmSize,
      long completedOuterGenerations) {
    return decide(evaluationsBeforeGeneration, maximumEvaluations, swarmSize,
        completedOuterGenerations, -1L);
  }

  /**
   * Uses the actually observed round/generation at which the FE warmup ended.
   * A negative anchor preserves the historical calculation for callers that
   * do not run local search during warmup.
   */
  public Decision decide(
      long evaluationsBeforeGeneration, long maximumEvaluations, int swarmSize,
      long completedOuterGenerations, long observedWarmupEndOuterGeneration) {
    if (evaluationsBeforeGeneration < 0L || maximumEvaluations <= 0L || swarmSize <= 0) {
      throw new IllegalArgumentException("Invalid dual-Q scheduling inputs");
    }
    if (completedOuterGenerations < 0L) {
      throw new IllegalArgumentException("completedOuterGenerations must be non-negative");
    }
    if (!configuration.isBlockFrozen()) {
      return new Decision(Phase.SYNCHRONOUS, evaluationsBeforeGeneration,
          evaluationsBeforeGeneration, -1L, -1L, -1);
    }
    long initialEvaluations = swarmSize;
    long targetWarmup = (long) Math.ceil(
        configuration.getWarmupRatio() * maximumEvaluations);
    long extra = Math.max(0L, targetWarmup - initialEvaluations);
    long warmupGenerations = divideRoundingUp(extra, swarmSize);
    long warmupEnd = initialEvaluations + warmupGenerations * swarmSize;
    if (evaluationsBeforeGeneration < warmupEnd) {
      return new Decision(Phase.WARMUP, evaluationsBeforeGeneration,
          warmupEnd, -1L, -1L, -1);
    }
    long warmupOuterGenerations = observedWarmupEndOuterGeneration >= 0L
        ? observedWarmupEndOuterGeneration : warmupGenerations;
    long postWarmupGeneration = Math.max(0L,
        completedOuterGenerations - warmupOuterGenerations);
    int pLength = configuration.getBlockLength();
    int gLength = configuration.getGBlockLength();
    // P and G blocks alternate; each P/G pair consumes pLength + gLength
    // generations. Equal lengths reproduce the historical formula exactly.
    long pairLength = (long) pLength + gLength;
    long pairIndex = postWarmupGeneration / pairLength;
    long withinPair = postWarmupGeneration % pairLength;
    boolean inPBlock = withinPair < pLength;
    long blockIndex = pairIndex * 2L + (inPBlock ? 0L : 1L);
    int offset = (int) (inPBlock ? withinPair : withinPair - pLength);
    Phase phase = inPBlock ? Phase.P_BLOCK : Phase.G_BLOCK;
    return new Decision(phase, evaluationsBeforeGeneration, warmupEnd,
        postWarmupGeneration, blockIndex, offset);
  }

  private static long generationFromEvaluationCount(long evaluations, int swarmSize) {
    return Math.max(0L, (evaluations - swarmSize) / swarmSize);
  }

  private static long divideRoundingUp(long numerator, long denominator) {
    return numerator == 0L ? 0L : 1L + (numerator - 1L) / denominator;
  }

  public static final class Decision implements Serializable {
    private static final long serialVersionUID = 1L;
    private final Phase phase;
    private final long evaluationsBeforeGeneration;
    private final long warmupEndEvaluations;
    private final long postWarmupGeneration;
    private final long blockIndex;
    private final int blockOffset;

    private Decision(
        Phase phase, long evaluationsBeforeGeneration, long warmupEndEvaluations,
        long postWarmupGeneration, long blockIndex, int blockOffset) {
      this.phase = phase;
      this.evaluationsBeforeGeneration = evaluationsBeforeGeneration;
      this.warmupEndEvaluations = warmupEndEvaluations;
      this.postWarmupGeneration = postWarmupGeneration;
      this.blockIndex = blockIndex;
      this.blockOffset = blockOffset;
    }

    public Phase getPhase() { return phase; }
    public long getEvaluationsBeforeGeneration() { return evaluationsBeforeGeneration; }
    public long getWarmupEndEvaluations() { return warmupEndEvaluations; }
    public long getPostWarmupGeneration() { return postWarmupGeneration; }
    public long getBlockIndex() { return blockIndex; }
    public int getBlockOffset() { return blockOffset; }
    public boolean isWarmup() { return phase == Phase.WARMUP; }
    public boolean isPBlock() { return phase == Phase.P_BLOCK; }
    public boolean isGBlock() { return phase == Phase.G_BLOCK; }
    public boolean isSynchronous() { return phase == Phase.SYNCHRONOUS; }

    public String toCanonicalText() {
      return "phase=" + phase + ",evaluationsBefore=" + evaluationsBeforeGeneration
          + ",warmupEnd=" + warmupEndEvaluations + ",postWarmupGeneration="
          + postWarmupGeneration + ",blockIndex=" + blockIndex
          + ",blockOffset=" + blockOffset;
    }
  }
}
