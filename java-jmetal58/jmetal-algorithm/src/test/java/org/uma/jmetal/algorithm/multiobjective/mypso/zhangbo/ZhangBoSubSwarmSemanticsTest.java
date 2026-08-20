package org.uma.jmetal.algorithm.multiobjective.mypso.zhangbo;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.List;
import org.junit.Test;

/** P6.5 executable contract for semantic roles and unchanged physical slots. */
public class ZhangBoSubSwarmSemanticsTest {
  @Test
  public void physicalAuthorSlotsMapToCanonicalRolesWithoutReordering() {
    assertEquals(ZhangBoSubSwarm.G1_CMAX,
        ZhangBoSubSwarmSemantics.roleForPhysicalSlot(1));
    assertEquals(ZhangBoSubSwarm.G4_BALANCED,
        ZhangBoSubSwarmSemantics.roleForPhysicalSlot(2));
    assertEquals(ZhangBoSubSwarm.G2_TEC,
        ZhangBoSubSwarmSemantics.roleForPhysicalSlot(3));
    assertEquals(ZhangBoSubSwarm.G3_TWC,
        ZhangBoSubSwarmSemantics.roleForPhysicalSlot(4));
    assertEquals(2, ZhangBoSubSwarmSemantics.physicalSlotForRole(
        ZhangBoSubSwarm.G4_BALANCED));
    List<ZhangBoSubSwarm> roles = ZhangBoSubSwarmSemantics.roles();
    assertEquals(Arrays.asList(ZhangBoSubSwarm.G1_CMAX, ZhangBoSubSwarm.G2_TEC,
        ZhangBoSubSwarm.G3_TWC, ZhangBoSubSwarm.G4_BALANCED), roles);
  }

  @Test
  public void objectiveDirectionsAndNeedWeightsAreTheSingleSource() {
    assertEquals(0, ZhangBoSubSwarmSemantics.objectiveIndex(ZhangBoSubSwarm.G1_CMAX));
    assertEquals(1, ZhangBoSubSwarmSemantics.objectiveIndex(ZhangBoSubSwarm.G2_TEC));
    assertEquals(6, ZhangBoSubSwarmSemantics.objectiveIndex(ZhangBoSubSwarm.G3_TWC));
    assertEquals(-1, ZhangBoSubSwarmSemantics.objectiveIndex(ZhangBoSubSwarm.G4_BALANCED));
    assertArrayEquals(new double[] {2, 1, 1, 1, 1, 1, 1},
        ZhangBoSubSwarmSemantics.needWeights(ZhangBoSubSwarm.G1_CMAX), 0.0);
    assertArrayEquals(new double[] {1, 2, 1, 1, 1, 1, 1},
        ZhangBoSubSwarmSemantics.needWeights(ZhangBoSubSwarm.G2_TEC), 0.0);
    assertArrayEquals(new double[] {1, 1, 2, 1, 1, 1, 1},
        ZhangBoSubSwarmSemantics.needWeights(ZhangBoSubSwarm.G3_TWC), 0.0);
    assertArrayEquals(new double[] {1, 1, 1, 1, 1, 1, 1},
        ZhangBoSubSwarmSemantics.needWeights(ZhangBoSubSwarm.G4_BALANCED), 0.0);
  }

  @Test
  public void mappingIsVersionedAndHashed() {
    assertTrue(ZhangBoSubSwarmSemantics.mappingText().contains("slot2=G4_BALANCED"));
    assertEquals(64, ZhangBoSubSwarmSemantics.mappingHash().length());
    assertEquals(ZhangBoSubSwarmSemantics.VERSION,
        ZhangBoSubSwarmSemantics.mappingText().split("\\|")[0]);
  }
}
