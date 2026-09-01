package org.uma.jmetal.algorithm.multiobjective.mypso.v35;

import java.nio.file.Path;
import java.util.List;
import org.uma.jmetal.problem.Problem;
import org.uma.jmetal.solution.PermutationSolution;

/**
 * The sole in-process entry for dormant ND0--ND4 archive experiments.
 * Production and formal runners do not expose an archive-mode parameter.
 */
public final class V35ArchiveExperimentRunner {
  public static final String VERSION = "v35-nd-archive-dormant-runner-v1";

  private V35ArchiveExperimentRunner() { }

  public static V35FairRunner.RunRecord run(V35ArchiveExperimentProfile profile,
      Problem<PermutationSolution<Integer>> problem,
      List<PermutationSolution<Integer>> initialPopulation,
      int maxEvaluations, long seed) {
    if (profile == null) throw new IllegalArgumentException("profile");
    V35ProductionConfiguration configuration =
        V35FinalAblationProfile.configurationFor(
            V35FinalAblationProfile.Arm.A4_BUDGET_AWARE_CATA,
            seed, initialPopulation.size(), maxEvaluations);
    V35FinalAblationProfile.validate(
        V35FinalAblationProfile.Arm.A4_BUDGET_AWARE_CATA, configuration);
    V35FairRunner.RunRecord record = V35FairRunner.runArchiveExperiment(
        V35FairRunner.Mode.V35_FULL_POOL_OFF,
        problem, initialPopulation, maxEvaluations, seed,
        V35BottleneckDiagnosisConfiguration.fullMaskNoShadow(), configuration, profile);
    if (profile.isControl() && "COMPLETED".equals(record.getStatus())
        && (record.getArchiveExperimentArtifacts() == null
            || !record.getArchiveExperimentArtifacts()
                .isDecisionEqualsObservedAfterExactDedup())) {
      throw new IllegalStateException(
          "ND0 decision-front differs from observed-full-front after exact dedup");
    }
    return record;
  }

  /**
   * Observation-only Gate-A entry that preserves a failing decision/observed
   * comparison as evidence.  It never relaxes the normal {@link #run} gate and
   * is intentionally restricted to ND0.
   */
  public static V35FairRunner.RunRecord runGateAudit(
      Problem<PermutationSolution<Integer>> problem,
      List<PermutationSolution<Integer>> initialPopulation,
      int maxEvaluations, long seed) {
    V35ProductionConfiguration configuration =
        V35FinalAblationProfile.configurationFor(
            V35FinalAblationProfile.Arm.A4_BUDGET_AWARE_CATA,
            seed, initialPopulation.size(), maxEvaluations);
    V35FinalAblationProfile.validate(
        V35FinalAblationProfile.Arm.A4_BUDGET_AWARE_CATA, configuration);
    return V35FairRunner.runArchiveExperiment(
        V35FairRunner.Mode.V35_FULL_POOL_OFF, problem, initialPopulation,
        maxEvaluations, seed, V35BottleneckDiagnosisConfiguration.fullMaskNoShadow(),
        configuration, V35ArchiveExperimentProfile.ND0_FULL_ARCHIVE_CONTROL);
  }

  public static void writeRecord(V35FairRunner.RunRecord record,
      V35ArchiveExperimentProfile profile, Path directory,
      long seed, int populationSize, int requestedMaxEvaluations) throws java.io.IOException {
    if (profile == null) throw new IllegalArgumentException("profile");
    String configuration = "archiveRunnerVersion=" + VERSION + '\n'
        + V35FinalAblationProfile.canonicalTextFor(
            V35FinalAblationProfile.Arm.A4_BUDGET_AWARE_CATA,
            seed, populationSize, requestedMaxEvaluations)
        + profile.canonicalText();
    V35FairRunner.writeRecord(record, directory, configuration);
  }
}
