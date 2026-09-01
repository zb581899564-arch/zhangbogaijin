package org.uma.jmetal.algorithm.multiobjective.mypso.v35;

/** Ordering of the two local-search families inside one shared local-FE window. */
public enum V35LocalSearchOrder {
  /** Historical v3.5 order. */
  CATA_THEN_INHERITED,
  /** FC-6A.4 single-variable alternative. */
  INHERITED_THEN_CATA
}
