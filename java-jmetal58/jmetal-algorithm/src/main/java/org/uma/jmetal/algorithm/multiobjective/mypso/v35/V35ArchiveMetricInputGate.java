package org.uma.jmetal.algorithm.multiobjective.mypso.v35;

/** Fail-closed gate used by future archive experiment analysis before metrics or PFref. */
public final class V35ArchiveMetricInputGate {
  private V35ArchiveMetricInputGate() { }

  public static void requireMainMetricInput(V35FrontKind kind) {
    if (kind == null) throw new IllegalArgumentException("kind");
    kind.requireMainMetricEligible();
  }

  public static void requireReferenceFreezeInput(V35FrontKind kind,
      boolean allRegisteredRunsCompleted) {
    if (kind == null) throw new IllegalArgumentException("kind");
    if (!kind.isReferenceEligible()) {
      throw new IllegalStateException(kind + " is forbidden for PFref");
    }
    if (!allRegisteredRunsCompleted) {
      throw new IllegalStateException("PFref cannot freeze before every registered run completes");
    }
  }
}
