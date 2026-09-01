package org.uma.jmetal.algorithm.multiobjective.mypso.v35;

import java.util.List;
import org.junit.Test;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class V35SubSwarmMixtureDesignTest {
  @Test public void latticeHasExactly111LegalCompositions() {
    List<V35SubSwarmMixture> lattice = V35SubSwarmMixtureDesign.candidateLattice();
    assertEquals(111, lattice.size());
    for (V35SubSwarmMixture m : lattice) assertEquals(100, m.getTotal());
  }

  @Test public void designIsDeterministicRankTenAndContainsForcedPoints() {
    V35SubSwarmMixtureDesign.Selection a = V35SubSwarmMixtureDesign.select15();
    V35SubSwarmMixtureDesign.Selection b = V35SubSwarmMixtureDesign.select15();
    assertEquals(15, a.getTreatments().size());
    assertEquals(a.getTreatments(), b.getTreatments());
    assertEquals(10, a.getRank());
    assertTrue(a.getConditionNumber() <= 1e4);
    assertTrue(a.getTreatments().contains(V35SubSwarmMixture.BASELINE));
    assertTrue(a.getTreatments().contains(V35SubSwarmMixture.HISTORICAL_REGION_CONTROL));
    assertTrue(a.getTreatments().contains(V35SubSwarmMixture.BALANCED_CONTROL));
  }

  @Test public void explicitMixtureIsBoundIntoConfigurationButLegacyHashIsUnchanged() {
    V35ProductionConfiguration legacy = V35ProductionConfiguration.formal(1L);
    V35ProductionConfiguration explicit = V35ProductionConfiguration.builder()
        .seed(1L).populationSize(100).maxEvaluations(500000)
        .decoderMode(org.uma.jmetal.problem.multiobjective.dfsp.decoder.ProductionDecodeMode.FM3)
        .dscr(true).cfvf(true).qg(true).qp(true).caTaLite(true)
        .pddrSelectionMode(org.uma.jmetal.algorithm.multiobjective.mypso.zhangbo.PddrSelectionMode.GLOBAL_ORIGINAL)
        .subSwarmMixture(V35SubSwarmMixture.BASELINE).build();
    assertTrue(explicit.canonicalText().contains("subSwarmMixtureVersion=doe1-mixture-v1"));
    assertTrue(!legacy.canonicalText().contains("subSwarmMixtureVersion"));
    assertEquals(V35SubSwarmMixture.BASELINE, explicit.getSubSwarmMixtureOrDefault());
  }
}
