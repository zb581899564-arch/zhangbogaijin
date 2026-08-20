package org.uma.jmetal.algorithm.multiobjective.hmopsoqgs;

import org.junit.Test;
import org.uma.jmetal.problem.multiobjective.dfsp.decoder.DecodeOptions;
import org.uma.jmetal.problem.multiobjective.dfsp.decoder.DhhfspProblem;
import org.uma.jmetal.problem.multiobjective.dfsp.decoder.EvaluationCounter;
import org.uma.jmetal.problem.multiobjective.dfsp.decoder.OriginalDhhfspDecoder;
import org.uma.jmetal.problem.multiobjective.dfsp.decoder.RandomDhhfspSolutionFactory;
import org.uma.jmetal.problem.multiobjective.dfsp.model.Chapter4GoldenFixture;
import org.uma.jmetal.problem.multiobjective.dfsp.model.DhhfspInstance;
import org.uma.jmetal.util.pseudorandom.impl.JavaRandomGenerator;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class PublishedHmopsoQgsTest {
  @Test
  public void shortClosedLoopClosesBudgetAndReplaysByteForByte() {
    Run first = run(20260808L);
    Run second = run(20260808L);
    assertEquals(40L, first.evaluations);
    assertEquals(first.trace, second.trace);
    assertEquals(first.q, second.q);
    assertFalse(first.result.isEmpty());
    assertTrue(first.trace.contains("phase=BEFORE_LOCAL_SEARCH"));
    assertTrue(first.trace.contains("O9_MA_SWAP"));
    assertFalse(first.trace.equals(run(20260809L).trace));
  }

  private static Run run(long seed) {
    Chapter4GoldenFixture fixture = Chapter4GoldenFixture.load();
    DhhfspInstance instance = fixture.getInstance();
    JavaRandomGenerator factoryRandom = new JavaRandomGenerator(seed ^ 17L);
    EvaluationCounter counter = new EvaluationCounter();
    DhhfspProblem problem = new DhhfspProblem(instance, new OriginalDhhfspDecoder(),
        DecodeOptions.deterministic(seed),
        new RandomDhhfspSolutionFactory(instance, factoryRandom, "published_baseline"), counter);
    HmopsoQgsConfiguration configuration = new HmopsoQgsConfiguration(
        4, new int[] {1, 1, 1, 1}, 0.6,
        new double[] {0.2, 0.5, 0.5}, new double[] {0.08, 0.15, 0.25},
        1, 1, 0.8, 0.8, 1.0, 40L, seed);
    PublishedHmopsoQgs algorithm = new PublishedHmopsoQgsBuilder(problem, instance)
        .setConfiguration(configuration)
        .setRandomGenerator(new JavaRandomGenerator(seed))
        .build();
    algorithm.run();
    return new Run(counter.getSuccessfulEvaluations(), algorithm.traceText(),
        algorithm.qTablesText(), algorithm.getResult().toString());
  }

  private static final class Run {
    private final long evaluations;
    private final String trace;
    private final String q;
    private final String result;
    private Run(long evaluations, String trace, String q, String result) {
      this.evaluations = evaluations;
      this.trace = trace;
      this.q = q;
      this.result = result;
    }
  }
}
