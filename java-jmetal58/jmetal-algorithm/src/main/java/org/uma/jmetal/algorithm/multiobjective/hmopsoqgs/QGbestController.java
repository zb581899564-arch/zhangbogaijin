package org.uma.jmetal.algorithm.multiobjective.hmopsoqgs;

import org.uma.jmetal.problem.multiobjective.dfsp.model.DhhfspFourVectorSolution;
import org.uma.jmetal.util.pseudorandom.PseudoRandomGenerator;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/** Original 2-state/3-action Q-gbest controller, one table for each sub-swarm. */
public final class QGbestController implements Serializable {
  private static final long serialVersionUID = 1L;
  private final Map<SubSwarm, double[][]> tables = new EnumMap<>(SubSwarm.class);
  private final Map<SubSwarm, DhhfspFourVectorSolution> previous = new EnumMap<>(SubSwarm.class);
  private final Map<SubSwarm, DhhfspFourVectorSolution> historical = new EnumMap<>(SubSwarm.class);
  private final PseudoRandomGenerator random;
  private final double epsilon;
  private final double alpha;
  private final double gamma;

  public QGbestController(
      PseudoRandomGenerator random, double epsilon, double alpha, double gamma) {
    if (random == null) throw new IllegalArgumentException("random");
    this.random = random;
    this.epsilon = epsilon;
    this.alpha = alpha;
    this.gamma = gamma;
    for (SubSwarm group : SubSwarm.values()) tables.put(group, new double[2][3]);
  }

  public int selectAction(SubSwarm group, int state) {
    if (state < 0 || state > 1) throw new IllegalArgumentException("state");
    if (random.nextDouble() < epsilon) {
      double[] values = tables.get(group)[state];
      int best = 0;
      for (int action = 1; action < values.length; action++) {
        if (values[action] > values[best]) best = action;
      }
      return best;
    }
    return random.nextInt(0, 2);
  }

  public DhhfspFourVectorSolution selectLeader(
      SubSwarm group, int action, List<DhhfspFourVectorSolution> gbestSet) {
    if (gbestSet == null || gbestSet.isEmpty()) throw new IllegalArgumentException("gbestSet");
    DhhfspFourVectorSolution selected;
    if (action == 0 && previous.containsKey(group)) {
      selected = previous.get(group);
    } else if (action == 1 && historical.containsKey(group)) {
      selected = historical.get(group);
    } else {
      DhhfspFourVectorSolution left = gbestSet.get(random.nextInt(0, gbestSet.size() - 1));
      DhhfspFourVectorSolution right = gbestSet.get(random.nextInt(0, gbestSet.size() - 1));
      selected = better(group, left, right, gbestSet);
    }
    previous.put(group, selected.copy());
    DhhfspFourVectorSolution best = historical.get(group);
    if (best == null || better(group, selected, best, gbestSet) == selected) {
      historical.put(group, selected.copy());
    }
    return selected.copy();
  }

  public void initialize(SubSwarm group, List<DhhfspFourVectorSolution> gbestSet) {
    if (!previous.containsKey(group)) selectLeader(group, 2, gbestSet);
  }

  /** Delta >= 0 is paper state 0, negative delta is state 1. */
  public static int stateFor(double delta) { return delta >= 0.0 ? 0 : 1; }

  public void update(SubSwarm group, int state, int action, double reward, int nextState) {
    double[][] q = tables.get(group);
    double maximum = q[nextState][0];
    for (int candidate = 1; candidate < 3; candidate++) {
      maximum = Math.max(maximum, q[nextState][candidate]);
    }
    q[state][action] = q[state][action]
        + alpha * (reward + gamma * maximum - q[state][action]);
  }

  public double[][] getTable(SubSwarm group) {
    double[][] source = tables.get(group);
    return new double[][] {source[0].clone(), source[1].clone()};
  }

  public String toCanonicalText() {
    StringBuilder builder = new StringBuilder();
    for (SubSwarm group : SubSwarm.values()) {
      double[][] q = tables.get(group);
      builder.append(group).append('=')
          .append(java.util.Arrays.toString(q[0])).append('|')
          .append(java.util.Arrays.toString(q[1])).append('\n');
    }
    return builder.toString();
  }

  public static double reward(
      SubSwarm group, List<DhhfspFourVectorSolution> before,
      List<DhhfspFourVectorSolution> after) {
    if (before.size() != after.size() || before.isEmpty()) {
      throw new IllegalArgumentException("paired non-empty groups required");
    }
    double total = 0.0;
    for (int index = 0; index < before.size(); index++) {
      if (group.isBoundary()) {
        total += before.get(index).getObjective(group.getObjective())
            - after.get(index).getObjective(group.getObjective());
      } else {
        double relative = 0.0;
        for (int objective = 0; objective < 3; objective++) {
          double base = Math.max(Math.abs(before.get(index).getObjective(objective)), 1.0e-12);
          relative += (before.get(index).getObjective(objective)
              - after.get(index).getObjective(objective)) / base;
        }
        total += relative;
      }
    }
    return total / before.size();
  }

  private static DhhfspFourVectorSolution better(
      SubSwarm group, DhhfspFourVectorSolution left, DhhfspFourVectorSolution right,
      List<DhhfspFourVectorSolution> reference) {
    int comparison;
    if (group.isBoundary()) {
      comparison = Double.compare(
          left.getObjective(group.getObjective()), right.getObjective(group.getObjective()));
    } else {
      PddrFf pddr = new PddrFf();
      comparison = Double.compare(pddr.score(left, reference), pddr.score(right, reference));
    }
    if (comparison == 0) {
      comparison = PddrFf.fingerprint(left).compareTo(PddrFf.fingerprint(right));
    }
    return comparison <= 0 ? left : right;
  }
}
