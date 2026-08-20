package org.uma.jmetal.runner.lc_psode;

import java.nio.file.Path;
import java.nio.file.Paths;

/** Isolated JFR launcher for the P9 runtime audit; not a production experiment entry. */
public final class RuntimeAuditRunner {
  private RuntimeAuditRunner() { }

  public static void main(String[] args) throws Exception {
    if (args.length != 4) {
      throw new IllegalArgumentException(
          "Usage: <FULL|HMOPSO_QGS_F> <project-root> <output> <maxFEs>");
    }
    ZhangBoP9SingleComparisonRunner.Phase phase =
        ZhangBoP9SingleComparisonRunner.Phase.valueOf(args[0]);
    if (phase == ZhangBoP9SingleComparisonRunner.Phase.REPORT) {
      throw new IllegalArgumentException("Runtime audit only accepts algorithm phases");
    }
    Path project = Paths.get(args[1]).toAbsolutePath().normalize();
    Path output = Paths.get(args[2]).toAbsolutePath().normalize();
    int maxFEs = Integer.parseInt(args[3]);
    if (maxFEs != 100000) {
      throw new IllegalArgumentException("Runtime audit is locked to 100000 FE");
    }
    ZhangBoP9SingleComparisonRunner.runPhase(
        phase, project, output, ZhangBoP9FormalParameters.engineering(20260808L, maxFEs));
  }
}
