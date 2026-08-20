package org.uma.jmetal.problem.multiobjective.dfsp.fatigue;

/** Immutable cumulative decoder timing for one problem/experiment instance. */
public final class ZhangBoDecoderTimingSnapshot {
  private final long successfulDecoderCalls;
  private final long baseDecodeNanos;
  private final long leftShiftNanos;
  private final long rightShiftNanos;
  private final long decoderTotalNanos;
  private final long leftFullRecomputations;
  private final long rightFullRecomputations;
  private final long leftAccepted;
  private final long rightAccepted;

  ZhangBoDecoderTimingSnapshot(
      long successfulDecoderCalls, long baseDecodeNanos, long leftShiftNanos,
      long rightShiftNanos, long decoderTotalNanos, long leftFullRecomputations,
      long rightFullRecomputations, long leftAccepted, long rightAccepted) {
    this.successfulDecoderCalls = successfulDecoderCalls;
    this.baseDecodeNanos = baseDecodeNanos;
    this.leftShiftNanos = leftShiftNanos;
    this.rightShiftNanos = rightShiftNanos;
    this.decoderTotalNanos = decoderTotalNanos;
    this.leftFullRecomputations = leftFullRecomputations;
    this.rightFullRecomputations = rightFullRecomputations;
    this.leftAccepted = leftAccepted;
    this.rightAccepted = rightAccepted;
  }

  public static ZhangBoDecoderTimingSnapshot empty() {
    return new ZhangBoDecoderTimingSnapshot(0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L);
  }

  public long getSuccessfulDecoderCalls() { return successfulDecoderCalls; }
  public long getBaseDecodeNanos() { return baseDecodeNanos; }
  public long getLeftShiftNanos() { return leftShiftNanos; }
  public long getRightShiftNanos() { return rightShiftNanos; }
  public long getDecoderTotalNanos() { return decoderTotalNanos; }
  public long getDecoderFrameworkOverheadNanos() {
    return decoderTotalNanos - baseDecodeNanos - leftShiftNanos - rightShiftNanos;
  }
  public long getLeftFullRecomputations() { return leftFullRecomputations; }
  public long getRightFullRecomputations() { return rightFullRecomputations; }
  public long getInternalPropagations() {
    return leftFullRecomputations + rightFullRecomputations;
  }
  public long getLeftAccepted() { return leftAccepted; }
  public long getRightAccepted() { return rightAccepted; }
}
