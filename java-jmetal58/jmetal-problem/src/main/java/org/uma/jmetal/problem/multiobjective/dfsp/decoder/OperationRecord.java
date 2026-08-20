package org.uma.jmetal.problem.multiobjective.dfsp.decoder;

import java.io.Serializable;
import java.util.Locale;

/** One non-preemptive dual-resource operation. */
public final class OperationRecord implements Serializable {
  private static final long serialVersionUID = 1L;

  private final int job;
  private final int stage;
  private final int factory;
  private final int machine;
  private final int worker;
  private final double startTime;
  private final double setupDuration;
  private final double processingDuration;
  private final double endTime;
  private final int dispatchOrdinal;

  public OperationRecord(
      int job, int stage, int factory, int machine, int worker, double startTime,
      double setupDuration, double processingDuration, double endTime, int dispatchOrdinal) {
    this.job = job;
    this.stage = stage;
    this.factory = factory;
    this.machine = machine;
    this.worker = worker;
    this.startTime = startTime;
    this.setupDuration = setupDuration;
    this.processingDuration = processingDuration;
    this.endTime = endTime;
    this.dispatchOrdinal = dispatchOrdinal;
  }

  public int getJob() { return job; }
  public int getStage() { return stage; }
  public int getFactory() { return factory; }
  public int getMachine() { return machine; }
  public int getWorker() { return worker; }
  public double getStartTime() { return startTime; }
  public double getSetupDuration() { return setupDuration; }
  public double getProcessingDuration() { return processingDuration; }
  public double getDuration() { return setupDuration + processingDuration; }
  public double getEndTime() { return endTime; }
  public int getDispatchOrdinal() { return dispatchOrdinal; }

  public OperationRecord shiftedTo(double newStart) {
    return new OperationRecord(job, stage, factory, machine, worker, newStart,
        setupDuration, processingDuration, newStart + getDuration(), dispatchOrdinal);
  }

  public String operationKey() {
    return job + ":" + stage;
  }

  public String toCsv() {
    return String.format(Locale.ROOT, "%d,%d,%d,%d,%d,%.12f,%.12f,%.12f,%.12f,%d",
        job, stage, factory, machine, worker, startTime, setupDuration,
        processingDuration, endTime, dispatchOrdinal);
  }
}
