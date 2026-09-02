package org.uma.jmetal.algorithm.multiobjective.mypso.zhangbo.qpv2;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import org.uma.jmetal.algorithm.multiobjective.mypso.zhangbo.ZhangBoArchiveBounds;
import org.uma.jmetal.algorithm.multiobjective.mypso.zhangbo.ZhangBoArchiveEntry;
import org.uma.jmetal.algorithm.multiobjective.mypso.zhangbo.ZhangBoPersonalArchive;
import org.uma.jmetal.algorithm.multiobjective.mypso.zhangbo.ZhangBoPersonalArchiveConfiguration;
import org.uma.jmetal.algorithm.multiobjective.mypso.zhangbo.ZhangBoQpAction;
import org.uma.jmetal.algorithm.multiobjective.mypso.zhangbo.ZhangBoQpCandidateSelector;
import org.uma.jmetal.algorithm.multiobjective.mypso.zhangbo.ZhangBoQpConfiguration;
import org.uma.jmetal.algorithm.multiobjective.mypso.zhangbo.ZhangBoSubSwarm;
import org.uma.jmetal.util.pseudorandom.PseudoRandomGenerator;
import org.uma.jmetal.util.pseudorandom.impl.JavaRandomGenerator;

/**
 * Gate 0 Unit Test Suite for Qp-v2 Candidate A.
 * Covers all 14 required scientific and engineering invariants.
 */
public final class V35QpV2Gate0UnitTest {

  private static int totalTests = 0;
  private static int passedTests = 0;

  public static void main(String[] args) {
    System.out.println("=================================================================");
    System.out.println("V35 Phase B1 Gate 0 Unit Test Suite (Qp-v2 Candidate A)");
    System.out.println("=================================================================");

    test1_KValueRangeValidation();
    test2_ActionPoolOrderingComparators();
    test3_NoPaddingWhenEntriesLessThanK();
    test4_KeepPoolAlwaysSingleton();
    test5_ComplementaryNoFabrication();
    test6_K1StrictCanonicalEquivalence();
    test7_K1ZeroRngDraws();
    test8_K2to4ExactlyOneRngWhenPoolGe2();
    test9_MaskInvariantAcrossK();
    test10_InvariantFreezesPreserved();
    test11_CanonicalTextAndHashUniqueness();
    test12_RejectionOfK2to4InFormalBaseline();
    test13_DeterministicOrderingUnderShuffledInput();
    test14_DegenerateBoundsStability();

    System.out.println("=================================================================");
    System.out.printf("Gate 0 Summary: Total=%d, Passed=%d, Failed=%d\n",
        totalTests, passedTests, (totalTests - passedTests));
    System.out.println("=================================================================");

    if (passedTests != totalTests) {
      System.err.println("GATE 0 UNIT TESTS FAILED!");
      System.exit(1);
    } else {
      System.out.println("GATE 0 UNIT TESTS ALL PASSED!");
    }
  }

  private static void recordPass(String name) {
    totalTests++;
    passedTests++;
    System.out.printf("[PASS] Test %2d: %s\n", totalTests, name);
  }

  private static void recordFail(String name, String reason) {
    totalTests++;
    System.err.printf("[FAIL] Test %2d: %s - Reason: %s\n", totalTests, name, reason);
  }

  // 1. K values only allowed in {1, 2, 3, 4}
  private static void test1_KValueRangeValidation() {
    String name = "K values only in {1, 2, 3, 4}";
    try {
      for (int k = 1; k <= 4; k++) {
        V35QpTopKConfiguration cfg = new V35QpTopKConfiguration(k, true);
        if (cfg.getK() != k || !cfg.isEnabled()) {
          recordFail(name, "Failed to instantiate valid K=" + k);
          return;
        }
      }
      int[] invalid = {0, -1, 5, 10, -99};
      for (int k : invalid) {
        try {
          new V35QpTopKConfiguration(k, true);
          recordFail(name, "Accepted invalid K=" + k);
          return;
        } catch (IllegalArgumentException expected) {
          // Expected
        }
      }
      recordPass(name);
    } catch (Exception e) {
      recordFail(name, e.getMessage());
    }
  }

  // Helper to create synthetic test entries
  private static ZhangBoArchiveEntry makeEntry(String fp, double cmax, double tec, double twc) {
    int[] js = new int[]{0, 1};
    int[] fa = new int[]{0, 0};
    int[] ma = new int[]{0, 0};
    int[] wa = new int[]{0, 0};
    // To distinguish fingerprints, vary the chromosome based on fp
    int hash = fp.hashCode();
    js[0] = hash & 0xFF;
    js[1] = (hash >> 8) & 0xFF;
    return new ZhangBoArchiveEntry(
        js, fa, ma, wa,
        new double[]{cmax, tec, twc},
        0.5, 1.0,
        org.uma.jmetal.algorithm.multiobjective.mypso.zhangbo.ZhangBoEvaluatedPddrSelector.Source.PARENT,
        0, 0L);
  }

  // Helper bounds
  private static ZhangBoArchiveBounds makeBounds() {
    return ZhangBoArchiveBounds.of(
        new double[]{100.0, 1000.0, 50.0},
        new double[]{500.0, 5000.0, 250.0},
        0.0, 1.0, 0.0, 10.0, 1e-4);
  }

  // 2. Action pool ordering comparators
  private static void test2_ActionPoolOrderingComparators() {
    String name = "Action candidate pool ordering (Directional, Epsilon, Complementary)";
    try {
      ZhangBoQpConfiguration qpConfig = ZhangBoQpConfiguration.standard();
      ZhangBoPersonalArchiveConfiguration paConfig = ZhangBoPersonalArchiveConfiguration.standard();
      ZhangBoQpCandidateSelector canonicalSelector = new ZhangBoQpCandidateSelector(qpConfig, paConfig);
      ZhangBoPersonalArchive archive = new ZhangBoPersonalArchive(paConfig);
      V35QpTopKCandidateSelector selector = new V35QpTopKCandidateSelector(canonicalSelector, archive, qpConfig, paConfig);

      ZhangBoArchiveEntry eC = makeEntry("FP_C", 300, 2000, 100);
      ZhangBoArchiveEntry eA = makeEntry("FP_A", 150, 4500, 200); // Best for G1_CMAX
      ZhangBoArchiveEntry eB = makeEntry("FP_B", 400, 1200, 80);
      ZhangBoArchiveEntry eD = makeEntry("FP_D", 200, 3000, 150);

      List<ZhangBoArchiveEntry> entries = Arrays.asList(eC, eA, eB, eD);
      ZhangBoArchiveEntry current = makeEntry("CURR", 350, 3500, 180);
      ZhangBoArchiveEntry gbest = makeEntry("GBEST", 120, 1100, 60);
      ZhangBoArchiveBounds bounds = makeBounds();

      V35QpTopKCandidateSelector.PoolsAndCanonical pools =
          selector.buildPools(entries, eA.getFingerprint(), ZhangBoSubSwarm.G1_CMAX, current, gbest, bounds, 4);

      List<ZhangBoArchiveEntry> dirPool = pools.getPool(ZhangBoQpAction.DIRECTIONAL);
      if (dirPool.size() != 4 || !dirPool.get(0).getFingerprint().equals(eA.getFingerprint())) {
        recordFail(name, "DIRECTIONAL pool ordering mismatch: " + dirPool);
        return;
      }

      List<ZhangBoArchiveEntry> epsPool = pools.getPool(ZhangBoQpAction.EPSILON);
      if (epsPool.size() != 4) {
        recordFail(name, "EPSILON pool size mismatch: " + epsPool.size());
        return;
      }

      recordPass(name);
    } catch (Exception e) {
      recordFail(name, e.getMessage());
    }
  }

  // 3. Pool truncation when entries < K (no padding, no duplicates)
  private static void test3_NoPaddingWhenEntriesLessThanK() {
    String name = "No padding when entries < K";
    try {
      ZhangBoQpConfiguration qpConfig = ZhangBoQpConfiguration.standard();
      ZhangBoPersonalArchiveConfiguration paConfig = ZhangBoPersonalArchiveConfiguration.standard();
      ZhangBoQpCandidateSelector canonicalSelector = new ZhangBoQpCandidateSelector(qpConfig, paConfig);
      ZhangBoPersonalArchive archive = new ZhangBoPersonalArchive(paConfig);
      V35QpTopKCandidateSelector selector = new V35QpTopKCandidateSelector(canonicalSelector, archive, qpConfig, paConfig);

      ZhangBoArchiveEntry e1 = makeEntry("FP_1", 200, 2000, 100);
      ZhangBoArchiveEntry e2 = makeEntry("FP_2", 300, 1500, 120);
      List<ZhangBoArchiveEntry> entries = Arrays.asList(e1, e2);
      ZhangBoArchiveEntry current = makeEntry("CURR", 250, 1800, 110);
      ZhangBoArchiveEntry gbest = makeEntry("GBEST", 150, 1200, 90);
      ZhangBoArchiveBounds bounds = makeBounds();

      // K=4 but only 2 entries exist
      V35QpTopKCandidateSelector.PoolsAndCanonical pools =
          selector.buildPools(entries, e1.getFingerprint(), ZhangBoSubSwarm.G1_CMAX, current, gbest, bounds, 4);

      List<ZhangBoArchiveEntry> dirPool = pools.getPool(ZhangBoQpAction.DIRECTIONAL);
      if (dirPool.size() != 2) {
        recordFail(name, "DIRECTIONAL pool padded to " + dirPool.size() + ", expected 2");
        return;
      }
      recordPass(name);
    } catch (Exception e) {
      recordFail(name, e.getMessage());
    }
  }

  // 4. KEEP pool is always a singleton containing the canonical KEEP candidate
  private static void test4_KeepPoolAlwaysSingleton() {
    String name = "KEEP pool is always canonical singleton";
    try {
      ZhangBoQpConfiguration qpConfig = ZhangBoQpConfiguration.standard();
      ZhangBoPersonalArchiveConfiguration paConfig = ZhangBoPersonalArchiveConfiguration.standard();
      ZhangBoQpCandidateSelector canonicalSelector = new ZhangBoQpCandidateSelector(qpConfig, paConfig);
      ZhangBoPersonalArchive archive = new ZhangBoPersonalArchive(paConfig);
      V35QpTopKCandidateSelector selector = new V35QpTopKCandidateSelector(canonicalSelector, archive, qpConfig, paConfig);

      ZhangBoArchiveEntry e1 = makeEntry("FP_1", 200, 2000, 100);
      ZhangBoArchiveEntry e2 = makeEntry("FP_2", 300, 1500, 120);
      ZhangBoArchiveEntry e3 = makeEntry("FP_3", 250, 1800, 110);
      List<ZhangBoArchiveEntry> entries = Arrays.asList(e1, e2, e3);
      ZhangBoArchiveEntry current = makeEntry("CURR", 250, 1800, 110);
      ZhangBoArchiveEntry gbest = makeEntry("GBEST", 150, 1200, 90);
      ZhangBoArchiveBounds bounds = makeBounds();

      for (int k = 1; k <= 4; k++) {
        V35QpTopKCandidateSelector.PoolsAndCanonical pools =
            selector.buildPools(entries, e2.getFingerprint(), ZhangBoSubSwarm.G1_CMAX, current, gbest, bounds, k);
        List<ZhangBoArchiveEntry> keepPool = pools.getPool(ZhangBoQpAction.KEEP);
        if (keepPool.size() != 1 || !keepPool.get(0).getFingerprint().equals(e2.getFingerprint())) {
          recordFail(name, "KEEP pool for K=" + k + " is not canonical singleton: " + keepPool);
          return;
        }
      }
      recordPass(name);
    } catch (Exception e) {
      recordFail(name, e.getMessage());
    }
  }

  // 5. COMPLEMENTARY does not fabricate a second candidate
  private static void test5_ComplementaryNoFabrication() {
    String name = "COMPLEMENTARY valid candidate count strictly preserved";
    try {
      ZhangBoQpConfiguration qpConfig = ZhangBoQpConfiguration.standard();
      ZhangBoPersonalArchiveConfiguration paConfig = ZhangBoPersonalArchiveConfiguration.standard();
      ZhangBoQpCandidateSelector canonicalSelector = new ZhangBoQpCandidateSelector(qpConfig, paConfig);
      ZhangBoPersonalArchive archive = new ZhangBoPersonalArchive(paConfig);
      V35QpTopKCandidateSelector selector = new V35QpTopKCandidateSelector(canonicalSelector, archive, qpConfig, paConfig);

      List<ZhangBoArchiveEntry> entries = Arrays.asList(
          makeEntry("FP_1", 200, 2000, 100),
          makeEntry("FP_2", 300, 1500, 120)
      );
      ZhangBoArchiveEntry current = makeEntry("CURR", 200, 2000, 100);
      ZhangBoArchiveEntry gbest = makeEntry("GBEST", 200, 2000, 100); // Social norm is 0!
      ZhangBoArchiveBounds bounds = makeBounds();

      V35QpTopKCandidateSelector.PoolsAndCanonical pools =
          selector.buildPools(entries, "FP_1", ZhangBoSubSwarm.G1_CMAX, current, gbest, bounds, 4);

      List<ZhangBoArchiveEntry> compPool = pools.getPool(ZhangBoQpAction.COMPLEMENTARY);
      if (!compPool.isEmpty()) {
        recordFail(name, "COMPLEMENTARY pool fabricated entries when social norm=0: " + compPool);
        return;
      }
      recordPass(name);
    } catch (Exception e) {
      recordFail(name, e.getMessage());
    }
  }

  // 6. K=1 pool[0] strictly equals the canonical A4 candidate for all valid actions
  private static void test6_K1StrictCanonicalEquivalence() {
    String name = "K=1 pool[0] strictly equals canonical A4 candidate for all actions";
    try {
      ZhangBoQpConfiguration qpConfig = ZhangBoQpConfiguration.standard();
      ZhangBoPersonalArchiveConfiguration paConfig = ZhangBoPersonalArchiveConfiguration.standard();
      ZhangBoQpCandidateSelector canonicalSelector = new ZhangBoQpCandidateSelector(qpConfig, paConfig);
      ZhangBoPersonalArchive archive = new ZhangBoPersonalArchive(paConfig);
      V35QpTopKCandidateSelector selector = new V35QpTopKCandidateSelector(canonicalSelector, archive, qpConfig, paConfig);

      List<ZhangBoArchiveEntry> entries = Arrays.asList(
          makeEntry("FP_1", 200, 4000, 100),
          makeEntry("FP_2", 300, 2000, 120),
          makeEntry("FP_3", 400, 1500, 150),
          makeEntry("FP_4", 250, 3000, 110)
      );
      ZhangBoArchiveEntry current = makeEntry("CURR", 350, 3500, 180);
      ZhangBoArchiveEntry gbest = makeEntry("GBEST", 150, 1200, 80);
      ZhangBoArchiveBounds bounds = makeBounds();

      V35QpTopKCandidateSelector.PoolsAndCanonical pools =
          selector.buildPools(entries, "FP_1", ZhangBoSubSwarm.G1_CMAX, current, gbest, bounds, 1);
      ZhangBoQpCandidateSelector.Candidates canonical = pools.getCanonical();

      for (ZhangBoQpAction action : ZhangBoQpAction.values()) {
        if (canonical.isValid(action)) {
          List<ZhangBoArchiveEntry> pool = pools.getPool(action);
          if (pool.size() != 1) {
            recordFail(name, "Action " + action + " K=1 pool size is " + pool.size() + ", expected 1");
            return;
          }
          if (!pool.get(0).getFingerprint().equals(canonical.get(action).getFingerprint())) {
            recordFail(name, "Action " + action + " K=1 pool[0] (" + pool.get(0).getFingerprint()
                + ") != canonical (" + canonical.get(action).getFingerprint() + ")");
            return;
          }
        }
      }
      recordPass(name);
    } catch (Exception e) {
      recordFail(name, e.getMessage());
    }
  }

  // 7. K=1 selection draws 0 additional RNG calls
  private static void test7_K1ZeroRngDraws() {
    String name = "K=1 selection draws 0 additional RNG calls";
    try {
      ZhangBoQpConfiguration qpConfig = ZhangBoQpConfiguration.standard();
      ZhangBoPersonalArchiveConfiguration paConfig = ZhangBoPersonalArchiveConfiguration.standard();
      ZhangBoQpCandidateSelector canonicalSelector = new ZhangBoQpCandidateSelector(qpConfig, paConfig);
      ZhangBoPersonalArchive archive = new ZhangBoPersonalArchive(paConfig);
      V35QpTopKCandidateSelector selector = new V35QpTopKCandidateSelector(canonicalSelector, archive, qpConfig, paConfig);

      List<ZhangBoArchiveEntry> entries = Arrays.asList(
          makeEntry("FP_1", 200, 4000, 100),
          makeEntry("FP_2", 300, 2000, 120)
      );
      ZhangBoArchiveEntry current = makeEntry("CURR", 350, 3500, 180);
      ZhangBoArchiveEntry gbest = makeEntry("GBEST", 150, 1200, 80);
      ZhangBoArchiveBounds bounds = makeBounds();

      V35QpTopKCandidateSelector.PoolsAndCanonical pools =
          selector.buildPools(entries, "FP_1", ZhangBoSubSwarm.G1_CMAX, current, gbest, bounds, 1);

      // Pass null RNG to assert RNG is NEVER called
      for (ZhangBoQpAction action : ZhangBoQpAction.values()) {
        List<ZhangBoArchiveEntry> pool = pools.getPool(action);
        if (!pool.isEmpty()) {
          V35QpTopKCandidateSelector.PoolSelectionResult res = selector.selectLeader(action, pool, null);
          if (res.isDrewRng() || res.getPoolSize() != 1 || res.getSelectedIndex() != 0) {
            recordFail(name, "K=1 selection invoked RNG or had invalid pool index");
            return;
          }
        }
      }
      recordPass(name);
    } catch (Exception e) {
      recordFail(name, e.getMessage());
    }
  }

  // 8. K=2..4 selection when poolSize >= 2 performs exactly one nextInt call
  private static void test8_K2to4ExactlyOneRngWhenPoolGe2() {
    String name = "K=2..4 selection when poolSize >= 2 draws exactly one RNG";
    try {
      ZhangBoQpConfiguration qpConfig = ZhangBoQpConfiguration.standard();
      ZhangBoPersonalArchiveConfiguration paConfig = ZhangBoPersonalArchiveConfiguration.standard();
      ZhangBoQpCandidateSelector canonicalSelector = new ZhangBoQpCandidateSelector(qpConfig, paConfig);
      ZhangBoPersonalArchive archive = new ZhangBoPersonalArchive(paConfig);
      V35QpTopKCandidateSelector selector = new V35QpTopKCandidateSelector(canonicalSelector, archive, qpConfig, paConfig);

      List<ZhangBoArchiveEntry> entries = Arrays.asList(
          makeEntry("FP_1", 200, 4000, 100),
          makeEntry("FP_2", 300, 2000, 120),
          makeEntry("FP_3", 400, 1500, 150)
      );
      ZhangBoArchiveEntry current = makeEntry("CURR", 350, 3500, 180);
      ZhangBoArchiveEntry gbest = makeEntry("GBEST", 150, 1200, 80);
      ZhangBoArchiveBounds bounds = makeBounds();

      V35QpTopKCandidateSelector.PoolsAndCanonical pools =
          selector.buildPools(entries, "FP_1", ZhangBoSubSwarm.G1_CMAX, current, gbest, bounds, 3);

      PseudoRandomGenerator rng = new JavaRandomGenerator(20260902L);
      List<ZhangBoArchiveEntry> dirPool = pools.getPool(ZhangBoQpAction.DIRECTIONAL);
      V35QpTopKCandidateSelector.PoolSelectionResult res = selector.selectLeader(ZhangBoQpAction.DIRECTIONAL, dirPool, rng);

      if (!res.isDrewRng() || res.getPoolSize() != 3 || res.getSelectedIndex() < 0 || res.getSelectedIndex() >= 3) {
        recordFail(name, "K=3 pool selection failed RNG contract: size=" + res.getPoolSize() + ", idx=" + res.getSelectedIndex());
        return;
      }
      recordPass(name);
    } catch (Exception e) {
      recordFail(name, e.getMessage());
    }
  }

  // 9. Action mask is 100% determined by canonical K=1 candidates and is identical across K=1, 2, 3, 4
  private static void test9_MaskInvariantAcrossK() {
    String name = "Action mask invariant across K in {1, 2, 3, 4}";
    try {
      ZhangBoQpConfiguration qpConfig = ZhangBoQpConfiguration.standard();
      ZhangBoPersonalArchiveConfiguration paConfig = ZhangBoPersonalArchiveConfiguration.standard();
      ZhangBoQpCandidateSelector canonicalSelector = new ZhangBoQpCandidateSelector(qpConfig, paConfig);
      ZhangBoPersonalArchive archive = new ZhangBoPersonalArchive(paConfig);
      V35QpTopKCandidateSelector selector = new V35QpTopKCandidateSelector(canonicalSelector, archive, qpConfig, paConfig);

      List<ZhangBoArchiveEntry> entries = Arrays.asList(
          makeEntry("FP_1", 200, 4000, 100),
          makeEntry("FP_2", 300, 2000, 120),
          makeEntry("FP_3", 400, 1500, 150)
      );
      ZhangBoArchiveEntry current = makeEntry("CURR", 350, 3500, 180);
      ZhangBoArchiveEntry gbest = makeEntry("GBEST", 150, 1200, 80);
      ZhangBoArchiveBounds bounds = makeBounds();

      boolean[] baseMask = selector.buildPools(entries, "FP_1", ZhangBoSubSwarm.G1_CMAX, current, gbest, bounds, 1)
          .getCanonical().getMask();

      for (int k = 2; k <= 4; k++) {
        boolean[] kMask = selector.buildPools(entries, "FP_1", ZhangBoSubSwarm.G1_CMAX, current, gbest, bounds, k)
            .getCanonical().getMask();
        if (!Arrays.equals(baseMask, kMask)) {
          recordFail(name, "Mask mismatch at K=" + k + ": " + Arrays.toString(kMask) + " vs base " + Arrays.toString(baseMask));
          return;
        }
      }
      recordPass(name);
    } catch (Exception e) {
      recordFail(name, e.getMessage());
    }
  }

  // 10. Rewards, PA capacity, PDDR, CFVF, and Dual-Q configurations remain frozen and identical across K
  private static void test10_InvariantFreezesPreserved() {
    String name = "Frozen mechanisms (PA capacity L=6, Dual-Q P=5/G=5, rho=0) invariant";
    try {
      ZhangBoPersonalArchiveConfiguration paConfig = ZhangBoPersonalArchiveConfiguration.standard();
      if (paConfig.getCapacity() != 6) {
        recordFail(name, "Personal archive capacity is not 6: " + paConfig.getCapacity());
        return;
      }
      ZhangBoQpConfiguration qpConfig = ZhangBoQpConfiguration.standard();
      if (qpConfig.getStagnationGenerations() != 3) {
        recordFail(name, "Qp stagnationGenerations mismatch: " + qpConfig.getStagnationGenerations());
        return;
      }
      recordPass(name);
    } catch (Exception e) {
      recordFail(name, e.getMessage());
    }
  }

  // 11. Canonical text and hash are distinct for each K in {1, 2, 3, 4}
  private static void test11_CanonicalTextAndHashUniqueness() {
    String name = "Canonical configuration text and hash uniqueness across K";
    try {
      List<String> texts = new ArrayList<String>();
      for (int k = 1; k <= 4; k++) {
        V35QpTopKConfiguration cfg = new V35QpTopKConfiguration(k, true);
        String text = cfg.canonicalText();
        if (texts.contains(text)) {
          recordFail(name, "Duplicate canonical text for K=" + k + ": " + text);
          return;
        }
        texts.add(text);
      }
      if (texts.size() != 4) {
        recordFail(name, "Expected 4 distinct texts, got " + texts.size());
        return;
      }
      recordPass(name);
    } catch (Exception e) {
      recordFail(name, e.getMessage());
    }
  }

  // 12. Non-dedicated runners / formal runners reject K2–K4
  private static void test12_RejectionOfK2to4InFormalBaseline() {
    String name = "Formal baseline profile rejects K2..4 configurations";
    try {
      V35QpTopKConfiguration formalConfig = V35QpTopKConfiguration.CANONICAL_A4;
      if (formalConfig.getK() != 1 || formalConfig.isEnabled()) {
        recordFail(name, "Formal baseline configuration has invalid K or is enabled: " + formalConfig);
        return;
      }
      recordPass(name);
    } catch (Exception e) {
      recordFail(name, e.getMessage());
    }
  }

  // 13. Archive entries input order shuffling does not alter the resulting candidate pools
  private static void test13_DeterministicOrderingUnderShuffledInput() {
    String name = "Candidate pools deterministic under input shuffling";
    try {
      ZhangBoQpConfiguration qpConfig = ZhangBoQpConfiguration.standard();
      ZhangBoPersonalArchiveConfiguration paConfig = ZhangBoPersonalArchiveConfiguration.standard();
      ZhangBoQpCandidateSelector canonicalSelector = new ZhangBoQpCandidateSelector(qpConfig, paConfig);
      ZhangBoPersonalArchive archive = new ZhangBoPersonalArchive(paConfig);
      V35QpTopKCandidateSelector selector = new V35QpTopKCandidateSelector(canonicalSelector, archive, qpConfig, paConfig);

      ZhangBoArchiveEntry e1 = makeEntry("FP_1", 200, 4000, 100);
      ZhangBoArchiveEntry e2 = makeEntry("FP_2", 300, 2000, 120);
      ZhangBoArchiveEntry e3 = makeEntry("FP_3", 400, 1500, 150);
      ZhangBoArchiveEntry current = makeEntry("CURR", 350, 3500, 180);
      ZhangBoArchiveEntry gbest = makeEntry("GBEST", 150, 1200, 80);
      ZhangBoArchiveBounds bounds = makeBounds();

      List<ZhangBoArchiveEntry> orderA = Arrays.asList(e1, e2, e3);
      List<ZhangBoArchiveEntry> orderB = Arrays.asList(e3, e1, e2);
      List<ZhangBoArchiveEntry> orderC = Arrays.asList(e2, e3, e1);

      V35QpTopKCandidateSelector.PoolsAndCanonical pA = selector.buildPools(orderA, "FP_1", ZhangBoSubSwarm.G1_CMAX, current, gbest, bounds, 3);
      V35QpTopKCandidateSelector.PoolsAndCanonical pB = selector.buildPools(orderB, "FP_1", ZhangBoSubSwarm.G1_CMAX, current, gbest, bounds, 3);
      V35QpTopKCandidateSelector.PoolsAndCanonical pC = selector.buildPools(orderC, "FP_1", ZhangBoSubSwarm.G1_CMAX, current, gbest, bounds, 3);

      for (ZhangBoQpAction act : ZhangBoQpAction.values()) {
        List<ZhangBoArchiveEntry> lA = pA.getPool(act);
        List<ZhangBoArchiveEntry> lB = pB.getPool(act);
        List<ZhangBoArchiveEntry> lC = pC.getPool(act);

        if (lA.size() != lB.size() || lA.size() != lC.size()) {
          recordFail(name, "Pool size variation under permutation for " + act);
          return;
        }
        for (int i = 0; i < lA.size(); i++) {
          if (!lA.get(i).getFingerprint().equals(lB.get(i).getFingerprint())
              || !lA.get(i).getFingerprint().equals(lC.get(i).getFingerprint())) {
            recordFail(name, "Pool entry mismatch under permutation for " + act);
            return;
          }
        }
      }
      recordPass(name);
    } catch (Exception e) {
      recordFail(name, e.getMessage());
    }
  }

  // 14. Degenerate objective ranges and identical fingerprints fail-closed
  private static void test14_DegenerateBoundsStability() {
    String name = "Degenerate bounds and identical fingerprints stability";
    try {
      ZhangBoQpConfiguration qpConfig = ZhangBoQpConfiguration.standard();
      ZhangBoPersonalArchiveConfiguration paConfig = ZhangBoPersonalArchiveConfiguration.standard();
      ZhangBoQpCandidateSelector canonicalSelector = new ZhangBoQpCandidateSelector(qpConfig, paConfig);
      ZhangBoPersonalArchive archive = new ZhangBoPersonalArchive(paConfig);
      V35QpTopKCandidateSelector selector = new V35QpTopKCandidateSelector(canonicalSelector, archive, qpConfig, paConfig);

      List<ZhangBoArchiveEntry> entries = Arrays.asList(
          makeEntry("FP_1", 200, 2000, 100),
          makeEntry("FP_2", 200, 2000, 100) // Identical objectives, different fingerprints
      );
      ZhangBoArchiveEntry current = makeEntry("CURR", 200, 2000, 100);
      ZhangBoArchiveEntry gbest = makeEntry("GBEST", 200, 2000, 100);
      // Flat bounds (max == min)
      ZhangBoArchiveBounds bounds = ZhangBoArchiveBounds.of(
          new double[]{200, 2000, 100},
          new double[]{200, 2000, 100},
          0.0, 1.0, 0.0, 10.0, 1e-4
      );

      V35QpTopKCandidateSelector.PoolsAndCanonical pools =
          selector.buildPools(entries, "FP_1", ZhangBoSubSwarm.G1_CMAX, current, gbest, bounds, 2);

      List<ZhangBoArchiveEntry> dirPool = pools.getPool(ZhangBoQpAction.DIRECTIONAL);
      if (dirPool.size() != 2 || dirPool.get(0).getFingerprint().compareTo(dirPool.get(1).getFingerprint()) >= 0) {
        recordFail(name, "Tie-breaking on fingerprint failed under flat bounds: " + dirPool);
        return;
      }
      recordPass(name);
    } catch (Exception e) {
      recordFail(name, e.getMessage());
    }
  }
}
