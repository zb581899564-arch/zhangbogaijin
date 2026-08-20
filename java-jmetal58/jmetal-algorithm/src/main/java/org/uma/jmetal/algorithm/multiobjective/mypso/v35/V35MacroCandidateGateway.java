package org.uma.jmetal.algorithm.multiobjective.mypso.v35;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import org.uma.jmetal.algorithm.multiobjective.mypso.zhangbo.neighborhood.ZhangBoCriticalDagAnalyzer;
import org.uma.jmetal.problem.multiobjective.dfsp.fatigue.ZhangBoFatigueEvaluationResult;
import org.uma.jmetal.problem.multiobjective.dfsp.fatigue.ZhangBoFatigueInstanceData;
import org.uma.jmetal.problem.multiobjective.dfsp.fatigue.ZhangBoFatigueOperationRecord;
import org.uma.jmetal.problem.multiobjective.dfsp.model.DhhfspFourVectorSolution;
import org.uma.jmetal.solution.PermutationSolution;

/**
 * Independent v3.5 macro-action constructor.  It deliberately does not call
 * the historical O10-O13 suite or any shift/release-override code.
 *
 * <p>V35-FC-1: N3/N4 candidate selection is FM3-consistent.  When the caller
 * supplies the parent's {@link ZhangBoFatigueEvaluationResult} (the actual
 * fatigue-adjusted schedule trace), the critical structure comes from the
 * real zero-slack DAG (reusing {@link ZhangBoCriticalDagAnalyzer}) and the
 * resource bottleneck from the actual durations/fatigue of the trace.  The
 * gateway only ever reads the trace - it never edits start times.  A null or
 * empty trace falls back to the historical PT0 standard-time proxy, so the
 * legacy signatures keep their exact behaviour.</p>
 */
public final class V35MacroCandidateGateway implements Serializable {
  private static final long serialVersionUID = 1L;
  /** Zero-slack tolerance for the FM3 critical DAG, aligned with O10. */
  private static final double CRITICAL_TOLERANCE = 1.0e-9;

  public interface NanoClock { long nanoTime(); }
  public interface CompleteEvaluator { void evaluate(PermutationSolution<Integer> solution); }

  public static final class Prepared implements Serializable {
    private static final long serialVersionUID = 1L;
    /** Candidate selection read the FM3 actual (fatigue-adjusted) trace. */
    public static final String SOURCE_FM3_ACTUAL = "FM3_ACTUAL";
    /** Candidate selection fell back to the historical PT0 standard proxy. */
    public static final String SOURCE_PT0_PROXY = "PT0_PROXY";
    private final V35MacroNeighborhood action;
    private final PermutationSolution<Integer> parent;
    private final PermutationSolution<Integer> candidate;
    private final boolean applicable;
    private final String reason;
    private final String route;
    private final String structureSource;
    private final long workUnits;
    private final long elapsedNanos;
    private Prepared(V35MacroNeighborhood action, PermutationSolution<Integer> parent,
        PermutationSolution<Integer> candidate, boolean applicable, String reason,
        String route, String structureSource, long elapsedNanos, long workUnits) {
      this.action = action; this.parent = parent; this.candidate = candidate;
      this.applicable = applicable; this.reason = reason; this.route = route;
      this.structureSource = structureSource;
      this.elapsedNanos = elapsedNanos;
      this.workUnits = workUnits;
    }
    public V35MacroNeighborhood getAction() { return action; }
    public PermutationSolution<Integer> getParent() { return parent; }
    public PermutationSolution<Integer> getCandidate() { return candidate; }
    public boolean isApplicable() { return applicable; }
    public String getReason() { return reason; }
    public String getRoute() { return route; }
    public String getStructureSource() { return structureSource; }
    public long getWorkUnits() { return workUnits; }
  }

  public static final class Attempt implements Serializable {
    private static final long serialVersionUID = 1L;
    private final Prepared prepared;
    private final int completeEvaluations;
    private final long elapsedNanos;
    public Attempt(Prepared prepared, int completeEvaluations, long elapsedNanos) {
      this.prepared = prepared; this.completeEvaluations = completeEvaluations;
      this.elapsedNanos = elapsedNanos;
    }
    public Prepared getPrepared() { return prepared; }
    public PermutationSolution<Integer> getCandidate() { return prepared.getCandidate(); }
    public boolean isApplicable() { return prepared.isApplicable(); }
    public int getCompleteEvaluations() { return completeEvaluations; }
    public long getElapsedNanos() { return elapsedNanos; }
    public long getWorkUnits() { return prepared.getWorkUnits(); }
  }

  private final NanoClock clock;

  public V35MacroCandidateGateway() {
    this(new NanoClock() { @Override public long nanoTime() { return System.nanoTime(); } });
  }

  public V35MacroCandidateGateway(NanoClock clock) {
    if (clock == null) throw new IllegalArgumentException("clock");
    this.clock = clock;
  }

  public Prepared prepare(V35MacroNeighborhood action, PermutationSolution<Integer> parent,
      ZhangBoFatigueInstanceData instance, int targetFactory) {
    return prepare(action, parent, instance, targetFactory, V35Bottleneck.BAL);
  }

  public Prepared prepare(V35MacroNeighborhood action, PermutationSolution<Integer> parent,
      ZhangBoFatigueInstanceData instance, int targetFactory, V35Bottleneck bottleneck) {
    return prepareWithEvaluation(action, parent, instance, targetFactory, bottleneck, null);
  }

  /**
   * V35-FC-1 FM3-consistent preparation.  {@code evaluation} is the parent's
   * actual FM3 trace (read-only); a null/empty trace degrades to the PT0
   * proxy with identical legacy behaviour.
   */
  public Prepared prepareWithEvaluation(V35MacroNeighborhood action,
      PermutationSolution<Integer> parent, ZhangBoFatigueInstanceData instance,
      int targetFactory, V35Bottleneck bottleneck, ZhangBoFatigueEvaluationResult evaluation) {
    if (action == null || parent == null || instance == null) {
      throw new IllegalArgumentException("macro action inputs cannot be null");
    }
    long started = clock.nanoTime();
    if (!(parent instanceof DhhfspFourVectorSolution)) {
      return prepared(action, parent, null, false, "NON_CANONICAL_SOLUTION", "NONE",
          Prepared.SOURCE_PT0_PROXY, started);
    }
    DhhfspFourVectorSolution source = (DhhfspFourVectorSolution) parent;
    if (source.getNumberOfVariables() != instance.getJobs()) {
      return prepared(action, parent, null, false, "JOB_DIMENSION_MISMATCH", "NONE",
          Prepared.SOURCE_PT0_PROXY, started);
    }
    DhhfspFourVectorSolution candidate = (DhhfspFourVectorSolution) source.copy();
    List<ZhangBoFatigueOperationRecord> factoryTrace = factoryTrace(evaluation, targetFactory);
    boolean changed;
    String structureSource = Prepared.SOURCE_PT0_PROXY;
    switch (action) {
      case N1: changed = reorder(candidate, instance); break;
      case N2: changed = reallocateFactory(candidate, instance, targetFactory); break;
      case N3:
        changed = structuralRelocationFm3(candidate, instance, bottleneck, factoryTrace);
        if (factoryTrace != null && !factoryTrace.isEmpty()) {
          structureSource = Prepared.SOURCE_FM3_ACTUAL;
        }
        break;
      case N4:
        changed = resourceReallocationFm3(candidate, instance, bottleneck, factoryTrace);
        if (factoryTrace != null && !factoryTrace.isEmpty()) {
          structureSource = Prepared.SOURCE_FM3_ACTUAL;
        }
        break;
      case N5: changed = mixed(candidate, instance, targetFactory); break;
      default: changed = false;
    }
    if (!changed) return prepared(action, parent, null, false, "NO_APPLICABLE_ACTION",
        route(action, bottleneck), structureSource, started);
    return prepared(action, parent, candidate, true, "OK", route(action, bottleneck),
        structureSource, started);
  }

  public Attempt evaluateOne(Prepared prepared, CompleteEvaluator evaluator) {
    if (prepared == null || evaluator == null) throw new IllegalArgumentException("prepared/evaluator");
    if (!prepared.isApplicable()) return new Attempt(prepared, 0, prepared.elapsedNanos);
    long started = clock.nanoTime();
    evaluator.evaluate(prepared.getCandidate());
    return new Attempt(prepared, 1, saturatedAdd(prepared.elapsedNanos,
        Math.max(0L, clock.nanoTime() - started)));
  }

  private Prepared prepared(V35MacroNeighborhood action, PermutationSolution<Integer> parent,
      PermutationSolution<Integer> candidate, boolean applicable, String reason,
      String route, String structureSource, long started) {
    return new Prepared(action, parent, candidate, applicable, reason,
        route, structureSource,
        Math.max(0L, clock.nanoTime() - started),
        parent == null ? 0L : (long) parent.getNumberOfVariables() * (action.ordinal() + 1L));
  }

  private static long saturatedAdd(long left, long right) {
    return Long.MAX_VALUE - left < right ? Long.MAX_VALUE : left + right;
  }

  private static boolean reorder(DhhfspFourVectorSolution s, ZhangBoFatigueInstanceData data) {
    List<Integer> js = s.getJobSequence();
    for (int i = 0; i + 1 < js.size(); i++) {
      if (s.getFactoryAssignments().get(i).equals(s.getFactoryAssignments().get(i + 1))) {
        swapPackage(s, i, i + 1);
        return true;
      }
    }
    return false;
  }

  private static boolean recoveryWindow(DhhfspFourVectorSolution s, ZhangBoFatigueInstanceData data) {
    List<Integer> js = s.getJobSequence();
    for (int i = 0; i + 2 < js.size(); i++) {
      if (s.getFactoryAssignments().get(i).equals(s.getFactoryAssignments().get(i + 2))
          && !s.getWorkerAssignments().get(i).equals(s.getWorkerAssignments().get(i + 2))) {
        swapPackage(s, i + 1, i + 2);
        return true;
      }
    }
    return reorder(s, data);
  }

  /**
   * N3: deterministic structural relocation.  SET is routed by setup-edge
   * pressure; SEQ/BAL use a processing-critical proxy.  The resource package
   * is moved with the job, so this is a JS-only action by job identity.
   */
  private static boolean structuralRelocation(DhhfspFourVectorSolution s,
      ZhangBoFatigueInstanceData data, V35Bottleneck bottleneck) {
    int selected = -1;
    double largestPressure = Double.NEGATIVE_INFINITY;
    for (int p = 0; p < s.getJobSequence().size(); p++) {
      int job = s.getJobSequence().get(p);
      double setupPressure = data.hasStandardSetupTimes()
          ? data.getStandardSetupTime(job, 0) : 0.0;
      double processingPressure = data.getStandardTime(job, 0);
      double pressure = bottleneck == V35Bottleneck.SET
          ? setupPressure : processingPressure + setupPressure;
      if (pressure > largestPressure) {
        largestPressure = pressure;
        selected = p;
      }
    }
    if (selected <= 0) return false;
    int factory = s.getFactoryAssignments().get(selected);
    for (int target = 0; target < selected; target++) {
      if (s.getFactoryAssignments().get(target) == factory) {
        movePackage(s, selected, target);
        return true;
      }
    }
    return false;
  }

  /**
   * V35-FC-1 N3 with the FM3 actual trace: the "critical" job is picked from
   * the real zero-slack DAG (SEQ/BAL) or from the actual setup durations
   * (SET).  Only the trace is read; the genotype move keeps the legacy
   * same-factory package relocation semantics.  An unusable trace falls back
   * to {@link #structuralRelocation}.
   */
  private static boolean structuralRelocationFm3(DhhfspFourVectorSolution s,
      ZhangBoFatigueInstanceData data, V35Bottleneck bottleneck,
      List<ZhangBoFatigueOperationRecord> factoryTrace) {
    if (factoryTrace == null || factoryTrace.isEmpty()) {
      return structuralRelocation(s, data, bottleneck);
    }
    int job;
    if (bottleneck == V35Bottleneck.SET) {
      ZhangBoFatigueOperationRecord best = null;
      for (ZhangBoFatigueOperationRecord operation : factoryTrace) {
        if (operation.stage != 0) continue;
        if (best == null || operation.actualSetupDuration > best.actualSetupDuration
            || (operation.actualSetupDuration == best.actualSetupDuration
                && operation.sequence < best.sequence)) {
          best = operation;
        }
      }
      if (best == null) return structuralRelocation(s, data, bottleneck);
      job = best.job;
    } else {
      ZhangBoCriticalDagAnalyzer.Analysis analysis = ZhangBoCriticalDagAnalyzer.analyze(
          factoryTrace, factoryTrace.get(0).factory, CRITICAL_TOLERANCE);
      ZhangBoFatigueOperationRecord best = null;
      for (ZhangBoFatigueOperationRecord operation : analysis.critical) {
        if (operation.stage != 0) continue;
        if (best == null || operation.actualDuration > best.actualDuration
            || (operation.actualDuration == best.actualDuration
                && operation.sequence < best.sequence)) {
          best = operation;
        }
      }
      if (best == null) return structuralRelocation(s, data, bottleneck);
      job = best.job;
    }
    int selected = s.getJobSequence().indexOf(job);
    if (selected <= 0) return false;
    int factory = s.getFactoryAssignments().get(selected);
    for (int target = 0; target < selected; target++) {
      if (s.getFactoryAssignments().get(target) == factory) {
        movePackage(s, selected, target);
        return true;
      }
    }
    return false;
  }

  /** N4: bottleneck-routed resource reassignment, first stage only. */
  private static boolean resourceReallocation(DhhfspFourVectorSolution s,
      ZhangBoFatigueInstanceData data, V35Bottleneck bottleneck) {
    int selected = selectedResourcePosition(s, data, bottleneck);
    if (selected < 0) return false;
    return reassignFrom(s, data, bottleneck, selected);
  }

  /**
   * V35-FC-1 N4 with the FM3 actual trace: the bottleneck position is picked
   * from actual durations and fatigue values of the trace (machine load /
   * worker load / fatigue risk / actual setup) instead of the PT0 proxy.
   * The genotype edit keeps the legacy resource reassignment semantics.
   */
  private static boolean resourceReallocationFm3(DhhfspFourVectorSolution s,
      ZhangBoFatigueInstanceData data, V35Bottleneck bottleneck,
      List<ZhangBoFatigueOperationRecord> factoryTrace) {
    if (factoryTrace == null || factoryTrace.isEmpty()) {
      return resourceReallocation(s, data, bottleneck);
    }
    int selected = -1;
    double best = Double.NEGATIVE_INFINITY;
    int bestSequence = Integer.MAX_VALUE;
    for (int p = 0; p < s.getJobSequence().size(); p++) {
      int job = s.getJobSequence().get(p);
      ZhangBoFatigueOperationRecord record = firstStageOfJob(factoryTrace, job);
      if (record == null) continue;
      double pressure;
      switch (bottleneck) {
        case WOR: pressure = record.actualDuration + record.fatigueAtStart; break;
        case FAT: pressure = record.fatigueAfter; break;
        case MAC: pressure = record.actualDuration; break;
        case SET: pressure = record.actualSetupDuration; break;
        default: pressure = record.actualDuration + record.actualSetupDuration;
      }
      if (pressure > best || (pressure == best && record.sequence < bestSequence)) {
        best = pressure;
        bestSequence = record.sequence;
        selected = p;
      }
    }
    if (selected < 0) return false;
    return reassignFrom(s, data, bottleneck, selected);
  }

  /** The legacy N4 genotype edit, isolated so the PT0 and FM3 routes share
   *  byte-identical reassignment behaviour. */
  private static boolean reassignFrom(DhhfspFourVectorSolution s,
      ZhangBoFatigueInstanceData data, V35Bottleneck bottleneck, int selected) {
    for (int p = selected; p < s.getJobSequence().size(); p++) {
      int factory = s.getFactoryAssignments().get(p);
      int currentMachine = s.getMachineAssignments().get(p);
      int currentWorker = s.getWorkerAssignments().get(p);
      int[] workers = data.getEligibleWorkers(factory, 0);
      if (bottleneck == V35Bottleneck.WOR && workers.length > 1) {
        s.getWorkerAssignments().set(p, workers[0] == currentWorker ? workers[1] : workers[0]);
        return true;
      }
      if (bottleneck == V35Bottleneck.FAT && workers.length > 1) {
        // A deterministic alternative worker is the only legal proxy before
        // a complete fatigue replay; the evaluator decides whether it wins.
        s.getWorkerAssignments().set(p, workers[0] == currentWorker ? workers[1] : workers[0]);
        return true;
      }
      int machineCount = data.getMachineCount(factory, 0);
      if (machineCount > 1 || workers.length > 1) {
        int nextMachine = machineCount > 1 ? (currentMachine + 1) % machineCount : currentMachine;
        int nextWorker = workers.length > 1
            ? (workers[0] == currentWorker ? workers[1] : workers[0]) : currentWorker;
        if (nextMachine != currentMachine || nextWorker != currentWorker) {
          s.getMachineAssignments().set(p, nextMachine);
          s.getWorkerAssignments().set(p, nextWorker);
          return true;
        }
      }
    }
    return false;
  }

  /** V35-FC-1: the parent's FM3 trace restricted to {@code factory}, all
   *  stages (the critical DAG needs cross-stage job edges).  Null-safe. */
  private static List<ZhangBoFatigueOperationRecord> factoryTrace(
      ZhangBoFatigueEvaluationResult evaluation, int factory) {
    if (evaluation == null || evaluation.getOperations() == null) return null;
    List<ZhangBoFatigueOperationRecord> trace = new ArrayList<>();
    for (ZhangBoFatigueOperationRecord operation : evaluation.getOperations()) {
      if (operation.factory == factory) trace.add(operation);
    }
    return trace;
  }

  private static ZhangBoFatigueOperationRecord firstStageOfJob(
      List<ZhangBoFatigueOperationRecord> trace, int job) {
    ZhangBoFatigueOperationRecord found = null;
    for (ZhangBoFatigueOperationRecord operation : trace) {
      if (operation.job != job || operation.stage != 0) continue;
      if (found == null || operation.sequence < found.sequence) found = operation;
    }
    return found;
  }

  private static int selectedResourcePosition(DhhfspFourVectorSolution s,
      ZhangBoFatigueInstanceData data, V35Bottleneck bottleneck) {
    int selected = -1;
    double best = Double.NEGATIVE_INFINITY;
    for (int p = 0; p < s.getJobSequence().size(); p++) {
      int job = s.getJobSequence().get(p);
      double setup = data.hasStandardSetupTimes() ? data.getStandardSetupTime(job, 0) : 0.0;
      double pressure;
      switch (bottleneck) {
        case SET: pressure = setup; break;
        case FAT: pressure = data.getStandardTime(job, 0) + setup; break;
        default: pressure = data.getStandardTime(job, 0) + setup;
      }
      if (pressure > best) { best = pressure; selected = p; }
    }
    return selected;
  }

  private static boolean reallocateFactory(DhhfspFourVectorSolution s,
      ZhangBoFatigueInstanceData data, int targetFactory) {
    int destination = targetFactory < 0 ? 0 : Math.floorMod(targetFactory, data.getFactories());
    for (int p = 0; p < s.getJobSequence().size(); p++) {
      if (s.getFactoryAssignments().get(p) != destination) {
        s.getFactoryAssignments().set(p, destination);
        s.getMachineAssignments().set(p, 0);
        int[] eligible = data.getEligibleWorkers(destination, 0);
        s.getWorkerAssignments().set(p, eligible[0]);
        return true;
      }
    }
    return false;
  }

  private static boolean jointResource(DhhfspFourVectorSolution s, ZhangBoFatigueInstanceData data) {
    for (int p = 0; p < s.getJobSequence().size(); p++) {
      int f = s.getFactoryAssignments().get(p);
      int m = s.getMachineAssignments().get(p);
      int w = s.getWorkerAssignments().get(p);
      int machines = data.getMachineCount(f, 0);
      int[] workers = data.getEligibleWorkers(f, 0);
      if (machines > 1 || workers.length > 1) {
        s.getMachineAssignments().set(p, (m + 1) % machines);
        s.getWorkerAssignments().set(p, workers[0] == w && workers.length > 1 ? workers[1] : workers[0]);
        return true;
      }
    }
    return false;
  }

  private static boolean mixed(DhhfspFourVectorSolution s, ZhangBoFatigueInstanceData data,
      int targetFactory) {
    // N5 is intentionally a two-part action: a structural JS move followed
    // by exactly one legal first-stage resource move.  It never edits times.
    if (!recoveryWindow(s, data)) return false;
    return jointResource(s, data) || reallocateFactory(s, data, targetFactory);
  }

  private static String route(V35MacroNeighborhood action, V35Bottleneck bottleneck) {
    if (action == V35MacroNeighborhood.N3) {
      return bottleneck == V35Bottleneck.SET ? "SETUP_EDGE_SOURCE" : "CRITICAL_SOURCE";
    }
    if (action == V35MacroNeighborhood.N4) return bottleneck.name() + "_RESOURCE_ROUTE";
    if (action == V35MacroNeighborhood.N5) return "STRUCTURAL_RECOVERY_MIX";
    return action.name();
  }

  private static void swapPackage(DhhfspFourVectorSolution s, int a, int b) {
    swap(s.getJobSequence(), a, b);
    swap(s.getFactoryAssignments(), a, b);
    swap(s.getMachineAssignments(), a, b);
    swap(s.getWorkerAssignments(), a, b);
  }

  private static void movePackage(DhhfspFourVectorSolution s, int from, int to) {
    if (from == to) return;
    move(s.getJobSequence(), from, to);
    move(s.getFactoryAssignments(), from, to);
    move(s.getMachineAssignments(), from, to);
    move(s.getWorkerAssignments(), from, to);
  }

  private static <T> void move(List<T> list, int from, int to) {
    T value = list.remove(from);
    list.add(to, value);
  }

  private static <T> void swap(List<T> list, int a, int b) {
    T temp = list.get(a); list.set(a, list.get(b)); list.set(b, temp);
  }
}
