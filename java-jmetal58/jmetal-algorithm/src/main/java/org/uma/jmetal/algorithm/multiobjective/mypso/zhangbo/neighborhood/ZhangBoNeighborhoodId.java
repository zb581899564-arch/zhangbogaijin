package org.uma.jmetal.algorithm.multiobjective.mypso.zhangbo.neighborhood;

/** Stable neighborhood numbering defined by the integrated v2 scheme. */
public enum ZhangBoNeighborhoodId {
  O1_JS_INSERT,
  O2_JS_REVERSE,
  O3_JS_SWAP,
  O4_WA_LOAD_TRANSFER,
  O5_WA_WEAK_TO_STRONG,
  O6_WA_SWAP,
  O7_MA_LOAD_TRANSFER,
  O8_MA_WEAK_TO_STRONG,
  O9_MA_SWAP,
  O10_CRITICAL_BLOCK,
  O11_FATIGUE_WORKER_REASSIGNMENT,
  O12_JOINT_MACHINE_WORKER,
  O13_NATURAL_RECOVERY_WINDOW;

  public int getNumber() {
    switch (this) {
      case O1_JS_INSERT: return 1;
      case O2_JS_REVERSE: return 2;
      case O3_JS_SWAP: return 3;
      case O4_WA_LOAD_TRANSFER: return 4;
      case O5_WA_WEAK_TO_STRONG: return 5;
      case O6_WA_SWAP: return 6;
      case O7_MA_LOAD_TRANSFER: return 7;
      case O8_MA_WEAK_TO_STRONG: return 8;
      case O9_MA_SWAP: return 9;
      case O10_CRITICAL_BLOCK: return 10;
      case O11_FATIGUE_WORKER_REASSIGNMENT: return 11;
      case O12_JOINT_MACHINE_WORKER: return 12;
      case O13_NATURAL_RECOVERY_WINDOW: return 13;
      default: throw new IllegalStateException("Unhandled neighborhood=" + this);
    }
  }
}
