package org.uma.jmetal.problem.multiobjective.dfsp.fatigue.shift;

import java.util.ArrayList;
import java.util.List;
import org.uma.jmetal.problem.multiobjective.dfsp.fatigue.ZhangBoFatigueEvaluationMode;
import org.uma.jmetal.problem.multiobjective.dfsp.fatigue.ZhangBoFatigueEvaluationResult;
import org.uma.jmetal.problem.multiobjective.dfsp.fatigue.ZhangBoFatigueInstanceData;
import org.uma.jmetal.problem.multiobjective.dfsp.fatigue.ZhangBoFatigueMetrics;
import org.uma.jmetal.problem.multiobjective.dfsp.fatigue.ZhangBoFatigueModel;
import org.uma.jmetal.problem.multiobjective.dfsp.fatigue.ZhangBoFatigueOperationRecord;
import org.uma.jmetal.problem.multiobjective.dfsp.fatigue.ZhangBoFatigueParameters;

/** Recomputes the complete recovery-fatigue-duration state over a fixed schedule DAG. */
public final class ZhangBoFatigueSchedulePropagator {
  private static final double EPS = 1.0e-12;

  public ZhangBoFatigueEvaluationResult propagate(
      ZhangBoScheduleGraph graph, ZhangBoFatigueInstanceData instance,
      ZhangBoFatigueParameters parameters, ZhangBoFatigueEvaluationMode mode) {
    int operations = graph.operationCount();
    ZhangBoFatigueOperationRecord[] byOperation = new ZhangBoFatigueOperationRecord[operations];
    double[] continuous = new double[operations];
    double[][][] completionMatrix = new double[instance.getFactories()][instance.getStages()][instance.getJobs()];
    double[][][] energyMatrix = new double[instance.getFactories()][instance.getStages()][instance.getJobs()];
    double[][][] costMatrix = new double[instance.getFactories()][instance.getStages()][instance.getJobs()];
    double[] factoryEnergy = new double[instance.getFactories()];
    double[] factoryCost = new double[instance.getFactories()];
    double[] factoryMakespan = new double[instance.getFactories()];
    double fatigueSum = 0.0;
    double fatigueMaximum = 0.0;
    double excessIntegral = 0.0;
    double highFatigueTime = 0.0;
    double naturalRecovery = 0.0;
    double longestContinuous = 0.0;
    int safeEvents = 0;
    int sequence = 0;

    for (int operation : graph.topologicalOrder()) {
      int job = graph.getJob(operation);
      int stage = graph.getStage(operation);
      int factory = graph.getFactory(operation);
      int machine = graph.getMachine(operation);
      int worker = graph.getWorker(operation);
      validateAssignment(instance, job, stage, factory, machine, worker);
      int jobPrevious = stage == 0 ? -1 : job * graph.getStages() + stage - 1;
      int machinePrevious = graph.predecessorInMachine(operation);
      int workerPrevious = graph.predecessorInWorker(operation);
      double jobEnd = end(byOperation, jobPrevious);
      double machineEnd = end(byOperation, machinePrevious);
      double workerEnd = end(byOperation, workerPrevious);
      OperationTransition transition = previewOperation(graph, operation, instance, parameters,
          mode, jobEnd, machineEnd, workerPrevious < 0 ? null : byOperation[workerPrevious],
          graph.getReleaseOverride(operation));
      double start = transition.start;
      double fatigueBefore = transition.fatigueBefore;
      double recoveryDuration = transition.recoveryDuration;
      double fatigueAtStart = transition.fatigueAtStart;
      if (mode.recoversNaturally() && recoveryDuration > 0.0) {
        excessIntegral += ZhangBoFatigueModel.excessIntegralDuringRecovery(
            fatigueBefore, parameters.getMu(factory, worker, graph.getStage(workerPrevious)),
            recoveryDuration, parameters.getWarningThreshold());
        highFatigueTime += ZhangBoFatigueModel.timeAboveDuringRecovery(
            fatigueBefore, parameters.getMu(factory, worker, graph.getStage(workerPrevious)),
            recoveryDuration, parameters.getWarningThreshold());
        naturalRecovery += recoveryDuration;
      }
      double baseProcessing = transition.baseProcessing;
      double baseSetup = transition.baseSetup;
      double baseDuration = transition.baseDuration;
      double multiplier = transition.multiplier;
      double actualProcessing = transition.actualProcessing;
      double actualSetup = transition.actualSetup;
      double actualDuration = transition.actualDuration;
      double finish = transition.finish;
      double fatigueAfter = transition.fatigueAfter;
      if (mode.accumulatesFatigue()) {
        excessIntegral += ZhangBoFatigueModel.excessIntegralDuringWork(
            fatigueAtStart, parameters.getLambda(factory, worker, stage),
            actualDuration, parameters.getWarningThreshold());
        highFatigueTime += ZhangBoFatigueModel.timeAboveDuringWork(
            fatigueAtStart, parameters.getLambda(factory, worker, stage),
            actualDuration, parameters.getWarningThreshold());
      }

      double idle = machinePrevious < 0 ? 0.0 : Math.max(0.0, start - machineEnd);
      double energy = actualDuration * instance.getMachinePower(factory, stage, machine) + idle;
      double cost = (actualDuration + idle) * instance.getWorkerCost(factory, worker);
      boolean safe = mode.accumulatesFatigue()
          && fatigueAfter > parameters.getSafeThreshold();
      if (safe) safeEvents++;
      continuous[operation] = workerPrevious >= 0 && Math.abs(start - workerEnd) <= EPS
          ? continuous[workerPrevious] + actualDuration : actualDuration;
      longestContinuous = Math.max(longestContinuous, continuous[operation]);

      ZhangBoFatigueOperationRecord record = new ZhangBoFatigueOperationRecord(
          sequence++, job, stage, factory, machine, worker, jobEnd, machineEnd,
          workerEnd, start, recoveryDuration, fatigueBefore, fatigueAtStart,
          baseProcessing, baseSetup, baseDuration, multiplier, actualProcessing,
          actualSetup, actualDuration, finish, fatigueAfter, energy, cost, safe);
      byOperation[operation] = record;
      completionMatrix[factory][stage][job] = finish;
      energyMatrix[factory][stage][job] = energy;
      costMatrix[factory][stage][job] = cost;
      factoryEnergy[factory] += energy;
      factoryCost[factory] += cost;
      if (stage == instance.getStages() - 1) {
        factoryMakespan[factory] = Math.max(factoryMakespan[factory], finish);
      }
      fatigueSum += fatigueAfter;
      fatigueMaximum = Math.max(fatigueMaximum, fatigueAfter);
    }

    double makespan = maximum(factoryMakespan);
    double finalFatigueSum = 0.0;
    double finalFatigueSquareSum = 0.0;
    int totalWorkers = 0;
    for (int factory = 0; factory < instance.getFactories(); factory++) {
      totalWorkers += instance.getWorkerCount(factory);
      for (int worker = 0; worker < instance.getWorkerCount(factory); worker++) {
        int last = lastWorkerOperation(graph, factory, worker);
        double finalFatigue = last < 0 ? 0.0 : byOperation[last].fatigueAfter;
        if (last >= 0 && mode.recoversNaturally() && makespan > byOperation[last].end) {
          double tail = makespan - byOperation[last].end;
          double mu = parameters.getMu(factory, worker, graph.getStage(last));
          excessIntegral += ZhangBoFatigueModel.excessIntegralDuringRecovery(
              finalFatigue, mu, tail, parameters.getWarningThreshold());
          highFatigueTime += ZhangBoFatigueModel.timeAboveDuringRecovery(
              finalFatigue, mu, tail, parameters.getWarningThreshold());
          finalFatigue = ZhangBoFatigueModel.recover(finalFatigue, mu, tail);
        }
        finalFatigueSum += finalFatigue;
        finalFatigueSquareSum += finalFatigue * finalFatigue;
      }
    }
    double finalMean = totalWorkers == 0 ? 0.0 : finalFatigueSum / totalWorkers;
    double variance = totalWorkers == 0 ? 0.0
        : Math.max(0.0, finalFatigueSquareSum / totalWorkers - finalMean * finalMean);
    double totalEnergy = sum(factoryEnergy);
    double totalCost = sum(factoryCost);
    double[] objectives = new double[] {
        makespan, totalEnergy, indexMaximum(factoryMakespan), indexMinimum(factoryMakespan),
        indexMinimum(factoryEnergy), indexMaximum(factoryEnergy), totalCost
    };
    ZhangBoFatigueMetrics metrics = new ZhangBoFatigueMetrics(
        fatigueMaximum, operations == 0 ? 0.0 : fatigueSum / operations,
        excessIntegral, variance, totalWorkers * makespan == 0.0 ? 0.0
            : highFatigueTime / (totalWorkers * makespan),
        longestContinuous, naturalRecovery, safeEvents);
    List<ZhangBoFatigueOperationRecord> records = new ArrayList<>();
    for (ZhangBoFatigueOperationRecord record : byOperation) records.add(record);
    records.sort(new java.util.Comparator<ZhangBoFatigueOperationRecord>() {
      @Override public int compare(ZhangBoFatigueOperationRecord left,
          ZhangBoFatigueOperationRecord right) {
        return Integer.compare(left.sequence, right.sequence);
      }
    });
    return new ZhangBoFatigueEvaluationResult(instance.getInstanceSha256(),
        parameters.getConfigurationSha256(), instance.getInstanceExtensionSha256(),
        records, metrics, objectives, completionMatrix, energyMatrix, costMatrix);
  }

  /**
   * Computes one operation with the same recovery/fatigue/duration equations used by the
   * full propagator. Package-private so FCLS gap preview cannot drift from committed replay.
   */
  OperationTransition previewOperation(
      ZhangBoScheduleGraph graph, int operation, ZhangBoFatigueInstanceData instance,
      ZhangBoFatigueParameters parameters, ZhangBoFatigueEvaluationMode mode,
      double jobEnd, double machineEnd, ZhangBoFatigueOperationRecord workerPrevious,
      double releaseOverride) {
    int job = graph.getJob(operation);
    int stage = graph.getStage(operation);
    int factory = graph.getFactory(operation);
    int machine = graph.getMachine(operation);
    int worker = graph.getWorker(operation);
    validateAssignment(instance, job, stage, factory, machine, worker);
    double workerEnd = workerPrevious == null ? 0.0 : workerPrevious.end;
    double start = Math.max(releaseOverride, Math.max(jobEnd, Math.max(machineEnd, workerEnd)));
    double fatigueBefore = workerPrevious == null ? 0.0 : workerPrevious.fatigueAfter;
    double recoveryDuration = workerPrevious == null ? 0.0 : Math.max(0.0, start - workerEnd);
    double fatigueAtStart = fatigueBefore;
    if (mode.recoversNaturally() && recoveryDuration > 0.0) {
      fatigueAtStart = ZhangBoFatigueModel.recover(fatigueBefore,
          parameters.getMu(factory, worker, workerPrevious.stage), recoveryDuration);
    }
    if (!mode.accumulatesFatigue()) fatigueAtStart = 0.0;
    double efficiency = instance.getWorkerEfficiency(factory, worker);
    double baseProcessing = instance.getStandardTime(job, stage)
        / (instance.getMachineSpeed(factory, stage, machine) * efficiency);
    double baseSetup = instance.getStandardSetupTime(job, stage) / efficiency;
    double baseDuration = baseProcessing + baseSetup;
    double multiplier = mode.accumulatesFatigue()
        ? ZhangBoFatigueModel.durationMultiplier(
            fatigueAtStart, parameters.getMaximumIncrease(stage)) : 1.0;
    double actualProcessing = baseProcessing * multiplier;
    double actualSetup = baseSetup * multiplier;
    double actualDuration = actualProcessing + actualSetup;
    double finish = start + actualDuration;
    double fatigueAfter = mode.accumulatesFatigue()
        ? ZhangBoFatigueModel.accumulate(fatigueAtStart,
            parameters.getLambda(factory, worker, stage), actualDuration) : 0.0;
    return new OperationTransition(start, recoveryDuration, fatigueBefore, fatigueAtStart,
        baseProcessing, baseSetup, baseDuration, multiplier, actualProcessing, actualSetup,
        actualDuration, finish, fatigueAfter);
  }

  static final class OperationTransition {
    final double start;
    final double recoveryDuration;
    final double fatigueBefore;
    final double fatigueAtStart;
    final double baseProcessing;
    final double baseSetup;
    final double baseDuration;
    final double multiplier;
    final double actualProcessing;
    final double actualSetup;
    final double actualDuration;
    final double finish;
    final double fatigueAfter;

    OperationTransition(
        double start, double recoveryDuration, double fatigueBefore, double fatigueAtStart,
        double baseProcessing, double baseSetup, double baseDuration, double multiplier,
        double actualProcessing, double actualSetup, double actualDuration, double finish,
        double fatigueAfter) {
      this.start = start;
      this.recoveryDuration = recoveryDuration;
      this.fatigueBefore = fatigueBefore;
      this.fatigueAtStart = fatigueAtStart;
      this.baseProcessing = baseProcessing;
      this.baseSetup = baseSetup;
      this.baseDuration = baseDuration;
      this.multiplier = multiplier;
      this.actualProcessing = actualProcessing;
      this.actualSetup = actualSetup;
      this.actualDuration = actualDuration;
      this.finish = finish;
      this.fatigueAfter = fatigueAfter;
    }
  }

  private static double end(ZhangBoFatigueOperationRecord[] records, int operation) {
    return operation < 0 ? 0.0 : records[operation].end;
  }
  private static void validateAssignment(
      ZhangBoFatigueInstanceData instance, int job, int stage, int factory,
      int machine, int worker) {
    if (job < 0 || job >= instance.getJobs() || stage < 0 || stage >= instance.getStages()
        || factory < 0 || factory >= instance.getFactories()
        || machine < 0 || machine >= instance.getMachineCount(factory, stage)
        || worker < 0 || worker >= instance.getWorkerCount(factory)) {
      throw new IllegalArgumentException("Invalid shift assignment job=" + job + ", stage="
          + stage + ", factory=" + factory + ", machine=" + machine + ", worker=" + worker);
    }
    boolean eligible = false;
    for (int candidate : instance.getEligibleWorkers(factory, stage)) {
      if (candidate == worker) { eligible = true; break; }
    }
    if (!eligible) {
      throw new IllegalArgumentException("Shift assignment uses an ineligible worker=" + worker
          + " for factory=" + factory + ", stage=" + stage);
    }
  }
  private static int lastWorkerOperation(ZhangBoScheduleGraph graph, int factory, int worker) {
    for (int operation = 0; operation < graph.operationCount(); operation++) {
      if (graph.getFactory(operation) != factory || graph.getWorker(operation) != worker) continue;
      boolean hasSuccessor = false;
      for (int candidate = 0; candidate < graph.operationCount(); candidate++) {
        if (graph.predecessorInWorker(candidate) == operation) {
          hasSuccessor = true;
          break;
        }
      }
      if (!hasSuccessor) return operation;
    }
    return -1;
  }
  private static double maximum(double[] values) {
    double result = 0.0;
    for (double value : values) result = Math.max(result, value);
    return result;
  }
  private static double sum(double[] values) {
    double result = 0.0;
    for (double value : values) result += value;
    return result;
  }
  private static int indexMaximum(double[] values) {
    int result = 0;
    for (int i = 1; i < values.length; i++) if (values[i] > values[result]) result = i;
    return result;
  }
  private static int indexMinimum(double[] values) {
    int result = 0;
    for (int i = 1; i < values.length; i++) if (values[i] < values[result]) result = i;
    return result;
  }
}
