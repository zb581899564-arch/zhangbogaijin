package org.uma.jmetal.runner.lc_psode;

import java.nio.file.Path;
import java.nio.file.Paths;

/** Scope-locked 20k/100k FE launcher for the P8.3 runtime gate. */
public final class ZhangBoP83RuntimeGateRunner {
  private ZhangBoP83RuntimeGateRunner() { }

  public static void main(String[] args) throws Exception {
    if (args.length != 4) {
      throw new IllegalArgumentException(
          "Usage: <FULL|HMOPSO_QGS_F> <project-root> <output> <20000|100000>");
    }
    ZhangBoP9SingleComparisonRunner.Phase phase =
        ZhangBoP9SingleComparisonRunner.Phase.valueOf(args[0]);
    if (phase == ZhangBoP9SingleComparisonRunner.Phase.REPORT) {
      throw new IllegalArgumentException("P8.3 runtime gate only accepts algorithm phases");
    }
    int maxFEs = Integer.parseInt(args[3]);
    if (maxFEs != 20000 && maxFEs != 100000) {
      throw new IllegalArgumentException("P8.3 runtime gate is locked to 20000 or 100000 FE");
    }
    Path project = Paths.get(args[1]).toAbsolutePath().normalize();
    Path output = Paths.get(args[2]).toAbsolutePath().normalize();
    ZhangBoP9SingleComparisonRunner.runPhase(phase, project, output,
        ZhangBoP9FormalParameters.engineering(20260808L, maxFEs));
  }
}
