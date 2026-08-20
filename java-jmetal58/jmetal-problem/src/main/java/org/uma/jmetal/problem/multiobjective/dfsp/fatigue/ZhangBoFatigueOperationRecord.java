package org.uma.jmetal.problem.multiobjective.dfsp.fatigue;

/** Immutable trace row for one fatigue-aware operation. */
public final class ZhangBoFatigueOperationRecord {
  public final int sequence;
  public final int job;
  public final int stage;
  public final int factory;
  public final int machine;
  public final int worker;
  public final double predecessorCompletion;
  public final double machineAvailableBefore;
  public final double workerAvailableBefore;
  public final double start;
  public final double recoveryDuration;
  public final double fatigueBeforeRecovery;
  public final double fatigueAtStart;
  public final double baseProcessingDuration;
  public final double baseSetupDuration;
  public final double baseDuration;
  public final double durationMultiplier;
  public final double actualProcessingDuration;
  public final double actualSetupDuration;
  public final double actualDuration;
  public final double end;
  public final double fatigueAfter;
  public final double energy;
  public final double cost;
  public final boolean safeThresholdExceeded;

  public ZhangBoFatigueOperationRecord(
      int sequence, int job, int stage, int factory, int machine, int worker,
      double predecessorCompletion, double machineAvailableBefore,
      double workerAvailableBefore, double start, double recoveryDuration,
      double fatigueBeforeRecovery, double fatigueAtStart, double baseDuration,
      double durationMultiplier, double actualDuration, double end,
      double fatigueAfter, double energy, double cost, boolean safeThresholdExceeded) {
    this(sequence, job, stage, factory, machine, worker, predecessorCompletion,
        machineAvailableBefore, workerAvailableBefore, start, recoveryDuration,
        fatigueBeforeRecovery, fatigueAtStart, baseDuration, 0.0, baseDuration,
        durationMultiplier, actualDuration, 0.0, actualDuration, end,
        fatigueAfter, energy, cost, safeThresholdExceeded);
  }

  public ZhangBoFatigueOperationRecord(
      int sequence, int job, int stage, int factory, int machine, int worker,
      double predecessorCompletion, double machineAvailableBefore,
      double workerAvailableBefore, double start, double recoveryDuration,
      double fatigueBeforeRecovery, double fatigueAtStart,
      double baseProcessingDuration, double baseSetupDuration, double baseDuration,
      double durationMultiplier, double actualProcessingDuration,
      double actualSetupDuration, double actualDuration, double end,
      double fatigueAfter, double energy, double cost, boolean safeThresholdExceeded) {
    this.sequence = sequence;
    this.job = job;
    this.stage = stage;
    this.factory = factory;
    this.machine = machine;
    this.worker = worker;
    this.predecessorCompletion = predecessorCompletion;
    this.machineAvailableBefore = machineAvailableBefore;
    this.workerAvailableBefore = workerAvailableBefore;
    this.start = start;
    this.recoveryDuration = recoveryDuration;
    this.fatigueBeforeRecovery = fatigueBeforeRecovery;
    this.fatigueAtStart = fatigueAtStart;
    this.baseProcessingDuration = baseProcessingDuration;
    this.baseSetupDuration = baseSetupDuration;
    this.baseDuration = baseDuration;
    this.durationMultiplier = durationMultiplier;
    this.actualProcessingDuration = actualProcessingDuration;
    this.actualSetupDuration = actualSetupDuration;
    this.actualDuration = actualDuration;
    this.end = end;
    this.fatigueAfter = fatigueAfter;
    this.energy = energy;
    this.cost = cost;
    this.safeThresholdExceeded = safeThresholdExceeded;
  }

  public double deltaFatigue() { return fatigueAfter - fatigueAtStart; }
}
