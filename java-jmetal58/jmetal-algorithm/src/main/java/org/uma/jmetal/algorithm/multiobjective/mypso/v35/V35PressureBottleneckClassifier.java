package org.uma.jmetal.algorithm.multiobjective.mypso.v35;

import java.io.Serializable;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.uma.jmetal.problem.multiobjective.dfsp.fatigue.ZhangBoFatigueEvaluationResult;
import org.uma.jmetal.problem.multiobjective.dfsp.fatigue.ZhangBoFatigueInstanceData;
import org.uma.jmetal.problem.multiobjective.dfsp.fatigue.ZhangBoFatigueOperationRecord;
import org.uma.jmetal.problem.multiobjective.dfsp.fatigue.ZhangBoFatigueParameters;

/** Factory-local, deterministic pressure diagnosis for the v3.5 CA-TA-Lite line. */
public final class V35PressureBottleneckClassifier implements Serializable {
  private static final long serialVersionUID = 1L;
  private static final double EPSILON = 1.0e-12;
  private static final double CRITICAL_EPSILON = 1.0e-9;

  public static final class Classification implements Serializable {
    private static final long serialVersionUID = 1L;
    private final int factory;
    private final V35Bottleneck bottleneck;
    private final Map<V35Bottleneck, Double> pressures;
    private final V35Bottleneck maximumType;
    private final V35Bottleneck secondType;
    private final double maximum;
    private final double second;
    private final boolean confident;
    private final String reason;
    private final int criticalOperations;

    private Classification(int factory, V35Bottleneck bottleneck,
        Map<V35Bottleneck, Double> pressures, V35Bottleneck maximumType,
        V35Bottleneck secondType, double maximum, double second,
        boolean confident, String reason, int criticalOperations) {
      this.factory = factory;
      this.bottleneck = bottleneck;
      this.pressures = Collections.unmodifiableMap(new EnumMap<>(pressures));
      this.maximumType = maximumType;
      this.secondType = secondType;
      this.maximum = maximum;
      this.second = second;
      this.confident = confident;
      this.reason = reason;
      this.criticalOperations = criticalOperations;
    }

    public int getFactory() { return factory; }
    public V35Bottleneck getBottleneck() { return bottleneck; }
    public double getPressure(V35Bottleneck value) {
      Double result = pressures.get(value);
      return result == null ? 0.0 : result;
    }
    public Map<V35Bottleneck, Double> getPressures() { return pressures; }
    public V35Bottleneck getMaximumType() { return maximumType; }
    public V35Bottleneck getSecondType() { return secondType; }
    public double getMaximumPressure() { return maximum; }
    public double getSecondPressure() { return second; }
    public double getGap() { return maximum - second; }
    public boolean isConfident() { return confident; }
    public String getReason() { return reason; }
    public int getCriticalOperations() { return criticalOperations; }

    public String toCsv(long generation, long evaluations, int parentSlot,
        V35SubSwarmRole role) {
      return generation + "," + evaluations + "," + parentSlot + "," + factory + ","
          + role + "," + getPressure(V35Bottleneck.SEQ) + ","
          + getPressure(V35Bottleneck.MAC) + "," + getPressure(V35Bottleneck.WOR) + ","
          + getPressure(V35Bottleneck.SET) + "," + getPressure(V35Bottleneck.FAT) + ","
          + maximumType + "," + secondType + "," + maximum + "," + second + ","
          + getGap() + "," + confident + "," + bottleneck + "," + reason + ","
          + criticalOperations;
    }
  }

  public Classification classify(ZhangBoFatigueEvaluationResult evaluation,
      ZhangBoFatigueInstanceData instance, ZhangBoFatigueParameters parameters,
      int factory, V35BottleneckDiagnosisConfiguration configuration) {
    if (evaluation == null || instance == null || parameters == null || configuration == null) {
      throw new IllegalArgumentException("pressure diagnosis dependencies cannot be null");
    }
    if (factory < 0 || factory >= instance.getFactories()) {
      return fallback(factory, "INVALID_FACTORY");
    }
    List<Node> nodes = nodes(evaluation.getOperations(), factory);
    if (nodes.isEmpty()) return fallback(factory, "NO_ACTIVE_OPERATIONS");
    try {
      Graph graph = graph(nodes);
      List<Node> order = graph.topologicalOrder();
      if (order.size() != nodes.size()) return fallback(factory, "DAG_CYCLE");
      Set<Node> critical = critical(graph, order);
      Map<V35Bottleneck, Double> pressure = pressures(
          nodes, critical, instance, parameters, factory);
      return decide(factory, pressure, configuration, critical.size());
    } catch (RuntimeException exception) {
      return fallback(factory, "PRESSURE_ERROR_" + exception.getClass().getSimpleName());
    }
  }

  private static Classification decide(int factory, Map<V35Bottleneck, Double> pressure,
      V35BottleneckDiagnosisConfiguration configuration, int criticalOperations) {
    List<V35Bottleneck> ranked = new ArrayList<>();
    ranked.add(V35Bottleneck.SEQ);
    ranked.add(V35Bottleneck.MAC);
    ranked.add(V35Bottleneck.WOR);
    ranked.add(V35Bottleneck.SET);
    ranked.add(V35Bottleneck.FAT);
    Collections.sort(ranked, new Comparator<V35Bottleneck>() {
      @Override public int compare(V35Bottleneck left, V35Bottleneck right) {
        int value = Double.compare(pressure.get(right), pressure.get(left));
        return value != 0 ? value : Integer.compare(left.ordinal(), right.ordinal());
      }
    });
    V35Bottleneck maximumType = ranked.get(0);
    V35Bottleneck secondType = ranked.get(1);
    double maximum = pressure.get(maximumType);
    double second = pressure.get(secondType);
    boolean thresholdMode = configuration.getMode()
        == V35BottleneckDiagnosisConfiguration.Mode.CONFIDENCE;
    boolean absolute = maximum + EPSILON >= configuration.getTauAbs();
    boolean separated = maximum - second + EPSILON >= configuration.getTauGap();
    boolean confident = thresholdMode && absolute && separated;
    String reason;
    if (!thresholdMode) reason = "FULL_MASK_AUDIT";
    else if (!absolute) reason = "BELOW_ABSOLUTE_THRESHOLD";
    else if (!separated) reason = "INSUFFICIENT_PRESSURE_GAP";
    else reason = "HIGH_CONFIDENCE";
    return new Classification(factory, confident ? maximumType : V35Bottleneck.BAL,
        pressure, maximumType, secondType, maximum, second, confident, reason,
        criticalOperations);
  }

  private static Map<V35Bottleneck, Double> pressures(List<Node> nodes, Set<Node> critical,
      ZhangBoFatigueInstanceData instance, ZhangBoFatigueParameters parameters, int factory) {
    double cmax = 0.0;
    double total = 0.0;
    double setup = 0.0;
    double criticalTotal = 0.0;
    double criticalSetup = 0.0;
    double inflation = 0.0;
    double highExposure = 0.0;
    Map<String, Double> machineLoads = new HashMap<>();
    Map<Integer, Double> workerLoads = new HashMap<>();
    Map<String, Double> criticalMachineLoads = new HashMap<>();
    for (int stage = 0; stage < instance.getStages(); stage++) {
      for (int machine = 0; machine < instance.getMachineCount(factory, stage); machine++) {
        machineLoads.put(machineKey(stage, machine), 0.0);
        criticalMachineLoads.put(machineKey(stage, machine), 0.0);
      }
    }
    for (int worker = 0; worker < instance.getWorkerCount(factory); worker++) {
      workerLoads.put(worker, 0.0);
    }
    double maximumIncrease = 0.0;
    for (int stage = 0; stage < parameters.getStages(); stage++) {
      maximumIncrease = Math.max(maximumIncrease, parameters.getMaximumIncrease(stage));
    }
    for (Node node : nodes) {
      ZhangBoFatigueOperationRecord operation = node.operation;
      double duration = nonnegative(operation.actualDuration);
      cmax = Math.max(cmax, operation.end);
      total += duration;
      setup += nonnegative(operation.actualSetupDuration);
      inflation += Math.max(0.0, duration - nonnegative(operation.baseDuration));
      String machine = machineKey(operation.stage, operation.machine);
      machineLoads.put(machine, value(machineLoads, machine) + duration);
      workerLoads.put(operation.worker, value(workerLoads, operation.worker) + duration);
      if (critical.contains(node)) {
        criticalTotal += duration;
        criticalSetup += nonnegative(operation.actualSetupDuration);
        criticalMachineLoads.put(machine, value(criticalMachineLoads, machine) + duration);
      }
      highExposure += highFatigueDuration(operation, parameters);
    }
    double seriality = clamp(cmax / (total + EPSILON));
    double criticalBlock = clamp(longestCriticalBlock(nodes, critical) / (cmax + EPSILON));
    double machineImbalance = imbalance(machineLoads.values());
    double criticalMachine = clamp(maximum(criticalMachineLoads.values())
        / (criticalTotal + EPSILON));
    double workerImbalance = imbalance(workerLoads.values());
    double workerUtilization = clamp(maximum(workerLoads.values()) / (cmax + EPSILON));
    double setupShare = clamp(setup / (total + EPSILON));
    double criticalSetupShare = clamp(criticalSetup / (criticalTotal + EPSILON));
    double maximumInflationFraction = maximumIncrease / (1.0 + maximumIncrease + EPSILON);
    double inflationShare = inflation / (total + EPSILON);
    double normalizedInflation = maximumInflationFraction <= EPSILON
        ? 0.0 : clamp(inflationShare / maximumInflationFraction);
    double exposure = clamp(highExposure / (total + EPSILON));

    Map<V35Bottleneck, Double> result = new EnumMap<>(V35Bottleneck.class);
    result.put(V35Bottleneck.SEQ, clamp(0.5 * seriality + 0.5 * criticalBlock));
    result.put(V35Bottleneck.MAC, clamp(0.5 * machineImbalance + 0.5 * criticalMachine));
    result.put(V35Bottleneck.WOR, clamp(0.5 * workerImbalance + 0.5 * workerUtilization));
    result.put(V35Bottleneck.SET, clamp(0.5 * setupShare + 0.5 * criticalSetupShare));
    result.put(V35Bottleneck.FAT, clamp(0.5 * normalizedInflation + 0.5 * exposure));
    result.put(V35Bottleneck.BAL, 0.0);
    for (double value : result.values()) {
      if (!Double.isFinite(value)) throw new IllegalStateException("non-finite pressure");
    }
    return result;
  }

  private static double highFatigueDuration(ZhangBoFatigueOperationRecord operation,
      ZhangBoFatigueParameters parameters) {
    double duration = nonnegative(operation.actualDuration);
    double warning = parameters.getWarningThreshold();
    double start = clampFatigue(operation.fatigueAtStart);
    if (duration <= EPSILON || operation.factory < 0 || operation.stage < 0
        || operation.worker < 0 || operation.factory >= parameters.getFactories()
        || operation.stage >= parameters.getStages()
        || operation.worker >= parameters.getWorkers(operation.factory, operation.stage)) {
      return 0.0;
    }
    if (start >= warning) return duration;
    double lambda = parameters.getLambda(operation.factory, operation.worker, operation.stage);
    if (lambda <= EPSILON) return 0.0;
    double ratio = (1.0 - warning) / Math.max(EPSILON, 1.0 - start);
    if (ratio <= 0.0 || ratio >= 1.0) return 0.0;
    double crossing = -Math.log(ratio) / lambda;
    return clamp(duration - crossing, 0.0, duration);
  }

  private static Set<Node> critical(Graph graph, List<Node> order) {
    double cmax = 0.0;
    for (Node node : order) cmax = Math.max(cmax, node.operation.end);
    Map<Node, Double> latestStart = new HashMap<>();
    for (int index = order.size() - 1; index >= 0; index--) {
      Node node = order.get(index);
      double latestFinish = cmax;
      List<Node> successors = graph.successors.get(node);
      if (successors != null && !successors.isEmpty()) {
        latestFinish = Double.POSITIVE_INFINITY;
        for (Node successor : successors) {
          latestFinish = Math.min(latestFinish, latestStart.get(successor));
        }
      }
      latestStart.put(node, latestFinish - nonnegative(node.operation.actualDuration));
    }
    Set<Node> result = new HashSet<>();
    for (Node node : order) {
      if (latestStart.get(node) - node.operation.start <= CRITICAL_EPSILON) result.add(node);
    }
    return result;
  }

  private static Graph graph(List<Node> nodes) {
    Graph graph = new Graph(nodes);
    Map<Integer, List<Node>> jobs = new HashMap<>();
    Map<String, List<Node>> machines = new HashMap<>();
    Map<Integer, List<Node>> workers = new HashMap<>();
    for (Node node : nodes) {
      add(jobs, node.operation.job, node);
      add(machines, machineKey(node.operation.stage, node.operation.machine), node);
      add(workers, node.operation.worker, node);
    }
    Comparator<Node> byStage = Comparator.comparingInt((Node n) -> n.operation.stage)
        .thenComparingInt(n -> n.operation.sequence);
    Comparator<Node> byTime = Comparator.comparingDouble((Node n) -> n.operation.start)
        .thenComparingDouble(n -> n.operation.end)
        .thenComparingInt(n -> n.operation.job)
        .thenComparingInt(n -> n.operation.stage);
    for (List<Node> list : jobs.values()) addChain(graph, list, byStage);
    for (List<Node> list : machines.values()) addChain(graph, list, byTime);
    for (List<Node> list : workers.values()) addChain(graph, list, byTime);
    return graph;
  }

  private static double longestCriticalBlock(List<Node> nodes, Set<Node> critical) {
    Map<String, List<Node>> resources = new HashMap<>();
    for (Node node : nodes) {
      add(resources, "M:" + machineKey(node.operation.stage, node.operation.machine), node);
      add(resources, "W:" + node.operation.worker, node);
    }
    Comparator<Node> byTime = Comparator.comparingDouble((Node n) -> n.operation.start)
        .thenComparingDouble(n -> n.operation.end)
        .thenComparingInt(n -> n.operation.job)
        .thenComparingInt(n -> n.operation.stage);
    double longest = 0.0;
    for (List<Node> list : resources.values()) {
      Collections.sort(list, byTime);
      double current = 0.0;
      Node previous = null;
      for (Node node : list) {
        if (!critical.contains(node)) {
          current = 0.0;
          previous = null;
          continue;
        }
        if (previous == null
            || node.operation.start > previous.operation.end + CRITICAL_EPSILON) {
          current = nonnegative(node.operation.actualDuration);
        } else {
          current += nonnegative(node.operation.actualDuration);
        }
        longest = Math.max(longest, current);
        previous = node;
      }
    }
    return longest;
  }

  private static List<Node> nodes(List<ZhangBoFatigueOperationRecord> operations, int factory) {
    List<Node> result = new ArrayList<>();
    Set<String> identities = new HashSet<>();
    for (ZhangBoFatigueOperationRecord operation : operations) {
      if (operation.factory != factory) continue;
      String identity = operation.job + ":" + operation.stage;
      if (!identities.add(identity)) throw new IllegalArgumentException("duplicate operation");
      result.add(new Node(operation));
    }
    return result;
  }

  private static <K> void add(Map<K, List<Node>> values, K key, Node node) {
    List<Node> list = values.get(key);
    if (list == null) { list = new ArrayList<>(); values.put(key, list); }
    list.add(node);
  }

  private static void addChain(Graph graph, List<Node> list, Comparator<Node> comparator) {
    Collections.sort(list, comparator);
    for (int index = 1; index < list.size(); index++) graph.edge(list.get(index - 1), list.get(index));
  }

  private static String machineKey(int stage, int machine) { return stage + ":" + machine; }
  private static double value(Map<?, Double> values, Object key) {
    Double result = values.get(key);
    return result == null ? 0.0 : result;
  }
  private static double maximum(Iterable<Double> values) {
    double result = 0.0;
    for (double value : values) result = Math.max(result, value);
    return result;
  }
  private static double imbalance(Iterable<Double> values) {
    double maximum = 0.0;
    double sum = 0.0;
    int count = 0;
    for (double value : values) { maximum = Math.max(maximum, value); sum += value; count++; }
    if (count == 0 || maximum <= EPSILON) return 0.0;
    return clamp((maximum - sum / count) / (maximum + EPSILON));
  }
  private static double nonnegative(double value) {
    if (!Double.isFinite(value)) throw new IllegalArgumentException("non-finite trace value");
    return Math.max(0.0, value);
  }
  private static double clampFatigue(double value) { return clamp(value, 0.0, 1.0); }
  private static double clamp(double value) { return clamp(value, 0.0, 1.0); }
  private static double clamp(double value, double lower, double upper) {
    if (!Double.isFinite(value)) throw new IllegalArgumentException("non-finite value");
    return Math.max(lower, Math.min(upper, value));
  }

  private static Classification fallback(int factory, String reason) {
    Map<V35Bottleneck, Double> pressure = new EnumMap<>(V35Bottleneck.class);
    for (V35Bottleneck value : V35Bottleneck.values()) pressure.put(value, 0.0);
    return new Classification(factory, V35Bottleneck.BAL, pressure, V35Bottleneck.SEQ,
        V35Bottleneck.MAC, 0.0, 0.0, false, reason, 0);
  }

  private static final class Node {
    final ZhangBoFatigueOperationRecord operation;
    Node(ZhangBoFatigueOperationRecord operation) { this.operation = operation; }
  }

  private static final class Graph {
    final List<Node> nodes;
    final Map<Node, List<Node>> successors = new HashMap<>();
    final Map<Node, Integer> indegree = new HashMap<>();
    Graph(List<Node> nodes) {
      this.nodes = nodes;
      for (Node node : nodes) { successors.put(node, new ArrayList<Node>()); indegree.put(node, 0); }
    }
    void edge(Node from, Node to) {
      if (from == to || successors.get(from).contains(to)) return;
      successors.get(from).add(to);
      indegree.put(to, indegree.get(to) + 1);
    }
    List<Node> topologicalOrder() {
      Map<Node, Integer> remaining = new HashMap<>(indegree);
      ArrayDeque<Node> queue = new ArrayDeque<>();
      List<Node> stable = new ArrayList<>(nodes);
      Collections.sort(stable, Comparator.comparingInt(n -> n.operation.sequence));
      for (Node node : stable) if (remaining.get(node) == 0) queue.add(node);
      List<Node> result = new ArrayList<>();
      while (!queue.isEmpty()) {
        Node node = queue.removeFirst();
        result.add(node);
        List<Node> next = new ArrayList<>(successors.get(node));
        Collections.sort(next, Comparator.comparingInt(n -> n.operation.sequence));
        for (Node successor : next) {
          int value = remaining.get(successor) - 1;
          remaining.put(successor, value);
          if (value == 0) queue.addLast(successor);
        }
      }
      return result;
    }
  }
}
