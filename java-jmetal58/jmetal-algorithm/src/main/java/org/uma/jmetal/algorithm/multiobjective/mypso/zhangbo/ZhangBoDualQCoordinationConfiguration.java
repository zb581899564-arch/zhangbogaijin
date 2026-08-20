package org.uma.jmetal.algorithm.multiobjective.mypso.zhangbo;

import java.io.Serializable;

/** Immutable P6.4 coordination policy. Synchronous mode preserves P6.3. */
public final class ZhangBoDualQCoordinationConfiguration implements Serializable {
  private static final long serialVersionUID = 1L;

  public enum Mode { SYNCHRONOUS, BLOCK_FROZEN }
  public enum FrozenSelectionPolicy { GREEDY }

  public static final double DEFAULT_WARMUP_RATIO = 0.10;
  public static final int DEFAULT_BLOCK_LENGTH = 5;

  private final Mode mode;
  private final double warmupRatio;
  private final int blockLength;
  private final int gBlockLength;
  private final FrozenSelectionPolicy frozenSelectionPolicy;
  /**
   * V35-FC-4: contribution-gated soft-freeze coefficient.  In a P-block the
   * frozen Qg (and symmetrically the frozen Qp in a G-block) still absorbs
   * experience at learning rate {@code rho * alpha}, but only for offspring
   * that actually executed at least one teacher-derived CFVF action
   * ({@code I_contrib = 1}); untouched offspring keep the pure observation.
   * {@code 0.0} (the default) is the hard freeze of the archived A4.
   */
  private final double softFreezeRho;

  private ZhangBoDualQCoordinationConfiguration(
      Mode mode, double warmupRatio, int blockLength, int gBlockLength,
      FrozenSelectionPolicy frozenSelectionPolicy, double softFreezeRho) {
    if (mode == null || frozenSelectionPolicy == null) {
      throw new IllegalArgumentException("Dual-Q coordination fields cannot be null");
    }
    if (!Double.isFinite(warmupRatio) || warmupRatio < 0.0 || warmupRatio > 1.0) {
      throw new IllegalArgumentException("warmupRatio must be finite and in [0,1]");
    }
    if (blockLength < 1) throw new IllegalArgumentException("blockLength must be >= 1");
    if (gBlockLength < 1) throw new IllegalArgumentException("gBlockLength must be >= 1");
    if (!Double.isFinite(softFreezeRho) || softFreezeRho < 0.0 || softFreezeRho > 1.0) {
      throw new IllegalArgumentException("softFreezeRho must be finite and in [0,1]");
    }
    this.mode = mode;
    this.warmupRatio = warmupRatio;
    this.blockLength = blockLength;
    this.gBlockLength = gBlockLength;
    this.frozenSelectionPolicy = frozenSelectionPolicy;
    this.softFreezeRho = softFreezeRho;
  }

  public static ZhangBoDualQCoordinationConfiguration synchronous() {
    return new ZhangBoDualQCoordinationConfiguration(
        Mode.SYNCHRONOUS, DEFAULT_WARMUP_RATIO, DEFAULT_BLOCK_LENGTH,
        DEFAULT_BLOCK_LENGTH, FrozenSelectionPolicy.GREEDY, 0.0);
  }

  public static ZhangBoDualQCoordinationConfiguration blockFrozen() {
    return blockFrozen(DEFAULT_WARMUP_RATIO, DEFAULT_BLOCK_LENGTH);
  }

  public static ZhangBoDualQCoordinationConfiguration blockFrozen(
      double warmupRatio, int blockLength) {
    return blockFrozen(warmupRatio, blockLength, blockLength);
  }

  /**
   * P/G-block scheduling with an independent G-block length. The default
   * {@code gBlockLength == blockLength} reproduces the equal-length schedule
   * exactly; a longer G-block lets the original Qg learn for a larger share
   * of the post-warmup budget without changing any mechanism.
   */
  public static ZhangBoDualQCoordinationConfiguration blockFrozen(
      double warmupRatio, int blockLength, int gBlockLength) {
    return new ZhangBoDualQCoordinationConfiguration(
        Mode.BLOCK_FROZEN, warmupRatio, blockLength, gBlockLength,
        FrozenSelectionPolicy.GREEDY, 0.0);
  }

  /**
   * V35-FC-4: block scheduling with a contribution-gated soft freeze
   * ({@code rho > 0}).  Identical to {@link #blockFrozen(double, int, int)}
   * apart from the soft-freeze coefficient.
   */
  public static ZhangBoDualQCoordinationConfiguration blockFrozenSoftFreeze(
      double warmupRatio, int blockLength, int gBlockLength, double softFreezeRho) {
    return new ZhangBoDualQCoordinationConfiguration(
        Mode.BLOCK_FROZEN, warmupRatio, blockLength, gBlockLength,
        FrozenSelectionPolicy.GREEDY, softFreezeRho);
  }

  public Mode getMode() { return mode; }
  public double getWarmupRatio() { return warmupRatio; }
  public int getBlockLength() { return blockLength; }
  public int getGBlockLength() { return gBlockLength; }
  public FrozenSelectionPolicy getFrozenSelectionPolicy() { return frozenSelectionPolicy; }
  public boolean isBlockFrozen() { return mode == Mode.BLOCK_FROZEN; }
  public double getSoftFreezeRho() { return softFreezeRho; }

  public String toCanonicalText() {
    StringBuilder text = new StringBuilder()
        .append("dualQ.mode=").append(mode).append('\n')
        .append("dualQ.warmupRatio=").append(warmupRatio).append('\n')
        .append("dualQ.blockLength=").append(blockLength).append('\n')
        .append("dualQ.gBlockLength=").append(gBlockLength).append('\n')
        .append("dualQ.frozenSelectionPolicy=").append(frozenSelectionPolicy).append('\n');
    // rho=0 is the archived hard freeze: keep the canonical text byte-stable.
    if (softFreezeRho > 0.0) {
      text.append("dualQ.softFreezeRho=").append(softFreezeRho).append('\n');
    }
    return text.toString();
  }
}
