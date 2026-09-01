package org.uma.jmetal.runner.lc_psode;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import org.uma.jmetal.algorithm.multiobjective.mypso.p8.P8InitialPopulationProvider;
import org.uma.jmetal.algorithm.multiobjective.mypso.v35.V35BottleneckDiagnosisConfiguration;
import org.uma.jmetal.algorithm.multiobjective.mypso.v35.V35CaTaLiteConfiguration;
import org.uma.jmetal.algorithm.multiobjective.mypso.v35.V35FairRunner;
import org.uma.jmetal.algorithm.multiobjective.mypso.v35.V35LocalFeBudgetConfiguration;
import org.uma.jmetal.algorithm.multiobjective.mypso.v35.V35LocalSearchOrder;
import org.uma.jmetal.algorithm.multiobjective.mypso.v35.V35ProductionConfiguration;
import org.uma.jmetal.algorithm.multiobjective.mypso.v35.V35SubSwarmMixture;
import org.uma.jmetal.algorithm.multiobjective.mypso.v35.V35SubSwarmMixtureDesign;
import org.uma.jmetal.algorithm.multiobjective.mypso.zhangbo.PddrSelectionMode;
import org.uma.jmetal.problem.multiobjective.dfsp.decoder.ProductionDecodeMode;
import org.uma.jmetal.problem.multiobjective.dfsp.fatigue.ZhangBoCanonicalProblemLoader;
import org.uma.jmetal.problem.multiobjective.dfsp.fatigue.ZhangBoCanonicalProductionProblem;
import org.uma.jmetal.problem.multiobjective.dfsp.fatigue.shift.ZhangBoShiftConfiguration;
import org.uma.jmetal.solution.PermutationSolution;
import org.uma.jmetal.util.pseudorandom.JMetalRandom;

/**
 * DOE-1 single-run/preflight entry point.  It is intentionally a runner for
 * the frozen A4 arm only; no region quota, BP selector, pressure mask, shift,
 * or directional teacher-pool field can be supplied by the command line.
 */
public final class ZhangBoV35Doe1MixtureRunner {
  public static final String VERSION = "v35-doe1-mixture-v1";
  public static final int POPULATION = 100;
  public static final int FORMAL_FES = 500000;
  public static final int PREFLIGHT_FES = 2000;
  private static final long[] SEEDS = {20260822L, 20260823L, 20260824L};
  private static final String[] INSTANCES = {"20_2_3_1", "50_2_3_1", "100_2_3_1"};

  private ZhangBoV35Doe1MixtureRunner() { }

  public static void main(String[] args) throws Exception {
    Arguments a = Arguments.parse(args);
    Path project = a.projectRoot.toAbsolutePath().normalize();
    Path javaProject = Files.isDirectory(project.resolve("EADHFSP")) ? project : project.resolve("java-jmetal58");
    Path output = a.output.toAbsolutePath().normalize();
    Files.createDirectories(output);
    if ("PREFLIGHT".equals(a.phase)) {
      writeDesignArtifacts(output);
      List<String> rows = new ArrayList<>();
      rows.add("treatment,mixture,instance,seed,status,FE,frontSize,initialPopulationHash,reason");
      List<V35SubSwarmMixture> treatments = V35SubSwarmMixtureDesign.select15().getTreatments();
      for (int i = 0; i < treatments.size(); i++) {
        // The 15-run preflight is deliberately a single fixed instance/seed;
        // it checks all capacity rows before the 135-run development matrix.
        RunResult result = runOne(javaProject, output.resolve("preflight"), i,
            treatments.get(i), "20_2_3_1", 20260822L, PREFLIGHT_FES);
        rows.add(result.csvRow());
      }
      Files.write(output.resolve("preflight-results.csv"), (String.join("\n", rows) + "\n").getBytes(StandardCharsets.UTF_8));
      System.out.println("V35_DOE1_PREFLIGHT_COMPLETED treatments=" + treatments.size());
    } else if ("RUN".equals(a.phase)) {
      if (a.treatment < 0 || a.treatment >= 15) throw new IllegalArgumentException("--treatment 0..14 required");
      if (a.instance == null || a.seed == Long.MIN_VALUE) throw new IllegalArgumentException("--instance and --seed required");
      V35SubSwarmMixture mixture = V35SubSwarmMixtureDesign.select15().getTreatments().get(a.treatment);
      RunResult result = runOne(javaProject, output, a.treatment, mixture, a.instance, a.seed, a.maxFes);
      System.out.println("V35_DOE1_RUN_COMPLETED " + result.csvRow());
    } else if ("REGISTRY".equals(a.phase)) {
      writeDesignArtifacts(output);
      System.out.println("V35_DOE1_REGISTRY_WRITTEN treatments=15 candidates="
          + V35SubSwarmMixtureDesign.candidateLattice().size());
    } else {
      throw new IllegalArgumentException("--phase PREFLIGHT|REGISTRY|RUN");
    }
  }

  static RunResult runOne(Path javaProject, Path output, int treatmentIndex,
      V35SubSwarmMixture mixture, String instanceName, long seed, int maxFes) throws Exception {
    Path runDir = output.resolve("treatment-" + treatmentIndex + "-" + mixture.toString()
        .replace('/', '_') + "/" + instanceName + "/seed-" + seed);
    return runOne(javaProject, runDir, treatmentIndex, mixture, instanceName, seed, maxFes,
        maxFes == PREFLIGHT_FES ? "PREFLIGHT_STARTUP_BOUNDARY_ONLY" : "FORMAL_DEVELOPMENT");
  }

  /** One confirmation run; callers launch this process once per physical run. */
  static RunResult runHeldout(Path javaProject, Path output, String arm, int treatmentIndex,
      V35SubSwarmMixture mixture, String instanceName, long seed) throws Exception {
    if (FORMAL_FES != 500000) throw new IllegalStateException("held-out budget drift");
    Path runDir = output.resolve("runs").resolve(arm + "-" + mixture.toString().replace('/', '_'))
        .resolve(instanceName).resolve("seed-" + seed);
    return runOne(javaProject, runDir, treatmentIndex, mixture, instanceName, seed, FORMAL_FES,
        "HELDOUT_CONFIRMATION");
  }

  private static RunResult runOne(Path javaProject, Path runDir, int treatmentIndex,
      V35SubSwarmMixture mixture, String instanceName, long seed, int maxFes,
      String campaignPhase) throws Exception {
    if (mixture == null || !V35SubSwarmMixtureDesign.candidateLattice().contains(mixture)) {
      throw new IllegalArgumentException("mixture is not in the DOE-1 lattice");
    }
    if (maxFes <= 0 || (maxFes != PREFLIGHT_FES && maxFes != FORMAL_FES)) {
      throw new IllegalArgumentException("DOE-1 only permits 2000 or 500000 FE");
    }
    Path instance = javaProject.resolve("EADHFSP/" + instanceName + ".txt");
    Path extension = javaProject.resolve("instance-extensions/v1/" + instanceName + ".setup.txt");
    Path fatigue = javaProject.resolve("fatigue-parameters/v1/" + instanceName + ".fatigue.txt");
    require(instance); require(extension); require(fatigue);
    if (Files.exists(runDir.resolve("status.properties"))) throw new IllegalStateException("refusing overwrite: " + runDir);
    Files.createDirectories(runDir);
    ZhangBoCanonicalProductionProblem problem = ZhangBoCanonicalProblemLoader.load(
        instance, ProductionDecodeMode.FM3, seed, extension.getParent(), fatigue.getParent(),
        ZhangBoShiftConfiguration.none());
    JMetalRandom.getInstance().setSeed(seed);
    List<PermutationSolution<Integer>> initial = new ArrayList<>();
    for (int i = 0; i < POPULATION; i++) initial.add(problem.createSolution());
    String initialHash = P8InitialPopulationProvider.sha256(initial);
    V35ProductionConfiguration configuration = V35ProductionConfiguration.builder()
        .seed(seed).populationSize(POPULATION).maxEvaluations(maxFes)
        .decoderMode(ProductionDecodeMode.FM3).dscr(true).cfvf(true).qg(true).qp(true).caTaLite(true)
        .directionalTeacherPool(false).teacherPoolSize(10)
        .pddrSelectionMode(PddrSelectionMode.GLOBAL_ORIGINAL)
        .localSearchOrder(V35LocalSearchOrder.CATA_THEN_INHERITED)
        .localFeBudget(V35LocalFeBudgetConfiguration.of(0.25, 0.65))
        .caTaLiteConfiguration(V35CaTaLiteConfiguration.standard())
        .subSwarmMixture(mixture).build();
    if (configuration.getPddrSelectionMode() != PddrSelectionMode.GLOBAL_ORIGINAL
        || configuration.getLocalSearchOrder() != V35LocalSearchOrder.CATA_THEN_INHERITED
        || configuration.isDirectionalTeacherPoolEnabled()
        || configuration.getShiftMode() != org.uma.jmetal.problem.multiobjective.dfsp.fatigue.shift.ZhangBoShiftMode.NONE) {
      throw new IllegalStateException("DOE-1 frozen boundary mismatch");
    }
    V35FairRunner.RunRecord record = V35FairRunner.run(V35FairRunner.Mode.V35_FULL_POOL_OFF,
        problem, P8InitialPopulationProvider.copy(initial), maxFes, seed, false,
        V35BottleneckDiagnosisConfiguration.fullMaskNoShadow(), false, configuration);
    String configText = "doe1Version=" + VERSION + "\ntreatmentIndex=" + treatmentIndex
        + "\nmixture=" + mixture + "\nmixtureHash=" + mixture.hash()
        + "\ninstance=" + instanceName + "\nseed=" + seed + "\nmaxFEs=" + maxFes
        + "\ncampaignPhase=" + campaignPhase
        + "\nselector=GLOBAL_ORIGINAL\nlocalSearchOrder=CATA_THEN_INHERITED\n"
        + "familyMode=DEGENERATE_SINGLE_FAMILY\nsetupMode=SEQUENCE_INDEPENDENT\nshiftMode=NONE\n"
        + "decoderMode=FM3\nobjectiveAdapter=0,1,6\nsoftFreezeRho=0\npressureMask=BAL_FULL_OPEN\n"
        + "directionalTeacherPool=false\ninitialPopulationHash=" + initialHash
        + "\npreflightSemantics=" + campaignPhase
        + "\nmechanismVectorHash=" + configuration.configurationHash()
        + "\nv35ConfigurationBegin\n" + configuration.canonicalText()
        + "v35ConfigurationEnd\n";
    V35FairRunner.writeRecord(record, runDir, configText);
    Files.write(runDir.resolve("mechanism-summary.txt"), (record.getMechanismSummary() + "\n").getBytes(StandardCharsets.UTF_8));
    Files.write(runDir.resolve("run-record.csv"), ("treatment,mixture,instance,seed,status,FE,decoderCalls,illegalSolutions,duplicateEvaluations,runtimeSubSwarmSizes,frontSize,initialPopulationHash,configurationHash\n"
        + treatmentIndex + "," + mixture + "," + instanceName + "," + seed + "," + record.getStatus() + ","
        + record.getFullEvaluations() + "," + record.getDecoderCalls() + "," + record.getIllegalSolutions() + ","
        + record.getDuplicateEvaluations() + ",\"" + record.getRuntimeSubSwarmSizes() + "\"," + record.getFront().size() + "," + initialHash + ","
        + configuration.configurationHash() + "\n").getBytes(StandardCharsets.UTF_8));
    return new RunResult(treatmentIndex, mixture, instanceName, seed, record, initialHash);
  }

  private static void writeDesignArtifacts(Path output) throws IOException {
    List<V35SubSwarmMixture> lattice = V35SubSwarmMixtureDesign.candidateLattice();
    List<String> c = new ArrayList<>(); c.add("index,G1_CMAX,G4_BALANCED,G2_TEC,G3_TWC,mixtureHash");
    for (int i=0;i<lattice.size();i++){V35SubSwarmMixture m=lattice.get(i);c.add(i+","+m.getGroupU1()+","+m.getGroupC2()+","+m.getGroupD3()+","+m.getGroupUNew()+","+m.hash());}
    Files.write(output.resolve("mixture-candidate-lattice.csv"), (String.join("\n", c)+"\n").getBytes(StandardCharsets.UTF_8));
    V35SubSwarmMixtureDesign.Selection s=V35SubSwarmMixtureDesign.select15();
    List<String> t=new ArrayList<>();t.add("treatment,mixture,mixtureHash,forced");for(int i=0;i<s.getTreatments().size();i++){V35SubSwarmMixture m=s.getTreatments().get(i);t.add(i+","+m+","+m.hash()+","+(m.equals(V35SubSwarmMixture.BASELINE)||m.equals(V35SubSwarmMixture.HISTORICAL_REGION_CONTROL)||m.equals(V35SubSwarmMixture.BALANCED_CONTROL)));}
    Files.write(output.resolve("treatment-registry.csv"),(String.join("\n",t)+"\n").getBytes(StandardCharsets.UTF_8));
    Files.write(output.resolve("doptimal-selection-trace.csv"),("step,event\n"+String.join("\n",s.getTrace().stream().map(x -> "-1,\""+x.replace("\"","\"\"")+"\"").toArray(String[]::new))+"\n").getBytes(StandardCharsets.UTF_8));
    Files.write(output.resolve("design-summary.properties"),("candidateCount="+lattice.size()+"\ntreatmentCount="+s.getTreatments().size()+"\nrank="+s.getRank()+"\nlogDet="+s.getLogDet()+"\nconditionNumber="+s.getConditionNumber()+"\nmodelColumns=10\n").getBytes(StandardCharsets.UTF_8));
  }
  private static void require(Path p){if(!Files.isRegularFile(p))throw new IllegalArgumentException("missing file: "+p);}

  static final class RunResult {
    final int index; final V35SubSwarmMixture mixture; final String instance; final long seed; final V35FairRunner.RunRecord record; final String initialHash;
    RunResult(int i,V35SubSwarmMixture m,String n,long s,V35FairRunner.RunRecord r,String h){index=i;mixture=m;instance=n;seed=s;record=r;initialHash=h;}
    String csvRow(){return index+","+mixture+","+instance+","+seed+","+record.getStatus()+","+record.getFullEvaluations()+","+record.getFront().size()+","+initialHash+","+record.getStopReason();}
  }
  static final class Arguments {
    String phase="REGISTRY",instance;long seed=Long.MIN_VALUE;int treatment=-1,maxFes=FORMAL_FES;Path projectRoot=Paths.get(".");Path output=Paths.get("docs/evidence/V35-DOE1-subgroup-mixture");
    static Arguments parse(String[] args){Arguments a=new Arguments();for(int i=0;i<args.length;i++){String x=args[i];if(x.equals("--phase"))a.phase=args[++i].toUpperCase();else if(x.equals("--instance"))a.instance=args[++i];else if(x.equals("--seed"))a.seed=Long.parseLong(args[++i]);else if(x.equals("--treatment"))a.treatment=Integer.parseInt(args[++i]);else if(x.equals("--max-fes"))a.maxFes=Integer.parseInt(args[++i]);else if(x.equals("--project-root"))a.projectRoot=Paths.get(args[++i]);else if(x.equals("--output"))a.output=Paths.get(args[++i]);else throw new IllegalArgumentException("unknown arg "+x);}return a;}
  }
}
