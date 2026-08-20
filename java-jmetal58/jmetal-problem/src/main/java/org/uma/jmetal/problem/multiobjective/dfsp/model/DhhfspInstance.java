package org.uma.jmetal.problem.multiobjective.dfsp.model;

import java.io.Serializable;

/** Immutable data contract for a dual-resource distributed hybrid flow-shop instance. */
public final class DhhfspInstance implements Serializable {
  private static final long serialVersionUID = 1L;

  private final int numberOfJobs;
  private final int numberOfStages;
  private final int numberOfFactories;
  private final double[][] standardProcessingTimes;
  private final double[][] standardSetupTimes;
  private final double[][][] machineSpeeds;
  private final double[][][] machineEnergyPerUnit;
  private final double[][][] workerEfficiencies;
  private final double[][][] workerCostPerUnit;

  public DhhfspInstance(
      int numberOfJobs,
      int numberOfStages,
      int numberOfFactories,
      double[][] standardProcessingTimes,
      double[][] standardSetupTimes,
      double[][][] machineSpeeds,
      double[][][] machineEnergyPerUnit,
      double[][][] workerEfficiencies,
      double[][][] workerCostPerUnit) {
    if (numberOfJobs <= 0 || numberOfStages <= 0 || numberOfFactories <= 0) {
      throw new IllegalArgumentException("Instance dimensions must all be positive");
    }
    this.numberOfJobs = numberOfJobs;
    this.numberOfStages = numberOfStages;
    this.numberOfFactories = numberOfFactories;
    this.standardProcessingTimes = copyAndValidateStageJobMatrix(
        "standardProcessingTimes", standardProcessingTimes, numberOfStages, numberOfJobs);
    this.standardSetupTimes = copyAndValidateStageJobMatrix(
        "standardSetupTimes", standardSetupTimes, numberOfStages, numberOfJobs);
    this.machineSpeeds = copyAndValidateResourceTensor(
        "machineSpeeds", machineSpeeds, numberOfFactories, numberOfStages);
    this.machineEnergyPerUnit = copyAndValidateMatchingTensor(
        "machineEnergyPerUnit", machineEnergyPerUnit, this.machineSpeeds);
    this.workerEfficiencies = copyAndValidateResourceTensor(
        "workerEfficiencies", workerEfficiencies, numberOfFactories, numberOfStages);
    this.workerCostPerUnit = copyAndValidateMatchingTensor(
        "workerCostPerUnit", workerCostPerUnit, this.workerEfficiencies);
  }

  public int getNumberOfJobs() {
    return numberOfJobs;
  }

  public int getNumberOfStages() {
    return numberOfStages;
  }

  public int getNumberOfFactories() {
    return numberOfFactories;
  }

  public double getStandardProcessingTime(int stage, int job) {
    return standardProcessingTimes[stage][job];
  }

  public double[] getStandardProcessingTimes(int stage) {
    return standardProcessingTimes[stage].clone();
  }

  public double getStandardSetupTime(int stage, int job) {
    return standardSetupTimes[stage][job];
  }

  public double[] getStandardSetupTimes(int stage) {
    return standardSetupTimes[stage].clone();
  }

  public int getMachineCount(int factory, int stage) {
    return machineSpeeds[factory][stage].length;
  }

  public double[] getMachineSpeeds(int factory, int stage) {
    return machineSpeeds[factory][stage].clone();
  }

  public double[] getMachineEnergyPerUnit(int factory, int stage) {
    return machineEnergyPerUnit[factory][stage].clone();
  }

  public int getWorkerCount(int factory, int stage) {
    return workerEfficiencies[factory][stage].length;
  }

  public double[] getWorkerEfficiencies(int factory, int stage) {
    return workerEfficiencies[factory][stage].clone();
  }

  public double[] getWorkerCostPerUnit(int factory, int stage) {
    return workerCostPerUnit[factory][stage].clone();
  }

  private static double[][] copyAndValidateStageJobMatrix(
      String name, double[][] source, int stages, int jobs) {
    if (source == null || source.length != stages) {
      throw new IllegalArgumentException(name + " must contain " + stages + " stages");
    }
    double[][] copy = new double[stages][];
    for (int stage = 0; stage < stages; stage++) {
      if (source[stage] == null || source[stage].length != jobs) {
        throw new IllegalArgumentException(
            name + " stage " + stage + " must contain " + jobs + " jobs");
      }
      copy[stage] = source[stage].clone();
      validatePositive(name + " stage " + stage, copy[stage]);
    }
    return copy;
  }

  private static double[][][] copyAndValidateResourceTensor(
      String name, double[][][] source, int factories, int stages) {
    if (source == null || source.length != factories) {
      throw new IllegalArgumentException(name + " must contain " + factories + " factories");
    }
    double[][][] copy = new double[factories][stages][];
    for (int factory = 0; factory < factories; factory++) {
      if (source[factory] == null || source[factory].length != stages) {
        throw new IllegalArgumentException(
            name + " factory " + factory + " must contain " + stages + " stages");
      }
      for (int stage = 0; stage < stages; stage++) {
        if (source[factory][stage] == null || source[factory][stage].length == 0) {
          throw new IllegalArgumentException(
              name + " factory " + factory + " stage " + stage + " must not be empty");
        }
        copy[factory][stage] = source[factory][stage].clone();
        validatePositive(name + " factory " + factory + " stage " + stage,
            copy[factory][stage]);
      }
    }
    return copy;
  }

  private static double[][][] copyAndValidateMatchingTensor(
      String name, double[][][] source, double[][][] dimensions) {
    if (source == null || source.length != dimensions.length) {
      throw new IllegalArgumentException(name + " factory dimension mismatch");
    }
    double[][][] copy = new double[dimensions.length][][];
    for (int factory = 0; factory < dimensions.length; factory++) {
      if (source[factory] == null || source[factory].length != dimensions[factory].length) {
        throw new IllegalArgumentException(name + " stage dimension mismatch at factory " + factory);
      }
      copy[factory] = new double[dimensions[factory].length][];
      for (int stage = 0; stage < dimensions[factory].length; stage++) {
        if (source[factory][stage] == null
            || source[factory][stage].length != dimensions[factory][stage].length) {
          throw new IllegalArgumentException(
              name + " resource count mismatch at factory " + factory + ", stage " + stage);
        }
        copy[factory][stage] = source[factory][stage].clone();
        validatePositive(name + " factory " + factory + " stage " + stage,
            copy[factory][stage]);
      }
    }
    return copy;
  }

  private static void validatePositive(String name, double[] values) {
    for (int index = 0; index < values.length; index++) {
      if (!Double.isFinite(values[index]) || values[index] <= 0.0) {
        throw new IllegalArgumentException(
            name + " contains non-positive value at index " + index + ": " + values[index]);
      }
    }
  }
}
