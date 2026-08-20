package org.uma.jmetal.problem.multiobjective.dfsp.decoder;

import java.io.Serializable;

/** Immutable configuration for a single decoder invocation. */
public final class DecodeOptions implements Serializable {
  private static final long serialVersionUID = 1L;

  private final DecodeMode mode;
  private final long seed;
  private final StandbyEnergyRateProvider standbyEnergyRateProvider;

  public DecodeOptions(
      DecodeMode mode, long seed, StandbyEnergyRateProvider standbyEnergyRateProvider) {
    if (mode == null) {
      throw new IllegalArgumentException("mode must not be null");
    }
    if (standbyEnergyRateProvider == null) {
      throw new IllegalArgumentException("standbyEnergyRateProvider must not be null");
    }
    this.mode = mode;
    this.seed = seed;
    this.standbyEnergyRateProvider = standbyEnergyRateProvider;
  }

  public static DecodeOptions deterministic(long seed) {
    return new DecodeOptions(
        DecodeMode.DETERMINISTIC_CANONICAL, seed, new UnitStandbyEnergyRateProvider());
  }

  public static DecodeOptions published(long seed) {
    return new DecodeOptions(
        DecodeMode.PUBLISHED_STOCHASTIC, seed, new UnitStandbyEnergyRateProvider());
  }

  public DecodeMode getMode() {
    return mode;
  }

  public long getSeed() {
    return seed;
  }

  public StandbyEnergyRateProvider getStandbyEnergyRateProvider() {
    return standbyEnergyRateProvider;
  }
}
