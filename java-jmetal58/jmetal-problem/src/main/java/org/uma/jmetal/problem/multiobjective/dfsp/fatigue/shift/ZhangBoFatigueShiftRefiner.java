package org.uma.jmetal.problem.multiobjective.dfsp.fatigue.shift;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.uma.jmetal.problem.multiobjective.dfsp.fatigue.ZhangBoFatigueEvaluationMode;
import org.uma.jmetal.problem.multiobjective.dfsp.fatigue.ZhangBoFatigueEvaluationResult;
import org.uma.jmetal.problem.multiobjective.dfsp.fatigue.ZhangBoFatigueInstanceData;
import org.uma.jmetal.problem.multiobjective.dfsp.fatigue.ZhangBoFatigueOperationRecord;
import org.uma.jmetal.problem.multiobjective.dfsp.fatigue.ZhangBoFatigueParameters;
import org.uma.jmetal.problem.multiobjective.dfsp.fatigue.ZhangBoDecoderNanoClock;

/** One deterministic FCLS pass followed by one deterministic FCRS pass. */
public final class ZhangBoFatigueShiftRefiner {
  private final ZhangBoFatigueSchedulePropagator propagator;
  private final ZhangBoDecoderNanoClock clock;

  public ZhangBoFatigueShiftRefiner() {
    this(new ZhangBoFatigueSchedulePropagator(), ZhangBoDecoderNanoClock.SYSTEM);
  }

  ZhangBoFatigueShiftRefiner(ZhangBoFatigueSchedulePropagator propagator) {
    this(propagator, ZhangBoDecoderNanoClock.SYSTEM);
  }

  public ZhangBoFatigueShiftRefiner(ZhangBoDecoderNanoClock clock) {
    this(new ZhangBoFatigueSchedulePropagator(), clock);
  }

  ZhangBoFatigueShiftRefiner(
      ZhangBoFatigueSchedulePropagator propagator, ZhangBoDecoderNanoClock clock) {
    if (propagator == null || clock == null) {
      throw new IllegalArgumentException("propagator and clock must not be null");
    }
    this.propagator = propagator;
    this.clock = clock;
  }

  public ZhangBoFatigueEvaluationResult refine(
      ZhangBoFatigueEvaluationResult base, ZhangBoFatigueInstanceData instance,
      ZhangBoFatigueParameters parameters, ZhangBoFatigueEvaluationMode evaluationMode,
      ZhangBoShiftConfiguration configuration) {
    if (base == null || instance == null || parameters == null || evaluationMode == null
        || configuration == null) {
      throw new IllegalArgumentException("Shift refinement inputs must not be null");
    }
    long started = clock.nanoTime();
    ZhangBoScheduleGraph graph = ZhangBoScheduleGraph.from(
        base, instance.getJobs(), instance.getStages());
    ZhangBoFatigueEvaluationResult current = base;
    ZhangBoFatigueEvaluationResult afterLeft = base;
    List<ZhangBoShiftEvent> events = new ArrayList<>();
    int[] leftCounts = new int[2];
    int[] rightCounts = new int[2];
    int leftPropagations = 0;
    int rightPropagations = 0;
    long leftShiftNanos = 0L;
    long rightShiftNanos = 0L;

    if (configuration.getMode().usesLeftShift()) {
      long leftStarted = clock.nanoTime();
      List<Integer> order = operationOrder(base, instance.getStages());
      for (int operation : order) {
        ShiftState state = leftOperation(graph, current, operation, instance, parameters,
            evaluationMode, configuration, events);
        graph = state.graph;
        current = state.result;
        leftCounts[0] += state.candidates;
        leftCounts[1] += state.accepted ? 1 : 0;
        leftPropagations += state.propagations;
      }
      afterLeft = current;
      leftShiftNanos = elapsed(leftStarted, clock.nanoTime());
    }

    double cmaxStar = afterLeft.getObjectives()[0];
    if (configuration.getMode().usesRightShift()) {
      long rightStarted = clock.nanoTime();
      int[] order = graph.topologicalOrder();
      for (int index = order.length - 1; index >= 0; index--) {
        ShiftState state = rightOperation(graph, current, order[index], cmaxStar,
            instance, parameters, evaluationMode, configuration, events);
        graph = state.graph;
        current = state.result;
        rightCounts[0] += state.candidates;
        rightCounts[1] += state.accepted ? 1 : 0;
        rightPropagations += state.propagations;
      }
      rightShiftNanos = elapsed(rightStarted, clock.nanoTime());
    }

    long propagationNanos = elapsed(started, clock.nanoTime());
    ZhangBoShiftSummary summary = new ZhangBoShiftSummary(
        configuration.getMode(), base.getObjectives(), afterLeft.getObjectives(),
        current.getObjectives(), afterLeft.getMetrics(), cmaxStar,
        leftCounts[0], leftCounts[1], rightCounts[0],
        rightCounts[1], leftPropagations, rightPropagations,
        leftShiftNanos, rightShiftNanos, propagationNanos, events,
        configuration.isCaptureFullTrace(), base.getOperations(), afterLeft.getOperations(),
        current.getOperations());
    return current.withShiftSummary(summary);
  }

  private static long elapsed(long started, long finished) {
    if (finished < started) {
      throw new IllegalStateException("Decoder NanoClock must be monotonic");
    }
    return finished - started;
  }

  private ShiftState leftOperation(
      ZhangBoScheduleGraph graph, ZhangBoFatigueEvaluationResult current, int operation,
      ZhangBoFatigueInstanceData instance, ZhangBoFatigueParameters parameters,
      ZhangBoFatigueEvaluationMode mode, ZhangBoShiftConfiguration configuration,
      List<ZhangBoShiftEvent> events) {
    int machinePosition = graph.machinePosition(operation);
    int workerPosition = graph.workerPosition(operation);
    if (machinePosition <= 0 && workerPosition <= 0) {
      return ShiftState.unchanged(graph, current);
    }
    Map<Integer, ZhangBoFatigueOperationRecord> records = byOperation(current, graph.getStages());
    ZhangBoFatigueOperationRecord old = records.get(operation);
    List<Proposal> proposals = new ArrayList<>();
    Set<String> fingerprints = new LinkedHashSet<>();
    for (int machineSlot = 0; machineSlot <= machinePosition; machineSlot++) {
      for (int workerSlot = 0; workerSlot <= workerPosition; workerSlot++) {
        if (machineSlot == machinePosition && workerSlot == workerPosition) continue;
        ZhangBoScheduleGraph candidateGraph;
        try {
          candidateGraph = graph.moveEarlier(operation, machineSlot, workerSlot);
          candidateGraph.topologicalOrder();
        } catch (IllegalArgumentException invalid) {
          events.add(event("FCLS", current, null, operation, graph.getStages(), machineSlot,
              workerSlot, Double.NaN, Double.NaN, Double.NaN, Double.NaN, 0, false,
              classify(invalid)));
          continue;
        }
        if (!fingerprints.add(candidateGraph.fingerprint())) continue;
        int jobPrevious = candidateGraph.getStage(operation) == 0 ? -1
            : candidateGraph.getJob(operation) * candidateGraph.getStages()
                + candidateGraph.getStage(operation) - 1;
        int machinePrevious = candidateGraph.predecessorInMachine(operation);
        int workerPrevious = candidateGraph.predecessorInWorker(operation);
        int machineSuccessor = candidateGraph.successorInMachine(operation);
        int workerSuccessor = candidateGraph.successorInWorker(operation);
        double jobEnd = end(records, jobPrevious);
        double machineEnd = end(records, machinePrevious);
        double workerEnd = end(records, workerPrevious);
        double gapLeft = Math.max(jobEnd, Math.max(machineEnd, workerEnd));
        double gapRight = Math.min(start(records, machineSuccessor),
            start(records, workerSuccessor));
        ZhangBoFatigueSchedulePropagator.OperationTransition preview =
            propagator.previewOperation(candidateGraph, operation, instance, parameters, mode,
                jobEnd, machineEnd, workerPrevious < 0 ? null : records.get(workerPrevious),
                gapLeft);
        boolean fits = preview.start < old.start - configuration.getEpsilon()
            && preview.finish <= gapRight + configuration.getEpsilon();
        proposals.add(new Proposal(candidateGraph, machineSlot, workerSlot, gapLeft, gapRight,
            preview.start, preview.finish, fits));
      }
    }
    Collections.sort(proposals, new Comparator<Proposal>() {
      @Override public int compare(Proposal left, Proposal right) {
        int value = Double.compare(left.predictedStart, right.predictedStart);
        if (value == 0) value = Double.compare(left.predictedFinish, right.predictedFinish);
        if (value == 0) value = Integer.compare(left.machineSlot, right.machineSlot);
        if (value == 0) value = Integer.compare(left.workerSlot, right.workerSlot);
        return value != 0 ? value : left.graph.fingerprint().compareTo(right.graph.fingerprint());
      }
    });
    if (proposals.size() > configuration.getMaximumLeftCandidates()) {
      proposals = new ArrayList<>(proposals.subList(0, configuration.getMaximumLeftCandidates()));
    }

    int attempts = 0;
    int propagations = 0;
    for (Proposal proposal : proposals) {
      attempts++;
      if (!proposal.previewFits) {
        events.add(event("FCLS", current, null, operation, graph.getStages(),
            proposal.machineSlot, proposal.workerSlot, proposal.gapLeft, proposal.gapRight,
            0.0, 0.0, 0, false, "COMMON_GAP_TOO_SHORT"));
        continue;
      }
      ZhangBoFatigueEvaluationResult candidate;
      try {
        candidate = propagator.propagate(proposal.graph, instance, parameters, mode);
        propagations++;
      } catch (IllegalArgumentException invalid) {
        events.add(event("FCLS", current, null, operation, graph.getStages(),
            proposal.machineSlot, proposal.workerSlot, proposal.gapLeft, proposal.gapRight,
            0.0, 0.0, 0, false, classify(invalid)));
        continue;
      }
      String invalid = feasibilityReason(proposal.graph, candidate, configuration.getEpsilon());
      ZhangBoFatigueOperationRecord shifted = operation(candidate, operation, graph.getStages());
      String reason = invalid;
      if (reason == null) reason = leftObjectiveReason(old.start, shifted.start,
          current.getObjectives(), candidate.getObjectives(), configuration.getEpsilon());
      boolean accepted = reason == null;
      events.add(event("FCLS", current, candidate, operation, graph.getStages(),
          proposal.machineSlot, proposal.workerSlot, proposal.gapLeft, proposal.gapRight,
          Double.NaN, old.start - proposal.predictedStart,
          accepted ? old.start - shifted.start : 0.0, 0, accepted,
          accepted ? "ACCEPTED" : reason));
      if (accepted) {
        return new ShiftState(proposal.graph, candidate, attempts, true, propagations);
      }
    }
    return new ShiftState(graph, current, attempts, false, propagations);
  }

  private ShiftState rightOperation(
      ZhangBoScheduleGraph graph, ZhangBoFatigueEvaluationResult current, int operation,
      double cmaxStar, ZhangBoFatigueInstanceData instance,
      ZhangBoFatigueParameters parameters, ZhangBoFatigueEvaluationMode mode,
      ZhangBoShiftConfiguration configuration, List<ZhangBoShiftEvent> events) {
    ZhangBoFatigueOperationRecord old = operation(current, operation, graph.getStages());
    double latestEnd = cmaxStar;
    for (int successor : graph.successors()[operation]) {
      latestEnd = Math.min(latestEnd, operation(current, successor, graph.getStages()).start);
    }
    double proposalUpper = latestEnd - old.actualDuration;
    double initialShift = proposalUpper - old.start;
    if (initialShift <= configuration.getEpsilon()) return ShiftState.unchanged(graph, current);
    int candidates = 0;
    int propagations = 0;
    for (int attempt = 0; attempt < configuration.getMaximumRightAttempts(); attempt++) {
      double proposalShift = initialShift / Math.pow(2.0, attempt);
      if (proposalShift <= configuration.getEpsilon()) break;
      double release = old.start + proposalShift;
      ZhangBoScheduleGraph candidateGraph = graph.withRelease(operation, release);
      ZhangBoFatigueEvaluationResult candidate;
      candidates++;
      try {
        candidate = propagator.propagate(candidateGraph, instance, parameters, mode);
        propagations++;
      } catch (IllegalArgumentException invalid) {
        events.add(event("FCRS", current, null, operation, graph.getStages(),
            graph.machinePosition(operation), graph.workerPosition(operation), Double.NaN,
            Double.NaN, cmaxStar, proposalShift, attempt + 1, false, classify(invalid)));
        continue;
      }
      ZhangBoFatigueOperationRecord shifted = operation(candidate, operation, graph.getStages());
      double[] before = current.getObjectives();
      double[] after = candidate.getObjectives();
      String reason = feasibilityReason(candidateGraph, candidate, configuration.getEpsilon());
      if (reason == null) reason = rightObjectiveReason(old.start, shifted.start,
          cmaxStar, before, after, configuration.getEpsilon());
      boolean accepted = reason == null;
      events.add(event("FCRS", current, candidate, operation, graph.getStages(),
          graph.machinePosition(operation), graph.workerPosition(operation), Double.NaN,
          Double.NaN, cmaxStar, proposalShift,
          accepted ? shifted.start - old.start : 0.0, attempt + 1, accepted,
          accepted ? "ACCEPTED" : reason));
      if (accepted) {
        return new ShiftState(candidateGraph, candidate, candidates, true, propagations);
      }
    }
    return new ShiftState(graph, current, candidates, false, propagations);
  }

  private static String feasibilityReason(
      ZhangBoScheduleGraph graph, ZhangBoFatigueEvaluationResult result, double epsilon) {
    try {
      graph.topologicalOrder();
    } catch (IllegalArgumentException cycle) {
      return "DAG_CYCLE";
    }
    Map<Integer, ZhangBoFatigueOperationRecord> records = byOperation(result, graph.getStages());
    if (records.size() != graph.operationCount()) return "MISSING_OPERATION";
    for (int operation = 0; operation < graph.operationCount(); operation++) {
      ZhangBoFatigueOperationRecord record = records.get(operation);
      if (record == null || record.factory != graph.getFactory(operation)
          || record.machine != graph.getMachine(operation)
          || record.worker != graph.getWorker(operation)) {
        return "INVALID_RESOURCE_ASSIGNMENT";
      }
      if (record.stage > 0) {
        ZhangBoFatigueOperationRecord previous = records.get(
            record.job * graph.getStages() + record.stage - 1);
        if (previous == null || record.start < previous.end - epsilon) {
          return "JOB_PRECEDENCE_VIOLATION";
        }
      }
    }
    List<ZhangBoFatigueOperationRecord> values = new ArrayList<>(result.getOperations());
    for (int left = 0; left < values.size(); left++) {
      ZhangBoFatigueOperationRecord a = values.get(left);
      for (int right = left + 1; right < values.size(); right++) {
        ZhangBoFatigueOperationRecord b = values.get(right);
        boolean overlap = a.start < b.end - epsilon && b.start < a.end - epsilon;
        if (!overlap || a.factory != b.factory) continue;
        if (a.stage == b.stage && a.machine == b.machine) return "MACHINE_OVERLAP";
        if (a.worker == b.worker) return "WORKER_OVERLAP";
      }
    }
    return null;
  }

  static String leftObjectiveReason(
      double oldStart, double newStart, double[] before, double[] after, double epsilon) {
    if (newStart >= oldStart - epsilon) return "NOT_EARLIER";
    if (after[0] > before[0] + epsilon) return "CMAX_WORSE";
    return null;
  }

  static String rightObjectiveReason(
      double oldStart, double newStart, double cmaxStar, double[] before,
      double[] after, double epsilon) {
    if (newStart <= oldStart + epsilon) return "NOT_LATER";
    if (after[0] > cmaxStar + epsilon) return "CMAX_STAR_EXCEEDED";
    if (after[1] > before[1] + epsilon) return "TEC_WORSE";
    if (after[6] > before[6] + epsilon) return "TWC_WORSE";
    if (after[1] >= before[1] - epsilon && after[6] >= before[6] - epsilon) {
      return "NO_TEC_TWC_GAIN";
    }
    return null;
  }

  private static String classify(IllegalArgumentException invalid) {
    String message = invalid.getMessage() == null ? "" : invalid.getMessage();
    if (message.contains("cycle")) return "DAG_CYCLE";
    if (message.contains("assignment") || message.contains("worker")) {
      return "INVALID_RESOURCE_ASSIGNMENT";
    }
    return "INVALID_SCHEDULE";
  }

  private static List<Integer> operationOrder(
      ZhangBoFatigueEvaluationResult result, final int stages) {
    List<ZhangBoFatigueOperationRecord> records = new ArrayList<>(result.getOperations());
    Collections.sort(records, new Comparator<ZhangBoFatigueOperationRecord>() {
      @Override public int compare(ZhangBoFatigueOperationRecord left,
          ZhangBoFatigueOperationRecord right) {
        int value = Integer.compare(left.stage, right.stage);
        if (value == 0) value = Double.compare(left.start, right.start);
        if (value == 0) value = Integer.compare(left.job, right.job);
        if (value == 0) value = Integer.compare(left.factory, right.factory);
        return value;
      }
    });
    List<Integer> resultOrder = new ArrayList<>();
    for (ZhangBoFatigueOperationRecord record : records) {
      resultOrder.add(record.job * stages + record.stage);
    }
    return resultOrder;
  }

  private static Map<Integer, ZhangBoFatigueOperationRecord> byOperation(
      ZhangBoFatigueEvaluationResult result, int stages) {
    Map<Integer, ZhangBoFatigueOperationRecord> records = new HashMap<>();
    for (ZhangBoFatigueOperationRecord record : result.getOperations()) {
      records.put(record.job * stages + record.stage, record);
    }
    return records;
  }

  private static double end(Map<Integer, ZhangBoFatigueOperationRecord> records, int operation) {
    return operation < 0 ? 0.0 : records.get(operation).end;
  }

  private static double start(Map<Integer, ZhangBoFatigueOperationRecord> records, int operation) {
    return operation < 0 ? Double.POSITIVE_INFINITY : records.get(operation).start;
  }

  private static ZhangBoFatigueOperationRecord operation(
      ZhangBoFatigueEvaluationResult result, int operation, int stages) {
    int job = operation / stages;
    int stage = operation % stages;
    for (ZhangBoFatigueOperationRecord record : result.getOperations()) {
      if (record.job == job && record.stage == stage) return record;
    }
    throw new IllegalArgumentException("Missing operation job=" + job + ", stage=" + stage);
  }

  private static ZhangBoShiftEvent event(
      String phase, ZhangBoFatigueEvaluationResult before,
      ZhangBoFatigueEvaluationResult after, int operation, int stages,
      int machineSlot, int workerSlot, double gapLeft, double gapRight,
      double cmaxStar, double proposalShift, int attempt, boolean accepted, String reason) {
    return event(phase, before, after, operation, stages, machineSlot, workerSlot, gapLeft,
        gapRight, cmaxStar, proposalShift, accepted && after != null
            ? Math.abs(operation(after, operation, stages).start
                - operation(before, operation, stages).start) : 0.0,
        attempt, accepted, reason);
  }

  private static ZhangBoShiftEvent event(
      String phase, ZhangBoFatigueEvaluationResult before,
      ZhangBoFatigueEvaluationResult after, int operation, int stages,
      int machineSlot, int workerSlot, double gapLeft, double gapRight,
      double cmaxStar, double proposalShift, double acceptedShift, int attempt,
      boolean accepted, String reason) {
    ZhangBoFatigueOperationRecord old = operation(before, operation, stages);
    ZhangBoFatigueOperationRecord value = after == null ? old : operation(after, operation, stages);
    double[] oldObjectives = before.getObjectives();
    double[] newObjectives = after == null ? oldObjectives : after.getObjectives();
    return new ZhangBoShiftEvent(phase, old.job, old.stage, machineSlot, workerSlot,
        gapLeft, gapRight, old.start, value.start, old.fatigueAtStart,
        value.fatigueAtStart, old.actualDuration, value.actualDuration, old.end, value.end,
        oldObjectives[0], newObjectives[0], oldObjectives[1], newObjectives[1],
        oldObjectives[6], newObjectives[6], cmaxStar, proposalShift, acceptedShift,
        attempt, accepted, reason);
  }

  private static final class Proposal {
    private final ZhangBoScheduleGraph graph;
    private final int machineSlot;
    private final int workerSlot;
    private final double gapLeft;
    private final double gapRight;
    private final double predictedStart;
    private final double predictedFinish;
    private final boolean previewFits;

    private Proposal(
        ZhangBoScheduleGraph graph, int machineSlot, int workerSlot, double gapLeft,
        double gapRight, double predictedStart, double predictedFinish,
        boolean previewFits) {
      this.graph = graph;
      this.machineSlot = machineSlot;
      this.workerSlot = workerSlot;
      this.gapLeft = gapLeft;
      this.gapRight = gapRight;
      this.predictedStart = predictedStart;
      this.predictedFinish = predictedFinish;
      this.previewFits = previewFits;
    }
  }

  private static final class ShiftState {
    private final ZhangBoScheduleGraph graph;
    private final ZhangBoFatigueEvaluationResult result;
    private final int candidates;
    private final boolean accepted;
    private final int propagations;

    private ShiftState(
        ZhangBoScheduleGraph graph, ZhangBoFatigueEvaluationResult result,
        int candidates, boolean accepted, int propagations) {
      this.graph = graph;
      this.result = result;
      this.candidates = candidates;
      this.accepted = accepted;
      this.propagations = propagations;
    }

    private static ShiftState unchanged(
        ZhangBoScheduleGraph graph, ZhangBoFatigueEvaluationResult result) {
      return new ShiftState(graph, result, 0, false, 0);
    }
  }
}
