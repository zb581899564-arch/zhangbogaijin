package org.uma.jmetal.algorithm.multiobjective.mypso.p8;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/** Immutable summary of a completed, failed, or unavailable P8 run. */
public final class P8RunRecord {
  private final String instance;
  private final String runId;
  private final String sourceRunId;
  private final boolean reused;
  private final String instanceSha256;
  private final String matrix;
  private final String label;
  private final long seed;
  private final P8RunStatus status;
  private final String reason;
  private final String configurationSha256;
  private final String configurationText;
  private final String initialPopulationSha256;
  private final long fullEvaluations;
  private final long wallClockMillis;
  private final long cpuNanos;
  private final long cfvfRepairs;
  private final long caTaEvaluations;
  private final int illegalSolutions;
  private final double fmax;
  private final double favg;
  private final double fatigueExcess;
  private final double workerFatigueVariance;
  private final double highFatigueRatio;
  private final double longestContinuousWork;
  private final double totalNaturalRecovery;
  private final double loadImbalance;
  private final List<double[]> front;

  public P8RunRecord(String instance, String instanceSha256, String matrix, String label,
      long seed, P8RunStatus status, String reason, String configurationSha256,
      String configurationText, String initialPopulationSha256, long fullEvaluations, long wallClockMillis,
      long cpuNanos, long cfvfRepairs, long caTaEvaluations, int illegalSolutions,
      double fmax, double favg, double fatigueExcess, double workerFatigueVariance,
      double highFatigueRatio, double longestContinuousWork, double totalNaturalRecovery,
      double loadImbalance,
      List<double[]> front) {
    this(instance, instanceSha256, matrix, label, seed, status, reason,
        configurationSha256, configurationText, initialPopulationSha256,
        fullEvaluations, wallClockMillis, cpuNanos, cfvfRepairs, caTaEvaluations,
        illegalSolutions, fmax, favg, fatigueExcess, workerFatigueVariance,
        highFatigueRatio, longestContinuousWork, totalNaturalRecovery,
        loadImbalance, front, runId(instance, matrix, label, seed),
        runId(instance, matrix, label, seed), false);
  }

  private P8RunRecord(String instance, String instanceSha256, String matrix, String label,
      long seed, P8RunStatus status, String reason, String configurationSha256,
      String configurationText, String initialPopulationSha256, long fullEvaluations,
      long wallClockMillis, long cpuNanos, long cfvfRepairs, long caTaEvaluations,
      int illegalSolutions, double fmax, double favg, double fatigueExcess,
      double workerFatigueVariance, double highFatigueRatio,
      double longestContinuousWork, double totalNaturalRecovery, double loadImbalance,
      List<double[]> front, String runId, String sourceRunId, boolean reused) {
    this.instance = value(instance);
    this.runId = value(runId);
    this.sourceRunId = value(sourceRunId);
    this.reused = reused;
    this.instanceSha256 = value(instanceSha256);
    this.matrix = value(matrix);
    this.label = value(label);
    this.seed = seed;
    this.status = status;
    this.reason = value(reason);
    this.configurationSha256 = value(configurationSha256);
    this.configurationText = value(configurationText);
    this.initialPopulationSha256 = value(initialPopulationSha256);
    this.fullEvaluations = fullEvaluations;
    this.wallClockMillis = wallClockMillis;
    this.cpuNanos = cpuNanos;
    this.cfvfRepairs = cfvfRepairs;
    this.caTaEvaluations = caTaEvaluations;
    this.illegalSolutions = illegalSolutions;
    this.fmax = fmax;
    this.favg = favg;
    this.fatigueExcess = fatigueExcess;
    this.workerFatigueVariance = workerFatigueVariance;
    this.highFatigueRatio = highFatigueRatio;
    this.longestContinuousWork = longestContinuousWork;
    this.totalNaturalRecovery = totalNaturalRecovery;
    this.loadImbalance = loadImbalance;
    List<double[]> copy = new ArrayList<>();
    if (front != null) for (double[] point : front) copy.add(point.clone());
    this.front = Collections.unmodifiableList(copy);
  }

  /** Creates a label-level record that explicitly reuses one exact physical run. */
  public P8RunRecord alias(P8ExperimentSpec spec) {
    return alias(spec, instance, instanceSha256, seed, initialPopulationSha256);
  }

  /** Alias variant with explicit target identity checks for runner reuse maps. */
  public P8RunRecord alias(P8ExperimentSpec spec, String targetInstance,
      String targetInstanceSha256, long targetSeed, String targetInitialPopulationSha256) {
    if (spec == null) throw new IllegalArgumentException("spec cannot be null");
    if (!spec.isFrontEligible()) {
      throw new IllegalArgumentException("Only formal P8-v3 entries may reuse a physical run");
    }
    if (status != P8RunStatus.COMPLETED) {
      throw new IllegalArgumentException("Only a completed physical run may be aliased");
    }
    String sourceHash = getMechanismVectorHash();
    String targetHash = spec.getMechanismVectorHash();
    if (sourceHash.isEmpty() || !sourceHash.equals(targetHash)) {
      throw new IllegalArgumentException(
          "sourceRunId reuse requires an exact mechanism-vector hash match");
    }
    if (!instance.equals(value(targetInstance))
        || !instanceSha256.equals(value(targetInstanceSha256))
        || seed != targetSeed
        || !initialPopulationSha256.equals(value(targetInitialPopulationSha256))) {
      throw new IllegalArgumentException("sourceRunId reuse cannot cross instance, seed or input hash");
    }
    return new P8RunRecord(targetInstance, targetInstanceSha256, spec.getMatrix().name(), spec.getLabel(),
        targetSeed, status, "EXACT_PROFILE_REUSE hash=" + sourceHash + " from " + sourceRunId, configurationSha256,
        configurationText, targetInitialPopulationSha256, fullEvaluations, wallClockMillis,
        cpuNanos, cfvfRepairs, caTaEvaluations, illegalSolutions, fmax, favg,
        fatigueExcess, workerFatigueVariance, highFatigueRatio,
        longestContinuousWork, totalNaturalRecovery, loadImbalance, front,
        runId(instance, spec.getMatrix().name(), spec.getLabel(), seed), sourceRunId, true);
  }

  public String getInstance() { return instance; }
  public String getRunId() { return runId; }
  public String getSourceRunId() { return sourceRunId; }
  public boolean isReused() { return reused; }
  public String getInstanceSha256() { return instanceSha256; }
  public String getMatrix() { return matrix; }
  public String getLabel() { return label; }
  public long getSeed() { return seed; }
  public P8RunStatus getStatus() { return status; }
  public String getReason() { return reason; }
  public String getConfigurationSha256() { return configurationSha256; }
  public String getConfigurationText() { return configurationText; }
  public String getInitialPopulationSha256() { return initialPopulationSha256; }
  /** Hash of the exact mechanism vector embedded at the start of configurationText. */
  public String getMechanismVectorHash() {
    int marker = configurationText.indexOf("mechanismVectorHash=");
    if (marker < 0) marker = configurationText.indexOf("populationSize=");
    if (marker <= 0 || !configurationText.startsWith("ablationSchema=")) return "";
    String profileText = configurationText.substring(0, marker);
    try {
      byte[] digest = MessageDigest.getInstance("SHA-256")
          .digest(profileText.getBytes(StandardCharsets.UTF_8));
      StringBuilder result = new StringBuilder();
      for (byte value : digest) result.append(String.format("%02x", value & 0xff));
      return result.toString();
    } catch (NoSuchAlgorithmException exception) {
      throw new IllegalStateException("SHA-256 unavailable", exception);
    }
  }
  public long getFullEvaluations() { return fullEvaluations; }
  public long getWallClockMillis() { return wallClockMillis; }
  public long getCpuNanos() { return cpuNanos; }
  public long getCfvfRepairs() { return cfvfRepairs; }
  public long getCaTaEvaluations() { return caTaEvaluations; }
  public int getIllegalSolutions() { return illegalSolutions; }
  public double getFmax() { return fmax; }
  public double getFavg() { return favg; }
  public double getFatigueExcess() { return fatigueExcess; }
  public double getWorkerFatigueVariance() { return workerFatigueVariance; }
  public double getHighFatigueRatio() { return highFatigueRatio; }
  public double getLongestContinuousWork() { return longestContinuousWork; }
  public double getTotalNaturalRecovery() { return totalNaturalRecovery; }
  public double getLoadImbalance() { return loadImbalance; }
  public List<double[]> getFront() {
    List<double[]> copy = new ArrayList<>();
    for (double[] point : front) copy.add(point.clone());
    return copy;
  }

  private static String value(String value) { return value == null ? "" : value; }

  private static String runId(String instance, String matrix, String label, long seed) {
    return value(instance) + '|' + value(matrix) + '|' + value(label) + '|' + seed;
  }

}
