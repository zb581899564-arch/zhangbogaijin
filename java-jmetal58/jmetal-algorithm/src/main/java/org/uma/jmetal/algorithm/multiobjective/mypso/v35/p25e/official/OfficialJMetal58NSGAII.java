package org.uma.jmetal.algorithm.multiobjective.mypso.v35.p25e.official;
import org.uma.jmetal.algorithm.impl.AbstractGeneticAlgorithm;
import org.uma.jmetal.operator.CrossoverOperator;
import org.uma.jmetal.operator.MutationOperator;
import org.uma.jmetal.operator.SelectionOperator;
import org.uma.jmetal.operator.impl.selection.RankingAndCrowdingSelection;
import org.uma.jmetal.problem.Problem;
import org.uma.jmetal.solution.Solution;
import org.uma.jmetal.util.SolutionListUtils;
import org.uma.jmetal.util.comparator.DominanceComparator;
import org.uma.jmetal.util.evaluator.SolutionListEvaluator;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/** Official jMetal 5.8 NSGA-II; only package and class/constructor names are isolated. */
@SuppressWarnings("serial")
public class OfficialJMetal58NSGAII<S extends Solution<?>>
    extends AbstractGeneticAlgorithm<S, List<S>> {
  protected final int maxEvaluations;
  protected final SolutionListEvaluator<S> evaluator;
  protected int evaluations;
  protected Comparator<S> dominanceComparator ;
  protected int matingPoolSize;
  protected int offspringPopulationSize ;

  public OfficialJMetal58NSGAII(Problem<S> problem, int maxEvaluations, int populationSize,
      int matingPoolSize, int offspringPopulationSize,
      CrossoverOperator<S> crossoverOperator, MutationOperator<S> mutationOperator,
      SelectionOperator<List<S>, S> selectionOperator, SolutionListEvaluator<S> evaluator) {
    this(problem, maxEvaluations, populationSize, matingPoolSize, offspringPopulationSize,
        crossoverOperator, mutationOperator, selectionOperator,
        new DominanceComparator<S>(), evaluator);
  }

  public OfficialJMetal58NSGAII(Problem<S> problem, int maxEvaluations, int populationSize,
      int matingPoolSize, int offspringPopulationSize,
      CrossoverOperator<S> crossoverOperator, MutationOperator<S> mutationOperator,
      SelectionOperator<List<S>, S> selectionOperator, Comparator<S> dominanceComparator,
      SolutionListEvaluator<S> evaluator) {
    super(problem);
    this.maxEvaluations = maxEvaluations;
    setMaxPopulationSize(populationSize); ;
    this.crossoverOperator = crossoverOperator;
    this.mutationOperator = mutationOperator;
    this.selectionOperator = selectionOperator;
    this.evaluator = evaluator;
    this.dominanceComparator = dominanceComparator ;
    this.matingPoolSize = matingPoolSize ;
    this.offspringPopulationSize = offspringPopulationSize ;
  }

  @Override protected void initProgress() { evaluations = getMaxPopulationSize(); }
  @Override protected void updateProgress() { evaluations += offspringPopulationSize ; }
  @Override protected boolean isStoppingConditionReached() { return evaluations >= maxEvaluations; }
  @Override protected List<S> evaluatePopulation(List<S> population) {
    population = evaluator.evaluate(population, getProblem());
    return population;
  }
  @Override protected List<S> selection(List<S> population) {
    List<S> matingPopulation = new ArrayList<>(population.size());
    for (int i = 0; i < matingPoolSize; i++) {
      S solution = selectionOperator.execute(population);
      matingPopulation.add(solution);
    }
    return matingPopulation;
  }
  @Override protected List<S> reproduction(List<S> matingPool) {
    int numberOfParents = crossoverOperator.getNumberOfRequiredParents() ;
    checkNumberOfParents(matingPool, numberOfParents);
    List<S> offspringPopulation = new ArrayList<>(offspringPopulationSize);
    for (int i = 0; i < matingPool.size(); i += numberOfParents) {
      List<S> parents = new ArrayList<>(numberOfParents);
      for (int j = 0; j < numberOfParents; j++) {
        parents.add(population.get(i+j));
      }
      List<S> offspring = crossoverOperator.execute(parents);
      for(S s: offspring){
        mutationOperator.execute(s);
        offspringPopulation.add(s);
        if (offspringPopulation.size() >= offspringPopulationSize) break;
      }
    }
    return offspringPopulation;
  }
  @Override protected List<S> replacement(List<S> population, List<S> offspringPopulation) {
    List<S> jointPopulation = new ArrayList<>();
    jointPopulation.addAll(population);
    jointPopulation.addAll(offspringPopulation);
    RankingAndCrowdingSelection<S> rankingAndCrowdingSelection ;
    rankingAndCrowdingSelection = new RankingAndCrowdingSelection<S>(
        getMaxPopulationSize(), dominanceComparator) ;
    return rankingAndCrowdingSelection.execute(jointPopulation) ;
  }
  @Override public List<S> getResult() {
    return SolutionListUtils.getNondominatedSolutions(getPopulation());
  }
  @Override public String getName() { return "NSGAII" ; }
  @Override public String getDescription() {
    return "Nondominated Sorting Genetic Algorithm version II" ;
  }
}
