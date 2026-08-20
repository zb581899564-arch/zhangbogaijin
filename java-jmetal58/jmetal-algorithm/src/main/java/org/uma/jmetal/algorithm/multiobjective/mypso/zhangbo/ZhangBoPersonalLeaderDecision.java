package org.uma.jmetal.algorithm.multiobjective.mypso.zhangbo;

import org.uma.jmetal.solution.PermutationSolution;

import java.io.Serializable;

/** Common CFVF personal-leader decision for Qp and warmup directional guidance. */
public final class ZhangBoPersonalLeaderDecision implements Serializable {
  private static final long serialVersionUID = 1L;

  public enum Source { QP_ACTION, WARMUP_DIRECTIONAL, ARCHIVE_DIRECTIONAL, ARCHIVE_RANDOM_POLICY }

  private final long branchId;
  private final ZhangBoSubSwarm group;
  private final Source source;
  private final ZhangBoArchiveEntry directionalEntry;
  private final ZhangBoQpController.Selection qpSelection;

  private ZhangBoPersonalLeaderDecision(
      long branchId, ZhangBoSubSwarm group, Source source,
      ZhangBoArchiveEntry directionalEntry, ZhangBoQpController.Selection qpSelection) {
    this.branchId = branchId;
    this.group = group;
    this.source = source;
    this.directionalEntry = directionalEntry;
    this.qpSelection = qpSelection;
  }

  public static ZhangBoPersonalLeaderDecision fromQp(
      ZhangBoQpController.Selection selection) {
    if (selection == null) throw new IllegalArgumentException("selection");
    return new ZhangBoPersonalLeaderDecision(selection.getBranchId(), selection.getGroup(),
        Source.QP_ACTION, null, selection);
  }

  public static ZhangBoPersonalLeaderDecision warmupDirectional(
      long branchId, ZhangBoSubSwarm group, ZhangBoArchiveEntry entry) {
    if (group == null || entry == null) throw new IllegalArgumentException("Warmup leader fields");
    return new ZhangBoPersonalLeaderDecision(branchId, group,
        Source.WARMUP_DIRECTIONAL, entry, null);
  }

  public static ZhangBoPersonalLeaderDecision archive(
      long branchId, ZhangBoSubSwarm group, ZhangBoArchiveEntry entry, boolean randomPolicy) {
    if (group == null || entry == null) throw new IllegalArgumentException("Archive leader fields");
    return new ZhangBoPersonalLeaderDecision(branchId, group,
        randomPolicy ? Source.ARCHIVE_RANDOM_POLICY : Source.ARCHIVE_DIRECTIONAL,
        entry, null);
  }

  public long getBranchId() { return branchId; }
  public ZhangBoSubSwarm getGroup() { return group; }
  public Source getSource() { return source; }
  public String getSelectedPbestFingerprint() {
    return source == Source.QP_ACTION ? qpSelection.getSelectedPbestFingerprint()
        : directionalEntry.getFingerprint();
  }
  public PermutationSolution<Integer> pbestSolution(PermutationSolution<Integer> template) {
    return source == Source.QP_ACTION ? qpSelection.pbestSolution(template)
        : directionalEntry.toSolution(template);
  }
}
