package org.uma.jmetal.algorithm.multiobjective.mypso.v35;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.HashMap;
import java.util.Map;

/**
 * Observation-only DSCR ledger. The actual teachers live in the Qg controller's
 * previous/historical caches; this class never owns or selects a teacher.
 */
public final class V35DscrTeacherCache implements Serializable {
  private static final long serialVersionUID = 2L;
  public static final String VERSION = "v35-dscr-metrics-v2";

  public enum CacheType { PREVIOUS, HISTORICAL }

  public static final class Refresh implements Serializable {
    private static final long serialVersionUID = 2L;
    private final long decisionCycle;
    private final long generation;
    private final long fe;
    private final V35SubSwarmRole role;
    private final CacheType cacheType;
    private final V35SocialTeacher before;
    private final V35SocialTeacher after;
    private final int dominatorCount;
    private final boolean stale;
    private final boolean replaced;
    private final double directionScore;
    private final Long firstKnownDominatedFe;
    private final Long refreshFe;
    private final Long dominanceAge;
    private final long teacherExposure;

    private Refresh(long decisionCycle, long generation, long fe,
        V35SubSwarmRole role, CacheType cacheType, V35SocialTeacher before,
        V35SocialTeacher after, int dominatorCount, boolean stale,
        boolean replaced, double directionScore, Long firstKnownDominatedFe,
        Long refreshFe, Long dominanceAge, long teacherExposure) {
      this.decisionCycle = decisionCycle;
      this.generation = generation;
      this.fe = fe;
      this.role = role;
      this.cacheType = cacheType;
      this.before = before;
      this.after = after;
      this.dominatorCount = dominatorCount;
      this.stale = stale;
      this.replaced = replaced;
      this.directionScore = directionScore;
      this.firstKnownDominatedFe = firstKnownDominatedFe;
      this.refreshFe = refreshFe;
      this.dominanceAge = dominanceAge;
      this.teacherExposure = teacherExposure;
    }

    public long getDecisionCycle() { return decisionCycle; }
    public long getGeneration() { return generation; }
    public long getFe() { return fe; }
    public V35SubSwarmRole getRole() { return role; }
    public CacheType getCacheType() { return cacheType; }
    public V35SocialTeacher getBefore() { return before; }
    public V35SocialTeacher getAfter() { return after; }
    public int getDominatorCount() { return dominatorCount; }
    public boolean isStale() { return stale; }
    public boolean isReplaced() { return replaced; }
    public boolean isStrictDominatorFound() { return dominatorCount > 0; }
    public double getDirectionScore() { return directionScore; }
    public Long getFirstKnownDominatedFe() { return firstKnownDominatedFe; }
    public Long getRefreshFe() { return refreshFe; }
    public Long getDominanceAgeValue() { return dominanceAge; }
    /** Compatibility getter; -1 means NOT_APPLICABLE. */
    public long getDominanceAge() { return dominanceAge == null ? -1L : dominanceAge; }
    public long getTeacherExposure() { return teacherExposure; }
    /** Compatibility getter. */
    public long getExposure() { return teacherExposure; }
    public double getDtur() { return Double.NaN; }
    public double getScrr() { return Double.NaN; }

    public String toCsv() {
      return decisionCycle + "," + generation + "," + fe + "," + role + ","
          + cacheType + "," + id(before) + "," + objectives(before) + ","
          + dominatorCount + "," + stale + "," + id(after) + ","
          + objectives(after) + "," + number(directionScore) + ","
          + optional(firstKnownDominatedFe) + "," + optional(refreshFe) + ","
          + optional(dominanceAge) + "," + teacherExposure;
    }
  }

  public static final class TeacherUse implements Serializable {
    private static final long serialVersionUID = 1L;
    private final long decisionCycle;
    private final long generation;
    private final long fe;
    private final V35SubSwarmRole role;
    private final String teacherId;
    private final double[] objectives;
    private final boolean dominated;
    private final int dominatorCount;

    private TeacherUse(long decisionCycle, long generation, long fe,
        V35SubSwarmRole role, V35SocialTeacher teacher, int dominatorCount) {
      this.decisionCycle = decisionCycle;
      this.generation = generation;
      this.fe = fe;
      this.role = role;
      this.teacherId = teacher.getFingerprint();
      this.objectives = teacher.getObjectives();
      this.dominatorCount = dominatorCount;
      this.dominated = dominatorCount > 0;
    }

    public boolean isDominated() { return dominated; }
    public String toCsv() {
      return decisionCycle + "," + generation + "," + fe + "," + role + ","
          + teacherId + "," + vector(objectives) + "," + dominated + ","
          + dominatorCount;
    }
  }

  private final List<Refresh> refreshEvents = new ArrayList<>();
  private final List<TeacherUse> teacherUses = new ArrayList<>();
  private final Map<String, Long> firstKnownDominated = new HashMap<>();
  private long validityChecks;
  private long replacements;
  private long dominatedTeacherUses;

  public Refresh recordRefresh(long decisionCycle, long generation, long fe,
      V35SubSwarmRole role, CacheType cacheType, V35SocialTeacher before,
      V35SocialTeacher after, V35SocialKnowledgeSnapshot snapshot,
      long teacherExposure) {
    if (role == null || cacheType == null || before == null || after == null
        || snapshot == null || decisionCycle < 0L || generation < 0L || fe < 0L) {
      throw new IllegalArgumentException("invalid DSCR refresh event");
    }
    int dominators = snapshot.strictDominatorCount(before.getObjectives());
    boolean stale = dominators > 0;
    boolean replaced = !before.getFingerprint().equals(after.getFingerprint());
    if (replaced && !stale) {
      throw new IllegalArgumentException("DSCR cannot replace a nondominated teacher");
    }
    validityChecks++;
    if (replaced) replacements++;
    String staleKey = role + "|" + cacheType + "|" + before.getFingerprint();
    if (stale) firstKnownDominated.putIfAbsent(staleKey, fe);
    Long firstKnown = stale ? firstKnownDominated.get(staleKey) : null;
    Long refreshed = replaced ? fe : null;
    Long age = replaced ? Long.valueOf(refreshed - firstKnown) : null;
    if (replaced) firstKnownDominated.remove(staleKey);
    Refresh event = new Refresh(decisionCycle, generation, fe, role, cacheType,
        before, after, dominators, stale, replaced,
        V35DscrSanitizer.directionScore(role, after.getObjectives(), snapshot),
        firstKnown, refreshed, age, teacherExposure);
    refreshEvents.add(event);
    return event;
  }

  public TeacherUse recordTeacherUse(long decisionCycle, long generation, long fe,
      V35SubSwarmRole role, V35SocialTeacher teacher,
      V35SocialKnowledgeSnapshot snapshot) {
    if (role == null || teacher == null || snapshot == null || decisionCycle < 0L
        || generation < 0L || fe < 0L) {
      throw new IllegalArgumentException("invalid DSCR teacher-use event");
    }
    TeacherUse event = new TeacherUse(decisionCycle, generation, fe, role, teacher,
        snapshot.strictDominatorCount(teacher.getObjectives()));
    teacherUses.add(event);
    if (event.isDominated()) dominatedTeacherUses++;
    return event;
  }

  public long getTeacherUses() { return teacherUses.size(); }
  public long getDominatedTeacherUses() { return dominatedTeacherUses; }
  public long getValidityChecks() { return validityChecks; }
  public long getReplacements() { return replacements; }
  public boolean isDturDefined() { return !teacherUses.isEmpty(); }
  public double getDtur() {
    return isDturDefined() ? (double) dominatedTeacherUses / teacherUses.size() : Double.NaN;
  }
  public double getScrr() {
    return validityChecks == 0L ? Double.NaN : (double) replacements / validityChecks;
  }
  public List<Refresh> getRefreshEvents() {
    return Collections.unmodifiableList(new ArrayList<>(refreshEvents));
  }
  public List<TeacherUse> getTeacherUseEvents() {
    return Collections.unmodifiableList(new ArrayList<>(teacherUses));
  }

  public String canonicalStatistics() {
    return "schema=" + VERSION
        + "|teacherUses=" + getTeacherUses()
        + "|dominatedTeacherUses=" + dominatedTeacherUses
        + "|dtur=" + number(getDtur())
        + "|dturDefined=" + isDturDefined()
        + "|validityChecks=" + validityChecks
        + "|replacements=" + replacements
        + "|scrr=" + number(getScrr());
  }

  public String eventsCsv() {
    StringBuilder out = new StringBuilder("decisionCycle,generation,FE,group,cacheType,"
        + "oldLeaderId,oldLeaderObjectives,dominatorCount,stale,replacementId,"
        + "replacementObjectives,directionScore,firstKnownDominatedFE,refreshFE,"
        + "dominanceAge,teacherExposure\n");
    for (Refresh event : refreshEvents) out.append(event.toCsv()).append('\n');
    return out.toString();
  }

  public String teacherUsesCsv() {
    StringBuilder out = new StringBuilder(
        "decisionCycle,generation,FE,group,teacherId,teacherObjectives,dominated,dominatorCount\n");
    for (TeacherUse event : teacherUses) out.append(event.toCsv()).append('\n');
    return out.toString();
  }

  private static String id(V35SocialTeacher teacher) {
    return teacher == null ? "" : teacher.getFingerprint();
  }
  private static String objectives(V35SocialTeacher teacher) {
    return teacher == null ? "" : vector(teacher.getObjectives());
  }
  private static String vector(double[] values) {
    return number(values[0]) + ";" + number(values[1]) + ";" + number(values[2]);
  }
  private static String optional(Long value) {
    return value == null ? "NOT_APPLICABLE" : Long.toString(value);
  }
  private static String number(double value) {
    return Double.isFinite(value) ? String.format(Locale.ROOT, "%.12f", value)
        : "NOT_APPLICABLE";
  }
}
