package org.uma.jmetal.problem.multiobjective.dfsp.model;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Visible Fig. 4 facts, deliberately separate from the executable Fig. 3 encoding. */
public final class Chapter4DecodeIllustrationFixture {
  public static final String RESOURCE =
      "/dfsp/chapter4/eswa-2026-fig4-decode-illustration.properties";
  private final List<Integer> fig3Factory2Jobs;
  private final List<Integer> fig4LegendJobs;
  private final double initialCmax;
  private final double fineTunedCmax;
  private final double rightShiftedCmax;
  private final String fineTuningAction;
  private final String rightShiftAction;

  private Chapter4DecodeIllustrationFixture(Map<String, String> values) {
    Set<String> required = new HashSet<>(Arrays.asList(
        "schemaVersion", "semanticTag", "sourceIndexBase", "fig3.factory2.jobs",
        "fig4.factory2.legendJobs", "fig4.initial.cmax", "fig4.fineTuned.cmax",
        "fig4.rightShifted.cmax", "fig4.fineTuning.action", "fig4.rightShift.action"));
    if (!values.keySet().equals(required)) {
      throw new IllegalArgumentException("Fig.4 fixture keys mismatch: " + values.keySet());
    }
    if (!"1".equals(values.get("schemaVersion"))
        || !"published_schedule_illustration".equals(values.get("semanticTag"))
        || !"1".equals(values.get("sourceIndexBase"))) {
      throw new IllegalArgumentException("Unsupported Fig.4 fixture metadata");
    }
    fig3Factory2Jobs = oneBasedJobs(values.get("fig3.factory2.jobs"));
    fig4LegendJobs = oneBasedJobs(values.get("fig4.factory2.legendJobs"));
    initialCmax = Double.parseDouble(values.get("fig4.initial.cmax"));
    fineTunedCmax = Double.parseDouble(values.get("fig4.fineTuned.cmax"));
    rightShiftedCmax = Double.parseDouble(values.get("fig4.rightShifted.cmax"));
    fineTuningAction = values.get("fig4.fineTuning.action");
    rightShiftAction = values.get("fig4.rightShift.action");
  }

  public static Chapter4DecodeIllustrationFixture load() {
    try (InputStream stream = Chapter4DecodeIllustrationFixture.class
        .getResourceAsStream(RESOURCE)) {
      return new Chapter4DecodeIllustrationFixture(StrictKeyValueParser.parse(stream, RESOURCE));
    } catch (IOException exception) {
      throw new IllegalStateException("Cannot load " + RESOURCE, exception);
    }
  }

  public List<Integer> getFig3Factory2Jobs() { return new ArrayList<>(fig3Factory2Jobs); }
  public List<Integer> getFig4LegendJobs() { return new ArrayList<>(fig4LegendJobs); }
  public double getInitialCmax() { return initialCmax; }
  public double getFineTunedCmax() { return fineTunedCmax; }
  public double getRightShiftedCmax() { return rightShiftedCmax; }
  public String getFineTuningAction() { return fineTuningAction; }
  public String getRightShiftAction() { return rightShiftAction; }

  private static List<Integer> oneBasedJobs(String text) {
    List<Integer> jobs = new ArrayList<>();
    for (String token : text.split(",")) jobs.add(Integer.parseInt(token.trim()));
    return jobs;
  }
}
