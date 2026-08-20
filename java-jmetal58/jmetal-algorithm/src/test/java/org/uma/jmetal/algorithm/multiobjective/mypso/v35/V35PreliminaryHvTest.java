package org.uma.jmetal.algorithm.multiobjective.mypso.v35;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import org.junit.Test;
import org.uma.jmetal.algorithm.multiobjective.mypso.p8.P8MetricCalculator;

/**
 * HV/IGD/coverage for the preliminary single-seed 500k comparison, computed
 * offline from the saved fronts (no rerun).  Per instance, the pooled union of
 * the two fronts is the empirical reference; objectives are normalized by the
 * reference min/max and HV uses the project-standard 1.1 reference point
 * (P8MetricCalculator convention — the same convention V35-P26 will adopt).
 * Diagnostic only; values are comparable within an instance, not across.
 */
public class V35PreliminaryHvTest {

  @Test(timeout = 300000)
  public void hypervolumeOnSavedPreliminaryFronts() throws Exception {
    Path project = Paths.get("").toAbsolutePath().normalize();
    if (project.getFileName() != null
        && "jmetal-algorithm".equals(project.getFileName().toString())) {
      project = project.getParent();
    }
    while (project.getParent() != null && !Files.exists(project.resolve("AGENTS.md"))) {
      project = project.getParent();
    }
    final Path root = project;
    Path runs = root.resolve("docs/evidence/V35-PR/runs");

    String[] tags = {"20_2_3_1", "20_2_4_1", "20_5_3_1"};
    List<String> rows = new ArrayList<>();
    rows.add("instance,hvBaseline,hvFull,hvRatio,hvUnion,hvFullShare,hvBaselineShare,"
        + "cBaselineOverFull,cFullOverBaseline,igdBaseline,igdFull,"
        + "unionNondominated,baseNondominated,fullNondominated");
    for (String tag : tags) {
      List<double[]> baseline = readFront(runs.resolve("baseline-500k-" + tag + "/front.csv"));
      List<double[]> full = readFront(runs.resolve("full-500k-" + tag + "/front.csv"));
      List<double[]> union = new ArrayList<>();
      union.addAll(baseline);
      union.addAll(full);

      P8MetricCalculator.Metrics base = P8MetricCalculator.calculate(baseline, union);
      P8MetricCalculator.Metrics ful = P8MetricCalculator.calculate(full, union);
      P8MetricCalculator.Metrics unionMetrics = P8MetricCalculator.calculate(union, union);
      P8MetricCalculator.Metrics baseVsFull = P8MetricCalculator.calculate(baseline, full);

      rows.add(String.format(Locale.ROOT,
          "%s,%f,%f,%f,%f,%f,%f,%f,%f,%f,%f,%d,%d,%d",
          tag, base.hv, ful.hv, ful.hv / base.hv, unionMetrics.hv,
          ful.hv / unionMetrics.hv, base.hv / unionMetrics.hv,
          baseVsFull.cForward, baseVsFull.cReverse,
          base.igd, ful.igd, unionMetrics.nondominatedCount,
          base.nondominatedCount, ful.nondominatedCount));
    }

    Path evidence = root.resolve("docs/evidence/V35-PR");
    Files.write(evidence.resolve("HV_METRICS.csv"),
        String.join("\n", rows).getBytes(StandardCharsets.UTF_8));
  }

  private static List<double[]> readFront(Path path) throws Exception {
    List<double[]> front = new ArrayList<>();
    for (String line : Files.readAllLines(path)) {
      if (line.isEmpty() || line.startsWith("Cmax,")) continue;
      String[] columns = line.split(",", -1);
      front.add(new double[]{Double.parseDouble(columns[0]),
          Double.parseDouble(columns[1]), Double.parseDouble(columns[2])});
    }
    return front;
  }
}
