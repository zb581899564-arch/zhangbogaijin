package org.uma.jmetal.problem.multiobjective.dfsp.decoder;

import org.uma.jmetal.problem.multiobjective.dfsp.model.DhhfspEncodingValidator;
import org.uma.jmetal.problem.multiobjective.dfsp.model.DhhfspFourVectorSolution;
import org.uma.jmetal.problem.multiobjective.dfsp.model.DhhfspInstance;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

/** Paper-aligned Chapter 4 decoder with an explicit deterministic compatibility mode. */
public final class OriginalDhhfspDecoder implements DhhfspDecoder {
  private static final double EPSILON = 1.0e-9;

  @Override
  public DecodeResult decode(
      DhhfspInstance instance, DhhfspFourVectorSolution solution, DecodeOptions options) {
    if (instance == null || solution == null || options == null) {
      throw new IllegalArgumentException("instance, solution and options must not be null");
    }
    if (options.getMode() == DecodeMode.AUTHOR_ACTUAL) {
      throw new UnsupportedOperationException(
          "AUTHOR_ACTUAL is diagnostic-only; use AuthorActualDiagnostic instead");
    }
    DhhfspEncodingValidator.validateOrThrow(solution, instance);
    List<DecisionEvent> events = new ArrayList<>();

    List<OperationRecord> initialOperations =
        construct(instance, solution, options, false, "INITIAL_APPEND_ONLY", events);
    ScheduleSnapshot initial = snapshot(
        "INITIAL_APPEND_ONLY", initialOperations, instance, solution, options, true,
        "append-only resource tails");

    List<OperationRecord> fineOperations =
        construct(instance, solution, options, true, "FINE_TUNED", events);
    ScheduleSnapshot fineTuned = snapshot(
        "FINE_TUNED", fineOperations, instance, solution, options, true,
        "earliest common machine-worker idle-slot insertion");

    ScheduleSnapshot rightShifted = rightShift(
        fineOperations, fineTuned, instance, solution, options, events);
    return new DecodeResult(
        options.getMode(), options.getSeed(),
        options.getStandbyEnergyRateProvider().provenance(),
        initial, fineTuned, rightShifted, events);
  }

  private List<OperationRecord> construct(
      DhhfspInstance instance, DhhfspFourVectorSolution solution, DecodeOptions options,
      boolean activeInsertion, String phase, List<DecisionEvent> events) {
    State state = new State();
    int ordinal = 0;
    for (int factory = 0; factory < instance.getNumberOfFactories(); factory++) {
      List<Integer> factoryJobs = jobsInFactory(solution, factory);
      if (factoryJobs.isEmpty()) {
        continue;
      }
      for (int stage = 0; stage < instance.getNumberOfStages(); stage++) {
        List<Integer> dispatchJobs = new ArrayList<>(factoryJobs);
        if (stage > 0) {
          final int previousStage = stage - 1;
          final State currentState = state;
          Collections.sort(dispatchJobs, new Comparator<Integer>() {
            @Override
            public int compare(Integer left, Integer right) {
              OperationRecord leftPrevious = currentState.byJobStage.get(key(left, previousStage));
              OperationRecord rightPrevious = currentState.byJobStage.get(key(right, previousStage));
              int value = Double.compare(
                  leftPrevious.getEndTime(), rightPrevious.getEndTime());
              if (value == 0) {
                value = Integer.compare(
                    leftPrevious.getDispatchOrdinal(), rightPrevious.getDispatchOrdinal());
              }
              if (value == 0) value = Integer.compare(left, right);
              return value;
            }
          });
          events.add(new DecisionEvent(
              phase, "ETC_FIFO_ORDER", "f=" + factory + ",s=" + stage,
              dispatchJobs.toString()));
          recordEtcTies(state, dispatchJobs, previousStage, phase, factory, stage, events);
        }

        int[] workerPermutation = workerPermutation(
            options, factory, stage, instance.getWorkerCount(factory, stage));
        events.add(new DecisionEvent(
            phase,
            options.getMode() == DecodeMode.PUBLISHED_STOCHASTIC
                ? "SEEDED_WORKER_PERMUTATION" : "DETERMINISTIC_WORKER_ORDER",
            "f=" + factory + ",s=" + stage,
            Arrays.toString(workerPermutation)));

        for (int stageOrdinal = 0; stageOrdinal < dispatchJobs.size(); stageOrdinal++) {
          int job = dispatchJobs.get(stageOrdinal);
          double release = stage == 0
              ? 0.0 : state.byJobStage.get(key(job, stage - 1)).getEndTime();
          int machine;
          int worker;
          if (stage == 0) {
            machine = solution.getMachineAssignmentForJob(job);
            worker = solution.getWorkerAssignmentForJob(job);
            events.add(new DecisionEvent(
                phase, "STAGE1_DIRECT", "job=" + job,
                "f=" + factory + ",m=" + machine + ",w=" + worker));
          } else {
            machine = firstAvailableMachine(
                state, instance, factory, stage, release, phase, job, events);
            worker = selectWorker(
                state, instance, factory, stage, stageOrdinal, workerPermutation, release,
                phase, job, events);
            events.add(new DecisionEvent(
                phase, "WORKER_KEYED_CHOICE",
                "factory=" + factory + ",stage=" + stage + ",jobOrdinal=" + stageOrdinal,
                "worker=" + worker + ",mode=" + options.getMode()));
            events.add(new DecisionEvent(
                phase, "FAM_WORKER", "job=" + job + ",s=" + stage,
                "machine=" + machine + ",worker=" + worker
                    + ",branch=" + (stageOrdinal < workerPermutation.length
                    ? "FIRST_WAVE_WITHOUT_REPLACEMENT" : "EARLIEST_AVAILABLE")));
          }
          double setup = instance.getStandardSetupTime(stage, job)
              / instance.getWorkerEfficiencies(factory, stage)[worker];
          double processing = instance.getStandardProcessingTime(stage, job)
              / (instance.getMachineSpeeds(factory, stage)[machine]
              * instance.getWorkerEfficiencies(factory, stage)[worker]);
          double duration = setup + processing;
          double start = activeInsertion
              ? earliestCommonStart(state, factory, stage, machine, worker, release, duration)
              : Math.max(release, Math.max(
                  state.tail(machineKey(factory, stage, machine)),
                  state.tail(workerKey(factory, stage, worker))));
          OperationRecord operation = new OperationRecord(
              job, stage, factory, machine, worker, start, setup, processing,
              start + duration, ordinal++);
          state.add(operation);
          events.add(new DecisionEvent(
              phase, activeInsertion ? "COMMON_IDLE_SLOT" : "APPEND_TAIL",
              "job=" + job + ",s=" + stage,
              "start=" + Double.toString(start) + ",end="
                  + Double.toString(operation.getEndTime())));
        }
      }
    }
    return new ArrayList<>(state.operations);
  }

  private static List<Integer> jobsInFactory(
      DhhfspFourVectorSolution solution, int factory) {
    List<Integer> jobs = new ArrayList<>();
    for (int position = 0; position < solution.getNumberOfVariables(); position++) {
      if (solution.getFactoryAssignments().get(position) == factory) {
        jobs.add(solution.jobAtPosition(position));
      }
    }
    return jobs;
  }

  private static int[] workerPermutation(
      DecodeOptions options, int factory, int stage, int workerCount) {
    int[] workers = new int[workerCount];
    for (int index = 0; index < workerCount; index++) workers[index] = index;
    if (options.getMode() == DecodeMode.PUBLISHED_STOCHASTIC) {
      long derivedSeed = mixSeed(options.getSeed(), factory, stage);
      Random random = new Random(derivedSeed);
      for (int index = workerCount - 1; index > 0; index--) {
        int other = random.nextInt(index + 1);
        int value = workers[index];
        workers[index] = workers[other];
        workers[other] = value;
      }
    }
    return workers;
  }

  private static long mixSeed(long seed, int factory, int stage) {
    long value = seed ^ 0x9E3779B97F4A7C15L;
    value ^= ((long) factory + 1L) * 0xBF58476D1CE4E5B9L;
    value ^= ((long) stage + 1L) * 0x94D049BB133111EBL;
    return value;
  }

  private static int firstAvailableMachine(
      State state, DhhfspInstance instance, int factory, int stage, double release,
      String phase, int job, List<DecisionEvent> events) {
    int selected = 0;
    double best = Math.max(release, state.tail(machineKey(factory, stage, 0)));
    List<Integer> tied = new ArrayList<>();
    tied.add(0);
    for (int machine = 1; machine < instance.getMachineCount(factory, stage); machine++) {
      double available = Math.max(release, state.tail(machineKey(factory, stage, machine)));
      if (available + EPSILON < best) {
        best = available;
        selected = machine;
        tied.clear();
        tied.add(machine);
      } else if (Math.abs(available - best) <= EPSILON) {
        tied.add(machine);
      }
    }
    if (tied.size() > 1) {
      events.add(new DecisionEvent(
          phase, "FAM_TIE_BREAK", "job=" + job + ",stage=" + stage,
          "candidates=" + tied + ",selected=" + selected + ",rule=LOWEST_MACHINE_ID"));
    }
    return selected;
  }

  private static int selectWorker(
      State state, DhhfspInstance instance, int factory, int stage, int stageOrdinal,
      int[] workerPermutation, double release, String phase, int job,
      List<DecisionEvent> events) {
    if (stageOrdinal < workerPermutation.length) {
      return workerPermutation[stageOrdinal];
    }
    int selected = 0;
    double best = Math.max(release, state.tail(workerKey(factory, stage, 0)));
    List<Integer> tied = new ArrayList<>();
    tied.add(0);
    for (int worker = 1; worker < instance.getWorkerCount(factory, stage); worker++) {
      double available = Math.max(release, state.tail(workerKey(factory, stage, worker)));
      if (available + EPSILON < best) {
        best = available;
        selected = worker;
        tied.clear();
        tied.add(worker);
      } else if (Math.abs(available - best) <= EPSILON) {
        tied.add(worker);
      }
    }
    if (tied.size() > 1) {
      events.add(new DecisionEvent(
          phase, "WORKER_TIE_BREAK", "job=" + job + ",stage=" + stage,
          "candidates=" + tied + ",selected=" + selected + ",rule=LOWEST_WORKER_ID"));
    }
    return selected;
  }

  private static void recordEtcTies(
      State state, List<Integer> jobs, int previousStage, String phase,
      int factory, int stage, List<DecisionEvent> events) {
    for (int index = 1; index < jobs.size(); index++) {
      OperationRecord left = state.byJobStage.get(key(jobs.get(index - 1), previousStage));
      OperationRecord right = state.byJobStage.get(key(jobs.get(index), previousStage));
      if (Math.abs(left.getEndTime() - right.getEndTime()) <= EPSILON) {
        events.add(new DecisionEvent(
            phase, "ETC_TIE_BREAK", "f=" + factory + ",s=" + stage
                + ",completion=" + left.getEndTime(),
            "jobs=" + left.getJob() + "," + right.getJob()
                + ",rule=PREVIOUS_DISPATCH_ORDINAL_THEN_JOB_ID"));
      }
    }
  }

  private static double earliestCommonStart(
      State state, int factory, int stage, int machine, int worker,
      double release, double duration) {
    double candidate = release;
    String machineKey = machineKey(factory, stage, machine);
    String workerKey = workerKey(factory, stage, worker);
    while (true) {
      double conflictEnd = candidate;
      conflictEnd = Math.max(conflictEnd,
          conflictEnd(state.resources.get(machineKey), candidate, candidate + duration));
      conflictEnd = Math.max(conflictEnd,
          conflictEnd(state.resources.get(workerKey), candidate, candidate + duration));
      if (conflictEnd <= candidate + EPSILON) {
        return candidate;
      }
      candidate = conflictEnd;
    }
  }

  private static double conflictEnd(
      List<OperationRecord> operations, double start, double end) {
    if (operations == null) return start;
    double conflictEnd = start;
    for (OperationRecord operation : operations) {
      if (start < operation.getEndTime() - EPSILON
          && end > operation.getStartTime() + EPSILON) {
        conflictEnd = Math.max(conflictEnd, operation.getEndTime());
      }
    }
    return conflictEnd;
  }

  private static ScheduleSnapshot snapshot(
      String phase, List<OperationRecord> operations, DhhfspInstance instance,
      DhhfspFourVectorSolution solution, DecodeOptions options,
      boolean accepted, String note) {
    ObjectiveBreakdown objectives = objectives(operations, instance, options);
    ScheduleValidationReport validation =
        ScheduleValidator.validate(instance, solution, operations);
    validation.throwIfInvalid();
    return new ScheduleSnapshot(phase, operations, objectives, validation, accepted, note);
  }

  private static ObjectiveBreakdown objectives(
      List<OperationRecord> operations, DhhfspInstance instance, DecodeOptions options) {
    double makespan = 0.0;
    double processingEnergy = 0.0;
    double workerCost = 0.0;
    Map<String, List<OperationRecord>> machines = new HashMap<>();
    Map<String, Double> workerCosts = new HashMap<>();
    for (OperationRecord operation : operations) {
      if (operation.getStage() == instance.getNumberOfStages() - 1) {
        makespan = Math.max(makespan, operation.getEndTime());
      }
      processingEnergy += operation.getDuration()
          * instance.getMachineEnergyPerUnit(operation.getFactory(), operation.getStage())[
              operation.getMachine()];
      double cost = operation.getDuration()
          * instance.getWorkerCostPerUnit(operation.getFactory(), operation.getStage())[
              operation.getWorker()];
      workerCost += cost;
      String workerKey = workerKey(
          operation.getFactory(), operation.getStage(), operation.getWorker());
      workerCosts.put(workerKey, value(workerCosts, workerKey) + cost);
      add(machines, machineKey(
          operation.getFactory(), operation.getStage(), operation.getMachine()), operation);
    }

    double standbyEnergy = 0.0;
    Map<String, Double> standbyTimes = new HashMap<>();
    List<String> machineKeys = new ArrayList<>(machines.keySet());
    Collections.sort(machineKeys);
    for (String key : machineKeys) {
      List<OperationRecord> assigned = machines.get(key);
      double first = Double.POSITIVE_INFINITY;
      double last = 0.0;
      double busy = 0.0;
      for (OperationRecord operation : assigned) {
        first = Math.min(first, operation.getStartTime());
        last = Math.max(last, operation.getEndTime());
        busy += operation.getDuration();
      }
      double standby = Math.max(0.0, last - first - busy);
      standbyTimes.put(key, standby);
      OperationRecord sample = assigned.get(0);
      standbyEnergy += standby * options.getStandbyEnergyRateProvider().rate(
          sample.getFactory(), sample.getStage(), sample.getMachine());
    }
    return new ObjectiveBreakdown(
        makespan, processingEnergy, standbyEnergy, workerCost,
        sortedMap(standbyTimes), sortedMap(workerCosts));
  }

  private static ScheduleSnapshot rightShift(
      List<OperationRecord> source, ScheduleSnapshot fineTuned, DhhfspInstance instance,
      DhhfspFourVectorSolution solution, DecodeOptions options, List<DecisionEvent> events) {
    Map<String, OperationRecord> originals = new HashMap<>();
    Map<String, List<String>> successors = new HashMap<>();
    Map<Integer, Double> factoryMakespan = new HashMap<>();
    for (OperationRecord operation : source) {
      originals.put(operation.operationKey(), operation);
      factoryMakespan.put(operation.getFactory(), Math.max(
          value(factoryMakespan, operation.getFactory()), operation.getEndTime()));
      successors.put(operation.operationKey(), new ArrayList<String>());
    }
    for (OperationRecord operation : source) {
      OperationRecord nextStage = originals.get(key(operation.getJob(), operation.getStage() + 1));
      if (nextStage != null) successors.get(operation.operationKey()).add(nextStage.operationKey());
    }
    addResourceSuccessors(source, successors, true);
    addResourceSuccessors(source, successors, false);

    List<OperationRecord> reverse = new ArrayList<>(source);
    Collections.sort(reverse, new Comparator<OperationRecord>() {
      @Override
      public int compare(OperationRecord left, OperationRecord right) {
        int value = Double.compare(right.getStartTime(), left.getStartTime());
        if (value == 0) value = Integer.compare(
            right.getDispatchOrdinal(), left.getDispatchOrdinal());
        return value;
      }
    });
    Map<String, OperationRecord> shifted = new HashMap<>();
    for (OperationRecord operation : reverse) {
      double latestEnd = factoryMakespan.get(operation.getFactory());
      for (String successorKey : successors.get(operation.operationKey())) {
        OperationRecord successor = shifted.get(successorKey);
        if (successor == null) successor = originals.get(successorKey);
        latestEnd = Math.min(latestEnd, successor.getStartTime());
      }
      double candidate = Math.max(
          operation.getStartTime(), latestEnd - operation.getDuration());
      shifted.put(operation.operationKey(), operation.shiftedTo(candidate));
    }
    List<OperationRecord> candidate = new ArrayList<>(shifted.values());
    ScheduleValidationReport validation = ScheduleValidator.validate(instance, solution, candidate);
    ObjectiveBreakdown candidateObjectives = objectives(candidate, instance, options);
    ObjectiveBreakdown fineObjectives = fineTuned.getObjectives();
    boolean accepted = validation.isValid()
        && Math.abs(candidateObjectives.getMakespan() - fineObjectives.getMakespan()) <= EPSILON
        && candidateObjectives.getTotalEnergy() <= fineObjectives.getTotalEnergy() + EPSILON
        && Math.abs(candidateObjectives.getTotalWorkerCost()
            - fineObjectives.getTotalWorkerCost()) <= EPSILON;
    events.add(new DecisionEvent(
        "RIGHT_SHIFTED", "RIGHT_SHIFT_ACCEPTANCE", "candidate",
        "accepted=" + accepted + ",valid=" + validation.isValid()
            + ",tecBefore=" + fineObjectives.getTotalEnergy()
            + ",tecAfter=" + candidateObjectives.getTotalEnergy()));
    if (!accepted) {
      return new ScheduleSnapshot(
          "RIGHT_SHIFTED", fineTuned.getOperations(), fineObjectives,
          fineTuned.getValidation(), false,
          "candidate rejected: must preserve constraints/Cmax/TWC and not increase TEC");
    }
    return new ScheduleSnapshot(
        "RIGHT_SHIFTED", candidate, candidateObjectives, validation, true,
        "reverse latest-feasible schedule accepted");
  }

  private static void addResourceSuccessors(
      List<OperationRecord> source, Map<String, List<String>> successors, boolean machine) {
    Map<String, List<OperationRecord>> grouped = new HashMap<>();
    for (OperationRecord operation : source) {
      String resource = machine
          ? machineKey(operation.getFactory(), operation.getStage(), operation.getMachine())
          : workerKey(operation.getFactory(), operation.getStage(), operation.getWorker());
      add(grouped, resource, operation);
    }
    for (List<OperationRecord> operations : grouped.values()) {
      Collections.sort(operations, byStart());
      for (int index = 0; index + 1 < operations.size(); index++) {
        String current = operations.get(index).operationKey();
        String next = operations.get(index + 1).operationKey();
        if (!successors.get(current).contains(next)) successors.get(current).add(next);
      }
    }
  }

  private static Comparator<OperationRecord> byStart() {
    return new Comparator<OperationRecord>() {
      @Override
      public int compare(OperationRecord left, OperationRecord right) {
        int value = Double.compare(left.getStartTime(), right.getStartTime());
        if (value == 0) value = Integer.compare(
            left.getDispatchOrdinal(), right.getDispatchOrdinal());
        return value;
      }
    };
  }

  private static String key(int job, int stage) { return job + ":" + stage; }
  private static String machineKey(int f, int s, int m) { return "f" + f + ":s" + s + ":m" + m; }
  private static String workerKey(int f, int s, int w) { return "f" + f + ":s" + s + ":w" + w; }

  private static <T> void add(Map<String, List<T>> values, String key, T value) {
    List<T> list = values.get(key);
    if (list == null) {
      list = new ArrayList<>();
      values.put(key, list);
    }
    list.add(value);
  }

  private static double value(Map<String, Double> values, String key) {
    Double value = values.get(key);
    return value == null ? 0.0 : value;
  }

  private static double value(Map<Integer, Double> values, Integer key) {
    Double value = values.get(key);
    return value == null ? 0.0 : value;
  }

  private static Map<String, Double> sortedMap(Map<String, Double> values) {
    List<String> keys = new ArrayList<>(values.keySet());
    Collections.sort(keys);
    Map<String, Double> sorted = new LinkedHashMap<>();
    for (String key : keys) sorted.put(key, values.get(key));
    return sorted;
  }

  private static final class State {
    private final List<OperationRecord> operations = new ArrayList<>();
    private final Map<String, OperationRecord> byJobStage = new HashMap<>();
    private final Map<String, List<OperationRecord>> resources = new HashMap<>();

    void add(OperationRecord operation) {
      operations.add(operation);
      byJobStage.put(operation.operationKey(), operation);
      OriginalDhhfspDecoder.add(resources,
          machineKey(operation.getFactory(), operation.getStage(), operation.getMachine()),
          operation);
      OriginalDhhfspDecoder.add(resources,
          workerKey(operation.getFactory(), operation.getStage(), operation.getWorker()),
          operation);
      Collections.sort(resources.get(
          machineKey(operation.getFactory(), operation.getStage(), operation.getMachine())),
          byStart());
      Collections.sort(resources.get(
          workerKey(operation.getFactory(), operation.getStage(), operation.getWorker())),
          byStart());
    }

    double tail(String resource) {
      List<OperationRecord> assigned = resources.get(resource);
      if (assigned == null || assigned.isEmpty()) return 0.0;
      double tail = 0.0;
      for (OperationRecord operation : assigned) tail = Math.max(tail, operation.getEndTime());
      return tail;
    }
  }
}
