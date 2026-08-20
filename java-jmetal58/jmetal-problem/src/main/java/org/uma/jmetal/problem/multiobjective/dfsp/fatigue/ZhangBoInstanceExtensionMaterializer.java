package org.uma.jmetal.problem.multiobjective.dfsp.fatigue;

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

/** One-shot materializer for the 45 immutable EADHFSP setup-time extensions. */
public final class ZhangBoInstanceExtensionMaterializer {
  private static final Pattern NAME = Pattern.compile("(\\d+)_(\\d+)_(\\d+)_(\\d+)\\.txt");

  private ZhangBoInstanceExtensionMaterializer() { }

  public static void main(String[] args) throws Exception {
    Path data = args.length > 0 ? Paths.get(args[0]) : Paths.get("EADHFSP");
    Path output = args.length > 1 ? Paths.get(args[1])
        : ZhangBoInstanceExtensionCodec.configuredDirectory();
    List<Path> instances = list(data);
    if (instances.size() != 45) {
      throw new IllegalStateException("Expected 45 EADHFSP instances, found " + instances.size());
    }
    Files.createDirectories(output);
    StringBuilder manifest = new StringBuilder(
        "schemaVersion=1\nsemanticTag=li_compatible_instance_extensions\ninstances=45\n");
    for (Path path : instances) {
      Matcher matcher = NAME.matcher(path.getFileName().toString());
      matcher.matches();
      int jobs = Integer.parseInt(matcher.group(1));
      int stages = Integer.parseInt(matcher.group(2));
      int factories = Integer.parseInt(matcher.group(3));
      int problemId = Integer.parseInt(matcher.group(4));
      String instanceSha = ZhangBoFatigueParameterCodec.sha256(Files.readAllBytes(path));
      ZhangBoInstanceExtension generated = ZhangBoInstanceExtensionGenerator.generate(
          instanceSha, jobs, stages);
      Path target = ZhangBoInstanceExtensionCodec.fileFor(
          output, jobs, stages, factories, problemId);
      ZhangBoInstanceExtension persisted = ZhangBoInstanceExtensionCodec.write(target, generated);
      ZhangBoInstanceExtension loaded = ZhangBoInstanceExtensionCodec.read(
          target, instanceSha, jobs, stages);
      if (!persisted.getConfigurationSha256().equals(loaded.getConfigurationSha256())) {
        throw new IllegalStateException("Extension round-trip failed: " + target);
      }
      manifest.append(path.getFileName()).append('|').append(instanceSha).append('|')
          .append(target.getFileName()).append('|')
          .append(loaded.getConfigurationSha256()).append('\n');
    }
    byte[] payload = manifest.toString().getBytes(StandardCharsets.UTF_8);
    String text = "manifestSha256=" + ZhangBoFatigueParameterCodec.sha256(payload)
        + "\n" + manifest;
    Files.write(output.resolve("MANIFEST.txt"), text.getBytes(StandardCharsets.UTF_8));
    System.out.println("Materialized 45 setup-time extensions at "
        + output.toAbsolutePath().normalize());
  }

  private static List<Path> list(Path directory) throws Exception {
    List<Path> result = new ArrayList<>();
    try (DirectoryStream<Path> stream = Files.newDirectoryStream(directory, "*.txt")) {
      for (Path path : stream) if (NAME.matcher(path.getFileName().toString()).matches()) {
        result.add(path);
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
