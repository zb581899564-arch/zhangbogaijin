package org.uma.jmetal.problem.multiobjective.dfsp.fatigue;

import org.uma.jmetal.problem.multiobjective.dfsp.ZhangBoEDHHFSPW;
import org.uma.jmetal.solution.PermutationSolution;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/** Deterministic evidence exporter for one explicit round-robin chromosome. */
public final class ZhangBoFatigueTraceExporter {
  private ZhangBoFatigueTraceExporter() { }

  public static void main(String[] args) throws Exception {
    if (args.length != 7) {
      throw new IllegalArgumentException(
          "Usage: dataDir fatigueDir output jobs stages factories problemId");
    }
    System.setProperty("dhfsp.data.dir", Paths.get(args[0]).toAbsolutePath().normalize().toString());
    System.setProperty("dhfsp.fatigue.dir", Paths.get(args[1]).toAbsolutePath().normalize().toString());
    Path output = Paths.get(args[2]);
    int jobs = Integer.parseInt(args[3]);
    int stages = Integer.parseInt(args[4]);
    int factories = Integer.parseInt(args[5]);
    int problemId = Integer.parseInt(args[6]);
    ZhangBoEDHHFSPW problem = ZhangBoEDHHFSPW.withConfiguredFatigueParameters(
        jobs, stages, factories, problemId);
    ZhangBoFatigueInstanceData instance = problem.getFatigueInstanceData();
    PermutationSolution<Integer> solution = problem.createSolution();
    for (int position = 0; position < jobs; position++) {
      int factory = position % factories;
      solution.setVariableValue(position, position);
      solution.setVariableValueid(position, factory);
      solution.setVariableValueworker(position, instance.getEligibleWorkers(factory, 0)[0]);
    }
    problem.evaluate(solution);
    ZhangBoFatigueEvaluationResult result = (ZhangBoFatigueEvaluationResult)
        solution.getAttribute(ZhangBoFatigueEvaluationResult.class);
    if (result == null) throw new IllegalStateException("Fatigue path was not active");
    Path parent = output.toAbsolutePath().normalize().getParent();
    if (parent != null) Files.createDirectories(parent);
    Files.write(output, result.toCanonicalUtf8());
    System.out.println("Wrote deterministic fatigue trace to " + output.toAbsolutePath().normalize());
  }
}
