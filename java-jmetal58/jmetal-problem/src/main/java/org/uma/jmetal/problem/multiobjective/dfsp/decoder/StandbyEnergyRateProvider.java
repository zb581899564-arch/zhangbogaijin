package org.uma.jmetal.problem.multiobjective.dfsp.decoder;

import java.io.Serializable;

/** Supplies the paper's otherwise missing machine standby-energy parameter. */
public interface StandbyEnergyRateProvider extends Serializable {
  double rate(int factory, int stage, int machine);

  String provenance();
}
