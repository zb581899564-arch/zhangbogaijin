package org.uma.jmetal.algorithm.multiobjective.mypso.zhangbo;

/** Explicit environmental-selection semantics for evaluated PDDR. */
public enum PddrSelectionMode {
  /** Original author-compatible ranking: (PDDR score, original pool order). */
  GLOBAL_ORIGINAL,
  /** Historical three-boundary reservation; unavailable to new FC-6 experiments. */
  BP_RESERVED_LEGACY,
  /** Fixed physical-region allocation followed by original global backfill. */
  REGION_AWARE
}
