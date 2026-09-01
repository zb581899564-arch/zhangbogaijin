package org.uma.jmetal.algorithm.multiobjective.mypso.v35;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * Opt-in, append-only telemetry for the A2-to-A3 causal decomposition.
 * It is deliberately separate from the formal event logs and never enters a
 * configuration hash, action hash, random draw, or search decision.
 */
public final class V35A2A3PersonalLeaderAudit implements Serializable {
  private static final long serialVersionUID = 1L;
  private boolean enabled;
  private final List<String> rows = new ArrayList<>();

  public void setEnabled(boolean value) { enabled = value; }

  public void record(long generation, long fe, String group, long branchId, long lineageId,
      String source, String action, String mask, int archiveSize, String fingerprint,
      boolean fallback) {
    if (!enabled) return;
    rows.add(generation + "," + fe + "," + csv(group) + "," + branchId + "," + lineageId
        + "," + csv(source) + "," + csv(action) + "," + csv(mask) + "," + archiveSize
        + "," + csv(fingerprint) + "," + fallback);
  }

  public String toCsv() {
    StringBuilder out = new StringBuilder(
        "generation,FE,group,branchId,lineageId,source,action,mask,archiveSize,selectedPbestFingerprint,fallback\n");
    for (String row : rows) out.append(row).append('\n');
    return out.toString();
  }

  public long getEventCount() { return rows.size(); }

  private static String csv(String value) {
    if (value == null) return "";
    String escaped = value.replace("\"", "\"\"");
    return '"' + escaped + '"';
  }
}
