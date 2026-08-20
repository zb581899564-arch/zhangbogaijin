package org.uma.jmetal.problem.multiobjective.dfsp.setup;

import org.junit.Test;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;

public class ProductFamilySetupModelTest {
  @Test
  public void formalModelIsSingleFamilyAndSequenceIndependent() {
    ProductFamilySetupModel model = ProductFamilySetupModel.degenerate(10, 2);
    assertEquals(FamilyMode.DEGENERATE_SINGLE_FAMILY, model.getFamilyData().getFamilyMode());
    assertEquals(SetupMode.SEQUENCE_INDEPENDENT, model.getSetupMode());
    assertEquals(0, model.getFamilyData().getAssignment().getFamilyOfJob(9));
    assertEquals(3.0, model.setupTime(3.0, 1, 4, 9), 0.0);
    assertArrayEquals(new int[10], model.getFamilyData().getAssignment().getFamilyOfJob());
  }

  @Test(expected = IllegalArgumentException.class)
  public void formalModelRejectsMultipleFamilies() {
    new ProductFamilySetupModel(SetupMode.SEQUENCE_INDEPENDENT,
        new ProductFamilyData(FamilyMode.DEGENERATE_SINGLE_FAMILY,
            new ProductFamilyAssignment(new int[] {0, 1}, 2),
            new ProductFamilyTransitionMatrix(new double[2][2][2], 2, 2)), 1.0);
  }

  @Test
  public void futureModeRemainsAnExplicitNonFormalExtensionPoint() {
    ProductFamilySetupModel future = new ProductFamilySetupModel(
        SetupMode.SEQUENCE_DEPENDENT_FUTURE, ProductFamilyData.degenerate(2, 2), 1.0);
    org.junit.Assert.assertFalse(future.isFormalDegenerate());
  }
}
