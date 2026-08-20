package org.uma.jmetal.algorithm.multiobjective.mypso.zhangbo.cata;

import java.io.Serializable;
import org.uma.jmetal.solution.PermutationSolution;

/** Immutable marker preventing jMetal's outer evaluateSwarm from charging an evaluated local candidate twice. */
public final class ZhangBoPreEvaluatedTag implements Serializable {
  private static final long serialVersionUID = 1L;

  public enum Source {
    GLOBAL_OFFSPRING,
    GLOBAL_CFVF,
    INTRA_FACTORY_VNS
  }

  private final Source source;
  private final int parentSlot;
  private final long lineageId;
  private final long evaluationOrdinal;

  public ZhangBoPreEvaluatedTag(Source source, int parentSlot, long lineageId,
      long evaluationOrdinal) {
    if (source == null || parentSlot < 0 || evaluationOrdinal < 1L) {
      throw new IllegalArgumentException("Invalid pre-evaluated candidate metadata");
    }
    this.source = source;
    this.parentSlot = parentSlot;
    this.lineageId = lineageId;
    this.evaluationOrdinal = evaluationOrdinal;
  }

  public Source getSource() { return source; }
  public int getParentSlot() { return parentSlot; }
  public long getLineageId() { return lineageId; }
  public long getEvaluationOrdinal() { return evaluationOrdinal; }

  public String toCanonicalText() {
    return "source=" + source + ",parentSlot=" + parentSlot + ",lineageId="
        + lineageId + ",evaluationOrdinal=" + evaluationOrdinal;
  }

  public static ZhangBoPreEvaluatedTag get(PermutationSolution<Integer> solution) {
    if (solution == null) return null;
    Object value = solution.getAttribute(ZhangBoPreEvaluatedTag.class);
    return value instanceof ZhangBoPreEvaluatedTag ? (ZhangBoPreEvaluatedTag) value : null;
  }

  public static boolean isMarked(PermutationSolution<Integer> solution) {
    return get(solution) != null;
  }

  public static void mark(PermutationSolution<Integer> solution, ZhangBoPreEvaluatedTag tag) {
    if (solution == null || tag == null) throw new IllegalArgumentException("solution and tag");
    solution.setAttribute(ZhangBoPreEvaluatedTag.class, tag);
  }
}
