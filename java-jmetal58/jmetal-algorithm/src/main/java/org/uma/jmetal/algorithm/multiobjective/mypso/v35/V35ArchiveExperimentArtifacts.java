package org.uma.jmetal.algorithm.multiobjective.mypso.v35;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.uma.jmetal.solution.PermutationSolution;

/** Immutable end-of-run archive artifacts, separated from algorithm decisions. */
public final class V35ArchiveExperimentArtifacts implements Serializable {
  private static final long serialVersionUID = 1L;
  private final String profileCanonicalText;
  private final String profileHash;
  private final String auditSummary;
  private final String auditEventsCsv;
  private final String representativeK30Csv;
  private final String sensitivityK25Csv;
  private final String sensitivityK50Csv;
  private final boolean decisionEqualsObservedAfterExactDedup;

  public V35ArchiveExperimentArtifacts(V35ArchiveExperimentProfile profile,
      V35ArchiveAuditLedger ledger, List<PermutationSolution<Integer>> decisionFront,
      List<PermutationSolution<Integer>> observedFront) {
    if (profile == null || ledger == null || decisionFront == null || observedFront == null) {
      throw new IllegalArgumentException("archive artifacts arguments");
    }
    profileCanonicalText = profile.canonicalText();
    profileHash = profile.configurationHash();
    auditEventsCsv = ledger.eventsCsv();
    List<double[]> decision = objectives(decisionFront);
    List<double[]> observed = objectives(observedFront);
    List<double[]> exactDecision = V35DeterministicObjectiveSubsetter.selectPoints(
        decision, Math.max(3, decision.size()));
    List<double[]> exactObserved = V35DeterministicObjectiveSubsetter.selectPoints(
        observed, Math.max(3, observed.size()));
    decisionEqualsObservedAfterExactDedup = keys(exactDecision).equals(keys(exactObserved));
    representativeK30Csv = V35DeterministicObjectiveSubsetter.pointsToCsv(
        V35DeterministicObjectiveSubsetter.selectPoints(exactDecision, 30));
    sensitivityK25Csv = V35DeterministicObjectiveSubsetter.pointsToCsv(
        V35DeterministicObjectiveSubsetter.selectPoints(exactDecision, 25));
    sensitivityK50Csv = V35DeterministicObjectiveSubsetter.pointsToCsv(
        V35DeterministicObjectiveSubsetter.selectPoints(exactDecision, 50));
    auditSummary = ledger.summaryText()
        + "decisionFrontSize=" + decision.size() + '\n'
        + "decisionFrontExactSize=" + exactDecision.size() + '\n'
        + "observedFrontSize=" + observed.size() + '\n'
        + "observedFrontExactSize=" + exactObserved.size() + '\n'
        + "decisionEqualsObservedAfterExactDedup="
        + decisionEqualsObservedAfterExactDedup + '\n'
        + nearestNeighborSummary(exactDecision);
  }

  private static List<double[]> objectives(List<PermutationSolution<Integer>> values) {
    List<double[]> result = new ArrayList<>();
    for (PermutationSolution<Integer> value : values) {
      result.add(new double[] {value.getObjective(0), value.getObjective(1), value.getObjective(6)});
    }
    return result;
  }

  private static Set<String> keys(List<double[]> values) {
    Set<String> result = new HashSet<>();
    for (double[] value : values) result.add(Double.toHexString(value[0]) + '|'
        + Double.toHexString(value[1]) + '|' + Double.toHexString(value[2]));
    return result;
  }

  private static String nearestNeighborSummary(List<double[]> values) {
    if (values.size() < 2) {
      return "nearestNeighborMin=NaN\nnearDuplicateRate0_01Pct=0.0\n"
          + "nearDuplicateRate0_05Pct=0.0\nnearDuplicateRate0_1Pct=0.0\n";
    }
    double[] min = {Double.POSITIVE_INFINITY, Double.POSITIVE_INFINITY, Double.POSITIVE_INFINITY};
    double[] max = {Double.NEGATIVE_INFINITY, Double.NEGATIVE_INFINITY, Double.NEGATIVE_INFINITY};
    for (double[] value : values) for (int i = 0; i < 3; i++) {
      min[i] = Math.min(min[i], value[i]); max[i] = Math.max(max[i], value[i]);
    }
    int at001 = 0, at005 = 0, at01 = 0;
    double globalMinimum = Double.POSITIVE_INFINITY;
    for (int left = 0; left < values.size(); left++) {
      double nearest = Double.POSITIVE_INFINITY;
      for (int right = 0; right < values.size(); right++) if (left != right) {
        double sum = 0.0;
        for (int i = 0; i < 3; i++) {
          double range = Math.max(max[i] - min[i], 1.0e-12);
          double delta = (values.get(left)[i] - values.get(right)[i]) / range;
          sum += delta * delta;
        }
        nearest = Math.min(nearest, Math.sqrt(sum));
      }
      globalMinimum = Math.min(globalMinimum, nearest);
      if (nearest <= 0.0001) at001++;
      if (nearest <= 0.0005) at005++;
      if (nearest <= 0.001) at01++;
    }
    return "nearestNeighborMin=" + globalMinimum + '\n'
        + "nearDuplicateRate0_01Pct=" + ((double) at001 / values.size()) + '\n'
        + "nearDuplicateRate0_05Pct=" + ((double) at005 / values.size()) + '\n'
        + "nearDuplicateRate0_1Pct=" + ((double) at01 / values.size()) + '\n';
  }

  public String getProfileCanonicalText() { return profileCanonicalText; }
  public String getProfileHash() { return profileHash; }
  public String getAuditSummary() { return auditSummary; }
  public String getAuditEventsCsv() { return auditEventsCsv; }
  public String getRepresentativeK30Csv() { return representativeK30Csv; }
  public String getSensitivityK25Csv() { return sensitivityK25Csv; }
  public String getSensitivityK50Csv() { return sensitivityK50Csv; }
  public boolean isDecisionEqualsObservedAfterExactDedup() {
    return decisionEqualsObservedAfterExactDedup;
  }
}
