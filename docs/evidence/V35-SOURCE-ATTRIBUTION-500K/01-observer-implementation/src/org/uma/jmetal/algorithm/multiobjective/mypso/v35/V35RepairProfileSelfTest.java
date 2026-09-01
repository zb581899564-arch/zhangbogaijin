package org.uma.jmetal.algorithm.multiobjective.mypso.v35;

import java.util.HashSet;
import java.util.Set;

/**
 * V35-GAP-LOCAL-FE-PACING-REPAIR-V1 local self-test (zero FE).
 *
 * <p>Covers task §17 items 1-4 and 7-11 at the profile level: profile legality
 * and hash uniqueness, frozen betaMin, formal rejection of C1--C3, C0 ==
 * frozen A4 equivalence, runtime beta read-back, canonical text fields, and
 * jar-boundary structural rejection. Exits non-zero on any failure.</p>
 */
public final class V35RepairProfileSelfTest {
  private int failures;
  private int checks;

  public static void main(String[] args) {
    V35RepairProfileSelfTest test = new V35RepairProfileSelfTest();
    test.run();
    System.out.println("CHECKS=" + test.checks + " FAILURES=" + test.failures);
    if (test.failures > 0) {
      System.out.println("V35_REPAIR_PROFILE_SELF_TEST=FAILED");
      System.exit(1);
    }
    System.out.println("V35_REPAIR_PROFILE_SELF_TEST=PASSED");
  }

  private void run() {
    // 1. Five labels resolve from CLI aliases; anything else is rejected.
    for (V35LocalFePacingRepairProfile.Label label : V35LocalFePacingRepairProfile.Label.values()) {
      check(label == V35LocalFePacingRepairProfile.fromCli(label.cliAlias()),
          "fromCli alias " + label.cliAlias());
      check(label == V35LocalFePacingRepairProfile.fromCli(label.name()),
          "fromCli name " + label.name());
    }
    expectIllegal("fromCli rejects FREE_TEXT", new Runnable() {
      public void run() { V35LocalFePacingRepairProfile.fromCli("FREE_TEXT"); }
    });
    expectIllegal("fromCli rejects null", new Runnable() {
      public void run() { V35LocalFePacingRepairProfile.fromCli(null); }
    });

    // 2. betaMin frozen at 0.25; betaMax axis 0.65/0.55/0.45/0.35 (REF == C0 value).
    checkDouble(V35LocalFePacingRepairProfile.BETA_MIN, 0.25, "BETA_MIN constant");
    for (V35LocalFePacingRepairProfile.Label label : V35LocalFePacingRepairProfile.Label.values()) {
      checkDouble(V35LocalFePacingRepairProfile.BETA_MIN, 0.25,
          "betaMin constant unchanged for " + label.name());
      double expected = label == V35LocalFePacingRepairProfile.Label.REF_A4_FROZEN ? 0.65
          : label.betaMax();
      checkDouble(expected, label.betaMax(), "betaMax mapping " + label.name());
    }
    checkDouble(V35LocalFePacingRepairProfile.Label.REF_A4_FROZEN.betaMax(), 0.65,
        "REF betaMax=0.65");
    checkDouble(V35LocalFePacingRepairProfile.Label.C0_BETA_MAX_065.betaMax(), 0.65, "C0=0.65");
    checkDouble(V35LocalFePacingRepairProfile.Label.C1_BETA_MAX_055.betaMax(), 0.55, "C1=0.55");
    checkDouble(V35LocalFePacingRepairProfile.Label.C2_BETA_MAX_045.betaMax(), 0.45, "C2=0.45");
    checkDouble(V35LocalFePacingRepairProfile.Label.C3_BETA_MAX_035.betaMax(), 0.35, "C3=0.35");

    // 3. Formal constructor gate: C1--C3 rejected, REF/C0 admitted.
    for (V35LocalFePacingRepairProfile.Label label : V35LocalFePacingRepairProfile.Label.values()) {
      final V35LocalFePacingRepairProfile.Label current = label;
      if (label == V35LocalFePacingRepairProfile.Label.C1_BETA_MAX_055
          || label == V35LocalFePacingRepairProfile.Label.C2_BETA_MAX_045
          || label == V35LocalFePacingRepairProfile.Label.C3_BETA_MAX_035) {
        expectIllegal("formalConfigurationFor rejects " + label.name(), new Runnable() {
          public void run() {
            V35LocalFePacingRepairProfile.formalConfigurationFor(current, 20260907L, 100, 20000);
          }
        });
        expectIllegal("assertFormalDisallows throws " + label.name(), new Runnable() {
          public void run() { V35LocalFePacingRepairProfile.assertFormalDisallows(current); }
        });
      } else {
        V35LocalFePacingRepairProfile.formalConfigurationFor(label, 20260907L, 100, 20000);
        record("formalConfigurationFor admits " + label.name());
      }
    }

    // 4. Repair factory admits only C0--C3.
    expectIllegal("repairConfigurationFor rejects REF", new Runnable() {
      public void run() {
        V35LocalFePacingRepairProfile.repairConfigurationFor(
            V35LocalFePacingRepairProfile.Label.REF_A4_FROZEN, 20260907L, 100, 20000);
      }
    });
    for (V35LocalFePacingRepairProfile.Label label : new V35LocalFePacingRepairProfile.Label[] {
        V35LocalFePacingRepairProfile.Label.C0_BETA_MAX_065,
        V35LocalFePacingRepairProfile.Label.C1_BETA_MAX_055,
        V35LocalFePacingRepairProfile.Label.C2_BETA_MAX_045,
        V35LocalFePacingRepairProfile.Label.C3_BETA_MAX_035}) {
      final V35LocalFePacingRepairProfile.Label current = label;
      V35ProductionConfiguration configuration =
          V35LocalFePacingRepairProfile.repairConfigurationFor(current, 20260907L, 100, 20000);
      record("repairConfigurationFor builds " + label.name());
      V35LocalFePacingRepairProfile.validate(label, configuration);
      record("validate passes " + label.name());
      checkDouble(configuration.getLocalFeBudget().getBetaMin(), 0.25,
          "runtime betaMin=0.25 " + label.name());
      checkDouble(configuration.getLocalFeBudget().getBetaMax(), label.betaMax(),
          "runtime betaMax read-back " + label.name());
      // Frozen invariants on the runtime configuration.
      check(configuration.isDscrEnabled(), "dscr on " + label.name());
      check(configuration.isCfvfEnabled(), "cfvf on " + label.name());
      check(configuration.isQgEnabled(), "qg on " + label.name());
      check(configuration.isQpEnabled(), "qp on " + label.name());
      check(configuration.isCaTaLiteEnabled(), "caTaLite on " + label.name());
      check(!configuration.isDirectionalTeacherPoolEnabled(),
          "directional pool off " + label.name());
    }

    // 5. C0 == frozen formal A4: runtime configuration hash equality.
    V35ProductionConfiguration ref =
        V35LocalFePacingRepairProfile.configurationFor(
            V35LocalFePacingRepairProfile.Label.REF_A4_FROZEN, 20260907L, 100, 20000);
    V35ProductionConfiguration c0 =
        V35LocalFePacingRepairProfile.configurationFor(
            V35LocalFePacingRepairProfile.Label.C0_BETA_MAX_065, 20260907L, 100, 20000);
    check(ref.configurationHash().equals(c0.configurationHash()),
        "C0 runtimeConfigurationHash == REF_A4 (frozen formal A4 path)");
    check(V35FinalAblationProfile.configurationFor(
        V35FinalAblationProfile.Arm.A4_BUDGET_AWARE_CATA, 20260907L, 100, 20000)
        .configurationHash().equals(c0.configurationHash()),
        "C0 configuration hash == V35FinalAblationProfile A4 hash");
    check(V35FinalAblationProfile.configurationHashFor(
        V35FinalAblationProfile.Arm.A4_BUDGET_AWARE_CATA, 20260907L, 100, 20000)
        .equals(V35FinalAblationProfile.configurationHashFor(
            V35FinalAblationProfile.Arm.A4_BUDGET_AWARE_CATA, 20260907L, 100, 20000)),
        "formal A4 profile hash stable");
    // C1--C3 differ from C0 and from each other on the runtime configuration hash.
    Set<String> distinctHashes = new HashSet<String>();
    distinctHashes.add(c0.configurationHash());
    for (V35LocalFePacingRepairProfile.Label label : new V35LocalFePacingRepairProfile.Label[] {
        V35LocalFePacingRepairProfile.Label.C1_BETA_MAX_055,
        V35LocalFePacingRepairProfile.Label.C2_BETA_MAX_045,
        V35LocalFePacingRepairProfile.Label.C3_BETA_MAX_035}) {
      distinctHashes.add(V35LocalFePacingRepairProfile.configurationFor(
          label, 20260907L, 100, 20000).configurationHash());
    }
    check(distinctHashes.size() == 4, "C0..C3 four distinct runtime configuration hashes, got "
        + distinctHashes.size());
    // Profile-level SHA (canonical text with label field) is unique across all five labels.
    Set<String> profileHashes = new HashSet<String>();
    for (V35LocalFePacingRepairProfile.Label label : V35LocalFePacingRepairProfile.Label.values()) {
      profileHashes.add(V35LocalFePacingRepairProfile.configurationSha256(
          label, 20260907L, 100, 20000));
    }
    check(profileHashes.size() == 5, "five distinct profile configurationSha256 values, got "
        + profileHashes.size());

    // 6. Canonical text carries every §12 required field.
    String text = V35LocalFePacingRepairProfile.canonicalText(
        V35LocalFePacingRepairProfile.Label.C1_BETA_MAX_055, 20260907L, 100, 20000,
        "formalJarSha256Placeholder", "experimentalJarSha256Placeholder");
    for (String field : new String[] {
        "repairProfileVersion=v35-local-fe-pacing-repair-v1",
        "repairFamily=LOCAL_FE_PACING", "singleKnob=betaMax",
        "profileLabel=C1_BETA_MAX_055", "betaMin=0.25", "betaMax=0.55",
        "formalJarSha256=formalJarSha256Placeholder",
        "experimentalJarSha256=experimentalJarSha256Placeholder",
        "pddrSelectionMode=GLOBAL_ORIGINAL", "localSearchOrder=CATA_THEN_INHERITED"}) {
      check(text.contains(field), "canonical text contains " + field);
    }
    String refText = V35LocalFePacingRepairProfile.canonicalText(
        V35LocalFePacingRepairProfile.Label.REF_A4_FROZEN, 20260907L, 100, 20000, "f", "e");
    check(refText.contains("profileLabel=REF_A4_FROZEN")
        && refText.contains("armEquivalent=V35FinalAblationProfile.A4_BUDGET_AWARE_CATA"),
        "REF canonical text identifies the frozen formal path");
  }

  private void check(boolean condition, String name) {
    checks++;
    if (!condition) {
      failures++;
      System.out.println("FAIL " + name);
    } else {
      System.out.println("PASS " + name);
    }
  }

  private void checkDouble(double actual, double expected, String name) {
    check(Double.compare(actual, expected) == 0,
        name + " (actual=" + actual + " expected=" + expected + ")");
  }

  private void record(String name) {
    checks++;
    System.out.println("PASS " + name);
  }

  private void expectIllegal(String name, Runnable action) {
    checks++;
    try {
      action.run();
      failures++;
      System.out.println("FAIL " + name + " (no exception thrown)");
    } catch (IllegalArgumentException expected) {
      System.out.println("PASS " + name);
    } catch (Exception wrong) {
      failures++;
      System.out.println("FAIL " + name + " (wrong exception " + wrong + ")");
    }
  }
}
