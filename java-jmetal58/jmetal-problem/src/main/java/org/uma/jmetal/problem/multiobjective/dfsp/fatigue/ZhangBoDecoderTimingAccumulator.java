package org.uma.jmetal.problem.multiobjective.dfsp.fatigue;

/** Thread-safe, append-only timing accumulator owned by one production problem. */
final class ZhangBoDecoderTimingAccumulator {
  private long successfulDecoderCalls;
  private long baseDecodeNanos;
  private long leftShiftNanos;
  private long rightShiftNanos;
  private long decoderTotalNanos;
  private long leftFullRecomputations;
  private long rightFullRecomputations;
  private long leftAccepted;
  private long rightAccepted;

  synchronized void record(ZhangBoDecoderTimingSample sample) {
    if (sample == null) throw new IllegalArgumentException("timing sample must not be null");
    successfulDecoderCalls = add(successfulDecoderCalls, 1L);
    baseDecodeNanos = add(baseDecodeNanos, sample.getBaseDecodeNanos());
    leftShiftNanos = add(leftShiftNanos, sample.getLeftShiftNanos());
    rightShiftNanos = add(rightShiftNanos, sample.getRightShiftNanos());
    decoderTotalNanos = add(decoderTotalNanos, sample.getDecoderTotalNanos());
    leftFullRecomputations = add(
        leftFullRecomputations, sample.getLeftFullRecomputations());
    rightFullRecomputations = add(
        rightFullRecomputations, sample.getRightFullRecomputations());
    leftAccepted = add(leftAccepted, sample.getLeftAccepted());
    rightAccepted = add(rightAccepted, sample.getRightAccepted());
  }

  synchronized ZhangBoDecoderTimingSnapshot snapshot() {
    return new ZhangBoDecoderTimingSnapshot(successfulDecoderCalls, baseDecodeNanos,
        leftShiftNanos, rightShiftNanos, decoderTotalNanos,
        leftFullRecomputations, rightFullRecomputations, leftAccepted, rightAccepted);
  }

  private static long add(long left, long right) {
    if (right > 0L && left > Long.MAX_VALUE - right) return Long.MAX_VALUE;
    return left + right;
  }
}
