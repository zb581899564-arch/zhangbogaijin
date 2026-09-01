/** Self-contained contract test for the post-run phase-budget classifier. */
public final class V35ProductionPreflightBudgetTest {
  private V35ProductionPreflightBudgetTest() { }

  public static void main(String[] args) {
    assertDecision(50000, 50000, 50000, "EXACT_MAX_FE", true, 0L);
    assertDecision(50000, 48269, 48269, "PHASE_CONSISTENT_TAIL_STOP", true, 1731L);
    assertDecision(100000, 96025, 96025, "PHASE_CONSISTENT_TAIL_STOP", true, 3975L);
    assertDecision(50000, 45000, 45000, "INVALID", false, 5000L);
    assertDecision(50000, 44999, 44999, "INVALID", false, 5001L);
    assertDecision(50000, 50001, 50001, "INVALID", false, -1L);
    assertDecision(50000, 0, 0, "INVALID", false, 50000L);
    assertDecision(50000, 48269, 48268, "INVALID", false, 1731L);
    assertArm("A0", "A0_BASELINE");
    assertArm("A1", "A1_DSCR");
    assertArm("A2", "A2_CFVF");
    assertArm("A3", "A3_QP_PERSONAL_ARCHIVE");
    assertArm("A4", "A4_BUDGET_AWARE_CATA");
    try {
      V35ProductionPreflight.armForLabel("A9");
      throw new AssertionError("invalid paper arm label was accepted");
    } catch (IllegalArgumentException expected) {
      // Expected fail-closed CLI behaviour.
    }
    System.out.println("V35_PHASE_BUDGET_CLASSIFIER_TEST_PASSED");
  }

  private static void assertDecision(long requested, long actual, long decoder, String kind,
      boolean accepted, long remaining) {
    V35ProductionPreflight.BudgetTermination decision =
        V35ProductionPreflight.BudgetTermination.classify(requested, actual, decoder, 100, 50);
    if (!kind.equals(decision.getTerminationKind()) || accepted != decision.isAccepted()
        || remaining != decision.getRemainingFE() || decision.getQPhaseFE() != 5000L) {
      throw new AssertionError("budget classifier mismatch requested=" + requested + " actual=" + actual
          + " decoder=" + decoder + " kind=" + decision.getTerminationKind() + " accepted="
          + decision.isAccepted() + " remaining=" + decision.getRemainingFE());
    }
  }

  private static void assertArm(String label, String enumName) {
    if (!enumName.equals(V35ProductionPreflight.armForLabel(label).name())) {
      throw new AssertionError("arm label mapping mismatch=" + label);
    }
  }
}
