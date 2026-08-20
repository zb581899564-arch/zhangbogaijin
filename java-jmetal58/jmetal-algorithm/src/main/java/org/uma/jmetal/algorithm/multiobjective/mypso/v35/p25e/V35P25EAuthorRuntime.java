package org.uma.jmetal.algorithm.multiobjective.mypso.v35.p25e;

import java.util.Random;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.atomic.AtomicLong;
import org.uma.jmetal.problem.multiobjective.dfsp.fatigue.ZhangBoFatigueInstanceData;

/**
 * Compatibility boundary for the isolated paper-author algorithm copies.
 * It changes only random-source and instance-domain access, both explicitly
 * permitted by the P25E adaptation whitelist.
 */
public final class V35P25EAuthorRuntime {
  private static final ThreadLocal<Session> CURRENT = new ThreadLocal<>();

  private V35P25EAuthorRuntime() { }

  public static void install(long seed, ZhangBoFatigueInstanceData instance) {
    if (instance == null) throw new IllegalArgumentException("instance");
    if (CURRENT.get() != null) throw new IllegalStateException("author runtime already installed");
    CURRENT.set(new Session(seed, instance));
  }

  public static void clear() { CURRENT.remove(); }

  public static Random newRandom() {
    Session session = require();
    long ordinal = session.randomOrdinal.getAndIncrement();
    return new Random(mix(session.seed + 0x9E3779B97F4A7C15L * ordinal));
  }

  public static int[][][] workerDomain() {
    int[][][] source = require().workerDomain;
    int[][][] copy = new int[source.length][][];
    for (int factory = 0; factory < source.length; factory++) {
      copy[factory] = new int[source[factory].length][];
      for (int stage = 0; stage < source[factory].length; stage++) {
        copy[factory][stage] = source[factory][stage].clone();
      }
    }
    return copy;
  }

  public static int[] workerCounts() { return require().workerCounts.clone(); }

  public static int[][] machineCounts() {
    int[][] source = require().machineCounts;
    int[][] copy = new int[source.length][];
    for (int index = 0; index < source.length; index++) copy[index] = source[index].clone();
    return copy;
  }

  public static double[][] workerEfficiencies() {
    double[][] source = require().workerEfficiencies;
    double[][] copy = new double[source.length][];
    for (int index = 0; index < source.length; index++) copy[index] = source[index].clone();
    return copy;
  }

  public static long randomObjectsCreated() { return require().randomOrdinal.get(); }
  public static void log(Object ignored) { require().suppressedConsoleLines.incrementAndGet(); }
  public static long suppressedConsoleLines() { return require().suppressedConsoleLines.get(); }

  public static void recordEvent(String event) {
    if (event == null || event.trim().isEmpty()) throw new IllegalArgumentException("event");
    Session session = require();
    Long old = session.events.get(event);
    session.events.put(event, old == null ? 1L : old + 1L);
  }

  public static long eventCount(String event) {
    Long value = require().events.get(event);
    return value == null ? 0L : value;
  }

  public static String eventSummary() {
    StringBuilder out = new StringBuilder();
    for (Map.Entry<String, Long> entry : require().events.entrySet()) {
      if (out.length() > 0) out.append('|');
      out.append(entry.getKey()).append('=').append(entry.getValue());
    }
    return out.toString();
  }

  private static Session require() {
    Session value = CURRENT.get();
    if (value == null) throw new IllegalStateException("P25E author runtime is not installed");
    return value;
  }

  private static long mix(long value) {
    value ^= value >>> 30; value *= 0xBF58476D1CE4E5B9L;
    value ^= value >>> 27; value *= 0x94D049BB133111EBL;
    return value ^ (value >>> 31);
  }

  private static final class Session {
    private final long seed;
    private final AtomicLong randomOrdinal = new AtomicLong();
    private final AtomicLong suppressedConsoleLines = new AtomicLong();
    private final Map<String, Long> events = new TreeMap<>();
    private final int[][][] workerDomain;
    private final int[] workerCounts;
    private final int[][] machineCounts;
    private final double[][] workerEfficiencies;

    private Session(long seed, ZhangBoFatigueInstanceData instance) {
      this.seed = seed;
      workerCounts = instance.getWorkerCounts();
      machineCounts = instance.getMachineCounts();
      workerEfficiencies = new double[instance.getFactories()][];
      workerDomain = new int[instance.getFactories()][instance.getStages()][];
      for (int factory = 0; factory < instance.getFactories(); factory++) {
        workerEfficiencies[factory] = new double[instance.getWorkerCount(factory)];
        for (int worker = 0; worker < workerEfficiencies[factory].length; worker++) {
          workerEfficiencies[factory][worker] = instance.getWorkerEfficiency(factory, worker);
        }
        for (int stage = 0; stage < instance.getStages(); stage++) {
          workerDomain[factory][stage] = instance.getEligibleWorkers(factory, stage);
        }
      }
    }
  }
}
