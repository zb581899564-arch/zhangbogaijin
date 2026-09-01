package org.uma.jmetal.algorithm.multiobjective.mypso.zhangbo;

import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.junit.Test;
import org.uma.jmetal.algorithm.multiobjective.mypso.v35.V35Fc5TransferAudit;
import org.uma.jmetal.solution.PermutationSolution;

/** Unit contract for FC5-T's observer-only merge-pool ledger. */
public class V35Fc5TransferAuditTest {
  @Test
  public void recordsExactNdDirectionsSelectionAndTeacherUtilization() {
    ZhangBoTestPermutationSolution cmax = solution(1, 1.0, 10.0, 10.0);
    ZhangBoTestPermutationSolution tec = solution(2, 10.0, 1.0, 10.0);
    ZhangBoTestPermutationSolution twc = solution(3, 10.0, 10.0, 1.0);
    ZhangBoTestPermutationSolution balanced = solution(4, 2.0, 2.0, 2.0);
    // A different genotype with an exactly duplicate objective tuple must not
    // inflate the strict exact-deduplicated ND count.
    ZhangBoTestPermutationSolution duplicate = solution(5, 1.0, 10.0, 10.0);
    List<PermutationSolution<Integer>> pool = Arrays.<PermutationSolution<Integer>>asList(
        cmax, tec, twc, balanced, duplicate);
    List<List<PermutationSolution<Integer>>> histories = new ArrayList<>();
    for (PermutationSolution<Integer> value : pool) histories.add(Arrays.asList(value));
    List<ZhangBoEvaluatedPddrSelector.Candidate> selected =
        new ZhangBoEvaluatedPddrSelector().select(pool, histories,
            new ArrayList<PermutationSolution<Integer>>(),
            new ArrayList<List<PermutationSolution<Integer>>>(), 3, 1L);

    V35Fc5TransferAudit audit = new V35Fc5TransferAudit();
    audit.setEnabled(true);
    audit.setSeed(20260911L);
    List<ZhangBoEvaluatedPddrSelector.Source> sources = Arrays.asList(
        ZhangBoEvaluatedPddrSelector.Source.GLOBAL_OFFSPRING,
        ZhangBoEvaluatedPddrSelector.Source.GLOBAL_OFFSPRING,
        ZhangBoEvaluatedPddrSelector.Source.GLOBAL_OFFSPRING,
        ZhangBoEvaluatedPddrSelector.Source.GLOBAL_OFFSPRING,
        ZhangBoEvaluatedPddrSelector.Source.GLOBAL_OFFSPRING);
    audit.recordPddrRound(pool, sources, selected, 50_000L, 1, 1, 1, 1, 0);
    audit.observeTeacherUse("QG", ZhangBoSubSwarm.G1_CMAX, cmax, 50_000L, 1);
    ZhangBoTestPermutationSolution child = solution(11, 0.5, 10.0, 10.0);
    audit.observeGeneratedOffspring(solution(12, 2.0, 11.0, 11.0), cmax, child,
        "QG", ZhangBoSubSwarm.G1_CMAX);
    // Equal-genotype children can occur in distinct physical offspring calls.
    // Their observer settlement must remain FIFO rather than crediting both
    // calls to the first completed decoder evaluation.
    audit.observeGeneratedOffspring(solution(13, 2.0, 11.0, 11.0), cmax, child,
        "QG", ZhangBoSubSwarm.G1_CMAX);
    audit.observeEvaluatedCandidate(child, 50_001L);
    audit.observeArchiveWorkingGap(1, 50_001L,
        Arrays.<PermutationSolution<Integer>>asList(tec),
        Arrays.<PermutationSolution<Integer>>asList(cmax, tec));

    String rounds = audit.mergeRoundsCsv();
    assertTrue(rounds.contains("50000,5,4,4,0.04"));
    String windows = audit.windowedMergeCsv();
    assertTrue(windows.contains("20260911,50000,1"));
    String representatives = audit.representativesCsv();
    assertTrue(representatives.contains(",E_C,"));
    assertTrue(representatives.contains(",E_E,"));
    assertTrue(representatives.contains(",E_W,"));
    assertTrue(representatives.contains(",E_B,"));
    assertTrue(representatives.contains("PDDR_SCORE_RANK_NOT_SELECTED"));
    assertTrue(representatives, representatives.contains(",1,0,1,1,50001,QG,G1_CMAX"));
    assertTrue(representatives, !representatives.contains(",2,0,1,1,50001,QG,G1_CMAX"));
    audit.observeEvaluatedCandidate(child, 50_002L);
    assertTrue(audit.representativesCsv(), audit.representativesCsv()
        .contains(",1,0,1,2,50002,QG,G1_CMAX"));
    assertTrue(audit.archiveWorkingGapCsv().contains(",10.0,1.0,9.0,"));
    assertTrue(audit.summaryProperties().contains("pddrRounds=1"));
    assertTrue(audit.summaryProperties().contains("observerErrors=0"));
  }

  private static ZhangBoTestPermutationSolution solution(int variant, double cmax,
      double tec, double twc) {
    return new ZhangBoTestPermutationSolution(new int[]{0, 1},
        new int[]{variant % 2, (variant / 2) % 2},
        new int[]{variant % 3, (variant + 1) % 3},
        new int[]{variant % 2, (variant + 1) % 2}, new int[0], cmax, tec, twc)
        .withFatigue(0.2, 0.1);
  }
}
