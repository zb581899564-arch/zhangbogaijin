package org.uma.jmetal.problem.multiobjective.dfsp.fatigue;

/** Immutable Li-compatible per-instance extension used by the improved decoder. */
public final class ZhangBoInstanceExtension {
  public static final int SCHEMA_VERSION = 1;
  public static final String SEMANTIC_TAG = "li_compatible_instance_extension";
  public static final long SUT_SEED = 20260808L;
  public static final String DISTRIBUTION = "DISCRETE_UNIFORM_INT_1_9";
  public static final String BRIDGE_SEMANTIC_TAG = "author_compatibility_bridge";
  public static final String BRIDGE_SAMPLER = "PAPER_TABLE_SUT_V1";
  public static final String BRIDGE_DISTRIBUTION = "EXACT_PUBLISHED_TABLE";

  private final String instanceSha256;
  private final int jobs;
  private final int stages;
  private final int[][] standardSetupTimes;
  private final String configurationSha256;
  private final String semanticTag;
  private final String sampler;
  private final String seedText;
  private final String distribution;

  public ZhangBoInstanceExtension(
      String instanceSha256, int jobs, int stages, int[][] standardSetupTimes,
      String configurationSha256) {
    this(instanceSha256, jobs, stages, standardSetupTimes, configurationSha256,
        SEMANTIC_TAG, ZhangBoInstanceExtensionGenerator.SAMPLER_ID,
        Long.toString(SUT_SEED), DISTRIBUTION);
  }

  public ZhangBoInstanceExtension(
      String instanceSha256, int jobs, int stages, int[][] standardSetupTimes,
      String configurationSha256, String semanticTag, String sampler,
      String seedText, String distribution) {
    if (instanceSha256 == null || !instanceSha256.matches("[0-9A-Fa-f]{64}")) {
      throw new IllegalArgumentException("Invalid instance SHA-256: " + instanceSha256);
    }
    if (jobs <= 0 || stages <= 0 || standardSetupTimes == null
        || standardSetupTimes.length != jobs) {
      throw new IllegalArgumentException("Invalid setup-time dimensions");
    }
    this.standardSetupTimes = new int[jobs][stages];
    for (int job = 0; job < jobs; job++) {
      if (standardSetupTimes[job] == null || standardSetupTimes[job].length != stages) {
        throw new IllegalArgumentException("Invalid setup-time row for job=" + job);
      }
      for (int stage = 0; stage < stages; stage++) {
        int value = standardSetupTimes[job][stage];
        if (value < 1 || value > 9) {
          throw new IllegalArgumentException(
              "SUT must be in 1..9 at job=" + job + ", stage=" + stage + ": " + value);
        }
        this.standardSetupTimes[job][stage] = value;
      }
    }
    if (configurationSha256 == null) configurationSha256 = "";
    if (!configurationSha256.isEmpty()
        && !configurationSha256.matches("[0-9A-F]{64}")) {
      throw new IllegalArgumentException("Invalid extension configuration SHA-256");
    }
    this.instanceSha256 = instanceSha256.toUpperCase();
    this.jobs = jobs;
    this.stages = stages;
    this.configurationSha256 = configurationSha256;
    if (semanticTag == null || sampler == null || seedText == null || distribution == null
        || semanticTag.isEmpty() || sampler.isEmpty() || seedText.isEmpty()
        || distribution.isEmpty()) {
      throw new IllegalArgumentException("Extension provenance fields cannot be empty");
    }
    this.semanticTag = semanticTag;
    this.sampler = sampler;
    this.seedText = seedText;
    this.distribution = distribution;
  }

  public static ZhangBoInstanceExtension authorCompatibilityBridge(
      String instanceSha256, int jobs, int stages, int[][] standardSetupTimes) {
    return new ZhangBoInstanceExtension(instanceSha256, jobs, stages, standardSetupTimes, "",
        BRIDGE_SEMANTIC_TAG, BRIDGE_SAMPLER, "NOT_APPLICABLE", BRIDGE_DISTRIBUTION);
  }

  public String getInstanceSha256() { return instanceSha256; }
  public int getJobs() { return jobs; }
  public int getStages() { return stages; }
  public int getStandardSetupTime(int job, int stage) {
    return standardSetupTimes[job][stage];
  }
  public int[][] copyStandardSetupTimes() {
    int[][] result = new int[jobs][stages];
    for (int job = 0; job < jobs; job++) {
      System.arraycopy(standardSetupTimes[job], 0, result[job], 0, stages);
    }
    return result;
  }
  public String getConfigurationSha256() { return configurationSha256; }
  public String getSemanticTag() { return semanticTag; }
  public String getSampler() { return sampler; }
  public String getSeedText() { return seedText; }
  public String getDistribution() { return distribution; }
}
