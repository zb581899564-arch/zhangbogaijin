package org.uma.jmetal.algorithm.multiobjective.mypso.v35;

import java.util.Collections;
import org.junit.Test;
import static org.junit.Assert.*;

public class V35DscrTeacherCacheTest {
  @Test public void reportsRatioMetricsAndCompleteLifecycleFields() {
    V35DscrTeacherCache ledger = new V35DscrTeacherCache();
    V35SocialTeacher old = new V35SocialTeacher(new double[] {5, 5, 5}, "old");
    V35SocialTeacher replacement = new V35SocialTeacher(new double[] {4, 4, 4}, "new");
    V35SocialKnowledgeSnapshot snapshot = new V35SocialKnowledgeSnapshot(
        Collections.singletonList(replacement));

    V35DscrTeacherCache.Refresh refresh = ledger.recordRefresh(7L, 3L, 1200L,
        V35SubSwarmRole.G1_CMAX, V35DscrTeacherCache.CacheType.PREVIOUS,
        old, replacement, snapshot, 9L);
    ledger.recordRefresh(7L, 3L, 1200L, V35SubSwarmRole.G1_CMAX,
        V35DscrTeacherCache.CacheType.HISTORICAL, replacement, replacement,
        snapshot, 9L);
    ledger.recordTeacherUse(7L, 3L, 1200L, V35SubSwarmRole.G1_CMAX,
        replacement, snapshot);

    assertEquals(2L, ledger.getValidityChecks());
    assertEquals(1L, ledger.getReplacements());
    assertEquals(0.5, ledger.getScrr(), 0.0);
    assertTrue(ledger.isDturDefined());
    assertEquals(0.0, ledger.getDtur(), 0.0);
    assertEquals(Long.valueOf(1200L), refresh.getFirstKnownDominatedFe());
    assertEquals(Long.valueOf(1200L), refresh.getRefreshFe());
    assertEquals(Long.valueOf(0L), refresh.getDominanceAgeValue());
    assertTrue(ledger.eventsCsv().contains("decisionCycle,generation,FE,group,cacheType"));
    assertTrue(ledger.eventsCsv().contains("7,3,1200,G1_CMAX,PREVIOUS"));
    assertTrue(ledger.teacherUsesCsv().contains("dominated,dominatorCount"));
    assertTrue(ledger.canonicalStatistics().contains("teacherUses=1"));
    assertTrue(ledger.canonicalStatistics().contains("scrr=0.500000000000"));
  }

  @Test public void dominatedTeacherUseMakesTheMechanismGateFail() {
    V35DscrTeacherCache ledger = new V35DscrTeacherCache();
    V35SocialTeacher stale = new V35SocialTeacher(new double[] {5, 5, 5}, "old");
    V35SocialTeacher dominator = new V35SocialTeacher(new double[] {4, 4, 4}, "new");
    V35SocialKnowledgeSnapshot snapshot = new V35SocialKnowledgeSnapshot(
        Collections.singletonList(dominator));
    ledger.recordTeacherUse(1L, 0L, 100L, V35SubSwarmRole.G1_CMAX, stale, snapshot);
    assertEquals(1L, ledger.getDominatedTeacherUses());
    assertEquals(1.0, ledger.getDtur(), 0.0);
  }
}
