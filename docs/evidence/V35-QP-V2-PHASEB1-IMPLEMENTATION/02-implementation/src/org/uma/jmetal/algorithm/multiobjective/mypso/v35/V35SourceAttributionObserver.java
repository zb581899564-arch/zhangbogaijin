package org.uma.jmetal.algorithm.multiobjective.mypso.v35;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.uma.jmetal.algorithm.multiobjective.mypso.zhangbo.ZhangBoEvaluatedPddrSelector;
import org.uma.jmetal.algorithm.multiobjective.mypso.zhangbo.ZhangBoLineageTag;
import org.uma.jmetal.algorithm.multiobjective.mypso.zhangbo.ZhangBoQgController;
import org.uma.jmetal.problem.multiobjective.dfsp.fatigue.ZhangBoCanonicalProductionProblem;
import org.uma.jmetal.solution.PermutationSolution;

/**
 * V35-SOURCE-ATTRIBUTION Observer V5 (schema correction, 2026-09-01).
 * Frozen schema: docs/evidence/V35-SOURCE-ATTRIBUTION-500K/00-preregistration/
 * observer-schema.md (bounded streaming; fail-closed; four first-level sources;
 * no x25 memory model; event rows == successfulDecoderCalls).
 *
 * Pure observation: never enters the search archive, never changes PDDR input
 * or teacher selection, never consumes randomness, never evaluates, never
 * mutates candidates, never reads wall clock into any output ordering.
 */
public final class V35SourceAttributionObserver {

  public static final String SCHEMA_VERSION = "v35-source-attribution-observer-schema-v2";
  public static final long NOMINAL_WINDOW_FE = 25000L;
  public static final int ND_SAMPLE_CAPACITY_PER_SOURCE = 512;
  public static final int FORENSIC_RESERVOIR_CAPACITY = 256;
  public static final int LINEAGE_INDEX_CAPACITY = 4096;
  public static final int PARENT_RAW_CACHE_CAPACITY = 512;
  public static final int MAX_ROWS_BEFORE_FLUSH = 25000;
  public static final int WORST_CASE_BYTES_PER_ROW_RESIDENT = 1024;

  private static final String[] FIRST_LEVEL = {"GLOBAL_CFVF", "CATA",
      "INHERITED_LS", "PARENT_CARRYOVER"};

  private static boolean armed = false;
  private static String runId = "", instance = "", seed = "", arm = "";
  private static long errorCount = 0L;
  private static String lastError = "";
  private static long droppedEvents = 0L;
  private static long unknownSourceEvents = 0L;
  private static long invalidObjectiveRows = 0L;
  private static long duplicateCandidateEventRows = 0L;
  private static long boundedCapacityViolations = 0L;

  private static ZhangBoCanonicalProductionProblem problem = null;

  // current round context (updated by the shadowed algorithm at round/cycle boundaries)
  private static long ctxGeneration = 0L;
  private static long ctxOuterCycle = 0L;
  private static long ctxQRound = -1L;
  private static long currentActualFE = 0L;
  // current Qg selection context per group (group name -> [actionOrdinal, teacherHash16])
  private static final Map<String, String[]> ctxQg = new LinkedHashMap<String, String[]>();
  // current Qp context per branch (branchId -> [actionOrdinal, teacherHash16])
  private static final Map<Long, String[]> ctxQp = new HashMap<Long, String[]>();

  // bounded lineage index: lineageId -> fingerprint(SHA hex); FIFO beyond capacity
  private static final Map<Long, String> lineageIndex = new LinkedHashMap<Long, String>();
  // bounded parent raw-fingerprint cache: lineageId -> raw four-vector text (for vector diffs)
  private static final Map<Long, String> parentRawCache = new LinkedHashMap<Long, String>();
  private static final Map<Long, double[]> parentObjectiveCache =
      new LinkedHashMap<Long, double[]>();
  // lifecycle registries (bounded): fingerprintHex -> flag
  private static final Map<String, Boolean> usedAsQgTeacher = new HashMap<String, Boolean>();
  private static final Map<String, Boolean> usedAsQpTeacher = new HashMap<String, Boolean>();
  private static final Map<String, Boolean> enteredPersonalArchive = new HashMap<String, Boolean>();
  private static final Map<String, Boolean> inPersonalArchiveNow = new HashMap<String, Boolean>();

  // streaming ledger state
  private static final StringBuilder eventBuffer = new StringBuilder();
  private static long bufferedRows = 0L;
  private static long totalEventRows = 0L;
  // TRUE STREAMING (acceptance correction 2026-09-01): flushed rows go to a
  // temp file on disk, NOT back into memory.  Memory holds only the bounded
  // unflushed buffer (maxRowsBeforeFlush x worstCaseBytesPerRowResident).
  private static java.io.BufferedWriter ledgerWriter = null;
  private static java.io.File ledgerTempFile = null;
  private static long flushCount = 0L;
  // per-window streaming digest chain
  private static java.security.MessageDigest windowDigest = null;
  private static final List<String> windowDigestChain = new ArrayList<String>();

  // pddr round ledger (streamed like events; small rows)
  private static java.io.BufferedWriter pddrWriter = null;
  private static java.io.File pddrTempFile = null;
  private static long pddrRounds = 0L;
  // Lifecycle is an append-only event stream.  Later events cannot safely
  // rewrite already-flushed evaluation rows, so the offline join is keyed by
  // subjectFingerprint and actualFE.
  private static java.io.BufferedWriter lifecycleWriter = null;
  private static java.io.File lifecycleTempFile = null;
  private static long lifecycleRows = 0L;

  // bounded per-window ND sample / forensic reservoir (hex fp + objectives)
  private static final Map<String, List<double[]>> ndSample = new HashMap<String, List<double[]>>();
  private static long ndSampleDropped = 0L;
  private static final List<String[]> forensicReservoir = new ArrayList<String[]>();
  private static long forensicDropped = 0L;
  private static java.util.Random reservoirRandom = null;

  // working-population front (last PDDR round selected objectives, for checkpoints)
  private static final List<double[]> workingPopulationFront = new ArrayList<double[]>();

  // checkpoint captures: [targetFE][kind] -> csv body (decision/observed/working)
  private static final Map<Long, Map<String, String>> checkpointFronts =
      new LinkedHashMap<Long, Map<String, String>>();
  private static long b0Captured = 0L;
  private static final List<double[]> initialObjectiveRows = new ArrayList<double[]>();

  private V35SourceAttributionObserver() { }

  public static void arm(String runIdIn, String instanceIn, String seedIn, String armIn) {
    disarm();
    armed = true;
    runId = runIdIn;
    instance = instanceIn;
    seed = seedIn;
    arm = armIn;
  }

  public static void disarm() {
    armed = false;
    errorCount = 0L; lastError = ""; droppedEvents = 0L; unknownSourceEvents = 0L;
    invalidObjectiveRows = 0L; duplicateCandidateEventRows = 0L;
    boundedCapacityViolations = 0L;
    ctxGeneration = 0L; ctxOuterCycle = 0L; ctxQRound = -1L; currentActualFE = 0L;
    ctxQg.clear(); ctxQp.clear();
    lineageIndex.clear(); parentRawCache.clear(); parentObjectiveCache.clear();
    usedAsQgTeacher.clear(); usedAsQpTeacher.clear(); enteredPersonalArchive.clear();
    inPersonalArchiveNow.clear();
    eventBuffer.setLength(0); bufferedRows = 0L; totalEventRows = 0L;
    closeLedgerWriters();
    flushCount = 0L;
    pddrRounds = 0L; lifecycleRows = 0L;
    ndSample.clear(); ndSampleDropped = 0L;
    forensicReservoir.clear(); forensicDropped = 0L; reservoirRandom = null;
    workingPopulationFront.clear();
    checkpointFronts.clear(); b0Captured = 0L; initialObjectiveRows.clear();
    windowDigest = null; windowDigestChain.clear();
  }

  public static boolean isArmed() { return armed; }

  /** Called by the shadowed runner after problem construction. */
  public static void attach(ZhangBoCanonicalProductionProblem canonical) {
    if (!armed) return;
    problem = canonical;
  }

  /** Context updates from the shadowed algorithm (round/cycle boundaries). */
  public static void context(long generation, long outerCycle, long qRound) {
    if (!armed) return;
    ctxGeneration = generation;
    ctxOuterCycle = outerCycle;
    ctxQRound = qRound;
  }

  /** Qg selection context (called by the shadow at pendingQgSelections.put). */
  public static void onQgSelection(String groupName, int actionOrdinal,
      PermutationSolution<Integer> leader) {
    if (!armed) return;
    try {
      String teacherHash = sha16(fingerprintRaw(leader));
      ctxQg.put(groupName, new String[]{String.valueOf(actionOrdinal), teacherHash});
      String fullTeacherHash = sha256(fingerprintRaw(leader));
      usedAsQgTeacher.put(fullTeacherHash, Boolean.TRUE);
      lifecycle("QG_TEACHER", fullTeacherHash, "-1", "NOT_APPLICABLE",
          String.valueOf(actionOrdinal), groupName);
    } catch (RuntimeException error) {
      fail(error.toString());
    }
  }

  /** Qp selection context (called by the shadow when pendingQpSelections is filled). */
  public static void onQpSelections(java.util.List<PermutationSolution<Integer>> children) {
    if (!armed) return;
    // per-child Qp teacher context is recorded by the shadowed QpController at
    // settle time (onPersonalArchiveUpdate); here only the round marker is needed.
  }

  /** Personal-archive updates (called by the shadowed ZhangBoQpController.settle). */
  public static void onPersonalArchiveUpdate(String childRawFingerprint,
      String qpTeacherRawFingerprint, boolean insertedEntrySurvived, int actionOrdinal) {
    if (!armed) return;
    try {
      String childHash = sha256(childRawFingerprint);
      String teacherHash = sha256(qpTeacherRawFingerprint);
      enteredPersonalArchive.put(childHash, Boolean.valueOf(insertedEntrySurvived));
      inPersonalArchiveNow.put(childHash, Boolean.TRUE);
      usedAsQpTeacher.put(teacherHash, Boolean.TRUE);
      ctxQp.put(System.identityHashCode(childHash) * 0L + actionOrdinal,
          new String[]{String.valueOf(actionOrdinal), sha16(qpTeacherRawFingerprint)});
      lifecycle("QP_ACTION", childHash, teacherHash, "NOT_APPLICABLE",
          String.valueOf(actionOrdinal), "selection");
      lifecycle("QP_TEACHER", teacherHash, childHash, "NOT_APPLICABLE",
          String.valueOf(actionOrdinal), "selectedPbest");
      if (insertedEntrySurvived) {
        lifecycle("PERSONAL_ARCHIVE", childHash, "-1", "NOT_APPLICABLE",
            String.valueOf(actionOrdinal), "insertedEntrySurvived");
      }
    } catch (RuntimeException error) {
      fail(error.toString());
    }
  }

  /**
   * Candidate event (called by the shadowed passive archive after every
   * successful evaluation; observedCount == actualFE position).  One row per
   * successful decoder call; events are NEVER folded by objective triple here.
   */
  public static void onEvaluated(PermutationSolution<Integer> solution,
      V35EvaluationSourceContext.Source source, long observedCount) {
    if (!armed) return;
    try {
      String rawFp = fingerprintRaw(solution);
      String fp = sha256(rawFp);
      currentActualFE = observedCount;
      long nominalFE = nominalWindow(observedCount);
      String rawSource = source == null ? "UNSET" : source.name();
      if (source == null) unknownSourceEvents++;
      String firstLevel = firstLevel(rawSource);
      boolean attributionEligible = !"NOT_APPLICABLE".equals(firstLevel);
      long lineageId = -1L, parentLineageId = -1L;
      String parentFingerprint = "-1";
      Object tag = solution.getAttribute(ZhangBoLineageTag.class);
      if (tag instanceof ZhangBoLineageTag) {
        ZhangBoLineageTag t = (ZhangBoLineageTag) tag;
        lineageId = t.getLineageId();
        parentLineageId = t.getParentLineageId();
        String parent = lineageIndex.get(parentLineageId);
        if (parent != null) parentFingerprint = parent;
      }
      Object swarm = solution.getAttribute(
          org.uma.jmetal.algorithm.multiobjective.mypso.zhangbo.ZhangBoSubSwarm.class);
      String subSwarmRole = swarm == null ? "UNASSIGNED" : swarm.toString();
      // vector-change vs parent raw text (bounded cache)
      String parentRaw = parentLineageId >= 0 ? parentRawCache.get(parentLineageId) : null;
      boolean[] changed = new boolean[4];
      long[] counts = new long[4];
      vectorDiff(rawFp, parentRaw, changed, counts);
      // Qg/Qp context stamped by the candidate's group role
      String[] qg = ctxQg.get(subSwarmRole);
      String qgAction = qg == null ? "NA" : qg[0];
      String qgTeacher = qg == null ? "NA" : qg[1];
      String qpAction = "EVENT_LEDGER";
      String qpTeacher = "NA";
      Boolean qpT = usedAsQpTeacher.get(fp);
      if (qpT != null) qpTeacher = "SELF_OR_PA";
      appendEventRow(observedCount, nominalFE, ctxGeneration, ctxOuterCycle, ctxQRound,
          rawSource, firstLevel,
          attributionEligible ? "true" : "false", fp,
          solution.getObjective(0), solution.getObjective(1), solution.getObjective(6),
          parentFingerprint, lineageId, parentLineageId, qgAction, qpAction,
          qgTeacher, qpTeacher, subSwarmRole,
          changed[0], changed[1], changed[2], changed[3],
          counts[0], counts[1], counts[2], counts[3],
          "FINAL_EVALUATE".equals(rawSource) ? "true" : "false");
      lifecycle("GENERATED", fp, parentFingerprint, firstLevel, "NA", rawSource);
      if (!"-1".equals(parentFingerprint)) {
        lifecycle("DESCENDANT", parentFingerprint, fp, firstLevel, "NA", rawSource);
        double[] parentObjectives = parentObjectiveCache.get(parentLineageId);
        double[] childObjectives = new double[]{solution.getObjective(0),
            solution.getObjective(1), solution.getObjective(6)};
        if (parentObjectives != null && strictlyDominates(childObjectives, parentObjectives)) {
          lifecycle("IMPROVING_DESCENDANT", parentFingerprint, fp, firstLevel,
              "NA", "strictThreeObjectiveDominance");
        }
      }
      // lineage index feed (bounded FIFO)
      if (lineageId >= 0) {
        lineageIndex.put(lineageId, fp);
        parentRawCache.put(lineageId, rawFp);
        parentObjectiveCache.put(lineageId, new double[]{solution.getObjective(0),
            solution.getObjective(1), solution.getObjective(6)});
        while (lineageIndex.size() > LINEAGE_INDEX_CAPACITY) {
          java.util.Iterator<Long> it = lineageIndex.keySet().iterator();
          if (it.hasNext()) { it.next(); it.remove(); } else break;
        }
        while (parentRawCache.size() > PARENT_RAW_CACHE_CAPACITY) {
          java.util.Iterator<Long> it = parentRawCache.keySet().iterator();
          if (it.hasNext()) { it.next(); it.remove(); } else break;
        }
        while (parentObjectiveCache.size() > PARENT_RAW_CACHE_CAPACITY) {
          java.util.Iterator<Long> it = parentObjectiveCache.keySet().iterator();
          if (it.hasNext()) { it.next(); it.remove(); } else break;
        }
      }
      if ("INITIAL_POPULATION".equals(rawSource) && initialObjectiveRows.size() < 100) {
        initialObjectiveRows.add(new double[]{solution.getObjective(0),
            solution.getObjective(1), solution.getObjective(6)});
      }
      // teacher/PA registry bounded FIFO (cap 4096 each)
      boundRegistry(usedAsQgTeacher);
      boundRegistry(usedAsQpTeacher);
      boundRegistry(enteredPersonalArchive);
      boundRegistry(inPersonalArchiveNow);
      // ND sample / forensic reservoir (bounded, observer-owned deterministic RNG)
      if (attributionEligible) {
        List<double[]> sample = ndSample.get(firstLevel);
        if (sample == null) {
          sample = new ArrayList<double[]>();
          ndSample.put(firstLevel, sample);
        }
        if (sample.size() < ND_SAMPLE_CAPACITY_PER_SOURCE) {
          sample.add(new double[]{solution.getObjective(0),
              solution.getObjective(1), solution.getObjective(2 == 1 ? 1 : 6)});
        } else {
          ndSampleDropped++;
        }
        if (forensicReservoir.size() < FORENSIC_RESERVOIR_CAPACITY) {
          forensicReservoir.add(new String[]{fp,
              String.valueOf(solution.getObjective(0)),
              String.valueOf(solution.getObjective(1)),
              String.valueOf(solution.getObjective(6)), rawSource,
              String.valueOf(observedCount)});
        } else {
          if (reservoirRandom == null) {
            reservoirRandom = new java.util.Random(0x0B5B1L ^ runId.hashCode());
          }
          int slot = reservoirRandom.nextInt(FORENSIC_RESERVOIR_CAPACITY);
          if (slot < FORENSIC_RESERVOIR_CAPACITY) {
            forensicReservoir.set(slot, new String[]{fp,
                String.valueOf(solution.getObjective(0)),
                String.valueOf(solution.getObjective(1)),
                String.valueOf(solution.getObjective(6)), rawSource,
                String.valueOf(observedCount)});
          }
          forensicDropped++;
        }
      }
      // window/streaming digest
      if (windowDigest == null) {
        try { windowDigest = java.security.MessageDigest.getInstance("SHA-256"); } catch (java.security.NoSuchAlgorithmException e) { fail(e.toString()); return; }
      }
      windowDigest.update((observedCount + "|" + rawSource + "|" + fp + "\n")
          .getBytes(java.nio.charset.StandardCharsets.UTF_8));
      // B_0: after the initial population completes (first 100 events).
      // B0 = decision front of the initial population = ND of the first 100
      // objective triples. Captured inline (no external front provider needed).
      if (observedCount == 100L && b0Captured == 0L) {
        List<double[]> b0Points = strictNondominated(initialObjectiveRows);
        Map<String, String> entry = new LinkedHashMap<String, String>();
        entry.put("decision-front", frontCsv(b0Points));
        entry.put("observed-full-front", frontCsv(b0Points));
        entry.put("working-population-front", frontCsv(b0Points));
        checkpointFronts.put(0L, entry);
        b0Captured = 1L;
      }
      // buffered flush at capacity
      if (bufferedRows >= MAX_ROWS_BEFORE_FLUSH) {
        flushEventBuffer();
      }
    } catch (RuntimeException error) {
      fail(error.toString());
    }
  }

  /** Merge-pool + PDDR selection observation (called by the shadow algorithm). */
  public static void onPddrRound(List<PermutationSolution<Integer>> poolSolutions,
      List<String> poolSourceNames, List<ZhangBoEvaluatedPddrSelector.Candidate> selected,
      long fe, int outerCycle) {
    if (!armed) return;
    try {
      pddrRounds++;
      Map<String, java.util.ArrayDeque<double[]>> selByFp =
          new HashMap<String, java.util.ArrayDeque<double[]>>();
      workingPopulationFront.clear();
      for (int rank = 0; rank < selected.size(); rank++) {
        ZhangBoEvaluatedPddrSelector.Candidate c = selected.get(rank);
        String raw = fingerprintRaw(c.getSolution());
        String fp = sha256(raw);
        java.util.ArrayDeque<double[]> q = selByFp.get(fp);
        if (q == null) { q = new java.util.ArrayDeque<double[]>(); selByFp.put(fp, q); }
        q.add(new double[]{rank + 1, c.getPddrScore()});
        workingPopulationFront.add(new double[]{c.getSolution().getObjective(0),
            c.getSolution().getObjective(1), c.getSolution().getObjective(6)});
      }
      for (int index = 0; index < poolSolutions.size(); index++) {
        String raw = fingerprintRaw(poolSolutions.get(index));
        String fp = sha256(raw);
        java.util.ArrayDeque<double[]> q = selByFp.get(fp);
        boolean sel = q != null && !q.isEmpty();
        double rank = -1, score = Double.NaN;
        if (sel) { double[] hit = q.poll(); rank = hit[0]; score = hit[1]; }
        if (pddrWriter != null) {
          pddrWriter.write(outerCycle + "," + fe + "," + (index + 1) + ","
              + poolSourceNames.get(index) + "," + fp + ",true," + sel + ","
              + rank + ","
              + (Double.isNaN(score) ? "NOT_EXPORTED_AT_POOL_LEVEL" : score));
          pddrWriter.write("\n");
        }
        String selectorSource = poolSourceNames.get(index);
        String lifecycleSource = "PARENT".equals(selectorSource)
            ? "PARENT_CARRYOVER" : "NOT_APPLICABLE";
        lifecycle("MERGE_POOL", fp, "-1", lifecycleSource, "NA", selectorSource);
        if (sel) {
          lifecycle("PDDR_SELECTED", fp, "-1", lifecycleSource, "NA",
              "rank=" + (long) rank);
          lifecycle("WORKING_POPULATION", fp, "-1", lifecycleSource, "NA",
              "rank=" + (long) rank);
        }
      }
      int leftover = 0;
      for (java.util.ArrayDeque<double[]> q : selByFp.values()) leftover += q.size();
      if (leftover != 0) fail("selectedRowsNotMatchedToPool=" + leftover);
    } catch (RuntimeException | java.io.IOException error) {
      fail(error.toString());
    }
  }

  /** Checkpoint front capture (called by the shadowed runner hook or archive). */
  /** Snapshot of the current observed (passive archive) state for checkpoints. */
  public static void captureCheckpointFromArchive(long targetFE, String checkpointKind,
      List<double[]> decisionFront, List<double[]> observedFront) {
    captureCheckpoint(targetFE, checkpointKind, decisionFront, observedFront);
  }

  public static void captureFront(long targetFE, String kind) {
    // Compatibility no-op: real front capture goes through captureCheckpoint()
    // which is called by the runner with the algorithm result fronts.
  }

  /** Terminal/window flush + checkpoint capture with the given fronts. */
  public static void captureCheckpoint(long targetFE, String checkpointKind,
      List<double[]> decisionFront, List<double[]> observedFront) {
    if (!armed) return;
    try {
      Map<String, String> entry = checkpointFronts.get(targetFE);
      if (entry == null) {
        entry = new LinkedHashMap<String, String>();
        checkpointFronts.put(targetFE, entry);
      }
      entry.put("decision-front", frontCsv(decisionFront));
      entry.put("observed-full-front", frontCsv(observedFront));
      entry.put("working-population-front", frontCsv(new ArrayList<double[]>(
          workingPopulationFront)));
    } catch (RuntimeException error) {
      fail(error.toString());
    }
  }

  public static void flushEventBuffer() {
    if (bufferedRows == 0L) return;
    String digest = windowDigest == null ? "NONE"
        : hex(windowDigest.digest());
    windowDigestChain.add("flush" + flushCount + "=" + digest);
    try {
      if (ledgerWriter == null) {
        ledgerTempFile = java.io.File.createTempFile("v35-source-ledger-", ".csv");
        ledgerTempFile.deleteOnExit();
        ledgerWriter = new java.io.BufferedWriter(
            new java.io.OutputStreamWriter(
                new java.io.FileOutputStream(ledgerTempFile),
                java.nio.charset.StandardCharsets.UTF_8), 1 << 16);
        ledgerWriter.write(eventHeader());
      }
      ledgerWriter.write(eventBuffer.toString());
      ledgerWriter.flush();
    } catch (java.io.IOException error) {
      fail("ledgerWriteError:" + error.toString());
    }
    eventBuffer.setLength(0);
    bufferedRows = 0L;
    flushCount++;
    if (windowDigest != null) {
      // chain: next window digest seeded from previous
      windowDigest.update(digest.getBytes(java.nio.charset.StandardCharsets.UTF_8));
    }
  }

  private static void appendEventRow(Object... fields) {
    for (int index = 0; index < fields.length; index++) {
      if (index > 0) eventBuffer.append(',');
      eventBuffer.append(fields[index]);
    }
    eventBuffer.append('\n');
    bufferedRows++;
    totalEventRows++;
  }

  private static void boundRegistry(Map<String, Boolean> registry) {
    while (registry.size() > LINEAGE_INDEX_CAPACITY) {
      java.util.Iterator<String> it = registry.keySet().iterator();
      if (it.hasNext()) { it.next(); it.remove(); } else break;
    }
  }

  private static String firstLevel(String rawSource) {
    if ("INITIAL_POPULATION".equals(rawSource)) return "NOT_APPLICABLE";
    if ("GLOBAL_CFVF".equals(rawSource) || "FINAL_EVALUATE".equals(rawSource)) return "GLOBAL_CFVF";
    if ("CATA_TEST".equals(rawSource) || "CATA_APPLY".equals(rawSource)) return "CATA";
    if ("INTER_FACTORY_LS".equals(rawSource) || "INTRA_FACTORY_VNS".equals(rawSource)) return "INHERITED_LS";
    if ("PARENT_CARRYOVER".equals(rawSource)) return "PARENT_CARRYOVER";
    unknownSourceEvents++;
    return "UNSET";
  }

  private static void vectorDiff(String childRaw, String parentRaw,
      boolean[] changed, long[] counts) {
    Arrays.fill(changed, false);
    Arrays.fill(counts, 0L);
    if (parentRaw == null) return; // NOT_OBSERVABLE for this row (no parent text cached)
    String[] a = childRaw.split("\\|");
    String[] b = parentRaw.split("\\|");
    for (int v = 0; v < 4 && v < a.length && v < b.length; v++) {
      String[] av = a[v].split(",");
      String[] bv = b[v].split(",");
      int n = Math.min(av.length, bv.length);
      long diff = 0L;
      for (int i = 0; i < n; i++) {
        if (!av[i].trim().equals(bv[i].trim())) diff++;
      }
      if (diff > 0) changed[v] = true;
      counts[v] = diff;
    }
  }

  private static String fingerprintRaw(PermutationSolution<Integer> solution) {
    return ZhangBoQgController.fingerprint(solution);
  }

  private static String sha256(String raw) {
    try {
      byte[] bytes = java.security.MessageDigest.getInstance("SHA-256")
          .digest(raw.getBytes(java.nio.charset.StandardCharsets.UTF_8));
      StringBuilder out = new StringBuilder();
      for (byte value : bytes) out.append(String.format("%02x", value & 0xff));
      return out.toString();
    } catch (java.security.NoSuchAlgorithmException error) {
      throw new IllegalStateException(error);
    }
  }

  private static String sha16(String raw) {
    return sha256(raw).substring(0, 16);
  }

  private static String hex(byte[] bytes) {
    StringBuilder out = new StringBuilder();
    for (byte value : bytes) out.append(String.format("%02x", value & 0xff));
    return out.toString();
  }

  private static String frontCsv(List<double[]> front) {
    StringBuilder out = new StringBuilder("Cmax,TEC,TWC\n");
    for (double[] pnt : front) {
      out.append(pnt[0]).append(',').append(pnt[1]).append(',').append(pnt[2]).append('\n');
    }
    return out.toString();
  }

  private static String eventHeader() {
    return "actualFE,nominalFE,generation,outerCycle,qRound,rawSource,firstLevelSource,attributionEligible,candidateFingerprint,"
        + "Cmax,TEC,TWC,parentFingerprint,lineageId,parentLineageId,QgAction,QpAction,"
        + "QgTeacherHash16,QpTeacherHash16,subSwarmRole,JSChanged,FAChanged,MAChanged,"
        + "WAChanged,JSChangeCount,FAChangeCount,MAChangeCount,WAChangeCount,finalEvaluate\n";
  }

  private static String pddrHeader() {
    return "outerCycle,fe,poolIndex,selectorSource,candidateFingerprint,enteredMergePool,"
        + "selectedByPddr,selectedRank,pddrScore\n";
  }

  private static String lifecycleHeader() {
    return "actualFE,nominalFE,outerCycle,qRound,eventType,subjectFingerprint,"
        + "relatedFingerprint,source,action,detail\n";
  }

  private static long nominalWindow(long actualFE) {
    if (actualFE <= 0L) return 0L;
    return ((actualFE + NOMINAL_WINDOW_FE - 1L) / NOMINAL_WINDOW_FE)
        * NOMINAL_WINDOW_FE;
  }

  private static void lifecycle(String eventType, String subject, String related,
      String source, String action, String detail) {
    if (!armed || lifecycleWriter == null) return;
    try {
      lifecycleWriter.write(currentActualFE + "," + nominalWindow(currentActualFE) + ","
          + ctxOuterCycle + "," + ctxQRound + "," + clean(eventType) + ","
          + clean(subject) + "," + clean(related) + "," + clean(source) + ","
          + clean(action) + "," + clean(detail) + "\n");
      lifecycleRows++;
    } catch (java.io.IOException error) {
      fail("lifecycleWriteError:" + error.toString());
    }
  }

  private static String clean(String value) {
    if (value == null) return "";
    return value.replace(',', ';').replace('\n', ' ').replace('\r', ' ');
  }

  private static boolean strictlyDominates(double[] left, double[] right) {
    boolean strict = false;
    for (int index = 0; index < 3; index++) {
      if (left[index] > right[index] + 1.0e-12) return false;
      if (left[index] + 1.0e-12 < right[index]) strict = true;
    }
    return strict;
  }

  private static List<double[]> strictNondominated(List<double[]> input) {
    List<double[]> out = new ArrayList<double[]>();
    for (int i = 0; i < input.size(); i++) {
      double[] point = input.get(i);
      boolean duplicate = false;
      boolean dominated = false;
      for (int j = 0; j < input.size(); j++) {
        if (i == j) continue;
        double[] other = input.get(j);
        if (equalObjectives(point, other) && j < i) duplicate = true;
        if (strictlyDominates(other, point)) dominated = true;
        if (duplicate || dominated) break;
      }
      if (!duplicate && !dominated) out.add(new double[]{point[0], point[1], point[2]});
    }
    return out;
  }

  private static boolean equalObjectives(double[] left, double[] right) {
    for (int index = 0; index < 3; index++) {
      if (Math.abs(left[index] - right[index]) > 1.0e-12) return false;
    }
    return true;
  }

  private static void fail(String message) {
    errorCount++;
    lastError = message;
  }

  // ---- getters for the runner ----
  public static long getErrorCount() { return errorCount; }
  public static String getLastError() { return lastError; }
  public static long getDroppedEvents() { return droppedEvents; }
  public static long getUnknownSourceEvents() { return unknownSourceEvents; }
  public static long getInvalidObjectiveRows() { return invalidObjectiveRows; }
  public static long getDuplicateCandidateEventRows() { return duplicateCandidateEventRows; }
  public static long getBoundedCapacityViolations() { return boundedCapacityViolations; }
  public static long getTotalEventRows() { return totalEventRows; }
  public static long getPddrRounds() { return pddrRounds; }
  public static long getLifecycleRows() { return lifecycleRows; }
  public static long getB0Captured() { return b0Captured; }
  public static Map<Long, Map<String, String>> getCheckpointFronts() {
    return checkpointFronts;
  }
  public static String getEvaluationLedgerCsv() {
    return "STREAMED_TO_FILE:" + (ledgerTempFile == null ? "NONE"
        : ledgerTempFile.getAbsolutePath());
  }
  public static String getPddrLedgerCsv() {
    return "STREAMED_TO_FILE:" + (pddrTempFile == null ? "NONE"
        : pddrTempFile.getAbsolutePath());
  }
  public static java.io.File getLifecycleTempFile() { return lifecycleTempFile; }

  /** Opens the PDDR streaming writer (called by the runner at run start). */
  public static void openLedgerFiles(java.io.File parentDir) {
    if (!armed) return;
    try {
      pddrTempFile = java.io.File.createTempFile("v35-pddr-ledger-", ".csv",
          parentDir);
      pddrTempFile.deleteOnExit();
      pddrWriter = new java.io.BufferedWriter(
          new java.io.OutputStreamWriter(
              new java.io.FileOutputStream(pddrTempFile),
              java.nio.charset.StandardCharsets.UTF_8), 1 << 16);
      pddrWriter.write(pddrHeader());
      lifecycleTempFile = java.io.File.createTempFile("v35-source-lifecycle-", ".csv",
          parentDir);
      lifecycleTempFile.deleteOnExit();
      lifecycleWriter = new java.io.BufferedWriter(
          new java.io.OutputStreamWriter(
              new java.io.FileOutputStream(lifecycleTempFile),
              java.nio.charset.StandardCharsets.UTF_8), 1 << 16);
      lifecycleWriter.write(lifecycleHeader());
    } catch (java.io.IOException error) {
      fail("pddrOpenError:" + error.toString());
    }
  }

  /** Closes both streaming writers (flushes remaining buffer first). */
  public static void closeLedgerWriters() {
    if (bufferedRows > 0L) {
      flushEventBuffer();  // flush remaining unflushed rows to disk
    }
    try {
      if (ledgerWriter != null) { ledgerWriter.flush(); ledgerWriter.close(); ledgerWriter = null; }
    } catch (java.io.IOException error) { fail("ledgerCloseError:" + error.toString()); }
    try {
      if (pddrWriter != null) { pddrWriter.flush(); pddrWriter.close(); pddrWriter = null; }
    } catch (java.io.IOException error) { fail("pddrCloseError:" + error.toString()); }
    try {
      if (lifecycleWriter != null) {
        lifecycleWriter.flush(); lifecycleWriter.close(); lifecycleWriter = null;
      }
    } catch (java.io.IOException error) { fail("lifecycleCloseError:" + error.toString()); }
  }

  public static java.io.File getLedgerTempFile() { return ledgerTempFile; }
  public static java.io.File getPddrTempFile() { return pddrTempFile; }
  public static long nominalWindowForTest(long actualFE) { return nominalWindow(actualFE); }
  public static boolean strictlyDominatesForTest(double[] left, double[] right) {
    return strictlyDominates(left, right);
  }
  public static String eventHeaderForTest() { return eventHeader(); }
  public static String lifecycleHeaderForTest() { return lifecycleHeader(); }
  public static List<String> getWindowDigestChain() { return windowDigestChain; }
  public static String getCapacityConfigText() {
    return "ndSampleCapacityPerSource=" + ND_SAMPLE_CAPACITY_PER_SOURCE + "\n"
        + "forensicReservoirCapacity=" + FORENSIC_RESERVOIR_CAPACITY + "\n"
        + "lineageIndexCapacity=" + LINEAGE_INDEX_CAPACITY + "\n"
        + "parentRawCacheCapacity=" + PARENT_RAW_CACHE_CAPACITY + "\n"
        + "maxRowsBeforeFlush=" + MAX_ROWS_BEFORE_FLUSH + "\n"
        + "worstCaseBytesPerRowResident=" + WORST_CASE_BYTES_PER_ROW_RESIDENT + "\n"
        + "observerBoundedResidentCap=" + observerBoundedResidentCap() + "\n"
        + "observerUnflushedBufferCap=" + observerUnflushedBufferCap() + "\n";
  }
  public static long observerBoundedResidentCap() {
    // Conservative accounting for observer-owned bounded collections. The two
    // parent caches and the lifecycle writer buffer were added by schema v2 and
    // must not be hidden by reusing the v1 estimate.
    return 4L * ND_SAMPLE_CAPACITY_PER_SOURCE * 3 * 16L
        + FORENSIC_RESERVOIR_CAPACITY * 256L
        + LINEAGE_INDEX_CAPACITY * 96L
        + 4L * LINEAGE_INDEX_CAPACITY * 96L
        + PARENT_RAW_CACHE_CAPACITY * 256L
        + PARENT_RAW_CACHE_CAPACITY * 64L
        + (1L << 16);
  }
  public static long observerUnflushedBufferCap() {
    return (long) MAX_ROWS_BEFORE_FLUSH * WORST_CASE_BYTES_PER_ROW_RESIDENT;
  }
}
