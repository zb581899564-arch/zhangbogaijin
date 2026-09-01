package org.uma.jmetal.algorithm.multiobjective.mypso.v35;

import java.io.Serializable;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.uma.jmetal.algorithm.multiobjective.mypso.zhangbo.ZhangBoEventLog;
import org.uma.jmetal.algorithm.multiobjective.mypso.zhangbo.ZhangBoIncrementalParetoArchive;
import org.uma.jmetal.algorithm.multiobjective.mypso.zhangbo.ZhangBoQgController;
import org.uma.jmetal.algorithm.multiobjective.mypso.zhangbo.ZhangBoSubSwarm;
import org.uma.jmetal.algorithm.multiobjective.mypso.zhangbo.ZhangBoSubSwarmSemantics;
import org.uma.jmetal.solution.PermutationSolution;

/** Observation-only ledger for archive size, pruning and Qg teacher exposure. */
public final class V35ArchiveAuditLedger implements Serializable {
  private static final long serialVersionUID = 1L;
  private static final double EPSILON = 1.0e-12;

  private static final String EVENT_HEADER =
      "event,fe,generation,group,action,teacherSource,teacherCache,"
          + "beforeDecisionSize,afterDecisionSize,"
          + "observedFullSize,viewSize,added,"
          + "dominatedReject,equalReject,removedDominated,activePruned,selectedFingerprint,"
          + "bestEligibleDirectionalScore,selectedDirectionalScore,directionalRegret,"
          + "teacherExposure,elapsedNanos,heapUsedBytes,"
          + "normalizedNearestNeighborDistance,nearDuplicateRate0_01Pct,"
          + "nearDuplicateRate0_05Pct,nearDuplicateRate0_1Pct";
  private final ZhangBoEventLog events = new ZhangBoEventLog();
  private long archiveAddCalls;
  private long candidateAdds;
  private long dominatedRejects;
  private long equalRejects;
  private long removedDominated;
  private long activePruned;
  private long teacherViewCalls;
  private long teacherFullCandidates;
  private long teacherViewCandidates;
  private long archiveCopyItems;
  private long archiveScanCalls;
  private long archiveItemsVisited;
  private long archiveUpdateNanos;
  private long archiveCopyNanos;
  private long archiveSanitationNanos;
  private long teacherViewBuildNanos;
  private long teacherSelectionNanos;
  private long peakHeapUsedBytes;
  private double teacherRegretTotal;
  private long teacherRegretCount;
  private double teacherRegretMaximum;
  private final Map<String, Long> teacherExposure = new HashMap<>();
  private long nextArchiveCheckpointFe;
  private double latestNearestNeighbor = Double.NaN;
  private double latestNearDuplicateRate001;
  private double latestNearDuplicateRate005;
  private double latestNearDuplicateRate01;

  public void observeArchiveUpdate(long fe, long generation, int beforeSize,
      List<PermutationSolution<Integer>> decisionArchive, int observedSize,
      ZhangBoIncrementalParetoArchive.Update update, int pruned, long elapsedNanos) {
    if (update == null) throw new IllegalArgumentException("update");
    if (decisionArchive == null) throw new IllegalArgumentException("decisionArchive");
    archiveAddCalls++;
    archiveScanCalls++;
    archiveItemsVisited += Math.max(0, beforeSize);
    archiveUpdateNanos += Math.max(0L, elapsedNanos);
    switch (update.getDisposition()) {
      case ADDED: candidateAdds++; break;
      case REJECTED_EQUAL: equalRejects++; break;
      case REJECTED_DOMINATED: dominatedRejects++; break;
      default: throw new IllegalStateException("unknown disposition");
    }
    removedDominated += update.getRemovedDominated();
    activePruned += Math.max(0, pruned);
    sampleHeap();
    if (fe >= nextArchiveCheckpointFe) {
      double[] cardinality = cardinality(decisionArchive);
      latestNearestNeighbor = cardinality[0];
      latestNearDuplicateRate001 = cardinality[1];
      latestNearDuplicateRate005 = cardinality[2];
      latestNearDuplicateRate01 = cardinality[3];
      append("ARCHIVE", fe, generation, "NA", -1, "NOT_APPLICABLE", "NOT_APPLICABLE",
          beforeSize, decisionArchive.size(),
          observedSize, 0,
          update.getDisposition() == ZhangBoIncrementalParetoArchive.Disposition.ADDED,
          update.getDisposition() == ZhangBoIncrementalParetoArchive.Disposition.REJECTED_DOMINATED,
          update.getDisposition() == ZhangBoIncrementalParetoArchive.Disposition.REJECTED_EQUAL,
          update.getRemovedDominated(), pruned, "NA", Double.NaN, Double.NaN,
          Double.NaN, 0L, elapsedNanos, cardinality);
      nextArchiveCheckpointFe = ((fe / 1000L) + 1L) * 1000L;
    }
  }

  public void observeTeacherSelection(long fe, long generation, ZhangBoSubSwarm group,
      int action, List<PermutationSolution<Integer>> fullCandidates,
      List<PermutationSolution<Integer>> viewCandidates,
      PermutationSolution<Integer> selected, long copyNanos, long sanitationNanos,
      long viewBuildNanos, long selectionNanos) {
    if (group == null || fullCandidates == null || viewCandidates == null || selected == null) {
      throw new IllegalArgumentException("teacher observation arguments");
    }
    teacherViewCalls++;
    teacherFullCandidates += fullCandidates.size();
    teacherViewCandidates += viewCandidates.size();
    archiveCopyItems += fullCandidates.size();
    archiveScanCalls++;
    archiveItemsVisited += fullCandidates.size();
    archiveCopyNanos += Math.max(0L, copyNanos);
    archiveSanitationNanos += Math.max(0L, sanitationNanos);
    teacherViewBuildNanos += Math.max(0L, viewBuildNanos);
    teacherSelectionNanos += Math.max(0L, selectionNanos);
    DirectionalScores scores = directionalScores(group, fullCandidates, selected);
    double regret = action == 2 ? scores.regret : Double.NaN;
    if (Double.isFinite(regret)) {
      teacherRegretTotal += regret;
      teacherRegretCount++;
      teacherRegretMaximum = Math.max(teacherRegretMaximum, regret);
    }
    String fingerprint = ZhangBoQgController.fingerprint(selected);
    Long oldExposure = teacherExposure.get(fingerprint);
    long exposure = oldExposure == null ? 1L : oldExposure + 1L;
    teacherExposure.put(fingerprint, exposure);
    sampleHeap();
    String cache = action == 0 ? "PREVIOUS" : action == 1 ? "HISTORICAL" : "CURRENT";
    append("TEACHER", fe, generation, group.name(), action, "QG_SOCIAL", cache,
        fullCandidates.size(),
        fullCandidates.size(), -1, viewCandidates.size(), false, false, false, 0, 0,
        fingerprint, scores.best, scores.selected, regret, exposure, selectionNanos,
        new double[] {Double.NaN, Double.NaN, Double.NaN, Double.NaN});
  }

  private static DirectionalScores directionalScores(ZhangBoSubSwarm group,
      List<PermutationSolution<Integer>> candidates, PermutationSolution<Integer> selected) {
    if (candidates.isEmpty()) return new DirectionalScores(Double.NaN, Double.NaN, Double.NaN);
    double best = Double.POSITIVE_INFINITY;
    double actual;
    if (ZhangBoSubSwarmSemantics.isBoundary(group)) {
      int objective = ZhangBoSubSwarmSemantics.objectiveIndex(group);
      for (PermutationSolution<Integer> candidate : candidates) {
        best = Math.min(best, candidate.getObjective(objective));
      }
      actual = selected.getObjective(objective);
    } else {
      double[] min = {Double.POSITIVE_INFINITY, Double.POSITIVE_INFINITY, Double.POSITIVE_INFINITY};
      double[] max = {Double.NEGATIVE_INFINITY, Double.NEGATIVE_INFINITY, Double.NEGATIVE_INFINITY};
      int[] objectives = {0, 1, 6};
      for (PermutationSolution<Integer> candidate : candidates) for (int i = 0; i < 3; i++) {
        min[i] = Math.min(min[i], candidate.getObjective(objectives[i]));
        max[i] = Math.max(max[i], candidate.getObjective(objectives[i]));
      }
      for (PermutationSolution<Integer> candidate : candidates) {
        best = Math.min(best, balancedScore(candidate, objectives, min, max));
      }
      actual = balancedScore(selected, objectives, min, max);
    }
    return new DirectionalScores(best, actual,
        Math.max(0.0, (actual - best) / Math.max(Math.abs(best), EPSILON)));
  }

  private static final class DirectionalScores {
    private final double best;
    private final double selected;
    private final double regret;
    private DirectionalScores(double best, double selected, double regret) {
      this.best = best;
      this.selected = selected;
      this.regret = regret;
    }
  }

  private static double balancedScore(PermutationSolution<Integer> value, int[] objectives,
      double[] min, double[] max) {
    double score = 0.0;
    for (int i = 0; i < 3; i++) {
      double range = Math.max(max[i] - min[i], EPSILON);
      score = Math.max(score, (value.getObjective(objectives[i]) - min[i]) / range);
    }
    return score;
  }

  private void sampleHeap() {
    Runtime runtime = Runtime.getRuntime();
    peakHeapUsedBytes = Math.max(peakHeapUsedBytes,
        runtime.totalMemory() - runtime.freeMemory());
  }

  private void append(String event, long fe, long generation, String group, int action,
      String teacherSource, String teacherCache,
      int beforeSize, int afterSize, int observedSize, int viewSize, boolean added,
      boolean dominatedReject, boolean equalReject, int removed, int pruned,
      String selected, double bestScore, double selectedScore, double regret,
      long exposure, long elapsedNanos, double[] cardinality) {
    StringBuilder row = new StringBuilder();
    row.append(event).append(',').append(fe).append(',').append(generation).append(',')
        .append(group).append(',').append(action).append(',').append(teacherSource).append(',')
        .append(teacherCache).append(',').append(beforeSize).append(',')
        .append(afterSize).append(',').append(observedSize).append(',').append(viewSize)
        .append(',').append(added).append(',')
        .append(dominatedReject).append(',').append(equalReject).append(',').append(removed)
        .append(',').append(pruned).append(',').append(selected).append(',').append(bestScore)
        .append(',').append(selectedScore).append(',').append(regret).append(',')
        .append(exposure).append(',').append(elapsedNanos).append(',')
        .append(peakHeapUsedBytes).append(',').append(cardinality[0]).append(',')
        .append(cardinality[1]).append(',').append(cardinality[2]).append(',')
        .append(cardinality[3]);
    events.add(row.toString());
  }

  private static double[] cardinality(List<PermutationSolution<Integer>> values) {
    if (values.size() < 2) return new double[] {Double.NaN, 0.0, 0.0, 0.0};
    int[] objectives = {0, 1, 6};
    double[] min = {Double.POSITIVE_INFINITY, Double.POSITIVE_INFINITY,
        Double.POSITIVE_INFINITY};
    double[] max = {Double.NEGATIVE_INFINITY, Double.NEGATIVE_INFINITY,
        Double.NEGATIVE_INFINITY};
    for (PermutationSolution<Integer> value : values) for (int index = 0; index < 3; index++) {
      min[index] = Math.min(min[index], value.getObjective(objectives[index]));
      max[index] = Math.max(max[index], value.getObjective(objectives[index]));
    }
    int at001 = 0, at005 = 0, at01 = 0;
    double globalMinimum = Double.POSITIVE_INFINITY;
    for (int left = 0; left < values.size(); left++) {
      double nearest = Double.POSITIVE_INFINITY;
      for (int right = 0; right < values.size(); right++) if (left != right) {
        double sum = 0.0;
        for (int index = 0; index < 3; index++) {
          double range = Math.max(max[index] - min[index], EPSILON);
          double delta = (values.get(left).getObjective(objectives[index])
              - values.get(right).getObjective(objectives[index])) / range;
          sum += delta * delta;
        }
        nearest = Math.min(nearest, Math.sqrt(sum));
      }
      globalMinimum = Math.min(globalMinimum, nearest);
      if (nearest <= 0.0001) at001++;
      if (nearest <= 0.0005) at005++;
      if (nearest <= 0.001) at01++;
    }
    return new double[] {globalMinimum, (double) at001 / values.size(),
        (double) at005 / values.size(), (double) at01 / values.size()};
  }

  public String eventsCsv() {
    StringBuilder result = new StringBuilder(EVENT_HEADER).append('\n');
    for (String event : events.snapshot()) result.append(event).append('\n');
    return result.toString();
  }

  public String summaryText() {
    return "archiveAddCalls=" + archiveAddCalls + '\n'
        + "candidateAdds=" + candidateAdds + '\n'
        + "dominatedRejects=" + dominatedRejects + '\n'
        + "equalObjectiveRejects=" + equalRejects + '\n'
        + "removedDominated=" + removedDominated + '\n'
        + "activeArchivePruned=" + activePruned + '\n'
        + "teacherViewCalls=" + teacherViewCalls + '\n'
        + "teacherFullCandidates=" + teacherFullCandidates + '\n'
        + "teacherViewCandidates=" + teacherViewCandidates + '\n'
        + "archiveCopyItems=" + archiveCopyItems + '\n'
        + "archiveScanCalls=" + archiveScanCalls + '\n'
        + "archiveItemsVisited=" + archiveItemsVisited + '\n'
        + "teacherDirectionalRegretMean="
        + (teacherRegretCount == 0L ? Double.NaN : teacherRegretTotal / teacherRegretCount) + '\n'
        + "teacherDirectionalRegretMax="
        + (teacherRegretCount == 0L ? Double.NaN : teacherRegretMaximum) + '\n'
        + "archiveUpdateNanos=" + archiveUpdateNanos + '\n'
        + "archiveCopyNanos=" + archiveCopyNanos + '\n'
        + "archiveSanitationNanos=" + archiveSanitationNanos + '\n'
        + "teacherViewBuildNanos=" + teacherViewBuildNanos + '\n'
        + "teacherSelectionNanos=" + teacherSelectionNanos + '\n'
        + "teacherPipelineNanos=" + (archiveCopyNanos + archiveSanitationNanos
            + teacherViewBuildNanos + teacherSelectionNanos) + '\n'
        + "auditEventsTotal=" + events.getTotalCount() + '\n'
        + "auditEventsRetained=" + events.size() + '\n'
        + "auditEventStreamHash=" + events.rollingSha256() + '\n'
        + "latestNormalizedNearestNeighborDistance=" + latestNearestNeighbor + '\n'
        + "latestNearDuplicateRate0_01Pct=" + latestNearDuplicateRate001 + '\n'
        + "latestNearDuplicateRate0_05Pct=" + latestNearDuplicateRate005 + '\n'
        + "latestNearDuplicateRate0_1Pct=" + latestNearDuplicateRate01 + '\n'
        + "peakHeapUsedBytes=" + peakHeapUsedBytes + '\n';
  }
}
