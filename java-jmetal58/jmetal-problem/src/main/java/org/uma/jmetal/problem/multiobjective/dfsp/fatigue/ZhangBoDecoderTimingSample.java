package org.uma.jmetal.problem.multiobjective.dfsp.fatigue;

/** Immutable timing sample for one successful external decoder evaluation. */
public final class ZhangBoDecoderTimingSample {
  private final long baseDecodeNanos;
  private final long leftShiftNanos;
  private final long rightShiftNanos;
  private final long decoderTotalNanos;
  private final long leftFullRecomputations;
  private final long rightFullRecomputations;
  private final long leftAccepted;
  private final long rightAccepted;

  public ZhangBoDecoderTimingSample(
      long baseDecodeNanos, long leftShiftNanos, long rightShiftNanos,
      long decoderTotalNanos, long leftFullRecomputations,
      long rightFullRecomputations, long leftAccepted, long rightAccepted) {
    requireNonnegative("baseDecodeNanos", baseDecodeNanos);
    requireNonnegative("leftShiftNanos", leftShiftNanos);
    requireNonnegative("rightShiftNanos", rightShiftNanos);
    requireNonnegative("decoderTotalNanos", decoderTotalNanos);
    requireNonnegative("leftFullRecomputations", leftFullRecomputations);
    requireNonnegative("rightFullRecomputations", rightFullRecomputations);
    requireNonnegative("leftAccepted", leftAccepted);
    requireNonnegative("rightAccepted", rightAccepted);
    if (decoderTotalNanos < baseDecodeNanos + leftShiftNanos + rightShiftNanos) {
      throw new IllegalArgumentException(
          "decoderTotalNanos must cover base, left and right stages");
    }
    this.baseDecodeNanos = baseDecodeNanos;
    this.leftShiftNanos = leftShiftNanos;
    this.rightShiftNanos = rightShiftNanos;
    this.decoderTotalNanos = decoderTotalNanos;
    this.leftFullRecomputations = leftFullRecomputations;
    this.rightFullRecomputations = rightFullRecomputations;
    this.leftAccepted = leftAccepted;
    this.rightAccepted = rightAccepted;
  }

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

  private static void requireNonnegative(String name, long value) {
    if (value < 0L) throw new IllegalArgumentException(name + " must be nonnegative");
  }
}
