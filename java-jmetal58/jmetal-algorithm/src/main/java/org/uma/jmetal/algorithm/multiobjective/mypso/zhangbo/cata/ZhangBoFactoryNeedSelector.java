package org.uma.jmetal.algorithm.multiobjective.mypso.zhangbo.cata;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.uma.jmetal.algorithm.multiobjective.mypso.zhangbo.ZhangBoSubSwarm;
import org.uma.jmetal.algorithm.multiobjective.mypso.zhangbo.ZhangBoSubSwarmSemantics;
import org.uma.jmetal.problem.multiobjective.dfsp.fatigue.ZhangBoFatigueEvaluationResult;
import org.uma.jmetal.problem.multiobjective.dfsp.fatigue.ZhangBoFatigueOperationRecord;
import org.uma.jmetal.util.pseudorandom.PseudoRandomGenerator;

/** 80/20 factory selector based on the fixed seven-component Need vector. */
public final class ZhangBoFactoryNeedSelector {
  private static final double EPSILON = 1.0e-12;

  public static final class Need {
    private final int factory;
    private final double[] normalizedComponents;
    private final double weightedNeed;

    private Need(int factory, double[] normalizedComponents, double weightedNeed) {
      this.factory = factory;
      this.normalizedComponents = normalizedComponents.clone();
      this.weightedNeed = weightedNeed;
    }

    public int getFactory() { return factory; }
    /** C_f,E_f,WC_f,I^M_f,I^W_f,SUT_f,FRisk_f. */
    public double[] getNormalizedComponents() { return normalizedComponents.clone(); }
    public double getWeightedNeed() { return weightedNeed; }

    public String toCanonicalText() {
      StringBuilder out = new StringBuilder("factory=").append(factory)
          .append(",need=").append(weightedNeed).append(",components=");
      for (int i = 0; i < normalizedComponents.length; i++) {
        if (i > 0) out.append(',');
        out.append(normalizedComponents[i]);
      }
      return out.toString();
    }
  }

  public static final class Selection {
    private final boolean applicable;
    private final String reason;
    private final int factory;
    private final boolean exploratory;
    private final List<Need> needs;

    private Selection(boolean applicable, String reason, int factory,
        boolean exploratory, List<Need> needs) {
      this.applicable = applicable;
      this.reason = reason;
      this.factory = factory;
      this.exploratory = exploratory;
      this.needs = Collections.unmodifiableList(new ArrayList<>(needs));
    }

    public boolean isApplicable() { return applicable; }
    public String getReason() { return reason; }
    public int getFactory() { return factory; }
    public boolean isExploratory() { return exploratory; }
    public List<Need> getNeeds() { return needs; }
  }

  public List<Need> calculate(
      ZhangBoFatigueEvaluationResult evaluation, int factories, ZhangBoSubSwarm role) {
    if (evaluation == null || role == null) throw new IllegalArgumentException("evaluation and role");
    if (factories < 1) throw new IllegalArgumentException("factories must be positive");
    double[][] raw = new double[factories][7];
    boolean[] active = new boolean[factories];
    Map<String, Timeline> machines = new HashMap<>();
    Map<String, Timeline> workers = new HashMap<>();
    for (ZhangBoFatigueOperationRecord operation : evaluation.getOperations()) {
      if (operation.factory < 0 || operation.factory >= factories) continue;
      int factory = operation.factory;
      active[factory] = true;
      raw[factory][0] = Math.max(raw[factory][0], operation.end);
      raw[factory][1] += Math.max(0.0, operation.energy);
      raw[factory][2] += Math.max(0.0, operation.cost);
      raw[factory][5] += Math.max(0.0, operation.baseSetupDuration);
      raw[factory][6] += Math.max(0.0, operation.fatigueAfter - 0.80)
          * Math.max(0.0, operation.actualDuration);
      timeline(machines, factory + ":" + operation.stage + ":M:" + operation.machine)
          .add(operation);
      timeline(workers, factory + ":" + operation.stage + ":W:" + operation.worker)
          .add(operation);
    }
    for (Timeline timeline : machines.values()) raw[timeline.factory][3] += timeline.idle();
    for (Timeline timeline : workers.values()) raw[timeline.factory][4] += timeline.idle();

    List<Integer> activeFactories = new ArrayList<>();
    for (int factory = 0; factory < factories; factory++) if (active[factory]) activeFactories.add(factory);
    if (activeFactories.isEmpty()) return Collections.emptyList();

    double[][] normalized = new double[factories][7];
    for (int component = 0; component < 7; component++) {
      double minimum = Double.POSITIVE_INFINITY;
      double maximum = Double.NEGATIVE_INFINITY;
      for (int factory : activeFactories) {
        minimum = Math.min(minimum, raw[factory][component]);
        maximum = Math.max(maximum, raw[factory][component]);
      }
      for (int factory : activeFactories) {
        normalized[factory][component] = (raw[factory][component] - minimum)
            / (maximum - minimum + EPSILON);
      }
    }
    double[] weights = ZhangBoSubSwarmSemantics.needWeights(role);
    List<Need> result = new ArrayList<>();
    for (int factory : activeFactories) {
      double weighted = 0.0;
      for (int component = 0; component < weights.length; component++) {
        weighted += weights[component] * normalized[factory][component];
      }
      result.add(new Need(factory, normalized[factory], weighted));
    }
    Collections.sort(result, Comparator.comparingInt(Need::getFactory));
    return result;
  }

  public Selection select(
      ZhangBoFatigueEvaluationResult evaluation, int factories,
      ZhangBoSubSwarm role, double needWeightedProbability,
      PseudoRandomGenerator random) {
    if (random == null) throw new IllegalArgumentException("random");
    if (!Double.isFinite(needWeightedProbability)
        || needWeightedProbability < 0.0 || needWeightedProbability > 1.0) {
      throw new IllegalArgumentException("needWeightedProbability must be in [0,1]");
    }
    List<Need> needs = calculate(evaluation, factories, role);
    if (needs.isEmpty()) return new Selection(false, "NO_ACTIVE_FACTORY", -1, false, needs);
    boolean weighted = random.nextDouble() < needWeightedProbability;
    int selected = weighted ? weightedIndex(needs, random) : random.nextInt(0, needs.size() - 1);
    return new Selection(true, weighted ? "NEED_WEIGHTED" : "UNIFORM_EXPLORATION",
        needs.get(selected).getFactory(), !weighted, needs);
  }

  private static int weightedIndex(List<Need> values, PseudoRandomGenerator random) {
    double maximum = Double.NEGATIVE_INFINITY;
    for (Need value : values) maximum = Math.max(maximum, value.getWeightedNeed());
    double total = 0.0;
    double[] masses = new double[values.size()];
    for (int i = 0; i < values.size(); i++) {
      masses[i] = Math.exp(values.get(i).getWeightedNeed() - maximum);
      total += masses[i];
    }
    double draw = random.nextDouble() * total;
    double cumulative = 0.0;
    for (int i = 0; i < masses.length; i++) {
      cumulative += masses[i];
      if (draw < cumulative || i + 1 == masses.length) return i;
    }
    return masses.length - 1;
  }

  private static Timeline timeline(Map<String, Timeline> values, String key) {
    Timeline result = values.get(key);
    if (result == null) {
      String[] split = key.split(":");
      result = new Timeline(Integer.parseInt(split[0]));
      values.put(key, result);
    }
    return result;
  }

  private static final class Timeline {
    final int factory;
    double first = Double.POSITIVE_INFINITY;
    double last = Double.NEGATIVE_INFINITY;
    double occupied;

    Timeline(int factory) { this.factory = factory; }

    void add(ZhangBoFatigueOperationRecord operation) {
      first = Math.min(first, operation.start);
      last = Math.max(last, operation.end);
      occupied += Math.max(0.0, operation.actualDuration);
    }

    double idle() { return Math.max(0.0, last - first - occupied); }
  }
}
