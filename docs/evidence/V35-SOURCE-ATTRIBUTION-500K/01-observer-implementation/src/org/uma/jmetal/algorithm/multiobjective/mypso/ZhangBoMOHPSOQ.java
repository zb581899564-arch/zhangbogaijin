package org.uma.jmetal.algorithm.multiobjective.mypso;

import org.uma.jmetal.algorithm.impl.AbstractParticleSwarmOptimization;
import org.uma.jmetal.algorithm.multiobjective.mypso.util.Experience;
import org.uma.jmetal.algorithm.multiobjective.mypso.util.ReplayBuffer;
import org.uma.jmetal.algorithm.multiobjective.mypso.util.SO;
import org.uma.jmetal.algorithm.multiobjective.mypso.util.ST;
import org.uma.jmetal.algorithm.multiobjective.mypso.p8.P8AblationProfile;
import org.uma.jmetal.algorithm.multiobjective.mypso.zhangbo.ZhangBoCfvfDiagnostics;
import org.uma.jmetal.algorithm.multiobjective.mypso.zhangbo.ZhangBoCfvfResult;
import org.uma.jmetal.algorithm.multiobjective.mypso.zhangbo.ZhangBoCfvfUpdater;
import org.uma.jmetal.algorithm.multiobjective.mypso.zhangbo.ZhangBoCriticalFactoryNeighborhoods;
import org.uma.jmetal.algorithm.multiobjective.mypso.zhangbo.ZhangBoArchiveEntry;
import org.uma.jmetal.algorithm.multiobjective.mypso.zhangbo.ZhangBoArchiveBounds;
import org.uma.jmetal.algorithm.multiobjective.mypso.zhangbo.ZhangBoBaselineUpdater;
import org.uma.jmetal.algorithm.multiobjective.mypso.zhangbo.ZhangBoDualQCoordinationConfiguration;
import org.uma.jmetal.algorithm.multiobjective.mypso.zhangbo.ZhangBoDualQCoordinator;
import org.uma.jmetal.algorithm.multiobjective.mypso.zhangbo.ZhangBoGlobalSearchConfiguration;
import org.uma.jmetal.algorithm.multiobjective.mypso.zhangbo.ZhangBoIncrementalParetoArchive;
import org.uma.jmetal.algorithm.multiobjective.mypso.zhangbo.ZhangBoEvaluatedPddrSelector;
import org.uma.jmetal.algorithm.multiobjective.mypso.zhangbo.PddrSelectionMode;
import org.uma.jmetal.algorithm.multiobjective.mypso.zhangbo.ZhangBoEventLog;
import org.uma.jmetal.algorithm.multiobjective.mypso.zhangbo.ZhangBoFormalHmopsoQgsConfiguration;
import org.uma.jmetal.algorithm.multiobjective.mypso.zhangbo.ZhangBoLineageCoordinator;
import org.uma.jmetal.algorithm.multiobjective.mypso.zhangbo.ZhangBoLineageMemory;
import org.uma.jmetal.algorithm.multiobjective.mypso.zhangbo.ZhangBoLineageTag;
import org.uma.jmetal.algorithm.multiobjective.mypso.zhangbo.ZhangBoMachineVectorSupport;
import org.uma.jmetal.algorithm.multiobjective.mypso.zhangbo.ZhangBoQgController;
import org.uma.jmetal.algorithm.multiobjective.mypso.zhangbo.ZhangBoPersonalLeaderDecision;
import org.uma.jmetal.algorithm.multiobjective.mypso.zhangbo.ZhangBoQpAction;
import org.uma.jmetal.algorithm.multiobjective.mypso.zhangbo.ZhangBoQpBranchTag;
import org.uma.jmetal.algorithm.multiobjective.mypso.zhangbo.ZhangBoQpController;
import org.uma.jmetal.algorithm.multiobjective.mypso.zhangbo.ZhangBoQpCandidateSelector;
import org.uma.jmetal.algorithm.multiobjective.mypso.zhangbo.ZhangBoQpConfiguration;
import org.uma.jmetal.algorithm.multiobjective.mypso.zhangbo.ZhangBoQpLineageState;
import org.uma.jmetal.algorithm.multiobjective.mypso.zhangbo.ZhangBoResourceDomain;
import org.uma.jmetal.algorithm.multiobjective.mypso.zhangbo.ZhangBoResourceVelocity;
import org.uma.jmetal.algorithm.multiobjective.mypso.zhangbo.ZhangBoProblemContext;
import org.uma.jmetal.algorithm.multiobjective.mypso.zhangbo.ZhangBoProblemContexts;
import org.uma.jmetal.algorithm.multiobjective.mypso.zhangbo.ZhangBoSubSwarm;
import org.uma.jmetal.algorithm.multiobjective.mypso.zhangbo.ZhangBoSubSwarmSemantics;
import org.uma.jmetal.algorithm.multiobjective.mypso.zhangbo.ZhangBoSolutionSupport;
import org.uma.jmetal.algorithm.multiobjective.mypso.zhangbo.audit.ZhangBoCmaxAudit;
import org.uma.jmetal.algorithm.multiobjective.mypso.zhangbo.cata.ZhangBoBottleneckClassifier;
import org.uma.jmetal.algorithm.multiobjective.mypso.zhangbo.cata.ZhangBoBottleneck;
import org.uma.jmetal.algorithm.multiobjective.mypso.zhangbo.cata.ZhangBoCaTaConfiguration;
import org.uma.jmetal.algorithm.multiobjective.mypso.zhangbo.cata.ZhangBoCaTaContext;
import org.uma.jmetal.algorithm.multiobjective.mypso.zhangbo.cata.ZhangBoCaTaController;
import org.uma.jmetal.algorithm.multiobjective.mypso.zhangbo.cata.ZhangBoCaTaPhase;
import org.uma.jmetal.algorithm.multiobjective.mypso.zhangbo.cata.ZhangBoFactoryNeedSelector;
import org.uma.jmetal.algorithm.multiobjective.mypso.zhangbo.cata.ZhangBoLocalSearchAcceptance;
import org.uma.jmetal.algorithm.multiobjective.mypso.zhangbo.cata.ZhangBoNaturalRecoveryGate;
import org.uma.jmetal.algorithm.multiobjective.mypso.zhangbo.cata.ZhangBoNeighborhoodCandidateGateway;
import org.uma.jmetal.algorithm.multiobjective.mypso.zhangbo.cata.ZhangBoPreEvaluatedTag;
import org.uma.jmetal.algorithm.multiobjective.mypso.zhangbo.neighborhood.ZhangBoFatigueFocus;
import org.uma.jmetal.algorithm.multiobjective.mypso.zhangbo.neighborhood.ZhangBoNeighborhoodId;
import org.uma.jmetal.algorithm.multiobjective.mypso.zhangbo.neighborhood.ZhangBoNeighborhoodRequest;
import org.uma.jmetal.algorithm.multiobjective.mypso.zhangbo.neighborhood.ZhangBoNeighborhoodSuite;
import org.uma.jmetal.algorithm.multiobjective.mypso.v35.V35Bottleneck;
import org.uma.jmetal.algorithm.multiobjective.mypso.v35.V35CaTaContext;
import org.uma.jmetal.algorithm.multiobjective.mypso.v35.V35CaTaLiteController;
import org.uma.jmetal.algorithm.multiobjective.mypso.v35.V35MacroNeighborhood;
import org.uma.jmetal.algorithm.multiobjective.mypso.v35.V35PassiveEvaluationArchive;
import org.uma.jmetal.algorithm.multiobjective.mypso.v35.V35SubSwarmRole;
import org.uma.jmetal.algorithm.multiobjective.mypso.v35.V35DscrSanitizer;
import org.uma.jmetal.algorithm.multiobjective.mypso.v35.V35DscrTeacherCache;
import org.uma.jmetal.algorithm.multiobjective.mypso.v35.V35MacroCandidateGateway;
import org.uma.jmetal.algorithm.multiobjective.mypso.v35.V35SocialKnowledgeSnapshot;
import org.uma.jmetal.algorithm.multiobjective.mypso.v35.V35SocialTeacher;
import org.uma.jmetal.algorithm.multiobjective.mypso.v35.V35PressureBottleneckClassifier;
import org.uma.jmetal.algorithm.multiobjective.mypso.v35.V35ShadowDiagnosisAudit;
import org.uma.jmetal.algorithm.multiobjective.mypso.v35.V35EvaluationSourceContext;
import org.uma.jmetal.algorithm.multiobjective.mypso.v35.V35Fc52LifecycleAudit;
import org.uma.jmetal.algorithm.multiobjective.mypso.v35.V35Fc6LocalCandidateAudit;
import org.uma.jmetal.problem.Problem;
import org.uma.jmetal.problem.multiobjective.dfsp.DHFSP;
import org.uma.jmetal.problem.multiobjective.dfsp.ZhangBoEDHHFSPW;
import org.uma.jmetal.problem.multiobjective.dfsp.fatigue.ZhangBoFatigueEvaluationResult;
import org.uma.jmetal.problem.multiobjective.dfsp.fatigue.ZhangBoFatigueOperationRecord;
import org.uma.jmetal.problem.multiobjective.dfsp.fatigue.ZhangBoCanonicalProductionProblem;
import org.uma.jmetal.solution.PermutationSolution;
import org.uma.jmetal.solution.impl.DefaultIntegerPermutationSolution;
import org.uma.jmetal.util.evaluator.SolutionListEvaluator;
import org.uma.jmetal.util.pseudorandom.JMetalRandom;
import org.uma.jmetal.util.pseudorandom.PseudoRandomGenerator;
import org.uma.jmetal.util.pseudorandom.impl.JavaRandomGenerator;

import java.util.*;
import java.util.stream.Collectors;


/**
 * Class implementing the OMOPSO algorithm
 */

@SuppressWarnings("serial")
public class ZhangBoMOHPSOQ extends AbstractParticleSwarmOptimization<PermutationSolution<Integer>, List<PermutationSolution<Integer>>> {

    public static List<PermutationSolution<Integer>> GbestsetG1 = new ArrayList<>();
    public static List<PermutationSolution<Integer>> GbestsetG2 = new ArrayList<>();
    public static List<PermutationSolution<Integer>> GbestsetG3 = new ArrayList<>();
    public static List<PermutationSolution<Integer>> GbestsetG4 = new ArrayList<>();

    public static int actionnum = 3;
    public static int indextest;
    private Problem<PermutationSolution<Integer>> problem;
    private final SolutionListEvaluator<PermutationSolution<Integer>> evaluator;

    private int swarmSize;
    private int upSize;
    private int upNewSize;
    private int centralSize;
    private int downSize;

    //    private int archiveSize;
    private int maxIterations;

    private ArrayList<List<PermutationSolution<Integer>>> tempSwarm;
    private List<PermutationSolution<Integer>> globallyOptimalIndividual;

    private List<PermutationSolution<Integer>> groupU1Solution;
    private List<PermutationSolution<Integer>> groupUNewSolution;
    private List<PermutationSolution<Integer>> groupC2Solution;
    private List<PermutationSolution<Integer>> groupD3Solution;

    private ArrayList<List<PermutationSolution<Integer>>> upGroup1Population;
    private ArrayList<List<PermutationSolution<Integer>>> upNewGroup1Population;
    private ArrayList<List<PermutationSolution<Integer>>> centralGroup2Population;
    private ArrayList<List<PermutationSolution<Integer>>> downGroup3Population;

    private List<PermutationSolution<Integer>> upGr1HisOptIndividual;
    private List<PermutationSolution<Integer>> upNewGr1HisOptIndividual;
    private List<PermutationSolution<Integer>> centralGr2HisOptIndividual;
    private List<PermutationSolution<Integer>> downGr3HisOptIndividual;

    private List<PermutationSolution<Integer>> all3GlobalOptIndividuals;

    private double Rand_k;
    private double Cross_c;
    private double Mutation_m;
    private int currentIteration;
    private JMetalRandom randomGenerator;
    private ArrayList<List<Integer>> action;
    private double Qnums;
    private int numberOfFactories;
    private double CrossoverRates4worker;
    private double CrossoverRates4machine;
    private double mutationRate4worker;
    private double mutationRate4machine;
    private int localsearch;
    private double gamma = 0.8, tl = 0.85;//之前是tl=0.85

    private int t = 0;

    /** P6 switches are disabled in the original constructor to preserve P4.1 exactly. */
    private ZhangBoGlobalSearchConfiguration globalSearchConfiguration =
            ZhangBoGlobalSearchConfiguration.disabled();
    private ZhangBoFormalHmopsoQgsConfiguration formalBaselineConfiguration =
            ZhangBoFormalHmopsoQgsConfiguration.disabled();
    private ZhangBoQgController zhangBoQgController;
    private ZhangBoCfvfUpdater zhangBoCfvfUpdater;
    private ZhangBoBaselineUpdater zhangBoBaselineUpdater;
    private ZhangBoResourceDomain zhangBoResourceDomain;
    private ZhangBoProblemContext problemContext;
    private PseudoRandomGenerator zhangBoP6Random;
    private final Map<ZhangBoSubSwarm, ZhangBoQgController.Selection> pendingQgSelections =
            new EnumMap<>(ZhangBoSubSwarm.class);
    private final Map<ZhangBoSubSwarm, List<PermutationSolution<Integer>>> pendingQgBefore =
            new EnumMap<>(ZhangBoSubSwarm.class);
    private final ZhangBoEventLog zhangBoP6Events = new ZhangBoEventLog();
    private long fullEvaluationCount;
    /** Number of completed outer PSO generations; local-search FE never increments it. */
    private long completedOuterGenerations;
    /** Per-global-offspring-round counter (one increment per updatePosition call); it drives
     *  dual-Q P/G block progress so that blockLength is measured in rounds/generations,
     *  not in Q_Times-sized outer cycles. */
    private long dualQRoundCounter;
    private long dualQWarmupEndOuterGeneration = -1L;
    private long cfvfOffspringCount;
    private long cfvfRepairCount;
    private long cfvfInitializationCorrections;
    private long authorUpdateResourceCorrections;
    private long baselineUpdateEventCount;
    private long fixedNeighborhoodEventCount;
    private long formalBaselineOuterCycles;
    private long formalBaselineQgRounds;
    private boolean allowTerminalPartialFormalQPhase;
    private long formalCriticalFactorySwapEvaluations;
    private long formalCriticalFactoryInsertEvaluations;
    private long formalOriginalNeighborhoodEvaluations;
    private int formalQRoundIndex = -1;
    private ZhangBoEvaluatedPddrSelector zhangBoEvaluatedPddrSelector;
    private ZhangBoLineageCoordinator zhangBoLineageCoordinator;
    private List<PermutationSolution<Integer>> pendingPddrParents = new ArrayList<>();
    private List<List<PermutationSolution<Integer>>> pendingPddrParentHistories = new ArrayList<>();
    private List<List<PermutationSolution<Integer>>> pendingPddrOffspringHistories = new ArrayList<>();
    private final ZhangBoEventLog zhangBoPddrEvents = new ZhangBoEventLog();
    private long evaluatedPddrSelections;
    private ZhangBoQpController zhangBoQpController;
    private ZhangBoQpCandidateSelector zhangBoArchivePersonalLeaderSelector;
    private final Map<Long, ZhangBoQpController.Selection> pendingQpSelections =
            new LinkedHashMap<>();
    private final Map<Long, ZhangBoPersonalLeaderDecision> pendingPersonalLeaders =
            new LinkedHashMap<>();
    private ZhangBoDualQCoordinator zhangBoDualQCoordinator;
    private ZhangBoDualQCoordinator.Decision pendingDualQDecision;
    /** V35-FC-4: per-group count of offspring that executed a gbest-derived CFVF action. */
    private final Map<ZhangBoSubSwarm, Long> pendingCfvfGbestContrib =
            new EnumMap<>(ZhangBoSubSwarm.class);
    /** V35-FC-4: branches whose offspring executed a pbest-derived CFVF action. */
    private final Map<ZhangBoSubSwarm, java.util.Set<Long>> pendingCfvfPbestContribBranches =
            new EnumMap<>(ZhangBoSubSwarm.class);
    /** V35-FC-5: read-only CFVF GIR audit; never influences behaviour. */
    private org.uma.jmetal.algorithm.multiobjective.mypso.v35.V35CfvfGirAudit
            v35CfvfGirAudit = new org.uma.jmetal.algorithm.multiobjective.mypso.v35.V35CfvfGirAudit();
    /** FC-TIME-1: per-cycle module timing lines (pure observation; empty unless profiling enabled). */
    private final java.util.List<String> v35ModulePerCycleLines = new java.util.ArrayList<>();
    /** FC-5: read-only Cmax lifecycle audit (four-layer funnel + G1 conditional GIR + lineage). */
    private final org.uma.jmetal.algorithm.multiobjective.mypso.v35.V35CmaxLifecycleAudit
            v35CmaxLifecycleAudit =
            new org.uma.jmetal.algorithm.multiobjective.mypso.v35.V35CmaxLifecycleAudit();
    private final ZhangBoEventLog zhangBoDualQEvents = new ZhangBoEventLog();
    private final Map<ZhangBoDualQCoordinator.Phase, Long> dualQPhaseCounts =
            new EnumMap<>(ZhangBoDualQCoordinator.Phase.class);
    private String pendingQgTableHashBefore;
    private String pendingQpTableHashBefore;
    private long pendingQgSelectionsBefore;
    private long pendingQgUpdatesBefore;
    private long pendingQpActionsBefore;
    private long pendingQpTransitionsBefore;
    private List<PermutationSolution<Integer>> initialSwarmOverride;
    private ZhangBoCaTaController zhangBoCaTaController;
    private V35CaTaLiteController v35CaTaLiteController;
    private V35DscrTeacherCache v35DscrTeacherCache;
    private V35SocialKnowledgeSnapshot pendingV35SocialSnapshot;
    private long v35DscrDecisionCycle;
    private V35MacroCandidateGateway v35MacroCandidateGateway;
    private ZhangBoBottleneckClassifier zhangBoBottleneckClassifier;
    private V35PressureBottleneckClassifier v35PressureBottleneckClassifier;
    private V35ShadowDiagnosisAudit v35ShadowDiagnosisAudit;
    private ZhangBoCanonicalProductionProblem v35ShadowProblem;
    private final ZhangBoEventLog v35PressureDiagnosisEvents = new ZhangBoEventLog();
    private ZhangBoFactoryNeedSelector zhangBoFactoryNeedSelector;
    private ZhangBoNeighborhoodCandidateGateway zhangBoNeighborhoodCandidateGateway;
    private PseudoRandomGenerator zhangBoCaTaRandom;
    private final List<PendingCaTaLocalCandidate> pendingCaTaLocalCandidates = new ArrayList<>();
    private final ZhangBoEventLog zhangBoCaTaEvents = new ZhangBoEventLog();
    private long caTaTestCalls;
    /** V35-FC-1 audit: N3/N4 previews fed by FM3 actual structure vs PT0 proxy. */
    private long v35Fm3StructurePreviews;
    private long v35ProxyStructurePreviews;
    private long caTaApplyCalls;
    private long caTaFullEvaluations;
    private boolean caTaRewardsSettled;
    private long authorRandomInvocation;
    /** Optional observer only; null is the production default and preserves the historical path. */
    private ZhangBoCmaxAudit cmaxAudit;
    /** V35-P17 passive observer only; null is the production default (pure bypass, no decisions read it). */
    private V35PassiveEvaluationArchive v35PassiveArchive;

    /**
     * Constructor
     */
    public ZhangBoMOHPSOQ(int factories, double crossoverRate, double mutationRate, double rand_k,
                         Problem<PermutationSolution<Integer>> problem, SolutionListEvaluator<PermutationSolution<Integer>> evaluator,
                         int swarmSize, int maxIterations, int upSize, int centralSize, int downSize, int upNewSize, double DERate, double DEcrossoverRates, double DEmutationRate, double Qnums, double CrossoverRates4worker, double CrossoverRates4machine,
                         double mutationRate4worker, double mutationRate4machine, int localsearch
    ) {
        this.problem = problem;
        this.evaluator = evaluator;

        this.swarmSize = swarmSize;
        this.maxIterations = maxIterations;
        this.numberOfFactories = factories;
        this.upSize = upSize;
        this.upNewSize = upNewSize;
        this.centralSize = centralSize;
        this.downSize = downSize;
        this.Mutation_m = mutationRate;
        this.Cross_c = crossoverRate;
        this.Rand_k = rand_k;
        //     this.archiveSize = archiveSize ;
        this.Qnums = Qnums;
        this.gamma = DEcrossoverRates;
        this.tl = DEmutationRate;
        this.CrossoverRates4worker = CrossoverRates4worker;
        this.CrossoverRates4machine = CrossoverRates4machine;
        this.mutationRate4worker = mutationRate4worker;
        this.mutationRate4machine = mutationRate4machine;
        this.localsearch = localsearch;
        tempSwarm = new ArrayList<List<PermutationSolution<Integer>>>(swarmSize);
//        tempSwarm = new ArrayList<List<PermutationSolution<Integer>>>(50);

        globallyOptimalIndividual = new ArrayList<PermutationSolution<Integer>>();

        groupU1Solution = new ArrayList<>(upSize);
        groupUNewSolution = new ArrayList<>(upNewSize);
        groupC2Solution = new ArrayList<>(centralSize);
        groupD3Solution = new ArrayList<>(downSize);

        upGroup1Population = new ArrayList<List<PermutationSolution<Integer>>>(upSize);
        upNewGroup1Population = new ArrayList<List<PermutationSolution<Integer>>>(upNewSize);
        centralGroup2Population = new ArrayList<List<PermutationSolution<Integer>>>(centralSize);
        downGroup3Population = new ArrayList<List<PermutationSolution<Integer>>>(downSize);

        upGr1HisOptIndividual = new ArrayList<PermutationSolution<Integer>>(upSize);
        upNewGr1HisOptIndividual = new ArrayList<PermutationSolution<Integer>>(upNewSize);
        centralGr2HisOptIndividual = new ArrayList<PermutationSolution<Integer>>(centralSize);
        downGr3HisOptIndividual = new ArrayList<PermutationSolution<Integer>>(downSize);
        all3GlobalOptIndividuals = new ArrayList<PermutationSolution<Integer>>(4);

        randomGenerator = JMetalRandom.getInstance();
//        System.out.println("MOPSODivSubDE 构造函数参数：");
//        System.out.println("factories: " + factories);
//        System.out.println("crossoverRate: " + crossoverRate);
//        System.out.println("mutationRate: " + mutationRate);
//        System.out.println("rand_k: " + rand_k);
//        System.out.println("problem: " + problem.getClass().getSimpleName());  // 打印问题类名
//        System.out.println("evaluator: " + evaluator.getClass().getSimpleName());  // 打印评估器类名
//        System.out.println("swarmSize: " + swarmSize);
//        System.out.println("maxIterations: " + maxIterations);
//        System.out.println("upSize: " + upSize);
//        System.out.println("centralSize: " + centralSize);
//        System.out.println("downSize: " + downSize);
//        System.out.println("upNewSize: " + upNewSize);
//        System.out.println("DERate: " + DERate);
//        System.out.println("DEcrossoverRates: " + DEcrossoverRates);
//        System.out.println("DEmutationRate: " + DEmutationRate);
//        System.out.println("Qnums: " + Qnums);
//        System.out.println("CrossoverRates4worker: " + CrossoverRates4worker);
//        System.out.println("CrossoverRates4machine: " + CrossoverRates4machine);
//        System.out.println("mutationRate4worker: " + mutationRate4worker);
//        System.out.println("mutationRate4machine: " + mutationRate4machine);
        //        sleep();
    }

    public ZhangBoMOHPSOQ(int factories, double crossoverRate, double mutationRate, double rand_k,
                         Problem<PermutationSolution<Integer>> problem, SolutionListEvaluator<PermutationSolution<Integer>> evaluator,
                         int swarmSize, int maxIterations, int upSize, int centralSize, int downSize, int upNewSize,
                         double DERate, double DEcrossoverRates, double DEmutationRate, double Qnums,
                         double CrossoverRates4worker, double CrossoverRates4machine,
                         double mutationRate4worker, double mutationRate4machine, int localsearch,
                         ZhangBoGlobalSearchConfiguration globalSearchConfiguration) {
        this(factories, crossoverRate, mutationRate, rand_k, problem, evaluator, swarmSize,
                maxIterations, upSize, centralSize, downSize, upNewSize, DERate,
                DEcrossoverRates, DEmutationRate, Qnums, CrossoverRates4worker,
                CrossoverRates4machine, mutationRate4worker, mutationRate4machine, localsearch,
                globalSearchConfiguration, ZhangBoFormalHmopsoQgsConfiguration.disabled());
    }

    public ZhangBoMOHPSOQ(int factories, double crossoverRate, double mutationRate, double rand_k,
                         Problem<PermutationSolution<Integer>> problem,
                         SolutionListEvaluator<PermutationSolution<Integer>> evaluator,
                         int swarmSize, int maxIterations, int upSize, int centralSize,
                         int downSize, int upNewSize, double DERate, double DEcrossoverRates,
                         double DEmutationRate, double Qnums, double CrossoverRates4worker,
                         double CrossoverRates4machine, double mutationRate4worker,
                         double mutationRate4machine, int localsearch,
                         ZhangBoGlobalSearchConfiguration globalSearchConfiguration,
                         ZhangBoFormalHmopsoQgsConfiguration formalBaselineConfiguration) {
        this(factories, crossoverRate, mutationRate, rand_k, problem, evaluator, swarmSize,
                maxIterations, upSize, centralSize, downSize, upNewSize, DERate,
                DEcrossoverRates, DEmutationRate, Qnums, CrossoverRates4worker,
                CrossoverRates4machine, mutationRate4worker, mutationRate4machine, localsearch);
        configureFormalBaseline(formalBaselineConfiguration);
        configureGlobalSearch(globalSearchConfiguration);
    }

    private void configureFormalBaseline(
            ZhangBoFormalHmopsoQgsConfiguration configuration) {
        if (configuration == null) {
            throw new IllegalArgumentException("formalBaselineConfiguration cannot be null");
        }
        if (configuration.isEnabled()) {
            requireSame("randomCoefficientUpperBound", Rand_k,
                    configuration.getRandomCoefficientUpperBound());
            requireSame("faCrossover", Cross_c, configuration.getFaCrossover());
            requireSame("maCrossover", CrossoverRates4machine,
                    configuration.getMaCrossover());
            requireSame("waCrossover", CrossoverRates4worker,
                    configuration.getWaCrossover());
            requireSame("faMutation", Mutation_m, configuration.getFaMutation());
            requireSame("maMutation", mutationRate4machine,
                    configuration.getMaMutation());
            requireSame("waMutation", mutationRate4worker,
                    configuration.getWaMutation());
            requireSame("qTimes", Qnums, configuration.getQTimes());
            if (localsearch != configuration.getLocalSearchTimes()) {
                throw new IllegalArgumentException("localSearchTimes runtime=" + localsearch
                        + ", configuration=" + configuration.getLocalSearchTimes());
            }
            requireSame("gamma", gamma, configuration.getGamma());
            requireSame("epsilon", tl, configuration.getEpsilon());
        }
        this.formalBaselineConfiguration = configuration;
    }

    private static void requireSame(String name, double runtime, double configured) {
        if (Double.compare(runtime, configured) != 0) {
            throw new IllegalArgumentException(name + " runtime=" + runtime
                    + ", configuration=" + configured);
        }
    }

    private void configureGlobalSearch(ZhangBoGlobalSearchConfiguration configuration) {
        if (configuration == null) {
            throw new IllegalArgumentException("globalSearchConfiguration cannot be null");
        }
        this.globalSearchConfiguration = configuration;
        // FC-6B REGION_AWARE assigns the four physical slots according to the
        // registered region capacities (15/55/15/15).  The historical
        // bootstrap uses 20/40/20/20, so leaving these fields untouched would
        // make the first region-aware survivor partition fail its own
        // capacity invariant.  Switch the runtime slot capacities before the
        // first updateVelocity call; ArrayList capacities above are only
        // allocation hints and do not encode the semantic sizes.
        if (configuration.getPddrSelectionMode() == PddrSelectionMode.REGION_AWARE) {
            if (swarmSize != 100) {
                throw new IllegalArgumentException(
                        "REGION_AWARE requires swarmSize=100 for capacities 15/55/15/15");
            }
            this.upSize = 15;
            this.centralSize = 55;
            this.downSize = 15;
            this.upNewSize = 15;
        }
        boolean needsP8Runtime = configuration.isQgEnabled()
                || configuration.isResourceFlightEnabled()
                || configuration.isEvaluatedPddrEnabled()
                || configuration.isLineageArchiveEnabled()
                || configuration.isQpEnabled()
                || configuration.isLocalSearchEnabled()
                || configuration.isReplayableAuthorRandomEnabled();
        if (!needsP8Runtime) {
            return;
        }
        boolean needsProblemData = configuration.isResourceFlightEnabled()
                || configuration.isEvaluatedPddrEnabled()
                || configuration.isLineageArchiveEnabled()
                || configuration.isQpEnabled()
                || configuration.isLocalSearchEnabled();
        problemContext = ZhangBoProblemContexts.resolve(problem);
        if (needsProblemData && problemContext == null) {
            throw new IllegalArgumentException(
                    "P6.0/P6.1 require a ZhangBoProblemContext implementation");
        }
        ZhangBoProblemContext fatigueProblem = problemContext;
        if (configuration.getP8AblationProfile() == null && configuration.isQgEnabled()
                && (fatigueProblem == null || fatigueProblem.getFatigueParameters() == null
                || fatigueProblem.getFatigueParameters().isZeroImpact())) {
            throw new IllegalArgumentException(
                    "P6.0/P6.1 require an enabled nonzero P5 fatigue parameter manifest");
        }
        zhangBoP6Random = new JavaRandomGenerator(configuration.getSeed());
        if (configuration.isQgEnabled()) {
            zhangBoQgController = new ZhangBoQgController(zhangBoP6Random,
                    configuration.getQEpsilon(), configuration.getQAlpha(), configuration.getQGamma());
            // V35-P10.1: directional top-k teacher pool for boundary sub-swarms (FULL-only).
            zhangBoQgController.setDirectionalTeacherPool(
                    configuration.isDirectionalTeacherPoolEnabled(),
                    configuration.getTeacherPoolSize());
        }
        if (configuration.isResourceFlightEnabled()) {
            zhangBoResourceDomain = new ZhangBoResourceDomain(fatigueProblem.getFatigueInstanceData());
            zhangBoCfvfUpdater = new ZhangBoCfvfUpdater();
            if (configuration.isStructuredBaselineEnabled()) {
                zhangBoBaselineUpdater = new ZhangBoBaselineUpdater();
            }
        }
        if (configuration.isEvaluatedPddrEnabled()) {
            zhangBoEvaluatedPddrSelector = new ZhangBoEvaluatedPddrSelector();
        }
        if (configuration.isLineageArchiveEnabled()) {
            zhangBoLineageCoordinator = new ZhangBoLineageCoordinator(
                    configuration.getPersonalArchiveConfiguration(),
                    configuration.getP8AblationProfile() != null
                            && configuration.getP8AblationProfile().isAuthorDiagnostic());
        }
        if (configuration.isQpEnabled()) {
            long qpSeed = configuration.getSeed() ^ 0x515042455354L;
            zhangBoQpController = new ZhangBoQpController(
                    configuration.getQpConfiguration(),
                    configuration.getPersonalArchiveConfiguration(),
                    new JavaRandomGenerator(qpSeed), qpSeed,
                    configuration.getP8AblationProfile() != null
                            && configuration.getP8AblationProfile().isAuthorDiagnostic());
            zhangBoDualQCoordinator = new ZhangBoDualQCoordinator(
                    configuration.getDualQCoordinationConfiguration());
        }
        if (usesArchivePersonalLeader() && !configuration.isQpEnabled()) {
            zhangBoArchivePersonalLeaderSelector = new ZhangBoQpCandidateSelector(
                    ZhangBoQpConfiguration.standard(),
                    configuration.getPersonalArchiveConfiguration());
        }
        // The formal baseline loop always ends each outer cycle with the inherited
        // inter-factory + O1-O9 search (runFormalInheritedLocalSearch), which requires
        // the neighbourhood candidate gateway even when CFVF replaces the structured
        // baseline update (V35 A2/A3 rungs run the formal loop with caTa disabled).
        boolean needsFormalLocalSearch = formalBaselineConfiguration.isEnabled();
        if (configuration.isLocalSearchEnabled() || needsFormalLocalSearch) {
            long caTaSeed = configuration.getSeed() ^ ZhangBoCaTaConfiguration.DOMAIN_SEED;
            zhangBoCaTaRandom = new JavaRandomGenerator(caTaSeed);
            if (configuration.isCaTaEnabled()) {
                zhangBoCaTaController = new ZhangBoCaTaController(
                        configuration.getCaTaConfiguration(),
                        configuration.getP8AblationProfile() == null
                                || configuration.getP8AblationProfile().isCostCreditEnabled());
                if (configuration.isV35CaTaLiteEnabled()) {
                    v35CaTaLiteController = new V35CaTaLiteController(
                            globalSearchConfiguration.getV35CaTaLiteConfiguration());
                    v35MacroCandidateGateway = new V35MacroCandidateGateway();
                    v35PressureBottleneckClassifier = new V35PressureBottleneckClassifier();
                    if (configuration.getV35BottleneckDiagnosis().isShadowAuditEnabled()) {
                        if (!(problem instanceof ZhangBoCanonicalProductionProblem)) {
                            throw new IllegalArgumentException(
                                    "v3.5 shadow diagnosis requires canonical production problem");
                        }
                        ZhangBoCanonicalProductionProblem canonical =
                                (ZhangBoCanonicalProductionProblem) problem;
                        v35ShadowDiagnosisAudit = new V35ShadowDiagnosisAudit(
                                configuration.getV35BottleneckDiagnosis());
                        v35ShadowProblem = new ZhangBoCanonicalProductionProblem(
                                canonical.getInstance(), canonical.getParameters(), canonical.getMode(),
                                new org.uma.jmetal.problem.multiobjective.dfsp.fatigue.ZhangBoFatigueEvaluator(),
                                new org.uma.jmetal.problem.multiobjective.dfsp.decoder.EvaluationCounter(),
                                configuration.getSeed() ^ 0x534841444F57L,
                                canonical.getShiftConfiguration(), canonical.getSetupModel());
                    }
                }
            }
            zhangBoBottleneckClassifier = new ZhangBoBottleneckClassifier(
                    fatigueProblem.getFatigueParameters().getWarningThreshold());
            zhangBoFactoryNeedSelector = new ZhangBoFactoryNeedSelector();
            zhangBoNeighborhoodCandidateGateway = new ZhangBoNeighborhoodCandidateGateway();
            zhangBoCaTaEvents.add("initialize:semanticTag="
                    + globalSearchConfiguration.getSemanticTag() + ",seed=" + caTaSeed
                    + ",subSwarmSemanticsVersion=" + ZhangBoSubSwarmSemantics.VERSION
                    + ",subSwarmRoleMappingSha256=" + ZhangBoSubSwarmSemantics.mappingHash());
        }
        if (configuration.isDscrEnabled()) {
            v35DscrTeacherCache = new V35DscrTeacherCache();
        }
    }

    /**
     * The paper baseline has a nested Q-search loop that the jMetal PSO template cannot
     * express: one outer generation contains Q_Times complete population updates and is
     * followed by the inherited inter-factory search plus LS_Times passes of O1-O9.  Only
     * the structured baseline uses this override.  CFVF/Qp/CA-TA profiles continue to use
     * the v2 one-global-child-per-particle loop.
     */
    @Override
    public void run() {
        if (!formalBaselineConfiguration.isEnabled()) {
            super.run();
            finishCmaxAudit();
            return;
        }
        runFormalHmopsoQgsBaseline();
        finishCmaxAudit();
    }

    private void finishCmaxAudit() {
        if (cmaxAudit != null) {
            cmaxAudit.finish(fullEvaluationCount, getSwarm(), globallyOptimalIndividual);
        }
    }

    /**
     * V35-FC-2 hard local-search FE ceiling for the current outer cycle.
     * Negative means the legacy semantics: only the global {@code maxIterations}
     * bounds the local search (the A4-PREFINAL archive behaviour).
     */
    private long localSearchFeCeiling = -1L;

    /**
     * Opens the local-FE budget window for one outer cycle that consumed
     * {@code globalFeThisCycle} complete evaluations in its Q phase.  The
     * inter-factory local search and CA-TA-Lite share the resulting ceiling;
     * neither may push {@code fullEvaluationCount} past
     * {@code #localFeHardLimit()}.
     */
    private void beginLocalFeBudgetWindow(long globalFeThisCycle) {
        org.uma.jmetal.algorithm.multiobjective.mypso.v35.V35LocalFeBudgetConfiguration budget =
                globalSearchConfiguration.getLocalFeBudget();
        if (budget == null || globalFeThisCycle <= 0L) {
            localSearchFeCeiling = -1L;
            return;
        }
        double progress = maxIterations <= 0L ? 1.0
                : Math.min(1.0, (double) fullEvaluationCount / (double) maxIterations);
        localSearchFeCeiling = fullEvaluationCount
                + budget.localBudgetFor(progress, globalFeThisCycle);
    }

    /** The effective hard limit for local-search-family evaluations. */
    private long localFeHardLimit() {
        return localSearchFeCeiling < 0L ? maxIterations
                : Math.min(maxIterations, localSearchFeCeiling);
    }

    private void runFormalHmopsoQgsBaseline() {
        List<PermutationSolution<Integer>> swarm = createInitialSwarm();
        super.setSwarm(swarm);
        long tDecode0 = System.nanoTime();
        V35EvaluationSourceContext.begin(V35EvaluationSourceContext.Source.INITIAL_POPULATION);
        try {
            swarm = evaluateSwarm(swarm);
        } finally {
            V35EvaluationSourceContext.end();
        }
        org.uma.jmetal.algorithm.multiobjective.mypso.v35.V35ModuleTimer.record(
                org.uma.jmetal.algorithm.multiobjective.mypso.v35.V35ModuleTimer.DECODE,
                System.nanoTime() - tDecode0, 1L);
        super.setSwarm(swarm);
        initializeVelocity(swarm);
        initializeParticlesMemory(swarm);
        initializeLeader(swarm);
        initProgress();

        long qPhaseEvaluations = (long) formalBaselineConfiguration.getQTimes() * swarmSize;
        while (allowTerminalPartialFormalQPhase
                ? fullEvaluationCount + swarmSize <= maxIterations
                : fullEvaluationCount + qPhaseEvaluations <= maxIterations) {
            long beforeCycle = fullEvaluationCount;
            // FC-5: four-layer Cmax lifecycle audit (pure observation).
            v35CmaxLifecycleAudit.beginCycle((int) formalBaselineOuterCycles + 1,
                    bestCmaxOf(globallyOptimalIndividual));
            // FC-TIME-1: per-cycle timing snapshot (pure observation).
            java.util.Map<String, long[]> cycleStart =
                    org.uma.jmetal.algorithm.multiobjective.mypso.v35.V35ModuleTimer.snapshot();
            java.util.Map<String, long[]> cycleCounterStart =
                    org.uma.jmetal.algorithm.multiobjective.mypso.v35.V35ModuleTimer.counterSnapshot();
            int admittedRounds = allowTerminalPartialFormalQPhase
                    ? (int) Math.min(formalBaselineConfiguration.getQTimes(),
                            (maxIterations - fullEvaluationCount) / swarmSize)
                    : formalBaselineConfiguration.getQTimes();
            long tOther0 = System.nanoTime();
            updateVelocity(swarm);
            org.uma.jmetal.algorithm.multiobjective.mypso.v35.V35ModuleTimer.record(
                    org.uma.jmetal.algorithm.multiobjective.mypso.v35.V35ModuleTimer.OTHER,
                    System.nanoTime() - tOther0, 1L);
            if (globalSearchConfiguration.isLineageArchiveEnabled()
                    && zhangBoLineageCoordinator.getMemories().isEmpty()) {
                List<PermutationSolution<Integer>> initialLineageParents =
                        groupedCurrentSolutions();
                annotateSubSwarmSlots(initialLineageParents);
                zhangBoLineageCoordinator.initialize(initialLineageParents,
                        globallyOptimalIndividual, generationNumber());
            }
            long tCopy0 = System.nanoTime();
            List<PermutationSolution<Integer>> generationParents =
                    ZhangBoSolutionSupport.deepCopySolutions(groupedCurrentSolutions());
            List<List<PermutationSolution<Integer>>> generationParentHistories =
                    groupedAuthorHistories();
            org.uma.jmetal.algorithm.multiobjective.mypso.v35.V35ModuleTimer.record(
                    org.uma.jmetal.algorithm.multiobjective.mypso.v35.V35ModuleTimer.COPY,
                    System.nanoTime() - tCopy0, 1L);
            int completedRounds = 0;
            for (int round = 0; round < admittedRounds; round++) {
                if (fullEvaluationCount + swarmSize > maxIterations) {
                    throw new IllegalStateException(
                            "Formal Q phase budget was admitted but cannot finish all rounds");
                }
                if (round > 0) {
                    long tVelocity = System.nanoTime();
                    updateVelocity(swarm);
                    org.uma.jmetal.algorithm.multiobjective.mypso.v35.V35ModuleTimer.record(
                            org.uma.jmetal.algorithm.multiobjective.mypso.v35.V35ModuleTimer.OTHER,
                            System.nanoTime() - tVelocity, 1L);
                }
                formalQRoundIndex = round;
                // V35-SOURCE-ATTRIBUTION-PATCH: round context (pure observation).
                org.uma.jmetal.algorithm.multiobjective.mypso.v35.V35SourceAttributionObserver.context(generationNumber(), formalBaselineOuterCycles, formalQRoundIndex);
                v35CmaxLifecycleAudit.beginG1Round();
                long tCfvf0 = System.nanoTime();
                updatePosition(swarm);
                org.uma.jmetal.algorithm.multiobjective.mypso.v35.V35ModuleTimer.record(
                        org.uma.jmetal.algorithm.multiobjective.mypso.v35.V35ModuleTimer.CFVF,
                        System.nanoTime() - tCfvf0, 1L);
                // New global offspring must never inherit a local candidate's pre-evaluated tag.
                clearPreEvaluationMarkers(swarm);
                long tDecode1 = System.nanoTime();
                V35EvaluationSourceContext.begin(V35EvaluationSourceContext.Source.GLOBAL_CFVF);
                try {
                    swarm = evaluateSwarm(swarm);
                } finally {
                    V35EvaluationSourceContext.end();
                }
                org.uma.jmetal.algorithm.multiobjective.mypso.v35.V35ModuleTimer.record(
                        org.uma.jmetal.algorithm.multiobjective.mypso.v35.V35ModuleTimer.DECODE,
                        System.nanoTime() - tDecode1, 1L);
                double[] cmaxBySlot = new double[swarm.size()];
                for (int slotIndex = 0; slotIndex < swarm.size(); slotIndex++) {
                    cmaxBySlot[slotIndex] = swarm.get(slotIndex).getObjective(0);
                }
                v35CmaxLifecycleAudit.resolveG1Improvements(cmaxBySlot);
                super.setSwarm(swarm);
                if (globalSearchConfiguration.isQgEnabled()) {
                    long tQg = System.nanoTime();
                    settleOriginalQg(swarm);
                    org.uma.jmetal.algorithm.multiobjective.mypso.v35.V35ModuleTimer.record(
                            org.uma.jmetal.algorithm.multiobjective.mypso.v35.V35ModuleTimer.QG,
                            System.nanoTime() - tQg, 1L);
                }
                if (globalSearchConfiguration.isQpEnabled()) {
                    long tQp = System.nanoTime();
                    settleQp(swarm);
                    org.uma.jmetal.algorithm.multiobjective.mypso.v35.V35ModuleTimer.record(
                            org.uma.jmetal.algorithm.multiobjective.mypso.v35.V35ModuleTimer.QP,
                            System.nanoTime() - tQp, 1L);
                }
                recordDualQCoordination();
                long tDom0 = System.nanoTime();
                appendAndPrunePersonalHistories(swarm);
                org.uma.jmetal.algorithm.multiobjective.mypso.v35.V35ModuleTimer.record(
                        org.uma.jmetal.algorithm.multiobjective.mypso.v35.V35ModuleTimer.DOMINANCE,
                        System.nanoTime() - tDom0, 1L);
                long tArch0 = System.nanoTime();
                updateParticlesMemory(swarm);
                org.uma.jmetal.algorithm.multiobjective.mypso.v35.V35ModuleTimer.record(
                        org.uma.jmetal.algorithm.multiobjective.mypso.v35.V35ModuleTimer.ARCHIVE,
                        System.nanoTime() - tArch0, 1L);
                formalBaselineQgRounds++;
                completedRounds++;
            }
            formalQRoundIndex = -1;
            // V35-SOURCE-ATTRIBUTION-PATCH: cycle context reset (pure observation).
            org.uma.jmetal.algorithm.multiobjective.mypso.v35.V35SourceAttributionObserver.context(generationNumber(), formalBaselineOuterCycles, formalQRoundIndex);
            if (completedRounds == 0) break;
            int auditCycle = (int) formalBaselineOuterCycles + 1;
            double auditAfterQRounds = bestCmaxOf(swarm);

            // Environmental selection is deliberately deferred until all Q rounds and local
            // searches have completed, matching the P4 paper oracle.
            pendingPddrParents = generationParents;
            pendingPddrParentHistories = generationParentHistories;
            long tCopy1 = System.nanoTime();
            pendingPddrOffspringHistories =
                    ZhangBoSolutionSupport.deepCopyHistories(tempSwarm);
            org.uma.jmetal.algorithm.multiobjective.mypso.v35.V35ModuleTimer.record(
                    org.uma.jmetal.algorithm.multiobjective.mypso.v35.V35ModuleTimer.COPY,
                    System.nanoTime() - tCopy1, 1L);
            pendingCaTaLocalCandidates.clear();
            clearPreEvaluationMarkers(swarm);
            annotateSubSwarmSlots(swarm);
            long tAudit0 = System.nanoTime();
            markAlreadyEvaluatedFormalGlobalOffspring(swarm, completedRounds);
            org.uma.jmetal.algorithm.multiobjective.mypso.v35.V35ModuleTimer.record(
                    org.uma.jmetal.algorithm.multiobjective.mypso.v35.V35ModuleTimer.AUDIT,
                    System.nanoTime() - tAudit0, 1L);
            caTaRewardsSettled = true;
            // V35-FC-2: open the shared local-FE window for this cycle before
            // CA-TA-Lite and the inherited local search consume it.
            beginLocalFeBudgetWindow(fullEvaluationCount - beforeCycle);
            double auditAfterCaTa;
            double auditAfterLs;
            if (globalSearchConfiguration.getV35LocalSearchOrder()
                    == org.uma.jmetal.algorithm.multiobjective.mypso.v35.V35LocalSearchOrder
                    .INHERITED_THEN_CATA) {
                long tLs0 = System.nanoTime();
                runFormalInheritedLocalSearch(swarm, problemContext);
                org.uma.jmetal.algorithm.multiobjective.mypso.v35.V35ModuleTimer.record(
                        org.uma.jmetal.algorithm.multiobjective.mypso.v35.V35ModuleTimer.LS,
                        System.nanoTime() - tLs0, 1L);
                auditAfterLs = bestCmaxOf(swarm);
                if (globalSearchConfiguration.isV35CaTaLiteEnabled()) {
                    long tCata = System.nanoTime();
                    runV35CaTaLiteLocalSearch(swarm, problemContext);
                    org.uma.jmetal.algorithm.multiobjective.mypso.v35.V35ModuleTimer.record(
                            org.uma.jmetal.algorithm.multiobjective.mypso.v35.V35ModuleTimer.CATA,
                            System.nanoTime() - tCata, 1L);
                }
                auditAfterCaTa = bestCmaxOf(swarm);
            } else {
                if (globalSearchConfiguration.isV35CaTaLiteEnabled()) {
                    long tCata = System.nanoTime();
                    runV35CaTaLiteLocalSearch(swarm, problemContext);
                    org.uma.jmetal.algorithm.multiobjective.mypso.v35.V35ModuleTimer.record(
                            org.uma.jmetal.algorithm.multiobjective.mypso.v35.V35ModuleTimer.CATA,
                            System.nanoTime() - tCata, 1L);
                }
                auditAfterCaTa = bestCmaxOf(swarm);
                long tLs0 = System.nanoTime();
                runFormalInheritedLocalSearch(swarm, problemContext);
                org.uma.jmetal.algorithm.multiobjective.mypso.v35.V35ModuleTimer.record(
                        org.uma.jmetal.algorithm.multiobjective.mypso.v35.V35ModuleTimer.LS,
                        System.nanoTime() - tLs0, 1L);
                auditAfterLs = bestCmaxOf(swarm);
            }
            long tDecode2 = System.nanoTime();
            V35EvaluationSourceContext.begin(V35EvaluationSourceContext.Source.FINAL_EVALUATE);
            try {
                swarm = evaluateSwarm(swarm);
            } finally {
                V35EvaluationSourceContext.end();
            }
            org.uma.jmetal.algorithm.multiobjective.mypso.v35.V35ModuleTimer.record(
                    org.uma.jmetal.algorithm.multiobjective.mypso.v35.V35ModuleTimer.DECODE,
                    System.nanoTime() - tDecode2, 1L);
            super.setSwarm(swarm);
            v35CmaxLifecycleAudit.observeGeneration(auditCycle - 1, auditAfterQRounds,
                    auditAfterCaTa, auditAfterLs, bestCmaxOf(swarm));
            double auditPool = bestCmaxOf(swarm);
            long tPddr0 = System.nanoTime();
            updateLeaders(swarm);
            org.uma.jmetal.algorithm.multiobjective.mypso.v35.V35ModuleTimer.record(
                    org.uma.jmetal.algorithm.multiobjective.mypso.v35.V35ModuleTimer.PDDR,
                    System.nanoTime() - tPddr0, 1L);
            double auditNext = bestCmaxOf(swarm);
            v35CmaxLifecycleAudit.observeSurvival(auditCycle - 1, auditPool, auditNext);
            long tArch1 = System.nanoTime();
            updateParticlesMemory(swarm);
            org.uma.jmetal.algorithm.multiobjective.mypso.v35.V35ModuleTimer.record(
                    org.uma.jmetal.algorithm.multiobjective.mypso.v35.V35ModuleTimer.ARCHIVE,
                    System.nanoTime() - tArch1, 1L);
            updateProgress();
            formalBaselineOuterCycles++;
            // V35-SOURCE-ATTRIBUTION-PATCH: cycle context (pure observation).
            org.uma.jmetal.algorithm.multiobjective.mypso.v35.V35SourceAttributionObserver.context(generationNumber(), formalBaselineOuterCycles, formalQRoundIndex);
            // FC-6A-POST / Build-C2: per-cycle population/archive geometry + lineage snapshot
            // (pure observation; enabled flag short-circuits when the audit is off).
            if (org.uma.jmetal.algorithm.multiobjective.mypso.v35.V35Fc6BpPddrDiagnosticAudit
                    .isEnabled()) {
                org.uma.jmetal.algorithm.multiobjective.mypso.v35.V35Fc6BpPddrDiagnosticAudit
                        .current().observeCycle((int) formalBaselineOuterCycles, swarm,
                        globallyOptimalIndividual, fullEvaluationCount);
            }
            if (fullEvaluationCount == beforeCycle) break;
            v35CmaxLifecycleAudit.endCycle(bestCmaxOf(globallyOptimalIndividual));
            // FC-5.1: archive-best Cmax lineage exposure tracking (pure observation).
            v35CmaxLifecycleAudit.observeArchiveBest(auditCycle,
                    bestCmaxOf(globallyOptimalIndividual), archiveBestLineageId());
            registerTop5CmaxAudit(auditCycle, swarm);
            // FC-TIME-1: per-cycle record (pure observation; never affects behaviour).
            if (org.uma.jmetal.algorithm.multiobjective.mypso.v35.V35ModuleTimer.isEnabled()) {
                java.util.Map<String, long[]> cycleDelta =
                        org.uma.jmetal.algorithm.multiobjective.mypso.v35.V35ModuleTimer
                                .delta(cycleStart,
                                        org.uma.jmetal.algorithm.multiobjective.mypso.v35
                                                .V35ModuleTimer.snapshot());
                java.util.Map<String, long[]> counterDelta =
                        org.uma.jmetal.algorithm.multiobjective.mypso.v35.V35ModuleTimer
                                .counterDelta(cycleCounterStart,
                                        org.uma.jmetal.algorithm.multiobjective.mypso.v35
                                                .V35ModuleTimer.counterSnapshot());
                StringBuilder line = new StringBuilder();
                line.append("cycle=").append(formalBaselineOuterCycles)
                        .append(",fe=").append(fullEvaluationCount)
                        .append(",archiveSize=").append(globallyOptimalIndividual.size())
                        .append(",timePerQRound=").append(cycleDelta.getOrDefault(
                                org.uma.jmetal.algorithm.multiobjective.mypso.v35
                                        .V35ModuleTimer.CFVF, new long[2])[1] / Math.max(1L,
                                admittedRounds));
                for (java.util.Map.Entry<String, long[]> e : cycleDelta.entrySet()) {
                    line.append(',').append(e.getKey()).append('=').append(e.getValue()[1]);
                }
                for (java.util.Map.Entry<String, long[]> e : counterDelta.entrySet()) {
                    line.append(",c_").append(e.getKey()).append('=').append(e.getValue()[0]);
                }
                v35ModulePerCycleLines.add(line.toString());
            }
        }
    }

        /** FC-5: best (minimum) Cmax across a solution list (pure observation). */
    private static double bestCmaxOf(List<PermutationSolution<Integer>> solutions) {
        double best = Double.POSITIVE_INFINITY;
        for (PermutationSolution<Integer> solution : solutions) {
            double value = solution.getObjective(0);
            if (value < best) {
                best = value;
            }
        }
        return best;
    }

    /** FC-5.1: lineage of the archive solution with the smallest Cmax (pure observation). */
    private long archiveBestLineageId() {
        double best = Double.POSITIVE_INFINITY;
        long id = -1L;
        for (PermutationSolution<Integer> solution : globallyOptimalIndividual) {
            double value = solution.getObjective(0);
            if (value < best) {
                best = value;
                id = lineageId(solution);
            }
        }
        return id;
    }

    /** FC-5: register Top-5 Cmax lineages of the current swarm (pure observation). */
    private void registerTop5CmaxAudit(int cycle, List<PermutationSolution<Integer>> swarm) {
        if (swarm.isEmpty()) {
            return;
        }
        List<PermutationSolution<Integer>> sorted = new ArrayList<>(swarm);
        sorted.sort(java.util.Comparator.comparingDouble(s -> s.getObjective(0)));
        for (int index = 0; index < Math.min(5, sorted.size()); index++) {
            PermutationSolution<Integer> solution = sorted.get(index);
            v35CmaxLifecycleAudit.registerLineage(lineageId(solution), cycle,
                    birthOperatorOf(solution), solution.getObjective(0));
        }
    }

    /** FC-5: birth operator source of a solution (pure observation). */
    private static String birthOperatorOf(PermutationSolution<Integer> solution) {
        Object tag = solution.getAttribute(
                org.uma.jmetal.algorithm.multiobjective.mypso.zhangbo.cata.ZhangBoPreEvaluatedTag.class);
        if (tag instanceof org.uma.jmetal.algorithm.multiobjective.mypso.zhangbo.cata.ZhangBoPreEvaluatedTag) {
            return ((org.uma.jmetal.algorithm.multiobjective.mypso.zhangbo.cata.ZhangBoPreEvaluatedTag) tag)
                    .getSource().name();
        }
        return "UNKNOWN";
    }

    private void markAlreadyEvaluatedFormalGlobalOffspring(
            List<PermutationSolution<Integer>> swarm, int completedRounds) {
        long first = fullEvaluationCount - swarm.size() + 1L;
        for (int slot = 0; slot < swarm.size(); slot++) {
            ZhangBoPreEvaluatedTag.mark(swarm.get(slot), new ZhangBoPreEvaluatedTag(
                    ZhangBoPreEvaluatedTag.Source.GLOBAL_OFFSPRING, slot,
                    lineageId(swarm.get(slot)), first + slot));
        }
        zhangBoP6Events.add("formalBaseline:outer=" + completedOuterGenerations
                + ",qRounds=" + completedRounds
                + ",feAfterQg=" + fullEvaluationCount);
    }

    @Override
    protected void initProgress() {
        currentIteration = swarmSize + currentIteration;
        //currentIteration = 1;
        //    crowdingDistance.computeDensityEstimator(leaderArchive.getSolutionList());
    }

    @Override
    protected void updateProgress() {
//        System.out.println(currentIteration);
        currentIteration = currentIteration + swarmSize;
        completedOuterGenerations++;
//        currentIteration =  swarmSize;
//        System.out.println(currentIteration);

//        currentIteration = swarmSize;
//        System.out.println(currentIteration);
//        try {
//            Thread.sleep(9999999);
//        } catch (InterruptedException e) {
//            throw new RuntimeException(e);
//        }
        //currentIteration += 1;
        //   crowdingDistance.computeDensityEstimator(leaderArchive.getSolutionList());
    }

    @Override
    protected boolean isStoppingConditionReached() {
        if (globalSearchConfiguration.isLocalSearchEnabled()
                && fullEvaluationCount + swarmSize > maxIterations) {
            return true;
        }
        return currentIteration >= maxIterations;
    }

    @Override
    protected List<PermutationSolution<Integer>> createInitialSwarm() {

        if (initialSwarmOverride != null) {
            List<PermutationSolution<Integer>> fixed =
                    ZhangBoSolutionSupport.deepCopySolutions(initialSwarmOverride);
            for (int index = 0; index < fixed.size(); index++) {
                if (globalSearchConfiguration.isResourceFlightEnabled()) {
                    canonicalizeInitialCfvfResources(fixed.get(index), index);
                }
            }
            return fixed;
        }

        List<PermutationSolution<Integer>> swarm = new ArrayList<>(swarmSize);

        PermutationSolution<Integer> newSolution;

        // for (int i = 0; i < swarmSize/4; i++) {

        // newSolution =  problem.createSolution();
        //Random A = authorRandom();
        //PermutationSolution<Integer> solutionNew  = problem.createSolution();
        //int a, i , j;
/*            PermutationSolution<Integer> solutiontemp = problem.createSolution();
            //List<List<Integer>> t = new ArrayList<>();  //存工厂号下对应的工件序列的下标
            //ArrayList<List<Integer>> N = new ArrayList<>();
            int [][] N= new int[3][newSolution.getNumberOfVariablesid()];

            List<Integer> v = new ArrayList<>();
            //int[] ind = new int[selectFac.size()];     //存工厂号
//对0，1，2工厂的工件顺序排列
            int [][] len=new int[3][1];
            for (int r = 0; r < 3; r++) {
                int h=0;
                for (int y = 0; y < newSolution.getNumberOfVariablesid(); y++) {
                    if (newSolution.getVariableValueid(y) == r) {  //等于工厂号的下标
                        N[r][h]=y;     //等于工厂号的下标
                        h++;
                        //System.out.println(N[r]);
                        // x++;
                    }
                }
                len[r][0]=h;   //工厂的工件个数  0，1，2长度
            }
            int nums = 0;
            int[] flag = new int[3];
            int[] flagBeg = new int[3];
            for (int r = 0; r < 3; r++) {
                for (int y = 0; y < len[r][0]; y++) {
                    int temp1 = newSolution.getVariableValue(N[r][y]);    //工件号
                    solutiontemp.setVariableValue(nums, temp1);
                    //int temp2 =  solution.getVariableValueid(NEW.get(r).get(y));    //工厂号
                    solutiontemp.setVariableValueid(nums, r);
                    //solutionNew.setVariableValueid(nums,solutionNew.getVariableValue(nums));
                    nums++;
                }
                flagBeg[r]=nums-len[r][0];
                flag[r] = nums - 1; //工厂的最后一个下标
            }*/
        //swarm.add(newSolution);
        //}
        Random A = authorRandom();
        for (int k = 0; k < swarmSize; k++) {
            PermutationSolution<Integer> solutionNew = problem.createSolution();

//            for (int i = 0; i < 18; i++) {
//                solutionNew.setVariableValueworker(i,i);   //工人向量
//            }

//            for (int j = 0; j < numberOfFactories; j++) {
//                solutionNew.setVariableValueid(j, j);       //用于工厂序列
//
//                solutionNew.setVariableValueid(j + numberOfFactories, j);
//
//                solutionNew.setVariableValueid(j + 2 * numberOfFactories, j);
//
//            }
////            System.out.println(solutionNew);
////            try {
////                Thread.sleep(99999);
////            } catch (InterruptedException e) {
////                throw new RuntimeException(e);
////            }
//            for (int j = 3 * numberOfFactories; j < solutionNew.getNumberOfVariables(); j++) {
//                int f = A.nextInt(numberOfFactories);
//                solutionNew.setVariableValueid(j, f);       //用于工厂序列
//            }


//            for (int i = 0; i < solutionNew.getNumberOfVariables(); i++) {
//                solutionNew.setVariableValueworker(i,i);
//            }
//            System.out.println(solutionNew);
//sleep();
            if (globalSearchConfiguration.isResourceFlightEnabled()) {
                Object problemCorrections = solutionNew.getAttribute(
                        "ZhangBoFirstStageMachineInitializationCorrections");
                if (problemCorrections instanceof Number) {
                    long count = ((Number) problemCorrections).longValue();
                    cfvfInitializationCorrections += count;
                    if (count > 0) {
                        zhangBoP6Events.add("initializationCanonicalization:particle=" + k
                                + ",vector=MA,count=" + count
                                + ",reason=P5.1_problem_domain_precondition");
                    }
                }
                canonicalizeInitialCfvfResources(solutionNew, k);
            }
            swarm.add(solutionNew);

        }
//        System.out.println(swarm);
//        sleep();
//        try {
//            Thread.sleep(99999);
//        } catch (InterruptedException e) {
//            throw new RuntimeException(e);
//        }
        return swarm;
    }

    // FC-5.2: pure-observation lifecycle record helpers (no decision impact; off when not enabled).
    private void fc52RecordEvaluated(PermutationSolution<Integer> solution, long fe,
            V35EvaluationSourceContext.Source source) {
        V35Fc52LifecycleAudit fc52 = V35Fc52LifecycleAudit.current();
        if (fc52 == null) {
            return;
        }
        fc52.recordEvaluated(solution, source, fe,
                (int) formalBaselineOuterCycles + 1, formalQRoundIndex, lineageId(solution));
    }

    private void fc52LocalAccepted(PermutationSolution<Integer> candidate, long fe,
            String reason) {
        V35Fc52LifecycleAudit fc52 = V35Fc52LifecycleAudit.current();
        if (fc52 != null) {
            fc52.recordLocalAccepted(candidate, fe, reason);
        }
    }

    private void fc52LocalRejected(PermutationSolution<Integer> candidate, long fe,
            String reason) {
        V35Fc52LifecycleAudit fc52 = V35Fc52LifecycleAudit.current();
        if (fc52 != null) {
            fc52.recordLocalRejected(candidate, fe, reason);
        }
    }

    /** FC-6A.3 audit hooks; all are read-only and disappear completely when disabled. */
    private void fc6RecordEvaluated(ZhangBoEvaluatedPddrSelector.Source source,
            PermutationSolution<Integer> candidate) {
        V35Fc6LocalCandidateAudit audit = V35Fc6LocalCandidateAudit.current();
        if (audit != null) audit.recordEvaluated((int) formalBaselineOuterCycles + 1,
                // Every caller reaches this hook immediately after exactly one
                // complete local candidate evaluation.  The ledger therefore
                // records source-local FE, not the running global FE counter.
                source, 1L, candidate);
    }

    private void fc6RecordAccepted(ZhangBoEvaluatedPddrSelector.Source source) {
        V35Fc6LocalCandidateAudit audit = V35Fc6LocalCandidateAudit.current();
        if (audit != null) audit.recordAccepted((int) formalBaselineOuterCycles + 1, source);
    }

    private void fc6RecordSuperseded(LocalCandidateOrigin source) {
        V35Fc6LocalCandidateAudit audit = V35Fc6LocalCandidateAudit.current();
        if (audit != null && source != null) {
            audit.recordSuperseded((int) formalBaselineOuterCycles + 1, source.selectorSource);
        }
    }

    @Override
    protected List<PermutationSolution<Integer>> evaluateSwarm(List<PermutationSolution<Integer>> swarm) {
        long evaluationsBefore = fullEvaluationCount;
        // V35-P25-gate fix (acceptance review 2026-08-13): the formal baseline loop
        // marks every global offspring pre-evaluated before the inherited local
        // search, then calls evaluateSwarm(); the markers must be honored whenever
        // the formal loop runs, not only for the structured baseline update.  With
        // CFVF replacing the structured update (V35 A2/A3 rungs) the old condition
        // fell through and re-evaluated the whole swarm once per outer cycle
        // (18x100 duplicated evaluations at 500k, 2x10 on the I1 link arms).
        boolean honorsPreEvaluatedMarkers = globalSearchConfiguration.isLocalSearchEnabled()
                || formalBaselineConfiguration.isEnabled();
        if (honorsPreEvaluatedMarkers) {
            List<PermutationSolution<Integer>> unevaluated = new ArrayList<>();
            for (PermutationSolution<Integer> solution : swarm) {
                if (!ZhangBoPreEvaluatedTag.isMarked(solution)) {
                    unevaluated.add(solution);
                }
            }
            if (!unevaluated.isEmpty()) {
                evaluator.evaluate(unevaluated, (Problem<PermutationSolution<Integer>>) problem);
                fullEvaluationCount += unevaluated.size();
                if (cmaxAudit != null && evaluationsBefore == 0L) {
                    cmaxAudit.observeInitialPopulation(1L, unevaluated);
                } else if (cmaxAudit != null) {
                    // Formal global offspring (CFVF/structured baseline) are
                    // evaluated here, one full swarm per Q round; observe them
                    // so the audit's generation coverage is complete.
                    for (int index = 0; index < unevaluated.size(); index++) {
                        PermutationSolution<Integer> solution = unevaluated.get(index);
                        Object groupValue = solution.getAttribute(ZhangBoSubSwarm.class);
                        ZhangBoSubSwarm group = groupValue instanceof ZhangBoSubSwarm
                                ? (ZhangBoSubSwarm) groupValue : null;
                        cmaxAudit.observeGenerated(
                                fullEvaluationCount - unevaluated.size() + index + 1L,
                                generationNumber(), solution, group,
                                globalSearchConfiguration.isCfvfEnabled()
                                        ? ZhangBoCmaxAudit.Mechanism.CFVF
                                        : ZhangBoCmaxAudit.Mechanism.BASELINE_GLOBAL,
                                globalSearchConfiguration.isCfvfEnabled()
                                        ? ZhangBoCmaxAudit.Operator.CFVF
                                        : ZhangBoCmaxAudit.Operator.BASELINE_GLOBAL,
                                "GLOBAL_SLOT_" + (group == null ? "UNASSIGNED" : group), true);
                        fc52RecordEvaluated(solution,
                                fullEvaluationCount - unevaluated.size() + index + 1L,
                                V35EvaluationSourceContext.current());
                    }
                }
                if (v35PassiveArchive != null) {
                    for (PermutationSolution<Integer> solution : unevaluated) {
                        // V35-SOURCE-LEDGER-PATCH: source readable inside the window.
                        v35PassiveArchive.observeWithSource(solution, V35EvaluationSourceContext.current());
                    }
                }
            }
            return swarm;
        }
        swarm = evaluator.evaluate(swarm, (Problem<PermutationSolution<Integer>>) problem);
        fullEvaluationCount += swarm.size();
        if (cmaxAudit != null && evaluationsBefore == 0L) {
            cmaxAudit.observeInitialPopulation(1L, swarm);
        } else if (cmaxAudit != null) {
            for (int index = 0; index < swarm.size(); index++) {
                PermutationSolution<Integer> solution = swarm.get(index);
                Object groupValue = solution.getAttribute(ZhangBoSubSwarm.class);
                ZhangBoSubSwarm group = groupValue instanceof ZhangBoSubSwarm
                        ? (ZhangBoSubSwarm) groupValue : null;
                cmaxAudit.observeGenerated(
                        fullEvaluationCount - swarm.size() + index + 1L,
                        generationNumber(), solution, group,
                        globalSearchConfiguration.isCfvfEnabled()
                                ? ZhangBoCmaxAudit.Mechanism.CFVF
                                : ZhangBoCmaxAudit.Mechanism.BASELINE_GLOBAL,
                        globalSearchConfiguration.isCfvfEnabled()
                                ? ZhangBoCmaxAudit.Operator.CFVF
                                : ZhangBoCmaxAudit.Operator.BASELINE_GLOBAL,
                                "GLOBAL_SLOT_" + (group == null ? "UNASSIGNED" : group), true);
                        fc52RecordEvaluated(solution,
                                fullEvaluationCount - swarm.size() + index + 1L,
                                V35EvaluationSourceContext.current());
                    }
                }
                if (v35PassiveArchive != null) {
                    for (PermutationSolution<Integer> solution : swarm) {
                // V35-SOURCE-LEDGER-PATCH: source readable inside the window.
                v35PassiveArchive.observeWithSource(solution, V35EvaluationSourceContext.current());
            }
        }

        //currentIteration=currentIteration+swarmSize;
        return swarm;
    }

    @Override
    protected void initializeLeader(List<PermutationSolution<Integer>> swarm) {


        List<Double> aa = new ArrayList<>(swarmSize);
        for (int i = 0; i < swarmSize; i++) {
            double count1 = 0;
            double count2 = 0;
            for (int j = 0; j < swarmSize; j++) {
                if (i != j) {
                    if (swarm.get(i).getObjective(0) <= swarm.get(j).getObjective(0) &&
                            swarm.get(i).getObjective(1) <= swarm.get(j).getObjective(1) &&
                            swarm.get(i).getObjective(6) <= swarm.get(j).getObjective(6)) {
                        count1 = count1 + 1;
                    }
                    if (swarm.get(i).getObjective(0) >= swarm.get(j).getObjective(0) &&
                            swarm.get(i).getObjective(1) >= swarm.get(j).getObjective(1) &&
                            swarm.get(i).getObjective(6) >= swarm.get(j).getObjective(6)) {
                        count2 = count2 + 1;
                    }
                }
            }
            aa.add(count2 + 1 / (count1 + 1));
        }


        // Step 1: Calculate the average value of the list
        double sum = 0;
        for (double num : aa) {
            sum += num;
        }
        double average = sum / aa.size();
        // Step 2 and 3: Find indices of elements that are greater than the average
        List<Integer> indices = new ArrayList<>();
        for (int i = 0; i < aa.size(); i++) {
            if (aa.get(i) > average) {
                indices.add(i); // Add index to the list if element is above average
            }
        }
        for (Integer i : indices) {
            PermutationSolution<Integer> archived = tempSwarm.get(i).get(0);
            globallyOptimalIndividual.add(archived);
            // FC-5: mark this lineage as having entered the external archive.
            v35CmaxLifecycleAudit.markArchive(lineageId(archived));
        }

        if (cmaxAudit != null) {
            cmaxAudit.refreshState(fullEvaluationCount, swarm, globallyOptimalIndividual);
        }


    }

    @Override
    protected void initializeParticlesMemory(List<PermutationSolution<Integer>> swarm) {
        for (int i = 0; i < swarm.size(); i++) {
            ArrayList<PermutationSolution<Integer>> A = new ArrayList<PermutationSolution<Integer>>();
            A.add(swarm.get(i));
            tempSwarm.add(A);
        }
    }

    @Override
    protected void initializeVelocity(List<PermutationSolution<Integer>> swarm) {

    }


    //分群
    protected void updateVelocity(List<PermutationSolution<Integer>> swarm) {

        upGroup1Population.clear();
        upNewGroup1Population.clear();
        centralGroup2Population.clear();
        downGroup3Population.clear();

        groupU1Solution.clear();
        groupUNewSolution.clear();
        groupC2Solution.clear();
        groupD3Solution.clear();

        // FC-6B: REGION_AWARE PDDR already assigned every survivor to the
        // immutable physical role. Do not silently overwrite that assignment
        // by re-running the historical global extrema regrouping below.
        if (globalSearchConfiguration.getPddrSelectionMode()
                == PddrSelectionMode.REGION_AWARE) {
            int assignedRoles = countRegionAwareRoles(swarm);
            // The initial population predates its first PDDR selection and
            // therefore deliberately has no assigned region.  It is grouped
            // by the historical deterministic bootstrap once; every later
            // survivor must carry the PDDR-assigned physical role.
            if (assignedRoles == swarmSize) {
                partitionRegionAwareSwarm(swarm);
                select();
                return;
            }
            if (assignedRoles != 0) {
                throw new IllegalStateException("REGION_AWARE partial physical-role assignment: "
                        + assignedRoles + "/" + swarmSize);
            }
        }

        List<PermutationSolution<Integer>> temp1 = new ArrayList<>(swarmSize);
        List<PermutationSolution<Integer>> temp2 = new ArrayList<>(swarmSize);
        List<PermutationSolution<Integer>> temp3 = new ArrayList<>(swarmSize);
        List<PermutationSolution<Integer>> temp4 = new ArrayList<>(swarmSize);  //新加的，是保存Cost的

        ArrayList<List<PermutationSolution<Integer>>> tempPd1 = new ArrayList<List<PermutationSolution<Integer>>>(swarmSize);
        ArrayList<List<PermutationSolution<Integer>>> tempPd2 = new ArrayList<List<PermutationSolution<Integer>>>(swarmSize);
        ArrayList<List<PermutationSolution<Integer>>> tempPd3 = new ArrayList<List<PermutationSolution<Integer>>>(swarmSize);
        ArrayList<List<PermutationSolution<Integer>>> tempPd4 = new ArrayList<List<PermutationSolution<Integer>>>(swarmSize);    //新加的，是保存Cost的

        for (int i = 0; i < swarmSize; i++) {
            temp1.add((PermutationSolution<Integer>) swarm.get(i).copy());
            temp2.add((PermutationSolution<Integer>) swarm.get(i).copy());
            temp3.add((PermutationSolution<Integer>) swarm.get(i).copy());
            temp4.add((PermutationSolution<Integer>) swarm.get(i).copy());

            ArrayList<PermutationSolution<Integer>> A = new ArrayList<>(tempSwarm.get(i).size());
            for (int j = 0; j < tempSwarm.get(i).size(); j++) {

                A.add((PermutationSolution<Integer>) tempSwarm.get(i).get(j).copy());
            }

            tempPd1.add(A);
            tempPd2.add(A);
            tempPd3.add(A);
            tempPd4.add(A);
        }


        //划分sub1  25
        for (int i = 0; i < upSize; i++) {
            int b = 0;
            for (int j = 1; j < temp1.size(); j++) {
                if ((temp1.get(j).getObjective(0) < temp1.get(b).getObjective(0))) {
                    b = j;
                }
            }
            groupU1Solution.add(temp1.get(b));
            upGroup1Population.add(tempPd1.get(b));

            temp1.remove(b);
            tempPd1.remove(b);

        }


        //划分sub2
        List<Double> aa = new ArrayList<>(swarmSize);
        for (int i = 0; i < swarmSize; i++) {
            double count1 = 0;
            double count2 = 0;
            for (int j = 0; j < swarmSize; j++) {
                if (i != j) {
                    if (temp2.get(i).getObjective(0) <= temp2.get(j).getObjective(0) &&
                            temp2.get(i).getObjective(1) <= temp2.get(j).getObjective(1) &&
                            temp2.get(i).getObjective(6) <= temp2.get(j).getObjective(6)) {
                        count1 = count1 + 1;
                    }
                    if (temp2.get(i).getObjective(0) >= temp2.get(j).getObjective(0) &&
                            temp2.get(i).getObjective(1) >= temp2.get(j).getObjective(1) &&
                            temp2.get(i).getObjective(6) >= temp2.get(j).getObjective(6)) {
                        count2 = count2 + 1;
                    }
                }
            }
            aa.add(count2 + 1 / (count1 + 1));
        }

        for (int i = 0; i < centralSize; i++) {
            int b = 0;
            for (int j = 1; j < aa.size(); j++) {
                if (aa.get(j) < aa.get(b)) {
                    b = j;
                }
            }
            groupC2Solution.add(temp2.get(b));
            centralGroup2Population.add(tempPd2.get(b));

            aa.remove(b);
            tempPd2.remove(b);
            temp2.remove(b);
        }


        //划分sub3
        for (int i = 0; i < downSize; i++) {
            int b = 0;
            for (int j = 1; j < temp3.size(); j++) {
                if ((temp3.get(j).getObjective(1) < temp3.get(b).getObjective(1))) {
                    b = j;
                }
            }
            groupD3Solution.add(temp3.get(b));
            downGroup3Population.add(tempPd3.get(b));

            temp3.remove(b);
            tempPd3.remove(b);
        }


        //划分sub4
        for (int i = 0; i < upNewSize; i++) {
            int b = 0;
            for (int j = 1; j < temp4.size(); j++) {
                if ((temp4.get(j).getObjective(6) < temp4.get(b).getObjective(6))) {
                    b = j;
                }
            }
            groupUNewSolution.add(temp4.get(b));
            upNewGroup1Population.add(tempPd4.get(b));

            temp4.remove(b);
            tempPd4.remove(b);
        }


        select();

    }

    /** Rebuilds the four author physical slots from REGION_AWARE PDDR roles. */
    private void partitionRegionAwareSwarm(List<PermutationSolution<Integer>> swarm) {
        if (swarm.size() != swarmSize || tempSwarm.size() != swarmSize) {
            throw new IllegalStateException("Region-aware swarm/history alignment is incomplete");
        }
        for (int index = 0; index < swarm.size(); index++) {
            Object raw = swarm.get(index).getAttribute(ZhangBoSubSwarm.class);
            if (!(raw instanceof ZhangBoSubSwarm)) {
                throw new IllegalStateException("REGION_AWARE survivor has no physical role at index=" + index);
            }
            ZhangBoSubSwarm role = (ZhangBoSubSwarm) raw;
            PermutationSolution<Integer> copy = (PermutationSolution<Integer>) swarm.get(index).copy();
            List<PermutationSolution<Integer>> history =
                    ZhangBoSolutionSupport.deepCopySolutions(tempSwarm.get(index));
            switch (role) {
                case G1_CMAX:
                    groupU1Solution.add(copy);
                    upGroup1Population.add(history);
                    break;
                case G4_BALANCED:
                    groupC2Solution.add(copy);
                    centralGroup2Population.add(history);
                    break;
                case G2_TEC:
                    groupD3Solution.add(copy);
                    downGroup3Population.add(history);
                    break;
                case G3_TWC:
                    groupUNewSolution.add(copy);
                    upNewGroup1Population.add(history);
                    break;
                default:
                    throw new IllegalStateException("Unhandled REGION_AWARE role=" + role);
            }
        }
        if (groupU1Solution.size() != upSize || groupC2Solution.size() != centralSize
                || groupD3Solution.size() != downSize || groupUNewSolution.size() != upNewSize) {
            throw new IllegalStateException("REGION_AWARE physical capacity mismatch: slot1="
                    + groupU1Solution.size() + ",slot2=" + groupC2Solution.size()
                    + ",slot3=" + groupD3Solution.size() + ",slot4=" + groupUNewSolution.size());
        }
    }

    private int countRegionAwareRoles(List<PermutationSolution<Integer>> swarm) {
        int count = 0;
        for (PermutationSolution<Integer> solution : swarm) {
            if (solution.getAttribute(ZhangBoSubSwarm.class) instanceof ZhangBoSubSwarm) {
                count++;
            }
        }
        return count;
    }

    /**
     * Random index used by the shared partition/selection routine.  The
     * historical author path deliberately keeps the jMetal singleton, but
     * the formal v3.5 path must consume only its run-local seeded stream.
     */
    private int formalRandomInt(int lower, int upper) {
        if (formalBaselineConfiguration.isEnabled()) {
            return zhangBoP6Random.nextInt(lower, upper);
        }
        return randomGenerator.nextInt(lower, upper);
    }

    //二元锦标赛法（选三个，取最好）
    //选择Gbest和Pbest
    private void select() {

        upGr1HisOptIndividual.clear();
        upNewGr1HisOptIndividual.clear();
        centralGr2HisOptIndividual.clear();
        downGr3HisOptIndividual.clear();
        all3GlobalOptIndividuals.clear();

        for (int i = 0; i < upSize; i++) {
            int a1 = formalRandomInt(0, upGroup1Population.get(i).size() - 1);
            int a2 = formalRandomInt(0, upGroup1Population.get(i).size() - 1);
            int a3 = formalRandomInt(0, upGroup1Population.get(i).size() - 1);
            int temp = a1;
            if ((upGroup1Population.get(i).get(a1).getObjective(0) >= upGroup1Population.get(i).get(a2).getObjective(0) &&
                    upGroup1Population.get(i).get(a3).getObjective(0) >= upGroup1Population.get(i).get(a2).getObjective(0))
            ) {
                temp = a2;
            }

            if ((upGroup1Population.get(i).get(a1).getObjective(0) >= upGroup1Population.get(i).get(a3).getObjective(0) &&
                    upGroup1Population.get(i).get(a2).getObjective(0) >= upGroup1Population.get(i).get(a3).getObjective(0))
            ) {
                temp = a3;
            }
//            System.out.println(upGroup1Population.get(i).get(temp));
            upGr1HisOptIndividual.add(upGroup1Population.get(i).get(temp));

        }

        for (int i = 0; i < centralSize; i++) {
            int a1 = formalRandomInt(0, centralGroup2Population.get(i).size() - 1);
            int a2 = formalRandomInt(0, centralGroup2Population.get(i).size() - 1);
            int a3 = formalRandomInt(0, centralGroup2Population.get(i).size() - 1);
            int temp = a1;
            List<Double> bb = new ArrayList<>(centralGroup2Population.get(i).size());

            for (int j = 0; j < centralGroup2Population.get(i).size(); j++) {
                double count1 = 0;
                double count2 = 0;
                for (int k = 0; k < centralGroup2Population.get(i).size(); k++) {
                    if (j != k) {
                        if (centralGroup2Population.get(i).get(j).getObjective(0) <= centralGroup2Population.get(i).get(k).getObjective(0) &&
                                centralGroup2Population.get(i).get(j).getObjective(1) <= centralGroup2Population.get(i).get(k).getObjective(1) &&
                                centralGroup2Population.get(i).get(j).getObjective(6) <= centralGroup2Population.get(i).get(k).getObjective(6)

                        ) {
                            count1 = count1 + 1;
                        }
                        if (centralGroup2Population.get(i).get(j).getObjective(0) >= centralGroup2Population.get(i).get(k).getObjective(0) &&
                                centralGroup2Population.get(i).get(j).getObjective(1) >= centralGroup2Population.get(i).get(k).getObjective(1) &&
                                centralGroup2Population.get(i).get(j).getObjective(6) >= centralGroup2Population.get(i).get(k).getObjective(6)) {
                            count2 = count2 + 1;
                        }
                    }
                }
                bb.add(count2 + 1 / (count1 + 1));
            }

            if (bb.get(a1) >= bb.get(a2) && bb.get(a3) >= bb.get(a2)) {
                temp = a2;
            }
            if (bb.get(a1) >= bb.get(a3) && bb.get(a2) >= bb.get(a3)) {
                temp = a3;
            }
            centralGr2HisOptIndividual.add(centralGroup2Population.get(i).get(temp));

        }

        for (int i = 0; i < downSize; i++) {
            int a1 = formalRandomInt(0, downGroup3Population.get(i).size() - 1);
            int a2 = formalRandomInt(0, downGroup3Population.get(i).size() - 1);
            int a3 = formalRandomInt(0, downGroup3Population.get(i).size() - 1);
            int temp = a1;
            if ((downGroup3Population.get(i).get(a1).getObjective(1) >= downGroup3Population.get(i).get(a2).getObjective(1) &&
                    downGroup3Population.get(i).get(a3).getObjective(1) >= downGroup3Population.get(i).get(a2).getObjective(1))
            ) {
                temp = a2;
            }
            if ((downGroup3Population.get(i).get(a1).getObjective(1) >= downGroup3Population.get(i).get(a3).getObjective(1) &&
                    downGroup3Population.get(i).get(a2).getObjective(1) >= downGroup3Population.get(i).get(a3).getObjective(1))
            ) {
                temp = a3;
            }
            downGr3HisOptIndividual.add(downGroup3Population.get(i).get(temp));
        }


        for (int i = 0; i < upNewSize; i++) {
            int a1 = formalRandomInt(0, upNewGroup1Population.get(i).size() - 1);
            int a2 = formalRandomInt(0, upNewGroup1Population.get(i).size() - 1);
            int a3 = formalRandomInt(0, upNewGroup1Population.get(i).size() - 1);
            int temp = a1;
            if ((upNewGroup1Population.get(i).get(a1).getObjective(6) >= upNewGroup1Population.get(i).get(a2).getObjective(6) &&
                    upNewGroup1Population.get(i).get(a3).getObjective(6) >= upNewGroup1Population.get(i).get(a2).getObjective(6))
            ) {
                temp = a2;
            }
            if ((upNewGroup1Population.get(i).get(a1).getObjective(6) >= upNewGroup1Population.get(i).get(a3).getObjective(6) &&
                    upNewGroup1Population.get(i).get(a2).getObjective(6) >= upNewGroup1Population.get(i).get(a3).getObjective(6))
            ) {
                temp = a3;
            }
            upNewGr1HisOptIndividual.add(upNewGroup1Population.get(i).get(temp));
        }

//        System.out.println(indextest++);
//        判断选择全局最优解Gbest
//        把下面部分放到全局搜索里面
        int a1 = formalRandomInt(0, globallyOptimalIndividual.size() - 1);
        int a2 = formalRandomInt(0, globallyOptimalIndividual.size() - 1);
        int a3 = formalRandomInt(0, globallyOptimalIndividual.size() - 1);
        int temp = a1;
        if ((globallyOptimalIndividual.get(a1).getObjective(0) >= globallyOptimalIndividual.get(a2).getObjective(0) &&
                globallyOptimalIndividual.get(a3).getObjective(0) >= globallyOptimalIndividual.get(a2).getObjective(0))
        ) {
            temp = a2;
        }

        if ((globallyOptimalIndividual.get(a1).getObjective(0) >= globallyOptimalIndividual.get(a3).getObjective(0) &&
                globallyOptimalIndividual.get(a2).getObjective(0) >= globallyOptimalIndividual.get(a3).getObjective(0))
        ) {
            temp = a3;
        }
        all3GlobalOptIndividuals.add(globallyOptimalIndividual.get(temp));


        a1 = formalRandomInt(0, globallyOptimalIndividual.size() - 1);
        a2 = formalRandomInt(0, globallyOptimalIndividual.size() - 1);
        a3 = formalRandomInt(0, globallyOptimalIndividual.size() - 1);
        temp = a1;
        List<Double> cc = new ArrayList<>(globallyOptimalIndividual.size());
        for (int i = 0; i < globallyOptimalIndividual.size(); i++) {
            double count1 = 0;
            double count2 = 0;
            for (int j = 0; j < globallyOptimalIndividual.size(); j++) {
                if (i != j) {
                    if (globallyOptimalIndividual.get(i).getObjective(0) <= globallyOptimalIndividual.get(j).getObjective(0) &&
                            globallyOptimalIndividual.get(i).getObjective(1) <= globallyOptimalIndividual.get(j).getObjective(1) &&
                            globallyOptimalIndividual.get(i).getObjective(6) <= globallyOptimalIndividual.get(j).getObjective(6)) {
                        count1 = count1 + 1;
                    }
                    if (globallyOptimalIndividual.get(i).getObjective(0) >= globallyOptimalIndividual.get(j).getObjective(0) &&
                            globallyOptimalIndividual.get(i).getObjective(1) >= globallyOptimalIndividual.get(j).getObjective(1) &&
                            globallyOptimalIndividual.get(i).getObjective(6) >= globallyOptimalIndividual.get(j).getObjective(6)) {
                        count2 = count2 + 1;
                    }
                }
            }
            cc.add(count2 + 1 / (count1 + 1));
        }

        if (cc.get(a1) >= cc.get(a2) && cc.get(a3) >= cc.get(a2)) {
            temp = a2;
        }
        if (cc.get(a1) >= cc.get(a3) && cc.get(a2) >= cc.get(a3)) {
            temp = a3;
        }
        all3GlobalOptIndividuals.add(globallyOptimalIndividual.get(temp));

        a1 = formalRandomInt(0, globallyOptimalIndividual.size() - 1);
        a2 = formalRandomInt(0, globallyOptimalIndividual.size() - 1);
        a3 = formalRandomInt(0, globallyOptimalIndividual.size() - 1);
        temp = a1;
        if ((globallyOptimalIndividual.get(a1).getObjective(1) >= globallyOptimalIndividual.get(a2).getObjective(1) &&
                globallyOptimalIndividual.get(a3).getObjective(1) >= globallyOptimalIndividual.get(a2).getObjective(1))
        ) {
            temp = a2;
        }

        if ((globallyOptimalIndividual.get(a1).getObjective(1) >= globallyOptimalIndividual.get(a3).getObjective(1) &&
                globallyOptimalIndividual.get(a2).getObjective(1) >= globallyOptimalIndividual.get(a3).getObjective(1))
        ) {
            temp = a3;
        }
        all3GlobalOptIndividuals.add(globallyOptimalIndividual.get(temp));


        a1 = formalRandomInt(0, globallyOptimalIndividual.size() - 1);
        a2 = formalRandomInt(0, globallyOptimalIndividual.size() - 1);
        a3 = formalRandomInt(0, globallyOptimalIndividual.size() - 1);
        temp = a1;
        if ((globallyOptimalIndividual.get(a1).getObjective(6) >= globallyOptimalIndividual.get(a2).getObjective(6) &&
                globallyOptimalIndividual.get(a3).getObjective(6) >= globallyOptimalIndividual.get(a2).getObjective(6))
        ) {
            temp = a2;
        }

        if ((globallyOptimalIndividual.get(a1).getObjective(6) >= globallyOptimalIndividual.get(a3).getObjective(6) &&
                globallyOptimalIndividual.get(a2).getObjective(6) >= globallyOptimalIndividual.get(a3).getObjective(6))
        ) {
            temp = a3;
        }
        all3GlobalOptIndividuals.add(globallyOptimalIndividual.get(temp));


        /*
        //新的判断全局最优
        double object1 = Integer.MAX_VALUE;
        double object2 = Integer.MAX_VALUE;
        double object3 = Integer.MAX_VALUE;
        int temp1 = 0;
        int temp2 = 0;
        int temp3 = 0;
        for (int i = 0; i < globallyOptimalIndividual.size(); i++) {
            if (globallyOptimalIndividual.get(i).getObjective(0)<object1){
                object1=globallyOptimalIndividual.get(i).getObjective(0);
                temp1 = i;
            }
            if (globallyOptimalIndividual.get(i).getObjective(1)<object2){
                object2=globallyOptimalIndividual.get(i).getObjective(1);
                temp2 = i;
            }
            if (globallyOptimalIndividual.get(i).getObjective(6)<object3){
                object3=globallyOptimalIndividual.get(i).getObjective(6);
                temp3 = i;
            }
        }

        all3GlobalOptIndividuals.add(globallyOptimalIndividual.get(temp1));
        all3GlobalOptIndividuals.add(globallyOptimalIndividual.get(temp2));
        all3GlobalOptIndividuals.add(globallyOptimalIndividual.get(temp3));

        List<Double> aa = new ArrayList<>(swarmSize);
        for (int i = 0; i < globallyOptimalIndividual.size(); i++) {
            double count1 = 0;
            double count2 = 0;
            for (int j = 0; j < globallyOptimalIndividual.size(); j++) {
                if (i != j) {
                    if (globallyOptimalIndividual.get(i).getObjective(0) <= globallyOptimalIndividual.get(j).getObjective(0) &&
                            globallyOptimalIndividual.get(i).getObjective(1) <= globallyOptimalIndividual.get(j).getObjective(1) &&
                            globallyOptimalIndividual.get(i).getObjective(6) <= globallyOptimalIndividual.get(j).getObjective(6)) {
                        count1 = count1 + 1;
                    }
                    if (globallyOptimalIndividual.get(i).getObjective(0) >= globallyOptimalIndividual.get(j).getObjective(0) &&
                            globallyOptimalIndividual.get(i).getObjective(1) >= globallyOptimalIndividual.get(j).getObjective(1) &&
                            globallyOptimalIndividual.get(i).getObjective(6) >= globallyOptimalIndividual.get(j).getObjective(6)) {
                        count2 = count2 + 1;
                    }
                }
            }
            aa.add(count2 + 1 / (count1 + 1));
        }

        int minIndex = 0; // 假设第一个元素是最小的
        for (int i = 1; i < aa.size(); i++) {
            if (aa.get(i) < aa.get(minIndex)) {
                minIndex = i; // 更新最小值的下标
            }
        }
        all3GlobalOptIndividuals.add(globallyOptimalIndividual.get(minIndex));*/

        if (cmaxAudit != null) {
            cmaxAudit.refreshG1(groupU1Solution);
        }

    }

    protected PermutationSolution<Integer> actionset(int i){
        if (i==0){

            int a1 = formalRandomInt(0, globallyOptimalIndividual.size() - 1);
            int a2 = formalRandomInt(0, globallyOptimalIndividual.size() - 1);
            int a3 = formalRandomInt(0, globallyOptimalIndividual.size() - 1);
            int temp = a1;
            if ((globallyOptimalIndividual.get(a1).getObjective(0) >= globallyOptimalIndividual.get(a2).getObjective(0) &&
                    globallyOptimalIndividual.get(a3).getObjective(0) >= globallyOptimalIndividual.get(a2).getObjective(0))
            ) {
                temp = a2;
            }

            if ((globallyOptimalIndividual.get(a1).getObjective(0) >= globallyOptimalIndividual.get(a3).getObjective(0) &&
                    globallyOptimalIndividual.get(a2).getObjective(0) >= globallyOptimalIndividual.get(a3).getObjective(0))
            ) {
                temp = a3;
            }
            all3GlobalOptIndividuals.add(globallyOptimalIndividual.get(temp));
            return globallyOptimalIndividual.get(temp);
        } else if (i==1) {
            if (GbestsetG1.size()==0){
                int a1 = formalRandomInt(0, globallyOptimalIndividual.size() - 1);
                int a2 = formalRandomInt(0, globallyOptimalIndividual.size() - 1);
                int a3 = formalRandomInt(0, globallyOptimalIndividual.size() - 1);
                int temp = a1;
                if ((globallyOptimalIndividual.get(a1).getObjective(0) >= globallyOptimalIndividual.get(a2).getObjective(0) &&
                        globallyOptimalIndividual.get(a3).getObjective(0) >= globallyOptimalIndividual.get(a2).getObjective(0))
                ) {
                    temp = a2;
                }

                if ((globallyOptimalIndividual.get(a1).getObjective(0) >= globallyOptimalIndividual.get(a3).getObjective(0) &&
                        globallyOptimalIndividual.get(a2).getObjective(0) >= globallyOptimalIndividual.get(a3).getObjective(0))
                ) {
                    temp = a3;
                }
                GbestsetG1.add(globallyOptimalIndividual.get(temp));
                return globallyOptimalIndividual.get(temp);
            }else {
                double min = Integer.MAX_VALUE;
                PermutationSolution<Integer> temp = null;
                for (int i1 = 0; i1 < GbestsetG1.size(); i1++) {
                    if (GbestsetG1.get(i1).getObjective(0)<min){
                        temp=GbestsetG1.get(i1);
                        min = GbestsetG1.get(i1).getObjective(0);
                    }
                }
                return temp;
            }
        } else if (i==2) {
            if (GbestsetG1.size()==0){
                int a1 = formalRandomInt(0, globallyOptimalIndividual.size() - 1);
                int a2 = formalRandomInt(0, globallyOptimalIndividual.size() - 1);
                int a3 = formalRandomInt(0, globallyOptimalIndividual.size() - 1);
                int temp = a1;
                if ((globallyOptimalIndividual.get(a1).getObjective(0) >= globallyOptimalIndividual.get(a2).getObjective(0) &&
                        globallyOptimalIndividual.get(a3).getObjective(0) >= globallyOptimalIndividual.get(a2).getObjective(0))
                ) {
                    temp = a2;
                }

                if ((globallyOptimalIndividual.get(a1).getObjective(0) >= globallyOptimalIndividual.get(a3).getObjective(0) &&
                        globallyOptimalIndividual.get(a2).getObjective(0) >= globallyOptimalIndividual.get(a3).getObjective(0))
                ) {
                    temp = a3;
                }
                GbestsetG1.add(globallyOptimalIndividual.get(temp));
                return globallyOptimalIndividual.get(temp);
            }else return GbestsetG1.get(GbestsetG1.size()-1);
        }
        return null;
    }

    public double learnG1(int a, double[][] R, double[][] Q, List<PermutationSolution<Integer>> bestsolution, int next1,int group) {
        int[] nw = ZhangBoEDHHFSPW.nw;
        Random random = authorRandom();

        List<Integer> selectFac = new ArrayList<>();


        List<PermutationSolution<Integer>> solutionnext;
        ArrayList<PermutationSolution<Integer>> groupU1Solutionbefore = new ArrayList<>();
        ArrayList<PermutationSolution<Integer>> groupC2Solutionbefore = new ArrayList<>();
        ArrayList<PermutationSolution<Integer>> groupD3Solutionbefore = new ArrayList<>();
        ArrayList<PermutationSolution<Integer>> groupUNewSolutionbefore = new ArrayList<>();
        if (group==1){
            PermutationSolution<Integer> action = actionset(a);
            GbestsetG1.add(action);
            for (int i = 0; i < groupU1Solution.size(); i++) {
                groupU1Solutionbefore.add(groupU1Solution.get(i));
            }
//        System.out.println(groupU1Solutionbefore+"groupU1Solutionbefore");
            G1PSO(random,nw,action,group);//进行粒子群进化并将结果写进groupU1Solution
            evaluateSwarm(groupU1Solution);

//        System.out.println(groupU1Solution+"groupU1Solution");

            double distance = 0.0;
            for (int i = 0; i < groupU1Solution.size(); i++) {
                double objectivebefore = groupU1Solutionbefore.get(i).getObjective(0);
                double objectivenext = groupU1Solution.get(i).getObjective(0);
                double distance0fobjective = objectivenext - objectivebefore;
                distance = distance + distance0fobjective;
            }
//        System.out.println(distance+"dis");

            double reward = R[next1][a];
            double Qvalue = calculateNewQ(reward, Q[next1][a]);
            Q[next1][a] = Qvalue;
            return distance;
        } else if (group==2) {
            PermutationSolution<Integer> action = actionset(a);
            GbestsetG2.add(action);
            for (int i = 0; i < groupC2Solution.size(); i++) {
                groupC2Solutionbefore.add(groupC2Solution.get(i));
            }
//        System.out.println(groupU1Solutionbefore+"groupU1Solutionbefore");
            G1PSO(random,nw,action,group);//进行粒子群进化并将结果写进groupU1Solution
            evaluateSwarm(groupC2Solution);

//        System.out.println(groupU1Solution+"groupU1Solution");

            double distance = 0.0;
            for (int i = 0; i < groupC2Solution.size(); i++) {
                //todo 下面用归一化对目标值进行修改
                double objective1before = groupC2Solutionbefore.get(i).getObjective(0);
                double objective1next = groupC2Solution.get(i).getObjective(0);
                double objective2before = groupC2Solutionbefore.get(i).getObjective(1);
                double objective2next = groupC2Solution.get(i).getObjective(1);
                double objective3before = groupC2Solutionbefore.get(i).getObjective(6);
                double objective3next = groupC2Solution.get(i).getObjective(6);
                double distance0fobjective1 = objective1next - objective1before;
                distance = ((objective1before-objective1next)/objective1before)+((objective2before-objective2next)/objective2before)+((objective3before-objective3next)/objective3before);

            }
//        System.out.println(distance+"dis");

            double reward = R[next1][a];
            double Qvalue = calculateNewQ(reward, Q[next1][a]);
            Q[next1][a] = Qvalue;
            return distance;

        } else if (group==3) {
            PermutationSolution<Integer> action = actionset(a);
            GbestsetG3.add(action);
            for (int i = 0; i < groupD3Solution.size(); i++) {
                groupD3Solutionbefore.add(groupD3Solution.get(i));
            }
//        System.out.println(groupU1Solutionbefore+"groupU1Solutionbefore");
            G1PSO(random,nw,action,group);//进行粒子群进化并将结果写进groupU1Solution
            evaluateSwarm(groupD3Solution);

//        System.out.println(groupU1Solution+"groupU1Solution");

            double distance = 0.0;
            for (int i = 0; i < groupD3Solution.size(); i++) {
                double objectivebefore = groupD3Solutionbefore.get(i).getObjective(1);
                double objectivenext = groupD3Solution.get(i).getObjective(1);
                double distance0fobjective = objectivenext - objectivebefore;
                distance = distance + distance0fobjective;
            }
//        System.out.println(distance+"dis");

            double reward = R[next1][a];
            double Qvalue = calculateNewQ(reward, Q[next1][a]);
            Q[next1][a] = Qvalue;
            return distance;

        } else if (group==4) {
            PermutationSolution<Integer> action = actionset(a);
            GbestsetG4.add(action);
            for (int i = 0; i < groupUNewSolution.size(); i++) {
                groupUNewSolutionbefore.add(groupUNewSolution.get(i));
            }
//        System.out.println(groupU1Solutionbefore+"groupU1Solutionbefore");
            G1PSO(random,nw,action,group);//进行粒子群进化并将结果写进groupU1Solution
            evaluateSwarm(groupUNewSolution);

//        System.out.println(groupU1Solution+"groupU1Solution");

            double distance = 0.0;
            for (int i = 0; i < groupUNewSolution.size(); i++) {
                double objectivebefore = groupUNewSolutionbefore.get(i).getObjective(6);
                double objectivenext = groupUNewSolution.get(i).getObjective(6);
                double distance0fobjective = objectivenext - objectivebefore;
                distance = distance + distance0fobjective;
            }
//        System.out.println(distance+"dis");

            double reward = R[next1][a];
            double Qvalue = calculateNewQ(reward, Q[next1][a]);
            Q[next1][a] = Qvalue;
            return distance;

        }

        return 0;

    }

    public double calculateG1Q1(double[][] R, double[][] Q, int a, int next1, int Qiannext, double distance) {
        // return (r + rew * q);
        //double reward= R[Qiannext][a];
        double reward = distance;
        Q[Qiannext][a] = reward + gamma * maxNextQ(Q[next1]);
        //Q[Qiannext][a] = (1-alpha) * Q[Qiannext][a] + alpha * (reward+ gamma * maxNextQ(Q[next1]));
        return Q[Qiannext][a];
    }

    @Override
    protected void updatePosition(List<PermutationSolution<Integer>> swarm) {
        long tDualQ0 = System.nanoTime();
        prepareDualQCoordination();
        org.uma.jmetal.algorithm.multiobjective.mypso.v35.V35ModuleTimer.record(
                org.uma.jmetal.algorithm.multiobjective.mypso.v35.V35ModuleTimer.CFVF_PREP_DUALQ,
                System.nanoTime() - tDualQ0, 1L);
        if (globalSearchConfiguration.isQgEnabled()) {
            long tOg = System.nanoTime();
            prepareOriginalQg();
            org.uma.jmetal.algorithm.multiobjective.mypso.v35.V35ModuleTimer.record(
                    org.uma.jmetal.algorithm.multiobjective.mypso.v35.V35ModuleTimer.CFVF_PREP_OG,
                    System.nanoTime() - tOg, 1L);
        }
        if (globalSearchConfiguration.isStructuredBaselineEnabled()) {
            updatePositionWithStructuredBaseline(swarm);
            return;
        }
        if (globalSearchConfiguration.isResourceFlightEnabled()) {
            updatePositionWithCfvf(swarm);
            return;
        }
        if (globalSearchConfiguration.isEvaluatedPddrEnabled()) {
            prepareEvaluatedPddrAuthorUpdate();
        }
//        System.out.println(groupU1Solution);
//        System.out.println(upGr1HisOptIndividual);
//
//        System.out.println(all3GlobalOptIndividuals);
//        sleep();
        Random random = authorRandom();
        double r1, r2;
        double c, m;
        double c_worker;
        double c_machine;
        double m_worker;
        double m_machine;
        int[] nw = DHFSP.nw;
        for (int i = 0; i < upSize; i++) {

            ArrayList<SO> listV = new ArrayList<>();
            ArrayList<SO> listVa = new ArrayList<>();  //用于工厂向量
            //用于工厂向量的DE交换序
            ArrayList<ST> listV2 = new ArrayList<>(); //用于工厂向量
            //用于工厂向量的变异

            int len = 0;
            int len1 = 0;
            Random r = authorRandom();
            int i1 = r.nextInt(upSize);
            //选择一个粒子
            PermutationSolution<Integer> particle = (PermutationSolution<Integer>) groupU1Solution.get(i).copy();
            detachMachineAttribute(particle);

            //Parameters for velocity equation
            r1 = random.nextDouble() * Rand_k;
            r2 = random.nextDouble() * Rand_k;  //生成一个0~Rand_k的数
            //

            //自身初速度
            SO s1 = new SO(formalRandomInt(0, particle.getNumberOfVariables() - 1),
                    formalRandomInt(0, particle.getNumberOfVariables() - 1));
//            System.out.println(s1.getX());
//            System.out.println(s1.getY());
//            sleep();
            listV.add(s1);
//            System.out.println(listV);
//            sleep();
            addNew4JobSequenceVectorByExchangeSequence(particle, listV);

            //历史最优
            listV.clear();
            ArrayList<SO> vtemp1 = getDifferenceOfJobSequenceVectorByExchangeSequence(upGr1HisOptIndividual.get(i1), particle);
            len = (int) (vtemp1.size() * r1);

            for (int j = 0; j < len; j++) {
                listV.add(vtemp1.get(j));
            }
            addNew4JobSequenceVectorByExchangeSequence(particle, listV);

            c = random.nextDouble();
            m = random.nextDouble();
//            System.out.println(groupU1Solution);
//            System.out.println(upGr1HisOptIndividual);
//
//            System.out.println(all3GlobalOptIndividuals);
//            sleep();
            if (c < Cross_c) {
//                ArrayList<SO> vtempa = getDifferenceOfFactoryVectorByExchangeSequence(upGr1HisOptIndividual.get(i), particle); //针对工厂向量的差异
//                len1 = (int) (vtempa.size() * r1);
//                for (int j = 0; j < len1; j++) {
//                    listVa.add(vtempa.get(j));
//                }
//                addNew4FactoryVectorByExchangeSequence(particle, listVa);
//                exchange4WorkerSequence(particle);
                getCrossOfFactoryVectorBySingle(upGr1HisOptIndividual.get(i1), particle);    //单点交叉
            }
            c_worker = random.nextDouble();
            if (c_worker < CrossoverRates4worker) {
//                ArrayList<SO> vtempa = getDifferenceOfFactoryVectorByExchangeSequence(upGr1HisOptIndividual.get(i), particle); //针对工厂向量的差异
//                len1 = (int) (vtempa.size() * r1);
//                for (int j = 0; j < len1; j++) {
//                    listVa.add(vtempa.get(j));
//                }
//                addNew4FactoryVectorByExchangeSequence(particle, listVa);
//                exchange4WorkerSequence(particle);
                crossover4workersequence(upGr1HisOptIndividual.get(i1),particle,nw);//工人向量交叉

            }

            c_machine = random.nextDouble();
            if (c_machine < CrossoverRates4machine) {
//                ArrayList<SO> vtempa = getDifferenceOfFactoryVectorByExchangeSequence(upGr1HisOptIndividual.get(i), particle); //针对工厂向量的差异
//                len1 = (int) (vtempa.size() * r1);
//                for (int j = 0; j < len1; j++) {
//                    listVa.add(vtempa.get(j));
//                }
//                addNew4FactoryVectorByExchangeSequence(particle, listVa);
//                exchange4WorkerSequence(particle);
                crossover4machinesequence(upGr1HisOptIndividual.get(i1),particle,nw);//工人向量交叉
            }


            if (m < Mutation_m) {
                //确定针对工厂向量
                ST q = new ST(formalRandomInt(0, particle.getNumberOfVariables() - 1));
                listV2.add(q);
                addNew4FactoryVectorByRandom(particle, listV2);
            }
            m_worker = random.nextDouble();
            if (m_worker < mutationRate4worker){
                mutation4worker(particle);
            }
            m_machine = random.nextDouble();
            if (m_machine < mutationRate4machine) {
                mutation4machine(particle);
            }

            //全局最优
            listV.clear();
            listVa.clear();
            listV2.clear();

            ArrayList<SO> vtemp2 = getDifferenceOfJobSequenceVectorByExchangeSequence(all3GlobalOptIndividuals.get(0), particle);
            len = (int) (vtemp2.size() * r2);

            for (int j = 0; j < len; j++) {
                listV.add(vtemp2.get(j));
            }
            addNew4JobSequenceVectorByExchangeSequence(particle, listV);

            c = random.nextDouble();
            m = random.nextDouble();
            if (c < Cross_c) {
//                ArrayList<SO> vtempa1 = getDifferenceOfFactoryVectorByExchangeSequence(all3GlobalOptIndividuals.get(0), particle);
//                len1 = (int) (vtempa1.size() * r2);
//                for (int j = 0; j < len1; j++) {
//                    listVa.add(vtempa1.get(j));
//                }
//                addNew4FactoryVectorByExchangeSequence(particle, listVa);
                getCrossOfFactoryVectorBySingle(all3GlobalOptIndividuals.get(0), particle);    //单点交叉
            }
            c_worker = random.nextDouble();
            if (c_worker < CrossoverRates4worker) {
//                ArrayList<SO> vtempa = getDifferenceOfFactoryVectorByExchangeSequence(upGr1HisOptIndividual.get(i), particle); //针对工厂向量的差异
//                len1 = (int) (vtempa.size() * r1);
//                for (int j = 0; j < len1; j++) {
//                    listVa.add(vtempa.get(j));
//                }
//                addNew4FactoryVectorByExchangeSequence(particle, listVa);
//                exchange4WorkerSequence(particle);
                crossover4workersequence(all3GlobalOptIndividuals.get(0),particle,nw);//工人向量交叉

            }
            c_machine = random.nextDouble();
            if (c_machine < CrossoverRates4machine) {
//                ArrayList<SO> vtempa = getDifferenceOfFactoryVectorByExchangeSequence(upGr1HisOptIndividual.get(i), particle); //针对工厂向量的差异
//                len1 = (int) (vtempa.size() * r1);
//                for (int j = 0; j < len1; j++) {
//                    listVa.add(vtempa.get(j));
//                }
//                addNew4FactoryVectorByExchangeSequence(particle, listVa);
//                exchange4WorkerSequence(particle);
                crossover4machinesequence(all3GlobalOptIndividuals.get(0),particle,nw);//工人向量交叉
            }
            if (m < Mutation_m) {
                ST q1 = new ST(formalRandomInt(0, particle.getNumberOfVariables() - 1));
                listV2.add(q1);
                addNew4FactoryVectorByRandom(particle, listV2);
            }
            m_worker = random.nextDouble();
            if (m_worker < mutationRate4worker){
//                mutation4worker(particle);
            }
            m_machine = random.nextDouble();
            if (m_machine < mutationRate4machine) {
                mutation4machine(particle);
            }
            canonicalizeActiveFatigueAuthorUpdate(particle, "G1_CMAX", i);
            groupU1Solution.set(i, particle);
        }


        for (int i = 0; i < centralSize; i++) {
            Random r = authorRandom();
            int i1 = r.nextInt(centralSize);
            ArrayList<SO> listV = new ArrayList<>();
            ArrayList<SO> listVa = new ArrayList<>();
            ArrayList<ST> listV2 = new ArrayList<>();
            int len = 0;
            int len1 = 0;
            PermutationSolution<Integer> particle = (PermutationSolution<Integer>) groupC2Solution.get(i).copy();
            detachMachineAttribute(particle);

            //Parameters for velocity equation
            r1 = random.nextDouble() * Rand_k;
            r2 = random.nextDouble() * Rand_k;  //生成一个0~Rand_k的数
            //

            //自身初速度
            SO s1 = new SO(formalRandomInt(0, particle.getNumberOfVariables() - 1),
                    formalRandomInt(0, particle.getNumberOfVariables() - 1));

            listV.add(s1);
            addNew4JobSequenceVectorByExchangeSequence(particle, listV);

            //历史最优
            listV.clear();
            listVa.clear();
            listV2.clear();

            ArrayList<SO> vtemp1 = getDifferenceOfJobSequenceVectorByExchangeSequence(centralGr2HisOptIndividual.get(i1), particle);
            len = (int) (vtemp1.size() * r1);

            for (int j = 0; j < len; j++) {
                listV.add(vtemp1.get(j));
            }
            addNew4JobSequenceVectorByExchangeSequence(particle, listV);

            c = random.nextDouble();
            m = random.nextDouble();
            if (c < Cross_c) {
//                ArrayList<SO> vtempa = getDifferenceOfFactoryVectorByExchangeSequence(centralGr2HisOptIndividual.get(i), particle);
//                len1 = (int) (vtempa.size() * r1);
//                for (int j = 0; j < len1; j++) {
//                    listVa.add(vtempa.get(j));
//                }
//                addNew4FactoryVectorByExchangeSequence(particle, listVa);
                getCrossOfFactoryVectorBySingle(centralGr2HisOptIndividual.get(i1), particle);    //单点交叉
            }
            c_worker = random.nextDouble();
            if (c_worker < CrossoverRates4worker) {
//                ArrayList<SO> vtempa = getDifferenceOfFactoryVectorByExchangeSequence(upGr1HisOptIndividual.get(i), particle); //针对工厂向量的差异
//                len1 = (int) (vtempa.size() * r1);
//                for (int j = 0; j < len1; j++) {
//                    listVa.add(vtempa.get(j));
//                }
//                addNew4FactoryVectorByExchangeSequence(particle, listVa);
//                exchange4WorkerSequence(particle);
                crossover4workersequence(centralGr2HisOptIndividual.get(i1),particle,nw);//工人向量交叉
            }
            c_machine = random.nextDouble();
            if (c_machine < CrossoverRates4machine) {
//                ArrayList<SO> vtempa = getDifferenceOfFactoryVectorByExchangeSequence(upGr1HisOptIndividual.get(i), particle); //针对工厂向量的差异
//                len1 = (int) (vtempa.size() * r1);
//                for (int j = 0; j < len1; j++) {
//                    listVa.add(vtempa.get(j));
//                }
//                addNew4FactoryVectorByExchangeSequence(particle, listVa);
//                exchange4WorkerSequence(particle);
                crossover4machinesequence(centralGr2HisOptIndividual.get(i1),particle,nw);//工人向量交叉
            }
            if (m < Mutation_m) {
                ST q = new ST(formalRandomInt(0, particle.getNumberOfVariables() - 1));
                listV2.add(q);
                addNew4FactoryVectorByRandom(particle, listV2);
            }

            m_worker = random.nextDouble();
            if (m_worker < mutationRate4worker){
//                mutation4worker(particle);
            }
            m_machine = random.nextDouble();
            if (m_machine < mutationRate4machine) {
                mutation4machine(particle);
            }

            //全局最优
            listV.clear();
            listVa.clear();
            listV2.clear();

            ArrayList<SO> vtemp2 = getDifferenceOfJobSequenceVectorByExchangeSequence(all3GlobalOptIndividuals.get(1), particle);
            len = (int) (vtemp2.size() * r2);

            for (int j = 0; j < len; j++) {
                listV.add(vtemp2.get(j));
            }
            addNew4JobSequenceVectorByExchangeSequence(particle, listV);

            if (c < Cross_c) {
//                ArrayList<SO> vtempa1 = getDifferenceOfFactoryVectorByExchangeSequence(all3GlobalOptIndividuals.get(1), particle);
//                len1 = (int) (vtempa1.size() * r2);
//                for (int j = 0; j < len1; j++) {
//                    listVa.add(vtempa1.get(j));
//                }
//                addNew4FactoryVectorByExchangeSequence(particle, listVa);
                getCrossOfFactoryVectorBySingle(all3GlobalOptIndividuals.get(1), particle);    //单点交叉
            }
            c_worker = random.nextDouble();
            if (c_worker < CrossoverRates4worker) {
//                ArrayList<SO> vtempa = getDifferenceOfFactoryVectorByExchangeSequence(upGr1HisOptIndividual.get(i), particle); //针对工厂向量的差异
//                len1 = (int) (vtempa.size() * r1);
//                for (int j = 0; j < len1; j++) {
//                    listVa.add(vtempa.get(j));
//                }
//                addNew4FactoryVectorByExchangeSequence(particle, listVa);
//                exchange4WorkerSequence(particle);
                crossover4workersequence(all3GlobalOptIndividuals.get(1),particle,nw);//工人向量交叉
            }
            c_machine = random.nextDouble();
            if (c_machine < CrossoverRates4machine) {
//                ArrayList<SO> vtempa = getDifferenceOfFactoryVectorByExchangeSequence(upGr1HisOptIndividual.get(i), particle); //针对工厂向量的差异
//                len1 = (int) (vtempa.size() * r1);
//                for (int j = 0; j < len1; j++) {
//                    listVa.add(vtempa.get(j));
//                }
//                addNew4FactoryVectorByExchangeSequence(particle, listVa);
//                exchange4WorkerSequence(particle);
                crossover4machinesequence(all3GlobalOptIndividuals.get(1),particle,nw);//工人向量交叉
            }
            if (m < Mutation_m) {
                ST q1 = new ST(formalRandomInt(0, particle.getNumberOfVariables() - 1));
                listV2.add(q1);
                addNew4FactoryVectorByRandom(particle, listV2);
            }
            m_worker = random.nextDouble();
            if (m_worker < mutationRate4worker){
//                mutation4worker(particle);
            }
            m_machine = random.nextDouble();
            if (m_machine < mutationRate4machine) {
                mutation4machine(particle);
            }
            canonicalizeActiveFatigueAuthorUpdate(particle, "G4_BALANCED", i);
            groupC2Solution.set(i, particle);
        }

        for (int i = 0; i < downSize; i++) {
            Random r = authorRandom();
            int i1 = r.nextInt(downSize);
            ArrayList<SO> listV = new ArrayList<>();
            ArrayList<SO> listVa = new ArrayList<>();
            ArrayList<ST> listV2 = new ArrayList<>();
            int len = 0;
            int len1 = 0;
            PermutationSolution<Integer> particle = (PermutationSolution<Integer>) groupD3Solution.get(i).copy();
            detachMachineAttribute(particle);

            //Parameters for velocity equation
            r1 = random.nextDouble()* Rand_k;
            r2 = random.nextDouble()* Rand_k;
            //自身初速度
            SO s1 = new SO(formalRandomInt(0, particle.getNumberOfVariables() - 1),
                    formalRandomInt(0, particle.getNumberOfVariables() - 1));

            listV.add(s1);
            addNew4JobSequenceVectorByExchangeSequence(particle, listV);

            //历史最优
            listV.clear();
            listVa.clear();
            listV2.clear();
            ArrayList<SO> vtemp1 = getDifferenceOfJobSequenceVectorByExchangeSequence(downGr3HisOptIndividual.get(i1), particle);
            len = (int) (vtemp1.size() * r1);

            for (int j = 0; j < len; j++) {
                listV.add(vtemp1.get(j));
            }
            addNew4JobSequenceVectorByExchangeSequence(particle, listV);


            c = random.nextDouble();
            m = random.nextDouble();
            if (c < Cross_c) {
//                ArrayList<SO> vtempa = getDifferenceOfFactoryVectorByExchangeSequence(downGr3HisOptIndividual.get(i), particle);
//                len1 = (int) (vtempa.size() * r1);
//                for (int j = 0; j < len1; j++) {
//                    listVa.add(vtempa.get(j));
//                }
//                addNew4FactoryVectorByExchangeSequence(particle, listVa);
                getCrossOfFactoryVectorBySingle(downGr3HisOptIndividual.get(i1), particle);    //单点交叉
            }
            c_worker = random.nextDouble();
            if (c_worker < CrossoverRates4worker) {
//                ArrayList<SO> vtempa = getDifferenceOfFactoryVectorByExchangeSequence(upGr1HisOptIndividual.get(i), particle); //针对工厂向量的差异
//                len1 = (int) (vtempa.size() * r1);
//                for (int j = 0; j < len1; j++) {
//                    listVa.add(vtempa.get(j));
//                }
//                addNew4FactoryVectorByExchangeSequence(particle, listVa);
//                exchange4WorkerSequence(particle);
                crossover4workersequence(downGr3HisOptIndividual.get(i1),particle,nw);//工人向量交叉
            }
            c_machine = random.nextDouble();
            if (c_machine < CrossoverRates4machine) {
//                ArrayList<SO> vtempa = getDifferenceOfFactoryVectorByExchangeSequence(upGr1HisOptIndividual.get(i), particle); //针对工厂向量的差异
//                len1 = (int) (vtempa.size() * r1);
//                for (int j = 0; j < len1; j++) {
//                    listVa.add(vtempa.get(j));
//                }
//                addNew4FactoryVectorByExchangeSequence(particle, listVa);
//                exchange4WorkerSequence(particle);
                crossover4machinesequence(downGr3HisOptIndividual.get(i1),particle,nw);//工人向量交叉
            }
            if (m < Mutation_m) {
                ST q = new ST(formalRandomInt(0, particle.getNumberOfVariables() - 1));
                listV2.add(q);
                addNew4FactoryVectorByRandom(particle, listV2);
            }
            m_worker = random.nextDouble();
            if (m_worker < mutationRate4worker){
//                mutation4worker(particle);
            }
            m_machine = random.nextDouble();
            if (m_machine < mutationRate4machine) {
                mutation4machine(particle);
            }

            //全局最优
            listV.clear();
            listVa.clear();
            listV2.clear();

            ArrayList<SO> vtemp2 = getDifferenceOfJobSequenceVectorByExchangeSequence(all3GlobalOptIndividuals.get(2), particle);
            len = (int) (vtemp2.size() * r2);

            for (int j = 0; j < len; j++) {
                listV.add(vtemp2.get(j));
            }
            addNew4JobSequenceVectorByExchangeSequence(particle, listV);

            c = random.nextDouble();
            m = random.nextDouble();
            if (c < Cross_c) {
//                ArrayList<SO> vtempa1 = getDifferenceOfFactoryVectorByExchangeSequence(all3GlobalOptIndividuals.get(2), particle);
//                len1 = (int) (vtempa1.size() * r2);
//                for (int j = 0; j < len1; j++) {
//                    listVa.add(vtempa1.get(j));
//                }
//                addNew4FactoryVectorByExchangeSequence(particle, listVa);
                getCrossOfFactoryVectorBySingle(all3GlobalOptIndividuals.get(2), particle);    //单点交叉
            }
            c_worker = random.nextDouble();
            if (c_worker < CrossoverRates4worker) {
//                ArrayList<SO> vtempa = getDifferenceOfFactoryVectorByExchangeSequence(upGr1HisOptIndividual.get(i), particle); //针对工厂向量的差异
//                len1 = (int) (vtempa.size() * r1);
//                for (int j = 0; j < len1; j++) {
//                    listVa.add(vtempa.get(j));
//                }
//                addNew4FactoryVectorByExchangeSequence(particle, listVa);
//                exchange4WorkerSequence(particle);
                crossover4workersequence(all3GlobalOptIndividuals.get(2),particle,nw);//工人向量交叉
            }
            c_machine = random.nextDouble();
            if (c_machine < CrossoverRates4machine) {
//                ArrayList<SO> vtempa = getDifferenceOfFactoryVectorByExchangeSequence(upGr1HisOptIndividual.get(i), particle); //针对工厂向量的差异
//                len1 = (int) (vtempa.size() * r1);
//                for (int j = 0; j < len1; j++) {
//                    listVa.add(vtempa.get(j));
//                }
//                addNew4FactoryVectorByExchangeSequence(particle, listVa);
//                exchange4WorkerSequence(particle);
                crossover4machinesequence(all3GlobalOptIndividuals.get(2),particle,nw);//工人向量交叉
            }
            if (m < Mutation_m) {
                ST q1 = new ST(formalRandomInt(0, particle.getNumberOfVariables() - 1));
                listV2.add(q1);
                addNew4FactoryVectorByRandom(particle, listV2);
            }
            m_worker = random.nextDouble();
            if (m_worker < mutationRate4worker){
//                mutation4worker(particle);
            }
            m_machine = random.nextDouble();
            if (m_machine < mutationRate4machine) {
                mutation4machine(particle);
            }
//

            canonicalizeActiveFatigueAuthorUpdate(particle, "G2_TEC", i);
            groupD3Solution.set(i, particle);
        }


        for (int i = 0; i < upNewSize; i++) {
            Random r = authorRandom();
            int i1 = r.nextInt(upNewSize);
            ArrayList<SO> listV = new ArrayList<>();
            ArrayList<SO> listVa = new ArrayList<>();
            ArrayList<ST> listV2 = new ArrayList<>();
            int len = 0;
            int len1 = 0;
            PermutationSolution<Integer> particle = (PermutationSolution<Integer>) groupUNewSolution.get(i).copy();
            detachMachineAttribute(particle);
            //20241105  检查一下怎么在交换工厂的时候判断工厂是否为空

            //Parameters for velocity equation
            r1 = random.nextDouble()* Rand_k;
            r2 = random.nextDouble()* Rand_k;
            //自身初速度
            SO s1 = new SO(formalRandomInt(0, particle.getNumberOfVariables() - 1),
                    formalRandomInt(0, particle.getNumberOfVariables() - 1));

            listV.add(s1);
            addNew4JobSequenceVectorByExchangeSequence(particle, listV);

            //历史最优
            listV.clear();
            listVa.clear();
            listV2.clear();
            ArrayList<SO> vtemp1 = getDifferenceOfJobSequenceVectorByExchangeSequence(upNewGr1HisOptIndividual.get(i1), particle);
            len = (int) (vtemp1.size() * r1);

            for (int j = 0; j < len; j++) {
                listV.add(vtemp1.get(j));
            }
            addNew4JobSequenceVectorByExchangeSequence(particle, listV);

            c = random.nextDouble();
            m = random.nextDouble();
            if (c < Cross_c) {
//                ArrayList<SO> vtempa = getDifferenceOfFactoryVectorByExchangeSequence(upNewGr1HisOptIndividual.get(i), particle);
//                len1 = (int) (vtempa.size() * r1);
//                for (int j = 0; j < len1; j++) {
//                    listVa.add(vtempa.get(j));
//                }
//                addNew4FactoryVectorByExchangeSequence(particle, listVa);
                getCrossOfFactoryVectorBySingle(upNewGr1HisOptIndividual.get(i1), particle);    //单点交叉
            }
            c_worker = random.nextDouble();
            if (c_worker < CrossoverRates4worker) {
//                ArrayList<SO> vtempa = getDifferenceOfFactoryVectorByExchangeSequence(upGr1HisOptIndividual.get(i), particle); //针对工厂向量的差异
//                len1 = (int) (vtempa.size() * r1);
//                for (int j = 0; j < len1; j++) {
//                    listVa.add(vtempa.get(j));
//                }
//                addNew4FactoryVectorByExchangeSequence(particle, listVa);
//                exchange4WorkerSequence(particle);
                crossover4workersequence(upNewGr1HisOptIndividual.get(i1),particle,nw);//工人向量交叉
            }
            c_machine = random.nextDouble();
            if (c_machine < CrossoverRates4machine) {
//                ArrayList<SO> vtempa = getDifferenceOfFactoryVectorByExchangeSequence(upGr1HisOptIndividual.get(i), particle); //针对工厂向量的差异
//                len1 = (int) (vtempa.size() * r1);
//                for (int j = 0; j < len1; j++) {
//                    listVa.add(vtempa.get(j));
//                }
//                addNew4FactoryVectorByExchangeSequence(particle, listVa);
//                exchange4WorkerSequence(particle);
                crossover4machinesequence(upNewGr1HisOptIndividual.get(i1),particle,nw);//工人向量交叉
            }
            if (m < Mutation_m) {
                ST q = new ST(formalRandomInt(0, particle.getNumberOfVariables() - 1));
                listV2.add(q);
                addNew4FactoryVectorByRandom(particle, listV2);
            }
            m_worker = random.nextDouble();
            if (m_worker < mutationRate4worker){
//                mutation4worker(particle);
            }
            m_machine = random.nextDouble();
            if (m_machine < mutationRate4machine) {
                mutation4machine(particle);
            }

            //全局最优
            listV.clear();
            listVa.clear();
            listV2.clear();

            ArrayList<SO> vtemp2 = getDifferenceOfJobSequenceVectorByExchangeSequence(all3GlobalOptIndividuals.get(3), particle);
            len = (int) (vtemp2.size() * r2);

            for (int j = 0; j < len; j++) {
                listV.add(vtemp2.get(j));
            }
            addNew4JobSequenceVectorByExchangeSequence(particle, listV);

            c = random.nextDouble();
            m = random.nextDouble();
            if (c < Cross_c) {
//                ArrayList<SO> vtempa1 = getDifferenceOfFactoryVectorByExchangeSequence(all3GlobalOptIndividuals.get(3), particle);
//                len1 = (int) (vtempa1.size() * r2);
//                for (int j = 0; j < len1; j++) {
//                    listVa.add(vtempa1.get(j));
//                }
//                addNew4FactoryVectorByExchangeSequence(particle, listVa);
                getCrossOfFactoryVectorBySingle(all3GlobalOptIndividuals.get(3), particle);    //单点交叉
            }
            if (c_worker < CrossoverRates4worker) {
//                ArrayList<SO> vtempa = getDifferenceOfFactoryVectorByExchangeSequence(upGr1HisOptIndividual.get(i), particle); //针对工厂向量的差异
//                len1 = (int) (vtempa.size() * r1);
//                for (int j = 0; j < len1; j++) {
//                    listVa.add(vtempa.get(j));
//                }
//                addNew4FactoryVectorByExchangeSequence(particle, listVa);
//                exchange4WorkerSequence(particle);
                crossover4workersequence(all3GlobalOptIndividuals.get(3),particle,nw);//工人向量交叉
            }
            c_machine = random.nextDouble();
            if (c_machine < CrossoverRates4machine) {
//                ArrayList<SO> vtempa = getDifferenceOfFactoryVectorByExchangeSequence(upGr1HisOptIndividual.get(i), particle); //针对工厂向量的差异
//                len1 = (int) (vtempa.size() * r1);
//                for (int j = 0; j < len1; j++) {
//                    listVa.add(vtempa.get(j));
//                }
//                addNew4FactoryVectorByExchangeSequence(particle, listVa);
//                exchange4WorkerSequence(particle);
                crossover4machinesequence(all3GlobalOptIndividuals.get(3),particle,nw);//工人向量交叉
            }
            if (m < Mutation_m) {
                ST q1 = new ST(formalRandomInt(0, particle.getNumberOfVariables() - 1));
                listV2.add(q1);
                addNew4FactoryVectorByRandom(particle, listV2);
            }
            m_worker = random.nextDouble();
            if (m_worker < mutationRate4worker){
//                mutation4worker(particle);
            }
            m_machine = random.nextDouble();
            if (m_machine < mutationRate4machine) {
                mutation4machine(particle);
            }
            canonicalizeActiveFatigueAuthorUpdate(particle, "G3_TWC", i);
            groupUNewSolution.set(i, particle);
        }

        List<PermutationSolution<Integer>> tempSwarm = new ArrayList<>();
        for (int i = 0; i < swarm.size(); i++) {
            tempSwarm.add(swarm.get(i));
        }
        merge(swarm);
        if (globalSearchConfiguration.isEvaluatedPddrEnabled()) {
            if (globalSearchConfiguration.isLocalSearchEnabled()) {
                clearPreEvaluationMarkers(swarm);
            }
            annotateSubSwarmSlots(swarm);
            pendingPddrOffspringHistories =
                    ZhangBoSolutionSupport.deepCopyHistories(this.tempSwarm);
            return;
        }
        if (globalSearchConfiguration.isQgEnabled()) {
            annotateSubSwarmSlots(swarm);
            annotateSubSwarmSlots(tempSwarm);
        }
        int[] DEswarmtempPdflag = new int[swarmSize];
        for (int i = 0; i < swarmSize; i++) {
            DEswarmtempPdflag[i] = i;
        }

        swarm = PDDRFFselect(swarm,tempSwarm,DEswarmtempPdflag);


    }

    private void prepareEvaluatedPddrAuthorUpdate() {
        if (usesArchivePersonalLeader()) isolateQpGroupBranches();
        List<PermutationSolution<Integer>> groupedParents = groupedCurrentSolutions();
        annotateSubSwarmSlots(groupedParents);
        if (globalSearchConfiguration.isLineageArchiveEnabled()
                && zhangBoLineageCoordinator.getMemories().isEmpty()) {
            zhangBoLineageCoordinator.initialize(groupedParents, globallyOptimalIndividual,
                    generationNumber());
        } else if (globalSearchConfiguration.isLineageArchiveEnabled()) {
            zhangBoLineageCoordinator.freezeBounds(groupedParents, globallyOptimalIndividual);
        }
        if (globalSearchConfiguration.isQpEnabled()) {
            if (pendingDualQDecision != null && pendingDualQDecision.isWarmup()) {
                prepareWarmupPersonalLeaders();
            } else {
                prepareQpSelections();
            }
        } else if (usesArchivePersonalLeader()) {
            prepareP8ArchivePersonalLeaders();
        }
        pendingPddrParents = ZhangBoSolutionSupport.deepCopySolutions(groupedParents);
        pendingPddrParentHistories = groupedAuthorHistories();
        if (usesArchivePersonalLeader()) replaceAuthorPersonalHistoriesFromArchive();
    }

    private void replaceAuthorPersonalHistoriesFromArchive() {
        upGr1HisOptIndividual = archiveLeadersFor(groupU1Solution, ZhangBoSubSwarm.G1_CMAX);
        centralGr2HisOptIndividual = archiveLeadersFor(
                groupC2Solution, ZhangBoSubSwarm.G4_BALANCED);
        downGr3HisOptIndividual = archiveLeadersFor(groupD3Solution, ZhangBoSubSwarm.G2_TEC);
        upNewGr1HisOptIndividual = archiveLeadersFor(
                groupUNewSolution, ZhangBoSubSwarm.G3_TWC);
    }

    private List<PermutationSolution<Integer>> archiveLeadersFor(
            List<PermutationSolution<Integer>> particles, ZhangBoSubSwarm group) {
        List<PermutationSolution<Integer>> result = new ArrayList<>();
        for (PermutationSolution<Integer> particle : particles) {
            Object value = particle.getAttribute(ZhangBoQpBranchTag.class);
            if (!(value instanceof ZhangBoQpBranchTag)) {
                throw new IllegalStateException("Archive author-update particle has no branch tag");
            }
            ZhangBoPersonalLeaderDecision decision = pendingPersonalLeaders.get(
                    ((ZhangBoQpBranchTag) value).getBranchId());
            if (decision == null || decision.getGroup() != group) {
                throw new IllegalStateException("Missing archive author-update personal leader");
            }
            result.add(decision.pbestSolution(particle));
        }
        return result;
    }

    private void prepareDualQCoordination() {
        if (!globalSearchConfiguration.isBlockFrozenDualQEnabled()) {
            pendingDualQDecision = null;
            return;
        }
        dualQRoundCounter++;
        pendingDualQDecision = zhangBoDualQCoordinator.decide(
                fullEvaluationCount, maxIterations, swarmSize,
                dualQRoundCounter, dualQWarmupEndOuterGeneration);
        if (!pendingDualQDecision.isWarmup() && dualQWarmupEndOuterGeneration < 0L) {
            dualQWarmupEndOuterGeneration = dualQRoundCounter;
            pendingDualQDecision = zhangBoDualQCoordinator.decide(
                    fullEvaluationCount, maxIterations, swarmSize,
                    dualQRoundCounter, dualQWarmupEndOuterGeneration);
        }
        ZhangBoDualQCoordinator.Phase phase = pendingDualQDecision.getPhase();
        Long count = dualQPhaseCounts.get(phase);
        dualQPhaseCounts.put(phase, count == null ? 1L : count + 1L);
        pendingQgTableHashBefore = zhangBoQgController.tableHash();
        pendingQpTableHashBefore = zhangBoQpController.tableHash();
        pendingQgSelectionsBefore = zhangBoQgController.getSelectionCount();
        pendingQgUpdatesBefore = zhangBoQgController.getTdUpdateCount();
        pendingQpActionsBefore = zhangBoQpController.getExecutedActionCount();
        pendingQpTransitionsBefore = zhangBoQpController.getTrainedTransitionCount();
    }

    private void prepareOriginalQg() {
        pendingQgSelections.clear();
        pendingQgBefore.clear();
        long tOgCopy0 = System.nanoTime();
        List<PermutationSolution<Integer>> candidates = new ArrayList<>();
        for (PermutationSolution<Integer> solution : globallyOptimalIndividual) {
            candidates.add((PermutationSolution<Integer>) solution.copy());
        }
        org.uma.jmetal.algorithm.multiobjective.mypso.v35.V35ModuleTimer.record(
                org.uma.jmetal.algorithm.multiobjective.mypso.v35.V35ModuleTimer.OG_ARCHIVE_COPY,
                System.nanoTime() - tOgCopy0, 1L);
        org.uma.jmetal.algorithm.multiobjective.mypso.v35.V35ModuleTimer.increment(
                org.uma.jmetal.algorithm.multiobjective.mypso.v35.V35ModuleTimer.C_ARCHIVE_SCAN, 1L);
        org.uma.jmetal.algorithm.multiobjective.mypso.v35.V35ModuleTimer.increment(
                org.uma.jmetal.algorithm.multiobjective.mypso.v35.V35ModuleTimer.C_ARCHIVE_ITEMS,
                globallyOptimalIndividual.size());
        if (candidates.isEmpty()) {
            candidates.addAll(copySolutions(groupU1Solution));
            candidates.addAll(copySolutions(groupC2Solution));
            candidates.addAll(copySolutions(groupD3Solution));
            candidates.addAll(copySolutions(groupUNewSolution));
        }
        long tOgDscr0 = System.nanoTime();
        if (globalSearchConfiguration.isDscrEnabled()) {
            candidates = applyV35Dscr(candidates);
            org.uma.jmetal.algorithm.multiobjective.mypso.v35.V35ModuleTimer.increment(
                    org.uma.jmetal.algorithm.multiobjective.mypso.v35.V35ModuleTimer.C_DSCR_CALLS, 1L);
        }
        org.uma.jmetal.algorithm.multiobjective.mypso.v35.V35ModuleTimer.record(
                org.uma.jmetal.algorithm.multiobjective.mypso.v35.V35ModuleTimer.OG_DSCR,
                System.nanoTime() - tOgDscr0, 1L);
        long tOgSelect0 = System.nanoTime();
        selectQgLeader(ZhangBoSubSwarm.G1_CMAX, groupU1Solution, candidates);
        selectQgLeader(ZhangBoSubSwarm.G4_BALANCED, groupC2Solution, candidates);
        selectQgLeader(ZhangBoSubSwarm.G2_TEC, groupD3Solution, candidates);
        selectQgLeader(ZhangBoSubSwarm.G3_TWC, groupUNewSolution, candidates);
        org.uma.jmetal.algorithm.multiobjective.mypso.v35.V35ModuleTimer.record(
                org.uma.jmetal.algorithm.multiobjective.mypso.v35.V35ModuleTimer.OG_LEADER_SELECT,
                System.nanoTime() - tOgSelect0, 1L);
        all3GlobalOptIndividuals.clear();
        for (ZhangBoSubSwarm group : new ZhangBoSubSwarm[]{
                ZhangBoSubSwarm.G1_CMAX, ZhangBoSubSwarm.G4_BALANCED,
                ZhangBoSubSwarm.G2_TEC, ZhangBoSubSwarm.G3_TWC}) {
            all3GlobalOptIndividuals.add(pendingQgSelections.get(group).getLeader());
        }
        zhangBoP6Events.add("generation=" + currentIteration + ":Qg leaders prepared");
    }

    /**
     * v3.5 DSCR: refresh the social candidate snapshot using only evaluated,
     * strictly nondominated three-objective teachers.  This is a zero-FE
     * cache operation and does not touch Q values or random streams.
     */
    private List<PermutationSolution<Integer>> applyV35Dscr(
            List<PermutationSolution<Integer>> candidates) {
        org.uma.jmetal.algorithm.multiobjective.mypso.v35.V35ModuleTimer.increment(
                org.uma.jmetal.algorithm.multiobjective.mypso.v35.V35ModuleTimer.C_SOCIAL_CANDIDATE_BUILD, 1L);
        V35SocialKnowledgeSnapshot snapshot =
                V35SocialKnowledgeSnapshot.fromEvaluatedSolutions(candidates);
        org.uma.jmetal.algorithm.multiobjective.mypso.v35.V35ModuleTimer.increment(
                org.uma.jmetal.algorithm.multiobjective.mypso.v35.V35ModuleTimer.C_ARCHIVE_SCAN, 1L);
        org.uma.jmetal.algorithm.multiobjective.mypso.v35.V35ModuleTimer.increment(
                org.uma.jmetal.algorithm.multiobjective.mypso.v35.V35ModuleTimer.C_ARCHIVE_ITEMS,
                candidates.size());
        pendingV35SocialSnapshot = snapshot;
        v35DscrDecisionCycle++;
        for (ZhangBoSubSwarm group : ZhangBoSubSwarmSemantics.roles()) {
            org.uma.jmetal.algorithm.multiobjective.mypso.v35.V35ModuleTimer.increment(
                    org.uma.jmetal.algorithm.multiobjective.mypso.v35.V35ModuleTimer.C_ARCHIVE_SCAN, 1L);
            org.uma.jmetal.algorithm.multiobjective.mypso.v35.V35ModuleTimer.increment(
                    org.uma.jmetal.algorithm.multiobjective.mypso.v35.V35ModuleTimer.C_ARCHIVE_ITEMS,
                    snapshot.getTeachers().size());
            List<V35DscrTeacherCache.Refresh> refreshes =
                    zhangBoQgController.sanitizeTeacherCaches(group, snapshot,
                            v35DscrTeacherCache, v35DscrDecisionCycle,
                            generationNumber(), fullEvaluationCount);
            for (V35DscrTeacherCache.Refresh refresh : refreshes) {
                zhangBoP6Events.add("DSCR_REFRESH cycle=" + refresh.getDecisionCycle()
                        + ",generation=" + refresh.getGeneration() + ",FE=" + refresh.getFe()
                        + ",role=" + refresh.getRole() + ",cacheType=" + refresh.getCacheType()
                        + ",teacherBefore=" + refresh.getBefore().getFingerprint()
                        + ",teacherAfter=" + refresh.getAfter().getFingerprint()
                        + ",dominatorCount=" + refresh.getDominatorCount()
                        + ",stale=" + refresh.isStale() + ",replaced=" + refresh.isReplaced()
                        + ",dominanceAge=" + refresh.getDominanceAge());
            }
        }
        List<PermutationSolution<Integer>> filtered = new ArrayList<>();
        int removed = 0;
        for (PermutationSolution<Integer> solution : candidates) {
            double[] objective = {solution.getObjective(0), solution.getObjective(1), solution.getObjective(6)};
            // FC-TIME-2-A1: fingerprint(solution) is a pure function of the frozen
            // four-vector genotype, so it is computed once per solution and reused
            // across all teachers (was: recomputed for every (solution, teacher) pair).
            String solutionFingerprint = ZhangBoQgController.fingerprint(solution);
            boolean dominated = false;
            for (V35SocialTeacher teacher : snapshot.getTeachers()) {
                org.uma.jmetal.algorithm.multiobjective.mypso.v35.V35ModuleTimer.increment(
                        org.uma.jmetal.algorithm.multiobjective.mypso.v35.V35ModuleTimer.C_FP_BEFORE, 1L);
                org.uma.jmetal.algorithm.multiobjective.mypso.v35.V35ModuleTimer.increment(
                        org.uma.jmetal.algorithm.multiobjective.mypso.v35.V35ModuleTimer.C_FP_REUSE, 1L);
                org.uma.jmetal.algorithm.multiobjective.mypso.v35.V35ModuleTimer.increment(
                        org.uma.jmetal.algorithm.multiobjective.mypso.v35.V35ModuleTimer.C_DOMINATES_CALLS, 1L);
                if (teacher.getFingerprint().equals(solutionFingerprint)) continue;
                if (V35DscrSanitizer.strictlyDominates(teacher.getObjectives(), objective)) {
                    dominated = true;
                    break;
                }
            }
            if (dominated) removed++; else filtered.add(solution);
        }
        org.uma.jmetal.algorithm.multiobjective.mypso.v35.V35ModuleTimer.increment(
                org.uma.jmetal.algorithm.multiobjective.mypso.v35.V35ModuleTimer.C_FP_ACTUAL,
                candidates.size());
        if (filtered.isEmpty()) filtered.add(candidates.get(0));
        zhangBoP6Events.add("generation=" + currentIteration + ":DSCR evaluatedSnapshot="
                + candidates.size() + ",retained=" + filtered.size() + ",removed=" + removed);
        return filtered;
    }

    private void selectQgLeader(ZhangBoSubSwarm group,
                                List<PermutationSolution<Integer>> before,
                                List<PermutationSolution<Integer>> candidates) {
        pendingQgBefore.put(group, copySolutions(before));
        boolean frozenGreedy = pendingDualQDecision != null
                && pendingDualQDecision.isPBlock();
        ZhangBoQgController.Selection selection = frozenGreedy
                ? zhangBoQgController.selectGreedy(group, candidates)
                : zhangBoQgController.select(group, candidates);
        pendingQgSelections.put(group, selection);
        // V35-SOURCE-ATTRIBUTION-PATCH: Qg teacher/action round context (pure observation).
        org.uma.jmetal.algorithm.multiobjective.mypso.v35.V35SourceAttributionObserver.onQgSelection(group == null ? "UNASSIGNED" : group.name(),
                selection.getAction(), selection.getLeader());
        // FC-6B: region membership is observational only.  Qg remains free
        // to select a cross-region teacher; the audit records exposure rather
        // than adding a new teacher gate.
        V35Fc6LocalCandidateAudit fc6LocalAudit = V35Fc6LocalCandidateAudit.current();
        if (fc6LocalAudit != null) {
            fc6LocalAudit.recordTeacherExposure((int) formalBaselineOuterCycles + 1,
                    group, selection.getLeader());
        }
        // FC-6A-POST / Build-C2: Qg 教师曝光（纯观察；按 fingerprint 匹配 rescue 注册表）。
        if (org.uma.jmetal.algorithm.multiobjective.mypso.v35.V35Fc6BpPddrDiagnosticAudit
                .isEnabled()) {
            org.uma.jmetal.algorithm.multiobjective.mypso.v35.V35Fc6BpPddrDiagnosticAudit
                    .current().observeQgTeacher(group, selection.getLeader(),
                    fullEvaluationCount, (int) formalBaselineOuterCycles + 1);
        }
        // FC-5: mark the selected Qg leader's lineage as teacher-exposed.
        v35CmaxLifecycleAudit.markTeacher(lineageId(selection.getLeader()));
        // FC-5.1: is the selected Qg leader the archive-best Cmax solution?
        v35CmaxLifecycleAudit.observeQgTeacher(lineageId(selection.getLeader()));
        if (globalSearchConfiguration.isDscrEnabled()) {
            if (pendingV35SocialSnapshot == null) {
                throw new IllegalStateException("DSCR selection has no frozen social snapshot");
            }
            PermutationSolution<Integer> leader = selection.getLeader();
            V35DscrTeacherCache.TeacherUse use = v35DscrTeacherCache.recordTeacherUse(
                    v35DscrDecisionCycle, generationNumber(), fullEvaluationCount,
                    v35Role(group), new V35SocialTeacher(new double[]{leader.getObjective(0),
                            leader.getObjective(1), leader.getObjective(6)},
                            ZhangBoQgController.fingerprint(leader)), pendingV35SocialSnapshot);
            zhangBoP6Events.add("DSCR_TEACHER_USE cycle=" + v35DscrDecisionCycle
                    + ",generation=" + generationNumber() + ",FE=" + fullEvaluationCount
                    + ",group=" + group + ",teacher=" + ZhangBoQgController.fingerprint(leader)
                    + ",dominated=" + use.isDominated());
            if (use.isDominated()) {
                throw new IllegalStateException("DSCR selected a strictly dominated Qg teacher for " + group);
            }
        }
    }

    private void settleOriginalQg(List<PermutationSolution<Integer>> evaluated) {
        if (pendingQgSelections.isEmpty()) {
            return;
        }
        Map<ZhangBoSubSwarm, List<PermutationSolution<Integer>>> after =
                new EnumMap<>(ZhangBoSubSwarm.class);
        for (ZhangBoSubSwarm group : ZhangBoSubSwarmSemantics.roles()) {
            after.put(group, new ArrayList<PermutationSolution<Integer>>());
        }
        for (PermutationSolution<Integer> solution : evaluated) {
            Object tag = solution.getAttribute(ZhangBoSubSwarm.class);
            if (tag instanceof ZhangBoSubSwarm) {
                after.get((ZhangBoSubSwarm) tag).add(solution);
            }
        }
        for (ZhangBoSubSwarm group : ZhangBoSubSwarmSemantics.roles()) {
            List<PermutationSolution<Integer>> groupAfter = after.get(group);
            if (groupAfter.isEmpty()) {
                groupAfter = pendingQgBefore.get(group);
                zhangBoP6Events.add("generation=" + currentIteration + ":" + group
                        + ":empty selected subgroup; zero-change fallback");
            }
            if (pendingDualQDecision != null && pendingDualQDecision.isPBlock()) {
                double softRho = dualQSoftFreezeRho();
                Long gbestContrib = pendingCfvfGbestContrib.get(group);
                if (softRho > 0.0 && gbestContrib != null && gbestContrib > 0L) {
                    double reward = zhangBoQgController.settleWithScaledAlpha(
                            pendingQgSelections.get(group), pendingQgBefore.get(group),
                            groupAfter, softRho);
                    zhangBoP6Events.add("generation=" + currentIteration + ":" + group
                            + ":softFrozenQgReward=" + reward
                            + ",gbestContrib=" + gbestContrib + ",rho=" + softRho);
                } else {
                    double delta = zhangBoQgController.observeWithoutUpdate(
                            pendingQgSelections.get(group), pendingQgBefore.get(group), groupAfter);
                    zhangBoP6Events.add("generation=" + currentIteration + ":" + group
                            + ":frozenQgDelta=" + delta);
                }
            } else {
                double reward = zhangBoQgController.settle(
                        pendingQgSelections.get(group), pendingQgBefore.get(group), groupAfter);
                zhangBoP6Events.add("generation=" + currentIteration + ":" + group
                        + ":reward=" + reward);
            }
        }
        pendingQgSelections.clear();
        pendingQgBefore.clear();
        pendingCfvfGbestContrib.clear();
    }

    /** Structured replayable baseline used by formal P8 author-compatible controls. */
    private void updatePositionWithStructuredBaseline(
            List<PermutationSolution<Integer>> swarm) {
        if (globalSearchConfiguration.isEvaluatedPddrEnabled()) {
            prepareEvaluatedPddrAuthorUpdate();
        }
        updateStructuredBaselineGroup(ZhangBoSubSwarm.G1_CMAX, groupU1Solution,
                upGr1HisOptIndividual, all3GlobalOptIndividuals.get(0));
        updateStructuredBaselineGroup(ZhangBoSubSwarm.G4_BALANCED, groupC2Solution,
                centralGr2HisOptIndividual, all3GlobalOptIndividuals.get(1));
        updateStructuredBaselineGroup(ZhangBoSubSwarm.G2_TEC, groupD3Solution,
                downGr3HisOptIndividual, all3GlobalOptIndividuals.get(2));
        updateStructuredBaselineGroup(ZhangBoSubSwarm.G3_TWC, groupUNewSolution,
                upNewGr1HisOptIndividual, all3GlobalOptIndividuals.get(3));

        if (globalSearchConfiguration.isEvaluatedPddrEnabled()) {
            merge(swarm);
            if (globalSearchConfiguration.isLocalSearchEnabled()) {
                clearPreEvaluationMarkers(swarm);
            }
            annotateSubSwarmSlots(swarm);
            pendingPddrOffspringHistories =
                    ZhangBoSolutionSupport.deepCopyHistories(tempSwarm);
            return;
        }
        List<PermutationSolution<Integer>> previous = copySolutions(swarm);
        merge(swarm);
        annotateSubSwarmSlots(swarm);
        annotateSubSwarmSlots(previous);
        int[] flags = new int[swarmSize];
        for (int index = 0; index < flags.length; index++) flags[index] = index;
        PDDRFFselect(swarm, previous, flags);
    }

    private void updateStructuredBaselineGroup(
            ZhangBoSubSwarm group,
            List<PermutationSolution<Integer>> particles,
            List<PermutationSolution<Integer>> personalHistory,
            PermutationSolution<Integer> socialLeader) {
        if (personalHistory.isEmpty()) {
            throw new IllegalStateException("Missing personal history for " + group);
        }
        for (int index = 0; index < particles.size(); index++) {
            PermutationSolution<Integer> current = particles.get(index);
            PermutationSolution<Integer> personalLeader;
            int personalIndex = -1;
            if (usesArchivePersonalLeader()) {
                Object branchValue = current.getAttribute(ZhangBoQpBranchTag.class);
                if (!(branchValue instanceof ZhangBoQpBranchTag)) {
                    throw new IllegalStateException("Archive-guided baseline particle has no branch tag");
                }
                ZhangBoPersonalLeaderDecision decision = pendingPersonalLeaders.get(
                        ((ZhangBoQpBranchTag) branchValue).getBranchId());
                if (decision == null || decision.getGroup() != group) {
                    throw new IllegalStateException("Missing archive baseline personal leader");
                }
                personalLeader = decision.pbestSolution(current);
            } else {
                personalIndex = zhangBoP6Random.nextInt(0, personalHistory.size() - 1);
                personalLeader = personalHistory.get(personalIndex);
            }
            ZhangBoBaselineUpdater.Result result = formalBaselineConfiguration.isEnabled()
                    ? zhangBoBaselineUpdater.update(current, personalLeader, socialLeader,
                    zhangBoResourceDomain,
                    formalBaselineConfiguration.getRandomCoefficientUpperBound(),
                    formalBaselineConfiguration.getFaCrossover(),
                    formalBaselineConfiguration.getMaCrossover(),
                    formalBaselineConfiguration.getWaCrossover(),
                    formalBaselineConfiguration.getFaMutation(),
                    formalBaselineConfiguration.getMaMutation(),
                    formalBaselineConfiguration.getWaMutation(), zhangBoP6Random)
                    : zhangBoBaselineUpdater.update(current, personalLeader, socialLeader,
                    zhangBoResourceDomain, Rand_k, Cross_c, Mutation_m,
                    mutationRate4machine, mutationRate4worker, zhangBoP6Random);
            PermutationSolution<Integer> offspring = result.getSolution();
            offspring.setAttribute(ZhangBoSubSwarm.class, group);
            particles.set(index, offspring);
            zhangBoP6Events.add("generation=" + currentIteration + ":baseline=" + group
                    + ":particle=" + index + ":pbestIndex=" + personalIndex
                    + ":events=" + result.getEvents().size());
            baselineUpdateEventCount++;
        }
    }

    private void updatePositionWithCfvf(List<PermutationSolution<Integer>> swarm) {
        long tCfvfPrep0 = System.nanoTime();
        if (globalSearchConfiguration.isEvaluatedPddrEnabled()) {
            if (usesArchivePersonalLeader()) {
                isolateQpGroupBranches();
            }
            List<PermutationSolution<Integer>> groupedParents = groupedCurrentSolutions();
            annotateSubSwarmSlots(groupedParents);
            if (globalSearchConfiguration.isLineageArchiveEnabled()
                    && zhangBoLineageCoordinator.getMemories().isEmpty()) {
                zhangBoLineageCoordinator.initialize(groupedParents, globallyOptimalIndividual,
                        generationNumber());
            } else if (globalSearchConfiguration.isLineageArchiveEnabled()) {
                zhangBoLineageCoordinator.freezeBounds(groupedParents, globallyOptimalIndividual);
            }
            if (globalSearchConfiguration.isQpEnabled()) {
                if (pendingDualQDecision != null && pendingDualQDecision.isWarmup()) {
                    prepareWarmupPersonalLeaders();
                } else {
                    prepareQpSelections();
                }
            } else if (usesArchivePersonalLeader()) {
                prepareP8ArchivePersonalLeaders();
            }
            pendingPddrParents = ZhangBoSolutionSupport.deepCopySolutions(groupedParents);
            pendingPddrParentHistories = groupedAuthorHistories();
        }
        org.uma.jmetal.algorithm.multiobjective.mypso.v35.V35ModuleTimer.record(
                org.uma.jmetal.algorithm.multiobjective.mypso.v35.V35ModuleTimer.CFVF_PREP,
                System.nanoTime() - tCfvfPrep0, 1L);

        long tCfvfGroup0 = System.nanoTime();
        updateCfvfGroup(ZhangBoSubSwarm.G1_CMAX, groupU1Solution,
                upGr1HisOptIndividual, all3GlobalOptIndividuals.get(0));
        updateCfvfGroup(ZhangBoSubSwarm.G4_BALANCED, groupC2Solution,
                centralGr2HisOptIndividual, all3GlobalOptIndividuals.get(1));
        updateCfvfGroup(ZhangBoSubSwarm.G2_TEC, groupD3Solution,
                downGr3HisOptIndividual, all3GlobalOptIndividuals.get(2));
        updateCfvfGroup(ZhangBoSubSwarm.G3_TWC, groupUNewSolution,
                upNewGr1HisOptIndividual, all3GlobalOptIndividuals.get(3));
        org.uma.jmetal.algorithm.multiobjective.mypso.v35.V35ModuleTimer.record(
                org.uma.jmetal.algorithm.multiobjective.mypso.v35.V35ModuleTimer.CFVF_GROUP,
                System.nanoTime() - tCfvfGroup0, 1L);

        long tCfvfTail0 = System.nanoTime();
        if (globalSearchConfiguration.isEvaluatedPddrEnabled()) {
            merge(swarm);
            if (globalSearchConfiguration.isLocalSearchEnabled()) {
                clearPreEvaluationMarkers(swarm);
            }
            annotateSubSwarmSlots(swarm);
            pendingPddrOffspringHistories =
                    ZhangBoSolutionSupport.deepCopyHistories(tempSwarm);
            org.uma.jmetal.algorithm.multiobjective.mypso.v35.V35ModuleTimer.record(
                    org.uma.jmetal.algorithm.multiobjective.mypso.v35.V35ModuleTimer.CFVF_TAIL,
                    System.nanoTime() - tCfvfTail0, 1L);
            return;
        }

        List<PermutationSolution<Integer>> previous = copySolutions(swarm);
        merge(swarm);
        annotateSubSwarmSlots(swarm);
        annotateSubSwarmSlots(previous);
        int[] flags = new int[swarmSize];
        for (int index = 0; index < flags.length; index++) flags[index] = index;
        PDDRFFselect(swarm, previous, flags);
        org.uma.jmetal.algorithm.multiobjective.mypso.v35.V35ModuleTimer.record(
                org.uma.jmetal.algorithm.multiobjective.mypso.v35.V35ModuleTimer.CFVF_TAIL,
                System.nanoTime() - tCfvfTail0, 1L);
    }

    private List<PermutationSolution<Integer>> groupedCurrentSolutions() {
        List<PermutationSolution<Integer>> result = new ArrayList<>(swarmSize);
        result.addAll(groupU1Solution);
        result.addAll(groupC2Solution);
        result.addAll(groupD3Solution);
        result.addAll(groupUNewSolution);
        if (result.size() != swarmSize) {
            throw new IllegalStateException("Grouped parent size=" + result.size()
                    + ", expected=" + swarmSize);
        }
        return result;
    }

    private List<List<PermutationSolution<Integer>>> groupedAuthorHistories() {
        List<List<PermutationSolution<Integer>>> result = new ArrayList<>(swarmSize);
        result.addAll(ZhangBoSolutionSupport.deepCopyHistories(upGroup1Population));
        result.addAll(ZhangBoSolutionSupport.deepCopyHistories(centralGroup2Population));
        result.addAll(ZhangBoSolutionSupport.deepCopyHistories(downGroup3Population));
        result.addAll(ZhangBoSolutionSupport.deepCopyHistories(upNewGroup1Population));
        if (result.size() != swarmSize) {
            throw new IllegalStateException("Grouped history size=" + result.size()
                    + ", expected=" + swarmSize);
        }
        return result;
    }

    private int generationNumber() {
        return Math.max(0, currentIteration / Math.max(1, swarmSize));
    }

    @SuppressWarnings("unchecked")
    private static void detachMachineAttribute(PermutationSolution<Integer> solution) {
        Object value = solution.getAttribute("machine");
        if (value instanceof List) {
            solution.setAttribute("machine", new ArrayList<>((List<Integer>) value));
        }
    }

    /**
     * P5.1 legality closure for the legacy author updater. This runs while the
     * offspring is being constructed, before evaluation; the evaluator itself
     * remains strict and never repairs an illegal MA/WA.
     */
    @SuppressWarnings("unchecked")
    private void canonicalizeActiveFatigueAuthorUpdate(
            PermutationSolution<Integer> solution, String group, int particleIndex) {
        if (problemContext == null) return;
        ZhangBoProblemContext fatigueProblem = problemContext;
        if (fatigueProblem.getFatigueParameters() == null
                || fatigueProblem.getFatigueParameters().isZeroImpact()) return;
        Object machineAttribute = solution.getAttribute("machine");
        if (!(machineAttribute instanceof List)) {
            throw new IllegalArgumentException("Active fatigue offspring has no machine vector");
        }
        List<Integer> machines = (List<Integer>) machineAttribute;
        for (int position = 0; position < solution.getNumberOfVariables(); position++) {
            int factory = solution.getVariableValueid(position);
            int machineCount = fatigueProblem.getFatigueInstanceData()
                    .getMachineCount(factory, 0);
            Integer machine = machines.get(position);
            if (machine == null || machine < 0 || machine >= machineCount) {
                int old = machine == null ? -1 : machine;
                machines.set(position, 0);
                authorUpdateResourceCorrections++;
                zhangBoP6Events.add("authorUpdateCanonicalization:group=" + group
                        + ",particle=" + particleIndex + ",job="
                        + solution.getVariableValue(position) + ",vector=MA,old=" + old
                        + ",new=0");
            }
            int worker = solution.getVariableValueworker(position);
            if (!fatigueProblem.getFatigueInstanceData().isWorkerEligible(factory, 0, worker)) {
                int replacement = fatigueProblem.getFatigueInstanceData()
                        .getEligibleWorkers(factory, 0)[0];
                solution.setVariableValueworker(position, replacement);
                authorUpdateResourceCorrections++;
                zhangBoP6Events.add("authorUpdateCanonicalization:group=" + group
                        + ",particle=" + particleIndex + ",job="
                        + solution.getVariableValue(position) + ",vector=WA,old=" + worker
                        + ",new=" + replacement);
            }
        }
    }

    @SuppressWarnings("unchecked")
    private void canonicalizeInitialCfvfResources(
            PermutationSolution<Integer> solution, int particleIndex) {
        List<Integer> machines = ZhangBoMachineVectorSupport.copy(
                solution, solution.getNumberOfVariables());
        for (int position = 0; position < solution.getNumberOfVariables(); position++) {
            int job = solution.getVariableValue(position);
            int factory = solution.getVariableValueid(position);
            if (!zhangBoResourceDomain.isFactoryValid(factory)) {
                throw new IllegalArgumentException("CFVF initial FA is invalid at position=" + position
                        + ", value=" + factory);
            }
            int machine = machines.get(position);
            if (!zhangBoResourceDomain.isMachineValid(factory, machine)) {
                int replacement = zhangBoResourceDomain.firstMachine(factory);
                machines.set(position, replacement);
                cfvfInitializationCorrections++;
                zhangBoP6Events.add("initializationCanonicalization:particle=" + particleIndex
                        + ",job=" + job + ",vector=MA,old=" + machine + ",new=" + replacement
                        + ",reason=author_150_8_5_1_domain_mismatch");
            }
            int worker = solution.getVariableValueworker(position);
            if (!zhangBoResourceDomain.isWorkerValid(factory, worker)) {
                int replacement = zhangBoResourceDomain.firstWorker(factory);
                solution.setVariableValueworker(position, replacement);
                cfvfInitializationCorrections++;
                zhangBoP6Events.add("initializationCanonicalization:particle=" + particleIndex
                        + ",job=" + job + ",vector=WA,old=" + worker + ",new=" + replacement
                        + ",reason=active_fatigue_domain_precondition");
            }
        }
        ZhangBoMachineVectorSupport.write(solution, machines);
        ZhangBoCfvfUpdater.validate(solution, zhangBoResourceDomain, "initialParticle[" + particleIndex + "]");
    }

    private void updateCfvfGroup(ZhangBoSubSwarm group,
                                 List<PermutationSolution<Integer>> particles,
                                 List<PermutationSolution<Integer>> personalHistory,
                                 PermutationSolution<Integer> leader) {
        org.uma.jmetal.algorithm.multiobjective.mypso.v35.V35ModuleTimer.increment(
                org.uma.jmetal.algorithm.multiobjective.mypso.v35.V35ModuleTimer.C_SUBGROUP_UPDATE, 1L);
        if (personalHistory.isEmpty()) {
            throw new IllegalStateException("Missing personal history for " + group);
        }
        for (int index = 0; index < particles.size(); index++) {
            PermutationSolution<Integer> current = particles.get(index);
            int personalIndex = -1;
            PermutationSolution<Integer> personalLeader;
            if (usesArchivePersonalLeader()) {
                Object branchValue = current.getAttribute(ZhangBoQpBranchTag.class);
                if (!(branchValue instanceof ZhangBoQpBranchTag)) {
                    throw new IllegalStateException("Archive-guided particle has no branch tag");
                }
                long branchId = ((ZhangBoQpBranchTag) branchValue).getBranchId();
                ZhangBoPersonalLeaderDecision decision = pendingPersonalLeaders.get(branchId);
                if (decision == null || decision.getGroup() != group) {
                    throw new IllegalStateException("Missing personal leader for branch " + branchId);
                }
                personalLeader = decision.pbestSolution(current);
            } else {
                personalIndex = zhangBoP6Random.nextInt(0, personalHistory.size() - 1);
                personalLeader = personalHistory.get(personalIndex);
            }
            Object velocityAttribute = current.getAttribute(ZhangBoResourceVelocity.class);
            ZhangBoResourceVelocity velocity = velocityAttribute instanceof ZhangBoResourceVelocity
                    ? (ZhangBoResourceVelocity) velocityAttribute : ZhangBoResourceVelocity.EMPTY;
            if (cmaxAudit != null) {
                cmaxAudit.observeTeacherUse(fullEvaluationCount, generationNumber(), group,
                        personalLeader, leader);
            }
            double g1OldCmax = Double.NaN;
            if (group == ZhangBoSubSwarm.G1_CMAX) {
                g1OldCmax = current.getObjective(0);
            }
            // FC-5: count teacher usage by lineage (pure observation).
            v35CmaxLifecycleAudit.markCfvfUse(lineageId(leader));
            v35CmaxLifecycleAudit.markCfvfUse(lineageId(personalLeader));
            // FC-5.1: is the teacher the current archive-best Cmax solution?
            boolean g1Group = group == ZhangBoSubSwarm.G1_CMAX;
            v35CmaxLifecycleAudit.observeLearning(lineageId(leader), g1Group);
            v35CmaxLifecycleAudit.observeLearning(lineageId(personalLeader), g1Group);
            ZhangBoCfvfResult result = zhangBoCfvfUpdater.update(current,
                    personalLeader, leader, velocity, zhangBoResourceDomain,
                    globalSearchConfiguration, zhangBoP6Random);
            if (group == ZhangBoSubSwarm.G1_CMAX) {
                ZhangBoCfvfDiagnostics g1Diagnostics = result.getDiagnostics();
                v35CmaxLifecycleAudit.observeG1Update(index, g1OldCmax,
                        g1Diagnostics.getJsHamming() > 0, g1Diagnostics.getFaHamming() > 0,
                        g1Diagnostics.getMaHamming() > 0, g1Diagnostics.getWaHamming() > 0);
            }
            PermutationSolution<Integer> offspring = result.getSolution();
            offspring.setAttribute(ZhangBoSubSwarm.class, group);
            particles.set(index, offspring);
            ZhangBoCfvfDiagnostics diagnostics = result.getDiagnostics();
            // FC-6A-POST / Build-C2: CFVF 学习曝光（纯观察；gbest/pbestInherited>0 为真实学习）。
            if (org.uma.jmetal.algorithm.multiobjective.mypso.v35.V35Fc6BpPddrDiagnosticAudit
                    .isEnabled()) {
                org.uma.jmetal.algorithm.multiobjective.mypso.v35.V35Fc6BpPddrDiagnosticAudit
                        .current().observeCfvfLearning(group, leader, personalLeader,
                        diagnostics.getGbestInherited(), diagnostics.getPbestInherited(),
                        fullEvaluationCount, (int) formalBaselineOuterCycles + 1);
            }
            cfvfOffspringCount++;
            cfvfRepairCount += diagnostics.getRepairs();
            v35CfvfGirAudit.observe(group, diagnostics, lineageId(current),
                    fullEvaluationCount, generationNumber());
            if (diagnostics.getGbestInherited() > 0) {
                pendingCfvfGbestContrib.merge(group, 1L, Long::sum);
            }
            if (usesArchivePersonalLeader() && diagnostics.getPbestInherited() > 0) {
                Object branchValue = current.getAttribute(ZhangBoQpBranchTag.class);
                if (branchValue instanceof ZhangBoQpBranchTag) {
                    pendingCfvfPbestContribBranches
                            .computeIfAbsent(group, key -> new java.util.HashSet<>())
                            .add(((ZhangBoQpBranchTag) branchValue).getBranchId());
                }
            }
            zhangBoP6Events.add("generation=" + currentIteration + ":" + group
                    + ":particle=" + index + (usesArchivePersonalLeader()
                    ? ":pbestFingerprint=" + (globalSearchConfiguration.isBlockFrozenDualQEnabled()
                    ? pendingPersonalLeaders.get(((ZhangBoQpBranchTag) current
                            .getAttribute(ZhangBoQpBranchTag.class)).getBranchId())
                            .getSelectedPbestFingerprint()
                    : pendingPersonalLeaders.get(((ZhangBoQpBranchTag) current
                            .getAttribute(ZhangBoQpBranchTag.class)).getBranchId())
                            .getSelectedPbestFingerprint())
                    : ":pbestIndex=" + personalIndex)
                    + ":lineage=" + lineageId(current)
                    + ":currentFingerprint=" + ZhangBoQgController.fingerprint(current)
                    + ":currentObjectives=" + objectiveText(current)
                    + ":pbestFingerprintActual=" + ZhangBoQgController.fingerprint(personalLeader)
                    + ":pbestObjectives=" + objectiveText(personalLeader)
                    + ":gbestFingerprint=" + ZhangBoQgController.fingerprint(leader)
                    + ":gbestObjectives=" + objectiveText(leader)
                    + ":offspringFingerprint=" + ZhangBoQgController.fingerprint(offspring)
                    + ":offspringObjectives=" + objectiveText(offspring) + "\n"
                    + diagnostics.toCanonicalText());
        }
    }

    private boolean usesArchivePersonalLeader() {
        P8AblationProfile profile = globalSearchConfiguration.getP8AblationProfile();
        return globalSearchConfiguration.isQpEnabled() || (profile != null
                && profile.getPersonalLeaderMode()
                != P8AblationProfile.PersonalLeaderMode.AUTHOR_SINGLE);
    }

    private void prepareP8ArchivePersonalLeaders() {
        pendingPersonalLeaders.clear();
        Map<Long, ZhangBoLineageMemory> memories = zhangBoLineageCoordinator.getMemories();
        org.uma.jmetal.algorithm.multiobjective.mypso.zhangbo.ZhangBoArchiveBounds bounds =
                zhangBoLineageCoordinator.getFrozenBounds();
        long branch = ((long) generationNumber()) * swarmSize;
        branch = selectP8ArchiveGroup(ZhangBoSubSwarm.G1_CMAX, groupU1Solution,
                all3GlobalOptIndividuals.get(0), memories, bounds, branch);
        branch = selectP8ArchiveGroup(ZhangBoSubSwarm.G4_BALANCED, groupC2Solution,
                all3GlobalOptIndividuals.get(1), memories, bounds, branch);
        branch = selectP8ArchiveGroup(ZhangBoSubSwarm.G2_TEC, groupD3Solution,
                all3GlobalOptIndividuals.get(2), memories, bounds, branch);
        selectP8ArchiveGroup(ZhangBoSubSwarm.G3_TWC, groupUNewSolution,
                all3GlobalOptIndividuals.get(3), memories, bounds, branch);
        if (pendingPersonalLeaders.size() != swarmSize) {
            throw new IllegalStateException("Archive personal leader count="
                    + pendingPersonalLeaders.size() + ", expected=" + swarmSize);
        }
    }

    private long selectP8ArchiveGroup(
            ZhangBoSubSwarm group, List<PermutationSolution<Integer>> particles,
            PermutationSolution<Integer> socialLeader, Map<Long, ZhangBoLineageMemory> memories,
            org.uma.jmetal.algorithm.multiobjective.mypso.zhangbo.ZhangBoArchiveBounds bounds,
            long firstBranch) {
        P8AblationProfile.PersonalLeaderMode mode = globalSearchConfiguration
                .getP8AblationProfile().getPersonalLeaderMode();
        for (int index = 0; index < particles.size(); index++) {
            PermutationSolution<Integer> particle = particles.get(index);
            Object lineageValue = particle.getAttribute(ZhangBoLineageTag.class);
            if (!(lineageValue instanceof ZhangBoLineageTag)) {
                throw new IllegalStateException("Archive-guided particle has no lineage tag");
            }
            long lineage = ((ZhangBoLineageTag) lineageValue).getLineageId();
            ZhangBoLineageMemory memory = memories.get(lineage);
            if (memory == null) throw new IllegalStateException("Missing lineage " + lineage);
            List<ZhangBoArchiveEntry> entries = memory.getEntries();
            ZhangBoArchiveEntry selected = zhangBoArchivePersonalLeaderSelector.directional(
                    entries, group, bounds);
            boolean randomPolicy = mode == P8AblationProfile.PersonalLeaderMode.ARCHIVE_RANDOM_FOUR;
            if (randomPolicy && entries.size() > 1) {
                String selectedFingerprint = null;
                Object state = particle.getAttribute(ZhangBoQpLineageState.class);
                if (state instanceof ZhangBoQpLineageState) {
                    selectedFingerprint = ((ZhangBoQpLineageState) state)
                            .getSelectedPbestFingerprint();
                }
                ZhangBoArchiveEntry current = ZhangBoArchiveEntry.fromSolution(particle,
                        ZhangBoEvaluatedPddrSelector.Source.PARENT,
                        generationNumber(), fullEvaluationCount);
                ZhangBoArchiveEntry social = ZhangBoArchiveEntry.fromSolution(socialLeader,
                        ZhangBoEvaluatedPddrSelector.Source.PARENT,
                        generationNumber(), fullEvaluationCount);
                ZhangBoQpCandidateSelector.Candidates candidates =
                        zhangBoArchivePersonalLeaderSelector.build(entries, selectedFingerprint,
                                group, current, social, bounds);
                List<ZhangBoQpAction> valid = new ArrayList<>();
                for (ZhangBoQpAction action : ZhangBoQpAction.values()) {
                    if (candidates.isValid(action)) valid.add(action);
                }
                ZhangBoQpAction action = valid.get(zhangBoP6Random.nextInt(0, valid.size() - 1));
                selected = candidates.get(action);
                zhangBoP6Events.add("generation=" + currentIteration + ":" + group
                        + ":archiveRandomPolicy=" + action + ",lineage=" + lineage);
            }
            long branchId = firstBranch + index;
            particle.setAttribute(ZhangBoQpLineageState.class,
                    new ZhangBoQpLineageState(selected.getFingerprint()));
            particle.setAttribute(ZhangBoQpBranchTag.class,
                    new ZhangBoQpBranchTag(branchId, lineage));
            pendingPersonalLeaders.put(branchId,
                    ZhangBoPersonalLeaderDecision.archive(branchId, group, selected, randomPolicy));
        }
        return firstBranch + particles.size();
    }

    private void prepareQpSelections() {
        pendingQpSelections.clear();
        pendingPersonalLeaders.clear();
        Map<Long, ZhangBoLineageMemory> memories = zhangBoLineageCoordinator.getMemories();
        org.uma.jmetal.algorithm.multiobjective.mypso.zhangbo.ZhangBoArchiveBounds bounds =
                zhangBoLineageCoordinator.getFrozenBounds();
        long base = ((long) generationNumber()) * swarmSize;
        selectQpGroup(ZhangBoSubSwarm.G1_CMAX, groupU1Solution,
                all3GlobalOptIndividuals.get(0), memories, bounds, base);
        base += groupU1Solution.size();
        selectQpGroup(ZhangBoSubSwarm.G4_BALANCED, groupC2Solution,
                all3GlobalOptIndividuals.get(1), memories, bounds, base);
        base += groupC2Solution.size();
        selectQpGroup(ZhangBoSubSwarm.G2_TEC, groupD3Solution,
                all3GlobalOptIndividuals.get(2), memories, bounds, base);
        base += groupD3Solution.size();
        selectQpGroup(ZhangBoSubSwarm.G3_TWC, groupUNewSolution,
                all3GlobalOptIndividuals.get(3), memories, bounds, base);
        if (pendingQpSelections.size() != swarmSize) {
            throw new IllegalStateException("Qp selection count=" + pendingQpSelections.size()
                    + ", expected=" + swarmSize);
        }
        if (pendingPersonalLeaders.size() != swarmSize) {
            throw new IllegalStateException("Personal leader count=" + pendingPersonalLeaders.size()
                    + ", expected=" + swarmSize);
        }
    }

    private void selectQpGroup(
            ZhangBoSubSwarm group,
            List<PermutationSolution<Integer>> particles,
            PermutationSolution<Integer> leader,
            Map<Long, ZhangBoLineageMemory> memories,
            org.uma.jmetal.algorithm.multiobjective.mypso.zhangbo.ZhangBoArchiveBounds bounds,
            long firstBranchId) {
        ZhangBoQpController.SelectionMode selectionMode = pendingDualQDecision != null
                && pendingDualQDecision.isGBlock()
                ? ZhangBoQpController.SelectionMode.GREEDY_FROZEN
                : ZhangBoQpController.SelectionMode.EPSILON_GREEDY;
        List<ZhangBoQpController.Selection> selections = zhangBoQpController.selectGroup(
                group, particles, leader, memories, bounds, fullEvaluationCount, maxIterations,
                firstBranchId, selectionMode);
        for (ZhangBoQpController.Selection selection : selections) {
            // V35-SOURCE-ATTRIBUTION-PATCH: Qp round context marker (pure observation).
            org.uma.jmetal.algorithm.multiobjective.mypso.v35.V35SourceAttributionObserver.onQpSelections(null);
            if (pendingQpSelections.put(selection.getBranchId(), selection) != null) {
                throw new IllegalStateException("Duplicate Qp branch " + selection.getBranchId());
            }
            pendingPersonalLeaders.put(selection.getBranchId(),
                    ZhangBoPersonalLeaderDecision.fromQp(selection));
        }
    }

    private void prepareWarmupPersonalLeaders() {
        pendingQpSelections.clear();
        pendingPersonalLeaders.clear();
        Map<Long, ZhangBoLineageMemory> memories = zhangBoLineageCoordinator.getMemories();
        org.uma.jmetal.algorithm.multiobjective.mypso.zhangbo.ZhangBoArchiveBounds bounds =
                zhangBoLineageCoordinator.getFrozenBounds();
        long base = ((long) generationNumber()) * swarmSize;
        selectWarmupGroup(ZhangBoSubSwarm.G1_CMAX, groupU1Solution, memories, bounds, base);
        base += groupU1Solution.size();
        selectWarmupGroup(ZhangBoSubSwarm.G4_BALANCED, groupC2Solution, memories, bounds, base);
        base += groupC2Solution.size();
        selectWarmupGroup(ZhangBoSubSwarm.G2_TEC, groupD3Solution, memories, bounds, base);
        base += groupD3Solution.size();
        selectWarmupGroup(ZhangBoSubSwarm.G3_TWC, groupUNewSolution, memories, bounds, base);
        if (pendingPersonalLeaders.size() != swarmSize || !pendingQpSelections.isEmpty()) {
            throw new IllegalStateException("Warmup personal leader count="
                    + pendingPersonalLeaders.size() + ", Qp transitions="
                    + pendingQpSelections.size());
        }
    }

    private void selectWarmupGroup(
            ZhangBoSubSwarm group,
            List<PermutationSolution<Integer>> particles,
            Map<Long, ZhangBoLineageMemory> memories,
            org.uma.jmetal.algorithm.multiobjective.mypso.zhangbo.ZhangBoArchiveBounds bounds,
            long firstBranchId) {
        List<ZhangBoPersonalLeaderDecision> decisions =
                zhangBoQpController.selectDirectionalWarmupGroup(
                        group, particles, memories, bounds, firstBranchId);
        for (ZhangBoPersonalLeaderDecision decision : decisions) {
            if (pendingPersonalLeaders.put(decision.getBranchId(), decision) != null) {
                throw new IllegalStateException("Duplicate warmup branch " + decision.getBranchId());
            }
        }
    }

    private void isolateQpGroupBranches() {
        groupU1Solution = ZhangBoSolutionSupport.deepCopySolutions(groupU1Solution);
        groupC2Solution = ZhangBoSolutionSupport.deepCopySolutions(groupC2Solution);
        groupD3Solution = ZhangBoSolutionSupport.deepCopySolutions(groupD3Solution);
        groupUNewSolution = ZhangBoSolutionSupport.deepCopySolutions(groupUNewSolution);
    }

    private void settleQp(List<PermutationSolution<Integer>> evaluated) {
        if (pendingDualQDecision != null && pendingDualQDecision.isWarmup()) {
            if (!pendingQpSelections.isEmpty()) {
                throw new IllegalStateException("Warmup created Qp transitions");
            }
            pendingPersonalLeaders.clear();
            return;
        }
        if (pendingQpSelections.isEmpty()) {
            throw new IllegalStateException("Qp enabled without pending selections");
        }
        long firstOrdinal = fullEvaluationCount - evaluated.size() + 1L;
        double softRho = dualQSoftFreezeRho();
        if (pendingDualQDecision != null && pendingDualQDecision.isGBlock()
                && softRho > 0.0) {
            java.util.Set<Long> contributingBranches = new java.util.HashSet<>();
            for (java.util.Set<Long> branches : pendingCfvfPbestContribBranches.values()) {
                contributingBranches.addAll(branches);
            }
            zhangBoQpController.settle(evaluated, pendingQpSelections,
                    zhangBoLineageCoordinator.getFrozenBounds(), firstOrdinal,
                    ZhangBoQpController.SettlementMode.SOFT_LEARN, softRho,
                    contributingBranches);
        } else {
            ZhangBoQpController.SettlementMode settlementMode =
                    pendingDualQDecision != null && pendingDualQDecision.isGBlock()
                    ? ZhangBoQpController.SettlementMode.OBSERVE_ONLY
                    : ZhangBoQpController.SettlementMode.LEARN;
            zhangBoQpController.settle(evaluated, pendingQpSelections,
                    zhangBoLineageCoordinator.getFrozenBounds(), firstOrdinal, settlementMode);
        }
        pendingQpSelections.clear();
        pendingPersonalLeaders.clear();
        pendingCfvfPbestContribBranches.clear();
    }

    /** V35-FC-5: the read-only CFVF GIR audit summary (never influences behaviour). */
    public String v35CfvfGirAuditSummary() {
        return v35CfvfGirAudit.summaryText();
    }

    /** FC-TIME-1: read-only per-cycle module timing lines (cycleId/FE/archiveSize/module deltas). */
    public java.util.List<String> v35ModulePerCycleLines() {
        return v35ModulePerCycleLines;
    }

    /** FC-5: read-only Cmax lifecycle audit summary (four-layer funnel + G1 GIR + lineage). */
    public String v35CmaxLifecycleAuditSummary() {
        return v35CmaxLifecycleAudit.summaryText();
    }

    public org.uma.jmetal.algorithm.multiobjective.mypso.v35.V35CmaxLifecycleAudit
            getV35CmaxLifecycleAudit() {
        return v35CmaxLifecycleAudit;
    }

    public org.uma.jmetal.algorithm.multiobjective.mypso.v35.V35CfvfGirAudit
            getV35CfvfGirAudit() {
        return v35CfvfGirAudit;
    }

    /** V35-FC-4: the contribution-gated soft-freeze coefficient, 0.0 by default. */
    private double dualQSoftFreezeRho() {
        ZhangBoDualQCoordinationConfiguration coordination =
                globalSearchConfiguration.getDualQCoordinationConfiguration();
        return coordination == null ? 0.0 : coordination.getSoftFreezeRho();
    }

    private void annotateSubSwarmSlots(List<PermutationSolution<Integer>> values) {
        int position = 0;
        position = annotateRange(values, position, upSize, ZhangBoSubSwarm.G1_CMAX);
        position = annotateRange(values, position, centralSize, ZhangBoSubSwarm.G4_BALANCED);
        position = annotateRange(values, position, downSize, ZhangBoSubSwarm.G2_TEC);
        annotateRange(values, position, upNewSize, ZhangBoSubSwarm.G3_TWC);
    }

    private int annotateRange(List<PermutationSolution<Integer>> values, int start, int count,
                              ZhangBoSubSwarm group) {
        int end = Math.min(values.size(), start + count);
        for (int index = start; index < end; index++) {
            values.get(index).setAttribute(ZhangBoSubSwarm.class, group);
        }
        return start + count;
    }

    private static List<PermutationSolution<Integer>> copySolutions(
            List<PermutationSolution<Integer>> source) {
        List<PermutationSolution<Integer>> result = new ArrayList<>(source.size());
        for (PermutationSolution<Integer> solution : source) {
            result.add((PermutationSolution<Integer>) solution.copy());
        }
        return result;
    }

    private void G1PSO(Random random, int[] nw, PermutationSolution<Integer> action,int group) {
        double m_worker;
        double c_worker;
        double r1;
        double m;
        double c;
        double c_machine;
        double r2;
        double m_machine;

        if (group==1){
            for (int i = 0; i < upSize; i++) {

                ArrayList<SO> listV = new ArrayList<>();
                ArrayList<SO> listVa = new ArrayList<>();  //用于工厂向量
                //用于工厂向量的DE交换序
                ArrayList<ST> listV2 = new ArrayList<>(); //用于工厂向量
                //用于工厂向量的变异

                int len = 0;
                int len1 = 0;
                Random r = authorRandom();
                int i1 = r.nextInt(upSize);
                //选择一个粒子
                PermutationSolution<Integer> particle = (PermutationSolution<Integer>) groupU1Solution.get(i).copy();

                //Parameters for velocity equation
                r1 = random.nextDouble() * Rand_k;
                r2 = random.nextDouble() * Rand_k;  //生成一个0~Rand_k的数
                //

                //自身初速度
                SO s1 = new SO(formalRandomInt(0, particle.getNumberOfVariables() - 1),
                        formalRandomInt(0, particle.getNumberOfVariables() - 1));
                listV.add(s1);
                addNew4JobSequenceVectorByExchangeSequence(particle, listV);

                //历史最优
                listV.clear();
                ArrayList<SO> vtemp1 = getDifferenceOfJobSequenceVectorByExchangeSequence(upGr1HisOptIndividual.get(i1), particle);
                len = (int) (vtemp1.size() * r1);

                for (int j = 0; j < len; j++) {
                    listV.add(vtemp1.get(j));
                }
                addNew4JobSequenceVectorByExchangeSequence(particle, listV);

                c = random.nextDouble();
                m = random.nextDouble();

                //Cross_c
                if (c < Cross_c) {
                    getCrossOfFactoryVectorBySingle(upGr1HisOptIndividual.get(i1), particle);    //单点交叉
                }

                c_worker = random.nextDouble();
                if (c_worker < CrossoverRates4worker) {
                    crossover4workersequence(upGr1HisOptIndividual.get(i1), particle, nw);//工人向量交叉
                }

                c_machine = random.nextDouble();
                if (c_machine < CrossoverRates4machine) {
                    crossover4machinesequence(upGr1HisOptIndividual.get(i1), particle, nw);//工人向量交叉
                }


                if (m < Mutation_m) {
                    //确定针对工厂向量
                    ST q = new ST(formalRandomInt(0, particle.getNumberOfVariables() - 1));
                    listV2.add(q);
                    addNew4FactoryVectorByRandom(particle, listV2);
                }

                m_worker = random.nextDouble();
                if (m_worker < mutationRate4worker) {
                    mutation4worker(particle);
                }

                m_machine = random.nextDouble();
                if (m_machine < mutationRate4machine) {
                    mutation4machine(particle);
                }

                //全局最优
                listV.clear();
                listVa.clear();
                listV2.clear();

                ArrayList<SO> vtemp2 = getDifferenceOfJobSequenceVectorByExchangeSequence(action, particle);
                len = (int) (vtemp2.size() * r2);

                for (int j = 0; j < len; j++) {
                    listV.add(vtemp2.get(j));
                }
                addNew4JobSequenceVectorByExchangeSequence(particle, listV);

                c = random.nextDouble();
                m = random.nextDouble();
                if (c < Cross_c) {
                    getCrossOfFactoryVectorBySingle(action, particle);    //单点交叉
                }
                c_worker = random.nextDouble();
                if (c_worker < CrossoverRates4worker) {
                    crossover4workersequence(action, particle, nw);//工人向量交叉
                }
                c_machine = random.nextDouble();
                if (c_machine < CrossoverRates4machine) {
                    crossover4machinesequence(action, particle, nw);//工人向量交叉
                }
                if (m < Mutation_m) {
                    ST q1 = new ST(formalRandomInt(0, particle.getNumberOfVariables() - 1));
                    listV2.add(q1);
                    addNew4FactoryVectorByRandom(particle, listV2);
                }
                m_worker = random.nextDouble();
                if (m_worker < mutationRate4worker) {
                    mutation4worker(particle);
                }
                m_machine = random.nextDouble();
                if (m_machine < mutationRate4machine) {
                    mutation4machine(particle);
                }
                groupU1Solution.set(i, particle);
            }
        }else if (group == 2){
            for (int i = 0; i < centralSize; i++) {
                Random r = authorRandom();
                int i1 = r.nextInt(centralSize);
                ArrayList<SO> listV = new ArrayList<>();
                ArrayList<SO> listVa = new ArrayList<>();
                ArrayList<ST> listV2 = new ArrayList<>();
                int len = 0;
                int len1 = 0;
                PermutationSolution<Integer> particle = (PermutationSolution<Integer>) groupC2Solution.get(i).copy();

                //Parameters for velocity equation
                r1 = random.nextDouble() * Rand_k;
                r2 = random.nextDouble() * Rand_k;  //生成一个0~Rand_k的数
                //

                //自身初速度
                SO s1 = new SO(formalRandomInt(0, particle.getNumberOfVariables() - 1),
                        formalRandomInt(0, particle.getNumberOfVariables() - 1));

                listV.add(s1);
                addNew4JobSequenceVectorByExchangeSequence(particle, listV);

                //历史最优
                listV.clear();
                listVa.clear();
                listV2.clear();

                ArrayList<SO> vtemp1 = getDifferenceOfJobSequenceVectorByExchangeSequence(centralGr2HisOptIndividual.get(i1), particle);
                len = (int) (vtemp1.size() * r1);

                for (int j = 0; j < len; j++) {
                    listV.add(vtemp1.get(j));
                }
                addNew4JobSequenceVectorByExchangeSequence(particle, listV);

                c = random.nextDouble();
                m = random.nextDouble();
                if (c < Cross_c) {
                    getCrossOfFactoryVectorBySingle(centralGr2HisOptIndividual.get(i1), particle);    //单点交叉
                }
                c_worker = random.nextDouble();
                if (c_worker < CrossoverRates4worker) {
                    crossover4workersequence(centralGr2HisOptIndividual.get(i1), particle, nw);//工人向量交叉
                }
                c_machine = random.nextDouble();
                if (c_machine < CrossoverRates4machine) {
                    crossover4machinesequence(centralGr2HisOptIndividual.get(i1), particle, nw);//工人向量交叉
                }
                if (m < Mutation_m) {
                    ST q = new ST(formalRandomInt(0, particle.getNumberOfVariables() - 1));
                    listV2.add(q);
                    addNew4FactoryVectorByRandom(particle, listV2);
                }

                m_worker = random.nextDouble();
                if (m_worker < mutationRate4worker) {
                    mutation4worker(particle);
                }

                m_machine = random.nextDouble();
                if (m_machine < mutationRate4machine) {
                    mutation4machine(particle);
                }

                //全局最优
                listV.clear();
                listVa.clear();
                listV2.clear();

                ArrayList<SO> vtemp2 = getDifferenceOfJobSequenceVectorByExchangeSequence(all3GlobalOptIndividuals.get(1), particle);
                len = (int) (vtemp2.size() * r2);

                for (int j = 0; j < len; j++) {
                    listV.add(vtemp2.get(j));
                }
                addNew4JobSequenceVectorByExchangeSequence(particle, listV);

                if (c < Cross_c) {
                    getCrossOfFactoryVectorBySingle(all3GlobalOptIndividuals.get(1), particle);    //单点交叉
                }
                c_worker = random.nextDouble();
                if (c_worker < CrossoverRates4worker) {
                    crossover4workersequence(all3GlobalOptIndividuals.get(1), particle, nw);//工人向量交叉
                }
                c_machine = random.nextDouble();
                if (c_machine < CrossoverRates4machine) {
                    crossover4machinesequence(all3GlobalOptIndividuals.get(1), particle, nw);//工人向量交叉
                }
                if (m < Mutation_m) {
                    ST q1 = new ST(formalRandomInt(0, particle.getNumberOfVariables() - 1));
                    listV2.add(q1);
                    addNew4FactoryVectorByRandom(particle, listV2);
                }
                m_worker = random.nextDouble();
                if (m_worker < mutationRate4worker) {
//                mutation4worker(particle);
                }
                m_machine = random.nextDouble();
                if (m_machine < mutationRate4machine) {
                    mutation4machine(particle);
                }
                groupC2Solution.set(i, particle);
            }
        } else if (group == 3) {
            for (int i = 0; i < downSize; i++) {
                Random r = authorRandom();
                int i1 = r.nextInt(downSize);
                ArrayList<SO> listV = new ArrayList<>();
                ArrayList<SO> listVa = new ArrayList<>();
                ArrayList<ST> listV2 = new ArrayList<>();
                int len = 0;
                int len1 = 0;
                PermutationSolution<Integer> particle = (PermutationSolution<Integer>) groupD3Solution.get(i).copy();

                //Parameters for velocity equation
                r1 = random.nextDouble() * Rand_k;
                r2 = random.nextDouble() * Rand_k;
                //自身初速度
                SO s1 = new SO(formalRandomInt(0, particle.getNumberOfVariables() - 1),
                        formalRandomInt(0, particle.getNumberOfVariables() - 1));

                listV.add(s1);
                addNew4JobSequenceVectorByExchangeSequence(particle, listV);

                //历史最优
                listV.clear();
                listVa.clear();
                listV2.clear();
                ArrayList<SO> vtemp1 = getDifferenceOfJobSequenceVectorByExchangeSequence(downGr3HisOptIndividual.get(i1), particle);
                len = (int) (vtemp1.size() * r1);

                for (int j = 0; j < len; j++) {
                    listV.add(vtemp1.get(j));
                }
                addNew4JobSequenceVectorByExchangeSequence(particle, listV);


                c = random.nextDouble();
                m = random.nextDouble();
                if (c < Cross_c) {
                    getCrossOfFactoryVectorBySingle(downGr3HisOptIndividual.get(i1), particle);    //单点交叉
                }
                c_worker = random.nextDouble();
                if (c_worker < CrossoverRates4worker) {
                    crossover4workersequence(downGr3HisOptIndividual.get(i1), particle, nw);//工人向量交叉
                }
                c_machine = random.nextDouble();
                if (c_machine < CrossoverRates4machine) {
                    crossover4machinesequence(downGr3HisOptIndividual.get(i1), particle, nw);//工人向量交叉
                }
                if (m < Mutation_m) {
                    ST q = new ST(formalRandomInt(0, particle.getNumberOfVariables() - 1));
                    listV2.add(q);
                    addNew4FactoryVectorByRandom(particle, listV2);
                }
                m_worker = random.nextDouble();
                if (m_worker < mutationRate4worker) {
                    mutation4worker(particle);
                }
                m_machine = random.nextDouble();
                if (m_machine < mutationRate4machine) {
                    mutation4machine(particle);
                }

                //全局最优
                listV.clear();
                listVa.clear();
                listV2.clear();

                ArrayList<SO> vtemp2 = getDifferenceOfJobSequenceVectorByExchangeSequence(all3GlobalOptIndividuals.get(2), particle);
                len = (int) (vtemp2.size() * r2);

                for (int j = 0; j < len; j++) {
                    listV.add(vtemp2.get(j));
                }
                addNew4JobSequenceVectorByExchangeSequence(particle, listV);

                c = random.nextDouble();
                m = random.nextDouble();
                if (c < Cross_c) {
                    getCrossOfFactoryVectorBySingle(all3GlobalOptIndividuals.get(2), particle);    //单点交叉
                }
                c_worker = random.nextDouble();
                if (c_worker < CrossoverRates4worker) {
                    crossover4workersequence(all3GlobalOptIndividuals.get(2), particle, nw);//工人向量交叉
                }
                c_machine = random.nextDouble();
                if (c_machine < CrossoverRates4machine) {
                    crossover4machinesequence(all3GlobalOptIndividuals.get(2), particle, nw);//工人向量交叉
                }
                if (m < Mutation_m) {
                    ST q1 = new ST(formalRandomInt(0, particle.getNumberOfVariables() - 1));
                    listV2.add(q1);
                    addNew4FactoryVectorByRandom(particle, listV2);
                }
                m_worker = random.nextDouble();
                if (m_worker < mutationRate4worker) {
                    mutation4worker(particle);
                }
                m_machine = random.nextDouble();
                if (m_machine < mutationRate4machine) {
                    mutation4machine(particle);
                }
//

                groupD3Solution.set(i, particle);
            }
        } else if (group == 4) {
            for (int i = 0; i < upNewSize; i++) {
                Random r = authorRandom();
                int i1 = r.nextInt(upNewSize);
                ArrayList<SO> listV = new ArrayList<>();
                ArrayList<SO> listVa = new ArrayList<>();
                ArrayList<ST> listV2 = new ArrayList<>();
                int len = 0;
                int len1 = 0;
                PermutationSolution<Integer> particle = (PermutationSolution<Integer>) groupUNewSolution.get(i).copy();

                //Parameters for velocity equation
                r1 = random.nextDouble() * Rand_k;
                r2 = random.nextDouble() * Rand_k;
                //自身初速度
                SO s1 = new SO(formalRandomInt(0, particle.getNumberOfVariables() - 1),
                        formalRandomInt(0, particle.getNumberOfVariables() - 1));

                listV.add(s1);
                addNew4JobSequenceVectorByExchangeSequence(particle, listV);

                //历史最优
                listV.clear();
                listVa.clear();
                listV2.clear();
                ArrayList<SO> vtemp1 = getDifferenceOfJobSequenceVectorByExchangeSequence(upNewGr1HisOptIndividual.get(i1), particle);
                len = (int) (vtemp1.size() * r1);

                for (int j = 0; j < len; j++) {
                    listV.add(vtemp1.get(j));
                }
                addNew4JobSequenceVectorByExchangeSequence(particle, listV);

                c = random.nextDouble();
                m = random.nextDouble();
                if (c < Cross_c) {
                    getCrossOfFactoryVectorBySingle(upNewGr1HisOptIndividual.get(i1), particle);    //单点交叉
                }
                c_worker = random.nextDouble();
                if (c_worker < CrossoverRates4worker) {
                    crossover4workersequence(upNewGr1HisOptIndividual.get(i1), particle, nw);//工人向量交叉
                }
                c_machine = random.nextDouble();
                if (c_machine < CrossoverRates4machine) {
                    crossover4machinesequence(upNewGr1HisOptIndividual.get(i1), particle, nw);//工人向量交叉
                }
                if (m < Mutation_m) {
                    ST q = new ST(formalRandomInt(0, particle.getNumberOfVariables() - 1));
                    listV2.add(q);
                    addNew4FactoryVectorByRandom(particle, listV2);
                }
                m_worker = random.nextDouble();
                if (m_worker < mutationRate4worker) {
                    mutation4worker(particle);
                }
                m_machine = random.nextDouble();
                if (m_machine < mutationRate4machine) {
                    mutation4machine(particle);
                }

                //全局最优
                listV.clear();
                listVa.clear();
                listV2.clear();

                ArrayList<SO> vtemp2 = getDifferenceOfJobSequenceVectorByExchangeSequence(all3GlobalOptIndividuals.get(3), particle);
                len = (int) (vtemp2.size() * r2);

                for (int j = 0; j < len; j++) {
                    listV.add(vtemp2.get(j));
                }
                addNew4JobSequenceVectorByExchangeSequence(particle, listV);

                c = random.nextDouble();
                m = random.nextDouble();
                if (c < Cross_c) {
                    getCrossOfFactoryVectorBySingle(all3GlobalOptIndividuals.get(3), particle);    //单点交叉
                }
                if (c_worker < CrossoverRates4worker) {
                    crossover4workersequence(all3GlobalOptIndividuals.get(3), particle, nw);//工人向量交叉
                }
                c_machine = random.nextDouble();
                if (c_machine < CrossoverRates4machine) {
                    crossover4machinesequence(all3GlobalOptIndividuals.get(3), particle, nw);//工人向量交叉
                }
                if (m < Mutation_m) {
                    ST q1 = new ST(formalRandomInt(0, particle.getNumberOfVariables() - 1));
                    listV2.add(q1);
                    addNew4FactoryVectorByRandom(particle, listV2);
                }
                m_worker = random.nextDouble();
                if (m_worker < mutationRate4worker) {
                    mutation4worker(particle);
                }
                m_machine = random.nextDouble();
                if (m_machine < mutationRate4machine) {
                    mutation4machine(particle);
                }
                groupUNewSolution.set(i, particle);
            }
        }

    }


    //整合
    private void merge(List<PermutationSolution<Integer>> swarm) {
        swarm.clear();
        tempSwarm.clear();

        for (int i = 0; i < upSize; i++) {
            swarm.add((PermutationSolution<Integer>) groupU1Solution.get(i).copy());

            ArrayList<PermutationSolution<Integer>> A = new ArrayList<>(upGroup1Population.get(i).size());
            for (int j = 0; j < upGroup1Population.get(i).size(); j++) {
                A.add((PermutationSolution<Integer>) upGroup1Population.get(i).get(j).copy());
            }
            tempSwarm.add(A);
        }
        for (int i = 0; i < centralSize; i++) {
            swarm.add((PermutationSolution<Integer>) groupC2Solution.get(i).copy());

            ArrayList<PermutationSolution<Integer>> A = new ArrayList<>(centralGroup2Population.get(i).size());
            for (int j = 0; j < centralGroup2Population.get(i).size(); j++) {
                A.add((PermutationSolution<Integer>) centralGroup2Population.get(i).get(j).copy());
            }
            tempSwarm.add(A);
        }
        for (int i = 0; i < downSize; i++) {
            swarm.add((PermutationSolution<Integer>) groupD3Solution.get(i).copy());

            ArrayList<PermutationSolution<Integer>> A = new ArrayList<>(downGroup3Population.get(i).size());
            for (int j = 0; j < downGroup3Population.get(i).size(); j++) {
                A.add((PermutationSolution<Integer>) downGroup3Population.get(i).get(j).copy());
            }
            tempSwarm.add(A);
        }

        for (int i = 0; i < upNewSize; i++) {
            swarm.add((PermutationSolution<Integer>) groupUNewSolution.get(i).copy());

            ArrayList<PermutationSolution<Integer>> A = new ArrayList<>(upNewGroup1Population.get(i).size());
            for (int j = 0; j < upNewGroup1Population.get(i).size(); j++) {
                A.add((PermutationSolution<Integer>) upNewGroup1Population.get(i).get(j).copy());
            }
            tempSwarm.add(A);
        }

//        System.out.println(tempSwarm.size());

//        System.out.println(upGroup1Population.size());
//        System.out.println(centralGroup2Population.size());
//        System.out.println(downGroup3Population.size());
//        System.out.println(upNewGroup1Population.size());
//        try {
//            Thread.sleep(99999);
//        } catch (InterruptedException e) {
//            throw new RuntimeException(e);
//        }
    }

    public static ArrayList<List<Integer>> action(int numberOfFactories) {

        List<Integer> nums = new ArrayList<>();
        for (int i = 0; i < numberOfFactories; i++) nums.add(i);
        List<List<Integer>> result = subsets(nums);
        result.remove(0);
        //System.out.println(result);
        //System.out.println("length:" + result.size());
        return (ArrayList<List<Integer>>) result;
    }

    public static List<List<Integer>> subsets(List<Integer> nums) {
        //用于生成给定列表 nums 的所有子集。这个方法通过递归地处理列表中的每个元素，并生成包含和不包含当前元素的所有子集。
        if (nums.size() == 0) return Arrays.asList(new ArrayList[]{new ArrayList<Integer>()});
        // System.out.println("nums:" + nums);

        Integer currentNums = nums.get(nums.size() - 1);
        //System.out.println("currentNums:" + currentNums);
        nums.remove(nums.size() - 1);

        List<List<Integer>> res = subsets(nums);

        List<List<Integer>> res2 = new ArrayList<>();
        //System.out.println("===============================");
        for (List<Integer> re : res) {
            List<Integer> r = new ArrayList<>();
            if (re.size() != 0) {
                for (Integer integer : re) {
                    r.add(integer);
                }
            }
            //System.out.println(r);
            res2.add(r);
        }
        // System.out.println("===============================");
        int size = res.size();
        for (int i = 0; i < size; i++) {
            List<Integer> integers = res.get(i);
            integers.add(currentNums);
            res2.add(integers);
        }
        //System.out.println("result:" + res2);
        return res2;
    }

    //    @Override
    //todo
    protected void perturbation_new(List<PermutationSolution<Integer>> swarm) {

//        try {
//            Thread.sleep(9999);
//        } catch (InterruptedException e) {
//            throw new RuntimeException(e);
//        }
//        super.setSwarm(evaluateSwarm(swarm));
//        updateVelocity(swarm);
//        List<PermutationSolution<Integer>> swarmtemp1 = new ArrayList<PermutationSolution<Integer>>(swarm.size());
//        int[] DEswarmtempPdflag1 = new int[swarmSize];
//        int group1=1,group2=2,group3=3,group4 = 4;
//
//        for (int i = 0; i < upSize; i++) {
//            PermutationSolution<Integer>  getswarm1 = null;
//            getswarm1=selectFac1(groupU1Solution.get(i),group1);
//            swarmtemp1.add(getswarm1);
//            groupU1Solution.set(i,getswarm1);
//        }
//
//
//        for (int i = 0; i < upNewSize; i++) {
//            PermutationSolution<Integer>  getswarm1 = null;
//
//            getswarm1=selectFac1(groupUNewSolution.get(i),group4);
//            System.out.println("111");
//
//            swarmtemp1.add(getswarm1);
//
//            groupUNewSolution.set(i,getswarm1);
//        }
//
//        for (int i = 0; i < centralSize; i++) {
//            PermutationSolution<Integer>  getswarm1 = null;
//            getswarm1=selectFac1(groupC2Solution.get(i),group2);
//            swarmtemp1.add(getswarm1);
//            groupC2Solution.set(i,getswarm1);
//        }
//
//        for (int i = 0; i < downSize; i++) {
//            PermutationSolution<Integer>  getswarm1 = null;
//            getswarm1=selectFac1(groupD3Solution.get(i),group3);
//            swarmtemp1.add(getswarm1);
//            groupD3Solution.set(i,getswarm1);
//        }
//
//
//        mergeNew(swarmtemp1);
//
//
//        System.out.println("--");
//        System.out.println(swarmtemp1);
//
//
//        swarmtemp1 = evaluateSwarm(swarmtemp1);
//
//        for (int i = 0; i < swarmSize; i++) {
//            DEswarmtempPdflag1[i] = i;
//        }
//        super.setSwarm(PDDRFFselect(swarm,swarmtemp1,DEswarmtempPdflag1));

/*        List<PermutationSolution<Integer>> swarmFac = new ArrayList<PermutationSolution<Integer>>(swarmSize/2);
        for(int i=0;i<swarmFac.size();i++)
        {
            swarmFac.add(selectFac1(swarm).get(i));
        }
        //swarmFac = selectFac1(swarm);
        System.out.println(swarmFac);
        //swarmFac = evaluateSwarm(swarmFac);

        for (int i = 0; i < swarmSize/2; i++) {
            swarm.set(i+swarmSize/2,swarmFac.get(i));
        }*/

/////////////////////////////////////////////////////////////////////////////////////
        int QN = (int) Qnums;
//        QN=50;
        super.setSwarm(evaluateSwarm(swarm));
        updateVelocity(swarm);    //分群

        //////////////////////////////////////////////////////////////////////////////
        //super.setSwarm(evaluateSwarm(swarm));
        List<PermutationSolution<Integer>> swarmtemp = new ArrayList<PermutationSolution<Integer>>(swarm.size());

        int[] DEswarmtempPdflag = new int[swarmSize];


        int group = 1;
        action = action(numberOfFactories);
        int anum = action.size();
//        try {
//            Thread.sleep(999);
//        } catch (InterruptedException e) {
//            throw new RuntimeException(e);
//        }
        ReplayBuffer replayBuffer = new ReplayBuffer(32); // 设置经验回放池的最大容量
        int batchSize = 8; // 设置每次训练的批量大小
        for (int i = 0; i < upSize; i++) {
            System.out.println(i);

            double[][] Q = new double[2][anum];
            double[][] R = new double[2][anum];
            for (int j = 0; j < anum; j++) {
                Q[0][j] = 0;
                Q[1][j] = 0;
            }

            for (int j = 0; j < anum; j++) {
                R[0][j] = 1;
                R[1][j] = 1;
            }
            //double r = 0.75, s = 0.85;
            Random random = authorRandom();
            int next = 0;
            int actionIndex;
            PermutationSolution<Integer> getswarm1 = null;
            double old0, old1, new0, new1;
            double[] max = new double[QN];
            List<PermutationSolution<Integer>> temp = new ArrayList<PermutationSolution<Integer>>(QN);
            for (int k = 0; k < QN; k++) {

                double p = random.nextDouble();

                double[][] currentState = Q.clone(); // 当前状态
                double currentReward = 0; // 当前奖励

                if (k == 0) {
                    actionIndex = random.nextInt(action.size());
                    getswarm1 = learn(actionIndex, R, Q, groupU1Solution.get(i), next, group);
                    int Qiannext = next;
                    if ((getswarm1.getObjective(0) < groupU1Solution.get(i).getObjective(0))) next = 0;
                    else next = 1;
                    old0 = groupU1Solution.get(i).getObjective(0);
                    new0 = getswarm1.getObjective(0);

                    currentReward = new0 - old0; // 计算奖励

//                    Q[Qiannext][actionIndex] = calculateNewQ(R, Q,actionIndex,next,Qiannext);
                    Q[Qiannext][actionIndex] = calculateNewQ1(R, Q, actionIndex, next, Qiannext, old0, new0);
                } else {
                    if (p < 1 - tl) {
                        actionIndex = random.nextInt(action.size());
                        getswarm1 = learn(actionIndex, R, Q, groupU1Solution.get(i), next, group);
                        int Qiannext = next;
                        if ((getswarm1.getObjective(0) < groupU1Solution.get(i).getObjective(0)))
                            next = 0;
                        else next = 1;
                        old0 = groupU1Solution.get(i).getObjective(0);
                        new0 = getswarm1.getObjective(0);

                        currentReward = new0 - old0; // 计算奖励

                        Q[Qiannext][actionIndex] = calculateNewQ1(R, Q, actionIndex, next, Qiannext, old0, new0);

//                        Q[Qiannext][actionIndex] = calculateNewQ(R, Q,actionIndex,next,Qiannext);
                    } else {
                        //actionIndex = getMaxQ(Q);

                        actionIndex = max(Q[next]);
                        getswarm1 = learn(actionIndex, R, Q, groupU1Solution.get(i), next, group);
                        int Qiannext = next;
                        if ((getswarm1.getObjective(0) < groupU1Solution.get(i).getObjective(0)))
                            next = 0;
                        else next = 1;
                        old0 = groupU1Solution.get(i).getObjective(0);
                        new0 = getswarm1.getObjective(0);

                        currentReward = new0 - old0; // 计算奖励

                        Q[Qiannext][actionIndex] = calculateNewQ1(R, Q, actionIndex, next, Qiannext, old0, new0);

                        // Q[Qiannext][actionIndex] = calculateNewQ(R, Q,actionIndex,next,Qiannext);
                    }

                }
                replayBuffer.add(new Experience(currentState, actionIndex, currentReward, Q.clone(), false));

                // 从经验回放池中随机抽取一批经历进行训练
                if (replayBuffer.buffer.size() >= batchSize) {
                    List<Experience> batch = replayBuffer.sample(batchSize);
                    for (Experience exp : batch) {
                        double learningRate = 0.1;
                        double discountFactor = 0.99;
                        double maxNextQ = Math.max(exp.nextState[0][exp.action], exp.nextState[1][exp.action]);
                        double targetQ = exp.reward + discountFactor * maxNextQ;
                        double currentQ = exp.state[0][exp.action];
                        Q[0][exp.action] = currentQ + learningRate * (targetQ - currentQ);
                    }
                }


                max[k] = groupU1Solution.get(i).getObjective(0) - getswarm1.getObjective(0);
                temp.add(k, getswarm1);
            }
            int best = 0;
            for (int y = 1; y < QN; y++) {
                if (max[best] < max[y]) {
                    best = y;
                }
            }
            if (max[best] < max[QN - 1]) {
                swarmtemp.add(getswarm1);
            } else {
                swarmtemp.add(temp.get(best));
            }
//            groupU1Solution.set(i,swarmtemp.get(i));
            //swarmtemp.add(getswarm1);
        }

//        try {
//            Thread.sleep(99999);
//        } catch (InterruptedException e) {
//            throw new RuntimeException(e);
//        }
//

        ReplayBuffer replayBuffer2 = new ReplayBuffer(32); // 设置经验回放池的最大容量
        group = 2;
        for (int i = 0; i < centralSize; i++) {
            System.out.println(i);

            double[][] Q = new double[2][anum];
            double[][] R = new double[2][anum];
            for (int j = 0; j < anum; j++) {
                Q[0][j] = 0;
                Q[1][j] = 0;
            }

            for (int j = 0; j < anum; j++) {
                R[0][j] = 1;
                R[1][j] = 1;
            }
            //double r = 0.75, s = 0.85;
            Random random = authorRandom();
            int next = 1;
            int actionIndex;
            PermutationSolution<Integer> getswarm1 = null;
            double old0, old1, new0, new1, old2, new2;
            double[] max = new double[QN];
            List<PermutationSolution<Integer>> temp = new ArrayList<PermutationSolution<Integer>>(QN);
            for (int k = 0; k < QN; k++) {
                double p = random.nextDouble();
                double[][] currentState = Q.clone(); // 当前状态
                double currentReward = 0; // 当前奖励
                if (k == 0) {
                    actionIndex = random.nextInt(action.size());
                    getswarm1 = learn(actionIndex, R, Q, groupC2Solution.get(i), next, group);
                    int Qiannext = next;
                    if ((getswarm1.getObjective(0) < groupC2Solution.get(i).getObjective(0) && getswarm1.getObjective(1) < groupC2Solution.get(i).getObjective(1) && getswarm1.getObjective(6) < groupC2Solution.get(i).getObjective(6)) ||
                            getswarm1.getObjective(0) < groupC2Solution.get(i).getObjective(0) ||
                            getswarm1.getObjective(1) < groupC2Solution.get(i).getObjective(1) ||
                            getswarm1.getObjective(6) < groupC2Solution.get(i).getObjective(6))
                        next = 0;
                    else next = 1;

                    old0 = groupC2Solution.get(i).getObjective(0);
                    old1 = groupC2Solution.get(i).getObjective(1);
                    old2 = groupC2Solution.get(i).getObjective(6);
                    new0 = getswarm1.getObjective(0);
                    new1 = getswarm1.getObjective(1);
                    new2 = getswarm1.getObjective(6);
                    Q[Qiannext][actionIndex] = calculateNewQ2(R, Q, actionIndex, next, Qiannext, old0, old1, old2, new0, new1, new2);

                    //Q[Qiannext][actionIndex] = calculateNewQ(R, Q,actionIndex,next,Qiannext);
                } else {
                    if (p < 1 - tl) {
                        actionIndex = random.nextInt(action.size());
                        getswarm1 = learn(actionIndex, R, Q, groupC2Solution.get(i), next, group);
                        int Qiannext = next;
                        if ((getswarm1.getObjective(0) < groupC2Solution.get(i).getObjective(0) && getswarm1.getObjective(1) < groupC2Solution.get(i).getObjective(1) && getswarm1.getObjective(6) < groupC2Solution.get(i).getObjective(6)) ||
                                getswarm1.getObjective(0) < groupC2Solution.get(i).getObjective(0) ||
                                getswarm1.getObjective(1) < groupC2Solution.get(i).getObjective(1) ||
                                getswarm1.getObjective(6) < groupC2Solution.get(i).getObjective(6))
                            next = 0;
                        else next = 1;
                        old0 = groupC2Solution.get(i).getObjective(0);
                        old1 = groupC2Solution.get(i).getObjective(1);
                        old2 = groupC2Solution.get(i).getObjective(6);
                        new0 = getswarm1.getObjective(0);
                        new1 = getswarm1.getObjective(1);
                        new2 = getswarm1.getObjective(6);
                        currentReward = new0 - old0 + new1 - old1 + new2 - old2; // 计算奖励
                        Q[Qiannext][actionIndex] = calculateNewQ2(R, Q, actionIndex, next, Qiannext, old0, old1, old2, new0, new1, new2);

                        // Q[Qiannext][actionIndex] = calculateNewQ(R, Q,actionIndex,next,Qiannext);
                    } else {

                        actionIndex = max(Q[next]);
                        getswarm1 = learn(actionIndex, R, Q, groupC2Solution.get(i), next, group);
                        int Qiannext = next;
                        if ((getswarm1.getObjective(0) < groupC2Solution.get(i).getObjective(0) && getswarm1.getObjective(1) < groupC2Solution.get(i).getObjective(1) && getswarm1.getObjective(6) < groupC2Solution.get(i).getObjective(6)) ||
                                getswarm1.getObjective(0) < groupC2Solution.get(i).getObjective(0) ||
                                getswarm1.getObjective(1) < groupC2Solution.get(i).getObjective(1) ||
                                getswarm1.getObjective(6) < groupC2Solution.get(i).getObjective(6))
                            next = 0;
                        else next = 1;
                        old0 = groupC2Solution.get(i).getObjective(0);
                        old1 = groupC2Solution.get(i).getObjective(1);
                        old2 = groupC2Solution.get(i).getObjective(6);
                        new0 = getswarm1.getObjective(0);
                        new1 = getswarm1.getObjective(1);
                        new2 = getswarm1.getObjective(6);
                        currentReward = new0 - old0 + new1 - old1 + new2 - old2; // 计算奖励
                        Q[Qiannext][actionIndex] = calculateNewQ2(R, Q, actionIndex, next, Qiannext, old0, old1, old2, new0, new1, new2);

                        // Q[Qiannext][actionIndex] = calculateNewQ(R, Q,actionIndex,next,Qiannext);
                    }
                }

                replayBuffer2.add(new Experience(currentState, actionIndex, currentReward, Q.clone(), false));

                // 从经验回放池中随机抽取一批经历进行训练
                if (replayBuffer2.buffer.size() >= batchSize) {
                    List<Experience> batch = replayBuffer2.sample(batchSize);
                    for (Experience exp : batch) {
                        double learningRate = 0.1;
                        double discountFactor = 0.99;
                        double maxNextQ = Math.max(exp.nextState[0][exp.action], exp.nextState[1][exp.action]);
                        double targetQ = exp.reward + discountFactor * maxNextQ;
                        double currentQ = exp.state[0][exp.action];
                        Q[0][exp.action] = currentQ + learningRate * (targetQ - currentQ);
                    }
                }
                max[k] = (groupC2Solution.get(i).getObjective(0) - getswarm1.getObjective(0)) + (groupC2Solution.get(i).getObjective(1) - getswarm1.getObjective(1)) + (groupC2Solution.get(i).getObjective(6) - getswarm1.getObjective(6));
                temp.add(k, getswarm1);
            }
            int best = 0;
            for (int y = 1; y < QN; y++) {
                if (max[best] < max[y]) {
                    best = y;
                }
            }
            if (max[best] < max[QN - 1]) {
                swarmtemp.add(getswarm1);
            } else {
                swarmtemp.add(temp.get(best));
            }
            groupC2Solution.set(i, swarmtemp.get(i));
        }


        ReplayBuffer replayBuffer3 = new ReplayBuffer(32); // 设置经验回放池的最大容量
        group = 3;
        for (int i = 0; i < downSize; i++) {
            System.out.println(i);

            double[][] Q = new double[2][anum];
            double[][] R = new double[2][anum];
            for (int j = 0; j < anum; j++) {
                Q[0][j] = 0;
                Q[1][j] = 0;
            }

            for (int j = 0; j < anum; j++) {
                R[0][j] = 1;
                R[1][j] = 1;
            }
            //double r = 0.75, s = 0.85;
            Random random = authorRandom();
            int next = 1;
            int actionIndex;
            PermutationSolution<Integer> getswarm1 = null;
            double old0, old1, new0, new1;
            double[] max = new double[QN];
            List<PermutationSolution<Integer>> temp = new ArrayList<PermutationSolution<Integer>>(QN);
            for (int k = 0; k < QN; k++) {
                double p = random.nextDouble();
                double[][] currentState = Q.clone(); // 当前状态
                double currentReward = 0; // 当前奖励
                if (k == 0) {
                    actionIndex = random.nextInt(action.size());
                    getswarm1 = learn(actionIndex, R, Q, groupD3Solution.get(i), next, group);
                    int Qiannext = next;
                    if ((getswarm1.getObjective(1) < groupD3Solution.get(i).getObjective(1)))
                        next = 0;
                    else next = 1;

                    old0 = groupD3Solution.get(i).getObjective(1);
                    new0 = getswarm1.getObjective(1);
                    currentReward = new0 - old0; // 计算奖励
                    Q[Qiannext][actionIndex] = calculateNewQ1(R, Q, actionIndex, next, Qiannext, old0, new0);
                    //Q[Qiannext][actionIndex] = calculateNewQ(R, Q,actionIndex,next,Qiannext);
                } else {
                    if (p < 1 - tl) {
                        actionIndex = random.nextInt(action.size());
                        getswarm1 = learn(actionIndex, R, Q, groupD3Solution.get(i), next, group);
                        int Qiannext = next;
                        if ((getswarm1.getObjective(1) < groupD3Solution.get(i).getObjective(1)))
                            next = 0;
                        else next = 1;
                        old0 = groupD3Solution.get(i).getObjective(1);
                        new0 = getswarm1.getObjective(1);
                        currentReward = new0 - old0; // 计算奖励
                        Q[Qiannext][actionIndex] = calculateNewQ1(R, Q, actionIndex, next, Qiannext, old0, new0);

                        //Q[Qiannext][actionIndex] = calculateNewQ(R, Q,actionIndex,next,Qiannext);
                    } else {

                        actionIndex = max(Q[next]);
                        getswarm1 = learn(actionIndex, R, Q, groupD3Solution.get(i), next, group);
                        int Qiannext = next;
                        if ((getswarm1.getObjective(1) < groupD3Solution.get(i).getObjective(1)))
                            next = 0;
                        else next = 1;
                        old0 = groupD3Solution.get(i).getObjective(1);
                        new0 = getswarm1.getObjective(1);
                        currentReward = new0 - old0; // 计算奖励
                        Q[Qiannext][actionIndex] = calculateNewQ1(R, Q, actionIndex, next, Qiannext, old0, new0);

                        //Q[Qiannext][actionIndex] = calculateNewQ(R, Q,actionIndex,next,Qiannext);
                    }
                }

                // 添加当前经历到经验回放池
                replayBuffer3.add(new Experience(currentState, actionIndex, currentReward, Q.clone(), false));

                // 从经验回放池中随机抽取一批经历进行训练
                if (replayBuffer3.buffer.size() >= batchSize) {
                    List<Experience> batch = replayBuffer3.sample(batchSize);
                    for (Experience exp : batch) {
                        double learningRate = 0.1;
                        double discountFactor = 0.99;
                        double maxNextQ = Math.max(exp.nextState[0][exp.action], exp.nextState[1][exp.action]);
                        double targetQ = exp.reward + discountFactor * maxNextQ;
                        double currentQ = exp.state[0][exp.action];
                        Q[0][exp.action] = currentQ + learningRate * (targetQ - currentQ);
                    }
                }
                max[k] = groupD3Solution.get(i).getObjective(1) - getswarm1.getObjective(1);
                temp.add(k, getswarm1);
            }
            int best = 0;
            for (int y = 1; y < QN; y++) {
                if (max[best] < max[y]) {
                    best = y;
                }
            }
            if (max[best] < max[QN - 1]) {
                swarmtemp.add(getswarm1);
            } else {
                swarmtemp.add(temp.get(best));
            }
            groupD3Solution.set(i, swarmtemp.get(i));
        }


        ReplayBuffer replayBuffer4 = new ReplayBuffer(32); // 设置经验回放池的最大容量
        group = 4;
        for (int i = 0; i < upNewSize; i++) {
            System.out.println(i);
            double[][] Q = new double[2][anum];
            double[][] R = new double[2][anum];
            for (int j = 0; j < anum; j++) {
                Q[0][j] = 0;
                Q[1][j] = 0;
            }

            for (int j = 0; j < anum; j++) {
                R[0][j] = 1;
                R[1][j] = 1;
            }
            //double r = 0.75, s = 0.85;
            Random random = authorRandom();
            int next = 1;
            int actionIndex;
            PermutationSolution<Integer> getswarm1 = null;
            double old0, old1, new0, new1;
            double[] max = new double[QN];
            List<PermutationSolution<Integer>> temp = new ArrayList<PermutationSolution<Integer>>(QN);
            for (int k = 0; k < QN; k++) {
                double p = random.nextDouble();
                double[][] currentState = Q.clone(); // 当前状态
                double currentReward = 0; // 当前奖励
                if (k == 0) {
                    actionIndex = random.nextInt(action.size());
                    getswarm1 = learn(actionIndex, R, Q, groupUNewSolution.get(i), next, group);
                    int Qiannext = next;
                    if ((getswarm1.getObjective(6) < groupUNewSolution.get(i).getObjective(6)))
                        next = 0;
                    else next = 1;

                    old0 = groupUNewSolution.get(i).getObjective(6);
                    new0 = getswarm1.getObjective(6);
                    currentReward = new0 - old0; // 计算奖励
                    Q[Qiannext][actionIndex] = calculateNewQ1(R, Q, actionIndex, next, Qiannext, old0, new0);
                    //Q[Qiannext][actionIndex] = calculateNewQ(R, Q,actionIndex,next,Qiannext);
                } else {
                    if (p < 1 - tl) {
                        actionIndex = random.nextInt(action.size());
                        getswarm1 = learn(actionIndex, R, Q, groupUNewSolution.get(i), next, group);
                        int Qiannext = next;
                        if ((getswarm1.getObjective(6) < groupUNewSolution.get(i).getObjective(6)))
                            next = 0;
                        else next = 1;
                        old0 = groupUNewSolution.get(i).getObjective(6);
                        new0 = getswarm1.getObjective(6);
                        currentReward = new0 - old0; // 计算奖励
                        Q[Qiannext][actionIndex] = calculateNewQ1(R, Q, actionIndex, next, Qiannext, old0, new0);

                        //Q[Qiannext][actionIndex] = calculateNewQ(R, Q,actionIndex,next,Qiannext);
                    } else {

                        actionIndex = max(Q[next]);
                        getswarm1 = learn(actionIndex, R, Q, groupUNewSolution.get(i), next, group);
                        int Qiannext = next;
                        if ((getswarm1.getObjective(6) < groupUNewSolution.get(i).getObjective(6)))
                            next = 0;
                        else next = 1;
                        old0 = groupUNewSolution.get(i).getObjective(6);
                        new0 = getswarm1.getObjective(6);
                        currentReward = new0 - old0; // 计算奖励
                        Q[Qiannext][actionIndex] = calculateNewQ1(R, Q, actionIndex, next, Qiannext, old0, new0);

                        //Q[Qiannext][actionIndex] = calculateNewQ(R, Q,actionIndex,next,Qiannext);
                    }
                }

                // 添加当前经历到经验回放池
                replayBuffer4.add(new Experience(currentState, actionIndex, currentReward, Q.clone(), false));

                // 从经验回放池中随机抽取一批经历进行训练
                if (replayBuffer4.buffer.size() >= batchSize) {
                    List<Experience> batch = replayBuffer4.sample(batchSize);
                    for (Experience exp : batch) {
                        double learningRate = 0.1;
                        double discountFactor = 0.99;
                        double maxNextQ = Math.max(exp.nextState[0][exp.action], exp.nextState[1][exp.action]);
                        double targetQ = exp.reward + discountFactor * maxNextQ;
                        double currentQ = exp.state[0][exp.action];
                        Q[0][exp.action] = currentQ + learningRate * (targetQ - currentQ);
                    }
                }
                max[k] = groupUNewSolution.get(i).getObjective(6) - getswarm1.getObjective(6);
                temp.add(k, getswarm1);
            }
            int best = 0;
            for (int y = 1; y < QN; y++) {
                if (max[best] < max[y]) {
                    best = y;
                }
            }
            if (max[best] < max[QN - 1]) {
                swarmtemp.add(getswarm1);
            } else {
                swarmtemp.add(temp.get(best));
            }
            groupUNewSolution.set(i, swarmtemp.get(i));
        }

//        try {
//            Thread.sleep(9999999);
//        } catch (InterruptedException e) {
//            throw new RuntimeException(e);
//        }

        mergeNew(swarmtemp);

        swarmtemp = selectFac(swarmtemp);

        swarmtemp = evaluateSwarm(swarmtemp);
//        System.out.println(swarmtemp.size());
//        try {
//            Thread.sleep(9999);
//        } catch (InterruptedException e) {
//            throw new RuntimeException(e);
//        }

        for (int i = 0; i < swarmSize; i++) {
            DEswarmtempPdflag[i] = i;
        }


        super.setSwarm(PDDRFFselect(swarm, swarmtemp, DEswarmtempPdflag));
//////////////////////////////////////////////////////////////////////////////////////
/*        List<PermutationSolution<Integer>> swarmFac = new ArrayList<PermutationSolution<Integer>>(swarm.size());

        swarmFac=selectFac(swarm);
        swarmFac = evaluateSwarm(swarmFac);

        for (int i = 0; i < swarmSize; i++) {
            DEswarmtempPdflag[i] = i;
        }
        super.setSwarm(PDDRFFselect(swarm,swarmFac,DEswarmtempPdflag));*/
    }


    //没有经验池的版本，20241117之前一直用的
    protected void perturbation(List<PermutationSolution<Integer>> swarm) {
        if (!globalSearchConfiguration.isLocalSearchEnabled()) {
            return;
        }
        if (problemContext == null) {
            throw new IllegalStateException("CA-TA requires a ZhangBoProblemContext");
        }
        if (swarm.size() != swarmSize) {
            throw new IllegalStateException("CA-TA expects one global offspring per particle before local search");
        }
        if (fullEvaluationCount + swarm.size() > maxIterations) {
            throw new IllegalStateException("Insufficient FE budget for a complete global offspring generation");
        }
        pendingCaTaLocalCandidates.clear();
        caTaRewardsSettled = false;
        ZhangBoProblemContext fatigueProblem = problemContext;
        evaluateAndMarkGlobalOffspring(swarm);
        // Q credit is deliberately settled before any local candidate is created.
        settleOriginalQg(swarm);
        if (globalSearchConfiguration.isQpEnabled()) {
            settleQp(swarm);
        }
        recordDualQCoordination();
        caTaRewardsSettled = true;
        if (globalSearchConfiguration.isCaTaEnabled()) {
            if (globalSearchConfiguration.isV35CaTaLiteEnabled()) {
                runV35CaTaLiteLocalSearch(swarm, fatigueProblem);
            } else {
                runCaTaLocalSearch(swarm, fatigueProblem);
            }
        } else {
            runFixedNeighborhoodSearch(swarm, fatigueProblem);
        }
    }

    private void evaluateAndMarkGlobalOffspring(List<PermutationSolution<Integer>> swarm) {
        for (int slot = 0; slot < swarm.size(); slot++) {
            PermutationSolution<Integer> candidate = swarm.get(slot);
            if (ZhangBoPreEvaluatedTag.isMarked(candidate)) {
                throw new IllegalStateException("Global CFVF offspring unexpectedly carried a pre-evaluation marker");
            }
            V35EvaluationSourceContext.begin(V35EvaluationSourceContext.Source.GLOBAL_CFVF);
            try {
                problem.evaluate(candidate);
                fullEvaluationCount++;
            } finally {
                V35EvaluationSourceContext.end();
            }
            if (cmaxAudit != null) {
                Object groupValue = candidate.getAttribute(ZhangBoSubSwarm.class);
                ZhangBoSubSwarm group = groupValue instanceof ZhangBoSubSwarm
                        ? (ZhangBoSubSwarm) groupValue : null;
                cmaxAudit.observeGenerated(fullEvaluationCount, generationNumber(), candidate,
                        group,
                        globalSearchConfiguration.isCfvfEnabled()
                                ? ZhangBoCmaxAudit.Mechanism.CFVF
                                : ZhangBoCmaxAudit.Mechanism.BASELINE_GLOBAL,
                        globalSearchConfiguration.isCfvfEnabled()
                                ? ZhangBoCmaxAudit.Operator.CFVF
                                : ZhangBoCmaxAudit.Operator.BASELINE_GLOBAL,
                        "GLOBAL_SLOT_" + (group == null ? "UNASSIGNED" : group), true);
            }
            fc52RecordEvaluated(candidate, fullEvaluationCount,
                    V35EvaluationSourceContext.Source.GLOBAL_CFVF);
            // V35-SOURCE-LEDGER-PATCH
            observePassiveArchive(candidate, V35EvaluationSourceContext.Source.GLOBAL_CFVF);
            ZhangBoPreEvaluatedTag.mark(candidate, new ZhangBoPreEvaluatedTag(
                    globalSearchConfiguration.isCfvfEnabled()
                            ? ZhangBoPreEvaluatedTag.Source.GLOBAL_CFVF
                            : ZhangBoPreEvaluatedTag.Source.GLOBAL_OFFSPRING,
                    slot, lineageId(candidate),
                    fullEvaluationCount));
        }
        zhangBoCaTaEvents.add("generation=" + generationNumber()
                + ",globalOffspringEvaluations=" + swarm.size()
                + ",feAfterGlobal=" + fullEvaluationCount);
    }

    /**
     * Returns the generation-frozen archive normalization when lineage archive is enabled;
     * otherwise derives objective-only bounds from the current offspring.  B0 intentionally
     * has no lineage archive, but its fixed O1-O9 path still needs the same quality metric.
     */
    private ZhangBoArchiveBounds localSearchQualityBounds(
            List<PermutationSolution<Integer>> globalOffspring) {
        if (zhangBoLineageCoordinator != null
                && zhangBoLineageCoordinator.getFrozenBounds() != null) {
            return zhangBoLineageCoordinator.getFrozenBounds();
        }
        return ZhangBoArchiveBounds.fromSolutions(globalOffspring,
                globallyOptimalIndividual, 1.0e-9, true);
    }

    private void runCaTaLocalSearch(
            List<PermutationSolution<Integer>> globalOffspring, ZhangBoProblemContext fatigueProblem) {
        ZhangBoArchiveBounds qualityBounds = localSearchQualityBounds(globalOffspring);
        List<CaTaParent> parents = caTaParents(globalOffspring);
        P8AblationProfile profile = globalSearchConfiguration.getP8AblationProfile();
        for (CaTaParent parent : parents) {
            long parentLineage = lineageId(parent.solution);
            if (fullEvaluationCount >= maxIterations) {
                zhangBoCaTaEvents.add("generation=" + generationNumber()
                        + ",localStop=FE_BUDGET_EXHAUSTED");
                return;
            }
            ZhangBoFatigueEvaluationResult evaluation = fatigueResult(parent.solution);
            if (evaluation == null) {
                zhangBoCaTaEvents.add("generation=" + generationNumber() + ",slot="
                        + parent.slot + ",localSkip=NO_FATIGUE_RESULT");
                continue;
            }
            ZhangBoBottleneckClassifier.Classification classification =
                    zhangBoBottleneckClassifier.classify(evaluation);
            ZhangBoCaTaPhase phase = ZhangBoCaTaPhase.fromProgress(fullEvaluationCount, maxIterations);
            ZhangBoBottleneck bottleneck = classification.getBottleneck();
            ZhangBoSubSwarm contextGroup = parent.group;
            if (profile != null && !profile.isContextEnabled()) {
                contextGroup = ZhangBoSubSwarm.G4_BALANCED;
                phase = ZhangBoCaTaPhase.MIDDLE;
                bottleneck = ZhangBoBottleneck.BAL;
            } else if (profile != null && !profile.isFatBottleneckEnabled()
                    && bottleneck == ZhangBoBottleneck.FAT) {
                bottleneck = ZhangBoBottleneck.BAL;
            }
            boolean stagnated = profile != null && !profile.isContextEnabled() ? false
                    : zhangBoCaTaController.isStagnated(contextGroup, phase, bottleneck);
            ZhangBoCaTaContext context = new ZhangBoCaTaContext(contextGroup, phase, stagnated,
                    bottleneck);
            ZhangBoFactoryNeedSelector.Selection factory = zhangBoFactoryNeedSelector.select(
                    evaluation, fatigueProblem.getFatigueInstanceData().getFactories(), parent.group,
                    globalSearchConfiguration.getCaTaConfiguration().getNeedWeightedProbability(),
                    zhangBoCaTaRandom);
            if (!factory.isApplicable()) {
                zhangBoCaTaEvents.add("generation=" + generationNumber() + ",slot="
                        + parent.slot + ",context=" + context + ",localSkip=" + factory.getReason());
                continue;
            }
            long requestSeed = caTaRequestSeed(parent, context,
                    zhangBoCaTaController.getContextEpoch(context), -1L,
                    null, 0);
            ZhangBoNeighborhoodRequest request = new ZhangBoNeighborhoodRequest(parent.solution,
                    fatigueProblem.getFatigueInstanceData(), fatigueProblem.getFatigueParameters(),
                    factory.getFactory(), parent.group, requestSeed,
                    bottleneck == ZhangBoBottleneck.FAT ? ZhangBoFatigueFocus.FMAX : ZhangBoFatigueFocus.FE);
            CaTaPreparedMask preparedMask = prepareCaTaNeighborhoods(context, request);
            ZhangBoCaTaController.Decision decision = zhangBoCaTaController.decide(
                    context, preparedMask.valid,
                    zhangBoCaTaRandom);
            zhangBoCaTaEvents.add("generation=" + generationNumber() + ",slot=" + parent.slot
                    + ",lineage=" + parentLineage
                    + ",context=" + context + ",factory=" + factory.getFactory()
                    + ",factoryMode=" + factory.getReason() + ",decision=" + decision.getReason()
                    + ",test=" + decision.isTestPhase() + ",exploratory=" + decision.isExploratory()
                    + ",contextEpoch=" + decision.getContextEpoch()
                    + ",callOrdinal=" + decision.getCallOrdinal()
                    + ",remainingApplyCalls=" + decision.getRemainingApplyCalls()
                    + ",neighborhoods=" + decision.getNeighborhoods());
            for (ZhangBoNeighborhoodId id : decision.getNeighborhoods()) {
                for (int repetition = 0; repetition < decision.getRepetitions(); repetition++) {
                    if (fullEvaluationCount >= maxIterations) {
                        zhangBoCaTaEvents.add("generation=" + generationNumber()
                                + ",localStop=FE_BUDGET_EXHAUSTED");
                        return;
                    }
                    // The applicability preview is not evaluated.  The actual candidate must
                    // be regenerated from the complete governed key after the controller has
                    // assigned its epoch/call ordinal; otherwise a mask change can accidentally
                    // reuse a candidate from the preceding Test epoch.
                    long attemptSeed = caTaRequestSeed(parent, context,
                            decision.getContextEpoch(), decision.getCallOrdinal(), id, repetition);
                    ZhangBoNeighborhoodCandidateGateway.PreparedPreview prepared =
                            zhangBoNeighborhoodCandidateGateway.prepare(
                                    id, request.withSeed(attemptSeed));
                    ZhangBoNeighborhoodCandidateGateway.Attempt attempt =
                            zhangBoNeighborhoodCandidateGateway.evaluateOne(prepared,
                                    new ZhangBoNeighborhoodCandidateGateway.CompleteEvaluator() {
                                        @Override
                                        public void evaluate(PermutationSolution<Integer> candidate) {
                                            V35EvaluationSourceContext.begin(decision.isTestPhase()
                                                    ? V35EvaluationSourceContext.Source.CATA_TEST
                                                    : V35EvaluationSourceContext.Source.CATA_APPLY);
                                            try {
                                                problem.evaluate(candidate);
                                            } finally {
                                                V35EvaluationSourceContext.end();
                                            }
                                        }
                                    });
                    if (!attempt.isApplicable()) {
                        // Masked actions are not failures and consume no FE.
                        zhangBoCaTaEvents.add("generation=" + generationNumber() + ",slot="
                                + parent.slot + ",id=" + id + ",notApplicable=" + attempt.getReason());
                        continue;
                    }
                    fullEvaluationCount += attempt.getCompleteEvaluations();
                    caTaFullEvaluations += attempt.getCompleteEvaluations();
                    PermutationSolution<Integer> local = attempt.getCandidate();
                    fc52RecordEvaluated(local, fullEvaluationCount,
                            decision.isTestPhase()
                                    ? V35EvaluationSourceContext.Source.CATA_TEST
                                    : V35EvaluationSourceContext.Source.CATA_APPLY);
                    ZhangBoFatigueEvaluationResult localEvaluation = fatigueResult(local);
                    boolean recoveryGain = ZhangBoNaturalRecoveryGate.allows(
                            id, evaluation, localEvaluation);
                    boolean accepted = recoveryGain && ZhangBoLocalSearchAcceptance.accepts(
                            parent.solution, local, parent.group);
                    if (accepted) {
                        fc52LocalAccepted(local, fullEvaluationCount, "ACCEPTED");
                    } else {
                        fc52LocalRejected(local, fullEvaluationCount,
                                recoveryGain ? "NOT_BETTER" : "NO_RECOVERY_GAIN");
                    }
                    double gain = ZhangBoLocalSearchAcceptance.qualityGain(parent.solution, local,
                            parent.group, qualityBounds);
                    if (cmaxAudit != null) {
                    cmaxAudit.observeGenerated(fullEvaluationCount, generationNumber(), local,
                            parent.group, ZhangBoCmaxAudit.Mechanism.CA_TA,
                            cmaxAuditOperator(id),
                            "PARENT_SLOT_" + parent.slot + "_LINEAGE_" + parentLineage,
                            accepted);
                }
                // V35-SOURCE-LEDGER-PATCH
                observePassiveArchive(local, decision.isTestPhase()
                        ? V35EvaluationSourceContext.Source.CATA_TEST
                        : V35EvaluationSourceContext.Source.CATA_APPLY);
                    zhangBoCaTaController.record(context, id, accepted, gain,
                            attempt.getElapsedNanos(), attempt.getCompleteEvaluations());
                    if (decision.isTestPhase()) caTaTestCalls++;
                    else caTaApplyCalls++;
                    zhangBoCaTaEvents.add("generation=" + generationNumber() + ",slot="
                            + parent.slot + ",lineage=" + parentLineage
                            + ",id=" + id + ",accepted=" + accepted
                            + ",contextEpoch=" + decision.getContextEpoch()
                            + ",callOrdinal=" + decision.getCallOrdinal()
                            + ",repetition=" + repetition
                            + ",attemptSeed=" + attemptSeed
                            + ",recoveryGain=" + recoveryGain + ",qGain=" + gain
                            + ",parentFingerprint=" + ZhangBoQgController.fingerprint(parent.solution)
                            + ",parentObjectives=" + objectiveText(parent.solution)
                            + ",localFingerprint=" + ZhangBoQgController.fingerprint(local)
                            + ",localObjectives=" + objectiveText(local)
                            + ",fe=" + attempt.getCompleteEvaluations());
                    if (accepted) {
                        local.setAttribute(ZhangBoSubSwarm.class, parent.group);
                        ZhangBoPreEvaluatedTag.mark(local, new ZhangBoPreEvaluatedTag(
                                ZhangBoPreEvaluatedTag.Source.INTRA_FACTORY_VNS, parent.slot,
                                lineageId(parent.solution), fullEvaluationCount));
                        pendingCaTaLocalCandidates.add(new PendingCaTaLocalCandidate(local,
                                parent.slot, parent.history, LocalCandidateOrigin.O1_O9));
                        globalOffspring.add(local);
                    }
                }
            }
        }
    }

    /** v3.5 CA-TA-Lite bridge: 24 role/bottleneck contexts and five macro actions. */
    private void runV35CaTaLiteLocalSearch(
            List<PermutationSolution<Integer>> globalOffspring, ZhangBoProblemContext fatigueProblem) {
        if (v35CaTaLiteController == null) {
            throw new IllegalStateException("v3.5 CA-TA-Lite controller is not initialized");
        }
        ZhangBoArchiveBounds qualityBounds = localSearchQualityBounds(globalOffspring);
        for (CaTaParent parent : caTaParents(globalOffspring)) {
            if (fullEvaluationCount >= localFeHardLimit()) return;
            ZhangBoFatigueEvaluationResult evaluation = fatigueResult(parent.solution);
            if (evaluation == null) continue;
            ZhangBoFactoryNeedSelector.Selection factory = zhangBoFactoryNeedSelector.select(
                    evaluation, fatigueProblem.getFatigueInstanceData().getFactories(), parent.group,
                    globalSearchConfiguration.getCaTaConfiguration().getNeedWeightedProbability(),
                    zhangBoCaTaRandom);
            if (!factory.isApplicable()) continue;
            V35PressureBottleneckClassifier.Classification classification =
                    v35PressureBottleneckClassifier.classify(evaluation,
                            fatigueProblem.getFatigueInstanceData(), fatigueProblem.getFatigueParameters(),
                            factory.getFactory(), globalSearchConfiguration.getV35BottleneckDiagnosis());
            V35CaTaContext context = new V35CaTaContext(
                    v35Role(parent.group), classification.getBottleneck());
            List<V35MacroNeighborhood> valid = new ArrayList<>();
            Map<V35MacroNeighborhood, V35MacroCandidateGateway.Prepared> previews =
                    new EnumMap<>(V35MacroNeighborhood.class);
            for (V35MacroNeighborhood action : V35MacroNeighborhood.values()) {
                if (!context.allows(action)) continue;
                // V35-FC-1: N3/N4 candidate generation reads the parent's FM3
                // actual trace (zero-slack critical DAG / actual bottleneck);
                // the shadow audit below keeps the PT0 proxy for comparability.
                V35MacroCandidateGateway.Prepared preview = v35MacroCandidateGateway
                        .prepareWithEvaluation(
                        action, parent.solution, fatigueProblem.getFatigueInstanceData(),
                        factory.getFactory(), context.getBottleneck(), evaluation);
                if (preview.isApplicable()) {
                    valid.add(action);
                    previews.put(action, preview);
                    // V35-FC-1 audit: only N3/N4 are structure-routed; the
                    // other macros count neither as FM3 nor as proxy.
                    if (action == V35MacroNeighborhood.N3
                            || action == V35MacroNeighborhood.N4) {
                        if (V35MacroCandidateGateway.Prepared.SOURCE_FM3_ACTUAL
                                .equals(preview.getStructureSource())) {
                            v35Fm3StructurePreviews++;
                        } else {
                            v35ProxyStructurePreviews++;
                        }
                    }
                }
            }
            if (v35ShadowDiagnosisAudit != null) {
                List<V35MacroCandidateGateway.Prepared> shadowPreviews = new ArrayList<>();
                for (V35MacroNeighborhood action : V35MacroNeighborhood.values()) {
                    V35MacroCandidateGateway.Prepared shadowPreview =
                            v35MacroCandidateGateway.prepare(action, parent.solution,
                                    fatigueProblem.getFatigueInstanceData(), factory.getFactory(),
                                    classification.getMaximumType());
                    if (shadowPreview.isApplicable()) shadowPreviews.add(shadowPreview);
                }
                if (v35ShadowDiagnosisAudit.shouldSample(shadowPreviews.size())) {
                    List<V35ShadowDiagnosisAudit.Outcome> outcomes = new ArrayList<>();
                    for (V35MacroCandidateGateway.Prepared shadowPreview : shadowPreviews) {
                        V35MacroCandidateGateway.Attempt shadowAttempt =
                                v35MacroCandidateGateway.evaluateOne(shadowPreview,
                                        new V35MacroCandidateGateway.CompleteEvaluator() {
                                            @Override public void evaluate(
                                                    PermutationSolution<Integer> candidate) {
                                                V35EvaluationSourceContext.begin(
                                                        V35EvaluationSourceContext.Source.SHADOW);
                                                try {
                                                    v35ShadowProblem.evaluate(candidate);
                                                } finally {
                                                    V35EvaluationSourceContext.end();
                                                }
                                            }
                                        });
                        if (!shadowAttempt.isApplicable()) continue;
                        PermutationSolution<Integer> shadowCandidate = shadowAttempt.getCandidate();
                        ZhangBoFatigueEvaluationResult shadowEvaluation = fatigueResult(shadowCandidate);
                        V35MacroNeighborhood shadowAction = shadowPreview.getAction();
                        boolean recoveryGain = (shadowAction != V35MacroNeighborhood.N4
                                && shadowAction != V35MacroNeighborhood.N5)
                                || (shadowEvaluation != null
                                && shadowEvaluation.getMetrics().totalNaturalRecovery
                                > evaluation.getMetrics().totalNaturalRecovery + 1.0e-12);
                        boolean accepted = recoveryGain && ZhangBoLocalSearchAcceptance.accepts(
                                parent.solution, shadowCandidate, parent.group);
                        double gain = ZhangBoLocalSearchAcceptance.qualityGain(parent.solution,
                                shadowCandidate, parent.group, qualityBounds);
                        outcomes.add(new V35ShadowDiagnosisAudit.Outcome(shadowAction,
                                shadowCandidate.getObjective(0), shadowCandidate.getObjective(1),
                                shadowCandidate.getObjective(6), gain, accepted));
                    }
                    v35ShadowDiagnosisAudit.record(generationNumber(), fullEvaluationCount,
                            parent.slot, factory.getFactory(), v35Role(parent.group), classification,
                            valid, outcomes);
                }
            }
            V35CaTaLiteController.Decision decision = v35CaTaLiteController.decide(
                    context, valid, zhangBoCaTaRandom);
            v35PressureDiagnosisEvents.add(classification.toCsv(generationNumber(),
                    fullEvaluationCount, parent.slot, v35Role(parent.group))
                    + "," + macroListText(valid) + ","
                    + (decision.isTest() ? "TEST" : "APPLY") + ","
                    + macroListText(decision.getActions()));
            zhangBoCaTaEvents.add("v35Lite:generation=" + generationNumber()
                    + ",slot=" + parent.slot + ",context=" + context.getRole() + "|"
                    + context.getBottleneck() + ",phase=" + ZhangBoCaTaPhase.fromProgress(fullEvaluationCount, maxIterations)
                    + ",factory=" + factory.getFactory()
                    + ",pressures=SEQ:" + classification.getPressure(V35Bottleneck.SEQ)
                    + "|MAC:" + classification.getPressure(V35Bottleneck.MAC)
                    + "|WOR:" + classification.getPressure(V35Bottleneck.WOR)
                    + "|SET:" + classification.getPressure(V35Bottleneck.SET)
                    + "|FAT:" + classification.getPressure(V35Bottleneck.FAT)
                    + ",pressureMax=" + classification.getMaximumType() + ":"
                    + classification.getMaximumPressure()
                    + ",pressureSecond=" + classification.getSecondType() + ":"
                    + classification.getSecondPressure()
                    + ",pressureGap=" + classification.getGap()
                    + ",diagnosisReason=" + classification.getReason()
                    + ",test=" + decision.isTest() + ",reason=" + decision.getReason()
                    + ",actions=" + decision.getActions() + ",remaining=" + decision.getRemainingApplyCalls()
                    + ",routes=" + routesText(previews));
            for (V35MacroNeighborhood action : decision.getActions()) {
                if (fullEvaluationCount >= localFeHardLimit()) return;
                long attemptSeed = mixCaTaSeed(globalSearchConfiguration.getSeed()
                        ^ generationNumber() ^ parent.slot ^ decision.getEpoch()
                        ^ decision.getCallOrdinal() ^ action.ordinal());
                V35MacroCandidateGateway.Prepared prepared = previews.get(action);
                if (prepared == null) continue;
                V35MacroCandidateGateway.Attempt attempt = v35MacroCandidateGateway.evaluateOne(prepared,
                        new V35MacroCandidateGateway.CompleteEvaluator() {
                            @Override public void evaluate(PermutationSolution<Integer> candidate) {
                                V35EvaluationSourceContext.begin(decision.isTest()
                                        ? V35EvaluationSourceContext.Source.CATA_TEST
                                        : V35EvaluationSourceContext.Source.CATA_APPLY);
                                try {
                                    problem.evaluate(candidate);
                                } finally {
                                    V35EvaluationSourceContext.end();
                                }
                            }
                        });
                if (!attempt.isApplicable()) continue;
                fullEvaluationCount += attempt.getCompleteEvaluations();
                caTaFullEvaluations += attempt.getCompleteEvaluations();
                ZhangBoEvaluatedPddrSelector.Source fc6Source = decision.isTest()
                        ? ZhangBoEvaluatedPddrSelector.Source.CATA_TEST
                        : ZhangBoEvaluatedPddrSelector.Source.CATA_APPLY;
                PermutationSolution<Integer> local = attempt.getCandidate();
                fc6RecordEvaluated(fc6Source, local);
                fc52RecordEvaluated(local, fullEvaluationCount,
                        decision.isTest()
                                ? V35EvaluationSourceContext.Source.CATA_TEST
                                : V35EvaluationSourceContext.Source.CATA_APPLY);
                ZhangBoFatigueEvaluationResult localEvaluation = fatigueResult(local);
                boolean recoveryGain = (action != V35MacroNeighborhood.N4
                        && action != V35MacroNeighborhood.N5)
                        || (localEvaluation != null
                        && localEvaluation.getMetrics().totalNaturalRecovery
                        > evaluation.getMetrics().totalNaturalRecovery + 1.0e-12);
                boolean accepted = recoveryGain
                        && ZhangBoLocalSearchAcceptance.accepts(parent.solution, local, parent.group);
                if (accepted) {
                    fc52LocalAccepted(local, fullEvaluationCount, "ACCEPTED");
                    fc6RecordAccepted(fc6Source);
                } else {
                    fc52LocalRejected(local, fullEvaluationCount,
                            recoveryGain ? "NOT_BETTER" : "NO_RECOVERY_GAIN");
                }
                double gain = ZhangBoLocalSearchAcceptance.qualityGain(parent.solution, local, parent.group, qualityBounds);
                if (cmaxAudit != null) {
                    cmaxAudit.observeGenerated(fullEvaluationCount, generationNumber(), local,
                            parent.group, ZhangBoCmaxAudit.Mechanism.CA_TA_LITE,
                            cmaxAuditOperator(action),
                            "PARENT_SLOT_" + parent.slot + "_LINEAGE_" + lineageId(parent.solution),
                            accepted);
                }
                // V35-SOURCE-LEDGER-PATCH
                observePassiveArchive(local, decision.isTest()
                        ? V35EvaluationSourceContext.Source.CATA_TEST
                        : V35EvaluationSourceContext.Source.CATA_APPLY);
                v35CaTaLiteController.record(context, action, accepted, gain,
                        attempt.getCompleteEvaluations(), attempt.getWorkUnits(),
                        attempt.getElapsedNanos(),
                        decision.isTest());
                if (decision.isTest()) caTaTestCalls++; else caTaApplyCalls++;
                zhangBoCaTaEvents.add("v35Lite:action=" + action
                        + ",accepted=" + accepted + ",fe=" + fullEvaluationCount
                        + ",qGain=" + gain + ",attemptSeed=" + attemptSeed);
                if (accepted) {
                    local.setAttribute(ZhangBoSubSwarm.class, parent.group);
                    ZhangBoPreEvaluatedTag.mark(local, new ZhangBoPreEvaluatedTag(
                            ZhangBoPreEvaluatedTag.Source.INTRA_FACTORY_VNS, parent.slot,
                            lineageId(parent.solution), fullEvaluationCount));
                    retainFinalLocalCandidate(new PendingCaTaLocalCandidate(local, parent.slot,
                            parent.history, decision.isTest() ? LocalCandidateOrigin.CATA_TEST
                                    : LocalCandidateOrigin.CATA_APPLY));
                }
            }
        }
    }

    private static V35SubSwarmRole v35Role(ZhangBoSubSwarm group) {
        switch (group) {
            case G1_CMAX: return V35SubSwarmRole.G1_CMAX;
            case G2_TEC: return V35SubSwarmRole.G2_TEC;
            case G3_TWC: return V35SubSwarmRole.G3_TWC;
            case G4_BALANCED: default: return V35SubSwarmRole.G4_BALANCED;
        }
    }

    /**
     * Deterministic, readable route summary for the v35Lite event log.  The
     * previous {@code previews.values().toString()} emitted JVM identity values
     * (Prepared@37f79887) that cannot be replayed across JVM runs; the route key
     * is derived purely from the macro action and bottleneck.
     */
    private static String routesText(
            java.util.Map<V35MacroNeighborhood, V35MacroCandidateGateway.Prepared> previews) {
        StringBuilder out = new StringBuilder();
        for (java.util.Map.Entry<V35MacroNeighborhood, V35MacroCandidateGateway.Prepared> entry
                : previews.entrySet()) {
            if (out.length() > 0) out.append('|');
            out.append(entry.getKey().name()).append(':').append(entry.getValue().getRoute());
        }
        return out.toString();
    }

    private static String macroListText(List<V35MacroNeighborhood> values) {
        StringBuilder out = new StringBuilder();
        if (values != null) {
            for (V35MacroNeighborhood value : values) {
                if (out.length() > 0) out.append('|');
                out.append(value.name());
            }
        }
        return out.length() == 0 ? "NONE" : out.toString();
    }

    private static V35Bottleneck v35Bottleneck(ZhangBoBottleneck value) {
        switch (value) {
            case SEQ: return V35Bottleneck.SEQ;
            case MAC: return V35Bottleneck.MAC;
            case WOR: return V35Bottleneck.WOR;
            case SET: return V35Bottleneck.SET;
            case FAT: return V35Bottleneck.FAT;
            case BAL: default: return V35Bottleneck.BAL;
        }
    }

    private static String objectiveText(PermutationSolution<Integer> solution) {
        return "[" + solution.getObjective(0) + ',' + solution.getObjective(1) + ','
                + solution.getObjective(6) + ']';
    }

    private List<CaTaParent> caTaParents(List<PermutationSolution<Integer>> values) {
        List<CaTaParent> result = new ArrayList<>();
        for (int index = 0; index < values.size(); index++) {
            PermutationSolution<Integer> solution = values.get(index);
            Object value = solution.getAttribute(ZhangBoSubSwarm.class);
            if (!(value instanceof ZhangBoSubSwarm)) continue;
            ZhangBoSubSwarm group = (ZhangBoSubSwarm) value;
            List<PermutationSolution<Integer>> history = index < pendingPddrOffspringHistories.size()
                    ? pendingPddrOffspringHistories.get(index)
                    : Collections.<PermutationSolution<Integer>>emptyList();
            result.add(new CaTaParent(solution, index, group, history));
        }
        return result;
    }

    private void runFixedNeighborhoodSearch(
            List<PermutationSolution<Integer>> globalOffspring, ZhangBoProblemContext fatigueProblem) {
        ZhangBoArchiveBounds qualityBounds = localSearchQualityBounds(globalOffspring);
        P8AblationProfile profile = globalSearchConfiguration.getP8AblationProfile();
        if (profile == null || !profile.isFixedNeighborhoodEnabled()
                && profile.getVnsMode() != P8AblationProfile.VnsMode.NEED_AWARE) {
            throw new IllegalStateException("Unsupported fixed VNS profile");
        }
        int maximumNeighborhood = profile.getVnsMode()
                == P8AblationProfile.VnsMode.O1_O9_FIXED ? 9 : 13;
        for (CaTaParent parent : caTaParents(globalOffspring)) {
            ZhangBoFatigueEvaluationResult evaluation = fatigueResult(parent.solution);
            if (evaluation == null) continue;
            int factory;
            String factoryMode;
            if (profile.isNeedSelectionEnabled()) {
                ZhangBoFactoryNeedSelector.Selection selected = zhangBoFactoryNeedSelector.select(
                        evaluation, fatigueProblem.getFatigueInstanceData().getFactories(),
                        parent.group, 1.0, zhangBoCaTaRandom);
                if (!selected.isApplicable()) continue;
                factory = selected.getFactory();
                factoryMode = selected.getReason();
            } else {
                factory = Math.floorMod(parent.slot,
                        fatigueProblem.getFatigueInstanceData().getFactories());
                factoryMode = "FIXED_SLOT_FACTORY";
            }
            long requestSeed = (((long) zhangBoCaTaRandom.nextInt(0, Integer.MAX_VALUE - 1)) << 1)
                    ^ (((long) generationNumber()) << 32) ^ parent.slot;
            ZhangBoNeighborhoodRequest request = new ZhangBoNeighborhoodRequest(parent.solution,
                    fatigueProblem.getFatigueInstanceData(), fatigueProblem.getFatigueParameters(),
                    factory, parent.group, requestSeed, ZhangBoFatigueFocus.FE);
            for (ZhangBoNeighborhoodId id : ZhangBoNeighborhoodId.values()) {
                if (id.getNumber() > maximumNeighborhood) continue;
                if (fullEvaluationCount >= maxIterations) return;
                ZhangBoNeighborhoodCandidateGateway.Attempt attempt =
                        zhangBoNeighborhoodCandidateGateway.evaluateOne(id, request,
                                new ZhangBoNeighborhoodCandidateGateway.CompleteEvaluator() {
                                    @Override public void evaluate(
                                            PermutationSolution<Integer> candidate) {
                                        V35EvaluationSourceContext.begin(
                                                V35EvaluationSourceContext.Source.INTRA_FACTORY_VNS);
                                        try {
                                            problem.evaluate(candidate);
                                        } finally {
                                            V35EvaluationSourceContext.end();
                                        }
                                    }
                                });
                if (!attempt.isApplicable()) {
                    zhangBoCaTaEvents.add("generation=" + generationNumber() + ",slot="
                            + parent.slot + ",fixedId=" + id + ",notApplicable="
                            + attempt.getReason());
                    fixedNeighborhoodEventCount++;
                    continue;
                }
                fullEvaluationCount += attempt.getCompleteEvaluations();
                caTaFullEvaluations += attempt.getCompleteEvaluations();
                PermutationSolution<Integer> local = attempt.getCandidate();
                fc52RecordEvaluated(local, fullEvaluationCount,
                        V35EvaluationSourceContext.Source.INTRA_FACTORY_VNS);
                ZhangBoFatigueEvaluationResult localEvaluation = fatigueResult(local);
                boolean recoveryGain = ZhangBoNaturalRecoveryGate.allows(
                        id, evaluation, localEvaluation);
                boolean accepted = recoveryGain && ZhangBoLocalSearchAcceptance.accepts(
                        parent.solution, local, parent.group);
                if (accepted) {
                    fc52LocalAccepted(local, fullEvaluationCount, "ACCEPTED");
                } else {
                    fc52LocalRejected(local, fullEvaluationCount,
                            recoveryGain ? "NOT_BETTER" : "NO_RECOVERY_GAIN");
                }
                double gain = ZhangBoLocalSearchAcceptance.qualityGain(parent.solution, local,
                        parent.group, qualityBounds);
                if (cmaxAudit != null) {
                    cmaxAudit.observeGenerated(fullEvaluationCount, generationNumber(), local,
                            parent.group, ZhangBoCmaxAudit.Mechanism.FIXED_VNS,
                            cmaxAuditOperator(id), accepted);
                }
                // V35-SOURCE-LEDGER-PATCH
                observePassiveArchive(local, V35EvaluationSourceContext.Source.INTRA_FACTORY_VNS);
                zhangBoCaTaEvents.add("generation=" + generationNumber() + ",slot="
                        + parent.slot + ",fixedId=" + id + ",factory=" + factory
                        + ",factoryMode=" + factoryMode + ",accepted=" + accepted
                        + ",recoveryGain=" + recoveryGain + ",qGain=" + gain
                        + ",fe=" + attempt.getCompleteEvaluations());
                fixedNeighborhoodEventCount++;
                if (accepted) {
                    local.setAttribute(ZhangBoSubSwarm.class, parent.group);
                    ZhangBoPreEvaluatedTag.mark(local, new ZhangBoPreEvaluatedTag(
                            ZhangBoPreEvaluatedTag.Source.INTRA_FACTORY_VNS, parent.slot,
                            lineageId(parent.solution), fullEvaluationCount));
                    pendingCaTaLocalCandidates.add(new PendingCaTaLocalCandidate(local,
                            parent.slot, parent.history, LocalCandidateOrigin.O1_O9));
                    globalOffspring.add(local);
                }
            }
        }
    }

    /** Executes the complete inherited local-search schedule for the formal B0/B1 backbone. */
    private void runFormalInheritedLocalSearch(
            List<PermutationSolution<Integer>> globalOffspring,
            ZhangBoProblemContext fatigueProblem) {
        if (fatigueProblem == null) {
            throw new IllegalStateException("Formal local search requires canonical problem data");
        }
        ZhangBoArchiveBounds qualityBounds = localSearchQualityBounds(globalOffspring);
        ZhangBoCriticalFactoryNeighborhoods critical =
                new ZhangBoCriticalFactoryNeighborhoods(zhangBoResourceDomain, zhangBoP6Random);
        List<CaTaParent> parents = caTaParents(globalOffspring);
        for (CaTaParent parent : parents) {
            PermutationSolution<Integer> current = parent.solution;
            long acceptedOrdinal = -1L;
            LocalCandidateOrigin acceptedOrigin = null;
            int[] factories = criticalFactories(current, parent.group,
                    fatigueProblem.getFatigueInstanceData().getFactories());

            if (fullEvaluationCount < localFeHardLimit()) {
                PermutationSolution<Integer> candidate = critical.swap(
                        current, factories[0], factories[1]);
                V35EvaluationSourceContext.begin(V35EvaluationSourceContext.Source.INTER_FACTORY_LS);
                try {
                    problem.evaluate(candidate);
                    fullEvaluationCount++;
                } finally {
                    V35EvaluationSourceContext.end();
                }
                caTaFullEvaluations++;
                formalCriticalFactorySwapEvaluations++;
                fc6RecordEvaluated(ZhangBoEvaluatedPddrSelector.Source.CRITICAL_SWAP, candidate);
                boolean accepted = ZhangBoLocalSearchAcceptance.accepts(
                        current, candidate, parent.group);
                if (accepted) {
                    fc52LocalAccepted(candidate, fullEvaluationCount, "ACCEPTED");
                    fc6RecordAccepted(ZhangBoEvaluatedPddrSelector.Source.CRITICAL_SWAP);
                } else {
                    fc52LocalRejected(candidate, fullEvaluationCount, "NOT_BETTER");
                }
                fc52RecordEvaluated(candidate, fullEvaluationCount,
                        V35EvaluationSourceContext.Source.INTER_FACTORY_LS);
                if (cmaxAudit != null) {
                    cmaxAudit.observeGenerated(fullEvaluationCount, generationNumber(), candidate,
                            parent.group, ZhangBoCmaxAudit.Mechanism.INTER_FACTORY,
                            ZhangBoCmaxAudit.Operator.INTER_FACTORY_EXCHANGE, accepted);
                }
                // V35-SOURCE-LEDGER-PATCH
                observePassiveArchive(candidate, V35EvaluationSourceContext.Source.INTER_FACTORY_LS);
                zhangBoCaTaEvents.add("formalLocal:outer=" + completedOuterGenerations
                        + ",slot=" + parent.slot + ",op=CRITICAL_FACTORY_SWAP,accepted="
                        + accepted + ",fe=" + fullEvaluationCount);
                if (accepted) {
                    fc6RecordSuperseded(acceptedOrigin);
                    current = candidate;
                    acceptedOrdinal = fullEvaluationCount;
                    acceptedOrigin = LocalCandidateOrigin.CRITICAL_SWAP;
                }
            }

            factories = criticalFactories(current, parent.group,
                    fatigueProblem.getFatigueInstanceData().getFactories());
            if (fullEvaluationCount < localFeHardLimit()) {
                PermutationSolution<Integer> candidate = critical.insert(
                        current, factories[0], factories[1]);
                V35EvaluationSourceContext.begin(V35EvaluationSourceContext.Source.INTER_FACTORY_LS);
                try {
                    problem.evaluate(candidate);
                    fullEvaluationCount++;
                } finally {
                    V35EvaluationSourceContext.end();
                }
                caTaFullEvaluations++;
                formalCriticalFactoryInsertEvaluations++;
                fc6RecordEvaluated(ZhangBoEvaluatedPddrSelector.Source.CRITICAL_INSERT, candidate);
                boolean accepted = ZhangBoLocalSearchAcceptance.accepts(
                        current, candidate, parent.group);
                if (accepted) {
                    fc52LocalAccepted(candidate, fullEvaluationCount, "ACCEPTED");
                    fc6RecordAccepted(ZhangBoEvaluatedPddrSelector.Source.CRITICAL_INSERT);
                } else {
                    fc52LocalRejected(candidate, fullEvaluationCount, "NOT_BETTER");
                }
                fc52RecordEvaluated(candidate, fullEvaluationCount,
                        V35EvaluationSourceContext.Source.INTER_FACTORY_LS);
                if (cmaxAudit != null) {
                    cmaxAudit.observeGenerated(fullEvaluationCount, generationNumber(), candidate,
                            parent.group, ZhangBoCmaxAudit.Mechanism.INTER_FACTORY,
                            ZhangBoCmaxAudit.Operator.INTER_FACTORY_INSERTION, accepted);
                }
                // V35-SOURCE-LEDGER-PATCH
                observePassiveArchive(candidate, V35EvaluationSourceContext.Source.INTER_FACTORY_LS);
                zhangBoCaTaEvents.add("formalLocal:outer=" + completedOuterGenerations
                        + ",slot=" + parent.slot + ",op=CRITICAL_FACTORY_INSERT,accepted="
                        + accepted + ",fe=" + fullEvaluationCount);
                if (accepted) {
                    fc6RecordSuperseded(acceptedOrigin);
                    current = candidate;
                    acceptedOrdinal = fullEvaluationCount;
                    acceptedOrigin = LocalCandidateOrigin.CRITICAL_INSERT;
                }
            }

            boolean budgetExhausted = false;
            for (int pass = 0; pass < formalBaselineConfiguration.getLocalSearchTimes()
                    && !budgetExhausted; pass++) {
                for (ZhangBoNeighborhoodId id : ZhangBoNeighborhoodId.values()) {
                    if (id.getNumber() > 9) continue;
                    if (fullEvaluationCount >= localFeHardLimit()) {
                        budgetExhausted = true;
                        break;
                    }
                    ZhangBoFatigueEvaluationResult evaluation = fatigueResult(current);
                    int[] currentFactories = criticalFactories(current, parent.group,
                            fatigueProblem.getFatigueInstanceData().getFactories());
                    long requestSeed = mixCaTaSeed(globalSearchConfiguration.getSeed()
                            ^ (((long) completedOuterGenerations) << 40)
                            ^ (((long) parent.slot) << 24)
                            ^ (((long) pass) << 8) ^ id.getNumber());
                    ZhangBoNeighborhoodRequest request = new ZhangBoNeighborhoodRequest(current,
                            fatigueProblem.getFatigueInstanceData(),
                            fatigueProblem.getFatigueParameters(), currentFactories[0],
                            parent.group, requestSeed, ZhangBoFatigueFocus.FE);
                    ZhangBoNeighborhoodCandidateGateway.Attempt attempt =
                            zhangBoNeighborhoodCandidateGateway.evaluateOne(id, request,
                                    new ZhangBoNeighborhoodCandidateGateway.CompleteEvaluator() {
                                        @Override public void evaluate(
                                                PermutationSolution<Integer> candidate) {
                                            V35EvaluationSourceContext.begin(
                                                    V35EvaluationSourceContext.Source.INTRA_FACTORY_VNS);
                                            try {
                                                problem.evaluate(candidate);
                                            } finally {
                                                V35EvaluationSourceContext.end();
                                            }
                                        }
                                    });
                    fixedNeighborhoodEventCount++;
                    if (!attempt.isApplicable()) {
                        zhangBoCaTaEvents.add("formalLocal:outer=" + completedOuterGenerations
                                + ",slot=" + parent.slot + ",pass=" + pass + ",op=" + id
                                + ",notApplicable=" + attempt.getReason());
                        continue;
                    }
                    fullEvaluationCount += attempt.getCompleteEvaluations();
                    caTaFullEvaluations += attempt.getCompleteEvaluations();
                    formalOriginalNeighborhoodEvaluations += attempt.getCompleteEvaluations();
                    PermutationSolution<Integer> candidate = attempt.getCandidate();
                    fc6RecordEvaluated(ZhangBoEvaluatedPddrSelector.Source.O1_O9, candidate);
                    fc52RecordEvaluated(candidate, fullEvaluationCount,
                            V35EvaluationSourceContext.Source.INTRA_FACTORY_VNS);
                    boolean recoveryGain = ZhangBoNaturalRecoveryGate.allows(
                            id, evaluation, fatigueResult(candidate));
                    boolean accepted = recoveryGain && ZhangBoLocalSearchAcceptance.accepts(
                            current, candidate, parent.group);
                    if (accepted) {
                        fc52LocalAccepted(candidate, fullEvaluationCount, "ACCEPTED");
                        fc6RecordAccepted(ZhangBoEvaluatedPddrSelector.Source.O1_O9);
                    } else {
                        fc52LocalRejected(candidate, fullEvaluationCount,
                                recoveryGain ? "NOT_BETTER" : "NO_RECOVERY_GAIN");
                    }
                    double gain = ZhangBoLocalSearchAcceptance.qualityGain(
                            current, candidate, parent.group, qualityBounds);
                    if (cmaxAudit != null) {
                        cmaxAudit.observeGenerated(fullEvaluationCount, generationNumber(), candidate,
                                parent.group, ZhangBoCmaxAudit.Mechanism.FIXED_VNS,
                                cmaxAuditOperator(id), accepted);
                    }
                    // V35-SOURCE-LEDGER-PATCH
                    observePassiveArchive(candidate, V35EvaluationSourceContext.Source.INTRA_FACTORY_VNS);
                    zhangBoCaTaEvents.add("formalLocal:outer=" + completedOuterGenerations
                            + ",slot=" + parent.slot + ",pass=" + pass + ",op=" + id
                            + ",accepted=" + accepted + ",recoveryGain=" + recoveryGain
                            + ",qGain=" + gain + ",fe=" + fullEvaluationCount);
                    if (accepted) {
                        fc6RecordSuperseded(acceptedOrigin);
                        current = candidate;
                        acceptedOrdinal = fullEvaluationCount;
                        acceptedOrigin = LocalCandidateOrigin.O1_O9;
                    }
                }
            }

            if (acceptedOrdinal >= 0L) {
                current.setAttribute(ZhangBoSubSwarm.class, parent.group);
                ZhangBoPreEvaluatedTag.mark(current, new ZhangBoPreEvaluatedTag(
                        ZhangBoPreEvaluatedTag.Source.INTRA_FACTORY_VNS, parent.slot,
                        lineageId(parent.solution), acceptedOrdinal));
                retainFinalLocalCandidate(new PendingCaTaLocalCandidate(
                        current, parent.slot, parent.history, acceptedOrigin));
            }
        }
    }

    /**
     * FC-6A.3 final-carrier contract: a parent has at most one accepted local
     * incumbent in the PDDR merge pool.  A later accepted local result replaces
     * the earlier one, which is retained only in the observation ledger as
     * {@code superseded}; no candidate is re-evaluated and no random event is
     * consumed here.
     */
    private void retainFinalLocalCandidate(PendingCaTaLocalCandidate replacement) {
        if (replacement == null) throw new IllegalArgumentException("replacement");
        java.util.Iterator<PendingCaTaLocalCandidate> values =
                pendingCaTaLocalCandidates.iterator();
        while (values.hasNext()) {
            PendingCaTaLocalCandidate prior = values.next();
            if (prior.parentSlot == replacement.parentSlot) {
                fc6RecordSuperseded(prior.origin);
                values.remove();
            }
        }
        pendingCaTaLocalCandidates.add(replacement);
    }

    private int[] criticalFactories(PermutationSolution<Integer> solution,
            ZhangBoSubSwarm group, int factories) {
        ZhangBoFatigueEvaluationResult evaluation = fatigueResult(solution);
        if (evaluation == null) {
            throw new IllegalStateException("Critical-factory search requires an evaluated schedule");
        }
        double[] values = new double[factories];
        for (ZhangBoFatigueOperationRecord operation : evaluation.getOperations()) {
            if (group == ZhangBoSubSwarm.G1_CMAX) {
                values[operation.factory] = Math.max(values[operation.factory], operation.end);
            } else if (group == ZhangBoSubSwarm.G2_TEC) {
                values[operation.factory] += operation.energy;
            } else if (group == ZhangBoSubSwarm.G3_TWC) {
                values[operation.factory] += operation.cost;
            } else {
                values[operation.factory] += operation.end;
            }
        }
        int maximum = 0;
        int minimum = 0;
        for (int factory = 1; factory < factories; factory++) {
            if (values[factory] > values[maximum]) maximum = factory;
            if (values[factory] < values[minimum]) minimum = factory;
        }
        return new int[] {maximum, minimum};
    }

    private static ZhangBoCmaxAudit.Operator cmaxAuditOperator(ZhangBoNeighborhoodId id) {
        if (id == null) return ZhangBoCmaxAudit.Operator.O1_O9;
        if (id.getNumber() <= 9) return ZhangBoCmaxAudit.Operator.O1_O9;
        if (id == ZhangBoNeighborhoodId.O10_CRITICAL_BLOCK) return ZhangBoCmaxAudit.Operator.O10;
        if (id == ZhangBoNeighborhoodId.O11_FATIGUE_WORKER_REASSIGNMENT) {
            return ZhangBoCmaxAudit.Operator.O11;
        }
        if (id == ZhangBoNeighborhoodId.O12_JOINT_MACHINE_WORKER) {
            return ZhangBoCmaxAudit.Operator.O12;
        }
        return ZhangBoCmaxAudit.Operator.O13;
    }

    private CaTaPreparedMask prepareCaTaNeighborhoods(
            ZhangBoCaTaContext context, ZhangBoNeighborhoodRequest request) {
        List<ZhangBoNeighborhoodId> result = new ArrayList<>();
        for (ZhangBoNeighborhoodId id : caTaMask(context.getBottleneck())) {
            ZhangBoNeighborhoodCandidateGateway.PreparedPreview preview =
                    zhangBoNeighborhoodCandidateGateway.prepare(id, request);
            if (preview.isApplicable()) result.add(id);
        }
        return new CaTaPreparedMask(result);
    }

    private long caTaRequestSeed(CaTaParent parent, ZhangBoCaTaContext context,
            long contextEpoch, long callOrdinal, ZhangBoNeighborhoodId id,
            int repetition) {
        long value = globalSearchConfiguration.getSeed()
                ^ ZhangBoCaTaConfiguration.DOMAIN_SEED;
        value = mixCaTaSeed(value ^ generationNumber());
        value = mixCaTaSeed(value ^ parent.slot);
        value = mixCaTaSeed(value ^ lineageId(parent.solution));
        value = mixCaTaSeed(value ^ context.toCanonicalKey().hashCode());
        value = mixCaTaSeed(value ^ contextEpoch);
        value = mixCaTaSeed(value ^ callOrdinal);
        value = mixCaTaSeed(value ^ (id == null ? 0L : id.getNumber()));
        return mixCaTaSeed(value ^ repetition);
    }

    private static long mixCaTaSeed(long value) {
        value ^= value >>> 33;
        value *= 0xff51afd7ed558ccdL;
        value ^= value >>> 33;
        value *= 0xc4ceb9fe1a85ec53L;
        return value ^ value >>> 33;
    }

    private static List<ZhangBoNeighborhoodId> caTaMask(ZhangBoBottleneck bottleneck) {
        switch (bottleneck) {
            case SEQ:
                return Arrays.asList(ZhangBoNeighborhoodId.O1_JS_INSERT,
                        ZhangBoNeighborhoodId.O2_JS_REVERSE, ZhangBoNeighborhoodId.O3_JS_SWAP,
                        ZhangBoNeighborhoodId.O10_CRITICAL_BLOCK);
            case MAC:
                return Arrays.asList(ZhangBoNeighborhoodId.O7_MA_LOAD_TRANSFER,
                        ZhangBoNeighborhoodId.O8_MA_WEAK_TO_STRONG, ZhangBoNeighborhoodId.O9_MA_SWAP,
                        ZhangBoNeighborhoodId.O10_CRITICAL_BLOCK,
                        ZhangBoNeighborhoodId.O12_JOINT_MACHINE_WORKER);
            case WOR:
                return Arrays.asList(ZhangBoNeighborhoodId.O4_WA_LOAD_TRANSFER,
                        ZhangBoNeighborhoodId.O5_WA_WEAK_TO_STRONG, ZhangBoNeighborhoodId.O6_WA_SWAP,
                        ZhangBoNeighborhoodId.O11_FATIGUE_WORKER_REASSIGNMENT,
                        ZhangBoNeighborhoodId.O12_JOINT_MACHINE_WORKER,
                        ZhangBoNeighborhoodId.O13_NATURAL_RECOVERY_WINDOW);
            case SET:
                return Arrays.asList(ZhangBoNeighborhoodId.O11_FATIGUE_WORKER_REASSIGNMENT,
                        ZhangBoNeighborhoodId.O12_JOINT_MACHINE_WORKER);
            case FAT:
                return Arrays.asList(ZhangBoNeighborhoodId.O11_FATIGUE_WORKER_REASSIGNMENT,
                        ZhangBoNeighborhoodId.O13_NATURAL_RECOVERY_WINDOW);
            case BAL:
            default:
                return Arrays.asList(ZhangBoNeighborhoodId.values());
        }
    }

    private static long lineageId(PermutationSolution<Integer> solution) {
        Object tag = solution.getAttribute(ZhangBoLineageTag.class);
        return tag instanceof ZhangBoLineageTag ? ((ZhangBoLineageTag) tag).getLineageId() : -1L;
    }

    private static ZhangBoFatigueEvaluationResult fatigueResult(PermutationSolution<Integer> solution) {
        Object value = solution.getAttribute(ZhangBoFatigueEvaluationResult.class);
        return value instanceof ZhangBoFatigueEvaluationResult
                ? (ZhangBoFatigueEvaluationResult) value : null;
    }

    private static void clearPreEvaluationMarkers(List<PermutationSolution<Integer>> values) {
        for (PermutationSolution<Integer> value : values) {
            value.setAttribute(ZhangBoPreEvaluatedTag.class, null);
        }
    }

    private static final class CaTaParent {
        final PermutationSolution<Integer> solution;
        final int slot;
        final ZhangBoSubSwarm group;
        final List<PermutationSolution<Integer>> history;

        CaTaParent(PermutationSolution<Integer> solution, int slot, ZhangBoSubSwarm group,
                List<PermutationSolution<Integer>> history) {
            this.solution = solution;
            this.slot = slot;
            this.group = group;
            this.history = ZhangBoSolutionSupport.deepCopySolutions(history);
        }
    }

    private static final class CaTaPreparedMask {
        private final List<ZhangBoNeighborhoodId> valid;

        private CaTaPreparedMask(List<ZhangBoNeighborhoodId> valid) {
            this.valid = valid;
        }
    }

    /** Immutable audited provenance; never consulted by local acceptance or PDDR score. */
    private enum LocalCandidateOrigin {
        CATA_TEST(ZhangBoEvaluatedPddrSelector.Source.CATA_TEST),
        CATA_APPLY(ZhangBoEvaluatedPddrSelector.Source.CATA_APPLY),
        CRITICAL_SWAP(ZhangBoEvaluatedPddrSelector.Source.CRITICAL_SWAP),
        CRITICAL_INSERT(ZhangBoEvaluatedPddrSelector.Source.CRITICAL_INSERT),
        O1_O9(ZhangBoEvaluatedPddrSelector.Source.O1_O9);

        private final ZhangBoEvaluatedPddrSelector.Source selectorSource;
        LocalCandidateOrigin(ZhangBoEvaluatedPddrSelector.Source selectorSource) {
            this.selectorSource = selectorSource;
        }
    }

    private static final class PendingCaTaLocalCandidate {
        final PermutationSolution<Integer> solution;
        final int parentSlot;
        final List<PermutationSolution<Integer>> history;
        final LocalCandidateOrigin origin;

        PendingCaTaLocalCandidate(PermutationSolution<Integer> solution, int parentSlot,
                List<PermutationSolution<Integer>> history, LocalCandidateOrigin origin) {
            if (origin == null) throw new IllegalArgumentException("origin");
            this.solution = ZhangBoSolutionSupport.deepCopy(solution);
            this.parentSlot = parentSlot;
            this.history = ZhangBoSolutionSupport.deepCopySolutions(history);
            this.origin = origin;
        }
    }


    protected void perturbation_Q(List<PermutationSolution<Integer>> swarm) {


//        for (int i1 = 0; i1 < localsearch; i1++) {
//            //工厂间局部搜索  用的时候打开
//            swarm = factorySearch(swarm);
//        }


//        swarmtemp1 = evaluateSwarm(swarmtemp1);

//        for (int i = 0; i < swarmSize; i++) {
//            DEswarmtempPdflag1[i] = i;
//        }
//        super.setSwarm(PDDRFFselect(swarm,swarmtemp1,DEswarmtempPdflag1));
//
//        List<PermutationSolution<Integer>> swarmFac = new ArrayList<PermutationSolution<Integer>>(swarmSize/2);
//        for(int i=0;i<swarmFac.size();i++)
//        {
//            swarmFac.add(selectFac1(swarm).get(i));
//        }
//        //swarmFac = selectFac1(swarm);
//        System.out.println(swarmFac);
//        //swarmFac = evaluateSwarm(swarmFac);
//
//        for (int i = 0; i < swarmSize/2; i++) {
//            swarm.set(i+swarmSize/2,swarmFac.get(i));
//        }

/////////////////////////////////////////////////////////////////////////////////////
        //下面是工厂内部vns
        int QN = (int) Qnums;

//        QN = 55;
        super.setSwarm(evaluateSwarm(swarm));
        updateVelocity(swarm);    //分群

        //////////////////////////////////////////////////////////////////////////////
        //super.setSwarm(evaluateSwarm(swarm));
        List<PermutationSolution<Integer>> swarmtemp = new ArrayList<PermutationSolution<Integer>>(swarm.size());

        int[] DEswarmtempPdflag = new int[swarmSize];


        int group = 1;
        action = action(numberOfFactories);
        int anum = action.size();
//        try {
//            Thread.sleep(999);
//        } catch (InterruptedException e) {
//            throw new RuntimeException(e);
//        }
//        ReplayBuffer replayBuffer = new ReplayBuffer(10000); // 设置经验回放池的最大容量
//        int batchSize = 32; // 设置每次训练的批量大小


        for (int i = 0; i < upSize; i++) {

            double[][] Q = new double[2][anum];
            double[][] R = new double[2][anum];
            for (int j = 0; j < anum; j++) {
                Q[0][j] = 0;
                Q[1][j] = 0;
            }

            for (int j = 0; j < anum; j++) {
                R[0][j] = 1;
                R[1][j] = 1;
            }
            //double r = 0.75, s = 0.85;
            Random random = authorRandom();
            int next = 0;
            int actionIndex;
            PermutationSolution<Integer> getswarm1 = null;
            double old0, old1, new0, new1;
            double[] max = new double[QN];
            List<PermutationSolution<Integer>> temp = new ArrayList<PermutationSolution<Integer>>(QN);

            for (int k = 0; k < QN; k++) {

                double p = random.nextDouble();

                double[][] currentState = Q.clone(); // 当前状态
                double currentReward = 0; // 当前奖励

                if (k == 0) {
                    actionIndex = random.nextInt(action.size());


                    getswarm1 = learn(actionIndex, R, Q, groupU1Solution.get(i), next, group);


                    int Qiannext = next;
                    if ((getswarm1.getObjective(0) < groupU1Solution.get(i).getObjective(0))) next = 0;
                    else next = 1;
                    old0 = groupU1Solution.get(i).getObjective(0);
                    new0 = getswarm1.getObjective(0);

                    currentReward = new0 - old0; // 计算奖励

                    //Q[Qiannext][actionIndex] = calculateNewQ(R, Q,actionIndex,next,Qiannext);
                    Q[Qiannext][actionIndex] = calculateNewQ1(R, Q, actionIndex, next, Qiannext, old0, new0);
                } else {
                    if (p < 1 - tl) {
                        actionIndex = random.nextInt(action.size());
                        getswarm1 = learn(actionIndex, R, Q, groupU1Solution.get(i), next, group);
                        int Qiannext = next;
                        if ((getswarm1.getObjective(0) < groupU1Solution.get(i).getObjective(0)))
                            next = 0;
                        else next = 1;
                        old0 = groupU1Solution.get(i).getObjective(0);
                        new0 = getswarm1.getObjective(0);

                        currentReward = new0 - old0; // 计算奖励

                        Q[Qiannext][actionIndex] = calculateNewQ1(R, Q, actionIndex, next, Qiannext, old0, new0);

                        //Q[Qiannext][actionIndex] = calculateNewQ(R, Q,actionIndex,next,Qiannext);
                    } else {
                        //actionIndex = getMaxQ(Q);

                        actionIndex = max(Q[next]);
                        getswarm1 = learn(actionIndex, R, Q, groupU1Solution.get(i), next, group);
                        int Qiannext = next;
                        if ((getswarm1.getObjective(0) < groupU1Solution.get(i).getObjective(0)))
                            next = 0;
                        else next = 1;
                        old0 = groupU1Solution.get(i).getObjective(0);
                        new0 = getswarm1.getObjective(0);

                        currentReward = new0 - old0; // 计算奖励

                        Q[Qiannext][actionIndex] = calculateNewQ1(R, Q, actionIndex, next, Qiannext, old0, new0);

                        // Q[Qiannext][actionIndex] = calculateNewQ(R, Q,actionIndex,next,Qiannext);
                    }

                }

//                replayBuffer.add(new Experience(currentState, actionIndex, currentReward, Q.clone(), false));
//
//                // 从经验回放池中随机抽取一批经历进行训练
//                if (replayBuffer.buffer.size() >= batchSize) {
//                    List<Experience> batch = replayBuffer.sample(batchSize);
//                    for (Experience exp : batch) {
//                        double learningRate = 0.1;
//                        double discountFactor = 0.99;
//                        double maxNextQ = Math.max(exp.nextState[0][exp.action], exp.nextState[1][exp.action]);
//                        double targetQ = exp.reward + discountFactor * maxNextQ;
//                        double currentQ = exp.state[0][exp.action];
//                        Q[0][exp.action] = currentQ + learningRate * (targetQ - currentQ);
//                    }
//                }


                max[k] = groupU1Solution.get(i).getObjective(0) - getswarm1.getObjective(0);
                temp.add(k, getswarm1);
            }

            int best = 0;
            for (int y = 1; y < QN; y++) {
                if (max[best] < max[y]) {
                    best = y;
                }
            }
            if (max[best] < max[QN - 1]) {
                swarmtemp.add(getswarm1);
            } else {
                swarmtemp.add(temp.get(best));
            }
//            groupU1Solution.set(i,swarmtemp.get(i));
            //swarmtemp.add(getswarm1);
        }


        //        System.out.println("upSize没问题");
//        try {
//            Thread.sleep(99999);
//        } catch (InterruptedException e) {
//            throw new RuntimeException(e);
//        }
//
        group = 2;
        for (int i = 0; i < centralSize; i++) {


            double[][] Q = new double[2][anum];
            double[][] R = new double[2][anum];
            for (int j = 0; j < anum; j++) {
                Q[0][j] = 0;
                Q[1][j] = 0;
            }

            for (int j = 0; j < anum; j++) {
                R[0][j] = 1;
                R[1][j] = 1;
            }
            //double r = 0.75, s = 0.85;
            Random random = authorRandom();
            int next = 1;
            int actionIndex;
            PermutationSolution<Integer> getswarm1 = null;
            double old0, old1, new0, new1, old2, new2;
            double[] max = new double[QN];
            List<PermutationSolution<Integer>> temp = new ArrayList<PermutationSolution<Integer>>(QN);
            for (int k = 0; k < QN; k++) {
                double p = random.nextDouble();
                if (k == 0) {

                    actionIndex = random.nextInt(action.size());
                    getswarm1 = learn(actionIndex, R, Q, groupC2Solution.get(i), next, group);
                    int Qiannext = next;
                    if ((getswarm1.getObjective(0) < groupC2Solution.get(i).getObjective(0) && getswarm1.getObjective(1) < groupC2Solution.get(i).getObjective(1) && getswarm1.getObjective(6) < groupC2Solution.get(i).getObjective(6)) || getswarm1.getObjective(0) < groupC2Solution.get(i).getObjective(0) || getswarm1.getObjective(1) < groupC2Solution.get(i).getObjective(1) || getswarm1.getObjective(6) < groupC2Solution.get(i).getObjective(6))
                        next = 0;
                    else next = 1;

                    old0 = groupC2Solution.get(i).getObjective(0);
                    old1 = groupC2Solution.get(i).getObjective(1);
                    old2 = groupC2Solution.get(i).getObjective(6);
                    new0 = getswarm1.getObjective(0);
                    new1 = getswarm1.getObjective(1);
                    new2 = getswarm1.getObjective(6);
                    Q[Qiannext][actionIndex] = calculateNewQ2(R, Q, actionIndex, next, Qiannext, old0, old1, old2, new0, new1, new2);

                    //Q[Qiannext][actionIndex] = calculateNewQ(R, Q,actionIndex,next,Qiannext);
                } else {

                    if (p < 1 - tl) {


                        actionIndex = random.nextInt(action.size());
                        getswarm1 = learn(actionIndex, R, Q, groupC2Solution.get(i), next, group);
                        int Qiannext = next;
                        if ((getswarm1.getObjective(0) < groupC2Solution.get(i).getObjective(0) && getswarm1.getObjective(1) < groupC2Solution.get(i).getObjective(1) && getswarm1.getObjective(6) < groupC2Solution.get(i).getObjective(6)) || getswarm1.getObjective(0) < groupC2Solution.get(i).getObjective(0) || getswarm1.getObjective(1) < groupC2Solution.get(i).getObjective(1) || getswarm1.getObjective(6) < groupC2Solution.get(i).getObjective(6))
                            next = 0;
                        else next = 1;
                        old0 = groupC2Solution.get(i).getObjective(0);
                        old1 = groupC2Solution.get(i).getObjective(1);
                        old2 = groupC2Solution.get(i).getObjective(6);
                        new0 = getswarm1.getObjective(0);
                        new1 = getswarm1.getObjective(1);
                        new2 = getswarm1.getObjective(6);
                        Q[Qiannext][actionIndex] = calculateNewQ2(R, Q, actionIndex, next, Qiannext, old0, old1, old2, new0, new1, new2);

                        // Q[Qiannext][actionIndex] = calculateNewQ(R, Q,actionIndex,next,Qiannext);
                    } else {


                        actionIndex = max(Q[next]);
                        getswarm1 = learn(actionIndex, R, Q, groupC2Solution.get(i), next, group);
                        int Qiannext = next;
                        if ((getswarm1.getObjective(0) < groupC2Solution.get(i).getObjective(0) && getswarm1.getObjective(1) < groupC2Solution.get(i).getObjective(1) && getswarm1.getObjective(6) < groupC2Solution.get(i).getObjective(6)) || getswarm1.getObjective(0) < groupC2Solution.get(i).getObjective(0) || getswarm1.getObjective(1) < groupC2Solution.get(i).getObjective(1) || getswarm1.getObjective(6) < groupC2Solution.get(i).getObjective(6))
                            next = 0;
                        else next = 1;
                        old0 = groupC2Solution.get(i).getObjective(0);
                        old1 = groupC2Solution.get(i).getObjective(1);
                        old2 = groupC2Solution.get(i).getObjective(6);
                        new0 = getswarm1.getObjective(0);
                        new1 = getswarm1.getObjective(1);
                        new2 = getswarm1.getObjective(6);
                        Q[Qiannext][actionIndex] = calculateNewQ2(R, Q, actionIndex, next, Qiannext, old0, old1, old2, new0, new1, new2);

                        // Q[Qiannext][actionIndex] = calculateNewQ(R, Q,actionIndex,next,Qiannext);
                    }
                }
                max[k] = (groupC2Solution.get(i).getObjective(0) - getswarm1.getObjective(0)) + (groupC2Solution.get(i).getObjective(1) - getswarm1.getObjective(1)) + (groupC2Solution.get(i).getObjective(6) - getswarm1.getObjective(6));
                temp.add(k, getswarm1);
            }
            int best = 0;
            for (int y = 1; y < QN; y++) {
                if (max[best] < max[y]) {
                    best = y;
                }
            }
            if (max[best] < max[QN - 1]) {
                swarmtemp.add(getswarm1);
            } else {
                swarmtemp.add(temp.get(best));
            }
            groupC2Solution.set(i, swarmtemp.get(i));
        }

//        System.out.println("centralSize没问题");

        group = 3;
        for (int i = 0; i < downSize; i++) {

            double[][] Q = new double[2][anum];
            double[][] R = new double[2][anum];
            for (int j = 0; j < anum; j++) {
                Q[0][j] = 0;
                Q[1][j] = 0;
            }

            for (int j = 0; j < anum; j++) {
                R[0][j] = 1;
                R[1][j] = 1;
            }
            //double r = 0.75, s = 0.85;
            Random random = authorRandom();
            int next = 1;
            int actionIndex;
            PermutationSolution<Integer> getswarm1 = null;
            double old0, old1, new0, new1;
            double[] max = new double[QN];
            List<PermutationSolution<Integer>> temp = new ArrayList<PermutationSolution<Integer>>(QN);
            for (int k = 0; k < QN; k++) {
                double p = random.nextDouble();
                if (k == 0) {
                    actionIndex = random.nextInt(action.size());
                    getswarm1 = learn(actionIndex, R, Q, groupD3Solution.get(i), next, group);
                    int Qiannext = next;
                    if ((getswarm1.getObjective(1) < groupD3Solution.get(i).getObjective(1)))
                        next = 0;
                    else next = 1;

                    old0 = groupD3Solution.get(i).getObjective(1);
                    new0 = getswarm1.getObjective(1);
                    Q[Qiannext][actionIndex] = calculateNewQ1(R, Q, actionIndex, next, Qiannext, old0, new0);
                    //Q[Qiannext][actionIndex] = calculateNewQ(R, Q,actionIndex,next,Qiannext);
                } else {
                    if (p < 1 - tl) {
                        actionIndex = random.nextInt(action.size());
                        getswarm1 = learn(actionIndex, R, Q, groupD3Solution.get(i), next, group);
                        int Qiannext = next;
                        if ((getswarm1.getObjective(1) < groupD3Solution.get(i).getObjective(1)))
                            next = 0;
                        else next = 1;
                        old0 = groupD3Solution.get(i).getObjective(1);
                        new0 = getswarm1.getObjective(1);
                        Q[Qiannext][actionIndex] = calculateNewQ1(R, Q, actionIndex, next, Qiannext, old0, new0);

                        //Q[Qiannext][actionIndex] = calculateNewQ(R, Q,actionIndex,next,Qiannext);
                    } else {

                        actionIndex = max(Q[next]);
                        getswarm1 = learn(actionIndex, R, Q, groupD3Solution.get(i), next, group);
                        int Qiannext = next;
                        if ((getswarm1.getObjective(1) < groupD3Solution.get(i).getObjective(1)))
                            next = 0;
                        else next = 1;
                        old0 = groupD3Solution.get(i).getObjective(1);
                        new0 = getswarm1.getObjective(1);
                        Q[Qiannext][actionIndex] = calculateNewQ1(R, Q, actionIndex, next, Qiannext, old0, new0);

                        //Q[Qiannext][actionIndex] = calculateNewQ(R, Q,actionIndex,next,Qiannext);
                    }
                }
                max[k] = groupD3Solution.get(i).getObjective(1) - getswarm1.getObjective(1);
                temp.add(k, getswarm1);
            }
            int best = 0;
            for (int y = 1; y < QN; y++) {
                if (max[best] < max[y]) {
                    best = y;
                }
            }
            if (max[best] < max[QN - 1]) {
                swarmtemp.add(getswarm1);
            } else {
                swarmtemp.add(temp.get(best));
            }
            groupD3Solution.set(i, swarmtemp.get(i));
        }

//        System.out.println("downSize没问题");
        group = 4;
        for (int i = 0; i < upNewSize; i++) {

            double[][] Q = new double[2][anum];
            double[][] R = new double[2][anum];
            for (int j = 0; j < anum; j++) {
                Q[0][j] = 0;
                Q[1][j] = 0;
            }

            for (int j = 0; j < anum; j++) {
                R[0][j] = 1;
                R[1][j] = 1;
            }
            //double r = 0.75, s = 0.85;
            Random random = authorRandom();
            int next = 1;
            int actionIndex;
            PermutationSolution<Integer> getswarm1 = null;
            double old0, old1, new0, new1;
            double[] max = new double[QN];
            List<PermutationSolution<Integer>> temp = new ArrayList<PermutationSolution<Integer>>(QN);
            for (int k = 0; k < QN; k++) {
                double p = random.nextDouble();
                if (k == 0) {
                    actionIndex = random.nextInt(action.size());
                    getswarm1 = learn(actionIndex, R, Q, groupUNewSolution.get(i), next, group);
                    int Qiannext = next;
                    if ((getswarm1.getObjective(6) < groupUNewSolution.get(i).getObjective(6)))
                        next = 0;
                    else next = 1;

                    old0 = groupUNewSolution.get(i).getObjective(6);
                    new0 = getswarm1.getObjective(6);
                    Q[Qiannext][actionIndex] = calculateNewQ1(R, Q, actionIndex, next, Qiannext, old0, new0);
                    //Q[Qiannext][actionIndex] = calculateNewQ(R, Q,actionIndex,next,Qiannext);
                } else {
                    if (p < 1 - tl) {
                        actionIndex = random.nextInt(action.size());
                        getswarm1 = learn(actionIndex, R, Q, groupUNewSolution.get(i), next, group);
                        int Qiannext = next;
                        if ((getswarm1.getObjective(6) < groupUNewSolution.get(i).getObjective(6)))
                            next = 0;
                        else next = 1;
                        old0 = groupUNewSolution.get(i).getObjective(6);
                        new0 = getswarm1.getObjective(6);
                        Q[Qiannext][actionIndex] = calculateNewQ1(R, Q, actionIndex, next, Qiannext, old0, new0);

                        //Q[Qiannext][actionIndex] = calculateNewQ(R, Q,actionIndex,next,Qiannext);
                    } else {

                        actionIndex = max(Q[next]);
                        getswarm1 = learn(actionIndex, R, Q, groupUNewSolution.get(i), next, group);
                        int Qiannext = next;
                        if ((getswarm1.getObjective(6) < groupUNewSolution.get(i).getObjective(6)))
                            next = 0;
                        else next = 1;
                        old0 = groupUNewSolution.get(i).getObjective(6);
                        new0 = getswarm1.getObjective(6);
                        Q[Qiannext][actionIndex] = calculateNewQ1(R, Q, actionIndex, next, Qiannext, old0, new0);

                        //Q[Qiannext][actionIndex] = calculateNewQ(R, Q,actionIndex,next,Qiannext);
                    }
                }
                max[k] = groupUNewSolution.get(i).getObjective(6) - getswarm1.getObjective(6);
                temp.add(k, getswarm1);
            }
            int best = 0;
            for (int y = 1; y < QN; y++) {
                if (max[best] < max[y]) {
                    best = y;
                }
            }
            if (max[best] < max[QN - 1]) {
                swarmtemp.add(getswarm1);
            } else {
                swarmtemp.add(temp.get(best));
            }
            groupUNewSolution.set(i, swarmtemp.get(i));
        }

//        System.out.println("upNewSize没问题");
//        try {
//            Thread.sleep(9999999);
//        } catch (InterruptedException e) {
//            throw new RuntimeException(e);
//        }

        mergeNew(swarmtemp);

//        swarmtemp = selectFac(swarmtemp);

        swarmtemp = evaluateSwarm(swarmtemp);
//        System.out.println(swarmtemp.size());
//        try {
//            Thread.sleep(9999);
//        } catch (InterruptedException e) {
//            throw new RuntimeException(e);
//        }

        for (int i = 0; i < swarmSize; i++) {
            DEswarmtempPdflag[i] = i;
        }


        super.setSwarm(PDDRFFselect(swarm, swarmtemp, DEswarmtempPdflag));


//////////////////////////////////////////////////////////////////////////////////////
/*        List<PermutationSolution<Integer>> swarmFac = new ArrayList<PermutationSolution<Integer>>(swarm.size());

        swarmFac=selectFac(swarm);
        swarmFac = evaluateSwarm(swarmFac);

        for (int i = 0; i < swarmSize; i++) {
            DEswarmtempPdflag[i] = i;
        }
        super.setSwarm(PDDRFFselect(swarm,swarmFac,DEswarmtempPdflag));*/
    }
    protected void perturbation_4(List<PermutationSolution<Integer>> swarm) {
        updateVelocity(swarm);
        for (int i = 0; i < upSize; i++) {
            int group = 1;
            PermutationSolution<Integer> currentsolution = groupU1Solution.get(i);
            action = action(numberOfFactories);
            Random r = authorRandom();
            int index = r.nextInt(action.size());
            List<Integer> actionselect = action.get(index);
//            System.out.println(action.size());
            PermutationSolution<Integer> newsolution;
            newsolution = V_N_Search(currentsolution, actionselect, group);
            groupU1Solution.set(i, newsolution);
//            sleep();
        }

        for (int i = 0; i < centralSize; i++) {
            int group = 2;
            PermutationSolution<Integer> currentsolution = groupC2Solution.get(i);
            action = action(numberOfFactories);
            Random r = authorRandom();
            int index = r.nextInt(action.size());
            List<Integer> actionselect = action.get(index);
//            System.out.println(action.size());
            PermutationSolution<Integer> newsolution;
            newsolution = V_N_Search(currentsolution, actionselect, group);
            groupC2Solution.set(i, newsolution);
//            sleep();
        }

        for (int i = 0; i < downSize; i++) {
            int group = 3;
            PermutationSolution<Integer> currentsolution = groupD3Solution.get(i);
            action = action(numberOfFactories);
            Random r = authorRandom();
            int index = r.nextInt(action.size());
            List<Integer> actionselect = action.get(index);
//            System.out.println(action.size());
            PermutationSolution<Integer> newsolution;
            newsolution = V_N_Search(currentsolution, actionselect, group);
            groupD3Solution.set(i, newsolution);
//            sleep();
        }

        for (int i = 0; i < upNewSize; i++) {
            int group = 4;
            PermutationSolution<Integer> currentsolution = groupUNewSolution.get(i);
            action = action(numberOfFactories);
            Random r = authorRandom();
            int index = r.nextInt(action.size());
            List<Integer> actionselect = action.get(index);
//            System.out.println(action.size());
            PermutationSolution<Integer> newsolution;
            newsolution = V_N_Search(currentsolution, actionselect, group);
            groupUNewSolution.set(i, newsolution);
//            sleep();
        }
    }

    private List<PermutationSolution<Integer>> factorySearch(List<PermutationSolution<Integer>> swarm) {
        //下面是针对工厂间的操作
        super.setSwarm(evaluateSwarm(swarm));
        updateVelocity(swarm);
        List<PermutationSolution<Integer>> swarmtemp1 = new ArrayList<PermutationSolution<Integer>>(swarm.size());
        int[] DEswarmtempPdflag1 = new int[swarmSize];
        int group1 = 1, group2 = 2, group3 = 3, group4 = 4;

        for (int i = 0; i < upSize; i++) {
            PermutationSolution<Integer> getswarm1 = null;
            getswarm1 = selectFac1(groupU1Solution.get(i), group1);
            swarmtemp1.add(getswarm1);
            groupU1Solution.set(i, getswarm1);
        }

        for (int i = 0; i < upNewSize; i++) {
            PermutationSolution<Integer> getswarm1 = null;
            getswarm1 = selectFac1(groupUNewSolution.get(i), group4);
            swarmtemp1.add(getswarm1);
            groupUNewSolution.set(i, getswarm1);
        }

        for (int i = 0; i < centralSize; i++) {
            PermutationSolution<Integer> getswarm1 = null;
            getswarm1 = selectFac1(groupC2Solution.get(i), group2);
            swarmtemp1.add(getswarm1);
            groupC2Solution.set(i, getswarm1);
        }

        for (int i = 0; i < downSize; i++) {
            PermutationSolution<Integer> getswarm1 = null;
            getswarm1 = selectFac1(groupD3Solution.get(i), group3);
            swarmtemp1.add(getswarm1);
            groupD3Solution.set(i, getswarm1);
        }
        mergeNew(swarmtemp1);
        swarm = evaluateSwarm(swarmtemp1);
        return swarm;
    }


    //二元锦标赛法（选三个，取最好）
    private void selectTS() {
        int size = swarmSize / 2;
        upGr1HisOptIndividual.clear();
        centralGr2HisOptIndividual.clear();
        downGr3HisOptIndividual.clear();
        all3GlobalOptIndividuals.clear();

        for (int i = 0; i < size / 3; i++) {
            int a1 = formalRandomInt(0, upGroup1Population.get(i + size / 3).size() - 1);
            int a2 = formalRandomInt(0, upGroup1Population.get(i + size / 3).size() - 1);
            int a3 = formalRandomInt(0, upGroup1Population.get(i + size / 3).size() - 1);
            int temp = a1;
            if (upGroup1Population.get(i + size / 3).get(a1).getObjective(0) >= upGroup1Population.get(i + size / 3).get(a2).getObjective(0) &&
                    upGroup1Population.get(i + size / 3).get(a3).getObjective(0) >= upGroup1Population.get(i + size / 3).get(a2).getObjective(0)) {
                temp = a2;
            }

            if (upGroup1Population.get(i + size / 3).get(a1).getObjective(0) >= upGroup1Population.get(i + size / 3).get(a3).getObjective(0) &&
                    upGroup1Population.get(i + size / 3).get(a2).getObjective(0) >= upGroup1Population.get(i + size / 3).get(a3).getObjective(0)) {
                temp = a3;
            }
            upGr1HisOptIndividual.add(upGroup1Population.get(i + size / 3).get(temp));

        }

        for (int i = 0; i < size - 2 * size / 3; i++) {
            int a1 = formalRandomInt(0, centralGroup2Population.get(i + size - 2 * size / 3).size() - 1);
            int a2 = formalRandomInt(0, centralGroup2Population.get(i + size - 2 * size / 3).size() - 1);
            int a3 = formalRandomInt(0, centralGroup2Population.get(i + size - 2 * size / 3).size() - 1);
            int temp = a1;
            List<Double> bb = new ArrayList<>(centralGroup2Population.get(i + size - 2 * size / 3).size());

            for (int j = 0; j < centralGroup2Population.get(i + size - 2 * size / 3).size(); j++) {
                double count1 = 0;
                double count2 = 0;
                for (int k = 0; k < centralGroup2Population.get(i + size - 2 * size / 3).size(); k++) {
                    if (j != k) {
                        if (centralGroup2Population.get(i + size - 2 * size / 3).get(j).getObjective(0) <= centralGroup2Population.get(i + size - 2 * size / 3).get(k).getObjective(0) &&
                                centralGroup2Population.get(i + size - 2 * size / 3).get(j).getObjective(1) <= centralGroup2Population.get(i + size - 2 * size / 3).get(k).getObjective(1)) {
                            count1 = count1 + 1;
                        }
                        if (centralGroup2Population.get(i + size - 2 * size / 3).get(j).getObjective(0) >= centralGroup2Population.get(i + size - 2 * size / 3).get(k).getObjective(0) &&
                                centralGroup2Population.get(i + size - 2 * size / 3).get(j).getObjective(1) >= centralGroup2Population.get(i + size - 2 * size / 3).get(k).getObjective(1)) {
                            count2 = count2 + 1;
                        }
                    }
                }
                bb.add(count2 + 1 / (count1 + 1));
            }

            if (bb.get(a1) >= bb.get(a2) && bb.get(a3) >= bb.get(a2)) {
                temp = a2;
            }
            if (bb.get(a1) >= bb.get(a3) && bb.get(a2) >= bb.get(a3)) {
                temp = a3;
            }
            centralGr2HisOptIndividual.add(centralGroup2Population.get(i + size - 2 * size / 3).get(temp));

        }

        for (int i = 0; i < size / 3; i++) {
            int a1 = formalRandomInt(0, downGroup3Population.get(i + size / 3).size() - 1);
            int a2 = formalRandomInt(0, downGroup3Population.get(i + size / 3).size() - 1);
            int a3 = formalRandomInt(0, downGroup3Population.get(i + size / 3).size() - 1);
            int temp = a1;
            if (downGroup3Population.get(i + size / 3).get(a1).getObjective(1) >= downGroup3Population.get(i + size / 3).get(a2).getObjective(1) &&
                    downGroup3Population.get(i + size / 3).get(a3).getObjective(1) >= downGroup3Population.get(i + size / 3).get(a2).getObjective(1)) {
                temp = a2;
            }
            if (downGroup3Population.get(i + size / 3).get(a1).getObjective(1) >= downGroup3Population.get(i + size / 3).get(a3).getObjective(1) &&
                    downGroup3Population.get(i + size / 3).get(a2).getObjective(1) >= downGroup3Population.get(i + size / 3).get(a3).getObjective(1)) {
                temp = a3;
            }
            downGr3HisOptIndividual.add(downGroup3Population.get(i + size / 3).get(temp));
        }

        //判断选择全局最优解
        int a1 = formalRandomInt(0, globallyOptimalIndividual.size() - 1);
        int a2 = formalRandomInt(0, globallyOptimalIndividual.size() - 1);
        int a3 = formalRandomInt(0, globallyOptimalIndividual.size() - 1);
        int temp = a1;
        if (globallyOptimalIndividual.get(a1).getObjective(0) >= globallyOptimalIndividual.get(a2).getObjective(0) &&
                globallyOptimalIndividual.get(a3).getObjective(0) >= globallyOptimalIndividual.get(a2).getObjective(0)) {
            temp = a2;
        }

        if (globallyOptimalIndividual.get(a1).getObjective(0) >= globallyOptimalIndividual.get(a3).getObjective(0) &&
                globallyOptimalIndividual.get(a2).getObjective(0) >= globallyOptimalIndividual.get(a3).getObjective(0)) {
            temp = a3;
        }
        all3GlobalOptIndividuals.add(globallyOptimalIndividual.get(temp));


        a1 = formalRandomInt(0, globallyOptimalIndividual.size() - 1);
        a2 = formalRandomInt(0, globallyOptimalIndividual.size() - 1);
        a3 = formalRandomInt(0, globallyOptimalIndividual.size() - 1);
        temp = a1;
        List<Double> cc = new ArrayList<>(globallyOptimalIndividual.size());
        for (int i = 0; i < globallyOptimalIndividual.size(); i++) {
            double count1 = 0;
            double count2 = 0;
            for (int j = 0; j < globallyOptimalIndividual.size(); j++) {
                if (i != j) {
                    if (globallyOptimalIndividual.get(i).getObjective(0) <= globallyOptimalIndividual.get(j).getObjective(0) &&
                            globallyOptimalIndividual.get(i).getObjective(1) <= globallyOptimalIndividual.get(j).getObjective(1) &&
                            globallyOptimalIndividual.get(i).getObjective(6) <= globallyOptimalIndividual.get(j).getObjective(6)) {
                        count1 = count1 + 1;
                    }
                    if (globallyOptimalIndividual.get(i).getObjective(0) >= globallyOptimalIndividual.get(j).getObjective(0) &&
                            globallyOptimalIndividual.get(i).getObjective(1) >= globallyOptimalIndividual.get(j).getObjective(1) &&
                            globallyOptimalIndividual.get(i).getObjective(6) >= globallyOptimalIndividual.get(j).getObjective(6)) {
                        count2 = count2 + 1;
                    }
                }
            }
            cc.add(count2 + 1 / (count1 + 1));
        }

        if (cc.get(a1) >= cc.get(a2) && cc.get(a3) >= cc.get(a2)) {
            temp = a2;
        }
        if (cc.get(a1) >= cc.get(a3) && cc.get(a2) >= cc.get(a3)) {
            temp = a3;
        }
        all3GlobalOptIndividuals.add(globallyOptimalIndividual.get(temp));

        a1 = formalRandomInt(0, globallyOptimalIndividual.size() - 1);
        a2 = formalRandomInt(0, globallyOptimalIndividual.size() - 1);
        a3 = formalRandomInt(0, globallyOptimalIndividual.size() - 1);
        temp = a1;
        if (globallyOptimalIndividual.get(a1).getObjective(1) >= globallyOptimalIndividual.get(a2).getObjective(1) &&
                globallyOptimalIndividual.get(a3).getObjective(1) >= globallyOptimalIndividual.get(a2).getObjective(1)) {
            temp = a2;
        }

        if (globallyOptimalIndividual.get(a1).getObjective(1) >= globallyOptimalIndividual.get(a3).getObjective(1) &&
                globallyOptimalIndividual.get(a2).getObjective(1) >= globallyOptimalIndividual.get(a3).getObjective(1)) {
            temp = a3;
        }
        all3GlobalOptIndividuals.add(globallyOptimalIndividual.get(temp));

    }

    protected PermutationSolution<Integer> selectFac1(PermutationSolution<Integer> swarmtemp, int group) {
        List<PermutationSolution<Integer>> swarmFac = new ArrayList<PermutationSolution<Integer>>(swarmSize / 2);
/*        for(int f = 0; f < swarmSize/2; f++){
            swarmFac.add(swarmtemp.get(f+swarmFac.size()));
        }*/
/*        for(int f = swarmSize/2; f < swarmSize; f++){
            swarmFac.add(swarmtemp.get(f));
        }*/
        //System.out.print(swarmFac);
        //updateVelocity1(swarmFac);    //分群
        //swarmFac.clear();

/*        for(int i=0; i<swarmFac.size()/3; i++){
            int group=1;
            PermutationSolution<Integer>  getswarm = null;
            getswarm = factorySearch(groupU1Solution.get(i), group);
            PermutationSolution<Integer>  getswarm1 = (PermutationSolution<Integer>) getswarm.copy();
            groupU1Solution.set(i,getswarm1);
            swarmFac.set(i,getswarm1);
        }
        for(int i=0; i<swarmFac.size()-2*swarmFac.size()/3; i++){
            int group=2;
            PermutationSolution<Integer>  getswarm = null;
            getswarm = factorySearch(groupC2Solution.get(i), group);
            PermutationSolution<Integer>  getswarm1 = (PermutationSolution<Integer>) getswarm.copy();
            groupC2Solution.set(i,getswarm1);
            swarmFac.set(i,getswarm1);
        }
        for(int i=0; i<swarmFac.size()/3; i++){
            int group=3;*/
        PermutationSolution<Integer> getswarm = null;
        getswarm = factorySearch(swarmtemp, group);

        //mergeNew1(swarmFac);

        return getswarm;
    }

    protected void updateVelocity1(List<PermutationSolution<Integer>> swarm1) {

        upGroup1Population.clear();
        centralGroup2Population.clear();
        downGroup3Population.clear();

        groupU1Solution.clear();
        groupC2Solution.clear();
        groupD3Solution.clear();

        List<PermutationSolution<Integer>> temp1 = new ArrayList<>(swarmSize / 2);
        List<PermutationSolution<Integer>> temp2 = new ArrayList<>(swarmSize / 2);
        List<PermutationSolution<Integer>> temp3 = new ArrayList<>(swarmSize / 2);

        ArrayList<List<PermutationSolution<Integer>>> tempPd1 = new ArrayList<List<PermutationSolution<Integer>>>(swarmSize / 2);
        ArrayList<List<PermutationSolution<Integer>>> tempPd2 = new ArrayList<List<PermutationSolution<Integer>>>(swarmSize / 2);
        ArrayList<List<PermutationSolution<Integer>>> tempPd3 = new ArrayList<List<PermutationSolution<Integer>>>(swarmSize / 2);

        for (int i = 0; i < swarmSize / 2; i++) {
            temp1.add((PermutationSolution<Integer>) swarm1.get(i).copy());
            temp2.add((PermutationSolution<Integer>) swarm1.get(i).copy());
            temp3.add((PermutationSolution<Integer>) swarm1.get(i).copy());

            ArrayList<PermutationSolution<Integer>> A = new ArrayList<>(tempSwarm.get(i + swarmSize / 2).size());
            for (int j = 0; j < tempSwarm.get(i + swarmSize / 2).size(); j++) {

                A.add((PermutationSolution<Integer>) tempSwarm.get(i + swarmSize / 2).get(j).copy());
            }

            tempPd1.add(A);
            tempPd2.add(A);
            tempPd3.add(A);
        }

        //划分sub1
        for (int i = 0; i < swarm1.size() / 3; i++) {
            int b = 0;
            for (int j = 1; j < temp1.size(); j++) {
                if (temp1.get(j).getObjective(0) < temp1.get(b).getObjective(0)) {
                    b = j;
                }
            }
            groupU1Solution.add(temp1.get(b));
            upGroup1Population.add(tempPd1.get(b));

            temp1.remove(b);
            tempPd1.remove(b);

        }

        //划分sub2
        List<Double> aa = new ArrayList<>(swarm1.size());
        for (int i = 0; i < swarm1.size(); i++) {
            double count1 = 0;
            double count2 = 0;
            for (int j = 0; j < swarm1.size(); j++) {
                if (i != j) {
                    if (temp2.get(i).getObjective(0) <= temp2.get(j).getObjective(0) &&
                            temp2.get(i).getObjective(1) <= temp2.get(j).getObjective(1)) {
                        count1 = count1 + 1;
                    }
                    if (temp2.get(i).getObjective(0) >= temp2.get(j).getObjective(0) &&
                            temp2.get(i).getObjective(1) >= temp2.get(j).getObjective(1)) {
                        count2 = count2 + 1;
                    }
                }
            }
            aa.add(count2 + 1 / (count1 + 1));
        }

        for (int i = 0; i < swarm1.size() - 2 * swarm1.size() / 3; i++) {
            int b = 0;
            for (int j = 1; j < aa.size(); j++) {
                if (aa.get(j) < aa.get(b)) {
                    b = j;
                }
            }
            groupC2Solution.add(temp2.get(b));
            centralGroup2Population.add(tempPd2.get(b));

            aa.remove(b);
            tempPd2.remove(b);
            temp2.remove(b);
        }

        for (int i = 0; i < swarm1.size() / 3; i++) {
            int b = 0;
            for (int j = 1; j < temp3.size(); j++) {
                if (temp3.get(j).getObjective(1) < temp3.get(b).getObjective(1)) {
                    b = j;
                }
            }
            groupD3Solution.add(temp3.get(b));
            downGroup3Population.add(tempPd3.get(b));

            temp3.remove(b);
            tempPd3.remove(b);
        }

        //selectTS();

    }

    protected List<PermutationSolution<Integer>> selectFac(List<PermutationSolution<Integer>> swarmtemp) {
        List<PermutationSolution<Integer>> swarmFac = new ArrayList<PermutationSolution<Integer>>(swarmSize);
        for (int f = 0; f < swarmSize / 2; f++) {
            swarmFac.add(swarmtemp.get(f + swarmFac.size()));
        }
        for (int f = swarmSize / 2; f < swarmSize; f++) {
            swarmFac.add(swarmtemp.get(f));
        }
        //System.out.print(swarmFac);
        updateVelocity(swarmFac);    //分群
        //swarmFac.clear();

        for (int i = 0; i < upSize; i++) {
            int group = 1;
            PermutationSolution<Integer> getswarm = null;
            getswarm = factorySearch(groupU1Solution.get(i), group);
            //groupU1Solution.set(i,getswarm);
            swarmFac.set(i, getswarm);
        }
        for (int i = 0; i < centralSize; i++) {
            int group = 2;
            PermutationSolution<Integer> getswarm = null;
            getswarm = factorySearch(groupC2Solution.get(i), group);
            //groupC2Solution.set(i,getswarm);
            swarmFac.set(i, getswarm);
        }
        for (int i = 0; i < downSize; i++) {
            int group = 3;
            PermutationSolution<Integer> getswarm = null;
            getswarm = factorySearch(groupD3Solution.get(i), group);
            swarmFac.set(i, getswarm);
            //groupD3Solution.set(i,getswarm);
        }
        for (int i = 0; i < upNewSize; i++) {
            int group = 4;
            PermutationSolution<Integer> getswarm = null;
            getswarm = factorySearch(groupUNewSolution.get(i), group);
            swarmFac.set(i, getswarm);
            //groupD3Solution.set(i,getswarm);
        }
        return swarmFac;
    }

    public PermutationSolution<Integer> factorySearch(PermutationSolution<Integer> bestsolution, int group) {
        int count = 0;
        int max_iterations = 10;

        PermutationSolution<Integer> currentsolution = bestsolution;
        //shaking(solution,k);
        do {
            currentsolution = vnd(currentsolution, group);
            count++;
        }
        while (count <= max_iterations);
        return currentsolution;

    }    //针对工厂间的变邻域搜索

    private PermutationSolution<Integer> vnd(PermutationSolution<Integer> solution, int group) {//solution就是main里的current_solution
        PermutationSolution<Integer> solution1 = (PermutationSolution<Integer>) solution.copy();
        PermutationSolution<Integer> current_solution = solution;
        List<PermutationSolution<Integer>> current_pop1 = new ArrayList<PermutationSolution<Integer>>(1);
        List<PermutationSolution<Integer>> pop1 = new ArrayList<PermutationSolution<Integer>>(1);

        int l = 0;
        while (l < 4) {
            pop1.clear();
            current_pop1.clear();
            current_solution = insert_otherfac(current_solution, group);
            pop1.add(solution);
            current_pop1.add(current_solution);
            current_pop1 = evaluator.evaluate(current_pop1, problem);
            pop1 = evaluator.evaluate(pop1, problem);

            if ((current_pop1.get(0).getObjective(0) < pop1.get(0).getObjective(0) && current_pop1.get(0).getObjective(1) < pop1.get(0).getObjective(1) && current_pop1.get(0).getObjective(6) < pop1.get(0).getObjective(6)) ||
                    (current_pop1.get(0).getObjective(0) < pop1.get(0).getObjective(0)) ||
                    (current_pop1.get(0).getObjective(1) < pop1.get(0).getObjective(1)) ||
                    (current_pop1.get(0).getObjective(6) < pop1.get(0).getObjective(6))) {
                //if ((current_pop1.get(0).getObjective(0) < pop1.get(0).getObjective(0) && current_pop1.get(0).getObjective(1) < pop1.get(0).getObjective(1))) {
                solution1 = current_solution;
                current_solution = insertion_fac(solution1, group);
            } else {
                current_solution = insertion_fac(current_solution, group);
            }
            //current_solution = insertion_fac(current_solution,group);
            pop1.add(solution);
            current_pop1.add(current_solution);
            current_pop1 = evaluator.evaluate(current_pop1, problem);
            pop1 = evaluator.evaluate(pop1, problem);
            if (group == 1) {
                if ((current_pop1.get(0).getObjective(0) < pop1.get(0).getObjective(0))) {
                    //if ((current_pop1.get(0).getObjective(0) < pop1.get(0).getObjective(0) && current_pop1.get(0).getObjective(1) < pop1.get(0).getObjective(1))) {
                    solution = current_solution;
                    //l=0;
                    break;
                } else {
                    l++;
                }
            }
            if (group == 2) {
                if ((current_pop1.get(0).getObjective(0) < pop1.get(0).getObjective(0) && current_pop1.get(0).getObjective(1) < pop1.get(0).getObjective(1) && current_pop1.get(0).getObjective(6) < pop1.get(0).getObjective(6)) || (current_pop1.get(0).getObjective(0) < pop1.get(0).getObjective(0)) || (current_pop1.get(0).getObjective(1) < pop1.get(0).getObjective(1)) || (current_pop1.get(0).getObjective(6) < pop1.get(0).getObjective(6))) {
                    //if ((current_pop1.get(0).getObjective(0) < pop1.get(0).getObjective(0) && current_pop1.get(0).getObjective(1) < pop1.get(0).getObjective(1))) {
                    solution = current_solution;
                    break;
                    //l=0;
                } else {
                    l++;
                }
            }
            if (group == 3) {
                if ((current_pop1.get(0).getObjective(1) < pop1.get(0).getObjective(1))) {
                    //if ((current_pop1.get(0).getObjective(0) < pop1.get(0).getObjective(0) && current_pop1.get(0).getObjective(1) < pop1.get(0).getObjective(1))) {
                    solution = current_solution;
                    break;
                    //l=0;
                } else {
                    l++;
                }
            }
            if (group == 4) {
                if ((current_pop1.get(0).getObjective(6) < pop1.get(0).getObjective(6))) {
                    //if ((current_pop1.get(0).getObjective(0) < pop1.get(0).getObjective(0) && current_pop1.get(0).getObjective(1) < pop1.get(0).getObjective(1))) {
                    solution = current_solution;
                    //l=0;
                    break;
                } else {
                    l++;
                }
            }


        }
        return solution;
    }


    public PermutationSolution<Integer> insertion_fac(PermutationSolution<Integer> solution, int group) {
        Random A = authorRandom();
        PermutationSolution<Integer> solutionNew = (PermutationSolution<Integer>) solution.copy();
        int a, i, j;
        // PermutationSolution<Integer> solutiontemp = problem.createSolution();
        //List<List<Integer>> t = new ArrayList<>();  //存工厂号下对应的工件序列的下标
        //ArrayList<List<Integer>> N = new ArrayList<>();
        int[][] N = new int[numberOfFactories][solution.getNumberOfVariablesid()];       //存相同工厂的工件下标

        List<Integer> v = new ArrayList<>();
        int[] ind = new int[numberOfFactories];     //存工厂号

        int[][] len = new int[numberOfFactories][1];
        for (int r = 0; r < numberOfFactories; r++) {
            int h = 0;
            for (int y = 0; y < solution.getNumberOfVariablesid(); y++) {
                if (solution.getVariableValueid(y) == r) {  //等于工厂号的下标
                    N[r][h] = y;     //等于工厂号的下标
                    h++;
                }
            }
            len[r][0] = h;   //工厂的工件个数  0，1，2长度
        }            //没问题
        //System.out.print(len);
//        System.out.print(N);
        int maxfac1 = (int) solution.getObjective(3);
        int[] rListmax = new int[len[maxfac1][0]];
        for (int k = 0; k < len[maxfac1][0]; k++) {
            rListmax[k] = N[maxfac1][k];      // rList里面存相应工厂号下的工件的下标
        }
        //System.out.print(rList1);
        int minfac1 = (int) solution.getObjective(2);
        int[] rListmin = new int[len[minfac1][0]];
        for (int k = 0; k < len[minfac1][0]; k++) {
            rListmin[k] = N[minfac1][k];      // rList里面存相应工厂号下的工件的下标
        }
        // System.out.print(rList1);
        int maxfac2 = (int) solution.getObjective(5);
        int[] rListmax1 = new int[len[maxfac2][0]];
        for (int k = 0; k < len[maxfac2][0]; k++) {
            rListmax1[k] = N[maxfac2][k];      // rList里面存相应工厂号下的工件的下标
        }
        int minfac2 = (int) solution.getObjective(4);
        int[] rListmin1 = new int[len[minfac2][0]];
        for (int k = 0; k < len[minfac2][0]; k++) {
            rListmin1[k] = N[minfac2][k];      // rList里面存相应工厂号下的工件的下标
        }
        //System.out.print(rList1);
//        System.out.print(solution);
//        sleep();
        if (group == 1) {
            int[] listtemp1 = rListmax;
            int[] listtemp2 = rListmin;

            int max = A.nextInt(listtemp1.length);
            int min = A.nextInt(listtemp2.length);

            int jobindexa = listtemp1[max];    //工件下标
            int jobindexb = listtemp2[min];
            int joba = solution.getVariableValue(jobindexa);
            int jobb = solution.getVariableValue(jobindexb);
            solutionNew.setVariableValue(jobindexa, jobb);
            solutionNew.setVariableValue(jobindexb, joba);
            // System.out.print(solutionNew);
        }


        if (group == 2) {
            ArrayList<ST> listV2 = new ArrayList<>();
            double m = A.nextDouble();
            if (m < Mutation_m) {
                ST q = new ST(formalRandomInt(0, solution.getNumberOfVariables() - 1));
                listV2.add(q);
                addNew4FactoryVectorByRandom(solution, listV2);
            }
        }

        if (group == 3) {
            int[] listtemp1 = rListmax1;
            int[] listtemp2 = rListmin1;

            int max = A.nextInt(listtemp1.length);
            int min = A.nextInt(listtemp2.length);

            int jobindexa = listtemp1[max];    //工件下标
            int jobindexb = listtemp2[min];
            int joba = solution.getVariableValue(jobindexa);
            int jobb = solution.getVariableValue(jobindexb);
            solutionNew.setVariableValue(jobindexa, jobb);
            solutionNew.setVariableValue(jobindexb, joba);
        }

        if (group == 4) {
            int[] listtemp1 = rListmax1;
            int[] listtemp2 = rListmin1;

            int max = A.nextInt(listtemp1.length);
            int min = A.nextInt(listtemp2.length);

            int jobindexa = listtemp1[max];    //工件下标
            int jobindexb = listtemp2[min];
            int joba = solution.getVariableValue(jobindexa);
            int jobb = solution.getVariableValue(jobindexb);
            solutionNew.setVariableValue(jobindexa, jobb);
            solutionNew.setVariableValue(jobindexb, joba);
        }


        return solutionNew;
    }

    public PermutationSolution<Integer> insert_otherfac(PermutationSolution<Integer> solution, int group) {
        Random A = authorRandom();
        PermutationSolution<Integer> solutionNew = (PermutationSolution<Integer>) solution.copy();
        int a, i, j;
        // PermutationSolution<Integer> solutiontemp = problem.createSolution();
        //List<List<Integer>> t = new ArrayList<>();  //存工厂号下对应的工件序列的下标
        //ArrayList<List<Integer>> N = new ArrayList<>();
        int[][] N = new int[numberOfFactories][solution.getNumberOfVariablesid()];       //存相同工厂的工件下标

        List<Integer> v = new ArrayList<>();
        int[] ind = new int[numberOfFactories];     //存工厂号

        int[][] len = new int[numberOfFactories][1];
        for (int r = 0; r < numberOfFactories; r++) {
            int h = 0;
            for (int y = 0; y < solution.getNumberOfVariablesid(); y++) {
                if (solution.getVariableValueid(y) == r) {  //等于工厂号的下标
                    N[r][h] = y;     //等于工厂号的下标
                    h++;
                }
            }
            len[r][0] = h;   //工厂的工件个数  0，1，2长度
        }            //没问题
        //System.out.print(len);
//        System.out.print(N);
        int maxfac1 = (int) solution.getObjective(3);
        int[] rListmaxa = new int[len[maxfac1][0]];
        for (int k = 0; k < len[maxfac1][0]; k++) {
            rListmaxa[k] = N[maxfac1][k];      // rList里面存相应工厂号下的工件的下标
        }
        //System.out.print(rList1);
        int minfac1 = (int) solution.getObjective(2);
        int[] rListmina = new int[len[minfac1][0]];
        for (int k = 0; k < len[minfac1][0]; k++) {
            rListmina[k] = N[minfac1][k];      // rList里面存相应工厂号下的工件的下标
        }
        // System.out.print(rList1);
        int maxfac2 = (int) solution.getObjective(5);
        int[] rListmax1 = new int[len[maxfac2][0]];
        for (int k = 0; k < len[maxfac2][0]; k++) {
            rListmax1[k] = N[maxfac2][k];      // rList里面存相应工厂号下的工件的下标
        }
        int minfac2 = (int) solution.getObjective(4);
        int[] rListmin1 = new int[len[minfac2][0]];
        for (int k = 0; k < len[minfac2][0]; k++) {
            rListmin1[k] = N[minfac2][k];      // rList里面存相应工厂号下的工件的下标
        }
        //System.out.print(rList1);
//        System.out.print(solution);
//        try {
//            Thread.sleep(9999);
//        } catch (InterruptedException e) {
//            throw new RuntimeException(e);
//        }
        if (group == 1 && len[maxfac1][0] - len[minfac1][0] > 2) {
            int[] listtemp1 = rListmaxa;
            int[] listtemp2 = rListmina;

            int max = A.nextInt(listtemp1.length);
            //int min = A.nextInt(listtemp2.length);

            int jobindexa = listtemp1[max];    //工件下标
            //int jobindexb = listtemp2[min];
            //int joba = solution.getVariableValue(jobindexa);
            //int jobb = solution.getVariableValue(jobindexb);
            solutionNew.setVariableValueid(jobindexa, minfac1);
            // System.out.print(solutionNew);
        }

        if (group == 2) {
            ArrayList<ST> listV2 = new ArrayList<>();
            double m = A.nextDouble();
            if (m < Mutation_m) {
                ST q = new ST(formalRandomInt(0, solution.getNumberOfVariables() - 1));
                listV2.add(q);
                addNew4FactoryVectorByRandom(solution, listV2);
            }
        }

        if (group == 3) {
            int[] listtemp1 = rListmax1;
            int[] listtemp2 = rListmin1;

            int max = A.nextInt(listtemp1.length);
            //int min = A.nextInt(listtemp2.length);

            int jobindexa = listtemp1[max];    //工件下标
            //int jobindexb = listtemp2[min];
            //int joba = solution.getVariableValue(jobindexa);
            //int jobb = solution.getVariableValue(jobindexb);
            //solutionNew.setVariableValue(jobindexa,jobb);
            //solutionNew.setVariableValue(jobindexb,joba);
            solutionNew.setVariableValueid(jobindexa, minfac2);
        }

        if (group == 4) {
            int[] listtemp1 = rListmax1;
            int[] listtemp2 = rListmin1;

            int max = A.nextInt(listtemp1.length);
            //int min = A.nextInt(listtemp2.length);

            int jobindexa = listtemp1[max];    //工件下标
            //int jobindexb = listtemp2[min];
            //int joba = solution.getVariableValue(jobindexa);
            //int jobb = solution.getVariableValue(jobindexb);
            //solutionNew.setVariableValue(jobindexa,jobb);
            //solutionNew.setVariableValue(jobindexb,joba);
            solutionNew.setVariableValueid(jobindexa, minfac2);
        }

        return solutionNew;
    }

    public double calculateNewQ1(double[][] R, double[][] Q, int a, int next1, int Qiannext, double old0, double new0) {
        // return (r + rew * q);
        //double reward= R[Qiannext][a];
        double reward = old0 - new0;
        Q[Qiannext][a] = reward + gamma * maxNextQ(Q[next1]);
        //Q[Qiannext][a] = (1-alpha) * Q[Qiannext][a] + alpha * (reward+ gamma * maxNextQ(Q[next1]));
        return Q[Qiannext][a];
    }

    public double calculateNewQ2(double[][] R, double[][] Q, int a, int next1, int Qiannext, double old0, double old1, double old2, double new0, double new1, double new2) {
        // return (r + rew * q);
        //double reward= R[Qiannext][a];
        double reward = (old0 - new0) + (old1 - new1) + (old2 - new2);
        Q[Qiannext][a] = reward + gamma * maxNextQ(Q[next1]);
        //Q[Qiannext][a] = (1-alpha) * Q[Qiannext][a] + alpha * (reward+ gamma * maxNextQ(Q[next1]));
        return Q[Qiannext][a];
    }

    private double maxNextQ(double[] is) {
        double max = is[0];
        for (int i = 1; i < is.length; ++i) {
            if (is[i] > max) max = is[i];
        }
        return max;
    }

    private int max(double[] is) {
        int max = 0;
        for (int i = 1; i < is.length; ++i) {
            if (is[i] > is[max]) max = i;
        }
        return max;
    }

    private void mergeNew(List<PermutationSolution<Integer>> swarm) {
        //swarm.clear();
        tempSwarm.clear();

        for (int i = 0; i < upSize; i++) {
            //swarm.add((PermutationSolution<Integer>) groupU1Solution.get(i).copy());

            ArrayList<PermutationSolution<Integer>> A = new ArrayList<>(upGroup1Population.get(i).size());

            for (int j = 0; j < upGroup1Population.get(i).size(); j++) {
                A.add((PermutationSolution<Integer>) upGroup1Population.get(i).get(j).copy());
            }

            tempSwarm.add(A);

        }


        for (int i = 0; i < centralSize; i++) {
            //swarm.add((PermutationSolution<Integer>) groupC2Solution.get(i).copy());

            ArrayList<PermutationSolution<Integer>> A = new ArrayList<>(centralGroup2Population.get(i).size());

            for (int j = 0; j < centralGroup2Population.get(i).size(); j++) {
                A.add((PermutationSolution<Integer>) centralGroup2Population.get(i).get(j).copy());
            }
            tempSwarm.add(A);
        }

        for (int i = 0; i < downSize; i++) {
            //swarm.add((PermutationSolution<Integer>) groupD3Solution.get(i).copy());
            ArrayList<PermutationSolution<Integer>> A = new ArrayList<>(downGroup3Population.get(i).size());
            for (int j = 0; j < downGroup3Population.get(i).size(); j++) {
                A.add((PermutationSolution<Integer>) downGroup3Population.get(i).get(j).copy());
            }
            tempSwarm.add(A);
        }

        for (int i = 0; i < upNewSize; i++) {
            //swarm.add((PermutationSolution<Integer>) groupU1Solution.get(i).copy());

            ArrayList<PermutationSolution<Integer>> A = new ArrayList<>(upNewGroup1Population.get(i).size());

            for (int j = 0; j < upNewGroup1Population.get(i).size(); j++) {
                A.add((PermutationSolution<Integer>) upNewGroup1Population.get(i).get(j).copy());
            }
            tempSwarm.add(A);
        }

    }

    private void mergeNew1(List<PermutationSolution<Integer>> swarm) {
        swarm.clear();
        tempSwarm.clear();

        for (int i = 0; i < swarm.size() / 3; i++) {
            swarm.add((PermutationSolution<Integer>) groupU1Solution.get(i).copy());

            ArrayList<PermutationSolution<Integer>> A = new ArrayList<>(upGroup1Population.get(i).size());

            for (int j = 0; j < upGroup1Population.get(i).size(); j++) {
                A.add((PermutationSolution<Integer>) upGroup1Population.get(i).get(j).copy());
            }
            tempSwarm.add(A);
        }


        for (int i = 0; i < swarm.size() - 2 * swarm.size() / 3; i++) {
            swarm.add((PermutationSolution<Integer>) groupC2Solution.get(i).copy());

            ArrayList<PermutationSolution<Integer>> A = new ArrayList<>(centralGroup2Population.get(i).size());

            for (int j = 0; j < centralGroup2Population.get(i).size(); j++) {
                A.add((PermutationSolution<Integer>) centralGroup2Population.get(i).get(j).copy());
            }
            tempSwarm.add(A);
        }

        for (int i = 0; i < swarm.size() / 3; i++) {
            swarm.add((PermutationSolution<Integer>) groupD3Solution.get(i).copy());
            ArrayList<PermutationSolution<Integer>> A = new ArrayList<>(downGroup3Population.get(i).size());
            for (int j = 0; j < downGroup3Population.get(i).size(); j++) {
                A.add((PermutationSolution<Integer>) downGroup3Population.get(i).get(j).copy());
            }
            tempSwarm.add(A);
        }

    }

    public PermutationSolution<Integer> learn(int a, double[][] R, double[][] Q, PermutationSolution<Integer> bestsolution, int next1, int group) {

        List<Integer> selectFac = new ArrayList<>();
        selectFac = action.get(a);
        PermutationSolution<Integer> solutiont;
        solutiont = V_N_Search(bestsolution, selectFac, group);
/*        if (solutiont.getObjective(0) == bestsolution.getObjective(0) && solutiont.getObjective(1) == bestsolution.getObjective(1)) next1 = 1;
        else next1 = 0;*/
        double reward = R[next1][a];
        double Qvalue = calculateNewQ(reward, Q[next1][a]);
        Q[next1][a] = Qvalue;


        return solutiont;
    }

    public PermutationSolution<Integer> V_N_Search(PermutationSolution<Integer> bestsolution, List<Integer> selectFac, int group) {
        int count = 0;
        int max_iterations = 3;
        List<PermutationSolution<Integer>> current_pop1 = new ArrayList<PermutationSolution<Integer>>(1);
        List<PermutationSolution<Integer>> pop1 = new ArrayList<PermutationSolution<Integer>>(1);
        PermutationSolution<Integer> currentsolution = bestsolution;
        //shaking(solution,k);
        do {
            current_pop1.clear();
            pop1.clear();

            currentsolution = variable_neighborhood_descent(currentsolution, selectFac, group);
            count++;
        }
        while (count <= max_iterations);
        return currentsolution;

    }    //变邻域搜索

    private PermutationSolution<Integer> variable_neighborhood_descent(PermutationSolution<Integer> solution, List<Integer> selectFac, int group) {//solution就是main里的current_solution
        PermutationSolution<Integer> current_solution = (PermutationSolution<Integer>) solution.copy();
        List<PermutationSolution<Integer>> current_pop1 = new ArrayList<PermutationSolution<Integer>>(1);
        List<PermutationSolution<Integer>> pop1 = new ArrayList<PermutationSolution<Integer>>(1);

        int l = 1;
        while (l <= 6) {
//            System.out.println(l);

            if (l == 1) {
                pop1.clear();
                current_pop1.clear();
                current_solution = insertion_neighborhood_new(current_solution, selectFac);
                pop1.add(solution);
                current_pop1.add(current_solution);

                current_pop1 = evaluator.evaluate(current_pop1, problem);
                pop1 = evaluator.evaluate(pop1, problem);
                if (group == 1) {
                    if ((current_pop1.get(0).getObjective(0) < pop1.get(0).getObjective(0))
                    ) {
                        //if ((current_pop1.get(0).getObjective(0) < pop1.get(0).getObjective(0) && current_pop1.get(0).getObjective(1) < pop1.get(0).getObjective(1))) {
                        solution = current_solution;
                        l++;
                    } else {
                        l++;
                    }
                }
                if (group == 2) {
                    if ((current_pop1.get(0).getObjective(0) < pop1.get(0).getObjective(0) && current_pop1.get(0).getObjective(1) < pop1.get(0).getObjective(1) && current_pop1.get(0).getObjective(6) < pop1.get(0).getObjective(6)) ||
                            (current_pop1.get(0).getObjective(1) < pop1.get(0).getObjective(1)) ||
                            (current_pop1.get(0).getObjective(0) < pop1.get(0).getObjective(0)) ||
                            (current_pop1.get(0).getObjective(6) < pop1.get(0).getObjective(6))

                    ) {
                        //if ((current_pop1.get(0).getObjective(0) < pop1.get(0).getObjective(0) && current_pop1.get(0).getObjective(1) < pop1.get(0).getObjective(1))) {
                        solution = current_solution;
                        l++;
                    } else {
                        l++;
                    }
                }
                if (group == 3) {
                    if ((current_pop1.get(0).getObjective(1) < pop1.get(0).getObjective(1))
                    ) {
                        //if ((current_pop1.get(0).getObjective(0) < pop1.get(0).getObjective(0) && current_pop1.get(0).getObjective(1) < pop1.get(0).getObjective(1))) {
                        solution = current_solution;
                        l++;
                    } else {
                        l++;
                    }
                }
                if (group == 4) {
                    if ((current_pop1.get(0).getObjective(6) < pop1.get(0).getObjective(6))
                    ) {
                        //if ((current_pop1.get(0).getObjective(0) < pop1.get(0).getObjective(0) && current_pop1.get(0).getObjective(1) < pop1.get(0).getObjective(1))) {
                        solution = current_solution;
                        l++;
                    } else {
                        l++;
                    }
                }
            }

            if (l == 2) {
                pop1.clear();
                current_pop1.clear();

                current_solution = exchange_neighborhood_new(solution, selectFac);
                pop1.add(solution);
                current_pop1.add(current_solution);
                current_pop1 = evaluator.evaluate(current_pop1, (Problem<PermutationSolution<Integer>>) problem);
                pop1 = evaluator.evaluate(pop1, (Problem<PermutationSolution<Integer>>) problem);
                if (group == 1) {
                    if ((current_pop1.get(0).getObjective(0) < pop1.get(0).getObjective(0))
                    ) {
                        //if ((current_pop1.get(0).getObjective(0) < pop1.get(0).getObjective(0) && current_pop1.get(0).getObjective(1) < pop1.get(0).getObjective(1))) {
                        solution = current_solution;
                        l++;
                    } else {
                        l++;
                    }
                }
                if (group == 2) {
                    if ((current_pop1.get(0).getObjective(0) < pop1.get(0).getObjective(0) && current_pop1.get(0).getObjective(1) < pop1.get(0).getObjective(1) && current_pop1.get(0).getObjective(6) < pop1.get(0).getObjective(6)) ||
                            (current_pop1.get(0).getObjective(1) < pop1.get(0).getObjective(1)) ||
                            (current_pop1.get(0).getObjective(0) < pop1.get(0).getObjective(0)) ||
                            (current_pop1.get(0).getObjective(6) < pop1.get(0).getObjective(6))
                    ) {
                        //if ((current_pop1.get(0).getObjective(0) < pop1.get(0).getObjective(0) && current_pop1.get(0).getObjective(1) < pop1.get(0).getObjective(1))) {
                        solution = current_solution;
                        l++;
                    } else {
                        l++;
                    }
                }
                if (group == 3) {
                    if ((current_pop1.get(0).getObjective(1) < pop1.get(0).getObjective(1))
                    ) {
                        //if ((current_pop1.get(0).getObjective(0) < pop1.get(0).getObjective(0) && current_pop1.get(0).getObjective(1) < pop1.get(0).getObjective(1))) {
                        solution = current_solution;
                        l++;
                    } else {
                        l++;
                    }
                }
                if (group == 4) {
                    if (current_pop1.get(0).getObjective(6) < pop1.get(0).getObjective(6)
                    ) {
                        //if ((current_pop1.get(0).getObjective(0) < pop1.get(0).getObjective(0) && current_pop1.get(0).getObjective(1) < pop1.get(0).getObjective(1))) {
                        solution = current_solution;
                        l++;
                    } else {
                        l++;
                    }
                }

/*               if((current_pop1.get(0).getObjective(0) < pop1.get(0).getObjective(0)&&current_pop1.get(0).getObjective(1) < pop1.get(0).getObjective(1)) || (current_pop1.get(0).getObjective(1) < pop1.get(0).getObjective(1))|| (current_pop1.get(0).getObjective(0) < pop1.get(0).getObjective(0) ) )
               {
                //if ((current_pop1.get(0).getObjective(0) < pop1.get(0).getObjective(0) && current_pop1.get(0).getObjective(1) < pop1.get(0).getObjective(1))) {
                    solution = current_solution;
                    //l=0;
                } else {
                    l++;
                }*/
            }

            if (l == 3) {
                pop1.clear();
                current_pop1.clear();

                current_solution = reverse_neighborhood_new(solution, selectFac);
                pop1.add(solution);
                current_pop1.add(current_solution);
                current_pop1 = evaluator.evaluate(current_pop1, (Problem<PermutationSolution<Integer>>) problem);
                pop1 = evaluator.evaluate(pop1, (Problem<PermutationSolution<Integer>>) problem);
                if (group == 1) {
                    if ((current_pop1.get(0).getObjective(0) < pop1.get(0).getObjective(0))
                    ) {
                        //if ((current_pop1.get(0).getObjective(0) < pop1.get(0).getObjective(0) && current_pop1.get(0).getObjective(1) < pop1.get(0).getObjective(1))) {
                        solution = current_solution;
                        l++;
                    } else {
                        l++;
                    }
                }
                if (group == 2) {
                    if ((current_pop1.get(0).getObjective(0) < pop1.get(0).getObjective(0) && current_pop1.get(0).getObjective(1) < pop1.get(0).getObjective(1) && current_pop1.get(0).getObjective(6) < pop1.get(0).getObjective(6)) ||
                            (current_pop1.get(0).getObjective(1) < pop1.get(0).getObjective(1)) ||
                            (current_pop1.get(0).getObjective(0) < pop1.get(0).getObjective(0)) ||
                            (current_pop1.get(0).getObjective(6) < pop1.get(0).getObjective(6))
                    ) {
                        //if ((current_pop1.get(0).getObjective(0) < pop1.get(0).getObjective(0) && current_pop1.get(0).getObjective(1) < pop1.get(0).getObjective(1))) {
                        solution = current_solution;
                        l++;
                    } else {
                        l++;
                    }
                }
                if (group == 3) {
                    if ((current_pop1.get(0).getObjective(1) < pop1.get(0).getObjective(1))
                    ) {
                        //if ((current_pop1.get(0).getObjective(0) < pop1.get(0).getObjective(0) && current_pop1.get(0).getObjective(1) < pop1.get(0).getObjective(1))) {
                        solution = current_solution;
                        l++;
                    } else {
                        l++;
                    }
                }
                if (group == 4) {
                    if (current_pop1.get(0).getObjective(6) < pop1.get(0).getObjective(6)
                    ) {
                        //if ((current_pop1.get(0).getObjective(0) < pop1.get(0).getObjective(0) && current_pop1.get(0).getObjective(1) < pop1.get(0).getObjective(1))) {
                        solution = current_solution;
                        l++;
                    } else {
                        l++;
                    }
                }

/*               if((current_pop1.get(0).getObjective(0) < pop1.get(0).getObjective(0)&&current_pop1.get(0).getObjective(1) < pop1.get(0).getObjective(1)) || (current_pop1.get(0).getObjective(1) < pop1.get(0).getObjective(1))|| (current_pop1.get(0).getObjective(0) < pop1.get(0).getObjective(0) ) )
               {
                //if ((current_pop1.get(0).getObjective(0) < pop1.get(0).getObjective(0) && current_pop1.get(0).getObjective(1) < pop1.get(0).getObjective(1))) {
                    solution = current_solution;
                    //l=0;
                } else {
                    l++;
                }*/
            }

            if (l == 5) {


                pop1.clear();
                current_pop1.clear();

                current_solution = worker_level_insert(solution, selectFac);
                pop1.add(solution);
                current_pop1.add(current_solution);
                current_pop1 = evaluator.evaluate(current_pop1, (Problem<PermutationSolution<Integer>>) problem);
                pop1 = evaluator.evaluate(pop1, (Problem<PermutationSolution<Integer>>) problem);
                if (group == 1) {
                    if ((current_pop1.get(0).getObjective(0) < pop1.get(0).getObjective(0))
                    ) {
                        //if ((current_pop1.get(0).getObjective(0) < pop1.get(0).getObjective(0) && current_pop1.get(0).getObjective(1) < pop1.get(0).getObjective(1))) {
                        solution = current_solution;
                        l++;
                    } else {
                        l++;
                    }
                }
                if (group == 2) {
                    if ((current_pop1.get(0).getObjective(0) < pop1.get(0).getObjective(0) && current_pop1.get(0).getObjective(1) < pop1.get(0).getObjective(1) && current_pop1.get(0).getObjective(6) < pop1.get(0).getObjective(6)) ||
                            (current_pop1.get(0).getObjective(1) < pop1.get(0).getObjective(1)) ||
                            (current_pop1.get(0).getObjective(0) < pop1.get(0).getObjective(0)) ||
                            (current_pop1.get(0).getObjective(6) < pop1.get(0).getObjective(6))
                    ) {
                        //if ((current_pop1.get(0).getObjective(0) < pop1.get(0).getObjective(0) && current_pop1.get(0).getObjective(1) < pop1.get(0).getObjective(1))) {
                        solution = current_solution;
                        l++;
                    } else {
                        l++;
                    }
                }
                if (group == 3) {
                    if ((current_pop1.get(0).getObjective(1) < pop1.get(0).getObjective(1))
                    ) {
                        //if ((current_pop1.get(0).getObjective(0) < pop1.get(0).getObjective(0) && current_pop1.get(0).getObjective(1) < pop1.get(0).getObjective(1))) {
                        solution = current_solution;
                        l++;
                    } else {
                        l++;
                    }
                }
                if (group == 4) {
                    if (current_pop1.get(0).getObjective(6) < pop1.get(0).getObjective(6)
                    ) {
                        //if ((current_pop1.get(0).getObjective(0) < pop1.get(0).getObjective(0) && current_pop1.get(0).getObjective(1) < pop1.get(0).getObjective(1))) {
                        solution = current_solution;
                        l++;
                    } else {
                        l++;
                    }
                }

/*               if((current_pop1.get(0).getObjective(0) < pop1.get(0).getObjective(0)&&current_pop1.get(0).getObjective(1) < pop1.get(0).getObjective(1)) || (current_pop1.get(0).getObjective(1) < pop1.get(0).getObjective(1))|| (current_pop1.get(0).getObjective(0) < pop1.get(0).getObjective(0) ) )
               {
                //if ((current_pop1.get(0).getObjective(0) < pop1.get(0).getObjective(0) && current_pop1.get(0).getObjective(1) < pop1.get(0).getObjective(1))) {
                    solution = current_solution;
                    //l=0;
                } else {
                    l++;
                }*/
            }

            if (l == 4) {


                pop1.clear();
                current_pop1.clear();

                current_solution = worker_load_balancing(solution, selectFac);
                pop1.add(solution);
                current_pop1.add(current_solution);
                current_pop1 = evaluator.evaluate(current_pop1, (Problem<PermutationSolution<Integer>>) problem);
                pop1 = evaluator.evaluate(pop1, (Problem<PermutationSolution<Integer>>) problem);
                if (group == 1) {
                    if ((current_pop1.get(0).getObjective(0) < pop1.get(0).getObjective(0))
                    ) {
                        //if ((current_pop1.get(0).getObjective(0) < pop1.get(0).getObjective(0) && current_pop1.get(0).getObjective(1) < pop1.get(0).getObjective(1))) {
                        solution = current_solution;
                        l++;
                    } else {
                        l++;
                    }
                }
                if (group == 2) {
                    if ((current_pop1.get(0).getObjective(0) < pop1.get(0).getObjective(0) && current_pop1.get(0).getObjective(1) < pop1.get(0).getObjective(1) && current_pop1.get(0).getObjective(6) < pop1.get(0).getObjective(6)) ||
                            (current_pop1.get(0).getObjective(1) < pop1.get(0).getObjective(1)) ||
                            (current_pop1.get(0).getObjective(0) < pop1.get(0).getObjective(0)) ||
                            (current_pop1.get(0).getObjective(6) < pop1.get(0).getObjective(6))
                    ) {
                        //if ((current_pop1.get(0).getObjective(0) < pop1.get(0).getObjective(0) && current_pop1.get(0).getObjective(1) < pop1.get(0).getObjective(1))) {
                        solution = current_solution;
                        l++;
                    } else {
                        l++;
                    }
                }
                if (group == 3) {
                    if ((current_pop1.get(0).getObjective(1) < pop1.get(0).getObjective(1))
                    ) {
                        //if ((current_pop1.get(0).getObjective(0) < pop1.get(0).getObjective(0) && current_pop1.get(0).getObjective(1) < pop1.get(0).getObjective(1))) {
                        solution = current_solution;
                        l++;
                    } else {
                        l++;
                    }
                }
                if (group == 4) {
                    if ((current_pop1.get(0).getObjective(6) < pop1.get(0).getObjective(6))
                    ) {
                        //if ((current_pop1.get(0).getObjective(0) < pop1.get(0).getObjective(0) && current_pop1.get(0).getObjective(1) < pop1.get(0).getObjective(1))) {
                        solution = current_solution;
                        l++;
                    } else {
                        l++;
                    }
                }

/*               if((current_pop1.get(0).getObjective(0) < pop1.get(0).getObjective(0)&&current_pop1.get(0).getObjective(1) < pop1.get(0).getObjective(1)) || (current_pop1.get(0).getObjective(1) < pop1.get(0).getObjective(1))|| (current_pop1.get(0).getObjective(0) < pop1.get(0).getObjective(0) ) )
               {
                //if ((current_pop1.get(0).getObjective(0) < pop1.get(0).getObjective(0) && current_pop1.get(0).getObjective(1) < pop1.get(0).getObjective(1))) {
                    solution = current_solution;
                    //l=0;
                } else {
                    l++;
                }*/
            }

            if (l == 6) {

                pop1.clear();
                current_pop1.clear();

                current_solution = worker_exchange(solution, selectFac);
                pop1.add(solution);
                current_pop1.add(current_solution);
                current_pop1 = evaluator.evaluate(current_pop1, (Problem<PermutationSolution<Integer>>) problem);
                pop1 = evaluator.evaluate(pop1, (Problem<PermutationSolution<Integer>>) problem);
                if (group == 1) {
                    if ((current_pop1.get(0).getObjective(0) < pop1.get(0).getObjective(0))
                    ) {
                        //if ((current_pop1.get(0).getObjective(0) < pop1.get(0).getObjective(0) && current_pop1.get(0).getObjective(1) < pop1.get(0).getObjective(1))) {
                        solution = current_solution;
                        l++;
                    } else {
                        l++;
                    }
                }
                if (group == 2) {
                    if ((current_pop1.get(0).getObjective(0) < pop1.get(0).getObjective(0) && current_pop1.get(0).getObjective(1) < pop1.get(0).getObjective(1) && current_pop1.get(0).getObjective(6) < pop1.get(0).getObjective(6)) ||
                            (current_pop1.get(0).getObjective(1) < pop1.get(0).getObjective(1)) ||
                            (current_pop1.get(0).getObjective(0) < pop1.get(0).getObjective(0)) ||
                            (current_pop1.get(0).getObjective(6) < pop1.get(0).getObjective(6))
                    ) {
                        //if ((current_pop1.get(0).getObjective(0) < pop1.get(0).getObjective(0) && current_pop1.get(0).getObjective(1) < pop1.get(0).getObjective(1))) {
                        solution = current_solution;
                        l++;
                    } else {
                        l++;
                    }
                }
                if (group == 3) {
                    if ((current_pop1.get(0).getObjective(1) < pop1.get(0).getObjective(1))
                    ) {
                        //if ((current_pop1.get(0).getObjective(0) < pop1.get(0).getObjective(0) && current_pop1.get(0).getObjective(1) < pop1.get(0).getObjective(1))) {
                        solution = current_solution;
                        l++;
                    } else {
                        l++;
                    }
                }
                if (group == 4) {
                    if (current_pop1.get(0).getObjective(6) < pop1.get(0).getObjective(6)
                    ) {
                        //if ((current_pop1.get(0).getObjective(0) < pop1.get(0).getObjective(0) && current_pop1.get(0).getObjective(1) < pop1.get(0).getObjective(1))) {
                        solution = current_solution;
                        l++;
                    } else {
                        l++;
                    }
                }

/*               if((current_pop1.get(0).getObjective(0) < pop1.get(0).getObjective(0)&&current_pop1.get(0).getObjective(1) < pop1.get(0).getObjective(1)) || (current_pop1.get(0).getObjective(1) < pop1.get(0).getObjective(1))|| (current_pop1.get(0).getObjective(0) < pop1.get(0).getObjective(0) ) )
               {
                //if ((current_pop1.get(0).getObjective(0) < pop1.get(0).getObjective(0) && current_pop1.get(0).getObjective(1) < pop1.get(0).getObjective(1))) {
                    solution = current_solution;
                    //l=0;
                } else {
                    l++;
                }*/
            }
        }
        return solution;
    }


    //工件序列插入
    public PermutationSolution<Integer> insertion_neighborhood(PermutationSolution<Integer> solution, List<Integer> selectFac) {
        Random A = authorRandom();
        PermutationSolution<Integer> solutionNew = problem.createSolution();
        int a, i, j;
        // PermutationSolution<Integer> solutiontemp = problem.createSolution();
        //List<List<Integer>> t = new ArrayList<>();  //存工厂号下对应的工件序列的下标
        //ArrayList<List<Integer>> N = new ArrayList<>();
        int[][] N = new int[numberOfFactories][solution.getNumberOfVariablesid()];       //存相同工厂的工件下标

        List<Integer> v = new ArrayList<>();
        int[] ind = new int[selectFac.size()];     //存工厂号
//对0，1，2工厂的工件顺序排列
        int[][] len = new int[numberOfFactories][1];
        for (int r = 0; r < numberOfFactories; r++) {
            int h = 0;
            for (int y = 0; y < solution.getNumberOfVariablesid(); y++) {
                if (solution.getVariableValueid(y) == r) {  //等于工厂号的下标
                    N[r][h] = y;     //等于工厂号的下标
                    h++;
                }
            }
            len[r][0] = h;   //工厂的工件个数  0，1，2长度
        }            //没问题

        for (int r = 0; r < selectFac.size(); r++) {
            int m;
            int n;//int hao=0;
            ind[r] = selectFac.get(r);  //几号工厂
            int c = ind[r];             //几号工厂
            int[] rList = new int[len[c][0]];
            for (int k = 0; k < len[c][0]; k++) {
                //rList.add(N[r][k]);            // rList里面存的工件的下标
                rList[k] = N[c][k];      // rList里面存的工件的下标
            }          //没问题

            int t = A.nextInt(rList.length); //m = N[r][t];           // rList里面存的工件的下标
            int g = A.nextInt(rList.length); //n = N[r][g];            //t存的是rList里内容的下标   t下标下对应的是工件本身号

            if (t == g) {
                int end = 0;
                int num = 0;
                while (end != -1) {
                    g = A.nextInt(rList.length); //n = N[r][g];
                    //n =A.nextInt(len[r].length);
                    if (t != g) {
                        end = -1;
                    }
                    num++;
                }
                if (t < g) {
                    // i = m;j = n;
                    i = t;
                    j = g;
                } else {
                    //j = m;i = n;
                    i = g;
                    j = t;
                }
            } else {
                if (t < g) {
                    //i = m;j = n;
                    i = t;
                    j = g;
                } else {
                    //j = m;i = n;
                    i = g;
                    j = t;
                }
            }
            //i，j存的是 rList 里内容(粒子下标)的下标         即N[r]里面的下标

            if (i != 0 && j != rList.length - 1) {
                int jobi = rList[i]; //N[r][i];
                int jobj = rList[j];//N[r][j];
                for (a = 0; a < i; a++) {
                    int jobIdx = rList[a];   //工件的下标
                    int temp1 = solution.getVariableValue(jobIdx);
                    solutionNew.setVariableValue(jobIdx, temp1);
                }
                int temp2 = solution.getVariableValue(jobj);
                solutionNew.setVariableValue(jobi, temp2);
                for (a = i; a < j; a++) {
                    int jobIdx = rList[a];
                    int jobIdx1 = rList[a + 1];
                    int temp3 = solution.getVariableValue(jobIdx);
                    solutionNew.setVariableValue(jobIdx1, temp3);
                }
                for (a = j + 1; a <= rList.length - 1; a++) {
                    int jobIdx = rList[a];
                    int temp4 = solution.getVariableValue(jobIdx);
                    solutionNew.setVariableValue(jobIdx, temp4);
                }
            }   //没问题
            else if (i == 0 && i != j && j != rList.length - 1) {
                int jobi = rList[i];
                int jobj = rList[j];
                int temp1 = solution.getVariableValue(jobj);
                solutionNew.setVariableValue(jobi, temp1);
                for (a = i; a < j; a++) {
                    int jobIdx = rList[a];
                    int jobIdx1 = rList[a + 1];
                    int temp3 = solution.getVariableValue(jobIdx);
                    solutionNew.setVariableValue(jobIdx1, temp3);
                }
                for (a = j + 1; a <= rList.length - 1; a++) {
                    int jobIdx = rList[a];
                    int temp4 = solution.getVariableValue(jobIdx);
                    solutionNew.setVariableValue(jobIdx, temp4);
                }

            }     //没问题
            else if (i == 0 && j == rList.length - 1) {
                int jobi = rList[i];
                int jobj = rList[j];
                int temp1 = solution.getVariableValue(jobj);
                solutionNew.setVariableValue(jobi, temp1);
                for (a = i; a < rList.length - 1; a++) {
                    int jobIdx = rList[a];
                    int jobIdx1 = rList[a + 1];
                    int temp3 = solution.getVariableValue(jobIdx);
                    solutionNew.setVariableValue(jobIdx1, temp3);
                }

            }// 没问题
            else {
                int jobi = rList[i];
                int jobj = rList[j];
                int temp = solution.getVariableValue(jobj);
                solutionNew.setVariableValue(jobi, temp);
                for (a = 0; a < i; a++) {
                    int jobIdx = rList[a];
                    int temp1 = solution.getVariableValue(jobIdx);
                    solutionNew.setVariableValue(jobIdx, temp1);
                }
                    /*int temp2 = solutiontemp.getVariableValue(j);
                    solutionNew.setVariableValue(i, temp2);*/
                for (a = i; a < rList.length - 1; a++) {
                    int jobIdx = rList[a];
                    int jobIdx1 = rList[a + 1];
                    int temp3 = solution.getVariableValue(jobIdx);
                    solutionNew.setVariableValue(jobIdx1, temp3);
                }
            }   //没问题
        }


        ///////////////////////////////////////////////////////


        int[] no = new int[numberOfFactories];
        for (int d = 0; d < numberOfFactories; d++) {
            no[d] = d;
        }                                            //        int [] no={0,1,2};

        for (int d = 0; d < numberOfFactories; d++) {
            for (int l = 0; l < selectFac.size(); l++) {
                if (selectFac.get(l) == no[d]) no[d] = -1;
            }
        }            //没问题

        for (int d = 0; d < numberOfFactories; d++) {
            if (no[d] != -1) {
                for (a = 0; a < len[d][0]; a++) {
                    int jobIdx = N[d][a];
                    int temp = solution.getVariableValue(jobIdx);
                    solutionNew.setVariableValue(jobIdx, temp);
                }
            }
        }

        for (a = 0; a < solution.getNumberOfVariablesid(); a++) {
            int temp = solution.getVariableValueid(a);
            solutionNew.setVariableValueid(a, temp);
        }

        return solutionNew;
    }


    //20241223 新的工件序列插入
    public PermutationSolution<Integer> insertion_neighborhood_new(PermutationSolution<Integer> solution, List<Integer> selectFac) {


        List<Integer> variables = solution.getVariables();//工件向量
        List<Integer> variablesid = solution.getVariablesid();//工厂向量


        for (int i = 0; i < selectFac.size(); i++) {
            int factory = selectFac.get(i);
            List<Integer> tempfactory = new ArrayList<>();
            List<Integer> tempsequence = new ArrayList<>();
            for (int i1 = 0; i1 < variablesid.size(); i1++) {
                if (variablesid.get(i1) == factory) {
                    tempfactory.add(i1);//工厂向量的部分
                    tempsequence.add(variables.get(i1));//工序向量的部分
                }
            }

            if (tempsequence == null || tempsequence.size() <= 1) {
                continue;
            }
            Random random = authorRandom();
            int chosenIndex = random.nextInt(tempsequence.size());
            Integer chosenElement = tempsequence.get(chosenIndex);
            int newIndex = 0;
            if (chosenIndex > 0) {
                newIndex = random.nextInt(chosenIndex); // 新索引在[0, chosenIndex-1]之间
            }
            tempsequence.remove(chosenIndex);
            tempsequence.add(newIndex, chosenElement);
            for (int j = 0; j < tempfactory.size(); j++) {
                solution.setVariableValue(tempfactory.get(j), tempsequence.get(j));
            }

        }

        return solution;
    }

    //20241223 新的工件序列交换
    public PermutationSolution<Integer> exchange_neighborhood_new(PermutationSolution<Integer> solution, List<Integer> selectFac) {
        List<Integer> variables = solution.getVariables();//工件向量
        List<Integer> variablesid = solution.getVariablesid();//工厂向量


        for (int i = 0; i < selectFac.size(); i++) {
            int factory = selectFac.get(i);
            List<Integer> tempfactory = new ArrayList<>();
            List<Integer> tempsequence = new ArrayList<>();
            for (int i1 = 0; i1 < variablesid.size(); i1++) {
                if (variablesid.get(i1) == factory) {
                    tempfactory.add(i1);//工厂向量的部分
                    tempsequence.add(variables.get(i1));//工序向量的部分
                }
            }

            if (tempsequence == null || tempsequence.size() <= 1) {
                continue;
            }
            Random random = authorRandom();
            int chosenIndex1 = random.nextInt(tempsequence.size());
            int chosenIndex2 = random.nextInt(tempsequence.size());
            if (chosenIndex1 == chosenIndex2) {
                if (chosenIndex2 + 1 == tempsequence.size()) {
                    chosenIndex2 = chosenIndex2 - 1;
                } else if (chosenIndex2 == 0) {
                    chosenIndex2 = chosenIndex2 + 1;
                }
            }
            int tempindex1 = tempsequence.get(chosenIndex1);
            int tempindex2 = tempsequence.get(chosenIndex2);
            tempsequence.set(chosenIndex1, tempindex2);
            tempsequence.set(chosenIndex2, tempindex1);

            for (int j = 0; j < tempfactory.size(); j++) {
                solution.setVariableValue(tempfactory.get(j), tempsequence.get(j));
            }
        }

        return solution;
    }

    //20241223 新的工件序列翻转
    public PermutationSolution<Integer> reverse_neighborhood_new(PermutationSolution<Integer> solution, List<Integer> selectFac) {
        List<Integer> variables = solution.getVariables();//工件向量
        List<Integer> variablesid = solution.getVariablesid();//工厂向量

        for (int i = 0; i < selectFac.size(); i++) {
            int factory = selectFac.get(i);
            List<Integer> tempfactory = new ArrayList<>();
            List<Integer> tempsequence = new ArrayList<>();
            for (int i1 = 0; i1 < variablesid.size(); i1++) {
                if (variablesid.get(i1) == factory) {
                    tempfactory.add(i1);//工厂向量的部分
                    tempsequence.add(variables.get(i1));//工序向量的部分
                }
            }

            if (tempsequence == null || tempsequence.size() <= 1) {
                continue;
            }
            Random random = authorRandom();
            int startIndex = random.nextInt(tempsequence.size());
            int endIndex = random.nextInt(tempsequence.size());
            if (startIndex > endIndex) {
                int temp = startIndex;
                startIndex = endIndex;
                endIndex = temp;
            }


            if (startIndex < 0 || endIndex >= tempsequence.size() || startIndex > endIndex) {
                throw new IllegalArgumentException("索引无效");
            }

            // 使用双指针法反转子列表
            while (startIndex < endIndex) {
                // 交换元素
                Integer temp = tempsequence.get(startIndex);
                tempsequence.set(startIndex, tempsequence.get(endIndex));
                tempsequence.set(endIndex, temp);

                // 移动指针
                startIndex++;
                endIndex--;
            }

            for (int j = 0; j < tempfactory.size(); j++) {
                solution.setVariableValue(tempfactory.get(j), tempsequence.get(j));
            }

        }

        return solution;
    }

    //20250304 工人负载平衡
    public PermutationSolution<Integer> worker_load_balancing(PermutationSolution<Integer> solution, List<Integer> selectFac) {
        int[][][] workerinfactory = DefaultIntegerPermutationSolution.result;
        Random r = authorRandom();
        int stage = solution.getNumberOfVariablesworker() / solution.getNumberOfVariables();
        for (int i = 0; i < selectFac.size(); i++) {

            int currentfactory = selectFac.get(i);

            List<Integer> variablesfactory = solution.getVariablesid();

            List<Integer> positions = new ArrayList<>();
            // 遍历列表
            for (int i1 = 0; i1 < variablesfactory.size(); i1++) {
                if (variablesfactory.get(i1) == currentfactory) {
                    positions.add(i1); // 将索引添加到新列表
                }
            }
            for (int i2 = 0; i2 < stage; i2++) {
                List<Integer> temp = new ArrayList<>();
                Map<Integer, Integer> workermap = new HashMap<>();
                for (int i1 = 0; i1 < positions.size(); i1++) {
                    int index = solution.getNumberOfVariables() * i2 + positions.get(i1);
                    temp.add(solution.getVariableValueworker(index));
                    workermap.put(index, solution.getVariableValueworker(index));
                }

                Map<Integer, Integer> countMap = new HashMap<>();
                for (Integer num : temp) {
                    countMap.put(num, countMap.getOrDefault(num, 0) + 1);
                }

                // 找到出现次数最多和最少的数字
                int maxCount = Integer.MIN_VALUE;
                int minCount = Integer.MAX_VALUE;
                List<Integer> maxNumbers = new ArrayList<>();
                List<Integer> minNumbers = new ArrayList<>();

                for (Map.Entry<Integer, Integer> entry : countMap.entrySet()) {
                    int num = entry.getKey();
                    int count = entry.getValue();

                    // 更新最大值
                    if (count > maxCount) {
                        maxCount = count;
                        maxNumbers.clear();
                        maxNumbers.add(num);
                    } else if (count == maxCount) {
                        maxNumbers.add(num);
                    }

                    // 更新最小值
                    if (count < minCount) {
                        minCount = count;
                        minNumbers.clear();
                        minNumbers.add(num);
                    } else if (count == minCount) {
                        minNumbers.add(num);
                    }
                }

                for (Map.Entry<Integer, Integer> entry : workermap.entrySet()) {
                    if (entry.getValue() == maxNumbers.get(0)) {
//                        workermap.put(entry.getKey(), minNumbers.get(0)); // 修改 value 为 minNumbers
                        solution.setVariableValueworker(entry.getKey(), minNumbers.get(0));
                        break; // 只修改第一个符合条件的条目
                    }
                }

            }

        }


        return solution;
    }

    //20250305 工人互换
    public PermutationSolution<Integer> worker_exchange(PermutationSolution<Integer> solution, List<Integer> selectFac) {
        //        PermutationSolution<Integer> solutionNew = problem.createSolution();
//        System.out.println(solution);
        int[][][] workerinfactory = DefaultIntegerPermutationSolution.result;
        double[][] lw = ZhangBoEDHHFSPW.lw;
//        System.out.println("每个工厂中的每个工人的加工能力");
//        for (double[] doubles : lw) {
//            System.out.println(Arrays.toString(doubles));
//        }
//        System.out.println("每个工厂中每个阶段的工人可以选择哪些");
//        for (int[][] ints : workerinfactory) {
//            for (int[] anInt : ints) {
//                System.out.print(Arrays.toString(anInt) + " ");
//            }
//            System.out.println();
//        }
        Random r = authorRandom();
        int stage = solution.getNumberOfVariablesworker() / solution.getNumberOfVariables();
        for (int i = 0; i < selectFac.size(); i++) {

            int currentfactory = selectFac.get(i);

            List<Integer> variablesfactory = solution.getVariablesid();

            List<Integer> positions = new ArrayList<>();//是该工厂包含的工件有哪些
            // 遍历列表
            for (int i1 = 0; i1 < variablesfactory.size(); i1++) {
                if (variablesfactory.get(i1) == currentfactory) {
                    positions.add(i1); // 将索引添加到新列表
                }
            }
            //遍历每个阶段
            for (int i2 = 0; i2 < stage; i2++) {
                int[] ints = workerinfactory[currentfactory][i2];
//                System.out.println("第" + currentfactory + "个工厂第" + i2 + "阶段的工人可以选择" + Arrays.toString(ints));
                if (ints.length > 1) {
                    List<Integer> temp = new ArrayList<>();
                    Map<Integer, Integer> workermap = new HashMap<>();
                    for (int i1 = 0; i1 < positions.size(); i1++) {
                        int index1 = solution.getNumberOfVariables() * i2 + positions.get(i1);
                        temp.add(solution.getVariableValueworker(index1));
                        workermap.put(index1, solution.getVariableValueworker(index1));
                    }
                    Map<Integer, List<Map.Entry<Integer, Integer>>> groupedMap = workermap.entrySet().stream()
                            .collect(Collectors.groupingBy(Map.Entry::getValue));
                    List<Integer> keys = new ArrayList<>(groupedMap.keySet());
                    if (keys.size() < 2) {
                        continue;
                    }
                    Collections.shuffle(keys, authorRandom()); // formal path uses a run-local seed
                    int firstValue = keys.get(0);
                    int secondValue = keys.get(1);

                    // 获取对应的Map
                    List<Map.Entry<Integer, Integer>> firstMap = groupedMap.get(firstValue);
                    List<Map.Entry<Integer, Integer>> secondMap = groupedMap.get(secondValue);

                    // 3. 从选中的Map中取出随机的键值对
                    Random random = authorRandom();
                    Map.Entry<Integer, Integer> firstEntry = firstMap.get(random.nextInt(firstMap.size()));
                    Map.Entry<Integer, Integer> secondEntry = secondMap.get(random.nextInt(secondMap.size()));

                    Integer key1 = firstEntry.getKey();
                    Integer key2 = secondEntry.getKey();
                    Integer value1 = firstEntry.getValue();
                    Integer value2 = secondEntry.getValue();

                    solution.setVariableValueworker(key1,value2);
                    solution.setVariableValueworker(key2,value1);

                    // 打印结果
//                    System.out.println("随机选择的第一个键值对: " + firstEntry);
//                    System.out.println("随机选择的第二个键值对: " + secondEntry);
//                    System.out.println("分组后的Map: " + groupedMap);
//                    System.out.println("第" + currentfactory + "个工厂的第" + i2 + "阶段");
//                    System.out.println(workermap);
//                    for (Map.Entry<Integer, Integer> entry : workermap.entrySet()) {
//                        if (entry.getValue() == minindex) {
////                            System.out.println("找到的key: " + entry.getKey());
//                            solution.setVariableValueworker(entry.getKey(), maxindex);
//                            break;
//                        }
//                    }

                }
            }
        }
//        System.out.println(solution);
//        sleep();
        return solution;
    }

    //20250305 根据工人能力进行插入
    public PermutationSolution<Integer> worker_level_insert(PermutationSolution<Integer> solution, List<Integer> selectFac) {
//        PermutationSolution<Integer> solutionNew = problem.createSolution();
//        System.out.println(solution);
        int[][][] workerinfactory = DefaultIntegerPermutationSolution.result;
        double[][] lw = ZhangBoEDHHFSPW.lw;
//        System.out.println("每个工厂中的每个工人的加工能力");
//        for (double[] doubles : lw) {
//            System.out.println(Arrays.toString(doubles));
//        }
//        System.out.println("每个工厂中每个阶段的工人可以选择哪些");
//        for (int[][] ints : workerinfactory) {
//            for (int[] anInt : ints) {
//                System.out.print(Arrays.toString(anInt) + " ");
//            }
//            System.out.println();
//        }
        Random r = authorRandom();
        int stage = solution.getNumberOfVariablesworker() / solution.getNumberOfVariables();
        for (int i = 0; i < selectFac.size(); i++) {

            int currentfactory = selectFac.get(i);

            List<Integer> variablesfactory = solution.getVariablesid();

            List<Integer> positions = new ArrayList<>();//是该工厂包含的工件有哪些
            // 遍历列表
            for (int i1 = 0; i1 < variablesfactory.size(); i1++) {
                if (variablesfactory.get(i1) == currentfactory) {
                    positions.add(i1); // 将索引添加到新列表
                }
            }
            //遍历每个阶段
            for (int i2 = 0; i2 < stage; i2++) {
                int[] ints = workerinfactory[currentfactory][i2];
//                System.out.println("第" + currentfactory + "个工厂第" + i2 + "阶段的工人可以选择" + Arrays.toString(ints));
                if (ints.length > 1) {
                    double max = 0;
                    double min = Integer.MAX_VALUE;
                    int maxindex = ints[0];
                    int minindex = ints[0];
                    int index = 0;
                    for (int workerindex = ints[0]; workerindex < ints[0] + ints.length; workerindex++) {
                        double workerleveltemp = lw[currentfactory][workerindex];
                        if (workerleveltemp > max) {
                            max = workerleveltemp;
                            maxindex = ints[index];
                        }
                        if (workerleveltemp < min) {
                            min = workerleveltemp;
                            minindex = ints[index];
                        }
                        index++;
                    }
                    List<Integer> temp = new ArrayList<>();
                    Map<Integer, Integer> workermap = new HashMap<>();
                    for (int i1 = 0; i1 < positions.size(); i1++) {
                        int index1 = solution.getNumberOfVariables() * i2 + positions.get(i1);
                        temp.add(solution.getVariableValueworker(index1));
                        workermap.put(index1, solution.getVariableValueworker(index1));
                    }
//                    System.out.println("第" + currentfactory + "个工厂的第" + i2 + "阶段");
//                    System.out.println(workermap);
                    for (Map.Entry<Integer, Integer> entry : workermap.entrySet()) {
                        if (entry.getValue() == minindex) {
//                            System.out.println("找到的key: " + entry.getKey());
                            solution.setVariableValueworker(entry.getKey(), maxindex);
                            break;
                        }
                    }

                }
            }
        }
//        System.out.println(solution);
//        sleep();
        return solution;
    }

    //20241205 机器序列交换
    public PermutationSolution<Integer> exchange_neighborhood_machine(PermutationSolution<Integer> solution, List<Integer> selectFac) {
//        PermutationSolution<Integer> solutionNew = problem.createSolution();
        List<Integer> variables4factory = solution.getVariablesid();
        List<Integer> variables4job = solution.getVariables();
        List<Integer> machine = (List<Integer>) solution.getAttribute("machine");
        Random random = authorRandom();

        // 遍历 selectFac 中的每个工厂
        for (int factory : selectFac) {
            // 找到该工厂对应的工件索引和机器选择
            List<Integer> jobIndices = new ArrayList<>();
            List<Integer> machinesInFactory = new ArrayList<>();
            for (int i = 0; i < variables4factory.size(); i++) {
                if (variables4factory.get(i) == factory) {
                    jobIndices.add(i);
                    machinesInFactory.add(machine.get(i));
                }
            }

            // 随机选择一个机器
            int selectedMachine = machinesInFactory.get(random.nextInt(machinesInFactory.size()));

            // 随机选择一个插入位置
            int insertPosition = random.nextInt(machinesInFactory.size());

            // 插入到随机位置
            machinesInFactory.add(insertPosition, selectedMachine);

            // 更新 machine 列表
            for (int i = 0; i < jobIndices.size(); i++) {
                machine.set(jobIndices.get(i), machinesInFactory.get(i));
            }
        }

        return solution;
    }

    //20241205 机器序列插入
    public PermutationSolution<Integer> insert_neighborhood_machine(PermutationSolution<Integer> solution, List<Integer> selectFac) {
//        PermutationSolution<Integer> solutionNew = problem.createSolution();
        List<Integer> variables4factory = solution.getVariablesid();
        List<Integer> variables4job = solution.getVariables();
        List<Integer> machine = (List<Integer>) solution.getAttribute("machine");
        Random random = authorRandom();

        // 遍历 selectFac 中的每个工厂
        for (int factory : selectFac) {
            // 找到该工厂对应的工件索引和机器选择
            List<Integer> jobIndices = new ArrayList<>();
            List<Integer> machinesInFactory = new ArrayList<>();
            for (int i = 0; i < variables4factory.size(); i++) {
                if (variables4factory.get(i) == factory) {
                    jobIndices.add(i);
                    machinesInFactory.add(machine.get(i));
                }
            }

            // 随机选择一个机器
            int i1 = random.nextInt(machinesInFactory.size());
            int selectedMachine = machinesInFactory.get(i1);
            machinesInFactory.remove(i1);
            // 随机选择一个插入位置
            int insertPosition = random.nextInt(machinesInFactory.size());

            // 插入到随机位置
            machinesInFactory.add(insertPosition, selectedMachine);

            // 更新 machine 列表
            for (int i = 0; i < jobIndices.size(); i++) {
                machine.set(jobIndices.get(i), machinesInFactory.get(i));
            }
        }

        solution.setAttribute("machine", machine);

        return solution;
    }
    //20241205 机器序列倒序


    //工件序列交换
    public PermutationSolution<Integer> exchange_neighborhood(PermutationSolution<Integer> solution, List<Integer> selectFac) {
        Random A = authorRandom();
        PermutationSolution<Integer> solutionNew = problem.createSolution();
        int a, i, j, b;
        // PermutationSolution<Integer> solutiontemp = problem.createSolution();
        //List<List<Integer>> t = new ArrayList<>();  //存工厂号下对应的工件序列的下标
        //ArrayList<List<Integer>> N = new ArrayList<>();
        int[][] N = new int[numberOfFactories][solution.getNumberOfVariablesid()];

        List<Integer> v = new ArrayList<>();
        int[] ind = new int[selectFac.size()];     //存工厂号
//对0，1，2工厂的工件顺序排列
        int[][] len = new int[numberOfFactories][1];
        for (int r = 0; r < numberOfFactories; r++) {
            int h = 0;
            for (int y = 0; y < solution.getNumberOfVariablesid(); y++) {
                if (solution.getVariableValueid(y) == r) {  //等于工厂号的下标
                    N[r][h] = y;     //等于工厂号的下标
                    h++;
                }
            }
            len[r][0] = h;   //工厂的工件个数  0，1，2长度
        }

        for (int r = 0; r < selectFac.size(); r++) {
            int m, n;
            ind[r] = selectFac.get(r);  //几号工厂

            int c = ind[r];
            int[] rList = new int[len[c][0]];
            for (int k = 0; k < len[c][0]; k++) {
                //rList.add(N[r][k]);            // rList里面存的工件的下标
                rList[k] = N[c][k];      // rList里面存的工件的下标
            }

///////////////////////////////////////没问题

            int t = A.nextInt(rList.length); //m = N[r][t];           // rList里面存的工件的下标
            int g = A.nextInt(rList.length); //n = N[r][g];            //t存的是rList里内容的下标   t下标下对应的是工件本身号

            if (t == g) {
                int end = 0;
                while (end != -1) {
                    g = A.nextInt(rList.length); //n = N[r][g];
                    //n =A.nextInt(len[r].length);
                    if (t != g) {
                        end = -1;
                    }
                }
                if (t < g) {
                    // i = m;j = n;
                    i = t;
                    j = g;
                } else {
                    //j = m;i = n;
                    i = g;
                    j = t;
                }
            } else {
                if (t < g) {
                    //i = m;j = n;
                    i = t;
                    j = g;
                } else {
                    //j = m;i = n;
                    i = g;
                    j = t;
                }
            }
            if (i != 0 && (j != rList.length - 1)) {
                for (a = 0; a < i; a++) {
                    int jobIdx = rList[a];
                    int temp1 = solution.getVariableValue(jobIdx);
                    solutionNew.setVariableValue(jobIdx, temp1);
                }
                int jobi = rList[i];
                int jobj = rList[j];
                int temp2 = solution.getVariableValue(jobj);
                solutionNew.setVariableValue(jobi, temp2);
                int temp4 = solution.getVariableValue(jobi);
                solutionNew.setVariableValue(jobj, temp4);
                for (a = i + 1; a < j; a++) {
                    int jobIdx = rList[a];
                    int temp3 = solution.getVariableValue(jobIdx);
                    solutionNew.setVariableValue(jobIdx, temp3);
                }

                for (a = j + 1; a <= rList.length - 1; a++) {
                    int jobIdx = rList[a];
                    int temp5 = solution.getVariableValue(jobIdx);
                    solutionNew.setVariableValue(jobIdx, temp5);
                }
                //System.out.println(solutionNew);
            } else if (i == 0 && j == rList.length - 1) {
                int jobi = rList[i];
                int jobj = rList[j];
                int temp1 = solution.getVariableValue(jobj);
                solutionNew.setVariableValue(jobi, temp1);
                int temp2 = solution.getVariableValue(jobi);
                solutionNew.setVariableValue(jobj, temp2);
                for (a = i + 1; a < rList.length - 1; a++) {
                    int jobIdx = rList[a];
                    int temp3 = solution.getVariableValue(jobIdx);
                    solutionNew.setVariableValue(jobIdx, temp3);
                }
                //System.out.println(solutionNew);
            } else if (i == 0 && j != (rList.length - 1)) {
                int jobi = rList[i];
                int jobj = rList[j];
                int temp1 = solution.getVariableValue(jobj);
                solutionNew.setVariableValue(jobi, temp1);
                for (a = i + 1; a < j; a++) {
                    int jobIdx = rList[a];
                    int temp3 = solution.getVariableValue(jobIdx);
                    solutionNew.setVariableValue(jobIdx, temp3);
                }
                int temp2 = solution.getVariableValue(jobi);
                solutionNew.setVariableValue(jobj, temp2);
                for (a = j + 1; a <= (rList.length - 1); a++) {
                    int jobIdx = rList[a];
                    int temp4 = solution.getVariableValue(jobIdx);
                    solutionNew.setVariableValue(jobIdx, temp4);
                }
                //System.out.println(solutionNew);
            } else {
                //if(i!=0 && j==rList.length-1){
                for (a = 0; a < i; a++) {
                    int jobIdx = rList[a];
                    int temp3 = solution.getVariableValue(jobIdx);
                    solutionNew.setVariableValue(jobIdx, temp3);
                }
                int jobi = rList[i];
                int jobj = rList[j];
                int temp1 = solution.getVariableValue(jobj);
                solutionNew.setVariableValue(jobi, temp1);

                int temp2 = solution.getVariableValue(jobi);
                solutionNew.setVariableValue(jobj, temp2);
                for (a = i + 1; a < (rList.length - 1); a++) {
                    int jobIdx = rList[a];
                    int temp4 = solution.getVariableValue(jobIdx);
                    solutionNew.setVariableValue(jobIdx, temp4);
                }
                //System.out.println(solutionNew);
            }


        }

/////////////////////////////////////////////////////////////
        int[] no = new int[numberOfFactories];
        for (int d = 0; d < numberOfFactories; d++) {
            no[d] = d;
        }
        //int [] no={0,1,2};
        for (int d = 0; d < numberOfFactories; d++) {
            for (int l = 0; l < selectFac.size(); l++) {
                if (selectFac.get(l) == no[d]) no[d] = -1;
            }
        }

        for (int d = 0; d < numberOfFactories; d++) {
            if (no[d] != -1) {
                for (a = 0; a < len[d][0]; a++) {
                    int jobIdx = N[d][a];
                    int temp = solution.getVariableValue(jobIdx);
                    solutionNew.setVariableValue(jobIdx, temp);
                }
            }
        }
        //System.out.println(solutionNew);

        for (a = 0; a < solution.getNumberOfVariablesid(); a++) {
            int temp = solution.getVariableValueid(a);
            solutionNew.setVariableValueid(a, temp);
        }

        return solutionNew;
    }

    public double calculateNewQ(double r, double q) {
        return (r + gamma * q);
    }

    public int getMaxQ(double[][] Q) {
        int maxQ = 0;
        for (int i = 0; i < Q.length; i++) {
            if (Q[0][i] > Q[0][maxQ]) maxQ = i;
        }
        return maxQ;
    }


    /**
     * @param swarm             没有局部搜索的种群
     * @param swarm2            局部搜索后的种群
     * @param DEswarmtempPdflag
     * @return
     */
    protected List<PermutationSolution<Integer>> PDDRFFselect(List<PermutationSolution<Integer>> swarm,
                                                              List<PermutationSolution<Integer>> swarm2, int[] DEswarmtempPdflag) {

        List<PermutationSolution<Integer>> swarmtemp = new ArrayList<>(swarm.size());

        int swarmSize = swarm.size() + swarm2.size();        // 分群后的原始种群 M(t) + DE后的种群M’(t)
        List<PermutationSolution<Integer>> temp2 = new ArrayList<>(swarmSize);
        ArrayList<List<PermutationSolution<Integer>>> tempPd2 = new ArrayList<List<PermutationSolution<Integer>>>(swarmSize);
        ArrayList<List<int[]>> tempPd2flag = new ArrayList<List<int[]>>();

        for (int i = 0; i < swarm.size(); i++) {
            temp2.add((PermutationSolution<Integer>) swarm.get(i).copy());

            ArrayList<PermutationSolution<Integer>> A = new ArrayList<>(tempSwarm.get(i).size());
            ArrayList<int[]> B = new ArrayList<int[]>();

            for (int j = 0; j < tempSwarm.get(i).size(); j++) {

                A.add((PermutationSolution<Integer>) tempSwarm.get(i).get(j).copy());


            }
            tempPd2.add(A);
            tempPd2flag.add(B);
        }

        for (int i = 0; i < swarm2.size(); i++) {
            temp2.add((PermutationSolution<Integer>) swarm2.get(i).copy());

            ArrayList<PermutationSolution<Integer>> A = new ArrayList<>(tempSwarm.get(DEswarmtempPdflag[i]).size());
            ArrayList<int[]> B = new ArrayList<int[]>();

            for (int j = 0; j < tempSwarm.get(DEswarmtempPdflag[i]).size(); j++) {

                A.add((PermutationSolution<Integer>) tempSwarm.get(DEswarmtempPdflag[i]).get(j).copy());
            }
            tempPd2.add(A);
            tempPd2flag.add(B);
        }


        List<Double> aa = new ArrayList<>(swarmSize);    // 原始种群个数的两倍
        for (int i = 0; i < swarmSize; i++) {
            double count1 = 0;
            double count2 = 0;
            for (int j = 0; j < swarmSize; j++) {
                if (i != j) {
                    if (temp2.get(i).getObjective(0) <= temp2.get(j).getObjective(0) &&
                            temp2.get(i).getObjective(1) <= temp2.get(j).getObjective(1)
                            && temp2.get(i).getObjective(6) <= temp2.get(j).getObjective(6)
                    ) {
                        count1 = count1 + 1;
                    }
                    if (temp2.get(i).getObjective(0) >= temp2.get(j).getObjective(0) &&
                            temp2.get(i).getObjective(1) >= temp2.get(j).getObjective(1)
                            && temp2.get(i).getObjective(6) >= temp2.get(j).getObjective(6)
                    ) {
                        count2 = count2 + 1;
                    }
                }
            }
            aa.add(count2 + 1 / (count1 + 1));
        }

        tempSwarm.clear();
        //Pdflag.clear();

        for (int i = 0; i < swarm.size(); i++) {
            int b = 0;
            for (int j = 1; j < aa.size(); j++) {
                if (aa.get(j) < aa.get(b)) {
                    b = j;
                }
            }
            swarmtemp.add(temp2.get(b));
            tempSwarm.add(tempPd2.get(b));
            //Pdflag.add(tempPd2flag.get(b));

            aa.remove(b);
            tempPd2.remove(b);
            temp2.remove(b);
            tempPd2flag.remove(b);
        }
        swarm = swarmtemp;   //新加


//        if (t%10==0){
//            System.out.println(t);
//            System.out.println("--------");
//            for (int i=0;i<swarm.size();i++){
////                System.out.print(swarm.get(i).getObjective(0)+"    "+swarm.get(i).getObjective(1));
//                System.out.println(swarm.get(i).getObjective(0));
//            }
//            for (int i=0;i<swarm.size();i++){
//                System.out.println(swarm.get(i).getObjective(1));
//            }
//            System.out.println("--------");
//        }
//        t=t+1;

//        System.out.println("swarm.size"+swarm.size());
//
//        try {
//            Thread.sleep(999999);
//        } catch (InterruptedException e) {
//            throw new RuntimeException(e);
//        }
        return swarm;   //  原始种群的个数
    }


    private ArrayList<SO> getDifferenceOfJobSequenceVectorByExchangeSequence(PermutationSolution<Integer> a, PermutationSolution<Integer> b) {
        PermutationSolution<Integer> tempb = (PermutationSolution<Integer>) b.copy();

        int index;
        // 交换子
        SO s;
        // 交换序列
        ArrayList<SO> list = new ArrayList<SO>();
        Random random = authorRandom();
        int boundflag = random.nextInt(b.getNumberOfVariables());

        for (int i = boundflag; i < b.getNumberOfVariables(); i++) {
            if (a.getVariableValue(i) != tempb.getVariableValue(i)) {

                // 在temp中找出与a[i]相同数值的下标index
                index = findSameIndexFromJobSequence(tempb, a.getVariableValue(i));
                // 在temp中交换下标i与下标index的值
                exchangeIndex4JobSequenceVectorByExchangeSequence(tempb, i, index);
                // 记住交换子
                s = new SO(i, index);
                // 保存交换子
                list.add(s);
            }
        }

        for (int i = 0; i < boundflag; i++) {
            if (a.getVariableValue(i) != tempb.getVariableValue(i)) {

                // 在temp中找出与a[i]相同数值的下标index
                index = findSameIndexFromJobSequence(tempb, a.getVariableValue(i));
                // 在temp中交换下标i与下标index的值
                exchangeIndex4JobSequenceVectorByExchangeSequence(tempb, i, index);
                // 记住交换子
                s = new SO(i, index);
                // 保存交换子
                list.add(s);
            }
        }

        return list;
    }

    private int findSameIndexFromJobSequence(PermutationSolution<Integer> a, int num) {
        int index = -1;
        for (int i = 0; i < a.getNumberOfVariables(); i++) {
            if (a.getVariableValue(i) == num) {
                index = i;
                break;
            }
        }
        return index;
    }

    private void getCrossOfFactoryVectorBySingle(PermutationSolution<Integer> a, PermutationSolution<Integer> b) {       //对工厂向量进行单点交叉
        int index;
        Random random = authorRandom();
        int boundflag = random.nextInt(b.getNumberOfVariablesid());          //  随机产生一个单点下标
        for (int i = boundflag; i < b.getNumberOfVariablesid(); i++) {
            index = i;
            cross4FactoryVectorBySingle(a, b, i, index);   //交换下标i与下标index的值
        }
    }

    private void cross4FactoryVectorBySingle(PermutationSolution<Integer> a, PermutationSolution<Integer> b, int index1, int index2) {     //交换值
        int temp1 = a.getVariableValueid(index1);    //工厂
        int temp2 = b.getVariableValueid(index2);
        int stage = activeWorkerStages(a, "factory_crossover");

        int[][][] workerinfactory = DefaultIntegerPermutationSolution.result;
//        System.out.println(a);
//        System.out.println(b);

        for (int i = 0; i < stage; i++) {
//            System.out.println(a.getNumberOfVariables());
            int workerneedexchange = a.getNumberOfVariables() * i + index1;
//            System.out.println("workerneedexchange"+workerneedexchange);
//            System.out.println("index1"+index1);
            int workertemp1 = a.getVariableValueworker(workerneedexchange);
            int workertemp2 = b.getVariableValueworker(workerneedexchange);
            a.setVariableValueworker(workerneedexchange, workertemp2);
            b.setVariableValueworker(workerneedexchange, workertemp1);
        }
//        System.out.println(stage);
//        System.out.println(a);
//        System.out.println(b);

        a.setVariableValueid(index1, temp2);
        b.setVariableValueid(index2, temp1);
//        System.out.println(a);
//        System.out.println(b);
//        sleep();

    }


    private ArrayList<SO> getDifferenceOfFactoryVectorByExchangeSequence(PermutationSolution<Integer> a, PermutationSolution<Integer> b) {
        //TODO 为什么针对工厂向量计算交换序？
        //因为对工厂向量用了DE所以就使用交换序来进行
        //  做差 得到交换序
        PermutationSolution<Integer> tempb = (PermutationSolution<Integer>) b.copy();

        int index;
        // 交换子
        SO s;
        // 交换序列
        ArrayList<SO> list = new ArrayList<SO>();
        Random random = authorRandom();
        int boundflag = random.nextInt(b.getNumberOfVariablesid());          //  随机产生交换序的第一个下标

        for (int i = boundflag; i < b.getNumberOfVariablesid(); i++) {
            if (a.getVariableValueid(i) != tempb.getVariableValueid(i)) {

                // 在temp中找出与a[i]相同数值的下标index
                index = findSameIndexFromFactoryVector(tempb, a.getVariableValueid(i));
                if (index == -1) {
                    continue;
                }
                // 在temp中交换下标i与下标index的值
                exchangeIndex4FactoryVectorByExchangeSequence(tempb, i, index);
                // 记住交换子
                s = new SO(i, index);
                // 保存交换子
                list.add(s);
            }
        }

        for (int i = 0; i < boundflag; i++) {
            if (a.getVariableValueid(i) != tempb.getVariableValueid(i)) {

                // 在temp中找出与a[i]相同数值的下标index
                index = findSameIndexFromFactoryVector(tempb, a.getVariableValueid(i));
                // 在temp中交换下标i与下标index的值
                if (index == -1) {
                    continue;
                }
                exchangeIndex4FactoryVectorByExchangeSequence(tempb, i, index);
                // 记住交换子
                s = new SO(i, index);
                // 保存交换子
                list.add(s);
            }
        }

        return list;
    }

    private int findSameIndexFromFactoryVector(PermutationSolution<Integer> a, int num) {
        int index = -1;
        for (int i = 0; i < a.getNumberOfVariablesid(); i++) {
            if (a.getVariableValueid(i) == num) {
                index = i;
                break;
            }
        }
        return index;
    }

    private void exchangeIndex4JobSequenceVectorByExchangeSequence(PermutationSolution<Integer> a, int index1, int index2) {
        int temp1 = a.getVariableValue(index1);
        int temp2 = a.getVariableValue(index2);

        a.setVariableValue(index1, temp2);
        a.setVariableValue(index2, temp1);

    }

    private void addNew4JobSequenceVectorByExchangeSequence(PermutationSolution<Integer> arr, ArrayList<SO> list) {
        SO s;

        for (int i = 0; i < list.size(); i++) {
            s = list.get(i);

            exchangeIndex4JobSequenceVectorByExchangeSequence(arr, s.getX(), s.getY());

        }

    }

    private void addNew4FactoryVectorByExchangeSequence(PermutationSolution<Integer> arr, ArrayList<SO> list) {
        SO s;
        for (int i = 0; i < list.size(); i++) {
            s = list.get(i);
            exchangeIndex4FactoryVectorByExchangeSequence(arr, s.getX(), s.getY());             //      根据下标交换值
        }
    }

    /***
     *
     * @param arr 解
     * @param list 对于哪一位的工厂进行突变
     */
    private void addNew4FactoryVectorByRandom(PermutationSolution<Integer> arr, ArrayList<ST> list) {
        ST s;
        Random random = authorRandom();
        int factorysize = ZhangBoEDHHFSPW.numberOfMachines_.length;

        for (int i = 0; i < list.size(); i++) {
            s = list.get(i);
            int r = random.nextInt(factorysize); //TODO 为什么写成了3? 难道是因为3个工厂吗？
            exchangeIndex4FactoryVectorByRandom(arr, s.getX(), r);             //   随机选一位数进行改变
        }
    }

    /***
     *
     * @param a 解
     * @param index1 变异的位置
     * @param value 变成哪个
     */
    private void exchangeIndex4FactoryVectorByRandom(PermutationSolution<Integer> a, int index1, int value) {     //交换值
        //int temp1 =  a.getVariableValueid(index1);
//        System.out.println(a);
//        System.out.println("变异位置"+index1);
        a.setVariableValueid(index1, value);
        int[][][] workerinfactory = DefaultIntegerPermutationSolution.result;
        int stage = activeWorkerStages(a, "factory_mutation");
        for (int i = 0; i < stage; i++) {
            int i1 = a.getNumberOfVariables() * i + index1;
            Random r = authorRandom();
            int worker = r.nextInt(workerinfactory[value][i].length);
            int workerchoose = workerinfactory[value][i][worker];
            a.setVariableValueworker(i1, workerchoose);
        }
//        System.out.println(a);


    }


    private void exchangeIndex4FactoryVectorByExchangeSequence(PermutationSolution<Integer> a, int index1, int index2) {     //交换值
        int temp1 = a.getVariableValueid(index1);    //工厂
        int temp2 = a.getVariableValueid(index2);
//        System.out.println("index1="+index1);
//        System.out.println("index2="+index2);
        a.setVariableValueid(index1, temp2);
        a.setVariableValueid(index2, temp1);
    }

    private static void sleep() {
        try {
            Thread.sleep(99999);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * P8 compatibility bridge for the author class' many short-lived Random instances.
     * The default path still constructs an ordinary uncontrolled Random exactly at each
     * historical call site; only an explicit P8_REPLAYABLE profile derives stable seeds.
     */
    private Random authorRandom() {
        if (!globalSearchConfiguration.isReplayableAuthorRandomEnabled()
                && !formalBaselineConfiguration.isEnabled()) {
            return new Random();
        }
        long ordinal = authorRandomInvocation++;
        long value = globalSearchConfiguration.getSeed()
                ^ 0x415554484F52524CL ^ (ordinal * 0x9E3779B97F4A7C15L);
        value ^= value >>> 33;
        value *= 0xff51afd7ed558ccdL;
        value ^= value >>> 33;
        value *= 0xc4ceb9fe1a85ec53L;
        value ^= value >>> 33;
        return new Random(value);
    }


    @Override  //更新个体历史最优
    protected void updateLeaders(List<PermutationSolution<Integer>> swarm) {
        if (globalSearchConfiguration.isQgEnabled() && !caTaRewardsSettled) {
            settleOriginalQg(swarm);
        }
        if (globalSearchConfiguration.isQpEnabled() && !caTaRewardsSettled) {
            settleQp(swarm);
        }
        if (!caTaRewardsSettled) {
            recordDualQCoordination();
        }
        if (globalSearchConfiguration.isEvaluatedPddrEnabled()) {
            applyEvaluatedPddr(swarm);
        }
        caTaRewardsSettled = false;
        appendAndPrunePersonalHistories(swarm);
    }

    /** Strict three-objective personal history; equal objectives do not dominate each other. */
    private void appendAndPrunePersonalHistories(List<PermutationSolution<Integer>> swarm) {
        if (tempSwarm.size() != swarm.size()) {
            throw new IllegalStateException("Personal history does not align with swarm slots");
        }
        for (int slot = 0; slot < swarm.size(); slot++) {
            tempSwarm.get(slot).add(ZhangBoSolutionSupport.deepCopy(swarm.get(slot)));
            LinkedHashMap<String, PermutationSolution<Integer>> unique = new LinkedHashMap<>();
            for (PermutationSolution<Integer> candidate : tempSwarm.get(slot)) {
                String fingerprint = ZhangBoQgController.fingerprint(candidate);
                if (!unique.containsKey(fingerprint)) {
                    unique.put(fingerprint, ZhangBoSolutionSupport.deepCopy(candidate));
                }
            }
            List<PermutationSolution<Integer>> values = new ArrayList<>(unique.values());
            List<PermutationSolution<Integer>> nonDominated = new ArrayList<>();
            for (int left = 0; left < values.size(); left++) {
                boolean dominated = false;
                for (int right = 0; right < values.size(); right++) {
                    if (left != right && strictlyDominates(values.get(right), values.get(left))) {
                        dominated = true;
                        break;
                    }
                }
                if (!dominated) nonDominated.add(values.get(left));
            }
            tempSwarm.set(slot, nonDominated);
        }
    }

    private static boolean strictlyDominates(PermutationSolution<Integer> left,
            PermutationSolution<Integer> right) {
        boolean strict = false;
        for (int objective : new int[] {0, 1, 6}) {
            if (left.getObjective(objective) > right.getObjective(objective)) return false;
            if (left.getObjective(objective) < right.getObjective(objective)) strict = true;
        }
        return strict;
    }


    private int index = 1;

    @Override  //更新全局最优
    protected void updateParticlesMemory(List<PermutationSolution<Integer>> swarm) {


//        System.out.println("swarm.size="+swarm.size());
//        try {
//            Thread.sleep(9999);
//        } catch (InterruptedException e) {
//            throw new RuntimeException(e);
//        }
        //添加

        //todo 这里是全局最优，也就是精英解？
//        for (int k = 0; k < swarm.size(); k++) {
        for (int k = 0; k < swarm.size(); k++) {
            PermutationSolution<Integer> candidate = (PermutationSolution<Integer>)
                    tempSwarm.get(k).get(tempSwarm.get(k).size() - 1).copy();
            V35Fc52LifecycleAudit fc52 = V35Fc52LifecycleAudit.current();
            if (fc52 != null) {
                fc52.observeArchiveAdd(candidate, globallyOptimalIndividual,
                        fullEvaluationCount, generationNumber());
            }
            ZhangBoIncrementalParetoArchive.add(globallyOptimalIndividual, candidate);
        }
//        System.out.println("globallyOptimalIndividual.size"+globallyOptimalIndividual.size());


//        if (t%100==0||t==498)
//        {
//            System.out.println(t);
//            System.out.println("---------------");
//            for (int i=0;i<globallyOptimalIndividual.size();i++){
//                System.out.println(globallyOptimalIndividual.get(i).getObjective(0));
//            }
//            for (int i=0;i<globallyOptimalIndividual.size();i++){
//                System.out.println(globallyOptimalIndividual.get(i).getObjective(1));
//            }
//            System.out.println("---------------");
//        }
//        t=t+1;
        double object1 = Double.MAX_VALUE;
        double object2 = Double.MAX_VALUE;
        double object3 = Double.MAX_VALUE;
        for (PermutationSolution<Integer> integerPermutationSolution : globallyOptimalIndividual) {
            if (object1 > integerPermutationSolution.getObjective(0))
                object1 = integerPermutationSolution.getObjective(0);
            if (object2 > integerPermutationSolution.getObjective(1))
                object2 = integerPermutationSolution.getObjective(1);
            if (object3 > integerPermutationSolution.getObjective(6))
                object3 = integerPermutationSolution.getObjective(6);
        }
        if (isIterationProgressOutputEnabled()) {
            System.out.println("第" + index + "代：" + "object1=" + object1 + " "
                    + "object2=" + object2 + " " + "object3=" + object3);
        }
        if (cmaxAudit != null) {
            cmaxAudit.refreshState(fullEvaluationCount, swarm, globallyOptimalIndividual);
        }
        index++;
    }

    @Override
    protected boolean isIterationProgressOutputEnabled() {
        return Boolean.getBoolean("zhangbo.progress.output");
    }

    @Override
    public List<PermutationSolution<Integer>> getResult() {
        return globallyOptimalIndividual;
    }

    @Override
    public String getName() {
        return "MOPSODivideSubgroup";
    }

    @Override
    public String getDescription() {
        return "Optimized MOPSODivideSubgroup";
    }


    //todo
    protected void exchange4WorkerSequence(PermutationSolution<Integer> swarm) {
        int[] nw = DHFSP.nw;
        int[] tempArray = new int[swarm.getVariablesworker().size()];
        for (int i = 0; i < swarm.getVariablesworker().size(); i++) {
            tempArray[i] = swarm.getVariableValueworker(i);
        }
        List<List<Integer>> lists = segmentArray(tempArray, nw);

        for (List<Integer> list : lists) {
            int r1, r2;
            r1 = formalRandomInt(0, list.size() - 1);
            r2 = formalRandomInt(0, list.size() - 1);
            if (r1 != r2) {
                exchangeIndex4WorkerSequenceVectorByExchangeSequence(r1, r2, list);
            }
        }
//        System.out.println(swarm);
        int tempindex = 0;
        for (List<Integer> list : lists) {
            for (Integer i : list) {
                swarm.setVariableValueworker(tempindex, i);
                tempindex++;
            }
        }
//        System.out.println(swarm);
//        sleep();
    }

    protected void exchangeIndex4WorkerSequenceVectorByExchangeSequence(int r1, int r2, List<Integer> list) {
//        Integer variableValueworker1 = swarm.getVariableValueworker(r1);
//        Integer variableValueworker2 = swarm.getVariableValueworker(r2);
//        swarm.setVariableValueworker(r1,variableValueworker2);
//        swarm.setVariableValueworker(r2,variableValueworker1);
//        System.out.println(list);
//        System.out.println(r1);
//        System.out.println(r2);
        Integer i1 = list.get(r1);
        Integer i2 = list.get(r2);
        list.set(r1, i2);
        list.set(r2, i1);
    }


    public static List<List<Integer>> segmentArray(int[] solution, int[] nw) {
        List<List<Integer>> segments = new ArrayList<>();
        int currentIndex = 0;

        for (int stepLength : nw) {
            if (currentIndex + stepLength > solution.length) {
                throw new IllegalArgumentException("Step length exceeds the remaining array size");
            }

            // 获取当前步长的子数组
            List<Integer> segment = new ArrayList<>();
            for (int i = currentIndex; i < currentIndex + stepLength; i++) {
                segment.add(solution[i]);
            }

            // 将子数组添加到结果列表中
            segments.add(segment);

            // 更新当前索引
            currentIndex += stepLength;
        }

        return segments;
    }

    //工人交换
    public void exchangeWorker(int[][] array, List<Integer> rowsToAdjust) {
        Random A = authorRandom();

        for (int row : rowsToAdjust) {
            if (row < 0 || row >= array.length) {
                throw new IllegalArgumentException("Row index out of bounds");
            }

            int[] currentRow = array[row];
            int length = currentRow.length;

            if (length < 2) {
                continue; // 如果行长度小于2，无法进行交换
            }

            // 随机选择两个不同的元素索引
            int index1 = A.nextInt(length);
            int index2 = A.nextInt(length);

            // 确保两个索引不同
            while (index1 == index2) {
                index2 = A.nextInt(length);
            }

            // 交换这两个元素
            int temp = currentRow[index1];
            currentRow[index1] = currentRow[index2];
            currentRow[index2] = temp;
        }
    }

    //    工人倒序
    public void reverseWorker(int[][] array, List<Integer> rowsToAdjust) {
        Random A = authorRandom();

        for (int row : rowsToAdjust) {
            if (row < 0 || row >= array.length) {
                throw new IllegalArgumentException("Row index out of bounds");
            }

            int[] currentRow = array[row];
            int length = currentRow.length;

            if (length < 2) {
                continue; // 如果行长度小于2，无法进行调整
            }

            // 随机选择两个不同的元素索引
            int index1 = A.nextInt(length);
            int index2 = A.nextInt(length);

            // 确保两个索引不同
            while (index1 == index2) {
                index2 = A.nextInt(length);
            }

            // 确保 index1 < index2
            if (index1 > index2) {
                int temp = index1;
                index1 = index2;
                index2 = temp;
            }

            // 对选定范围内的元素进行倒序操作
            reverseSubArray(currentRow, index1, index2);
        }
    }

    public static void reverseSubArray(int[] array, int start, int end) {
        while (start < end) {
            int temp = array[start];
            array[start] = array[end];
            array[end] = temp;
            start++;
            end--;
        }
    }


    //工人插入
    public void insertWorker(List<List<Integer>> matrix, List<Integer> indices) {
        Random random = authorRandom();
        for (int index : indices) {
            if (index >= 0 && index < matrix.size()) {
                List<Integer> row = matrix.get(index);
                // 随机选择一个元素的位置
                int elementPos = 0;
                while (elementPos == 0) {
                    elementPos = random.nextInt(row.size());
                }

                // 再次随机选择一个新的位置（不能与原位置相同）
                int newPos = elementPos;
                while (newPos == elementPos) {
                    newPos = random.nextInt(elementPos + 1); // 新位置必须在原位置之前

                }
                // 将选中的元素移动到新的位置
                moveElement(row, elementPos, newPos);
            }
        }
    }

    private static void moveElement(List<Integer> row, int fromIndex, int toIndex) {
        int element = row.get(fromIndex);
        // 删除原位置的元素
        row.remove(fromIndex);
        // 插入到新位置
        row.add(toIndex, element);
    }


    public static void replaceWorkerInSolution(int[][] array, List<Integer> replacementList) {
        if (replacementList.size() != countElements(array)) {
            throw new IllegalArgumentException("Replacement list size does not match the number of elements in the array");
        }

        int arrayIndex = 0;

        for (int i = 0; i < array.length; i++) {
            for (int j = 0; j < array[i].length; j++) {
                replacementList.set(arrayIndex++, array[i][j]);
            }
        }
    }

    public static int countElements(int[][] array) {
        int count = 0;
        for (int[] row : array) {
            count += row.length;
        }
        return count;
    }

    //二维数组转一维数组
    public static int[] flatten(int[][] matrix) {
        // 计算总元素数量
        int totalSize = 0;
        for (int[] row : matrix) {
            totalSize += row.length;
        }

        // 创建一维数组
        int[] result = new int[totalSize];
        int index = 0;

        // 遍历二维数组并将元素添加到一维数组中
        for (int[] row : matrix) {
            for (int element : row) {
                result[index++] = element;
            }
        }

        return result;
    }

    public void crossover4workersequence(PermutationSolution<Integer> HisOptIndividual, PermutationSolution<Integer> particle, int[] nw) {
        List<Integer> variablesworker1 = HisOptIndividual.getVariablesworker();
        List<Integer> variablesworker2 = particle.getVariablesworker();

        List<Integer> variablesid1 = HisOptIndividual.getVariablesid();
        List<Integer> variablesid2 = particle.getVariablesid();

        int numberOfVariables = HisOptIndividual.getNumberOfVariables();//每一阶段的工件数

        int stage = variablesworker1.size() / HisOptIndividual.getNumberOfVariables();
//        System.out.println("stage"+stage);
//        System.out.println(HisOptIndividual);
//        System.out.println(particle);

        for (int i = 0; i < stage; i++) {
            int begin = numberOfVariables * i;
//            int end = numberOfVariables*i+numberOfVariables-1;

            Random r = authorRandom();
            int r1 = r.nextInt(numberOfVariables);//5
            int r2 = r.nextInt(numberOfVariables);//9
//            int random1 = r1+begin;
//            int random2 = r2+begin;
//
            if (r1 > r2) {
                int temp;
                temp = r1;
                r1 = r2;
                r2 = temp;
            }
//            System.out.println("第"+i+"阶段的起始是："+r1);
//            System.out.println("第"+i+"阶段的终止是："+r2);

            //getVariablesid()这是工厂向量
//            System.out.println(HisOptIndividual.getVariablesid());
//            System.out.println(HisOptIndividual.getVariableValueid(14));
            for (int i1 = r1; i1 < r2; i1++) {
                int index = i1 + begin;
                if (HisOptIndividual.getVariableValueid(i1) == particle.getVariableValueid(i1)) {
                    Integer variableValueworker1 = HisOptIndividual.getVariableValueworker(index);
                    Integer variableValueworker2 = particle.getVariableValueworker(index);
//                    System.out.println("index"+index+",variableValueworker1:"+variableValueworker1+",variableValueworker2:"+variableValueworker2);
                    HisOptIndividual.setVariableValueworker(index, variableValueworker2);
                    particle.setVariableValueworker(index, variableValueworker1);
                }
            }

        }


//        System.out.println(HisOptIndividual);
//        System.out.println(particle);
//        sleep();
    }

    private static List<int[]> calculateBlocks(int[] nw, int listSize) {
        List<int[]> blocks = new ArrayList<>();
        int currentIndex = 0;

        for (int step : nw) {
            if (currentIndex < listSize) {
                int endIndex = Math.min(currentIndex + step, listSize);
                blocks.add(new int[]{currentIndex, endIndex});
                currentIndex = endIndex;
            }
        }

        // 处理剩余部分，如果有的话
        if (currentIndex < listSize) {
            blocks.add(new int[]{currentIndex, listSize});
        }

        return blocks;
    }

    private static void swapBlocks(List<Integer> a1, List<Integer> a2, int start, int end) {
        // 创建临时列表来保存 a1 的分块
        List<Integer> temp = new ArrayList<>(a1.subList(start, end));

        // 用 a2 的分块替换 a1 的分块
        for (int i = start; i < end; i++) {
            a1.set(i, a2.get(i));
        }

        // 用 a1 的分块（现在在 temp 中）替换 a2 的分块
        for (int i = start; i < end; i++) {
            a2.set(i, temp.get(i - start));
        }
    }

    //机器交叉
    private void crossover4machinesequence(PermutationSolution<Integer> integerPermutationSolution, PermutationSolution<Integer> particle, int[] nw) {

        ArrayList<Integer> machineintegerPermutationSolution = (ArrayList<Integer>) integerPermutationSolution.getAttribute("machine");
        ArrayList<Integer> machineparticle = (ArrayList<Integer>) particle.getAttribute("machine");


        List<Integer> integerPermutationSolutionfactory = integerPermutationSolution.getVariablesid();
        List<Integer> particlefactory = particle.getVariablesid();

        Random r = authorRandom();
        int max = r.nextInt(machineintegerPermutationSolution.size() - 1);
        int min = r.nextInt(machineintegerPermutationSolution.size() - 1);
        if (max < min) {
            int temp = 0;
            temp = max;
            max = min;
            min = temp;
        }
        List<Integer> machinetemp1 = new ArrayList<>(machineintegerPermutationSolution.subList(min, max + 1));
        List<Integer> machinetemp2 = new ArrayList<>(machineparticle.subList(min, max + 1));

        List<Integer> factorytemp1 = new ArrayList<>(integerPermutationSolutionfactory.subList(min, max + 1));
        List<Integer> factorytemp2 = new ArrayList<>(particlefactory.subList(min, max + 1));

        int[][] numberOfMachines = ZhangBoEDHHFSPW.numberOfMachines_;


        for (int i = 0; i < machinetemp1.size(); i++) {
            if (factorytemp1.get(i) == factorytemp2.get(i)) {
                int temp1 = machinetemp1.get(i);
                int temp2 = machinetemp2.get(i);
                machinetemp2.set(i, temp1);
                machinetemp1.set(i, temp2);
            } else {
                int temp1 = numberOfMachines[factorytemp1.get(i)][0];
                temp1 = r.nextInt(temp1);
                machinetemp1.set(i, temp1);

                int temp2 = numberOfMachines[factorytemp2.get(i)][0];
                temp2 = r.nextInt(temp2);
                machinetemp2.set(i, temp2);
            }
        }
        for (int i = 0; i < machinetemp1.size(); i++) {
            machineparticle.set(min, machinetemp2.get(i));
            min++;
        }

        particle.setAttribute("machine", machineparticle);

    }

    //工人变异
    private void mutation4worker(PermutationSolution<Integer> particle) {

        List<Integer> variablesworker = particle.getVariablesworker();
        int[] nw = ZhangBoEDHHFSPW.nw;
        Random r = authorRandom();
        int stage = activeWorkerStages(particle, "worker_mutation");
        int[][][] workerinfactory = DefaultIntegerPermutationSolution.result;


        for (int i = 0; i < stage; i++) {
            int index = r.nextInt(particle.getNumberOfVariables());
            int i1 = particle.getNumberOfVariables() * i + index;

            int worker = r.nextInt(workerinfactory[particle.getVariableValueid(index)][i].length);
            int workerchoose = workerinfactory[particle.getVariableValueid(index)][i][worker];
            particle.setVariableValueworker(i1, workerchoose);
        }

    }

    // 根据 nw 数组对 variablesworker 列表进行划分
    private static List<List<Integer>> splitIntoBlocks(List<Integer> variablesworker, int[] nw) {
        List<List<Integer>> blocks = new ArrayList<>();
        int currentIndex = 0;

        for (int size : nw) {
            // 创建一个新的分块
            List<Integer> block = new ArrayList<>(variablesworker.subList(currentIndex, currentIndex + size));
            blocks.add(block);
            currentIndex += size;
        }

        return blocks;
    }


    //机器变异
    private void mutation4machine(PermutationSolution<Integer> particle) {
        ArrayList<Integer> machine = (ArrayList<Integer>) particle.getAttribute("machine");
        List<Integer> variablesid = particle.getVariablesid();
        Random r = authorRandom();
        int i = r.nextInt(machine.size());
        int tempfactory = variablesid.get(i);
        int[][] numberOfMachines = ZhangBoEDHHFSPW.numberOfMachines_;
        int numberOfMachine = numberOfMachines[tempfactory][0];

        int temp = r.nextInt(numberOfMachine);
        machine.set(numberOfMachine, temp);
        particle.setAttribute("machine", machine);
    }

    private void applyEvaluatedPddr(List<PermutationSolution<Integer>> evaluatedOffspring) {
        int globalOffspringCount = pendingPddrOffspringHistories.size();
        V35Fc6LocalCandidateAudit fc6LocalAudit = V35Fc6LocalCandidateAudit.current();
        // Local v3.5 candidates are pre-evaluated outside the jMetal swarm list;
        // materialize them here exactly once before the common PDDR pass.
        if (evaluatedOffspring.size() == globalOffspringCount
                && !pendingCaTaLocalCandidates.isEmpty()) {
            for (PendingCaTaLocalCandidate local : pendingCaTaLocalCandidates) {
                evaluatedOffspring.add(local.solution);
                if (fc6LocalAudit != null) {
                    fc6LocalAudit.recordEnteredPddr((int) formalBaselineOuterCycles + 1,
                            local.origin.selectorSource);
                }
                V35Fc52LifecycleAudit fc52 = V35Fc52LifecycleAudit.current();
                if (fc52 != null) {
                    fc52.recordMergePool(Collections.singletonList(local.solution),
                            fullEvaluationCount);
                }
            }
        }
        if (pendingPddrParents.size() != swarmSize
                || pendingPddrParentHistories.size() != swarmSize
                || globalOffspringCount + pendingCaTaLocalCandidates.size()
                != evaluatedOffspring.size()) {
            throw new IllegalStateException("Incomplete evaluated-PDDR candidate state");
        }
        long firstOrdinal = fullEvaluationCount - evaluatedOffspring.size() + 1L;
        List<ZhangBoEvaluatedPddrSelector.CandidateInput> inputs = new ArrayList<>();
        List<ZhangBoEvaluatedPddrSelector.Source> fc6PddrSources =
                fc6LocalAudit == null ? null : new ArrayList<ZhangBoEvaluatedPddrSelector.Source>();
        for (int index = 0; index < globalOffspringCount; index++) {
            PermutationSolution<Integer> candidate = evaluatedOffspring.get(index);
            ZhangBoPreEvaluatedTag marker = ZhangBoPreEvaluatedTag.get(candidate);
            long ordinal = marker != null ? marker.getEvaluationOrdinal() : firstOrdinal + index;
            inputs.add(ZhangBoEvaluatedPddrSelector.CandidateInput.ofEvaluated(candidate,
                    pendingPddrOffspringHistories.get(index),
                    ZhangBoEvaluatedPddrSelector.Source.GLOBAL_OFFSPRING,
                    index, ordinal, index));
            if (fc6LocalAudit != null) {
                fc6LocalAudit.recordEnteredPddr((int) formalBaselineOuterCycles + 1,
                        ZhangBoEvaluatedPddrSelector.Source.GLOBAL_OFFSPRING);
                fc6PddrSources.add(ZhangBoEvaluatedPddrSelector.Source.GLOBAL_OFFSPRING);
            }
        }
        for (int index = 0; index < pendingCaTaLocalCandidates.size(); index++) {
            PendingCaTaLocalCandidate local = pendingCaTaLocalCandidates.get(index);
            ZhangBoPreEvaluatedTag marker = ZhangBoPreEvaluatedTag.get(local.solution);
            long ordinal = marker != null ? marker.getEvaluationOrdinal() : fullEvaluationCount;
            inputs.add(ZhangBoEvaluatedPddrSelector.CandidateInput.ofEvaluated(local.solution,
                    local.history, local.origin.selectorSource,
                    local.parentSlot, ordinal, globalOffspringCount + index));
            if (fc6LocalAudit != null) fc6PddrSources.add(local.origin.selectorSource);
        }
        if (fc6LocalAudit != null) {
            for (int index = 0; index < pendingPddrParents.size(); index++) {
                fc6LocalAudit.recordEnteredPddr((int) formalBaselineOuterCycles + 1,
                        ZhangBoEvaluatedPddrSelector.Source.PARENT);
                fc6PddrSources.add(ZhangBoEvaluatedPddrSelector.Source.PARENT);
            }
        }
        List<ZhangBoEvaluatedPddrSelector.Candidate> selected =
                zhangBoEvaluatedPddrSelector.select(inputs, pendingPddrParents,
                        pendingPddrParentHistories, swarmSize,
                        globalSearchConfiguration.getPddrSelectionMode());
        if (fc6LocalAudit != null) {
            fc6LocalAudit.recordPddrOutcome((int) formalBaselineOuterCycles + 1,
                    fc6PddrSources, selected);
        }
        V35Fc52LifecycleAudit fc52Audit = V35Fc52LifecycleAudit.current();
        org.uma.jmetal.algorithm.multiobjective.mypso.v35.V35Fc6BpPddrDiagnosticAudit
            fc6DiagAudit = org.uma.jmetal.algorithm.multiobjective.mypso.v35
                .V35Fc6BpPddrDiagnosticAudit.current();
        List<PermutationSolution<Integer>> pddrAll = null;
        if (fc52Audit != null || fc6DiagAudit != null) {
            // Order must mirror the select-internal values order: global offspring
            // (evaluatedOffspring[0..globalOffspringCount-1]), then local candidates
            // (pendingCaTaLocalCandidates[0..n-1]), then parents.
            pddrAll = new ArrayList<>(inputs.size() + pendingPddrParents.size());
            for (int index = 0; index < inputs.size(); index++) {
                pddrAll.add(index < globalOffspringCount
                        ? evaluatedOffspring.get(index)
                        : pendingCaTaLocalCandidates.get(index - globalOffspringCount).solution);
            }
            pddrAll.addAll(pendingPddrParents);
        }
        if (fc52Audit != null) {
            fc52Audit.recordPddrRound(pddrAll, selected, fullEvaluationCount);
        }
        if (cmaxAudit != null) {
            cmaxAudit.observePddrSelection(selected, generationNumber());
        }
        // V35-SOURCE-LEDGER-PATCH: pure observation of merge-pool composition and
        // PDDR selection (no-op unless armed; no behavior change, no RNG, no FE).
        if (org.uma.jmetal.algorithm.multiobjective.mypso.v35.V35SourceAttributionObserver.isArmed()) {
            java.util.List<PermutationSolution<Integer>> ledgerPoolSolutions =
                    new ArrayList<>(evaluatedOffspring);
            java.util.List<String> ledgerPoolSourceNames = new ArrayList<>();
            for (int ledgerIndex = 0; ledgerIndex < evaluatedOffspring.size(); ledgerIndex++) {
                ledgerPoolSourceNames.add(ledgerIndex < globalOffspringCount
                        ? ZhangBoEvaluatedPddrSelector.Source.GLOBAL_OFFSPRING.name()
                        : pendingCaTaLocalCandidates.get(ledgerIndex - globalOffspringCount)
                                .origin.selectorSource.name());
            }
            for (int ledgerIndex = 0; ledgerIndex < pendingPddrParents.size(); ledgerIndex++) {
                ledgerPoolSolutions.add(pendingPddrParents.get(ledgerIndex));
                ledgerPoolSourceNames.add(ZhangBoEvaluatedPddrSelector.Source.PARENT.name());
            }
            org.uma.jmetal.algorithm.multiobjective.mypso.v35.V35SourceAttributionObserver.onPddrRound(
                    ledgerPoolSolutions, ledgerPoolSourceNames, selected,
                    fullEvaluationCount, (int) formalBaselineOuterCycles + 1);
        }

        evaluatedOffspring.clear();
        tempSwarm.clear();
        if (globalSearchConfiguration.isLineageArchiveEnabled()) {
            List<ZhangBoLineageCoordinator.Branch> branches =
                    zhangBoLineageCoordinator.rebuild(selected, generationNumber());
            for (int index = 0; index < selected.size(); index++) {
                ZhangBoEvaluatedPddrSelector.Candidate candidate = selected.get(index);
                PermutationSolution<Integer> solution = candidate.getSolution();
                if (candidate.getAssignedRegionRole() != null) {
                    solution.setAttribute(ZhangBoSubSwarm.class, candidate.getAssignedRegionRole());
                }
                Object lineage = branches.get(index).getSolution()
                        .getAttribute(ZhangBoLineageTag.class);
                solution.setAttribute(ZhangBoLineageTag.class, lineage);
                evaluatedOffspring.add(solution);
                tempSwarm.add(candidate.getAuthorHistory());
            }
            if (globalSearchConfiguration.isQpEnabled()) {
                zhangBoQpController.reconcilePopulation(evaluatedOffspring,
                        zhangBoLineageCoordinator.getMemories(),
                        zhangBoLineageCoordinator.getFrozenBounds());
            }
            if (cmaxAudit != null) {
                cmaxAudit.observeLineageArchives(zhangBoLineageCoordinator.getMemories());
            }
        } else {
            for (ZhangBoEvaluatedPddrSelector.Candidate candidate : selected) {
                PermutationSolution<Integer> solution = candidate.getSolution();
                if (candidate.getAssignedRegionRole() != null) {
                    solution.setAttribute(ZhangBoSubSwarm.class, candidate.getAssignedRegionRole());
                }
                evaluatedOffspring.add(solution);
                tempSwarm.add(candidate.getAuthorHistory());
            }
        }
        // FC-6A-POST / Build-C2: PDDR counterfactual + rescue 事件 + rescue lineage 注册。
        // evaluatedOffspring 此时 = 重建打标后的下一轮 population（与 selected 同序）；
        // 必须放在 rebuild 之后才能读到新 lineage 标签（纯观察）。
        if (fc6DiagAudit != null) {
            fc6DiagAudit.recordPddrRound(pddrAll, selected, evaluatedOffspring,
                    fullEvaluationCount, (int) formalBaselineOuterCycles + 1);
        }
        evaluatedPddrSelections += selected.size();
        int globalSelected = 0;
        int localSelected = 0;
        int parentSelected = 0;
        for (ZhangBoEvaluatedPddrSelector.Candidate candidate : selected) {
            if (candidate.getSource()
                    == ZhangBoEvaluatedPddrSelector.Source.GLOBAL_OFFSPRING) {
                globalSelected++;
            } else if (candidate.getSource()
                    != ZhangBoEvaluatedPddrSelector.Source.PARENT) {
                localSelected++;
            } else if (candidate.getSource()
                    == ZhangBoEvaluatedPddrSelector.Source.PARENT) {
                parentSelected++;
            }
        }
        zhangBoPddrEvents.add("generation=" + generationNumber()
                + ",candidates=" + (pendingPddrParents.size()
                + pendingPddrOffspringHistories.size() + pendingCaTaLocalCandidates.size())
                + ",selected=" + selected.size() + ",globalOffspring=" + globalSelected
                + ",localOffspring=" + localSelected + ",parents=" + parentSelected
                + ",firstOffspringEvaluation=" + firstOrdinal);
        pendingPddrParents = new ArrayList<>();
        pendingPddrParentHistories = new ArrayList<>();
        pendingPddrOffspringHistories = new ArrayList<>();
        pendingCaTaLocalCandidates.clear();
    }

    private void recordDualQCoordination() {
        if (pendingDualQDecision == null) return;
        String qgAfter = zhangBoQgController.tableHash();
        String qpAfter = zhangBoQpController.tableHash();
        ZhangBoDualQCoordinator.Phase phase = pendingDualQDecision.getPhase();
        // V35-FC-4: with rho>0 the frozen side may absorb contribution-gated
        // soft TD updates, so the strict hash invariants only apply to the
        // hard freeze (rho=0, the archived A4 behaviour).
        double softRho = dualQSoftFreezeRho();
        if (phase == ZhangBoDualQCoordinator.Phase.P_BLOCK
                && softRho <= 0.0
                && !pendingQgTableHashBefore.equals(qgAfter)) {
            throw new IllegalStateException("Frozen Qg table changed during P-block");
        }
        if ((phase == ZhangBoDualQCoordinator.Phase.G_BLOCK
                || phase == ZhangBoDualQCoordinator.Phase.WARMUP)
                && softRho <= 0.0
                && !pendingQpTableHashBefore.equals(qpAfter)) {
            throw new IllegalStateException("Frozen Qp table changed during " + phase);
        }
        long qgSelections = zhangBoQgController.getSelectionCount() - pendingQgSelectionsBefore;
        long qgUpdates = zhangBoQgController.getTdUpdateCount() - pendingQgUpdatesBefore;
        long qpActions = zhangBoQpController.getExecutedActionCount() - pendingQpActionsBefore;
        long qpTransitions = zhangBoQpController.getTrainedTransitionCount()
                - pendingQpTransitionsBefore;
        String qgPolicy = phase == ZhangBoDualQCoordinator.Phase.P_BLOCK
                ? "GREEDY_FROZEN" : "EPSILON_GREEDY_LEARN";
        String qpPolicy;
        if (phase == ZhangBoDualQCoordinator.Phase.WARMUP) {
            qpPolicy = "DIRECTIONAL_WARMUP";
        } else if (phase == ZhangBoDualQCoordinator.Phase.G_BLOCK) {
            qpPolicy = "GREEDY_FROZEN";
        } else {
            qpPolicy = "EPSILON_GREEDY_LEARN";
        }
        zhangBoDualQEvents.add("generation=" + generationNumber() + ','
                + pendingDualQDecision.toCanonicalText()
                + ",evaluationsAfter=" + fullEvaluationCount
                + ",QgPolicy=" + qgPolicy + ",QpPolicy=" + qpPolicy
                + ",QgHashBefore=" + pendingQgTableHashBefore
                + ",QgHashAfter=" + qgAfter
                + ",QpHashBefore=" + pendingQpTableHashBefore
                + ",QpHashAfter=" + qpAfter
                + ",QgSelections=" + qgSelections + ",QgTdUpdates=" + qgUpdates
                + ",QpActions=" + qpActions + ",QpTrainedTransitions=" + qpTransitions);
        pendingDualQDecision = null;
    }

    private int activeWorkerStages(PermutationSolution<Integer> solution, String operation) {
        int encodedStages = solution.getNumberOfVariablesworker() / solution.getNumberOfVariables();
        if (!globalSearchConfiguration.isQgEnabled() || problemContext == null) {
            return encodedStages;
        }
        int activeStages = problemContext.getFatigueInstanceData().getStages();
        if (encodedStages > activeStages) {
            zhangBoP6Events.add("authorCompatibility:operation=" + operation
                    + ",WA blocks=" + encodedStages + ",activeStages=" + activeStages
                    + ",action=limit_to_active_stages");
            return activeStages;
        }
        return encodedStages;
    }

    public ZhangBoGlobalSearchConfiguration getGlobalSearchConfiguration() {
        return globalSearchConfiguration;
    }

    /**
     * Validation-only timing injection.  It changes no random event, FE, or configuration hash;
     * production callers that do not invoke it continue to use System.nanoTime().
     */
    public void setCaTaNanoClock(ZhangBoNeighborhoodCandidateGateway.NanoClock clock) {
        if (clock == null) throw new IllegalArgumentException("clock cannot be null");
        this.zhangBoNeighborhoodCandidateGateway =
                new ZhangBoNeighborhoodCandidateGateway(
                        new ZhangBoNeighborhoodSuite(), clock);
    }

    /** Enables the opt-in observation-only Cmax audit. No random source or decision reads it. */
    public void setCmaxAudit(ZhangBoCmaxAudit audit) {
        if (audit == null) throw new IllegalArgumentException("audit cannot be null");
        this.cmaxAudit = audit;
    }

    /** Enables the opt-in observation-only passive evaluation archive (V35-P17). */
    public void setPassiveEvaluationArchive(V35PassiveEvaluationArchive archive) {
        if (archive == null) throw new IllegalArgumentException("archive cannot be null");
        this.v35PassiveArchive = archive;
    }

    /** V35-P17 passive feed; a pure copy-only bypass, never read by any search mechanism. */
    // V35-SOURCE-LEDGER-PATCH: explicit source parameter (pure label, no behavior change).
    private void observePassiveArchive(PermutationSolution<Integer> evaluated,
            V35EvaluationSourceContext.Source source) {
        if (v35PassiveArchive != null) v35PassiveArchive.observeWithSource(evaluated, source);
    }

    public ZhangBoCmaxAudit getCmaxAudit() {
        return cmaxAudit;
    }

    public void setInitialSwarmOverride(List<PermutationSolution<Integer>> initialSwarm) {
        if (initialSwarm == null || initialSwarm.size() != swarmSize) {
            throw new IllegalArgumentException("Initial swarm override must contain "
                    + swarmSize + " solutions");
        }
        this.initialSwarmOverride = ZhangBoSolutionSupport.deepCopySolutions(initialSwarm);
    }

    public long getFullEvaluationCount() {
        return fullEvaluationCount;
    }

    public long getCfvfOffspringCount() {
        return cfvfOffspringCount;
    }

    /**
     * Pure run-end audit of the four physical capacities.  The result lists
     * are intentionally released by the algorithm after run(), so their final
     * element counts are not evidence of the capacities that governed search.
     */
    public String getRuntimeSubSwarmSizes() {
        return "G1_CMAX=" + upSize
                + ";G4_BALANCED=" + centralSize
                + ";G2_TEC=" + downSize
                + ";G3_TWC=" + upNewSize;
    }

    public long getCfvfRepairCount() {
        return cfvfRepairCount;
    }

    public long getCfvfInitializationCorrections() {
        return cfvfInitializationCorrections;
    }

    public long getAuthorUpdateResourceCorrections() {
        return authorUpdateResourceCorrections;
    }

    public long getEvaluatedPddrSelections() {
        return evaluatedPddrSelections;
    }

    public List<String> getZhangBoPddrEvents() {
        return new ArrayList<>(zhangBoPddrEvents);
    }

    public long getZhangBoPddrEventCount() { return zhangBoPddrEvents.getTotalCount(); }
    public String getZhangBoPddrEventStreamHash() { return zhangBoPddrEvents.rollingSha256(); }

    public Map<Long, ZhangBoLineageMemory> getZhangBoLineageMemories() {
        return zhangBoLineageCoordinator == null
                ? Collections.<Long, ZhangBoLineageMemory>emptyMap()
                : zhangBoLineageCoordinator.getMemories();
    }

    public List<String> getZhangBoLineageEvents() {
        return zhangBoLineageCoordinator == null
                ? Collections.<String>emptyList()
                : zhangBoLineageCoordinator.getEvents();
    }

    public long getZhangBoLineageEventCount() {
        return zhangBoLineageCoordinator == null ? 0L : zhangBoLineageCoordinator.getEventCount();
    }

    public String getZhangBoLineageEventStreamHash() {
        return zhangBoLineageCoordinator == null
                ? "disabled" : zhangBoLineageCoordinator.getEventStreamHash();
    }

    public String getZhangBoLineageCanonicalText() {
        return zhangBoLineageCoordinator == null
                ? "disabled\n" : zhangBoLineageCoordinator.toCanonicalText();
    }

    public long getZhangBoLineageSplitCount() {
        return zhangBoLineageCoordinator == null ? 0L : zhangBoLineageCoordinator.getSplits();
    }

    public long getZhangBoLineageDeletionCount() {
        return zhangBoLineageCoordinator == null ? 0L : zhangBoLineageCoordinator.getDeletions();
    }

    public long getZhangBoLineageMigrationCount() {
        return zhangBoLineageCoordinator == null ? 0L : zhangBoLineageCoordinator.getMigrations();
    }

    public long getZhangBoArchiveInsertionCount() {
        return zhangBoLineageCoordinator == null ? 0L : zhangBoLineageCoordinator.getInsertions();
    }

    public long getZhangBoArchiveDominatedRemovalCount() {
        return zhangBoLineageCoordinator == null ? 0L : zhangBoLineageCoordinator.getDominatedRemoved();
    }

    public long getZhangBoArchiveDuplicateRemovalCount() {
        return zhangBoLineageCoordinator == null ? 0L : zhangBoLineageCoordinator.getDuplicatesRemoved();
    }

    public long getZhangBoArchiveTruncationCount() {
        return zhangBoLineageCoordinator == null ? 0L : zhangBoLineageCoordinator.getTruncatedRemoved();
    }

    public String getV35DscrTeacherStatistics() {
        return v35DscrTeacherCache == null ? "disabled" : v35DscrTeacherCache.canonicalStatistics();
    }

    private static ZhangBoCmaxAudit.Operator cmaxAuditOperator(V35MacroNeighborhood action) {
        switch (action) {
            case N1: return ZhangBoCmaxAudit.Operator.N1;
            case N2: return ZhangBoCmaxAudit.Operator.N2;
            case N3: return ZhangBoCmaxAudit.Operator.N3;
            case N4: return ZhangBoCmaxAudit.Operator.N4;
            case N5: default: return ZhangBoCmaxAudit.Operator.N5;
        }
    }

    public String getV35DscrEventsCsv() {
        return v35DscrTeacherCache == null ? "" : v35DscrTeacherCache.eventsCsv();
    }

    public String getV35DscrTeacherUsesCsv() {
        return v35DscrTeacherCache == null ? "" : v35DscrTeacherCache.teacherUsesCsv();
    }

    public List<String> getZhangBoP6Events() {
        return new ArrayList<>(zhangBoP6Events);
    }

    public long getZhangBoP6EventCount() { return zhangBoP6Events.getTotalCount(); }
    public String getZhangBoP6EventStreamHash() { return zhangBoP6Events.rollingSha256(); }
    public long getBaselineUpdateEventCount() { return baselineUpdateEventCount; }
    public long getFixedNeighborhoodEventCount() { return fixedNeighborhoodEventCount; }
    public ZhangBoFormalHmopsoQgsConfiguration getFormalBaselineConfiguration() {
        return formalBaselineConfiguration;
    }
    public long getFormalBaselineOuterCycles() { return formalBaselineOuterCycles; }
    public long getFormalBaselineQgRounds() { return formalBaselineQgRounds; }
    public void setAllowTerminalPartialFormalQPhase(boolean value) {
        this.allowTerminalPartialFormalQPhase = value;
    }
    public long getFormalCriticalFactorySwapEvaluations() {
        return formalCriticalFactorySwapEvaluations;
    }
    public long getFormalCriticalFactoryInsertEvaluations() {
        return formalCriticalFactoryInsertEvaluations;
    }
    public long getFormalOriginalNeighborhoodEvaluations() {
        return formalOriginalNeighborhoodEvaluations;
    }

    public String getQgCanonicalText() {
        return zhangBoQgController == null ? "disabled\n" : zhangBoQgController.toCanonicalText();
    }

    /** Detailed Q-gbest decisions for the opt-in I1 paper trace. */
    public List<String> getQgEvents() {
        return zhangBoQgController == null
                ? Collections.<String>emptyList() : zhangBoQgController.getEvents();
    }

    public long getQgEventCount() {
        return zhangBoQgController == null ? 0L : zhangBoQgController.getEventCount();
    }

    public String getQgEventStreamHash() {
        return zhangBoQgController == null ? "disabled" : zhangBoQgController.getEventStreamHash();
    }

    public String getQpCanonicalText() {
        return zhangBoQpController == null ? "disabled\n" : zhangBoQpController.toCanonicalText();
    }

    public List<String> getQpEvents() {
        return zhangBoQpController == null
                ? Collections.<String>emptyList() : zhangBoQpController.getEvents();
    }

    public long getQpEventCount() {
        return zhangBoQpController == null ? 0L : zhangBoQpController.getEventCount();
    }

    public String getQpEventStreamHash() {
        return zhangBoQpController == null ? "disabled" : zhangBoQpController.getEventStreamHash();
    }

    public long getQpActionCount(ZhangBoQpAction action) {
        return zhangBoQpController == null ? 0L : zhangBoQpController.getActionCount(action);
    }

    public double getQpAverageReward(ZhangBoQpAction action) {
        return zhangBoQpController == null ? 0.0 : zhangBoQpController.getAverageReward(action);
    }

    public long getQpPbestSwitches() {
        return zhangBoQpController == null ? 0L : zhangBoQpController.getPbestSwitches();
    }

    public List<String> getDualQCoordinationEvents() {
        return new ArrayList<>(zhangBoDualQEvents);
    }

    public long getDualQEventCount() { return zhangBoDualQEvents.getTotalCount(); }
    public String getDualQEventStreamHash() { return zhangBoDualQEvents.rollingSha256(); }

    public long getDualQPhaseCount(ZhangBoDualQCoordinator.Phase phase) {
        Long value = dualQPhaseCounts.get(phase);
        return value == null ? 0L : value;
    }

    public long getQgSelectionCount() {
        return zhangBoQgController == null ? 0L : zhangBoQgController.getSelectionCount();
    }

    public long getQgTdUpdateCount() {
        return zhangBoQgController == null ? 0L : zhangBoQgController.getTdUpdateCount();
    }

    public long getDirectionalTeacherPoolRequestCount() {
        return zhangBoQgController == null ? 0L
                : zhangBoQgController.getDirectionalPoolRequestCount();
    }

    public long getDirectionalTeacherPoolFilteredCount() {
        return zhangBoQgController == null ? 0L
                : zhangBoQgController.getDirectionalPoolFilteredCount();
    }

    public long getQpExecutedActionCount() {
        return zhangBoQpController == null ? 0L : zhangBoQpController.getExecutedActionCount();
    }

    public long getQpTrainedTransitionCount() {
        return zhangBoQpController == null ? 0L : zhangBoQpController.getTrainedTransitionCount();
    }

    public String getQgTableHash() {
        return zhangBoQgController == null ? "disabled" : zhangBoQgController.tableHash();
    }

    public String getQpTableHash() {
        return zhangBoQpController == null ? "disabled" : zhangBoQpController.tableHash();
    }

    /** P7.2 CA-TA event stream; returns a defensive copy for reproducibility audits. */
    public List<String> getCaTaEvents() {
        return new ArrayList<>(zhangBoCaTaEvents);
    }

    public long getCaTaEventCount() { return zhangBoCaTaEvents.getTotalCount(); }
    public String getCaTaEventStreamHash() { return zhangBoCaTaEvents.rollingSha256(); }

    /** Canonical Test-and-Apply statistics, or an explicit disabled marker. */
    public String getCaTaStatisticsCanonicalText() {
        return zhangBoCaTaController == null
                ? "disabled\n" : zhangBoCaTaController.getStatistics().toCanonicalText();
    }

    public long getCaTaTestCalls() {
        return caTaTestCalls;
    }

    /** V35-FC-1 audit: FM3-structure-fed N3/N4 preview count. */
    public long getV35Fm3StructurePreviews() { return v35Fm3StructurePreviews; }
    /** V35-FC-1 audit: PT0-proxy-fed N3/N4 preview count. */
    public long getV35ProxyStructurePreviews() { return v35ProxyStructurePreviews; }

    public long getCaTaApplyCalls() {
        return caTaApplyCalls;
    }

    public long getCaTaFullEvaluations() {
        return caTaFullEvaluations;
    }

    public String getV35PressureDiagnosisEventsCsv() {
        String header = "generation,mainFE,parentSlot,factory,role,pSeq,pMac,pWor,pSet,pFat,"
                + "maximumType,secondType,maximumPressure,secondPressure,pressureGap,confident,"
                + "diagnosis,reason,criticalOperations,mask,decision,decisionActions\n";
        return v35PressureDiagnosisEvents.isEmpty() ? header
                : header + String.join("\n", v35PressureDiagnosisEvents) + "\n";
    }

    public long getV35PressureDiagnosisEventCount() {
        return v35PressureDiagnosisEvents.getTotalCount();
    }

    public String getV35PressureDiagnosisEventHash() {
        return v35PressureDiagnosisEvents.rollingSha256();
    }

    public String getV35ShadowDiagnosisCsv() {
        return v35ShadowDiagnosisAudit == null
                ? V35ShadowDiagnosisAudit.HEADER + "\n" : v35ShadowDiagnosisAudit.toCsv();
    }

    public long getV35ShadowDiagnosisSamples() {
        return v35ShadowDiagnosisAudit == null ? 0L : v35ShadowDiagnosisAudit.getSamples();
    }

    public int getV35ShadowDiagnosisEvaluations() {
        return v35ShadowDiagnosisAudit == null ? 0 : v35ShadowDiagnosisAudit.getFullEvaluations();
    }

    public long getV35ShadowEligibleInvocations() {
        return v35ShadowDiagnosisAudit == null ? 0L
                : v35ShadowDiagnosisAudit.getEligibleInvocations();
    }

    public String getSubSwarmSemanticsVersion() {
        return ZhangBoSubSwarmSemantics.VERSION;
    }

    public String getSubSwarmRoleMappingHash() {
        return ZhangBoSubSwarmSemantics.mappingHash();
    }

}
