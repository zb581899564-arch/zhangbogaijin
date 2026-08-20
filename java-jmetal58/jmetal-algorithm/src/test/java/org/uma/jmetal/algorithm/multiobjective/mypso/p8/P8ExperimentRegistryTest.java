package org.uma.jmetal.algorithm.multiobjective.mypso.p8;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.EnumMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.Test;
import org.uma.jmetal.algorithm.multiobjective.mypso.zhangbo.ZhangBoGlobalSearchConfiguration;
import org.uma.jmetal.problem.multiobjective.dfsp.fatigue.shift.ZhangBoShiftMode;
import org.uma.jmetal.problem.multiobjective.dfsp.fatigue.shift.ZhangBoShiftConfiguration;

public class P8ExperimentRegistryTest {
  @Test
  public void registersP8V3FormalMatrixWithoutLegacyControls() {
    List<P8ExperimentSpec> specs = P8ExperimentRegistry.all();
    assertEquals(34, specs.size());
    Map<P8MatrixKind, Integer> counts = new EnumMap<>(P8MatrixKind.class);
    Set<String> labels = new HashSet<>();
    for (P8ExperimentSpec spec : specs) {
      counts.put(spec.getMatrix(), counts.containsKey(spec.getMatrix())
          ? counts.get(spec.getMatrix()) + 1 : 1);
      assertTrue(labels.add(spec.getLabel()));
      assertEquals(P8AblationProfile.VERSION,
          spec.getAblationProfile().canonicalText().split("\\n")[0].substring("ablationSchema=".length()));
      assertFalse(spec.isDiagnosticOnly());
      assertEquals(100, spec.getPopulationSize());
      assertEquals(2000, spec.getMaxFEs());
      assertArrayEquals(new int[] {20, 40, 20, 20}, spec.getPhysicalSubswarmSizes());
      assertEquals(64, spec.getMechanismVectorHash().length());
      assertEquals(ZhangBoShiftMode.LEFT_RIGHT,
          spec.getAblationProfile().getShiftMode());
      assertEquals("fatigue-shift-v2-common-gap",
          ZhangBoShiftConfiguration.ALGORITHM_SEMANTICS_VERSION);
      assertEquals(ZhangBoShiftConfiguration.formalLeftRight().toCanonicalText(),
          spec.getAblationProfile().getShiftConfiguration().toCanonicalText());
    }
    assertEquals(Integer.valueOf(7), counts.get(P8MatrixKind.FV));
    assertEquals(Integer.valueOf(4), counts.get(P8MatrixKind.FM));
    assertEquals(Integer.valueOf(7), counts.get(P8MatrixKind.QP));
    assertEquals(Integer.valueOf(7), counts.get(P8MatrixKind.VNS));
    assertEquals(Integer.valueOf(9), counts.get(P8MatrixKind.FULL));
    assertTrue(P8ExperimentRegistry.controls().isEmpty());
    assertEquals(34, P8ExperimentRegistry.allWithControls().size());
    assertEquals(1, P8ExperimentRegistry.diagnostics().size());
    assertTrue(P8ExperimentRegistry.diagnostic().isDiagnosticOnly());
    assertEquals("A0_AUTHOR_DIAGNOSTIC", P8ExperimentRegistry.diagnostic().getLabel());
    assertEquals(ZhangBoShiftMode.NONE,
        P8ExperimentRegistry.diagnostic().getAblationProfile().getShiftMode());
  }

  @Test
  public void canonicalB0AndFm0CarryQgPddrAndO1O9() {
    for (String label : new String[] {"B0", "FM0"}) {
      P8AblationProfile profile = P8ExperimentRegistry.find(label).getAblationProfile();
      assertTrue(profile.isCanonicalBaseline());
      assertTrue(profile.isDeterministicCanonical());
      assertTrue(profile.isQgEnabled());
      assertTrue(profile.isEvaluatedPddrEnabled());
      assertEquals(P8AblationProfile.VnsMode.O1_O9_FIXED, profile.getVnsMode());
      assertEquals("deterministic_canonical", profile.getSemanticTag());
      assertEquals("deterministic_canonical", profile.getSolutionSemanticTag());
    }
    assertEquals("fatigue_fm1", P8ExperimentRegistry.find("FM1")
        .getAblationProfile().getSolutionSemanticTag());
    assertEquals("fatigue_fm2", P8ExperimentRegistry.find("FM2")
        .getAblationProfile().getSolutionSemanticTag());
    assertEquals("fatigue_fm3", P8ExperimentRegistry.find("FM3")
        .getAblationProfile().getSolutionSemanticTag());
    for (String label : new String[] {"FV0", "FV1", "FV2", "FV3", "FV4", "FV5",
        "QP0", "QP1", "QP2", "QP3", "QP4", "QP5"}) {
      assertEquals(P8AblationProfile.VnsMode.O1_O9_FIXED,
          P8ExperimentRegistry.find(label).getAblationProfile().getVnsMode());
    }
  }

  @Test
  public void attributionPairsHaveObservableMechanismDifferences() {
    P8ExperimentRegistry.assertAdjacentMechanismDifferences(P8ExperimentRegistry.all());
    assertEquals(java.util.Collections.singletonList("decoderMode"),
        differences("FM0", "FM1"));
    assertEquals(java.util.Collections.singletonList("decoderMode"),
        differences("FM1", "FM2"));
    assertEquals(java.util.Collections.singletonList("decoderMode"),
        differences("FM2", "FM3"));
    assertEquals(java.util.Collections.singletonList("qg"), differences("QP3", "QP4"));
    assertEquals(java.util.Collections.singletonList("blockFrozenDualQ"),
        differences("QP4", "QP5"));
    assertEquals(java.util.Collections.singletonList("vnsMode"),
        differences("B7", "FULL"));
    assertEquals(P8AblationProfile.VnsMode.O1_O9_FIXED,
        P8ExperimentRegistry.find("V0").getAblationProfile().getVnsMode());
  }

  @Test
  public void canonicalProfileHashesAreStableAndA0CannotEnterConfiguration() {
    P8ExperimentSpec b0 = P8ExperimentRegistry.find("B0");
    assertEquals(b0.getMechanismVectorHash(),
        P8ExperimentRegistry.mechanismVectorHash(b0));
    try {
      P8ExperimentRegistry.configurationFor(P8ExperimentRegistry.diagnostic(), 1L);
      throw new AssertionError("A0 must not be exposed as a formal configuration");
    } catch (IllegalArgumentException expected) {
      assertTrue(expected.getMessage().contains("formal v3"));
    }
  }

  @Test
  public void runtimeConfigurationCarriesOneModeSpecificSemanticTag() {
    for (String label : new String[] {"FM0", "FM1", "FM2", "FM3", "B0", "FULL"}) {
      P8AblationProfile profile = P8ExperimentRegistry.find(label).getAblationProfile();
      ZhangBoGlobalSearchConfiguration configuration =
          P8ExperimentRegistry.configurationFor(P8ExperimentRegistry.find(label), 20260808L);
      String text = configuration.toCanonicalText();
      assertEquals(profile.getSolutionSemanticTag(), configuration.getSemanticTag());
      assertEquals(1, occurrences(text, "semanticTag="));
      assertTrue(text.contains("semanticTag=" + profile.getSolutionSemanticTag() + "\n"));
      assertEquals(0.6, configuration.getResourceCognitiveScale(), 0.0);
      assertEquals(0.6, configuration.getResourceSocialScale(), 0.0);
    }
  }

  private static int occurrences(String text, String token) {
    int count = 0;
    int position = 0;
    while ((position = text.indexOf(token, position)) >= 0) {
      count++;
      position += token.length();
    }
    return count;
  }

  private static List<String> differences(String left, String right) {
    return P8ExperimentRegistry.find(left).getAblationProfile().differenceKeys(
        P8ExperimentRegistry.find(right).getAblationProfile());
  }
}
