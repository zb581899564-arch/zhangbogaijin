package org.uma.jmetal.algorithm.multiobjective.mypso.v35;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.uma.jmetal.problem.multiobjective.dfsp.fatigue.ZhangBoCanonicalProductionProblem;
import org.uma.jmetal.problem.multiobjective.dfsp.model.DhhfspFourVectorSolution;

/**
 * Exhaustive four-vector enumerator for V35-P23 exact-front verification.
 * Enumerates every legal (JS, FA, MA, WA) combination of a small instance —
 * JS as all permutations, FA as all factory vectors, MA over the stage-0
 * machine indices of the assigned factory and WA over the stage-0 eligible
 * worker indices — evaluates each candidate through the production problem's
 * own FM3 evaluate() and incrementally keeps the non-dominated set.
 * Enumeration instances pin stage-0 machines and workers to 2, so the space is
 * N! * 2^N * 2^N * 2^N (3.93M decodes for 5 jobs).
 */
final class V35ExactFrontEnumerator {
  private static final double EPSILON = 1e-9;

  private final ZhangBoCanonicalProductionProblem problem;
  private final int jobs;
  private final int factories;
  private final int[] machineCounts;
  private final int[][] eligibleWorkers;
  private long evaluated;

  private final List<double[]> front = new ArrayList<>();

  V35ExactFrontEnumerator(ZhangBoCanonicalProductionProblem problem) {
    this.problem = problem;
    this.jobs = problem.getInstance().getJobs();
    this.factories = problem.getInstance().getFactories();
    this.machineCounts = new int[factories];
    this.eligibleWorkers = new int[factories][];
    for (int f = 0; f < factories; f++) {
      machineCounts[f] = problem.getInstance().getMachineCount(f, 0);
      eligibleWorkers[f] = problem.getInstance().getEligibleWorkers(f, 0);
    }
  }

  List<double[]> enumerate() {
    int[] permutation = new int[jobs];
    for (int job = 0; job < jobs; job++) permutation[job] = job;
    heapPermutations(permutation, jobs);
    return new ArrayList<>(front);
  }

  long getEvaluatedCount() { return evaluated; }

  private void heapPermutations(int[] permutation, int size) {
    if (size == 1) {
      enumerateAssignments(permutation);
      return;
    }
    for (int i = 0; i < size; i++) {
      heapPermutations(permutation, size - 1);
      int swap = size % 2 == 0 ? i : 0;
      int temporary = permutation[swap];
      permutation[swap] = permutation[size - 1];
      permutation[size - 1] = temporary;
    }
  }

  private void enumerateAssignments(int[] permutation) {
    long factorySpace = 1L;
    for (int p = 0; p < jobs; p++) factorySpace *= factories;
    for (long factoryCode = 0; factoryCode < factorySpace; factoryCode++) {
      int[] fa = new int[jobs];
      long rest = factoryCode;
      long combinedSpace = 1L;
      for (int p = 0; p < jobs; p++) {
        fa[p] = (int) (rest % factories);
        rest /= factories;
        combinedSpace *= (long) machineCounts[fa[p]] * eligibleWorkers[fa[p]].length;
      }
      for (long combined = 0; combined < combinedSpace; combined++) {
        int[] ma = new int[jobs];
        int[] wa = new int[jobs];
        long code = combined;
        for (int p = 0; p < jobs; p++) {
          int radix = machineCounts[fa[p]] * eligibleWorkers[fa[p]].length;
          int choice = (int) (code % radix);
          code /= radix;
          ma[p] = choice / eligibleWorkers[fa[p]].length;
          wa[p] = eligibleWorkers[fa[p]][choice % eligibleWorkers[fa[p]].length];
        }
        evaluateCandidate(permutation, fa, ma, wa);
      }
    }
  }

  private void evaluateCandidate(int[] permutation, int[] fa, int[] ma, int[] wa) {
    List<Integer> js = new ArrayList<>(jobs);
    List<Integer> faList = new ArrayList<>(jobs);
    List<Integer> maList = new ArrayList<>(jobs);
    List<Integer> waList = new ArrayList<>(jobs);
    for (int p = 0; p < jobs; p++) {
      js.add(permutation[p]);
      faList.add(fa[p]);
      maList.add(ma[p]);
      waList.add(wa[p]);
    }
    DhhfspFourVectorSolution solution = new DhhfspFourVectorSolution(
        js, faList, maList, waList, problem.getMode().getSemanticTag(),
        ZhangBoCanonicalProductionProblem.NUMBER_OF_OBJECTIVES);
    problem.evaluate(solution);
    evaluated++;
    double[] point = {solution.getObjective(0), solution.getObjective(1), solution.getObjective(6)};
    archive(point);
  }

  private void archive(double[] candidate) {
    for (double[] existing : front) {
      if (dominates(existing, candidate)) return;
      // Symmetric encodings decode to identical objectives; keep unique points.
      if (equal(existing, candidate)) return;
    }
    List<double[]> survivors = new ArrayList<>();
    for (double[] existing : front) {
      if (!dominates(candidate, existing)) survivors.add(existing);
    }
    survivors.add(candidate);
    front.clear();
    front.addAll(survivors);
  }

  /** Strict dominance with a small tolerance for accumulated FP error. */
  static boolean dominates(double[] a, double[] b) {
    boolean anyBetter = false;
    for (int i = 0; i < a.length; i++) {
      if (a[i] > b[i] + EPSILON) return false;
      if (a[i] < b[i] - EPSILON) anyBetter = true;
    }
    return anyBetter;
  }

  private static boolean equal(double[] a, double[] b) {
    for (int i = 0; i < a.length; i++) {
      if (Math.abs(a[i] - b[i]) > EPSILON) return false;
    }
    return true;
  }
}
