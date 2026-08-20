package org.uma.jmetal.algorithm.multiobjective.mypso.p8;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.List;
import java.util.Set;
import org.uma.jmetal.algorithm.multiobjective.mypso.p8.P8AblationProfile.DecoderMode;
import org.uma.jmetal.algorithm.multiobjective.mypso.p8.P8AblationProfile.PersonalLeaderMode;
import org.uma.jmetal.algorithm.multiobjective.mypso.p8.P8AblationProfile.RandomnessMode;
import org.uma.jmetal.algorithm.multiobjective.mypso.p8.P8AblationProfile.ResourceFlightMode;
import org.uma.jmetal.algorithm.multiobjective.mypso.p8.P8AblationProfile.VnsMode;
import org.uma.jmetal.algorithm.multiobjective.mypso.zhangbo.ZhangBoGlobalSearchConfiguration;
import org.uma.jmetal.problem.multiobjective.dfsp.fatigue.shift.ZhangBoShiftMode;

/** Single source of truth for the 34 current shift-semantic labels and isolated A0 diagnostic. */
public final class P8ExperimentRegistry {
  public static final int DEFAULT_POPULATION = 100;
  public static final int DEFAULT_MAX_FES = 2000;
  /** Physical order: groupU1, groupC2, groupD3, groupUNew. */
  public static final int[] DEFAULT_PHYSICAL_SUBSWARMS = {20, 40, 20, 20};

  private P8ExperimentRegistry() { }

  public static List<P8ExperimentSpec> all() {
    List<P8ExperimentSpec> result = new ArrayList<>();
    addFv(result);
    addFm(result);
    addQp(result);
    addVns(result);
    addFull(result);
    assertUniqueLabels(result);
    assertCurrentMatrix(result);
    return Collections.unmodifiableList(result);
  }

  /**
   * Historical compatibility method.  P8-v2 controls are archived and deliberately not
   * returned from any current-matrix accessor.
   */
  @Deprecated
  public static List<P8ExperimentSpec> controls() {
    return Collections.emptyList();
  }

  /** @deprecated use {@link #all()}; A0 is available only through {@link #diagnostics()}. */
  @Deprecated
  public static List<P8ExperimentSpec> allWithControls() {
    return all();
  }

  /** Explicit author-code diagnostic line, excluded from formal matrices and reference fronts. */
  public static List<P8ExperimentSpec> diagnostics() {
    P8AblationProfile profile = P8AblationProfile.builder()
        .decoder(DecoderMode.AUTHOR_DIAGNOSTIC)
        .randomness(RandomnessMode.AUTHOR_UNCONTROLLED)
        .resourceFlight(ResourceFlightMode.AUTHOR_GA)
        .resourceInertia(false).legalExploration(false)
        .shiftMode(ZhangBoShiftMode.NONE)
        .build();
    P8ExperimentSpec diagnostic = new P8ExperimentSpec(
        P8MatrixKind.DIAGNOSTIC, "A0_AUTHOR_DIAGNOSTIC",
        "author Java actual behavior; diagnosis only",
        "A0_AUTHOR_DIAGNOSTIC", profile, P8RunStatus.DIAGNOSTIC_ONLY,
        "Excluded from formal matrix, reference fronts and paper claims",
        DEFAULT_POPULATION, DEFAULT_MAX_FES, DEFAULT_PHYSICAL_SUBSWARMS);
    return Collections.singletonList(diagnostic);
  }

  public static P8ExperimentSpec diagnostic() { return diagnostics().get(0); }

  /** Current matrix alias used by new runners to make the v3 boundary explicit. */
  public static List<P8ExperimentSpec> currentMatrix() { return all(); }

  public static P8ExperimentSpec find(String label) {
    for (P8ExperimentSpec spec : all()) {
      if (spec.getLabel().equals(label)) return spec;
    }
    throw new IllegalArgumentException("Unknown P8 label: " + label);
  }

  public static boolean usesFatigue(P8ExperimentSpec spec) {
    return spec.getAblationProfile().usesFatigueParameters();
  }

  public static boolean usesCorrectedDecoder(P8ExperimentSpec spec) {
    return spec.getAblationProfile().usesCorrectedDecoder();
  }

  public static ZhangBoGlobalSearchConfiguration configurationFor(
      P8ExperimentSpec spec, long seed) {
    if (spec == null) throw new IllegalArgumentException("spec cannot be null");
    if (spec.isDiagnosticOnly() || spec.getStatus() != P8RunStatus.COMPLETED) {
      throw new IllegalArgumentException("P8 entry is not a formal v3 run: " + spec.getLabel());
    }
    if (!P8AblationProfile.VERSION.equals(profileVersion(spec))) {
      throw new IllegalArgumentException("Legacy/A0 profile cannot enter current shift matrix: " + spec.getLabel());
    }
    return ZhangBoGlobalSearchConfiguration.forP8(spec.getAblationProfile(), seed);
  }

  public static String mechanismVectorHash(P8ExperimentSpec spec) {
    if (spec == null) throw new IllegalArgumentException("spec cannot be null");
    return spec.getAblationProfile().mechanismVectorHash();
  }

  private static String profileVersion(P8ExperimentSpec spec) {
    String canonical = spec.getAblationProfile().canonicalText();
    String prefix = "ablationSchema=";
    int start = canonical.indexOf(prefix);
    int end = canonical.indexOf('\n', start);
    return start < 0 ? "" : canonical.substring(start + prefix.length(), end < 0 ? canonical.length() : end);
  }

  private static void addFv(List<P8ExperimentSpec> result) {
    P8AblationProfile.Builder base = fvBase();
    add(result, P8MatrixKind.FV, "FV0", "canonical baseline GA resource update",
        base.resourceFlight(ResourceFlightMode.BASELINE_GA).build());
    add(result, P8MatrixKind.FV, "FV1", "FA-only leader update",
        fvBase().resourceFlight(ResourceFlightMode.FA_LEADER_ONLY).build());
    add(result, P8MatrixKind.FV, "FV2", "independent FA/MA/WA update",
        fvBase().resourceFlight(ResourceFlightMode.INDEPENDENT_FMW).build());
    add(result, P8MatrixKind.FV, "FV3", "coupled FMW/MW/M/W update",
        fvBase().resourceFlight(ResourceFlightMode.COUPLED_FMW).build());
    add(result, P8MatrixKind.FV, "FV4", "FV3 without resource inertia",
        fvBase().resourceFlight(ResourceFlightMode.COUPLED_FMW)
            .resourceInertia(false).build());
    add(result, P8MatrixKind.FV, "FV5", "FV3 without legal exploration",
        fvBase().resourceFlight(ResourceFlightMode.COUPLED_FMW)
            .legalExploration(false).build());
    add(result, P8MatrixKind.FV, "FV-Full", "Qg/Qp plus complete CFVF",
        b5());
  }

  private static void addFm(List<P8ExperimentSpec> result) {
    add(result, P8MatrixKind.FM, "FM0", "deterministic canonical fixed worker efficiency",
        fmBackbone(DecoderMode.DETERMINISTIC_CANONICAL).build());
    add(result, P8MatrixKind.FM, "FM1", "fatigue accumulation and duration feedback only",
        fmBackbone(DecoderMode.ACCUMULATION_ONLY).build());
    add(result, P8MatrixKind.FM, "FM2", "accumulation plus natural recovery",
        fmBackbone(DecoderMode.ACCUMULATION_RECOVERY).build());
    add(result, P8MatrixKind.FM, "FM3", "FM2 plus fatigue-aware worker selection",
        fmBackbone(DecoderMode.FATIGUE_AWARE_SELECTION).build());
  }

  private static void addQp(List<P8ExperimentSpec> result) {
    add(result, P8MatrixKind.QP, "QP0", "single baseline pbest", qpBase().build());
    add(result, P8MatrixKind.QP, "QP1", "lineage archive plus directional pbest",
        qpBase().lineageArchive(true).personalLeader(PersonalLeaderMode.ARCHIVE_DIRECTIONAL).build());
    add(result, P8MatrixKind.QP, "QP2", "lineage archive plus four random policies",
        qpBase().lineageArchive(true).personalLeader(PersonalLeaderMode.ARCHIVE_RANDOM_FOUR).build());
    add(result, P8MatrixKind.QP, "QP3", "lineage archive plus Q-pbest",
        qpBase().lineageArchive(true).personalLeader(PersonalLeaderMode.QP_LINEAGE_ARCHIVE).build());
    add(result, P8MatrixKind.QP, "QP4", "QP3 plus synchronous Q-gbest",
        qpBase().lineageArchive(true).personalLeader(PersonalLeaderMode.QP_LINEAGE_ARCHIVE)
            .qg(true).build());
    add(result, P8MatrixKind.QP, "QP5", "QP3 plus block-frozen Qp/Qg",
        qpBase().lineageArchive(true).personalLeader(PersonalLeaderMode.QP_LINEAGE_ARCHIVE)
            .qg(true).blockFrozenDualQ(true).build());
    add(result, P8MatrixKind.QP, "QP6", "QP5 plus complete CFVF",
        P8AblationProfile.from(b5()).vns(VnsMode.O1_O9_FIXED).canonicalBaseline(false).build());
  }

  private static void addVns(List<P8ExperimentSpec> result) {
    add(result, P8MatrixKind.VNS, "V0", "fixed-order original O1-O9",
        vnsBackbone(VnsMode.O1_O9_FIXED));
    add(result, P8MatrixKind.VNS, "V1", "fixed-order O1-O13",
        vnsBackbone(VnsMode.O1_O13_FIXED));
    add(result, P8MatrixKind.VNS, "V2", "V1 plus Need-aware factory selection",
        vnsBackbone(VnsMode.NEED_AWARE));
    add(result, P8MatrixKind.VNS, "V3", "V2 plus context-free Test-and-Apply",
        vnsBackbone(VnsMode.TA_CONTEXT_FREE));
    add(result, P8MatrixKind.VNS, "V4", "V3 plus context without FAT/cost credit",
        vnsBackbone(VnsMode.TA_CONTEXT));
    add(result, P8MatrixKind.VNS, "V5", "V4 plus cost credit without FAT",
        vnsBackbone(VnsMode.TA_COST));
    add(result, P8MatrixKind.VNS, "V-Full", "V5 plus FAT fatigue context",
        vnsBackbone(VnsMode.TA_FAT_FULL));
  }

  private static void addFull(List<P8ExperimentSpec> result) {
    add(result, P8MatrixKind.FULL, "B0",
        "deterministic canonical HMOPSO-QGS (Qg/PDDR/O1-O9)", b0());
    add(result, P8MatrixKind.FULL, "B1", "B0 plus fatigue-aware canonical decoder", b1());
    add(result, P8MatrixKind.FULL, "B2", "B1 plus complete CFVF",
        P8AblationProfile.from(b1()).resourceFlight(ResourceFlightMode.COUPLED_FMW)
            .resourceInertia(true).legalExploration(true).build());
    add(result, P8MatrixKind.FULL, "B3", "B2 plus lineage personal archive",
        P8AblationProfile.from(b2()).lineageArchive(true).build());
    add(result, P8MatrixKind.FULL, "B4", "B3 plus Q-pbest",
        P8AblationProfile.from(b2()).lineageArchive(true)
            .personalLeader(PersonalLeaderMode.QP_LINEAGE_ARCHIVE).build());
    add(result, P8MatrixKind.FULL, "B5", "B4 plus block-frozen dual Q", b5());
    add(result, P8MatrixKind.FULL, "B6", "B5 plus fixed-order O1-O13",
        P8AblationProfile.from(b5()).vns(VnsMode.O1_O13_FIXED).build());
    add(result, P8MatrixKind.FULL, "B7", "B6 plus contextual Test-and-Apply without FAT",
        P8AblationProfile.from(b5()).vns(VnsMode.TA_COST).build());
    add(result, P8MatrixKind.FULL, "FULL", "all approved modules including FAT context",
        P8AblationProfile.from(b5()).vns(VnsMode.TA_FAT_FULL).build());
  }

  private static P8AblationProfile.Builder base(DecoderMode decoder) {
    return P8AblationProfile.builder().decoder(decoder)
        .randomness(RandomnessMode.P8_REPLAYABLE)
        .resourceFlight(ResourceFlightMode.BASELINE_GA)
        .resourceInertia(false).legalExploration(false);
  }

  private static P8AblationProfile.Builder canonicalBackbone(DecoderMode decoder) {
    return base(decoder).qg(true).evaluatedPddr(true)
        .vns(VnsMode.O1_O9_FIXED).canonicalBaseline(true);
  }

  private static P8AblationProfile.Builder fvBase() {
    return base(DecoderMode.FATIGUE_AWARE_SELECTION).qg(true).evaluatedPddr(true)
        .vns(VnsMode.O1_O9_FIXED)
        .resourceInertia(true).legalExploration(true);
  }

  private static P8AblationProfile.Builder fmBackbone(DecoderMode decoder) {
    // Current fatigue ablation keeps the deterministic canonical HMOPSO-QGS backbone fixed.
    // Only decoderMode changes across FM0 -> FM1 -> FM2 -> FM3.
    P8AblationProfile.Builder result = base(decoder).qg(true).evaluatedPddr(true)
        .vns(VnsMode.O1_O9_FIXED);
    return result.canonicalBaseline(true);
  }

  private static P8AblationProfile.Builder qpBase() {
    return base(DecoderMode.FATIGUE_AWARE_SELECTION).evaluatedPddr(true)
        .vns(VnsMode.O1_O9_FIXED);
  }

  private static P8AblationProfile vnsBackbone(VnsMode mode) {
    return P8AblationProfile.from(b5()).vns(mode).build();
  }

  private static P8AblationProfile b0() {
    return canonicalBackbone(DecoderMode.DETERMINISTIC_CANONICAL).build();
  }

  private static P8AblationProfile b1() {
    return P8AblationProfile.from(b0()).decoder(DecoderMode.FATIGUE_AWARE_SELECTION)
        .canonicalBaseline(true).build();
  }

  private static P8AblationProfile b2() {
    return P8AblationProfile.from(b1()).qg(true)
        .resourceFlight(ResourceFlightMode.COUPLED_FMW)
        .resourceInertia(true).legalExploration(true).build();
  }

  private static P8AblationProfile b5() {
    return P8AblationProfile.from(b2()).evaluatedPddr(true).lineageArchive(true)
        .personalLeader(PersonalLeaderMode.QP_LINEAGE_ARCHIVE)
        .blockFrozenDualQ(true).build();
  }

  private static void add(List<P8ExperimentSpec> result, P8MatrixKind matrix,
      String label, String mechanism, P8AblationProfile profile) {
    result.add(new P8ExperimentSpec(matrix, label, mechanism, label, profile,
        P8RunStatus.COMPLETED, "Exact P8 ablation profile " + P8AblationProfile.VERSION,
        DEFAULT_POPULATION, DEFAULT_MAX_FES, DEFAULT_PHYSICAL_SUBSWARMS));
  }

  private static void assertUniqueLabels(List<P8ExperimentSpec> specs) {
    Set<String> labels = new HashSet<>();
    for (P8ExperimentSpec spec : specs) {
      if (!labels.add(spec.getLabel())) {
        throw new IllegalStateException("Duplicate P8 label: " + spec.getLabel());
      }
    }
  }

  /** Runtime guard for the current shift boundary and canonical baseline invariants. */
  public static void assertCurrentMatrix(List<P8ExperimentSpec> specs) {
    if (specs == null || specs.size() != 34) {
      throw new IllegalStateException("Current shift matrix requires exactly 34 formal labels");
    }
    Map<String, P8ExperimentSpec> byLabel = new HashMap<>();
    Map<String, String> hashOwner = new HashMap<>();
    for (P8ExperimentSpec spec : specs) {
      if (spec == null || spec.isDiagnosticOnly()
          || spec.getStatus() != P8RunStatus.COMPLETED
          || spec.getMatrix() == P8MatrixKind.CONTROL
          || spec.getLabel().startsWith("A0")
          || !P8AblationProfile.VERSION.equals(profileVersion(spec))) {
        throw new IllegalStateException("Non-v3/diagnostic/control entry in current matrix");
      }
      byLabel.put(spec.getLabel(), spec);
      String hash = spec.getMechanismVectorHash();
      if (hash == null || hash.length() != 64) {
        throw new IllegalStateException("Invalid mechanism vector hash: " + spec.getLabel());
      }
      // Duplicate hashes are legal aliases, but the hash must be an exact canonical identity.
      String previous = hashOwner.get(hash);
      if (previous == null) hashOwner.put(hash, spec.getLabel());
      else if (!spec.getAblationProfile().canonicalText().equals(
          byLabel.get(previous).getAblationProfile().canonicalText())) {
        throw new IllegalStateException("Hash collision between P8 profiles");
      }
    }
    assertCount(byLabel, P8MatrixKind.FV, 7);
    assertCount(byLabel, P8MatrixKind.FM, 4);
    assertCount(byLabel, P8MatrixKind.QP, 7);
    assertCount(byLabel, P8MatrixKind.VNS, 7);
    assertCount(byLabel, P8MatrixKind.FULL, 9);
    P8ExperimentSpec b0 = byLabel.get("B0");
    P8ExperimentSpec fm0 = byLabel.get("FM0");
    for (P8ExperimentSpec canonical : new P8ExperimentSpec[] {b0, fm0}) {
      if (canonical == null || !canonical.getAblationProfile().isCanonicalBaseline()
          || !canonical.getAblationProfile().isDeterministicCanonical()
          || !canonical.getAblationProfile().isQgEnabled()
          || !canonical.getAblationProfile().isEvaluatedPddrEnabled()
          || canonical.getAblationProfile().getVnsMode() != VnsMode.O1_O9_FIXED) {
        throw new IllegalStateException("B0/FM0 canonical baseline contract is incomplete");
      }
    }
    assertAdjacentMechanismDifferences(new ArrayList<P8ExperimentSpec>(byLabel.values()));
    assertExactDifference(byLabel, "FM0", "FM1", "decoderMode");
    assertExactDifference(byLabel, "FM1", "FM2", "decoderMode");
    assertExactDifference(byLabel, "FM2", "FM3", "decoderMode");
    assertExactDifference(byLabel, "B0", "B1", "decoderMode");
    assertExactDifference(byLabel, "B1", "B2", "resourceFlightMode", "resourceInertia",
        "legalExploration");
    assertExactDifference(byLabel, "B2", "B3", "lineageArchive");
    assertExactDifference(byLabel, "B3", "B4", "personalLeaderMode");
    assertExactDifference(byLabel, "B4", "B5", "blockFrozenDualQ");
    assertExactDifference(byLabel, "B5", "B6", "vnsMode");
    assertExactDifference(byLabel, "B6", "B7", "vnsMode");
    assertExactDifference(byLabel, "B7", "FULL", "vnsMode");
  }

  private static void assertCount(Map<String, P8ExperimentSpec> byLabel,
      P8MatrixKind matrix, int expected) {
    int count = 0;
    for (P8ExperimentSpec spec : byLabel.values()) if (spec.getMatrix() == matrix) count++;
    if (count != expected) {
      throw new IllegalStateException("Unexpected " + matrix + " count: " + count);
    }
  }

  private static void assertExactDifference(Map<String, P8ExperimentSpec> byLabel,
      String leftLabel, String rightLabel, String... expected) {
    P8ExperimentSpec left = byLabel.get(leftLabel);
    P8ExperimentSpec right = byLabel.get(rightLabel);
    if (left == null || right == null) {
      throw new IllegalStateException("Missing exact P8 pair: " + leftLabel + " -> " + rightLabel);
    }
    List<String> actual = left.getAblationProfile().differenceKeys(right.getAblationProfile());
    List<String> wanted = Arrays.asList(expected);
    if (!wanted.equals(actual)) {
      throw new IllegalStateException("Unexpected mechanism delta " + leftLabel + " -> "
          + rightLabel + ": expected " + wanted + ", got " + actual);
    }
  }

  /** Public so tests/runners can fail before producing a partially attributed matrix. */
  public static void assertAdjacentMechanismDifferences(List<P8ExperimentSpec> specs) {
    Map<String, P8ExperimentSpec> byLabel = new HashMap<>();
    for (P8ExperimentSpec spec : specs) byLabel.put(spec.getLabel(), spec);
    String[][] pairs = {
        {"FV0", "FV1"}, {"FV1", "FV2"}, {"FV2", "FV3"}, {"FV3", "FV4"},
        {"FV3", "FV5"}, {"FV5", "FV-Full"},
        {"FM0", "FM1"}, {"FM1", "FM2"}, {"FM2", "FM3"},
        {"QP0", "QP1"}, {"QP1", "QP2"}, {"QP2", "QP3"}, {"QP3", "QP4"},
        {"QP4", "QP5"}, {"QP5", "QP6"},
        {"V0", "V1"}, {"V1", "V2"}, {"V2", "V3"}, {"V3", "V4"},
        {"V4", "V5"}, {"V5", "V-Full"},
        {"B0", "B1"}, {"B1", "B2"}, {"B2", "B3"}, {"B3", "B4"},
        {"B4", "B5"}, {"B5", "B6"}, {"B6", "B7"}, {"B7", "FULL"}
    };
    for (String[] pair : pairs) {
      P8ExperimentSpec left = byLabel.get(pair[0]);
      P8ExperimentSpec right = byLabel.get(pair[1]);
      if (left == null || right == null) {
        throw new IllegalStateException("Missing P8 adjacent pair: " + pair[0] + " -> " + pair[1]);
      }
      if (left.getAblationProfile().differenceKeys(right.getAblationProfile()).isEmpty()) {
        throw new IllegalStateException("Adjacent P8 profiles are identical: "
            + pair[0] + " -> " + pair[1]);
      }
    }
  }
}
