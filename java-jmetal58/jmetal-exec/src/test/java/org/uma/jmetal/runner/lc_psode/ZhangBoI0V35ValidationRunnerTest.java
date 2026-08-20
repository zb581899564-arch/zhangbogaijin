package org.uma.jmetal.runner.lc_psode;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class ZhangBoI0V35ValidationRunnerTest {
  @Test
  public void runsOneFormalFm3DecodeWithoutShift() throws Exception {
    Path output = Files.createTempDirectory("i0-v35-java-");
    ZhangBoI0V35ValidationRunner.run(output);
    List<String> trace = Files.readAllLines(output.resolve("java_trace.csv"),
        StandardCharsets.UTF_8);
    String manifest = new String(Files.readAllBytes(output.resolve("manifest.properties")),
        StandardCharsets.UTF_8);
    assertEquals(11, trace.size());
    assertTrue(manifest.contains("decoderMode=FM3"));
    assertTrue(manifest.contains("setupMode=SEQUENCE_INDEPENDENT"));
    assertTrue(manifest.contains("shiftMode=NONE"));
    assertTrue(manifest.contains("evaluations=1"));
  }
}
