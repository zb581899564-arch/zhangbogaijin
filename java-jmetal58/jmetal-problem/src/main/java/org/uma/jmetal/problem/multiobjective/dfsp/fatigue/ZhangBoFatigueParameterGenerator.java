package org.uma.jmetal.problem.multiobjective.dfsp.fatigue;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/** Order-independent keyed generator for the standardized fatigue scenario. */
public final class ZhangBoFatigueParameterGenerator {
  public static final double LAMBDA_MIN = 0.01;
  public static final double LAMBDA_MAX = 0.03;
  public static final double MU_MIN = 0.03;
  public static final double MU_MAX = 0.07;
  public static final double DEFAULT_MAXIMUM_INCREASE = 0.30;
  public static final double DEFAULT_WARNING = 0.80;
  public static final double DEFAULT_SAFE = 0.90;
  public static final String SAMPLER_ID = "sha256-keyed-u53-v1";

  private ZhangBoFatigueParameterGenerator() { }

  public static ZhangBoFatigueParameters generate(ZhangBoFatigueInstanceData instance) {
    int factories = instance.getFactories();
    int stages = instance.getStages();
    double[][][] lambda = new double[factories][stages][];
    double[][][] mu = new double[factories][stages][];
    for (int f = 0; f < factories; f++) {
      for (int k = 0; k < stages; k++) {
        int workers = instance.getWorkerCount(f);
        lambda[f][k] = new double[workers];
        mu[f][k] = new double[workers];
        for (int w = 0; w < workers; w++) {
          lambda[f][k][w] = scale(keyedUnit(instance.getInstanceSha256(), "lambda", f, w, k), LAMBDA_MIN, LAMBDA_MAX);
          mu[f][k][w] = scale(keyedUnit(instance.getInstanceSha256(), "mu", f, w, k), MU_MIN, MU_MAX);
        }
      }
    }
    double[] maximumIncrease = new double[stages];
    for (int k = 0; k < stages; k++) maximumIncrease[k] = DEFAULT_MAXIMUM_INCREASE;
    return new ZhangBoFatigueParameters(instance.getInstanceSha256(), lambda, mu,
        maximumIncrease, DEFAULT_WARNING, DEFAULT_SAFE, "");
  }

  static double keyedUnit(String instanceSha256, String kind, int factory, int worker, int stage) {
    String key = "zhangbo-fatigue-v1|" + ZhangBoFatigueParameters.SAMPLER_SEED + "|"
        + instanceSha256.toUpperCase() + "|" + kind + "|" + factory + "|" + worker + "|" + stage;
    try {
      byte[] digest = MessageDigest.getInstance("SHA-256").digest(key.getBytes(StandardCharsets.UTF_8));
      long bits = 0L;
      for (int index = 0; index < 8; index++) bits = (bits << 8) | (digest[index] & 0xffL);
      long mantissa = bits >>> 11;
      return mantissa / 9007199254740992.0;
    } catch (NoSuchAlgorithmException exception) {
      throw new IllegalStateException("SHA-256 is unavailable", exception);
    }
  }

  private static double scale(double unit, double lower, double upper) {
    return lower + (upper - lower) * unit;
  }
}
