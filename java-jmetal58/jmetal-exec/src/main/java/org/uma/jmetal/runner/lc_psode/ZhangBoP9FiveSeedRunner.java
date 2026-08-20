package org.uma.jmetal.runner.lc_psode;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/** Runs one of the five additional, hard-coded P9 diagnostic seed slots. */
public final class ZhangBoP9FiveSeedRunner {
  private static final long[] APPROVED_SEEDS = {
      20260809L, 20260810L, 20260811L, 20260812L, 20260813L
  };

  private ZhangBoP9FiveSeedRunner() { }

  public static void main(String[] args) throws Exception {
    Arguments parsed = Arguments.parse(args);
    long seed = approvedSeed(parsed.seedSlot);
    ZhangBoP9FormalParameters parameters =
        ZhangBoP9FormalParameters.formalForApprovedSeed(seed);
    Path seedOutput = parsed.output.resolve("seed-" + seed);
    Files.createDirectories(seedOutput);
    ZhangBoP9SingleComparisonRunner.runPhase(
        ZhangBoP9SingleComparisonRunner.Phase.FULL,
        parsed.projectRoot, seedOutput, parameters);
    ZhangBoP9SingleComparisonRunner.runPhase(
        ZhangBoP9SingleComparisonRunner.Phase.HMOPSO_QGS_F,
        parsed.projectRoot, seedOutput, parameters);
    ZhangBoP9SingleComparisonRunner.runPhase(
        ZhangBoP9SingleComparisonRunner.Phase.REPORT,
        parsed.projectRoot, seedOutput, parameters);
    System.out.println("P9_ADDITIONAL_SEED_COMPLETED slot=" + parsed.seedSlot
        + " seed=" + seed + " output=" + seedOutput);
  }

  static long approvedSeed(int seedSlot) {
    if (seedSlot < 1 || seedSlot > APPROVED_SEEDS.length) {
      throw new IllegalArgumentException("seed-slot must be 1..5");
    }
    return APPROVED_SEEDS[seedSlot - 1];
  }

  private static final class Arguments {
    private final int seedSlot;
    private final Path projectRoot;
    private final Path output;

    private Arguments(int seedSlot, Path projectRoot, Path output) {
      this.seedSlot = seedSlot;
      this.projectRoot = projectRoot;
      this.output = output;
    }

    private static Arguments parse(String[] args) {
      Integer slot = null;
      Path project = null;
      Path output = null;
      for (int index = 0; index < args.length; index += 2) {
        if (index + 1 >= args.length) throw usage();
        if ("--seed-slot".equals(args[index])) slot = Integer.parseInt(args[index + 1]);
        else if ("--project-root".equals(args[index])) {
          project = Paths.get(args[index + 1]).toAbsolutePath().normalize();
        } else if ("--output".equals(args[index])) {
          output = Paths.get(args[index + 1]).toAbsolutePath().normalize();
        } else throw usage();
      }
      if (slot == null || project == null || output == null) throw usage();
      approvedSeed(slot);
      return new Arguments(slot, project, output);
    }

    private static IllegalArgumentException usage() {
      return new IllegalArgumentException(
          "Usage: --seed-slot 1..5 --project-root <path> --output <path>");
    }
  }
}
