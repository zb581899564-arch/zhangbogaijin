package org.uma.jmetal.algorithm.multiobjective.mypso.zhangbo.neighborhood;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import org.uma.jmetal.algorithm.multiobjective.mypso.zhangbo.ZhangBoSubSwarm;
import org.uma.jmetal.algorithm.multiobjective.mypso.zhangbo.ZhangBoSubSwarmSemantics;
import org.uma.jmetal.problem.multiobjective.dfsp.fatigue.ZhangBoFatigueEvaluationResult;
import org.uma.jmetal.problem.multiobjective.dfsp.fatigue.ZhangBoFatigueInstanceData;
import org.uma.jmetal.problem.multiobjective.dfsp.fatigue.ZhangBoFatigueModel;
import org.uma.jmetal.problem.multiobjective.dfsp.fatigue.ZhangBoFatigueOperationRecord;
import org.uma.jmetal.problem.multiobjective.dfsp.fatigue.ZhangBoFatigueParameters;
import org.uma.jmetal.solution.PermutationSolution;
import org.uma.jmetal.util.pseudorandom.PseudoRandomGenerator;
import org.uma.jmetal.util.pseudorandom.impl.JavaRandomGenerator;

/**
 * Standalone P7.1 implementation of O1-O13. It is intentionally not called by
 * {@code ZhangBoMOHPSOQ.perturbation()}; P7.2 owns production integration.
 */
public final class ZhangBoNeighborhoodSuite {
  private static final double EPS = 1e-12;

  /**
   * Generates stable, legal first-stage candidates without consuming an FE.
   * O13 is intentionally returned before its recovery-gain post-check; that
   * check requires the one budgeted complete evaluation in P7.2.
   */
  public ZhangBoNeighborhoodPreview preview(
      ZhangBoNeighborhoodId id, ZhangBoNeighborhoodRequest request) {
    if (id == null || request == null) throw new IllegalArgumentException("id and request are required");
    List<String> diagnostics = new ArrayList<>();
    PseudoRandomGenerator random = new JavaRandomGenerator(
        request.getSeed() ^ (0x9E3779B97F4A7C15L * (id.getNumber() + 1L)));
    List<PermutationSolution<Integer>> candidates;
    switch (id) {
      case O1_JS_INSERT: candidates = basicJs(request, random, 1, diagnostics); break;
      case O2_JS_REVERSE: candidates = basicJs(request, random, 2, diagnostics); break;
      case O3_JS_SWAP: candidates = basicJs(request, random, 3, diagnostics); break;
      case O4_WA_LOAD_TRANSFER: candidates = workerMove(request, 4, diagnostics); break;
      case O5_WA_WEAK_TO_STRONG: candidates = workerMove(request, 5, diagnostics); break;
      case O6_WA_SWAP: candidates = workerMove(request, 6, diagnostics); break;
      case O7_MA_LOAD_TRANSFER: candidates = machineMove(request, 7, diagnostics); break;
      case O8_MA_WEAK_TO_STRONG: candidates = machineMove(request, 8, diagnostics); break;
      case O9_MA_SWAP: candidates = machineMove(request, 9, diagnostics); break;
      case O10_CRITICAL_BLOCK: candidates = criticalBlock(request, diagnostics); break;
      case O11_FATIGUE_WORKER_REASSIGNMENT: candidates = fatigueWorker(request, diagnostics); break;
      case O12_JOINT_MACHINE_WORKER: candidates = jointMachineWorker(request, diagnostics); break;
      case O13_NATURAL_RECOVERY_WINDOW: candidates = rawRecoveryWindow(request, diagnostics); break;
      default: throw new IllegalArgumentException("Unsupported neighborhood: " + id);
    }
    int cap = ZhangBoNeighborhoodConfiguration.cap(id);
    if (candidates.size() > cap) candidates = new ArrayList<>(candidates.subList(0, cap));
    List<PermutationSolution<Integer>> legal = new ArrayList<>();
    for (PermutationSolution<Integer> candidate : candidates) {
      try {
        ZhangBoNeighborhoodVectors.validateFirstStage(candidate, request.getInstance());
        legal.add(candidate);
      } catch (IllegalArgumentException exception) {
        diagnostics.add("ILLEGAL_CANDIDATE=" + exception.getMessage());
      }
    }
    Collections.sort(legal, Comparator.comparing(ZhangBoNeighborhoodVectors::fingerprint));
    if (legal.isEmpty()) {
      String reason = diagnostics.isEmpty() ? "NOT_APPLICABLE" : diagnostics.get(diagnostics.size() - 1);
      return new ZhangBoNeighborhoodPreview(id, false, reason, legal, diagnostics);
    }
    diagnostics.add("previewCandidateCap=" + cap);
    return new ZhangBoNeighborhoodPreview(id, true, "APPLICABLE", legal, diagnostics);
  }

  public ZhangBoNeighborhoodResult apply(
      ZhangBoNeighborhoodId id,
      ZhangBoNeighborhoodRequest request,
      ZhangBoNeighborhoodEvaluationGateway gateway) {
    if (id == null || request == null || gateway == null) {
      throw new IllegalArgumentException("id, request and gateway are required");
    }
    long start = System.nanoTime();
    int before = gateway.getEvaluationCount();
    List<String> diagnostics = new ArrayList<>();
    List<PermutationSolution<Integer>> candidates;
    PseudoRandomGenerator random = new JavaRandomGenerator(
        request.getSeed() ^ (0x9E3779B97F4A7C15L * (id.ordinal() + 1L)));
    switch (id) {
      case O1_JS_INSERT: candidates = basicJs(request, random, 1, diagnostics); break;
      case O2_JS_REVERSE: candidates = basicJs(request, random, 2, diagnostics); break;
      case O3_JS_SWAP: candidates = basicJs(request, random, 3, diagnostics); break;
      case O4_WA_LOAD_TRANSFER: candidates = workerMove(request, 4, diagnostics); break;
      case O5_WA_WEAK_TO_STRONG: candidates = workerMove(request, 5, diagnostics); break;
      case O6_WA_SWAP: candidates = workerMove(request, 6, diagnostics); break;
      case O7_MA_LOAD_TRANSFER: candidates = machineMove(request, 7, diagnostics); break;
      case O8_MA_WEAK_TO_STRONG: candidates = machineMove(request, 8, diagnostics); break;
      case O9_MA_SWAP: candidates = machineMove(request, 9, diagnostics); break;
      case O10_CRITICAL_BLOCK: candidates = criticalBlock(request, diagnostics); break;
      case O11_FATIGUE_WORKER_REASSIGNMENT:
        candidates = fatigueWorker(request, diagnostics); break;
      case O12_JOINT_MACHINE_WORKER:
        candidates = jointMachineWorker(request, diagnostics); break;
      case O13_NATURAL_RECOVERY_WINDOW:
        candidates = recoveryWindow(request, diagnostics, gateway); break;
      default: throw new IllegalArgumentException("Unsupported neighborhood: " + id);
    }

    int cap = ZhangBoNeighborhoodConfiguration.cap(id);
    if (candidates.size() > cap) candidates = new ArrayList<>(candidates.subList(0, cap));
    if (candidates.isEmpty()) {
      String reason = diagnostics.isEmpty() ? "NOT_APPLICABLE"
          : diagnostics.get(diagnostics.size() - 1);
      int spent = gateway.getEvaluationCount() - before;
      return new ZhangBoNeighborhoodResult(id, false, reason, 0, spent,
          System.nanoTime() - start, diagnostics, null);
    }
    if (id != ZhangBoNeighborhoodId.O13_NATURAL_RECOVERY_WINDOW) {
      for (PermutationSolution<Integer> candidate : candidates) {
        try {
          ZhangBoNeighborhoodVectors.validateFirstStage(candidate, request.getInstance());
        } catch (IllegalArgumentException exception) {
          throw new IllegalArgumentException(id + " produced an illegal candidate: "
              + exception.getMessage(), exception);
        }
        gateway.evaluate(candidate);
      }
    }
    List<PermutationSolution<Integer>> evaluated = new ArrayList<>();
    for (PermutationSolution<Integer> candidate : candidates) {
      if (fatigueResult(candidate) != null) evaluated.add(candidate);
    }
    PermutationSolution<Integer> selected = evaluated.isEmpty() ? null
        : id == ZhangBoNeighborhoodId.O13_NATURAL_RECOVERY_WINDOW
            ? evaluated.get(0) : bestByDirection(evaluated, request.getSubSwarm());
    int evaluations = gateway.getEvaluationCount() - before;
    diagnostics.add("logicalLayers=" + logicalLayers(id));
    diagnostics.add("candidateCap=" + cap);
    diagnostics.add("completeEvaluations=" + evaluations);
    return new ZhangBoNeighborhoodResult(id, true, "APPLICABLE", candidates.size(),
        evaluations, System.nanoTime() - start, diagnostics, selected);
  }

  private static List<PermutationSolution<Integer>> basicJs(
      ZhangBoNeighborhoodRequest request, PseudoRandomGenerator random, int type,
      List<String> diagnostics) {
    List<Integer> positions = factoryPositions(request);
    if (positions.size() < 2) return none(diagnostics, "INSUFFICIENT_FACTORY_JOBS");
    int firstIndex = random.nextInt(0, positions.size() - 1);
    int secondIndex = random.nextInt(0, positions.size() - 2);
    if (secondIndex >= firstIndex) secondIndex++;
    int first = positions.get(firstIndex);
    int second = positions.get(secondIndex);
    if (first > second) { int swap = first; first = second; second = swap; }
    PermutationSolution<Integer> candidate = ZhangBoNeighborhoodVectors.copy(request.getSource());
    if (type == 1) ZhangBoNeighborhoodVectors.insertBundle(candidate, first, second);
    else if (type == 2) ZhangBoNeighborhoodVectors.reverseBundles(candidate, first, second);
    else ZhangBoNeighborhoodVectors.swapBundles(candidate, first, second);
    diagnostics.add("physicalPositions=" + first + "," + second);
    diagnostics.add("resourceBundleReassignment=false");
    return one(candidate);
  }

  private static List<PermutationSolution<Integer>> workerMove(
      ZhangBoNeighborhoodRequest request, int type, List<String> diagnostics) {
    List<Integer> positions = factoryPositions(request);
    if (positions.isEmpty()) return none(diagnostics, "NO_FACTORY_JOB");
    int factory = request.getFactory();
    int[] eligible = request.getInstance().getEligibleWorkers(factory, 0);
    if (eligible.length < 2) return none(diagnostics, "SINGLE_ELIGIBLE_WORKER");
    PermutationSolution<Integer> candidate = ZhangBoNeighborhoodVectors.copy(request.getSource());
    if (type == 6) {
      int[] pair = firstDifferentPair(positions, candidate.getVariablesworker());
      if (pair == null) return none(diagnostics, "NO_DISTINCT_WORKER_PAIR");
      int value = candidate.getVariableValueworker(pair[0]);
      candidate.setVariableValueworker(pair[0], candidate.getVariableValueworker(pair[1]));
      candidate.setVariableValueworker(pair[1], value);
      diagnostics.add("workerSwapJobs=" + candidate.getVariableValue(pair[0]) + ","
          + candidate.getVariableValue(pair[1]));
      return one(candidate);
    }
    int[] load = new int[request.getInstance().getWorkerCount(factory)];
    for (int position : positions) load[candidate.getVariableValueworker(position)]++;
    int target = positions.get(0);
    for (int position : positions) {
      int current = candidate.getVariableValueworker(position);
      int selected = candidate.getVariableValueworker(target);
      if (type == 4 ? load[current] > load[selected]
          : request.getInstance().getWorkerEfficiency(factory, current)
              < request.getInstance().getWorkerEfficiency(factory, selected)) target = position;
    }
    int replacement = candidate.getVariableValueworker(target);
    for (int worker : eligible) {
      if (type == 4) {
        if (load[worker] < load[replacement]) replacement = worker;
      } else if (request.getInstance().getWorkerEfficiency(factory, worker)
          > request.getInstance().getWorkerEfficiency(factory, replacement)) replacement = worker;
    }
    if (replacement == candidate.getVariableValueworker(target)) {
      return none(diagnostics, type == 4 ? "NO_WORKER_LOAD_IMPROVEMENT" : "NO_STRONGER_WORKER");
    }
    diagnostics.add("job=" + candidate.getVariableValue(target) + ",oldWorker="
        + candidate.getVariableValueworker(target) + ",newWorker=" + replacement);
    candidate.setVariableValueworker(target, replacement);
    return one(candidate);
  }

  private static List<PermutationSolution<Integer>> machineMove(
      ZhangBoNeighborhoodRequest request, int type, List<String> diagnostics) {
    List<Integer> positions = factoryPositions(request);
    if (positions.isEmpty()) return none(diagnostics, "NO_FACTORY_JOB");
    int factory = request.getFactory();
    int machineCount = request.getInstance().getMachineCount(factory, 0);
    if (machineCount < 2) return none(diagnostics, "SINGLE_MACHINE");
    PermutationSolution<Integer> candidate = ZhangBoNeighborhoodVectors.copy(request.getSource());
    if (type == 9) {
      List<Integer> values = new ArrayList<>();
      for (int position : positions) values.add(ZhangBoNeighborhoodVectors.machine(candidate, position));
      int[] localPair = firstDifferentPair(range(values.size()), values);
      if (localPair == null) return none(diagnostics, "NO_DISTINCT_MACHINE_PAIR");
      int left = positions.get(localPair[0]);
      int right = positions.get(localPair[1]);
      int value = ZhangBoNeighborhoodVectors.machine(candidate, left);
      ZhangBoNeighborhoodVectors.machine(candidate, left,
          ZhangBoNeighborhoodVectors.machine(candidate, right));
      ZhangBoNeighborhoodVectors.machine(candidate, right, value);
      diagnostics.add("machineSwapPositions=" + left + "," + right);
      return one(candidate);
    }
    int[] load = new int[machineCount];
    for (int position : positions) load[ZhangBoNeighborhoodVectors.machine(candidate, position)]++;
    int target = positions.get(0);
    for (int position : positions) {
      int current = ZhangBoNeighborhoodVectors.machine(candidate, position);
      int selected = ZhangBoNeighborhoodVectors.machine(candidate, target);
      if (type == 7 ? load[current] > load[selected]
          : request.getInstance().getMachineSpeed(factory, 0, current)
              < request.getInstance().getMachineSpeed(factory, 0, selected)) target = position;
    }
    int replacement = ZhangBoNeighborhoodVectors.machine(candidate, target);
    for (int machine = 0; machine < machineCount; machine++) {
      if (type == 7) {
        if (load[machine] < load[replacement]) replacement = machine;
      } else if (request.getInstance().getMachineSpeed(factory, 0, machine)
          > request.getInstance().getMachineSpeed(factory, 0, replacement)) replacement = machine;
    }
    if (replacement == ZhangBoNeighborhoodVectors.machine(candidate, target)) {
      return none(diagnostics, type == 7 ? "NO_MACHINE_LOAD_IMPROVEMENT" : "NO_FASTER_MACHINE");
    }
    diagnostics.add("job=" + candidate.getVariableValue(target) + ",oldMachine="
        + ZhangBoNeighborhoodVectors.machine(candidate, target) + ",newMachine=" + replacement);
    ZhangBoNeighborhoodVectors.machine(candidate, target, replacement);
    return one(candidate);
  }

  private static List<PermutationSolution<Integer>> criticalBlock(
      ZhangBoNeighborhoodRequest request, List<String> diagnostics) {
    ZhangBoCriticalDagAnalyzer.Analysis analysis = ZhangBoCriticalDagAnalyzer.analyze(
        fatigueResult(request.getSource()).getOperations(), request.getFactory(),
        ZhangBoNeighborhoodConfiguration.CRITICAL_TOLERANCE);
    if (analysis.critical.isEmpty()) return none(diagnostics, "NO_CRITICAL_OPERATION");
    diagnostics.add("criticalJobs=" + jobs(analysis.critical));
    diagnostics.add("criticalBlocks=" + analysis.blocks.size());
    Set<String> moves = new LinkedHashSet<>();
    for (List<ZhangBoFatigueOperationRecord> block : analysis.blocks) {
      addCriticalMoves(request.getSource(), block.get(0).job, moves);
      addCriticalMoves(request.getSource(), block.get(block.size() - 1).job, moves);
    }
    if (moves.isEmpty()) {
      for (ZhangBoFatigueOperationRecord operation : analysis.critical) {
        addCriticalMoves(request.getSource(), operation.job, moves);
      }
    }
    List<String> sorted = new ArrayList<>(moves);
    Collections.sort(sorted);
    List<PermutationSolution<Integer>> result = new ArrayList<>();
    for (String move : sorted) {
      if (result.size() == ZhangBoNeighborhoodConfiguration.O10_CAP) break;
      String[] parts = move.split(":");
      int from = Integer.parseInt(parts[1]);
      int to = Integer.parseInt(parts[2]);
      PermutationSolution<Integer> candidate = ZhangBoNeighborhoodVectors.copy(request.getSource());
      if ("I".equals(parts[0])) ZhangBoNeighborhoodVectors.insertBundle(candidate, from, to);
      else ZhangBoNeighborhoodVectors.swapBundles(candidate, from, to);
      result.add(candidate);
      diagnostics.add("criticalMove=" + move);
    }
    return result.isEmpty() ? none(diagnostics, "NO_CRITICAL_MOVE") : result;
  }

  private static void addCriticalMoves(
      PermutationSolution<Integer> solution, int job, Set<String> moves) {
    int position = ZhangBoNeighborhoodVectors.positionOfJob(solution, job);
    if (position > 0) {
      moves.add("I:" + position + ":" + (position - 1));
      moves.add("S:" + position + ":" + (position - 1));
    }
    if (position + 1 < solution.getNumberOfVariables()) {
      moves.add("I:" + position + ":" + (position + 1));
      moves.add("S:" + position + ":" + (position + 1));
    }
  }

  private static List<PermutationSolution<Integer>> fatigueWorker(
      ZhangBoNeighborhoodRequest request, List<String> diagnostics) {
    ZhangBoFatigueEvaluationResult evaluation = fatigueResult(request.getSource());
    List<ZhangBoFatigueOperationRecord> stageZero = factoryStageZero(evaluation, request.getFactory());
    if (stageZero.isEmpty()) return none(diagnostics, "NO_FIRST_STAGE_OPERATION");
    boolean setupAvailable = request.getInstance().hasStandardSetupTimes();
    double[] setup = values(stageZero, 0);
    double[] fatigue = values(stageZero, 1);
    double[] duration = values(stageZero, 2);
    ZhangBoFatigueOperationRecord target = stageZero.get(0);
    double maximum = Double.NEGATIVE_INFINITY;
    for (int i = 0; i < stageZero.size(); i++) {
      double score = setupAvailable
          ? (normalize(setup[i], setup) + normalize(fatigue[i], fatigue)
              + normalize(duration[i], duration)) / 3.0
          : (normalize(fatigue[i], fatigue) + normalize(duration[i], duration)) / 2.0;
      if (score > maximum + EPS || Math.abs(score - maximum) <= EPS
          && stageZero.get(i).job < target.job) {
        maximum = score;
        target = stageZero.get(i);
      }
    }
    diagnostics.add("targetJob=" + target.job + ",setupComponentEnabled=" + setupAvailable);
    int[] workers = request.getInstance().getEligibleWorkers(request.getFactory(), 0);
    List<PredictedMove> moves = new ArrayList<>();
    for (int worker : workers) {
      if (worker == target.worker) continue;
      Prediction prediction = predict(request, target, target.machine, worker);
      moves.add(new PredictedMove(target.job, target.machine, worker, 0.0, prediction));
    }
    if (moves.isEmpty()) return none(diagnostics, "NO_ALTERNATIVE_WORKER");
    normalizePredictedScores(moves, setupAvailable ? new double[] {1.0 / 3.0, 0.0, 0.0,
        1.0 / 3.0, 1.0 / 3.0} : new double[] {0.0, 0.0, 0.0, 0.5, 0.5});
    moves.sort(Comparator.comparingDouble((PredictedMove value) -> value.score)
        .thenComparingInt(value -> value.worker));
    List<PermutationSolution<Integer>> candidates = new ArrayList<>();
    for (PredictedMove move : moves) {
      if (candidates.size() == ZhangBoNeighborhoodConfiguration.O11_CAP) break;
      PermutationSolution<Integer> candidate = ZhangBoNeighborhoodVectors.copy(request.getSource());
      int position = ZhangBoNeighborhoodVectors.positionOfJob(candidate, move.job);
      candidate.setVariableValueworker(position, move.worker);
      candidates.add(candidate);
      diagnostics.add("predictedWorker=" + move.worker + ",setup=" + number(move.prediction.baseSetup)
          + ",deltaFatigue=" + number(move.prediction.deltaFatigue)
          + ",actualDuration=" + number(move.prediction.actualDuration));
    }
    return candidates;
  }

  private static List<PermutationSolution<Integer>> jointMachineWorker(
      ZhangBoNeighborhoodRequest request, List<String> diagnostics) {
    ZhangBoFatigueEvaluationResult evaluation = fatigueResult(request.getSource());
    List<ZhangBoFatigueOperationRecord> stageZero = factoryStageZero(evaluation, request.getFactory());
    if (stageZero.isEmpty()) return none(diagnostics, "NO_FIRST_STAGE_OPERATION");
    double[] weights = directionWeights(request.getSubSwarm());
    double[] at = values(stageZero, 2);
    double[] energy = values(stageZero, 3);
    double[] cost = values(stageZero, 4);
    double[] fatigue = values(stageZero, 1);
    ZhangBoFatigueOperationRecord target = stageZero.get(0);
    double maximum = Double.NEGATIVE_INFINITY;
    for (int i = 0; i < stageZero.size(); i++) {
      double score = weights[0] * normalize(at[i], at) + weights[1] * normalize(energy[i], energy)
          + weights[2] * normalize(cost[i], cost) + weights[3] * normalize(fatigue[i], fatigue);
      if (score > maximum + EPS || Math.abs(score - maximum) <= EPS
          && stageZero.get(i).job < target.job) {
        maximum = score;
        target = stageZero.get(i);
      }
    }
    diagnostics.add("targetJob=" + target.job + ",weights=" + Arrays.toString(weights));
    List<PredictedMove> moves = new ArrayList<>();
    int machines = request.getInstance().getMachineCount(request.getFactory(), 0);
    int[] workers = request.getInstance().getEligibleWorkers(request.getFactory(), 0);
    for (int machine = 0; machine < machines; machine++) {
      for (int worker : workers) {
        if (machine == target.machine && worker == target.worker) continue;
        Prediction prediction = predict(request, target, machine, worker);
        moves.add(new PredictedMove(target.job, machine, worker, 0.0, prediction));
      }
    }
    if (moves.isEmpty()) return none(diagnostics, "NO_ALTERNATIVE_MACHINE_WORKER_PAIR");
    normalizePredictedScores(moves,
        new double[] {0.0, weights[1], weights[2], weights[3], weights[0]});
    moves.sort(Comparator.comparingDouble((PredictedMove value) -> value.score)
        .thenComparingInt(value -> value.machine).thenComparingInt(value -> value.worker));
    List<PermutationSolution<Integer>> candidates = new ArrayList<>();
    for (PredictedMove move : moves) {
      if (candidates.size() == ZhangBoNeighborhoodConfiguration.O12_CAP) break;
      PermutationSolution<Integer> candidate = ZhangBoNeighborhoodVectors.copy(request.getSource());
      int position = ZhangBoNeighborhoodVectors.positionOfJob(candidate, move.job);
      ZhangBoNeighborhoodVectors.machine(candidate, position, move.machine);
      candidate.setVariableValueworker(position, move.worker);
      candidates.add(candidate);
      diagnostics.add("predictedPair=" + move.machine + "," + move.worker
          + ",AT=" + number(move.prediction.actualDuration)
          + ",EC=" + number(move.prediction.energy) + ",WC=" + number(move.prediction.cost)
          + ",dF=" + number(move.prediction.deltaFatigue));
    }
    return candidates;
  }

  private static List<PermutationSolution<Integer>> recoveryWindow(
      ZhangBoNeighborhoodRequest request, List<String> diagnostics,
      ZhangBoNeighborhoodEvaluationGateway gateway) {
    ZhangBoFatigueEvaluationResult parentEvaluation = fatigueResult(request.getSource());
    List<ZhangBoFatigueOperationRecord> highBlock = largestHighBlock(request, parentEvaluation);
    if (highBlock.isEmpty()) return none(diagnostics, "NO_HIGH_LOAD_BLOCK");
    ZhangBoFatigueOperationRecord target = maximumContribution(
        request, highBlock, request.getFatigueFocus());
    double parentRecovery = workerRecovery(parentEvaluation, target.factory, target.worker);
    diagnostics.add("focus=" + request.getFatigueFocus() + ",targetJob=" + target.job
        + ",targetWorker=" + target.worker);
    Map<String, PermutationSolution<Integer>> raw = new LinkedHashMap<>();
    int position = ZhangBoNeighborhoodVectors.positionOfJob(request.getSource(), target.job);
    if (position > 0) {
      PermutationSolution<Integer> candidate = ZhangBoNeighborhoodVectors.copy(request.getSource());
      ZhangBoNeighborhoodVectors.insertBundle(candidate, position, position - 1);
      raw.put(ZhangBoNeighborhoodVectors.fingerprint(candidate), candidate);
    }
    if (position + 1 < request.getSource().getNumberOfVariables()) {
      PermutationSolution<Integer> candidate = ZhangBoNeighborhoodVectors.copy(request.getSource());
      ZhangBoNeighborhoodVectors.insertBundle(candidate, position, position + 1);
      raw.put(ZhangBoNeighborhoodVectors.fingerprint(candidate), candidate);
    }
    int[] workers = request.getInstance().getEligibleWorkers(target.factory, 0);
    int workerMoves = 0;
    for (int worker : workers) {
      if (worker == target.worker) continue;
      PermutationSolution<Integer> candidate = ZhangBoNeighborhoodVectors.copy(request.getSource());
      int targetPosition = ZhangBoNeighborhoodVectors.positionOfJob(candidate, target.job);
      candidate.setVariableValueworker(targetPosition, worker);
      raw.put(ZhangBoNeighborhoodVectors.fingerprint(candidate), candidate);
      if (++workerMoves == 2) break;
    }
    List<RankedRecovery> accepted = new ArrayList<>();
    int attempted = 0;
    for (PermutationSolution<Integer> candidate : raw.values()) {
      if (attempted++ == ZhangBoNeighborhoodConfiguration.O13_CAP) break;
      ZhangBoNeighborhoodVectors.validateFirstStage(candidate, request.getInstance());
      gateway.evaluate(candidate);
      ZhangBoFatigueEvaluationResult evaluation = fatigueResult(candidate);
      double gain = workerRecovery(evaluation, target.factory, target.worker) - parentRecovery;
      if (gain > EPS) {
        double focus = request.getFatigueFocus() == ZhangBoFatigueFocus.FMAX
            ? evaluation.getMetrics().maximumFatigue : evaluation.getMetrics().fatigueExcessIntegral;
        accepted.add(new RankedRecovery(candidate, gain, focus));
        diagnostics.add("recoveryGain=" + number(gain) + ",candidate="
            + ZhangBoNeighborhoodVectors.fingerprint(candidate));
      }
    }
    if (accepted.isEmpty()) return none(diagnostics, "NO_RECOVERY_GAIN");
    List<PermutationSolution<Integer>> acceptedSolutions = new ArrayList<>();
    for (RankedRecovery value : accepted) acceptedSolutions.add(value.solution);
    final double[][] directionBounds = bounds(acceptedSolutions);
    accepted.sort((left, right) -> {
      int comparison = Double.compare(right.gain, left.gain);
      if (comparison == 0) comparison = Double.compare(left.focus, right.focus);
      if (comparison == 0) {
        comparison = compareDirection(
            left.solution, right.solution, request.getSubSwarm(), directionBounds);
      }
      if (comparison == 0) comparison = ZhangBoNeighborhoodVectors.fingerprint(left.solution)
          .compareTo(ZhangBoNeighborhoodVectors.fingerprint(right.solution));
      return comparison;
    });
    List<PermutationSolution<Integer>> result = new ArrayList<>();
    for (RankedRecovery value : accepted) result.add(value.solution);
    return result;
  }

  private static List<PermutationSolution<Integer>> rawRecoveryWindow(
      ZhangBoNeighborhoodRequest request, List<String> diagnostics) {
    ZhangBoFatigueEvaluationResult parentEvaluation = fatigueResult(request.getSource());
    List<ZhangBoFatigueOperationRecord> highBlock = largestHighBlock(request, parentEvaluation);
    if (highBlock.isEmpty()) return none(diagnostics, "NO_HIGH_LOAD_BLOCK");
    ZhangBoFatigueOperationRecord target = maximumContribution(
        request, highBlock, request.getFatigueFocus());
    diagnostics.add("previewFocus=" + request.getFatigueFocus() + ",targetJob=" + target.job
        + ",targetWorker=" + target.worker);
    Map<String, PermutationSolution<Integer>> raw = new LinkedHashMap<>();
    int position = ZhangBoNeighborhoodVectors.positionOfJob(request.getSource(), target.job);
    if (position > 0) {
      PermutationSolution<Integer> candidate = ZhangBoNeighborhoodVectors.copy(request.getSource());
      ZhangBoNeighborhoodVectors.insertBundle(candidate, position, position - 1);
      raw.put(ZhangBoNeighborhoodVectors.fingerprint(candidate), candidate);
    }
    if (position + 1 < request.getSource().getNumberOfVariables()) {
      PermutationSolution<Integer> candidate = ZhangBoNeighborhoodVectors.copy(request.getSource());
      ZhangBoNeighborhoodVectors.insertBundle(candidate, position, position + 1);
      raw.put(ZhangBoNeighborhoodVectors.fingerprint(candidate), candidate);
    }
    int[] workers = request.getInstance().getEligibleWorkers(target.factory, 0);
    int workerMoves = 0;
    for (int worker : workers) {
      if (worker == target.worker) continue;
      PermutationSolution<Integer> candidate = ZhangBoNeighborhoodVectors.copy(request.getSource());
      int targetPosition = ZhangBoNeighborhoodVectors.positionOfJob(candidate, target.job);
      candidate.setVariableValueworker(targetPosition, worker);
      raw.put(ZhangBoNeighborhoodVectors.fingerprint(candidate), candidate);
      if (++workerMoves == 2) break;
    }
    return new ArrayList<>(raw.values());
  }

  private static Prediction predict(
      ZhangBoNeighborhoodRequest request, ZhangBoFatigueOperationRecord target,
      int machine, int worker) {
    ZhangBoFatigueInstanceData instance = request.getInstance();
    ZhangBoFatigueParameters parameters = request.getFatigueParameters();
    double workerAvailable = 0.0;
    double fatigueBefore = 0.0;
    int previousStage = target.stage;
    double machineAvailable = 0.0;
    for (ZhangBoFatigueOperationRecord operation : fatigueResult(request.getSource()).getOperations()) {
      if (operation.sequence >= target.sequence || operation.factory != target.factory) continue;
      if (operation.stage == target.stage && operation.machine == machine
          && operation.end > machineAvailable) machineAvailable = operation.end;
      if (operation.worker == worker && operation.end >= workerAvailable) {
        workerAvailable = operation.end;
        fatigueBefore = operation.fatigueAfter;
        previousStage = operation.stage;
      }
    }
    double start = Math.max(target.predecessorCompletion, Math.max(machineAvailable, workerAvailable));
    double recovered = fatigueBefore;
    if (workerAvailable > 0.0) {
      recovered = ZhangBoFatigueModel.recover(fatigueBefore,
          parameters.getMu(target.factory, worker, previousStage), start - workerAvailable);
    }
    double efficiency = instance.getWorkerEfficiency(target.factory, worker);
    double baseProcessing = instance.getStandardTime(target.job, target.stage)
        / (instance.getMachineSpeed(target.factory, target.stage, machine) * efficiency);
    double baseSetup = instance.hasStandardSetupTimes()
        ? instance.getStandardSetupTime(target.job, target.stage) / efficiency : 0.0;
    double multiplier = ZhangBoFatigueModel.durationMultiplier(
        recovered, parameters.getMaximumIncrease(target.stage));
    double actual = (baseProcessing + baseSetup) * multiplier;
    double after = ZhangBoFatigueModel.accumulate(
        recovered, parameters.getLambda(target.factory, worker, target.stage), actual);
    double energy = actual * instance.getMachinePower(target.factory, target.stage, machine);
    double cost = actual * instance.getWorkerCost(target.factory, worker);
    return new Prediction(baseSetup, actual, after - recovered, energy, cost);
  }

  private static List<ZhangBoFatigueOperationRecord> largestHighBlock(
      ZhangBoNeighborhoodRequest request, ZhangBoFatigueEvaluationResult evaluation) {
    Map<Integer, List<ZhangBoFatigueOperationRecord>> byWorker = new HashMap<>();
    for (ZhangBoFatigueOperationRecord operation : evaluation.getOperations()) {
      if (operation.factory == request.getFactory()) {
        byWorker.computeIfAbsent(operation.worker,
            ignored -> new ArrayList<ZhangBoFatigueOperationRecord>()).add(operation);
      }
    }
    List<ZhangBoFatigueOperationRecord> best = Collections.emptyList();
    double bestContribution = Double.NEGATIVE_INFINITY;
    for (List<ZhangBoFatigueOperationRecord> values : byWorker.values()) {
      values.sort(Comparator.comparingDouble((ZhangBoFatigueOperationRecord value) -> value.start)
          .thenComparingInt(value -> value.sequence));
      List<ZhangBoFatigueOperationRecord> current = new ArrayList<>();
      for (ZhangBoFatigueOperationRecord value : values) {
        boolean contiguous = current.isEmpty()
            || Math.abs(current.get(current.size() - 1).end - value.start) <= EPS;
        if (!contiguous) current.clear();
        current.add(value);
        if (current.size() >= 2 && maximumFatigue(current)
            >= request.getFatigueParameters().getWarningThreshold()) {
          double contribution = focusContribution(request, current, request.getFatigueFocus());
          if (contribution > bestContribution) {
            bestContribution = contribution;
            best = new ArrayList<>(current);
          }
        }
      }
    }
    return best;
  }

  private static ZhangBoFatigueOperationRecord maximumContribution(
      ZhangBoNeighborhoodRequest request, List<ZhangBoFatigueOperationRecord> block,
      ZhangBoFatigueFocus focus) {
    ZhangBoFatigueOperationRecord best = block.get(0);
    for (ZhangBoFatigueOperationRecord value : block) {
      double contribution = operationContribution(request, value, focus);
      double current = operationContribution(request, best, focus);
      if (contribution > current + EPS || Math.abs(contribution - current) <= EPS
          && value.job < best.job) best = value;
    }
    return best;
  }

  private static double focusContribution(
      ZhangBoNeighborhoodRequest request, List<ZhangBoFatigueOperationRecord> block,
      ZhangBoFatigueFocus focus) {
    double value = 0.0;
    for (ZhangBoFatigueOperationRecord operation : block) {
      if (focus == ZhangBoFatigueFocus.FMAX) value = Math.max(value, operation.fatigueAfter);
      else value += operationContribution(request, operation, focus);
    }
    return value;
  }

  private static double operationContribution(
      ZhangBoNeighborhoodRequest request, ZhangBoFatigueOperationRecord operation,
      ZhangBoFatigueFocus focus) {
    if (focus == ZhangBoFatigueFocus.FMAX) return operation.fatigueAfter;
    return ZhangBoFatigueModel.excessIntegralDuringWork(
        operation.fatigueAtStart,
        request.getFatigueParameters().getLambda(operation.factory, operation.worker, operation.stage),
        operation.actualDuration, request.getFatigueParameters().getWarningThreshold());
  }

  private static double workerRecovery(
      ZhangBoFatigueEvaluationResult evaluation, int factory, int worker) {
    double result = 0.0;
    for (ZhangBoFatigueOperationRecord operation : evaluation.getOperations()) {
      if (operation.factory == factory && operation.worker == worker) result += operation.recoveryDuration;
    }
    return result;
  }

  private static PermutationSolution<Integer> bestByDirection(
      List<PermutationSolution<Integer>> candidates, ZhangBoSubSwarm group) {
    List<PermutationSolution<Integer>> sorted = new ArrayList<>(candidates);
    if (ZhangBoSubSwarmSemantics.isBoundary(group)) {
      int objective = objectiveIndex(sorted.get(0),
          ZhangBoSubSwarmSemantics.objectiveIndex(group));
      sorted.sort(Comparator.comparingDouble((PermutationSolution<Integer> value) -> value.getObjective(objective))
          .thenComparing(ZhangBoNeighborhoodVectors::fingerprint));
      return sorted.get(0);
    }
    double[][] bounds = bounds(sorted);
    sorted.sort(Comparator.comparingDouble((PermutationSolution<Integer> value) -> center(value, bounds))
        .thenComparing(ZhangBoNeighborhoodVectors::fingerprint));
    return sorted.get(0);
  }

  private static int compareDirection(PermutationSolution<Integer> left,
      PermutationSolution<Integer> right, ZhangBoSubSwarm group, double[][] bounds) {
    if (ZhangBoSubSwarmSemantics.isBoundary(group)) {
      int objective = objectiveIndex(left, ZhangBoSubSwarmSemantics.objectiveIndex(group));
      return Double.compare(left.getObjective(objective), right.getObjective(objective));
    }
    return Double.compare(center(left, bounds), center(right, bounds));
  }

  private static double[][] bounds(List<PermutationSolution<Integer>> values) {
    double[][] result = {{Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY},
        {Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY},
        {Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY}};
    for (PermutationSolution<Integer> value : values) {
      int[] objectives = {0, 1, objectiveIndex(value, 6)};
      for (int i = 0; i < objectives.length; i++) {
        result[i][0] = Math.min(result[i][0], value.getObjective(objectives[i]));
        result[i][1] = Math.max(result[i][1], value.getObjective(objectives[i]));
      }
    }
    return result;
  }

  private static double center(PermutationSolution<Integer> value, double[][] bounds) {
    int[] objectives = {0, 1, objectiveIndex(value, 6)};
    double maximum = 0.0;
    for (int i = 0; i < objectives.length; i++) {
      maximum = Math.max(maximum, (value.getObjective(objectives[i]) - bounds[i][0])
          / Math.max(bounds[i][1] - bounds[i][0], EPS));
    }
    return maximum;
  }

  private static int objectiveIndex(PermutationSolution<Integer> solution, int productionIndex) {
    if (productionIndex != 6) return productionIndex;
    return solution.getNumberOfObjectives() > 6 ? 6 : 2;
  }

  private static List<Integer> factoryPositions(ZhangBoNeighborhoodRequest request) {
    List<Integer> result = new ArrayList<>();
    for (int i = 0; i < request.getSource().getNumberOfVariables(); i++) {
      if (request.getSource().getVariableValueid(i) == request.getFactory()) result.add(i);
    }
    return result;
  }

  private static List<ZhangBoFatigueOperationRecord> factoryStageZero(
      ZhangBoFatigueEvaluationResult evaluation, int factory) {
    List<ZhangBoFatigueOperationRecord> result = new ArrayList<>();
    for (ZhangBoFatigueOperationRecord operation : evaluation.getOperations()) {
      if (operation.factory == factory && operation.stage == 0) result.add(operation);
    }
    result.sort(Comparator.comparingInt(value -> value.sequence));
    return result;
  }

  private static double[] values(List<ZhangBoFatigueOperationRecord> values, int type) {
    double[] result = new double[values.size()];
    for (int i = 0; i < values.size(); i++) {
      ZhangBoFatigueOperationRecord value = values.get(i);
      if (type == 0) result[i] = value.baseSetupDuration;
      else if (type == 1) result[i] = value.deltaFatigue();
      else if (type == 2) result[i] = value.actualDuration;
      else if (type == 3) result[i] = value.energy;
      else result[i] = value.cost;
    }
    return result;
  }

  private static double normalize(double value, double[] values) {
    double minimum = Double.POSITIVE_INFINITY;
    double maximum = Double.NEGATIVE_INFINITY;
    for (double candidate : values) { minimum = Math.min(minimum, candidate); maximum = Math.max(maximum, candidate); }
    return (value - minimum) / Math.max(maximum - minimum, EPS);
  }

  /** Component order: setup, energy, cost, fatigue, actual duration. */
  private static void normalizePredictedScores(List<PredictedMove> moves, double[] weights) {
    double[][] components = new double[5][moves.size()];
    for (int i = 0; i < moves.size(); i++) {
      Prediction value = moves.get(i).prediction;
      components[0][i] = value.baseSetup;
      components[1][i] = value.energy;
      components[2][i] = value.cost;
      components[3][i] = value.deltaFatigue;
      components[4][i] = value.actualDuration;
    }
    for (int i = 0; i < moves.size(); i++) {
      double score = 0.0;
      for (int component = 0; component < components.length; component++) {
        score += weights[component] * normalize(components[component][i], components[component]);
      }
      moves.get(i).score = score;
    }
  }

  private static double[] directionWeights(ZhangBoSubSwarm group) {
    return ZhangBoSubSwarmSemantics.neighborhoodPredictionWeights(group);
  }

  private static int[] firstDifferentPair(List<Integer> positions, List<Integer> values) {
    for (int i = 0; i < positions.size(); i++) {
      for (int j = i + 1; j < positions.size(); j++) {
        if (!values.get(positions.get(i)).equals(values.get(positions.get(j)))) {
          return new int[] {positions.get(i), positions.get(j)};
        }
      }
    }
    return null;
  }

  private static List<Integer> range(int size) {
    List<Integer> result = new ArrayList<>();
    for (int i = 0; i < size; i++) result.add(i);
    return result;
  }

  private static String jobs(List<ZhangBoFatigueOperationRecord> operations) {
    Set<Integer> jobs = new LinkedHashSet<>();
    for (ZhangBoFatigueOperationRecord operation : operations) jobs.add(operation.job);
    return jobs.toString();
  }

  private static double maximumFatigue(List<ZhangBoFatigueOperationRecord> operations) {
    double value = 0.0;
    for (ZhangBoFatigueOperationRecord operation : operations) value = Math.max(value, operation.fatigueAfter);
    return value;
  }

  private static String logicalLayers(ZhangBoNeighborhoodId id) {
    switch (id) {
      case O1_JS_INSERT: case O2_JS_REVERSE: case O3_JS_SWAP: case O10_CRITICAL_BLOCK:
        return "JS";
      case O4_WA_LOAD_TRANSFER: case O5_WA_WEAK_TO_STRONG: case O6_WA_SWAP:
      case O11_FATIGUE_WORKER_REASSIGNMENT: return "WA";
      case O7_MA_LOAD_TRANSFER: case O8_MA_WEAK_TO_STRONG: case O9_MA_SWAP: return "MA";
      case O12_JOINT_MACHINE_WORKER: return "MA+WA";
      case O13_NATURAL_RECOVERY_WINDOW: return "JS|WA";
      default: return "";
    }
  }

  private static ZhangBoFatigueEvaluationResult fatigueResult(PermutationSolution<Integer> solution) {
    Object value = solution.getAttribute(ZhangBoFatigueEvaluationResult.class);
    return value instanceof ZhangBoFatigueEvaluationResult ? (ZhangBoFatigueEvaluationResult) value : null;
  }

  private static List<PermutationSolution<Integer>> one(PermutationSolution<Integer> value) {
    return new ArrayList<>(Collections.singletonList(value));
  }

  private static List<PermutationSolution<Integer>> none(List<String> diagnostics, String reason) {
    diagnostics.add(reason);
    return new ArrayList<>();
  }

  private static String number(double value) {
    return String.format(Locale.ROOT, "%.12g", value);
  }

  private static final class Prediction {
    final double baseSetup;
    final double actualDuration;
    final double deltaFatigue;
    final double energy;
    final double cost;

    Prediction(double baseSetup, double actualDuration, double deltaFatigue,
        double energy, double cost) {
      this.baseSetup = baseSetup;
      this.actualDuration = actualDuration;
      this.deltaFatigue = deltaFatigue;
      this.energy = energy;
      this.cost = cost;
    }
  }

  private static final class PredictedMove {
    final int job;
    final int machine;
    final int worker;
    double score;
    final Prediction prediction;

    PredictedMove(int job, int machine, int worker, double score, Prediction prediction) {
      this.job = job;
      this.machine = machine;
      this.worker = worker;
      this.score = score;
      this.prediction = prediction;
    }
  }

  private static final class RankedRecovery {
    final PermutationSolution<Integer> solution;
    final double gain;
    final double focus;

    RankedRecovery(PermutationSolution<Integer> solution, double gain, double focus) {
      this.solution = solution;
      this.gain = gain;
      this.focus = focus;
    }
  }
}
