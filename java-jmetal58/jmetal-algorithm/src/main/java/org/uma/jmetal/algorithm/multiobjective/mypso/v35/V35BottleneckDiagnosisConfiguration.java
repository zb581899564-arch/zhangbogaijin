package org.uma.jmetal.algorithm.multiobjective.mypso.v35;

import java.io.Serializable;

/** Immutable boundary for the pressure-based v3.5 bottleneck diagnosis. */
public final class V35BottleneckDiagnosisConfiguration implements Serializable {
  private static final long serialVersionUID = 1L;

  public static final String VERSION = "v35-pressure-confidence-diagnosis-v1";
  public static final int DEFAULT_SHADOW_STRIDE = 20;
  public static final int DEFAULT_MAX_SHADOW_EVALUATIONS = 5000;

  public enum Mode {
    /** Diagnostic calibration: every uncertain context is BAL and all macros remain visible. */
    FULL_MASK_AUDIT,
    /** Frozen pressure thresholds decide between one strict bottleneck and BAL. */
    CONFIDENCE
  }

  private final Mode mode;
  private final double tauAbs;
  private final double tauGap;
  private final boolean shadowAudit;
  private final int shadowStride;
  private final int maxShadowEvaluations;

  private V35BottleneckDiagnosisConfiguration(Mode mode, double tauAbs, double tauGap,
      boolean shadowAudit, int shadowStride, int maxShadowEvaluations) {
    if (mode == null) throw new IllegalArgumentException("diagnosis mode");
    if (mode == Mode.CONFIDENCE
        && (!finiteUnit(tauAbs) || !finiteUnit(tauGap))) {
      throw new IllegalArgumentException("confidence thresholds must be finite and in [0,1]");
    }
    if (shadowStride < 1 || maxShadowEvaluations < 0) {
      throw new IllegalArgumentException("invalid shadow-audit budget");
    }
    this.mode = mode;
    this.tauAbs = mode == Mode.CONFIDENCE ? tauAbs : 1.0;
    this.tauGap = mode == Mode.CONFIDENCE ? tauGap : 1.0;
    this.shadowAudit = shadowAudit;
    this.shadowStride = shadowStride;
    this.maxShadowEvaluations = maxShadowEvaluations;
  }

  public static V35BottleneckDiagnosisConfiguration calibrationAudit() {
    return new V35BottleneckDiagnosisConfiguration(Mode.FULL_MASK_AUDIT, 1.0, 1.0,
        true, DEFAULT_SHADOW_STRIDE, DEFAULT_MAX_SHADOW_EVALUATIONS);
  }

  public static V35BottleneckDiagnosisConfiguration fullMaskNoShadow() {
    return new V35BottleneckDiagnosisConfiguration(Mode.FULL_MASK_AUDIT, 1.0, 1.0,
        false, DEFAULT_SHADOW_STRIDE, 0);
  }

  public static V35BottleneckDiagnosisConfiguration confidence(
      double tauAbs, double tauGap, boolean shadowAudit) {
    return new V35BottleneckDiagnosisConfiguration(Mode.CONFIDENCE, tauAbs, tauGap,
        shadowAudit, DEFAULT_SHADOW_STRIDE, DEFAULT_MAX_SHADOW_EVALUATIONS);
  }

  public Mode getMode() { return mode; }
  public double getTauAbs() { return tauAbs; }
  public double getTauGap() { return tauGap; }
  public boolean isShadowAuditEnabled() { return shadowAudit; }
  public int getShadowStride() { return shadowStride; }
  public int getMaxShadowEvaluations() { return maxShadowEvaluations; }

  public String canonicalText() {
    return "diagnosisVersion=" + VERSION + '\n'
        + "diagnosisMode=" + mode + '\n'
        + "tauAbs=" + tauAbs + '\n'
        + "tauGap=" + tauGap + '\n'
        + "shadowAudit=" + shadowAudit + '\n'
        + "shadowStride=" + shadowStride + '\n'
        + "maxShadowEvaluations=" + maxShadowEvaluations + '\n'
        + "pressureWeights=SEQ:0.5/0.5,MAC:0.5/0.5,WOR:0.5/0.5,SET:0.5/0.5,FAT:0.5/0.5\n";
  }

  private static boolean finiteUnit(double value) {
    return Double.isFinite(value) && value >= 0.0 && value <= 1.0;
  }
}
