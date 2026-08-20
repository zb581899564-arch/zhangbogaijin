package org.uma.jmetal.algorithm.multiobjective.mypso.v35;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Recomputable Q50--Q90 confidence-threshold selection from shadow-probe CSV. */
public final class V35DiagnosisThresholdSelector implements Serializable {
  private static final long serialVersionUID = 1L;
  private static final double EPSILON = 1.0e-12;
  private static final double MIN_COVERAGE = 0.10;
  private static final double MAX_MISSED_POSITIVE = 0.05;
  private static final double[] QUANTILES = {0.50, 0.60, 0.70, 0.80, 0.90};

  public static final class Candidate implements Serializable {
    private static final long serialVersionUID = 1L;
    private final double tauAbs;
    private final double tauGap;
    private final double coverage;
    private final double missedPositiveBestRate;
    private final double meanRegret;
    private final double p95Regret;
    private final int samples;
    private final int positiveSamples;
    private final boolean feasible;

    private Candidate(double tauAbs, double tauGap, double coverage,
        double missedPositiveBestRate, double meanRegret, double p95Regret,
        int samples, int positiveSamples) {
      this.tauAbs = tauAbs;
      this.tauGap = tauGap;
      this.coverage = coverage;
      this.missedPositiveBestRate = missedPositiveBestRate;
      this.meanRegret = meanRegret;
      this.p95Regret = p95Regret;
      this.samples = samples;
      this.positiveSamples = positiveSamples;
      this.feasible = coverage + EPSILON >= MIN_COVERAGE
          && missedPositiveBestRate <= MAX_MISSED_POSITIVE + EPSILON;
    }

    public double getTauAbs() { return tauAbs; }
    public double getTauGap() { return tauGap; }
    public double getCoverage() { return coverage; }
    public double getMissedPositiveBestRate() { return missedPositiveBestRate; }
    public double getMeanRegret() { return meanRegret; }
    public double getP95Regret() { return p95Regret; }
    public int getSamples() { return samples; }
    public int getPositiveSamples() { return positiveSamples; }
    public boolean isFeasible() { return feasible; }

    public String csvRow() {
      return tauAbs + "," + tauGap + "," + coverage + ","
          + missedPositiveBestRate + "," + meanRegret + "," + p95Regret + ","
          + samples + "," + positiveSamples + "," + feasible;
    }
  }

  public static final class Selection implements Serializable {
    private static final long serialVersionUID = 1L;
    private final List<Candidate> candidates;
    private final Candidate selected;

    private Selection(List<Candidate> candidates, Candidate selected) {
      this.candidates = Collections.unmodifiableList(new ArrayList<>(candidates));
      this.selected = selected;
    }

    public List<Candidate> getCandidates() { return candidates; }
    public Candidate getSelected() { return selected; }
    public boolean hasFrozenThresholds() { return selected != null; }
    public String candidatesCsv() {
      StringBuilder out = new StringBuilder(
          "tauAbs,tauGap,strictCoverage,missedPositiveBestRate,meanRegret,p95Regret,"
              + "samples,positiveSamples,feasible\n");
      for (Candidate value : candidates) out.append(value.csvRow()).append('\n');
      return out.toString();
    }
    public String selectionCsv() {
      String header = "status,tauAbs,tauGap,strictCoverage,missedPositiveBestRate,"
          + "meanRegret,p95Regret,samples,positiveSamples\n";
      return selected == null ? header + "NO_FEASIBLE_THRESHOLD,,,,,,,,\n"
          : header + "SELECTED," + selected.tauAbs + "," + selected.tauGap + ","
              + selected.coverage + "," + selected.missedPositiveBestRate + ","
              + selected.meanRegret + "," + selected.p95Regret + ","
              + selected.samples + "," + selected.positiveSamples + "\n";
    }
  }

  public Selection select(String shadowCsv) {
    List<Sample> samples = parse(shadowCsv);
    if (samples.isEmpty()) return new Selection(Collections.<Candidate>emptyList(), null);
    List<Double> maxima = new ArrayList<>();
    List<Double> gaps = new ArrayList<>();
    for (Sample sample : samples) {
      maxima.add(sample.maximum);
      gaps.add(sample.maximum - sample.second);
    }
    List<Candidate> candidates = new ArrayList<>();
    for (double maximumQuantile : QUANTILES) {
      double tauAbs = quantile(maxima, maximumQuantile);
      for (double gapQuantile : QUANTILES) {
        candidates.add(evaluate(samples, tauAbs, quantile(gaps, gapQuantile)));
      }
    }
    Candidate selected = null;
    for (Candidate value : candidates) {
      if (!value.isFeasible()) continue;
      if (selected == null || better(value, selected)) selected = value;
    }
    return new Selection(candidates, selected);
  }

  public Candidate validate(String shadowCsv, double tauAbs, double tauGap) {
    List<Sample> samples = parse(shadowCsv);
    return evaluate(samples, tauAbs, tauGap);
  }

  private static boolean better(Candidate left, Candidate right) {
    int value = Double.compare(left.coverage, right.coverage);
    if (value != 0) return value > 0;
    value = Double.compare(left.p95Regret, right.p95Regret);
    if (value != 0) return value < 0;
    value = Double.compare(left.meanRegret, right.meanRegret);
    if (value != 0) return value < 0;
    value = Double.compare(left.tauAbs, right.tauAbs);
    if (value != 0) return value > 0;
    return left.tauGap > right.tauGap;
  }

  private static Candidate evaluate(List<Sample> samples, double tauAbs, double tauGap) {
    int strict = 0;
    int positive = 0;
    int missed = 0;
    double totalRegret = 0.0;
    List<Double> regrets = new ArrayList<>();
    for (Sample sample : samples) {
      boolean confident = sample.maximum + EPSILON >= tauAbs
          && sample.maximum - sample.second + EPSILON >= tauGap;
      if (confident) strict++;
      double bestOverall = sample.best(null);
      if (bestOverall > EPSILON) positive++;
      V35CaTaContext context = new V35CaTaContext(sample.role,
          confident ? sample.maximumType : V35Bottleneck.BAL);
      double bestAllowed = sample.best(context);
      double regret = Math.max(0.0, bestOverall - bestAllowed);
      if (bestOverall > EPSILON && regret > EPSILON) missed++;
      totalRegret += regret;
      regrets.add(regret);
    }
    double coverage = samples.isEmpty() ? 0.0 : (double) strict / samples.size();
    double missedRate = positive == 0 ? 0.0 : (double) missed / positive;
    double mean = samples.isEmpty() ? 0.0 : totalRegret / samples.size();
    double p95 = quantile(regrets, 0.95);
    return new Candidate(tauAbs, tauGap, coverage, missedRate, mean, p95,
        samples.size(), positive);
  }

  private static List<Sample> parse(String csv) {
    if (csv == null || csv.trim().isEmpty()) return Collections.emptyList();
    Map<Long, Sample> grouped = new LinkedHashMap<>();
    String[] lines = csv.split("\\r?\\n");
    for (int index = 1; index < lines.length; index++) {
      if (lines[index].trim().isEmpty()) continue;
      String[] cell = lines[index].split(",", -1);
      if (cell.length != 27) throw new IllegalArgumentException(
          "shadow-probes.csv row must contain 27 fields, got " + cell.length);
      long id = Long.parseLong(cell[0]);
      Sample sample = grouped.get(id);
      V35SubSwarmRole role = V35SubSwarmRole.valueOf(cell[5]);
      V35Bottleneck maximumType = V35Bottleneck.valueOf(cell[7]);
      // A fail-closed classifier has no observable pressure label; it must not
      // participate in empirical threshold calibration.
      if (maximumType == V35Bottleneck.BAL) continue;
      double maximum = finite(cell[8]);
      double second = finite(cell[10]);
      if (sample == null) {
        sample = new Sample(role, maximumType, maximum, second);
        grouped.put(id, sample);
      } else if (sample.role != role || sample.maximumType != maximumType
          || Math.abs(sample.maximum - maximum) > EPSILON
          || Math.abs(sample.second - second) > EPSILON) {
        throw new IllegalArgumentException("inconsistent shadow sample " + id);
      }
      V35MacroNeighborhood action = V35MacroNeighborhood.valueOf(cell[17]);
      boolean accepted = Boolean.parseBoolean(cell[23]);
      double gain = finite(cell[22]);
      sample.gains.put(action, accepted ? Math.max(0.0, gain) : 0.0);
    }
    return new ArrayList<>(grouped.values());
  }

  private static double finite(String value) {
    double result = Double.parseDouble(value);
    if (!Double.isFinite(result)) throw new IllegalArgumentException("non-finite CSV value");
    return result;
  }

  private static double quantile(List<Double> values, double probability) {
    if (values.isEmpty()) return 0.0;
    List<Double> sorted = new ArrayList<>(values);
    Collections.sort(sorted);
    int index = (int) Math.ceil(probability * sorted.size()) - 1;
    return sorted.get(Math.max(0, Math.min(index, sorted.size() - 1)));
  }

  private static final class Sample {
    final V35SubSwarmRole role;
    final V35Bottleneck maximumType;
    final double maximum;
    final double second;
    final Map<V35MacroNeighborhood, Double> gains =
        new EnumMap<>(V35MacroNeighborhood.class);
    Sample(V35SubSwarmRole role, V35Bottleneck maximumType,
        double maximum, double second) {
      this.role = role;
      this.maximumType = maximumType;
      this.maximum = maximum;
      this.second = second;
    }
    double best(V35CaTaContext mask) {
      double result = 0.0;
      for (Map.Entry<V35MacroNeighborhood, Double> entry : gains.entrySet()) {
        if (mask == null || mask.allows(entry.getKey())) result = Math.max(result, entry.getValue());
      }
      return result;
    }
  }
}
