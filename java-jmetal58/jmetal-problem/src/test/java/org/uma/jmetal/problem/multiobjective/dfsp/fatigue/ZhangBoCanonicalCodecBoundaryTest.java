package org.uma.jmetal.problem.multiobjective.dfsp.fatigue;

import org.junit.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.Assert.assertTrue;

/** Strict UTF-8, finite, negative-zero and per-value range gates. */
public class ZhangBoCanonicalCodecBoundaryTest {
  @Test
  public void malformedUtf8IsRejectedBeforeParsing() throws Exception {
    ZhangBoFatigueInstanceData instance = instance();
    Path file = Files.createTempFile("p8-utf8-", ".fatigue.txt");
    try {
      Files.write(file, new byte[] {(byte) 0xC3, (byte) 0x28});
      expectFailure(file, instance, "Malformed UTF-8");
    } finally {
      Files.deleteIfExists(file);
    }
  }

  @Test
  public void negativeZeroAndOutOfRangeValuesAreRejected() throws Exception {
    ZhangBoFatigueInstanceData instance = instance();
    ZhangBoFatigueParameters parameters = ZhangBoFatigueParameterGenerator.generate(instance);
    Path file = Files.createTempFile("p8-range-", ".fatigue.txt");
    try {
      ZhangBoFatigueParameterCodec.write(file, instance, parameters);
      String valid = new String(Files.readAllBytes(file), StandardCharsets.UTF_8);
      String payload = valid.substring(valid.indexOf('\n') + 1)
          .replaceFirst("p\\.0\\.0\\.0\\.lambda=[^\\n]+", "p.0.0.0.lambda=-0.0");
      Files.write(file, withHash(payload).getBytes(StandardCharsets.UTF_8));
      expectFailure(file, instance, "Negative zero");

      valid = new String(Files.readAllBytes(file), StandardCharsets.UTF_8);
      payload = valid.substring(valid.indexOf('\n') + 1)
          .replaceFirst("p\\.0\\.0\\.0\\.lambda=[^\\n]+", "p.0.0.0.lambda=0.5");
      Files.write(file, withHash(payload).getBytes(StandardCharsets.UTF_8));
      expectFailure(file, instance, "outside allowed range");
    } finally {
      Files.deleteIfExists(file);
    }
  }

  private static void expectFailure(Path file, ZhangBoFatigueInstanceData instance,
                                    String expected) throws Exception {
    try {
      ZhangBoFatigueParameterCodec.read(file, instance);
    } catch (IllegalArgumentException exception) {
      assertTrue(exception.getMessage(), exception.getMessage().contains(expected));
      return;
    }
    throw new AssertionError("Expected failure containing " + expected);
  }

  private static String withHash(String payload) {
    return "configurationSha256="
        + ZhangBoFatigueParameterCodec.sha256(payload.getBytes(StandardCharsets.UTF_8))
        + "\n" + payload;
  }

  private static ZhangBoFatigueInstanceData instance() {
    return new ZhangBoFatigueInstanceData(repeat('A', 64), 1, 1, 1,
        new int[][] {{1}}, new double[][][] {{{1.0}}}, new int[][][] {{{1}}},
        new int[][] {{1}}, new int[] {1}, new double[][] {{1.0}}, new int[][] {{1}});
  }

  private static String repeat(char value, int count) {
    StringBuilder out = new StringBuilder(count);
    for (int i = 0; i < count; i++) out.append(value);
    return out.toString();
  }
}
