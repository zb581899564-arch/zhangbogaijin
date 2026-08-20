package org.uma.jmetal.problem.multiobjective.dfsp.decoder;

/** Explicit compatibility policy matching the author's unscaled idle-time energy term. */
public final class UnitStandbyEnergyRateProvider implements StandbyEnergyRateProvider {
  private static final long serialVersionUID = 1L;

  @Override
  public double rate(int factory, int stage, int machine) {
    return 1.0;
  }

  @Override
  public String provenance() {
    return "author_actual_compatibility:unit_standby_energy_rate=1.0";
  }
}
