package org.uma.jmetal.runner.lc_psode;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;
import org.uma.jmetal.algorithm.multiobjective.mypso.p8.P8AblationProfile;
import org.uma.jmetal.algorithm.multiobjective.mypso.p8.P8ExperimentRegistry;
import org.uma.jmetal.algorithm.multiobjective.mypso.p8.P8ExperimentSpec;
import org.uma.jmetal.algorithm.multiobjective.mypso.p8.P8InitialPopulationProvider;
import org.uma.jmetal.algorithm.multiobjective.mypso.p8.P8RunStatus;
import org.uma.jmetal.algorithm.multiobjective.mypso.zhangbo.audit.ZhangBoCmaxAudit;
import org.uma.jmetal.problem.multiobjective.dfsp.decoder.ProductionDecodeMode;
import org.uma.jmetal.problem.multiobjective.dfsp.fatigue.ZhangBoCanonicalProblemLoader;
import org.uma.jmetal.problem.multiobjective.dfsp.fatigue.ZhangBoCanonicalProductionProblem;
import org.uma.jmetal.solution.PermutationSolution;

/** Scope-locked observation-only FULL Cmax diagnostic on 20_2_3_1 / seed 20260808. */
public final class ZhangBoCmaxAuditRunner {
  private static final String INSTANCE_FILE = "20_2_3_1.txt";
  private static final String EXTENSION_FILE = "20_2_3_1.properties";
  private static final String FATIGUE_FILE = "20_2_3_1.properties";
  private static final String INSTANCE_NAME = "20_2_3_1";
  private static final long SEED = 20260808L;
  private static final int CHECKPOINT_FE = 1000;

  private ZhangBoCmaxAuditRunner() { }

  public static void main(String[] args) throws Exception {
    if (args.length != 3) {
      throw new IllegalArgumentException("Usage: <project-root> <output-directory> <20000|100000>");
    }
    Path project = Paths.get(args[0]).toAbsolutePath().normalize();
    Path output = Paths.get(args[1]).toAbsolutePath().normalize();
    int maxFEs = Integer.parseInt(args[2]);
    if (maxFEs != 20000 && maxFEs != 100000) {
      throw new IllegalArgumentException("Cmax audit is locked to 20000 or 100000 FE");
    }
    if (Files.exists(output)) {
      throw new IllegalStateException("Cmax audit output already exists: " + output);
    }
    Files.createDirectories(output);
    run(project, output, maxFEs);
  }

  static ZhangBoP9FormalRunResult run(Path project, Path output, int maxFEs) throws Exception {
    Path instance = project.resolve("EADHFSP").resolve(INSTANCE_FILE);
    Path extensionDirectory = project.resolve("instance-extensions/v1");
    Path fatigueDirectory = project.resolve("fatigue-parameters/v1");
    P8ExperimentSpec source = P8ExperimentRegistry.find("FULL");
    P8ExperimentSpec spec = new P8ExperimentSpec(source.getMatrix(), "ZHANGBO-FULL-CMAX-AUDIT",
        source.getMechanism(), source.getConfigurationKey() + "-cmax-audit",
        source.getAblationProfile(), P8RunStatus.COMPLETED,
        "Observation-only Cmax lifecycle audit", 100, maxFEs,
        new int[] {20, 40, 20, 20});
    ZhangBoP9FormalParameters parameters = ZhangBoP9FormalParameters.formalAudit(SEED, maxFEs);
    ZhangBoCanonicalProductionProblem problem = ZhangBoCanonicalProblemLoader.load(
        instance, ProductionDecodeMode.FM3, SEED, extensionDirectory, fatigueDirectory,
        source.getAblationProfile().getShiftConfiguration());
    List<PermutationSolution<Integer>> initial = new ArrayList<>();
    for (int index = 0; index < parameters.getPopulation(); index++) {
      initial.add(problem.createSolution());
    }
    String initialHash = P8InitialPopulationProvider.sha256(initial);
    ZhangBoCmaxAudit audit = new ZhangBoCmaxAudit(CHECKPOINT_FE);
    ZhangBoP9FormalRunResult result = ZhangBoP9FormalExecutor.execute(
        "ZHANGBO-FULL-CMAX-AUDIT", "FULL", spec, parameters, INSTANCE_NAME,
        sha256(instance), problem, P8InitialPopulationProvider.copy(initial), null, audit);
    if (result.record.getStatus() != P8RunStatus.COMPLETED) {
      throw new IllegalStateException("Cmax audit run failed: " + result.record.getReason());
    }
    if (result.record.getFullEvaluations() > maxFEs || result.record.getFront().isEmpty()) {
      throw new IllegalStateException("Cmax audit FE/front gate failed");
    }
    Files.write(output.resolve("cmax-curves.csv"), audit.curvesCsv().getBytes(StandardCharsets.UTF_8));
    Files.write(output.resolve("cmax-record-lifecycle.csv"),
        audit.recordsCsv().getBytes(StandardCharsets.UTF_8));
    Files.write(output.resolve("cmax-audit-summary.txt"),
        (audit.summaryText() + "initialPopulationSha256=" + initialHash + "\n"
            + "mechanismVectorHash=" + source.getMechanismVectorHash() + "\n"
            + "algorithmSemantics=" + P8AblationProfile.VERSION + "\n")
            .getBytes(StandardCharsets.UTF_8));
    Files.write(output.resolve("mechanism-summary.txt"),
        result.mechanismSummary.getBytes(StandardCharsets.UTF_8));
    Files.write(output.resolve("front.csv"), frontCsv(result).getBytes(StandardCharsets.UTF_8));
    Files.write(output.resolve("Cmax_AUDIT_REPORT.md"),
        report(result, audit, maxFEs).getBytes(StandardCharsets.UTF_8));
    writeHashes(output);
    System.out.println("CMAX_AUDIT_COMPLETED fe=" + result.record.getFullEvaluations()
        + " records=" + audit.getRecords().size() + " output=" + output);
    return result;
  }

  private static String frontCsv(ZhangBoP9FormalRunResult result) {
    StringBuilder out = new StringBuilder("Cmax,TEC,TWC\n");
    for (double[] point : result.record.getFront()) {
      out.append(point[0]).append(',').append(point[1]).append(',').append(point[2]).append('\n');
    }
    return out.toString();
  }

  private static String report(ZhangBoP9FormalRunResult result,
      ZhangBoCmaxAudit audit, int maxFEs) {
    List<ZhangBoCmaxAudit.Record> records = audit.getRecords();
    int pddr = 0;
    int archive = 0;
    int next = 0;
    for (ZhangBoCmaxAudit.Record record : records) {
      if (record.isPddrRetained()) pddr++;
      if (record.isPersonalArchive() || record.isGlobalArchive()) archive++;
      if (record.getSurvival() == ZhangBoCmaxAudit.Survival.YES) next++;
    }
    double finalCmax = Double.POSITIVE_INFINITY;
    for (double[] point : result.record.getFront()) finalCmax = Math.min(finalCmax, point[0]);
    return "# Cmax Audit 小规模诊断\n\n"
        + "## 技术摘要\n\n"
        + "本运行只增加旁路观测，不改变算法决策。实例为`20_2_3_1`，seed为`20260808`，"
        + "种群100，预算" + maxFEs + " FE，每1000 FE保存一次曲线。\n\n"
        + "- 最终非支配前沿最小Cmax：`" + finalCmax + "`。\n"
        + "- 产生新的历史Cmax纪录：`" + records.size() + "`次。\n"
        + "- PDDR保留：`" + pddr + "`次；进入个人/全局档案：`" + archive
        + "`次；下一轮仍存活：`" + next + "`次。\n\n"
        + "## 指标定义\n\n"
        + "- `BestCmaxGlobal`：截至检查点，全局非支配历史中的最小Cmax。\n"
        + "- `BestCmaxG1`：截至检查点，G1曾观察到的历史最小Cmax。\n"
        + "- `CurrentBestCmaxG1`：检查点时仍在G1子群中的最小Cmax。\n"
        + "- `BestCmaxGenerated`：截至检查点，所有已完整评价候选的历史最小Cmax。\n"
        + "- `BestCmaxSurvived`：截至检查点，至少被PDDR保留一次的纪录最小Cmax。\n\n"
        + "## 证据文件\n\n"
        + "- `cmax-curves.csv`：1000 FE粒度曲线。\n"
        + "- `cmax-record-lifecycle.csv`：纪录来源、候选集、PDDR、档案和下一轮存活。\n"
        + "- `mechanism-summary.txt`：机制触发与FE闭合。\n\n"
        + "## 限制\n\n"
        + "该结果是单实例、单seed的诊断实验，不构成正式统计或算法优越性结论。\n";
  }

  private static void writeHashes(Path output) throws IOException {
    StringBuilder out = new StringBuilder("sha256\tbytes\tpath\n");
    try (Stream<Path> stream = Files.list(output)) {
      List<Path> files = new ArrayList<>();
      stream.filter(Files::isRegularFile).sorted().forEach(files::add);
      for (Path file : files) {
        if (file.getFileName().toString().equals("evidence-sha256.tsv")) continue;
        out.append(sha256(file)).append('\t').append(Files.size(file)).append('\t')
            .append(file.getFileName()).append('\n');
      }
    }
    Files.write(output.resolve("evidence-sha256.tsv"), out.toString().getBytes(StandardCharsets.UTF_8));
  }

  private static String sha256(Path path) throws IOException {
    try {
      MessageDigest digest = MessageDigest.getInstance("SHA-256");
      byte[] bytes = Files.readAllBytes(path);
      byte[] hash = digest.digest(bytes);
      StringBuilder out = new StringBuilder();
      for (byte value : hash) out.append(String.format("%02x", value & 0xff));
      return out.toString();
    } catch (NoSuchAlgorithmException exception) {
      throw new IllegalStateException("SHA-256 unavailable", exception);
    }
  }
}
