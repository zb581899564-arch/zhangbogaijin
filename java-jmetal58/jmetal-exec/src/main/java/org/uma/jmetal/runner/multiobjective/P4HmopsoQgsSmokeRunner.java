package org.uma.jmetal.runner.multiobjective;

import org.uma.jmetal.algorithm.multiobjective.hmopsoqgs.HmopsoQgsConfiguration;
import org.uma.jmetal.algorithm.multiobjective.hmopsoqgs.PddrFf;
import org.uma.jmetal.algorithm.multiobjective.hmopsoqgs.PublishedHmopsoQgs;
import org.uma.jmetal.algorithm.multiobjective.hmopsoqgs.PublishedHmopsoQgsBuilder;
import org.uma.jmetal.problem.multiobjective.dfsp.decoder.DecodeOptions;
import org.uma.jmetal.problem.multiobjective.dfsp.decoder.DhhfspProblem;
import org.uma.jmetal.problem.multiobjective.dfsp.decoder.EvaluationCounter;
import org.uma.jmetal.problem.multiobjective.dfsp.decoder.OriginalDhhfspDecoder;
import org.uma.jmetal.problem.multiobjective.dfsp.decoder.RandomDhhfspSolutionFactory;
import org.uma.jmetal.problem.multiobjective.dfsp.model.CanonicalEadhfspInstanceLoader;
import org.uma.jmetal.problem.multiobjective.dfsp.model.Chapter4GoldenFixture;
import org.uma.jmetal.problem.multiobjective.dfsp.model.DhhfspFourVectorSolution;
import org.uma.jmetal.problem.multiobjective.dfsp.model.DhhfspInstance;
import org.uma.jmetal.util.pseudorandom.impl.JavaRandomGenerator;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;

/** Runs only the approved 2,000-FE P4 engineering smoke, never the 500,000-FE profile. */
public final class P4HmopsoQgsSmokeRunner {
  private static final long SEED = 20260808L;
  private P4HmopsoQgsSmokeRunner() { }

  public static void main(String[] args) throws Exception {
    String mode = args.length == 0 ? "all" : args[0];
    Path output = Paths.get(System.getProperty("dhfsp.output.dir", "results"), "p4");
    Files.createDirectories(output);
    write(output.resolve("table9-not-run.properties"),
        HmopsoQgsConfiguration.publishedTable9(SEED).toCanonicalText()
            + "executionStatus=NOT_RUN_FORMAL_CONFIGURATION\n");
    if ("golden".equals(mode) || "all".equals(mode)) {
      Chapter4GoldenFixture fixture = Chapter4GoldenFixture.load();
      runThree("golden", fixture.getInstance(), null, output);
    }
    if ("real".equals(mode) || "all".equals(mode)) {
      Path data = Paths.get(System.getProperty("dhfsp.data.dir", "EADHFSP"), "20_2_3_1.txt");
      CanonicalEadhfspInstanceLoader.Loaded loaded =
          new CanonicalEadhfspInstanceLoader(SEED).load(data);
      write(output.resolve("real-supplementation.txt"), loaded.getSupplementationManifest());
      runThree("real-20_2_3_1", loaded.getInstance(), data, output);
    }
    if (!"golden".equals(mode) && !"real".equals(mode) && !"all".equals(mode)) {
      throw new IllegalArgumentException("mode must be golden, real, or all");
    }
    writeManifest(output);
  }

  private static void runThree(
      String name, DhhfspInstance instance, Path source, Path output) throws IOException {
    String expectedTraceHash = null;
    String expectedResultHash = null;
    for (int repetition = 1; repetition <= 3; repetition++) {
      EvaluationCounter counter = new EvaluationCounter();
      DhhfspProblem problem = new DhhfspProblem(instance, new OriginalDhhfspDecoder(),
          DecodeOptions.deterministic(SEED),
          new RandomDhhfspSolutionFactory(instance,
              new JavaRandomGenerator(SEED ^ 0x13579BDFL), "published_baseline"), counter);
      PublishedHmopsoQgs algorithm = new PublishedHmopsoQgsBuilder(problem, instance)
          .setConfiguration(HmopsoQgsConfiguration.engineeringSmoke(SEED))
          .setRandomGenerator(new JavaRandomGenerator(SEED))
          .build();
      algorithm.run();
      String trace = algorithm.traceText();
      String result = resultText(algorithm.getResult(), counter.getSuccessfulEvaluations(), source);
      String traceHash = sha256(trace.getBytes(StandardCharsets.UTF_8));
      String resultHash = sha256(result.getBytes(StandardCharsets.UTF_8));
      if (expectedTraceHash == null) {
        expectedTraceHash = traceHash;
        expectedResultHash = resultHash;
      } else if (!expectedTraceHash.equals(traceHash) || !expectedResultHash.equals(resultHash)) {
        throw new IllegalStateException(name + " is not byte-replayable at repetition " + repetition);
      }
      write(output.resolve(name + "-run" + repetition + "-trace.txt"), trace);
      write(output.resolve(name + "-run" + repetition + "-result.txt"), result);
      write(output.resolve(name + "-run" + repetition + "-qtable.txt"), algorithm.qTablesText());
    }
    write(output.resolve(name + "-replay.properties"),
        "seed=" + SEED + '\n' + "repetitions=3\n" + "maxFEs=2000\n"
            + "traceSha256=" + expectedTraceHash + '\n'
            + "resultSha256=" + expectedResultHash + '\n'
            + "byteReplay=true\n");
  }

  private static String resultText(
      List<DhhfspFourVectorSolution> result, long evaluations, Path source) {
    StringBuilder builder = new StringBuilder();
    builder.append("semanticTag=published_baseline\n")
        .append("seed=").append(SEED).append('\n')
        .append("evaluations=").append(evaluations).append('\n')
        .append("input=").append(source == null ? "ESWA_FIG3_GOLDEN" : source).append('\n')
        .append("nonDominatedCount=").append(result.size()).append('\n');
    for (DhhfspFourVectorSolution solution : result) {
      builder.append(PddrFf.fingerprint(solution)).append('|')
          .append(Double.toString(solution.getObjective(0))).append(',')
          .append(Double.toString(solution.getObjective(1))).append(',')
          .append(Double.toString(solution.getObjective(2))).append('\n');
    }
    return builder.toString();
  }

  private static void writeManifest(Path directory) throws IOException {
    List<String> lines = new ArrayList<>();
    java.util.stream.Stream<Path> stream = Files.list(directory);
    try {
      stream.sorted().forEach(path -> {
        if (!path.getFileName().toString().equals("manifest.sha256")) {
          try {
            lines.add(sha256(Files.readAllBytes(path)) + "  " + path.getFileName());
          } catch (IOException exception) {
            throw new IllegalStateException(exception);
          }
        }
      });
    } finally {
      stream.close();
    }
    write(directory.resolve("manifest.sha256"), join(lines));
  }

  private static String join(List<String> lines) {
    StringBuilder builder = new StringBuilder();
    for (String line : lines) builder.append(line).append('\n');
    return builder.toString();
  }
  private static void write(Path path, String value) throws IOException {
    Files.write(path, value.getBytes(StandardCharsets.UTF_8));
  }
  private static String sha256(byte[] value) {
    try {
      byte[] digest = MessageDigest.getInstance("SHA-256").digest(value);
      StringBuilder builder = new StringBuilder();
      for (byte item : digest) builder.append(String.format("%02x", item & 0xff));
      return builder.toString();
    } catch (NoSuchAlgorithmException exception) {
      throw new IllegalStateException(exception);
    }
  }
}
