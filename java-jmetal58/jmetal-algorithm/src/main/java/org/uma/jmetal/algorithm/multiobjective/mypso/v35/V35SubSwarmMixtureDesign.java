package org.uma.jmetal.algorithm.multiobjective.mypso.v35;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** Deterministic constrained D-optimal design used by DOE-1. */
public final class V35SubSwarmMixtureDesign {
  private V35SubSwarmMixtureDesign() { }

  public static final class Selection implements Serializable {
    private static final long serialVersionUID = 1L;
    private final List<V35SubSwarmMixture> treatments;
    private final List<String> trace;
    private final int rank;
    private final double logDet;
    private final double conditionNumber;
    private Selection(List<V35SubSwarmMixture> treatments, List<String> trace,
        int rank, double logDet, double conditionNumber) {
      this.treatments = Collections.unmodifiableList(new ArrayList<>(treatments));
      this.trace = Collections.unmodifiableList(new ArrayList<>(trace));
      this.rank = rank; this.logDet = logDet; this.conditionNumber = conditionNumber;
    }
    public List<V35SubSwarmMixture> getTreatments() { return treatments; }
    public List<String> getTrace() { return trace; }
    public int getRank() { return rank; }
    public double getLogDet() { return logDet; }
    public double getConditionNumber() { return conditionNumber; }
  }

  public static List<V35SubSwarmMixture> candidateLattice() {
    List<V35SubSwarmMixture> result = new ArrayList<>();
    for (int g1 = 10; g1 <= 30; g1 += 5)
      for (int g4 = 25; g4 <= 60; g4 += 5)
        for (int g2 = 10; g2 <= 30; g2 += 5) {
          int g3 = 100 - g1 - g4 - g2;
          if (g3 >= 10 && g3 <= 30 && g3 % 5 == 0) {
            result.add(V35SubSwarmMixture.of(g1, g4, g2, g3));
          }
        }
    Collections.sort(result);
    return Collections.unmodifiableList(result);
  }

  public static Selection select15() {
    List<V35SubSwarmMixture> lattice = candidateLattice();
    List<V35SubSwarmMixture> selected = new ArrayList<>();
    selected.add(V35SubSwarmMixture.BASELINE);
    selected.add(V35SubSwarmMixture.HISTORICAL_REGION_CONTROL);
    selected.add(V35SubSwarmMixture.BALANCED_CONTROL);
    Set<V35SubSwarmMixture> forced = new HashSet<>(selected);
    List<String> trace = new ArrayList<>();
    trace.add("FORCED=" + V35SubSwarmMixture.BASELINE);
    trace.add("FORCED=" + V35SubSwarmMixture.HISTORICAL_REGION_CONTROL);
    trace.add("FORCED=" + V35SubSwarmMixture.BALANCED_CONTROL);

    // First obtain a full-rank ten-point support.  The score is rank first,
    // then log(det), then lexical order; this makes ties reproducible.
    while (rank(selected) < 10) {
      V35SubSwarmMixture best = null; int bestRank = -1; double bestScore = -Double.MAX_VALUE;
      for (V35SubSwarmMixture candidate : lattice) if (!selected.contains(candidate)) {
        List<V35SubSwarmMixture> trial = new ArrayList<>(selected); trial.add(candidate);
        int r = rank(trial); double score = logDet(trial);
        if (best == null || r > bestRank || (r == bestRank && better(score, bestScore))
            || (r == bestRank && equal(score, bestScore) && candidate.compareTo(best) < 0)) {
          best = candidate; bestRank = r; bestScore = score;
        }
      }
      if (best == null) throw new IllegalStateException("DOE lattice cannot reach rank 10");
      selected.add(best); trace.add("RANK_ADD=" + best + ";rank=" + bestRank);
    }
    // Fill the remaining support points greedily before Fedorov exchanges.
    while (selected.size() < 15) {
      V35SubSwarmMixture best = null; double bestScore = -Double.MAX_VALUE;
      for (V35SubSwarmMixture candidate : lattice) if (!selected.contains(candidate)) {
        List<V35SubSwarmMixture> trial = new ArrayList<>(selected); trial.add(candidate);
        double score = logDet(trial);
        if (best == null || better(score, bestScore)
            || (equal(score, bestScore) && candidate.compareTo(best) < 0)) {
          best = candidate; bestScore = score;
        }
      }
      selected.add(best); trace.add("FILL_ADD=" + best + ";logDet=" + bestScore);
    }
    boolean changed = true;
    while (changed) {
      changed = false; double current = logDet(selected);
      V35SubSwarmMixture bestOut = null, bestIn = null; double best = current;
      for (int i = 0; i < selected.size(); i++) {
        if (forced.contains(selected.get(i))) continue;
        for (V35SubSwarmMixture candidate : lattice) if (!selected.contains(candidate)) {
          List<V35SubSwarmMixture> trial = new ArrayList<>(selected);
          V35SubSwarmMixture out = trial.set(i, candidate);
          double score = logDet(trial);
          if (score > best + 1e-12 || (equal(score, best) && tieBreak(candidate, out, bestIn, bestOut))) {
            best = score; bestIn = candidate; bestOut = out;
          }
        }
      }
      if (bestIn != null && best > current + 1e-12) {
        int index = selected.indexOf(bestOut); selected.set(index, bestIn);
        trace.add("EXCHANGE=" + bestOut + "->" + bestIn + ";logDet=" + best);
        changed = true;
      }
    }
    Collections.sort(selected);
    return new Selection(selected, trace, rank(selected), logDet(selected), conditionNumber(selected));
  }

  private static boolean tieBreak(V35SubSwarmMixture in, V35SubSwarmMixture out,
      V35SubSwarmMixture bestIn, V35SubSwarmMixture bestOut) {
    if (bestIn == null) return true;
    int c = in.compareTo(bestIn); if (c != 0) return c < 0;
    return out.compareTo(bestOut) < 0;
  }
  private static boolean better(double a, double b) { return a > b + 1e-12; }
  private static boolean equal(double a, double b) { return Math.abs(a - b) <= 1e-12; }

  /** Four linear Scheffe terms followed by all six pairwise interactions. */
  public static double[] modelRow(V35SubSwarmMixture mixture) {
    double x1 = mixture.getG1Cmax() / 100.0, x2 = mixture.getG4Balanced() / 100.0;
    double x3 = mixture.getG2Tec() / 100.0, x4 = mixture.getG3Twc() / 100.0;
    return new double[]{x1, x2, x3, x4, x1*x2, x1*x3, x1*x4, x2*x3, x2*x4, x3*x4};
  }

  public static int rank(List<V35SubSwarmMixture> values) { return rank(matrix(values)); }
  public static double logDet(List<V35SubSwarmMixture> values) { return logDet(matrix(values)); }
  public static double conditionNumber(List<V35SubSwarmMixture> values) {
    return conditionNumber(matrix(values));
  }
  /** Condition number helper for report-layer regression diagnostics. */
  public static double conditionNumberFromXtX(double[][] xtx) {
    return conditionNumber(xtx);
  }

  private static double[][] matrix(List<V35SubSwarmMixture> values) {
    double[][] xtx = new double[10][10];
    for (V35SubSwarmMixture value : values) {
      double[] row = modelRow(value);
      for (int i = 0; i < 10; i++) for (int j = 0; j < 10; j++) xtx[i][j] += row[i]*row[j];
    }
    return xtx;
  }
  private static int rank(double[][] a) {
    double[][] m = copy(a); int n = m.length, r = 0;
    for (int c = 0; c < n && r < n; c++) {
      int pivot = r; for (int i = r+1; i < n; i++) if (Math.abs(m[i][c]) > Math.abs(m[pivot][c])) pivot = i;
      if (Math.abs(m[pivot][c]) <= 1e-10) continue;
      double[] tmp = m[r]; m[r] = m[pivot]; m[pivot] = tmp;
      for (int i = r+1; i < n; i++) { double f = m[i][c]/m[r][c]; for (int j=c; j<n; j++) m[i][j]-=f*m[r][j]; }
      r++;
    }
    return r;
  }
  private static double logDet(double[][] a) {
    double[][] m = copy(a); int n = m.length; double result = 0.0;
    for (int c=0;c<n;c++) { int p=c; for(int i=c+1;i<n;i++) if(Math.abs(m[i][c])>Math.abs(m[p][c]))p=i;
      if(Math.abs(m[p][c])<=1e-12)return -Double.MAX_VALUE; double[] t=m[c];m[c]=m[p];m[p]=t;
      result += Math.log(Math.abs(m[c][c])); for(int i=c+1;i<n;i++){double f=m[i][c]/m[c][c];for(int j=c+1;j<n;j++)m[i][j]-=f*m[c][j];}
    } return result;
  }
  private static double conditionNumber(double[][] a) {
    double[][] m=copy(a); int n=m.length;
    for(int iter=0;iter<100;iter++){int p=0,q=1;double max=0;for(int i=0;i<n;i++)for(int j=i+1;j<n;j++)if(Math.abs(m[i][j])>max){max=Math.abs(m[i][j]);p=i;q=j;}if(max<1e-12)break;double phi=.5*Math.atan2(2*m[p][q],m[q][q]-m[p][p]),c=Math.cos(phi),s=Math.sin(phi);for(int i=0;i<n;i++){double mip=m[i][p],miq=m[i][q];m[i][p]=c*mip-s*miq;m[i][q]=s*mip+c*miq;}for(int i=0;i<n;i++){double mpi=m[p][i],mqi=m[q][i];m[p][i]=c*mpi-s*mqi;m[q][i]=s*mpi+c*mqi;}}
    double min=Double.POSITIVE_INFINITY,max=0;for(int i=0;i<n;i++){min=Math.min(min,Math.abs(m[i][i]));max=Math.max(max,Math.abs(m[i][i]));}return min<=1e-15?Double.POSITIVE_INFINITY:Math.sqrt(max/min);
  }
  private static double[][] copy(double[][] a){double[][] r=new double[a.length][];for(int i=0;i<a.length;i++)r[i]=a[i].clone();return r;}
}
