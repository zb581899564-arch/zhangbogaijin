package org.uma.jmetal.problem.multiobjective.dfsp.fatigue.shift;

/** Immutable candidate-level shift diagnostic for fatigue-shift-v2-common-gap. */
public final class ZhangBoShiftEvent {
  public final String phase;
  public final int job;
  public final int stage;
  public final int machineSlot;
  public final int workerSlot;
  public final double commonGapLeft;
  public final double commonGapRight;
  public final double oldStart;
  public final double newStart;
  public final double oldFatigueAtStart;
  public final double newFatigueAtStart;
  public final double oldDuration;
  public final double newDuration;
  public final double oldEnd;
  public final double newEnd;
  public final double oldCmax;
  public final double newCmax;
  public final double oldTec;
  public final double newTec;
  public final double oldTwc;
  public final double newTwc;
  public final double cmaxStar;
  public final double proposalShift;
  public final double acceptedShift;
  public final int backtrackingAttempt;
  public final boolean accepted;
  public final String reason;

  public ZhangBoShiftEvent(
      String phase, int job, int stage, int machineSlot, int workerSlot,
      double commonGapLeft, double commonGapRight, double oldStart, double newStart,
      double oldFatigueAtStart, double newFatigueAtStart, double oldDuration,
      double newDuration, double oldEnd, double newEnd, double oldCmax, double newCmax,
      double oldTec, double newTec, double oldTwc, double newTwc, double cmaxStar,
      double proposalShift, double acceptedShift, int backtrackingAttempt,
      boolean accepted, String reason) {
    this.phase = phase;
    this.job = job;
    this.stage = stage;
    this.machineSlot = machineSlot;
    this.workerSlot = workerSlot;
    this.commonGapLeft = commonGapLeft;
    this.commonGapRight = commonGapRight;
    this.oldStart = oldStart;
    this.newStart = newStart;
    this.oldFatigueAtStart = oldFatigueAtStart;
    this.newFatigueAtStart = newFatigueAtStart;
    this.oldDuration = oldDuration;
    this.newDuration = newDuration;
    this.oldEnd = oldEnd;
    this.newEnd = newEnd;
    this.oldCmax = oldCmax;
    this.newCmax = newCmax;
    this.oldTec = oldTec;
    this.newTec = newTec;
    this.oldTwc = oldTwc;
    this.newTwc = newTwc;
    this.cmaxStar = cmaxStar;
    this.proposalShift = proposalShift;
    this.acceptedShift = acceptedShift;
    this.backtrackingAttempt = backtrackingAttempt;
    this.accepted = accepted;
    this.reason = reason;
  }

  public String canonicalLine() {
    return phase + ',' + job + ',' + stage + ',' + machineSlot + ',' + workerSlot
        + ',' + commonGapLeft + ',' + commonGapRight + ',' + oldStart + ',' + newStart
        + ',' + oldFatigueAtStart + ',' + newFatigueAtStart + ',' + oldDuration + ','
        + newDuration + ',' + oldEnd + ',' + newEnd + ',' + oldCmax + ',' + newCmax
        + ',' + oldTec + ',' + newTec + ',' + oldTwc + ',' + newTwc + ',' + cmaxStar
        + ',' + proposalShift + ',' + acceptedShift + ',' + backtrackingAttempt + ','
        + accepted + ',' + reason;
  }
}
