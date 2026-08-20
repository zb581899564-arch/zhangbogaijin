package org.uma.jmetal.algorithm.multiobjective.hmopsoqgs;

import org.uma.jmetal.problem.multiobjective.dfsp.model.DhhfspFourVectorSolution;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/** M3 decomposition; each group receives independent copies and may share source identities. */
public final class SubSwarmDecomposer implements Serializable {
  private static final long serialVersionUID = 1L;
  private final PddrFf pddr = new PddrFf();

  public Map<SubSwarm, List<DhhfspFourVectorSolution>> decompose(
      List<DhhfspFourVectorSolution> population, int[] sizes) {
    if (sizes == null || sizes.length != 4) throw new IllegalArgumentException("four sizes");
    Map<SubSwarm, List<DhhfspFourVectorSolution>> result = new EnumMap<>(SubSwarm.class);
    SubSwarm[] groups = SubSwarm.values();
    for (int group = 0; group < 3; group++) {
      final int objective = group;
      List<DhhfspFourVectorSolution> sorted = new ArrayList<>(population);
      Collections.sort(sorted, new Comparator<DhhfspFourVectorSolution>() {
        @Override public int compare(DhhfspFourVectorSolution left, DhhfspFourVectorSolution right) {
          int value = Double.compare(left.getObjective(objective), right.getObjective(objective));
          if (value == 0) value = PddrFf.fingerprint(left).compareTo(PddrFf.fingerprint(right));
          return value;
        }
      });
      result.put(groups[group], copyPrefix(sorted, sizes[group]));
    }
    result.put(SubSwarm.G4_CENTER, pddr.select(population, sizes[3]));
    return result;
  }

  private static List<DhhfspFourVectorSolution> copyPrefix(
      List<DhhfspFourVectorSolution> source, int size) {
    if (size > source.size()) throw new IllegalArgumentException("sub-swarm exceeds population");
    List<DhhfspFourVectorSolution> result = new ArrayList<>();
    for (int index = 0; index < size; index++) result.add(source.get(index).copy());
    return result;
  }
}
