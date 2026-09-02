package org.uma.jmetal.algorithm.multiobjective.mypso.v35;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * V35-GAP-LOCAL-FE-PACING-REPAIR-V1: the single-knob betaMax repair profile.
 *
 * <p>Naming per {@code docs/evidence/V35-GAP-LEVERAGE-AUDIT/
 * NAMING_AND_CAUSAL_BOUNDARY_CORRECTION.md}: selectedRepairFamily=LOCAL_FE_PACING,
 * singleKnob=betaMax. {@code beta(u) = betaMin + (betaMax - betaMin) * u^2} with
 * {@code betaMin=0.25} frozen; only {@code betaMax} moves on one semantic axis.</p>
 *
 * <p>Labels: {@code REF_A4} is the frozen formal A4 path (delegates to
 * {@link V35FinalAblationProfile#configurationFor}); {@code C0} is betaMax=0.65,
 * the exact current A4 value, and must remain behaviorally identical to
 * {@code REF_A4}; {@code C1/C2/C3} are 0.55/0.45/0.35. The formal constructor
 * {@link #formalConfigurationFor} refuses C1--C3: repair profiles are
 * experimental-only and may only be consumed by
 * {@code org.uma.jmetal.runner.lc_psode.V35LocalFePacingRepairRunner} from the
 * independent experimental jar. The frozen formal jar
 * ({@code 8dad8f40...bad8b9}) contains none of this class, so formal runners
 * structurally cannot select a repair profile.</p>
 *
 * <p>Frozen semantics (unchanged by this profile): FM3, ShiftMode=NONE,
 * DEGENERATE_SINGLE_FAMILY, SEQUENCE_INDEPENDENT, objectives slots [0,1,6],
 * population 100, mixture 20/40/20/20, PDDR=GLOBAL_ORIGINAL,
 * CA-TA-then-inherited LS order, Dual-Q P5/G5 rho=0, directional teacher pool
 * off. Only the betaMax value differs across C1--C3.</p>
 */
public final class V35LocalFePacingRepairProfile {
  public static final String SEMANTIC_VERSION = "v35-local-fe-pacing-repair-v1";
  public static final double BETA_MIN = 0.25;

  public enum Label {
    REF_A4_FROZEN("REF_A4", 0.65, false),
    C0_BETA_MAX_065("C0", 0.65, true),
    C1_BETA_MAX_055("C1", 0.55, true),
    C2_BETA_MAX_045("C2", 0.45, true),
    C3_BETA_MAX_035("C3", 0.35, true);

    private final String cliAlias;
    private final double betaMax;
    private final boolean repairProfile;

    Label(String cliAlias, double betaMax, boolean repairProfile) {
      this.cliAlias = cliAlias;
      this.betaMax = betaMax;
      this.repairProfile = repairProfile;
    }

    public String cliAlias() { return cliAlias; }
    public double betaMax() { return betaMax; }
    public boolean isRepairProfile() { return repairProfile; }
  }

  private V35LocalFePacingRepairProfile() { }

  /** Resolves the CLI label. Accepts only the five frozen aliases/full names. */
  public static Label fromCli(String text) {
    if (text == null) throw new IllegalArgumentException("profile label");
    for (Label label : Label.values()) {
      if (label.name().equals(text) || label.cliAlias().equals(text)) return label;
    }
    throw new IllegalArgumentException(
        "profile must be REF_A4|C0|C1|C2|C3: " + text);
  }

  /**
   * The formal constructor gate: C1--C3 are rejected wherever the formal
   * configuration path is required. REF_A4 and C0 are admitted because C0 is
   * parameter-identical to the frozen A4.
   */
  public static void assertFormalDisallows(Label label) {
    if (label == null) throw new IllegalArgumentException("label");
    if (label.isRepairProfile() && label != Label.C0_BETA_MAX_065) {
      throw new IllegalArgumentException(
          "formal configuration path rejects repair profile " + label.name()
              + " (betaMax=" + label.betaMax() + "); only the experimental "
              + "V35LocalFePacingRepairRunner may create it");
    }
  }

  /**
   * Builds the frozen formal A4 configuration (REF_A4) or rejects repair-only
   * profiles. This is the only entry point formal callers may use.
   */
  public static V35ProductionConfiguration formalConfigurationFor(
      Label label, long seed, int populationSize, int maxEvaluations) {
    assertFormalDisallows(label);
    if (label == Label.REF_A4_FROZEN) {
      return V35FinalAblationProfile.configurationFor(
          V35FinalAblationProfile.Arm.A4_BUDGET_AWARE_CATA, seed, populationSize,
          maxEvaluations);
    }
    return repairConfigurationFor(label, seed, populationSize, maxEvaluations);
  }

  /**
   * Experimental-only factory: C0--C3. Replicates the frozen A4 builder chain
   * from {@link V35FinalAblationProfile#configurationFor} with the single
   * substitution {@code localFeBudget(of(0.25, betaMax))}. Every other builder
   * call is byte-identical to the formal A4 path.
   */
  public static V35ProductionConfiguration repairConfigurationFor(
      Label label, long seed, int populationSize, int maxEvaluations) {
    if (label == null) throw new IllegalArgumentException("label");
    if (!label.isRepairProfile()) {
      throw new IllegalArgumentException(
          "repairConfigurationFor is experimental-only; use formalConfigurationFor for "
              + label.name());
    }
    return V35ProductionConfiguration.builder()
        .seed(seed)
        .populationSize(populationSize)
        .maxEvaluations(maxEvaluations)
        .decoderMode(org.uma.jmetal.problem.multiobjective.dfsp.decoder.ProductionDecodeMode.FM3)
        .dscr(true)
        .cfvf(true)
        .qg(true)
        .qp(true)
        .caTaLite(true)
        .directionalTeacherPool(false)
        .teacherPoolSize(10)
        .bottleneckDiagnosis(V35BottleneckDiagnosisConfiguration.fullMaskNoShadow())
        .pddrSelectionMode(
            org.uma.jmetal.algorithm.multiobjective.mypso.zhangbo.PddrSelectionMode.GLOBAL_ORIGINAL)
        .localSearchOrder(V35LocalSearchOrder.CATA_THEN_INHERITED)
        .dualQCoordination(
            org.uma.jmetal.algorithm.multiobjective.mypso.zhangbo.ZhangBoDualQCoordinationConfiguration
                .blockFrozen(0.10, 5, 5))
        .localFeBudget(V35LocalFeBudgetConfiguration.of(BETA_MIN, label.betaMax()))
        .build();
  }

  /** Resolves the configuration for any label through the appropriate path. */
  public static V35ProductionConfiguration configurationFor(
      Label label, long seed, int populationSize, int maxEvaluations) {
    if (label == Label.REF_A4_FROZEN) {
      return formalConfigurationFor(label, seed, populationSize, maxEvaluations);
    }
    return repairConfigurationFor(label, seed, populationSize, maxEvaluations);
  }

  /** Repair-specific runtime validation; also re-checks the formal invariants. */
  public static void validate(Label label, V35ProductionConfiguration configuration) {
    if (label == null || configuration == null) throw new IllegalArgumentException("label/configuration");
    V35FinalAblationProfile.validate(
        V35FinalAblationProfile.Arm.A4_BUDGET_AWARE_CATA, configuration);
    V35LocalFeBudgetConfiguration budget = configuration.getLocalFeBudget();
    if (budget == null) throw new IllegalArgumentException("localFeBudget must be present");
    if (Double.compare(budget.getBetaMin(), BETA_MIN) != 0) {
      throw new IllegalArgumentException(
          "betaMin must stay frozen at " + BETA_MIN + ", got " + budget.getBetaMin());
    }
    if (Double.compare(budget.getBetaMax(), label.betaMax()) != 0) {
      throw new IllegalArgumentException(
          "runtime betaMax=" + budget.getBetaMax() + " does not match profile "
              + label.name() + " betaMax=" + label.betaMax());
    }
    if (label == Label.REF_A4_FROZEN) {
      String formalRuntimeHash = V35FinalAblationProfile.configurationFor(
          V35FinalAblationProfile.Arm.A4_BUDGET_AWARE_CATA, configuration.getSeed(),
          configuration.getPopulationSize(), configuration.getMaxEvaluations())
          .configurationHash();
      if (!formalRuntimeHash.equals(configuration.configurationHash())) {
        throw new IllegalArgumentException("REF_A4 drifted from the frozen formal A4 path");
      }
    }
  }

  /** Canonical profile text; jar hashes bind the artifact set at run time. */
  public static String canonicalText(Label label, long seed, int populationSize,
      int maxEvaluations, String formalJarSha256, String experimentalJarSha256) {
    V35ProductionConfiguration configuration =
        configurationFor(label, seed, populationSize, maxEvaluations);
    StringBuilder text = new StringBuilder()
        .append("repairProfileVersion=").append(SEMANTIC_VERSION).append('\n')
        .append("repairFamily=LOCAL_FE_PACING\n")
        .append("singleKnob=betaMax\n")
        .append("profileLabel=").append(label.name()).append('\n')
        .append("profileCliAlias=").append(label.cliAlias()).append('\n')
        .append("betaMin=").append(BETA_MIN).append('\n')
        .append("betaMax=").append(label.betaMax()).append('\n')
        .append("schedule=beta(u)=betaMin+(betaMax-betaMin)*u^2\n")
        .append("blFormula=B_L=floor(beta/(1-beta)*B_G)\n")
        .append("armEquivalent=V35FinalAblationProfile.A4_BUDGET_AWARE_CATA\n")
        .append("pddrSelectionMode=GLOBAL_ORIGINAL\n")
        .append("localSearchOrder=CATA_THEN_INHERITED\n")
        .append("directionalTeacherPool=false\n")
        .append("softFreezeRho=0.0\n")
        .append("objectives=0,1,6\n")
        .append("seed=").append(seed).append('\n')
        .append("population=").append(populationSize).append('\n')
        .append("maxFEs=").append(maxEvaluations).append('\n')
        .append("formalJarSha256=").append(formalJarSha256).append('\n')
        .append("experimentalJarSha256=").append(experimentalJarSha256).append('\n')
        .append("profileConfigurationSha256=").append(
            configurationSha256(label, seed, populationSize, maxEvaluations)).append('\n')
        .append("dualQCoordinationBegin\n")
        .append(configuration.getDualQCoordination() == null
            ? "NOT_APPLICABLE_QP_DISABLED\n" : configuration.getDualQCoordination().toCanonicalText())
        .append("dualQCoordinationEnd\n")
        .append("v35ConfigurationBegin\n")
        .append(configuration.canonicalText())
        .append("v35ConfigurationEnd\n");
    return text.toString();
  }

  /**
   * SHA-256 of the profile-level canonical text (label + jar-independent
   * configuration canonical text). Five labels yield five distinct values.
   * The REF_A4 == C0 behavioral-equivalence claim is carried separately by the
   * runtime configuration hash equality asserted in the self-test and by the
   * 20k REF/C0 byte-equivalence gate.
   */
  public static String configurationSha256(Label label, long seed, int populationSize,
      int maxEvaluations) {
    V35ProductionConfiguration configuration =
        configurationFor(label, seed, populationSize, maxEvaluations);
    return sha256(label.name() + '\n' + configuration.canonicalText()
        + (configuration.getDualQCoordination() == null
            ? "" : configuration.getDualQCoordination().toCanonicalText()));
  }

  private static String sha256(String text) {
    try {
      byte[] digest = MessageDigest.getInstance("SHA-256").digest(
          text.getBytes(StandardCharsets.UTF_8));
      StringBuilder out = new StringBuilder();
      for (byte value : digest) out.append(String.format("%02x", value & 0xff));
      return out.toString();
    } catch (NoSuchAlgorithmException error) {
      throw new IllegalStateException("SHA-256 unavailable", error);
    }
  }
}
