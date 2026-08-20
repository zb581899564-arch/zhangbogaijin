package org.uma.jmetal.problem.multiobjective.dfsp.fatigue;

import org.uma.jmetal.problem.multiobjective.dfsp.decoder.EvaluationCounter;
import org.uma.jmetal.problem.multiobjective.dfsp.decoder.ProductionDecodeMode;
import org.uma.jmetal.problem.multiobjective.dfsp.fatigue.shift.ZhangBoShiftConfiguration;
import org.uma.jmetal.problem.multiobjective.dfsp.setup.ProductFamilySetupModel;

/**
 * Formal v3.5 problem entry point.  Unlike historical P8/P9 runners it has no
 * shift argument and always binds the single-family, sequence-independent model.
 */
public final class ZhangBoV35ProblemFactory {
  private ZhangBoV35ProblemFactory() { }

  public static ZhangBoCanonicalProductionProblem create(
      ZhangBoFatigueInstanceData instance,
      ZhangBoFatigueParameters parameters,
      ProductionDecodeMode mode,
      long seed) {
    if (mode == null || mode.isAuthorDiagnostic()) {
      throw new IllegalArgumentException("v3.5 factory requires an explicit production decoder mode");
    }
    if (instance == null) throw new IllegalArgumentException("instance cannot be null");
    return new ZhangBoCanonicalProductionProblem(
        instance, parameters, mode, new ZhangBoFatigueEvaluator(),
        new EvaluationCounter(), seed, ZhangBoShiftConfiguration.none(),
        ProductFamilySetupModel.degenerate(instance.getJobs(), instance.getStages()));
  }

  public static ZhangBoCanonicalProductionProblem fm3(
      ZhangBoFatigueInstanceData instance,
      ZhangBoFatigueParameters parameters,
      long seed) {
    return create(instance, parameters, ProductionDecodeMode.FM3, seed);
  }
}
