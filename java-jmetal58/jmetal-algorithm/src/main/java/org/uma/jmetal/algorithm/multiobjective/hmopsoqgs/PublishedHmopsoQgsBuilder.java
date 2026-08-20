package org.uma.jmetal.algorithm.multiobjective.hmopsoqgs;

import org.uma.jmetal.problem.multiobjective.dfsp.decoder.DhhfspProblem;
import org.uma.jmetal.problem.multiobjective.dfsp.model.DhhfspInstance;
import org.uma.jmetal.util.pseudorandom.PseudoRandomGenerator;
import org.uma.jmetal.util.pseudorandom.impl.JavaRandomGenerator;

/** jMetal 5.8 builder for the isolated published baseline. */
public final class PublishedHmopsoQgsBuilder {
  private final DhhfspProblem problem;
  private final DhhfspInstance instance;
  private HmopsoQgsConfiguration configuration;
  private PseudoRandomGenerator random;

  public PublishedHmopsoQgsBuilder(DhhfspProblem problem, DhhfspInstance instance) {
    this.problem = problem;
    this.instance = instance;
    this.configuration = HmopsoQgsConfiguration.engineeringSmoke(20260808L);
    this.random = new JavaRandomGenerator(configuration.getSeed());
  }

  public PublishedHmopsoQgsBuilder setConfiguration(HmopsoQgsConfiguration value) {
    if (value == null) throw new IllegalArgumentException("configuration");
    configuration = value;
    random = new JavaRandomGenerator(value.getSeed());
    return this;
  }

  public PublishedHmopsoQgsBuilder setRandomGenerator(PseudoRandomGenerator value) {
    if (value == null) throw new IllegalArgumentException("random");
    random = value;
    return this;
  }

  public PublishedHmopsoQgs build() {
    return new PublishedHmopsoQgs(problem, instance, configuration, random);
  }
}
