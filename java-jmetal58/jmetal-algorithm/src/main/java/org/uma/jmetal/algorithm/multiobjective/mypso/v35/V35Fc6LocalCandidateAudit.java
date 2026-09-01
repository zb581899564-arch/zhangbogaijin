package org.uma.jmetal.algorithm.multiobjective.mypso.v35;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.uma.jmetal.algorithm.multiobjective.mypso.zhangbo.ZhangBoEvaluatedPddrSelector;
import org.uma.jmetal.algorithm.multiobjective.mypso.zhangbo.ZhangBoQgController;
import org.uma.jmetal.algorithm.multiobjective.mypso.zhangbo.ZhangBoSubSwarm;
import org.uma.jmetal.solution.PermutationSolution;

/**
 * FC-6A.3 observation-only ledger for candidates consumed by the shared local
 * FE window. It intentionally stores counts only: no candidate, ranking or
 * random source is reachable from this audit object.
 */
public final class V35Fc6LocalCandidateAudit {
  private static volatile boolean enabled;
  private static V35Fc6LocalCandidateAudit current;

  private final Map<Integer, Round> rounds = new LinkedHashMap<Integer, Round>();
  private final List<TeacherExposure> teacherExposures = new ArrayList<TeacherExposure>();

  public static void setEnabled(boolean value) {
    enabled = value;
    if (!value) current = null;
  }

  public static void reset() {
    current = enabled ? new V35Fc6LocalCandidateAudit() : null;
  }

  public static V35Fc6LocalCandidateAudit current() { return current; }

  public void recordEvaluated(int cycle, ZhangBoEvaluatedPddrSelector.Source source, long fe) {
    recordEvaluated(cycle, source, fe, null);
  }

  public void recordEvaluated(int cycle, ZhangBoEvaluatedPddrSelector.Source source, long fe,
      PermutationSolution<Integer> candidate) {
    counts(cycle, source).evaluated++;
    counts(cycle, source).fe += fe;
    if (candidate != null && Double.isFinite(candidate.getObjective(0))) {
      counts(cycle, source).bestGeneratedCmax = Math.min(
          counts(cycle, source).bestGeneratedCmax, candidate.getObjective(0));
    }
  }

  public void recordAccepted(int cycle, ZhangBoEvaluatedPddrSelector.Source source) {
    counts(cycle, source).accepted++;
  }

  public void recordSuperseded(int cycle, ZhangBoEvaluatedPddrSelector.Source source) {
    counts(cycle, source).superseded++;
  }

  public void recordEnteredPddr(int cycle, ZhangBoEvaluatedPddrSelector.Source source) {
    counts(cycle, source).enteredPddr++;
  }

  public void recordPddrOutcome(int cycle,
      List<ZhangBoEvaluatedPddrSelector.Source> allSources,
      List<ZhangBoEvaluatedPddrSelector.Candidate> selected) {
    Map<ZhangBoEvaluatedPddrSelector.Source, Integer> selectedCounts =
        new EnumMap<ZhangBoEvaluatedPddrSelector.Source, Integer>(
            ZhangBoEvaluatedPddrSelector.Source.class);
    for (ZhangBoEvaluatedPddrSelector.Candidate candidate : selected) {
      Integer prior = selectedCounts.get(candidate.getSource());
      selectedCounts.put(candidate.getSource(), prior == null ? 1 : prior + 1);
      if (candidate.getAssignedRegionRole() != null) {
        counts(cycle, candidate.getSource()).regionAssignments++;
        counts(cycle, candidate.getSource()).regionRoles.put(candidate.getAssignedRegionRole(),
            Integer.valueOf(1 + regionCount(counts(cycle, candidate.getSource()),
                candidate.getAssignedRegionRole())));
      }
    }
    for (ZhangBoEvaluatedPddrSelector.Source source : allSources) {
      Count count = counts(cycle, source);
      int selectedForSource = selectedCounts.containsKey(source)
          ? selectedCounts.get(source) : 0;
      if (selectedForSource > 0) {
        count.pddrSelected++;
        selectedCounts.put(source, selectedForSource - 1);
      } else {
        count.pddrRejected++;
      }
    }
  }

  /** Pure side-channel telemetry; it never influences Qg selection. */
  public void recordTeacherExposure(int cycle, ZhangBoSubSwarm requester,
      PermutationSolution<Integer> teacher) {
    if (requester == null || teacher == null) return;
    Object assigned = teacher.getAttribute(ZhangBoSubSwarm.class);
    ZhangBoSubSwarm teacherRegion = assigned instanceof ZhangBoSubSwarm
        ? (ZhangBoSubSwarm) assigned : null;
    teacherExposures.add(new TeacherExposure(cycle, requester, teacherRegion,
        ZhangBoQgController.fingerprint(teacher), teacher.getObjective(0),
        teacher.getObjective(1), teacher.getObjective(6)));
  }

  public String summaryText() {
    StringBuilder text = new StringBuilder("fc6LocalCandidateAudit=true\n");
    for (Round round : rounds.values()) {
      for (Map.Entry<ZhangBoEvaluatedPddrSelector.Source, Count> entry : round.counts.entrySet()) {
        Count c = entry.getValue();
        text.append("cycle=").append(round.cycle).append(" source=").append(sourceName(entry.getKey()))
            .append(" evaluated=").append(c.evaluated).append(" accepted=").append(c.accepted)
            .append(" superseded=").append(c.superseded).append(" enteredPddr=")
            .append(c.enteredPddr).append(" pddrSelected=").append(c.pddrSelected)
            .append(" pddrRejected=").append(c.pddrRejected).append(" fe=").append(c.fe)
            .append(" regionAssignments=").append(c.regionAssignments)
            .append(" regionRoles=").append(c.regionRoles)
            .append(" bestGeneratedCmax=").append(c.bestGeneratedCmax)
            .append('\n');
      }
    }
    return text.toString();
  }

  public void writeCsv(Path output) throws IOException {
    StringBuilder text = new StringBuilder(
        "cycle,source,evaluated,accepted,superseded,enteredPddr,pddrSelected,pddrRejected,fe,regionAssignments,regionRoles,bestGeneratedCmax\n");
    for (Round round : rounds.values()) {
      for (Map.Entry<ZhangBoEvaluatedPddrSelector.Source, Count> entry : round.counts.entrySet()) {
        Count c = entry.getValue();
        text.append(round.cycle).append(',').append(sourceName(entry.getKey())).append(',')
            .append(c.evaluated).append(',').append(c.accepted).append(',')
            .append(c.superseded).append(',').append(c.enteredPddr).append(',')
            .append(c.pddrSelected).append(',').append(c.pddrRejected).append(',')
            .append(c.fe).append(',').append(c.regionAssignments).append(',')
            .append('"').append(c.regionRoles).append('"').append(',')
            .append(c.bestGeneratedCmax).append('\n');
      }
    }
    Files.write(output, text.toString().getBytes(StandardCharsets.UTF_8));
  }

  public void writeCrossRegionTeachersCsv(Path output) throws IOException {
    StringBuilder text = new StringBuilder(
        "cycle,requesterRole,teacherRegion,teacherFingerprint,Cmax,TEC,TWC,crossRegion\n");
    for (TeacherExposure value : teacherExposures) {
      text.append(value.cycle).append(',').append(value.requester).append(',')
          .append(value.teacherRegion == null ? "NOT_ASSIGNED" : value.teacherRegion).append(',')
          .append(value.fingerprint).append(',').append(value.cmax).append(',')
          .append(value.tec).append(',').append(value.twc).append(',')
          .append(value.isCrossRegion()).append('\n');
    }
    Files.write(output, text.toString().getBytes(StandardCharsets.UTF_8));
  }

  public String crossRegionTeacherSummary() {
    long cross = 0L;
    for (TeacherExposure value : teacherExposures) if (value.isCrossRegion()) cross++;
    return "teacherExposure=" + teacherExposures.size() + "\n"
        + "crossRegionTeacherCount=" + cross + "\n";
  }

  public static String callChainText() {
    return "formal Q final -> [CA-TA-Lite | inherited critical swap/insert/O1-O9]"
        + " -> evaluated PDDR merge pool -> selector -> next physical slots\n";
  }

  public int getRecordedCycleCount() { return rounds.size(); }

  /** REGION_AWARE has evidence only when a selected candidate carried a role. */
  public boolean hasRegionAwareEvidence() {
    for (Round round : rounds.values()) {
      for (Count count : round.counts.values()) if (count.regionAssignments > 0L) return true;
    }
    return false;
  }

  private static int regionCount(Count count, ZhangBoSubSwarm role) {
    Integer value = count.regionRoles.get(role);
    return value == null ? 0 : value.intValue();
  }

  private static String sourceName(ZhangBoEvaluatedPddrSelector.Source source) {
    return source == ZhangBoEvaluatedPddrSelector.Source.GLOBAL_OFFSPRING
        ? "GLOBAL_Q_FINAL" : source.name();
  }

  private Count counts(int cycle, ZhangBoEvaluatedPddrSelector.Source source) {
    Round round = rounds.get(cycle);
    if (round == null) {
      round = new Round(cycle);
      rounds.put(cycle, round);
    }
    Count value = round.counts.get(source);
    if (value == null) {
      value = new Count();
      round.counts.put(source, value);
    }
    return value;
  }

  private static final class Round {
    final int cycle;
    final Map<ZhangBoEvaluatedPddrSelector.Source, Count> counts =
        new EnumMap<ZhangBoEvaluatedPddrSelector.Source, Count>(
            ZhangBoEvaluatedPddrSelector.Source.class);
    Round(int cycle) { this.cycle = cycle; }
  }

  private static final class Count {
    long evaluated;
    long accepted;
    long superseded;
    long enteredPddr;
    long pddrSelected;
    long pddrRejected;
    long fe;
    long regionAssignments;
    double bestGeneratedCmax = Double.POSITIVE_INFINITY;
    final Map<ZhangBoSubSwarm, Integer> regionRoles =
        new EnumMap<ZhangBoSubSwarm, Integer>(ZhangBoSubSwarm.class);
  }

  private static final class TeacherExposure {
    final int cycle;
    final ZhangBoSubSwarm requester;
    final ZhangBoSubSwarm teacherRegion;
    final String fingerprint;
    final double cmax;
    final double tec;
    final double twc;
    TeacherExposure(int cycle, ZhangBoSubSwarm requester, ZhangBoSubSwarm teacherRegion,
        String fingerprint, double cmax, double tec, double twc) {
      this.cycle = cycle;
      this.requester = requester;
      this.teacherRegion = teacherRegion;
      this.fingerprint = fingerprint;
      this.cmax = cmax;
      this.tec = tec;
      this.twc = twc;
    }
    boolean isCrossRegion() { return teacherRegion != null && teacherRegion != requester; }
  }
}
