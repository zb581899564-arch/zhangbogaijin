package org.uma.jmetal.problem.multiobjective.dfsp.decoder;

import java.io.Serializable;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/** Three objectives plus their auditable components. */
public final class ObjectiveBreakdown implements Serializable {
  private static final long serialVersionUID = 1L;
  private final double makespan;
  private final double processingEnergy;
  private final double standbyEnergy;
  private final double totalWorkerCost;
  private final Map<String, Double> machineStandbyTimes;
  private final Map<String, Double> workerCosts;

  public ObjectiveBreakdown(
      double makespan, double processingEnergy, double standbyEnergy,
      double totalWorkerCost, Map<String, Double> machineStandbyTimes,
      Map<String, Double> workerCosts) {
    this.makespan = makespan;
    this.processingEnergy = processingEnergy;
    this.standbyEnergy = standbyEnergy;
    this.totalWorkerCost = totalWorkerCost;
    this.machineStandbyTimes = immutableCopy(machineStandbyTimes);
    this.workerCosts = immutableCopy(workerCosts);
  }

  public double getMakespan() { return makespan; }
  public double getProcessingEnergy() { return processingEnergy; }
  public double getStandbyEnergy() { return standbyEnergy; }
  public double getTotalEnergy() { return processingEnergy + standbyEnergy; }
  public double getTotalWorkerCost() { return totalWorkerCost; }
  public Map<String, Double> getMachineStandbyTimes() { return machineStandbyTimes; }
  public Map<String, Double> getWorkerCosts() { return workerCosts; }

  private static Map<String, Double> immutableCopy(Map<String, Double> source) {
    return Collections.unmodifiableMap(new LinkedHashMap<>(source));
  }
}
