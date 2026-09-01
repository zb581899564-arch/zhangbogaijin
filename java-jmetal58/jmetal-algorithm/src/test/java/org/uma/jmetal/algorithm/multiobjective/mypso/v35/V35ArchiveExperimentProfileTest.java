package org.uma.jmetal.algorithm.multiobjective.mypso.v35;

import java.util.HashSet;
import java.util.Set;
import org.junit.Test;
import static org.junit.Assert.*;

public class V35ArchiveExperimentProfileTest {
  @Test
  public void fiveProfilesAreUniqueAndDecisionComplete() {
    assertEquals(5, V35ArchiveExperimentProfile.values().length);
    Set<String> hashes = new HashSet<>();
    for (V35ArchiveExperimentProfile profile : V35ArchiveExperimentProfile.values()) {
      assertTrue(hashes.add(profile.configurationHash()));
      assertTrue(profile.canonicalText().contains("pddrSelectionMode=UNCHANGED_GLOBAL_ORIGINAL"));
    }
    assertTrue(V35ArchiveExperimentProfile.ND0_FULL_ARCHIVE_CONTROL.isControl());
    assertEquals(50, V35ArchiveExperimentProfile.ND1_TEACHER_VIEW_K50
        .getConfiguration().getTeacherViewCapacity());
    assertEquals(25, V35ArchiveExperimentProfile.ND2_TEACHER_VIEW_K25
        .getConfiguration().getTeacherViewCapacity());
    assertEquals(200, V35ArchiveExperimentProfile.ND3_ACTIVE_ARCHIVE_K200
        .getConfiguration().getActiveArchiveCapacity());
    assertEquals(100, V35ArchiveExperimentProfile.ND4_ACTIVE_ARCHIVE_K100
        .getConfiguration().getActiveArchiveCapacity());
    assertEquals("6b6d1362bc79079a342d8f98967d707452996a4bb9a75e78baed5566f06ca823",
        V35ArchiveExperimentProfile.ND0_FULL_ARCHIVE_CONTROL.configurationHash());
    assertEquals("bbba15f54c7256bb31bade7352b1bbd17bcd6f9c74bcd8d9521d7c988c1dd3ea",
        V35ArchiveExperimentProfile.ND1_TEACHER_VIEW_K50.configurationHash());
    assertEquals("b60277e0955168a2340163f56c63760f4acbe0b6f4bdc938a49093f3363bc93b",
        V35ArchiveExperimentProfile.ND2_TEACHER_VIEW_K25.configurationHash());
    assertEquals("e9395ffc0d5d7b024545892727dd9f0411188ee7d877bff98ee44b869e490f99",
        V35ArchiveExperimentProfile.ND3_ACTIVE_ARCHIVE_K200.configurationHash());
    assertEquals("c131bb96a8f103514c3edd532ef74d676d249d286b4e2905e27856f9bdf9f6bc",
        V35ArchiveExperimentProfile.ND4_ACTIVE_ARCHIVE_K100.configurationHash());
  }

  @Test
  public void productionConfigurationDoesNotExposeArchiveExperimentMode() {
    String canonical = V35FinalAblationProfile.configurationFor(
        V35FinalAblationProfile.Arm.A4_BUDGET_AWARE_CATA, 1L, 100, 500000)
        .canonicalText();
    assertFalse(canonical.contains("archiveMode"));
    assertFalse(canonical.contains("archiveExperiment"));
  }
}
