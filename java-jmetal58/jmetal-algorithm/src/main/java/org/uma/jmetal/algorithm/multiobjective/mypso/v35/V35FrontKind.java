package org.uma.jmetal.algorithm.multiobjective.mypso.v35;

/** Hard separation between scientific, audit and presentation front artifacts. */
public enum V35FrontKind {
  DECISION_FRONT(true, true, false),
  OBSERVED_FULL_FRONT(false, false, false),
  REPRESENTATIVE_FRONT_K30(false, false, true),
  SENSITIVITY_FRONT_K25(false, false, false),
  SENSITIVITY_FRONT_K50(false, false, false);

  private final boolean referenceEligible;
  private final boolean mainMetricEligible;
  private final boolean presentationEligible;

  V35FrontKind(boolean referenceEligible, boolean mainMetricEligible,
      boolean presentationEligible) {
    this.referenceEligible = referenceEligible;
    this.mainMetricEligible = mainMetricEligible;
    this.presentationEligible = presentationEligible;
  }

  public boolean isReferenceEligible() { return referenceEligible; }
  public boolean isMainMetricEligible() { return mainMetricEligible; }
  public boolean isPresentationEligible() { return presentationEligible; }

  public void requireMainMetricEligible() {
    if (!mainMetricEligible) {
      throw new IllegalStateException(name() + " is forbidden for main metrics");
    }
  }

  public static String registryCsv() {
    StringBuilder out = new StringBuilder(
        "frontKind,referenceEligible,mainMetricEligible,presentationEligible\n");
    for (V35FrontKind kind : values()) {
      out.append(kind.name()).append(',').append(kind.referenceEligible).append(',')
          .append(kind.mainMetricEligible).append(',').append(kind.presentationEligible)
          .append('\n');
    }
    return out.toString();
  }
}
