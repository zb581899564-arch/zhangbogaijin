package org.uma.jmetal.algorithm.multiobjective.mypso.zhangbo;

import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class ZhangBoPersonalArchiveTest {
  private static final ZhangBoArchiveBounds BOUNDS = ZhangBoArchiveBounds.of(
      new double[]{0.0, 0.0, 0.0}, new double[]{10.0, 10.0, 10.0},
      0.0, 1.0, 0.0, 10.0, 1.0e-12);

  @Test
  public void strictDominanceRemovesOnlyTheDominatedEntry() {
    ZhangBoPersonalArchive archive = archive(6);
    ZhangBoArchiveEntry good = entry(1, 1.0, 1.0, 1.0, 0.5, 2.0);
    ZhangBoArchiveEntry bad = entry(2, 2.0, 2.0, 2.0, 0.1, 0.1);
    ZhangBoPersonalArchive.Update update = archive.update(
        Collections.singletonList(bad), good, ZhangBoSubSwarm.G1_CMAX, BOUNDS);
    assertEquals(1, update.getEntries().size());
    assertEquals(good.getFingerprint(), update.getEntries().get(0).getFingerprint());
    assertEquals(1, update.getDominatedRemoved());
  }

  @Test
  public void nearDuplicateComponentKeepsLowerEqualWeightFatigueRisk() {
    ZhangBoPersonalArchive archive = archive(6);
    ZhangBoArchiveEntry tired = entry(1, 5.0, 5.0, 5.0, 0.9, 9.0);
    ZhangBoArchiveEntry rested = entry(2, 5.00001, 4.99999, 5.0, 0.1, 1.0);
    ZhangBoPersonalArchive.Update update = archive.update(
        Collections.singletonList(tired), rested, ZhangBoSubSwarm.G4_BALANCED, BOUNDS);
    assertEquals(1, update.getEntries().size());
    assertEquals(rested.getFingerprint(), update.getEntries().get(0).getFingerprint());
    assertEquals(1, update.getDuplicatesRemoved());
  }

  @Test
  public void capacitySixIsStableAcrossInputOrderAndKeepsDirectionAnchor() {
    ZhangBoPersonalArchive archive = archive(6);
    List<ZhangBoArchiveEntry> values = tradeoffEntries();
    ZhangBoArchiveEntry inserted = values.remove(values.size() - 1);
    ZhangBoPersonalArchive.Update forward = archive.update(values, inserted,
        ZhangBoSubSwarm.G1_CMAX, BOUNDS);
    Collections.reverse(values);
    ZhangBoPersonalArchive.Update reverse = archive.update(values, inserted,
        ZhangBoSubSwarm.G1_CMAX, BOUNDS);
    assertEquals(6, forward.getEntries().size());
    assertEquals(fingerprints(forward.getEntries()), fingerprints(reverse.getEntries()));
    assertTrue(fingerprints(forward.getEntries()).contains(entry(0, 1.0, 9.0, 5.0,
        0.5, 5.0).getFingerprint()));
  }

  @Test
  public void kappaPointZeroFiveProducesFiniteOrderedIndicatorFitness() {
    ZhangBoPersonalArchive archive = archive(6);
    List<ZhangBoArchiveEntry> values = Arrays.asList(
        entry(1, 1.0, 8.0, 8.0, 0.5, 5.0),
        entry(2, 4.0, 4.0, 4.0, 0.5, 5.0),
        entry(3, 8.0, 1.0, 8.0, 0.5, 5.0));
    Map<String, Double> fitness = archive.epsilonFitnessValues(values, BOUNDS);
    assertEquals(3, fitness.size());
    for (Double value : fitness.values()) assertTrue(Double.isFinite(value));
    assertFalse(fitness.get(values.get(0).getFingerprint())
        .equals(fitness.get(values.get(1).getFingerprint())));
  }

  @Test
  public void capacityOneKeepsEachSubgroupDirectionalAnchor() {
    ZhangBoArchiveEntry cmax = entry(10, 1.0, 9.0, 9.0, 0.5, 5.0);
    ZhangBoArchiveEntry center = entry(11, 4.0, 4.0, 4.0, 0.5, 5.0);
    ZhangBoArchiveEntry tec = entry(12, 9.0, 1.0, 9.0, 0.5, 5.0);
    ZhangBoArchiveEntry twc = entry(13, 9.0, 9.0, 1.0, 0.5, 5.0);
    List<ZhangBoArchiveEntry> previous = Arrays.asList(cmax, center, tec);
    assertAnchor(previous, twc, ZhangBoSubSwarm.G1_CMAX, cmax);
    assertAnchor(previous, twc, ZhangBoSubSwarm.G4_BALANCED, center);
    assertAnchor(previous, twc, ZhangBoSubSwarm.G2_TEC, tec);
    assertAnchor(previous, twc, ZhangBoSubSwarm.G3_TWC, twc);
  }

  private static void assertAnchor(
      List<ZhangBoArchiveEntry> previous, ZhangBoArchiveEntry inserted,
      ZhangBoSubSwarm group, ZhangBoArchiveEntry expected) {
    ZhangBoPersonalArchive.Update update = archive(1).update(previous, inserted, group, BOUNDS);
    assertEquals(1, update.getEntries().size());
    assertEquals(expected.getFingerprint(), update.getEntries().get(0).getFingerprint());
  }

  private static ZhangBoPersonalArchive archive(int capacity) {
    return new ZhangBoPersonalArchive(ZhangBoPersonalArchiveConfiguration.of(capacity,
        1.0e-12, 1.0e-4, 0.05, 0.5, 0.5, 1.0e-4));
  }

  private static List<ZhangBoArchiveEntry> tradeoffEntries() {
    List<ZhangBoArchiveEntry> result = new ArrayList<>();
    result.add(entry(0, 1.0, 9.0, 5.0, 0.5, 5.0));
    result.add(entry(1, 2.0, 8.0, 4.5, 0.4, 4.0));
    result.add(entry(2, 3.0, 7.0, 4.0, 0.3, 3.0));
    result.add(entry(3, 4.0, 6.0, 3.5, 0.2, 2.0));
    result.add(entry(4, 5.0, 5.0, 3.0, 0.1, 1.0));
    result.add(entry(5, 6.0, 4.0, 2.5, 0.2, 2.0));
    result.add(entry(6, 7.0, 3.0, 2.0, 0.3, 3.0));
    result.add(entry(7, 8.0, 2.0, 1.0, 0.4, 4.0));
    return result;
  }

  private static List<String> fingerprints(List<ZhangBoArchiveEntry> entries) {
    List<String> result = new ArrayList<>();
    for (ZhangBoArchiveEntry entry : entries) result.add(entry.getFingerprint());
    return result;
  }

  private static ZhangBoArchiveEntry entry(
      int variant, double cmax, double tec, double twc, double fmax, double fe) {
    return new ZhangBoArchiveEntry(new int[]{variant, 100 + variant},
        new int[]{variant % 3, (variant + 1) % 3},
        new int[]{variant % 4, (variant + 1) % 4},
        new int[]{variant % 2, (variant + 1) % 2},
        new double[]{cmax, tec, twc}, fmax, fe,
        ZhangBoEvaluatedPddrSelector.Source.GLOBAL_OFFSPRING, 1, variant + 1L);
  }
}
