package org.uma.jmetal.algorithm.multiobjective.mypso.zhangbo;

import java.io.Serializable;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import org.uma.jmetal.algorithm.multiobjective.mypso.p8.P8AblationProfile;
import org.uma.jmetal.algorithm.multiobjective.mypso.v35.V35ProductionConfiguration;
import org.uma.jmetal.algorithm.multiobjective.mypso.v35.V35BottleneckDiagnosisConfiguration;
import org.uma.jmetal.algorithm.multiobjective.mypso.zhangbo.cata.ZhangBoCaTaConfiguration;

/** Immutable switches and engineering defaults for the Zhang Bo global-search path. */
public final class ZhangBoGlobalSearchConfiguration implements Serializable {
  private static final long serialVersionUID = 1L;

  public enum GlobalLeaderMode { AUTHOR_ACTIVE, ORIGINAL_QG }
  /**
   * The structured baseline is the replayable author-compatible JS-PSO plus
   * FA/MA/WA GA updater.  AUTHOR_UPDATE remains available only for the strict
   * uncontrolled author diagnostic path.
   */
  public enum ParticleUpdateMode {
    AUTHOR_UPDATE, PUBLISHED_BASELINE, FA_LEADER_ONLY, INDEPENDENT_RESOURCE, CFVF
  }
  public enum EnvironmentalSelectionMode { AUTHOR_PDDR_ACTIVE, EVALUATED_PDDR }

  public static final long DEFAULT_SEED = 20260808L;
  public static final double DEFAULT_Q_EPSILON = 0.8;
  public static final double DEFAULT_Q_ALPHA = 1.0;
  public static final double DEFAULT_Q_GAMMA = 0.8;
  public static final double DEFAULT_RESOURCE_INERTIA = 0.5;
  public static final double DEFAULT_RESOURCE_EXPLORATION = 0.05;

  private final GlobalLeaderMode globalLeaderMode;
  private final ParticleUpdateMode particleUpdateMode;
  private final long seed;
  private final double qEpsilon;
  private final double qAlpha;
  private final double qGamma;
  private final double resourceCognitiveScale;
  private final double resourceSocialScale;
  private final double resourceInertia;
  private final double resourceExploration;
  private final EnvironmentalSelectionMode environmentalSelectionMode;
  private final ZhangBoPersonalArchiveConfiguration personalArchiveConfiguration;
  private final ZhangBoQpConfiguration qpConfiguration;
  private final ZhangBoDualQCoordinationConfiguration dualQCoordinationConfiguration;
  private final ZhangBoCaTaConfiguration caTaConfiguration;
  private final P8AblationProfile p8AblationProfile;
  private final boolean dscrEnabled;
  private boolean directionalTeacherPool;
  private int teacherPoolSize = 10;
  private V35BottleneckDiagnosisConfiguration v35BottleneckDiagnosis =
      V35BottleneckDiagnosisConfiguration.fullMaskNoShadow();
  /** V35-FC-2 dynamic local-FE budget; {@code null} keeps legacy LS_Times. */
  private org.uma.jmetal.algorithm.multiobjective.mypso.v35.V35LocalFeBudgetConfiguration
      localFeBudget;
  /** V35-FC-3 cheap-Test CA-TA-Lite tuning; defaults to the archived standard. */
  private org.uma.jmetal.algorithm.multiobjective.mypso.v35.V35CaTaLiteConfiguration
      v35CaTaLiteConfiguration =
          org.uma.jmetal.algorithm.multiobjective.mypso.v35.V35CaTaLiteConfiguration.standard();

  public ZhangBoGlobalSearchConfiguration(
      GlobalLeaderMode globalLeaderMode,
      ParticleUpdateMode particleUpdateMode,
      long seed,
      double qEpsilon,
      double qAlpha,
      double qGamma,
      double resourceCognitiveScale,
      double resourceSocialScale,
      double resourceInertia,
      double resourceExploration) {
    this(globalLeaderMode, particleUpdateMode, seed, qEpsilon, qAlpha, qGamma,
        resourceCognitiveScale, resourceSocialScale, resourceInertia,
        resourceExploration, EnvironmentalSelectionMode.AUTHOR_PDDR_ACTIVE,
        ZhangBoPersonalArchiveConfiguration.disabled(), ZhangBoQpConfiguration.disabled(),
        ZhangBoDualQCoordinationConfiguration.synchronous(), ZhangBoCaTaConfiguration.disabled());
  }

  public ZhangBoGlobalSearchConfiguration(
      GlobalLeaderMode globalLeaderMode,
      ParticleUpdateMode particleUpdateMode,
      long seed,
      double qEpsilon,
      double qAlpha,
      double qGamma,
      double resourceCognitiveScale,
      double resourceSocialScale,
      double resourceInertia,
      double resourceExploration,
      EnvironmentalSelectionMode environmentalSelectionMode,
      ZhangBoPersonalArchiveConfiguration personalArchiveConfiguration) {
    this(globalLeaderMode, particleUpdateMode, seed, qEpsilon, qAlpha, qGamma,
        resourceCognitiveScale, resourceSocialScale, resourceInertia,
        resourceExploration, environmentalSelectionMode,
        personalArchiveConfiguration, ZhangBoQpConfiguration.disabled(),
        ZhangBoDualQCoordinationConfiguration.synchronous(), ZhangBoCaTaConfiguration.disabled());
  }

  public ZhangBoGlobalSearchConfiguration(
      GlobalLeaderMode globalLeaderMode,
      ParticleUpdateMode particleUpdateMode,
      long seed,
      double qEpsilon,
      double qAlpha,
      double qGamma,
      double resourceCognitiveScale,
      double resourceSocialScale,
      double resourceInertia,
      double resourceExploration,
      EnvironmentalSelectionMode environmentalSelectionMode,
      ZhangBoPersonalArchiveConfiguration personalArchiveConfiguration,
      ZhangBoQpConfiguration qpConfiguration) {
    this(globalLeaderMode, particleUpdateMode, seed, qEpsilon, qAlpha, qGamma,
        resourceCognitiveScale, resourceSocialScale, resourceInertia,
        resourceExploration, environmentalSelectionMode, personalArchiveConfiguration,
        qpConfiguration, ZhangBoDualQCoordinationConfiguration.synchronous(),
        ZhangBoCaTaConfiguration.disabled());
  }

  public ZhangBoGlobalSearchConfiguration(
      GlobalLeaderMode globalLeaderMode,
      ParticleUpdateMode particleUpdateMode,
      long seed,
      double qEpsilon,
      double qAlpha,
      double qGamma,
      double resourceCognitiveScale,
      double resourceSocialScale,
      double resourceInertia,
      double resourceExploration,
      EnvironmentalSelectionMode environmentalSelectionMode,
      ZhangBoPersonalArchiveConfiguration personalArchiveConfiguration,
      ZhangBoQpConfiguration qpConfiguration,
      ZhangBoDualQCoordinationConfiguration dualQCoordinationConfiguration) {
    this(globalLeaderMode, particleUpdateMode, seed, qEpsilon, qAlpha, qGamma,
        resourceCognitiveScale, resourceSocialScale, resourceInertia, resourceExploration,
        environmentalSelectionMode, personalArchiveConfiguration, qpConfiguration,
        dualQCoordinationConfiguration, ZhangBoCaTaConfiguration.disabled());
  }

  public ZhangBoGlobalSearchConfiguration(
      GlobalLeaderMode globalLeaderMode,
      ParticleUpdateMode particleUpdateMode,
      long seed,
      double qEpsilon,
      double qAlpha,
      double qGamma,
      double resourceCognitiveScale,
      double resourceSocialScale,
      double resourceInertia,
      double resourceExploration,
      EnvironmentalSelectionMode environmentalSelectionMode,
      ZhangBoPersonalArchiveConfiguration personalArchiveConfiguration,
      ZhangBoQpConfiguration qpConfiguration,
      ZhangBoDualQCoordinationConfiguration dualQCoordinationConfiguration,
      ZhangBoCaTaConfiguration caTaConfiguration) {
    this(globalLeaderMode, particleUpdateMode, seed, qEpsilon, qAlpha, qGamma,
        resourceCognitiveScale, resourceSocialScale, resourceInertia, resourceExploration,
        environmentalSelectionMode, personalArchiveConfiguration, qpConfiguration,
        dualQCoordinationConfiguration, caTaConfiguration, null, false);
  }

  private ZhangBoGlobalSearchConfiguration(
      GlobalLeaderMode globalLeaderMode,
      ParticleUpdateMode particleUpdateMode,
      long seed,
      double qEpsilon,
      double qAlpha,
      double qGamma,
      double resourceCognitiveScale,
      double resourceSocialScale,
      double resourceInertia,
      double resourceExploration,
      EnvironmentalSelectionMode environmentalSelectionMode,
      ZhangBoPersonalArchiveConfiguration personalArchiveConfiguration,
      ZhangBoQpConfiguration qpConfiguration,
      ZhangBoDualQCoordinationConfiguration dualQCoordinationConfiguration,
      ZhangBoCaTaConfiguration caTaConfiguration,
      P8AblationProfile p8AblationProfile,
      boolean dscrEnabled) {
    if (globalLeaderMode == null || particleUpdateMode == null) {
      throw new IllegalArgumentException("Global-search modes cannot be null");
    }
    if (environmentalSelectionMode == null || personalArchiveConfiguration == null
        || qpConfiguration == null || dualQCoordinationConfiguration == null
        || caTaConfiguration == null) {
      throw new IllegalArgumentException("PDDR and personal-archive configuration cannot be null");
    }
    requireProbability(qEpsilon, "qEpsilon");
    requireProbability(qAlpha, "qAlpha");
    requireProbability(qGamma, "qGamma");
    requireProbability(resourceCognitiveScale, "resourceCognitiveScale");
    requireProbability(resourceSocialScale, "resourceSocialScale");
    requireProbability(resourceInertia, "resourceInertia");
    double minimumExploration = p8AblationProfile == null ? 0.02 : 0.0;
    if (resourceExploration < minimumExploration || resourceExploration > 0.10
        || !Double.isFinite(resourceExploration)) {
      throw new IllegalArgumentException("resourceExploration is outside the configured range");
    }
    if (p8AblationProfile == null && particleUpdateMode == ParticleUpdateMode.CFVF
        && globalLeaderMode != GlobalLeaderMode.ORIGINAL_QG) {
      throw new IllegalArgumentException("CFVF requires ORIGINAL_QG for the P6.1 acceptance path");
    }
    if (p8AblationProfile == null
        && environmentalSelectionMode == EnvironmentalSelectionMode.EVALUATED_PDDR
        && particleUpdateMode != ParticleUpdateMode.CFVF
        && particleUpdateMode != ParticleUpdateMode.PUBLISHED_BASELINE) {
      throw new IllegalArgumentException("EVALUATED_PDDR requires CFVF or the structured baseline");
    }
    if (personalArchiveConfiguration.isEnabled()
        && environmentalSelectionMode != EnvironmentalSelectionMode.EVALUATED_PDDR) {
      throw new IllegalArgumentException("Lineage archive requires EVALUATED_PDDR");
    }
    if (p8AblationProfile == null && qpConfiguration.isEnabled()
        && (globalLeaderMode != GlobalLeaderMode.ORIGINAL_QG
        || particleUpdateMode != ParticleUpdateMode.CFVF
        || environmentalSelectionMode != EnvironmentalSelectionMode.EVALUATED_PDDR
        || !personalArchiveConfiguration.isEnabled())) {
      throw new IllegalArgumentException(
          "Q-pbest requires ORIGINAL_QG, CFVF, EVALUATED_PDDR and lineage archive");
    }
    if (dualQCoordinationConfiguration.isBlockFrozen() && !qpConfiguration.isEnabled()) {
      throw new IllegalArgumentException("BLOCK_FROZEN dual-Q coordination requires Q-pbest");
    }
    if (p8AblationProfile == null && caTaConfiguration.isEnabled()
        && (globalLeaderMode != GlobalLeaderMode.ORIGINAL_QG
        || particleUpdateMode != ParticleUpdateMode.CFVF
        || environmentalSelectionMode != EnvironmentalSelectionMode.EVALUATED_PDDR
        || !personalArchiveConfiguration.isEnabled() || !qpConfiguration.isEnabled()
        || !dualQCoordinationConfiguration.isBlockFrozen())) {
      throw new IllegalArgumentException(
          "CA-TA requires ORIGINAL_QG, CFVF, evaluated PDDR, lineage archive, Q-pbest and block-frozen dual Q");
    }
    this.globalLeaderMode = globalLeaderMode;
    this.particleUpdateMode = particleUpdateMode;
    this.seed = seed;
    this.qEpsilon = qEpsilon;
    this.qAlpha = qAlpha;
    this.qGamma = qGamma;
    this.resourceCognitiveScale = resourceCognitiveScale;
    this.resourceSocialScale = resourceSocialScale;
    this.resourceInertia = resourceInertia;
    this.resourceExploration = resourceExploration;
    this.environmentalSelectionMode = environmentalSelectionMode;
    this.personalArchiveConfiguration = personalArchiveConfiguration;
    this.qpConfiguration = qpConfiguration;
    this.dualQCoordinationConfiguration = dualQCoordinationConfiguration;
    this.caTaConfiguration = caTaConfiguration;
    this.p8AblationProfile = p8AblationProfile;
    this.dscrEnabled = dscrEnabled;
  }

  /** Builds an explicit P8 profile without changing any legacy/default factory. */
  public static ZhangBoGlobalSearchConfiguration forP8(
      P8AblationProfile profile, long seed) {
    // Current formal/ablation profiles use the Table-9 upper bound.  The historical
    // 0.4 coefficient remains available only through the explicit three-argument
    // engineering overload so it cannot silently leak into P9 or the current matrix.
    return forP8(profile, seed, 0.6);
  }

  /**
   * Builds a governed P8/P9 configuration with an explicit resource-flight scale.
   * Engineering P8 callers retain the historical 0.4 default through the two-argument
   * overload; formal Table-9 runners must pass 0.6 here so the recorded coefficient is
   * exactly the coefficient consumed by CFVF.
   */
  public static ZhangBoGlobalSearchConfiguration forP8(
      P8AblationProfile profile, long seed, double resourceFlightScale) {
    if (profile == null) throw new IllegalArgumentException("profile cannot be null");
    requireProbability(resourceFlightScale, "resourceFlightScale");
    GlobalLeaderMode leader = profile.isQgEnabled()
        ? GlobalLeaderMode.ORIGINAL_QG : GlobalLeaderMode.AUTHOR_ACTIVE;
    ParticleUpdateMode update;
    switch (profile.getResourceFlightMode()) {
      case FA_LEADER_ONLY:
        update = ParticleUpdateMode.FA_LEADER_ONLY;
        break;
      case INDEPENDENT_FMW:
        update = ParticleUpdateMode.INDEPENDENT_RESOURCE;
        break;
      case COUPLED_FMW:
        update = ParticleUpdateMode.CFVF;
        break;
      case BASELINE_GA:
        // Formal P8-v3 runs use the structured, injectable baseline updater.
        update = ParticleUpdateMode.PUBLISHED_BASELINE;
        break;
      case AUTHOR_GA:
        // The uncontrolled author path is A0-only.  P8ExperimentRegistry rejects
        // diagnostic profiles before this method; fail closed if a caller attempts
        // to route an AUTHOR_GA profile into the formal configuration.
        if (profile.getRandomnessMode()
            != P8AblationProfile.RandomnessMode.AUTHOR_UNCONTROLLED) {
          throw new IllegalArgumentException(
              "AUTHOR_GA is reserved for the A0 author diagnostic profile");
        }
        update = ParticleUpdateMode.AUTHOR_UPDATE;
        break;
      default:
        throw new IllegalArgumentException("Unsupported P8 resource flight mode: "
            + profile.getResourceFlightMode());
    }
    EnvironmentalSelectionMode pddr = profile.isEvaluatedPddrEnabled()
        ? EnvironmentalSelectionMode.EVALUATED_PDDR
        : EnvironmentalSelectionMode.AUTHOR_PDDR_ACTIVE;
    ZhangBoPersonalArchiveConfiguration archive = profile.isLineageArchiveEnabled()
        ? ZhangBoPersonalArchiveConfiguration.standard()
        : ZhangBoPersonalArchiveConfiguration.disabled();
    ZhangBoQpConfiguration qp = profile.isQpEnabled()
        ? ZhangBoQpConfiguration.standard() : ZhangBoQpConfiguration.disabled();
    ZhangBoDualQCoordinationConfiguration dual = profile.isBlockFrozenDualQ()
        ? ZhangBoDualQCoordinationConfiguration.blockFrozen()
        : ZhangBoDualQCoordinationConfiguration.synchronous();
    ZhangBoCaTaConfiguration caTa = profile.isCaTaEnabled()
        ? ZhangBoCaTaConfiguration.standard() : ZhangBoCaTaConfiguration.disabled();
    return new ZhangBoGlobalSearchConfiguration(
        leader, update, seed, DEFAULT_Q_EPSILON, DEFAULT_Q_ALPHA, DEFAULT_Q_GAMMA,
        resourceFlightScale, resourceFlightScale,
        profile.hasResourceInertia() ? DEFAULT_RESOURCE_INERTIA : 0.0,
        profile.hasLegalExploration() ? DEFAULT_RESOURCE_EXPLORATION : 0.0,
        pddr, archive, qp, dual, caTa, profile, false);
  }

  /**
   * Builds the v3.5 production line without routing through the historical P8 shift profile.
   * The v3.5 configuration has no shift field by design; callers must use its NONE boundary.
   */
  public static ZhangBoGlobalSearchConfiguration forV35(V35ProductionConfiguration configuration) {
    if (configuration == null) throw new IllegalArgumentException("configuration cannot be null");
    GlobalLeaderMode leader = configuration.isQgEnabled()
        ? GlobalLeaderMode.ORIGINAL_QG : GlobalLeaderMode.AUTHOR_ACTIVE;
    ParticleUpdateMode update = configuration.isCfvfEnabled()
        ? ParticleUpdateMode.CFVF : ParticleUpdateMode.PUBLISHED_BASELINE;
    ZhangBoPersonalArchiveConfiguration archive = configuration.isQpEnabled()
        ? ZhangBoPersonalArchiveConfiguration.standard()
        : ZhangBoPersonalArchiveConfiguration.disabled();
    ZhangBoQpConfiguration qp = configuration.isQpEnabled()
        ? ZhangBoQpConfiguration.standard() : ZhangBoQpConfiguration.disabled();
    ZhangBoDualQCoordinationConfiguration dual = configuration.getDualQCoordination() != null
        ? configuration.getDualQCoordination()
        : (configuration.isQpEnabled()
            ? ZhangBoDualQCoordinationConfiguration.blockFrozen()
            : ZhangBoDualQCoordinationConfiguration.synchronous());
    ZhangBoCaTaConfiguration cata = configuration.isCaTaLiteEnabled()
        ? ZhangBoCaTaConfiguration.standard() : ZhangBoCaTaConfiguration.disabled();
    ZhangBoGlobalSearchConfiguration result = new ZhangBoGlobalSearchConfiguration(
        leader, update, configuration.getSeed(), DEFAULT_Q_EPSILON, DEFAULT_Q_ALPHA,
        DEFAULT_Q_GAMMA, 0.6, 0.6, DEFAULT_RESOURCE_INERTIA,
        DEFAULT_RESOURCE_EXPLORATION, EnvironmentalSelectionMode.EVALUATED_PDDR,
        archive, qp, dual, cata, null, configuration.isDscrEnabled());
    result.setDirectionalTeacherPool(configuration.isDirectionalTeacherPoolEnabled(),
        configuration.getTeacherPoolSize());
    result.v35BottleneckDiagnosis = configuration.getBottleneckDiagnosis();
    result.localFeBudget = configuration.getLocalFeBudget();
    if (configuration.getCaTaLiteConfiguration() != null) {
      result.v35CaTaLiteConfiguration = configuration.getCaTaLiteConfiguration();
    }
    return result;
  }

  public static ZhangBoGlobalSearchConfiguration disabled() {
    return new ZhangBoGlobalSearchConfiguration(
        GlobalLeaderMode.AUTHOR_ACTIVE, ParticleUpdateMode.AUTHOR_UPDATE,
        DEFAULT_SEED, DEFAULT_Q_EPSILON, DEFAULT_Q_ALPHA, DEFAULT_Q_GAMMA,
        0.4, 0.4, DEFAULT_RESOURCE_INERTIA, DEFAULT_RESOURCE_EXPLORATION);
  }

  public static ZhangBoGlobalSearchConfiguration originalQg(double randK) {
    return new ZhangBoGlobalSearchConfiguration(
        GlobalLeaderMode.ORIGINAL_QG, ParticleUpdateMode.AUTHOR_UPDATE,
        DEFAULT_SEED, DEFAULT_Q_EPSILON, DEFAULT_Q_ALPHA, DEFAULT_Q_GAMMA,
        randK, randK, DEFAULT_RESOURCE_INERTIA, DEFAULT_RESOURCE_EXPLORATION);
  }

  public static ZhangBoGlobalSearchConfiguration originalQgWithCfvf(
      double randK, long seed) {
    return new ZhangBoGlobalSearchConfiguration(
        GlobalLeaderMode.ORIGINAL_QG, ParticleUpdateMode.CFVF,
        seed, DEFAULT_Q_EPSILON, DEFAULT_Q_ALPHA, DEFAULT_Q_GAMMA,
        randK, randK, DEFAULT_RESOURCE_INERTIA, DEFAULT_RESOURCE_EXPLORATION);
  }

  public static ZhangBoGlobalSearchConfiguration originalQgWithCfvfEvaluatedPddr(
      double randK, long seed) {
    return new ZhangBoGlobalSearchConfiguration(
        GlobalLeaderMode.ORIGINAL_QG, ParticleUpdateMode.CFVF,
        seed, DEFAULT_Q_EPSILON, DEFAULT_Q_ALPHA, DEFAULT_Q_GAMMA,
        randK, randK, DEFAULT_RESOURCE_INERTIA, DEFAULT_RESOURCE_EXPLORATION,
        EnvironmentalSelectionMode.EVALUATED_PDDR,
        ZhangBoPersonalArchiveConfiguration.disabled(), ZhangBoQpConfiguration.disabled());
  }

  public static ZhangBoGlobalSearchConfiguration originalQgWithCfvfAndLineageArchive(
      double randK, long seed) {
    return new ZhangBoGlobalSearchConfiguration(
        GlobalLeaderMode.ORIGINAL_QG, ParticleUpdateMode.CFVF,
        seed, DEFAULT_Q_EPSILON, DEFAULT_Q_ALPHA, DEFAULT_Q_GAMMA,
        randK, randK, DEFAULT_RESOURCE_INERTIA, DEFAULT_RESOURCE_EXPLORATION,
        EnvironmentalSelectionMode.EVALUATED_PDDR,
        ZhangBoPersonalArchiveConfiguration.standard(), ZhangBoQpConfiguration.disabled());
  }

  public static ZhangBoGlobalSearchConfiguration originalQgWithCfvfLineageArchiveAndQp(
      double randK, long seed) {
    return new ZhangBoGlobalSearchConfiguration(
        GlobalLeaderMode.ORIGINAL_QG, ParticleUpdateMode.CFVF,
        seed, DEFAULT_Q_EPSILON, DEFAULT_Q_ALPHA, DEFAULT_Q_GAMMA,
        randK, randK, DEFAULT_RESOURCE_INERTIA, DEFAULT_RESOURCE_EXPLORATION,
        EnvironmentalSelectionMode.EVALUATED_PDDR,
        ZhangBoPersonalArchiveConfiguration.standard(), ZhangBoQpConfiguration.standard());
  }

  public static ZhangBoGlobalSearchConfiguration originalQgWithCfvfLineageArchiveQpBlockFrozen(
      double randK, long seed) {
    return new ZhangBoGlobalSearchConfiguration(
        GlobalLeaderMode.ORIGINAL_QG, ParticleUpdateMode.CFVF,
        seed, DEFAULT_Q_EPSILON, DEFAULT_Q_ALPHA, DEFAULT_Q_GAMMA,
        randK, randK, DEFAULT_RESOURCE_INERTIA, DEFAULT_RESOURCE_EXPLORATION,
        EnvironmentalSelectionMode.EVALUATED_PDDR,
        ZhangBoPersonalArchiveConfiguration.standard(), ZhangBoQpConfiguration.standard(),
        ZhangBoDualQCoordinationConfiguration.blockFrozen());
  }

  public static ZhangBoGlobalSearchConfiguration originalQgWithCfvfLineageArchiveQpBlockFrozenCaTa(
      double randK, long seed) {
    return new ZhangBoGlobalSearchConfiguration(
        GlobalLeaderMode.ORIGINAL_QG, ParticleUpdateMode.CFVF,
        seed, DEFAULT_Q_EPSILON, DEFAULT_Q_ALPHA, DEFAULT_Q_GAMMA,
        randK, randK, DEFAULT_RESOURCE_INERTIA, DEFAULT_RESOURCE_EXPLORATION,
        EnvironmentalSelectionMode.EVALUATED_PDDR,
        ZhangBoPersonalArchiveConfiguration.standard(), ZhangBoQpConfiguration.standard(),
        ZhangBoDualQCoordinationConfiguration.blockFrozen(), ZhangBoCaTaConfiguration.standard());
  }

  private static void requireProbability(double value, String name) {
    if (value < 0.0 || value > 1.0 || !Double.isFinite(value)) {
      throw new IllegalArgumentException(name + " must be finite and in [0,1]");
    }
  }

  public GlobalLeaderMode getGlobalLeaderMode() { return globalLeaderMode; }
  public ParticleUpdateMode getParticleUpdateMode() { return particleUpdateMode; }
  public long getSeed() { return seed; }
  public double getQEpsilon() { return qEpsilon; }
  public double getQAlpha() { return qAlpha; }
  public double getQGamma() { return qGamma; }
  public double getResourceCognitiveScale() { return resourceCognitiveScale; }
  public double getResourceSocialScale() { return resourceSocialScale; }
  public double getResourceInertia() { return resourceInertia; }
  public double getResourceExploration() { return resourceExploration; }
  public boolean isQgEnabled() { return globalLeaderMode == GlobalLeaderMode.ORIGINAL_QG; }
  public boolean isDscrEnabled() { return dscrEnabled; }

  /** Directional top-k teacher pool (V35-P10.1). Default off; disabled behaviour is legacy-identical. */
  public void setDirectionalTeacherPool(boolean enabled, int poolSize) {
    if (enabled && globalLeaderMode != GlobalLeaderMode.ORIGINAL_QG) {
      throw new IllegalArgumentException(
          "directional teacher pool requires the original Q-gbest social controller");
    }
    if (enabled && poolSize < 2) {
      throw new IllegalArgumentException("directional teacher pool size must be >= 2");
    }
    if (poolSize < 1) {
      throw new IllegalArgumentException("directional teacher pool size must be >= 1");
    }
    this.directionalTeacherPool = enabled;
    this.teacherPoolSize = poolSize;
  }
  public boolean isDirectionalTeacherPoolEnabled() { return directionalTeacherPool; }

  /** V35-FC-2: the dynamic local-FE budget, or {@code null} for the legacy
   *  fixed {@code LS_Times} control. */
  public org.uma.jmetal.algorithm.multiobjective.mypso.v35.V35LocalFeBudgetConfiguration
      getLocalFeBudget() {
    return localFeBudget;
  }

  /** V35-FC-3: the CA-TA-Lite tuning consumed by the v3.5 controller. */
  public org.uma.jmetal.algorithm.multiobjective.mypso.v35.V35CaTaLiteConfiguration
      getV35CaTaLiteConfiguration() {
    return v35CaTaLiteConfiguration;
  }
  public int getTeacherPoolSize() { return teacherPoolSize; }
  public boolean isCfvfEnabled() { return particleUpdateMode == ParticleUpdateMode.CFVF; }
  public boolean isStructuredBaselineEnabled() {
    return particleUpdateMode == ParticleUpdateMode.PUBLISHED_BASELINE;
  }
  public boolean isResourceFlightEnabled() {
    return particleUpdateMode != ParticleUpdateMode.AUTHOR_UPDATE;
  }
  public boolean isP8ResourceFlightEnabled() {
    return p8AblationProfile != null
        && p8AblationProfile.getResourceFlightMode()
        != P8AblationProfile.ResourceFlightMode.AUTHOR_GA;
  }
  public EnvironmentalSelectionMode getEnvironmentalSelectionMode() { return environmentalSelectionMode; }
  public ZhangBoPersonalArchiveConfiguration getPersonalArchiveConfiguration() {
    return personalArchiveConfiguration;
  }
  public boolean isEvaluatedPddrEnabled() {
    return environmentalSelectionMode == EnvironmentalSelectionMode.EVALUATED_PDDR;
  }
  public boolean isLineageArchiveEnabled() { return personalArchiveConfiguration.isEnabled(); }
  public ZhangBoQpConfiguration getQpConfiguration() { return qpConfiguration; }
  public boolean isQpEnabled() { return qpConfiguration.isEnabled(); }
  public ZhangBoDualQCoordinationConfiguration getDualQCoordinationConfiguration() {
    return dualQCoordinationConfiguration;
  }
  public boolean isBlockFrozenDualQEnabled() {
    return dualQCoordinationConfiguration.isBlockFrozen();
  }
  public ZhangBoCaTaConfiguration getCaTaConfiguration() { return caTaConfiguration; }
  public boolean isCaTaEnabled() { return caTaConfiguration.isEnabled(); }
  /** True only for the v3.5 production bridge; historical P8 profiles stay isolated. */
  public boolean isV35CaTaLiteEnabled() { return dscrEnabled && p8AblationProfile == null && caTaConfiguration.isEnabled(); }
  public V35BottleneckDiagnosisConfiguration getV35BottleneckDiagnosis() {
    return v35BottleneckDiagnosis;
  }
  public boolean isLocalSearchEnabled() {
    // The non-profile P6 factory is a production CA-TA configuration too.  A
    // profile may additionally select fixed/Need-aware VNS, so both sources
    // must enable the algorithmic local-search stage.
    return caTaConfiguration.isEnabled()
        || (p8AblationProfile != null && p8AblationProfile.isLocalSearchEnabled());
  }
  public boolean isFixedNeighborhoodEnabled() {
    return p8AblationProfile != null && p8AblationProfile.isFixedNeighborhoodEnabled();
  }
  public boolean isReplayableAuthorRandomEnabled() {
    return p8AblationProfile != null
        && p8AblationProfile.getRandomnessMode()
        == P8AblationProfile.RandomnessMode.P8_REPLAYABLE;
  }
  public P8AblationProfile getP8AblationProfile() { return p8AblationProfile; }

  /** Single authoritative semantic tag for configuration and runtime evidence. */
  public String getSemanticTag() {
    if (p8AblationProfile != null) return p8AblationProfile.getSolutionSemanticTag();
    if (particleUpdateMode == ParticleUpdateMode.AUTHOR_UPDATE
        && globalLeaderMode == GlobalLeaderMode.AUTHOR_ACTIVE) {
      return "author_actual";
    }
    if (particleUpdateMode == ParticleUpdateMode.PUBLISHED_BASELINE) {
      return "published_baseline";
    }
    return "fatigue_improved";
  }

  public String toCanonicalText() {
    return "schemaVersion=3\n"
        + "semanticTag=" + getSemanticTag() + "\n"
        + "subSwarmSemanticsVersion=" + ZhangBoSubSwarmSemantics.VERSION + "\n"
        + "subSwarmRoleMappingSha256=" + ZhangBoSubSwarmSemantics.mappingHash() + "\n"
        + "globalLeaderMode=" + globalLeaderMode + "\n"
        + "particleUpdateMode=" + particleUpdateMode + "\n"
        + "seed=" + seed + "\n"
        + "qEpsilon=" + qEpsilon + "\n"
        + "qAlpha=" + qAlpha + "\n"
        + "qGamma=" + qGamma + "\n"
        + "resourceCognitiveScale=" + resourceCognitiveScale + "\n"
        + "resourceSocialScale=" + resourceSocialScale + "\n"
        + "resourceInertia=" + resourceInertia + "\n"
        + "resourceExploration=" + resourceExploration + "\n"
        + "dscrEnabled=" + dscrEnabled + "\n"
        + (environmentalSelectionMode == EnvironmentalSelectionMode.AUTHOR_PDDR_ACTIVE
            ? "" : "environmentalSelectionMode=" + environmentalSelectionMode + "\n")
        + (personalArchiveConfiguration.isEnabled()
            ? personalArchiveConfiguration.toCanonicalText() : "")
        + (qpConfiguration.isEnabled() ? qpConfiguration.toCanonicalText() : "")
        + (dualQCoordinationConfiguration.isBlockFrozen()
            ? dualQCoordinationConfiguration.toCanonicalText() : "")
        + (caTaConfiguration.isEnabled() ? caTaConfiguration.toCanonicalText() : "")
        + (p8AblationProfile == null ? "" : p8AblationProfile.canonicalTextWithoutSemanticTag());
  }

  public String sha256() {
    try {
      byte[] bytes = MessageDigest.getInstance("SHA-256")
          .digest(toCanonicalText().getBytes(StandardCharsets.UTF_8));
      StringBuilder builder = new StringBuilder();
      for (byte value : bytes) builder.append(String.format("%02X", value & 0xff));
      return builder.toString();
    } catch (NoSuchAlgorithmException exception) {
      throw new IllegalStateException("SHA-256 is unavailable", exception);
    }
  }
}
