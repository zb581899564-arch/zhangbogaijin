package org.uma.jmetal.algorithm.multiobjective.mypso.v35;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.uma.jmetal.algorithm.multiobjective.mypso.zhangbo.ZhangBoEvaluatedPddrSelector.Candidate;
import org.uma.jmetal.algorithm.multiobjective.mypso.zhangbo.ZhangBoEvaluatedPddrSelector.Source;
import org.uma.jmetal.algorithm.multiobjective.mypso.zhangbo.ZhangBoLineageTag;
import org.uma.jmetal.algorithm.multiobjective.mypso.zhangbo.ZhangBoQgController;
import org.uma.jmetal.algorithm.multiobjective.mypso.zhangbo.ZhangBoSubSwarm;
import org.uma.jmetal.algorithm.multiobjective.mypso.zhangbo.ZhangBoSubSwarmSemantics;
import org.uma.jmetal.algorithm.multiobjective.mypso.zhangbo.cata.ZhangBoPreEvaluatedTag;
import org.uma.jmetal.solution.PermutationSolution;

/**
 * Read-only adapter from the real PDDR observer inputs to immutable metadata.
 * The current observer call point does not expose a complete candidate/parent
 * identity or physical capacity vector, so those fields intentionally retain
 * explicit unobservable reason tokens.
 */
public final class V35PddrCandidateMetadataAdapter {
  public static final String CANDIDATE_ID_ATTRIBUTE = "candidateId";
  public static final String PARENT_ID_ATTRIBUTE = "parentId";

  private V35PddrCandidateMetadataAdapter() { }

  public static List<V35PddrCandidateMetadata> capture(
      List<PermutationSolution<Integer>> pool,
      List<Source> sources,
      List<Candidate> selected) {
    return capture(pool, sources, selected, true);
  }

  /**
   * Captures the exact PDDR call inputs.  The lineage flag is a semantic
   * applicability bit, not a value inferred from a missing tag: A2 has no
   * lineage archive by design, while A4 does.
   */
  public static List<V35PddrCandidateMetadata> capture(
      List<PermutationSolution<Integer>> pool,
      List<Source> sources,
      List<Candidate> selected,
      boolean lineageApplicable) {
    if (pool == null || sources == null || selected == null
        || pool.size() != sources.size()) {
      throw new IllegalArgumentException("unaligned PDDR metadata inputs");
    }
    Map<Integer, Candidate> selectedByOrder = selectedByOrder(selected);
    List<V35PddrCandidateMetadata> result =
        new ArrayList<V35PddrCandidateMetadata>(pool.size());
    for (int index = 0; index < pool.size(); index++) {
      PermutationSolution<Integer> solution = pool.get(index);
      Source source = sources.get(index);
      Candidate selectedCandidate = selectedByOrder.get(index);
      result.add(captureOne(solution, source, selectedCandidate, lineageApplicable));
    }
    return Collections.unmodifiableList(result);
  }

  private static Map<Integer, Candidate> selectedByOrder(List<Candidate> selected) {
    Map<Integer, Candidate> result = new HashMap<Integer, Candidate>();
    for (Candidate candidate : selected) {
      if (candidate == null) throw new IllegalArgumentException("null selected candidate");
      int order = candidate.getOriginalOrder();
      if (result.put(order, candidate) != null) {
        throw new IllegalArgumentException("duplicate selected originalOrder=" + order);
      }
    }
    return result;
  }

  private static V35PddrCandidateMetadata captureOne(
      PermutationSolution<Integer> solution, Source source, Candidate selectedCandidate,
      boolean lineageApplicable) {
    EnumSet<V35PddrCandidateMetadata.UnobservableReason> reasons =
        EnumSet.noneOf(V35PddrCandidateMetadata.UnobservableReason.class);

    String candidateId = explicitText(solution, CANDIDATE_ID_ATTRIBUTE);
    if (candidateId == null) {
      candidateId = reason(V35PddrCandidateMetadata.UnobservableReason.CANDIDATE_ID, reasons);
    }

    String candidateFingerprint = fingerprint(solution, reasons);
    String parentId = explicitText(solution, PARENT_ID_ATTRIBUTE);
    if (parentId == null) {
      if (source == Source.PARENT) {
        parentId = V35PddrCandidateMetadata.NOT_APPLICABLE;
      } else {
        parentId = reason(V35PddrCandidateMetadata.UnobservableReason.PARENT_ID, reasons);
      }
    }

    ZhangBoPreEvaluatedTag preEvaluatedTag = ZhangBoPreEvaluatedTag.get(solution);
    ZhangBoLineageTag lineageTag = lineageTag(solution);
    String lineageId;
    String parentLineageId;
    if (!lineageApplicable) {
      lineageId = V35PddrCandidateMetadata.NOT_APPLICABLE;
      parentLineageId = V35PddrCandidateMetadata.NOT_APPLICABLE;
    } else if (lineageTag != null) {
      // These values must remain direct tag values.  Do not derive either one
      // from a candidate fingerprint, ordinal, or the other lineage field.
      lineageId = String.valueOf(lineageTag.getLineageId());
      parentLineageId = String.valueOf(lineageTag.getParentLineageId());
    } else {
      String unavailable = reason(V35PddrCandidateMetadata.UnobservableReason.LINEAGE_TAG,
          reasons);
      lineageId = unavailable;
      parentLineageId = unavailable;
    }

    Integer physicalSlot = physicalSlotBefore(source, preEvaluatedTag, selectedCandidate, solution);
    String physicalSlotText;
    if (physicalSlot == null) {
      physicalSlotText = reason(
          V35PddrCandidateMetadata.UnobservableReason.PHYSICAL_SLOT_BEFORE, reasons);
    } else {
      physicalSlotText = String.valueOf(physicalSlot);
    }

    String parentSlotText;
    if (source == Source.PARENT) {
      // A parent candidate has no parent candidate.  Its own physical slot is
      // kept separately in physicalSlotBefore when the selected Candidate
      // exposes it.
      parentSlotText = V35PddrCandidateMetadata.NOT_APPLICABLE;
    } else if (preEvaluatedTag != null) {
      parentSlotText = String.valueOf(preEvaluatedTag.getParentSlot());
    } else if (selectedCandidate != null && selectedCandidate.getSourceSlot() >= 0) {
      // Candidate.sourceSlot is the real source/parent slot supplied to the
      // selector.  It is a fallback only for selected candidates; pool index
      // is never used as a substitute.
      parentSlotText = String.valueOf(selectedCandidate.getSourceSlot());
    } else {
      parentSlotText = reason(
          V35PddrCandidateMetadata.UnobservableReason.PARENT_SLOT, reasons);
    }

    String roleBefore = roleBefore(solution, reasons);
    boolean newCandidate = source != null && source != Source.PARENT;
    boolean preEvaluated = preEvaluatedTag != null;
    String sourceText;
    if (source == null) {
      sourceText = reason(V35PddrCandidateMetadata.UnobservableReason.SOURCE, reasons);
    } else {
      sourceText = source.name();
    }
    return V35PddrCandidateMetadata.create(candidateId, candidateFingerprint, source,
        sourceText, parentId, parentSlotText, lineageId, parentLineageId,
        physicalSlotText, roleBefore, newCandidate, preEvaluated, reasons);
  }

  private static String explicitText(PermutationSolution<Integer> solution, String key) {
    if (solution == null) return null;
    Object value = solution.getAttribute(key);
    if (value == null) return null;
    String text = String.valueOf(value);
    return text.length() == 0 ? null : text;
  }

  private static String fingerprint(PermutationSolution<Integer> solution,
      Set<V35PddrCandidateMetadata.UnobservableReason> reasons) {
    if (solution == null) {
      return reason(V35PddrCandidateMetadata.UnobservableReason.CANDIDATE_FINGERPRINT, reasons);
    }
    try {
      // This is the real canonical permutation fingerprint.  The observer
      // may additionally emit a SHA-256 stable fingerprint, but that hash is
      // never used as candidateId or parentId.
      return ZhangBoQgController.fingerprint(solution);
    } catch (RuntimeException error) {
      return reason(V35PddrCandidateMetadata.UnobservableReason.CANDIDATE_FINGERPRINT, reasons);
    }
  }

  private static ZhangBoLineageTag lineageTag(PermutationSolution<Integer> solution) {
    if (solution == null) return null;
    Object value = solution.getAttribute(ZhangBoLineageTag.class);
    return value instanceof ZhangBoLineageTag ? (ZhangBoLineageTag) value : null;
  }

  private static Integer physicalSlotBefore(Source source,
      ZhangBoPreEvaluatedTag preEvaluatedTag, Candidate selectedCandidate,
      PermutationSolution<Integer> solution) {
    if (solution != null) {
      Object value = solution.getAttribute(ZhangBoSubSwarm.class);
      if (value instanceof ZhangBoSubSwarm) {
        // This is the registered physical subgroup slot (1..4).  It is
        // intentionally independent of PDDR pool order, candidate ordinal,
        // sourceSlot, or an index modulo four.
        return ZhangBoSubSwarmSemantics.physicalSlotForRole((ZhangBoSubSwarm) value);
      }
    }
    return null;
  }

  private static String roleBefore(PermutationSolution<Integer> solution,
      Set<V35PddrCandidateMetadata.UnobservableReason> reasons) {
    if (solution != null) {
      Object value = solution.getAttribute(ZhangBoSubSwarm.class);
      if (value instanceof ZhangBoSubSwarm) {
        ZhangBoSubSwarm taggedRole = (ZhangBoSubSwarm) value;
        // Resolve through the formal semantic mapping.  The round observer
        // must not reconstruct a role from an index or an ordinal.
        int physicalSubgroupSlot = ZhangBoSubSwarmSemantics.physicalSlotForRole(taggedRole);
        return ZhangBoSubSwarmSemantics.roleForPhysicalSlot(physicalSubgroupSlot).name();
      }
    }
    return reason(V35PddrCandidateMetadata.UnobservableReason.SEMANTIC_ROLE_BEFORE, reasons);
  }

  private static String reason(V35PddrCandidateMetadata.UnobservableReason reason,
      Set<V35PddrCandidateMetadata.UnobservableReason> reasons) {
    reasons.add(reason);
    return reason.token();
  }

  /**
   * Contract result for B's observable evidence.  A capacity vector is
   * deliberately an explicit input: the current forbidden call point does
   * not pass it, so the observer must report that aspect as unavailable.
   */
  public static final class ContractReport {
    private final boolean allRolesPresent;
    private final boolean physicalCapacityKnown;
    private final boolean physicalRoleLayoutConsistent;
    private final boolean sameParentSlotRole;
    private final boolean lineageNonConstant;
    private final boolean parentIdNotUniformPlaceholder;

    private ContractReport(boolean allRolesPresent, boolean physicalCapacityKnown,
        boolean physicalRoleLayoutConsistent, boolean sameParentSlotRole,
        boolean lineageNonConstant, boolean parentIdNotUniformPlaceholder) {
      this.allRolesPresent = allRolesPresent;
      this.physicalCapacityKnown = physicalCapacityKnown;
      this.physicalRoleLayoutConsistent = physicalRoleLayoutConsistent;
      this.sameParentSlotRole = sameParentSlotRole;
      this.lineageNonConstant = lineageNonConstant;
      this.parentIdNotUniformPlaceholder = parentIdNotUniformPlaceholder;
    }

    public boolean areAllRolesPresent() { return allRolesPresent; }
    public boolean isPhysicalCapacityKnown() { return physicalCapacityKnown; }
    public boolean isPhysicalRoleLayoutConsistent() { return physicalRoleLayoutConsistent; }
    public boolean isSameParentSlotRole() { return sameParentSlotRole; }
    public boolean isLineageNonConstant() { return lineageNonConstant; }
    public boolean isParentIdNotUniformPlaceholder() {
      return parentIdNotUniformPlaceholder;
    }

    public boolean isPass() {
      return allRolesPresent && physicalCapacityKnown && physicalRoleLayoutConsistent
          && sameParentSlotRole && lineageNonConstant && parentIdNotUniformPlaceholder;
    }

    public String toText() {
      return "roles=" + allRolesPresent
          + ",physicalCapacityKnown=" + physicalCapacityKnown
          + ",physicalRoleLayoutConsistent=" + physicalRoleLayoutConsistent
          + ",sameParentSlotRole=" + sameParentSlotRole
          + ",lineageNonConstant=" + lineageNonConstant
          + ",parentIdNotUniformPlaceholder=" + parentIdNotUniformPlaceholder;
    }
  }

  public static ContractReport validateContract(
      List<V35PddrCandidateMetadata> metadata, int[] physicalCapacities) {
    return validateContract(metadata, physicalCapacities, true);
  }

  public static ContractReport validateContract(
      List<V35PddrCandidateMetadata> metadata, int[] physicalCapacities,
      boolean lineageApplicable) {
    if (metadata == null) throw new IllegalArgumentException("metadata");
    Set<String> roles = new HashSet<String>();
    Map<String, String> roleByParentSlot = new HashMap<String, String>();
    Set<String> lineages = new HashSet<String>();
    Set<String> observedParentIds = new HashSet<String>();
    boolean sameParentSlotRole = true;
    boolean physicalRoleLayoutConsistent = physicalCapacities != null;
    for (V35PddrCandidateMetadata value : metadata) {
      if (value == null) continue;
      if (isRole(value.getSemanticRoleBefore())) roles.add(value.getSemanticRoleBefore());
      if (isObservedNumber(value.getParentSlot()) && isRole(value.getSemanticRoleBefore())) {
        String previous = roleByParentSlot.put(value.getParentSlot(),
            value.getSemanticRoleBefore());
        if (previous != null && !previous.equals(value.getSemanticRoleBefore())) {
          sameParentSlotRole = false;
        }
      }
      if (isObservedNumber(value.getLineageId())) lineages.add(value.getLineageId());
      if (isObservedId(value.getParentId())) observedParentIds.add(value.getParentId());
      if (physicalCapacities != null && isObservedNumber(value.getPhysicalSlotBefore())
          && isRole(value.getSemanticRoleBefore())) {
        int physicalSlot = Integer.parseInt(value.getPhysicalSlotBefore());
        ZhangBoSubSwarm expected = roleForPhysicalSlot(physicalSlot);
        if (expected == null || !expected.name().equals(value.getSemanticRoleBefore())) {
          physicalRoleLayoutConsistent = false;
        }
      }
    }
    boolean capacityKnown = physicalCapacities != null && validCapacities(physicalCapacities);
    if (!capacityKnown) physicalRoleLayoutConsistent = false;
    return new ContractReport(roles.contains("G1_CMAX") && roles.contains("G4_BALANCED")
        && roles.contains("G2_TEC") && roles.contains("G3_TWC"), capacityKnown,
        physicalRoleLayoutConsistent, sameParentSlotRole,
        !lineageApplicable || lineages.size() > 1,
        observedParentIds.size() > 1);
  }

  private static boolean isRole(String value) {
    return "G1_CMAX".equals(value) || "G4_BALANCED".equals(value)
        || "G2_TEC".equals(value) || "G3_TWC".equals(value);
  }

  private static boolean isObservedNumber(String value) {
    if (value == null || value.length() == 0) return false;
    try {
      Integer.parseInt(value);
      return true;
    } catch (NumberFormatException error) {
      return false;
    }
  }

  private static boolean isObservedId(String value) {
    return value != null && value.length() > 0
        && !V35PddrCandidateMetadata.NOT_APPLICABLE.equals(value)
        && !value.startsWith("UNOBSERVABLE_");
  }

  private static boolean validCapacities(int[] capacities) {
    if (capacities.length != 4) return false;
    int sum = 0;
    for (int value : capacities) {
      if (value < 1) return false;
      sum += value;
    }
    return sum > 0;
  }

  private static ZhangBoSubSwarm roleForPhysicalSlot(int physicalSlot) {
    if (physicalSlot < 1 || physicalSlot > 4) return null;
    return ZhangBoSubSwarmSemantics.roleForPhysicalSlot(physicalSlot);
  }

  /** Stable CSV escaping used by the observer for raw canonical fingerprints. */
  public static String csv(String value) {
    if (value == null) return "";
    if (value.indexOf(',') >= 0 || value.indexOf('"') >= 0
        || value.indexOf('\n') >= 0 || value.indexOf('\r') >= 0) {
      return '"' + value.replace("\"", "\"\"") + '"';
    }
    return value;
  }
}
