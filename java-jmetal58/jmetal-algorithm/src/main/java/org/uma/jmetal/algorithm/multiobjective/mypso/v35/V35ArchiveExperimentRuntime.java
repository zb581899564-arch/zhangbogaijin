package org.uma.jmetal.algorithm.multiobjective.mypso.v35;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import org.uma.jmetal.algorithm.multiobjective.mypso.zhangbo.ZhangBoIncrementalParetoArchive;
import org.uma.jmetal.algorithm.multiobjective.mypso.zhangbo.ZhangBoQgController;
import org.uma.jmetal.algorithm.multiobjective.mypso.zhangbo.ZhangBoSubSwarm;
import org.uma.jmetal.solution.PermutationSolution;

/** Runtime installed only by the dedicated dormant-archive experiment runner. */
public final class V35ArchiveExperimentRuntime implements Serializable {
  private static final long serialVersionUID = 1L;
  private final V35ArchiveExperimentProfile profile;
  private final V35PassiveEvaluationArchive observedArchive;
  private final V35ArchiveAuditLedger ledger = new V35ArchiveAuditLedger();

  V35ArchiveExperimentRuntime(V35ArchiveExperimentProfile profile,
      V35PassiveEvaluationArchive observedArchive) {
    if (profile == null) throw new IllegalArgumentException("profile");
    if (observedArchive == null) throw new IllegalArgumentException("observedArchive");
    this.profile = profile;
    this.observedArchive = observedArchive;
  }

  public List<PermutationSolution<Integer>> teacherCandidates(
      List<PermutationSolution<Integer>> fullCandidates) {
    if (!profile.getConfiguration().usesTeacherView()) return fullCandidates;
    return V35DeterministicObjectiveSubsetter.selectSolutions(fullCandidates,
        profile.getConfiguration().getTeacherViewCapacity());
  }

  public void observeTeacherSelection(long fe, long generation, ZhangBoSubSwarm group,
      ZhangBoQgController.Selection selection,
      List<PermutationSolution<Integer>> fullCandidates,
      List<PermutationSolution<Integer>> viewCandidates, long copyNanos,
      long sanitationNanos, long viewBuildNanos, long selectionNanos) {
    ledger.observeTeacherSelection(fe, generation, group, selection.getAction(), fullCandidates,
        viewCandidates, selection.getLeader(), copyNanos, sanitationNanos,
        viewBuildNanos, selectionNanos);
  }

  public void afterArchiveUpdate(List<PermutationSolution<Integer>> archive, long fe,
      long generation, int beforeSize, ZhangBoIncrementalParetoArchive.Update update,
      long updateStartedNanos) {
    int pruned = 0;
    if (profile.getConfiguration().boundsActiveArchive()
        && archive.size() > profile.getConfiguration().getActiveArchiveCapacity()) {
      int beforePrune = archive.size();
      List<PermutationSolution<Integer>> retained =
          V35DeterministicObjectiveSubsetter.selectSolutions(archive,
              profile.getConfiguration().getActiveArchiveCapacity());
      archive.clear();
      archive.addAll(new ArrayList<>(retained));
      pruned = beforePrune - archive.size();
    }
    ledger.observeArchiveUpdate(fe, generation, beforeSize, archive,
        observedArchive.size(), update, pruned, System.nanoTime() - updateStartedNanos);
  }

  public V35ArchiveExperimentArtifacts finish(List<PermutationSolution<Integer>> decisionFront,
      List<PermutationSolution<Integer>> observedFront) {
    return new V35ArchiveExperimentArtifacts(profile, ledger, decisionFront, observedFront);
  }

  public V35ArchiveExperimentProfile getProfile() { return profile; }
}
