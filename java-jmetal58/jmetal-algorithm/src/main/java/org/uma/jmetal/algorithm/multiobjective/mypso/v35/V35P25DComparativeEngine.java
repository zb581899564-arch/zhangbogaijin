package org.uma.jmetal.algorithm.multiobjective.mypso.v35;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.uma.jmetal.algorithm.multiobjective.mypso.zhangbo.ZhangBoBaselineUpdater;
import org.uma.jmetal.algorithm.multiobjective.mypso.zhangbo.ZhangBoMachineVectorSupport;
import org.uma.jmetal.algorithm.multiobjective.mypso.zhangbo.ZhangBoQgController;
import org.uma.jmetal.algorithm.multiobjective.mypso.zhangbo.ZhangBoResourceDomain;
import org.uma.jmetal.problem.multiobjective.dfsp.fatigue.ZhangBoCanonicalProductionProblem;
import org.uma.jmetal.solution.PermutationSolution;
import org.uma.jmetal.util.pseudorandom.PseudoRandomGenerator;
import org.uma.jmetal.util.pseudorandom.impl.JavaRandomGenerator;

/**
 * Deterministic four-vector adapters used only by the P25D comparison pilot.
 *
 * <p>The legacy comparison implementations cannot be used by the canonical
 * experiment: they allocate unseeded {@code Random} objects, read static
 * resource domains and account iterations rather than decoder calls.  This
 * engine keeps the published family structure (NSGA-II, SPEA2, MOPSO,
 * MOPSODS-DE, MOHEA-DE and HMOPSO-QLS) while binding every candidate to the
 * same canonical four-vector problem and successful-evaluation counter.</p>
 */
@Deprecated
public final class V35P25DComparativeEngine {
  /** Historical engineering comparator only; never admit its fronts to P25E. */
  public static final boolean LEGACY_ENHANCED_COMPARATOR_REWRITE = true;
  public static final boolean VALID_FOR_PAPER_COMPARISON = false;
  public enum Algorithm {
    HMOPSO_QLS_F("HMOPSO-QLS-F", Kind.PSO_QLS,
        Params.pso(0.5, 0.20, 0.50, 0.40, 0.08, 0.15, 0.15, 50, 40)),
    MOPSO_F("MOPSO-F", Kind.PSO,
        Params.pso(0.5, 0.30, 0.30, 0.40, 0.06, 0.20, 0.20, 0, 0)),
    MOPSODS_DE_F("MOPSODS-DE-F", Kind.PSO_DE,
        Params.ga(0.50, 0.20, 0.40, 0.30, 0.40, 0.06, 0.15, 0.20, 0.80)),
    MOHEADE_F("MOHEADE-F", Kind.MOHEA_DE,
        Params.ga(0.50, 0.30, 0.50, 0.40, 0.40, 0.08, 0.20, 0.20, 0.80)),
    NSGA_II_F("NSGA-II-F", Kind.NSGA_II,
        Params.ga(0.40, 0.30, 0.30, 0.40, 0.30, 0.04, 0.15, 0.15, 0.0)),
    SPEA2_F("SPEA2-F", Kind.SPEA2,
        Params.ga(0.50, 0.20, 0.30, 0.30, 0.30, 0.04, 0.10, 0.15, 0.0));

    private final String label;
    private final Kind kind;
    private final Params params;
    Algorithm(String label, Kind kind, Params params) {
      this.label = label; this.kind = kind; this.params = params;
    }
    public String getLabel() { return label; }
    public String canonicalParameters() { return params.canonicalText(); }
  }

  private enum Kind { NSGA_II, SPEA2, PSO, PSO_DE, MOHEA_DE, PSO_QLS }

  public static final class Result implements Serializable {
    private static final long serialVersionUID = 1L;
    private final Algorithm algorithm;
    private final int evaluations;
    private final List<double[]> front;
    private final long runNanos;
    private final long generations;
    private final long offspring;
    private final long localCandidates;
    private final long deCandidates;
    private final String eventHash;

    private Result(Algorithm algorithm, int evaluations, List<double[]> front,
        long runNanos, long generations, long offspring, long localCandidates,
        long deCandidates, String eventHash) {
      this.algorithm = algorithm; this.evaluations = evaluations;
      this.front = front; this.runNanos = runNanos; this.generations = generations;
      this.offspring = offspring; this.localCandidates = localCandidates;
      this.deCandidates = deCandidates; this.eventHash = eventHash;
    }
    public Algorithm getAlgorithm() { return algorithm; }
    public int getEvaluations() { return evaluations; }
    public List<double[]> getFront() { return front; }
    public long getRunNanos() { return runNanos; }
    public long getGenerations() { return generations; }
    public long getOffspring() { return offspring; }
    public long getLocalCandidates() { return localCandidates; }
    public long getDeCandidates() { return deCandidates; }
    public String getEventHash() { return eventHash; }
    public String mechanismSummary() {
      return "adapter=" + algorithm.getLabel() + ",generations=" + generations
          + ",offspring=" + offspring + ",localCandidates=" + localCandidates
          + ",deCandidates=" + deCandidates + ",eventHash=" + eventHash;
    }
  }

  private V35P25DComparativeEngine() { }

  public static Result run(Algorithm algorithm, ZhangBoCanonicalProductionProblem problem,
      List<PermutationSolution<Integer>> initialPopulation, int maxEvaluations, long seed) {
    if (algorithm == null || problem == null || initialPopulation == null
        || initialPopulation.isEmpty() || maxEvaluations < initialPopulation.size()) {
      throw new IllegalArgumentException("invalid P25D comparative request");
    }
    if (problem.getEvaluationCounter().getSuccessfulEvaluations() != 0L) {
      throw new IllegalArgumentException("P25D requires a fresh canonical problem");
    }
    PseudoRandomGenerator random = new JavaRandomGenerator(domainSeed(seed, algorithm.ordinal()));
    ZhangBoResourceDomain domain = new ZhangBoResourceDomain(problem.getInstance());
    List<PermutationSolution<Integer>> population = copy(initialPopulation);
    for (PermutationSolution<Integer> solution : population) problem.evaluate(solution);
    List<PermutationSolution<Integer>> archive = nondominated(copy(population));
    List<PermutationSolution<Integer>> personalBest = copy(population);
    Counters counters = new Counters();
    DigestEvents events = new DigestEvents();
    long start = System.nanoTime();

    while (problem.getEvaluationCounter().getSuccessfulEvaluations()
        + population.size() <= maxEvaluations) {
      List<PermutationSolution<Integer>> children = new ArrayList<>(population.size());
      if (algorithm.kind == Kind.PSO || algorithm.kind == Kind.PSO_DE
          || algorithm.kind == Kind.PSO_QLS) {
        makePsoGeneration(algorithm, population, personalBest, archive, children,
            domain, random, counters, events);
      } else {
        makeEvolutionaryGeneration(algorithm, population, archive, children,
            domain, random, counters, events);
      }
      for (PermutationSolution<Integer> child : children) problem.evaluate(child);
      counters.offspring += children.size();

      if (algorithm.kind == Kind.NSGA_II) {
        population = environmental(union(population, children), population.size());
      } else if (algorithm.kind == Kind.SPEA2) {
        archive = speaEnvironmental(union(population, children), population.size());
        population = copy(archive);
      } else if (algorithm.kind == Kind.MOHEA_DE) {
        archive = nondominated(union(archive, children));
        population = environmental(union(population, children, archive), population.size());
      } else {
        updatePersonalBest(personalBest, children, random);
        population = environmental(union(population, children), population.size());
      }
      archive = boundedArchive(nondominated(union(archive, population, children)),
          Math.max(population.size(), 200));
      counters.generations++;
    }

    // The approved pilot budget is divisible by the common population size.
    // Fail closed instead of producing a partial generation.
    int evaluations = (int) problem.getEvaluationCounter().getSuccessfulEvaluations();
    if (evaluations != maxEvaluations) {
      throw new IllegalStateException("P25D adapter stopped at " + evaluations
          + " instead of exact budget " + maxEvaluations);
    }
    List<PermutationSolution<Integer>> result = nondominated(union(archive, population));
    List<double[]> front = new ArrayList<>();
    for (PermutationSolution<Integer> solution : result) {
      front.add(new double[] {solution.getObjective(0), solution.getObjective(1),
          solution.getObjective(6)});
    }
    sortPoints(front);
    return new Result(algorithm, evaluations, front, System.nanoTime() - start,
        counters.generations, counters.offspring, counters.localCandidates,
        counters.deCandidates, events.hex());
  }

  private static void makePsoGeneration(Algorithm algorithm,
      List<PermutationSolution<Integer>> population,
      List<PermutationSolution<Integer>> personalBest,
      List<PermutationSolution<Integer>> archive,
      List<PermutationSolution<Integer>> children, ZhangBoResourceDomain domain,
      PseudoRandomGenerator random, Counters counters, DigestEvents events) {
    ZhangBoBaselineUpdater updater = new ZhangBoBaselineUpdater();
    Params p = algorithm.params;
    int localQuota = algorithm.kind == Kind.PSO_QLS && p.qTimes > 0
        && (counters.generations + 1L) % p.qTimes == 0L ? p.lsTimes : 0;
    for (int index = 0; index < population.size(); index++) {
      PermutationSolution<Integer> current = population.get(index);
      PermutationSolution<Integer> leader = directionalLeader(archive, index, population.size(), random);
      PermutationSolution<Integer> child;
      if (index < localQuota) {
        child = localCandidate(current, domain, random, index % 4, events);
        counters.localCandidates++;
      } else {
        ZhangBoBaselineUpdater.Result update = updater.update(current,
            personalBest.get(index), leader, domain, p.randUpper,
            p.faCross, p.maCross, p.waCross, p.faMutation, p.maMutation,
            p.waMutation, random);
        child = update.getSolution();
        events.add("PSO:" + index + ':' + update.getEvents().size());
      }
      if (algorithm.kind == Kind.PSO_DE && random.nextDouble() < p.deProbability) {
        applyDiscreteDe(child, population, domain, random, events);
        counters.deCandidates++;
      }
      children.add(child);
    }
  }

  private static void makeEvolutionaryGeneration(Algorithm algorithm,
      List<PermutationSolution<Integer>> population,
      List<PermutationSolution<Integer>> archive,
      List<PermutationSolution<Integer>> children, ZhangBoResourceDomain domain,
      PseudoRandomGenerator random, Counters counters, DigestEvents events) {
    Params p = algorithm.params;
    List<PermutationSolution<Integer>> pool = algorithm.kind == Kind.SPEA2
        ? union(population, archive) : population;
    while (children.size() < population.size()) {
      PermutationSolution<Integer> first = tournament(pool, random);
      PermutationSolution<Integer> second = tournament(pool, random);
      PermutationSolution<Integer> child = gaChild(first, second, p, domain, random, events);
      if (algorithm.kind == Kind.MOHEA_DE && random.nextDouble() < p.deProbability) {
        applyDiscreteDe(child, pool, domain, random, events);
        counters.deCandidates++;
      }
      children.add(child);
    }
  }

  private static PermutationSolution<Integer> gaChild(PermutationSolution<Integer> first,
      PermutationSolution<Integer> second, Params p, ZhangBoResourceDomain domain,
      PseudoRandomGenerator random, DigestEvents events) {
    PermutationSolution<Integer> child = copy(first);
    int jobs = child.getNumberOfVariables();
    if (random.nextDouble() < p.jsCross) orderCrossover(child, second, random);
    if (random.nextDouble() < p.jsMutation && jobs > 1) {
      swap(child.getVariables(), random.nextInt(0, jobs - 1), random.nextInt(0, jobs - 1));
    }
    List<Integer> machines = ZhangBoMachineVectorSupport.copy(child, jobs);
    List<Integer> otherMachines = ZhangBoMachineVectorSupport.copy(second, jobs);
    int left = random.nextInt(0, jobs - 1);
    int right = random.nextInt(0, jobs - 1);
    if (left > right) { int value = left; left = right; right = value; }
    for (int position = left; position <= right; position++) {
      if (random.nextDouble() < p.faCross) child.getVariablesid().set(position,
          second.getVariableValueid(position));
      if (random.nextDouble() < p.maCross) machines.set(position, otherMachines.get(position));
      if (random.nextDouble() < p.waCross) child.getVariablesworker().set(position,
          second.getVariableValueworker(position));
    }
    for (int position = 0; position < jobs; position++) {
      if (random.nextDouble() < p.faMutation) {
        int factory = random.nextInt(0, domain.getFactoryCount() - 1);
        child.getVariablesid().set(position, factory);
        machines.set(position, random.nextInt(0, domain.getMachineCount(factory) - 1));
        int[] workers = domain.getWorkers(factory);
        child.getVariablesworker().set(position, workers[random.nextInt(0, workers.length - 1)]);
      }
      int factory = child.getVariableValueid(position);
      if (random.nextDouble() < p.maMutation) {
        machines.set(position, random.nextInt(0, domain.getMachineCount(factory) - 1));
      }
      if (random.nextDouble() < p.waMutation) {
        int[] workers = domain.getWorkers(factory);
        child.getVariablesworker().set(position, workers[random.nextInt(0, workers.length - 1)]);
      }
      repairPosition(child, machines, position, domain);
    }
    ZhangBoMachineVectorSupport.write(child, machines);
    events.add("GA:" + left + '-' + right);
    return child;
  }

  private static void orderCrossover(PermutationSolution<Integer> child,
      PermutationSolution<Integer> donor, PseudoRandomGenerator random) {
    int jobs = child.getNumberOfVariables();
    int left = random.nextInt(0, jobs - 1);
    int right = random.nextInt(0, jobs - 1);
    if (left > right) { int value = left; left = right; right = value; }
    List<Integer> result = new ArrayList<>(Collections.nCopies(jobs, -1));
    for (int index = left; index <= right; index++) result.set(index, child.getVariableValue(index));
    int write = (right + 1) % jobs;
    for (int offset = 0; offset < jobs; offset++) {
      int value = donor.getVariableValue((right + 1 + offset) % jobs);
      if (!result.contains(value)) { result.set(write, value); write = (write + 1) % jobs; }
    }
    for (int index = 0; index < jobs; index++) child.setVariableValue(index, result.get(index));
  }

  private static void applyDiscreteDe(PermutationSolution<Integer> child,
      List<PermutationSolution<Integer>> pool, ZhangBoResourceDomain domain,
      PseudoRandomGenerator random, DigestEvents events) {
    if (pool.size() < 3) return;
    PermutationSolution<Integer> a = pool.get(random.nextInt(0, pool.size() - 1));
    PermutationSolution<Integer> b = pool.get(random.nextInt(0, pool.size() - 1));
    PermutationSolution<Integer> c = pool.get(random.nextInt(0, pool.size() - 1));
    int jobs = child.getNumberOfVariables();
    int position = random.nextInt(0, jobs - 1);
    int donorPosition = b.getVariables().indexOf(c.getVariableValue(position));
    if (donorPosition >= 0) swap(child.getVariables(), position, donorPosition);
    List<Integer> machines = ZhangBoMachineVectorSupport.copy(child, jobs);
    if (!b.getVariableValueid(position).equals(c.getVariableValueid(position))) {
      child.getVariablesid().set(position, a.getVariableValueid(position));
    }
    int factory = child.getVariableValueid(position);
    List<Integer> aMachines = ZhangBoMachineVectorSupport.copy(a, jobs);
    if (domain.isMachineValid(factory, aMachines.get(position))) {
      machines.set(position, aMachines.get(position));
    }
    if (domain.isWorkerValid(factory, a.getVariableValueworker(position))) {
      child.getVariablesworker().set(position, a.getVariableValueworker(position));
    }
    repairPosition(child, machines, position, domain);
    ZhangBoMachineVectorSupport.write(child, machines);
    events.add("DE:" + position);
  }

  private static PermutationSolution<Integer> localCandidate(
      PermutationSolution<Integer> source, ZhangBoResourceDomain domain,
      PseudoRandomGenerator random, int action, DigestEvents events) {
    PermutationSolution<Integer> child = copy(source);
    int jobs = child.getNumberOfVariables();
    int position = random.nextInt(0, jobs - 1);
    List<Integer> machines = ZhangBoMachineVectorSupport.copy(child, jobs);
    if (action == 0 && jobs > 1) {
      swap(child.getVariables(), position, random.nextInt(0, jobs - 1));
    } else if (action == 1) {
      int factory = random.nextInt(0, domain.getFactoryCount() - 1);
      child.getVariablesid().set(position, factory);
    } else if (action == 2) {
      int factory = child.getVariableValueid(position);
      machines.set(position, random.nextInt(0, domain.getMachineCount(factory) - 1));
    } else {
      int factory = child.getVariableValueid(position);
      int[] workers = domain.getWorkers(factory);
      child.getVariablesworker().set(position, workers[random.nextInt(0, workers.length - 1)]);
    }
    repairPosition(child, machines, position, domain);
    ZhangBoMachineVectorSupport.write(child, machines);
    events.add("QLS:" + action + ':' + position);
    return child;
  }

  private static void repairPosition(PermutationSolution<Integer> solution,
      List<Integer> machines, int position, ZhangBoResourceDomain domain) {
    int factory = solution.getVariableValueid(position);
    if (!domain.isFactoryValid(factory)) {
      factory = 0; solution.getVariablesid().set(position, factory);
    }
    if (!domain.isMachineValid(factory, machines.get(position))) {
      machines.set(position, domain.firstMachine(factory));
    }
    if (!domain.isWorkerValid(factory, solution.getVariableValueworker(position))) {
      solution.getVariablesworker().set(position, domain.firstWorker(factory));
    }
  }

  private static PermutationSolution<Integer> tournament(
      List<PermutationSolution<Integer>> pool, PseudoRandomGenerator random) {
    PermutationSolution<Integer> a = pool.get(random.nextInt(0, pool.size() - 1));
    PermutationSolution<Integer> b = pool.get(random.nextInt(0, pool.size() - 1));
    int dominance = dominance(a, b);
    if (dominance < 0) return a;
    if (dominance > 0) return b;
    int cmp = Double.compare(scalar(a), scalar(b));
    if (cmp < 0) return a;
    if (cmp > 0) return b;
    return ZhangBoQgController.fingerprint(a).compareTo(ZhangBoQgController.fingerprint(b)) <= 0
        ? a : b;
  }

  private static PermutationSolution<Integer> directionalLeader(
      List<PermutationSolution<Integer>> archive, int index, int populationSize,
      PseudoRandomGenerator random) {
    if (archive.isEmpty()) throw new IllegalStateException("empty leader archive");
    final int objective;
    int quarter = Math.max(1, populationSize / 5);
    if (index < quarter) objective = 0;
    else if (index >= populationSize - quarter * 2) objective = index < populationSize - quarter ? 1 : 6;
    else objective = -1;
    List<PermutationSolution<Integer>> candidates = new ArrayList<>(archive);
    Collections.sort(candidates, new Comparator<PermutationSolution<Integer>>() {
      @Override public int compare(PermutationSolution<Integer> a,
          PermutationSolution<Integer> b) {
        int cmp = objective < 0 ? Double.compare(scalar(a), scalar(b))
            : Double.compare(a.getObjective(objective), b.getObjective(objective));
        if (cmp != 0) return cmp;
        return ZhangBoQgController.fingerprint(a).compareTo(ZhangBoQgController.fingerprint(b));
      }
    });
    int limit = Math.min(10, candidates.size());
    return candidates.get(random.nextInt(0, limit - 1));
  }

  private static void updatePersonalBest(List<PermutationSolution<Integer>> personalBest,
      List<PermutationSolution<Integer>> children, PseudoRandomGenerator random) {
    for (int index = 0; index < personalBest.size(); index++) {
      int comparison = dominance(children.get(index), personalBest.get(index));
      if (comparison < 0 || (comparison == 0 && random.nextDouble() < 0.5)) {
        personalBest.set(index, copy(children.get(index)));
      }
    }
  }

  private static List<PermutationSolution<Integer>> environmental(
      List<PermutationSolution<Integer>> candidates, int size) {
    List<List<PermutationSolution<Integer>>> fronts = rank(candidates);
    List<PermutationSolution<Integer>> result = new ArrayList<>(size);
    for (List<PermutationSolution<Integer>> front : fronts) {
      if (result.size() + front.size() <= size) result.addAll(copy(front));
      else {
        Map<PermutationSolution<Integer>, Double> distance = crowding(front);
        Collections.sort(front, (a, b) -> {
          int cmp = -Double.compare(distance.get(a), distance.get(b));
          if (cmp != 0) return cmp;
          return ZhangBoQgController.fingerprint(a).compareTo(ZhangBoQgController.fingerprint(b));
        });
        for (PermutationSolution<Integer> value : front) {
          if (result.size() == size) break;
          result.add(copy(value));
        }
        break;
      }
    }
    return result;
  }

  private static List<PermutationSolution<Integer>> speaEnvironmental(
      List<PermutationSolution<Integer>> candidates, int size) {
    final Map<PermutationSolution<Integer>, Double> fitness = new HashMap<>();
    for (PermutationSolution<Integer> a : candidates) {
      int raw = 0;
      for (PermutationSolution<Integer> b : candidates) if (dominance(b, a) < 0) {
        for (PermutationSolution<Integer> c : candidates) if (dominance(b, c) < 0) raw++;
      }
      double nearest = Double.POSITIVE_INFINITY;
      for (PermutationSolution<Integer> b : candidates) if (a != b) {
        nearest = Math.min(nearest, objectiveDistance(a, b));
      }
      fitness.put(a, raw + 1.0 / (2.0 + nearest));
    }
    List<PermutationSolution<Integer>> sorted = new ArrayList<>(candidates);
    Collections.sort(sorted, (a, b) -> {
      int cmp = Double.compare(fitness.get(a), fitness.get(b));
      if (cmp != 0) return cmp;
      return ZhangBoQgController.fingerprint(a).compareTo(ZhangBoQgController.fingerprint(b));
    });
    return copy(sorted.subList(0, Math.min(size, sorted.size())));
  }

  private static List<List<PermutationSolution<Integer>>> rank(
      List<PermutationSolution<Integer>> values) {
    List<PermutationSolution<Integer>> remaining = unique(values);
    List<List<PermutationSolution<Integer>>> result = new ArrayList<>();
    while (!remaining.isEmpty()) {
      List<PermutationSolution<Integer>> front = new ArrayList<>();
      for (PermutationSolution<Integer> candidate : remaining) {
        boolean dominated = false;
        for (PermutationSolution<Integer> other : remaining) {
          if (candidate != other && dominance(other, candidate) < 0) { dominated = true; break; }
        }
        if (!dominated) front.add(candidate);
      }
      if (front.isEmpty()) throw new IllegalStateException("Pareto ranking made no progress");
      result.add(front); remaining.removeAll(front);
    }
    return result;
  }

  private static Map<PermutationSolution<Integer>, Double> crowding(
      List<PermutationSolution<Integer>> front) {
    Map<PermutationSolution<Integer>, Double> result = new LinkedHashMap<>();
    for (PermutationSolution<Integer> value : front) result.put(value, 0.0);
    if (front.size() <= 2) {
      for (PermutationSolution<Integer> value : front) result.put(value, Double.POSITIVE_INFINITY);
      return result;
    }
    int[] objectives = {0, 1, 6};
    for (final int objective : objectives) {
      List<PermutationSolution<Integer>> sorted = new ArrayList<>(front);
      Collections.sort(sorted, Comparator.comparingDouble(a -> a.getObjective(objective)));
      result.put(sorted.get(0), Double.POSITIVE_INFINITY);
      result.put(sorted.get(sorted.size() - 1), Double.POSITIVE_INFINITY);
      double range = sorted.get(sorted.size() - 1).getObjective(objective)
          - sorted.get(0).getObjective(objective);
      if (range <= 1.0e-12) continue;
      for (int index = 1; index + 1 < sorted.size(); index++) {
        PermutationSolution<Integer> value = sorted.get(index);
        if (!Double.isInfinite(result.get(value))) {
          result.put(value, result.get(value)
              + (sorted.get(index + 1).getObjective(objective)
              - sorted.get(index - 1).getObjective(objective)) / range);
        }
      }
    }
    return result;
  }

  private static List<PermutationSolution<Integer>> boundedArchive(
      List<PermutationSolution<Integer>> archive, int size) {
    return archive.size() <= size ? archive : environmental(archive, size);
  }

  private static List<PermutationSolution<Integer>> nondominated(
      List<PermutationSolution<Integer>> values) {
    return rank(values).get(0);
  }

  private static List<PermutationSolution<Integer>> unique(
      List<PermutationSolution<Integer>> values) {
    Map<String, PermutationSolution<Integer>> result = new LinkedHashMap<>();
    for (PermutationSolution<Integer> value : values) {
      String key = ZhangBoQgController.fingerprint(value) + '|' + value.getObjective(0)
          + '|' + value.getObjective(1) + '|' + value.getObjective(6);
      if (!result.containsKey(key)) result.put(key, value);
    }
    return new ArrayList<>(result.values());
  }

  private static int dominance(PermutationSolution<Integer> a,
      PermutationSolution<Integer> b) {
    boolean aBetter = false; boolean bBetter = false;
    int[] objectives = {0, 1, 6};
    for (int objective : objectives) {
      if (a.getObjective(objective) < b.getObjective(objective)) aBetter = true;
      else if (a.getObjective(objective) > b.getObjective(objective)) bBetter = true;
    }
    if (aBetter && !bBetter) return -1;
    if (bBetter && !aBetter) return 1;
    return 0;
  }

  private static double scalar(PermutationSolution<Integer> value) {
    return value.getObjective(0) + value.getObjective(1) + value.getObjective(6);
  }

  private static double objectiveDistance(PermutationSolution<Integer> a,
      PermutationSolution<Integer> b) {
    double d0 = a.getObjective(0) - b.getObjective(0);
    double d1 = a.getObjective(1) - b.getObjective(1);
    double d2 = a.getObjective(6) - b.getObjective(6);
    return Math.sqrt(d0 * d0 + d1 * d1 + d2 * d2);
  }

  @SafeVarargs
  private static List<PermutationSolution<Integer>> union(
      List<PermutationSolution<Integer>>... groups) {
    List<PermutationSolution<Integer>> result = new ArrayList<>();
    for (List<PermutationSolution<Integer>> group : groups) result.addAll(group);
    return result;
  }

  private static List<PermutationSolution<Integer>> copy(
      List<PermutationSolution<Integer>> values) {
    List<PermutationSolution<Integer>> result = new ArrayList<>(values.size());
    for (PermutationSolution<Integer> value : values) result.add(copy(value));
    return result;
  }

  @SuppressWarnings("unchecked")
  private static PermutationSolution<Integer> copy(PermutationSolution<Integer> value) {
    return (PermutationSolution<Integer>) value.copy();
  }

  private static void swap(List<Integer> values, int first, int second) {
    Integer value = values.get(first); values.set(first, values.get(second)); values.set(second, value);
  }

  private static void sortPoints(List<double[]> points) {
    Collections.sort(points, (a, b) -> {
      int cmp = Double.compare(a[0], b[0]);
      if (cmp == 0) cmp = Double.compare(a[1], b[1]);
      if (cmp == 0) cmp = Double.compare(a[2], b[2]);
      return cmp;
    });
  }

  private static long domainSeed(long seed, int ordinal) {
    long value = seed ^ (0x9E3779B97F4A7C15L * (ordinal + 1L));
    value ^= value >>> 33; value *= 0xff51afd7ed558ccdL;
    value ^= value >>> 33; value *= 0xc4ceb9fe1a85ec53L;
    return value ^ (value >>> 33);
  }

  private static final class Counters {
    private long generations; private long offspring;
    private long localCandidates; private long deCandidates;
  }

  private static final class DigestEvents {
    private final java.security.MessageDigest digest;
    private DigestEvents() {
      try { digest = java.security.MessageDigest.getInstance("SHA-256"); }
      catch (java.security.NoSuchAlgorithmException error) { throw new IllegalStateException(error); }
    }
    private void add(String value) {
      digest.update(value.getBytes(java.nio.charset.StandardCharsets.UTF_8));
      digest.update((byte) '\n');
    }
    private String hex() {
      byte[] bytes = digest.digest(); StringBuilder out = new StringBuilder();
      for (byte value : bytes) out.append(String.format("%02x", value & 0xff));
      return out.toString();
    }
  }

  private static final class Params {
    private final double randUpper;
    private final double jsCross, faCross, maCross, waCross;
    private final double jsMutation, faMutation, maMutation, waMutation;
    private final double deProbability;
    private final int qTimes, lsTimes;
    private Params(double randUpper, double jsCross, double faCross, double maCross,
        double waCross, double jsMutation, double faMutation, double maMutation,
        double waMutation, double deProbability, int qTimes, int lsTimes) {
      this.randUpper = randUpper; this.jsCross = jsCross; this.faCross = faCross;
      this.maCross = maCross; this.waCross = waCross; this.jsMutation = jsMutation;
      this.faMutation = faMutation; this.maMutation = maMutation;
      this.waMutation = waMutation; this.deProbability = deProbability;
      this.qTimes = qTimes; this.lsTimes = lsTimes;
    }
    private static Params ga(double jsC, double faC, double maC, double waC,
        double jsM, double faM, double maM, double waM, double de) {
      return new Params(0.0, jsC, faC, maC, waC, jsM, faM, maM, waM, de, 0, 0);
    }
    private static Params pso(double rand, double faC, double maC, double waC,
        double faM, double maM, double waM, int q, int ls) {
      return new Params(rand, 0.0, faC, maC, waC, 0.0, faM, maM, waM, 0.0, q, ls);
    }
    private String canonicalText() {
      return "randUpper=" + randUpper + "\njsCross=" + jsCross + "\nfaCross=" + faCross
          + "\nmaCross=" + maCross + "\nwaCross=" + waCross + "\njsMutation=" + jsMutation
          + "\nfaMutation=" + faMutation + "\nmaMutation=" + maMutation
          + "\nwaMutation=" + waMutation + "\ndeProbability=" + deProbability
          + "\nqTimes=" + qTimes + "\nlsTimes=" + lsTimes + "\n";
    }
  }
}
