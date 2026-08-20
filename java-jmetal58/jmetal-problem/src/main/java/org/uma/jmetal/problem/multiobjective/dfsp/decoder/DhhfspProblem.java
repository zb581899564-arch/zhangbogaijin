package org.uma.jmetal.problem.multiobjective.dfsp.decoder;

import org.uma.jmetal.problem.PermutationProblem;
import org.uma.jmetal.problem.multiobjective.dfsp.model.DhhfspFourVectorSolution;
import org.uma.jmetal.problem.multiobjective.dfsp.model.DhhfspInstance;

/** jMetal 5.8 problem adapter for the validated decoder. */
public final class DhhfspProblem implements PermutationProblem<DhhfspFourVectorSolution> {
  private static final long serialVersionUID = 1L;
  private final DhhfspInstance instance;
  private final DhhfspDecoder decoder;
  private final DecodeOptions options;
  private final DhhfspSolutionFactory solutionFactory;
  private final EvaluationCounter evaluationCounter;

  public DhhfspProblem(
      DhhfspInstance instance, DhhfspDecoder decoder, DecodeOptions options,
      DhhfspSolutionFactory solutionFactory, EvaluationCounter evaluationCounter) {
    if (instance == null || decoder == null || options == null
        || solutionFactory == null || evaluationCounter == null) {
      throw new IllegalArgumentException("DhhfspProblem dependencies must not be null");
    }
    if (options.getMode() == DecodeMode.AUTHOR_ACTUAL) {
      throw new IllegalArgumentException("AUTHOR_ACTUAL cannot be a production problem mode");
    }
    this.instance = instance;
    this.decoder = decoder;
    this.options = options;
    this.solutionFactory = solutionFactory;
    this.evaluationCounter = evaluationCounter;
  }

  @Override
  public int getNumberOfVariables() { return instance.getNumberOfJobs(); }

  @Override
  public int getNumberOfObjectives() { return DhhfspFourVectorSolution.NUMBER_OF_OBJECTIVES; }

  @Override
  public int getNumberOfConstraints() { return 0; }

  @Override
  public String getName() { return "DR-DHHFSP-ST-Chapter4"; }

  @Override
  public void evaluate(DhhfspFourVectorSolution solution) {
    DecodeResult result = decoder.decode(instance, solution, options);
    ObjectiveBreakdown objectives = result.getFinalSnapshot().getObjectives();
    solution.setObjective(0, objectives.getMakespan());
    solution.setObjective(1, objectives.getTotalEnergy());
    solution.setObjective(2, objectives.getTotalWorkerCost());
    solution.setAttribute(DecodeResult.class, result);
    evaluationCounter.recordSuccessfulEvaluation();
  }

  @Override
  public DhhfspFourVectorSolution createSolution() { return solutionFactory.create(); }

  @Override
  public int getPermutationLength() { return instance.getNumberOfJobs(); }

  @Override
  public int getNumberOfFactories() { return instance.getNumberOfFactories(); }

  public EvaluationCounter getEvaluationCounter() { return evaluationCounter; }
}
