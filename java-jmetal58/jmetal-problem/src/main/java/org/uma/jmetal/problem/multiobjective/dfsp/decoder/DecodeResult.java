package org.uma.jmetal.problem.multiobjective.dfsp.decoder;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Complete three-phase result of a decode. */
public final class DecodeResult implements Serializable {
  private static final long serialVersionUID = 1L;
  private final DecodeMode mode;
  private final long seed;
  private final String standbyEnergyProvenance;
  private final ScheduleSnapshot initial;
  private final ScheduleSnapshot fineTuned;
  private final ScheduleSnapshot rightShifted;
  private final List<DecisionEvent> events;

  public DecodeResult(
      DecodeMode mode, long seed, String standbyEnergyProvenance,
      ScheduleSnapshot initial, ScheduleSnapshot fineTuned,
      ScheduleSnapshot rightShifted, List<DecisionEvent> events) {
    this.mode = mode;
    this.seed = seed;
    this.standbyEnergyProvenance = standbyEnergyProvenance;
    this.initial = initial;
    this.fineTuned = fineTuned;
    this.rightShifted = rightShifted;
    this.events = Collections.unmodifiableList(new ArrayList<>(events));
  }

  public DecodeMode getMode() { return mode; }
  public long getSeed() { return seed; }
  public String getStandbyEnergyProvenance() { return standbyEnergyProvenance; }
  public ScheduleSnapshot getInitial() { return initial; }
  public ScheduleSnapshot getFineTuned() { return fineTuned; }
  public ScheduleSnapshot getRightShifted() { return rightShifted; }
  public ScheduleSnapshot getFinalSnapshot() { return rightShifted; }
  public List<DecisionEvent> getEvents() { return events; }

  public String toCanonicalText() {
    StringBuilder builder = new StringBuilder();
    builder.append("mode=").append(mode).append('\n');
    builder.append("seed=").append(seed).append('\n');
    builder.append("standbyEnergy=").append(standbyEnergyProvenance).append('\n');
    appendSnapshot(builder, initial);
    appendSnapshot(builder, fineTuned);
    appendSnapshot(builder, rightShifted);
    for (DecisionEvent event : events) {
      builder.append("event=").append(event.toCanonicalText()).append('\n');
    }
    return builder.toString();
  }

  private static void appendSnapshot(StringBuilder builder, ScheduleSnapshot snapshot) {
    ObjectiveBreakdown objective = snapshot.getObjectives();
    builder.append("phase=").append(snapshot.getPhase())
        .append(",accepted=").append(snapshot.isAccepted())
        .append(",cmax=").append(Double.toString(objective.getMakespan()))
        .append(",tec=").append(Double.toString(objective.getTotalEnergy()))
        .append(",twc=").append(Double.toString(objective.getTotalWorkerCost()))
        .append('\n').append(snapshot.operationsCsv());
  }
}
