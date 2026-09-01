package org.uma.jmetal.problem.multiobjective.dfsp.fatigue;

import org.uma.jmetal.problem.PermutationProblem;
import org.uma.jmetal.problem.multiobjective.dfsp.decoder.EvaluationCounter;
import org.uma.jmetal.problem.multiobjective.dfsp.decoder.ProductionDecodeMode;
import org.uma.jmetal.problem.multiobjective.dfsp.model.DhhfspFourVectorSolution;
import org.uma.jmetal.problem.multiobjective.dfsp.fatigue.shift.ZhangBoShiftConfiguration;
import org.uma.jmetal.problem.multiobjective.dfsp.setup.ProductFamilySetupModel;
import org.uma.jmetal.solution.PermutationSolution;

/**
 * Seven-slot canonical Zhang-Bo production problem.
 *
 * <p>Slots 0, 1 and 6 are the historic Cmax/TEC/TWC slots consumed by the
 * ZhangBo algorithm.  FM1--FM3 select an explicit evaluator mode; no value in
 * the parameter manifest is inspected to route between modes.</p>
 */
public final class ZhangBoCanonicalProductionProblem
    implements PermutationProblem<PermutationSolution<Integer>> {
  private static final long serialVersionUID = 1L;
  public static final int NUMBER_OF_OBJECTIVES = 7;

  private final ZhangBoFatigueInstanceData instance;
  private final ZhangBoFatigueParameters parameters;
  private final ProductionDecodeMode mode;
  private final ZhangBoCanonicalSolutionFactory solutionFactory;
  private final ZhangBoFatigueEvaluator evaluator;
  private final EvaluationCounter evaluationCounter;
  private final ZhangBoShiftConfiguration shiftConfiguration;
  private final ProductFamilySetupModel setupModel;
  private final ZhangBoDecoderTimingAccumulator decoderTimingAccumulator;
  private final ZhangBoEvaluationObservation evaluationObservation;

  public ZhangBoCanonicalProductionProblem(
      ZhangBoFatigueInstanceData instance,
      ZhangBoFatigueParameters parameters,
      ProductionDecodeMode mode,
      long seed) {
    this(instance, parameters, mode, new ZhangBoFatigueEvaluator(), new EvaluationCounter(),
        seed, ZhangBoShiftConfiguration.none());
  }

  public ZhangBoCanonicalProductionProblem(
      ZhangBoFatigueInstanceData instance,
      ZhangBoFatigueParameters parameters,
      ProductionDecodeMode mode,
      long seed,
      ZhangBoShiftConfiguration shiftConfiguration) {
    this(instance, parameters, mode, new ZhangBoFatigueEvaluator(), new EvaluationCounter(),
        seed, shiftConfiguration);
  }

  public ZhangBoCanonicalProductionProblem(
      ZhangBoFatigueInstanceData instance,
      ZhangBoFatigueParameters parameters,
      ProductionDecodeMode mode,
      ZhangBoFatigueEvaluator evaluator,
      EvaluationCounter evaluationCounter,
      long seed) {
    this(instance, parameters, mode, evaluator, evaluationCounter, seed,
        ZhangBoShiftConfiguration.none());
  }

  public ZhangBoCanonicalProductionProblem(
      ZhangBoFatigueInstanceData instance,
      ZhangBoFatigueParameters parameters,
      ProductionDecodeMode mode,
      ZhangBoFatigueEvaluator evaluator,
      EvaluationCounter evaluationCounter,
      long seed,
      ZhangBoShiftConfiguration shiftConfiguration) {
    this(instance, parameters, mode, evaluator, evaluationCounter, seed,
        shiftConfiguration, ProductFamilySetupModel.degenerate(instance.getJobs(), instance.getStages()));
  }

  public ZhangBoCanonicalProductionProblem(
      ZhangBoFatigueInstanceData instance,
      ZhangBoFatigueParameters parameters,
      ProductionDecodeMode mode,
      ZhangBoFatigueEvaluator evaluator,
      EvaluationCounter evaluationCounter,
      long seed,
      ZhangBoShiftConfiguration shiftConfiguration,
      ProductFamilySetupModel setupModel) {
    if (instance == null || parameters == null || mode == null
        || evaluator == null || evaluationCounter == null || shiftConfiguration == null
        || setupModel == null) {
      throw new IllegalArgumentException("Canonical production dependencies must not be null");
    }
    if (mode.isAuthorDiagnostic()) {
      throw new IllegalArgumentException(
          "AUTHOR_DIAGNOSTIC is read-only and cannot be a production problem");
    }
    if (!instance.getInstanceSha256().equals(parameters.getInstanceSha256())) {
      throw new IllegalArgumentException("Fatigue parameter instance hash mismatch");
    }
    if (parameters.getFactories() != instance.getFactories()
        || parameters.getStages() != instance.getStages()) {
      throw new IllegalArgumentException("Fatigue parameter dimensions do not match instance");
    }
    if (!setupModel.isFormalDegenerate()) {
      throw new IllegalArgumentException(
          "v3.5 formal production currently accepts only DEGENERATE_SINGLE_FAMILY/"
              + "SEQUENCE_INDEPENDENT setup semantics");
    }
    this.instance = instance;
    this.parameters = parameters;
    this.mode = mode;
    this.evaluator = evaluator;
    this.evaluationCounter = evaluationCounter;
    this.shiftConfiguration = shiftConfiguration;
    this.setupModel = setupModel;
    this.decoderTimingAccumulator = new ZhangBoDecoderTimingAccumulator();
    this.evaluationObservation = new ZhangBoEvaluationObservation();
    this.solutionFactory = new ZhangBoCanonicalSolutionFactory(instance, mode, seed);
  }

  @Override public int getNumberOfVariables() { return instance.getJobs(); }
  @Override public int getNumberOfObjectives() { return NUMBER_OF_OBJECTIVES; }
  @Override public int getNumberOfConstraints() { return 0; }
  @Override public int getPermutationLength() { return instance.getJobs(); }
  @Override public int getNumberOfFactories() { return instance.getFactories(); }
  @Override public String getName() {
    return "ZhangBo-Canonical-" + mode.name() + "-" + shiftConfiguration.getMode().name();
  }

  @Override
  public void evaluate(PermutationSolution<Integer> genericSolution) {
    if (genericSolution == null) throw new IllegalArgumentException("solution must not be null");
    if (!(genericSolution instanceof DhhfspFourVectorSolution)) {
      throw new IllegalArgumentException(
          "Canonical production requires the P2 DhhfspFourVectorSolution contract");
    }
    DhhfspFourVectorSolution solution = (DhhfspFourVectorSolution) genericSolution;
    evaluationObservation.beforeEvaluation(solution);
    if (solution.getNumberOfObjectives() != NUMBER_OF_OBJECTIVES) {
      throw new IllegalArgumentException(
          "Canonical production requires seven objective slots [0..6]");
    }
    if (!mode.getSemanticTag().equals(solution.getSemanticTag())) {
      throw new IllegalArgumentException(
          "Solution semanticTag " + solution.getSemanticTag()
              + " does not match explicit mode " + mode.name());
    }
    ZhangBoFatigueEvaluationResult result;
    try {
      result = evaluator.evaluate(
          instance, parameters, solution, evaluatorMode(mode), shiftConfiguration, setupModel);
    } catch (IllegalArgumentException error) {
      evaluationObservation.recordIllegalSolution();
      throw error;
    }
    result = result.withSemanticTag(mode.getSemanticTag());
    double[] objectives = result.getObjectives();
    if (objectives.length != NUMBER_OF_OBJECTIVES) {
      throw new IllegalStateException("Fatigue evaluator must return seven objective slots");
    }
    for (int index = 0; index < objectives.length; index++) {
      solution.setObjective(index, objectives[index]);
    }
    solution.setAttribute(ZhangBoFatigueEvaluationResult.class, result);
    solution.setAttribute(ProductionDecodeMode.class, mode);
    solution.setAttribute(ZhangBoShiftConfiguration.class, shiftConfiguration);
    decoderTimingAccumulator.record(result.getDecoderTiming());
    evaluationObservation.afterEvaluation(solution);
    evaluationCounter.recordSuccessfulEvaluation();
    // FC-5.1: pure-observation global best fully-evaluated Cmax (no decision impact).
    V35CmaxBestEver.observe(objectives[0], objectives[1], objectives[6]);
  }

  @Override public DhhfspFourVectorSolution createSolution() { return solutionFactory.create(); }
  public ZhangBoFatigueInstanceData getInstance() { return instance; }
  public ZhangBoFatigueParameters getParameters() { return parameters; }
  public ProductionDecodeMode getMode() { return mode; }
  public ZhangBoCanonicalSolutionFactory getSolutionFactory() { return solutionFactory; }
  public EvaluationCounter getEvaluationCounter() { return evaluationCounter; }
  public ZhangBoShiftConfiguration getShiftConfiguration() { return shiftConfiguration; }
  public ProductFamilySetupModel getSetupModel() { return setupModel; }
  public ZhangBoDecoderTimingSnapshot getDecoderTimingSnapshot() {
    return decoderTimingAccumulator.snapshot();
  }
  public ZhangBoEvaluationObservation getEvaluationObservation() {
    return evaluationObservation;
  }

  private static ZhangBoFatigueEvaluationMode evaluatorMode(ProductionDecodeMode mode) {
    if (mode == ProductionDecodeMode.AUTHOR_DIAGNOSTIC) {
      throw new IllegalArgumentException("AUTHOR_DIAGNOSTIC is not a production evaluator mode");
    }
    return mode.toFatigueEvaluationMode();
  }
}
