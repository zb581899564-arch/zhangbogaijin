package org.uma.jmetal.algorithm.multiobjective.mypso.zhangbo.audit;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import org.junit.Assert;
import org.junit.Test;
import org.uma.jmetal.algorithm.multiobjective.mypso.zhangbo.ZhangBoArchiveEntry;
import org.uma.jmetal.algorithm.multiobjective.mypso.zhangbo.ZhangBoEvaluatedPddrSelector;
import org.uma.jmetal.algorithm.multiobjective.mypso.zhangbo.ZhangBoLineageMemory;
import org.uma.jmetal.algorithm.multiobjective.mypso.zhangbo.ZhangBoSubSwarm;
import org.uma.jmetal.problem.multiobjective.dfsp.model.DhhfspFourVectorSolution;
import org.uma.jmetal.solution.PermutationSolution;

public class ZhangBoCmaxAuditTest {
  @Test
  public void recordsOnlyStrictCmaxRecordsAndEmitsCheckpoints() {
    ZhangBoCmaxAudit audit = new ZhangBoCmaxAudit(1000L);
    PermutationSolution<Integer> first = solution(10.0, ZhangBoSubSwarm.G1_CMAX);
    PermutationSolution<Integer> worse = solution(12.0, ZhangBoSubSwarm.G2_TEC);
    PermutationSolution<Integer> better = solution(9.0, ZhangBoSubSwarm.G1_CMAX);

    audit.observeGenerated(100L, 1, first, ZhangBoSubSwarm.G1_CMAX,
        ZhangBoCmaxAudit.Mechanism.CFVF, ZhangBoCmaxAudit.Operator.CFVF, true);
    audit.observeGenerated(900L, 1, worse, ZhangBoSubSwarm.G2_TEC,
        ZhangBoCmaxAudit.Mechanism.CA_TA, ZhangBoCmaxAudit.Operator.O11, false);
    audit.observeGenerated(1100L, 2, better, ZhangBoSubSwarm.G1_CMAX,
        ZhangBoCmaxAudit.Mechanism.CA_TA, ZhangBoCmaxAudit.Operator.O10, true);
    audit.refreshG1(Collections.singletonList(better));
    audit.finish(2000L, Arrays.asList(first, better), Collections.singletonList(better));

    Assert.assertEquals(2, audit.getRecords().size());
    Assert.assertEquals(2, audit.getCheckpoints().size());
    Assert.assertTrue(audit.curvesCsv().contains("2000,9.000000000000,9.000000000000"));
    Assert.assertTrue(audit.recordsCsv().contains("CA_TA,O10,true"));
  }

  @Test
  public void distinguishesActualG1TeacherUseFromArchiveEligibility() {
    ZhangBoCmaxAudit audit = new ZhangBoCmaxAudit(1000L);
    PermutationSolution<Integer> record = solution(9.0, ZhangBoSubSwarm.G1_CMAX);
    PermutationSolution<Integer> other = solution(10.0, ZhangBoSubSwarm.G1_CMAX);
    other.setVariableValue(0, 1);
    other.setVariableValue(1, 0);
    audit.observeGenerated(100L, 1, record, ZhangBoSubSwarm.G1_CMAX,
        ZhangBoCmaxAudit.Mechanism.CFVF, ZhangBoCmaxAudit.Operator.CFVF, true);
    audit.observeTeacherUse(200L, 2, ZhangBoSubSwarm.G1_CMAX, record, other);
    audit.observeTeacherUse(200L, 2, ZhangBoSubSwarm.G1_CMAX, other, record);
    audit.observeTeacherUse(300L, 3, ZhangBoSubSwarm.G2_TEC, record, record);

    ZhangBoCmaxAudit.Record observed = audit.getRecords().get(0);
    Assert.assertEquals(1L, observed.getG1SocialTeacherParticleUses());
    Assert.assertEquals(1L, observed.getG1PersonalTeacherParticleUses());
    Assert.assertEquals(1, observed.getG1SocialTeacherGenerations());
    Assert.assertEquals(1, observed.getG1PersonalTeacherGenerations());
  }

  @Test
  public void disabledAuditHasNoStaticOrGlobalState() {
    ZhangBoCmaxAudit left = new ZhangBoCmaxAudit(1000L);
    ZhangBoCmaxAudit right = new ZhangBoCmaxAudit(1000L);
    left.observeGenerated(1L, 0, solution(7.0, ZhangBoSubSwarm.G1_CMAX),
        ZhangBoSubSwarm.G1_CMAX, ZhangBoCmaxAudit.Mechanism.INITIAL,
        ZhangBoCmaxAudit.Operator.INITIAL, true);
    Assert.assertEquals(1, left.getRecords().size());
    Assert.assertEquals(0, right.getRecords().size());
  }

  @Test
  public void pddrSelectionResolvesYesNoAndNeverSelectedNotSelected() {
    ZhangBoCmaxAudit audit = new ZhangBoCmaxAudit(1000L);
    PermutationSolution<Integer> a = solution(10.0, ZhangBoSubSwarm.G1_CMAX);
    PermutationSolution<Integer> b = solution(9.0, ZhangBoSubSwarm.G1_CMAX);
    b.setVariableValue(0, 1);
    b.setVariableValue(1, 0);
    PermutationSolution<Integer> c = solution(8.0, ZhangBoSubSwarm.G1_CMAX);
    c.setVariableValue(0, 0);
    c.setVariableValue(1, 1);
    audit.observeGenerated(100L, 1, a, ZhangBoSubSwarm.G1_CMAX,
        ZhangBoCmaxAudit.Mechanism.CFVF, ZhangBoCmaxAudit.Operator.CFVF, true);
    audit.observeGenerated(200L, 1, b, ZhangBoSubSwarm.G1_CMAX,
        ZhangBoCmaxAudit.Mechanism.CFVF, ZhangBoCmaxAudit.Operator.CFVF, true);
    audit.observeGenerated(300L, 1, c, ZhangBoSubSwarm.G1_CMAX,
        ZhangBoCmaxAudit.Mechanism.CFVF, ZhangBoCmaxAudit.Operator.CFVF, false);

    audit.observePddrSelection(Arrays.asList(candidate(a, 100L), candidate(b, 200L)), 1);
    audit.observePddrSelection(Collections.singletonList(candidate(a, 100L)), 2);

    // c was never PDDR-selected: NOT_SELECTED from admission, never PENDING.
    Assert.assertEquals(ZhangBoCmaxAudit.Survival.YES,
        audit.getRecords().get(0).getSurvival());
    Assert.assertEquals(ZhangBoCmaxAudit.Survival.NO,
        audit.getRecords().get(1).getSurvival());
    Assert.assertEquals(ZhangBoCmaxAudit.Survival.NOT_SELECTED,
        audit.getRecords().get(2).getSurvival());
  }

  @Test
  public void finishResolvesPendingRecordsAgainstFinalPopulation() {
    ZhangBoCmaxAudit audit = new ZhangBoCmaxAudit(1000L);
    PermutationSolution<Integer> a = solution(10.0, ZhangBoSubSwarm.G1_CMAX);
    PermutationSolution<Integer> b = solution(9.0, ZhangBoSubSwarm.G1_CMAX);
    b.setVariableValue(0, 1);
    b.setVariableValue(1, 0);
    audit.observeGenerated(100L, 1, a, ZhangBoSubSwarm.G1_CMAX,
        ZhangBoCmaxAudit.Mechanism.CFVF, ZhangBoCmaxAudit.Operator.CFVF, true);
    audit.observeGenerated(200L, 1, b, ZhangBoSubSwarm.G1_CMAX,
        ZhangBoCmaxAudit.Mechanism.CFVF, ZhangBoCmaxAudit.Operator.CFVF, true);
    audit.observePddrSelection(Collections.singletonList(candidate(a, 100L)), 1);

    audit.finish(500L, Collections.singletonList(a), Collections.singletonList(a));

    Assert.assertEquals(2L, audit.getResolvedPendingByFinish());
    Assert.assertEquals(ZhangBoCmaxAudit.Survival.YES,
        audit.getRecords().get(0).getSurvival());
    Assert.assertEquals(ZhangBoCmaxAudit.Survival.NOT_SELECTED,
        audit.getRecords().get(1).getSurvival());
    for (ZhangBoCmaxAudit.Record record : audit.getRecords()) {
      Assert.assertNotEquals(ZhangBoCmaxAudit.Survival.PENDING, record.getSurvival());
    }
    Assert.assertTrue(audit.summaryText().contains("resolvedPendingByFinish=2"));
  }

  @Test
  public void lineageArchivesMatchSha256PersonalArchive() {
    ZhangBoCmaxAudit audit = new ZhangBoCmaxAudit(1000L);
    PermutationSolution<Integer> record = solution(9.0, ZhangBoSubSwarm.G1_CMAX);
    audit.observeGenerated(100L, 1, record, ZhangBoSubSwarm.G1_CMAX,
        ZhangBoCmaxAudit.Mechanism.CFVF, ZhangBoCmaxAudit.Operator.CFVF, true);

    ZhangBoArchiveEntry entry = ZhangBoArchiveEntry.fromSolution(record,
        ZhangBoEvaluatedPddrSelector.Source.GLOBAL_OFFSPRING, 1, 100L, true);
    ZhangBoLineageMemory memory = new ZhangBoLineageMemory(
        1L, 0L, 1, 0L, 0, ZhangBoSubSwarm.G1_CMAX, Collections.singletonList(entry));
    Map<Long, ZhangBoLineageMemory> memories = new HashMap<>();
    memories.put(1L, memory);
    audit.observeLineageArchives(memories);

    Assert.assertTrue(audit.getRecords().get(0).isPersonalArchive());
  }

  @Test
  public void enteredCandidateSetFalseIsImmediatelyNotSelected() {
    ZhangBoCmaxAudit audit = new ZhangBoCmaxAudit(1000L);
    audit.observeGenerated(100L, 1, solution(9.0, ZhangBoSubSwarm.G1_CMAX),
        ZhangBoSubSwarm.G1_CMAX, ZhangBoCmaxAudit.Mechanism.CFVF,
        ZhangBoCmaxAudit.Operator.CFVF, false);
    Assert.assertEquals(ZhangBoCmaxAudit.Survival.NOT_SELECTED,
        audit.getRecords().get(0).getSurvival());
  }

  @Test
  public void teacherUseOnNonRecordLeadersIsIgnored() {
    ZhangBoCmaxAudit audit = new ZhangBoCmaxAudit(1000L);
    PermutationSolution<Integer> record = solution(9.0, ZhangBoSubSwarm.G1_CMAX);
    audit.observeGenerated(100L, 1, record, ZhangBoSubSwarm.G1_CMAX,
        ZhangBoCmaxAudit.Mechanism.CFVF, ZhangBoCmaxAudit.Operator.CFVF, true);
    PermutationSolution<Integer> stranger = solution(8.0, ZhangBoSubSwarm.G1_CMAX);
    stranger.setVariableValue(0, 1);
    stranger.setVariableValue(1, 0);
    audit.observeTeacherUse(200L, 2, ZhangBoSubSwarm.G1_CMAX, stranger, stranger);

    ZhangBoCmaxAudit.Record observed = audit.getRecords().get(0);
    Assert.assertEquals(0L, observed.getG1SocialTeacherParticleUses());
    Assert.assertEquals(0L, observed.getG1PersonalTeacherParticleUses());
  }

  @Test
  public void finishDedupesFinalCheckpoint() {
    ZhangBoCmaxAudit audit = new ZhangBoCmaxAudit(1000L);
    audit.observeGenerated(100L, 1, solution(9.0, ZhangBoSubSwarm.G1_CMAX),
        ZhangBoSubSwarm.G1_CMAX, ZhangBoCmaxAudit.Mechanism.INITIAL,
        ZhangBoCmaxAudit.Operator.INITIAL, true);
    audit.finish(1000L,
        Collections.singletonList(solution(9.0, ZhangBoSubSwarm.G1_CMAX)),
        Collections.<PermutationSolution<Integer>>emptyList());
    Assert.assertEquals(1, audit.getCheckpoints().size());
    audit.finish(1000L,
        Collections.singletonList(solution(9.0, ZhangBoSubSwarm.G1_CMAX)),
        Collections.<PermutationSolution<Integer>>emptyList());
    Assert.assertEquals(1, audit.getCheckpoints().size());
  }

  @Test
  public void bestEverTracksAllThreeObjectivesIndependentlyWithSourceFingerprints() {
    ZhangBoCmaxAudit audit = new ZhangBoCmaxAudit(1000L);
    PermutationSolution<Integer> cmaxWinner = solution(9.0, ZhangBoSubSwarm.G1_CMAX);
    PermutationSolution<Integer> tecWinner = solution(11.0, ZhangBoSubSwarm.G2_TEC);
    tecWinner.setVariableValue(0, 1);
    tecWinner.setVariableValue(1, 0);
    tecWinner.setObjective(1, 20.0);
    PermutationSolution<Integer> twcWinner = solution(12.0, ZhangBoSubSwarm.G3_TWC);
    twcWinner.setVariableValue(0, 2);
    twcWinner.setVariableValue(1, 1);
    twcWinner.setObjective(6, 21.0);

    audit.observeGenerated(100L, 1, cmaxWinner, ZhangBoSubSwarm.G1_CMAX,
        ZhangBoCmaxAudit.Mechanism.CFVF, ZhangBoCmaxAudit.Operator.CFVF, true);
    audit.observeGenerated(200L, 1, tecWinner, ZhangBoSubSwarm.G2_TEC,
        ZhangBoCmaxAudit.Mechanism.CFVF, ZhangBoCmaxAudit.Operator.CFVF, true);
    audit.observeGenerated(300L, 1, twcWinner, ZhangBoSubSwarm.G3_TWC,
        ZhangBoCmaxAudit.Mechanism.CFVF, ZhangBoCmaxAudit.Operator.CFVF, true);

    Assert.assertEquals(20.0, audit.getBestTEC(), 0.0);
    Assert.assertEquals(21.0, audit.getBestTWC(), 0.0);
    // Only the Cmax winner is a strict Cmax record; TEC/TWC best-ever must
    // still have updated from non-record candidates.
    Assert.assertEquals(1, audit.getRecords().size());
    // Independence: the three minima come from three different solutions, so
    // the three source fingerprints are pairwise distinct (no concatenation).
    String cmaxSource = audit.getBestCmaxSource();
    String tecSource = audit.getBestTECSource();
    String twcSource = audit.getBestTWCSource();
    Assert.assertTrue(!cmaxSource.isEmpty() && !tecSource.isEmpty() && !twcSource.isEmpty());
    Assert.assertTrue(!cmaxSource.equals(tecSource));
    Assert.assertTrue(!tecSource.equals(twcSource));
    Assert.assertTrue(!cmaxSource.equals(twcSource));
  }

  @Test
  public void noFakeConcatenatedSolutionIsExposed() {
    ZhangBoCmaxAudit audit = new ZhangBoCmaxAudit(1000L);
    PermutationSolution<Integer> cmaxWinner = solution(9.0, ZhangBoSubSwarm.G1_CMAX);
    PermutationSolution<Integer> tecWinner = solution(11.0, ZhangBoSubSwarm.G2_TEC);
    tecWinner.setVariableValue(0, 1);
    tecWinner.setVariableValue(1, 0);
    tecWinner.setObjective(1, 20.0);
    audit.observeGenerated(100L, 1, cmaxWinner, ZhangBoSubSwarm.G1_CMAX,
        ZhangBoCmaxAudit.Mechanism.CFVF, ZhangBoCmaxAudit.Operator.CFVF, true);
    audit.observeGenerated(200L, 1, tecWinner, ZhangBoSubSwarm.G2_TEC,
        ZhangBoCmaxAudit.Mechanism.CFVF, ZhangBoCmaxAudit.Operator.CFVF, true);
    audit.finish(1000L, Collections.singletonList(cmaxWinner),
        Collections.singletonList(cmaxWinner));

    // Per-objective scalars and fingerprints only; the curves/summary expose
    // separate columns/fields, never a combined (minCmax, minTEC, minTWC) triple.
    String curves = audit.curvesCsv();
    Assert.assertTrue(curves.contains("bestTECGlobal,bestTWCGlobal,bestTECGenerated,bestTWCGenerated"));
    String summary = audit.summaryText();
    Assert.assertTrue(summary.contains("bestCmaxGenerated="));
    Assert.assertTrue(summary.contains("bestTECGenerated="));
    Assert.assertTrue(summary.contains("bestTWCGenerated="));
    Assert.assertTrue(summary.contains("bestCmaxGlobal="));
    Assert.assertTrue(summary.contains("bestTECGlobal="));
    Assert.assertTrue(summary.contains("bestTWCGlobal="));
    Assert.assertTrue(audit.getBestCmaxSource().equals(
        org.uma.jmetal.algorithm.multiobjective.mypso.zhangbo.ZhangBoQgController
            .fingerprint(cmaxWinner)));
    Assert.assertTrue(audit.getBestTECSource().equals(
        org.uma.jmetal.algorithm.multiobjective.mypso.zhangbo.ZhangBoQgController
            .fingerprint(tecWinner)));
  }

  private static ZhangBoEvaluatedPddrSelector.Candidate candidate(
      PermutationSolution<Integer> solution, long evaluationOrdinal) {
    return ZhangBoEvaluatedPddrSelector.Candidate.ofEvaluated(solution,
        Collections.<PermutationSolution<Integer>>emptyList(),
        ZhangBoEvaluatedPddrSelector.Source.GLOBAL_OFFSPRING, 0, evaluationOrdinal, 0, 0.0);
  }

  private static PermutationSolution<Integer> solution(double cmax, ZhangBoSubSwarm group) {
    DhhfspFourVectorSolution result = new DhhfspFourVectorSolution(
        Arrays.asList(0, 1), Arrays.asList(0, 0), Arrays.asList(0, 0),
        Arrays.asList(0, 0), "fatigue_improved", 7);
    result.setObjective(0, cmax);
    result.setObjective(1, 100.0 + cmax);
    result.setObjective(6, 200.0 + cmax);
    result.setAttribute(ZhangBoSubSwarm.class, group);
    return result;
  }
}
