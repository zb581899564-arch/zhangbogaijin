package org.uma.jmetal.problem.multiobjective.dfsp.fatigue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Strict UTF-8 persistence for immutable per-instance fatigue parameters. */
public final class ZhangBoFatigueParameterCodec {
  public static final String DEFAULT_DIRECTORY = "fatigue-parameters/v1";
  private static final Pattern FILE_NAME =
      Pattern.compile("(\\d+)_(\\d+)_(\\d+)_(\\d+)\\.fatigue\\.txt");

  private ZhangBoFatigueParameterCodec() { }

  public static Path configuredDirectory() {
    return Paths.get(System.getProperty("dhfsp.fatigue.dir", DEFAULT_DIRECTORY));
  }

  public static Path fileFor(Path directory, int jobs, int stages, int factories, int problemId) {
    return directory.resolve(jobs + "_" + stages + "_" + factories + "_" + problemId + ".fatigue.txt");
  }

  public static ZhangBoFatigueParameters write(
      Path path, ZhangBoFatigueInstanceData instance, ZhangBoFatigueParameters parameters)
      throws IOException {
    validateDimensions(instance, parameters);
    validateParameterValues(parameters);
    String payload = payload(instance, parameters);
    String hash = sha256(payload.getBytes(StandardCharsets.UTF_8));
    String file = "configurationSha256=" + hash + "\n" + payload;
    Path parent = path.toAbsolutePath().normalize().getParent();
    if (parent != null) Files.createDirectories(parent);
    Files.write(path, file.getBytes(StandardCharsets.UTF_8));
    return new ZhangBoFatigueParameters(parameters.getInstanceSha256(),
        parameters.copyLambda(), parameters.copyMu(), parameters.copyMaximumIncrease(),
        parameters.getWarningThreshold(), parameters.getSafeThreshold(), hash);
  }

  public static ZhangBoFatigueParameters read(Path path, ZhangBoFatigueInstanceData instance)
      throws IOException {
    byte[] bytes = Files.readAllBytes(path);
    String file = StrictUtf8.decode(bytes, path.toString());
    if (file.indexOf('\r') >= 0) throw new IllegalArgumentException("CR characters are forbidden: " + path);
    int firstBreak = file.indexOf('\n');
    if (firstBreak < 0 || !file.startsWith("configurationSha256=")) {
      throw new IllegalArgumentException("Missing configurationSha256 header: " + path);
    }
    String expectedHash = file.substring("configurationSha256=".length(), firstBreak);
    if (!expectedHash.matches("[0-9A-F]{64}")) {
      throw new IllegalArgumentException("Invalid configuration SHA-256: " + expectedHash);
    }
    String payload = file.substring(firstBreak + 1);
    String actualHash = sha256(payload.getBytes(StandardCharsets.UTF_8));
    if (!expectedHash.equals(actualHash)) {
      throw new IllegalArgumentException("Fatigue parameter configuration hash mismatch: " + path);
    }
    Map<String, String> values = parseUnique(payload, path);
    requireExact(values, "schemaVersion", Integer.toString(ZhangBoFatigueParameters.SCHEMA_VERSION));
    requireExact(values, "semanticTag", ZhangBoFatigueParameters.SEMANTIC_TAG);
    requireExact(values, "sampler", ZhangBoFatigueParameterGenerator.SAMPLER_ID);
    requireExact(values, "samplerSeed", Long.toString(ZhangBoFatigueParameters.SAMPLER_SEED));
    requireExact(values, "instanceSha256", instance.getInstanceSha256());
    requireExact(values, "factories", Integer.toString(instance.getFactories()));
    requireExact(values, "stages", Integer.toString(instance.getStages()));
    requireExact(values, "lambdaRange", "[0.01,0.03]");
    requireExact(values, "muRange", "[0.03,0.07]");
    double warning = finite(values, "warningThreshold");
    double safe = finite(values, "safeThreshold");
    requireRange(warning, 0.0, 1.0, "warningThreshold", false);
    requireRange(safe, 0.0, 1.0, "safeThreshold", false);
    if (!(safe > warning)) {
      throw new IllegalArgumentException("safeThreshold must exceed warningThreshold");
    }
    int factories = instance.getFactories();
    int stages = instance.getStages();
    double[][][] lambda = new double[factories][stages][];
    double[][][] mu = new double[factories][stages][];
    double[] r = new double[stages];
    for (int stage = 0; stage < stages; stage++) {
      r[stage] = finite(values, "r." + stage);
      requireRange(r[stage], 0.0, 1.0, "r." + stage, true);
    }
    int expectedFields = 11 + stages;
    for (int f = 0; f < factories; f++) {
      requireExact(values, "workers." + f, Integer.toString(instance.getWorkerCount(f)));
      expectedFields++;
      for (int k = 0; k < stages; k++) {
        int workers = instance.getWorkerCount(f);
        lambda[f][k] = new double[workers];
        mu[f][k] = new double[workers];
        for (int w = 0; w < workers; w++) {
          String prefix = "p." + f + "." + w + "." + k + ".";
          lambda[f][k][w] = finite(values, prefix + "lambda");
          mu[f][k][w] = finite(values, prefix + "mu");
          requireRange(lambda[f][k][w], ZhangBoFatigueParameterGenerator.LAMBDA_MIN,
              ZhangBoFatigueParameterGenerator.LAMBDA_MAX, prefix + "lambda", true);
          requireRange(mu[f][k][w], ZhangBoFatigueParameterGenerator.MU_MIN,
              ZhangBoFatigueParameterGenerator.MU_MAX, prefix + "mu", true);
          double storedR = finite(values, prefix + "r");
          requireRange(storedR, 0.0, 1.0, prefix + "r", true);
          double delta = finite(values, prefix + "delta");
          if (Double.doubleToLongBits(storedR) != Double.doubleToLongBits(r[k])) {
            throw new IllegalArgumentException("Per-worker r differs from stage r at " + prefix);
          }
          double expectedDelta = storedR / (lambda[f][k][w] * Math.log(2.0));
          if (Math.abs(delta - expectedDelta) > 1.0e-12 * Math.max(1.0, expectedDelta)) {
            throw new IllegalArgumentException("Derived delta mismatch at " + prefix);
          }
          expectedFields += 4;
        }
      }
    }
    if (values.size() != expectedFields) {
      throw new IllegalArgumentException("Unknown or unexpected fatigue parameter fields: expected="
          + expectedFields + ", actual=" + values.size());
    }
    ZhangBoFatigueParameters result = new ZhangBoFatigueParameters(instance.getInstanceSha256(),
        lambda, mu, r, warning, safe, expectedHash);
    validateDimensions(instance, result);
    validateParameterValues(result);
    validatePathBinding(path, instance, expectedHash);
    return result;
  }

  private static String payload(
      ZhangBoFatigueInstanceData instance, ZhangBoFatigueParameters parameters) {
    StringBuilder out = new StringBuilder();
    out.append("schemaVersion=").append(ZhangBoFatigueParameters.SCHEMA_VERSION).append('\n')
        .append("semanticTag=").append(ZhangBoFatigueParameters.SEMANTIC_TAG).append('\n')
        .append("sampler=").append(ZhangBoFatigueParameterGenerator.SAMPLER_ID).append('\n')
        .append("samplerSeed=").append(ZhangBoFatigueParameters.SAMPLER_SEED).append('\n')
        .append("instanceSha256=").append(instance.getInstanceSha256()).append('\n')
        .append("factories=").append(instance.getFactories()).append('\n')
        .append("stages=").append(instance.getStages()).append('\n')
        .append("lambdaRange=[0.01,0.03]\n")
        .append("muRange=[0.03,0.07]\n")
        .append("warningThreshold=").append(number(parameters.getWarningThreshold())).append('\n')
        .append("safeThreshold=").append(number(parameters.getSafeThreshold())).append('\n');
    for (int k = 0; k < instance.getStages(); k++) {
      out.append("r.").append(k).append('=').append(number(parameters.getMaximumIncrease(k))).append('\n');
    }
    for (int f = 0; f < instance.getFactories(); f++) {
      out.append("workers.").append(f).append('=').append(instance.getWorkerCount(f)).append('\n');
      for (int k = 0; k < instance.getStages(); k++) {
        for (int w = 0; w < instance.getWorkerCount(f); w++) {
          String prefix = "p." + f + "." + w + "." + k + ".";
          out.append(prefix).append("lambda=").append(number(parameters.getLambda(f, w, k))).append('\n')
              .append(prefix).append("mu=").append(number(parameters.getMu(f, w, k))).append('\n')
              .append(prefix).append("r=").append(number(parameters.getMaximumIncrease(k))).append('\n')
              .append(prefix).append("delta=").append(number(parameters.getDelta(f, w, k))).append('\n');
        }
      }
    }
    return out.toString();
  }

  private static Map<String, String> parseUnique(String payload, Path path) {
    Map<String, String> result = new LinkedHashMap<>();
    String[] lines = payload.split("\n", -1);
    for (int index = 0; index < lines.length; index++) {
      String line = lines[index];
      if (index == lines.length - 1 && line.isEmpty()) continue;
      int separator = line.indexOf('=');
      if (separator <= 0 || separator == line.length() - 1) {
        throw new IllegalArgumentException("Malformed field at line " + (index + 2) + ": " + path);
      }
      String key = line.substring(0, separator);
      String value = line.substring(separator + 1);
      if (result.put(key, value) != null) {
        throw new IllegalArgumentException("Duplicate field '" + key + "': " + path);
      }
    }
    return result;
  }

  private static void requireExact(Map<String, String> values, String key, String expected) {
    String value = values.get(key);
    if (!expected.equals(value)) {
      throw new IllegalArgumentException("Field " + key + " must equal " + expected + ": " + value);
    }
  }

  private static double finite(Map<String, String> values, String key) {
    String text = values.get(key);
    if (text == null) throw new IllegalArgumentException("Missing field: " + key);
    final double value;
    try {
      value = Double.parseDouble(text);
    } catch (NumberFormatException exception) {
      throw new IllegalArgumentException("Invalid numeric field " + key + ": " + text, exception);
    }
    if (!Double.isFinite(value)) throw new IllegalArgumentException("Non-finite field " + key);
    if (Double.doubleToRawLongBits(value) == Double.doubleToRawLongBits(-0.0d)) {
      throw new IllegalArgumentException("Negative zero is forbidden for field " + key);
    }
    return value;
  }

  private static void requireRange(
      double value, double lower, double upper, String key, boolean inclusiveLower) {
    boolean valid = inclusiveLower
        ? value >= lower && value <= upper : value > lower && value < upper;
    if (!valid || !Double.isFinite(value)) {
      throw new IllegalArgumentException(
          "Field " + key + " outside allowed range [" + lower + ',' + upper + "]: " + value);
    }
  }

  private static void validateParameterValues(ZhangBoFatigueParameters parameters) {
    for (int stage = 0; stage < parameters.getStages(); stage++) {
      double value = parameters.getMaximumIncrease(stage);
      requireRange(value, 0.0, 1.0, "r." + stage, true);
    }
    requireRange(parameters.getWarningThreshold(), 0.0, 1.0, "warningThreshold", false);
    requireRange(parameters.getSafeThreshold(), 0.0, 1.0, "safeThreshold", false);
    if (!(parameters.getSafeThreshold() > parameters.getWarningThreshold())) {
      throw new IllegalArgumentException("safeThreshold must exceed warningThreshold");
    }
    for (int factory = 0; factory < parameters.getFactories(); factory++) {
      for (int stage = 0; stage < parameters.getStages(); stage++) {
        for (int worker = 0; worker < parameters.getWorkers(factory, stage); worker++) {
          requireRange(parameters.getLambda(factory, worker, stage),
              ZhangBoFatigueParameterGenerator.LAMBDA_MIN,
              ZhangBoFatigueParameterGenerator.LAMBDA_MAX,
              "lambda[" + factory + ',' + worker + ',' + stage + ']', true);
          requireRange(parameters.getMu(factory, worker, stage),
              ZhangBoFatigueParameterGenerator.MU_MIN,
              ZhangBoFatigueParameterGenerator.MU_MAX,
              "mu[" + factory + ',' + worker + ',' + stage + ']', true);
        }
      }
    }
  }

  private static void validatePathBinding(
      Path path, ZhangBoFatigueInstanceData instance, String configurationSha256)
      throws IOException {
    Path fileName = path.getFileName();
    if (fileName == null) return;
    Matcher matcher = FILE_NAME.matcher(fileName.toString());
    if (matcher.matches()) {
      if (Integer.parseInt(matcher.group(1)) != instance.getJobs()
          || Integer.parseInt(matcher.group(2)) != instance.getStages()
          || Integer.parseInt(matcher.group(3)) != instance.getFactories()) {
        throw new IllegalArgumentException("Fatigue file dimensions do not match instance: " + path);
      }
    }
    Path manifest = path.toAbsolutePath().normalize().getParent();
    if (manifest == null) return;
    manifest = manifest.resolve("MANIFEST.txt");
    if (!Files.exists(manifest)) return;
    String text = StrictUtf8.decode(Files.readAllBytes(manifest), manifest.toString());
    verifyManifest(text, manifest);
    String target = fileName.toString();
    boolean found = false;
    for (String line : text.substring(text.indexOf('\n') + 1).split("\\n", -1)) {
      if (line.isEmpty() || line.startsWith("schemaVersion=")
          || line.startsWith("semanticTag=") || line.startsWith("instances=")) continue;
      String[] fields = line.split("\\|", -1);
      if (fields.length == 4 && target.equals(fields[2])) {
        if (!instance.getInstanceSha256().equalsIgnoreCase(fields[1])
            || !configurationSha256.equals(fields[3])) {
          throw new IllegalArgumentException("Fatigue manifest binding mismatch: " + path);
        }
        found = true;
      }
    }
    if (!found) throw new IllegalArgumentException("Fatigue file missing from manifest: " + path);
  }

  private static void verifyManifest(String text, Path path) {
    int firstBreak = text.indexOf('\n');
    if (firstBreak <= 0 || !text.startsWith("manifestSha256=")) {
      throw new IllegalArgumentException("Missing manifestSha256 header: " + path);
    }
    String expected = text.substring("manifestSha256=".length(), firstBreak);
    String payload = text.substring(firstBreak + 1);
    if (!expected.matches("[0-9A-F]{64}")
        || !expected.equals(sha256(payload.getBytes(StandardCharsets.UTF_8)))) {
      throw new IllegalArgumentException("Manifest hash mismatch: " + path);
    }
    if (!payload.startsWith("schemaVersion=1\nsemanticTag=standardized_fatigue_scenario\n")) {
      throw new IllegalArgumentException("Unexpected fatigue manifest semantic tag: " + path);
    }
  }

  private static void validateDimensions(
      ZhangBoFatigueInstanceData instance, ZhangBoFatigueParameters parameters) {
    if (!instance.getInstanceSha256().equals(parameters.getInstanceSha256())) {
      throw new IllegalArgumentException("Instance SHA-256 mismatch");
    }
    if (instance.getFactories() != parameters.getFactories()
        || instance.getStages() != parameters.getStages()) {
      throw new IllegalArgumentException("Fatigue parameter dimensions mismatch");
    }
    for (int f = 0; f < instance.getFactories(); f++) {
      for (int k = 0; k < instance.getStages(); k++) {
        if (parameters.getWorkers(f, k) != instance.getWorkerCount(f)) {
          throw new IllegalArgumentException("Worker dimension mismatch at factory=" + f + ", stage=" + k);
        }
      }
    }
  }

  public static String sha256(byte[] bytes) {
    try {
      byte[] digest = MessageDigest.getInstance("SHA-256").digest(bytes);
      StringBuilder out = new StringBuilder(64);
      for (byte value : digest) out.append(String.format(Locale.ROOT, "%02X", value & 0xff));
      return out.toString();
    } catch (NoSuchAlgorithmException exception) {
      throw new IllegalStateException("SHA-256 is unavailable", exception);
    }
  }

  private static String number(double value) {
    return String.format(Locale.ROOT, "%.17g", value);
  }
}
