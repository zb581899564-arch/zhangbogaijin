package org.uma.jmetal.problem.multiobjective.dfsp.fatigue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Strict UTF-8 codec for immutable setup-time extensions. */
public final class ZhangBoInstanceExtensionCodec {
  public static final String DEFAULT_DIRECTORY = "instance-extensions/v1";
  private static final Pattern FILE_NAME =
      Pattern.compile("(\\d+)_(\\d+)_(\\d+)_(\\d+)\\.setup\\.txt");

  private ZhangBoInstanceExtensionCodec() { }

  public static Path configuredDirectory() {
    String configured = System.getProperty("dhfsp.instance.extension.dir");
    if (configured != null && !configured.trim().isEmpty()) return Paths.get(configured);
    Path direct = Paths.get(DEFAULT_DIRECTORY);
    if (Files.exists(direct)) return direct;
    Path reactorParent = Paths.get("..").resolve(DEFAULT_DIRECTORY).normalize();
    return Files.exists(reactorParent) ? reactorParent : direct;
  }

  public static Path fileFor(
      Path directory, int jobs, int stages, int factories, int problemId) {
    return directory.resolve(jobs + "_" + stages + "_" + factories + "_"
        + problemId + ".setup.txt");
  }

  public static ZhangBoInstanceExtension write(
      Path path, ZhangBoInstanceExtension extension) throws IOException {
    String payload = payload(extension);
    String hash = ZhangBoFatigueParameterCodec.sha256(
        payload.getBytes(StandardCharsets.UTF_8));
    String text = "configurationSha256=" + hash + "\n" + payload;
    Path parent = path.toAbsolutePath().normalize().getParent();
    if (parent != null) Files.createDirectories(parent);
    Files.write(path, text.getBytes(StandardCharsets.UTF_8));
    return new ZhangBoInstanceExtension(extension.getInstanceSha256(), extension.getJobs(),
        extension.getStages(), extension.copyStandardSetupTimes(), hash,
        extension.getSemanticTag(), extension.getSampler(), extension.getSeedText(),
        extension.getDistribution());
  }

  public static ZhangBoInstanceExtension read(
      Path path, String instanceSha256, int jobs, int stages) throws IOException {
    String file = StrictUtf8.decode(Files.readAllBytes(path), path.toString());
    if (file.indexOf('\r') >= 0) {
      throw new IllegalArgumentException("CR characters are forbidden: " + path);
    }
    int firstBreak = file.indexOf('\n');
    if (firstBreak < 0 || !file.startsWith("configurationSha256=")) {
      throw new IllegalArgumentException("Missing configurationSha256 header: " + path);
    }
    String expectedHash = file.substring("configurationSha256=".length(), firstBreak);
    if (!expectedHash.matches("[0-9A-F]{64}")) {
      throw new IllegalArgumentException("Invalid extension configuration hash: " + path);
    }
    String payload = file.substring(firstBreak + 1);
    String actualHash = ZhangBoFatigueParameterCodec.sha256(
        payload.getBytes(StandardCharsets.UTF_8));
    if (!expectedHash.equals(actualHash)) {
      throw new IllegalArgumentException("Extension configuration hash mismatch: " + path);
    }
    Map<String, String> values = parseUnique(payload, path);
    exact(values, "schemaVersion", Integer.toString(ZhangBoInstanceExtension.SCHEMA_VERSION));
    String semanticTag = values.get("semanticTag");
    String sampler;
    String seedText;
    String distribution;
    if (ZhangBoInstanceExtension.SEMANTIC_TAG.equals(semanticTag)) {
      sampler = ZhangBoInstanceExtensionGenerator.SAMPLER_ID;
      seedText = Long.toString(ZhangBoInstanceExtension.SUT_SEED);
      distribution = ZhangBoInstanceExtension.DISTRIBUTION;
    } else if (ZhangBoInstanceExtension.BRIDGE_SEMANTIC_TAG.equals(semanticTag)) {
      sampler = ZhangBoInstanceExtension.BRIDGE_SAMPLER;
      seedText = "NOT_APPLICABLE";
      distribution = ZhangBoInstanceExtension.BRIDGE_DISTRIBUTION;
    } else {
      throw new IllegalArgumentException("Unsupported extension semanticTag: " + semanticTag);
    }
    exact(values, "sampler", sampler);
    exact(values, "sutSeed", seedText);
    exact(values, "distribution", distribution);
    exact(values, "instanceSha256", instanceSha256.toUpperCase());
    exact(values, "jobs", Integer.toString(jobs));
    exact(values, "stages", Integer.toString(stages));
    int[][] setup = new int[jobs][stages];
    int expectedFields = 8 + jobs * stages;
    for (int job = 0; job < jobs; job++) {
      for (int stage = 0; stage < stages; stage++) {
        String key = "sut." + job + "." + stage;
        String value = values.get(key);
        if (value == null) throw new IllegalArgumentException("Missing field: " + key);
        try {
          setup[job][stage] = Integer.parseInt(value);
        } catch (NumberFormatException exception) {
          throw new IllegalArgumentException("Invalid integer field " + key, exception);
        }
      }
    }
    if (values.size() != expectedFields) {
      throw new IllegalArgumentException("Unknown extension fields: expected="
          + expectedFields + ", actual=" + values.size());
    }
    ZhangBoInstanceExtension result = new ZhangBoInstanceExtension(instanceSha256, jobs, stages, setup, expectedHash,
        semanticTag, sampler, seedText, distribution);
    validatePathBinding(path, instanceSha256, jobs, stages, expectedHash);
    return result;
  }

  private static void validatePathBinding(
      Path path, String instanceSha256, int jobs, int stages,
      String configurationSha256) throws IOException {
    Path fileName = path.getFileName();
    if (fileName == null) return;
    Matcher matcher = FILE_NAME.matcher(fileName.toString());
    if (matcher.matches()) {
      if (Integer.parseInt(matcher.group(1)) != jobs
          || Integer.parseInt(matcher.group(2)) != stages
          || Integer.parseInt(matcher.group(3)) <= 0
          || Integer.parseInt(matcher.group(4)) <= 0) {
        throw new IllegalArgumentException("Extension filename dimensions do not match instance: " + path);
      }
    }
    Path parent = path.toAbsolutePath().normalize().getParent();
    if (parent == null) return;
    Path manifest = parent.resolve("MANIFEST.txt");
    if (!Files.exists(manifest)) return;
    String text = StrictUtf8.decode(Files.readAllBytes(manifest), manifest.toString());
    verifyManifest(text, manifest);
    boolean found = false;
    String target = fileName.toString();
    int firstBreak = text.indexOf('\n');
    for (String line : text.substring(firstBreak + 1).split("\\n", -1)) {
      if (line.isEmpty() || line.startsWith("schemaVersion=")
          || line.startsWith("semanticTag=") || line.startsWith("instances=")) continue;
      String[] fields = line.split("\\|", -1);
      if (fields.length == 4 && target.equals(fields[2])) {
        if (!instanceSha256.equalsIgnoreCase(fields[1])
            || !configurationSha256.equals(fields[3])) {
          throw new IllegalArgumentException("Extension manifest binding mismatch: " + path);
        }
        found = true;
      }
    }
    if (!found) throw new IllegalArgumentException("Extension file missing from manifest: " + path);
  }

  private static void verifyManifest(String text, Path path) {
    int firstBreak = text.indexOf('\n');
    if (firstBreak <= 0 || !text.startsWith("manifestSha256=")) {
      throw new IllegalArgumentException("Missing extension manifestSha256 header: " + path);
    }
    String expected = text.substring("manifestSha256=".length(), firstBreak);
    String payload = text.substring(firstBreak + 1);
    if (!expected.matches("[0-9A-F]{64}")
        || !expected.equals(ZhangBoFatigueParameterCodec.sha256(
            payload.getBytes(StandardCharsets.UTF_8)))) {
      throw new IllegalArgumentException("Extension manifest hash mismatch: " + path);
    }
    if (!payload.startsWith("schemaVersion=1\nsemanticTag=li_compatible_instance_extensions\n")) {
      throw new IllegalArgumentException("Unexpected extension manifest semantic tag: " + path);
    }
  }

  private static String payload(ZhangBoInstanceExtension extension) {
    StringBuilder out = new StringBuilder();
    out.append("schemaVersion=").append(ZhangBoInstanceExtension.SCHEMA_VERSION).append('\n')
        .append("semanticTag=").append(extension.getSemanticTag()).append('\n')
        .append("sampler=").append(extension.getSampler()).append('\n')
        .append("sutSeed=").append(extension.getSeedText()).append('\n')
        .append("distribution=").append(extension.getDistribution()).append('\n')
        .append("instanceSha256=").append(extension.getInstanceSha256()).append('\n')
        .append("jobs=").append(extension.getJobs()).append('\n')
        .append("stages=").append(extension.getStages()).append('\n');
    for (int job = 0; job < extension.getJobs(); job++) {
      for (int stage = 0; stage < extension.getStages(); stage++) {
        out.append("sut.").append(job).append('.').append(stage).append('=')
            .append(extension.getStandardSetupTime(job, stage)).append('\n');
      }
    }
    return out.toString();
  }

  private static Map<String, String> parseUnique(String payload, Path path) {
    Map<String, String> values = new LinkedHashMap<>();
    String[] lines = payload.split("\n", -1);
    for (int index = 0; index < lines.length; index++) {
      String line = lines[index];
      if (index == lines.length - 1 && line.isEmpty()) continue;
      int separator = line.indexOf('=');
      if (separator <= 0 || separator == line.length() - 1) {
        throw new IllegalArgumentException("Malformed extension line at " + path);
      }
      String key = line.substring(0, separator);
      if (values.put(key, line.substring(separator + 1)) != null) {
        throw new IllegalArgumentException("Duplicate field '" + key + "': " + path);
      }
    }
    return values;
  }

  private static void exact(Map<String, String> values, String key, String expected) {
    if (!expected.equals(values.get(key))) {
      throw new IllegalArgumentException("Field " + key + " must equal " + expected);
    }
  }
}
