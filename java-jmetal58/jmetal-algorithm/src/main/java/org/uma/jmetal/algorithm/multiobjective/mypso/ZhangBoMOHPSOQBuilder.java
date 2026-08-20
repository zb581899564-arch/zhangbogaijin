package org.uma.jmetal.algorithm.multiobjective.mypso;

import org.uma.jmetal.algorithm.multiobjective.mypso.zhangbo.ZhangBoGlobalSearchConfiguration;
import org.uma.jmetal.algorithm.multiobjective.mypso.zhangbo.ZhangBoFormalHmopsoQgsConfiguration;
import org.uma.jmetal.algorithm.multiobjective.mypso.v35.V35ProductionConfiguration;
import org.uma.jmetal.problem.Problem;
import org.uma.jmetal.solution.PermutationSolution;
import org.uma.jmetal.util.AlgorithmBuilder;
import org.uma.jmetal.util.evaluator.SolutionListEvaluator;
import org.uma.jmetal.util.evaluator.impl.SequentialSolutionListEvaluator;

import java.util.List;

/** Class implementing the MOPSODivideSubgroup algorithm */

public class ZhangBoMOHPSOQBuilder implements AlgorithmBuilder<ZhangBoMOHPSOQ> {

    protected final Problem<PermutationSolution<Integer>> problem;
    protected SolutionListEvaluator<PermutationSolution<Integer>> evaluator;

    private int swarmSize;
    private int maxIterations;
    private int upSize ;
    private int upNewSize;
    private int centralSize;
    private int downSize;
    private int factories;

    private double DERate;
    private double rand_k;
    private double crossoverRate;
    private double mutationRate;
    private double DEcrossoverRates;
    private double DEmutationRate;
    private double Qnums;
    private double CrossoverRates4worker;
    private double CrossoverRates4machine;
    private double mutationRate4worker;
    private double mutationRate4machine;
    private int localsearch;
    private ZhangBoGlobalSearchConfiguration globalSearchConfiguration =
            ZhangBoGlobalSearchConfiguration.disabled();
    private ZhangBoFormalHmopsoQgsConfiguration formalBaselineConfiguration =
            ZhangBoFormalHmopsoQgsConfiguration.disabled();
    private List<PermutationSolution<Integer>> initialSwarmOverride;
    private int configuredUpSize = -1;
    private int configuredCentralSize = -1;
    private int configuredDownSize = -1;
    private int configuredUpNewSize = -1;
    private V35ProductionConfiguration v35Configuration;

    /**
     *
     * @param problem
     * @param upSize
     * @param centralSize
     * @param downSize
     * @param Probability
     */
/*    public MOPSODivSubDEBuilder(Problem<PermutationSolution<Integer>> problem, int upSize, int centralSize, int downSize, double Probability) {
        this.problem = problem ;
        evaluator = new SequentialSolutionListEvaluator<PermutationSolution<Integer>>();
        this.upSize = upSize;
        this.centralSize = centralSize;
        this.downSize = downSize;
        this.DERate = Probability;
    }*/

    /**
     * Changed by zhangwq 20211008
     * @param problem
     * @param swarmSize
     * @param DERate
     */

    /**
     *
     * @param problem
     * @param swarmSize
     * @param numberOffactories
     * @param DERate
     * @param DEcrossoverRates
     * @param DEmutationRate
     * @param Qnums
     */
    public ZhangBoMOHPSOQBuilder(Problem<PermutationSolution<Integer>> problem, int swarmSize, int numberOffactories,double DERate,double DEcrossoverRates,double DEmutationRate,double Qnums) {
        this.problem = problem ;
        evaluator = new SequentialSolutionListEvaluator<PermutationSolution<Integer>>();

        //swarmSizes[s] / 3, swarmSizes[s] - (swarmSizes[s] / 3) * 2, swarmSizes[s] / 3,


        //新版分成四个种群 均分
//        this.upSize = swarmSize / 4;//20
//        this.upNewSize = swarmSize - (swarmSize / 4) * 3;//20
//        this.centralSize = swarmSize- (swarmSize / 4) * 3;//40
//        this.downSize = swarmSize -upSize-upNewSize-centralSize;//20


        //中心种群40，其余20
//        this.upSize = swarmSize / 5;//20
//        this.upNewSize = swarmSize - (swarmSize / 5) * 4;//20
//        this.centralSize = swarmSize- (swarmSize / 5) * 3;//40
//        this.downSize = swarmSize -upSize-upNewSize-centralSize;//20


        //中心种群55，其余15
        this.upSize      = 15;
        this.upNewSize   = 15;
        this.centralSize = 55;
        this.downSize    = 15;

        //中心种群70，其余10
//        this.upSize      = swarmSize/10;
//        this.upNewSize   = swarmSize/10;
//        this.downSize    = swarmSize/10;
//        this.centralSize = swarmSize-upSize-upNewSize-downSize;

        //中心种群46，其余18
//        this.upSize      = 18;
//        this.upNewSize   = 18;
//        this.downSize    = 18;
//        this.centralSize = swarmSize-upSize-upNewSize-downSize;

//        System.out.println(this.upSize);
//        System.out.println(this.upNewSize);
//        System.out.println(this.centralSize);
//        System.out.println(this.downSize);
//
//        try {
//            Thread.sleep(9999999);
//        } catch (InterruptedException e) {
//            throw new RuntimeException(e);
//        }

        //老版分成三个种群
//        this.upSize = swarmSize / 3;
//        this.centralSize = swarmSize - (swarmSize / 3) * 2;
//        this.downSize = swarmSize/  3;

        this.DEcrossoverRates=DEcrossoverRates;
        this.DEmutationRate=DEmutationRate;
        this.DERate = DERate;
        this.Qnums = Qnums;
        this.factories = numberOffactories;
    }

    public ZhangBoMOHPSOQBuilder setSwarmSize(int swarmSize) {
        this.swarmSize = swarmSize ;

        return this ;
    }

    public ZhangBoMOHPSOQBuilder setLocalSearch(int localsearch) {
        this.localsearch = localsearch ;
        return this ;
    }

    public ZhangBoMOHPSOQBuilder setMaxIterations(int maxIterations) {
        this.maxIterations = maxIterations ;

        return this ;
    }

    /* Getters */
    public int getSwarmSize() {
        return swarmSize;
    }
    public double getRand_k() {
        return rand_k;
    }
    public double getCrossoverRate() {
        return crossoverRate;
    }
    public double getMutationRate() {
        return mutationRate;
    }
    public int getMaxIterations() {
        return maxIterations;
    }

    public double getCrossoverRates4worker() {
        return CrossoverRates4worker;
    }

    public ZhangBoMOHPSOQBuilder setRand_k(double rand_k) {
        this.rand_k = rand_k;
        return this ;
    }
    public ZhangBoMOHPSOQBuilder setCrossoverRate(double Cross_c) {
        this.crossoverRate = Cross_c;
        return this ;
    }
    public ZhangBoMOHPSOQBuilder setMutationRate(double Mutation_m) {
        this.mutationRate = Mutation_m;
        return this ;
    }
    public ZhangBoMOHPSOQBuilder setFactories(int factories) {
        this.factories = factories;
        return this ;
    }


    public ZhangBoMOHPSOQBuilder setCrossoverRates4worker(double CrossoverRates4worker) {
        this.CrossoverRates4worker = CrossoverRates4worker;
        return this ;
    }

    public ZhangBoMOHPSOQBuilder setCrossoverRates4machine(double CrossoverRates4machine) {
        this.CrossoverRates4machine = CrossoverRates4machine;

        return this ;
    }


    public ZhangBoMOHPSOQBuilder setMutationRate4worker(double mutationRate4worker) {
        this.mutationRate4worker = mutationRate4worker;
        return this ;
    }

    public ZhangBoMOHPSOQBuilder setMutationRate4machine(double mutationRate4machine) {
        this.mutationRate4machine = mutationRate4machine;
        return this ;
    }

    public ZhangBoMOHPSOQBuilder setGlobalSearchConfiguration(
            ZhangBoGlobalSearchConfiguration globalSearchConfiguration) {
        if (globalSearchConfiguration == null) {
            throw new IllegalArgumentException("globalSearchConfiguration cannot be null");
        }
        this.globalSearchConfiguration = globalSearchConfiguration;
        return this;
    }

    /** Binds the v3.5 no-shift production boundary and Table-9 baseline atomically. */
    public ZhangBoMOHPSOQBuilder setV35Configuration(
            V35ProductionConfiguration configuration) {
        if (configuration == null) {
            throw new IllegalArgumentException("v35Configuration cannot be null");
        }
        this.v35Configuration = configuration;
        this.swarmSize = configuration.getPopulationSize();
        this.globalSearchConfiguration = ZhangBoGlobalSearchConfiguration.forV35(configuration);
        setFormalBaselineConfiguration(ZhangBoFormalHmopsoQgsConfiguration.table9());
        if (configuration.getPopulationSize() == 100) {
            setPhysicalSubswarmSizes(20, 40, 20, 20);
        } else if (configuration.getPopulationSize() == 10) {
            setPhysicalSubswarmSizes(2, 4, 2, 2);
        } else {
            throw new IllegalArgumentException(
                "v3.5 smoke/formal builder currently supports population 10 or 100");
        }
        return this;
    }

    public V35ProductionConfiguration getV35Configuration() {
        return v35Configuration;
    }

    public ZhangBoGlobalSearchConfiguration getGlobalSearchConfiguration() {
        return globalSearchConfiguration;
    }

    /**
     * Binds the paper-explicit HMOPSO-QGS parameters to both the legacy scalar fields and the
     * runtime contract consumed by the structured production path.
     */
    public ZhangBoMOHPSOQBuilder setFormalBaselineConfiguration(
            ZhangBoFormalHmopsoQgsConfiguration configuration) {
        if (configuration == null) {
            throw new IllegalArgumentException("formalBaselineConfiguration cannot be null");
        }
        this.formalBaselineConfiguration = configuration;
        if (configuration.isEnabled()) {
            this.rand_k = configuration.getRandomCoefficientUpperBound();
            this.crossoverRate = configuration.getFaCrossover();
            this.CrossoverRates4machine = configuration.getMaCrossover();
            this.CrossoverRates4worker = configuration.getWaCrossover();
            this.mutationRate = configuration.getFaMutation();
            this.mutationRate4machine = configuration.getMaMutation();
            this.mutationRate4worker = configuration.getWaMutation();
            this.Qnums = configuration.getQTimes();
            this.localsearch = configuration.getLocalSearchTimes();
            this.DEcrossoverRates = configuration.getGamma();
            this.DEmutationRate = configuration.getEpsilon();
        }
        return this;
    }

    public ZhangBoFormalHmopsoQgsConfiguration getFormalBaselineConfiguration() {
        return formalBaselineConfiguration;
    }

    public ZhangBoMOHPSOQBuilder setInitialSwarmOverride(
            List<PermutationSolution<Integer>> initialSwarmOverride) {
        this.initialSwarmOverride = initialSwarmOverride;
        return this;
    }

    /**
     * Sets the physical author slot sizes for an explicitly governed experiment.
     * The default constructor values remain untouched for the author-compatible path.
     * Order is groupU1, groupC2, groupD3, groupUNew.
     */
    public ZhangBoMOHPSOQBuilder setPhysicalSubswarmSizes(
            int groupU1, int groupC2, int groupD3, int groupUNew) {
        int sum = groupU1 + groupC2 + groupD3 + groupUNew;
        if (groupU1 <= 0 || groupC2 <= 0 || groupD3 <= 0 || groupUNew <= 0) {
            throw new IllegalArgumentException("Physical subgroup sizes must be positive");
        }
        if (swarmSize > 0 && sum != swarmSize) {
            throw new IllegalArgumentException("Physical subgroup sizes must equal swarmSize");
        }
        configuredUpSize = groupU1;
        configuredCentralSize = groupC2;
        configuredDownSize = groupD3;
        configuredUpNewSize = groupUNew;
        return this;
    }


    //新版三目标
    public ZhangBoMOHPSOQ build() {
        int effectiveUpSize = configuredUpSize > 0 ? configuredUpSize : upSize;
        int effectiveCentralSize = configuredCentralSize > 0 ? configuredCentralSize : centralSize;
        int effectiveDownSize = configuredDownSize > 0 ? configuredDownSize : downSize;
        int effectiveUpNewSize = configuredUpNewSize > 0 ? configuredUpNewSize : upNewSize;
        ZhangBoMOHPSOQ algorithm = new ZhangBoMOHPSOQ(factories,crossoverRate, mutationRate, rand_k,problem, evaluator, swarmSize, maxIterations, effectiveUpSize, effectiveCentralSize, effectiveDownSize, effectiveUpNewSize,DERate,DEcrossoverRates,DEmutationRate,Qnums,
                CrossoverRates4worker,CrossoverRates4machine,mutationRate4worker,mutationRate4machine,localsearch,
                globalSearchConfiguration, formalBaselineConfiguration) ;
        if (initialSwarmOverride != null) algorithm.setInitialSwarmOverride(initialSwarmOverride);
        return algorithm;
    }


    //老版双目标
//    public MOPSODivSubDE build() {
//        return new MOPSODivSubDE(factories,crossoverRate, mutationRate, rand_k,problem, evaluator, swarmSize, maxIterations, upSize, centralSize, downSize,DERate,DEcrossoverRates,DEmutationRate,Qnums) ;
//    }
}
