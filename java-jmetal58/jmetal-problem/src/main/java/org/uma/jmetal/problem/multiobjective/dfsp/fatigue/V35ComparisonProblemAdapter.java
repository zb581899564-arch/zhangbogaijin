package org.uma.jmetal.problem.multiobjective.dfsp.fatigue;

import java.util.ArrayList;
import java.util.List;
import org.uma.jmetal.problem.PermutationProblem;
import org.uma.jmetal.problem.multiobjective.dfsp.decoder.ProductionDecodeMode;
import org.uma.jmetal.problem.multiobjective.dfsp.model.DhhfspFourVectorSolution;
import org.uma.jmetal.solution.PermutationSolution;

/**
 * Minimal three-objective view of the canonical FM3 production problem.
 *
 * <p>The search algorithm sees exactly Cmax, TEC and TWC in slots 0..2. Every
 * call is delegated once to the seven-slot canonical decoder and copied back
 * from slots 0, 1 and 6. The adapter also owns a frozen initial-population
 * queue so different algorithms can start from byte-identical four-vectors.</p>
 */
public final class V35ComparisonProblemAdapter
    implements PermutationProblem<PermutationSolution<Integer>> {
  private static final long serialVersionUID = 1L;

  private final ZhangBoCanonicalProductionProblem delegate;
  private final List<DhhfspFourVectorSolution> initialPopulation;
  private final ObjectiveView objectiveView;
  private final V35ExactEvaluationBudget budget;
  private int nextInitial;
  private int representationRepairs;

  public enum ObjectiveView {
    /** Official jMetal algorithms see exactly Cmax, TEC and TWC in slots 0..2. */
    THREE_OBJECTIVE,
    /** Paper-author algorithms retain their historical 0/1/6 reads; slots 2..5 are inert. */
    AUTHOR_SEVEN_SLOT
  }

  public V35ComparisonProblemAdapter(ZhangBoCanonicalProductionProblem delegate,
      List<? extends PermutationSolution<Integer>> frozenInitialPopulation) {
    this(delegate, frozenInitialPopulation, ObjectiveView.THREE_OBJECTIVE,
        Integer.MAX_VALUE);
  }

  public V35ComparisonProblemAdapter(ZhangBoCanonicalProductionProblem delegate,
      List<? extends PermutationSolution<Integer>> frozenInitialPopulation,
      ObjectiveView objectiveView) {
    this(delegate, frozenInitialPopulation, objectiveView, Integer.MAX_VALUE);
  }

  public V35ComparisonProblemAdapter(ZhangBoCanonicalProductionProblem delegate,
      List<? extends PermutationSolution<Integer>> frozenInitialPopulation,
      ObjectiveView objectiveView, int maxEvaluations) {
    if (delegate == null || frozenInitialPopulation == null
        || frozenInitialPopulation.isEmpty()) {
      throw new IllegalArgumentException("delegate and initial population are required");
    }
    if (delegate.getMode() != ProductionDecodeMode.FM3) {
      throw new IllegalArgumentException("P25E comparisons require explicit FM3");
    }
    if (delegate.getShiftConfiguration().getMode()
        != org.uma.jmetal.problem.multiobjective.dfsp.fatigue.shift.ZhangBoShiftMode.NONE) {
      throw new IllegalArgumentException("P25E comparisons require ShiftMode.NONE");
    }
    if (objectiveView == null) throw new IllegalArgumentException("objectiveView");
    this.delegate = delegate;
    this.objectiveView = objectiveView;
    this.budget = new V35ExactEvaluationBudget(maxEvaluations);
    this.initialPopulation = new ArrayList<>(frozenInitialPopulation.size());
    for (PermutationSolution<Integer> solution : frozenInitialPopulation) {
      this.initialPopulation.add(toComparison(requireFourVector(solution)));
    }
  }

  @Override public int getNumberOfVariables() { return delegate.getNumberOfVariables(); }
  @Override public int getNumberOfObjectives() {
    return objectiveView == ObjectiveView.THREE_OBJECTIVE ? 3
        : ZhangBoCanonicalProductionProblem.NUMBER_OF_OBJECTIVES;
  }
  @Override public int getNumberOfConstraints() { return 0; }
  @Override public int getPermutationLength() { return delegate.getPermutationLength(); }
  @Override public int getNumberOfFactories() { return delegate.getNumberOfFactories(); }
  @Override public String getName() { return "V35-P25E-FM3-ThreeObjective"; }

  @Override
  public synchronized V35ComparisonSolution createSolution() {
    if (nextInitial >= initialPopulation.size()) {
      throw new IllegalStateException(
          "comparison algorithm requested more initial solutions than the frozen population");
    }
    return new V35ComparisonSolution(initialPopulation.get(nextInitial++));
  }

  @Override
  public void evaluate(PermutationSolution<Integer> solution) {
    DhhfspFourVectorSolution comparison = requireFourVector(solution);
    if (comparison.getNumberOfObjectives() != getNumberOfObjectives()) {
      throw new IllegalArgumentException("comparison solution objective layout mismatch");
    }
    synchronizeAuthorMachineAttribute(comparison);
    repairAuthorRepresentation(comparison);
    DhhfspFourVectorSolution canonical = toCanonical(comparison);
    budget.beforeEvaluation(solution);
    delegate.evaluate(canonical);
    budget.afterSuccessfulEvaluation();
    comparison.setObjective(0, canonical.getObjective(0));
    comparison.setObjective(1, canonical.getObjective(1));
    if (objectiveView == ObjectiveView.THREE_OBJECTIVE) {
      comparison.setObjective(2, canonical.getObjective(6));
    } else {
      for (int index = 2; index <= 5; index++) comparison.setObjective(index, 0.0);
      comparison.setObjective(6, canonical.getObjective(6));
    }
    for (java.util.Map.Entry<Object, Object> entry : canonical.getAttributes().entrySet()) {
      comparison.setAttribute(entry.getKey(), entry.getValue());
    }
    if (objectiveView == ObjectiveView.AUTHOR_SEVEN_SLOT) {
      comparison.setAttribute("machine", new ArrayList<Integer>(
          comparison.getMachineAssignments()));
    }
  }

  public ZhangBoCanonicalProductionProblem getCanonicalProblem() { return delegate; }
  public int getInitialPopulationSize() { return initialPopulation.size(); }
  public int getCreatedInitialSolutions() { return nextInitial; }
  public ObjectiveView getObjectiveView() { return objectiveView; }
  public V35ExactEvaluationBudget getBudget() { return budget; }
  public int getRepresentationRepairs() { return representationRepairs; }

  private DhhfspFourVectorSolution toComparison(DhhfspFourVectorSolution source) {
    DhhfspFourVectorSolution result = new DhhfspFourVectorSolution(
        source.getJobSequence(), source.getFactoryAssignments(),
        source.getMachineAssignments(), source.getWorkerAssignments(),
        delegate.getMode().getSemanticTag(), getNumberOfObjectives());
    if (objectiveView == ObjectiveView.AUTHOR_SEVEN_SLOT) {
      result.setAttribute("machine", new ArrayList<Integer>(result.getMachineAssignments()));
    }
    return result;
  }

  private DhhfspFourVectorSolution toCanonical(DhhfspFourVectorSolution source) {
    return new DhhfspFourVectorSolution(source.getJobSequence(), source.getFactoryAssignments(),
        source.getMachineAssignments(), source.getWorkerAssignments(),
        delegate.getMode().getSemanticTag(), ZhangBoCanonicalProductionProblem.NUMBER_OF_OBJECTIVES);
  }

  private static DhhfspFourVectorSolution requireFourVector(
      PermutationSolution<Integer> solution) {
    if (solution instanceof V35ComparisonSolution) {
      return ((V35ComparisonSolution) solution).asFourVector();
    }
    if (!(solution instanceof DhhfspFourVectorSolution)) {
      throw new IllegalArgumentException("P25E requires DhhfspFourVectorSolution");
    }
    return (DhhfspFourVectorSolution) solution;
  }

  @SuppressWarnings("unchecked")
  private void synchronizeAuthorMachineAttribute(DhhfspFourVectorSolution solution) {
    if (objectiveView != ObjectiveView.AUTHOR_SEVEN_SLOT) return;
    Object value = solution.getAttribute("machine");
    if (!(value instanceof List<?>)) {
      throw new IllegalStateException("paper-author solution lost machine vector attribute");
    }
    List<?> machines = (List<?>) value;
    if (machines.size() != solution.getMachineAssignments().size()) {
      throw new IllegalStateException("paper-author machine vector length mismatch");
    }
    for (int index = 0; index < machines.size(); index++) {
      Object machine = machines.get(index);
      if (!(machine instanceof Integer)) {
        throw new IllegalStateException("paper-author machine vector must be integer");
      }
      solution.getMachineAssignments().set(index, (Integer) machine);
    }
  }

  @SuppressWarnings("unchecked")
  private void repairAuthorRepresentation(DhhfspFourVectorSolution solution) {
    if (objectiveView != ObjectiveView.AUTHOR_SEVEN_SLOT) return;
    int evaluationIndex = budget.getEvaluations();
    List<Integer> machineAttribute = (List<Integer>) solution.getAttribute("machine");
    for (int position = 0; position < solution.getNumberOfVariables(); position++) {
      int factory = solution.getFactoryAssignments().get(position);
      if (factory < 0 || factory >= delegate.getInstance().getFactories()) {
        int oldFactory = factory;
        factory = Math.floorMod(factory, delegate.getInstance().getFactories());
        solution.getFactoryAssignments().set(position, factory);
        representationRepairs++;
        V35P25ERepairAudit.record("FA", position, oldFactory, factory, evaluationIndex);
      }
      int machines = delegate.getInstance().getMachineCount(factory, 0);
      int machine = solution.getMachineAssignments().get(position);
      if (machine < 0 || machine >= machines) {
        int oldMachine = machine;
        machine = Math.floorMod(machine, machines);
        solution.getMachineAssignments().set(position, machine);
        machineAttribute.set(position, machine);
        representationRepairs++;
        V35P25ERepairAudit.record("MA", position, oldMachine, machine, evaluationIndex);
      }
      int worker = solution.getWorkerAssignments().get(position);
      if (!delegate.getInstance().isWorkerEligible(factory, 0, worker)) {
        int oldWorker = worker;
        int[] eligible = delegate.getInstance().getEligibleWorkers(factory, 0);
        solution.getWorkerAssignments().set(position,
            eligible[Math.floorMod(worker, eligible.length)]);
        representationRepairs++;
        V35P25ERepairAudit.record("WA", position, oldWorker,
            solution.getWorkerAssignments().get(position), evaluationIndex);
      }
    }
  }
}
