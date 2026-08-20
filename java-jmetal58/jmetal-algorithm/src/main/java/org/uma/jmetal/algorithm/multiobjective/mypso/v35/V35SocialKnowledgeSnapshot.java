package org.uma.jmetal.algorithm.multiobjective.mypso.v35;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.uma.jmetal.algorithm.multiobjective.mypso.zhangbo.ZhangBoQgController;
import org.uma.jmetal.solution.PermutationSolution;

/** Frozen global nondominated social knowledge used by one Qg decision cycle. */
public final class V35SocialKnowledgeSnapshot implements Serializable {
  private static final long serialVersionUID = 1L;
  private final List<V35SocialTeacher> teachers;
  private final Map<String, PermutationSolution<Integer>> evaluatedSolutions;

  public V35SocialKnowledgeSnapshot(List<V35SocialTeacher> teachers) {
    this(teachers, Collections.<String, PermutationSolution<Integer>>emptyMap());
  }

  private V35SocialKnowledgeSnapshot(List<V35SocialTeacher> teachers,
      Map<String, PermutationSolution<Integer>> evaluatedSolutions) {
    if (teachers == null) throw new IllegalArgumentException("teachers cannot be null");
    List<V35SocialTeacher> copy = new ArrayList<>(teachers);
    copy.sort(Comparator.comparing(V35SocialTeacher::getFingerprint));
    this.teachers = Collections.unmodifiableList(copy);
    // FC-TIME-2-A2: the input map already holds the frozen candidates for this Q
    // round (prepareOriginalQg copies the archive before DSCR; the filter only
    // reads). Consumers never mutate these solutions (sanitizeOne copies the
    // replacement before caching it), so the extra deep copy is redundant —
    // semantics are unchanged and the snapshot stays immutable by contract.
    this.evaluatedSolutions = Collections.unmodifiableMap(
        new LinkedHashMap<>(evaluatedSolutions));
  }

  public static V35SocialKnowledgeSnapshot fromEvaluatedSolutions(
      List<PermutationSolution<Integer>> solutions) {
    if (solutions == null || solutions.isEmpty()) {
      throw new IllegalArgumentException("evaluated solutions must be non-empty");
    }
    List<V35SocialTeacher> teachers = new ArrayList<>();
    Map<String, PermutationSolution<Integer>> byFingerprint = new LinkedHashMap<>();
    for (PermutationSolution<Integer> solution : solutions) {
      if (solution == null || !Double.isFinite(solution.getObjective(0))
          || !Double.isFinite(solution.getObjective(1))
          || !Double.isFinite(solution.getObjective(6))) {
        throw new IllegalArgumentException("DSCR snapshot requires evaluated solutions");
      }
      String fingerprint = ZhangBoQgController.fingerprint(solution);
      teachers.add(new V35SocialTeacher(new double[] {solution.getObjective(0),
          solution.getObjective(1), solution.getObjective(6)}, fingerprint));
      if (!byFingerprint.containsKey(fingerprint)) {
        // FC-TIME-2-A2: reference, not deep copy — frozen by the caller's Q-round
        // candidate list, read-only consumers (see constructor contract above).
        byFingerprint.put(fingerprint, solution);
      }
    }
    return new V35SocialKnowledgeSnapshot(teachers, byFingerprint);
  }

  public List<V35SocialTeacher> getTeachers() { return teachers; }

  public PermutationSolution<Integer> solutionFor(String fingerprint) {
    PermutationSolution<Integer> solution = evaluatedSolutions.get(fingerprint);
    return solution == null ? null : copy(solution);
  }

  public int strictDominatorCount(double[] objectives) {
    int count = 0;
    for (V35SocialTeacher teacher : teachers) {
      if (V35DscrSanitizer.strictlyDominates(teacher.getObjectives(), objectives)) count++;
    }
    return count;
  }

  public boolean isStrictlyDominated(double[] objectives) {
    return strictDominatorCount(objectives) > 0;
  }

  @SuppressWarnings("unchecked")
  private static PermutationSolution<Integer> copy(PermutationSolution<Integer> solution) {
    return (PermutationSolution<Integer>) solution.copy();
  }
}
