package org.uma.jmetal.algorithm.multiobjective.mypso.v35;

import java.util.Collections;
import java.util.EnumSet;
import java.util.Set;
import org.uma.jmetal.algorithm.multiobjective.mypso.zhangbo.ZhangBoEvaluatedPddrSelector.Source;

/**
 * Immutable, observation-only metadata captured for one candidate entering the
 * PDDR ledger.  A value which is not available at the actual call point is
 * represented by an explicit reason token; it is never replaced with a pool
 * ordinal, a hash, or a lineage identifier.
 */
public final class V35PddrCandidateMetadata {
  public static final String NOT_APPLICABLE = "NOT_APPLICABLE";

  public enum UnobservableReason {
    CANDIDATE_ID,
    CANDIDATE_FINGERPRINT,
    SOURCE,
    PARENT_ID,
    PARENT_SLOT,
    LINEAGE_TAG,
    PHYSICAL_SLOT_BEFORE,
    SEMANTIC_ROLE_BEFORE,
    PHYSICAL_CAPACITY,
    GENERATION;

    public String token() {
      return "UNOBSERVABLE_" + name();
    }
  }

  private final String candidateId;
  private final String candidateFingerprint;
  private final Source source;
  private final String sourceText;
  private final String parentId;
  private final String parentSlot;
  private final String lineageId;
  private final String parentLineageId;
  private final String physicalSlotBefore;
  private final String semanticRoleBefore;
  private final boolean newCandidate;
  private final boolean preEvaluated;
  private final Set<UnobservableReason> unavailableReasons;

  private V35PddrCandidateMetadata(String candidateId, String candidateFingerprint,
      Source source, String sourceText, String parentId, String parentSlot,
      String lineageId, String parentLineageId, String physicalSlotBefore,
      String semanticRoleBefore, boolean newCandidate, boolean preEvaluated,
      Set<UnobservableReason> unavailableReasons) {
    this.candidateId = requireText(candidateId, "candidateId");
    this.candidateFingerprint = requireText(candidateFingerprint, "candidateFingerprint");
    this.source = source;
    this.sourceText = requireText(sourceText, "sourceText");
    this.parentId = requireText(parentId, "parentId");
    this.parentSlot = requireText(parentSlot, "parentSlot");
    this.lineageId = requireText(lineageId, "lineageId");
    this.parentLineageId = requireText(parentLineageId, "parentLineageId");
    this.physicalSlotBefore = requireText(physicalSlotBefore, "physicalSlotBefore");
    this.semanticRoleBefore = requireText(semanticRoleBefore, "semanticRoleBefore");
    this.newCandidate = newCandidate;
    this.preEvaluated = preEvaluated;
    EnumSet<UnobservableReason> copy = EnumSet.noneOf(UnobservableReason.class);
    if (unavailableReasons != null) copy.addAll(unavailableReasons);
    this.unavailableReasons = Collections.unmodifiableSet(copy);
  }

  static V35PddrCandidateMetadata create(String candidateId, String candidateFingerprint,
      Source source, String sourceText, String parentId, String parentSlot,
      String lineageId, String parentLineageId, String physicalSlotBefore,
      String semanticRoleBefore, boolean newCandidate, boolean preEvaluated,
      Set<UnobservableReason> unavailableReasons) {
    return new V35PddrCandidateMetadata(candidateId, candidateFingerprint, source, sourceText,
        parentId, parentSlot, lineageId, parentLineageId, physicalSlotBefore,
        semanticRoleBefore, newCandidate, preEvaluated, unavailableReasons);
  }

  private static String requireText(String value, String label) {
    if (value == null || value.length() == 0) {
      throw new IllegalArgumentException(label + " must not be blank");
    }
    return value;
  }

  public String getCandidateId() { return candidateId; }
  public String getCandidateFingerprint() { return candidateFingerprint; }
  public Source getSource() { return source; }
  public String getSourceText() { return sourceText; }
  public String getParentId() { return parentId; }
  public String getParentSlot() { return parentSlot; }
  public String getLineageId() { return lineageId; }
  public String getParentLineageId() { return parentLineageId; }
  public String getPhysicalSlotBefore() { return physicalSlotBefore; }
  public String getSemanticRoleBefore() { return semanticRoleBefore; }
  public boolean isNewCandidate() { return newCandidate; }
  public boolean isPreEvaluated() { return preEvaluated; }
  public Set<UnobservableReason> getUnavailableReasons() { return unavailableReasons; }

  public boolean hasUnavailableReason(UnobservableReason reason) {
    return unavailableReasons.contains(reason);
  }

  public String unavailableReasonsText() {
    if (unavailableReasons.isEmpty()) return "";
    StringBuilder result = new StringBuilder();
    for (UnobservableReason reason : unavailableReasons) {
      if (result.length() > 0) result.append('|');
      result.append(reason.token());
    }
    return result.toString();
  }
}
