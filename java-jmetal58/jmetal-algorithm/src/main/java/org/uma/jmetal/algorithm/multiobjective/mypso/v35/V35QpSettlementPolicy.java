package org.uma.jmetal.algorithm.multiobjective.mypso.v35;

/**
 * Settlement authority for a diagnostic Qp run.
 *
 * <p>{@link #OBSERVE_ONLY_ALL_CYCLES} is deliberately not a production
 * algorithm setting.  It preserves Qp's selected personal leader and lineage
 * archive update, while withholding both reward calculation and TD table
 * updates so that the Qp action policy can be isolated from learning.</p>
 */
public enum V35QpSettlementPolicy {
  /** Frozen A0--A4 behaviour: follow the dual-Q phase scheduler. */
  STANDARD_BY_DUAL_Q,
  /** V35-A3-D2 only: settle every selected Qp action as an observation. */
  OBSERVE_ONLY_ALL_CYCLES
}
