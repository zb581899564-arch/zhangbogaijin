package org.uma.jmetal.algorithm.multiobjective.mypso.v35;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Random;
import java.util.TreeMap;
import org.junit.Test;
import org.uma.jmetal.problem.multiobjective.dfsp.ZhangBoEDHHFSPW;
import org.uma.jmetal.problem.multiobjective.dfsp.fatigue.ZhangBoFatigueParameterCodec;
import org.uma.jmetal.problem.multiobjective.dfsp.fatigue.ZhangBoFatigueParameterGenerator;
import org.uma.jmetal.problem.multiobjective.dfsp.fatigue.ZhangBoFatigueParameters;
import org.uma.jmetal.problem.multiobjective.dfsp.fatigue.ZhangBoInstanceExtension;
import org.uma.jmetal.problem.multiobjective.dfsp.fatigue.ZhangBoInstanceExtensionCodec;
import org.uma.jmetal.problem.multiobjective.dfsp.fatigue.ZhangBoInstanceExtensionGenerator;
import org.uma.jmetal.problem.multiobjective.dfsp.fatigue.ZhangBoV35ProblemFactory;
import org.uma.jmetal.problem.multiobjective.dfsp.decoder.ProductionDecodeMode;
import static org.junit.Assert.*;

/**
 * Deterministic generator for the V35 small-instance pilot set (V35-P22/P23).
 * Produces four EADHFSP instances into java-jmetal58/EADHFSP-pilot/ with the
 * production serialization chain (instance txt -> fatigue parameters ->
 * instance extension), keyed by the SHA-256 of each instance name.  The
 * canonical data directories are never touched.  Rerunning the test must
 * reproduce byte-identical files; a second run verifies the recorded SHA-256
 * manifest instead of regenerating.
 */
public class V35PilotInstanceGeneratorTest {
  private static final String[] INSTANCES = {
      "10_2_3_1", "10_3_2_1", "5_2_2_1", "3_2_2_1"
  };

  @Test(timeout = 300000)
  public void generatePilotInstances() throws Exception {
    Path project = Paths.get("").toAbsolutePath().normalize();
    if (project.getFileName() != null
        && "jmetal-algorithm".equals(project.getFileName().toString())) {
      project = project.getParent();
    }
    while (project.getParent() != null && !Files.exists(project.resolve("AGENTS.md"))) {
      project = project.getParent();
    }
    final Path root = project;
    Path pilot = root.resolve("java-jmetal58/EADHFSP-pilot");
    Path instanceDir = pilot.resolve("EADHFSP");
    Path fatigueDir = pilot.resolve("fatigue-parameters/v1");
    Path extensionDir = pilot.resolve("instance-extensions/v1");
    Files.createDirectories(instanceDir);
    Files.createDirectories(fatigueDir);
    Files.createDirectories(extensionDir);

    Path manifestPath = pilot.resolve("instance-manifest.txt");
    Map<String, String> previous = readManifest(manifestPath);

    Map<String, String> hashes = new TreeMap<>();
    StringBuilder manifest = new StringBuilder();
    manifest.append("schemaVersion=1\nsemanticTag=v35_pilot_small_instances\n")
        .append("generator=V35PilotInstanceGeneratorTest\n")
        .append("sampler=sha256-name-keyed\n")
        .append("ranges=machinesPerFactoryStage=2..4,speed=0.9..1.3(step0.1),")
        .append("power=6..12,standom=8..20,")
        .append("workersPerFactory=sumOfStageNeeds(min(m,2)),")
        .append("level=0.8..1.2(step0.1),cost=8..12\n")
        .append("enumerationPin=jobs<=5 allStagesMachines=2 workersPerFactory=2*stages "
            + "(V35-P23 exact-front enumerability)\n");

    for (String name : INSTANCES) {
      String[] parts = name.split("_");
      int jobs = Integer.parseInt(parts[0]);
      int stages = Integer.parseInt(parts[1]);
      int factories = Integer.parseInt(parts[2]);
      Random random = new Random(keyedSeed(name));

      String text = instanceText(name, jobs, stages, factories, random);
      Path instancePath = instanceDir.resolve(name + ".txt");
      Files.write(instancePath, text.getBytes(StandardCharsets.UTF_8));
      String instanceSha = sha256(Files.readAllBytes(instancePath)).toUpperCase();
      hashes.put("EADHFSP/" + name + ".txt", instanceSha);

      System.setProperty("dhfsp.data.dir", instanceDir.toString());
      ZhangBoEDHHFSPW source = new ZhangBoEDHHFSPW(jobs, stages, factories, 1);
      assertEquals("instance sha must match loader", instanceSha,
          source.getFatigueInstanceData().getInstanceSha256());

      ZhangBoFatigueParameters fatigue = ZhangBoFatigueParameterGenerator.generate(
          source.getFatigueInstanceData());
      ZhangBoFatigueParameterCodec.write(ZhangBoFatigueParameterCodec.fileFor(
          fatigueDir, jobs, stages, factories, 1), source.getFatigueInstanceData(), fatigue);
      hashes.put("fatigue-parameters/v1/" + name + ".fatigue.txt",
          sha256(Files.readAllBytes(ZhangBoFatigueParameterCodec.fileFor(
              fatigueDir, jobs, stages, factories, 1))));

      ZhangBoInstanceExtension extension = ZhangBoInstanceExtensionGenerator.generate(
          instanceSha, jobs, stages);
      ZhangBoInstanceExtensionCodec.write(ZhangBoInstanceExtensionCodec.fileFor(
          extensionDir, jobs, stages, factories, 1), extension);
      hashes.put("instance-extensions/v1/" + name + ".setup.txt",
          sha256(Files.readAllBytes(ZhangBoInstanceExtensionCodec.fileFor(
              extensionDir, jobs, stages, factories, 1))));

      // Round-trip verification through the full production loading path.
      System.setProperty("dhfsp.fatigue.dir", fatigueDir.toString());
      System.setProperty("dhfsp.instance.extension.dir", extensionDir.toString());
      ZhangBoEDHHFSPW reloaded = ZhangBoEDHHFSPW.withConfiguredFatigueParameters(
          jobs, stages, factories, 1);
      assertEquals("reload fatigue sha", instanceSha,
          reloaded.getFatigueInstanceData().getInstanceSha256());
      ZhangBoV35ProblemFactory.create(reloaded.getFatigueInstanceData(),
          reloaded.getFatigueParameters(), ProductionDecodeMode.FM3, 20260808L);

      manifest.append("instance=").append(name).append(",jobs=").append(jobs)
          .append(",stages=").append(stages).append(",factories=").append(factories)
          .append(",instanceSha256=").append(instanceSha).append('\n');
    }

    for (Map.Entry<String, String> entry : hashes.entrySet()) {
      manifest.append(entry.getKey()).append('=').append(entry.getValue()).append('\n');
      if (previous.containsKey(entry.getKey())) {
        assertEquals("determinism recheck " + entry.getKey(),
            previous.get(entry.getKey()), entry.getValue());
      }
    }
    Files.write(manifestPath, manifest.toString().getBytes(StandardCharsets.UTF_8));
  }

  private static String instanceText(String name, int jobs, int stages, int factories,
      Random random) {
    int[][] machines = new int[factories][stages];
    for (int f = 0; f < factories; f++) {
      for (int s = 0; s < stages; s++) {
        machines[f][s] = 2 + random.nextInt(3);
      }
    }
    if (jobs <= 5) {
      // Enumeration instances (V35-P23): pin every stage's machine count to 2 so
      // the exact four-vector space stays enumerable (N! * 2^N * 2^N * 2^N =
      // 3.93M decodes for 5 jobs) and the author worker-stage partition stays
      // exact (workers = 2 * stages).
      for (int f = 0; f < factories; f++) {
        java.util.Arrays.fill(machines[f], 2);
      }
    }
    // The author worker-stage partition must consume the worker pool exactly:
    // each (factory, stage) needs min(machines, 2) workers when machines > 2,
    // otherwise machines workers.  workers[f] is derived, never sampled.
    int[] workers = new int[factories];
    for (int f = 0; f < factories; f++) {
      int total = 0;
      for (int s = 0; s < stages; s++) {
        total += machines[f][s] <= 2 ? machines[f][s] : machines[f][s] - 1;
      }
      workers[f] = total;
    }
    StringBuilder text = new StringBuilder();
    text.append(factories).append(' ').append(stages).append(' ').append(jobs).append('\n');
    text.append("number of machines at each stage in each factory:\n");
    for (int f = 0; f < factories; f++) {
      for (int s = 0; s < stages; s++) {
        if (s > 0) text.append(',');
        text.append(machines[f][s]);
      }
      text.append('\n');
    }
    text.append("speed of each machine:\n");
    for (int f = 0; f < factories; f++) {
      for (int s = 0; s < stages; s++) {
        appendDoubles(text, machines[f][s], random, 0.9, 0.1, 5);
      }
    }
    text.append("power consumption of each machine:\n");
    for (int f = 0; f < factories; f++) {
      for (int s = 0; s < stages; s++) {
        appendInts(text, machines[f][s], random, 6, 7);
      }
    }
    text.append("standom processing time of each job at each stage:\n");
    for (int j = 0; j < jobs; j++) {
      appendInts(text, stages, random, 8, 13);
    }
    text.append("number of workers in each factory:\n");
    for (int f = 0; f < factories; f++) {
      if (f > 0) text.append(',');
      text.append(workers[f]);
    }
    text.append('\n');
    text.append("level of each worker in each factory:\n");
    for (int f = 0; f < factories; f++) {
      appendDoubles(text, workers[f], random, 0.8, 0.1, 5);
    }
    text.append("cost of each worker in each factory:\n");
    for (int f = 0; f < factories; f++) {
      appendInts(text, workers[f], random, 8, 5);
    }
    return text.toString();
  }

  private static void appendInts(StringBuilder text, int count, Random random, int lower,
      int span) {
    for (int i = 0; i < count; i++) {
      if (i > 0) text.append(',');
      text.append(lower + random.nextInt(span));
    }
    text.append('\n');
  }

  private static void appendDoubles(StringBuilder text, int count, Random random, double lower,
      double step, int span) {
    for (int i = 0; i < count; i++) {
      if (i > 0) text.append(',');
      text.append(String.format(Locale.ROOT, "%.1f", lower + step * random.nextInt(span)));
    }
    text.append('\n');
  }

  private static long keyedSeed(String name) {
    try {
      byte[] digest = MessageDigest.getInstance("SHA-256")
          .digest(("v35-pilot-instance|" + name).getBytes(StandardCharsets.UTF_8));
      long seed = 0L;
      for (int i = 0; i < 8; i++) seed = (seed << 8) | (digest[i] & 0xffL);
      return seed;
    } catch (java.security.NoSuchAlgorithmException error) {
      throw new IllegalStateException(error);
    }
  }

  private static Map<String, String> readManifest(Path path) {
    Map<String, String> result = new TreeMap<>();
    if (!Files.exists(path)) return result;
    try {
      for (String line : Files.readAllLines(path)) {
        int separator = line.indexOf('=');
        if (separator > 0 && !line.startsWith("instance=")) {
          result.put(line.substring(0, separator), line.substring(separator + 1));
        }
      }
    } catch (java.io.IOException error) {
      throw new RuntimeException(error);
    }
    return result;
  }

  private static String sha256(byte[] bytes) {
    try {
      byte[] digest = MessageDigest.getInstance("SHA-256").digest(bytes);
      StringBuilder out = new StringBuilder();
      for (byte value : digest) out.append(String.format("%02x", value & 0xff));
      return out.toString();
    } catch (java.security.NoSuchAlgorithmException error) {
      throw new IllegalStateException(error);
    }
  }
}
