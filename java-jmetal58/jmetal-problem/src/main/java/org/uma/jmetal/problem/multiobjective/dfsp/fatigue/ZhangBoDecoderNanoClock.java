package org.uma.jmetal.problem.multiobjective.dfsp.fatigue;

/** Injectable monotonic clock used only for decoder performance accounting. */
public interface ZhangBoDecoderNanoClock {
  long nanoTime();

  ZhangBoDecoderNanoClock SYSTEM = new ZhangBoDecoderNanoClock() {
    @Override public long nanoTime() { return System.nanoTime(); }
  };
}
