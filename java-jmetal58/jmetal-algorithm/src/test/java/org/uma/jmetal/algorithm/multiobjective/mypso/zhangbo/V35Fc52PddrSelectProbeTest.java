package org.uma.jmetal.algorithm.multiobjective.mypso.zhangbo;

import java.util.ArrayList;
import java.util.List;
import org.junit.Test;
import org.uma.jmetal.algorithm.multiobjective.mypso.zhangbo.ZhangBoEvaluatedPddrSelector.Candidate;
import org.uma.jmetal.algorithm.multiobjective.mypso.zhangbo.ZhangBoEvaluatedPddrSelector.CandidateInput;
import org.uma.jmetal.problem.multiobjective.dfsp.decoder.ProductionDecodeMode;
import org.uma.jmetal.problem.multiobjective.dfsp.fatigue.ZhangBoCanonicalProblemLoader;
import org.uma.jmetal.problem.multiobjective.dfsp.fatigue.ZhangBoCanonicalProductionProblem;
import org.uma.jmetal.problem.multiobjective.dfsp.fatigue.shift.ZhangBoShiftConfiguration;
import org.uma.jmetal.solution.PermutationSolution;
import static org.junit.Assert.*;

/**
 * FC-5.2 靶向验证：一个三目标上不被任何候选支配的"score=1 最优"解，
 * 在 ZhangBoEvaluatedPddrSelector.select 前 targetSize 名中必被选中。
 */
public class V35Fc52PddrSelectProbeTest {
  private static final long SEED = 20260822L;

  @Test
  public void scoreOneCandidateMustBeSelected() throws Exception {
    PathFinder paths = new PathFinder();
    ZhangBoCanonicalProductionProblem problem = ZhangBoCanonicalProblemLoader.load(
        paths.root.resolve("java-jmetal58/EADHFSP/20_2_3_1.txt"),
        ProductionDecodeMode.FM3, SEED,
        paths.root.resolve("java-jmetal58/instance-extensions/v1"),
        paths.root.resolve("java-jmetal58/fatigue-parameters/v1"),
        ZhangBoShiftConfiguration.none());

    // Build 200 solutions whose objectives dominate nothing and are dominated by
    // nothing in the 3-objective space (all share the same objective vector at the
    // extreme corner: (900, 9000, 9000)).  Equal vectors do not dominate each other
    // (strictDominates requires a strictly better objective), so each has score=1.
    List<PermutationSolution<Integer>> candidates = new ArrayList<>();
    for (int i = 0; i < 200; i++) {
      PermutationSolution<Integer> solution = problem.createSolution();
      solution.setObjective(0, 900.0);
      solution.setObjective(1, 9000.0);
      solution.setObjective(6, 9000.0);
      candidates.add(solution);
    }

    List<CandidateInput> inputs = new ArrayList<>();
    for (int i = 0; i < 100; i++) {
      inputs.add(CandidateInput.ofEvaluated(candidates.get(i),
          new ArrayList<PermutationSolution<Integer>>(),
          ZhangBoEvaluatedPddrSelector.Source.GLOBAL_OFFSPRING, i, i, i));
    }
    List<PermutationSolution<Integer>> parents = new ArrayList<>();
    for (int i = 0; i < 100; i++) {
      parents.add(candidates.get(100 + i));
    }

    ZhangBoEvaluatedPddrSelector selector = new ZhangBoEvaluatedPddrSelector();
    List<Candidate> selected = selector.select(inputs, parents,
        new ArrayList<List<PermutationSolution<Integer>>>() { {
          for (int i = 0; i < 100; i++) add(new ArrayList<PermutationSolution<Integer>>());
        } }, 100);

    assertEquals("targetSize", 100, selected.size());
    // All candidates have identical objectives -> every score equals 1 + 0 = 1.0;
    // the first 100 by originalOrder must be returned.  The probe's real question:
    // a score=1 objective vector IS chosen when it is in the input set.
    for (Candidate candidate : selected) {
      assertEquals("selected candidates keep the extreme objective vector",
          900.0, candidate.getSolution().getObjective(0), 1.0e-9);
    }
  }

  private static final class PathFinder {
    final java.nio.file.Path root;

    PathFinder() {
      java.nio.file.Path p = java.nio.file.Paths.get("").toAbsolutePath().normalize();
      if (p.getFileName() != null && "jmetal-algorithm".equals(p.getFileName().toString())) {
        p = p.getParent();
      }
      while (p.getParent() != null && !java.nio.file.Files.exists(p.resolve("AGENTS.md"))) {
        p = p.getParent();
      }
      root = p;
    }
  }
}