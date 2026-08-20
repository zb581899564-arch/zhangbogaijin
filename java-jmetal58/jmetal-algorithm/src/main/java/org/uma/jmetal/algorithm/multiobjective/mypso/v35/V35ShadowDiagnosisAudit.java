package org.uma.jmetal.algorithm.multiobjective.mypso.v35;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** Audit-only counterfactual macro evaluations; no observation is fed back to search. */
public final class V35ShadowDiagnosisAudit implements Serializable {
  private static final long serialVersionUID = 1L;
  public static final String HEADER = "sample,generation,mainFE,parentSlot,factory,role,diagnosis,"
      + "maximumType,maximumPressure,secondType,secondPressure,pressureGap,"
      + "pSeq,pMac,pWor,pSet,pFat,action,allowed,cmax,tec,twc,qGain,accepted,"
      + "bestOverall,bestAllowed,regret";

  public static final class Outcome implements Serializable {
    private static final long serialVersionUID = 1L;
    private final V35MacroNeighborhood action;
    private final double cmax;
    private final double tec;
    private final double twc;
    private final double gain;
    private final boolean accepted;

    public Outcome(V35MacroNeighborhood action, double cmax, double tec, double twc,
        double gain, boolean accepted) {
      if (action == null || !Double.isFinite(cmax) || !Double.isFinite(tec)
          || !Double.isFinite(twc) || !Double.isFinite(gain)) {
        throw new IllegalArgumentException("invalid shadow outcome");
      }
      this.action = action;
      this.cmax = cmax;
      this.tec = tec;
      this.twc = twc;
      this.gain = gain;
      this.accepted = accepted;
    }
  }

  private final V35BottleneckDiagnosisConfiguration configuration;
  private final List<String> rows = new ArrayList<>();
  private long eligibleInvocations;
  private long samples;
  private int fullEvaluations;

  public V35ShadowDiagnosisAudit(V35BottleneckDiagnosisConfiguration configuration) {
    if (configuration == null) throw new IllegalArgumentException("configuration");
    this.configuration = configuration;
  }

  public boolean shouldSample(int applicableCandidates) {
    if (!configuration.isShadowAuditEnabled() || applicableCandidates <= 0) return false;
    eligibleInvocations++;
    if ((eligibleInvocations - 1L) % configuration.getShadowStride() != 0L) return false;
    return fullEvaluations + applicableCandidates <= configuration.getMaxShadowEvaluations();
  }

  public void record(long generation, long mainEvaluations, int parentSlot, int factory,
      V35SubSwarmRole role, V35PressureBottleneckClassifier.Classification classification,
      List<V35MacroNeighborhood> allowed, List<Outcome> outcomes) {
    if (role == null || classification == null || outcomes == null || outcomes.isEmpty()) {
      throw new IllegalArgumentException("incomplete shadow sample");
    }
    List<Outcome> stable = new ArrayList<>(outcomes);
    Collections.sort(stable, Comparator.comparingInt(value -> value.action.ordinal()));
    Set<V35MacroNeighborhood> mask = allowed == null
        ? Collections.<V35MacroNeighborhood>emptySet() : new HashSet<>(allowed);
    Outcome bestOverall = best(stable, null);
    Outcome bestAllowed = best(stable, mask);
    double overallGain = bestOverall == null ? 0.0 : bestOverall.gain;
    double allowedGain = bestAllowed == null ? 0.0 : bestAllowed.gain;
    double regret = Math.max(0.0, overallGain - allowedGain);
    long sample = ++samples;
    for (Outcome value : stable) {
      rows.add(sample + "," + generation + "," + mainEvaluations + "," + parentSlot + ","
          + factory + "," + role + "," + classification.getBottleneck() + ","
          + classification.getMaximumType() + "," + classification.getMaximumPressure() + ","
          + classification.getSecondType() + "," + classification.getSecondPressure() + ","
          + classification.getGap() + ","
          + classification.getPressure(V35Bottleneck.SEQ) + ","
          + classification.getPressure(V35Bottleneck.MAC) + ","
          + classification.getPressure(V35Bottleneck.WOR) + ","
          + classification.getPressure(V35Bottleneck.SET) + ","
          + classification.getPressure(V35Bottleneck.FAT) + "," + value.action + ","
          + mask.contains(value.action) + "," + value.cmax + "," + value.tec + ","
          + value.twc + "," + value.gain + "," + value.accepted + ","
          + (value == bestOverall) + "," + (value == bestAllowed) + "," + regret);
    }
    fullEvaluations += stable.size();
  }

  public long getEligibleInvocations() { return eligibleInvocations; }
  public long getSamples() { return samples; }
  public int getFullEvaluations() { return fullEvaluations; }
  public String toCsv() {
    return rows.isEmpty() ? HEADER + "\n" : HEADER + "\n" + String.join("\n", rows) + "\n";
  }

  private static Outcome best(List<Outcome> outcomes, Set<V35MacroNeighborhood> mask) {
    Outcome result = null;
    for (Outcome value : outcomes) {
      if (!value.accepted) continue;
      if (mask != null && !mask.contains(value.action)) continue;
      if (result == null || value.gain > result.gain + 1.0e-12
          || (Math.abs(value.gain - result.gain) <= 1.0e-12
              && value.action.ordinal() < result.action.ordinal())) result = value;
    }
    return result;
  }
}
