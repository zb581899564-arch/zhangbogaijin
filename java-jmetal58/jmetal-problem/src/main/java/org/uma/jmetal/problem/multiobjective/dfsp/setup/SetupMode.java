package org.uma.jmetal.problem.multiobjective.dfsp.setup;

/** Setup-time semantics. */
public enum SetupMode {
  /** Setup depends only on the current job and stage. */
  SEQUENCE_INDEPENDENT,
  /** Reserved for a separately approved sequence-dependent experiment. */
  SEQUENCE_DEPENDENT_FUTURE
}
