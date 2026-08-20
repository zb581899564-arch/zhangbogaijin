package org.uma.jmetal.problem.multiobjective.dfsp.fatigue;

import org.uma.jmetal.solution.PermutationSolution;
import org.uma.jmetal.problem.multiobjective.dfsp.model.DhhfspFourVectorSolution;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import org.uma.jmetal.problem.multiobjective.dfsp.fatigue.shift.ZhangBoFatigueShiftRefiner;
import org.uma.jmetal.problem.multiobjective.dfsp.fatigue.shift.ZhangBoShiftConfiguration;
import org.uma.jmetal.problem.multiobjective.dfsp.fatigue.shift.ZhangBoShiftSummary;
import org.uma.jmetal.problem.multiobjective.dfsp.setup.ProductFamilySetupModel;

/** Deterministic fatigue-aware evaluator directly derived from the active author schedule. */
public final class ZhangBoFatigueEvaluator {
  private static final double TIE_EPSILON = 1.0e-12;
  private final ZhangBoDecoderNanoClock clock;

  public ZhangBoFatigueEvaluator() {
    this(ZhangBoDecoderNanoClock.SYSTEM);
  }

  public ZhangBoFatigueEvaluator(ZhangBoDecoderNanoClock clock) {
    if (clock == null) throw new IllegalArgumentException("clock must not be null");
    this.clock = clock;
  }

  public ZhangBoFatigueEvaluationResult evaluate(
      ZhangBoFatigueInstanceData instance,
      ZhangBoFatigueParameters parameters,
      PermutationSolution<Integer> solution) {
    return evaluate(instance, parameters, solution,
        ZhangBoFatigueEvaluationMode.FATIGUE_AWARE_SELECTION);
  }

  public ZhangBoFatigueEvaluationResult evaluate(
      ZhangBoFatigueInstanceData instance,
      ZhangBoFatigueParameters parameters,
      PermutationSolution<Integer> solution,
      ZhangBoFatigueEvaluationMode mode) {
    return evaluate(instance, parameters, solution, mode, ZhangBoShiftConfiguration.none());
  }

  public ZhangBoFatigueEvaluationResult evaluate(
      ZhangBoFatigueInstanceData instance,
      ZhangBoFatigueParameters parameters,
      PermutationSolution<Integer> solution,
      ZhangBoFatigueEvaluationMode mode,
      ZhangBoShiftConfiguration shiftConfiguration) {
    return evaluate(instance, parameters, solution, mode, shiftConfiguration,
        ProductFamilySetupModel.degenerate(instance.getJobs(), instance.getStages()));
  }

  public ZhangBoFatigueEvaluationResult evaluate(
      ZhangBoFatigueInstanceData instance,
      ZhangBoFatigueParameters parameters,
      PermutationSolution<Integer> solution,
      ZhangBoFatigueEvaluationMode mode,
      ZhangBoShiftConfiguration shiftConfiguration,
      ProductFamilySetupModel setupModel) {
    if (shiftConfiguration == null) {
      throw new IllegalArgumentException("shiftConfiguration must not be null");
    }
    if (setupModel == null || !setupModel.isFormalDegenerate()) {
      throw new IllegalArgumentException("v3.5 evaluator currently requires formal degenerate setup semantics");
    }
    long decoderStarted = clock.nanoTime();
    long baseStarted = clock.nanoTime();
    ZhangBoFatigueEvaluationResult base = evaluateBase(instance, parameters, solution, mode, setupModel);
    long baseDecodeNanos = elapsed(baseStarted, clock.nanoTime());
    ZhangBoFatigueEvaluationResult result = shiftConfiguration.getMode()
            == org.uma.jmetal.problem.multiobjective.dfsp.fatigue.shift.ZhangBoShiftMode.NONE
        ? base : new ZhangBoFatigueShiftRefiner(clock).refine(
            base, instance, parameters, mode, shiftConfiguration);
    long decoderTotalNanos = elapsed(decoderStarted, clock.nanoTime());
    ZhangBoShiftSummary shift = result.getShiftSummary();
    long leftNanos = shift == null ? 0L : shift.getLeftShiftNanos();
    long rightNanos = shift == null ? 0L : shift.getRightShiftNanos();
    long leftRecomputations = shift == null ? 0L : shift.getLeftFullRecomputations();
    long rightRecomputations = shift == null ? 0L : shift.getRightFullRecomputations();
    long leftAccepted = shift == null ? 0L : shift.getLeftAccepted();
    long rightAccepted = shift == null ? 0L : shift.getRightAccepted();
    return result.withDecoderTiming(new ZhangBoDecoderTimingSample(
        baseDecodeNanos, leftNanos, rightNanos, decoderTotalNanos,
        leftRecomputations, rightRecomputations, leftAccepted, rightAccepted));
  }

  private static long elapsed(long started, long finished) {
    if (finished < started) {
      throw new IllegalStateException("Decoder NanoClock must be monotonic");
    }
    return finished - started;
  }

  private ZhangBoFatigueEvaluationResult evaluateBase(
      ZhangBoFatigueInstanceData instance,
      ZhangBoFatigueParameters parameters,
      PermutationSolution<Integer> solution,
      ZhangBoFatigueEvaluationMode mode,
      ProductFamilySetupModel setupModel) {
    if (mode == null || mode == ZhangBoFatigueEvaluationMode.AUTHOR_ACTUAL) {
      throw new IllegalArgumentException("The fatigue evaluator requires an explicit corrected mode");
    }
    validate(instance, parameters, solution);
    int jobs = instance.getJobs();
    int stages = instance.getStages();
    int factories = instance.getFactories();

    int[] positionOfJob = new int[jobs];
    Arrays.fill(positionOfJob, -1);
    List<List<Integer>> factoryJobs = new ArrayList<>(factories);
    for (int f = 0; f < factories; f++) factoryJobs.add(new ArrayList<Integer>());
    for (int position = 0; position < jobs; position++) {
      int job = solution.getVariableValue(position);
      int factory = solution.getVariableValueid(position);
      positionOfJob[job] = position;
      factoryJobs.get(factory).add(job);
    }

    double[][] completion = new double[jobs][stages];
    double[][][] machineAvailable = new double[factories][stages][];
    boolean[][][] machineUsed = new boolean[factories][stages][];
    for (int f = 0; f < factories; f++) {
      for (int k = 0; k < stages; k++) {
        machineAvailable[f][k] = new double[instance.getMachineCount(f, k)];
        machineUsed[f][k] = new boolean[instance.getMachineCount(f, k)];
      }
    }
    double[][] workerAvailable = new double[factories][];
    double[][] workerFatigue = new double[factories][];
    boolean[][] workerUsed = new boolean[factories][];
    int[][] workerLastStage = new int[factories][];
    double[][] currentContinuousWork = new double[factories][];
    double[][] longestContinuousWork = new double[factories][];
    for (int f = 0; f < factories; f++) {
      int count = instance.getWorkerCount(f);
      workerAvailable[f] = new double[count];
      workerFatigue[f] = new double[count];
      workerUsed[f] = new boolean[count];
      workerLastStage[f] = new int[count];
      Arrays.fill(workerLastStage[f], -1);
      currentContinuousWork[f] = new double[count];
      longestContinuousWork[f] = new double[count];
    }

    double[][][] completionMatrix = new double[factories][stages][jobs];
    double[][][] energyMatrix = new double[factories][stages][jobs];
    double[][][] costMatrix = new double[factories][stages][jobs];
    double[] factoryEnergy = new double[factories];
    double[] factoryCost = new double[factories];
    List<ZhangBoFatigueOperationRecord> records = new ArrayList<>();
    double fatigueSum = 0.0;
    double fatigueMaximum = 0.0;
    double excessIntegral = 0.0;
    double highFatigueTime = 0.0;
    double naturalRecovery = 0.0;
    int safeEvents = 0;
    int sequence = 0;

    for (int factory = 0; factory < factories; factory++) {
      List<Integer> order = new ArrayList<>(factoryJobs.get(factory));
      for (int stage = 0; stage < stages; stage++) {
        if (stage > 0) {
          final int previousStage = stage - 1;
          final List<Integer> previousOrder = new ArrayList<>(order);
          final int[] previousRank = new int[jobs];
          for (int rank = 0; rank < previousOrder.size(); rank++) {
            previousRank[previousOrder.get(rank)] = rank;
          }
          Collections.sort(order, new Comparator<Integer>() {
            @Override
            public int compare(Integer left, Integer right) {
              int byCompletion = Double.compare(completion[left][previousStage], completion[right][previousStage]);
              if (byCompletion != 0) return byCompletion;
              int byFifo = Integer.compare(previousRank[left], previousRank[right]);
              return byFifo != 0 ? byFifo : Integer.compare(left, right);
            }
          });
        }

        for (int ordinal = 0; ordinal < order.size(); ordinal++) {
          int job = order.get(ordinal);
          int machine = stage == 0
              ? firstStageMachine(solution, positionOfJob[job])
              : selectAuthorMachine(machineAvailable[factory][stage], ordinal);
          double predecessor = stage == 0 ? 0.0 : completion[job][stage - 1];
          int worker;
          Candidate candidate;
          if (stage == 0) {
            worker = solution.getVariableValueworker(positionOfJob[job]);
            candidate = candidate(instance, parameters, factory, stage, job, machine, worker,
                predecessor, machineAvailable[factory][stage][machine], workerAvailable,
                workerFatigue, workerUsed, workerLastStage, mode, setupModel, -1);
          } else {
            candidate = null;
            worker = -1;
            for (int possibleWorker : instance.getEligibleWorkers(factory, stage)) {
              Candidate possible = candidate(instance, parameters, factory, stage, job, machine,
                  possibleWorker, predecessor, machineAvailable[factory][stage][machine], workerAvailable,
                  workerFatigue, workerUsed, workerLastStage, mode, setupModel,
                  ordinal == 0 ? -1 : order.get(ordinal - 1));
              double possibleCriterion = mode.usesFatigueAwareWorkerSelection()
                  ? possible.end : possible.baseEnd;
              double currentCriterion = candidate == null ? Double.POSITIVE_INFINITY
                  : (mode.usesFatigueAwareWorkerSelection() ? candidate.end : candidate.baseEnd);
              if (candidate == null || possibleCriterion < currentCriterion - TIE_EPSILON
                  || (Math.abs(possibleCriterion - currentCriterion) <= TIE_EPSILON
                      && possibleWorker < worker)) {
                candidate = possible;
                worker = possibleWorker;
              }
            }
          }

          double machineBefore = machineAvailable[factory][stage][machine];
          double workerBefore = workerAvailable[factory][worker];
          double fatigueBefore = workerFatigue[factory][worker];
          if (mode.recoversNaturally()
              && workerUsed[factory][worker] && candidate.start > workerBefore) {
            int recoveryStage = workerLastStage[factory][worker];
            double mu = parameters.getMu(factory, worker, recoveryStage);
            double gap = candidate.start - workerBefore;
            excessIntegral += ZhangBoFatigueModel.excessIntegralDuringRecovery(
                fatigueBefore, mu, gap, parameters.getWarningThreshold());
            highFatigueTime += ZhangBoFatigueModel.timeAboveDuringRecovery(
                fatigueBefore, mu, gap, parameters.getWarningThreshold());
            naturalRecovery += gap;
          }
          if (mode.accumulatesFatigue()) {
            excessIntegral += ZhangBoFatigueModel.excessIntegralDuringWork(
                candidate.fatigueAtStart, parameters.getLambda(factory, worker, stage),
                candidate.actualDuration, parameters.getWarningThreshold());
            highFatigueTime += ZhangBoFatigueModel.timeAboveDuringWork(
                candidate.fatigueAtStart, parameters.getLambda(factory, worker, stage),
                candidate.actualDuration, parameters.getWarningThreshold());
          }

          double idle = machineUsed[factory][stage][machine]
              ? Math.max(0.0, candidate.start - machineBefore) : 0.0;
          double energy = candidate.actualDuration * instance.getMachinePower(factory, stage, machine) + idle;
          double cost = (candidate.actualDuration + idle) * instance.getWorkerCost(factory, worker);
          boolean safeExceeded = mode.accumulatesFatigue()
              && candidate.fatigueAfter > parameters.getSafeThreshold();
          if (safeExceeded) safeEvents++;

          if (workerUsed[factory][worker] && Math.abs(candidate.start - workerBefore) <= TIE_EPSILON) {
            currentContinuousWork[factory][worker] += candidate.actualDuration;
          } else {
            currentContinuousWork[factory][worker] = candidate.actualDuration;
          }
          longestContinuousWork[factory][worker] = Math.max(
              longestContinuousWork[factory][worker], currentContinuousWork[factory][worker]);

          machineAvailable[factory][stage][machine] = candidate.end;
          machineUsed[factory][stage][machine] = true;
          workerAvailable[factory][worker] = candidate.end;
          workerFatigue[factory][worker] = candidate.fatigueAfter;
          workerUsed[factory][worker] = true;
          workerLastStage[factory][worker] = stage;
          completion[job][stage] = candidate.end;
          completionMatrix[factory][stage][job] = candidate.end;
          energyMatrix[factory][stage][job] = energy;
          costMatrix[factory][stage][job] = cost;
          factoryEnergy[factory] += energy;
          factoryCost[factory] += cost;
          fatigueSum += candidate.fatigueAfter;
          fatigueMaximum = Math.max(fatigueMaximum, candidate.fatigueAfter);
          records.add(new ZhangBoFatigueOperationRecord(
              sequence++, job, stage, factory, machine, worker, predecessor, machineBefore,
              workerBefore, candidate.start, candidate.recoveryDuration, fatigueBefore,
              candidate.fatigueAtStart, candidate.baseProcessingDuration,
              candidate.baseSetupDuration, candidate.baseDuration, candidate.multiplier,
              candidate.actualProcessingDuration, candidate.actualSetupDuration,
              candidate.actualDuration, candidate.end, candidate.fatigueAfter,
              energy, cost, safeExceeded));
        }
      }
    }

    double[] factoryMakespan = new double[factories];
    double makespan = 0.0;
    for (int f = 0; f < factories; f++) {
      for (int job : factoryJobs.get(f)) {
        factoryMakespan[f] = Math.max(factoryMakespan[f], completion[job][stages - 1]);
      }
      makespan = Math.max(makespan, factoryMakespan[f]);
    }

    double finalFatigueSum = 0.0;
    double finalFatigueSquareSum = 0.0;
    int totalWorkers = 0;
    double longest = 0.0;
    for (int f = 0; f < factories; f++) {
      totalWorkers += instance.getWorkerCount(f);
      for (int worker = 0; worker < instance.getWorkerCount(f); worker++) {
        longest = Math.max(longest, longestContinuousWork[f][worker]);
        double finalFatigue = workerFatigue[f][worker];
        if (mode.recoversNaturally()
            && workerUsed[f][worker] && makespan > workerAvailable[f][worker]) {
          double tail = makespan - workerAvailable[f][worker];
          double mu = parameters.getMu(f, worker, workerLastStage[f][worker]);
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
    double ratioDenominator = totalWorkers * makespan;

    int maxMakespanFactory = indexOfMaximum(factoryMakespan);
    int minMakespanFactory = indexOfMinimum(factoryMakespan);
    int minEnergyFactory = indexOfMinimum(factoryEnergy);
    int maxEnergyFactory = indexOfMaximum(factoryEnergy);
    double totalEnergy = sum(factoryEnergy);
    double totalCost = sum(factoryCost);
    double[] objectives = new double[] {
        makespan, totalEnergy, maxMakespanFactory, minMakespanFactory,
        minEnergyFactory, maxEnergyFactory, totalCost
    };
    ZhangBoFatigueMetrics metrics = new ZhangBoFatigueMetrics(
        fatigueMaximum, records.isEmpty() ? 0.0 : fatigueSum / records.size(),
        excessIntegral, variance,
        ratioDenominator == 0.0 ? 0.0 : highFatigueTime / ratioDenominator,
        longest, naturalRecovery, safeEvents);
    return new ZhangBoFatigueEvaluationResult(instance.getInstanceSha256(),
        parameters.getConfigurationSha256(), instance.getInstanceExtensionSha256(),
        records, metrics, objectives,
        completionMatrix, energyMatrix, costMatrix);
  }

  private static Candidate candidate(
      ZhangBoFatigueInstanceData instance, ZhangBoFatigueParameters parameters,
      int factory, int stage, int job, int machine, int worker, double predecessor,
      double machineAvailable, double[][] workerAvailable, double[][] workerFatigue,
      boolean[][] workerUsed, int[][] workerLastStage,
      ZhangBoFatigueEvaluationMode mode, ProductFamilySetupModel setupModel,
      int previousJob) {
    double start = Math.max(predecessor, Math.max(machineAvailable, workerAvailable[factory][worker]));
    double fatigueBefore = workerFatigue[factory][worker];
    double recoveryDuration = workerUsed[factory][worker]
        ? Math.max(0.0, start - workerAvailable[factory][worker]) : 0.0;
    double fatigueAtStart = fatigueBefore;
    if (mode.recoversNaturally() && recoveryDuration > 0.0) {
      fatigueAtStart = ZhangBoFatigueModel.recover(fatigueBefore,
          parameters.getMu(factory, worker, workerLastStage[factory][worker]), recoveryDuration);
    }
    double workerEfficiency = instance.getWorkerEfficiency(factory, worker);
    double baseProcessingDuration = instance.getStandardTime(job, stage)
        / (instance.getMachineSpeed(factory, stage, machine) * workerEfficiency);
    double setupTime = setupModel.setupTime(
        instance.getStandardSetupTime(job, stage), stage, previousJob, job);
    double baseSetupDuration = setupTime / workerEfficiency;
    double baseDuration = baseProcessingDuration + baseSetupDuration;
    if (!mode.accumulatesFatigue()) fatigueAtStart = 0.0;
    double multiplier = mode.accumulatesFatigue()
        ? ZhangBoFatigueModel.durationMultiplier(
            fatigueAtStart, parameters.getMaximumIncrease(stage)) : 1.0;
    double actualProcessingDuration = baseProcessingDuration * multiplier;
    double actualSetupDuration = baseSetupDuration * multiplier;
    double actualDuration = actualProcessingDuration + actualSetupDuration;
    double fatigueAfter = mode.accumulatesFatigue()
        ? ZhangBoFatigueModel.accumulate(fatigueAtStart,
            parameters.getLambda(factory, worker, stage), actualDuration) : 0.0;
    return new Candidate(start, recoveryDuration, fatigueAtStart,
        baseProcessingDuration, baseSetupDuration, baseDuration, multiplier,
        actualProcessingDuration, actualSetupDuration, actualDuration,
        start + actualDuration, start + baseDuration, fatigueAfter);
  }

  @SuppressWarnings("unchecked")
  private static int firstStageMachine(PermutationSolution<Integer> solution, int position) {
    if (solution instanceof DhhfspFourVectorSolution) {
      return ((DhhfspFourVectorSolution) solution).getMachineAssignment(position);
    }
    Object attribute = solution.getAttribute("machine");
    if (!(attribute instanceof List<?>)) {
      throw new IllegalArgumentException("Missing first-stage MA vector in solution attribute 'machine'");
    }
    List<Integer> machines = (List<Integer>) attribute;
    if (position >= machines.size() || machines.get(position) == null) {
      throw new IllegalArgumentException("Invalid first-stage MA at position=" + position);
    }
    return machines.get(position);
  }

  private static int selectAuthorMachine(double[] availability, int ordinal) {
    if (ordinal == 0) return 0;
    int selected = 0;
    for (int machine = 1; machine < availability.length; machine++) {
      if (availability[machine] < availability[selected] - TIE_EPSILON) selected = machine;
    }
    return selected;
  }

  private static void validate(
      ZhangBoFatigueInstanceData instance, ZhangBoFatigueParameters parameters,
      PermutationSolution<Integer> solution) {
    if (!instance.getInstanceSha256().equals(parameters.getInstanceSha256())) {
      throw new IllegalArgumentException("Fatigue parameter instance hash mismatch");
    }
    if (parameters.getFactories() != instance.getFactories()
        || parameters.getStages() != instance.getStages()) {
      throw new IllegalArgumentException("Fatigue parameter dimensions do not match instance");
    }
    if (!instance.hasStandardSetupTimes()
        || instance.getInstanceExtensionSha256().isEmpty()) {
      throw new IllegalArgumentException("Nonzero fatigue evaluation requires a fixed SUT extension");
    }
    int jobs = instance.getJobs();
    if (solution.getVariables().size() != jobs || solution.getVariablesid().size() != jobs
        || solution.getVariablesworker().size() < jobs) {
      throw new IllegalArgumentException("JS/FA/first-stage WA dimensions are invalid");
    }
    boolean[] seen = new boolean[jobs];
    for (int position = 0; position < jobs; position++) {
      Integer jobValue = solution.getVariableValue(position);
      Integer factoryValue = solution.getVariableValueid(position);
      Integer workerValue = solution.getVariableValueworker(position);
      if (jobValue == null || jobValue < 0 || jobValue >= jobs || seen[jobValue]) {
        throw new IllegalArgumentException("Invalid JS at position=" + position + ": " + jobValue);
      }
      seen[jobValue] = true;
      if (factoryValue == null || factoryValue < 0 || factoryValue >= instance.getFactories()) {
        throw new IllegalArgumentException("Invalid FA at position=" + position + ": " + factoryValue);
      }
      if (workerValue == null || !instance.isWorkerEligible(factoryValue, 0, workerValue)) {
        throw new IllegalArgumentException("Invalid first-stage WA at position=" + position
            + ", factory=" + factoryValue + ": " + workerValue);
      }
      int machineValue = firstStageMachine(solution, position);
      if (machineValue < 0 || machineValue >= instance.getMachineCount(factoryValue, 0)) {
        throw new IllegalArgumentException("Invalid first-stage MA at position=" + position
            + ", factory=" + factoryValue + ": " + machineValue);
      }
    }
  }

  private static int indexOfMaximum(double[] values) {
    int result = 0;
    for (int i = 1; i < values.length; i++) if (values[i] > values[result]) result = i;
    return result;
  }

  private static int indexOfMinimum(double[] values) {
    int result = 0;
    for (int i = 1; i < values.length; i++) if (values[i] < values[result]) result = i;
    return result;
  }

  private static double sum(double[] values) {
    double result = 0.0;
    for (double value : values) result += value;
    return result;
  }

  private static final class Candidate {
    private final double start;
    private final double recoveryDuration;
    private final double fatigueAtStart;
    private final double baseProcessingDuration;
    private final double baseSetupDuration;
    private final double baseDuration;
    private final double multiplier;
    private final double actualProcessingDuration;
    private final double actualSetupDuration;
    private final double actualDuration;
    private final double end;
    private final double baseEnd;
    private final double fatigueAfter;

    private Candidate(double start, double recoveryDuration, double fatigueAtStart,
        double baseProcessingDuration, double baseSetupDuration, double baseDuration,
        double multiplier, double actualProcessingDuration, double actualSetupDuration,
        double actualDuration, double end, double baseEnd, double fatigueAfter) {
      this.start = start;
      this.recoveryDuration = recoveryDuration;
      this.fatigueAtStart = fatigueAtStart;
      this.baseProcessingDuration = baseProcessingDuration;
      this.baseSetupDuration = baseSetupDuration;
      this.baseDuration = baseDuration;
      this.multiplier = multiplier;
      this.actualProcessingDuration = actualProcessingDuration;
      this.actualSetupDuration = actualSetupDuration;
      this.actualDuration = actualDuration;
      this.end = end;
      this.baseEnd = baseEnd;
      this.fatigueAfter = fatigueAfter;
    }
  }
}
