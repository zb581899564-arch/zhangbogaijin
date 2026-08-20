package org.uma.jmetal.algorithm.multiobjective.mypso.zhangbo;

import org.uma.jmetal.problem.multiobjective.dfsp.fatigue.ZhangBoFatigueInstanceData;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/** Immutable first-stage resource domain derived from the active P5 instance snapshot. */
public final class ZhangBoResourceDomain implements Serializable {
  private static final long serialVersionUID = 1L;
  private final int factories;
  private final int[] machineCounts;
  private final int[][] workers;

  public ZhangBoResourceDomain(ZhangBoFatigueInstanceData instance) {
    if (instance == null) throw new IllegalArgumentException("instance");
    factories = instance.getFactories();
    machineCounts = new int[factories];
    workers = new int[factories][];
    for (int factory = 0; factory < factories; factory++) {
      machineCounts[factory] = instance.getMachineCount(factory, 0);
      workers[factory] = instance.getEligibleWorkers(factory, 0);
    }
  }

  public int getFactoryCount() { return factories; }
  public int getMachineCount(int factory) { checkFactory(factory); return machineCounts[factory]; }
  public int[] getWorkers(int factory) { checkFactory(factory); return workers[factory].clone(); }
  public boolean isFactoryValid(int factory) { return factory >= 0 && factory < factories; }
  public boolean isMachineValid(int factory, int machine) {
    return isFactoryValid(factory) && machine >= 0 && machine < machineCounts[factory];
  }
  public boolean isWorkerValid(int factory, int worker) {
    if (!isFactoryValid(factory)) return false;
    for (int value : workers[factory]) if (value == worker) return true;
    return false;
  }

  public int firstMachine(int factory) { checkFactory(factory); return 0; }
  public int firstWorker(int factory) { checkFactory(factory); return workers[factory][0]; }

  public int[] alternativeFactories(int current) {
    List<Integer> values = new ArrayList<>();
    for (int factory = 0; factory < factories; factory++) if (factory != current) values.add(factory);
    return toArray(values);
  }

  public int[] alternativeMachines(int factory, int current) {
    checkFactory(factory);
    List<Integer> values = new ArrayList<>();
    for (int machine = 0; machine < machineCounts[factory]; machine++) {
      if (machine != current) values.add(machine);
    }
    return toArray(values);
  }

  public int[] alternativeWorkers(int factory, int current) {
    checkFactory(factory);
    List<Integer> values = new ArrayList<>();
    for (int worker : workers[factory]) if (worker != current) values.add(worker);
    return toArray(values);
  }

  private void checkFactory(int factory) {
    if (!isFactoryValid(factory)) throw new IllegalArgumentException("Invalid factory=" + factory);
  }

  private static int[] toArray(List<Integer> values) {
    int[] result = new int[values.size()];
    for (int index = 0; index < values.size(); index++) result[index] = values.get(index);
    return result;
  }
}
