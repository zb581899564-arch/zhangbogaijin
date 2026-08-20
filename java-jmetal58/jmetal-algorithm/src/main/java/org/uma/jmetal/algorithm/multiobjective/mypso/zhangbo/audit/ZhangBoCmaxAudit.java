package org.uma.jmetal.algorithm.multiobjective.mypso.zhangbo.audit;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import org.uma.jmetal.algorithm.multiobjective.mypso.zhangbo.ZhangBoArchiveEntry;
import org.uma.jmetal.algorithm.multiobjective.mypso.zhangbo.ZhangBoEvaluatedPddrSelector;
import org.uma.jmetal.algorithm.multiobjective.mypso.zhangbo.ZhangBoLineageMemory;
import org.uma.jmetal.algorithm.multiobjective.mypso.zhangbo.ZhangBoLineageTag;
import org.uma.jmetal.algorithm.multiobjective.mypso.zhangbo.ZhangBoQgController;
import org.uma.jmetal.algorithm.multiobjective.mypso.zhangbo.ZhangBoSubSwarm;
import org.uma.jmetal.solution.PermutationSolution;

/**
 * Opt-in, read-only Cmax lifecycle audit. It never copies a solution, consumes randomness,
 * evaluates a candidate, or participates in an algorithm decision.
 */
public final class ZhangBoCmaxAudit {
  public static final String VERSION = "cmax-audit-v4-v35-lifecycle-three-objective";
  private static final double EPSILON = 1.0e-9;

  public enum Mechanism {
    INITIAL, CFVF, BASELINE_GLOBAL, FIXED_VNS, CA_TA, INTER_FACTORY, N1_N5, CA_TA_LITE
  }

  public enum Operator {
    INITIAL, CFVF, BASELINE_GLOBAL, O1_O9, O10, O11, O12, O13, N1, N2, N3, N4, N5,
    INTER_FACTORY_EXCHANGE, INTER_FACTORY_INSERTION
  }

  public enum Survival {
    PENDING, YES, NO, NOT_SELECTED
  }

  private final long checkpointInterval;
  private final List<Record> records = new ArrayList<>();
  private final Map<Long, Record> recordsByEvaluation = new HashMap<>();
  private final Map<String, Record> recordsByFingerprint = new HashMap<>();
  private final List<Checkpoint> checkpoints = new ArrayList<>();
  private final List<Record> selectedAwaitingNextRound = new ArrayList<>();
  private long nextCheckpoint;
  private double recordCmax = Double.POSITIVE_INFINITY;
  private double bestGenerated = Double.POSITIVE_INFINITY;
  private double bestGeneratedG1 = Double.POSITIVE_INFINITY;
  private double bestSurvived = Double.POSITIVE_INFINITY;
  private double bestG1 = Double.POSITIVE_INFINITY;
  private double currentGlobal = Double.NaN;
  private double currentG1 = Double.NaN;
  private double windowGenerated = Double.POSITIVE_INFINITY;
  private double windowSurvived = Double.POSITIVE_INFINITY;
  private long latestFe;
  private long resolvedPendingByFinish;
  // V35-P18 three-objective best-ever: per-objective minima over every observed
  // evaluated candidate plus the fingerprint of the solution achieving each one.
  // The three minima are independent scalars and are never materialized as one
  // concatenated pseudo-solution.
  private double bestTEC = Double.POSITIVE_INFINITY;
  private double bestTWC = Double.POSITIVE_INFINITY;
  private double currentTECGlobal = Double.NaN;
  private double currentTWCGlobal = Double.NaN;
  private String bestCmaxSource = "";
  private String bestTECSource = "";
  private String bestTWCSource = "";

  public ZhangBoCmaxAudit(long checkpointInterval) {
    if (checkpointInterval < 1L) {
      throw new IllegalArgumentException("checkpointInterval must be positive");
    }
    this.checkpointInterval = checkpointInterval;
    this.nextCheckpoint = checkpointInterval;
  }

  public void observeGenerated(long evaluationOrdinal, int generation,
      PermutationSolution<Integer> solution, ZhangBoSubSwarm group,
      Mechanism mechanism, Operator operator, boolean enteredCandidateSet) {
    observeGenerated(evaluationOrdinal, generation, solution, group, mechanism, operator,
        "NOT_APPLICABLE", enteredCandidateSet);
  }

  /** Records a newly evaluated candidate with an explicit parent identity. */
  public void observeGenerated(long evaluationOrdinal, int generation,
      PermutationSolution<Integer> solution, ZhangBoSubSwarm group,
      Mechanism mechanism, Operator operator, String parentId,
      boolean enteredCandidateSet) {
    requireSolution(solution);
    if (evaluationOrdinal < 1L || generation < 0 || mechanism == null || operator == null
        || parentId == null || parentId.isEmpty()) {
      throw new IllegalArgumentException("Invalid generated candidate audit event");
    }
    latestFe = Math.max(latestFe, evaluationOrdinal);
    double cmax = solution.getObjective(0);
    double tec = solution.getObjective(1);
    double twc = solution.getObjective(6);
    if (cmax < bestGenerated) {
      bestGenerated = cmax;
      bestCmaxSource = ZhangBoQgController.fingerprint(solution);
    }
    if (tec < bestTEC) {
      bestTEC = tec;
      bestTECSource = ZhangBoQgController.fingerprint(solution);
    }
    if (twc < bestTWC) {
      bestTWC = twc;
      bestTWCSource = ZhangBoQgController.fingerprint(solution);
    }
    windowGenerated = Math.min(windowGenerated, cmax);
    if (group == ZhangBoSubSwarm.G1_CMAX) {
      bestGeneratedG1 = Math.min(bestGeneratedG1, cmax);
    }
    if (cmax < recordCmax - EPSILON) {
      recordCmax = cmax;
      Record record = new Record(records.size() + 1L, evaluationOrdinal, generation,
          ZhangBoQgController.fingerprint(solution), cmax, solution.getObjective(1),
          solution.getObjective(6), group, mechanism, operator, enteredCandidateSet,
          parentId, lineageId(solution));
      records.add(record);
      recordsByEvaluation.put(evaluationOrdinal, record);
      recordsByFingerprint.put(record.fingerprint, record);
    }
    emitDueCheckpoints(evaluationOrdinal);
  }

  /** Records leaders that actually enter a CFVF update; eligibility alone is not counted. */
  public void observeTeacherUse(long fe, int generation, ZhangBoSubSwarm group,
      PermutationSolution<Integer> personalLeader,
      PermutationSolution<Integer> socialLeader) {
    if (fe < 0L || generation < 0 || group == null
        || personalLeader == null || socialLeader == null) {
      throw new IllegalArgumentException("Invalid Cmax teacher-use event");
    }
    Record personal = recordsByFingerprint.get(ZhangBoQgController.fingerprint(personalLeader));
    Record social = recordsByFingerprint.get(ZhangBoQgController.fingerprint(socialLeader));
    if (group == ZhangBoSubSwarm.G1_CMAX) {
      if (personal != null) personal.observeG1PersonalTeacher(fe, generation);
      if (social != null) social.observeG1SocialTeacher(fe, generation);
    }
  }

  public void observeInitialPopulation(long firstEvaluationOrdinal,
      List<PermutationSolution<Integer>> population) {
    if (population == null || population.isEmpty()) return;
    for (int index = 0; index < population.size(); index++) {
      observeGenerated(firstEvaluationOrdinal + index, 0, population.get(index), null,
        Mechanism.INITIAL, Operator.INITIAL, "INITIAL", true);
    }
    refreshState(firstEvaluationOrdinal + population.size() - 1L, population, population);
  }

  public void observePddrSelection(
      List<ZhangBoEvaluatedPddrSelector.Candidate> selected, int generation) {
    if (selected == null) return;
    Set<String> selectedFingerprints = new HashSet<>();
    for (ZhangBoEvaluatedPddrSelector.Candidate candidate : selected) {
      PermutationSolution<Integer> solution = candidate.getSolution();
      String fingerprint = ZhangBoQgController.fingerprint(solution);
      selectedFingerprints.add(fingerprint);
      // Evaluation ordinals are the primary key.  A copied/pre-evaluated
      // candidate can legitimately carry a different ordinal, so fall back to
      // its stable four-vector fingerprint before declaring the lifecycle
      // field unavailable.
      Record record = recordsByEvaluation.get(candidate.getEvaluationOrdinal());
      if (record == null) record = recordsByFingerprint.get(fingerprint);
      if (record != null) {
        record.pddrRetained = true;
        record.pddrGeneration = generation;
        record.survival = Survival.PENDING;
        bestSurvived = Math.min(bestSurvived, record.cmax);
        windowSurvived = Math.min(windowSurvived, record.cmax);
      }
    }
    for (Record prior : selectedAwaitingNextRound) {
      prior.survival = selectedFingerprints.contains(prior.fingerprint)
          ? Survival.YES : Survival.NO;
    }
    selectedAwaitingNextRound.clear();
    for (Record record : records) {
      if (record.pddrRetained && record.pddrGeneration == generation) {
        selectedAwaitingNextRound.add(record);
      }
    }
  }

  public void observeLineageArchives(Map<Long, ZhangBoLineageMemory> memories) {
    if (memories == null || memories.isEmpty()) return;
    Set<String> fingerprints = new HashSet<>();
    for (ZhangBoLineageMemory memory : memories.values()) {
      for (ZhangBoArchiveEntry entry : memory.getEntries()) {
        fingerprints.add(entry.getFingerprint());
      }
    }
    for (Record record : records) {
      if (fingerprints.contains(sha256VectorFingerprint(record.fingerprint))) {
        record.personalArchive = true;
      }
    }
  }

  /** The archive entry uses SHA-256 of vector text while Qg exposes the raw four-vector text. */
  private static String sha256VectorFingerprint(String qgFingerprint) {
    String[] vectors = qgFingerprint.split("\\|", -1);
    if (vectors.length != 4) return "";
    StringBuilder out = new StringBuilder();
    appendVector(out, "JS", vectors[0]);
    appendVector(out, "FA", vectors[1]);
    appendVector(out, "MA", vectors[2]);
    appendVector(out, "WA", vectors[3]);
    return sha256(out.toString()).toUpperCase(Locale.ROOT);
  }

  private static void appendVector(StringBuilder out, String name, String listText) {
    out.append(name).append('=');
    String body = listText.length() >= 2
        ? listText.substring(1, listText.length() - 1).replace(" ", "") : "";
    out.append(body).append('\n');
  }

  public void refreshState(long fe, List<PermutationSolution<Integer>> population,
      List<PermutationSolution<Integer>> globalArchive) {
    latestFe = Math.max(latestFe, fe);
    currentGlobal = minimum(globalArchive, null, 0);
    currentG1 = minimum(population, ZhangBoSubSwarm.G1_CMAX, 0);
    currentTECGlobal = minimum(globalArchive, null, 1);
    currentTWCGlobal = minimum(globalArchive, null, 6);
    if (Double.isFinite(currentG1)) bestG1 = Math.min(bestG1, currentG1);
    markGlobalArchive(globalArchive);
    emitDueCheckpoints(fe);
  }

  /** Refreshes the physical G1 partition without requiring subgroup attributes on initial copies. */
  public void refreshG1(List<PermutationSolution<Integer>> g1) {
    currentG1 = minimum(g1, null);
    if (Double.isFinite(currentG1)) bestG1 = Math.min(bestG1, currentG1);
  }

  private void markGlobalArchive(List<PermutationSolution<Integer>> globalArchive) {
    if (globalArchive == null) return;
    Set<String> fingerprints = new HashSet<>();
    for (PermutationSolution<Integer> solution : globalArchive) {
      fingerprints.add(ZhangBoQgController.fingerprint(solution));
    }
    for (Record record : records) {
      if (fingerprints.contains(record.fingerprint)) record.globalArchive = true;
    }
  }

  public void finish(long fe, List<PermutationSolution<Integer>> population,
      List<PermutationSolution<Integer>> globalArchive) {
    refreshState(fe, population, globalArchive);
    resolvePendingSurvival(population);
    if (checkpoints.isEmpty() || checkpoints.get(checkpoints.size() - 1).fe != fe) {
      checkpoints.add(snapshot(fe));
      resetWindow();
    }
  }

  /**
   * V35-P19 closure sweep: a PENDING record either entered the candidate set but
   * was never PDDR-selected (NOT_SELECTED), or was selected in the final round
   * with no next round left to resolve against the final population (YES/NO).
   * After this sweep the Survival stage is fully resolved and no PENDING remains.
   */
  private void resolvePendingSurvival(List<PermutationSolution<Integer>> population) {
    Set<String> populationFingerprints = new HashSet<>();
    if (population != null) {
      for (PermutationSolution<Integer> solution : population) {
        populationFingerprints.add(ZhangBoQgController.fingerprint(solution));
      }
    }
    for (Record record : records) {
      if (record.survival != Survival.PENDING) continue;
      if (!record.pddrRetained) {
        record.survival = Survival.NOT_SELECTED;
      } else {
        record.survival = populationFingerprints.contains(record.fingerprint)
            ? Survival.YES : Survival.NO;
      }
      resolvedPendingByFinish++;
    }
  }

  public long getResolvedPendingByFinish() { return resolvedPendingByFinish; }

  private void emitDueCheckpoints(long fe) {
    while (nextCheckpoint <= fe) {
      checkpoints.add(snapshot(nextCheckpoint));
      resetWindow();
      nextCheckpoint += checkpointInterval;
    }
  }

  private Checkpoint snapshot(long fe) {
    return new Checkpoint(fe, currentGlobal, finiteOrNaN(bestG1), currentG1,
        finiteOrNaN(bestGenerated), finiteOrNaN(bestGeneratedG1),
        finiteOrNaN(bestSurvived), finiteOrNaN(windowGenerated),
        finiteOrNaN(windowSurvived), currentTECGlobal, currentTWCGlobal,
        finiteOrNaN(bestTEC), finiteOrNaN(bestTWC));
  }

  private void resetWindow() {
    windowGenerated = Double.POSITIVE_INFINITY;
    windowSurvived = Double.POSITIVE_INFINITY;
  }

  private static double minimum(List<PermutationSolution<Integer>> solutions,
      ZhangBoSubSwarm requiredGroup) {
    return minimum(solutions, requiredGroup, 0);
  }

  private static double minimum(List<PermutationSolution<Integer>> solutions,
      ZhangBoSubSwarm requiredGroup, int objectiveIndex) {
    if (solutions == null || solutions.isEmpty()) return Double.NaN;
    double best = Double.POSITIVE_INFINITY;
    for (PermutationSolution<Integer> solution : solutions) {
      if (requiredGroup != null
          && solution.getAttribute(ZhangBoSubSwarm.class) != requiredGroup) continue;
      best = Math.min(best, solution.getObjective(objectiveIndex));
    }
    return finiteOrNaN(best);
  }

  private static double finiteOrNaN(double value) {
    return Double.isFinite(value) ? value : Double.NaN;
  }

  private static void requireSolution(PermutationSolution<Integer> solution) {
    if (solution == null || !Double.isFinite(solution.getObjective(0))
        || !Double.isFinite(solution.getObjective(1))
        || !Double.isFinite(solution.getObjective(6))) {
      throw new IllegalArgumentException("Cmax audit requires an evaluated solution");
    }
  }

  private static long lineageId(PermutationSolution<Integer> solution) {
    Object tag = solution.getAttribute(ZhangBoLineageTag.class);
    return tag instanceof ZhangBoLineageTag ? ((ZhangBoLineageTag) tag).getLineageId() : -1L;
  }

  public List<Record> getRecords() {
    return Collections.unmodifiableList(new ArrayList<>(records));
  }

  public List<Checkpoint> getCheckpoints() {
    return Collections.unmodifiableList(new ArrayList<>(checkpoints));
  }

  public String curvesCsv() {
    StringBuilder out = new StringBuilder();
    out.append("fe,bestCmaxGlobal,bestCmaxG1,currentBestCmaxG1,bestCmaxGenerated,")
        .append("bestCmaxGeneratedG1,bestCmaxSurvived,windowBestGenerated,")
        .append("windowBestSurvived,bestTECGlobal,bestTWCGlobal,")
        .append("bestTECGenerated,bestTWCGenerated\n");
    for (Checkpoint value : checkpoints) out.append(value.toCsv()).append('\n');
    return out.toString();
  }

  public String recordsCsv() {
    StringBuilder out = new StringBuilder();
    out.append("candidateId,parentId,lineageId,generated,admitted,evaluation,generation,cmax,tec,twc,subSwarm,mechanism,")
        .append("operator,enteredCandidateSet,pddrRetained,personalArchive,globalArchive,")
        .append("nextRoundSurvival,g1SocialTeacherParticleUses,g1SocialTeacherGenerations,")
        .append("g1PersonalTeacherParticleUses,g1PersonalTeacherGenerations,")
        .append("firstTeacherFE,lastTeacherFE,firstTeacherGeneration,lastTeacherGeneration,")
        .append("fingerprintSha256\n");
    for (Record value : records) out.append(value.toCsv()).append('\n');
    return out.toString();
  }

  public String summaryText() {
    int generatedOnly = 0;
    int candidateSet = 0;
    int pddr = 0;
    int personal = 0;
    int global = 0;
    int next = 0;
    Map<String, Integer> sources = new LinkedHashMap<>();
    for (Record record : records) {
      generatedOnly++;
      if (record.enteredCandidateSet) candidateSet++;
      if (record.pddrRetained) pddr++;
      if (record.personalArchive) personal++;
      if (record.globalArchive) global++;
      if (record.survival == Survival.YES) next++;
      String source = record.mechanism + "/" + record.operator;
      sources.put(source, sources.containsKey(source) ? sources.get(source) + 1 : 1);
    }
    return "schema=" + VERSION + "\ncheckpointInterval=" + checkpointInterval
        + "\nlatestFE=" + latestFe + "\nrecordCount=" + generatedOnly
        + "\nenteredCandidateSet=" + candidateSet + "\npddrRetained=" + pddr
        + "\npersonalArchive=" + personal + "\nglobalArchive=" + global
        + "\nnextRoundSurvived=" + next
        + "\nresolvedPendingByFinish=" + resolvedPendingByFinish
        + "\nbestCmaxGlobal=" + number(currentGlobal)
        + "\nbestTECGlobal=" + number(currentTECGlobal)
        + "\nbestTWCGlobal=" + number(currentTWCGlobal)
        + "\nbestCmaxGenerated=" + number(finiteOrNaN(bestGenerated))
        + "\nbestTECGenerated=" + number(finiteOrNaN(bestTEC))
        + "\nbestTWCGenerated=" + number(finiteOrNaN(bestTWC))
        + "\nrecordSources=" + sources + "\n";
  }

  /** V35-P18 three-objective best-ever accessors; each exposes an independent
   * scalar and the fingerprint of the real evaluated solution achieving it.
   * There is deliberately no API that concatenates the three minima into one
   * pseudo-solution. */
  public double getBestTEC() { return bestTEC; }
  public double getBestTWC() { return bestTWC; }
  public double getCurrentTECGlobal() { return currentTECGlobal; }
  public double getCurrentTWCGlobal() { return currentTWCGlobal; }
  public String getBestCmaxSource() { return bestCmaxSource; }
  public String getBestTECSource() { return bestTECSource; }
  public String getBestTWCSource() { return bestTWCSource; }

  public static final class Checkpoint {
    public final long fe;
    public final double bestGlobal;
    public final double bestG1;
    public final double currentG1;
    public final double bestGenerated;
    public final double bestGeneratedG1;
    public final double bestSurvived;
    public final double windowGenerated;
    public final double windowSurvived;
    public final double bestTECGlobal;
    public final double bestTWCGlobal;
    public final double bestTECGenerated;
    public final double bestTWCGenerated;

    private Checkpoint(long fe, double bestGlobal, double bestG1, double currentG1,
        double bestGenerated, double bestGeneratedG1, double bestSurvived,
        double windowGenerated, double windowSurvived, double bestTECGlobal,
        double bestTWCGlobal, double bestTECGenerated, double bestTWCGenerated) {
      this.fe = fe;
      this.bestGlobal = bestGlobal;
      this.bestG1 = bestG1;
      this.currentG1 = currentG1;
      this.bestGenerated = bestGenerated;
      this.bestGeneratedG1 = bestGeneratedG1;
      this.bestSurvived = bestSurvived;
      this.windowGenerated = windowGenerated;
      this.windowSurvived = windowSurvived;
      this.bestTECGlobal = bestTECGlobal;
      this.bestTWCGlobal = bestTWCGlobal;
      this.bestTECGenerated = bestTECGenerated;
      this.bestTWCGenerated = bestTWCGenerated;
    }

    private String toCsv() {
      return fe + "," + number(bestGlobal) + "," + number(bestG1) + ","
          + number(currentG1) + ","
          + number(bestGenerated) + "," + number(bestGeneratedG1) + ","
          + number(bestSurvived) + "," + number(windowGenerated) + ","
          + number(windowSurvived) + "," + number(bestTECGlobal) + ","
          + number(bestTWCGlobal) + "," + number(bestTECGenerated) + ","
          + number(bestTWCGenerated);
    }
  }

  public static final class Record {
    public final long id;
    public final String candidateId;
    public final String parentId;
    public final long lineageId;
    public final boolean generated;
    public final boolean admitted;
    public final long evaluation;
    public final int generation;
    public final String fingerprint;
    public final double cmax;
    public final double tec;
    public final double twc;
    public final ZhangBoSubSwarm subSwarm;
    public final Mechanism mechanism;
    public final Operator operator;
    public final boolean enteredCandidateSet;
    private boolean pddrRetained;
    private boolean personalArchive;
    private boolean globalArchive;
    private int pddrGeneration = -1;
    private Survival survival;
    private long g1SocialTeacherParticleUses;
    private long g1PersonalTeacherParticleUses;
    private int g1SocialTeacherGenerations;
    private int g1PersonalTeacherGenerations;
    private int lastSocialTeacherGeneration = -1;
    private int lastPersonalTeacherGeneration = -1;
    private long firstTeacherFE = -1L;
    private long lastTeacherFE = -1L;
    private int firstTeacherGeneration = -1;
    private int lastTeacherGeneration = -1;

    private Record(long id, long evaluation, int generation, String fingerprint,
        double cmax, double tec, double twc, ZhangBoSubSwarm subSwarm,
        Mechanism mechanism, Operator operator, boolean enteredCandidateSet,
        String parentId, long lineageId) {
      this.id = id;
      this.candidateId = Long.toString(id);
      this.parentId = parentId;
      this.lineageId = lineageId;
      this.generated = true;
      this.admitted = enteredCandidateSet;
      this.evaluation = evaluation;
      this.generation = generation;
      this.fingerprint = fingerprint;
      this.cmax = cmax;
      this.tec = tec;
      this.twc = twc;
      this.subSwarm = subSwarm;
      this.mechanism = mechanism;
      this.operator = operator;
      this.enteredCandidateSet = enteredCandidateSet;
      this.survival = enteredCandidateSet ? Survival.PENDING : Survival.NOT_SELECTED;
    }

    public boolean isPddrRetained() { return pddrRetained; }
    public boolean isPersonalArchive() { return personalArchive; }
    public boolean isGlobalArchive() { return globalArchive; }
    public Survival getSurvival() { return survival; }
    public long getG1SocialTeacherParticleUses() { return g1SocialTeacherParticleUses; }
    public long getG1PersonalTeacherParticleUses() { return g1PersonalTeacherParticleUses; }
    public int getG1SocialTeacherGenerations() { return g1SocialTeacherGenerations; }
    public int getG1PersonalTeacherGenerations() { return g1PersonalTeacherGenerations; }

    private void observeG1SocialTeacher(long fe, int generation) {
      g1SocialTeacherParticleUses++;
      if (lastSocialTeacherGeneration != generation) {
        g1SocialTeacherGenerations++;
        lastSocialTeacherGeneration = generation;
      }
      observeTeacherTime(fe, generation);
    }

    private void observeG1PersonalTeacher(long fe, int generation) {
      g1PersonalTeacherParticleUses++;
      if (lastPersonalTeacherGeneration != generation) {
        g1PersonalTeacherGenerations++;
        lastPersonalTeacherGeneration = generation;
      }
      observeTeacherTime(fe, generation);
    }

    private void observeTeacherTime(long fe, int generation) {
      if (firstTeacherFE < 0L) {
        firstTeacherFE = fe;
        firstTeacherGeneration = generation;
      }
      lastTeacherFE = fe;
      lastTeacherGeneration = generation;
    }

    private String toCsv() {
      return candidateId + "," + parentId + "," + lineageId + "," + generated + ","
          + admitted + "," + evaluation + "," + generation + "," + number(cmax) + ","
          + number(tec) + "," + number(twc) + ","
          + (subSwarm == null ? "UNASSIGNED" : subSwarm) + "," + mechanism + ","
          + operator + "," + enteredCandidateSet + "," + pddrRetained + ","
          + personalArchive + "," + globalArchive + "," + survival + ","
          + g1SocialTeacherParticleUses + "," + g1SocialTeacherGenerations + ","
          + g1PersonalTeacherParticleUses + "," + g1PersonalTeacherGenerations + ","
          + optional(firstTeacherFE) + "," + optional(lastTeacherFE) + ","
          + optional(firstTeacherGeneration) + "," + optional(lastTeacherGeneration) + ","
          + sha256(fingerprint);
    }

  }

  private static String number(double value) {
    return Double.isFinite(value) ? String.format(Locale.ROOT, "%.12f", value) : "";
  }

  private static String optional(long value) { return value < 0L ? "" : Long.toString(value); }

  private static String sha256(String value) {
    try {
      byte[] digest = MessageDigest.getInstance("SHA-256")
          .digest(value.getBytes(StandardCharsets.UTF_8));
      StringBuilder out = new StringBuilder();
      for (byte item : digest) out.append(String.format(Locale.ROOT, "%02x", item & 0xff));
      return out.toString();
    } catch (NoSuchAlgorithmException exception) {
      throw new IllegalStateException("SHA-256 unavailable", exception);
    }
  }
}
