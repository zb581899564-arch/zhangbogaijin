package org.uma.jmetal.algorithm.multiobjective.hmopsoqgs;

import org.uma.jmetal.problem.multiobjective.dfsp.model.Chapter4OperatorFixtures.SwapPair;
import org.uma.jmetal.problem.multiobjective.dfsp.model.DhhfspEncodingValidator;
import org.uma.jmetal.problem.multiobjective.dfsp.model.DhhfspFourVectorSolution;
import org.uma.jmetal.problem.multiobjective.dfsp.model.DhhfspInstance;
import org.uma.jmetal.util.pseudorandom.PseudoRandomGenerator;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Paper operators for the position-aligned JS/FA/MA/WA representation. */
public final class FourVectorOperators implements Serializable {
  private static final long serialVersionUID = 1L;
  private final DhhfspInstance instance;
  private final PseudoRandomGenerator random;
  private final List<String> events = new ArrayList<>();

  public FourVectorOperators(DhhfspInstance instance, PseudoRandomGenerator random) {
    if (instance == null || random == null) throw new IllegalArgumentException("dependencies");
    this.instance = instance;
    this.random = random;
  }

  /** Stable left-to-right exchange sequence which transforms current into target. */
  public static List<SwapPair> exchangeSequence(List<Integer> current, List<Integer> target) {
    if (current == null || target == null || current.size() != target.size()) {
      throw new IllegalArgumentException("JS vectors must have equal length");
    }
    List<Integer> work = new ArrayList<>(current);
    List<SwapPair> result = new ArrayList<>();
    for (int position = 0; position < work.size(); position++) {
      if (!work.get(position).equals(target.get(position))) {
        int other = work.indexOf(target.get(position));
        if (other < 0) throw new IllegalArgumentException("target is not the same permutation");
        Collections.swap(work, position, other);
        result.add(new SwapPair(position, other));
      }
    }
    if (!work.equals(target)) throw new IllegalArgumentException("target is not a permutation");
    return result;
  }

  public static List<Integer> applyExchangePrefix(
      List<Integer> source, List<SwapPair> exchanges, double coefficient) {
    List<Integer> result = new ArrayList<>(source);
    int count = Math.min(exchanges.size(), (int) Math.floor(coefficient * exchanges.size()));
    for (int index = 0; index < count; index++) {
      SwapPair pair = exchanges.get(index);
      Collections.swap(result, pair.getFirstPosition(), pair.getSecondPosition());
    }
    return result;
  }

  /** One complete published-baseline flight toward pbest and Q-gbest. */
  public DhhfspFourVectorSolution update(
      DhhfspFourVectorSolution current, DhhfspFourVectorSolution pbest,
      DhhfspFourVectorSolution qGbest, HmopsoQgsConfiguration configuration) {
    DhhfspFourVectorSolution child = current.copy();
    int first = random.nextInt(0, child.getNumberOfVariables() - 1);
    int second = random.nextInt(0, child.getNumberOfVariables() - 1);
    Collections.swap(child.getJobSequence(), first, second);
    events.add("JS_INITIAL_SWAP:" + first + ":" + second);
    applyGuideJs(child, pbest, configuration.getRandomCoefficientUpperBound(), "PBEST");
    applyGuideJs(child, qGbest, configuration.getRandomCoefficientUpperBound(), "QGBEST");

    double[] cross = configuration.getCrossoverProbabilities();
    double[] mutation = configuration.getMutationProbabilities();
    if (random.nextDouble() < cross[0]) crossoverFa(child, pbest, randomRange(child), true);
    if (random.nextDouble() < cross[0]) crossoverFa(child, qGbest, randomRange(child), true);
    if (random.nextDouble() < mutation[0]) mutateFa(child, randomPosition(child));
    if (random.nextDouble() < cross[1]) crossoverMa(child, pbest, randomRange(child), true);
    if (random.nextDouble() < cross[1]) crossoverMa(child, qGbest, randomRange(child), true);
    if (random.nextDouble() < mutation[1]) mutateMa(child, randomPosition(child));
    if (random.nextDouble() < cross[2]) crossoverWa(child, pbest, randomRange(child), true);
    if (random.nextDouble() < cross[2]) crossoverWa(child, qGbest, randomRange(child), true);
    if (random.nextDouble() < mutation[2]) mutateWa(child, randomPosition(child));
    DhhfspEncodingValidator.validateOrThrow(child, instance);
    return child;
  }

  public void crossoverFa(
      DhhfspFourVectorSolution child, DhhfspFourVectorSolution parent,
      int[] inclusiveRange, boolean repair) {
    copyRange(child.getFactoryAssignments(), parent.getFactoryAssignments(), inclusiveRange);
    if (repair) repairResources(child, inclusiveRange[0], inclusiveRange[1]);
    validate(child);
  }

  public void crossoverMa(
      DhhfspFourVectorSolution child, DhhfspFourVectorSolution parent,
      int[] inclusiveRange, boolean repair) {
    copyRange(child.getMachineAssignments(), parent.getMachineAssignments(), inclusiveRange);
    if (repair) repairMachines(child, inclusiveRange[0], inclusiveRange[1]);
    validate(child);
  }

  public void crossoverWa(
      DhhfspFourVectorSolution child, DhhfspFourVectorSolution parent,
      int[] inclusiveRange, boolean repair) {
    copyRange(child.getWorkerAssignments(), parent.getWorkerAssignments(), inclusiveRange);
    if (repair) repairWorkers(child, inclusiveRange[0], inclusiveRange[1]);
    validate(child);
  }

  public void mutateFa(DhhfspFourVectorSolution child, int position) {
    int old = child.getFactoryAssignments().get(position);
    int value = random.nextInt(0, instance.getNumberOfFactories() - 1);
    child.getFactoryAssignments().set(position, value);
    events.add("FA_MUTATE:" + position + ':' + old + ":" + value);
    synchronizeResources(child, position);
    validate(child);
  }

  public void mutateMa(DhhfspFourVectorSolution child, int position) {
    int factory = child.getFactoryAssignments().get(position);
    int old = child.getMachineAssignments().get(position);
    int value = random.nextInt(0, instance.getMachineCount(factory, 0) - 1);
    child.getMachineAssignments().set(position, value);
    events.add("MA_MUTATE:" + position + ':' + old + ":" + value);
    validate(child);
  }

  public void mutateWa(DhhfspFourVectorSolution child, int position) {
    int factory = child.getFactoryAssignments().get(position);
    int old = child.getWorkerAssignments().get(position);
    int value = random.nextInt(0, instance.getWorkerCount(factory, 0) - 1);
    child.getWorkerAssignments().set(position, value);
    events.add("WA_MUTATE:" + position + ':' + old + ":" + value);
    validate(child);
  }

  public List<String> drainEvents() {
    List<String> copy = new ArrayList<>(events);
    events.clear();
    return copy;
  }

  private void applyGuideJs(
      DhhfspFourVectorSolution child, DhhfspFourVectorSolution guide,
      double upper, String source) {
    List<SwapPair> sequence = exchangeSequence(child.getJobSequence(), guide.getJobSequence());
    double coefficient = random.nextDouble(0.0, upper);
    List<Integer> result = applyExchangePrefix(child.getJobSequence(), sequence, coefficient);
    child.getJobSequence().clear();
    child.getJobSequence().addAll(result);
    events.add("JS_GUIDE:" + source + ":r=" + coefficient + ":pairs=" + sequence);
  }

  private void synchronizeResources(DhhfspFourVectorSolution child, int position) {
    int factory = child.getFactoryAssignments().get(position);
    int oldMachine = child.getMachineAssignments().get(position);
    int oldWorker = child.getWorkerAssignments().get(position);
    int machine = random.nextInt(0, instance.getMachineCount(factory, 0) - 1);
    int worker = random.nextInt(0, instance.getWorkerCount(factory, 0) - 1);
    child.getMachineAssignments().set(position, machine);
    child.getWorkerAssignments().set(position, worker);
    events.add("FA_SYNC_MA:" + position + ':' + oldMachine + ':' + machine);
    events.add("FA_SYNC_WA:" + position + ':' + oldWorker + ':' + worker);
  }

  private void repairResources(DhhfspFourVectorSolution child, int from, int to) {
    repairMachines(child, from, to);
    repairWorkers(child, from, to);
  }

  private void repairMachines(DhhfspFourVectorSolution child, int from, int to) {
    for (int position = from; position <= to; position++) {
      int factory = child.getFactoryAssignments().get(position);
      int value = child.getMachineAssignments().get(position);
      int upper = instance.getMachineCount(factory, 0) - 1;
      if (value < 0 || value > upper) {
        int replacement = random.nextInt(0, upper);
        child.getMachineAssignments().set(position, replacement);
        events.add("REPAIR:MA:" + position + ':' + value + ':' + replacement + ":[0," + upper + ']');
      }
    }
  }

  private void repairWorkers(DhhfspFourVectorSolution child, int from, int to) {
    for (int position = from; position <= to; position++) {
      int factory = child.getFactoryAssignments().get(position);
      int value = child.getWorkerAssignments().get(position);
      int upper = instance.getWorkerCount(factory, 0) - 1;
      if (value < 0 || value > upper) {
        int replacement = random.nextInt(0, upper);
        child.getWorkerAssignments().set(position, replacement);
        events.add("REPAIR:WA:" + position + ':' + value + ':' + replacement + ":[0," + upper + ']');
      }
    }
  }

  private static void copyRange(List<Integer> child, List<Integer> parent, int[] range) {
    if (range == null || range.length != 2 || range[0] < 0
        || range[0] > range[1] || range[1] >= child.size()) {
      throw new IllegalArgumentException("invalid inclusive crossover range");
    }
    for (int position = range[0]; position <= range[1]; position++) {
      child.set(position, parent.get(position));
    }
  }

  private int[] randomRange(DhhfspFourVectorSolution solution) {
    int first = randomPosition(solution);
    int second = randomPosition(solution);
    return new int[] {Math.min(first, second), Math.max(first, second)};
  }

  private int randomPosition(DhhfspFourVectorSolution solution) {
    return random.nextInt(0, solution.getNumberOfVariables() - 1);
  }

  private void validate(DhhfspFourVectorSolution solution) {
    DhhfspEncodingValidator.validateOrThrow(solution, instance);
  }
}
