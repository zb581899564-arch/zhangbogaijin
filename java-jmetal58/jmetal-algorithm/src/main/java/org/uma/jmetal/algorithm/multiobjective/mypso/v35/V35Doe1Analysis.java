package org.uma.jmetal.algorithm.multiobjective.mypso.v35;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Pure, deterministic DOE-1 response/gate helpers. No algorithm state is touched. */
public final class V35Doe1Analysis {
  private V35Doe1Analysis() { }

  public static final class Block {
    public final V35SubSwarmMixture mixture;
    public final String instance;
    public final long seed;
    public final double cmax;
    public final double tec;
    public final double twc;
    public final double hv;
    public final double igd;
    public final double baseCmax;
    public final double baseTec;
    public final double baseTwc;
    public final double baseHv;
    public final double baseIgd;
    public Block(V35SubSwarmMixture mixture, String instance, long seed,
        double cmax, double tec, double twc, double hv, double igd,
        double baseCmax, double baseTec, double baseTwc, double baseHv, double baseIgd) {
      this.mixture=mixture;this.instance=instance;this.seed=seed;this.cmax=cmax;this.tec=tec;this.twc=twc;this.hv=hv;this.igd=igd;
      this.baseCmax=baseCmax;this.baseTec=baseTec;this.baseTwc=baseTwc;this.baseHv=baseHv;this.baseIgd=baseIgd;
    }
    public double dCmax(){return relative(baseCmax,cmax);}
    public double dTec(){return relative(baseTec,tec);}
    public double dTwc(){return relative(baseTwc,twc);}
    public double dHv(){return (hv-baseHv)/baseHv;}
    public double dIgd(){return (igd-baseIgd)/baseIgd;}
    private static double relative(double base,double value){return (base-value)/base;}
  }

  public static double median(List<Double> values) {
    if (values.isEmpty()) return Double.NaN;
    List<Double> copy = new ArrayList<>(values); Collections.sort(copy);
    int n=copy.size(); return n%2==1?copy.get(n/2):(copy.get(n/2-1)+copy.get(n/2))/2.0;
  }
  public static double median(List<Block> blocks, int response) {
    List<Double> values=new ArrayList<>();for(Block b:blocks)values.add(response(b,response));return median(values);
  }
  private static double response(Block b,int i){switch(i){case 0:return b.dCmax();case 1:return b.dTec();case 2:return b.dTwc();case 3:return b.dHv();case 4:return b.dIgd();default:throw new IllegalArgumentException("response 0..4");}}

  public static boolean passesQualityGates(List<Block> allBlocks) {
    if (median(allBlocks,3) < -0.02 || median(allBlocks,4) > 0.10) return false;
    Map<String,List<Block>> byInstance=new HashMap<>();for(Block b:allBlocks)byInstance.computeIfAbsent(b.instance,k->new ArrayList<>()).add(b);
    for(List<Block> blocks:byInstance.values())if(median(blocks,3)<-0.05&&median(blocks,4)>0.20)return false;
    boolean tecDown=true,twcDown=true;for(List<Block> blocks:byInstance.values()){tecDown &= median(blocks,1)<-0.02;twcDown &= median(blocks,2)<-0.02;}
    return !tecDown && !twcDown;
  }

  /** One observed paired-response summary.  Values retain the registered signs. */
  public static final class ResponseSummary {
    public final V35SubSwarmMixture mixture;
    public final double dCmax;
    public final double dTec;
    public final double dTwc;
    public final double dHv;
    public final double dIgd;

    ResponseSummary(V35SubSwarmMixture mixture, List<Block> blocks) {
      this.mixture = mixture;
      this.dCmax = median(blocks, 0);
      this.dTec = median(blocks, 1);
      this.dTwc = median(blocks, 2);
      this.dHv = median(blocks, 3);
      this.dIgd = median(blocks, 4);
    }
  }

  /** Groups observations deterministically by the physical-mixture identity. */
  public static List<ResponseSummary> responseSummaries(List<Block> blocks) {
    Map<V35SubSwarmMixture, List<Block>> grouped = new LinkedHashMap<>();
    for (Block block : blocks) {
      grouped.computeIfAbsent(block.mixture, key -> new ArrayList<Block>()).add(block);
    }
    List<ResponseSummary> summaries = new ArrayList<>();
    for (Map.Entry<V35SubSwarmMixture, List<Block>> entry : grouped.entrySet()) {
      summaries.add(new ResponseSummary(entry.getKey(), entry.getValue()));
    }
    Collections.sort(summaries, new Comparator<ResponseSummary>() {
      @Override public int compare(ResponseSummary left, ResponseSummary right) {
        return left.mixture.compareTo(right.mixture);
      }
    });
    return summaries;
  }

  /**
   * Registered five-dimensional observed Pareto filter used when the response
   * surface has failed its predictive-adequacy gate.  Higher is better for the
   * first four responses; lower is better for delta IGD.
   */
  public static List<ResponseSummary> observedParetoFront(List<Block> blocks) {
    List<ResponseSummary> all = responseSummaries(blocks);
    List<ResponseSummary> front = new ArrayList<>();
    for (ResponseSummary candidate : all) {
      boolean dominated = false;
      for (ResponseSummary other : all) {
        if (other != candidate && dominates(other, candidate)) {
          dominated = true;
          break;
        }
      }
      if (!dominated) front.add(candidate);
    }
    return front;
  }

  private static boolean dominates(ResponseSummary left, ResponseSummary right) {
    final double epsilon = 1e-12;
    boolean noWorse = left.dCmax >= right.dCmax - epsilon
        && left.dTec >= right.dTec - epsilon
        && left.dTwc >= right.dTwc - epsilon
        && left.dHv >= right.dHv - epsilon
        && left.dIgd <= right.dIgd + epsilon;
    boolean strictlyBetter = left.dCmax > right.dCmax + epsilon
        || left.dTec > right.dTec + epsilon
        || left.dTwc > right.dTwc + epsilon
        || left.dHv > right.dHv + epsilon
        || left.dIgd < right.dIgd - epsilon;
    return noWorse && strictlyBetter;
  }

  /** Observed fallback ranking mandated when model adequacy fails. */
  public static List<V35SubSwarmMixture> observedTopThree(List<Block> blocks) {
    final Map<V35SubSwarmMixture, ResponseSummary> by = new HashMap<>();
    for (ResponseSummary summary : observedParetoFront(blocks)) by.put(summary.mixture, summary);
    List<V35SubSwarmMixture> values=new ArrayList<>(by.keySet());
    Collections.sort(values,new Comparator<V35SubSwarmMixture>(){public int compare(V35SubSwarmMixture a,V35SubSwarmMixture b){
      ResponseSummary left = by.get(a), right = by.get(b);
      int c=Double.compare(-left.dCmax,-right.dCmax);if(c!=0)return c;
      c=Double.compare(-left.dHv,-right.dHv);if(c!=0)return c;
      c=Double.compare(left.dIgd,right.dIgd);if(c!=0)return c;return a.compareTo(b);
    }});
    return values.subList(0,Math.min(3,values.size()));
  }

  /** One point for the report-layer residual and held-out prediction tables. */
  public static final class ModelRow {
    public final V35SubSwarmMixture mixture;
    public final String instance;
    public final long seed;
    public final double observed;
    public final double fitted;
    public final double residual;
    public final double leaveTreatmentOutPrediction;

    ModelRow(V35SubSwarmMixture mixture, String instance, long seed,
        double observed, double fitted, double leaveTreatmentOutPrediction) {
      this.mixture = mixture;
      this.instance = instance;
      this.seed = seed;
      this.observed = observed;
      this.fitted = fitted;
      this.residual = observed - fitted;
      this.leaveTreatmentOutPrediction = leaveTreatmentOutPrediction;
    }
  }

  public static final class ModelDiagnostics {
    public final int rank; public final double conditionNumber; public final double adjustedR2;
    public final double predictedR2; public final double lotoRmse; public final boolean adequate;
    public final List<ModelRow> rows;
    ModelDiagnostics(int rank,double conditionNumber,double adjustedR2,double predictedR2,double lotoRmse){
      this(rank, conditionNumber, adjustedR2, predictedR2, lotoRmse,
          Collections.<ModelRow>emptyList());
    }
    ModelDiagnostics(int rank,double conditionNumber,double adjustedR2,double predictedR2,
        double lotoRmse,List<ModelRow> rows){
      this.rank=rank;this.conditionNumber=conditionNumber;this.adjustedR2=adjustedR2;
      this.predictedR2=predictedR2;this.lotoRmse=lotoRmse;
      this.rows=Collections.unmodifiableList(new ArrayList<>(rows));
      this.adequate=rank==10&&conditionNumber<=1e4&&adjustedR2>=0&&predictedR2>=0;
    }
  }

  /** Fits the registered 10-column quadratic Scheffe response to paired blocks. */
  public static ModelDiagnostics fitCmax(List<Block> blocks) {
    if (blocks == null || blocks.isEmpty()) return new ModelDiagnostics(0, Double.POSITIVE_INFINITY, Double.NaN, Double.NaN, Double.NaN);
    double[][] xtx=new double[10][10]; double[] xty=new double[10]; double mean=0;
    for(Block b:blocks){double[] x=V35SubSwarmMixtureDesign.modelRow(b.mixture);double y=b.dCmax();mean+=y;for(int i=0;i<10;i++){xty[i]+=x[i]*y;for(int j=0;j<10;j++)xtx[i][j]+=x[i]*x[j];}}
    mean/=blocks.size(); int rank=matrixRank(xtx); double cond=V35SubSwarmMixtureDesign.conditionNumberFromXtX(xtx); double[] beta=solve(xtx,xty);
    double sse=0,tss=0;for(Block b:blocks){double y=b.dCmax(),pred=dot(V35SubSwarmMixtureDesign.modelRow(b.mixture),beta);sse+=(y-pred)*(y-pred);tss+=(y-mean)*(y-mean);}double r2=tss<=1e-15?0:1-sse/tss;double adj=blocks.size()>10?1-(1-r2)*(blocks.size()-1.0)/(blocks.size()-10.0):Double.NaN;
    Map<V35SubSwarmMixture,double[]> leavePredictions = new HashMap<>();
    for(Block held:blocks){
      if (leavePredictions.containsKey(held.mixture)) continue;
      double[][] a=new double[10][10];double[] z=new double[10];
      for(Block b:blocks)if(!b.mixture.equals(held.mixture)){
        double[] x=V35SubSwarmMixtureDesign.modelRow(b.mixture);
        for(int i=0;i<10;i++){z[i]+=x[i]*b.dCmax();for(int j=0;j<10;j++)a[i][j]+=x[i]*x[j];}
      }
      leavePredictions.put(held.mixture, solve(a,z));
    }
    double press=0;List<ModelRow> rows = new ArrayList<>();
    for (Block b : blocks) {
      double observed = b.dCmax();
      double[] x = V35SubSwarmMixtureDesign.modelRow(b.mixture);
      double fitted = dot(x, beta);
      double leavePrediction = dot(x, leavePredictions.get(b.mixture));
      double error = observed - leavePrediction;
      press += error * error;
      rows.add(new ModelRow(b.mixture, b.instance, b.seed, observed, fitted, leavePrediction));
    }
    Collections.sort(rows, new Comparator<ModelRow>() {
      @Override public int compare(ModelRow left, ModelRow right) {
        int compared = left.mixture.compareTo(right.mixture);
        if (compared != 0) return compared;
        compared = left.instance.compareTo(right.instance);
        return compared != 0 ? compared : Long.compare(left.seed, right.seed);
      }
    });
    double predicted=tss<=1e-15?0:1-press/tss;double rmse=Math.sqrt(press/blocks.size());
    return new ModelDiagnostics(rank,cond,adj,predicted,rmse,rows);
  }

  private static int matrixRank(double[][] a){double[][]m=new double[a.length][];for(int i=0;i<a.length;i++)m[i]=a[i].clone();int r=0;for(int c=0;c<m.length&&r<m.length;c++){int p=r;for(int i=r+1;i<m.length;i++)if(Math.abs(m[i][c])>Math.abs(m[p][c]))p=i;if(Math.abs(m[p][c])<1e-10)continue;double[]t=m[r];m[r]=m[p];m[p]=t;for(int i=r+1;i<m.length;i++){double f=m[i][c]/m[r][c];for(int j=c;j<m.length;j++)m[i][j]-=f*m[r][j];}r++;}return r;}
  private static double[] solve(double[][] a,double[] b){int n=b.length;double[][]m=new double[n][n+1];for(int i=0;i<n;i++){System.arraycopy(a[i],0,m[i],0,n);m[i][n]=b[i];}for(int c=0;c<n;c++){int p=c;for(int i=c+1;i<n;i++)if(Math.abs(m[i][c])>Math.abs(m[p][c]))p=i;if(Math.abs(m[p][c])<1e-12)return new double[n];double[]t=m[c];m[c]=m[p];m[p]=t;double d=m[c][c];for(int j=c;j<=n;j++)m[c][j]/=d;for(int i=0;i<n;i++)if(i!=c){double f=m[i][c];for(int j=c;j<=n;j++)m[i][j]-=f*m[c][j];}}double[]x=new double[n];for(int i=0;i<n;i++)x[i]=m[i][n];return x;}
  private static double dot(double[]a,double[]b){double s=0;for(int i=0;i<a.length;i++)s+=a[i]*b[i];return s;}

  /** Design-only adequacy gate; response-model diagnostics are filled by the report layer. */
  public static ModelDiagnostics designAdequacy() {
    V35SubSwarmMixtureDesign.Selection s=V35SubSwarmMixtureDesign.select15();
    return new ModelDiagnostics(s.getRank(),s.getConditionNumber(),Double.NaN,Double.NaN,Double.NaN);
  }
}
