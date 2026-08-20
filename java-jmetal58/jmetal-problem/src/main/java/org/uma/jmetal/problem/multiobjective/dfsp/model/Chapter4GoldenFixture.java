package org.uma.jmetal.problem.multiobjective.dfsp.model;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Loads the ESWA 2026 Chapter 4 paper example without invoking any decoder. */
public final class Chapter4GoldenFixture {
  public static final String RESOURCE =
      "/dfsp/chapter4/eswa-2026-130934-golden.properties";
  private static final String SCHEMA_VERSION = "1";

  private final DhhfspInstance instance;
  private final DhhfspFourVectorSolution solution;
  private final int sourceIndexBase;
  private final List<Integer> publishedJobSequence;
  private final List<Integer> publishedFactoryAssignments;
  private final List<Integer> publishedMachineAssignments;
  private final List<Integer> publishedWorkerAssignments;

  private Chapter4GoldenFixture(Map<String, String> values) {
    requireExactKeys(values);
    requireValue(values, "schemaVersion", SCHEMA_VERSION);
    requireValue(values, "semanticTag", "published_baseline");
    sourceIndexBase = parseInteger(values, "sourceIndexBase");
    if (sourceIndexBase != 1) {
      throw new IllegalArgumentException("Golden paper resource must use sourceIndexBase=1");
    }

    int jobs = parseInteger(values, "jobs");
    int stages = parseInteger(values, "stages");
    int factories = parseInteger(values, "factories");
    double[][] processing = new double[][] {
        parseDoubles(values, "processing.stage1"),
        parseDoubles(values, "processing.stage2")
    };
    double[][] setup = new double[][] {
        parseDoubles(values, "setup.stage1"),
        parseDoubles(values, "setup.stage2")
    };
    double[][][] machineSpeed = parseResourceTensor(values, "machine.speed", factories, stages);
    double[][][] machineEnergy = parseResourceTensor(values, "machine.energy", factories, stages);
    double[][][] workerEfficiency =
        parseResourceTensor(values, "worker.efficiency", factories, stages);
    double[][][] workerCost = parseResourceTensor(values, "worker.cost", factories, stages);
    instance = new DhhfspInstance(
        jobs, stages, factories, processing, setup,
        machineSpeed, machineEnergy, workerEfficiency, workerCost);

    publishedJobSequence = immutableIntegers(values, "encoding.JS");
    publishedFactoryAssignments = immutableIntegers(values, "encoding.FA");
    publishedMachineAssignments = immutableIntegers(values, "encoding.MA");
    publishedWorkerAssignments = immutableIntegers(values, "encoding.WA");
    solution = new DhhfspFourVectorSolution(
        normalize(publishedJobSequence),
        normalize(publishedFactoryAssignments),
        normalize(publishedMachineAssignments),
        normalize(publishedWorkerAssignments),
        values.get("semanticTag"));
    DhhfspEncodingValidator.validateOrThrow(solution, instance);
  }

  public static Chapter4GoldenFixture load() {
    try (InputStream stream = Chapter4GoldenFixture.class.getResourceAsStream(RESOURCE)) {
      return new Chapter4GoldenFixture(StrictKeyValueParser.parse(stream, RESOURCE));
    } catch (IOException exception) {
      throw new IllegalStateException("Cannot load " + RESOURCE, exception);
    }
  }

  public DhhfspInstance getInstance() {
    return instance;
  }

  public DhhfspFourVectorSolution createSolution() {
    return solution.copy();
  }

  public int getSourceIndexBase() {
    return sourceIndexBase;
  }

  public List<Integer> getPublishedJobSequence() {
    return new ArrayList<>(publishedJobSequence);
  }

  public List<Integer> getPublishedFactoryAssignments() {
    return new ArrayList<>(publishedFactoryAssignments);
  }

  public List<Integer> getPublishedMachineAssignments() {
    return new ArrayList<>(publishedMachineAssignments);
  }

  public List<Integer> getPublishedWorkerAssignments() {
    return new ArrayList<>(publishedWorkerAssignments);
  }

  private List<Integer> normalize(List<Integer> values) {
    List<Integer> normalized = new ArrayList<>(values.size());
    for (Integer value : values) {
      normalized.add(value - sourceIndexBase);
    }
    return normalized;
  }

  private static double[][][] parseResourceTensor(
      Map<String, String> values, String prefix, int factories, int stages) {
    double[][][] tensor = new double[factories][stages][];
    for (int factory = 0; factory < factories; factory++) {
      for (int stage = 0; stage < stages; stage++) {
        String key = prefix + ".f" + (factory + 1) + ".s" + (stage + 1);
        tensor[factory][stage] = parseDoubles(values, key);
      }
    }
    return tensor;
  }

  private static List<Integer> immutableIntegers(Map<String, String> values, String key) {
    return java.util.Collections.unmodifiableList(parseIntegers(values.get(key), key));
  }

  private static List<Integer> parseIntegers(String text, String key) {
    String[] tokens = text.split(",", -1);
    List<Integer> result = new ArrayList<>(tokens.length);
    for (int index = 0; index < tokens.length; index++) {
      try {
        result.add(Integer.parseInt(tokens[index].trim()));
      } catch (NumberFormatException exception) {
        throw new IllegalArgumentException(
            key + " position " + index + " is not an integer: " + tokens[index], exception);
      }
    }
    return result;
  }

  private static double[] parseDoubles(Map<String, String> values, String key) {
    String[] tokens = values.get(key).split(",", -1);
    double[] result = new double[tokens.length];
    for (int index = 0; index < tokens.length; index++) {
      try {
        result[index] = Double.parseDouble(tokens[index].trim());
      } catch (NumberFormatException exception) {
        throw new IllegalArgumentException(
            key + " position " + index + " is not numeric: " + tokens[index], exception);
      }
    }
    return result;
  }

  private static int parseInteger(Map<String, String> values, String key) {
    try {
      return Integer.parseInt(values.get(key));
    } catch (NumberFormatException exception) {
      throw new IllegalArgumentException(key + " is not an integer: " + values.get(key), exception);
    }
  }

  private static void requireValue(Map<String, String> values, String key, String expected) {
    if (!expected.equals(values.get(key))) {
      throw new IllegalArgumentException(
          key + " must be " + expected + ": " + values.get(key));
    }
  }

  private static void requireExactKeys(Map<String, String> values) {
    Set<String> required = new HashSet<>(Arrays.asList(
        "schemaVersion", "semanticTag", "sourceIndexBase", "jobs", "stages", "factories",
        "processing.stage1", "processing.stage2", "setup.stage1", "setup.stage2",
        "machine.speed.f1.s1", "machine.speed.f1.s2", "machine.speed.f2.s1",
        "machine.speed.f2.s2", "machine.energy.f1.s1", "machine.energy.f1.s2",
        "machine.energy.f2.s1", "machine.energy.f2.s2", "worker.efficiency.f1.s1",
        "worker.efficiency.f1.s2", "worker.efficiency.f2.s1",
        "worker.efficiency.f2.s2", "worker.cost.f1.s1", "worker.cost.f1.s2",
        "worker.cost.f2.s1", "worker.cost.f2.s2", "encoding.JS", "encoding.FA",
        "encoding.MA", "encoding.WA"));
    if (!values.keySet().equals(required)) {
      Set<String> missing = new HashSet<>(required);
      missing.removeAll(values.keySet());
      Set<String> unknown = new HashSet<>(values.keySet());
      unknown.removeAll(required);
      throw new IllegalArgumentException(
          "Golden fixture keys mismatch; missing=" + missing + ", unknown=" + unknown);
    }
  }
}
