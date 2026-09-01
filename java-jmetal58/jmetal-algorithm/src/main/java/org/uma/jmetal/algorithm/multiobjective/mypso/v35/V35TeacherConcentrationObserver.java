package org.uma.jmetal.algorithm.multiobjective.mypso.v35;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import org.uma.jmetal.algorithm.multiobjective.mypso.zhangbo.ZhangBoSubSwarm;
import org.uma.jmetal.solution.PermutationSolution;

/**
 * Agent-C teacher lifecycle observer.
 *
 * <p>This class is a pure observation side-channel. A selection event is
 * created once, receives a stable event id, and is only backfilled through that
 * id after the corresponding offspring evaluation. It never participates in
 * Qg/Qp selection, reward, archive, or budget logic.</p>
 *
 * <p>The legacy five-argument bridge is intentionally retained because the
 * current telemetry coordinator does not expose q-state, q-action, slot,
 * candidate view, directional scores, or teacher provenance. Those fields are
 * emitted with field-specific {@code UNOBSERVABLE_*} states instead of being
 * guessed from a role, an ordinal, or a hash.</p>
 */
public final class V35TeacherConcentrationObserver {
  public static final String VERSION = "V35_MIDHORIZON_V3_C_TEACHER";

  private static final String ALL_QG = "ALL_QG";
  private static final String ALL_QP = "ALL_QP";
  private static final String PREVIOUS_CACHE = "PREVIOUS_CACHE";
  private static final String HISTORICAL_CACHE = "HISTORICAL_CACHE";
  private static final String PERSONAL_ARCHIVE = "PERSONAL_ARCHIVE";
  private static final String DIRECTIONAL_REPRESENTATIVE = "DIRECTIONAL_REPRESENTATIVE";
  private static final String GLOBAL_ARCHIVE = "GLOBAL_ARCHIVE";
  private static final List<String> SCOPES = Collections.unmodifiableList(Arrays.asList(
      ALL_QG, ALL_QP, PREVIOUS_CACHE, HISTORICAL_CACHE, PERSONAL_ARCHIVE,
      DIRECTIONAL_REPRESENTATIVE));

  private boolean enabled;
  private final String runId;
  private final String sourceJarSha256;
  private final String configurationHash;
  private final String instanceHash;
  private final long seed;
  private final String arm;
  private final String telemetryMode;
  private long observerErrors;
  private long eventSequence;
  private final List<TeacherEvent> events = new ArrayList<TeacherEvent>();
  private final Map<String, TeacherEvent> eventById = new LinkedHashMap<String, TeacherEvent>();

  public V35TeacherConcentrationObserver(String runId, String sourceJarSha256,
      String configurationHash, String instanceHash, long seed, String arm, boolean enabled) {
    this.runId = runId;
    this.sourceJarSha256 = sourceJarSha256;
    this.configurationHash = configurationHash;
    this.instanceHash = instanceHash;
    this.seed = seed;
    this.arm = arm;
    this.enabled = enabled;
    this.telemetryMode = enabled ? "ON" : "OFF";
  }

  public void setEnabled(boolean value) {
    enabled = value;
    if (!value) {
      events.clear();
      eventById.clear();
      eventSequence = 0L;
      observerErrors = 0L;
    }
  }

  public boolean isEnabled() { return enabled; }

  /** Known provenance labels supplied by a real Qg/Qp selection hook. */
  public enum TeacherSource {
    PREVIOUS_CACHE,
    HISTORICAL_CACHE,
    PERSONAL_ARCHIVE,
    DIRECTIONAL_REPRESENTATIVE,
    GLOBAL_ARCHIVE
  }

  /** Cache labels supplied by the real teacher-selection path. */
  public enum CacheType {
    PREVIOUS_CACHE,
    HISTORICAL_CACHE,
    NOT_A_CACHE
  }

  /**
   * Immutable selection metadata. Nullable fields are allowed only so a
   * caller can expose an explicit reason for a field that its route cannot
   * observe; the observer never substitutes a default value.
   */
  public static final class SelectionContext {
    private final String qSystem;
    private final Integer qState;
    private final String qAction;
    private final Integer requesterSlot;
    private final ZhangBoSubSwarm requesterRole;
    private final Integer candidateViewSize;
    private final Double eligibleBestDirectionalScore;
    private final Double selectedDirectionalScore;
    private final Double directionalRegret;
    private final String teacherSource;
    private final String cacheType;

    public SelectionContext(String qSystem, Integer qState, String qAction,
        Integer requesterSlot, ZhangBoSubSwarm requesterRole, Integer candidateViewSize,
        Double eligibleBestDirectionalScore, Double selectedDirectionalScore,
        Double directionalRegret, String teacherSource, String cacheType) {
      this.qSystem = qSystem;
      this.qState = qState;
      this.qAction = qAction;
      this.requesterSlot = requesterSlot;
      this.requesterRole = requesterRole;
      this.candidateViewSize = candidateViewSize;
      this.eligibleBestDirectionalScore = eligibleBestDirectionalScore;
      this.selectedDirectionalScore = selectedDirectionalScore;
      this.directionalRegret = directionalRegret;
      this.teacherSource = teacherSource;
      this.cacheType = cacheType;
    }

    public static SelectionContext observed(String qSystem, int qState, String qAction,
        int requesterSlot, ZhangBoSubSwarm requesterRole, int candidateViewSize,
        double eligibleBestDirectionalScore, double selectedDirectionalScore,
        double directionalRegret, String teacherSource, String cacheType) {
      return new SelectionContext(qSystem, Integer.valueOf(qState), qAction,
          Integer.valueOf(requesterSlot), requesterRole, Integer.valueOf(candidateViewSize),
          Double.valueOf(eligibleBestDirectionalScore), Double.valueOf(selectedDirectionalScore),
          Double.valueOf(directionalRegret), teacherSource, cacheType);
    }

    public static SelectionContext observed(String qSystem, int qState, String qAction,
        int requesterSlot, ZhangBoSubSwarm requesterRole, int candidateViewSize,
        double eligibleBestDirectionalScore, double selectedDirectionalScore,
        double directionalRegret, TeacherSource teacherSource, CacheType cacheType) {
      return observed(qSystem, qState, qAction, requesterSlot, requesterRole, candidateViewSize,
          eligibleBestDirectionalScore, selectedDirectionalScore, directionalRegret,
          teacherSource == null ? null : teacherSource.name(),
          cacheType == null ? null : cacheType.name());
    }

    private static SelectionContext legacy(String qSystem, ZhangBoSubSwarm requesterRole) {
      return new SelectionContext(qSystem, null, null, null, requesterRole, null,
          null, null, null, null, null);
    }

    public String getQSystem() { return qSystem; }
    public Integer getQState() { return qState; }
    public String getQAction() { return qAction; }
    public Integer getRequesterSlot() { return requesterSlot; }
    public ZhangBoSubSwarm getRequesterRole() { return requesterRole; }
    public Integer getCandidateViewSize() { return candidateViewSize; }
    public Double getEligibleBestDirectionalScore() { return eligibleBestDirectionalScore; }
    public Double getSelectedDirectionalScore() { return selectedDirectionalScore; }
    public Double getDirectionalRegret() { return directionalRegret; }
    public String getTeacherSource() { return teacherSource; }
    public String getCacheType() { return cacheType; }
  }

  /**
   * Compatibility bridge used by the current coordinator. The cycle is kept
   * as the legacy generation value because no separate generation is exposed
   * on this route. Missing metadata remains explicitly unobservable.
   */
  public void onTeacherUse(String teacherKind, ZhangBoSubSwarm requesterRole,
      PermutationSolution<Integer> teacher, long fe, int cycle) {
    recordTeacherSelection(SelectionContext.legacy(teacherKind, requesterRole), teacher,
        fe, cycle, cycle);
  }

  /** Records a complete selection event and returns its stable id. */
  public String recordTeacherSelection(SelectionContext context,
      PermutationSolution<Integer> teacher, long fe, int generation, int cycle) {
    if (!enabled) return "";
    try {
      if (context == null || teacher == null) {
        observerErrors++;
        return "";
      }
      String eventId = V35TeacherCaTaObservationSupport.eventId("Q-", ++eventSequence);
      TeacherEvent event = new TeacherEvent(eventId, context, teacher, fe, generation, cycle);
      events.add(event);
      eventById.put(eventId, event);
      return eventId;
    } catch (RuntimeException error) {
      observerErrors++;
      return "";
    }
  }

  /** Alias for callers that name the operation by its semantic event. */
  public String onTeacherSelection(SelectionContext context,
      PermutationSolution<Integer> teacher, long fe, int generation, int cycle) {
    return recordTeacherSelection(context, teacher, fe, generation, cycle);
  }

  /**
   * Full-argument convenience hook for a real Qg/Qp selector. All supplied
   * values are retained as-is after finite/range validation; no value is
   * derived from an enum ordinal or from a candidate fingerprint.
   */
  public String onTeacherSelection(String qSystem, Integer qState, String qAction,
      Integer requesterSlot, ZhangBoSubSwarm requesterRole, Integer candidateViewSize,
      Double eligibleBestDirectionalScore, Double selectedDirectionalScore,
      Double directionalRegret, String teacherSource, String cacheType,
      PermutationSolution<Integer> teacher, long fe, int generation, int cycle) {
    return recordTeacherSelection(new SelectionContext(qSystem, qState, qAction, requesterSlot,
        requesterRole, candidateViewSize, eligibleBestDirectionalScore,
        selectedDirectionalScore, directionalRegret, teacherSource, cacheType),
        teacher, fe, generation, cycle);
  }

  /** Backfills one real offspring result by immutable teacher event id. */
  public void onOffspringEvaluated(String eventId, PermutationSolution<Integer> offspring,
      boolean improved) {
    if (!enabled) return;
    TeacherEvent event = eventById.get(eventId);
    if (event == null) {
      observerErrors++;
      return;
    }
    if (event.offspringObserved) {
      observerErrors++;
      return;
    }
    if (offspring == null) {
      event.offspringFingerprint = status(
          V35TeacherCaTaObservationSupport.Status.UNOBSERVABLE_CALLER_DID_NOT_SUPPLY_OFFSPRING_LINK);
      event.offspringObjectives = event.offspringFingerprint;
      event.offspringImproved = event.offspringFingerprint;
      event.offspringReason = event.offspringFingerprint;
      return;
    }
    event.offspringFingerprint = V35TeacherCaTaObservationSupport.fingerprint(offspring);
    event.offspringObjectives = V35TeacherCaTaObservationSupport.objectives(offspring);
    event.offspringImproved = String.valueOf(improved);
    event.offspringReason = status(V35TeacherCaTaObservationSupport.Status.OBSERVED);
    event.offspringObserved = true;
  }

  /** Returns immutable event ids in creation order for hook composition. */
  public List<String> eventIds() {
    return Collections.unmodifiableList(new ArrayList<String>(eventById.keySet()));
  }

  public String eventsCsv() {
    StringBuilder out = new StringBuilder(
        "generatedByRunId,sourceJarSha256,configurationHash,instanceHash,seed,arm,telemetryMode,"
        + "eventId,generation,cycle,actualFE,qSystem,qSystemReason,scope,qState,qStateReason,"
        + "qAction,qActionReason,requesterSlot,requesterSlotReason,requesterRole,"
        + "requesterRoleReason,candidateViewSize,candidateViewSizeReason,"
        + "eligibleBestDirectionalScore,selectedDirectionalScore,directionalRegret,"
        + "directionalScoreReason,teacherSource,teacherSourceReason,cacheType,cacheTypeReason,"
        + "teacherFingerprint,teacherObjectives,offspringFingerprint,offspringObjectives,"
        + "offspringImproved,offspringReason\n");
    for (TeacherEvent event : events) out.append(event.toCsv()).append('\n');
    return out.toString();
  }

  /**
   * Aggregates six scopes independently. Cache/archive scopes are never
   * inferred from ALL_QG/ALL_QP or from a directional representative.
   */
  public String concentrationCsv() {
    StringBuilder out = new StringBuilder(
        "generatedByRunId,sourceJarSha256,configurationHash,instanceHash,seed,arm,telemetryMode,"
        + "scope,exposures,uniqueTeacherCount,top1Share,top5Share,shannonEntropy,"
        + "normalizedEntropy,cyclesObserved,exposuresPerCycle,closureStatus,closureReason\n");
    for (String scope : SCOPES) {
      ScopeStat stat = new ScopeStat();
      for (TeacherEvent event : events) {
        if (event.belongsTo(scope)) stat.add(event);
      }
      String closure = stat.exposures() == 0
          ? status(V35TeacherCaTaObservationSupport.Status.UNOBSERVABLE_NO_MATCHING_SCOPE_EVENT)
          : status(V35TeacherCaTaObservationSupport.Status.OBSERVED);
      out.append(V35TeacherCaTaObservationSupport.csv(runId)).append(',')
          .append(V35TeacherCaTaObservationSupport.csv(sourceJarSha256)).append(',')
          .append(V35TeacherCaTaObservationSupport.csv(configurationHash)).append(',')
          .append(V35TeacherCaTaObservationSupport.csv(instanceHash)).append(',')
          .append(seed).append(',').append(V35TeacherCaTaObservationSupport.csv(arm)).append(',')
          .append(telemetryMode).append(',').append(scope).append(',')
          .append(stat.exposures()).append(',').append(stat.uniqueTeacherCount()).append(',')
          .append(format(stat.top1Share())).append(',').append(format(stat.top5Share())).append(',')
          .append(format(stat.entropy())).append(',').append(format(stat.normalizedEntropy())).append(',')
          .append(stat.cycles.size()).append(',').append(format(stat.exposuresPerCycle())).append(',')
          .append(closure).append(',').append(closure).append('\n');
    }
    return out.toString();
  }

  public long getObserverErrors() { return observerErrors; }
  public int getRowCount() { return events.size(); }

  /**
   * Acceptance report for the real Qg/Qp hook. Required systems must have
   * observed metadata and a linked evaluated offspring for every event.
   */
  public ContractReport getContractReport(boolean qgRequired, boolean qpRequired) {
    boolean qgSeen = false;
    boolean qpSeen = false;
    boolean complete = true;
    for (TeacherEvent event : events) {
      if (ALL_QG.equals(event.scope)) qgSeen = true;
      if (ALL_QP.equals(event.scope)) qpSeen = true;
      if ((ALL_QG.equals(event.scope) && qgRequired)
          || (ALL_QP.equals(event.scope) && qpRequired)) {
        complete = complete && event.hasCompleteMetadata();
      }
    }
    return new ContractReport(!qgRequired || qgSeen, !qpRequired || qpSeen, complete,
        observerErrors == 0L);
  }

  public static final class ContractReport {
    private final boolean qgObserved;
    private final boolean qpObserved;
    private final boolean metadataAndOffspringComplete;
    private final boolean observerErrorsZero;

    private ContractReport(boolean qgObserved, boolean qpObserved,
        boolean metadataAndOffspringComplete, boolean observerErrorsZero) {
      this.qgObserved = qgObserved;
      this.qpObserved = qpObserved;
      this.metadataAndOffspringComplete = metadataAndOffspringComplete;
      this.observerErrorsZero = observerErrorsZero;
    }

    public boolean isQgObserved() { return qgObserved; }
    public boolean isQpObserved() { return qpObserved; }
    public boolean isMetadataAndOffspringComplete() { return metadataAndOffspringComplete; }
    public boolean isObserverErrorsZero() { return observerErrorsZero; }
    public boolean isPass() {
      return qgObserved && qpObserved && metadataAndOffspringComplete && observerErrorsZero;
    }
    public String toText() {
      return "qgObserved=" + qgObserved + ",qpObserved=" + qpObserved
          + ",metadataAndOffspringComplete=" + metadataAndOffspringComplete
          + ",observerErrorsZero=" + observerErrorsZero;
    }
  }

  private static String status(V35TeacherCaTaObservationSupport.Status value) {
    return value.name();
  }

  private static String format(double value) {
    return String.format(Locale.ROOT, "%.6f", value);
  }

  private static String text(Integer value,
      V35TeacherCaTaObservationSupport.Status missing) {
    return value == null ? status(missing) : String.valueOf(value);
  }

  private static String text(String value,
      V35TeacherCaTaObservationSupport.Status missing) {
    return value == null || value.length() == 0 ? status(missing) : value;
  }

  private static String numeric(Double value,
      V35TeacherCaTaObservationSupport.Status missing) {
    return V35TeacherCaTaObservationSupport.isFinite(value)
        ? V35TeacherCaTaObservationSupport.number(value) : status(missing);
  }

  private static boolean knownSource(String value) {
    return PREVIOUS_CACHE.equals(value) || HISTORICAL_CACHE.equals(value)
        || PERSONAL_ARCHIVE.equals(value) || DIRECTIONAL_REPRESENTATIVE.equals(value)
        || GLOBAL_ARCHIVE.equals(value);
  }

  private static boolean knownCache(String value) {
    return PREVIOUS_CACHE.equals(value) || HISTORICAL_CACHE.equals(value)
        || "NOT_A_CACHE".equals(value);
  }

  private final class TeacherEvent {
    private final String eventId;
    private final int generation;
    private final int cycle;
    private final long actualFe;
    private final String qSystem;
    private final String qSystemReason;
    private final String scope;
    private final String qState;
    private final String qStateReason;
    private final String qAction;
    private final String qActionReason;
    private final String requesterSlot;
    private final String requesterSlotReason;
    private final String requesterRole;
    private final String requesterRoleReason;
    private final String candidateViewSize;
    private final String candidateViewSizeReason;
    private final String eligibleBestDirectionalScore;
    private final String selectedDirectionalScore;
    private final String directionalRegret;
    private final String directionalScoreReason;
    private final String teacherSource;
    private final String teacherSourceReason;
    private final String cacheType;
    private final String cacheTypeReason;
    private final String teacherFingerprint;
    private final String teacherObjectives;
    private String offspringFingerprint;
    private String offspringObjectives;
    private String offspringImproved;
    private String offspringReason;
    private boolean offspringObserved;

    private TeacherEvent(String eventId, SelectionContext context,
        PermutationSolution<Integer> teacher, long actualFe, int generation, int cycle) {
      this.eventId = eventId;
      this.generation = generation;
      this.cycle = cycle;
      this.actualFe = actualFe;
      String rawQSystem = context.getQSystem();
      boolean qg = "QG".equals(rawQSystem);
      boolean qp = "QP".equals(rawQSystem);
      this.qSystem = (qg || qp) ? rawQSystem
          : status(V35TeacherCaTaObservationSupport.Status.UNOBSERVABLE_INVALID_Q_SYSTEM);
      this.qSystemReason = (qg || qp) ? status(V35TeacherCaTaObservationSupport.Status.OBSERVED)
          : status(V35TeacherCaTaObservationSupport.Status.UNOBSERVABLE_INVALID_Q_SYSTEM);
      this.scope = qg ? ALL_QG : qp ? ALL_QP
          : status(V35TeacherCaTaObservationSupport.Status.UNOBSERVABLE_INVALID_Q_SYSTEM);

      this.qState = text(context.getQState(),
          V35TeacherCaTaObservationSupport.Status.UNOBSERVABLE_CALLER_DID_NOT_SUPPLY_Q_STATE);
      this.qStateReason = context.getQState() == null
          ? status(V35TeacherCaTaObservationSupport.Status.UNOBSERVABLE_CALLER_DID_NOT_SUPPLY_Q_STATE)
          : status(V35TeacherCaTaObservationSupport.Status.OBSERVED);
      this.qAction = text(context.getQAction(),
          V35TeacherCaTaObservationSupport.Status.UNOBSERVABLE_CALLER_DID_NOT_SUPPLY_Q_ACTION);
      this.qActionReason = context.getQAction() == null
          ? status(V35TeacherCaTaObservationSupport.Status.UNOBSERVABLE_CALLER_DID_NOT_SUPPLY_Q_ACTION)
          : status(V35TeacherCaTaObservationSupport.Status.OBSERVED);
      this.requesterSlot = text(context.getRequesterSlot(),
          V35TeacherCaTaObservationSupport.Status.UNOBSERVABLE_CALLER_DID_NOT_SUPPLY_REQUESTER_SLOT);
      this.requesterSlotReason = context.getRequesterSlot() == null
          ? status(V35TeacherCaTaObservationSupport.Status.UNOBSERVABLE_CALLER_DID_NOT_SUPPLY_REQUESTER_SLOT)
          : status(V35TeacherCaTaObservationSupport.Status.OBSERVED);
      this.requesterRole = context.getRequesterRole() == null
          ? status(V35TeacherCaTaObservationSupport.Status.UNOBSERVABLE_CALLER_DID_NOT_SUPPLY_REQUESTER_ROLE)
          : context.getRequesterRole().name();
      this.requesterRoleReason = context.getRequesterRole() == null
          ? status(V35TeacherCaTaObservationSupport.Status.UNOBSERVABLE_CALLER_DID_NOT_SUPPLY_REQUESTER_ROLE)
          : status(V35TeacherCaTaObservationSupport.Status.OBSERVED);
      this.candidateViewSize = text(context.getCandidateViewSize(),
          V35TeacherCaTaObservationSupport.Status.UNOBSERVABLE_CALLER_DID_NOT_SUPPLY_CANDIDATE_VIEW);
      this.candidateViewSizeReason = context.getCandidateViewSize() == null
          ? status(V35TeacherCaTaObservationSupport.Status.UNOBSERVABLE_CALLER_DID_NOT_SUPPLY_CANDIDATE_VIEW)
          : status(V35TeacherCaTaObservationSupport.Status.OBSERVED);
      this.eligibleBestDirectionalScore = numeric(context.getEligibleBestDirectionalScore(),
          V35TeacherCaTaObservationSupport.Status.UNOBSERVABLE_CALLER_DID_NOT_SUPPLY_DIRECTIONAL_SCORES);
      this.selectedDirectionalScore = numeric(context.getSelectedDirectionalScore(),
          V35TeacherCaTaObservationSupport.Status.UNOBSERVABLE_CALLER_DID_NOT_SUPPLY_DIRECTIONAL_SCORES);
      this.directionalRegret = numeric(context.getDirectionalRegret(),
          V35TeacherCaTaObservationSupport.Status.UNOBSERVABLE_CALLER_DID_NOT_SUPPLY_DIRECTIONAL_SCORES);
      this.directionalScoreReason = context.getEligibleBestDirectionalScore() == null
          || context.getSelectedDirectionalScore() == null || context.getDirectionalRegret() == null
          ? status(V35TeacherCaTaObservationSupport.Status.UNOBSERVABLE_CALLER_DID_NOT_SUPPLY_DIRECTIONAL_SCORES)
          : status(V35TeacherCaTaObservationSupport.Status.OBSERVED);

      String rawSource = context.getTeacherSource();
      this.teacherSource = knownSource(rawSource) ? rawSource
          : status(rawSource == null
              ? V35TeacherCaTaObservationSupport.Status.UNOBSERVABLE_CALLER_DID_NOT_SUPPLY_TEACHER_SOURCE
              : V35TeacherCaTaObservationSupport.Status.UNOBSERVABLE_INVALID_TEACHER_SOURCE);
      this.teacherSourceReason = knownSource(rawSource) ? status(
          V35TeacherCaTaObservationSupport.Status.OBSERVED) : this.teacherSource;
      String rawCache = context.getCacheType();
      this.cacheType = knownCache(rawCache) ? rawCache
          : status(rawCache == null
              ? V35TeacherCaTaObservationSupport.Status.UNOBSERVABLE_CALLER_DID_NOT_SUPPLY_CACHE_TYPE
              : V35TeacherCaTaObservationSupport.Status.UNOBSERVABLE_INVALID_CACHE_TYPE);
      this.cacheTypeReason = knownCache(rawCache) ? status(
          V35TeacherCaTaObservationSupport.Status.OBSERVED) : this.cacheType;
      this.teacherFingerprint = V35TeacherCaTaObservationSupport.fingerprint(teacher);
      this.teacherObjectives = V35TeacherCaTaObservationSupport.objectives(teacher);
      this.offspringFingerprint = status(
          V35TeacherCaTaObservationSupport.Status.UNOBSERVABLE_CALLER_DID_NOT_SUPPLY_OFFSPRING_LINK);
      this.offspringObjectives = this.offspringFingerprint;
      this.offspringImproved = this.offspringFingerprint;
      this.offspringReason = this.offspringFingerprint;
      this.offspringObserved = false;
    }

    private boolean belongsTo(String requestedScope) {
      if (requestedScope.equals(scope)) return true;
      return (PREVIOUS_CACHE.equals(requestedScope) && PREVIOUS_CACHE.equals(teacherSource))
          || (HISTORICAL_CACHE.equals(requestedScope) && HISTORICAL_CACHE.equals(teacherSource))
          || (PERSONAL_ARCHIVE.equals(requestedScope) && PERSONAL_ARCHIVE.equals(teacherSource))
          || (DIRECTIONAL_REPRESENTATIVE.equals(requestedScope)
              && DIRECTIONAL_REPRESENTATIVE.equals(teacherSource));
    }

    private boolean hasCompleteMetadata() {
      return !startsUnobservable(qSystem) && !startsUnobservable(qState)
          && !startsUnobservable(qAction) && !startsUnobservable(requesterSlot)
          && !startsUnobservable(requesterRole) && !startsUnobservable(candidateViewSize)
          && !startsUnobservable(eligibleBestDirectionalScore)
          && !startsUnobservable(selectedDirectionalScore)
          && !startsUnobservable(directionalRegret) && !startsUnobservable(teacherSource)
          && !startsUnobservable(cacheType) && !startsUnobservable(teacherFingerprint)
          && !startsUnobservable(teacherObjectives) && offspringObserved
          && !startsUnobservable(offspringFingerprint)
          && !startsUnobservable(offspringObjectives)
          && !startsUnobservable(offspringImproved);
    }

    private boolean startsUnobservable(String value) {
      return value == null || value.startsWith("UNOBSERVABLE_");
    }

    private String toCsv() {
      StringBuilder out = new StringBuilder();
      append(out, runId);
      append(out, sourceJarSha256);
      append(out, configurationHash);
      append(out, instanceHash);
      append(out, seed);
      append(out, arm);
      append(out, telemetryMode);
      append(out, eventId);
      append(out, generation);
      append(out, cycle);
      append(out, actualFe);
      append(out, qSystem);
      append(out, qSystemReason);
      append(out, scope);
      append(out, qState);
      append(out, qStateReason);
      append(out, qAction);
      append(out, qActionReason);
      append(out, requesterSlot);
      append(out, requesterSlotReason);
      append(out, requesterRole);
      append(out, requesterRoleReason);
      append(out, candidateViewSize);
      append(out, candidateViewSizeReason);
      append(out, eligibleBestDirectionalScore);
      append(out, selectedDirectionalScore);
      append(out, directionalRegret);
      append(out, directionalScoreReason);
      append(out, teacherSource);
      append(out, teacherSourceReason);
      append(out, cacheType);
      append(out, cacheTypeReason);
      append(out, teacherFingerprint);
      append(out, teacherObjectives);
      append(out, offspringFingerprint);
      append(out, offspringObjectives);
      append(out, offspringImproved);
      append(out, offspringReason);
      return out.toString();
    }
  }

  private final class ScopeStat {
    private final List<String> fingerprints = new ArrayList<String>();
    private final Set<Integer> cycles = new HashSet<Integer>();
    private final Map<String, Integer> counts = new HashMap<String, Integer>();

    private void add(TeacherEvent event) {
      fingerprints.add(event.teacherFingerprint);
      cycles.add(event.cycle);
      Integer count = counts.get(event.teacherFingerprint);
      counts.put(event.teacherFingerprint, count == null ? 1 : count + 1);
    }

    private int exposures() { return fingerprints.size(); }
    private int uniqueTeacherCount() { return counts.size(); }

    private double top1Share() {
      if (exposures() == 0) return 0.0;
      int best = 0;
      for (Integer count : counts.values()) best = Math.max(best, count);
      return (double) best / exposures();
    }

    private double top5Share() {
      if (exposures() == 0) return 0.0;
      List<Integer> values = new ArrayList<Integer>(counts.values());
      Collections.sort(values, Collections.reverseOrder());
      int total = 0;
      for (int index = 0; index < Math.min(5, values.size()); index++) total += values.get(index);
      return (double) total / exposures();
    }

    private double entropy() {
      if (exposures() == 0) return 0.0;
      double value = 0.0;
      for (Integer count : counts.values()) {
        double probability = (double) count / exposures();
        value -= probability * Math.log(probability);
      }
      return value;
    }

    private double normalizedEntropy() {
      return uniqueTeacherCount() <= 1 ? 0.0 : entropy() / Math.log(uniqueTeacherCount());
    }

    private double exposuresPerCycle() {
      return cycles.isEmpty() ? 0.0 : (double) exposures() / cycles.size();
    }
  }

  private static void append(StringBuilder out, Object value) {
    if (out.length() > 0) out.append(',');
    out.append(V35TeacherCaTaObservationSupport.csv(value));
  }
}
