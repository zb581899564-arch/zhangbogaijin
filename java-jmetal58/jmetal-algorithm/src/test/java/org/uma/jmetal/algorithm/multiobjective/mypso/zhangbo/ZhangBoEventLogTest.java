package org.uma.jmetal.algorithm.multiobjective.mypso.zhangbo;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;

import java.util.Arrays;
import org.junit.Test;

public class ZhangBoEventLogTest {
  @Test
  public void boundedModeRetainsOnlyTailButCountsAndHashesEveryEvent() {
    ZhangBoEventLog log = new ZhangBoEventLog(false, 2);
    log.add("a");
    log.add("b");
    String firstHash = log.rollingSha256();
    log.add("c");
    assertEquals(3L, log.getTotalCount());
    assertEquals(Arrays.asList("b", "c"), log.snapshot());
    assertNotEquals(firstHash, log.rollingSha256());
  }

  @Test
  public void fullCapturePreservesEveryEventForI1Evidence() {
    ZhangBoEventLog log = new ZhangBoEventLog(true, 1);
    log.add("a");
    log.add("b");
    log.add("c");
    assertEquals(3L, log.getTotalCount());
    assertEquals(Arrays.asList("a", "b", "c"), log.snapshot());
  }
}
