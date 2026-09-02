package org.uma.jmetal.algorithm.multiobjective.mypso.zhangbo.qpv2;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Thread-safe / execution-bound telemetry sink for Qp-v2 Phase B1 observation.
 * Follows V5 observer discipline (observation-only, zero decision impact).
 */
public final class V35QpV2TelemetrySink {

  public static final class QpPoolSelectionEvent {
    public final long actualFE;
    public final int outerCycle;
    public final int qRound;
    public final long lineageId;
    public final String group;
    public final String action;
    public final String mask;
    public final int archiveSize;
    public final int qpPoolK;
    public final int qpPoolSize;
    public final int qpPoolIndex;
    public final boolean qpSelectedIsCanonical;
    public final boolean drewExtraRng;
    public final String selectedTeacherFingerprint;
    public final String canonicalTeacherFingerprint;

    public QpPoolSelectionEvent(
        long actualFE, int outerCycle, int qRound, long lineageId,
        String group, String action, String mask, int archiveSize,
        int qpPoolK, int qpPoolSize, int qpPoolIndex,
        boolean qpSelectedIsCanonical, boolean drewExtraRng,
        String selectedTeacherFingerprint, String canonicalTeacherFingerprint) {
      this.actualFE = actualFE;
      this.outerCycle = outerCycle;
      this.qRound = qRound;
      this.lineageId = lineageId;
      this.group = group;
      this.action = action;
      this.mask = mask;
      this.archiveSize = archiveSize;
      this.qpPoolK = qpPoolK;
      this.qpPoolSize = qpPoolSize;
      this.qpPoolIndex = qpPoolIndex;
      this.qpSelectedIsCanonical = qpSelectedIsCanonical;
      this.drewExtraRng = drewExtraRng;
      this.selectedTeacherFingerprint = selectedTeacherFingerprint;
      this.canonicalTeacherFingerprint = canonicalTeacherFingerprint;
    }
  }

  private final List<QpPoolSelectionEvent> events = new ArrayList<QpPoolSelectionEvent>();
  private long totalExtraRngDraws = 0L;
  private long totalQpSelections = 0L;
  private long poolSizeGe2Selections = 0L;
  private long nonCanonicalSelections = 0L;

  public synchronized void recordEvent(QpPoolSelectionEvent event) {
    if (event == null) return;
    events.add(event);
    totalQpSelections++;
    if (event.qpPoolSize >= 2) {
      poolSizeGe2Selections++;
    }
    if (!event.qpSelectedIsCanonical) {
      nonCanonicalSelections++;
    }
    if (event.drewExtraRng) {
      totalExtraRngDraws++;
    }
  }

  public synchronized List<QpPoolSelectionEvent> getEvents() {
    return Collections.unmodifiableList(new ArrayList<QpPoolSelectionEvent>(events));
  }

  public synchronized long getTotalExtraRngDraws() { return totalExtraRngDraws; }
  public synchronized long getTotalQpSelections() { return totalQpSelections; }
  public synchronized long getPoolSizeGe2Selections() { return poolSizeGe2Selections; }
  public synchronized long getNonCanonicalSelections() { return nonCanonicalSelections; }

  public synchronized void clear() {
    events.clear();
    totalExtraRngDraws = 0L;
    totalQpSelections = 0L;
    poolSizeGe2Selections = 0L;
    nonCanonicalSelections = 0L;
  }
}
