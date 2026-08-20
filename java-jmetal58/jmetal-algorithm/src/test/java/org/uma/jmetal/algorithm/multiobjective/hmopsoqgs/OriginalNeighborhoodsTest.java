package org.uma.jmetal.algorithm.multiobjective.hmopsoqgs;

import org.junit.Test;
import org.uma.jmetal.problem.multiobjective.dfsp.model.Chapter4GoldenFixture;
import org.uma.jmetal.problem.multiobjective.dfsp.model.DhhfspEncodingValidator;
import org.uma.jmetal.problem.multiobjective.dfsp.model.DhhfspFourVectorSolution;
import org.uma.jmetal.problem.multiobjective.dfsp.model.DhhfspInstance;

import static org.junit.Assert.assertEquals;

public class OriginalNeighborhoodsTest {
  @Test
  public void overallV2O1ThroughO9RemainLegalAndUseFrozenNames() {
    Chapter4GoldenFixture fixture = Chapter4GoldenFixture.load();
    DhhfspInstance instance = fixture.getInstance();
    for (int operation = 1; operation <= 9; operation++) {
      ScriptedRandomGenerator random = randomFor(operation);
      OriginalNeighborhoods neighborhoods = new OriginalNeighborhoods(instance, random);
      DhhfspFourVectorSolution candidate = neighborhoods.apply(
          operation, fixture.createSolution(), 0);
      DhhfspEncodingValidator.validateOrThrow(candidate, instance);
      assertEquals("O" + operation, neighborhoods.name(operation).substring(0, 2));
    }
  }

  @Test
  public void criticalInsertAndSwapRepairFactoryDependentResources() {
    Chapter4GoldenFixture fixture = Chapter4GoldenFixture.load();
    DhhfspInstance instance = fixture.getInstance();
    OriginalNeighborhoods insert = new OriginalNeighborhoods(instance,
        new ScriptedRandomGenerator().ints(0, 0, 0));
    DhhfspFourVectorSolution moved = insert.criticalFactoryInsert(
        fixture.createSolution(), 0, 1);
    DhhfspEncodingValidator.validateOrThrow(moved, instance);

    OriginalNeighborhoods swap = new OriginalNeighborhoods(instance,
        new ScriptedRandomGenerator().ints(0, 0, 0, 0, 0, 0));
    DhhfspFourVectorSolution exchanged = swap.criticalFactorySwap(
        fixture.createSolution(), 0, 1);
    DhhfspEncodingValidator.validateOrThrow(exchanged, instance);
  }

  private static ScriptedRandomGenerator randomFor(int operation) {
    if (operation <= 3 || operation == 6 || operation == 9) {
      return new ScriptedRandomGenerator().ints(0, 1);
    }
    if (operation == 4 || operation == 7) return new ScriptedRandomGenerator().ints(0);
    return new ScriptedRandomGenerator();
  }
}
