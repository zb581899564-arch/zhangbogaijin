package org.uma.jmetal.problem.multiobjective.dfsp.decoder;

import org.uma.jmetal.problem.multiobjective.dfsp.model.DhhfspEncodingValidator;
import org.uma.jmetal.problem.multiobjective.dfsp.model.DhhfspFourVectorSolution;
import org.uma.jmetal.problem.multiobjective.dfsp.model.DhhfspInstance;
import org.uma.jmetal.util.pseudorandom.PseudoRandomGenerator;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Injected-random factory for legal published-baseline four-vector particles. */
public final class RandomDhhfspSolutionFactory implements DhhfspSolutionFactory {
  private final DhhfspInstance instance;
  private final PseudoRandomGenerator random;
  private final String semanticTag;

  public RandomDhhfspSolutionFactory(
      DhhfspInstance instance, PseudoRandomGenerator random, String semanticTag) {
    this.instance = instance;
    this.random = random;
    this.semanticTag = semanticTag;
  }

  @Override
  public DhhfspFourVectorSolution create() {
    List<Integer> jobs = new ArrayList<>();
    for (int job = 0; job < instance.getNumberOfJobs(); job++) jobs.add(job);
    for (int index = jobs.size() - 1; index > 0; index--) {
      Collections.swap(jobs, index, random.nextInt(0, index));
    }
    List<Integer> factories = new ArrayList<>();
    List<Integer> machines = new ArrayList<>();
    List<Integer> workers = new ArrayList<>();
    for (int position = 0; position < jobs.size(); position++) {
      int factory = random.nextInt(0, instance.getNumberOfFactories() - 1);
      factories.add(factory);
      machines.add(random.nextInt(0, instance.getMachineCount(factory, 0) - 1));
      workers.add(random.nextInt(0, instance.getWorkerCount(factory, 0) - 1));
    }
    DhhfspFourVectorSolution result = new DhhfspFourVectorSolution(
        jobs, factories, machines, workers, semanticTag);
    DhhfspEncodingValidator.validateOrThrow(result, instance);
    return result;
  }
}
