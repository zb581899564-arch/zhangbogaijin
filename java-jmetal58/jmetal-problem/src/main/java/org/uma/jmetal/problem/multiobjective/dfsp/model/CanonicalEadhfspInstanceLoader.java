package org.uma.jmetal.problem.multiobjective.dfsp.model;

import org.uma.jmetal.util.pseudorandom.impl.JavaRandomGenerator;
import org.uma.jmetal.problem.multiobjective.dfsp.fatigue.ZhangBoFatigueParameterCodec;
import org.uma.jmetal.problem.multiobjective.dfsp.fatigue.ZhangBoInstanceExtension;
import org.uma.jmetal.problem.multiobjective.dfsp.fatigue.ZhangBoInstanceExtensionGenerator;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/** Deterministic canonical adapter for the author's EADHFSP text instances. */
public final class CanonicalEadhfspInstanceLoader {
  public static final class Loaded {
    private final DhhfspInstance instance;
    private final String supplementationManifest;

    private Loaded(DhhfspInstance instance, String supplementationManifest) {
      this.instance = instance;
      this.supplementationManifest = supplementationManifest;
    }
    public DhhfspInstance getInstance() { return instance; }
    public String getSupplementationManifest() { return supplementationManifest; }
  }

  private final long seed;
  public CanonicalEadhfspInstanceLoader(long seed) { this.seed = seed; }

  public Loaded load(Path path) throws IOException {
    byte[] instanceBytes = Files.readAllBytes(path);
    List<String> lines = Files.readAllLines(path, StandardCharsets.UTF_8);
    if (lines.isEmpty()) throw new IllegalArgumentException("empty EADHFSP instance");
    int[] dimensions = integers(lines.get(0));
    if (dimensions.length != 3) throw new IllegalArgumentException("first line must be F S N");
    int factories = dimensions[0];
    int stages = dimensions[1];
    int jobs = dimensions[2];
    int cursor = 2;
    int[][] machineCount = new int[factories][stages];
    for (int factory = 0; factory < factories; factory++) {
      int[] values = integers(lines.get(cursor++));
      if (values.length != stages) throw new IllegalArgumentException("machine count row");
      machineCount[factory] = values;
    }
    requireLabel(lines.get(cursor++), "speed of each machine:");
    double[][][] speeds = new double[factories][stages][];
    for (int factory = 0; factory < factories; factory++) {
      for (int stage = 0; stage < stages; stage++) {
        speeds[factory][stage] = doubles(lines.get(cursor++), machineCount[factory][stage]);
      }
    }
    requireLabel(lines.get(cursor++), "power consumption of each machine:");
    double[][][] energy = new double[factories][stages][];
    for (int factory = 0; factory < factories; factory++) {
      for (int stage = 0; stage < stages; stage++) {
        energy[factory][stage] = doubles(lines.get(cursor++), machineCount[factory][stage]);
      }
    }
    requireLabel(lines.get(cursor++), "standom processing time of each job at each stage:");
    double[][] processing = new double[stages][jobs];
    for (int job = 0; job < jobs; job++) {
      double[] values = doubles(lines.get(cursor++), stages);
      for (int stage = 0; stage < stages; stage++) processing[stage][job] = values[stage];
    }

    double[][] setup = new double[stages][jobs];
    String instanceSha = ZhangBoFatigueParameterCodec.sha256(instanceBytes);
    ZhangBoInstanceExtension extension = ZhangBoInstanceExtensionGenerator.generate(
        instanceSha, jobs, stages);
    StringBuilder manifest = new StringBuilder();
    manifest.append("semanticTag=deterministic_canonical\nseed=").append(seed).append('\n')
        .append("instanceSha256=").append(instanceSha).append('\n')
        .append("sutSampler=").append(ZhangBoInstanceExtensionGenerator.SAMPLER_ID).append('\n')
        .append("sutSeed=").append(ZhangBoInstanceExtension.SUT_SEED).append('\n');
    for (int stage = 0; stage < stages; stage++) {
      for (int job = 0; job < jobs; job++) {
        int value = extension.getStandardSetupTime(job, stage);
        setup[stage][job] = value;
        manifest.append("SUT,").append(stage).append(',').append(job).append(',')
            .append(value).append('\n');
      }
    }

    double[][][] workerEfficiency = new double[factories][stages][];
    double[][][] workerCost = new double[factories][stages][];
    for (int factory = 0; factory < factories; factory++) {
      for (int stage = 0; stage < stages; stage++) {
        int machines = machineCount[factory][stage];
        int workers = machines <= 2 ? machines : machines - 1;
        workerEfficiency[factory][stage] = new double[workers];
        workerCost[factory][stage] = new double[workers];
        for (int worker = 0; worker < workers; worker++) {
          double efficiency = keyed("WE:f=" + factory + ":s=" + stage + ":w=" + worker)
              .nextDouble(0.5, 1.5);
          workerEfficiency[factory][stage][worker] = efficiency;
          workerCost[factory][stage][worker] = efficiency * 10.0;
          manifest.append("WORKER,").append(factory).append(',').append(stage).append(',')
              .append(worker).append(',').append(Double.toString(efficiency)).append(',')
              .append(Double.toString(efficiency * 10.0)).append('\n');
        }
      }
    }
    DhhfspInstance instance = new DhhfspInstance(jobs, stages, factories,
        processing, setup, speeds, energy, workerEfficiency, workerCost);
    return new Loaded(instance, manifest.toString());
  }

  private JavaRandomGenerator keyed(String key) {
    long value = seed ^ 0x9E3779B97F4A7C15L;
    for (int index = 0; index < key.length(); index++) {
      value ^= key.charAt(index);
      value *= 0x100000001B3L;
      value ^= value >>> 32;
    }
    return new JavaRandomGenerator(value);
  }

  private static void requireLabel(String actual, String expected) {
    if (!expected.equals(actual.trim())) {
      throw new IllegalArgumentException("expected label " + expected + "; got " + actual);
    }
  }
  private static int[] integers(String text) {
    String[] tokens = text.trim().split("[ ,]+", -1);
    List<Integer> values = new ArrayList<>();
    for (String token : tokens) if (!token.isEmpty()) values.add(Integer.parseInt(token));
    int[] result = new int[values.size()];
    for (int index = 0; index < values.size(); index++) result[index] = values.get(index);
    return result;
  }
  private static double[] doubles(String text, int expected) {
    String[] tokens = text.trim().split("[ ,]+", -1);
    List<Double> values = new ArrayList<>();
    for (String token : tokens) if (!token.isEmpty()) values.add(Double.parseDouble(token));
    if (values.size() != expected) {
      throw new IllegalArgumentException("expected " + expected + " values: " + text);
    }
    double[] result = new double[expected];
    for (int index = 0; index < expected; index++) result[index] = values.get(index);
    return result;
  }
}
