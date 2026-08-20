package org.uma.jmetal.runner.lc_psode;

import java.nio.file.Path;
import java.nio.file.Paths;

/** Same-JVM warmup plus three-repeat FULL/BASE performance gate. */
public final class ZhangBoP83PerformanceSuiteRunner {
  private ZhangBoP83PerformanceSuiteRunner() { }

  public static void main(String[] args) throws Exception {
    if (args.length != 3) {
      throw new IllegalArgumentException("Usage: <project-root> <output> <20000|100000>");
    }
    Path project = Paths.get(args[0]).toAbsolutePath().normalize();
    Path output = Paths.get(args[1]).toAbsolutePath().normalize();
    int maxFEs = Integer.parseInt(args[2]);
    if (maxFEs != 20000 && maxFEs != 100000) {
      throw new IllegalArgumentException("P8.3 suite is locked to 20000 or 100000 FE");
    }
    runPair(project, output.resolve("warmup"), 2000);
    for (int repeat = 1; repeat <= 3; repeat++) {
      runPair(project, output.resolve("repeat-" + repeat), maxFEs);
    }
  }

  private static void runPair(Path project, Path output, int maxFEs) throws Exception {
    ZhangBoP9FormalParameters parameters =
        ZhangBoP9FormalParameters.engineering(20260808L, maxFEs);
    ZhangBoP9SingleComparisonRunner.runPhase(
        ZhangBoP9SingleComparisonRunner.Phase.FULL, project, output, parameters);
    ZhangBoP9SingleComparisonRunner.runPhase(
        ZhangBoP9SingleComparisonRunner.Phase.HMOPSO_QGS_F, project, output, parameters);
  }
}
