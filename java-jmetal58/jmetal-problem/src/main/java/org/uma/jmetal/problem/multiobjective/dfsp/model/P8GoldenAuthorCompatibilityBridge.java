package org.uma.jmetal.problem.multiobjective.dfsp.model;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import org.uma.jmetal.problem.multiobjective.dfsp.fatigue.ZhangBoFatigueInstanceData;
import org.uma.jmetal.problem.multiobjective.dfsp.fatigue.ZhangBoFatigueParameterCodec;
import org.uma.jmetal.problem.multiobjective.dfsp.fatigue.ZhangBoFatigueParameterGenerator;
import org.uma.jmetal.problem.multiobjective.dfsp.fatigue.ZhangBoFatigueParameters;
import org.uma.jmetal.problem.multiobjective.dfsp.fatigue.ZhangBoInstanceExtension;
import org.uma.jmetal.problem.multiobjective.dfsp.fatigue.ZhangBoInstanceExtensionCodec;

/** Materializes the P2 golden paper instance as an isolated author-compatible EADHFSP input. */
public final class P8GoldenAuthorCompatibilityBridge {
  public static final String INSTANCE_NAME = "10_2_2_1";

  private P8GoldenAuthorCompatibilityBridge() { }

  public static void main(String[] args) throws Exception {
    Path output = args.length == 0 ? Paths.get("p8-bridge/v1") : Paths.get(args[0]);
    Manifest manifest = materialize(output);
    System.out.println("P8_GOLDEN_BRIDGE " + manifest.manifestPath
        + " instanceSha256=" + manifest.instanceSha256);
  }

  public static Manifest materialize(Path root) throws IOException {
    if (root == null) throw new IllegalArgumentException("root cannot be null");
    root = root.toAbsolutePath().normalize();
    Chapter4GoldenFixture fixture = Chapter4GoldenFixture.load();
    DhhfspInstance source = fixture.getInstance();
    if (source.getNumberOfJobs() != 10 || source.getNumberOfStages() != 2
        || source.getNumberOfFactories() != 2) {
      throw new IllegalStateException("Unexpected Chapter 4 fixture dimensions");
    }

    String instanceText = eadhfspText(source);
    byte[] instanceBytes = instanceText.getBytes(StandardCharsets.UTF_8);
    String instanceSha = ZhangBoFatigueParameterCodec.sha256(instanceBytes);
    Path dataFile = root.resolve("EADHFSP").resolve(INSTANCE_NAME + ".txt");
    Files.createDirectories(dataFile.getParent());
    Files.write(dataFile, instanceBytes);

    int[][] setup = setupByJob(source);
    ZhangBoInstanceExtension extension = ZhangBoInstanceExtension.authorCompatibilityBridge(
        instanceSha, source.getNumberOfJobs(), source.getNumberOfStages(), setup);
    Path setupFile = root.resolve("instance-extensions/v1")
        .resolve(INSTANCE_NAME + ".setup.txt");
    extension = ZhangBoInstanceExtensionCodec.write(setupFile, extension);

    ZhangBoFatigueInstanceData fatigueData = fatigueData(source, instanceSha, extension);
    ZhangBoFatigueParameters parameters = ZhangBoFatigueParameterGenerator.generate(fatigueData);
    Path fatigueFile = root.resolve("fatigue-parameters/v1")
        .resolve(INSTANCE_NAME + ".fatigue.txt");
    parameters = ZhangBoFatigueParameterCodec.write(fatigueFile, fatigueData, parameters);

    String sourceHash = resourceSha256();
    String manifestText = "schemaVersion=1\n"
        + "semanticTag=author_compatibility_bridge\n"
        + "sourceSemanticTag=published_baseline\n"
        + "sourceResource=" + Chapter4GoldenFixture.RESOURCE + "\n"
        + "sourceResourceSha256=" + sourceHash + "\n"
        + "instanceFile=EADHFSP/" + INSTANCE_NAME + ".txt\n"
        + "instanceSha256=" + instanceSha + "\n"
        + "setupFile=instance-extensions/v1/" + INSTANCE_NAME + ".setup.txt\n"
        + "setupSha256=" + extension.getConfigurationSha256() + "\n"
        + "fatigueFile=fatigue-parameters/v1/" + INSTANCE_NAME + ".fatigue.txt\n"
        + "fatigueSha256=" + parameters.getConfigurationSha256() + "\n"
        + "processingMapping=paper stage-job to EADHFSP job-stage\n"
        + "machineMapping=paper factory-stage local order preserved\n"
        + "workerMapping=paper factory-stage arrays concatenated by stage\n"
        + "sutMapping=exact paper table; no sampling\n";
    Path manifestPath = root.resolve("bridge-manifest.txt");
    Files.createDirectories(manifestPath.getParent());
    Files.write(manifestPath, manifestText.getBytes(StandardCharsets.UTF_8));
    return new Manifest(root, dataFile, setupFile, fatigueFile, manifestPath, instanceSha);
  }

  private static String eadhfspText(DhhfspInstance source) {
    StringBuilder out = new StringBuilder();
    out.append(source.getNumberOfFactories()).append(' ')
        .append(source.getNumberOfStages()).append(' ')
        .append(source.getNumberOfJobs()).append('\n');
    out.append("number of machines at each stage in each factory:\n");
    for (int f = 0; f < source.getNumberOfFactories(); f++) {
      for (int k = 0; k < source.getNumberOfStages(); k++) {
        if (k > 0) out.append(',');
        out.append(source.getMachineCount(f, k));
      }
      out.append('\n');
    }
    out.append("speed of each machine:\n");
    appendMachineDoubles(out, source, false);
    out.append("power consumption of each machine:\n");
    appendMachineDoubles(out, source, true);
    out.append("standom processing time of each job at each stage:\n");
    for (int job = 0; job < source.getNumberOfJobs(); job++) {
      for (int stage = 0; stage < source.getNumberOfStages(); stage++) {
        if (stage > 0) out.append(',');
        out.append(exactInt(source.getStandardProcessingTime(stage, job), "processing"));
      }
      out.append('\n');
    }
    out.append("number of workers in each factory:\n");
    for (int f = 0; f < source.getNumberOfFactories(); f++) {
      if (f > 0) out.append(',');
      int workers = 0;
      for (int k = 0; k < source.getNumberOfStages(); k++) workers += source.getWorkerCount(f, k);
      out.append(workers);
    }
    out.append('\n').append("level of each worker in each factory:\n");
    appendWorkers(out, source, false);
    out.append("cost of each worker in each factory:\n");
    appendWorkers(out, source, true);
    return out.toString();
  }

  private static void appendMachineDoubles(
      StringBuilder out, DhhfspInstance source, boolean integerEnergy) {
    for (int f = 0; f < source.getNumberOfFactories(); f++) {
      for (int k = 0; k < source.getNumberOfStages(); k++) {
        double[] values = integerEnergy ? source.getMachineEnergyPerUnit(f, k)
            : source.getMachineSpeeds(f, k);
        for (int index = 0; index < values.length; index++) {
          if (index > 0) out.append(',');
          if (integerEnergy) out.append(exactInt(values[index], "machine energy"));
          else out.append(Double.toString(values[index]));
        }
        out.append('\n');
      }
    }
  }

  private static void appendWorkers(
      StringBuilder out, DhhfspInstance source, boolean integerCost) {
    for (int f = 0; f < source.getNumberOfFactories(); f++) {
      boolean first = true;
      for (int k = 0; k < source.getNumberOfStages(); k++) {
        double[] values = integerCost ? source.getWorkerCostPerUnit(f, k)
            : source.getWorkerEfficiencies(f, k);
        for (double value : values) {
          if (!first) out.append(',');
          if (integerCost) out.append(exactInt(value, "worker cost"));
          else out.append(Double.toString(value));
          first = false;
        }
      }
      out.append('\n');
    }
  }

  private static int[][] setupByJob(DhhfspInstance source) {
    int[][] result = new int[source.getNumberOfJobs()][source.getNumberOfStages()];
    for (int job = 0; job < result.length; job++) {
      for (int stage = 0; stage < result[job].length; stage++) {
        result[job][stage] = exactInt(source.getStandardSetupTime(stage, job), "setup");
      }
    }
    return result;
  }

  private static ZhangBoFatigueInstanceData fatigueData(
      DhhfspInstance source, String instanceSha, ZhangBoInstanceExtension extension) {
    int factories = source.getNumberOfFactories();
    int stages = source.getNumberOfStages();
    int[][] machines = new int[factories][stages];
    double[][][] speeds = new double[factories][stages][];
    int[][][] powers = new int[factories][stages][];
    int[] workerCounts = new int[factories];
    double[][] workerEfficiency = new double[factories][];
    int[][] workerCost = new int[factories][];
    for (int f = 0; f < factories; f++) {
      for (int k = 0; k < stages; k++) {
        machines[f][k] = source.getMachineCount(f, k);
        speeds[f][k] = source.getMachineSpeeds(f, k);
        double[] energy = source.getMachineEnergyPerUnit(f, k);
        powers[f][k] = new int[energy.length];
        for (int m = 0; m < energy.length; m++) powers[f][k][m] = exactInt(energy[m], "power");
        workerCounts[f] += source.getWorkerCount(f, k);
      }
      workerEfficiency[f] = new double[workerCounts[f]];
      workerCost[f] = new int[workerCounts[f]];
      int offset = 0;
      for (int k = 0; k < stages; k++) {
        double[] efficiency = source.getWorkerEfficiencies(f, k);
        double[] cost = source.getWorkerCostPerUnit(f, k);
        for (int w = 0; w < efficiency.length; w++) {
          workerEfficiency[f][offset] = efficiency[w];
          workerCost[f][offset] = exactInt(cost[w], "worker cost");
          offset++;
        }
      }
    }
    int[][] processing = new int[source.getNumberOfJobs()][stages];
    for (int job = 0; job < processing.length; job++) {
      for (int k = 0; k < stages; k++) {
        processing[job][k] = exactInt(source.getStandardProcessingTime(k, job), "processing");
      }
    }
    return new ZhangBoFatigueInstanceData(instanceSha, source.getNumberOfJobs(), stages,
        factories, machines, speeds, powers, processing, workerCounts,
        workerEfficiency, workerCost, extension);
  }

  private static int exactInt(double value, String field) {
    int result = (int) Math.rint(value);
    if (Math.abs(value - result) > 1e-12) {
      throw new IllegalArgumentException(field + " is not exactly representable by EADHFSP: " + value);
    }
    return result;
  }

  private static String resourceSha256() throws IOException {
    try (InputStream input = Chapter4GoldenFixture.class.getResourceAsStream(
        Chapter4GoldenFixture.RESOURCE)) {
      if (input == null) throw new IOException("Missing " + Chapter4GoldenFixture.RESOURCE);
      ByteArrayOutputStream output = new ByteArrayOutputStream();
      byte[] buffer = new byte[4096];
      int read;
      while ((read = input.read(buffer)) >= 0) output.write(buffer, 0, read);
      return ZhangBoFatigueParameterCodec.sha256(output.toByteArray());
    }
  }

  public static final class Manifest {
    public final Path root;
    public final Path dataFile;
    public final Path setupFile;
    public final Path fatigueFile;
    public final Path manifestPath;
    public final String instanceSha256;

    private Manifest(Path root, Path dataFile, Path setupFile, Path fatigueFile,
        Path manifestPath, String instanceSha256) {
      this.root = root;
      this.dataFile = dataFile;
      this.setupFile = setupFile;
      this.fatigueFile = fatigueFile;
      this.manifestPath = manifestPath;
      this.instanceSha256 = instanceSha256;
    }
  }
}
