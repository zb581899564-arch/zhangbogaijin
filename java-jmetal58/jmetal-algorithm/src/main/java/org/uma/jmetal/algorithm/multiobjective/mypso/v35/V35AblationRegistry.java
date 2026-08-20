package org.uma.jmetal.algorithm.multiobjective.mypso.v35;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.uma.jmetal.problem.multiobjective.dfsp.decoder.ProductionDecodeMode;

/**
 * V35-P21 ablation-ladder registry.  Each rung is one mechanism added on top of
 * the previous one, following the legal partial order qp=>cfvf=>qg and
 * caTaLite=>qp&&dscr (runtime constraint: {@code dscr=false} with
 * {@code caTaLite=true} silently degrades to the legacy CA-TA controller, so the
 * FULL-minus-DSCR cell is forbidden and deliberately absent from this ladder).
 * A0..A5 are expressible through {@link V35FairRunner.Mode}; the registry only
 * adds the decision-complete documentation and the single-switch adjacency
 * discipline used by the ladder test.
 */
public final class V35AblationRegistry {

  public enum Rung {
    A0_BASELINE("A0-baseline", V35FairRunner.Mode.V35_BASELINE,
        false, false, false, false, false, null, "Q-gbest controller only (degenerate baseline)"),
    A1_DSCR("A1-dscr", V35FairRunner.Mode.V35_QG1,
        true, false, false, false, false, "dscr", "+DSCR dominance-safe cache refresh"),
    A2_CFVF("A2-cfvf", V35FairRunner.Mode.V35_A2,
        true, true, false, false, false, "cfvf", "+CFVF all-vector flight"),
    A3_QP("A3-qp", V35FairRunner.Mode.V35_A3,
        true, true, true, false, false, "qp", "+Q-pbest lineage archive with block-frozen dual Q"),
    A4_CA_TA_LITE("A4-catalite", V35FairRunner.Mode.V35_FULL_POOL_OFF,
        true, true, true, true, false, "caTaLite", "+CA-TA-Lite 24x5 test/apply/re-test"),
    A5_FULL("A5-full", V35FairRunner.Mode.V35_FULL,
        true, true, true, true, true, "directionalTeacherPool", "+directional top-k teacher pool");

    private final String label;
    private final V35FairRunner.Mode mode;
    private final boolean dscr;
    private final boolean cfvf;
    private final boolean qp;
    private final boolean caTaLite;
    private final boolean directionalPool;
    private final String addedSwitch;
    private final String description;

    Rung(String label, V35FairRunner.Mode mode, boolean dscr, boolean cfvf, boolean qp,
        boolean caTaLite, boolean directionalPool, String addedSwitch, String description) {
      this.label = label;
      this.mode = mode;
      this.dscr = dscr;
      this.cfvf = cfvf;
      this.qp = qp;
      this.caTaLite = caTaLite;
      this.directionalPool = directionalPool;
      this.addedSwitch = addedSwitch;
      this.description = description;
    }

    public String getLabel() { return label; }
    public V35FairRunner.Mode getMode() { return mode; }
    public boolean isDscrEnabled() { return dscr; }
    public boolean isCfvfEnabled() { return cfvf; }
    public boolean isQpEnabled() { return qp; }
    public boolean isCaTaLiteEnabled() { return caTaLite; }
    public boolean isDirectionalPoolEnabled() { return directionalPool; }

    /** Switch key flipped when entering this rung from the previous one; null for A0. */
    public String getAddedSwitch() { return addedSwitch; }

    public String getDescription() { return description; }
  }

  public static final List<Rung> LADDER =
      Collections.unmodifiableList(java.util.Arrays.asList(Rung.values()));

  private V35AblationRegistry() { }

  /** Decision-complete production configuration for one rung. */
  public static V35ProductionConfiguration configFor(Rung rung, long seed, int populationSize,
      int maxEvaluations) {
    return V35ProductionConfiguration.builder()
        .seed(seed).populationSize(populationSize).maxEvaluations(maxEvaluations)
        .decoderMode(ProductionDecodeMode.FM3)
        .dscr(rung.dscr).cfvf(rung.cfvf).qg(true).qp(rung.qp).caTaLite(rung.caTaLite)
        .directionalTeacherPool(rung.directionalPool).teacherPoolSize(10).build();
  }

  /**
   * Adjacent-rung discipline: exactly one switch line may differ between the two
   * canonical texts, and it must be the expected added switch.  Returns the
   * differing keys; empty means the texts are identical (a ladder violation).
   */
  public static List<String> differingSwitchKeys(String lowerText, String upperText) {
    List<String> result = new ArrayList<>();
    String[] lowerLines = lowerText.split("\n");
    String[] upperLines = upperText.split("\n");
    if (lowerLines.length != upperLines.length) {
      result.add("<LINE_COUNT_MISMATCH>");
      return result;
    }
    for (int i = 0; i < lowerLines.length; i++) {
      if (!lowerLines[i].equals(upperLines[i])) {
        String key = lowerLines[i];
        int separator = key.indexOf('=');
        result.add(separator < 0 ? key : key.substring(0, separator));
      }
    }
    return result;
  }

  /**
   * Runtime trap documentation for the forbidden FULL-minus-DSCR cell:
   * {@code ZhangBoGlobalSearchConfiguration.isV35CaTaLiteEnabled()} requires
   * {@code dscrEnabled}; with dscr off the run silently falls back to the legacy
   * CA-TA controller, so that cell cannot legally express "+CA-TA-Lite without
   * DSCR" and must never appear in ladder evidence.
   */
  public static String forbiddenFullMinusDscrNote() {
    return "FORBIDDEN_CELL=FULL-minus-DSCR(dscr=false,cfvf=true,qp=true,caTaLite=true) "
        + "is excluded: caTaLite requires dscr at runtime "
        + "(ZhangBoGlobalSearchConfiguration.isV35CaTaLiteEnabled gated on dscrEnabled); "
        + "dscr=false with caTaLite=true would silently run the legacy CA-TA controller.";
  }
}
