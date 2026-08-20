package org.uma.jmetal.algorithm.multiobjective.mypso.v35.p25e;

import java.util.ArrayList;
import java.util.List;
import org.uma.jmetal.algorithm.multiobjective.mypso.zhangbo.ZhangBoResourceDomain;
import org.uma.jmetal.operator.CrossoverOperator;
import org.uma.jmetal.operator.MutationOperator;
import org.uma.jmetal.problem.multiobjective.dfsp.model.DhhfspFourVectorSolution;
import org.uma.jmetal.problem.multiobjective.dfsp.fatigue.V35ComparisonSolution;
import org.uma.jmetal.solution.PermutationSolution;
import org.uma.jmetal.util.pseudorandom.PseudoRandomGenerator;

/** Problem-representation operators only; contains no algorithm-level selection or archive logic. */
public final class V35FourVectorVariation {
  private V35FourVectorVariation() { }

  public static final class Crossover
      implements CrossoverOperator<PermutationSolution<Integer>> {
    private final double jsRate, faRate, maRate, waRate;
    private final ZhangBoResourceDomain domain;
    private final PseudoRandomGenerator random;
    private int crossoverFlag;

    public Crossover(double jsRate, double faRate, double maRate, double waRate,
        ZhangBoResourceDomain domain, PseudoRandomGenerator random) {
      check(jsRate); check(faRate); check(maRate); check(waRate);
      if (domain == null || random == null) throw new IllegalArgumentException("domain/random");
      this.jsRate = jsRate; this.faRate = faRate; this.maRate = maRate; this.waRate = waRate;
      this.domain = domain; this.random = random;
    }

    @Override public int getNumberOfRequiredParents() { return 2; }
    @Override public int getNumberOfGeneratedChildren() { return 2; }
    @Override public int getCrossoverProbabilityflag() { return crossoverFlag; }

    @Override
    public List<PermutationSolution<Integer>> execute(
        List<PermutationSolution<Integer>> parents) {
      if (parents == null || parents.size() != 2) {
        throw new IllegalArgumentException("four-vector crossover requires two parents");
      }
      DhhfspFourVectorSolution first = require(parents.get(0));
      DhhfspFourVectorSolution second = require(parents.get(1));
      DhhfspFourVectorSolution a = first.copy();
      DhhfspFourVectorSolution b = second.copy();
      int jobs = a.getNumberOfVariables();
      crossoverFlag = 0;
      if (jobs > 1 && event(jsRate)) { pmx(first, second, a, b); crossoverFlag = 1; }
      if (jobs > 1 && event(faRate)) { onePoint(first.getFactoryAssignments(),
          second.getFactoryAssignments(), a.getFactoryAssignments(), b.getFactoryAssignments());
        crossoverFlag = 1; }
      if (jobs > 1 && event(maRate)) { onePoint(first.getMachineAssignments(),
          second.getMachineAssignments(), a.getMachineAssignments(), b.getMachineAssignments());
        crossoverFlag = 1; }
      if (jobs > 1 && event(waRate)) { onePoint(first.getWorkerAssignments(),
          second.getWorkerAssignments(), a.getWorkerAssignments(), b.getWorkerAssignments());
        crossoverFlag = 1; }
      repair(a); repair(b);
      List<PermutationSolution<Integer>> result = new ArrayList<>(2);
      result.add(wrapLike(parents.get(0), a));
      result.add(wrapLike(parents.get(1), b));
      return result;
    }

    private void pmx(DhhfspFourVectorSolution p1, DhhfspFourVectorSolution p2,
        DhhfspFourVectorSolution a, DhhfspFourVectorSolution b) {
      int n = p1.getNumberOfVariables();
      int left = random.nextInt(0, n - 1), right = random.nextInt(0, n - 1);
      while (right == left) right = random.nextInt(0, n - 1);
      if (left > right) { int t = left; left = right; right = t; }
      pmxChild(p1, p2, a, left, right);
      pmxChild(p2, p1, b, left, right);
    }

    private static void pmxChild(DhhfspFourVectorSolution base,
        DhhfspFourVectorSolution donor, DhhfspFourVectorSolution child,
        int left, int right) {
      int n = base.getNumberOfVariables();
      int[] replacement = new int[n];
      java.util.Arrays.fill(replacement, -1);
      for (int i = left; i <= right; i++) {
        child.setVariableValue(i, donor.getVariableValue(i));
        replacement[donor.getVariableValue(i)] = base.getVariableValue(i);
      }
      for (int i = 0; i < n; i++) if (i < left || i > right) {
        int value = base.getVariableValue(i);
        while (replacement[value] != -1) value = replacement[value];
        child.setVariableValue(i, value);
      }
    }

    private void onePoint(List<Integer> p1, List<Integer> p2,
        List<Integer> a, List<Integer> b) {
      int cut = random.nextInt(1, p1.size() - 1);
      for (int i = cut; i < p1.size(); i++) {
        a.set(i, p2.get(i)); b.set(i, p1.get(i));
      }
    }

    private void repair(DhhfspFourVectorSolution solution) {
      for (int pos = 0; pos < solution.getNumberOfVariables(); pos++) {
        int factory = solution.getFactoryAssignments().get(pos);
        if (factory < 0 || factory >= domain.getFactoryCount()) factory = 0;
        solution.getFactoryAssignments().set(pos, factory);
        int machines = domain.getMachineCount(factory);
        int machine = solution.getMachineAssignments().get(pos);
        if (machine < 0 || machine >= machines) machine = 0;
        solution.getMachineAssignments().set(pos, machine);
        int[] workers = domain.getWorkers(factory);
        int worker = solution.getWorkerAssignments().get(pos);
        boolean valid = false;
        for (int candidate : workers) if (candidate == worker) { valid = true; break; }
        if (!valid) solution.getWorkerAssignments().set(pos, workers[0]);
      }
    }

    private boolean event(double rate) { return random.nextDouble() < rate; }
  }

  public static final class Mutation
      implements MutationOperator<PermutationSolution<Integer>> {
    private final double jsRate, faRate, maRate, waRate;
    private final ZhangBoResourceDomain domain;
    private final PseudoRandomGenerator random;
    private int mutationFlag;

    public Mutation(double jsRate, double faRate, double maRate, double waRate,
        ZhangBoResourceDomain domain, PseudoRandomGenerator random) {
      check(jsRate); check(faRate); check(maRate); check(waRate);
      if (domain == null || random == null) throw new IllegalArgumentException("domain/random");
      this.jsRate = jsRate; this.faRate = faRate; this.maRate = maRate; this.waRate = waRate;
      this.domain = domain; this.random = random;
    }

    @Override
    public PermutationSolution<Integer> execute(PermutationSolution<Integer> generic) {
      DhhfspFourVectorSolution solution = require(generic);
      int n = solution.getNumberOfVariables();
      mutationFlag = 0;
      if (n > 1 && event(jsRate)) {
        int a = random.nextInt(0, n - 1), b = random.nextInt(0, n - 1);
        while (a == b) b = random.nextInt(0, n - 1);
        Integer value = solution.getVariableValue(a);
        solution.setVariableValue(a, solution.getVariableValue(b));
        solution.setVariableValue(b, value);
        mutationFlag = 1;
      }
      if (event(faRate)) {
        int pos = random.nextInt(0, n - 1);
        int old = solution.getFactoryAssignments().get(pos);
        int next = alternate(old, domain.getFactoryCount());
        solution.getFactoryAssignments().set(pos, next);
        solution.getMachineAssignments().set(pos, 0);
        solution.getWorkerAssignments().set(pos, domain.getWorkers(next)[0]);
        mutationFlag = 1;
      }
      if (event(maRate)) {
        int pos = random.nextInt(0, n - 1);
        int factory = solution.getFactoryAssignments().get(pos);
        solution.getMachineAssignments().set(pos,
            alternate(solution.getMachineAssignments().get(pos), domain.getMachineCount(factory)));
        mutationFlag = 1;
      }
      if (event(waRate)) {
        int pos = random.nextInt(0, n - 1);
        int[] eligible = domain.getWorkers(solution.getFactoryAssignments().get(pos));
        int current = solution.getWorkerAssignments().get(pos), index = 0;
        for (int i = 0; i < eligible.length; i++) if (eligible[i] == current) index = i;
        solution.getWorkerAssignments().set(pos, eligible[alternate(index, eligible.length)]);
        mutationFlag = 1;
      }
      return solution;
    }

    @Override public int getMutationProbabilityflag() { return mutationFlag; }

    private int alternate(int current, int bound) {
      if (bound <= 1) return 0;
      int value = random.nextInt(0, bound - 2);
      return value >= current ? value + 1 : value;
    }
    private boolean event(double rate) { return random.nextDouble() < rate; }
  }

  private static DhhfspFourVectorSolution require(PermutationSolution<Integer> solution) {
    if (solution instanceof V35ComparisonSolution) {
      return ((V35ComparisonSolution) solution).asFourVector();
    }
    if (!(solution instanceof DhhfspFourVectorSolution)) {
      throw new IllegalArgumentException("four-vector variation requires DhhfspFourVectorSolution");
    }
    return (DhhfspFourVectorSolution) solution;
  }
  private static PermutationSolution<Integer> wrapLike(
      PermutationSolution<Integer> parent, DhhfspFourVectorSolution child) {
    return parent instanceof V35ComparisonSolution
        ? new V35ComparisonSolution(child) : child;
  }
  private static void check(double value) {
    if (!Double.isFinite(value) || value < 0.0 || value > 1.0) {
      throw new IllegalArgumentException("probability outside [0,1]");
    }
  }
}
