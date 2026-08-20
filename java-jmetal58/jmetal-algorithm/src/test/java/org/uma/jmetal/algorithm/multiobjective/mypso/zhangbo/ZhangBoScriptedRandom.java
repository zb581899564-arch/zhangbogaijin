package org.uma.jmetal.algorithm.multiobjective.mypso.zhangbo;

import org.uma.jmetal.util.pseudorandom.PseudoRandomGenerator;

import java.util.ArrayDeque;
import java.util.Queue;

final class ZhangBoScriptedRandom implements PseudoRandomGenerator {
  private final Queue<Double> doubles = new ArrayDeque<>();
  private final Queue<Integer> integers = new ArrayDeque<>();
  private long seed;

  ZhangBoScriptedRandom(double[] doubles, int[] integers) {
    for (double value : doubles) this.doubles.add(value);
    for (int value : integers) this.integers.add(value);
  }

  @Override public int nextInt(int lowerBound, int upperBound) {
    int value = integers.isEmpty() ? lowerBound : integers.remove();
    if (value < lowerBound || value > upperBound) throw new AssertionError("scripted int out of range");
    return value;
  }
  @Override public double nextDouble(double lowerBound, double upperBound) {
    return lowerBound + nextDouble() * (upperBound - lowerBound);
  }
  @Override public double nextDouble() { return doubles.isEmpty() ? 0.0 : doubles.remove(); }
  @Override public long getSeed() { return seed; }
  @Override public void setSeed(long seed) { this.seed = seed; }
  @Override public String getName() { return "ZhangBoScriptedRandom"; }
}
