package org.uma.jmetal.algorithm.multiobjective.hmopsoqgs;

import org.uma.jmetal.problem.multiobjective.dfsp.model.DhhfspFourVectorSolution;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Three-objective strict-Pareto PDDR-FF selection used by the published baseline. */
public final class PddrFf implements Serializable {
  private static final long serialVersionUID = 1L;

  public double score(
      DhhfspFourVectorSolution candidate, List<DhhfspFourVectorSolution> reference) {
    int dominatedBy = 0;
    int dominates = 0;
    for (DhhfspFourVectorSolution other : reference) {
      if (candidate == other) continue;
      if (dominates(other, candidate)) dominatedBy++;
      if (dominates(candidate, other)) dominates++;
    }
    return dominatedBy + 1.0 / (dominates + 1.0);
  }

  public List<DhhfspFourVectorSolution> select(
      List<DhhfspFourVectorSolution> source, int size) {
    if (size < 0 || size > source.size()) throw new IllegalArgumentException("selection size");
    final List<DhhfspFourVectorSolution> reference = new ArrayList<>(source);
    List<DhhfspFourVectorSolution> sorted = new ArrayList<>(source);
    Collections.sort(sorted, new Comparator<DhhfspFourVectorSolution>() {
      @Override public int compare(DhhfspFourVectorSolution left, DhhfspFourVectorSolution right) {
        int value = Double.compare(score(left, reference), score(right, reference));
        if (value == 0) value = fingerprint(left).compareTo(fingerprint(right));
        return value;
      }
    });
    List<DhhfspFourVectorSolution> selected = new ArrayList<>();
    for (int index = 0; index < size; index++) selected.add(sorted.get(index).copy());
    return selected;
  }

  public List<DhhfspFourVectorSolution> nonDominated(
      List<DhhfspFourVectorSolution> source) {
    Map<String, DhhfspFourVectorSolution> result = new LinkedHashMap<>();
    for (DhhfspFourVectorSolution candidate : source) {
      boolean dominated = false;
      for (DhhfspFourVectorSolution other : source) {
        if (candidate != other && dominates(other, candidate)) {
          dominated = true;
          break;
        }
      }
      if (!dominated) result.put(fingerprint(candidate), candidate.copy());
    }
    return new ArrayList<>(result.values());
  }

  public static boolean dominates(
      DhhfspFourVectorSolution left, DhhfspFourVectorSolution right) {
    boolean strict = false;
    for (int objective = 0; objective < 3; objective++) {
      if (left.getObjective(objective) > right.getObjective(objective)) return false;
      if (left.getObjective(objective) < right.getObjective(objective)) strict = true;
    }
    return strict;
  }

  public static String fingerprint(DhhfspFourVectorSolution solution) {
    return "JS=" + solution.getJobSequence()
        + "|FA=" + solution.getFactoryAssignments()
        + "|MA=" + solution.getMachineAssignments()
        + "|WA=" + solution.getWorkerAssignments();
  }
}
