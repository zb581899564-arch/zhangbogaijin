package org.uma.jmetal.algorithm.multiobjective.mypso.zhangbo.cata;

/** Search phase derived only from the consumed complete-evaluation budget. */
public enum ZhangBoCaTaPhase {
  EARLY,
  MIDDLE,
  LATE;

  public static ZhangBoCaTaPhase fromProgress(long consumedEvaluations, long maximumEvaluations) {
    if (maximumEvaluations <= 0L) {
      throw new IllegalArgumentException("maximumEvaluations must be positive");
    }
    double ratio = Math.max(0.0, Math.min(1.0,
        ((double) Math.max(0L, consumedEvaluations)) / maximumEvaluations));
    if (ratio < 0.33) return EARLY;
    if (ratio < 0.67) return MIDDLE;
    return LATE;
  }
}
