package org.uma.jmetal.problem.multiobjective.dfsp.fatigue;

import org.uma.jmetal.problem.multiobjective.dfsp.ZhangBoEDHHFSPW;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** One-shot CLI which materializes the fixed v1 parameter files for EADHFSP instances. */
public final class ZhangBoFatigueParameterMaterializer {
  private static final Pattern INSTANCE_NAME = Pattern.compile("(\\d+)_(\\d+)_(\\d+)_(\\d+)\\.txt");

  private ZhangBoFatigueParameterMaterializer() { }

  public static void main(String[] args) throws Exception {
    Path dataDirectory = args.length >= 1 ? Paths.get(args[0]) : Paths.get("EADHFSP");
    Path outputDirectory = args.length >= 2 ? Paths.get(args[1])
        : ZhangBoFatigueParameterCodec.configuredDirectory();
    List<Path> instances = listInstances(dataDirectory);
    if (instances.size() != 45) {
      throw new IllegalStateException("Expected 45 valid EADHFSP instances, found " + instances.size());
    }
    Files.createDirectories(outputDirectory);
    String previousDataDirectory = System.getProperty("dhfsp.data.dir");
    System.setProperty("dhfsp.data.dir", dataDirectory.toAbsolutePath().normalize().toString());
    StringBuilder manifest = new StringBuilder(
        "schemaVersion=1\nsemanticTag=standardized_fatigue_scenario\ninstances=45\n");
    try {
      for (Path instancePath : instances) {
        Matcher matcher = INSTANCE_NAME.matcher(instancePath.getFileName().toString());
        if (!matcher.matches()) throw new IllegalStateException("Invalid instance filename: " + instancePath);
        int jobs = Integer.parseInt(matcher.group(1));
        int stages = Integer.parseInt(matcher.group(2));
        int factories = Integer.parseInt(matcher.group(3));
        int problemId = Integer.parseInt(matcher.group(4));
        ZhangBoEDHHFSPW problem = new ZhangBoEDHHFSPW(jobs, stages, factories, problemId);
        ZhangBoFatigueInstanceData instance = problem.getFatigueInstanceData();
        ZhangBoFatigueParameters generated = ZhangBoFatigueParameterGenerator.generate(instance);
        Path target = ZhangBoFatigueParameterCodec.fileFor(
            outputDirectory, jobs, stages, factories, problemId);
        ZhangBoFatigueParameters persisted = ZhangBoFatigueParameterCodec.write(target, instance, generated);
        ZhangBoFatigueParameters loaded = ZhangBoFatigueParameterCodec.read(target, instance);
        if (!persisted.getConfigurationSha256().equals(loaded.getConfigurationSha256())) {
          throw new IllegalStateException("Round-trip configuration hash mismatch: " + target);
        }
        manifest.append(instancePath.getFileName()).append('|')
            .append(instance.getInstanceSha256()).append('|')
            .append(target.getFileName()).append('|')
            .append(loaded.getConfigurationSha256()).append('\n');
      }
    } finally {
      if (previousDataDirectory == null) System.clearProperty("dhfsp.data.dir");
      else System.setProperty("dhfsp.data.dir", previousDataDirectory);
    }
    byte[] manifestPayload = manifest.toString().getBytes(StandardCharsets.UTF_8);
    String output = "manifestSha256=" + ZhangBoFatigueParameterCodec.sha256(manifestPayload)
        + "\n" + manifest;
    Files.write(outputDirectory.resolve("MANIFEST.txt"), output.getBytes(StandardCharsets.UTF_8));
    System.out.println("Materialized 45 fatigue parameter files at "
        + outputDirectory.toAbsolutePath().normalize());
  }

  private static List<Path> listInstances(Path directory) throws IOException {
    List<Path> result = new ArrayList<>();
    try (DirectoryStream<Path> stream = Files.newDirectoryStream(directory, "*.txt")) {
      for (Path path : stream) {
        if (INSTANCE_NAME.matcher(path.getFileName().toString()).matches()) result.add(path);
      }
    }
    Collections.sort(result, new Comparator<Path>() {
      @Override public int compare(Path left, Path right) {
        return left.getFileName().toString().compareTo(right.getFileName().toString());
      }
    });
    return result;
  }
}
