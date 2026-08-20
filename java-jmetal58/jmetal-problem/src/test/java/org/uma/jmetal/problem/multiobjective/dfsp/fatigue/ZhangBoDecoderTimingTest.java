package org.uma.jmetal.problem.multiobjective.dfsp.fatigue;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.nio.file.Path;
import java.nio.file.Paths;
import org.junit.Test;
import org.uma.jmetal.problem.multiobjective.dfsp.decoder.EvaluationCounter;
import org.uma.jmetal.problem.multiobjective.dfsp.decoder.ProductionDecodeMode;
import org.uma.jmetal.problem.multiobjective.dfsp.fatigue.shift.ZhangBoShiftConfiguration;
import org.uma.jmetal.problem.multiobjective.dfsp.model.DhhfspFourVectorSolution;

public class ZhangBoDecoderTimingTest {
  @Test
  public void scriptedClockSeparatesBaseLeftRightAndAccumulatesAllEvaluations()
      throws Exception {
    ZhangBoCanonicalProductionProblem loaded = load();
    StepClock clock = new StepClock(1000L);
    ZhangBoCanonicalProductionProblem problem = new ZhangBoCanonicalProductionProblem(
        loaded.getInstance(), loaded.getParameters(), ProductionDecodeMode.FM3,
        new ZhangBoFatigueEvaluator(clock), new EvaluationCounter(), 20260808L,
        ZhangBoShiftConfiguration.formalLeftRight());

    problem.evaluate(problem.createSolution());
    ZhangBoDecoderTimingSnapshot first = problem.getDecoderTimingSnapshot();
    assertEquals(1L, first.getSuccessfulDecoderCalls());
    assertEquals(1000L, first.getBaseDecodeNanos());
    assertEquals(1000L, first.getLeftShiftNanos());
    assertEquals(1000L, first.getRightShiftNanos());
    assertTrue(first.getDecoderTotalNanos()
        >= first.getBaseDecodeNanos() + first.getLeftShiftNanos()
            + first.getRightShiftNanos());
    assertEquals(first.getInternalPropagations(),
        first.getLeftFullRecomputations() + first.getRightFullRecomputations());
    assertEquals(1L, problem.getEvaluationCounter().getSuccessfulEvaluations());

    problem.evaluate(problem.createSolution());
    ZhangBoDecoderTimingSnapshot second = problem.getDecoderTimingSnapshot();
    assertEquals(2L, second.getSuccessfulDecoderCalls());
    assertEquals(2L * first.getBaseDecodeNanos(), second.getBaseDecodeNanos());
    assertEquals(2L * first.getLeftShiftNanos(), second.getLeftShiftNanos());
    assertEquals(2L * first.getRightShiftNanos(), second.getRightShiftNanos());
    assertEquals(2L, problem.getEvaluationCounter().getSuccessfulEvaluations());
  }

  @Test
  public void noneModeRecordsOnlyBaseAndDoesNotChangeCanonicalResult() throws Exception {
    ZhangBoCanonicalProductionProblem loaded = load();
    DhhfspFourVectorSolution source = loaded.createSolution();
    ZhangBoFatigueEvaluationResult fast = new ZhangBoFatigueEvaluator(new StepClock(10L))
        .evaluate(loaded.getInstance(), loaded.getParameters(), source.copy(),
            ZhangBoFatigueEvaluationMode.FATIGUE_AWARE_SELECTION,
            ZhangBoShiftConfiguration.none());
    ZhangBoFatigueEvaluationResult slow = new ZhangBoFatigueEvaluator(new StepClock(1000L))
        .evaluate(loaded.getInstance(), loaded.getParameters(), source.copy(),
            ZhangBoFatigueEvaluationMode.FATIGUE_AWARE_SELECTION,
            ZhangBoShiftConfiguration.none());

    assertTrue(fast.getDecoderTiming().getBaseDecodeNanos() > 0L);
    assertEquals(0L, fast.getDecoderTiming().getLeftShiftNanos());
    assertEquals(0L, fast.getDecoderTiming().getRightShiftNanos());
    assertEquals(0L, fast.getDecoderTiming().getInternalPropagations());
    assertArrayEquals(fast.toCanonicalUtf8(), slow.toCanonicalUtf8());
  }

  @Test
  public void timingClockDoesNotChangeShiftedScheduleOrBehaviorHashes() throws Exception {
    ZhangBoCanonicalProductionProblem loaded = load();
    DhhfspFourVectorSolution source = loaded.createSolution();
    ZhangBoFatigueEvaluationResult fast = new ZhangBoFatigueEvaluator(new StepClock(10L))
        .evaluate(loaded.getInstance(), loaded.getParameters(), source.copy(),
            ZhangBoFatigueEvaluationMode.FATIGUE_AWARE_SELECTION,
            ZhangBoShiftConfiguration.formalLeftRight());
    ZhangBoFatigueEvaluationResult slow = new ZhangBoFatigueEvaluator(new StepClock(1000L))
        .evaluate(loaded.getInstance(), loaded.getParameters(), source.copy(),
            ZhangBoFatigueEvaluationMode.FATIGUE_AWARE_SELECTION,
            ZhangBoShiftConfiguration.formalLeftRight());

    assertArrayEquals(fast.toCanonicalUtf8(), slow.toCanonicalUtf8());
    assertEquals(fast.getShiftSummary().getEventSha256(),
        slow.getShiftSummary().getEventSha256());
    assertEquals(fast.getShiftSummary().getEvaluationTraceSha256(),
        slow.getShiftSummary().getEvaluationTraceSha256());
    assertTrue(fast.getDecoderTiming().getDecoderTotalNanos()
        != slow.getDecoderTiming().getDecoderTotalNanos());
  }

  private static ZhangBoCanonicalProductionProblem load() throws Exception {
    Path project = Paths.get("..").toAbsolutePath().normalize();
    return ZhangBoCanonicalProblemLoader.load(project.resolve("EADHFSP/20_2_3_1.txt"),
        ProductionDecodeMode.FM3, 20260808L,
        project.resolve("instance-extensions/v1"), project.resolve("fatigue-parameters/v1"));
  }

  private static final class StepClock implements ZhangBoDecoderNanoClock {
    private final long step;
    private long value;
    private StepClock(long step) { this.step = step; }
    @Override public long nanoTime() {
      value += step;
      return value;
    }
  }
}
