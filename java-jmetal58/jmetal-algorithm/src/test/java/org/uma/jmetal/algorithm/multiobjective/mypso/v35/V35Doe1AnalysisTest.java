package org.uma.jmetal.algorithm.multiobjective.mypso.v35;

import java.util.ArrayList;
import java.util.List;
import org.junit.Test;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class V35Doe1AnalysisTest {
  @Test public void pairedResponsesUseTheRegisteredSigns() {
    V35Doe1Analysis.Block b = new V35Doe1Analysis.Block(
        V35SubSwarmMixture.BASELINE, "20_2_3_1", 1L,
        90, 80, 70, 1.1, 0.9, 100, 100, 100, 1.0, 1.0);
    assertEquals(0.10, b.dCmax(), 1e-12);
    assertEquals(0.20, b.dTec(), 1e-12);
    assertEquals(0.30, b.dTwc(), 1e-12);
    assertEquals(0.10, b.dHv(), 1e-12);
    assertEquals(-0.10, b.dIgd(), 1e-12);
  }

  @Test public void qualityGateRejectsInstanceCatastrophe() {
    List<V35Doe1Analysis.Block> blocks = new ArrayList<>();
    V35SubSwarmMixture m = V35SubSwarmMixture.BALANCED_CONTROL;
    for (long seed=1;seed<=3;seed++) blocks.add(new V35Doe1Analysis.Block(m,"20",seed,90,100,100,1.0,1.0,100,100,100,1.0,1.0));
    for (long seed=1;seed<=3;seed++) blocks.add(new V35Doe1Analysis.Block(m,"100",seed,100,100,100,0.90,1.30,100,100,100,1.0,1.0));
    assertTrue(!V35Doe1Analysis.passesQualityGates(blocks));
  }

  @Test public void quadraticModelReportsFullDesignRank() {
    List<V35Doe1Analysis.Block> blocks = new ArrayList<>();
    List<V35SubSwarmMixture> design = V35SubSwarmMixtureDesign.select15().getTreatments();
    for (int i=0;i<design.size();i++) for (int seed=0;seed<9;seed++) {
      double y = 100.0 - i;
      blocks.add(new V35Doe1Analysis.Block(design.get(i), "I" + (seed % 3), seed,
          y, 100, 100, 1.0, 1.0, 100, 100, 100, 1.0, 1.0));
    }
    V35Doe1Analysis.ModelDiagnostics d = V35Doe1Analysis.fitCmax(blocks);
    assertEquals(10, d.rank);
    assertTrue(d.conditionNumber <= 1e4);
  }

  @Test public void observedFallbackFiltersFiveDimensionalDominatedMixtures() {
    List<V35Doe1Analysis.Block> blocks = new ArrayList<>();
    V35SubSwarmMixture better = V35SubSwarmMixture.BASELINE;
    V35SubSwarmMixture dominated = V35SubSwarmMixture.BALANCED_CONTROL;
    for (int seed = 1; seed <= 3; seed++) {
      blocks.add(new V35Doe1Analysis.Block(better, "20", seed,
          90, 90, 90, 1.10, .90, 100, 100, 100, 1.0, 1.0));
      blocks.add(new V35Doe1Analysis.Block(dominated, "20", seed,
          95, 95, 95, 1.05, .95, 100, 100, 100, 1.0, 1.0));
    }
    List<V35Doe1Analysis.ResponseSummary> front = V35Doe1Analysis.observedParetoFront(blocks);
    assertEquals(1, front.size());
    assertEquals(better, front.get(0).mixture);
    assertEquals(better, V35Doe1Analysis.observedTopThree(blocks).get(0));
  }
}
