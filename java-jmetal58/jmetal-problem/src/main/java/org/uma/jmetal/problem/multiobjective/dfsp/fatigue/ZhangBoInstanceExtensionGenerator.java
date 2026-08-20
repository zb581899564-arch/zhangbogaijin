package org.uma.jmetal.problem.multiobjective.dfsp.fatigue;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/** Traversal-order-independent SHA-256 sampler for fixed setup times. */
public final class ZhangBoInstanceExtensionGenerator {
  public static final String SAMPLER_ID = "KEYED_SHA256_REJECTION_V1";

  private ZhangBoInstanceExtensionGenerator() { }

  public static ZhangBoInstanceExtension generate(String instanceSha256, int jobs, int stages) {
    int[][] setup = new int[jobs][stages];
    for (int job = 0; job < jobs; job++) {
      for (int stage = 0; stage < stages; stage++) {
        setup[job][stage] = sample(instanceSha256, job, stage);
      }
    }
    return new ZhangBoInstanceExtension(instanceSha256, jobs, stages, setup, "");
  }

  private static int sample(String instanceSha256, int job, int stage) {
    String key = instanceSha256.toUpperCase() + "|" + ZhangBoInstanceExtension.SUT_SEED
        + "|SUT|" + job + "|" + stage;
    int counter = 0;
    while (true) {
      byte[] digest = sha256((key + "|" + counter).getBytes(StandardCharsets.UTF_8));
      long unsigned = ByteBuffer.wrap(digest).getLong() & Long.MAX_VALUE;
      long limit = Long.MAX_VALUE - (Long.MAX_VALUE % 9L);
      if (unsigned < limit) return (int) (unsigned % 9L) + 1;
      counter++;
    }
  }

  private static byte[] sha256(byte[] bytes) {
    try {
      return MessageDigest.getInstance("SHA-256").digest(bytes);
    } catch (NoSuchAlgorithmException exception) {
      throw new IllegalStateException("SHA-256 is unavailable", exception);
    }
  }
}
