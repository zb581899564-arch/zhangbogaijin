package org.uma.jmetal.algorithm.multiobjective.mypso.zhangbo;

import org.uma.jmetal.solution.PermutationSolution;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** P6.2 lineage initialization, inheritance, splitting, deletion, and diagnostics. */
public final class ZhangBoLineageCoordinator {
  public static final class Branch {
    private final PermutationSolution<Integer> solution;
    private final List<PermutationSolution<Integer>> authorHistory;
    private final ZhangBoEvaluatedPddrSelector.Source source;
    private final int sourceSlot;
    private final double pddrScore;

    private Branch(PermutationSolution<Integer> solution,
                   List<PermutationSolution<Integer>> authorHistory,
                   ZhangBoEvaluatedPddrSelector.Source source,
                   int sourceSlot, double pddrScore) {
      this.solution = ZhangBoSolutionSupport.deepCopy(solution);
      this.authorHistory = Collections.unmodifiableList(
          ZhangBoSolutionSupport.deepCopySolutions(authorHistory));
      this.source = source;
      this.sourceSlot = sourceSlot;
      this.pddrScore = pddrScore;
    }

    public PermutationSolution<Integer> getSolution() {
      return ZhangBoSolutionSupport.deepCopy(solution);
    }
    public List<PermutationSolution<Integer>> getAuthorHistory() {
      return ZhangBoSolutionSupport.deepCopySolutions(authorHistory);
    }
    public ZhangBoEvaluatedPddrSelector.Source getSource() { return source; }
    public int getSourceSlot() { return sourceSlot; }
    public double getPddrScore() { return pddrScore; }
  }

  private final ZhangBoPersonalArchiveConfiguration configuration;
  private final ZhangBoPersonalArchive archive;
  private final boolean allowMissingFatigueAsZero;
  private Map<Long, ZhangBoLineageMemory> memories = new LinkedHashMap<>();
  private final ZhangBoEventLog events = new ZhangBoEventLog();
  private long nextLineageId;
  private long insertions;
  private long dominatedRemoved;
  private long duplicatesRemoved;
  private long truncatedRemoved;
  private long splits;
  private long deletions;
  private long migrations;
  private ZhangBoArchiveBounds frozenBounds;

  public ZhangBoLineageCoordinator(ZhangBoPersonalArchiveConfiguration configuration) {
    this(configuration, false);
  }

  public ZhangBoLineageCoordinator(ZhangBoPersonalArchiveConfiguration configuration,
      boolean allowMissingFatigueAsZero) {
    if (configuration == null || !configuration.isEnabled()) {
      throw new IllegalArgumentException("Enabled archive configuration required");
    }
    this.configuration = configuration;
    this.archive = new ZhangBoPersonalArchive(configuration);
    this.allowMissingFatigueAsZero = allowMissingFatigueAsZero;
  }

  public void initialize(
      List<PermutationSolution<Integer>> population,
      List<PermutationSolution<Integer>> globalNondominated,
      int generation) {
    if (population == null || population.isEmpty()) {
      throw new IllegalArgumentException("Cannot initialize empty lineage population");
    }
    frozenBounds = ZhangBoArchiveBounds.fromSolutions(population, globalNondominated,
        configuration.getNormalizationEpsilon(), allowMissingFatigueAsZero);
    Map<Long, ZhangBoLineageMemory> initial = new LinkedHashMap<>();
    for (int index = 0; index < population.size(); index++) {
      PermutationSolution<Integer> solution = population.get(index);
      long lineageId = index;
      ZhangBoSubSwarm group = requireGroup(solution);
      solution.setAttribute(ZhangBoLineageTag.class, new ZhangBoLineageTag(lineageId, -1L));
      ZhangBoArchiveEntry entry = ZhangBoArchiveEntry.fromSolution(solution,
          ZhangBoEvaluatedPddrSelector.Source.PARENT, generation, index + 1L,
          allowMissingFatigueAsZero);
      initial.put(lineageId, new ZhangBoLineageMemory(lineageId, -1L, generation,
          1L, 0, group, Collections.singletonList(entry)));
      events.add("initialize:" + lineageId + ":" + group + ":" + entry.getFingerprint());
    }
    memories = initial;
    nextLineageId = population.size();
  }

  public void freezeBounds(
      List<PermutationSolution<Integer>> population,
      List<PermutationSolution<Integer>> globalNondominated) {
    frozenBounds = ZhangBoArchiveBounds.fromSolutions(population, globalNondominated,
        configuration.getNormalizationEpsilon(), allowMissingFatigueAsZero);
  }

  public List<Branch> rebuild(
      List<ZhangBoEvaluatedPddrSelector.Candidate> selected,
      int generation) {
    if (frozenBounds == null) throw new IllegalStateException("Archive bounds are not frozen");
    Map<Long, List<ZhangBoEvaluatedPddrSelector.Candidate>> byOldLineage = new HashMap<>();
    for (ZhangBoEvaluatedPddrSelector.Candidate candidate : selected) {
      PermutationSolution<Integer> solution = candidate.getSolution();
      ZhangBoLineageTag tag = requireTag(solution);
      if (!memories.containsKey(tag.getLineageId())) {
        throw new IllegalStateException("Unknown lineage " + tag.getLineageId());
      }
      List<ZhangBoEvaluatedPddrSelector.Candidate> values = byOldLineage.get(tag.getLineageId());
      if (values == null) {
        values = new ArrayList<>();
        byOldLineage.put(tag.getLineageId(), values);
      }
      values.add(candidate);
    }

    Map<Long, ZhangBoLineageMemory> rebuilt = new LinkedHashMap<>();
    Map<CandidateKey, Long> assignedIds = new HashMap<>();
    List<Long> oldIds = new ArrayList<>(memories.keySet());
    Collections.sort(oldIds);
    for (Long oldId : oldIds) {
      ZhangBoLineageMemory old = memories.get(oldId);
      List<ZhangBoEvaluatedPddrSelector.Candidate> survivors = byOldLineage.get(oldId);
      if (survivors == null || survivors.isEmpty()) {
        deletions++;
        events.add("delete:lineage=" + oldId + ",generation=" + generation);
        continue;
      }
      sortCandidates(survivors);
      if (survivors.size() == 1) {
        ZhangBoEvaluatedPddrSelector.Candidate survivor = survivors.get(0);
        ZhangBoLineageMemory memory = evolve(old, oldId, old.getParentLineageId(),
            survivor, generation);
        rebuilt.put(oldId, memory);
        assignedIds.put(CandidateKey.of(survivor), oldId);
      } else {
        splits += survivors.size() - 1L;
        for (ZhangBoEvaluatedPddrSelector.Candidate survivor : survivors) {
          long newId = nextLineageId++;
          ZhangBoLineageMemory memory = evolve(old, newId, oldId, survivor, generation);
          rebuilt.put(newId, memory);
          assignedIds.put(CandidateKey.of(survivor), newId);
          events.add("split:old=" + oldId + ",new=" + newId + ",source="
              + survivor.getSource() + ",generation=" + generation);
        }
      }
    }

    List<Branch> result = new ArrayList<>(selected.size());
    for (ZhangBoEvaluatedPddrSelector.Candidate candidate : selected) {
      Long lineageId = assignedIds.get(CandidateKey.of(candidate));
      if (lineageId == null) throw new IllegalStateException("Selected candidate was not assigned");
      ZhangBoLineageMemory memory = rebuilt.get(lineageId);
      PermutationSolution<Integer> solution = candidate.getSolution();
      solution.setAttribute(ZhangBoLineageTag.class,
          new ZhangBoLineageTag(lineageId, memory.getParentLineageId()));
      result.add(new Branch(solution, candidate.getAuthorHistory(), candidate.getSource(),
          candidate.getSourceSlot(), candidate.getPddrScore()));
    }
    memories = rebuilt;
    return result;
  }

  private ZhangBoLineageMemory evolve(
      ZhangBoLineageMemory old, long lineageId, long parentLineageId,
      ZhangBoEvaluatedPddrSelector.Candidate candidate, int generation) {
    PermutationSolution<Integer> solution = candidate.getSolution();
    ZhangBoSubSwarm group = requireGroup(solution);
    ZhangBoArchiveEntry entry = ZhangBoArchiveEntry.fromSolution(solution,
        candidate.getSource(), generation, candidate.getEvaluationOrdinal(),
        allowMissingFatigueAsZero);
    ZhangBoPersonalArchive.Update update = archive.update(old.getEntries(), entry, group,
        frozenBounds);
    insertions++;
    dominatedRemoved += update.getDominatedRemoved();
    duplicatesRemoved += update.getDuplicatesRemoved();
    truncatedRemoved += update.getTruncatedRemoved();
    if (old.getSubSwarm() != group) migrations++;
    int noUpdate = update.isInsertedEntrySurvived() ? 0 : old.getNoArchiveUpdateCount() + 1;
    long version = old.getArchiveVersion() + 1L;
    events.add("evolve:lineage=" + lineageId + ",source=" + candidate.getSource()
        + ",group=" + group + ",size=" + update.getEntries().size()
        + ",survived=" + update.isInsertedEntrySurvived() + ",generation=" + generation);
    return new ZhangBoLineageMemory(lineageId, parentLineageId, generation, version,
        noUpdate, group, update.getEntries());
  }

  private void sortCandidates(List<ZhangBoEvaluatedPddrSelector.Candidate> values) {
    Collections.sort(values, Comparator
        .comparingInt((ZhangBoEvaluatedPddrSelector.Candidate value) -> sourceRank(value.getSource()))
        .thenComparing(value -> ZhangBoArchiveEntry.fromSolution(value.getSolution(),
            value.getSource(), 0, value.getEvaluationOrdinal(),
            allowMissingFatigueAsZero).getFingerprint())
        .thenComparingInt(ZhangBoEvaluatedPddrSelector.Candidate::getOriginalOrder));
  }

  private static int sourceRank(ZhangBoEvaluatedPddrSelector.Source source) {
    switch (source) {
      case GLOBAL_OFFSPRING: return 0;
      case PARENT: return 1;
      case INTER_FACTORY_LOCAL: return 2;
      case INTRA_FACTORY_VNS:
      default: return 3;
    }
  }

  private static ZhangBoLineageTag requireTag(PermutationSolution<Integer> solution) {
    Object value = solution.getAttribute(ZhangBoLineageTag.class);
    if (!(value instanceof ZhangBoLineageTag)) {
      throw new IllegalStateException("Selected candidate has no lineage tag");
    }
    return (ZhangBoLineageTag) value;
  }

  private static ZhangBoSubSwarm requireGroup(PermutationSolution<Integer> solution) {
    Object value = solution.getAttribute(ZhangBoSubSwarm.class);
    if (!(value instanceof ZhangBoSubSwarm)) {
      throw new IllegalStateException("Lineage candidate has no subgroup tag");
    }
    return (ZhangBoSubSwarm) value;
  }

  public Map<Long, ZhangBoLineageMemory> getMemories() {
    return new LinkedHashMap<>(memories);
  }
  public List<String> getEvents() { return events.snapshot(); }
  public long getEventCount() { return events.getTotalCount(); }
  public String getEventStreamHash() { return events.rollingSha256(); }
  public long getInsertions() { return insertions; }
  public long getDominatedRemoved() { return dominatedRemoved; }
  public long getDuplicatesRemoved() { return duplicatesRemoved; }
  public long getTruncatedRemoved() { return truncatedRemoved; }
  public long getSplits() { return splits; }
  public long getDeletions() { return deletions; }
  public long getMigrations() { return migrations; }
  public ZhangBoArchiveBounds getFrozenBounds() { return frozenBounds; }

  public String toCanonicalText() {
    StringBuilder out = new StringBuilder();
    List<Long> ids = new ArrayList<>(memories.keySet());
    Collections.sort(ids);
    out.append("lineages=").append(ids.size()).append('\n');
    for (Long id : ids) {
      ZhangBoLineageMemory memory = memories.get(id);
      out.append("lineage=").append(id).append(",parent=")
          .append(memory.getParentLineageId()).append(",group=")
          .append(memory.getSubSwarm()).append(",version=")
          .append(memory.getArchiveVersion()).append(",size=")
          .append(memory.getEntries().size()).append('\n');
      for (ZhangBoArchiveEntry entry : memory.getEntries()) {
        out.append("entry=").append(entry.toCanonicalText()).append('\n');
      }
    }
    return out.toString();
  }

  private static final class CandidateKey {
    private final int originalOrder;
    private final ZhangBoEvaluatedPddrSelector.Source source;
    private CandidateKey(int originalOrder, ZhangBoEvaluatedPddrSelector.Source source) {
      this.originalOrder = originalOrder;
      this.source = source;
    }
    static CandidateKey of(ZhangBoEvaluatedPddrSelector.Candidate candidate) {
      return new CandidateKey(candidate.getOriginalOrder(), candidate.getSource());
    }
    @Override public boolean equals(Object other) {
      if (!(other instanceof CandidateKey)) return false;
      CandidateKey value = (CandidateKey) other;
      return originalOrder == value.originalOrder && source == value.source;
    }
    @Override public int hashCode() { return 31 * originalOrder + source.hashCode(); }
  }
}
