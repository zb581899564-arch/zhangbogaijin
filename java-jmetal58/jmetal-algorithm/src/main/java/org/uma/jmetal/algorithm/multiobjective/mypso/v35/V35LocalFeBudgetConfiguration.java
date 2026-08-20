package org.uma.jmetal.algorithm.multiobjective.mypso.v35;

import java.io.Serializable;
import java.util.Locale;

/**
 * V35-FC-2: the dynamic local-search FE budget scheduler.  Replaces the fixed
 * {@code LS_Times=30} resource control for the A4 line: per outer cycle, with
 * search progress {@code u = FE / MaxFEs}, the local share is
 *
 * <pre>{@code
 * beta(u) = betaMin + (betaMax - betaMin) * u^2
 * B_L     = floor( beta(u) / (1 - beta(u)) * B_G )
 * }</pre>
 *
 * so that {@code B_L / (B_G + B_L) ~= beta(u)}.  The inter-factory local
 * search and CA-TA-Lite share this hard budget; neither may exceed it.  A
 * {@code null} configuration keeps the legacy {@code LS_Times} semantics
 * byte-for-byte (the A4-PREFINAL archive contract).
 *
 * <p>First-round candidates {@code betaMin=0.25}, {@code betaMax=0.65} are
 * calibration candidates only: they must be frozen by the FC-2 experiment
 * before entering any formal default.</p>
 */
public final class V35LocalFeBudgetConfiguration implements Serializable {
  private static final long serialVersionUID = 1L;

  private final double betaMin;
  private final double betaMax;

  public static V35LocalFeBudgetConfiguration of(double betaMin, double betaMax) {
    return new V35LocalFeBudgetConfiguration(betaMin, betaMax);
  }

  private V35LocalFeBudgetConfiguration(double betaMin, double betaMax) {
    if (!(betaMin > 0.0) || !(betaMin < betaMax) || !(betaMax < 1.0)) {
      throw new IllegalArgumentException(
          "local FE budget requires 0 < betaMin < betaMax < 1, got "
              + betaMin + "/" + betaMax);
    }
    this.betaMin = betaMin;
    this.betaMax = betaMax;
  }

  public double getBetaMin() { return betaMin; }
  public double getBetaMax() { return betaMax; }

  /** The local share at search progress {@code u} (clamped to [0,1]). */
  public double betaAt(double progress) {
    double u = Math.max(0.0, Math.min(1.0, progress));
    return betaMin + (betaMax - betaMin) * u * u;
  }

  /** The hard local budget for one outer cycle that consumed {@code globalFe}
   *  complete evaluations, at search progress {@code progress}. */
  public long localBudgetFor(double progress, long globalFe) {
    if (globalFe <= 0L) return 0L;
    double beta = betaAt(progress);
    return (long) Math.floor(beta / (1.0 - beta) * (double) globalFe);
  }

  public String toCanonicalText() {
    return "localFeBudget.betaMin=" + String.format(Locale.ROOT, "%.6f", betaMin) + '\n'
        + "localFeBudget.betaMax=" + String.format(Locale.ROOT, "%.6f", betaMax) + '\n'
        + "localFeBudget.schedule=betaMin+(betaMax-betaMin)*u^2\n"
        + "localFeBudget.formula=B_L=floor(beta/(1-beta)*B_G)\n";
  }

  @Override public boolean equals(Object other) {
    if (!(other instanceof V35LocalFeBudgetConfiguration)) return false;
    V35LocalFeBudgetConfiguration that = (V35LocalFeBudgetConfiguration) other;
    return Double.compare(betaMin, that.betaMin) == 0
        && Double.compare(betaMax, that.betaMax) == 0;
  }

  @Override public int hashCode() {
    return java.util.Objects.hash(Double.valueOf(betaMin), Double.valueOf(betaMax));
  }

  @Override public String toString() {
    return "V35LocalFeBudgetConfiguration[betaMin=" + betaMin + ", betaMax=" + betaMax + "]";
  }
}
