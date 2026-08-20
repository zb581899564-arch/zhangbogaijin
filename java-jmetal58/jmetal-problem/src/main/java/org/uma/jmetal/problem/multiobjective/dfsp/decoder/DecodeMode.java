package org.uma.jmetal.problem.multiobjective.dfsp.decoder;

/** Isolated semantic modes for the Chapter 4 decoder. */
public enum DecodeMode {
  PUBLISHED_STOCHASTIC("published_baseline"),
  AUTHOR_ACTUAL("author_actual"),
  DETERMINISTIC_CANONICAL("deterministic_canonical");

  private final String semanticTag;

  DecodeMode(String semanticTag) {
    this.semanticTag = semanticTag;
  }

  public String getSemanticTag() {
    return semanticTag;
  }
}
