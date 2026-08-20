package org.uma.jmetal.algorithm.multiobjective.mypso.zhangbo.cata;

import java.util.List;
import org.uma.jmetal.algorithm.multiobjective.mypso.zhangbo.neighborhood.ZhangBoNeighborhoodId;
import org.uma.jmetal.algorithm.multiobjective.mypso.zhangbo.neighborhood.ZhangBoNeighborhoodPreview;
import org.uma.jmetal.algorithm.multiobjective.mypso.zhangbo.neighborhood.ZhangBoNeighborhoodRequest;
import org.uma.jmetal.algorithm.multiobjective.mypso.zhangbo.neighborhood.ZhangBoNeighborhoodSuite;
import org.uma.jmetal.solution.PermutationSolution;

/**
 * The only CA-TA bridge from a stable candidate preview to one complete
 * evaluation. Previewing and masks do not consume an FE.
 */
public final class ZhangBoNeighborhoodCandidateGateway {
  /** Injectable monotonic clock; production defaults to {@link System#nanoTime()}. */
  public interface NanoClock { long nanoTime(); }
  public interface CompleteEvaluator {
    void evaluate(PermutationSolution<Integer> candidate);
  }

  public static final class Attempt {
    private final ZhangBoNeighborhoodId id;
    private final boolean applicable;
    private final String reason;
    private final PermutationSolution<Integer> candidate;
    private final long elapsedNanos;
    private final int completeEvaluations;

    private Attempt(ZhangBoNeighborhoodId id, boolean applicable, String reason,
        PermutationSolution<Integer> candidate, long elapsedNanos, int completeEvaluations) {
      this.id = id;
      this.applicable = applicable;
      this.reason = reason;
      this.candidate = candidate;
      this.elapsedNanos = elapsedNanos;
      this.completeEvaluations = completeEvaluations;
    }

    public ZhangBoNeighborhoodId getId() { return id; }
    public boolean isApplicable() { return applicable; }
    public String getReason() { return reason; }
    public PermutationSolution<Integer> getCandidate() { return candidate; }
    public long getElapsedNanos() { return elapsedNanos; }
    public int getCompleteEvaluations() { return completeEvaluations; }
  }

  /** One immutable preview plus the time already spent constructing it. */
  public static final class PreparedPreview {
    private final ZhangBoNeighborhoodPreview preview;
    private final long elapsedNanos;

    private PreparedPreview(ZhangBoNeighborhoodPreview preview, long elapsedNanos) {
      this.preview = preview;
      this.elapsedNanos = elapsedNanos;
    }

    public ZhangBoNeighborhoodId getId() { return preview.getId(); }
    public boolean isApplicable() { return preview.isApplicable(); }
    public String getReason() { return preview.getReason(); }
    public long getElapsedNanos() { return elapsedNanos; }
  }

  private final ZhangBoNeighborhoodSuite suite;
  private final NanoClock clock;

  public ZhangBoNeighborhoodCandidateGateway() {
    this(new ZhangBoNeighborhoodSuite(), new NanoClock() {
      @Override public long nanoTime() { return System.nanoTime(); }
    });
  }

  public ZhangBoNeighborhoodCandidateGateway(ZhangBoNeighborhoodSuite suite) {
    this(suite, new NanoClock() {
      @Override public long nanoTime() { return System.nanoTime(); }
    });
  }

  public ZhangBoNeighborhoodCandidateGateway(
      ZhangBoNeighborhoodSuite suite, NanoClock clock) {
    if (suite == null || clock == null) throw new IllegalArgumentException("suite/clock");
    this.suite = suite;
    this.clock = clock;
  }

  public ZhangBoNeighborhoodPreview preview(
      ZhangBoNeighborhoodId id, ZhangBoNeighborhoodRequest request) {
    return suite.preview(id, request);
  }

  public PreparedPreview prepare(
      ZhangBoNeighborhoodId id, ZhangBoNeighborhoodRequest request) {
    long started = clock.nanoTime();
    ZhangBoNeighborhoodPreview value = preview(id, request);
    return new PreparedPreview(value, Math.max(0L, clock.nanoTime() - started));
  }

  public Attempt evaluateOne(
      ZhangBoNeighborhoodId id, ZhangBoNeighborhoodRequest request,
      CompleteEvaluator evaluator) {
    return evaluateOne(prepare(id, request), evaluator);
  }

  public Attempt evaluateOne(PreparedPreview prepared, CompleteEvaluator evaluator) {
    if (prepared == null || evaluator == null) {
      throw new IllegalArgumentException("prepared/evaluator");
    }
    ZhangBoNeighborhoodPreview preview = prepared.preview;
    if (!preview.isApplicable()) {
      return new Attempt(preview.getId(), false, preview.getReason(), null,
          prepared.elapsedNanos, 0);
    }
    PermutationSolution<Integer> candidate = preview.getFirstCandidate();
    long started = clock.nanoTime();
    evaluator.evaluate(candidate);
    long evaluationNanos = Math.max(0L, clock.nanoTime() - started);
    return new Attempt(preview.getId(), true, "EVALUATED", candidate,
        saturatedAdd(prepared.elapsedNanos, evaluationNanos), 1);
  }

  public List<PermutationSolution<Integer>> candidates(
      ZhangBoNeighborhoodId id, ZhangBoNeighborhoodRequest request) {
    return preview(id, request).getCandidates();
  }

  private static long saturatedAdd(long left, long right) {
    if (Long.MAX_VALUE - left < right) return Long.MAX_VALUE;
    return left + right;
  }
}
