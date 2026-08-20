package org.uma.jmetal.problem.multiobjective.dfsp.fatigue;

import java.util.Arrays;

/** Immutable author-instance snapshot used only by the Zhang Bo fatigue path. */
public final class ZhangBoFatigueInstanceData {
  private final String instanceSha256;
  private final int jobs;
  private final int stages;
  private final int factories;
  private final int[][] machineCounts;
  private final double[][][] machineSpeeds;
  private final int[][][] machinePowers;
  private final int[][] standardTimes;
  private final int[][] standardSetupTimes;
  private final String instanceExtensionSha256;
  private final int[] workerCounts;
  private final double[][] workerEfficiencies;
  private final int[][] workerCosts;
  private final int[][][] eligibleWorkers;

  public ZhangBoFatigueInstanceData(
      String instanceSha256,
      int jobs,
      int stages,
      int factories,
      int[][] machineCounts,
      double[][][] machineSpeeds,
      int[][][] machinePowers,
      int[][] standardTimes,
      int[] workerCounts,
      double[][] workerEfficiencies,
      int[][] workerCosts) {
    this(instanceSha256, jobs, stages, factories, machineCounts, machineSpeeds,
        machinePowers, standardTimes, workerCounts, workerEfficiencies, workerCosts, null);
  }

  public ZhangBoFatigueInstanceData(
      String instanceSha256,
      int jobs,
      int stages,
      int factories,
      int[][] machineCounts,
      double[][][] machineSpeeds,
      int[][][] machinePowers,
      int[][] standardTimes,
      int[] workerCounts,
      double[][] workerEfficiencies,
      int[][] workerCosts,
      ZhangBoInstanceExtension extension) {
    if (instanceSha256 == null || !instanceSha256.matches("[0-9A-Fa-f]{64}")) {
      throw new IllegalArgumentException("Invalid instance SHA-256: " + instanceSha256);
    }
    if (jobs <= 0 || stages <= 0 || factories <= 0) {
      throw new IllegalArgumentException("Instance dimensions must be positive");
    }
    this.instanceSha256 = instanceSha256.toUpperCase();
    this.jobs = jobs;
    this.stages = stages;
    this.factories = factories;
    this.machineCounts = copy(machineCounts, factories, stages, "machineCounts");
    this.machineSpeeds = copy(machineSpeeds, this.machineCounts, "machineSpeeds");
    this.machinePowers = copy(machinePowers, this.machineCounts, "machinePowers");
    this.standardTimes = copy(standardTimes, jobs, stages, "standardTimes");
    if (extension == null) {
      this.standardSetupTimes = null;
      this.instanceExtensionSha256 = "";
    } else {
      if (!this.instanceSha256.equals(extension.getInstanceSha256())
          || extension.getJobs() != jobs || extension.getStages() != stages) {
        throw new IllegalArgumentException("Setup-time extension does not belong to the instance");
      }
      this.standardSetupTimes = extension.copyStandardSetupTimes();
      this.instanceExtensionSha256 = extension.getConfigurationSha256();
    }
    this.workerCounts = copy(workerCounts, factories, "workerCounts");
    this.workerEfficiencies = copy(workerEfficiencies, this.workerCounts, "workerEfficiencies");
    this.workerCosts = copy(workerCosts, this.workerCounts, "workerCosts");
    this.eligibleWorkers = buildEligibleWorkers();
  }

  private int[][][] buildEligibleWorkers() {
    int[][][] result = new int[factories][stages][];
    for (int factory = 0; factory < factories; factory++) {
      int offset = 0;
      for (int stage = 0; stage < stages; stage++) {
        int machines = machineCounts[factory][stage];
        int needed = machines <= 2 ? machines : machines - 1;
        if (needed <= 0 || offset + needed > workerCounts[factory]) {
          throw new IllegalArgumentException("Author worker-stage partition is invalid at factory="
              + factory + ", stage=" + stage + ", offset=" + offset + ", needed=" + needed
              + ", workerCount=" + workerCounts[factory]);
        }
        result[factory][stage] = new int[needed];
        for (int index = 0; index < needed; index++) {
          result[factory][stage][index] = offset + index;
        }
        offset += needed;
      }
      if (offset != workerCounts[factory]) {
        throw new IllegalArgumentException("Author worker-stage partition does not consume all workers at factory="
            + factory + ": consumed=" + offset + ", workerCount=" + workerCounts[factory]);
      }
    }
    return result;
  }

  public String getInstanceSha256() { return instanceSha256; }
  public int getJobs() { return jobs; }
  public int getStages() { return stages; }
  public int getFactories() { return factories; }
  public int getMachineCount(int factory, int stage) { return machineCounts[factory][stage]; }
  public double getMachineSpeed(int factory, int stage, int machine) {
    return machineSpeeds[factory][stage][machine];
  }
  public int getMachinePower(int factory, int stage, int machine) {
    return machinePowers[factory][stage][machine];
  }
  public int getStandardTime(int job, int stage) { return standardTimes[job][stage]; }
  public boolean hasStandardSetupTimes() { return standardSetupTimes != null; }
  public int getStandardSetupTime(int job, int stage) {
    if (standardSetupTimes == null) {
      throw new IllegalStateException("The instance has no fixed standard setup-time extension");
    }
    return standardSetupTimes[job][stage];
  }
  public String getInstanceExtensionSha256() { return instanceExtensionSha256; }
  public int getWorkerCount(int factory) { return workerCounts[factory]; }
  public double getWorkerEfficiency(int factory, int worker) {
    return workerEfficiencies[factory][worker];
  }
  public int getWorkerCost(int factory, int worker) { return workerCosts[factory][worker]; }
  public int[] getEligibleWorkers(int factory, int stage) {
    return eligibleWorkers[factory][stage].clone();
  }
  public boolean isWorkerEligible(int factory, int stage, int worker) {
    for (int candidate : eligibleWorkers[factory][stage]) {
      if (candidate == worker) return true;
    }
    return false;
  }
  public int[][] getMachineCounts() { return copy(machineCounts); }
  public int[] getWorkerCounts() { return workerCounts.clone(); }

  private static int[] copy(int[] source, int length, String name) {
    if (source == null || source.length != length) {
      throw new IllegalArgumentException(name + " length must be " + length);
    }
    int[] result = source.clone();
    for (int value : result) if (value <= 0) throw new IllegalArgumentException(name + " must be positive");
    return result;
  }

  private static int[][] copy(int[][] source, int rows, int columns, String name) {
    if (source == null || source.length != rows) throw new IllegalArgumentException(name + " rows must be " + rows);
    int[][] result = new int[rows][];
    for (int row = 0; row < rows; row++) {
      if (source[row] == null || source[row].length < columns) {
        throw new IllegalArgumentException(name + " columns must be at least " + columns + " at row " + row);
      }
      result[row] = Arrays.copyOf(source[row], columns);
      for (int value : result[row]) if (value <= 0) throw new IllegalArgumentException(name + " must be positive");
    }
    return result;
  }

  private static int[][] copy(int[][] source) {
    int[][] result = new int[source.length][];
    for (int i = 0; i < source.length; i++) result[i] = source[i].clone();
    return result;
  }

  private static double[][][] copy(double[][][] source, int[][] counts, String name) {
    if (source == null || source.length != counts.length) throw new IllegalArgumentException(name + " factory dimension mismatch");
    double[][][] result = new double[counts.length][][];
    for (int f = 0; f < counts.length; f++) {
      if (source[f] == null || source[f].length < counts[f].length) throw new IllegalArgumentException(name + " stage dimension mismatch");
      result[f] = new double[counts[f].length][];
      for (int k = 0; k < counts[f].length; k++) {
        if (source[f][k] == null || source[f][k].length < counts[f][k]) throw new IllegalArgumentException(name + " machine dimension mismatch");
        result[f][k] = Arrays.copyOf(source[f][k], counts[f][k]);
        for (double value : result[f][k]) if (!(value > 0.0) || !Double.isFinite(value)) throw new IllegalArgumentException(name + " must be positive and finite");
      }
    }
    return result;
  }

  private static int[][][] copy(int[][][] source, int[][] counts, String name) {
    if (source == null || source.length != counts.length) throw new IllegalArgumentException(name + " factory dimension mismatch");
    int[][][] result = new int[counts.length][][];
    for (int f = 0; f < counts.length; f++) {
      if (source[f] == null || source[f].length < counts[f].length) throw new IllegalArgumentException(name + " stage dimension mismatch");
      result[f] = new int[counts[f].length][];
      for (int k = 0; k < counts[f].length; k++) {
        if (source[f][k] == null || source[f][k].length < counts[f][k]) throw new IllegalArgumentException(name + " machine dimension mismatch");
        result[f][k] = Arrays.copyOf(source[f][k], counts[f][k]);
        for (int value : result[f][k]) if (value < 0) throw new IllegalArgumentException(name + " must be nonnegative");
      }
    }
    return result;
  }

  private static double[][] copy(double[][] source, int[] counts, String name) {
    if (source == null || source.length != counts.length) throw new IllegalArgumentException(name + " factory dimension mismatch");
    double[][] result = new double[counts.length][];
    for (int f = 0; f < counts.length; f++) {
      if (source[f] == null || source[f].length < counts[f]) throw new IllegalArgumentException(name + " worker dimension mismatch");
      result[f] = Arrays.copyOf(source[f], counts[f]);
      for (double value : result[f]) if (!(value > 0.0) || !Double.isFinite(value)) throw new IllegalArgumentException(name + " must be positive and finite");
    }
    return result;
  }

  private static int[][] copy(int[][] source, int[] counts, String name) {
    if (source == null || source.length != counts.length) throw new IllegalArgumentException(name + " factory dimension mismatch");
    int[][] result = new int[counts.length][];
    for (int f = 0; f < counts.length; f++) {
      if (source[f] == null || source[f].length < counts[f]) throw new IllegalArgumentException(name + " worker dimension mismatch");
      result[f] = Arrays.copyOf(source[f], counts[f]);
      for (int value : result[f]) if (value < 0) throw new IllegalArgumentException(name + " must be nonnegative");
    }
    return result;
  }
}
