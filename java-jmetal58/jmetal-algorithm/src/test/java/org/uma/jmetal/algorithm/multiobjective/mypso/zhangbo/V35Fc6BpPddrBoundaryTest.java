package org.uma.jmetal.algorithm.multiobjective.mypso.zhangbo;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.junit.Test;
import org.uma.jmetal.algorithm.multiobjective.mypso.zhangbo.ZhangBoEvaluatedPddrSelector.Candidate;
import org.uma.jmetal.algorithm.multiobjective.mypso.zhangbo.ZhangBoEvaluatedPddrSelector.CandidateInput;
import org.uma.jmetal.solution.PermutationSolution;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * FC-6 BP-PDDR 保留规则单元测试。
 *
 * <p>验证 {@link ZhangBoEvaluatedPddrSelector#select} 的边界保留语义：(a) 三个独立 q==0
 * 极值（minCmax/minTEC/minTWC）在纯按分排序中被挤出时仍被保留，且只挤出同等数量的
 * 低分候选；(b) 同一解兼多个极值时去重，结果恒等于 targetSize；(c) 被支配的"全局最优点"
 * 不预留，q==0 的次优极点才预留；(d) 填充保持 (score, originalOrder) 序；(e) 多次调用
 * 结果确定；(f) 防御性 targetSize 截断。</p>
 */
public class V35Fc6BpPddrBoundaryTest {

  @Test
  public void reservesThreeBoundaryExtremesAndDisplacesOnlyOutranked() {
    // 池（顺序即 originalOrder）：A/B/C 三个独立 q==0 极值 + 100 个中心支配点 G + 100 个被 G 支配的 M。
    // G(400,4000,4000) 支配全部 100 个 M(500,5000,5000) -> G score=1/101，M score=100+1；
    // A/B/C score=1.0（q==0）。基线 top-100 = 全部 G；BP-PDDR = A/B/C + 前 97 个 G。
    List<PermutationSolution<Integer>> pool = new ArrayList<>();
    pool.add(solution(1, 100.0, 9000.0, 9000.0));     // idx0 A: min Cmax
    pool.add(solution(2, 900.0, 100.0, 9000.0));     // idx1 B: min TEC
    pool.add(solution(3, 900.0, 9000.0, 100.0));     // idx2 C: min TWC
    for (int i = 0; i < 100; i++) {
      pool.add(solution(100 + i, 400.0, 4000.0, 4000.0)); // G
    }
    for (int i = 0; i < 100; i++) {
      pool.add(solution(300 + i, 500.0, 5000.0, 5000.0)); // M
    }

    List<Candidate> selected = selectAll(pool, 100);

    assertEquals("total size must equal targetSize", 100, selected.size());
    // 前三席：minCmax / minTEC / minTWC 顺序。
    assertObjectives(selected.get(0).getSolution(), 100.0, 9000.0, 9000.0);
    assertObjectives(selected.get(1).getSolution(), 900.0, 100.0, 9000.0);
    assertObjectives(selected.get(2).getSolution(), 900.0, 9000.0, 100.0);
    // 其余 97 席全部是 G，且按 originalOrder 升序（score 相等 -> 原序破平）。
    for (int i = 3; i < 100; i++) {
      assertObjectives(selected.get(i).getSolution(), 400.0, 4000.0, 4000.0);
    }
    assertEquals("fill keeps originalOrder ascending on score ties",
        3, selected.get(3).getOriginalOrder());
    assertEquals("fill keeps originalOrder ascending on score ties",
        99, selected.get(99).getOriginalOrder());
    // 位移：最后 3 个 G（originalOrder 100..102）被挤出，M 一个都没进。
    int mCount = 0;
    boolean displacedGSelected = false;
    for (Candidate candidate : selected) {
      double cmax = candidate.getSolution().getObjective(0);
      if (cmax == 500.0) {
        mCount++;
      }
      if (candidate.getOriginalOrder() >= 100 && candidate.getOriginalOrder() <= 102) {
        displacedGSelected = true;
      }
    }
    assertEquals("no dominated M may enter", 0, mCount);
    assertTrue("only the three outranked G (originalOrder 100..102) are displaced",
        !displacedGSelected);
    List<Candidate> rerun = selectAll(pool, 100);
    assertEquals("selection must be deterministic",
        candidateKeys(selected), candidateKeys(rerun));
  }

  @Test
  public void deduplicatesWhenOneSolutionIsMultipleExtremes() {
    // 池：100 个 G + X(100,100,9000) 同时是 minCmax 与 minTEC + Y(900,9000,100) 是 minTWC
    // + 98 个 M。E = {X, Y}（X 只占一位）。
    List<PermutationSolution<Integer>> pool = new ArrayList<>();
    for (int i = 0; i < 100; i++) {
      pool.add(solution(100 + i, 400.0, 4000.0, 4000.0)); // G x100
    }
    pool.add(solution(1, 100.0, 100.0, 9000.0));   // X: minCmax + minTEC
    pool.add(solution(2, 900.0, 9000.0, 100.0));   // Y: minTWC
    for (int i = 0; i < 98; i++) {
      pool.add(solution(300 + i, 500.0, 5000.0, 5000.0)); // M x98（被 G 支配）
    }

    List<Candidate> selected = selectAll(pool, 100);
    assertEquals("total size must equal targetSize", 100, selected.size());
    int xCount = 0;
    int yCount = 0;
    int gCount = 0;
    for (Candidate candidate : selected) {
      double cmax = candidate.getSolution().getObjective(0);
      double tec = candidate.getSolution().getObjective(1);
      double twc = candidate.getSolution().getObjective(6);
      if (cmax == 100.0 && tec == 100.0 && twc == 9000.0) {
        xCount++;
      } else if (cmax == 900.0 && tec == 9000.0 && twc == 100.0) {
        yCount++;
      } else if (cmax == 400.0 && tec == 4000.0 && twc == 4000.0) {
        gCount++;
      }
    }
    assertEquals("deduplicated minCmax/minTEC solution must appear exactly once", 1, xCount);
    assertEquals("minTWC solution must appear exactly once", 1, yCount);
    assertEquals("98 G fill the remaining seats", 98, gCount);
  }

  @Test
  public void dominatedGlobalMinimumIsNotReserved() {
    // A_dom(95,9999,9999) 是全局 minCmax，但被 B(95,7000,7000) 严格支配（同 Cmax、更优 TEC/TWC）；
    // C(96,9000,9000) 与 B 都是 q==0，其中 B 是 q==0 的 minCmax。A_dom 不得被保留。
    List<PermutationSolution<Integer>> pool = Arrays.asList(
        solution(1, 95.0, 9999.0, 9999.0), // A_dom
        solution(2, 95.0, 7000.0, 7000.0), // B
        solution(3, 96.0, 9000.0, 9000.0)); // C
    double[] scores = new double[pool.size()];
    for (int left = 0; left < pool.size(); left++) {
      double dominates = 0;
      double dominatedBy = 0;
      for (int right = 0; right < pool.size(); right++) {
        if (left == right) continue;
        if (ZhangBoEvaluatedPddrSelector.strictlyDominates(
            pool.get(left), pool.get(right))) dominates++;
        if (ZhangBoEvaluatedPddrSelector.strictlyDominates(
            pool.get(right), pool.get(left))) dominatedBy++;
      }
      scores[left] = dominatedBy + 1.0 / (dominates + 1.0);
    }
    List<Integer> reserved = ZhangBoEvaluatedPddrSelector.boundaryReservedIndices(pool, scores);
    assertEquals("only B is q==0 therefore only B is reserved", 1, reserved.size());
    assertEquals("reserved index must be B (idx1)", 1, reserved.get(0).intValue());
    assertTrue("dominated global-min Cmax (idx0) must not be reserved",
        !reserved.contains(0));
    assertTrue("C (idx2) is dominated by B and must not be reserved",
        !reserved.contains(2));
  }

  @Test
  public void duplicateObjectInPoolIsDeduplicatedByFingerprint() {
    // 同一对象 X 出现在 idx0 与 idx1（同 fingerprint），Y 是 minTWC -> 扫描逐次收敛到
    // minCmax=minTEC=idx0，minTWC=idx2；dedup 后返回 [0, 2]。
    PermutationSolution<Integer> x = solution(1, 100.0, 100.0, 9000.0);
    List<PermutationSolution<Integer>> pool = new ArrayList<>();
    pool.add(x);
    pool.add(x);
    pool.add(solution(2, 900.0, 9000.0, 100.0)); // Y
    double[] scores = {1.0, 1.0, 1.0};
    List<Integer> reserved = ZhangBoEvaluatedPddrSelector.boundaryReservedIndices(pool, scores);
    assertEquals("expected two reserved slots", 2, reserved.size());
    assertEquals(0, reserved.get(0).intValue());
    assertEquals(2, reserved.get(1).intValue());
  }

  @Test
  public void trimsReservationWhenTargetSizeIsSmaller() {
    List<PermutationSolution<Integer>> pool = new ArrayList<>();
    pool.add(solution(1, 100.0, 9000.0, 9000.0));
    pool.add(solution(2, 900.0, 100.0, 9000.0));
    pool.add(solution(3, 900.0, 9000.0, 100.0));
    List<Candidate> selected = selectAll(pool, 2);
    assertEquals(2, selected.size());
    assertObjectives(selected.get(0).getSolution(), 100.0, 9000.0, 9000.0);
    assertObjectives(selected.get(1).getSolution(), 900.0, 100.0, 9000.0);
  }

  private static List<Candidate> selectAll(
      List<PermutationSolution<Integer>> pool, int targetSize) {
    List<CandidateInput> inputs = new ArrayList<>(pool.size());
    for (int index = 0; index < pool.size(); index++) {
      inputs.add(CandidateInput.ofEvaluated(pool.get(index),
          new ArrayList<PermutationSolution<Integer>>(),
          ZhangBoEvaluatedPddrSelector.Source.GLOBAL_OFFSPRING, index, index, index));
    }
    return new ZhangBoEvaluatedPddrSelector().select(inputs,
        Collections.<PermutationSolution<Integer>>emptyList(),
        Collections.<List<PermutationSolution<Integer>>>emptyList(), targetSize);
  }

  private static List<String> candidateKeys(List<Candidate> selected) {
    List<String> keys = new ArrayList<>(selected.size());
    for (Candidate candidate : selected) {
      keys.add(candidate.getSource() + ":" + candidate.getOriginalOrder());
    }
    return keys;
  }

  private static void assertObjectives(PermutationSolution<Integer> solution,
      double cmax, double tec, double twc) {
    assertEquals(cmax, solution.getObjective(0), 1.0e-9);
    assertEquals(tec, solution.getObjective(1), 1.0e-9);
    assertEquals(twc, solution.getObjective(6), 1.0e-9);
  }

  private static ZhangBoTestPermutationSolution solution(
      int variant, double cmax, double tec, double twc) {
    return new ZhangBoTestPermutationSolution(new int[]{0, 1},
        new int[]{variant % 2, (variant / 2) % 2},
        new int[]{variant % 3, (variant + 1) % 3},
        new int[]{variant % 2, (variant + 1) % 2}, new int[0], cmax, tec, twc)
        .withFatigue(0.2, 0.1);
  }
}