package org.uma.jmetal.problem.multiobjective.dfsp.setup;

import java.io.Serializable;

/** Immutable product-family data, retained even when the formal run is degenerate. */
public final class ProductFamilyData implements Serializable {
  private static final long serialVersionUID = 1L;
  private final FamilyMode familyMode;
  private final ProductFamilyAssignment assignment;
  private final ProductFamilyTransitionMatrix transitionMatrix;

  public ProductFamilyData(FamilyMode familyMode, ProductFamilyAssignment assignment,
      ProductFamilyTransitionMatrix transitionMatrix) {
    if (familyMode == null || assignment == null || transitionMatrix == null) {
      throw new IllegalArgumentException("product-family data must not be null");
    }
    if (assignment.getFamilyCount() != transitionMatrix.getFamilyCount()) {
      throw new IllegalArgumentException("family count mismatch");
    }
    this.familyMode = familyMode;
    this.assignment = assignment;
    this.transitionMatrix = transitionMatrix;
    if (familyMode == FamilyMode.DEGENERATE_SINGLE_FAMILY && assignment.getFamilyCount() != 1) {
      throw new IllegalArgumentException("degenerate mode requires exactly one family");
    }
  }

  public static ProductFamilyData degenerate(int jobs, int stages) {
    return new ProductFamilyData(FamilyMode.DEGENERATE_SINGLE_FAMILY,
        new ProductFamilyAssignment(new int[jobs], 1),
        ProductFamilyTransitionMatrix.zero(stages));
  }
  public FamilyMode getFamilyMode() { return familyMode; }
  public ProductFamilyAssignment getAssignment() { return assignment; }
  public ProductFamilyTransitionMatrix getTransitionMatrix() { return transitionMatrix; }
}
