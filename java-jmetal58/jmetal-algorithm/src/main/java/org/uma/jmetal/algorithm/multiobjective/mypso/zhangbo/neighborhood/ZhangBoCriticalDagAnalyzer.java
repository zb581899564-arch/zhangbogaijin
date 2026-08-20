package org.uma.jmetal.algorithm.multiobjective.mypso.zhangbo.neighborhood;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.uma.jmetal.problem.multiobjective.dfsp.fatigue.ZhangBoFatigueOperationRecord;

/** Builds the job/machine/worker DAG and extracts zero-slack critical blocks. */
public final class ZhangBoCriticalDagAnalyzer {
  public static final class Analysis {
    public final List<ZhangBoFatigueOperationRecord> critical;
    public final List<List<ZhangBoFatigueOperationRecord>> blocks;

    Analysis(List<ZhangBoFatigueOperationRecord> critical,
        List<List<ZhangBoFatigueOperationRecord>> blocks) {
      this.critical = critical;
      this.blocks = blocks;
    }
  }

  private ZhangBoCriticalDagAnalyzer() { }

  public static Analysis analyze(
      List<ZhangBoFatigueOperationRecord> all, int factory, double tolerance) {
    List<ZhangBoFatigueOperationRecord> operations = new ArrayList<>();
    for (ZhangBoFatigueOperationRecord operation : all) {
      if (operation.factory == factory) operations.add(operation);
    }
    operations.sort(Comparator.comparingDouble((ZhangBoFatigueOperationRecord value) -> value.start)
        .thenComparingDouble(value -> value.end).thenComparingInt(value -> value.sequence));
    if (operations.isEmpty()) return new Analysis(Collections.emptyList(), Collections.emptyList());

    Map<Integer, List<Integer>> successors = new HashMap<>();
    for (ZhangBoFatigueOperationRecord operation : operations) {
      successors.put(operation.sequence, new ArrayList<Integer>());
    }
    addJobEdges(operations, successors);
    addResourceEdges(operations, successors, true);
    addResourceEdges(operations, successors, false);

    double makespan = 0.0;
    for (ZhangBoFatigueOperationRecord operation : operations) makespan = Math.max(makespan, operation.end);
    Map<Integer, Double> latestStart = new HashMap<>();
    List<ZhangBoFatigueOperationRecord> reverse = new ArrayList<>(operations);
    reverse.sort(Comparator.comparingDouble((ZhangBoFatigueOperationRecord value) -> value.start)
        .thenComparingInt(value -> value.sequence).reversed());
    Map<Integer, ZhangBoFatigueOperationRecord> bySequence = new HashMap<>();
    for (ZhangBoFatigueOperationRecord operation : operations) bySequence.put(operation.sequence, operation);
    for (ZhangBoFatigueOperationRecord operation : reverse) {
      double latestEnd = makespan;
      List<Integer> next = successors.get(operation.sequence);
      if (!next.isEmpty()) {
        latestEnd = Double.POSITIVE_INFINITY;
        for (Integer sequence : next) {
          Double start = latestStart.get(sequence);
          if (start != null) latestEnd = Math.min(latestEnd, start);
        }
        if (!Double.isFinite(latestEnd)) latestEnd = makespan;
      }
      latestStart.put(operation.sequence, latestEnd - operation.actualDuration);
    }
    List<ZhangBoFatigueOperationRecord> critical = new ArrayList<>();
    Set<Integer> criticalSequence = new HashSet<>();
    for (ZhangBoFatigueOperationRecord operation : operations) {
      if (Math.abs(latestStart.get(operation.sequence) - operation.start) <= tolerance) {
        critical.add(operation);
        criticalSequence.add(operation.sequence);
      }
    }
    List<List<ZhangBoFatigueOperationRecord>> blocks = new ArrayList<>();
    collectBlocks(operations, criticalSequence, true, blocks);
    collectBlocks(operations, criticalSequence, false, blocks);
    blocks.sort(Comparator.comparingInt((List<ZhangBoFatigueOperationRecord> block) -> block.get(0).sequence)
        .thenComparingInt(List::size));
    return new Analysis(Collections.unmodifiableList(critical), Collections.unmodifiableList(blocks));
  }

  private static void addJobEdges(
      List<ZhangBoFatigueOperationRecord> values, Map<Integer, List<Integer>> successors) {
    Map<Integer, List<ZhangBoFatigueOperationRecord>> jobs = new HashMap<>();
    for (ZhangBoFatigueOperationRecord value : values) {
      jobs.computeIfAbsent(value.job, key -> new ArrayList<ZhangBoFatigueOperationRecord>()).add(value);
    }
    for (List<ZhangBoFatigueOperationRecord> job : jobs.values()) {
      job.sort(Comparator.comparingInt(value -> value.stage));
      for (int i = 1; i < job.size(); i++) add(successors, job.get(i - 1), job.get(i));
    }
  }

  private static void addResourceEdges(
      List<ZhangBoFatigueOperationRecord> values, Map<Integer, List<Integer>> successors,
      boolean machine) {
    Map<String, List<ZhangBoFatigueOperationRecord>> groups = new HashMap<>();
    for (ZhangBoFatigueOperationRecord value : values) {
      String key = value.stage + ":" + (machine ? value.machine : value.worker);
      groups.computeIfAbsent(key, ignored -> new ArrayList<ZhangBoFatigueOperationRecord>()).add(value);
    }
    for (List<ZhangBoFatigueOperationRecord> group : groups.values()) {
      group.sort(Comparator.comparingDouble((ZhangBoFatigueOperationRecord value) -> value.start)
          .thenComparingInt(value -> value.sequence));
      for (int i = 1; i < group.size(); i++) add(successors, group.get(i - 1), group.get(i));
    }
  }

  private static void add(Map<Integer, List<Integer>> successors,
      ZhangBoFatigueOperationRecord before, ZhangBoFatigueOperationRecord after) {
    List<Integer> values = successors.get(before.sequence);
    if (!values.contains(after.sequence)) values.add(after.sequence);
  }

  private static void collectBlocks(
      List<ZhangBoFatigueOperationRecord> values, Set<Integer> critical,
      boolean machine, List<List<ZhangBoFatigueOperationRecord>> output) {
    Map<String, List<ZhangBoFatigueOperationRecord>> groups = new HashMap<>();
    for (ZhangBoFatigueOperationRecord value : values) {
      String key = value.stage + ":" + (machine ? value.machine : value.worker);
      groups.computeIfAbsent(key, ignored -> new ArrayList<ZhangBoFatigueOperationRecord>()).add(value);
    }
    for (List<ZhangBoFatigueOperationRecord> group : groups.values()) {
      group.sort(Comparator.comparingDouble((ZhangBoFatigueOperationRecord value) -> value.start)
          .thenComparingInt(value -> value.sequence));
      List<ZhangBoFatigueOperationRecord> current = new ArrayList<>();
      for (ZhangBoFatigueOperationRecord value : group) {
        if (critical.contains(value.sequence)) {
          current.add(value);
        } else {
          if (current.size() >= 2) output.add(new ArrayList<>(current));
          current.clear();
        }
      }
      if (current.size() >= 2) output.add(new ArrayList<>(current));
    }
  }
}
