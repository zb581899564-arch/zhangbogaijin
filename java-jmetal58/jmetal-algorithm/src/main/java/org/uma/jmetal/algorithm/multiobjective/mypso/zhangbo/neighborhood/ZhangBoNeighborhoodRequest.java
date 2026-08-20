package org.uma.jmetal.algorithm.multiobjective.mypso.zhangbo.neighborhood;

import java.util.Objects;
import org.uma.jmetal.algorithm.multiobjective.mypso.zhangbo.ZhangBoSubSwarm;
import org.uma.jmetal.problem.multiobjective.dfsp.fatigue.ZhangBoFatigueInstanceData;
import org.uma.jmetal.problem.multiobjective.dfsp.fatigue.ZhangBoFatigueParameters;
import org.uma.jmetal.solution.PermutationSolution;

/** Immutable input of one independently auditable neighborhood call. */
public final class ZhangBoNeighborhoodRequest {
  private final PermutationSolution<Integer> source;
  private final ZhangBoFatigueInstanceData instance;
  private final ZhangBoFatigueParameters fatigueParameters;
  private final int factory;
  private final ZhangBoSubSwarm subSwarm;
  private final long seed;
  private final ZhangBoFatigueFocus fatigueFocus;

  public ZhangBoNeighborhoodRequest(
      PermutationSolution<Integer> source,
      ZhangBoFatigueInstanceData instance,
      ZhangBoFatigueParameters fatigueParameters,
      int factory,
      ZhangBoSubSwarm subSwarm,
      long seed,
      ZhangBoFatigueFocus fatigueFocus) {
    this.source = Objects.requireNonNull(source, "source");
    this.instance = Objects.requireNonNull(instance, "instance");
    this.fatigueParameters = Objects.requireNonNull(fatigueParameters, "fatigueParameters");
    this.subSwarm = Objects.requireNonNull(subSwarm, "subSwarm");
    this.fatigueFocus = Objects.requireNonNull(fatigueFocus, "fatigueFocus");
    if (factory < 0 || factory >= instance.getFactories()) {
      throw new IllegalArgumentException("factory out of range: " + factory);
    }
    if (source.getAttribute(
        org.uma.jmetal.problem.multiobjective.dfsp.fatigue.ZhangBoFatigueEvaluationResult.class)
        == null) {
      throw new IllegalArgumentException("source must already contain a fatigue evaluation result");
    }
    this.factory = factory;
    this.seed = seed;
  }

  public PermutationSolution<Integer> getSource() { return source; }
  public ZhangBoFatigueInstanceData getInstance() { return instance; }
  public ZhangBoFatigueParameters getFatigueParameters() { return fatigueParameters; }
  public int getFactory() { return factory; }
  public ZhangBoSubSwarm getSubSwarm() { return subSwarm; }
  public long getSeed() { return seed; }
  public ZhangBoFatigueFocus getFatigueFocus() { return fatigueFocus; }

  public ZhangBoNeighborhoodRequest withSeed(long value) {
    return new ZhangBoNeighborhoodRequest(source, instance, fatigueParameters,
        factory, subSwarm, value, fatigueFocus);
  }
}
