package org.uma.jmetal.runner.lc_psode;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import org.uma.jmetal.algorithm.multiobjective.mypso.v35.V35FinalAblationProfile;
import org.uma.jmetal.algorithm.multiobjective.mypso.v35.V35ProductionConfiguration;

/** Emits the seed-bound A0--A4 profile hashes consumed by Master v2. */
public final class V35ProfileRegistryPrinter {
  private V35ProfileRegistryPrinter() { }
  public static void main(String[] args) throws Exception {
    if (args.length != 6 || !"--seeds".equals(args[0]) || !"--max-fes".equals(args[2])
        || !"--output".equals(args[4])) {
      throw new IllegalArgumentException("usage: --seeds <comma-list> --max-fes <n> --output <csv>");
    }
    int maxFes = Integer.parseInt(args[3]);
    StringBuilder out = new StringBuilder("arm,seed,population,maxFEs,profileSha256,runtimeConfigurationSha256\n");
    for (String token : args[1].split(",")) {
      long seed = Long.parseLong(token.trim());
      for (V35FinalAblationProfile.Arm arm : V35FinalAblationProfile.ARMS) {
        V35ProductionConfiguration configuration =
            V35FinalAblationProfile.configurationFor(arm, seed, 100, maxFes);
        V35FinalAblationProfile.validate(arm, configuration);
        out.append(arm.getLabel()).append(',').append(seed).append(",100,").append(maxFes)
            .append(',').append(V35FinalAblationProfile.configurationHashFor(arm, seed, 100, maxFes))
            .append(',').append(configuration.configurationHash()).append('\n');
      }
    }
    Path output = Paths.get(args[5]).toAbsolutePath().normalize();
    Files.createDirectories(output.getParent());
    Files.write(output, out.toString().getBytes(StandardCharsets.UTF_8));
  }
}
