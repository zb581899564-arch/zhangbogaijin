package org.uma.jmetal.algorithm.multiobjective.mypso.zhangbo;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;
import org.uma.jmetal.algorithm.multiobjective.mypso.zhangbo.cata.ZhangBoLocalSearchAcceptance;

/** P6.5/P7.2 canonical subgroup acceptance and QGain tests. */
public class ZhangBoLocalSearchAcceptanceTest {
  @Test
  public void boundaryGroupsUseTheirCanonicalObjectiveAndBalancedUsesAuthorRule() {
    ZhangBoTestPermutationSolution parent = solution(10.0, 20.0, 30.0);
    assertTrue(ZhangBoLocalSearchAcceptance.accepts(parent,
        solution(9.0, 100.0, 100.0), ZhangBoSubSwarm.G1_CMAX));
    assertFalse(ZhangBoLocalSearchAcceptance.accepts(parent,
        solution(11.0, 19.0, 29.0), ZhangBoSubSwarm.G1_CMAX));
    assertTrue(ZhangBoLocalSearchAcceptance.accepts(parent,
        solution(100.0, 19.0, 100.0), ZhangBoSubSwarm.G2_TEC));
    assertTrue(ZhangBoLocalSearchAcceptance.accepts(parent,
        solution(100.0, 100.0, 29.0), ZhangBoSubSwarm.G3_TWC));
    assertTrue(ZhangBoLocalSearchAcceptance.accepts(parent,
        solution(100.0, 19.0, 100.0), ZhangBoSubSwarm.G4_BALANCED));
  }

  @Test
  public void qualityGainUsesFrozenCanonicalPhiAndClips() {
    ZhangBoArchiveBounds bounds = ZhangBoArchiveBounds.of(
        new double[] {0.0, 0.0, 0.0}, new double[] {100.0, 100.0, 100.0},
        0.0, 1.0, 0.0, 1.0, 1.0e-12);
    double gain = ZhangBoLocalSearchAcceptance.qualityGain(
        solution(50.0, 20.0, 30.0), solution(25.0, 20.0, 30.0),
        ZhangBoSubSwarm.G1_CMAX, bounds);
    assertEquals(0.5, gain, 1.0e-9);
    assertEquals(-1.0, ZhangBoLocalSearchAcceptance.clip(-5.0), 0.0);
    assertEquals(1.0, ZhangBoLocalSearchAcceptance.clip(5.0), 0.0);
  }

  private static ZhangBoTestPermutationSolution solution(double cmax, double tec, double twc) {
    return new ZhangBoTestPermutationSolution(new int[] {0, 1}, new int[] {0, 0},
        new int[] {0, 0}, new int[] {0, 0}, new int[0], cmax, tec, twc)
        .withFatigue(0.2, 0.1);
  }
}
