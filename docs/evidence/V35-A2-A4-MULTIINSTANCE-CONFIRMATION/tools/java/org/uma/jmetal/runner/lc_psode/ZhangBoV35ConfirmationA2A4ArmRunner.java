package org.uma.jmetal.runner.lc_psode;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.TreeMap;

/**
 * Campaign wrapper for the pre-registered A2/A4 held-out confirmation.
 *
 * <p>It delegates all search work to the previously accepted snapshot-bound
 * Stage2 arm launcher. The only difference is evidence classification: the
 * executor's strict phase-budget gate is retained while the resulting output
 * is explicitly labelled {@code CONFIRMATION}, never formal statistics.</p>
 */
public final class ZhangBoV35ConfirmationA2A4ArmRunner {
  public static final String VERSION = "v35-a2-a4-confirmation-wrapper-v1";
  public static final String CAMPAIGN_PURPOSE = "CONFIRMATION";
  private static final String EXECUTOR_PURPOSE = "LAUNCHER_ACCEPTANCE";

  private ZhangBoV35ConfirmationA2A4ArmRunner() { }

  public static void main(String[] args) throws Exception {
    if (args.length != 4 || !"--plan".equals(args[0]) || !"--output".equals(args[2])) {
      throw new IllegalArgumentException("usage: --plan <properties> --output <directory>");
    }
    execute(Paths.get(args[1]), Paths.get(args[3]));
  }

  static void execute(Path confirmationPlan, Path output) throws Exception {
    Path source = confirmationPlan.toAbsolutePath().normalize();
    Properties original = load(source);
    require("schema", "v35-a2-a4-confirmation-run-plan-v1", original.getProperty("schema"));
    require("purpose", CAMPAIGN_PURPOSE, original.getProperty("purpose"));
    String arm = required(original, "arm");
    if (!"A2".equals(arm) && !"A4".equals(arm)) {
      throw new IllegalArgumentException("confirmation arm must be A2 or A4");
    }
    require("includedInFormalStatistics", "false", original.getProperty("includedInFormalStatistics"));
    require("includedInReferenceFront", "false", original.getProperty("includedInReferenceFront"));
    require("launcherAcceptanceOnly", "false", original.getProperty("launcherAcceptanceOnly"));
    if (Files.exists(output.toAbsolutePath().normalize())) {
      throw new IllegalStateException("refusing overwrite: " + output);
    }

    Properties executor = new Properties();
    executor.putAll(original);
    executor.setProperty("schema", ZhangBoV35FormalAblationArmRunner.PLAN_SCHEMA);
    executor.setProperty("purpose", EXECUTOR_PURPOSE);
    Path temporary = source.resolveSibling(".executor-" + source.getFileName() + '-' + System.nanoTime());
    try {
      store(executor, temporary);
      ZhangBoV35FormalAblationArmRunner.execute(temporary, output);
      Path result = output.toAbsolutePath().normalize();
      relabel(result, original, source);
      writeManifest(result);
      System.out.println("V35_A2_A4_CONFIRMATION_COMPLETED runId=" + required(original, "runId")
          + " arm=" + arm + " output=" + result);
    } finally {
      Files.deleteIfExists(temporary);
    }
  }

  private static void relabel(Path result, Properties plan, Path planPath) throws Exception {
    Path configuration = result.resolve("configuration.txt");
    if (!Files.isRegularFile(configuration)) throw new IllegalStateException("missing configuration.txt");
    String text = new String(Files.readAllBytes(configuration), StandardCharsets.UTF_8);
    String expected = "purpose=" + EXECUTOR_PURPOSE + '\n';
    if (!text.contains(expected)) throw new IllegalStateException("executor purpose not found");
    text = text.replaceFirst(java.util.regex.Pattern.quote(expected),
        "purpose=" + CAMPAIGN_PURPOSE + "\nexecutorGatePurpose=" + EXECUTOR_PURPOSE + "\n");
    text += "confirmationWrapperVersion=" + VERSION + "\n"
        + "confirmationProtocolVersion=v35-a2-a4-heldout-confirmation-v1\n"
        + "confirmationPlanSha256=" + sha256(planPath) + "\n";
    Files.write(configuration, text.getBytes(StandardCharsets.UTF_8));

    String context = "campaignPurpose=" + CAMPAIGN_PURPOSE + "\n"
        + "executorGatePurpose=" + EXECUTOR_PURPOSE + "\n"
        + "confirmationWrapperVersion=" + VERSION + "\n"
        + "confirmationProtocolVersion=v35-a2-a4-heldout-confirmation-v1\n"
        + "runId=" + required(plan, "runId") + "\n"
        + "preRegisteredRunKey=" + required(plan, "preRegisteredRunKey") + "\n"
        + "preRegisteredArmLabel=" + required(plan, "preRegisteredArmLabel") + "\n"
        + "instance=" + required(plan, "instance") + "\n"
        + "seed=" + required(plan, "seed") + "\n"
        + "arm=" + required(plan, "arm") + "\n"
        + "includedInFormalStatistics=false\n"
        + "includedInReferenceFront=false\n"
        + "planSha256=" + sha256(planPath) + "\n";
    Files.write(result.resolve("confirmation-context.properties"), context.getBytes(StandardCharsets.UTF_8));
  }

  private static Properties load(Path path) throws Exception {
    Properties values = new Properties();
    try (InputStream input = Files.newInputStream(path)) {
      values.load(new InputStreamReader(input, StandardCharsets.UTF_8));
    }
    return values;
  }

  private static void store(Properties values, Path path) throws Exception {
    List<String> keys = new ArrayList<String>();
    for (Object key : values.keySet()) keys.add((String) key);
    java.util.Collections.sort(keys);
    StringBuilder text = new StringBuilder();
    for (String key : keys) text.append(key).append('=').append(values.getProperty(key)).append('\n');
    Files.write(path, text.toString().getBytes(StandardCharsets.UTF_8));
  }

  private static void writeManifest(Path root) throws Exception {
    Files.deleteIfExists(root.resolve("evidence-sha256.tsv"));
    TreeMap<String, String> rows = new TreeMap<String, String>();
    try (java.util.stream.Stream<Path> walk = Files.walk(root)) {
      java.util.Iterator<Path> iterator = walk.filter(Files::isRegularFile).iterator();
      while (iterator.hasNext()) {
        Path item = iterator.next();
        rows.put(root.relativize(item).toString().replace('\\', '/'), sha256(item));
      }
    }
    StringBuilder text = new StringBuilder("path\tsha256\n");
    for (Map.Entry<String, String> row : rows.entrySet()) {
      text.append(row.getKey()).append('\t').append(row.getValue()).append('\n');
    }
    Files.write(root.resolve("evidence-sha256.tsv"), text.toString().getBytes(StandardCharsets.UTF_8));
  }

  private static String sha256(Path path) throws Exception {
    return sha256(Files.readAllBytes(path));
  }

  private static String sha256(byte[] bytes) throws Exception {
    byte[] digest = MessageDigest.getInstance("SHA-256").digest(bytes);
    StringBuilder result = new StringBuilder();
    for (byte value : digest) result.append(String.format("%02x", value & 0xff));
    return result.toString();
  }

  private static String required(Properties values, String key) {
    String value = values.getProperty(key);
    if (value == null || value.trim().isEmpty()) throw new IllegalArgumentException("missing " + key);
    return value.trim();
  }

  private static void require(String key, String expected, String actual) {
    if (!expected.equals(actual)) throw new IllegalArgumentException(key + " expected=" + expected + " actual=" + actual);
  }
}
