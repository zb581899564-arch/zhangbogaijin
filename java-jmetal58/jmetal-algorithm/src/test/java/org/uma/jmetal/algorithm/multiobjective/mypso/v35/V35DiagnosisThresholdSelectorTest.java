package org.uma.jmetal.algorithm.multiobjective.mypso.v35;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.junit.Test;

public class V35DiagnosisThresholdSelectorTest {
  @Test
  public void buildsTwentyFiveCandidatesAndIsInputOrderIndependent() {
    String original = csv(20, false);
    String shuffled = csv(20, true);
    V35DiagnosisThresholdSelector selector = new V35DiagnosisThresholdSelector();
    V35DiagnosisThresholdSelector.Selection first = selector.select(original);
    V35DiagnosisThresholdSelector.Selection second = selector.select(shuffled);
    assertEquals(25, first.getCandidates().size());
    assertNotNull(first.getSelected());
    assertEquals(first.candidatesCsv(), second.candidatesCsv());
    assertEquals(first.selectionCsv(), second.selectionCsv());
  }

  @Test
  public void validationReportsMaskedPositiveActionAsMissed() {
    V35DiagnosisThresholdSelector.Candidate value =
        new V35DiagnosisThresholdSelector().validate(csv(1, false), 0.0, 0.0);
    assertEquals(1.0, value.getCoverage(), 0.0);
    assertEquals(1.0, value.getMissedPositiveBestRate(), 0.0);
    assertEquals(0.8, value.getMeanRegret(), 1.0e-12);
  }

  private static String csv(int samples, boolean reverse) {
    List<String> rows = new ArrayList<>();
    for (int sample = 1; sample <= samples; sample++) {
      for (V35MacroNeighborhood action : V35MacroNeighborhood.values()) {
        double gain = action == V35MacroNeighborhood.N5 && sample == 1 ? 1.0 :
            (action == V35MacroNeighborhood.N1 ? (sample == 1 ? 0.2 : 1.0) : 0.0);
        rows.add(sample + ",1,100,0,0,G1_CMAX,BAL,SEQ,"
            + (0.4 + sample * 0.01) + ",MAC,0.2,0.2,0.4,0.2,0.1,0.05,0.03,"
            + action + ",true,10,20,30," + gain + "," + (gain > 0.0)
            + ",false,false,0.0");
      }
    }
    if (reverse) Collections.reverse(rows);
    return V35ShadowDiagnosisAudit.HEADER + "\n" + String.join("\n", rows) + "\n";
  }
}
