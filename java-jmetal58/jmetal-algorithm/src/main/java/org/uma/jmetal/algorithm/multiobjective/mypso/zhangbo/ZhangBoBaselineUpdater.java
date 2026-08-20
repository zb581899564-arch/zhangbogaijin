package org.uma.jmetal.algorithm.multiobjective.mypso.zhangbo;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.uma.jmetal.solution.PermutationSolution;
import org.uma.jmetal.util.pseudorandom.PseudoRandomGenerator;

/**
 * Structured replayable author baseline: PSO exchange sequence plus GA
 * crossover/mutation for the first-stage FA/MA/WA vectors.
 *
 * <p>The updater deliberately has no dependency on the legacy giant update
 * routine.  Every stochastic decision is made through the injected
 * {@link PseudoRandomGenerator}; this makes baseline replays and unit tests
 * independent of the global jMetal random singleton.</p>
 */
public final class ZhangBoBaselineUpdater {
  private static final double EPSILON = 1.0e-12;

  public Result update(
      PermutationSolution<Integer> current,
      PermutationSolution<Integer> personalBest,
      PermutationSolution<Integer> globalBest,
      ZhangBoResourceDomain domain,
      double randK,
      double crossoverRate,
      double mutationRate,
      double machineMutationRate,
      double workerMutationRate,
      PseudoRandomGenerator random) {
    return update(current, personalBest, globalBest, domain, randK,
        crossoverRate, crossoverRate, crossoverRate,
        mutationRate, machineMutationRate, workerMutationRate, random);
  }

  /**
   * Paper-explicit Fig.5/Fig.6 update with independent FA/MA/WA probabilities.
   * Resource crossover is position aligned, exactly like the published four-vector operator;
   * CFVF is the separate identity-aligned innovation path.
   */
  public Result update(
      PermutationSolution<Integer> current,
      PermutationSolution<Integer> personalBest,
      PermutationSolution<Integer> globalBest,
      ZhangBoResourceDomain domain,
      double randomCoefficientUpperBound,
      double faCrossoverRate,
      double machineCrossoverRate,
      double workerCrossoverRate,
      double faMutationRate,
      double machineMutationRate,
      double workerMutationRate,
      PseudoRandomGenerator random) {
    if (current == null || personalBest == null || globalBest == null
        || domain == null || random == null) {
      throw new IllegalArgumentException("Baseline updater inputs cannot be null");
    }
    requireProbability(randomCoefficientUpperBound, "randomCoefficientUpperBound");
    requireProbability(faCrossoverRate, "faCrossoverRate");
    requireProbability(machineCrossoverRate, "machineCrossoverRate");
    requireProbability(workerCrossoverRate, "workerCrossoverRate");
    requireProbability(faMutationRate, "faMutationRate");
    requireProbability(machineMutationRate, "machineMutationRate");
    requireProbability(workerMutationRate, "workerMutationRate");
    validateShape(current, personalBest, globalBest);

    int jobs = current.getNumberOfVariables();
    PermutationSolution<Integer> offspring = copyWithMachineVector(current, jobs);
    List<String> events = new ArrayList<>();
    applyJobSequencePso(offspring, personalBest, globalBest,
        randomCoefficientUpperBound, random, events);

    List<Integer> personalMachines = machineVector(personalBest, jobs);
    List<Integer> globalMachines = machineVector(globalBest, jobs);
    List<Integer> offspringMachines = machineVector(offspring, jobs);

    if (random.nextDouble() < faCrossoverRate) {
      crossoverFactory(offspring, personalBest, offspringMachines,
          randomRange(jobs, random), domain, random, events, "PBEST");
    }
    if (random.nextDouble() < faCrossoverRate) {
      crossoverFactory(offspring, globalBest, offspringMachines,
          randomRange(jobs, random), domain, random, events, "QGBEST");
    }
    if (random.nextDouble() < faMutationRate) {
      mutateFactory(offspring, offspringMachines, random.nextInt(0, jobs - 1),
          domain, random, events);
    }

    if (random.nextDouble() < machineCrossoverRate) {
      crossoverMachine(offspring, offspringMachines, personalMachines,
          randomRange(jobs, random), domain, random, events, "PBEST");
    }
    if (random.nextDouble() < machineCrossoverRate) {
      crossoverMachine(offspring, offspringMachines, globalMachines,
          randomRange(jobs, random), domain, random, events, "QGBEST");
    }
    if (random.nextDouble() < machineMutationRate) {
      mutateMachine(offspring, offspringMachines, random.nextInt(0, jobs - 1),
          domain, random, events);
    }

    if (random.nextDouble() < workerCrossoverRate) {
      crossoverWorker(offspring, personalBest, randomRange(jobs, random),
          domain, random, events, "PBEST");
    }
    if (random.nextDouble() < workerCrossoverRate) {
      crossoverWorker(offspring, globalBest, randomRange(jobs, random),
          domain, random, events, "QGBEST");
    }
    if (random.nextDouble() < workerMutationRate) {
      mutateWorker(offspring, random.nextInt(0, jobs - 1), domain, random, events);
    }

    repair(offspring, offspringMachines, domain, events);
    ZhangBoMachineVectorSupport.write(offspring, offspringMachines);
    return new Result(offspring, events);
  }

  private static void crossoverFactory(PermutationSolution<Integer> child,
      PermutationSolution<Integer> parent, List<Integer> machines, int[] range,
      ZhangBoResourceDomain domain, PseudoRandomGenerator random, List<String> events,
      String source) {
    for (int position = range[0]; position <= range[1]; position++) {
      child.getVariablesid().set(position, parent.getVariableValueid(position));
    }
    repairRange(child, machines, range, true, true, domain, random, events);
    events.add("ga:FA-crossover:source=" + source + ",range="
        + range[0] + ".." + range[1]);
  }

  private static void crossoverMachine(PermutationSolution<Integer> child,
      List<Integer> childMachines, List<Integer> parentMachines, int[] range,
      ZhangBoResourceDomain domain, PseudoRandomGenerator random, List<String> events,
      String source) {
    for (int position = range[0]; position <= range[1]; position++) {
      childMachines.set(position, parentMachines.get(position));
    }
    repairRange(child, childMachines, range, true, false, domain, random, events);
    events.add("ga:MA-crossover:source=" + source + ",range="
        + range[0] + ".." + range[1]);
  }

  private static void crossoverWorker(PermutationSolution<Integer> child,
      PermutationSolution<Integer> parent, int[] range,
      ZhangBoResourceDomain domain, PseudoRandomGenerator random, List<String> events,
      String source) {
    for (int position = range[0]; position <= range[1]; position++) {
      child.getVariablesworker().set(position, parent.getVariableValueworker(position));
    }
    repairRange(child, machineVector(child, child.getNumberOfVariables()), range,
        false, true, domain, random, events);
    events.add("ga:WA-crossover:source=" + source + ",range="
        + range[0] + ".." + range[1]);
  }

  private static void mutateFactory(PermutationSolution<Integer> child,
      List<Integer> machines, int position, ZhangBoResourceDomain domain,
      PseudoRandomGenerator random, List<String> events) {
    int oldFactory = child.getVariableValueid(position);
    int factory = random.nextInt(0, domain.getFactoryCount() - 1);
    child.getVariablesid().set(position, factory);
    int machine = random.nextInt(0, domain.getMachineCount(factory) - 1);
    int[] workers = domain.getWorkers(factory);
    int worker = workers[random.nextInt(0, workers.length - 1)];
    machines.set(position, machine);
    child.getVariablesworker().set(position, worker);
    events.add("ga:FA-mutation:position=" + position + ",old=" + oldFactory
        + ",new=" + factory + ",syncMA=" + machine + ",syncWA=" + worker);
  }

  private static void mutateMachine(PermutationSolution<Integer> child,
      List<Integer> machines, int position, ZhangBoResourceDomain domain,
      PseudoRandomGenerator random, List<String> events) {
    int factory = child.getVariableValueid(position);
    int old = machines.get(position);
    int value = random.nextInt(0, domain.getMachineCount(factory) - 1);
    machines.set(position, value);
    events.add("ga:MA-mutation:position=" + position + ",old=" + old + ",new=" + value);
  }

  private static void mutateWorker(PermutationSolution<Integer> child, int position,
      ZhangBoResourceDomain domain, PseudoRandomGenerator random, List<String> events) {
    int factory = child.getVariableValueid(position);
    int old = child.getVariableValueworker(position);
    int[] workers = domain.getWorkers(factory);
    int value = workers[random.nextInt(0, workers.length - 1)];
    child.getVariablesworker().set(position, value);
    events.add("ga:WA-mutation:position=" + position + ",old=" + old + ",new=" + value);
  }

  private static void repairRange(PermutationSolution<Integer> child,
      List<Integer> machines, int[] range, boolean repairMachine, boolean repairWorker,
      ZhangBoResourceDomain domain, PseudoRandomGenerator random, List<String> events) {
    for (int position = range[0]; position <= range[1]; position++) {
      int factory = child.getVariableValueid(position);
      if (repairMachine && !domain.isMachineValid(factory, machines.get(position))) {
        int old = machines.get(position);
        int value = random.nextInt(0, domain.getMachineCount(factory) - 1);
        machines.set(position, value);
        events.add("repair:MA:position=" + position + ",old=" + old + ",new=" + value);
      }
      if (repairWorker && !domain.isWorkerValid(factory,
          child.getVariableValueworker(position))) {
        int old = child.getVariableValueworker(position);
        int[] workers = domain.getWorkers(factory);
        int value = workers[random.nextInt(0, workers.length - 1)];
        child.getVariablesworker().set(position, value);
        events.add("repair:WA:position=" + position + ",old=" + old + ",new=" + value);
      }
    }
  }

  private static int[] randomRange(int jobs, PseudoRandomGenerator random) {
    int first = random.nextInt(0, jobs - 1);
    int second = random.nextInt(0, jobs - 1);
    return new int[] {Math.min(first, second), Math.max(first, second)};
  }

  private static void applyJobSequencePso(
      PermutationSolution<Integer> offspring,
      PermutationSolution<Integer> personalBest,
      PermutationSolution<Integer> globalBest,
      double randK,
      PseudoRandomGenerator random,
      List<String> events) {
    int jobs = offspring.getNumberOfVariables();
    if (jobs > 1) {
      int left = random.nextInt(0, jobs - 1);
      int right = random.nextInt(0, jobs - 1);
      swap(offspring.getVariables(), left, right);
      events.add("pso:JS-exploreSwap=" + left + "," + right);
    }
    List<Swap> personal = exchangeDifference(personalBest.getVariables(), offspring.getVariables());
    double personalCoefficient = random.nextDouble(0.0, randK);
    int personalSwaps = (int) Math.floor(personal.size() * personalCoefficient);
    for (int index = 0; index < Math.min(personalSwaps, personal.size()); index++) {
      Swap swap = personal.get(index);
      swap(offspring.getVariables(), swap.left, swap.right);
      events.add("pso:JS-pbestSwap=" + swap.left + "," + swap.right);
    }
    events.add("pso:JS-pbestCoefficient=" + personalCoefficient
        + ",available=" + personal.size() + ",applied=" + personalSwaps);
    List<Swap> global = exchangeDifference(globalBest.getVariables(), offspring.getVariables());
    double globalCoefficient = random.nextDouble(0.0, randK);
    int globalSwaps = (int) Math.floor(global.size() * globalCoefficient);
    for (int index = 0; index < Math.min(globalSwaps, global.size()); index++) {
      Swap swap = global.get(index);
      swap(offspring.getVariables(), swap.left, swap.right);
      events.add("pso:JS-gbestSwap=" + swap.left + "," + swap.right);
    }
    events.add("pso:JS-gbestCoefficient=" + globalCoefficient
        + ",available=" + global.size() + ",applied=" + globalSwaps);
  }

  private static void repair(
      PermutationSolution<Integer> solution, List<Integer> machines,
      ZhangBoResourceDomain domain, List<String> events) {
    for (int position = 0; position < solution.getNumberOfVariables(); position++) {
      int factory = solution.getVariableValueid(position);
      if (!domain.isFactoryValid(factory)) {
        factory = 0;
        solution.getVariablesid().set(position, factory);
        events.add("repair:FA:position=" + position + ",new=0");
      }
      int machine = machines.get(position);
      if (!domain.isMachineValid(factory, machine)) {
        machine = domain.firstMachine(factory);
        machines.set(position, machine);
        events.add("repair:MA:position=" + position + ",new=" + machine);
      }
      int worker = solution.getVariableValueworker(position);
      if (!domain.isWorkerValid(factory, worker)) {
        worker = domain.firstWorker(factory);
        solution.getVariablesworker().set(position, worker);
        events.add("repair:WA:position=" + position + ",new=" + worker);
      }
    }
  }

  private static PermutationSolution<Integer> copyWithMachineVector(
      PermutationSolution<Integer> source, int jobs) {
    @SuppressWarnings("unchecked")
    PermutationSolution<Integer> copy = (PermutationSolution<Integer>) source.copy();
    ZhangBoMachineVectorSupport.write(copy, machineVector(source, jobs));
    return copy;
  }

  @SuppressWarnings("unchecked")
  private static List<Integer> machineVector(
      PermutationSolution<Integer> solution, int jobs) {
    try {
      return ZhangBoMachineVectorSupport.copy(solution, jobs);
    } catch (IllegalArgumentException missingMachineVector) {
      // The structured baseline can materialize a legal zero machine vector
      // for a legacy solution that omitted its optional attribute.
      return new ArrayList<>(Collections.nCopies(jobs, 0));
    }
  }

  private static int[] positions(PermutationSolution<Integer> solution) {
    int jobs = solution.getNumberOfVariables();
    int[] result = new int[jobs];
    boolean[] seen = new boolean[jobs];
    for (int position = 0; position < jobs; position++) {
      Integer job = solution.getVariableValue(position);
      if (job == null || job < 0 || job >= jobs) {
        throw new IllegalArgumentException("JS must contain job identities [0," + (jobs - 1) + "]");
      }
      if (seen[job]) {
        throw new IllegalArgumentException("JS contains duplicate job=" + job);
      }
      seen[job] = true;
      result[job] = position;
    }
    return result;
  }

  private static void validateShape(
      PermutationSolution<Integer> current,
      PermutationSolution<Integer> personalBest,
      PermutationSolution<Integer> globalBest) {
    int jobs = current.getNumberOfVariables();
    if (personalBest.getNumberOfVariables() != jobs || globalBest.getNumberOfVariables() != jobs
        || current.getNumberOfVariablesid() < jobs
        || personalBest.getNumberOfVariablesid() < jobs
        || globalBest.getNumberOfVariablesid() < jobs
        || current.getNumberOfVariablesworker() < jobs
        || personalBest.getNumberOfVariablesworker() < jobs
        || globalBest.getNumberOfVariablesworker() < jobs) {
      throw new IllegalArgumentException("Baseline vectors must all cover every job");
    }
    positions(current);
    positions(personalBest);
    positions(globalBest);
  }

  private static List<Swap> exchangeDifference(List<Integer> target, List<Integer> current) {
    if (target.size() != current.size()) throw new IllegalArgumentException("JS length mismatch");
    List<Integer> work = new ArrayList<>(current);
    List<Swap> result = new ArrayList<>();
    for (int position = 0; position < work.size(); position++) {
      if (!work.get(position).equals(target.get(position))) {
        int other = work.indexOf(target.get(position));
        if (other < 0) throw new IllegalArgumentException("JS is not the same permutation");
        result.add(new Swap(position, other));
        swap(work, position, other);
      }
    }
    return result;
  }

  private static void swap(List<Integer> values, int left, int right) {
    Integer value = values.get(left);
    values.set(left, values.get(right));
    values.set(right, value);
  }

  private static void requireProbability(double value, String name) {
    if (!Double.isFinite(value) || value < 0.0 || value > 1.0) {
      throw new IllegalArgumentException(name + " must be finite and in [0,1]");
    }
  }

  private static final class Swap {
    private final int left;
    private final int right;

    private Swap(int left, int right) {
      this.left = left;
      this.right = right;
    }
  }

  public static final class Result {
    private final PermutationSolution<Integer> solution;
    private final List<String> events;

    private Result(PermutationSolution<Integer> solution, List<String> events) {
      this.solution = solution;
      this.events = Collections.unmodifiableList(new ArrayList<>(events));
    }

    public PermutationSolution<Integer> getSolution() { return solution; }
    public List<String> getEvents() { return events; }
  }
}
