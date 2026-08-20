package org.uma.jmetal.problem.multiobjective.dfsp.fatigue;

import org.uma.jmetal.problem.multiobjective.dfsp.decoder.ProductionDecodeMode;
import org.uma.jmetal.problem.multiobjective.dfsp.fatigue.shift.ZhangBoShiftConfiguration;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Formal, legacy-independent loader for one EADHFSP instance and its
 * instance-bound SUT/fatigue manifests.
 */
public final class ZhangBoCanonicalProblemLoader {
  private static final Pattern INSTANCE_NAME =
      Pattern.compile("(\\d+)_(\\d+)_(\\d+)_(\\d+)\\.txt");

  private ZhangBoCanonicalProblemLoader() { }

  public static ZhangBoCanonicalProductionProblem load(
      Path instancePath, ProductionDecodeMode mode, long seed) throws IOException {
    return load(instancePath, mode, seed,
        ZhangBoInstanceExtensionCodec.configuredDirectory(),
        ZhangBoFatigueParameterCodec.configuredDirectory(), ZhangBoShiftConfiguration.none());
  }

  public static ZhangBoCanonicalProductionProblem load(
      Path instancePath, ProductionDecodeMode mode, long seed,
      ZhangBoShiftConfiguration shiftConfiguration) throws IOException {
    return load(instancePath, mode, seed,
        ZhangBoInstanceExtensionCodec.configuredDirectory(),
        ZhangBoFatigueParameterCodec.configuredDirectory(), shiftConfiguration);
  }

  public static ZhangBoCanonicalProductionProblem load(
      Path instancePath, ProductionDecodeMode mode, long seed,
      Path extensionDirectory, Path fatigueDirectory) throws IOException {
    return load(instancePath, mode, seed, extensionDirectory, fatigueDirectory,
        ZhangBoShiftConfiguration.none());
  }

  public static ZhangBoCanonicalProductionProblem load(
      Path instancePath, ProductionDecodeMode mode, long seed,
      Path extensionDirectory, Path fatigueDirectory,
      ZhangBoShiftConfiguration shiftConfiguration) throws IOException {
    if (instancePath == null || mode == null || extensionDirectory == null
        || fatigueDirectory == null || shiftConfiguration == null) {
      throw new IllegalArgumentException("loader arguments must not be null");
    }
    Matcher matcher = INSTANCE_NAME.matcher(instancePath.getFileName().toString());
    if (!matcher.matches()) {
      throw new IllegalArgumentException("Instance filename must be F/S/N/problem-id encoded: "
          + instancePath);
    }
    int jobs = Integer.parseInt(matcher.group(1));
    int stages = Integer.parseInt(matcher.group(2));
    int factories = Integer.parseInt(matcher.group(3));
    int problemId = Integer.parseInt(matcher.group(4));
    Parsed parsed = parse(instancePath, jobs, stages, factories);
    Path extensionPath = ZhangBoInstanceExtensionCodec.fileFor(
        extensionDirectory, jobs, stages, factories, problemId);
    ZhangBoInstanceExtension extension = ZhangBoInstanceExtensionCodec.read(
        extensionPath, parsed.instanceSha256, jobs, stages);
    ZhangBoFatigueInstanceData instance = new ZhangBoFatigueInstanceData(
        parsed.instanceSha256, jobs, stages, factories,
        parsed.machineCounts, parsed.machineSpeeds, parsed.machinePowers,
        parsed.standardTimes, parsed.workerCounts, parsed.workerEfficiencies,
        parsed.workerCosts, extension);
    Path fatiguePath = ZhangBoFatigueParameterCodec.fileFor(
        fatigueDirectory, jobs, stages, factories, problemId);
    ZhangBoFatigueParameters parameters = ZhangBoFatigueParameterCodec.read(
        fatiguePath, instance);
    return new ZhangBoCanonicalProductionProblem(
        instance, parameters, mode, seed, shiftConfiguration);
  }

  private static Parsed parse(Path path, int jobs, int stages, int factories)
      throws IOException {
    byte[] raw = Files.readAllBytes(path);
    String text = StrictUtf8.decode(raw, path.toString());
    text = text.replace("\r\n", "\n");
    if (text.indexOf('\r') >= 0) {
      throw new IllegalArgumentException("CR characters are forbidden: " + path);
    }
    String[] lines = text.split("\n", -1);
    int cursor = 0;
    int[] dimensions = integers(lines[cursor++], 3, "dimensions");
    if (dimensions[0] != factories || dimensions[1] != stages || dimensions[2] != jobs) {
      throw new IllegalArgumentException("Filename and instance dimensions disagree: " + path);
    }
    requireLabel(lines[cursor++], "number of machines at each stage in each factory:");
    int[][] machineCounts = new int[factories][stages];
    for (int factory = 0; factory < factories; factory++) {
      machineCounts[factory] = integers(lines[cursor++], stages,
          "machineCounts.factory=" + factory);
    }
    requireLabel(lines[cursor++], "speed of each machine:");
    double[][][] machineSpeeds = new double[factories][stages][];
    for (int factory = 0; factory < factories; factory++) {
      for (int stage = 0; stage < stages; stage++) {
        machineSpeeds[factory][stage] = doubles(lines[cursor++],
            machineCounts[factory][stage], "machineSpeeds");
      }
    }
    requireLabel(lines[cursor++], "power consumption of each machine:");
    int[][][] machinePowers = new int[factories][stages][];
    for (int factory = 0; factory < factories; factory++) {
      for (int stage = 0; stage < stages; stage++) {
        machinePowers[factory][stage] = integers(lines[cursor++],
            machineCounts[factory][stage], "machinePowers");
      }
    }
    requireLabel(lines[cursor++], "standom processing time of each job at each stage:");
    int[][] standardTimes = new int[jobs][stages];
    for (int job = 0; job < jobs; job++) {
      standardTimes[job] = integers(lines[cursor++], stages, "standardTimes.job=" + job);
    }
    requireLabel(lines[cursor++], "number of workers in each factory:");
    int[] workerCounts = integers(lines[cursor++], factories, "workerCounts");
    requireLabel(lines[cursor++], "level of each worker in each factory:");
    double[][] workerEfficiencies = new double[factories][];
    for (int factory = 0; factory < factories; factory++) {
      workerEfficiencies[factory] = doubles(lines[cursor++], workerCounts[factory],
          "workerEfficiencies.factory=" + factory);
    }
    requireLabel(lines[cursor++], "cost of each worker in each factory:");
    int[][] workerCosts = new int[factories][];
    for (int factory = 0; factory < factories; factory++) {
      workerCosts[factory] = integers(lines[cursor++], workerCounts[factory],
          "workerCosts.factory=" + factory);
    }
    while (cursor < lines.length && lines[cursor].isEmpty()) cursor++;
    if (cursor != lines.length) throw new IllegalArgumentException("Unexpected trailing instance data");
    return new Parsed(ZhangBoFatigueParameterCodec.sha256(raw), machineCounts,
        machineSpeeds, machinePowers, standardTimes, workerCounts,
        workerEfficiencies, workerCosts);
  }

  private static void requireLabel(String actual, String expected) {
    if (!expected.equals(actual.trim())) {
      throw new IllegalArgumentException("Expected label " + expected + "; got " + actual);
    }
  }

  private static int[] integers(String text, int expected, String name) {
    String[] tokens = text.trim().split("[ ,]+", -1);
    List<Integer> values = new ArrayList<>();
    for (String token : tokens) {
      if (token.isEmpty()) continue;
      try {
        values.add(Integer.parseInt(token));
      } catch (NumberFormatException exception) {
        throw new IllegalArgumentException(name + " contains non-integer: " + token, exception);
      }
    }
    if (values.size() != expected) {
      throw new IllegalArgumentException(name + " expected " + expected + " values");
    }
    int[] result = new int[expected];
    for (int index = 0; index < expected; index++) result[index] = values.get(index);
    return result;
  }

  private static double[] doubles(String text, int expected, String name) {
    String[] tokens = text.trim().split("[ ,]+", -1);
    List<Double> values = new ArrayList<>();
    for (String token : tokens) {
      if (token.isEmpty()) continue;
      try {
        double value = Double.parseDouble(token);
        if (!Double.isFinite(value)) throw new NumberFormatException("non-finite");
        values.add(value);
      } catch (NumberFormatException exception) {
        throw new IllegalArgumentException(name + " contains invalid number: " + token, exception);
      }
    }
    if (values.size() != expected) {
      throw new IllegalArgumentException(name + " expected " + expected + " values");
    }
    double[] result = new double[expected];
    for (int index = 0; index < expected; index++) result[index] = values.get(index);
    return result;
  }

  private static final class Parsed {
    private final String instanceSha256;
    private final int[][] machineCounts;
    private final double[][][] machineSpeeds;
    private final int[][][] machinePowers;
    private final int[][] standardTimes;
    private final int[] workerCounts;
    private final double[][] workerEfficiencies;
    private final int[][] workerCosts;

    private Parsed(String instanceSha256, int[][] machineCounts, double[][][] machineSpeeds,
                   int[][][] machinePowers, int[][] standardTimes, int[] workerCounts,
                   double[][] workerEfficiencies, int[][] workerCosts) {
      this.instanceSha256 = instanceSha256;
      this.machineCounts = machineCounts;
      this.machineSpeeds = machineSpeeds;
      this.machinePowers = machinePowers;
      this.standardTimes = standardTimes;
      this.workerCounts = workerCounts;
      this.workerEfficiencies = workerEfficiencies;
      this.workerCosts = workerCosts;
    }
  }
}
