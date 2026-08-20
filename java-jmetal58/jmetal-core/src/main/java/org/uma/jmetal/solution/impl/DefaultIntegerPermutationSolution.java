package org.uma.jmetal.solution.impl;

import org.uma.jmetal.problem.PermutationProblem;
import org.uma.jmetal.problem.Problem;
import org.uma.jmetal.solution.PermutationSolution;

import java.io.*;
import java.nio.file.Paths;
import java.util.*;

/**
 * Defines an implementation of solution composed of a permuation of integers
 *
 * @author Antonio J. Nebro <antonio@lcc.uma.es>
 */
@SuppressWarnings("serial")
public class DefaultIntegerPermutationSolution                     //编码
    extends AbstractGenericSolutionWithid<Integer, PermutationProblem<?>>
    implements PermutationSolution<Integer> {

  public static int[][][] result;
  protected int problemflag ;

  static int n = 0;  // 工厂数
  static int s = 0;  // 阶段数
  static int j = 0;  // 阶段数

  //protected int[][] timeMatrix_;     // 时间矩阵（问题集中的内容）
  //protected int[] timeArray1;      //一维时间和
//  protected int[][] jobindex = new int[500][500];


  static String filename = Paths.get(
      System.getProperty("dhfsp.data.dir", "EADHFSP"), "150_8_5_1.txt").toString();


  /** Constructor */
  public DefaultIntegerPermutationSolution(PermutationProblem<?> problem) {
    super(problem);

    String domainFile = System.getProperty("dhfsp.solution.domain.file", filename);
    int[][] ints = readFactoryData(domainFile);//读取文件中的工厂阶段中的机器数

    int[][] workerstage = new int[ints.length][];
    for (int i = 0; i < ints.length; i++) {
      workerstage[i] = Arrays.copyOf(ints[i], ints[i].length);
    }

    // 遍历新数组并修改符合条件的元素
    for (int i = 0; i < workerstage.length; i++) {
      for (int j = 0; j < workerstage[i].length; j++) {
        if (workerstage[i][j] > 2) {
          workerstage[i][j] -= 1;
        }
      }
    }

//
//
//
//    for (int[] anInt : ints) {
//      System.out.println(Arrays.toString(anInt));
//    }
//
//    for (int[] anInt : workerstage) {
//      System.out.println(Arrays.toString(anInt));
//    }


//    System.out.println(j);
//        try {
//      Thread.sleep(999999);
//    } catch (InterruptedException e) {
//      throw new RuntimeException(e);
//    }
    List<Integer> randomSequence = new ArrayList<>(problem.getPermutationLength());
/*    List<Integer> jobArr = new ArrayList<>(problem.getPermutationLength());
    List<Integer> facArrtemp = new ArrayList<>(problem.getPermutationLength());
    List<Integer> facArr = new ArrayList<>(problem.getPermutationLength());
    Random r = new Random();
    for (int i = 0; i < indexArr.length; i++) {
      //randomSequence.add(indexArr[0][i]);
      for(int k = 0;k<indexArr[i].length;k++){
         jobArr.add(indexArr[i][k]);
      }
      int mark = (indexArr[i].length) % 3;
      for(int j = 0; j<indexArr[i].length-mark; j=j+3){
        facArrtemp.add(0);
        facArrtemp.add(1);
        facArrtemp.add(2);
      }
      for(int j = indexArr[i].length-mark; j<indexArr[i].length; j++) {
        facArrtemp.add(r.nextInt(3));
      }
      java.util.Collections.shuffle(facArrtemp);        //打乱工厂
      for(int k = 0;k<indexArr[i].length;k++){
        facArr.add(facArrtemp.get(i));
      }
      facArrtemp.clear();
  }

    for (int i = 0; i < getNumberOfVariables(); i++) {
      setVariableValue(i, jobArr.get(i)) ;      //工件向量序列
    }

    for (int i = 0; i < getNumberOfVariablesid(); i++) {
      setVariableValueid(i, facArr.get(i)) ;         //工厂向量序列
    }*/
    /////////////////////////////////////////////////////////

    for (int j = 0; j < problem.getPermutationLength(); j++) {
      randomSequence.add(j); // 有序的序列

    }
//    System.out.println(randomSequence);
    java.util.Collections.shuffle(randomSequence);   //  打乱的序列    用于工件

    for (int i = 0; i < getNumberOfVariables(); i++) {
      setVariableValue(i, randomSequence.get(i)) ;      //工件向量序列
    }


    List<Integer> randomSequence1 = new ArrayList<>(problem.getPermutationLength());
/*
    for (int j = 0; j < problem.getPermutationLength(); j++) {
      randomSequence1.add(j % problem.getNumberOfFactories());       //用于工厂序列
    }   //之前
*/

    for (int j = 0; j < problem.getNumberOfFactories(); j++) {
      randomSequence1.add(j);       //用于工厂序列
    }
    for (int j = 0; j < problem.getNumberOfFactories(); j++) {
      randomSequence1.add(j);       //用于工厂序列
    }
    for (int j = 0; j < problem.getNumberOfFactories(); j++) {
      randomSequence1.add(j);       //用于工厂序列
    }
//    System.out.println(randomSequence);
    Random rand=new Random();
    for (int j = 3*problem.getNumberOfFactories(); j < getNumberOfVariables(); j++) {
      int f=rand.nextInt(problem.getNumberOfFactories());
      randomSequence1.add(f);       //用于工厂序列
    }
//    System.out.println(randomSequence1);

/*    for (int j = 0; j < 3; j++) {
      randomSequence1.add(j);       //用于工厂序列
    }
    Random rand=new Random();
    for (int j = 3; j < getNumberOfVariables(); j++) {
      int f=rand.nextInt(3);
      randomSequence1.add(f);       //用于工厂序列
    }*/

    //java.util.Collections.shuffle(randomSequence1);          //  打乱的序列

//    System.out.println(randomSequence1);
//    try {
//      Thread.sleep(999999);
//    } catch (InterruptedException e) {
//      throw new RuntimeException(e);
//    }

    for (int i = 0; i < getNumberOfVariablesid(); i++) {
      setVariableValueid(i, randomSequence1.get(i)) ;         //工厂向量序列
    }



//
//    for (int i = 0 ; i < getNumberOfVariablesid(); i++){
//    for (int i = 0 ; i < 54; i++){
//      setVariableValueworker(i,randomSequence.get(i));
//    }


    List<Integer> readfile = readfile(domainFile);
    int n = readfile.size();

    int[][] arrays = new int[n][];
    for (int i = 0; i < n; i++) {
      int length = readfile.get(i);
      arrays[i] = new int[length];
      for (int j = 0; j < length; j++) {
        arrays[i][j] = j % readfile.get(i);
      }
    }

    result = new int[arrays.length][][];

    for (int n1 = 0; n1 < arrays.length; n1++) {
      int[] workers = arrays[n1];          // 当前工厂所有工人
      int[] stages = workerstage[n1];       // 当前工厂各阶段需求
      result[n1] = new int[stages.length][]; // 初始化该工厂的阶段数组

      int startIdx = 0;  // 记录工人选取起始位置
      for (int s = 0; s < stages.length; s++) {
        int need = stages[s];            // 当前阶段需要工人数
        result[n1][s] = Arrays.copyOfRange(workers, startIdx, startIdx + need);
        startIdx += need;                // 更新起始位置
      }
    }
//    for (int[][] ints1 : result) {
//      for (int[] ints2 : ints1) {
//        System.out.println(Arrays.toString(ints2));
//      }
//    }
//    System.out.println("getVariablesid"+getVariablesid());
//    System.out.println("Variablesworker"+getVariablesworker());
//    System.out.println("getNumberOfVariablesworker"+getNumberOfVariablesworker());


//需要先确定每个工件的工厂选择，然后每个阶段从这个工厂内的工人随机选择一个进行分配
//    System.out.println("getNumberOfVariablesid"+getNumberOfVariablesid());
//    System.out.println("getVariablesid"+getVariablesid());
    for (int stage = 0; stage < s; stage++) {
      for (int i = 0; i < getVariablesid().size(); i++) {
        int index = stage * getNumberOfVariablesid() + i;
//        System.out.println(index);
        int currentFactory = getVariableValueid(i);//获取第i个工件所在的工厂
//      System.out.println("currentFactory"+currentFactory);
        int numberWorker = result[currentFactory][stage].length;//看这个工厂内有几个工人
        int randomIndex = rand.nextInt(numberWorker);
        setVariableValueworker(index,result[currentFactory][stage][randomIndex]);
      }
    }
//    try {
//      Thread.sleep(12312344);
//    } catch (InterruptedException e) {
//      throw new RuntimeException(e);
//    }

//    int index = 0;
//    for (int i = 0; i < n; i++) {
//      shuffleArray(arrays[i]);
//
//      for (int j = 0; j < arrays[i].length; j++) {
//        //工人向量编码
//          setVariableValueworker(index,arrays[i][j]);
//          index++;
//        }
//
//    }
//
//    for (int[] array : arrays) {
//      System.out.println(Arrays.toString(array));
//    }




    getVariablesworker().removeIf(item->item==null);



//    System.out.println(index);
//    try {
//      Thread.sleep(999999);
//    } catch (InterruptedException e) {
//      throw new RuntimeException(e);
//    }


//    for (int[] anInt : ints) {
//      for (int i : anInt) {
//        System.out.print(i+" ");
//      }
//      System.out.println();
//    }
//    System.out.println();
//        try {
//      Thread.sleep(999999);
//    } catch (InterruptedException e) {
//      throw new RuntimeException(e);
//    }

    List<Integer> factory = getVariablesid();  //工厂向量
//    int[] machineSelection = encodeMachineSelectionEvenly(ints, factory);
    int[] machineSelection = encodeMachineSelectionEvenly(ints, factory);
    for (int i = 0; i < machineSelection.length; i++) {
      setVariableValuemachine(i,machineSelection[i]);
    }
//    System.out.println("工厂向量：");
//    System.out.println(getVariablesid());
//    System.out.println("机器向量：");
//    for (int i : machineSelection) {
//      System.out.print(i+" ");
//    }
//    System.out.println();
//
//            try {
//      Thread.sleep(999999);
//    } catch (InterruptedException e) {
//      throw new RuntimeException(e);
//    }
  }

  /** Copy Constructor */
  public DefaultIntegerPermutationSolution(DefaultIntegerPermutationSolution solution) {
    super(solution.problem) ;
    for (int i = 0; i < problem.getNumberOfObjectives(); i++) {
      setObjective(i, solution.getObjective(i)) ;
    }

    for (int i = 0; i < problem.getNumberOfVariables(); i++) {
      setVariableValue(i, solution.getVariableValue(i));          //工件向量
    }

    for (int i = 0; i < problem.getNumberOfVariables(); i++) {
      setVariableValueid(i, solution.getVariableValueid(i));        //工厂向量
    }

//    for (int i = 0 ; i < problem.getNumberOfVariables(); i++){
//      setVariableValueworker(i,solution.getVariableValueworker(i));
//    }




    //todo

    for (int i = 0 ; i < solution.getNumberOfVariablesworker(); i++){
      setVariableValueworker(i,solution.getVariableValueworker(i));
    }
    getVariablesworker().removeIf(item->item==null);
    attributes = new HashMap<Object, Object>(solution.attributes) ;
  }



  @Override public String getVariableValueString(int index) {
    return getVariableValue(index).toString();
  }

  @Override
  public DefaultIntegerPermutationSolution copy() {
    return new DefaultIntegerPermutationSolution(this);
  }

	@Override
	public Map<Object, Object> getAttributes() {
		return attributes;
	}


  public static void shuffleArray(int[] array) {
    Random random = new Random();
    for (int i = array.length - 1; i > 0; i--) {
      // 随机选择一个索引 j，使得 0 <= j <= i
      int j = random.nextInt(i + 1);

      // 交换 array[i] 和 array[j]
      int temp = array[i];
      array[i] = array[j];
      array[j] = temp;
    }
  }


  public static List<Integer> readfile(String s){
    List<Integer> numbers = new ArrayList<>();
    String filePath = s;
    String targetLine = "number of workers in each factory:";

    try (BufferedReader br = new BufferedReader(new FileReader(filePath))) {
      String line;
      boolean foundTargetLine = false;

      while ((line = br.readLine()) != null) {
        if (foundTargetLine) {
          String[] parts = line.split(",");
          for (String part : parts) {
            try {
              int number = Integer.parseInt(part.trim());
              numbers.add(number);
            } catch (NumberFormatException e) {
            }
          }
          break;
        }
        if (line.trim().equals(targetLine)) {
          foundTargetLine = true;
        }
      }
      if (!foundTargetLine) {
      } else {
        // 如果找到了目标行但没有下一行
        if (line == null) {
        }
      }
    } catch (IOException e) {
      e.printStackTrace();
    }
    return numbers;
  }


  public static int[][] readFactoryData(String filePath) {
    BufferedReader br = null;
    int[][] numberOfMachines = null;

    try {
      br = new BufferedReader(new FileReader(filePath));
      String firstLine = br.readLine();
      String[] firstLineParts = firstLine.split(" ");
      n = Integer.parseInt(firstLineParts[0]);  // 工厂数
      s = Integer.parseInt(firstLineParts[1]);  // 阶段数
      j = Integer.parseInt(firstLineParts[2]);  // 工件数

      numberOfMachines = new int[n][s];

      String line;
      boolean foundHeader = false;
      int factoryIndex = 0;  // 用于记录当前工厂的索引
      while ((line = br.readLine()) != null) {
        if (line.equals("number of machines at each stage in each factory:")) {
          foundHeader = true;
          continue;
        }

        if (foundHeader && factoryIndex < n) {
          String[] parts = line.split(",");
          for (int i = 0; i < parts.length; i++) {
            numberOfMachines[factoryIndex][i] = Integer.parseInt(parts[i].trim());
          }
          factoryIndex++;
        }
      }
    } catch (IOException e) {
      e.printStackTrace();
    } finally {
      if (br != null) {
        try {
          br.close();
        } catch (IOException e) {
          e.printStackTrace();
        }
      }
    }

    return numberOfMachines;
  }

  //工件均匀分布到机器
  public static int[]  encodeMachineSelectionEvenly(int[][] numberOfMachines, List<Integer> VariableValueid) {

    int j = VariableValueid.size();  // 工件数
    int s = 1;  // 阶段数，这里仍然假设只有一个阶段
    int[] machineSelection = new int[s * j];

    for (int stage = 0; stage < s; stage++) {
      Map<Integer, List<Integer>> factoryToJobs = new HashMap<>();

      // 分配工件到各工厂
      for (int job = 0; job < j; job++) {
        int factory = VariableValueid.get(job);
        factoryToJobs.computeIfAbsent(factory, k -> new ArrayList<>()).add(job);
      }



      // 对每个工厂进行均匀分配
      for (Map.Entry<Integer, List<Integer>> entry : factoryToJobs.entrySet()) {
        int factory = entry.getKey();
        List<Integer> jobs = entry.getValue();
        int machineCount = numberOfMachines[factory][stage];
        int jobsPerMachine = jobs.size() / machineCount;  // 每个机器应分配的标准工件数
        int extraJobs = jobs.size() % machineCount;  // 额外的工件数

        // 均匀分配工件到机器
        int machineIndex = 0;
        for (int i = 0; i < jobs.size(); i++) {
          if (i < extraJobs) {
            machineIndex = i % machineCount;  // 前extraJobs个工件分配时，每台机器多分配一个工件
          } else {
            machineIndex = (i - extraJobs) / jobsPerMachine;  // 剩余工件按照标准数量分配
          }
          int jobIndex = jobs.get(i);
          machineSelection[stage * j + jobIndex] = machineIndex;
        }
      }
    }

    return machineSelection;
  }

  //工件随机分布到机器
  public static int[] encodeMachineSelectionRandomly(int[][] numberOfMachines, List<Integer> factorySelection) {


    int j = factorySelection.size();  // 工件数
    int[] machineSelection = new int[j];
    Random random = new Random();

    for (int job = 0; job < j; job++) {
      int factory = factorySelection.get(job);  // 获取工件选择的工厂编号
      int machineCount = numberOfMachines[factory][0];  // 获取该工厂第一阶段的机器数

      if (machineCount == 0) {
        throw new IllegalArgumentException("Factory " + factory + " has no machines in the first stage.");
      }

      // 随机分配工件到该工厂的第一阶段的某台机器
      int assignedMachine = random.nextInt(machineCount);
      machineSelection[job] = assignedMachine;
    }
//    System.out.println(Arrays.toString(machineSelection));
//    System.out.println(factorySelection);
//
//    try {
//      Thread.sleep(999999);
//    } catch (InterruptedException e) {
//      throw new RuntimeException(e);
//    }


    return machineSelection;
  }

	//增加的部分（没用到，不需要）
  /*
  @Override
  public Integer getVariableValueid(int index) {
    return variablesid.get(index);
  }

  @Override
  public List<Integer> getVariablesid() {
    return variablesid;
  }

  @Override
  public void setVariableValueid(int index, Integer value) {
    variablesid.set(index, value);
  }

  @Override
  public int getNumberOfVariablesid() {
    return variablesid.size();
  }

   */

}
