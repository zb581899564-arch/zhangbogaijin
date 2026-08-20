package org.uma.jmetal.algorithm.multiobjective.mypso.p8;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import org.uma.jmetal.problem.multiobjective.dfsp.fatigue.shift.ZhangBoShiftConfiguration;
import org.uma.jmetal.problem.multiobjective.dfsp.fatigue.shift.ZhangBoShiftMode;

/**
 * Immutable, decision-complete mechanism vector for one P8 ablation entry.
 * It is deliberately separate from legacy builder switches; the author path is retained
 * only as an explicit A0 diagnostic while formal vectors use the canonical production line.
 */
public final class P8AblationProfile implements Serializable {
  private static final long serialVersionUID = 1L;

  public enum DecoderMode {
    /** Explicit author-code diagnostic mode; never a formal P8 production mode. */
    AUTHOR_DIAGNOSTIC,
    /** @deprecated retained only for source compatibility with the P8-v2 API. */
    @Deprecated
    AUTHOR_ACTUAL,
    /** Deterministic canonical HMOPSO-QGS decoder used by the P8-v3 production line. */
    DETERMINISTIC_CANONICAL,
    CORRECTED_NO_FATIGUE,
    ACCUMULATION_ONLY,
    ACCUMULATION_RECOVERY,
    FATIGUE_AWARE_SELECTION
  }

  public enum RandomnessMode { AUTHOR_UNCONTROLLED, P8_REPLAYABLE }

  public enum ResourceFlightMode {
    /** Canonical/published baseline GA resource update used by formal P8-v3 labels. */
    BASELINE_GA,
    /** Uncontrolled author resource path, reserved for A0 diagnostics. */
    AUTHOR_GA,
    FA_LEADER_ONLY,
    INDEPENDENT_FMW,
    COUPLED_FMW
  }

  public enum PersonalLeaderMode {
    AUTHOR_SINGLE,
    ARCHIVE_DIRECTIONAL,
    ARCHIVE_RANDOM_FOUR,
    QP_LINEAGE_ARCHIVE
  }

  public enum VnsMode {
    OFF,
    O1_O9_FIXED,
    O1_O13_FIXED,
    NEED_AWARE,
    TA_CONTEXT_FREE,
    TA_CONTEXT,
    TA_COST,
    TA_FAT_FULL
  }

  /** Current P8 profile schema.  P8-v2 evidence is historical and not loadable here. */
  public static final String VERSION = "p8-ablation-v5-shift";
  public static final String LEGACY_VERSION = "p8-ablation-v3";

  private final DecoderMode decoderMode;
  private final RandomnessMode randomnessMode;
  private final ResourceFlightMode resourceFlightMode;
  private final boolean resourceInertia;
  private final boolean legalExploration;
  private final boolean qg;
  private final boolean evaluatedPddr;
  private final boolean lineageArchive;
  private final PersonalLeaderMode personalLeaderMode;
  private final boolean blockFrozenDualQ;
  private final VnsMode vnsMode;
  private final boolean canonicalBaseline;
  private final ZhangBoShiftMode shiftMode;

  private P8AblationProfile(Builder builder) {
    decoderMode = required(builder.decoderMode, "decoderMode");
    randomnessMode = required(builder.randomnessMode, "randomnessMode");
    resourceFlightMode = required(builder.resourceFlightMode, "resourceFlightMode");
    resourceInertia = builder.resourceInertia;
    legalExploration = builder.legalExploration;
    qg = builder.qg;
    evaluatedPddr = builder.evaluatedPddr;
    lineageArchive = builder.lineageArchive;
    personalLeaderMode = required(builder.personalLeaderMode, "personalLeaderMode");
    blockFrozenDualQ = builder.blockFrozenDualQ;
    vnsMode = required(builder.vnsMode, "vnsMode");
    canonicalBaseline = builder.canonicalBaseline;
    shiftMode = required(builder.shiftMode, "shiftMode");
    validate();
  }

  private void validate() {
    if (randomnessMode == RandomnessMode.AUTHOR_UNCONTROLLED
        && (decoderMode != DecoderMode.AUTHOR_DIAGNOSTIC
        && decoderMode != DecoderMode.AUTHOR_ACTUAL
        || resourceFlightMode != ResourceFlightMode.AUTHOR_GA
        || qg || evaluatedPddr || lineageArchive
        || personalLeaderMode != PersonalLeaderMode.AUTHOR_SINGLE
        || blockFrozenDualQ || vnsMode != VnsMode.OFF)) {
      throw new IllegalArgumentException(
          "AUTHOR_UNCONTROLLED is reserved for A0 author diagnostic profile");
    }
    if (decoderMode == DecoderMode.DETERMINISTIC_CANONICAL
        && randomnessMode != RandomnessMode.P8_REPLAYABLE) {
      throw new IllegalArgumentException(
          "DETERMINISTIC_CANONICAL requires the replayable P8 random source");
    }
    if (decoderMode == DecoderMode.AUTHOR_DIAGNOSTIC
        && randomnessMode != RandomnessMode.AUTHOR_UNCONTROLLED) {
      throw new IllegalArgumentException(
          "AUTHOR_DIAGNOSTIC must preserve the author's uncontrolled random path");
    }
    if (isAuthorDiagnostic() && shiftMode != ZhangBoShiftMode.NONE) {
      throw new IllegalArgumentException("Author diagnostic cannot use the canonical shift decoder");
    }
    if (!isAuthorDiagnostic() && shiftMode != ZhangBoShiftMode.LEFT_RIGHT) {
      throw new IllegalArgumentException(
          "All formal P8/P9 profiles must explicitly share LEFT_RIGHT shift semantics");
    }
    if (canonicalBaseline
        && (isAuthorDiagnostic()
        || randomnessMode != RandomnessMode.P8_REPLAYABLE
        || resourceFlightMode == ResourceFlightMode.AUTHOR_GA
        || !qg || !evaluatedPddr || vnsMode == VnsMode.OFF)) {
      throw new IllegalArgumentException(
          "canonicalBaseline requires replayable canonical decoder, Qg, PDDR and O1-O9+");
    }
    if (personalLeaderMode != PersonalLeaderMode.AUTHOR_SINGLE && !lineageArchive) {
      throw new IllegalArgumentException("Archive-derived personal leaders require lineage archive");
    }
    if (lineageArchive && !evaluatedPddr) {
      throw new IllegalArgumentException("Lineage archive requires evaluated PDDR");
    }
    if (blockFrozenDualQ
        && (personalLeaderMode != PersonalLeaderMode.QP_LINEAGE_ARCHIVE || !qg)) {
      throw new IllegalArgumentException("Block-frozen dual Q requires Q-pbest and Q-gbest");
    }
    if (isTestAndApply(vnsMode) && !blockFrozenDualQ) {
      throw new IllegalArgumentException("Test-and-Apply profiles require the B5 global backbone");
    }
  }

  private static <T> T required(T value, String name) {
    if (value == null) throw new IllegalArgumentException(name + " cannot be null");
    return value;
  }

  public static Builder builder() { return new Builder(); }

  public static Builder from(P8AblationProfile profile) {
    return new Builder()
        .decoder(profile.decoderMode)
        .randomness(profile.randomnessMode)
        .resourceFlight(profile.resourceFlightMode)
        .resourceInertia(profile.resourceInertia)
        .legalExploration(profile.legalExploration)
        .qg(profile.qg)
        .evaluatedPddr(profile.evaluatedPddr)
        .lineageArchive(profile.lineageArchive)
        .personalLeader(profile.personalLeaderMode)
        .blockFrozenDualQ(profile.blockFrozenDualQ)
        .vns(profile.vnsMode)
        .canonicalBaseline(profile.canonicalBaseline)
        .shiftMode(profile.shiftMode);
  }

  public DecoderMode getDecoderMode() { return decoderMode; }
  public RandomnessMode getRandomnessMode() { return randomnessMode; }
  public ResourceFlightMode getResourceFlightMode() { return resourceFlightMode; }
  public boolean hasResourceInertia() { return resourceInertia; }
  public boolean hasLegalExploration() { return legalExploration; }
  public boolean isQgEnabled() { return qg; }
  public boolean isEvaluatedPddrEnabled() { return evaluatedPddr; }
  public boolean isLineageArchiveEnabled() { return lineageArchive; }
  public PersonalLeaderMode getPersonalLeaderMode() { return personalLeaderMode; }
  public boolean isBlockFrozenDualQ() { return blockFrozenDualQ; }
  public VnsMode getVnsMode() { return vnsMode; }
  public boolean isCanonicalBaseline() { return canonicalBaseline; }
  public ZhangBoShiftMode getShiftMode() { return shiftMode; }
  public ZhangBoShiftConfiguration getShiftConfiguration() {
    return shiftMode == ZhangBoShiftMode.LEFT_RIGHT
        ? ZhangBoShiftConfiguration.formalLeftRight()
        : new ZhangBoShiftConfiguration(shiftMode,
            ZhangBoShiftConfiguration.DEFAULT_EPSILON,
            ZhangBoShiftConfiguration.DEFAULT_LEFT_CANDIDATES,
            ZhangBoShiftConfiguration.DEFAULT_RIGHT_ATTEMPTS, false);
  }
  public boolean isAuthorDiagnostic() {
    return decoderMode == DecoderMode.AUTHOR_DIAGNOSTIC
        || decoderMode == DecoderMode.AUTHOR_ACTUAL;
  }
  public boolean isDeterministicCanonical() {
    return decoderMode == DecoderMode.DETERMINISTIC_CANONICAL;
  }
  /** Semantic line for evidence and runtime guards. */
  public String getSemanticTag() {
    if (isAuthorDiagnostic()) return "author_diagnostic";
    if (decoderMode == DecoderMode.DETERMINISTIC_CANONICAL) return "deterministic_canonical";
    if (usesFatigueParameters()) return "fatigue_improved";
    return "deterministic_canonical";
  }
  /** Four-vector solution tag accepted by the canonical problem implementation. */
  public String getSolutionSemanticTag() {
    if (isAuthorDiagnostic()) return "author_actual";
    switch (decoderMode) {
      case ACCUMULATION_ONLY: return "fatigue_fm1";
      case ACCUMULATION_RECOVERY: return "fatigue_fm2";
      case FATIGUE_AWARE_SELECTION: return "fatigue_fm3";
      case DETERMINISTIC_CANONICAL:
      case CORRECTED_NO_FATIGUE:
      default: return "deterministic_canonical";
    }
  }
  public boolean usesFatigueParameters() {
    return decoderMode == DecoderMode.ACCUMULATION_ONLY
        || decoderMode == DecoderMode.ACCUMULATION_RECOVERY
        || decoderMode == DecoderMode.FATIGUE_AWARE_SELECTION;
  }
  public boolean usesCorrectedDecoder() { return !isAuthorDiagnostic(); }
  public boolean isCfvfFamily() {
    return resourceFlightMode == ResourceFlightMode.FA_LEADER_ONLY
        || resourceFlightMode == ResourceFlightMode.INDEPENDENT_FMW
        || resourceFlightMode == ResourceFlightMode.COUPLED_FMW;
  }
  public boolean isQpEnabled() {
    return personalLeaderMode == PersonalLeaderMode.QP_LINEAGE_ARCHIVE;
  }
  public boolean isCaTaEnabled() {
    return isTestAndApply(vnsMode);
  }
  public boolean isLocalSearchEnabled() { return vnsMode != VnsMode.OFF; }
  public boolean isFixedNeighborhoodEnabled() {
    return vnsMode == VnsMode.O1_O9_FIXED || vnsMode == VnsMode.O1_O13_FIXED;
  }
  public boolean isNeedSelectionEnabled() {
    return vnsMode == VnsMode.NEED_AWARE || isTestAndApply(vnsMode);
  }
  public boolean isContextEnabled() {
    return vnsMode == VnsMode.TA_CONTEXT || vnsMode == VnsMode.TA_COST
        || vnsMode == VnsMode.TA_FAT_FULL;
  }
  public boolean isCostCreditEnabled() {
    return vnsMode == VnsMode.TA_COST || vnsMode == VnsMode.TA_FAT_FULL;
  }
  public boolean isFatBottleneckEnabled() { return vnsMode == VnsMode.TA_FAT_FULL; }

  private static boolean isTestAndApply(VnsMode mode) {
    return mode == VnsMode.TA_CONTEXT_FREE || mode == VnsMode.TA_CONTEXT
        || mode == VnsMode.TA_COST || mode == VnsMode.TA_FAT_FULL;
  }

  public String canonicalText() {
    return "ablationSchema=" + VERSION + '\n'
        + "semanticTag=" + getSolutionSemanticTag() + '\n'
        + canonicalTextWithoutSemanticTag();
  }

  /** Canonical mechanism fields for embedding under a configuration-owned semantic tag. */
  public String canonicalTextWithoutSemanticTag() {
    return "ablationEvidenceFamily=" + getSemanticTag() + '\n'
        + "decoderMode=" + decoderMode + '\n'
        + "randomnessMode=" + randomnessMode + '\n'
        + "resourceFlightMode=" + resourceFlightMode + '\n'
        + "resourceInertia=" + resourceInertia + '\n'
        + "legalExploration=" + legalExploration + '\n'
        + "qg=" + qg + '\n'
        + "evaluatedPddr=" + evaluatedPddr + '\n'
        + "lineageArchive=" + lineageArchive + '\n'
        + "personalLeaderMode=" + personalLeaderMode + '\n'
        + "blockFrozenDualQ=" + blockFrozenDualQ + '\n'
        + "vnsMode=" + vnsMode + '\n'
        + "shiftSemanticsVersion=" + ZhangBoShiftConfiguration.ALGORITHM_SEMANTICS_VERSION + '\n'
        + "shiftMode=" + shiftMode + '\n'
        + "shiftEpsilon=" + ZhangBoShiftConfiguration.DEFAULT_EPSILON + '\n'
        + "shiftMaximumLeftCandidates="
        + ZhangBoShiftConfiguration.DEFAULT_LEFT_CANDIDATES + '\n'
        + "shiftMaximumRightAttempts="
        + ZhangBoShiftConfiguration.DEFAULT_RIGHT_ATTEMPTS + '\n'
        + "canonicalBaseline=" + canonicalBaseline + '\n';
  }

  /** Stable lowercase SHA-256 identity of the complete mechanism vector. */
  public String mechanismVectorHash() {
    try {
      byte[] digest = MessageDigest.getInstance("SHA-256")
          .digest(canonicalText().getBytes(StandardCharsets.UTF_8));
      StringBuilder result = new StringBuilder();
      for (byte value : digest) result.append(String.format("%02x", value & 0xff));
      return result.toString();
    } catch (NoSuchAlgorithmException exception) {
      throw new IllegalStateException("SHA-256 unavailable", exception);
    }
  }

  /** Returns the exact canonical field names that differ. */
  public List<String> differenceKeys(P8AblationProfile other) {
    if (other == null) throw new IllegalArgumentException("other cannot be null");
    List<String> result = new ArrayList<>();
    if (decoderMode != other.decoderMode) result.add("decoderMode");
    if (randomnessMode != other.randomnessMode) result.add("randomnessMode");
    if (resourceFlightMode != other.resourceFlightMode) result.add("resourceFlightMode");
    if (resourceInertia != other.resourceInertia) result.add("resourceInertia");
    if (legalExploration != other.legalExploration) result.add("legalExploration");
    if (qg != other.qg) result.add("qg");
    if (evaluatedPddr != other.evaluatedPddr) result.add("evaluatedPddr");
    if (lineageArchive != other.lineageArchive) result.add("lineageArchive");
    if (personalLeaderMode != other.personalLeaderMode) result.add("personalLeaderMode");
    if (blockFrozenDualQ != other.blockFrozenDualQ) result.add("blockFrozenDualQ");
    if (vnsMode != other.vnsMode) result.add("vnsMode");
    if (shiftMode != other.shiftMode) result.add("shiftMode");
    if (canonicalBaseline != other.canonicalBaseline) result.add("canonicalBaseline");
    return Collections.unmodifiableList(result);
  }

  public static final class Builder {
    private DecoderMode decoderMode = DecoderMode.DETERMINISTIC_CANONICAL;
    private RandomnessMode randomnessMode = RandomnessMode.P8_REPLAYABLE;
    private ResourceFlightMode resourceFlightMode = ResourceFlightMode.BASELINE_GA;
    private boolean resourceInertia = true;
    private boolean legalExploration = true;
    private boolean qg;
    private boolean evaluatedPddr;
    private boolean lineageArchive;
    private PersonalLeaderMode personalLeaderMode = PersonalLeaderMode.AUTHOR_SINGLE;
    private boolean blockFrozenDualQ;
    private VnsMode vnsMode = VnsMode.OFF;
    private boolean canonicalBaseline;
    private ZhangBoShiftMode shiftMode = ZhangBoShiftMode.LEFT_RIGHT;

    public Builder decoder(DecoderMode value) { decoderMode = value; return this; }
    public Builder randomness(RandomnessMode value) { randomnessMode = value; return this; }
    public Builder resourceFlight(ResourceFlightMode value) { resourceFlightMode = value; return this; }
    public Builder resourceInertia(boolean value) { resourceInertia = value; return this; }
    public Builder legalExploration(boolean value) { legalExploration = value; return this; }
    public Builder qg(boolean value) { qg = value; return this; }
    public Builder evaluatedPddr(boolean value) { evaluatedPddr = value; return this; }
    public Builder lineageArchive(boolean value) { lineageArchive = value; return this; }
    public Builder personalLeader(PersonalLeaderMode value) { personalLeaderMode = value; return this; }
    public Builder blockFrozenDualQ(boolean value) { blockFrozenDualQ = value; return this; }
    public Builder vns(VnsMode value) { vnsMode = value; return this; }
    public Builder canonicalBaseline(boolean value) { canonicalBaseline = value; return this; }
    public Builder shiftMode(ZhangBoShiftMode value) { shiftMode = value; return this; }
    public P8AblationProfile build() { return new P8AblationProfile(this); }
  }
}
