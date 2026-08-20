package org.uma.jmetal.algorithm.multiobjective.mypso.zhangbo.neighborhood;

/** P7.1 candidate caps; no main-loop enable flag is provided in this stage. */
public final class ZhangBoNeighborhoodConfiguration {
  public static final int BASIC_CAP = 1;
  public static final int O10_CAP = 6;
  public static final int O11_CAP = 3;
  public static final int O12_CAP = 3;
  public static final int O13_CAP = 4;
  public static final double CRITICAL_TOLERANCE = 1e-9;

  private ZhangBoNeighborhoodConfiguration() { }

  public static int cap(ZhangBoNeighborhoodId id) {
    switch (id) {
      case O10_CRITICAL_BLOCK: return O10_CAP;
      case O11_FATIGUE_WORKER_REASSIGNMENT: return O11_CAP;
      case O12_JOINT_MACHINE_WORKER: return O12_CAP;
      case O13_NATURAL_RECOVERY_WINDOW: return O13_CAP;
      default: return BASIC_CAP;
    }
  }
}
