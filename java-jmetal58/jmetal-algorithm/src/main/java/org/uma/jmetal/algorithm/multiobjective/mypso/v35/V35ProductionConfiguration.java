package org.uma.jmetal.algorithm.multiobjective.mypso.v35;

import java.io.Serializable;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import org.uma.jmetal.problem.multiobjective.dfsp.decoder.ProductionDecodeMode;
import org.uma.jmetal.problem.multiobjective.dfsp.fatigue.shift.ZhangBoShiftConfiguration;
import org.uma.jmetal.problem.multiobjective.dfsp.fatigue.shift.ZhangBoShiftMode;
import org.uma.jmetal.problem.multiobjective.dfsp.setup.FamilyMode;
import org.uma.jmetal.problem.multiobjective.dfsp.setup.ProductFamilySetupModel;
import org.uma.jmetal.problem.multiobjective.dfsp.setup.SetupMode;

/**
 * Decision-complete v3.5 production boundary.  It intentionally does not expose
 * a shift builder: the formal line is permanently shift-free.
 */
public final class V35ProductionConfiguration implements Serializable {
  private static final long serialVersionUID = 1L;
  public static final String VERSION = "v3.5-mainline-2";
  public static final String ALGORITHM_SEMANTICS_VERSION =
      "v35-pressure-confidence-diagnosis-v1";

  private final long seed;
  private final int populationSize;
  private final int maxEvaluations;
  private final ProductionDecodeMode decoderMode;
  private final FamilyMode familyMode;
  private final SetupMode setupMode;
  private final boolean dscr;
  private final boolean cfvf;
  private final boolean qg;
  private final boolean qp;
  private final boolean caTaLite;
  private final boolean directionalTeacherPool;
  private final int teacherPoolSize;
  private final V35BottleneckDiagnosisConfiguration bottleneckDiagnosis;
  private final org.uma.jmetal.algorithm.multiobjective.mypso.zhangbo.ZhangBoDualQCoordinationConfiguration dualQCoordination;
  private final V35LocalFeBudgetConfiguration localFeBudget;
  private final V35CaTaLiteConfiguration caTaLiteConfiguration;

  private V35ProductionConfiguration(Builder b) {
    if (b.decoderMode == null || b.familyMode == null || b.setupMode == null) {
      throw new IllegalArgumentException("v3.5 configuration modes must not be null");
    }
    if (b.populationSize <= 0 || b.maxEvaluations <= 0) {
      throw new IllegalArgumentException("population and evaluation budget must be positive");
    }
    if (b.familyMode != FamilyMode.DEGENERATE_SINGLE_FAMILY
        || b.setupMode != SetupMode.SEQUENCE_INDEPENDENT) {
      throw new IllegalArgumentException(
          "v3.5 formal line requires the single-family, sequence-independent setup boundary");
    }
    if (b.decoderMode == ProductionDecodeMode.AUTHOR_DIAGNOSTIC) {
      throw new IllegalArgumentException("author diagnostic mode is not a v3.5 production mode");
    }
    if (b.dscr && !b.qg) {
      throw new IllegalArgumentException("DSCR requires the Q-gbest social controller");
    }
    if (b.cfvf && !b.qg) {
      throw new IllegalArgumentException("CFVF requires the Q-gbest social controller");
    }
    if (b.qp && (!b.qg || !b.cfvf)) {
      throw new IllegalArgumentException("Q-pbest requires Q-gbest and CFVF");
    }
    if (b.caTaLite && (!b.qp || !b.qg || !b.cfvf)) {
      throw new IllegalArgumentException("CA-TA-Lite requires the full dual-Q backbone");
    }
    if (b.directionalTeacherPool && !b.qg) {
      throw new IllegalArgumentException("directional teacher pool requires the Q-gbest controller");
    }
    if (b.teacherPoolSize < 1 || (b.directionalTeacherPool && b.teacherPoolSize < 2)) {
      throw new IllegalArgumentException(
          "teacher pool size must be >= 1, and >= 2 when the directional pool is enabled");
    }
    seed = b.seed;
    populationSize = b.populationSize;
    maxEvaluations = b.maxEvaluations;
    decoderMode = b.decoderMode;
    familyMode = b.familyMode;
    setupMode = b.setupMode;
    dscr = b.dscr;
    cfvf = b.cfvf;
    qg = b.qg;
    qp = b.qp;
    caTaLite = b.caTaLite;
    directionalTeacherPool = b.directionalTeacherPool;
    teacherPoolSize = b.teacherPoolSize;
    bottleneckDiagnosis = b.bottleneckDiagnosis;
    dualQCoordination = b.dualQCoordination;
    localFeBudget = b.localFeBudget;
    caTaLiteConfiguration = b.caTaLiteConfiguration;
  }

  public static Builder builder() { return new Builder(); }
  public static V35ProductionConfiguration smoke(long seed) {
    return builder().seed(seed).populationSize(10).maxEvaluations(5000)
        .decoderMode(ProductionDecodeMode.FM3)
        .dscr(true).cfvf(true).qg(true).qp(true).caTaLite(true).build();
  }
  public static V35ProductionConfiguration formal(long seed) {
    return builder().seed(seed).populationSize(100).maxEvaluations(500000)
        .decoderMode(ProductionDecodeMode.FM3)
        .dscr(true).cfvf(true).qg(true).qp(true).caTaLite(true).build();
  }

  public long getSeed() { return seed; }
  public int getPopulationSize() { return populationSize; }
  public int getMaxEvaluations() { return maxEvaluations; }
  public ProductionDecodeMode getDecoderMode() { return decoderMode; }
  public FamilyMode getFamilyMode() { return familyMode; }
  public SetupMode getSetupMode() { return setupMode; }
  public boolean isDscrEnabled() { return dscr; }
  public boolean isCfvfEnabled() { return cfvf; }
  public boolean isQgEnabled() { return qg; }
  public boolean isQpEnabled() { return qp; }
  public boolean isCaTaLiteEnabled() { return caTaLite; }
  public boolean isDirectionalTeacherPoolEnabled() { return directionalTeacherPool; }
  public int getTeacherPoolSize() { return teacherPoolSize; }
  public V35BottleneckDiagnosisConfiguration getBottleneckDiagnosis() {
    return bottleneckDiagnosis;
  }

  /** Optional P/G-block override; {@code null} keeps the default equal-length schedule. */
  public org.uma.jmetal.algorithm.multiobjective.mypso.zhangbo.ZhangBoDualQCoordinationConfiguration getDualQCoordination() {
    return dualQCoordination;
  }

  /** V35-FC-2 optional local-FE budget scheduler; {@code null} (the default)
   *  keeps the legacy {@code LS_Times} semantics byte-for-byte. */
  public V35LocalFeBudgetConfiguration getLocalFeBudget() {
    return localFeBudget;
  }

  /** V35-FC-3 optional CA-TA-Lite tuning; {@code null} (default) keeps the
   *  archived {@link V35CaTaLiteConfiguration#standard()} semantics. */
  public V35CaTaLiteConfiguration getCaTaLiteConfiguration() {
    return caTaLiteConfiguration;
  }
  public ZhangBoShiftMode getShiftMode() { return ZhangBoShiftMode.NONE; }
  public ZhangBoShiftConfiguration getShiftConfiguration() {
    return ZhangBoShiftConfiguration.none();
  }
  public ProductFamilySetupModel getSetupModel(int jobs, int stages) {
    return ProductFamilySetupModel.degenerate(jobs, stages);
  }

  public String canonicalText() {
    StringBuilder text = new StringBuilder()
        .append("configurationVersion=").append(VERSION).append('\n')
        .append("algorithmSemanticsVersion=").append(ALGORITHM_SEMANTICS_VERSION).append('\n')
        .append("seed=").append(seed).append('\n')
        .append("populationSize=").append(populationSize).append('\n')
        .append("maxEvaluations=").append(maxEvaluations).append('\n')
        .append("decoderMode=").append(decoderMode).append('\n')
        .append("familyMode=").append(familyMode).append('\n')
        .append("setupMode=").append(setupMode).append('\n')
        .append("shiftMode=NONE\n")
        .append("dscr=").append(dscr).append('\n')
        .append("cfvf=").append(cfvf).append('\n')
        .append("qg=").append(qg).append('\n')
        .append("qp=").append(qp).append('\n')
        .append("caTaLite=").append(caTaLite).append('\n')
        .append("directionalTeacherPool=").append(directionalTeacherPool).append('\n')
        .append("teacherPoolSize=").append(teacherPoolSize).append('\n')
        .append(bottleneckDiagnosis.canonicalText());
    // V35-FC-2: absent by default, so the A4-PREFINAL archive hash is stable;
    // an explicit budget is part of the canonical record.
    if (localFeBudget != null) {
      text.append(localFeBudget.toCanonicalText());
    }
    if (caTaLiteConfiguration != null) {
      text.append("caTaLite.top2Probe=").append(caTaLiteConfiguration.isTop2ProbeEnabled())
          .append('\n')
          .append("caTaLite.testFeShareCap=").append(caTaLiteConfiguration.getTestFeShareCap())
          .append('\n');
    }
    return text.toString();
  }

  public String configurationHash() {
    try {
      byte[] digest = MessageDigest.getInstance("SHA-256")
          .digest(canonicalText().getBytes(StandardCharsets.UTF_8));
      StringBuilder result = new StringBuilder();
      for (byte value : digest) result.append(String.format("%02x", value & 0xff));
      return result.toString();
    } catch (NoSuchAlgorithmException e) {
      throw new IllegalStateException("SHA-256 unavailable", e);
    }
  }

  public static final class Builder {
    private long seed = 20260808L;
    private int populationSize = 100;
    private int maxEvaluations = 500000;
    private ProductionDecodeMode decoderMode = ProductionDecodeMode.FM3;
    private FamilyMode familyMode = FamilyMode.DEGENERATE_SINGLE_FAMILY;
    private SetupMode setupMode = SetupMode.SEQUENCE_INDEPENDENT;
    private boolean dscr;
    private boolean cfvf;
    private boolean qg;
    private boolean qp;
    private boolean caTaLite;
    private boolean directionalTeacherPool;
    private int teacherPoolSize = 10;
    private V35BottleneckDiagnosisConfiguration bottleneckDiagnosis =
        V35BottleneckDiagnosisConfiguration.fullMaskNoShadow();
    private org.uma.jmetal.algorithm.multiobjective.mypso.zhangbo.ZhangBoDualQCoordinationConfiguration dualQCoordination;
    private V35LocalFeBudgetConfiguration localFeBudget;
    private V35CaTaLiteConfiguration caTaLiteConfiguration;

    public Builder seed(long value) { seed = value; return this; }
    public Builder populationSize(int value) { populationSize = value; return this; }
    public Builder maxEvaluations(int value) { maxEvaluations = value; return this; }
    public Builder decoderMode(ProductionDecodeMode value) { decoderMode = value; return this; }
    public Builder familyMode(FamilyMode value) { familyMode = value; return this; }
    public Builder setupMode(SetupMode value) { setupMode = value; return this; }
    public Builder dscr(boolean value) { dscr = value; return this; }
    public Builder cfvf(boolean value) { cfvf = value; return this; }
    public Builder qg(boolean value) { qg = value; return this; }
    public Builder qp(boolean value) { qp = value; return this; }
    public Builder caTaLite(boolean value) { caTaLite = value; return this; }
    public Builder directionalTeacherPool(boolean value) { directionalTeacherPool = value; return this; }
    public Builder teacherPoolSize(int value) { teacherPoolSize = value; return this; }
    public Builder bottleneckDiagnosis(V35BottleneckDiagnosisConfiguration value) {
      if (value == null) throw new IllegalArgumentException("bottleneckDiagnosis");
      bottleneckDiagnosis = value;
      return this;
    }
    public Builder dualQCoordination(
        org.uma.jmetal.algorithm.multiobjective.mypso.zhangbo.ZhangBoDualQCoordinationConfiguration value) {
      if (value == null) throw new IllegalArgumentException("dualQCoordination");
      dualQCoordination = value;
      return this;
    }
    /** V35-FC-2: {@code null} (default) keeps the legacy LS_Times semantics. */
    public Builder localFeBudget(V35LocalFeBudgetConfiguration value) {
      localFeBudget = value;
      return this;
    }
    /** V35-FC-3: {@code null} (default) keeps the archived standard CA-TA-Lite. */
    public Builder caTaLiteConfiguration(V35CaTaLiteConfiguration value) {
      caTaLiteConfiguration = value;
      return this;
    }
    public V35ProductionConfiguration build() { return new V35ProductionConfiguration(this); }
  }
}
