package org.uma.jmetal.algorithm.multiobjective.mypso.v35;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.uma.jmetal.algorithm.multiobjective.mypso.zhangbo.PddrSelectionMode;
import org.uma.jmetal.algorithm.multiobjective.mypso.zhangbo.ZhangBoDualQCoordinationConfiguration;
import org.uma.jmetal.problem.multiobjective.dfsp.decoder.ProductionDecodeMode;

/**
 * Decision-complete A0--A4 profiles for the v3.5-Final ablation gate.
 *
 * <p>This is intentionally an innovation-level ladder, not a Boolean power
 * set.  A4 adds the complete budget-aware Test-and-Apply local-search package:
 * CA-TA-Lite and its shared dynamic local-FE budget are inseparable in this
 * protocol.  It must therefore never be reported as a CA-TA-only causal
 * contrast.</p>
 */
public final class V35FinalAblationProfile {
  public static final String VERSION = "v35-final-a0-a4-ablation-v1";
  public static final double LOCAL_FE_BETA_MIN = 0.25;
  public static final double LOCAL_FE_BETA_MAX = 0.65;

  public enum Arm {
    A0_BASELINE("A0", V35FairRunner.Mode.V35_BASELINE,
        false, false, false, false, false,
        "规范 HMOPSO-QGS：保留 FM3、原 Qg、严格 PDDR、继承工厂间/O1--O9 局部搜索；"
            + "不启用 DSCR、CFVF、PA_i/Qp 或 CA-TA-Lite。"),
    A1_DSCR("A1", V35FairRunner.Mode.V35_QG1,
        true, false, false, false, false,
        "A0 + DSCR：仅在 Qg 动作前清理 stale social cache；不新增教师、奖励或 FE。"),
    A2_CFVF("A2", V35FairRunner.Mode.V35_A2,
        true, true, false, false, false,
        "A1 + CFVF：四向量认知--社会飞行；Qp/PA_i 与 CA-TA-Lite 仍关闭。"),
    A3_QP_PERSONAL_ARCHIVE("A3", V35FairRunner.Mode.V35_A3,
        true, true, true, false, false,
        "A2 + 谱系个人档案 PA_i 与 Qp（P5/G5、rho=0 硬冻结）；CA-TA-Lite 仍关闭。"),
    A4_BUDGET_AWARE_CATA("A4", V35FairRunner.Mode.V35_FULL_POOL_OFF,
        true, true, true, true, true,
        "A3 + 预算感知 Test-and-Apply N1--N5：CA-TA-Lite 与共享 dynamic local-FE budget"
            + "（beta=0.25->0.65）作为同一第三创新包；方向教师池保持关闭。");

    private final String label;
    private final V35FairRunner.Mode mode;
    private final boolean dscr;
    private final boolean cfvf;
    private final boolean qp;
    private final boolean caTaLite;
    private final boolean dynamicLocalFeBudget;
    private final String description;

    Arm(String label, V35FairRunner.Mode mode, boolean dscr, boolean cfvf, boolean qp,
        boolean caTaLite, boolean dynamicLocalFeBudget, String description) {
      this.label = label;
      this.mode = mode;
      this.dscr = dscr;
      this.cfvf = cfvf;
      this.qp = qp;
      this.caTaLite = caTaLite;
      this.dynamicLocalFeBudget = dynamicLocalFeBudget;
      this.description = description;
    }

    public String getLabel() { return label; }
    public V35FairRunner.Mode getMode() { return mode; }
    public boolean isDscrEnabled() { return dscr; }
    public boolean isCfvfEnabled() { return cfvf; }
    public boolean isQpEnabled() { return qp; }
    public boolean isCaTaLiteEnabled() { return caTaLite; }
    public boolean isDynamicLocalFeBudgetEnabled() { return dynamicLocalFeBudget; }
    public String getDescription() { return description; }
  }

  public static final List<Arm> ARMS =
      Collections.unmodifiableList(Arrays.asList(Arm.values()));

  private V35FinalAblationProfile() { }

  /** Builds the only legal runtime configuration for one final ablation arm. */
  public static V35ProductionConfiguration configurationFor(
      Arm arm, long seed, int populationSize, int maxEvaluations) {
    if (arm == null) throw new IllegalArgumentException("arm");
    V35ProductionConfiguration.Builder builder = V35ProductionConfiguration.builder()
        .seed(seed)
        .populationSize(populationSize)
        .maxEvaluations(maxEvaluations)
        .decoderMode(ProductionDecodeMode.FM3)
        .dscr(arm.isDscrEnabled())
        .cfvf(arm.isCfvfEnabled())
        .qg(true)
        .qp(arm.isQpEnabled())
        .caTaLite(arm.isCaTaLiteEnabled())
        .directionalTeacherPool(false)
        .teacherPoolSize(10)
        .bottleneckDiagnosis(V35BottleneckDiagnosisConfiguration.fullMaskNoShadow())
        .pddrSelectionMode(PddrSelectionMode.GLOBAL_ORIGINAL)
        .localSearchOrder(V35LocalSearchOrder.CATA_THEN_INHERITED);
    // P5/G5 is a Dual-Q coordination mechanism, not an A0--A2 baseline detail.
    // The runtime explicitly rejects BLOCK_FROZEN without Q-pbest, so binding it
    // before A3 would either fail or silently contaminate the lower rungs.
    if (arm.isQpEnabled()) {
      builder.dualQCoordination(ZhangBoDualQCoordinationConfiguration.blockFrozen(0.10, 5, 5));
    }
    if (arm.isDynamicLocalFeBudgetEnabled()) {
      builder.localFeBudget(V35LocalFeBudgetConfiguration.of(
          LOCAL_FE_BETA_MIN, LOCAL_FE_BETA_MAX));
    }
    return builder.build();
  }

  /** Canonical text includes the fields intentionally omitted by the legacy config serializer. */
  public static String canonicalTextFor(Arm arm, long seed, int populationSize, int maxEvaluations) {
    V35ProductionConfiguration configuration = configurationFor(arm, seed, populationSize,
        maxEvaluations);
    StringBuilder text = new StringBuilder()
        .append("ablationProfileVersion=").append(VERSION).append('\n')
        .append("arm=").append(arm.getLabel()).append('\n')
        .append("semanticTag=fatigue_improved\n")
        .append("objectives=0,1,6\n")
        .append("originalQgRetained=true\n")
        .append("inheritedLocalSearchRetained=true\n")
        .append("pddrSelectionMode=GLOBAL_ORIGINAL\n")
        .append("directionalTeacherPool=false\n")
        .append("softFreezeRho=0.0\n")
        .append("localFeBudgetMode=")
        .append(arm.isDynamicLocalFeBudgetEnabled() ? "DYNAMIC_BETA" : "LEGACY_LS_TIMES_30")
        .append('\n')
        .append("a4CausalUnit=")
        .append(arm == Arm.A4_BUDGET_AWARE_CATA
            ? "BUDGET_AWARE_CATA_PACKAGE" : "NOT_APPLICABLE")
        .append('\n')
        .append("dualQCoordinationBegin\n")
        .append(configuration.getDualQCoordination() == null
            ? "NOT_APPLICABLE_QP_DISABLED\n" : configuration.getDualQCoordination().toCanonicalText())
        .append("dualQCoordinationEnd\n")
        .append("v35ConfigurationBegin\n")
        .append(configuration.canonicalText())
        .append("v35ConfigurationEnd\n");
    return text.toString();
  }

  public static String configurationHashFor(Arm arm, long seed, int populationSize,
      int maxEvaluations) {
    try {
      byte[] digest = MessageDigest.getInstance("SHA-256").digest(
          canonicalTextFor(arm, seed, populationSize, maxEvaluations)
              .getBytes(StandardCharsets.UTF_8));
      StringBuilder result = new StringBuilder();
      for (byte value : digest) result.append(String.format("%02x", value & 0xff));
      return result.toString();
    } catch (NoSuchAlgorithmException error) {
      throw new IllegalStateException("SHA-256 unavailable", error);
    }
  }

  /** Human-auditable mechanism dimensions that change from the preceding legal arm. */
  public static List<String> addedMechanismDimensions(Arm arm) {
    if (arm == null) throw new IllegalArgumentException("arm");
    switch (arm) {
      case A0_BASELINE:
        return Collections.emptyList();
      case A1_DSCR:
        return Collections.singletonList("DSCR");
      case A2_CFVF:
        return Collections.singletonList("CFVF");
      case A3_QP_PERSONAL_ARCHIVE:
        return Collections.singletonList("PA_i+Qp");
      case A4_BUDGET_AWARE_CATA:
        return Collections.unmodifiableList(Arrays.asList("CA-TA-Lite", "dynamicLocalFeBudget"));
      default:
        throw new IllegalArgumentException("unsupported arm=" + arm);
    }
  }

  /** No non-chain combination is admitted by this profile. */
  public static void validate(Arm arm, V35ProductionConfiguration configuration) {
    if (arm == null || configuration == null) throw new IllegalArgumentException("arm/configuration");
    if (configuration.getDecoderMode() != ProductionDecodeMode.FM3
        || !configuration.isQgEnabled()
        || configuration.getPddrSelectionMode() != PddrSelectionMode.GLOBAL_ORIGINAL
        || configuration.getLocalSearchOrder() != V35LocalSearchOrder.CATA_THEN_INHERITED
        || configuration.isDirectionalTeacherPoolEnabled()) {
      throw new IllegalArgumentException("final ablation invariant drift");
    }
    if (configuration.isDscrEnabled() != arm.isDscrEnabled()
        || configuration.isCfvfEnabled() != arm.isCfvfEnabled()
        || configuration.isQpEnabled() != arm.isQpEnabled()
        || configuration.isCaTaLiteEnabled() != arm.isCaTaLiteEnabled()
        || (configuration.getLocalFeBudget() != null) != arm.isDynamicLocalFeBudgetEnabled()) {
      throw new IllegalArgumentException("arm configuration does not match " + arm.getLabel());
    }
    if (arm.isQpEnabled()) {
      if (configuration.getDualQCoordination() == null
          || !configuration.getDualQCoordination().isBlockFrozen()
          || configuration.getDualQCoordination().getSoftFreezeRho() != 0.0) {
        throw new IllegalArgumentException("A3/A4 require P5/G5 hard-frozen dual-Q coordination");
      }
    } else if (configuration.getDualQCoordination() != null) {
      throw new IllegalArgumentException("A0--A2 must not inherit dual-Q coordination");
    }
  }

  /** Exact high-level field differences, avoiding accidental Boolean-power-set claims. */
  public static List<String> differingHighLevelFields(Arm lower, Arm upper) {
    if (lower == null || upper == null || upper.ordinal() != lower.ordinal() + 1) {
      throw new IllegalArgumentException("only adjacent A0--A4 comparisons are legal");
    }
    List<String> fields = new ArrayList<>();
    if (lower.isDscrEnabled() != upper.isDscrEnabled()) fields.add("dscr");
    if (lower.isCfvfEnabled() != upper.isCfvfEnabled()) fields.add("cfvf");
    if (lower.isQpEnabled() != upper.isQpEnabled()) fields.add("PA_i+qp");
    if (lower.isCaTaLiteEnabled() != upper.isCaTaLiteEnabled()) fields.add("caTaLite");
    if (lower.isDynamicLocalFeBudgetEnabled() != upper.isDynamicLocalFeBudgetEnabled()) {
      fields.add("localFeBudget");
    }
    return Collections.unmodifiableList(fields);
  }
}
