package org.uma.jmetal.problem.multiobjective.dfsp.fatigue;

/** Diagnostic fatigue metrics; they are not optimization objectives. */
public final class ZhangBoFatigueMetrics {
  public final double maximumFatigue;
  public final double averageEventFatigue;
  public final double fatigueExcessIntegral;
  public final double workerFatigueVarianceAtMakespan;
  public final double highFatigueTimeRatio;
  public final double longestContinuousWork;
  public final double totalNaturalRecovery;
  public final int safeThresholdEventCount;

  public ZhangBoFatigueMetrics(
      double maximumFatigue, double averageEventFatigue, double fatigueExcessIntegral,
      double workerFatigueVarianceAtMakespan, double highFatigueTimeRatio,
      double longestContinuousWork, double totalNaturalRecovery,
      int safeThresholdEventCount) {
    this.maximumFatigue = maximumFatigue;
    this.averageEventFatigue = averageEventFatigue;
    this.fatigueExcessIntegral = fatigueExcessIntegral;
    this.workerFatigueVarianceAtMakespan = workerFatigueVarianceAtMakespan;
    this.highFatigueTimeRatio = highFatigueTimeRatio;
    this.longestContinuousWork = longestContinuousWork;
    this.totalNaturalRecovery = totalNaturalRecovery;
    this.safeThresholdEventCount = safeThresholdEventCount;
  }
}
