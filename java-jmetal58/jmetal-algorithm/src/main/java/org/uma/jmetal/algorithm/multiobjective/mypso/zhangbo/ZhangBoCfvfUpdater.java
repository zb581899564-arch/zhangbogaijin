package org.uma.jmetal.algorithm.multiobjective.mypso.zhangbo;

import org.uma.jmetal.solution.PermutationSolution;
import org.uma.jmetal.util.pseudorandom.PseudoRandomGenerator;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.Set;

/** Coupled full-vector discrete flight for the author-derived four-vector layout. */
public final class ZhangBoCfvfUpdater {
  public ZhangBoCfvfResult update(
      PermutationSolution<Integer> current,
      PermutationSolution<Integer> pbest,
      PermutationSolution<Integer> gbest,
      ZhangBoResourceVelocity previousVelocity,
      ZhangBoResourceDomain domain,
      ZhangBoGlobalSearchConfiguration configuration,
      PseudoRandomGenerator random) {
    if (current == null || pbest == null || gbest == null || domain == null
        || configuration == null || random == null) {
      throw new IllegalArgumentException("CFVF inputs cannot be null");
    }
    org.uma.jmetal.algorithm.multiobjective.mypso.v35.V35ModuleTimer.increment(
        org.uma.jmetal.algorithm.multiobjective.mypso.v35.V35ModuleTimer.C_PARTICLE_UPDATE, 1L);
    long tValidate = System.nanoTime();
    validate(current, domain, "current");
    validate(pbest, domain, "pbest");
    validate(gbest, domain, "gbest");
    ZhangBoResourceVelocity history = previousVelocity == null
        ? ZhangBoResourceVelocity.EMPTY : previousVelocity;
    PermutationSolution<Integer> offspring = copyWithIndependentMachineVector(current);
    org.uma.jmetal.algorithm.multiobjective.mypso.v35.V35ModuleTimer.record(
        org.uma.jmetal.algorithm.multiobjective.mypso.v35.V35ModuleTimer.CTX_VALIDATE,
        System.nanoTime() - tValidate, 1L);
    MutableDiagnostics diagnostics = new MutableDiagnostics();

    double uP = random.nextDouble();
    double uG = random.nextDouble();
    double etaP = configuration.getResourceCognitiveScale() * uP;
    double etaG = configuration.getResourceSocialScale() * uG;
    diagnostics.events.add("draw:uP=" + uP + ",uG=" + uG + ",etaP=" + etaP + ",etaG=" + etaG);

    long tJs = System.nanoTime();
    applyJobSequenceChannel(offspring, pbest, gbest, etaP, etaG, random, diagnostics.events);
    org.uma.jmetal.algorithm.multiobjective.mypso.v35.V35ModuleTimer.record(
        org.uma.jmetal.algorithm.multiobjective.mypso.v35.V35ModuleTimer.CTX_JS_CHANNEL,
        System.nanoTime() - tJs, 1L);

    Map<Integer, ZhangBoResourceAction> merged;
    if (configuration.getParticleUpdateMode()
        == ZhangBoGlobalSearchConfiguration.ParticleUpdateMode.FA_LEADER_ONLY) {
      merged = faOnlyFlight(offspring, pbest, gbest, domain, etaP, etaG, random, diagnostics);
    } else if (configuration.getParticleUpdateMode()
        == ZhangBoGlobalSearchConfiguration.ParticleUpdateMode.INDEPENDENT_RESOURCE) {
      merged = independentFlight(offspring, pbest, gbest, history, domain,
          etaP, etaG, configuration, random, diagnostics);
    } else {
      long tDiff = System.nanoTime();
      Map<Integer, ZhangBoResourceAction> inertia = configuration.getResourceInertia() > 0.0
          ? sampleInertia(offspring, history, domain, configuration.getResourceInertia(), random,
              diagnostics.events) : Collections.<Integer, ZhangBoResourceAction>emptyMap();
      Map<Integer, ZhangBoResourceAction> personal = sampleDifference(
          resourceDifference(offspring, pbest, ZhangBoResourceAction.Source.PBEST),
          etaP, random, diagnostics.events);
      Map<Integer, ZhangBoResourceAction> social = sampleDifference(
          resourceDifference(offspring, gbest, ZhangBoResourceAction.Source.GBEST),
          etaG, random, diagnostics.events);
      org.uma.jmetal.algorithm.multiobjective.mypso.v35.V35ModuleTimer.record(
          org.uma.jmetal.algorithm.multiobjective.mypso.v35.V35ModuleTimer.CTX_PBEST_DIFF,
          System.nanoTime() - tDiff, 1L);

      long tMerge = System.nanoTime();
      merged = new HashMap<>();
      int jobs = offspring.getNumberOfVariables();
      for (int job = 0; job < jobs; job++) {
        org.uma.jmetal.algorithm.multiobjective.mypso.v35.V35ModuleTimer.increment(
            org.uma.jmetal.algorithm.multiobjective.mypso.v35.V35ModuleTimer.C_CONFLICT, 1L);
        ZhangBoResourceAction pAction = personal.get(job);
        ZhangBoResourceAction gAction = social.get(job);
        ZhangBoResourceAction selected = resolveLeadership(
            pAction, gAction, etaP, etaG, random, diagnostics);
        if (selected == null) selected = inertia.get(job);
        if (selected != null) merged.put(job, selected);
      }
      if (configuration.getResourceExploration() > 0.0) {
        maybeAddExploration(offspring, merged, domain,
            configuration.getResourceExploration(), random, diagnostics.events);
      }
      org.uma.jmetal.algorithm.multiobjective.mypso.v35.V35ModuleTimer.record(
          org.uma.jmetal.algorithm.multiobjective.mypso.v35.V35ModuleTimer.CTX_RESOURCE_MERGE,
          System.nanoTime() - tMerge, 1L);
    }

    long tApply = System.nanoTime();
    List<ZhangBoResourceAction> applied = new ArrayList<>(merged.values());
    Collections.sort(applied, Comparator.comparingInt(ZhangBoResourceAction::getJob));
    for (ZhangBoResourceAction action : applied) {
      applyResourceAction(offspring, action);
      increment(diagnostics.kindCounts, action.getKind());
      increment(diagnostics.sourceCounts, action.getSource());
      increment(diagnostics.crossCounts, action.getKind() + ":" + action.getSource());
      if (action.getSource() == ZhangBoResourceAction.Source.PBEST
          || action.getSource() == ZhangBoResourceAction.Source.BOTH) diagnostics.pbestInherited++;
      if (action.getSource() == ZhangBoResourceAction.Source.GBEST
          || action.getSource() == ZhangBoResourceAction.Source.BOTH) diagnostics.gbestInherited++;
      diagnostics.events.add("apply:" + action.toCanonicalText());
    }
    org.uma.jmetal.algorithm.multiobjective.mypso.v35.V35ModuleTimer.record(
        org.uma.jmetal.algorithm.multiobjective.mypso.v35.V35ModuleTimer.CTX_ACTION_APPLY,
        System.nanoTime() - tApply, 1L);
    long tRepair = System.nanoTime();
    diagnostics.repairs += repairForSafety(offspring, domain, diagnostics.events);
    validate(offspring, domain, "offspring");
    org.uma.jmetal.algorithm.multiobjective.mypso.v35.V35ModuleTimer.record(
        org.uma.jmetal.algorithm.multiobjective.mypso.v35.V35ModuleTimer.CTX_REPAIR,
        System.nanoTime() - tRepair, 1L);
    ZhangBoResourceVelocity velocity = new ZhangBoResourceVelocity(applied);
    offspring.setAttribute(ZhangBoResourceVelocity.class, velocity);

    long tTail = System.nanoTime();
    int jobs = offspring.getNumberOfVariables();
    diagnostics.jsHamming = hamming(current.getVariables(), offspring.getVariables(), jobs);
    diagnostics.faHamming = hamming(current.getVariablesid(), offspring.getVariablesid(), jobs);
    diagnostics.maHamming = hamming(machine(current), machine(offspring), jobs);
    diagnostics.waHamming = hamming(current.getVariablesworker(), offspring.getVariablesworker(), jobs);
    ZhangBoCfvfDiagnostics immutable = diagnostics.freeze();
    offspring.setAttribute(ZhangBoCfvfDiagnostics.class, immutable);
    org.uma.jmetal.algorithm.multiobjective.mypso.v35.V35ModuleTimer.record(
        org.uma.jmetal.algorithm.multiobjective.mypso.v35.V35ModuleTimer.CTX_TAIL,
        System.nanoTime() - tTail, 1L);
    return new ZhangBoCfvfResult(offspring, velocity, immutable);
  }

  /** FV1: only FA is leader-guided; MA/WA merely close the new factory's legal domain. */
  private static Map<Integer, ZhangBoResourceAction> faOnlyFlight(
      PermutationSolution<Integer> current, PermutationSolution<Integer> pbest,
      PermutationSolution<Integer> gbest, ZhangBoResourceDomain domain,
      double etaP, double etaG, PseudoRandomGenerator random,
      MutableDiagnostics diagnostics) {
    Map<Integer, ZhangBoResourceAction> personal = sampleDifference(
        factoryOnlyDifference(current, pbest, domain, ZhangBoResourceAction.Source.PBEST),
        etaP, random, diagnostics.events);
    Map<Integer, ZhangBoResourceAction> social = sampleDifference(
        factoryOnlyDifference(current, gbest, domain, ZhangBoResourceAction.Source.GBEST),
        etaG, random, diagnostics.events);
    Map<Integer, ZhangBoResourceAction> result = new HashMap<>();
    for (int job = 0; job < current.getNumberOfVariables(); job++) {
      ZhangBoResourceAction selected = resolveLeadership(personal.get(job), social.get(job),
          etaP, etaG, random, diagnostics);
      if (selected != null) result.put(job, selected);
    }
    diagnostics.events.add("resourceMode=FA_LEADER_ONLY");
    return result;
  }

  private static List<ZhangBoResourceAction> factoryOnlyDifference(
      PermutationSolution<Integer> current, PermutationSolution<Integer> leader,
      ZhangBoResourceDomain domain, ZhangBoResourceAction.Source source) {
    int[] cp = positions(current, "current");
    int[] lp = positions(leader, "leader");
    List<Integer> currentMachines = machine(current);
    List<ZhangBoResourceAction> result = new ArrayList<>();
    for (int job = 0; job < cp.length; job++) {
      int currentPosition = cp[job];
      int targetFactory = leader.getVariableValueid(lp[job]);
      if (targetFactory == current.getVariableValueid(currentPosition)) continue;
      int targetMachine = currentMachines.get(currentPosition);
      if (!domain.isMachineValid(targetFactory, targetMachine)) {
        targetMachine = domain.firstMachine(targetFactory);
      }
      int targetWorker = current.getVariableValueworker(currentPosition);
      if (!domain.isWorkerValid(targetFactory, targetWorker)) {
        targetWorker = domain.firstWorker(targetFactory);
      }
      result.add(new ZhangBoResourceAction(job, ZhangBoResourceAction.Kind.FMW, source,
          targetFactory, targetMachine, targetWorker));
    }
    return result;
  }

  /** FV2: FA, MA and WA decisions are sampled independently by job identity. */
  private static Map<Integer, ZhangBoResourceAction> independentFlight(
      PermutationSolution<Integer> current, PermutationSolution<Integer> pbest,
      PermutationSolution<Integer> gbest, ZhangBoResourceVelocity history,
      ZhangBoResourceDomain domain, double etaP, double etaG,
      ZhangBoGlobalSearchConfiguration configuration, PseudoRandomGenerator random,
      MutableDiagnostics diagnostics) {
    int[] cp = positions(current, "current");
    int[] pp = positions(pbest, "pbest");
    int[] gp = positions(gbest, "gbest");
    List<Integer> cm = machine(current);
    List<Integer> pm = machine(pbest);
    List<Integer> gm = machine(gbest);
    Map<Integer, ZhangBoResourceAction> result = new HashMap<>();
    for (int job = 0; job < cp.length; job++) {
      int position = cp[job];
      int factory = current.getVariableValueid(position);
      int machine = cm.get(position);
      int worker = current.getVariableValueworker(position);
      int sourceBits = 0;

      Integer selectedFactory = chooseIndependent(
          factory, pbest.getVariableValueid(pp[job]), gbest.getVariableValueid(gp[job]),
          etaP, etaG, "FA", job, random, diagnostics.events);
      if (selectedFactory != null) {
        factory = selectedFactory;
        sourceBits |= 3;
      }
      Integer selectedMachine = chooseIndependent(machine,
          pbest.getVariableValueid(pp[job]) == factory ? pm.get(pp[job]) : machine,
          gbest.getVariableValueid(gp[job]) == factory ? gm.get(gp[job]) : machine,
          etaP, etaG, "MA", job, random, diagnostics.events);
      if (selectedMachine != null && domain.isMachineValid(factory, selectedMachine)) {
        machine = selectedMachine;
        sourceBits |= 1;
      }
      Integer selectedWorker = chooseIndependent(worker,
          pbest.getVariableValueid(pp[job]) == factory
              ? pbest.getVariableValueworker(pp[job]) : worker,
          gbest.getVariableValueid(gp[job]) == factory
              ? gbest.getVariableValueworker(gp[job]) : worker,
          etaP, etaG, "WA", job, random, diagnostics.events);
      if (selectedWorker != null && domain.isWorkerValid(factory, selectedWorker)) {
        worker = selectedWorker;
        sourceBits |= 2;
      }
      if (!domain.isMachineValid(factory, machine)) machine = domain.firstMachine(factory);
      if (!domain.isWorkerValid(factory, worker)) worker = domain.firstWorker(factory);
      int oldFactory = current.getVariableValueid(position);
      int oldMachine = cm.get(position);
      int oldWorker = current.getVariableValueworker(position);
      ZhangBoResourceAction.Kind kind = classifyKind(oldFactory, oldMachine, oldWorker,
          factory, machine, worker);
      if (kind != null) {
        ZhangBoResourceAction.Source source = sourceBits == 1
            ? ZhangBoResourceAction.Source.PBEST : sourceBits == 2
            ? ZhangBoResourceAction.Source.GBEST : ZhangBoResourceAction.Source.BOTH;
        result.put(job, new ZhangBoResourceAction(job, kind, source, factory, machine, worker));
      }
    }
    if (configuration.getResourceExploration() > 0.0) {
      maybeAddExploration(current, result, domain, configuration.getResourceExploration(),
          random, diagnostics.events);
    }
    diagnostics.events.add("resourceMode=INDEPENDENT_FA_MA_WA,previousVelocityActions="
        + history.getActions().size() + ",inertia=" + configuration.getResourceInertia());
    return result;
  }

  private static Integer chooseIndependent(
      int current, int personal, int social, double etaP, double etaG,
      String vector, int job, PseudoRandomGenerator random, List<String> events) {
    boolean keepP = personal != current && random.nextDouble() < etaP;
    boolean keepG = social != current && random.nextDouble() < etaG;
    if (!keepP && !keepG) return null;
    if (keepP && !keepG) return personal;
    if (!keepP) return social;
    if (personal == social) return personal;
    double probability = etaP + etaG == 0.0 ? 0.5 : etaP / (etaP + etaG + 1.0e-12);
    double draw = random.nextDouble();
    int selected = draw < probability ? personal : social;
    events.add("independentConflict:job=" + job + ",vector=" + vector
        + ",draw=" + draw + ",pPersonal=" + probability + ",selected=" + selected);
    return selected;
  }

  private static ZhangBoResourceAction.Kind classifyKind(
      int oldFactory, int oldMachine, int oldWorker,
      int factory, int machine, int worker) {
    if (oldFactory != factory) return ZhangBoResourceAction.Kind.FMW;
    if (oldMachine != machine && oldWorker != worker) return ZhangBoResourceAction.Kind.MW;
    if (oldMachine != machine) return ZhangBoResourceAction.Kind.M;
    if (oldWorker != worker) return ZhangBoResourceAction.Kind.W;
    return null;
  }

  private static void applyJobSequenceChannel(
      PermutationSolution<Integer> offspring,
      PermutationSolution<Integer> pbest,
      PermutationSolution<Integer> gbest,
      double etaP,
      double etaG,
      PseudoRandomGenerator random,
      List<String> events) {
    int jobs = offspring.getNumberOfVariables();
    int left = random.nextInt(0, jobs - 1);
    int right = random.nextInt(0, jobs - 1);
    swap(offspring.getVariables(), left, right);
    events.add("js:exploreSwap=" + left + "," + right);

    List<Swap> personal = exchangeDifference(pbest.getVariables(), offspring.getVariables());
    int personalCount = Math.min(personal.size(), (int) Math.floor(personal.size() * etaP));
    for (int index = 0; index < personalCount; index++) {
      Swap value = personal.get(index);
      swap(offspring.getVariables(), value.left, value.right);
      events.add("js:pbestSwap=" + value.left + "," + value.right);
    }
    List<Swap> social = exchangeDifference(gbest.getVariables(), offspring.getVariables());
    int socialCount = Math.min(social.size(), (int) Math.floor(social.size() * etaG));
    for (int index = 0; index < socialCount; index++) {
      Swap value = social.get(index);
      swap(offspring.getVariables(), value.left, value.right);
      events.add("js:gbestSwap=" + value.left + "," + value.right);
    }
    org.uma.jmetal.algorithm.multiobjective.mypso.v35.V35ModuleTimer.increment(
        org.uma.jmetal.algorithm.multiobjective.mypso.v35.V35ModuleTimer.C_JS_ACTION,
        personalCount + socialCount + 2L);
  }

  private static List<Swap> exchangeDifference(List<Integer> target, List<Integer> current) {
    if (target.size() != current.size()) throw new IllegalArgumentException("JS length mismatch");
    List<Integer> work = new ArrayList<>(current);
    List<Swap> result = new ArrayList<>();
    for (int position = 0; position < work.size(); position++) {
      if (!work.get(position).equals(target.get(position))) {
        int other = work.indexOf(target.get(position));
        if (other < 0) throw new IllegalArgumentException("JS is not the same permutation");
        result.add(new Swap(position, other));
        swap(work, position, other);
      }
    }
    return result;
  }

  private static List<ZhangBoResourceAction> resourceDifference(
      PermutationSolution<Integer> current,
      PermutationSolution<Integer> leader,
      ZhangBoResourceAction.Source source) {
    int jobs = current.getNumberOfVariables();
    int[] currentPosition = positions(current, "current");
    int[] leaderPosition = positions(leader, "leader");
    List<Integer> currentMachine = machine(current);
    List<Integer> leaderMachine = machine(leader);
    List<ZhangBoResourceAction> result = new ArrayList<>();
    for (int job = 0; job < jobs; job++) {
      int cp = currentPosition[job];
      int lp = leaderPosition[job];
      int cf = current.getVariableValueid(cp);
      int cm = currentMachine.get(cp);
      int cw = current.getVariableValueworker(cp);
      int lf = leader.getVariableValueid(lp);
      int lm = leaderMachine.get(lp);
      int lw = leader.getVariableValueworker(lp);
      ZhangBoResourceAction.Kind kind = null;
      if (cf != lf) kind = ZhangBoResourceAction.Kind.FMW;
      else if (cm != lm && cw != lw) kind = ZhangBoResourceAction.Kind.MW;
      else if (cm != lm) kind = ZhangBoResourceAction.Kind.M;
      else if (cw != lw) kind = ZhangBoResourceAction.Kind.W;
      if (kind != null) result.add(new ZhangBoResourceAction(job, kind, source, lf, lm, lw));
    }
    org.uma.jmetal.algorithm.multiobjective.mypso.v35.V35ModuleTimer.increment(
        source == ZhangBoResourceAction.Source.PBEST
            ? org.uma.jmetal.algorithm.multiobjective.mypso.v35.V35ModuleTimer.C_PBEST_DIFF
            : org.uma.jmetal.algorithm.multiobjective.mypso.v35.V35ModuleTimer.C_GBEST_DIFF,
        result.size());
    return result;
  }

  private static Map<Integer, ZhangBoResourceAction> sampleDifference(
      List<ZhangBoResourceAction> actions,
      double probability,
      PseudoRandomGenerator random,
      List<String> events) {
    Map<Integer, ZhangBoResourceAction> result = new HashMap<>();
    for (ZhangBoResourceAction action : actions) {
      double draw = random.nextDouble();
      boolean keep = draw < probability;
      events.add("sample:" + action.getSource() + ":job=" + action.getJob()
          + ",draw=" + draw + ",p=" + probability + ",keep=" + keep);
      if (keep) result.put(action.getJob(), action);
    }
    return result;
  }

  private static Map<Integer, ZhangBoResourceAction> sampleInertia(
      PermutationSolution<Integer> current,
      ZhangBoResourceVelocity velocity,
      ZhangBoResourceDomain domain,
      double probability,
      PseudoRandomGenerator random,
      List<String> events) {
    Map<Integer, ZhangBoResourceAction> result = new HashMap<>();
    int[] positions = positions(current, "current");
    List<Integer> machines = machine(current);
    for (ZhangBoResourceAction old : velocity.getActions()) {
      boolean legal = old.getJob() < positions.length
          && domain.isFactoryValid(old.getFactory())
          && domain.isMachineValid(old.getFactory(), old.getMachine())
          && domain.isWorkerValid(old.getFactory(), old.getWorker());
      boolean reached = false;
      if (legal) {
        int position = positions[old.getJob()];
        reached = current.getVariableValueid(position) == old.getFactory()
            && machines.get(position) == old.getMachine()
            && current.getVariableValueworker(position) == old.getWorker();
      }
      double draw = random.nextDouble();
      boolean keep = legal && !reached && draw < probability;
      events.add("inertia:job=" + old.getJob() + ",legal=" + legal + ",reached=" + reached
          + ",draw=" + draw + ",p=" + probability + ",keep=" + keep);
      if (keep) result.put(old.getJob(), old.withSource(ZhangBoResourceAction.Source.INERTIA));
    }
    return result;
  }

  private static ZhangBoResourceAction resolveLeadership(
      ZhangBoResourceAction personal,
      ZhangBoResourceAction social,
      double etaP,
      double etaG,
      PseudoRandomGenerator random,
      MutableDiagnostics diagnostics) {
    if (personal == null) return social;
    if (social == null) return personal;
    if (personal.hasSameTarget(social)) {
      return personal.withSource(ZhangBoResourceAction.Source.BOTH);
    }
    if (personal.priority() > social.priority()) {
      diagnostics.pbestConflictWins++;
      diagnostics.events.add("conflict:job=" + personal.getJob() + ",winner=PBEST,reason=priority");
      return personal;
    }
    if (social.priority() > personal.priority()) {
      diagnostics.gbestConflictWins++;
      diagnostics.events.add("conflict:job=" + personal.getJob() + ",winner=GBEST,reason=priority");
      return social;
    }
    double denominator = etaP + etaG;
    double personalProbability = denominator == 0.0 ? 0.5 : etaP / (denominator + 1.0e-12);
    double draw = random.nextDouble();
    if (draw < personalProbability) {
      diagnostics.pbestConflictWins++;
      diagnostics.events.add("conflict:job=" + personal.getJob() + ",winner=PBEST,draw=" + draw
          + ",p=" + personalProbability);
      return personal;
    }
    diagnostics.gbestConflictWins++;
    diagnostics.events.add("conflict:job=" + personal.getJob() + ",winner=GBEST,draw=" + draw
        + ",p=" + personalProbability);
    return social;
  }

  static ZhangBoResourceAction resolveLeadershipForPolicyTest(
      ZhangBoResourceAction personal,
      ZhangBoResourceAction social,
      double etaP,
      double etaG,
      PseudoRandomGenerator random) {
    if (random == null) throw new IllegalArgumentException("random");
    return resolveLeadership(personal, social, etaP, etaG, random, new MutableDiagnostics());
  }

  private static void maybeAddExploration(
      PermutationSolution<Integer> solution,
      Map<Integer, ZhangBoResourceAction> selected,
      ZhangBoResourceDomain domain,
      double probability,
      PseudoRandomGenerator random,
      List<String> events) {
    double draw = random.nextDouble();
    events.add("explore:draw=" + draw + ",p=" + probability);
    if (draw >= probability) return;
    int[] positions = positions(solution, "solution");
    List<Integer> machines = machine(solution);
    List<Integer> eligibleJobs = new ArrayList<>();
    Map<Integer, List<ZhangBoResourceAction.Kind>> types = new HashMap<>();
    for (int job = 0; job < positions.length; job++) {
      if (selected.containsKey(job)) continue;
      int position = positions[job];
      int factory = solution.getVariableValueid(position);
      int machine = machines.get(position);
      int worker = solution.getVariableValueworker(position);
      List<ZhangBoResourceAction.Kind> available = new ArrayList<>();
      if (domain.alternativeMachines(factory, machine).length > 0) available.add(ZhangBoResourceAction.Kind.M);
      if (domain.alternativeWorkers(factory, worker).length > 0) available.add(ZhangBoResourceAction.Kind.W);
      if (domain.alternativeFactories(factory).length > 0) available.add(ZhangBoResourceAction.Kind.FMW);
      if (!available.isEmpty()) {
        eligibleJobs.add(job);
        types.put(job, available);
      }
    }
    if (eligibleJobs.isEmpty()) {
      events.add("explore:noEligibleJob");
      return;
    }
    int job = eligibleJobs.get(random.nextInt(0, eligibleJobs.size() - 1));
    List<ZhangBoResourceAction.Kind> available = types.get(job);
    ZhangBoResourceAction.Kind kind = available.get(random.nextInt(0, available.size() - 1));
    int position = positions[job];
    int factory = solution.getVariableValueid(position);
    int machine = machines.get(position);
    int worker = solution.getVariableValueworker(position);
    if (kind == ZhangBoResourceAction.Kind.M) {
      int[] alternatives = domain.alternativeMachines(factory, machine);
      machine = alternatives[random.nextInt(0, alternatives.length - 1)];
    } else if (kind == ZhangBoResourceAction.Kind.W) {
      int[] alternatives = domain.alternativeWorkers(factory, worker);
      worker = alternatives[random.nextInt(0, alternatives.length - 1)];
    } else {
      int[] alternatives = domain.alternativeFactories(factory);
      factory = alternatives[random.nextInt(0, alternatives.length - 1)];
      machine = random.nextInt(0, domain.getMachineCount(factory) - 1);
      int[] workers = domain.getWorkers(factory);
      worker = workers[random.nextInt(0, workers.length - 1)];
    }
    ZhangBoResourceAction action = new ZhangBoResourceAction(
        job, kind, ZhangBoResourceAction.Source.EXPLORE, factory, machine, worker);
    selected.put(job, action);
    events.add("explore:selected=" + action.toCanonicalText());
  }

  private static void applyResourceAction(
      PermutationSolution<Integer> solution, ZhangBoResourceAction action) {
    org.uma.jmetal.algorithm.multiobjective.mypso.v35.V35ModuleTimer.increment(
        org.uma.jmetal.algorithm.multiobjective.mypso.v35.V35ModuleTimer.C_RESOURCE_ACTION, 1L);
    int position = positions(solution, "solution")[action.getJob()];
    solution.setVariableValueid(position, action.getFactory());
    List<Integer> machines = machine(solution);
    machines.set(position, action.getMachine());
    ZhangBoMachineVectorSupport.write(solution, machines);
    solution.setVariableValueworker(position, action.getWorker());
  }

  static int repairForSafety(
      PermutationSolution<Integer> solution,
      ZhangBoResourceDomain domain,
      List<String> events) {
    org.uma.jmetal.algorithm.multiobjective.mypso.v35.V35ModuleTimer.increment(
        org.uma.jmetal.algorithm.multiobjective.mypso.v35.V35ModuleTimer.C_LEGALITY, 1L);
    int repairs = 0;
    List<Integer> machines = machine(solution);
    int jobs = solution.getNumberOfVariables();
    for (int position = 0; position < jobs; position++) {
      int job = solution.getVariableValue(position);
      int factory = solution.getVariableValueid(position);
      if (!domain.isFactoryValid(factory)) {
        events.add("repair:job=" + job + ",vector=FA,old=" + factory + ",new=0");
        factory = 0;
        solution.setVariableValueid(position, factory);
        repairs++;
      }
      int machine = machines.get(position);
      if (!domain.isMachineValid(factory, machine)) {
        int replacement = domain.firstMachine(factory);
        events.add("repair:job=" + job + ",vector=MA,old=" + machine + ",new=" + replacement);
        machines.set(position, replacement);
        repairs++;
      }
      int worker = solution.getVariableValueworker(position);
      if (!domain.isWorkerValid(factory, worker)) {
        int replacement = domain.firstWorker(factory);
        events.add("repair:job=" + job + ",vector=WA,old=" + worker + ",new=" + replacement);
        solution.setVariableValueworker(position, replacement);
        repairs++;
      }
    }
    ZhangBoMachineVectorSupport.write(solution, machines);
    return repairs;
  }

  public static void validate(
      PermutationSolution<Integer> solution, ZhangBoResourceDomain domain, String label) {
    int jobs = solution.getNumberOfVariables();
    if (solution.getVariablesid().size() != jobs) {
      throw new IllegalArgumentException(label + ".FA length=" + solution.getVariablesid().size() + ", expected=" + jobs);
    }
    if (solution.getVariablesworker().size() < jobs) {
      throw new IllegalArgumentException(label + ".WA length=" + solution.getVariablesworker().size() + ", expected at least=" + jobs);
    }
    List<Integer> machines = machine(solution);
    if (machines.size() < jobs) {
      throw new IllegalArgumentException(label + ".MA length=" + machines.size() + ", expected at least=" + jobs);
    }
    positions(solution, label);
    for (int position = 0; position < jobs; position++) {
      int factory = requireValue(solution.getVariablesid().get(position), label, "FA", position);
      int machine = requireValue(machines.get(position), label, "MA", position);
      int worker = requireValue(solution.getVariablesworker().get(position), label, "WA", position);
      if (!domain.isFactoryValid(factory)) {
        throw new IllegalArgumentException(label + ".FA position=" + position + ", value=" + factory);
      }
      if (!domain.isMachineValid(factory, machine)) {
        throw new IllegalArgumentException(label + ".MA position=" + position + ", value=" + machine + ", factory=" + factory);
      }
      if (!domain.isWorkerValid(factory, worker)) {
        throw new IllegalArgumentException(label + ".WA position=" + position + ", value=" + worker + ", factory=" + factory);
      }
    }
  }

  @SuppressWarnings("unchecked")
  private static List<Integer> machine(PermutationSolution<Integer> solution) {
    if (solution instanceof org.uma.jmetal.problem.multiobjective.dfsp.model.DhhfspFourVectorSolution) {
      return ZhangBoMachineVectorSupport.copy(solution, solution.getNumberOfVariables());
    }
    Object value = solution.getAttribute("machine");
    if (!(value instanceof List)) throw new IllegalArgumentException("Missing List<Integer> machine attribute");
    return (List<Integer>) value;
  }

  @SuppressWarnings("unchecked")
  private static PermutationSolution<Integer> copyWithIndependentMachineVector(
      PermutationSolution<Integer> solution) {
    PermutationSolution<Integer> copy = (PermutationSolution<Integer>) solution.copy();
    ZhangBoMachineVectorSupport.write(copy,
        new ArrayList<>(machine(solution)));
    return copy;
  }

  private static int[] positions(PermutationSolution<Integer> solution, String label) {
    int jobs = solution.getNumberOfVariables();
    int[] result = new int[jobs];
    java.util.Arrays.fill(result, -1);
    for (int position = 0; position < jobs; position++) {
      Integer value = solution.getVariableValue(position);
      if (value == null || value < 0 || value >= jobs || result[value] != -1) {
        throw new IllegalArgumentException(label + ".JS invalid at position=" + position + ", value=" + value);
      }
      result[value] = position;
    }
    return result;
  }

  private static int requireValue(Integer value, String label, String vector, int position) {
    if (value == null) throw new IllegalArgumentException(label + '.' + vector + " null at position=" + position);
    return value;
  }

  private static int hamming(List<Integer> left, List<Integer> right, int length) {
    int result = 0;
    for (int index = 0; index < length; index++) {
      if (!left.get(index).equals(right.get(index))) result++;
    }
    return result;
  }

  private static void swap(List<Integer> values, int left, int right) {
    Integer value = values.get(left);
    values.set(left, values.get(right));
    values.set(right, value);
  }

  private static <E extends Enum<E>> void increment(Map<E, Integer> values, E key) {
    Integer previous = values.get(key);
    values.put(key, previous == null ? 1 : previous + 1);
  }
  /** V35-FC-5 GIR audit: String-keyed cross counter. */
  private static void increment(Map<String, Integer> values, String key) {
    Integer current = values.get(key);
    values.put(key, (current == null ? 0 : current.intValue()) + 1);
  }


  private static final class Swap {
    private final int left;
    private final int right;
    private Swap(int left, int right) { this.left = left; this.right = right; }
  }

  private static final class MutableDiagnostics {
    private int jsHamming;
    private int faHamming;
    private int maHamming;
    private int waHamming;
    private int pbestInherited;
    private int gbestInherited;
    private int pbestConflictWins;
    private int gbestConflictWins;
    private int repairs;
    private final Map<ZhangBoResourceAction.Kind, Integer> kindCounts =
        new EnumMap<>(ZhangBoResourceAction.Kind.class);
    private final Map<ZhangBoResourceAction.Source, Integer> sourceCounts =
        new EnumMap<>(ZhangBoResourceAction.Source.class);
    private final Map<String, Integer> crossCounts = new TreeMap<>();
    private final List<String> events = new ArrayList<>();

    private ZhangBoCfvfDiagnostics freeze() {
      return new ZhangBoCfvfDiagnostics(
          jsHamming, faHamming, maHamming, waHamming,
          pbestInherited, gbestInherited, pbestConflictWins, gbestConflictWins,
          repairs, kindCounts, sourceCounts, crossCounts, events);
    }
  }
}
