package org.uma.jmetal.algorithm.multiobjective.hmopsoqgs;

import org.uma.jmetal.util.pseudorandom.PseudoRandomGenerator;

import java.util.ArrayDeque;
import java.util.Queue;

final class ScriptedRandomGenerator implements PseudoRandomGenerator {
  private final Queue<Integer> integers = new ArrayDeque<>();
  private final Queue<Double> doubles = new ArrayDeque<>();
  private long seed;

  ScriptedRandomGenerator ints(int... values) {
    for (int value : values) integers.add(value);
    return this;
  }
  ScriptedRandomGenerator doubles(double... values) {
    for (double value : values) doubles.add(value);
    return this;
  }
  @Override public int nextInt(int lowerBound, int upperBound) {
    if (integers.isEmpty()) throw new AssertionError("missing scripted int");
    int value = integers.remove();
    if (value < lowerBound || value > upperBound) {
      throw new AssertionError(value + " outside [" + lowerBound + ',' + upperBound + ']');
    }
    return value;
  }
  @Override public double nextDouble(double lowerBound, double upperBound) {
    double value = nextDouble();
    return lowerBound + value * (upperBound - lowerBound);
  }
  @Override public double nextDouble() {
    if (doubles.isEmpty()) throw new AssertionError("missing scripted double");
    return doubles.remove();
  }
  @Override public void setSeed(long value) { seed = value; }
  @Override public long getSeed() { return seed; }
  @Override public String getName() { return "ScriptedRandomGenerator"; }
}
