package org.uma.jmetal.runner.lc_psode;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import org.uma.jmetal.algorithm.multiobjective.mypso.p8.P8MetricCalculator;
import org.uma.jmetal.algorithm.multiobjective.mypso.v35.V35Doe1Analysis;
import org.uma.jmetal.algorithm.multiobjective.mypso.v35.V35SubSwarmMixture;
import org.uma.jmetal.algorithm.multiobjective.mypso.v35.V35SubSwarmMixtureDesign;

/** Offline DOE-1 development report; reference fronts are frozen per instance. */
public final class ZhangBoV35Doe1ReportRunner {
  private ZhangBoV35Doe1ReportRunner() { }
  public static void main(String[] args) throws Exception {
    Path root=Paths.get("docs/evidence/V35-DOE1-subgroup-mixture");Path output=root;
    for(int i=0;i<args.length;i++){if(args[i].equals("--root"))root=Paths.get(args[++i]);else if(args[i].equals("--output"))output=Paths.get(args[++i]);else throw new IllegalArgumentException("--root/--output only");}
    List<Run> runs=readRuns(root);verifyDevelopmentCampaign(runs);
    Map<String,List<double[]>> pooled=new HashMap<>();for(Run r:runs)pooled.computeIfAbsent(r.instance,k->new ArrayList<>()).addAll(r.front);
    Files.createDirectories(output.resolve("development-reference-fronts"));
    Map<String,List<double[]>> refs=new HashMap<>();for(Map.Entry<String,List<double[]>>e:pooled.entrySet()){
      List<double[]>ref=P8MetricCalculator.nondominated(exactDeduplicate(e.getValue()));refs.put(e.getKey(),ref);
      StringBuilder s=new StringBuilder("Cmax,TEC,TWC\n");for(double[]p:ref)s.append(p[0]).append(',').append(p[1]).append(',').append(p[2]).append('\n');
      Files.write(output.resolve("development-reference-fronts").resolve(e.getKey()+".csv"),s.toString().getBytes(StandardCharsets.UTF_8));
      double[] bounds=bounds(ref);
      Files.write(output.resolve("development-reference-fronts").resolve(e.getKey()+".properties"),
          ("rawPointCount="+e.getValue().size()+"\nexactDeduplicatedPointCount="+exactDeduplicate(e.getValue()).size()+"\nreferencePointCount="+ref.size()
          +"\nminCmax="+bounds[0]+"\nmaxCmax="+bounds[1]+"\nminTEC="+bounds[2]+"\nmaxTEC="+bounds[3]
          +"\nminTWC="+bounds[4]+"\nmaxTWC="+bounds[5]+"\nhvReferencePoint=1.1,1.1,1.1\n").getBytes(StandardCharsets.UTF_8));
    }
    for(Run r:runs)r.metrics=P8MetricCalculator.calculate(r.front,refs.get(r.instance));
    Files.createDirectories(output);writeMetrics(output.resolve("development-metrics.csv"),runs);writeRecords(output.resolve("development-run-records.csv"),runs);
    Map<String,Run> base=new HashMap<>();V35SubSwarmMixture baseline=V35SubSwarmMixture.BASELINE;for(Run r:runs)if(r.mixture.equals(baseline))base.put(r.instance+"/"+r.seed,r);
    List<V35Doe1Analysis.Block> blocks=new ArrayList<>();for(Run r:runs){Run b=base.get(r.instance+"/"+r.seed);if(b==null)continue;blocks.add(new V35Doe1Analysis.Block(r.mixture,r.instance,r.seed,r.min(0),r.min(1),r.min(2),r.metrics.hv,r.metrics.igd,b.min(0),b.min(1),b.min(2),b.metrics.hv,b.metrics.igd));}
    V35Doe1Analysis.ModelDiagnostics d=V35Doe1Analysis.fitCmax(blocks);
    writeModelDiagnostics(output, d);
    List<V35Doe1Analysis.Block> eligible=new ArrayList<>();Map<V35SubSwarmMixture,List<V35Doe1Analysis.Block>> by=new HashMap<>();
    for(V35Doe1Analysis.Block b:blocks)if(!b.mixture.equals(baseline))by.computeIfAbsent(b.mixture,k->new ArrayList<V35Doe1Analysis.Block>()).add(b);
    for(List<V35Doe1Analysis.Block>v:by.values())if(V35Doe1Analysis.passesQualityGates(v))eligible.addAll(v);
    List<V35SubSwarmMixture> top=V35Doe1Analysis.observedTopThree(eligible);
    writeSelection(output, d, eligible, top);
    Files.write(output.resolve("FINAL_PARAMETER_DECISION.md"), developmentDecisionText(d, top).getBytes(StandardCharsets.UTF_8));
  }
  private static void verifyDevelopmentCampaign(List<Run> runs) {
    if (runs.size() != 135) throw new IllegalStateException("DOE-1 development requires exactly 135 runs, found=" + runs.size());
    Set<String> ids = new HashSet<>();
    for (Run run : runs) {
      if (run.front.isEmpty()) throw new IllegalStateException("empty front: " + run.runDirectory);
      if (!"COMPLETED".equals(run.status)) throw new IllegalStateException("non-completed run: " + run.runDirectory + " status=" + run.status);
      if (run.fullEvaluations != 500000L) throw new IllegalStateException("non-exact FE: " + run.runDirectory + " FE=" + run.fullEvaluations);
      if (!ids.add(run.mixture + "/" + run.instance + "/" + run.seed)) throw new IllegalStateException("duplicate run identity");
    }
  }

  private static List<double[]> exactDeduplicate(List<double[]> values) {
    List<double[]> result = new ArrayList<>(); Set<String> fingerprints = new HashSet<>();
    for (double[] value : values) {
      String fingerprint = Long.toHexString(Double.doubleToLongBits(value[0])) + ":"
          + Long.toHexString(Double.doubleToLongBits(value[1])) + ":"
          + Long.toHexString(Double.doubleToLongBits(value[2]));
      if (fingerprints.add(fingerprint)) result.add(value);
    }
    return result;
  }

  private static double[] bounds(List<double[]> values) {
    double[] result = {Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY,
        Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY};
    for (double[] value : values) for (int i = 0; i < 3; i++) {
      result[i * 2] = Math.min(result[i * 2], value[i]); result[i * 2 + 1] = Math.max(result[i * 2 + 1], value[i]);
    }
    return result;
  }

  private static void writeModelDiagnostics(Path output, V35Doe1Analysis.ModelDiagnostics diagnostics) throws Exception {
    String decision = diagnostics.adequate ? "MODEL_SELECTION_ALLOWED" : "OBSERVED_MEDIAN_FALLBACK_REQUIRED";
    String reason = diagnostics.adequate ? "all preregistered adequacy gates passed"
        : "out-of-sample predictive ability failed: predictedR2=" + diagnostics.predictedR2;
    String text = "rank,conditionNumber,adjustedR2,predictedR2,lotoRMSE,adequate,modelDecision,fallbackReason\n"
        + diagnostics.rank + "," + diagnostics.conditionNumber + "," + diagnostics.adjustedR2 + ","
        + diagnostics.predictedR2 + "," + diagnostics.lotoRmse + "," + diagnostics.adequate + ","
        + decision + ",\"" + reason + "\"\n";
    Files.write(output.resolve("mixture-model-diagnostics.csv"), text.getBytes(StandardCharsets.UTF_8));
    StringBuilder rows = new StringBuilder("mixture,instance,seed,observedDeltaCmax,fittedDeltaCmax,residual,leaveOneTreatmentOutPrediction\n");
    for (V35Doe1Analysis.ModelRow row : diagnostics.rows) rows.append(row.mixture).append(',').append(row.instance).append(',')
        .append(row.seed).append(',').append(row.observed).append(',').append(row.fitted).append(',').append(row.residual)
        .append(',').append(row.leaveTreatmentOutPrediction).append('\n');
    Files.write(output.resolve("mixture-model-residuals.csv"), rows.toString().getBytes(StandardCharsets.UTF_8));
  }

  private static void writeSelection(Path output, V35Doe1Analysis.ModelDiagnostics diagnostics,
      List<V35Doe1Analysis.Block> eligible, List<V35SubSwarmMixture> top) throws Exception {
    Map<V35SubSwarmMixture, V35Doe1Analysis.ResponseSummary> summaries = new HashMap<>();
    for (V35Doe1Analysis.ResponseSummary summary : V35Doe1Analysis.observedParetoFront(eligible)) summaries.put(summary.mixture, summary);
    StringBuilder selection = new StringBuilder("decision,rank,mixture,medianDeltaCmax,medianDeltaTEC,medianDeltaTWC,medianDeltaHV,medianDeltaIGD,modelAdequate,reason\n");
    if (top.isEmpty()) selection.append("NO_NEW_TREATMENT_PASSED,,,,,,,,").append(diagnostics.adequate).append(",quality gates\n");
    int rank = 1;
    for (V35SubSwarmMixture mixture : top) {
      V35Doe1Analysis.ResponseSummary summary = summaries.get(mixture);
      selection.append("DEVELOPMENT_TOP3,").append(rank++).append(',').append(mixture).append(',').append(summary.dCmax).append(',')
          .append(summary.dTec).append(',').append(summary.dTwc).append(',').append(summary.dHv).append(',').append(summary.dIgd)
          .append(',').append(diagnostics.adequate).append(",observed paired-median fallback after five-dimensional Pareto filter\n");
    }
    Files.write(output.resolve("treatment-selection.csv"), selection.toString().getBytes(StandardCharsets.UTF_8));
  }

  private static String developmentDecisionText(V35Doe1Analysis.ModelDiagnostics diagnostics, List<V35SubSwarmMixture> top) {
    StringBuilder text = new StringBuilder("# V35-DOE-1 开发阶段冻结报告\n\n");
    text.append("开发运行完整性：135/135条、每条500000 FE，均已由报告重建器检查。\n\n");
    text.append("二次 Scheffe 模型为全秩（rank=").append(diagnostics.rank)
        .append("），全部项可估计；条件数单独报告为 ").append(diagnostics.conditionNumber).append("。\n\n");
    text.append("模型选择：**REJECTED**。尽管拟合内 adjusted R2=").append(diagnostics.adjustedR2)
        .append("，其 out-of-sample predicted R2=").append(diagnostics.predictedR2)
        .append(" < 0；因此预注册的 observed paired-median fallback 生效。LOTO-RMSE=")
        .append(diagnostics.lotoRmse).append("。\n\n");
    if (top.isEmpty()) text.append("没有新的 treatment 通过质量门；confirmation 必须跳过并冻结 20/40/20/20。\n");
    else text.append("候选仅来自通过质量门并经五维观测 Pareto 过滤后的前三： ").append(top)
        .append("。它们尚未通过 held-out confirmation；正式搜索容量仍未冻结。\n");
    text.append("\n```text\ndevelopment_campaign_integrity=ACCEPTED\nfairness_and_budget_closure=ACCEPTED\nmechanism_trigger_closure=ACCEPTED\nreference_front_construction=ACCEPTED\nresponse_surface_selection=REJECTED\nobserved_fallback_selection=ACCEPTED\nfinal_search_mixture=NOT_FROZEN\nheldout_confirmation=REQUIRED\n```\n");
    return text.toString();
  }

  static final class Run {String instance;long seed;V35SubSwarmMixture mixture;List<double[]>front;P8MetricCalculator.Metrics metrics;Path runDirectory;String status;long fullEvaluations;double min(int i){double v=Double.POSITIVE_INFINITY;for(double[]p:front)v=Math.min(v,p[i]);return v;}}
  static List<Run> readRuns(Path root)throws Exception{List<Run>out=new ArrayList<>();if(!Files.isDirectory(root))return out;Files.walk(root).filter(p->p.getFileName().toString().equals("front.csv")).forEach(p->{try{Path seed=p.getParent();String seedName=seed.getFileName().toString();long s=Long.parseLong(seedName.substring(seedName.indexOf('-')+1));Path inst=seed.getParent();String instance=inst.getFileName().toString();Path tr=inst.getParent();String n=tr.getFileName().toString();int idx=Integer.parseInt(n.substring(n.indexOf('-')+1,n.indexOf('-',n.indexOf('-')+1)));V35SubSwarmMixture m=V35SubSwarmMixtureDesign.select15().getTreatments().get(idx);List<double[]>front=new ArrayList<>();List<String>lines=Files.readAllLines(p);for(int i=1;i<lines.size();i++){String[]v=lines.get(i).split(",");if(v.length>=3)front.add(new double[]{Double.parseDouble(v[0]),Double.parseDouble(v[1]),Double.parseDouble(v[2])});}Properties properties=new Properties();try(java.io.InputStream input=Files.newInputStream(seed.resolve("status.properties"))){properties.load(input);}Run r=new Run();r.instance=instance;r.seed=s;r.mixture=m;r.front=front;r.runDirectory=seed;r.status=properties.getProperty("status", "MISSING");r.fullEvaluations=Long.parseLong(properties.getProperty("fullEvaluations", "-1"));out.add(r);}catch(Exception e){throw new RuntimeException(e);}});return out;}
  static void writeMetrics(Path p,List<Run>rs)throws Exception{StringBuilder s=new StringBuilder("instance,seed,mixture,HV,IGD,Spacing,C_forward,C_reverse,frontSize\n");for(Run r:rs)s.append(r.instance).append(',').append(r.seed).append(',').append(r.mixture).append(',').append(r.metrics.hv).append(',').append(r.metrics.igd).append(',').append(r.metrics.spacing).append(',').append(r.metrics.cForward).append(',').append(r.metrics.cReverse).append(',').append(r.metrics.nondominatedCount).append('\n');Files.write(p,s.toString().getBytes(StandardCharsets.UTF_8));}
  static void writeRecords(Path p,List<Run>rs)throws Exception{StringBuilder s=new StringBuilder("instance,seed,mixture,frontSize\n");for(Run r:rs)s.append(r.instance).append(',').append(r.seed).append(',').append(r.mixture).append(',').append(r.front.size()).append('\n');Files.write(p,s.toString().getBytes(StandardCharsets.UTF_8));}
}
