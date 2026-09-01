package org.uma.jmetal.algorithm.multiobjective.mypso.v35;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.uma.jmetal.algorithm.multiobjective.mypso.zhangbo.ZhangBoEvaluatedPddrSelector;
import org.uma.jmetal.algorithm.multiobjective.mypso.zhangbo.ZhangBoSubSwarm;
import org.uma.jmetal.solution.PermutationSolution;

/**
 * Agent-C CA-TA candidate lifecycle observer.
 *
 * <p>Every candidate has one immutable event id. Lifecycle callbacks update
 * only that event id; a fingerprint is used solely by the legacy PDDR bridge
 * to locate a candidate when the caller did not retain the event id. The
 * bridge never equates local acceptance with merge-pool entry, PDDR selection,
 * archive entry, or next-generation survival.</p>
 *
 * <p>This class is observation-only. It does not add candidates, call an
 * acceptance rule, consume random numbers, or alter a CA-TA budget.</p>
 */
public final class V35CaTaContributionObserver {
  public static final String VERSION = "V35_MIDHORIZON_V3_CATA";

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
  private final List<CandidateEvent> events = new ArrayList<CandidateEvent>();
  private final Map<String, CandidateEvent> eventById = new LinkedHashMap<String, CandidateEvent>();

  public V35CaTaContributionObserver(String runId, String sourceJarSha256,
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

  /** Archive side used by a real archive callback. */
  public enum ArchiveKind { PERSONAL, GLOBAL }

  /**
   * Starts a candidate at its actual generation point. The returned id is the
   * only supported key for later lifecycle backfills.
   */
  public String onCandidateGenerated(String testApply, V35MacroNeighborhood macro,
      ZhangBoSubSwarm group, String bottleneck,
      PermutationSolution<Integer> parent, PermutationSolution<Integer> candidate,
      long fe, int generation, int cycle) {
    if (!enabled) return "";
    try {
      if (candidate == null) {
        observerErrors++;
        return "";
      }
      String eventId = V35TeacherCaTaObservationSupport.eventId("CATA-", ++eventSequence);
      CandidateEvent event = new CandidateEvent(eventId, testApply, macro, group, bottleneck,
          parent, candidate, fe, generation, cycle);
      events.add(event);
      eventById.put(eventId, event);
      return eventId;
    } catch (RuntimeException error) {
      observerErrors++;
      return "";
    }
  }

  /** Records the actual full evaluation for a previously generated candidate. */
  public void onCandidateEvaluated(String eventId, PermutationSolution<Integer> candidate,
      long fe) {
    if (!enabled) return;
    CandidateEvent event = eventById.get(eventId);
    if (event == null) {
      observerErrors++;
      return;
    }
    if (candidate == null) {
      observerErrors++;
      return;
    }
    String fingerprint = V35TeacherCaTaObservationSupport.fingerprint(candidate);
    if (!event.candidateFingerprint.equals(fingerprint)) {
      observerErrors++;
      event.evaluated = status(
          V35TeacherCaTaObservationSupport.Status.UNOBSERVABLE_CANDIDATE_ID_MISMATCH);
      event.evaluatedReason = event.evaluated;
      return;
    }
    if (!isUnresolved(event.evaluated)) {
      observerErrors++;
      return;
    }
    event.evaluated = "true";
    event.evaluatedReason = status(V35TeacherCaTaObservationSupport.Status.OBSERVED);
    event.evaluatedFe = fe;
    event.objectives = V35TeacherCaTaObservationSupport.objectives(candidate);
    event.objectivesReason = status(V35TeacherCaTaObservationSupport.Status.OBSERVED);
  }

  /** Records the local acceptance result at its actual decision point. */
  public void onAcceptedLocally(String eventId, boolean accepted, String result) {
    if (!enabled) return;
    CandidateEvent event = eventById.get(eventId);
    if (event == null) {
      observerErrors++;
      return;
    }
    if (!isUnresolved(event.acceptedLocally)) {
      observerErrors++;
      return;
    }
    event.acceptedLocally = String.valueOf(accepted);
    event.acceptedLocallyReason = status(V35TeacherCaTaObservationSupport.Status.OBSERVED);
    event.result = result == null || result.length() == 0
        ? (accepted ? "ACCEPTED" : "REJECTED") : result;
    event.resultReason = status(V35TeacherCaTaObservationSupport.Status.OBSERVED);
  }

  /** Records whether a candidate really entered the PDDR merge pool. */
  public void onEnteredMergePool(String eventId, boolean entered) {
    if (!enabled) return;
    CandidateEvent event = eventById.get(eventId);
    if (event == null) {
      observerErrors++;
      return;
    }
    if (!isUnresolved(event.enteredMergePool)) {
      observerErrors++;
      return;
    }
    event.enteredMergePool = entered ? "true"
        : status(V35TeacherCaTaObservationSupport.Status.NOT_ENTERED_MERGE_POOL);
    event.enteredMergePoolReason = status(V35TeacherCaTaObservationSupport.Status.OBSERVED);
  }

  /** Records an actual PDDR outcome; false is retained as NOT_SELECTED. */
  public void onPddrDecision(String eventId, boolean selected) {
    if (!enabled) return;
    CandidateEvent event = eventById.get(eventId);
    if (event == null) {
      observerErrors++;
      return;
    }
    if (!isUnresolved(event.selectedByPddr)) {
      observerErrors++;
      return;
    }
    event.selectedByPddr = selected ? "true"
        : status(V35TeacherCaTaObservationSupport.Status.NOT_SELECTED);
    event.selectedByPddrReason = status(V35TeacherCaTaObservationSupport.Status.OBSERVED);
  }

  /** Records the actual result of a personal/global archive admission check. */
  public void onArchiveDecision(String eventId, ArchiveKind kind, boolean entered) {
    if (!enabled) return;
    CandidateEvent event = eventById.get(eventId);
    if (event == null || kind == null) {
      observerErrors++;
      return;
    }
    if (kind == ArchiveKind.PERSONAL) {
      if (!isUnresolved(event.enteredPersonalArchive)) {
        observerErrors++;
        return;
      }
      event.enteredPersonalArchive = entered ? "true"
          : status(V35TeacherCaTaObservationSupport.Status.NOT_ENTERED_ARCHIVE);
      event.enteredPersonalArchiveReason = status(
          V35TeacherCaTaObservationSupport.Status.OBSERVED);
    } else {
      if (!isUnresolved(event.enteredGlobalArchive)) {
        observerErrors++;
        return;
      }
      event.enteredGlobalArchive = entered ? "true"
          : status(V35TeacherCaTaObservationSupport.Status.NOT_ENTERED_ARCHIVE);
      event.enteredGlobalArchiveReason = status(
          V35TeacherCaTaObservationSupport.Status.OBSERVED);
    }
  }

  /** Records actual next-generation presence, without inferring it from PDDR. */
  public void onSurvivedNextGeneration(String eventId, boolean survived) {
    if (!enabled) return;
    CandidateEvent event = eventById.get(eventId);
    if (event == null) {
      observerErrors++;
      return;
    }
    if (!isUnresolved(event.survivedNextGeneration)) {
      observerErrors++;
      return;
    }
    event.survivedNextGeneration = String.valueOf(survived);
    event.survivedNextGenerationReason = status(
        V35TeacherCaTaObservationSupport.Status.OBSERVED);
  }

  /** Records later teacher use for this exact candidate event. */
  public void onTeacherUsedLater(String eventId, boolean used) {
    if (!enabled) return;
    CandidateEvent event = eventById.get(eventId);
    if (event == null) {
      observerErrors++;
      return;
    }
    if (!isUnresolved(event.teacherUsedLater)) {
      observerErrors++;
      return;
    }
    event.teacherUsedLater = String.valueOf(used);
    event.teacherUsedLaterReason = status(V35TeacherCaTaObservationSupport.Status.OBSERVED);
  }

  public void onTeacherUsedLater(String eventId) {
    onTeacherUsedLater(eventId, true);
  }

  /** Records whether a later offspring was improved by this candidate. */
  public void onImprovedOffspringLater(String eventId, boolean improved) {
    if (!enabled) return;
    CandidateEvent event = eventById.get(eventId);
    if (event == null) {
      observerErrors++;
      return;
    }
    if (!isUnresolved(event.improvedOffspringLater)) {
      observerErrors++;
      return;
    }
    event.improvedOffspringLater = String.valueOf(improved);
    event.improvedOffspringLaterReason = status(
        V35TeacherCaTaObservationSupport.Status.OBSERVED);
  }

  /**
   * Legacy post-evaluation hook. It records only what this hook really knows;
   * in particular, it does not set merge-pool or archive fields from accepted.
   */
  public void onCaTaCandidate(String testApply, V35MacroNeighborhood macro,
      ZhangBoSubSwarm group, String bottleneck,
      PermutationSolution<Integer> parent, PermutationSolution<Integer> candidate,
      boolean accepted, long fe, int cycle) {
    recordPostEvaluationCandidate(testApply, macro, group, bottleneck, parent, candidate,
        accepted, fe, cycle);
  }

  /** Same legacy event with the id returned for callers that need backfill. */
  public String recordPostEvaluationCandidate(String testApply, V35MacroNeighborhood macro,
      ZhangBoSubSwarm group, String bottleneck,
      PermutationSolution<Integer> parent, PermutationSolution<Integer> candidate,
      boolean accepted, long fe, int cycle) {
    String eventId = onCandidateGenerated(testApply, macro, group, bottleneck, parent, candidate,
        fe, cycle, cycle);
    if (eventId.length() == 0) return eventId;
    CandidateEvent event = eventById.get(eventId);
    // Existence is known, but the old hook runs after construction and cannot
    // provide the construction timestamp. Keep that limitation explicit.
    event.generatedReason = status(
        V35TeacherCaTaObservationSupport.Status.UNOBSERVABLE_NO_GENERATION_HOOK);
    onCandidateEvaluated(eventId, candidate, fe);
    onAcceptedLocally(eventId, accepted, accepted ? "ACCEPTED" : "REJECTED");
    return eventId;
  }

  /**
   * Compatibility bridge from the current coordinator. It receives selected
   * candidate ids only, so it can mark positive selections but cannot mark a
   * candidate as NOT_SELECTED without the complete PDDR pool.
   */
  public void onPddrSelected(Set<String> selectedFingerprints) {
    if (!enabled || selectedFingerprints == null || selectedFingerprints.isEmpty()) return;
    for (String eventFingerprint : selectedFingerprints) {
      for (CandidateEvent event : events) {
        if (sameFingerprint(event.candidateFingerprint, eventFingerprint)
            && isUnresolved(event.selectedByPddr)) {
          event.selectedByPddr = "true";
          event.selectedByPddrReason = status(
              V35TeacherCaTaObservationSupport.Status.OBSERVED);
        }
      }
    }
  }

  /**
   * Full PDDR hook. Pool membership is the only evidence used for
   * enteredMergePool; selectedByPddr is settled independently for every
   * CA-TA candidate in that pool.
   */
  public void onPddrRound(List<PermutationSolution<Integer>> pool,
      List<ZhangBoEvaluatedPddrSelector.Source> sources,
      List<ZhangBoEvaluatedPddrSelector.Candidate> selected,
      long fe, int cycle, int generation) {
    if (!enabled) return;
    if (pool == null || sources == null || selected == null || pool.size() != sources.size()) {
      observerErrors++;
      return;
    }
    Set<String> selectedFingerprints = new HashSet<String>();
    for (ZhangBoEvaluatedPddrSelector.Candidate value : selected) {
      if (value != null && value.getSolution() != null) {
        selectedFingerprints.add(V35TeacherCaTaObservationSupport.fingerprint(value.getSolution()));
      }
    }
    Set<String> settled = new HashSet<String>();
    for (PermutationSolution<Integer> value : pool) {
      if (value == null) continue;
      String fingerprint = V35TeacherCaTaObservationSupport.fingerprint(value);
      CandidateEvent event = matchByFingerprint(fingerprint, fe, settled);
      if (event == null) continue;
      settled.add(event.eventId);
      if (isUnresolved(event.enteredMergePool)) {
        event.enteredMergePool = "true";
        event.enteredMergePoolReason = status(
            V35TeacherCaTaObservationSupport.Status.OBSERVED);
      }
      if (isUnresolved(event.selectedByPddr)) {
        boolean selectedNow = selectedFingerprints.contains(fingerprint);
        event.selectedByPddr = selectedNow ? "true"
            : status(V35TeacherCaTaObservationSupport.Status.NOT_SELECTED);
        event.selectedByPddrReason = status(
            V35TeacherCaTaObservationSupport.Status.OBSERVED);
      }
    }
  }

  public void onPddrRound(List<PermutationSolution<Integer>> pool,
      List<ZhangBoEvaluatedPddrSelector.Source> sources,
      List<ZhangBoEvaluatedPddrSelector.Candidate> selected,
      long fe, int cycle) {
    onPddrRound(pool, sources, selected, fe, cycle, cycle);
  }

  /**
   * Finalizes unresolved lifecycle observations. Every unresolved field is
   * right-censored unless the caller explicitly says the CA-TA arm is not
   * applicable. No unresolved field is silently converted to false.
   */
  public void onRunEnd(boolean cataApplicable) {
    if (!enabled) return;
    for (CandidateEvent event : events) event.finish(cataApplicable);
  }

  public void onRunEnd() { onRunEnd(true); }

  public List<String> eventIds() {
    return Collections.unmodifiableList(new ArrayList<String>(eventById.keySet()));
  }

  public String eventsCsv() {
    StringBuilder out = new StringBuilder(
        "generatedByRunId,sourceJarSha256,configurationHash,instanceHash,seed,arm,telemetryMode,"
        + "eventId,generation,cycle,generatedFE,evaluatedFE,parentFingerprint,candidateFingerprint,"
        + "context,bottleneck,macroNeighborhood,TEST_APPLY,generated,generatedReason,evaluated,"
        + "evaluatedReason,objectives,objectivesReason,acceptedLocally,acceptedLocallyReason,result,"
        + "resultReason,enteredMergePool,enteredMergePoolReason,selectedByPddr,selectedByPddrReason,"
        + "enteredPersonalArchive,enteredPersonalArchiveReason,enteredGlobalArchive,"
        + "enteredGlobalArchiveReason,survivedNextGeneration,survivedNextGenerationReason,"
        + "teacherUsedLater,teacherUsedLaterReason,improvedOffspringLater,"
        + "improvedOffspringLaterReason\n");
    for (CandidateEvent event : events) out.append(event.toCsv()).append('\n');
    return out.toString();
  }

  /** Summary counts only literal true states and keeps unresolved counts visible. */
  public String summaryCsv() {
    StringBuilder out = new StringBuilder(
        "generatedByRunId,sourceJarSha256,configurationHash,instanceHash,seed,arm,telemetryMode,"
        + "window,macroNeighborhood,candidateCount,generated,evaluated,acceptedLocally,"
        + "enteredMergePool,selectedByPddr,enteredPersonalArchive,enteredGlobalArchive,"
        + "survivedNextGeneration,teacherUsedLater,improvedOffspringLater,rightCensoredFields,"
        + "unobservableFields,closureStatus\n");
    Map<String, List<CandidateEvent>> grouped = new HashMap<String, List<CandidateEvent>>();
    for (CandidateEvent event : events) {
      String key = window(event.generatedFe) + "|" + event.macroNeighborhood;
      List<CandidateEvent> values = grouped.get(key);
      if (values == null) {
        values = new ArrayList<CandidateEvent>();
        grouped.put(key, values);
      }
      values.add(event);
    }
    List<String> keys = new ArrayList<String>(grouped.keySet());
    Collections.sort(keys);
    for (String key : keys) {
      List<CandidateEvent> values = grouped.get(key);
      String[] split = key.split("\\|", 2);
      int rightCensored = 0;
      int unobservable = 0;
      for (CandidateEvent event : values) {
        rightCensored += event.rightCensoredFields();
        unobservable += event.unobservableFields();
      }
      out.append(V35TeacherCaTaObservationSupport.csv(runId)).append(',')
          .append(V35TeacherCaTaObservationSupport.csv(sourceJarSha256)).append(',')
          .append(V35TeacherCaTaObservationSupport.csv(configurationHash)).append(',')
          .append(V35TeacherCaTaObservationSupport.csv(instanceHash)).append(',')
          .append(seed).append(',').append(V35TeacherCaTaObservationSupport.csv(arm)).append(',')
          .append(telemetryMode).append(',').append(split[0]).append(',')
          .append(V35TeacherCaTaObservationSupport.csv(split[1])).append(',')
          .append(values.size()).append(',').append(count(values, "generated", "true")).append(',')
          .append(count(values, "evaluated", "true")).append(',')
          .append(count(values, "acceptedLocally", "true")).append(',')
          .append(count(values, "enteredMergePool", "true")).append(',')
          .append(count(values, "selectedByPddr", "true")).append(',')
          .append(count(values, "enteredPersonalArchive", "true")).append(',')
          .append(count(values, "enteredGlobalArchive", "true")).append(',')
          .append(count(values, "survivedNextGeneration", "true")).append(',')
          .append(count(values, "teacherUsedLater", "true")).append(',')
          .append(count(values, "improvedOffspringLater", "true")).append(',')
          .append(rightCensored).append(',').append(unobservable).append(',')
          .append(rightCensored == 0 && unobservable == 0 ? "CLOSED" : "INCOMPLETE")
          .append('\n');
    }
    return out.toString();
  }

  public long getObserverErrors() { return observerErrors; }
  public int getRowCount() { return events.size(); }

  public boolean hasUnobservableFields() {
    for (CandidateEvent event : events) {
      if (event.unobservableFields() > 0) return true;
    }
    return false;
  }

  private static boolean isUnresolved(String value) {
    return value != null && (value.startsWith("UNOBSERVABLE_")
        || value.equals(V35TeacherCaTaObservationSupport.Status.RIGHT_CENSORED_RUN_END.name()));
  }

  private static String window(long fe) {
    return "W" + (Math.max(0L, fe) / 25000L + 1L);
  }

  private static int count(List<CandidateEvent> values, String ignoredField, String expected) {
    int result = 0;
    for (CandidateEvent event : values) {
      String actual;
      if ("generated".equals(ignoredField)) actual = event.generated;
      else if ("evaluated".equals(ignoredField)) actual = event.evaluated;
      else if ("acceptedLocally".equals(ignoredField)) actual = event.acceptedLocally;
      else if ("enteredMergePool".equals(ignoredField)) actual = event.enteredMergePool;
      else if ("selectedByPddr".equals(ignoredField)) actual = event.selectedByPddr;
      else if ("enteredPersonalArchive".equals(ignoredField)) actual = event.enteredPersonalArchive;
      else if ("enteredGlobalArchive".equals(ignoredField)) actual = event.enteredGlobalArchive;
      else if ("survivedNextGeneration".equals(ignoredField)) actual = event.survivedNextGeneration;
      else if ("teacherUsedLater".equals(ignoredField)) actual = event.teacherUsedLater;
      else actual = event.improvedOffspringLater;
      if (expected.equals(actual)) result++;
    }
    return result;
  }

  private static boolean sameFingerprint(String actual, String supplied) {
    return actual.equals(supplied)
        || V35TeacherCaTaObservationSupport.transportFingerprint(actual).equals(supplied);
  }

  private CandidateEvent matchByFingerprint(String fingerprint, long fe, Set<String> settled) {
    CandidateEvent best = null;
    for (int index = events.size() - 1; index >= 0; index--) {
      CandidateEvent event = events.get(index);
      if (settled.contains(event.eventId) || !event.candidateFingerprint.equals(fingerprint)) continue;
      long eventFe = event.evaluatedFe < 0L ? event.generatedFe : event.evaluatedFe;
      if (eventFe <= fe && (best == null || eventFe > best.generatedFe)) best = event;
    }
    return best;
  }

  private final class CandidateEvent {
    private final String eventId;
    private final int generation;
    private final int cycle;
    private final long generatedFe;
    private long evaluatedFe = -1L;
    private final String parentFingerprint;
    private final String candidateFingerprint;
    private final String context;
    private final String bottleneck;
    private final String macroNeighborhood;
    private final String testApply;
    private String generated = "true";
    private String generatedReason;
    private String evaluated;
    private String evaluatedReason;
    private String objectives;
    private String objectivesReason;
    private String acceptedLocally;
    private String acceptedLocallyReason;
    private String result;
    private String resultReason;
    private String enteredMergePool;
    private String enteredMergePoolReason;
    private String selectedByPddr;
    private String selectedByPddrReason;
    private String enteredPersonalArchive;
    private String enteredPersonalArchiveReason;
    private String enteredGlobalArchive;
    private String enteredGlobalArchiveReason;
    private String survivedNextGeneration;
    private String survivedNextGenerationReason;
    private String teacherUsedLater;
    private String teacherUsedLaterReason;
    private String improvedOffspringLater;
    private String improvedOffspringLaterReason;

    private CandidateEvent(String eventId, String testApply, V35MacroNeighborhood macro,
        ZhangBoSubSwarm group, String bottleneck, PermutationSolution<Integer> parent,
        PermutationSolution<Integer> candidate, long generatedFe, int generation, int cycle) {
      this.eventId = eventId;
      this.testApply = testApply == null ? "UNOBSERVABLE_NO_TEST_APPLY_VALUE" : testApply;
      this.macroNeighborhood = macro == null ? "UNOBSERVABLE_NO_MACRO_VALUE" : macro.name();
      this.context = group == null ? "UNOBSERVABLE_NO_REQUESTER_ROLE" : group.name();
      this.bottleneck = bottleneck == null ? "UNOBSERVABLE_NO_BOTTLENECK_VALUE" : bottleneck;
      this.parentFingerprint = parent == null
          ? status(V35TeacherCaTaObservationSupport.Status.UNOBSERVABLE_CALLER_DID_NOT_SUPPLY_PARENT)
          : V35TeacherCaTaObservationSupport.fingerprint(parent);
      this.candidateFingerprint = V35TeacherCaTaObservationSupport.fingerprint(candidate);
      this.generatedFe = generatedFe;
      this.generation = generation;
      this.cycle = cycle;
      this.generatedReason = status(V35TeacherCaTaObservationSupport.Status.OBSERVED);
      this.evaluated = status(V35TeacherCaTaObservationSupport.Status.UNOBSERVABLE_NO_EVALUATION_HOOK);
      this.evaluatedReason = this.evaluated;
      this.objectives = this.evaluated;
      this.objectivesReason = this.evaluated;
      this.acceptedLocally = status(
          V35TeacherCaTaObservationSupport.Status.UNOBSERVABLE_NO_LOCAL_ACCEPTANCE_HOOK);
      this.acceptedLocallyReason = this.acceptedLocally;
      this.result = this.acceptedLocally;
      this.resultReason = this.result;
      this.enteredMergePool = status(
          V35TeacherCaTaObservationSupport.Status.UNOBSERVABLE_NO_MERGE_POOL_HOOK);
      this.enteredMergePoolReason = this.enteredMergePool;
      this.selectedByPddr = status(
          V35TeacherCaTaObservationSupport.Status.UNOBSERVABLE_NO_PDDR_ROUND_POOL);
      this.selectedByPddrReason = this.selectedByPddr;
      this.enteredPersonalArchive = status(
          V35TeacherCaTaObservationSupport.Status.UNOBSERVABLE_NO_PERSONAL_ARCHIVE_HOOK);
      this.enteredPersonalArchiveReason = this.enteredPersonalArchive;
      this.enteredGlobalArchive = status(
          V35TeacherCaTaObservationSupport.Status.UNOBSERVABLE_NO_GLOBAL_ARCHIVE_HOOK);
      this.enteredGlobalArchiveReason = this.enteredGlobalArchive;
      this.survivedNextGeneration = status(
          V35TeacherCaTaObservationSupport.Status.UNOBSERVABLE_NO_NEXT_GENERATION_HOOK);
      this.survivedNextGenerationReason = this.survivedNextGeneration;
      this.teacherUsedLater = status(
          V35TeacherCaTaObservationSupport.Status.UNOBSERVABLE_NO_LATER_TEACHER_HOOK);
      this.teacherUsedLaterReason = this.teacherUsedLater;
      this.improvedOffspringLater = status(
          V35TeacherCaTaObservationSupport.Status.UNOBSERVABLE_NO_LATER_OFFSPRING_HOOK);
      this.improvedOffspringLaterReason = this.improvedOffspringLater;
    }

    private void finish(boolean cataApplicable) {
      String terminal = cataApplicable
          ? status(V35TeacherCaTaObservationSupport.Status.RIGHT_CENSORED_RUN_END)
          : status(V35TeacherCaTaObservationSupport.Status.NOT_APPLICABLE_BY_ARM);
      if (isUnresolved(generated)) generated = terminal;
      if (isUnresolved(evaluated)) evaluated = terminal;
      if (isUnresolved(objectives)) objectives = terminal;
      if (isUnresolved(acceptedLocally)) acceptedLocally = terminal;
      if (isUnresolved(result)) result = terminal;
      if (isUnresolved(enteredMergePool)) enteredMergePool = terminal;
      if (isUnresolved(selectedByPddr)) selectedByPddr = terminal;
      if (isUnresolved(enteredPersonalArchive)) enteredPersonalArchive = terminal;
      if (isUnresolved(enteredGlobalArchive)) enteredGlobalArchive = terminal;
      if (isUnresolved(survivedNextGeneration)) survivedNextGeneration = terminal;
      if (isUnresolved(teacherUsedLater)) teacherUsedLater = terminal;
      if (isUnresolved(improvedOffspringLater)) improvedOffspringLater = terminal;
      if (isUnresolved(generatedReason)) generatedReason = terminal;
      if (isUnresolved(evaluatedReason)) evaluatedReason = terminal;
      if (isUnresolved(objectivesReason)) objectivesReason = terminal;
      if (isUnresolved(acceptedLocallyReason)) acceptedLocallyReason = terminal;
      if (isUnresolved(resultReason)) resultReason = terminal;
      if (isUnresolved(enteredMergePoolReason)) enteredMergePoolReason = terminal;
      if (isUnresolved(selectedByPddrReason)) selectedByPddrReason = terminal;
      if (isUnresolved(enteredPersonalArchiveReason)) enteredPersonalArchiveReason = terminal;
      if (isUnresolved(enteredGlobalArchiveReason)) enteredGlobalArchiveReason = terminal;
      if (isUnresolved(survivedNextGenerationReason)) survivedNextGenerationReason = terminal;
      if (isUnresolved(teacherUsedLaterReason)) teacherUsedLaterReason = terminal;
      if (isUnresolved(improvedOffspringLaterReason)) improvedOffspringLaterReason = terminal;
    }

    private int rightCensoredFields() {
      int result = 0;
      for (String value : lifecycleValues()) {
        if (status(V35TeacherCaTaObservationSupport.Status.RIGHT_CENSORED_RUN_END).equals(value)) {
          result++;
        }
      }
      return result;
    }

    private int unobservableFields() {
      int result = 0;
      for (String value : lifecycleValues()) if (value.startsWith("UNOBSERVABLE_")) result++;
      return result;
    }

    private List<String> lifecycleValues() {
      List<String> values = new ArrayList<String>();
      values.add(generated);
      values.add(evaluated);
      values.add(acceptedLocally);
      values.add(enteredMergePool);
      values.add(selectedByPddr);
      values.add(enteredPersonalArchive);
      values.add(enteredGlobalArchive);
      values.add(survivedNextGeneration);
      values.add(teacherUsedLater);
      values.add(improvedOffspringLater);
      return values;
    }

    private String toCsv() {
      StringBuilder out = new StringBuilder();
      append(out, runId); append(out, sourceJarSha256); append(out, configurationHash);
      append(out, instanceHash); append(out, seed); append(out, arm); append(out, telemetryMode);
      append(out, eventId); append(out, generation); append(out, cycle); append(out, generatedFe);
      append(out, evaluatedFe < 0L ? "UNOBSERVABLE_NO_EVALUATION_HOOK" : evaluatedFe);
      append(out, parentFingerprint); append(out, candidateFingerprint); append(out, context);
      append(out, bottleneck); append(out, macroNeighborhood); append(out, testApply);
      append(out, generated); append(out, generatedReason); append(out, evaluated);
      append(out, evaluatedReason); append(out, objectives); append(out, objectivesReason);
      append(out, acceptedLocally); append(out, acceptedLocallyReason); append(out, result);
      append(out, resultReason); append(out, enteredMergePool); append(out, enteredMergePoolReason);
      append(out, selectedByPddr); append(out, selectedByPddrReason);
      append(out, enteredPersonalArchive); append(out, enteredPersonalArchiveReason);
      append(out, enteredGlobalArchive); append(out, enteredGlobalArchiveReason);
      append(out, survivedNextGeneration); append(out, survivedNextGenerationReason);
      append(out, teacherUsedLater); append(out, teacherUsedLaterReason);
      append(out, improvedOffspringLater); append(out, improvedOffspringLaterReason);
      return out.toString();
    }
  }

  private static String status(V35TeacherCaTaObservationSupport.Status value) {
    return value.name();
  }

  private static void append(StringBuilder out, Object value) {
    if (out.length() > 0) out.append(',');
    out.append(V35TeacherCaTaObservationSupport.csv(value));
  }
}
