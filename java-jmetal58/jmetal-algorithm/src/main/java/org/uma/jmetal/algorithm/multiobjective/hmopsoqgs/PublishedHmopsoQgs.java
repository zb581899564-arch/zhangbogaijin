package org.uma.jmetal.algorithm.multiobjective.hmopsoqgs;

import org.uma.jmetal.algorithm.Algorithm;
import org.uma.jmetal.problem.multiobjective.dfsp.decoder.DecodeResult;
import org.uma.jmetal.problem.multiobjective.dfsp.decoder.DhhfspProblem;
import org.uma.jmetal.problem.multiobjective.dfsp.decoder.OperationRecord;
import org.uma.jmetal.problem.multiobjective.dfsp.model.DhhfspFourVectorSolution;
import org.uma.jmetal.problem.multiobjective.dfsp.model.DhhfspInstance;
import org.uma.jmetal.util.pseudorandom.PseudoRandomGenerator;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Isolated, three-objective published-baseline HMOPSO-QGS loop. */
public final class PublishedHmopsoQgs
    implements Algorithm<List<DhhfspFourVectorSolution>> {
  private static final long serialVersionUID = 1L;
  private static final String HISTORY = PublishedHmopsoQgs.class.getName() + ".history";

  private final DhhfspProblem problem;
  private final DhhfspInstance instance;
  private final HmopsoQgsConfiguration configuration;
  private final FourVectorOperators operators;
  private final OriginalNeighborhoods neighborhoods;
  private final SubSwarmDecomposer decomposer = new SubSwarmDecomposer();
  private final PddrFf pddr = new PddrFf();
  private final QGbestController qController;
  private final List<String> trace = new ArrayList<>();
  private List<DhhfspFourVectorSolution> result = new ArrayList<>();
  private List<DhhfspFourVectorSolution> archive = new ArrayList<>();

  public PublishedHmopsoQgs(
      DhhfspProblem problem, DhhfspInstance instance,
      HmopsoQgsConfiguration configuration, PseudoRandomGenerator random) {
    if (problem == null || instance == null || configuration == null || random == null) {
      throw new IllegalArgumentException("published baseline dependencies must not be null");
    }
    this.problem = problem;
    this.instance = instance;
    this.configuration = configuration;
    this.operators = new FourVectorOperators(instance, random);
    this.neighborhoods = new OriginalNeighborhoods(instance, random);
    this.qController = new QGbestController(random, configuration.getEpsilon(),
        configuration.getAlpha(), configuration.getGamma());
  }

  @Override
  public void run() {
    trace.clear();
    archive.clear();
    trace.add("CONFIG\n" + configuration.toCanonicalText());
    List<DhhfspFourVectorSolution> population = initialPopulation();
    updateArchive(population);
    int generation = 0;
    while (!budgetExhausted()) {
      long beforeGeneration = evaluations();
      Map<SubSwarm, List<DhhfspFourVectorSolution>> groups =
          decomposer.decompose(population, configuration.getSubSwarmSizes());
      traceGroups(generation, groups);
      for (SubSwarm group : SubSwarm.values()) {
        List<DhhfspFourVectorSolution> current = groups.get(group);
        qController.initialize(group, current);
        int state = 0;
        for (int qRound = 0; qRound < configuration.getQTimes() && !budgetExhausted(); qRound++) {
          List<DhhfspFourVectorSolution> before = copies(current);
          int action = qController.selectAction(group, state);
          DhhfspFourVectorSolution leader = qController.selectLeader(group, action, current);
          List<DhhfspFourVectorSolution> updated = new ArrayList<>();
          for (DhhfspFourVectorSolution particle : current) {
            if (budgetExhausted()) {
              updated.add(particle.copy());
              continue;
            }
            DhhfspFourVectorSolution pbest = personalBest(particle, group, current);
            DhhfspFourVectorSolution child;
            try {
              child = operators.update(particle, pbest, leader, configuration);
            } catch (IllegalArgumentException exception) {
              trace.add("ILLEGAL_REJECT:phase=GLOBAL_UPDATE,group=" + group
                  + ",reason=" + exception.getMessage() + ",fe=" + evaluations());
              updated.add(particle.copy());
              continue;
            }
            problem.evaluate(child);
            inheritAndUpdateHistory(particle, child);
            updated.add(child);
            trace.addAll(prefix("OP", operators.drainEvents()));
          }
          current = updated;
          groups.put(group, current);
          double reward = QGbestController.reward(group, before, current);
          int nextState = QGbestController.stateFor(reward);
          qController.update(group, state, action, reward, nextState);
          trace.add("Q:g=" + generation + ",group=" + group + ",round=" + qRound
              + ",state=" + state + ",action=" + (action + 1)
              + ",reward=" + reward + ",nextState=" + nextState
              + ",phase=BEFORE_LOCAL_SEARCH");
          state = nextState;
        }
      }

      List<DhhfspFourVectorSolution> preLocal = flatten(groups);
      List<DhhfspFourVectorSolution> postLocal = new ArrayList<>();
      for (SubSwarm group : SubSwarm.values()) {
        for (DhhfspFourVectorSolution particle : groups.get(group)) {
          postLocal.add(localSearch(particle, group, groups.get(group), generation));
        }
      }
      List<DhhfspFourVectorSolution> candidates = new ArrayList<>(preLocal);
      candidates.addAll(postLocal);
      population = pddr.select(candidates, configuration.getPopulationSize());
      updateArchive(population);
      trace.add("GENERATION_END:g=" + generation + ",fe=" + evaluations()
          + ",archive=" + archive.size());
      generation++;
      if (evaluations() == beforeGeneration) {
        trace.add("STOP:no_complete_evaluation");
        break;
      }
    }
    trace.add("STOP:fe=" + evaluations() + ",max=" + configuration.getMaxEvaluations()
        + ",stage=" + (budgetExhausted() ? "BUDGET_EXHAUSTED" : "NO_PROGRESS"));
    trace.add("FINAL_Q\n" + qController.toCanonicalText());
    result = pddr.nonDominated(archive);
    Collections.sort(result, fingerprintOrder());
  }

  @Override public List<DhhfspFourVectorSolution> getResult() { return copies(result); }
  @Override public String getName() { return "PublishedHmopsoQgs"; }
  @Override public String getDescription() {
    return "Chapter 4 published_baseline HMOPSO-QGS on jMetal 5.8";
  }

  public List<String> getTrace() { return new ArrayList<>(trace); }
  public String traceText() {
    StringBuilder builder = new StringBuilder();
    for (String line : trace) builder.append(line).append('\n');
    return builder.toString();
  }
  public String qTablesText() { return qController.toCanonicalText(); }

  private List<DhhfspFourVectorSolution> initialPopulation() {
    List<DhhfspFourVectorSolution> population = new ArrayList<>();
    while (population.size() < configuration.getPopulationSize() && !budgetExhausted()) {
      DhhfspFourVectorSolution solution = problem.createSolution();
      problem.evaluate(solution);
      List<DhhfspFourVectorSolution> history = new ArrayList<>();
      history.add(historySnapshot(solution));
      solution.setAttribute(HISTORY, history);
      population.add(solution);
      trace.add("INITIAL:index=" + (population.size() - 1) + ",fe=" + evaluations()
          + ",objectives=" + objectives(solution));
    }
    if (population.size() != configuration.getPopulationSize()) {
      throw new IllegalStateException("budget cannot create complete initial population");
    }
    return population;
  }

  private DhhfspFourVectorSolution localSearch(
      DhhfspFourVectorSolution source, SubSwarm group,
      List<DhhfspFourVectorSolution> reference, int generation) {
    DhhfspFourVectorSolution current = source.copy();
    int[] critical = criticalFactories(current, group);
    DhhfspFourVectorSolution candidate;
    try {
      candidate = neighborhoods.criticalFactorySwap(current, critical[0], critical[1]);
      current = evaluateAndAccept(current, candidate, group, reference,
          "CRITICAL_FACTORY_SWAP", generation);
      candidate = neighborhoods.criticalFactoryInsert(current, critical[0], critical[1]);
      current = evaluateAndAccept(current, candidate, group, reference,
          "CRITICAL_FACTORY_INSERT", generation);
    } catch (IllegalArgumentException exception) {
      trace.add("ILLEGAL_REJECT:phase=CRITICAL_SEARCH,group=" + group
          + ",reason=" + exception.getMessage() + ",fe=" + evaluations());
    }
    for (int pass = 0; pass < configuration.getLocalSearchTimes(); pass++) {
      for (int operation = 1; operation <= 9 && !budgetExhausted(); operation++) {
        critical = criticalFactories(current, group);
        try {
          candidate = neighborhoods.apply(operation, current, critical[0]);
          current = evaluateAndAccept(current, candidate, group, reference,
              neighborhoods.name(operation), generation);
        } catch (IllegalArgumentException exception) {
          trace.add("ILLEGAL_REJECT:phase=VNS,op=" + neighborhoods.name(operation)
              + ",reason=" + exception.getMessage() + ",fe=" + evaluations());
        }
      }
    }
    return current;
  }

  private DhhfspFourVectorSolution evaluateAndAccept(
      DhhfspFourVectorSolution current, DhhfspFourVectorSolution candidate,
      SubSwarm group, List<DhhfspFourVectorSolution> reference,
      String operation, int generation) {
    if (budgetExhausted()) {
      trace.add("LS:g=" + generation + ",op=" + operation + ",status=BUDGET_STOP");
      return current;
    }
    problem.evaluate(candidate);
    inheritAndUpdateHistory(current, candidate);
    boolean accepted = betterOrEqual(group, candidate, current, reference);
    trace.add("LS:g=" + generation + ",op=" + operation + ",accepted=" + accepted
        + ",fe=" + evaluations() + ",before=" + objectives(current)
        + ",after=" + objectives(candidate));
    return accepted ? candidate : current;
  }

  private boolean betterOrEqual(
      SubSwarm group, DhhfspFourVectorSolution candidate,
      DhhfspFourVectorSolution current, List<DhhfspFourVectorSolution> reference) {
    if (group.isBoundary()) {
      return candidate.getObjective(group.getObjective())
          <= current.getObjective(group.getObjective());
    }
    List<DhhfspFourVectorSolution> set = new ArrayList<>(reference);
    set.add(current);
    set.add(candidate);
    int value = Double.compare(pddr.score(candidate, set), pddr.score(current, set));
    if (value == 0) value = PddrFf.fingerprint(candidate).compareTo(PddrFf.fingerprint(current));
    return value <= 0;
  }

  private int[] criticalFactories(DhhfspFourVectorSolution solution, SubSwarm group) {
    double[] values = new double[instance.getNumberOfFactories()];
    DecodeResult decode = (DecodeResult) solution.getAttribute(DecodeResult.class);
    for (OperationRecord operation : decode.getFinalSnapshot().getOperations()) {
      int factory = operation.getFactory();
      if (group == SubSwarm.G1_CMAX) {
        if (operation.getStage() == instance.getNumberOfStages() - 1) {
          values[factory] = Math.max(values[factory], operation.getEndTime());
        }
      } else if (group == SubSwarm.G2_TEC) {
        values[factory] += operation.getDuration()
            * instance.getMachineEnergyPerUnit(factory, operation.getStage())[operation.getMachine()];
      } else if (group == SubSwarm.G3_TWC) {
        values[factory] += operation.getDuration()
            * instance.getWorkerCostPerUnit(factory, operation.getStage())[operation.getWorker()];
      } else {
        values[factory] += operation.getEndTime();
      }
    }
    int maximum = 0;
    int minimum = 0;
    for (int factory = 1; factory < values.length; factory++) {
      if (values[factory] > values[maximum]) maximum = factory;
      if (values[factory] < values[minimum]) minimum = factory;
    }
    return new int[] {maximum, minimum};
  }

  @SuppressWarnings("unchecked")
  private DhhfspFourVectorSolution personalBest(
      DhhfspFourVectorSolution particle, SubSwarm group,
      List<DhhfspFourVectorSolution> reference) {
    Object value = particle.getAttribute(HISTORY);
    List<DhhfspFourVectorSolution> history = value instanceof List
        ? (List<DhhfspFourVectorSolution>) value : Collections.singletonList(particle);
    DhhfspFourVectorSolution best = history.get(0);
    for (DhhfspFourVectorSolution candidate : history) {
      if (betterOrEqual(group, candidate, best, reference)) best = candidate;
    }
    return best.copy();
  }

  @SuppressWarnings("unchecked")
  private void inheritAndUpdateHistory(
      DhhfspFourVectorSolution parent, DhhfspFourVectorSolution child) {
    List<DhhfspFourVectorSolution> history = new ArrayList<>();
    Object value = parent.getAttribute(HISTORY);
    if (value instanceof List) {
      for (DhhfspFourVectorSolution item : (List<DhhfspFourVectorSolution>) value) {
        history.add(historySnapshot(item));
      }
    } else {
      history.add(historySnapshot(parent));
    }
    history.add(historySnapshot(child));
    history = pddr.nonDominated(history);
    child.setAttribute(HISTORY, history);
  }

  private void updateArchive(List<DhhfspFourVectorSolution> population) {
    List<DhhfspFourVectorSolution> union = new ArrayList<>(archive);
    union.addAll(population);
    archive = pddr.nonDominated(union);
  }

  private boolean budgetExhausted() {
    return evaluations() >= configuration.getMaxEvaluations();
  }
  private long evaluations() { return problem.getEvaluationCounter().getSuccessfulEvaluations(); }

  private static List<DhhfspFourVectorSolution> flatten(
      Map<SubSwarm, List<DhhfspFourVectorSolution>> groups) {
    List<DhhfspFourVectorSolution> result = new ArrayList<>();
    for (SubSwarm group : SubSwarm.values()) result.addAll(copies(groups.get(group)));
    return result;
  }
  private static List<DhhfspFourVectorSolution> copies(List<DhhfspFourVectorSolution> source) {
    List<DhhfspFourVectorSolution> result = new ArrayList<>();
    for (DhhfspFourVectorSolution solution : source) result.add(solution.copy());
    return result;
  }
  private static DhhfspFourVectorSolution historySnapshot(DhhfspFourVectorSolution source) {
    DhhfspFourVectorSolution copy = source.copy();
    copy.getAttributes().remove(HISTORY);
    return copy;
  }
  private static List<String> prefix(String prefix, List<String> values) {
    List<String> result = new ArrayList<>();
    for (String value : values) result.add(prefix + ':' + value);
    return result;
  }
  private static String objectives(DhhfspFourVectorSolution solution) {
    return '[' + Double.toString(solution.getObjective(0)) + ','
        + Double.toString(solution.getObjective(1)) + ','
        + Double.toString(solution.getObjective(2)) + ']';
  }
  private void traceGroups(
      int generation, Map<SubSwarm, List<DhhfspFourVectorSolution>> groups) {
    for (SubSwarm group : SubSwarm.values()) {
      StringBuilder line = new StringBuilder("GROUP:g=").append(generation)
          .append(",name=").append(group).append(",members=");
      for (DhhfspFourVectorSolution member : groups.get(group)) {
        line.append(PddrFf.fingerprint(member)).append('@').append(objectives(member)).append(';');
      }
      trace.add(line.toString());
    }
  }
  private static Comparator<DhhfspFourVectorSolution> fingerprintOrder() {
    return new Comparator<DhhfspFourVectorSolution>() {
      @Override public int compare(DhhfspFourVectorSolution left, DhhfspFourVectorSolution right) {
        return PddrFf.fingerprint(left).compareTo(PddrFf.fingerprint(right));
      }
    };
  }
}
