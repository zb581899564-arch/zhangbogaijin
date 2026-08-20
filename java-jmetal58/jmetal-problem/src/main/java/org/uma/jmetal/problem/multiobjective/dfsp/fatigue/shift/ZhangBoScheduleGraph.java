package org.uma.jmetal.problem.multiobjective.dfsp.fatigue.shift;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Set;
import java.util.TreeMap;
import org.uma.jmetal.problem.multiobjective.dfsp.fatigue.ZhangBoFatigueEvaluationResult;
import org.uma.jmetal.problem.multiobjective.dfsp.fatigue.ZhangBoFatigueOperationRecord;

/** Immutable operation DAG plus machine and worker sequences. */
public final class ZhangBoScheduleGraph {
  private final int jobs;
  private final int stages;
  private final int[] factory;
  private final int[] machine;
  private final int[] worker;
  private final int[] stableRank;
  private final double[] releaseOverride;
  private final Map<String, List<Integer>> machineSequences;
  private final Map<String, List<Integer>> workerSequences;

  private ZhangBoScheduleGraph(
      int jobs, int stages, int[] factory, int[] machine, int[] worker,
      int[] stableRank, double[] releaseOverride,
      Map<String, List<Integer>> machineSequences,
      Map<String, List<Integer>> workerSequences) {
    this.jobs = jobs;
    this.stages = stages;
    this.factory = factory.clone();
    this.machine = machine.clone();
    this.worker = worker.clone();
    this.stableRank = stableRank.clone();
    this.releaseOverride = releaseOverride.clone();
    this.machineSequences = immutableCopy(machineSequences);
    this.workerSequences = immutableCopy(workerSequences);
  }

  public static ZhangBoScheduleGraph from(
      ZhangBoFatigueEvaluationResult result, int jobs, int stages) {
    int count = jobs * stages;
    int[] factory = filled(count, -1);
    int[] machine = filled(count, -1);
    int[] worker = filled(count, -1);
    int[] rank = filled(count, Integer.MAX_VALUE);
    final ZhangBoFatigueOperationRecord[] byOperation =
        new ZhangBoFatigueOperationRecord[count];
    for (ZhangBoFatigueOperationRecord record : result.getOperations()) {
      if (record.job < 0 || record.job >= jobs || record.stage < 0 || record.stage >= stages) {
        throw new IllegalArgumentException("Invalid operation identity in base schedule");
      }
      int operation = operation(record.job, record.stage, stages);
      if (byOperation[operation] != null) {
        throw new IllegalArgumentException("Invalid or duplicate operation in base schedule");
      }
      byOperation[operation] = record;
      factory[operation] = record.factory;
      machine[operation] = record.machine;
      worker[operation] = record.worker;
      rank[operation] = record.sequence;
    }
    for (int operation = 0; operation < count; operation++) {
      if (byOperation[operation] == null) {
        throw new IllegalArgumentException("Base schedule is missing operation=" + operation);
      }
    }
    Map<String, List<Integer>> machines = new TreeMap<>();
    Map<String, List<Integer>> workers = new TreeMap<>();
    for (int operation = 0; operation < count; operation++) {
      append(machines, machineKey(factory[operation], stage(operation, stages), machine[operation]), operation);
      append(workers, workerKey(factory[operation], worker[operation]), operation);
    }
    Comparator<Integer> byTime = new Comparator<Integer>() {
      @Override public int compare(Integer left, Integer right) {
        int value = Double.compare(byOperation[left].start, byOperation[right].start);
        if (value == 0) value = Integer.compare(byOperation[left].sequence, byOperation[right].sequence);
        return value != 0 ? value : Integer.compare(left, right);
      }
    };
    for (List<Integer> sequence : machines.values()) Collections.sort(sequence, byTime);
    for (List<Integer> sequence : workers.values()) Collections.sort(sequence, byTime);
    return new ZhangBoScheduleGraph(jobs, stages, factory, machine, worker, rank,
        new double[count], machines, workers);
  }

  public ZhangBoScheduleGraph moveEarlier(int operation, int machineSlot, int workerSlot) {
    Map<String, List<Integer>> machines = mutableCopy(machineSequences);
    Map<String, List<Integer>> workers = mutableCopy(workerSequences);
    List<Integer> machineSequence = machines.get(machineKey(operation));
    List<Integer> workerSequence = workers.get(workerKey(operation));
    int currentMachine = machineSequence.indexOf(operation);
    int currentWorker = workerSequence.indexOf(operation);
    if (currentMachine < 0 || currentWorker < 0
        || machineSlot < 0 || machineSlot > currentMachine
        || workerSlot < 0 || workerSlot > currentWorker
        || (machineSlot == currentMachine && workerSlot == currentWorker)) {
      throw new IllegalArgumentException(
          "At least one shift slot must be earlier and neither may be later");
    }
    machineSequence.remove(currentMachine);
    workerSequence.remove(currentWorker);
    machineSequence.add(machineSlot, operation);
    workerSequence.add(workerSlot, operation);
    return new ZhangBoScheduleGraph(jobs, stages, factory, machine, worker, stableRank,
        releaseOverride, machines, workers);
  }

  public ZhangBoScheduleGraph withRelease(int operation, double release) {
    if (operation < 0 || operation >= operationCount() || !Double.isFinite(release)
        || release < 0.0) {
      throw new IllegalArgumentException("Invalid release override");
    }
    double[] overrides = releaseOverride.clone();
    overrides[operation] = release;
    return new ZhangBoScheduleGraph(jobs, stages, factory, machine, worker, stableRank,
        overrides, machineSequences, workerSequences);
  }

  public int[] topologicalOrder() {
    @SuppressWarnings("unchecked")
    Set<Integer>[] successors = successors();
    int[] indegree = new int[operationCount()];
    for (Set<Integer> values : successors) for (int value : values) indegree[value]++;
    PriorityQueue<Integer> ready = new PriorityQueue<>(new Comparator<Integer>() {
      @Override public int compare(Integer left, Integer right) {
        int value = Integer.compare(stableRank[left], stableRank[right]);
        return value != 0 ? value : Integer.compare(left, right);
      }
    });
    for (int operation = 0; operation < indegree.length; operation++) {
      if (indegree[operation] == 0) ready.add(operation);
    }
    int[] order = new int[operationCount()];
    int index = 0;
    while (!ready.isEmpty()) {
      int operation = ready.remove();
      order[index++] = operation;
      for (int successor : successors[operation]) {
        if (--indegree[successor] == 0) ready.add(successor);
      }
    }
    if (index != operationCount()) throw new IllegalArgumentException("Schedule DAG contains a cycle");
    return order;
  }

  @SuppressWarnings("unchecked")
  public Set<Integer>[] successors() {
    Set<Integer>[] result = new Set[operationCount()];
    for (int i = 0; i < result.length; i++) result[i] = new LinkedHashSet<>();
    for (int job = 0; job < jobs; job++) {
      for (int stage = 0; stage + 1 < stages; stage++) {
        addEdge(result, operation(job, stage, stages), operation(job, stage + 1, stages));
      }
    }
    addSequenceEdges(result, machineSequences);
    addSequenceEdges(result, workerSequences);
    return result;
  }

  public int predecessorInMachine(int operation) { return predecessor(machineSequences.get(machineKey(operation)), operation); }
  public int predecessorInWorker(int operation) { return predecessor(workerSequences.get(workerKey(operation)), operation); }
  public int successorInMachine(int operation) { return successor(machineSequences.get(machineKey(operation)), operation); }
  public int successorInWorker(int operation) { return successor(workerSequences.get(workerKey(operation)), operation); }
  public int getFactory(int operation) { return factory[operation]; }
  public int getMachine(int operation) { return machine[operation]; }
  public int getWorker(int operation) { return worker[operation]; }
  public int getJob(int operation) { return operation / stages; }
  public int getStage(int operation) { return operation % stages; }
  public double getReleaseOverride(int operation) { return releaseOverride[operation]; }
  public int operationCount() { return jobs * stages; }
  public int getJobs() { return jobs; }
  public int getStages() { return stages; }
  public int machinePosition(int operation) { return machineSequences.get(machineKey(operation)).indexOf(operation); }
  public int workerPosition(int operation) { return workerSequences.get(workerKey(operation)).indexOf(operation); }
  public List<Integer> machineSequence(int operation) { return machineSequences.get(machineKey(operation)); }
  public List<Integer> workerSequence(int operation) { return workerSequences.get(workerKey(operation)); }

  public String fingerprint() {
    StringBuilder text = new StringBuilder();
    appendSequences(text, machineSequences);
    appendSequences(text, workerSequences);
    text.append("release=").append(Arrays.toString(releaseOverride));
    try {
      MessageDigest digest = MessageDigest.getInstance("SHA-256");
      byte[] hash = digest.digest(text.toString().getBytes(StandardCharsets.UTF_8));
      StringBuilder out = new StringBuilder();
      for (byte value : hash) out.append(String.format("%02x", value & 0xff));
      return out.toString();
    } catch (NoSuchAlgorithmException exception) {
      throw new IllegalStateException(exception);
    }
  }

  private String machineKey(int operation) {
    return machineKey(factory[operation], getStage(operation), machine[operation]);
  }
  private String workerKey(int operation) { return workerKey(factory[operation], worker[operation]); }
  private static String machineKey(int factory, int stage, int machine) { return "M:" + factory + ':' + stage + ':' + machine; }
  private static String workerKey(int factory, int worker) { return "W:" + factory + ':' + worker; }
  private static int operation(int job, int stage, int stages) { return job * stages + stage; }
  private static int stage(int operation, int stages) { return operation % stages; }

  private static int[] filled(int length, int value) {
    int[] result = new int[length];
    Arrays.fill(result, value);
    return result;
  }
  private static void append(Map<String, List<Integer>> map, String key, int operation) {
    List<Integer> values = map.get(key);
    if (values == null) { values = new ArrayList<>(); map.put(key, values); }
    values.add(operation);
  }
  private static void addSequenceEdges(Set<Integer>[] successors, Map<String, List<Integer>> sequences) {
    for (List<Integer> sequence : sequences.values()) {
      for (int index = 0; index + 1 < sequence.size(); index++) {
        addEdge(successors, sequence.get(index), sequence.get(index + 1));
      }
    }
  }
  private static void addEdge(Set<Integer>[] successors, int from, int to) {
    if (from != to) successors[from].add(to);
  }
  private static int predecessor(List<Integer> sequence, int operation) {
    int index = sequence.indexOf(operation);
    return index <= 0 ? -1 : sequence.get(index - 1);
  }
  private static int successor(List<Integer> sequence, int operation) {
    int index = sequence.indexOf(operation);
    return index < 0 || index + 1 >= sequence.size() ? -1 : sequence.get(index + 1);
  }
  private static Map<String, List<Integer>> mutableCopy(Map<String, List<Integer>> source) {
    Map<String, List<Integer>> result = new TreeMap<>();
    for (Map.Entry<String, List<Integer>> entry : source.entrySet()) {
      result.put(entry.getKey(), new ArrayList<>(entry.getValue()));
    }
    return result;
  }
  private static Map<String, List<Integer>> immutableCopy(Map<String, List<Integer>> source) {
    Map<String, List<Integer>> result = new TreeMap<>();
    for (Map.Entry<String, List<Integer>> entry : source.entrySet()) {
      result.put(entry.getKey(), Collections.unmodifiableList(new ArrayList<>(entry.getValue())));
    }
    return Collections.unmodifiableMap(result);
  }
  private static void appendSequences(StringBuilder out, Map<String, List<Integer>> sequences) {
    for (Map.Entry<String, List<Integer>> entry : sequences.entrySet()) {
      out.append(entry.getKey()).append('=').append(entry.getValue()).append(';');
    }
  }
}
