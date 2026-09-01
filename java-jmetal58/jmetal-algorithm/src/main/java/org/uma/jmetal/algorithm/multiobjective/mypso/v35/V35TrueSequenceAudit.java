package org.uma.jmetal.algorithm.multiobjective.mypso.v35;

import java.nio.charset.StandardCharsets;
import java.lang.ref.ReferenceQueue;
import java.lang.ref.WeakReference;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.function.Consumer;
import org.uma.jmetal.algorithm.multiobjective.mypso.zhangbo.ZhangBoMachineVectorSupport;
import org.uma.jmetal.solution.PermutationSolution;
import org.uma.jmetal.util.pseudorandom.JMetalRandom;
import org.uma.jmetal.util.pseudorandom.PseudoRandomGenerator;
import org.uma.jmetal.util.pseudorandom.impl.AuditableRandomGenerator;

/**
 * Records actual random draws and actual newly-created candidates.
 *
 * <p>This class is deliberately an observation-only boundary. It does not
 * provide a replacement random value and it never participates in an
 * algorithm decision. Digests are updated as events arrive; individual
 * events are not retained in an unbounded list.</p>
 */
public final class V35TrueSequenceAudit {
  public static final String ACTUAL_RANDOM_DRAWS = "ACTUAL_RANDOM_DRAWS";
  public static final String RNG_AUDIT_UNAVAILABLE = "RNG_AUDIT_UNAVAILABLE";
  public static final String ACTUAL_PRE_EVALUATION_CANDIDATES =
      "ACTUAL_PRE_EVALUATION_CANDIDATES";
  public static final String CANDIDATE_AUDIT_UNAVAILABLE = "CANDIDATE_AUDIT_UNAVAILABLE";

  /** Reasons which may make a diagnostic field genuinely unobservable. */
  public enum UnobservableReason {
    NONE,
    DRAW_STREAM_UNAVAILABLE,
    SOURCE_UNOBSERVABLE,
    PARENT_SLOT_UNOBSERVABLE,
    PARENT_FINGERPRINT_UNOBSERVABLE,
    LINEAGE_ID_UNOBSERVABLE,
    MACHINE_VECTOR_UNOBSERVABLE
  }

  private static final ThreadLocal<V35TrueSequenceAudit> ACTIVE =
      new ThreadLocal<V35TrueSequenceAudit>();

  private final MessageDigest rngDigest;
  private final MessageDigest candidateDigest;
  /**
   * Identity-only duplicate guard which does not retain every evaluated
   * Solution for the whole run. A candidate that is eligible for duplicate
   * evaluation is still strongly referenced by the algorithm; once it is no
   * longer reachable, it cannot be evaluated again and its guard entry may be
   * reclaimed.
   */
  private final ReferenceQueue<PermutationSolution<Integer>> evaluatedCandidateQueue =
      new ReferenceQueue<PermutationSolution<Integer>>();
  private final Set<IdentityWeakReference> evaluatedCandidateObjects =
      new HashSet<IdentityWeakReference>();
  private final Map<AuditableRandomGenerator, Consumer<AuditableRandomGenerator.Audit>>
      generatorListeners =
      new IdentityHashMap<AuditableRandomGenerator, Consumer<AuditableRandomGenerator.Audit>>();

  private AuditableRandomGenerator globalAuditable;
  private PseudoRandomGenerator previousGlobalGenerator;
  private boolean installedGlobalWrapper;
  private boolean attached;
  private long rngAuditCount;
  private long candidateAuditCount;
  private String unavailableReason;
  private final Map<String, Long> candidateSourceCounts =
      new java.util.LinkedHashMap<String, Long>();

  public V35TrueSequenceAudit() {
    rngDigest = newDigest();
    candidateDigest = newDigest();
  }

  /** Returns the audit active on the current algorithm-construction thread. */
  public static V35TrueSequenceAudit activeOrNull() {
    return ACTIVE.get();
  }

  /**
   * Wraps a source which is created after the audit has been installed. When
   * no audit is active the original source is returned unchanged, preserving
   * OFF/search behaviour.
   */
  public static PseudoRandomGenerator wrapActive(
      PseudoRandomGenerator generator, String randomStreamId) {
    V35TrueSequenceAudit audit = ACTIVE.get();
    return audit == null ? generator : audit.wrapGenerator(generator, randomStreamId);
  }

  /** Wraps a java.util.Random source created after the audit was installed. */
  public static Random wrapActiveJavaRandom(Random generator, String randomStreamId) {
    V35TrueSequenceAudit audit = ACTIVE.get();
    return audit == null ? generator : audit.wrapJavaRandom(generator, randomStreamId);
  }

  /** Installs an auditable wrapper around the actual JMetal global source. */
  public synchronized void attachToJMetalRandom() {
    if (attached) {
      detachFromJMetalRandom();
    }

    JMetalRandom jmetal = JMetalRandom.getInstance();
    PseudoRandomGenerator base = jmetal.getRandomGenerator();
    try {
      previousGlobalGenerator = base;
      if (base instanceof AuditableRandomGenerator
          && "JMETAL_GLOBAL".equals(((AuditableRandomGenerator) base).getRandomStreamId())) {
        globalAuditable = (AuditableRandomGenerator) base;
        installedGlobalWrapper = false;
      } else {
        globalAuditable = new AuditableRandomGenerator(base, "JMETAL_GLOBAL");
        jmetal.setRandomGenerator(globalAuditable);
        installedGlobalWrapper = true;
      }
      addGeneratorListener(globalAuditable);
      attached = true;
      unavailableReason = null;
      ACTIVE.set(this);
    } catch (RuntimeException failure) {
      if (installedGlobalWrapper && previousGlobalGenerator != null) {
        jmetal.setRandomGenerator(previousGlobalGenerator);
      }
      globalAuditable = null;
      installedGlobalWrapper = false;
      previousGlobalGenerator = null;
      unavailableReason = UnobservableReason.DRAW_STREAM_UNAVAILABLE.name();
      throw new IllegalStateException(RNG_AUDIT_UNAVAILABLE, failure);
    }
  }

  /** Removes listeners and restores the pre-audit global source. */
  public synchronized void detachFromJMetalRandom() {
    for (Map.Entry<AuditableRandomGenerator, Consumer<AuditableRandomGenerator.Audit>> entry
        : generatorListeners.entrySet()) {
      entry.getKey().removeListener(entry.getValue());
    }
    generatorListeners.clear();

    if (installedGlobalWrapper && previousGlobalGenerator != null) {
      JMetalRandom jmetal = JMetalRandom.getInstance();
      if (jmetal.getRandomGenerator() == globalAuditable) {
        jmetal.setRandomGenerator(previousGlobalGenerator);
      }
    }
    globalAuditable = null;
    previousGlobalGenerator = null;
    installedGlobalWrapper = false;
    attached = false;
    if (ACTIVE.get() == this) {
      ACTIVE.remove();
    }
  }

  /**
   * Wraps a non-global PseudoRandomGenerator and gives it an explicit stream
   * identifier. The wrapper delegates every value-producing call unchanged.
   */
  public synchronized PseudoRandomGenerator wrapGenerator(
      PseudoRandomGenerator generator, String randomStreamId) {
    if (!attached) {
      return generator;
    }
    if (generator instanceof AuditableRandomGenerator) {
      AuditableRandomGenerator existing = (AuditableRandomGenerator) generator;
      if (randomStreamId.equals(existing.getRandomStreamId())) {
        addGeneratorListener(existing);
        return existing;
      }
      generator = existing.getDelegate();
    }
    AuditableRandomGenerator wrapped =
        new AuditableRandomGenerator(generator, requireStreamId(randomStreamId));
    addGeneratorListener(wrapped);
    return wrapped;
  }

  /** Wraps a java.util.Random without changing its generated values. */
  public synchronized Random wrapJavaRandom(Random generator, String randomStreamId) {
    if (!attached) {
      return generator;
    }
    return new AuditableJavaRandom(generator, requireStreamId(randomStreamId), this);
  }

  private String requireStreamId(String randomStreamId) {
    if (randomStreamId == null || randomStreamId.length() == 0) {
      throw new IllegalArgumentException("No random stream id provided");
    }
    return randomStreamId;
  }

  private synchronized void addGeneratorListener(AuditableRandomGenerator generator) {
    if (generatorListeners.containsKey(generator)) {
      return;
    }
    Consumer<AuditableRandomGenerator.Audit> listener =
        new Consumer<AuditableRandomGenerator.Audit>() {
          @Override
          public void accept(AuditableRandomGenerator.Audit audit) {
            recordRandomAudit(audit);
          }
        };
    generator.addListener(listener);
    generatorListeners.put(generator, listener);
  }

  private synchronized void recordRandomAudit(AuditableRandomGenerator.Audit audit) {
    if (audit == null) {
      unavailableReason = UnobservableReason.DRAW_STREAM_UNAVAILABLE.name();
      throw new IllegalStateException(RNG_AUDIT_UNAVAILABLE + ":null-audit");
    }
    long ordinal = ++rngAuditCount;
    StringBuilder line = new StringBuilder(128);
    appendField(line, Long.toString(ordinal));
    appendField(line, audit.getRandomStreamId());
    appendField(line, audit.getMethod().name());
    if (audit.getBounds().isPresent()) {
      appendField(line, stableNumber(audit.getBounds().get().getLowerBound()));
      appendField(line, stableNumber(audit.getBounds().get().getUpperBound()));
    } else {
      // PseudoRandomGenerator.nextDouble() has the stable [0, 1) domain.
      appendField(line, stableNumber(Double.valueOf(0.0d)));
      appendField(line, stableNumber(Double.valueOf(1.0d)));
    }
    appendField(line, stableNumber(audit.getResult()));
    update(rngDigest, line);
  }

  private synchronized void recordJavaDraw(
      String streamId, String method, Number lowerBound, Number upperBound, Number result) {
    long ordinal = ++rngAuditCount;
    StringBuilder line = new StringBuilder(128);
    appendField(line, Long.toString(ordinal));
    appendField(line, streamId);
    appendField(line, method);
    appendField(line, lowerBound == null ? "NA" : stableNumber(lowerBound));
    appendField(line, upperBound == null ? "NA" : stableNumber(upperBound));
    appendField(line, stableNumber(result));
    update(rngDigest, line);
  }

  /**
   * Records a candidate immediately before its first complete evaluation.
   * Parent metadata is nullable only when the corresponding field is truly
   * unavailable; the reason is emitted explicitly in that case.
   */
  public synchronized void recordCandidateBeforeEvaluation(
      PermutationSolution<Integer> solution,
      String source,
      long feBeforeEvaluation,
      int parentSlot,
      String parentFingerprint,
      Long lineageId,
      UnobservableReason metadataReason) {
    recordCandidateBeforeEvaluation(solution, source, feBeforeEvaluation, -1,
        parentSlot, parentFingerprint, lineageId, metadataReason);
  }

  /** Records the full pre-evaluation identity including the real generation. */
  public synchronized void recordCandidateBeforeEvaluation(
      PermutationSolution<Integer> solution,
      String source,
      long feBeforeEvaluation,
      int generation,
      int parentSlot,
      String parentFingerprint,
      Long lineageId,
      UnobservableReason metadataReason) {
    if (solution == null) {
      throw new IllegalArgumentException("Candidate solution is required");
    }
    if (source == null || source.length() == 0) {
      throw new IllegalStateException("SOURCE_UNOBSERVABLE");
    }

    // Candidate identity is a real evaluation-event id, assigned at the
    // actual pre-evaluation boundary. It is diagnostic metadata only and is
    // never read by a search decision.
    solution.setAttribute("candidateId", "EVAL-" + (candidateAuditCount + 1L));

    VectorSnapshot vectors;
    try {
      vectors = snapshot(solution);
    } catch (RuntimeException failure) {
      unavailableReason = UnobservableReason.MACHINE_VECTOR_UNOBSERVABLE.name();
      throw new IllegalStateException(
          CANDIDATE_AUDIT_UNAVAILABLE + ":" + UnobservableReason.MACHINE_VECTOR_UNOBSERVABLE,
          failure);
    }

    String candidateFingerprint = sha256(vectors.payload);
    purgeCollectedCandidateReferences();
    if (!evaluatedCandidateObjects.add(
        new IdentityWeakReference(solution, evaluatedCandidateQueue))) {
      throw new IllegalStateException("DUPLICATE_CANDIDATE_EVALUATION:" + candidateFingerprint);
    }

    UnobservableReason reason = metadataReason == null ? UnobservableReason.NONE : metadataReason;
    StringBuilder line = new StringBuilder(512);
    appendField(line, Long.toString(candidateAuditCount + 1L));
    appendField(line, Integer.toString(generation));
    appendField(line, Long.toString(feBeforeEvaluation));
    appendField(line, source);
    appendField(line, parentSlot < 0 ? "NA" : Integer.toString(parentSlot));
    appendField(line, parentFingerprint == null ? "NA" : parentFingerprint);
    appendField(line, lineageId == null ? "NA" : Long.toString(lineageId.longValue()));
    appendField(line, vectors.jobSequence);
    appendField(line, vectors.factorySequence);
    appendField(line, vectors.machineSequence);
    appendField(line, vectors.workerSequence);
    appendField(line, candidateFingerprint);
    appendField(line, reason.name());
    update(candidateDigest, line);
    candidateAuditCount++;
    Long previous = candidateSourceCounts.get(source);
    candidateSourceCounts.put(source, previous == null ? 1L : previous + 1L);
  }

  /** Compatibility overload for existing callers; new code should provide all metadata. */
  public synchronized void recordCandidate(
      PermutationSolution<Integer> solution,
      String source,
      String parentFingerprint,
      long feBeforeEvaluation,
      int parentSlot) {
    recordCandidateBeforeEvaluation(
        solution,
        source,
        feBeforeEvaluation,
        parentSlot,
        parentFingerprint,
        null,
        parentFingerprint == null
            ? UnobservableReason.PARENT_FINGERPRINT_UNOBSERVABLE
            : UnobservableReason.LINEAGE_ID_UNOBSERVABLE);
  }

  /**
   * PDDR pools are post-evaluation observations and therefore are not a
   * candidate-generation hook. Keeping this method as a no-op preserves the
   * existing telemetry call without counting parents or duplicate evaluations.
   */
  public void recordPddrPool(
      List<PermutationSolution<Integer>> pool,
      List<org.uma.jmetal.algorithm.multiobjective.mypso.zhangbo.ZhangBoEvaluatedPddrSelector.Source>
          sources,
      long fe) {
    // Candidate events are recorded at the unique pre-evaluation sites.
  }

  public synchronized String rngSequenceHash() {
    return rngAuditCount == 0L ? RNG_AUDIT_UNAVAILABLE : digestHex(rngDigest);
  }

  public synchronized String rngHashSource() {
    return rngAuditCount == 0L ? RNG_AUDIT_UNAVAILABLE : ACTUAL_RANDOM_DRAWS;
  }

  public synchronized String candidateSequenceHash() {
    return candidateAuditCount == 0L ? CANDIDATE_AUDIT_UNAVAILABLE : digestHex(candidateDigest);
  }

  public synchronized String candidateHashSource() {
    return candidateAuditCount == 0L ? CANDIDATE_AUDIT_UNAVAILABLE
        : ACTUAL_PRE_EVALUATION_CANDIDATES;
  }

  public synchronized long getRngAuditCount() {
    return rngAuditCount;
  }

  public synchronized long getCandidateAuditCount() {
    return candidateAuditCount;
  }

  public synchronized int rngCount() {
    return toIntCount(rngAuditCount);
  }

  public synchronized int candidateCount() {
    return toIntCount(candidateAuditCount);
  }

  public synchronized long candidateSourceCount(String source) {
    Long value = candidateSourceCounts.get(source);
    return value == null ? 0L : value.longValue();
  }

  public synchronized String candidateSourceCountsText() {
    StringBuilder result = new StringBuilder();
    for (Map.Entry<String, Long> entry : candidateSourceCounts.entrySet()) {
      if (result.length() > 0) result.append(',');
      result.append(entry.getKey()).append('=').append(entry.getValue());
    }
    return result.toString();
  }

  public synchronized boolean hasSource(String source) {
    return candidateSourceCount(source) > 0L;
  }

  public synchronized String unavailableReason() {
    if (unavailableReason != null) {
      return unavailableReason;
    }
    return rngAuditCount == 0L
        ? UnobservableReason.DRAW_STREAM_UNAVAILABLE.name()
        : UnobservableReason.NONE.name();
  }

  /** Clears event digests and counters; it does not alter the installed source. */
  public synchronized void clear() {
    rngDigest.reset();
    candidateDigest.reset();
    evaluatedCandidateObjects.clear();
    rngAuditCount = 0L;
    candidateAuditCount = 0L;
    unavailableReason = null;
    candidateSourceCounts.clear();
  }

  private void purgeCollectedCandidateReferences() {
    IdentityWeakReference collected;
    while ((collected = (IdentityWeakReference) evaluatedCandidateQueue.poll()) != null) {
      evaluatedCandidateObjects.remove(collected);
    }
  }

  /** Computes a stable fingerprint from the actual JS/FA/MA/WA storage. */
  public static String stableSolutionFingerprint(PermutationSolution<Integer> solution) {
    return sha256(snapshot(solution).payload);
  }

  private static VectorSnapshot snapshot(PermutationSolution<Integer> solution) {
    if (solution == null) {
      throw new IllegalArgumentException("solution");
    }
    List<Integer> js = solution.getVariables();
    List<Integer> fa = solution.getVariablesid();
    List<Integer> ma = ZhangBoMachineVectorSupport.copy(solution, solution.getNumberOfVariables());
    List<Integer> wa = solution.getVariablesworker();
    String jobSequence = stableVector(js);
    String factorySequence = stableVector(fa);
    String machineSequence = stableVector(ma);
    String workerSequence = stableVector(wa);
    StringBuilder payload = new StringBuilder(256);
    appendField(payload, "JS");
    appendField(payload, jobSequence);
    appendField(payload, "FA");
    appendField(payload, factorySequence);
    appendField(payload, "MA");
    appendField(payload, machineSequence);
    appendField(payload, "WA");
    appendField(payload, workerSequence);
    return new VectorSnapshot(
        jobSequence, factorySequence, machineSequence, workerSequence, payload.toString());
  }

  private static String stableVector(List<?> values) {
    if (values == null) {
      throw new IllegalArgumentException("vector is null");
    }
    StringBuilder result = new StringBuilder(values.size() * 4 + 2);
    result.append('[');
    for (int index = 0; index < values.size(); index++) {
      if (index > 0) {
        result.append(',');
      }
      Object value = values.get(index);
      if (!(value instanceof Number)) {
        throw new IllegalArgumentException("vector contains non-numeric value at " + index);
      }
      result.append(stableNumber((Number) value));
    }
    result.append(']');
    return result.toString();
  }

  private static String stableNumber(Number value) {
    if (value == null) {
      throw new IllegalArgumentException("numeric value is null");
    }
    if (value instanceof Byte
        || value instanceof Short
        || value instanceof Integer
        || value instanceof Long) {
      return Long.toString(value.longValue());
    }
    if (value instanceof Float || value instanceof Double) {
      return Double.toHexString(value.doubleValue());
    }
    throw new IllegalArgumentException("Unsupported numeric type: " + value.getClass().getName());
  }

  private static void appendField(StringBuilder line, String value) {
    if (value == null) {
      line.append("-1:");
    } else {
      line.append(value.length()).append(':').append(value);
    }
    line.append('|');
  }

  private static void update(MessageDigest digest, StringBuilder line) {
    digest.update(line.toString().getBytes(StandardCharsets.UTF_8));
  }

  private static MessageDigest newDigest() {
    try {
      return MessageDigest.getInstance("SHA-256");
    } catch (NoSuchAlgorithmException failure) {
      throw new IllegalStateException("SHA-256 unavailable", failure);
    }
  }

  private static String sha256(String value) {
    MessageDigest digest = newDigest();
    digest.update(value.getBytes(StandardCharsets.UTF_8));
    return hex(digest.digest());
  }

  private static String digestHex(MessageDigest digest) {
    try {
      MessageDigest copy = (MessageDigest) digest.clone();
      return hex(copy.digest());
    } catch (CloneNotSupportedException failure) {
      throw new IllegalStateException("Digest snapshot unavailable", failure);
    }
  }

  private static String hex(byte[] bytes) {
    final char[] digits = "0123456789ABCDEF".toCharArray();
    char[] result = new char[bytes.length * 2];
    for (int index = 0; index < bytes.length; index++) {
      int value = bytes[index] & 0xFF;
      result[index * 2] = digits[value >>> 4];
      result[index * 2 + 1] = digits[value & 0x0F];
    }
    return new String(result);
  }

  private static int toIntCount(long count) {
    return count > Integer.MAX_VALUE ? Integer.MAX_VALUE : (int) count;
  }

  private static final class VectorSnapshot {
    private final String jobSequence;
    private final String factorySequence;
    private final String machineSequence;
    private final String workerSequence;
    private final String payload;

    private VectorSnapshot(
        String jobSequence,
        String factorySequence,
        String machineSequence,
        String workerSequence,
        String payload) {
      this.jobSequence = jobSequence;
      this.factorySequence = factorySequence;
      this.machineSequence = machineSequence;
      this.workerSequence = workerSequence;
      this.payload = payload;
    }
  }

  /** Weak reference with identity equality, independent of Solution.equals(). */
  private static final class IdentityWeakReference
      extends WeakReference<PermutationSolution<Integer>> {
    private final int identityHashCode;

    private IdentityWeakReference(PermutationSolution<Integer> referent,
        ReferenceQueue<PermutationSolution<Integer>> queue) {
      super(referent, queue);
      identityHashCode = System.identityHashCode(referent);
    }

    @Override
    public int hashCode() {
      return identityHashCode;
    }

    @Override
    public boolean equals(Object other) {
      if (this == other) return true;
      if (!(other instanceof IdentityWeakReference)) return false;
      PermutationSolution<Integer> left = get();
      PermutationSolution<Integer> right = ((IdentityWeakReference) other).get();
      return left != null && left == right;
    }
  }

  /** A composition wrapper for the Random sources used by the algorithm. */
  private static final class AuditableJavaRandom extends Random {
    private static final long serialVersionUID = 1L;
    private final Random delegate;
    private final String streamId;
    private final V35TrueSequenceAudit audit;

    private AuditableJavaRandom(Random delegate, String streamId, V35TrueSequenceAudit audit) {
      super(0L);
      this.delegate = delegate;
      this.streamId = streamId;
      this.audit = audit;
    }

    @Override
    public void setSeed(long seed) {
      if (delegate != null) {
        delegate.setSeed(seed);
      }
      super.setSeed(seed);
    }

    @Override
    public int nextInt(int bound) {
      int result = delegate.nextInt(bound);
      audit.recordJavaDraw(
          streamId, "BOUNDED_INT", Integer.valueOf(0), Integer.valueOf(bound - 1),
          Integer.valueOf(result));
      return result;
    }

    @Override
    public int nextInt() {
      int result = delegate.nextInt();
      audit.recordJavaDraw(streamId, "INT", null, null, Integer.valueOf(result));
      return result;
    }

    @Override
    public long nextLong() {
      long result = delegate.nextLong();
      audit.recordJavaDraw(streamId, "LONG", null, null, Long.valueOf(result));
      return result;
    }

    @Override
    public boolean nextBoolean() {
      boolean result = delegate.nextBoolean();
      audit.recordJavaDraw(streamId, "BOOLEAN", Integer.valueOf(0), Integer.valueOf(1),
          Integer.valueOf(result ? 1 : 0));
      return result;
    }

    @Override
    public float nextFloat() {
      float result = delegate.nextFloat();
      audit.recordJavaDraw(streamId, "FLOAT", Float.valueOf(0.0f), Float.valueOf(1.0f),
          Float.valueOf(result));
      return result;
    }

    @Override
    public double nextDouble() {
      double result = delegate.nextDouble();
      audit.recordJavaDraw(streamId, "DOUBLE", Double.valueOf(0.0d), Double.valueOf(1.0d),
          Double.valueOf(result));
      return result;
    }

    @Override
    public double nextGaussian() {
      double result = delegate.nextGaussian();
      audit.recordJavaDraw(streamId, "GAUSSIAN", null, null, Double.valueOf(result));
      return result;
    }

    @Override
    public void nextBytes(byte[] bytes) {
      delegate.nextBytes(bytes);
      // This API produces bytes rather than a single numeric draw; current A
      // sources do not use it, so no fabricated scalar event is emitted.
    }
  }
}
