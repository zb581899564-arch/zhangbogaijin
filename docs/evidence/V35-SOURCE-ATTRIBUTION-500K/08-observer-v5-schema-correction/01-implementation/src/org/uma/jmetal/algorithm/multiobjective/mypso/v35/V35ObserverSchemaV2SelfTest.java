package org.uma.jmetal.algorithm.multiobjective.mypso.v35;

/** Zero-FE contract tests for the V5 source-attribution observer schema. */
public final class V35ObserverSchemaV2SelfTest {
  private V35ObserverSchemaV2SelfTest() { }

  public static void main(String[] args) {
    require("v35-source-attribution-observer-schema-v2".equals(
        V35SourceAttributionObserver.SCHEMA_VERSION), "schemaVersion");
    String event = V35SourceAttributionObserver.eventHeaderForTest();
    for (String required : new String[]{"actualFE", "nominalFE", "generation",
        "outerCycle", "qRound", "rawSource", "firstLevelSource", "QpAction"}) {
      require(has(event, required), "eventHeader:" + required);
    }
    String lifecycle = V35SourceAttributionObserver.lifecycleHeaderForTest();
    for (String required : new String[]{"actualFE", "nominalFE", "eventType",
        "subjectFingerprint", "relatedFingerprint", "source", "action"}) {
      require(has(lifecycle, required), "lifecycleHeader:" + required);
    }
    require(V35SourceAttributionObserver.nominalWindowForTest(0L) == 0L, "nominal0");
    require(V35SourceAttributionObserver.nominalWindowForTest(1L) == 25000L, "nominal1");
    require(V35SourceAttributionObserver.nominalWindowForTest(25000L) == 25000L,
        "nominal25000");
    require(V35SourceAttributionObserver.nominalWindowForTest(25001L) == 50000L,
        "nominal25001");
    require(V35SourceAttributionObserver.strictlyDominatesForTest(
        new double[]{1.0, 2.0, 3.0}, new double[]{2.0, 2.0, 3.0}), "dominates");
    require(!V35SourceAttributionObserver.strictlyDominatesForTest(
        new double[]{1.0, 3.0, 3.0}, new double[]{2.0, 2.0, 3.0}), "notDominates");
    System.out.println("V35_OBSERVER_SCHEMA_V2_SELF_TEST_PASSED");
  }

  private static boolean has(String header, String field) {
    for (String value : header.trim().split(",")) if (field.equals(value)) return true;
    return false;
  }

  private static void require(boolean condition, String label) {
    if (!condition) throw new IllegalStateException("FAILED:" + label);
  }
}

