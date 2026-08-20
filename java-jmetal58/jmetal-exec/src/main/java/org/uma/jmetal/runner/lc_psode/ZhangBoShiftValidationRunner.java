package org.uma.jmetal.runner.lc_psode;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.uma.jmetal.problem.multiobjective.dfsp.decoder.ProductionDecodeMode;
import org.uma.jmetal.problem.multiobjective.dfsp.fatigue.ZhangBoCanonicalProblemLoader;
import org.uma.jmetal.problem.multiobjective.dfsp.fatigue.ZhangBoCanonicalProductionProblem;
import org.uma.jmetal.problem.multiobjective.dfsp.fatigue.ZhangBoFatigueEvaluationResult;
import org.uma.jmetal.problem.multiobjective.dfsp.fatigue.ZhangBoFatigueOperationRecord;
import org.uma.jmetal.problem.multiobjective.dfsp.fatigue.shift.ZhangBoShiftConfiguration;
import org.uma.jmetal.problem.multiobjective.dfsp.fatigue.shift.ZhangBoShiftEvent;
import org.uma.jmetal.problem.multiobjective.dfsp.fatigue.shift.ZhangBoShiftSummary;
import org.uma.jmetal.problem.multiobjective.dfsp.model.Chapter4GoldenFixture;
import org.uma.jmetal.problem.multiobjective.dfsp.model.DhhfspFourVectorSolution;

/** Writes the immutable I1/X0 P8.4 shift evidence without starting an optimizer. */
public final class ZhangBoShiftValidationRunner {
  private static final long SEED = 20260808L;

  private ZhangBoShiftValidationRunner() { }

  public static void main(String[] args) throws Exception {
    Arguments parsed = Arguments.parse(args);
    run(parsed.projectRoot.toAbsolutePath().normalize(),
        parsed.output.toAbsolutePath().normalize());
  }

  static void run(Path projectRoot, Path output) throws Exception {
    requireEmpty(output);
    Files.createDirectories(output);
    Path bridge = projectRoot.resolve("java-jmetal58/p8-bridge/v1");
    Path instance = bridge.resolve("EADHFSP/10_2_2_1.txt");
    Path extensions = bridge.resolve("instance-extensions/v1");
    Path fatigue = bridge.resolve("fatigue-parameters/v1");
    ZhangBoShiftConfiguration shift =
        ZhangBoShiftConfiguration.formalLeftRight().withFullTrace(true);
    ZhangBoCanonicalProductionProblem baseProblem = ZhangBoCanonicalProblemLoader.load(
        instance, ProductionDecodeMode.FM3, SEED, extensions, fatigue,
        ZhangBoShiftConfiguration.none());
    ZhangBoCanonicalProductionProblem shiftProblem = ZhangBoCanonicalProblemLoader.load(
        instance, ProductionDecodeMode.FM3, SEED, extensions, fatigue, shift);
    Chapter4GoldenFixture fixture = Chapter4GoldenFixture.load();
    DhhfspFourVectorSolution baseSolution = x0(fixture);
    DhhfspFourVectorSolution shiftSolution = x0(fixture);
    baseProblem.evaluate(baseSolution);
    shiftProblem.evaluate(shiftSolution);
    ZhangBoFatigueEvaluationResult base = result(baseSolution);
    ZhangBoFatigueEvaluationResult shifted = result(shiftSolution);
    ZhangBoShiftSummary summary = shifted.getShiftSummary();
    if (summary == null) throw new IllegalStateException("LEFT_RIGHT result has no shift summary");
    if (baseProblem.getEvaluationCounter().getSuccessfulEvaluations() != 1L
        || shiftProblem.getEvaluationCounter().getSuccessfulEvaluations() != 1L) {
      throw new IllegalStateException("Each external decoder call must consume exactly one FE");
    }

    writeSchedule(output.resolve("S0_base_schedule.csv"), summary.getBaseOperations());
    writeSchedule(output.resolve("S1_after_left_shift.csv"), summary.getAfterLeftOperations());
    writeSchedule(output.resolve("S2_after_right_shift.csv"), shifted.getOperations());
    writeEvents(output.resolve("shift_candidates.csv"), summary.getEvents());
    writeObjectives(output.resolve("objectives_and_fatigue.csv"), base, shifted, summary);
    writeManualComparison(output.resolve("manual_repropagation_check.csv"),
        base, summary.getBaseOperations());
    writeGantt(output.resolve("figure12_S0_base.svg"), "Figure 12 - I1/X0 S0 base FM3",
        summary.getBaseOperations());
    writeGantt(output.resolve("figure13_S1_after_FCLS.svg"),
        "Figure 13 - I1/X0 S1 after FCLS (" + summary.getLeftAccepted()
            + " accepted)", summary.getAfterLeftOperations());
    writeGantt(output.resolve("figure14_S2_after_FCRS.svg"),
        "Figure 14 - I1/X0 S2 after FCRS", shifted.getOperations());

    boolean illustrationGate = summary.getLeftAccepted() > 0 && summary.getRightAccepted() > 0;
    String report = "# P8.6 I1/X0 Common-Gap Shift Validation\n\n"
        + "- semantic version: `" + ZhangBoShiftConfiguration.ALGORITHM_SEMANTICS_VERSION + "`\n"
        + "- mode: `LEFT_RIGHT`\n- seed: `" + SEED + "`\n"
        + "- FCLS: " + summary.getLeftAccepted() + " accepted / "
        + summary.getLeftCandidates() + " evaluated candidates\n"
        + "- FCRS: " + summary.getRightAccepted() + " accepted / "
        + summary.getRightCandidates() + " evaluated candidates\n"
        + "- action trace: `" + summary.getEventSha256() + "`\n"
        + "- final schedule: `" + summary.getFinalScheduleSha256() + "`\n"
        + "- evaluation trace: `" + summary.getEvaluationTraceSha256() + "`\n"
        + "- external FE: S0=1, LEFT_RIGHT=1; internal propagation adds no FE\n\n"
        + "## Honest acceptance result\n\n"
        + (illustrationGate
            ? "I1/X0 contains accepted FCLS and FCRS events.\n"
            : "`I1/X0` does **not** pass the two-direction illustration gate. Figure 13 therefore "
                + "truthfully shows `S1=S0`; no left-shift example is fabricated.\n")
        + "\nFCLS uses the earliest feasible machine-worker common gap and only requires Cmax "
        + "not to worsen. FCRS preserves the frozen post-left Cmax and requires TEC or TWC gain.\n";
    Files.write(output.resolve("SHIFT_VALIDATION.md"), report.getBytes(StandardCharsets.UTF_8));
    String manifest = "schemaVersion=2\nalgorithmSemanticsVersion="
        + ZhangBoShiftConfiguration.ALGORITHM_SEMANTICS_VERSION + "\nshiftMode=LEFT_RIGHT\n"
        + "seed=" + SEED + "\ninstanceSha256=" + base.getInstanceSha256() + "\n"
        + "instanceExtensionSha256=" + base.getInstanceExtensionSha256() + "\n"
        + "fatigueParametersSha256=" + base.getConfigurationSha256() + "\n"
        + "leftCandidates=" + summary.getLeftCandidates() + "\nleftAccepted="
        + summary.getLeftAccepted() + "\nrightCandidates=" + summary.getRightCandidates()
        + "\nrightAccepted=" + summary.getRightAccepted() + "\nillustrationGate="
        + illustrationGate + "\ncmaxStar=" + number(summary.getCmaxStar())
        + "\ninternalPropagations=" + summary.getInternalPropagations()
        + "\nactionTraceSha256=" + summary.getEventSha256() + "\n"
        + "baseScheduleSha256=" + summary.getBaseScheduleSha256() + "\n"
        + "afterLeftScheduleSha256=" + summary.getAfterLeftScheduleSha256() + "\n"
        + "finalScheduleSha256=" + summary.getFinalScheduleSha256() + "\n"
        + "evaluationTraceSha256=" + summary.getEvaluationTraceSha256() + "\n";
    Files.write(output.resolve("manifest.properties"), manifest.getBytes(StandardCharsets.UTF_8));
    writeEvidenceHashes(output);
    System.out.println("P8.4_I1_SHIFT left=" + summary.getLeftAccepted() + "/"
        + summary.getLeftCandidates() + " right=" + summary.getRightAccepted() + "/"
        + summary.getRightCandidates() + " illustrationGate=" + illustrationGate);
  }

  private static DhhfspFourVectorSolution x0(Chapter4GoldenFixture fixture) {
    DhhfspFourVectorSolution source = fixture.createSolution();
    return new DhhfspFourVectorSolution(source.getJobSequence(), source.getFactoryAssignments(),
        source.getMachineAssignments(), source.getWorkerAssignments(),
        ProductionDecodeMode.FM3.getSemanticTag(),
        ZhangBoCanonicalProductionProblem.NUMBER_OF_OBJECTIVES);
  }

  private static ZhangBoFatigueEvaluationResult result(DhhfspFourVectorSolution solution) {
    Object value = solution.getAttribute(ZhangBoFatigueEvaluationResult.class);
    if (!(value instanceof ZhangBoFatigueEvaluationResult)) {
      throw new IllegalStateException("Missing canonical fatigue result");
    }
    return (ZhangBoFatigueEvaluationResult) value;
  }

  private static void writeSchedule(Path path, List<ZhangBoFatigueOperationRecord> operations)
      throws IOException {
    StringBuilder out = new StringBuilder("sequence,job,stage,factory,machine,worker,start,end,"
        + "recoveryDuration,fatigueAtStart,baseProcessing,baseSetup,actualDuration,fatigueAfter,energy,cost\n");
    for (ZhangBoFatigueOperationRecord op : operations) {
      out.append(op.sequence).append(',').append(op.job).append(',').append(op.stage).append(',')
          .append(op.factory).append(',').append(op.machine).append(',').append(op.worker)
          .append(',').append(number(op.start)).append(',').append(number(op.end)).append(',')
          .append(number(op.recoveryDuration)).append(',').append(number(op.fatigueAtStart))
          .append(',').append(number(op.baseProcessingDuration)).append(',')
          .append(number(op.baseSetupDuration)).append(',').append(number(op.actualDuration))
          .append(',').append(number(op.fatigueAfter)).append(',').append(number(op.energy))
          .append(',').append(number(op.cost)).append('\n');
    }
    Files.write(path, out.toString().getBytes(StandardCharsets.UTF_8));
  }

  private static void writeEvents(Path path, List<ZhangBoShiftEvent> events) throws IOException {
    StringBuilder out = new StringBuilder("phase,job,stage,machineSlot,workerSlot,"
        + "commonGapLeft,commonGapRight,oldStart,newStart,oldFatigueAtStart,"
        + "newFatigueAtStart,oldDuration,newDuration,oldEnd,newEnd,oldCmax,newCmax,"
        + "oldTEC,newTEC,oldTWC,newTWC,CmaxStar,proposalShift,acceptedShift,"
        + "backtrackingAttempt,accepted,reason\n");
    for (ZhangBoShiftEvent event : events) out.append(event.canonicalLine()).append('\n');
    Files.write(path, out.toString().getBytes(StandardCharsets.UTF_8));
  }

  private static void writeObjectives(Path path, ZhangBoFatigueEvaluationResult base,
      ZhangBoFatigueEvaluationResult shifted, ZhangBoShiftSummary summary) throws IOException {
    double[] s0 = summary.getBaseObjectives();
    double[] s1 = summary.getAfterLeftObjectives();
    double[] s2 = summary.getFinalObjectives();
    org.uma.jmetal.problem.multiobjective.dfsp.fatigue.ZhangBoFatigueMetrics s1Metrics =
        summary.getAfterLeftMetrics();
    String text = "metric,S0,S1,S2\n"
        + row("Cmax", s0[0], s1[0], s2[0]) + row("TEC", s0[1], s1[1], s2[1])
        + row("TWC", s0[6], s1[6], s2[6])
        + row("Fmax", base.getMetrics().maximumFatigue,
            s1Metrics.maximumFatigue, shifted.getMetrics().maximumFatigue)
        + row("Favg", base.getMetrics().averageEventFatigue,
            s1Metrics.averageEventFatigue, shifted.getMetrics().averageEventFatigue)
        + row("FE", base.getMetrics().fatigueExcessIntegral,
            s1Metrics.fatigueExcessIntegral, shifted.getMetrics().fatigueExcessIntegral);
    Files.write(path, text.getBytes(StandardCharsets.UTF_8));
  }

  private static String row(String name, double s0, double s1, double s2) {
    return name + ',' + number(s0) + ',' + number(s1) + ',' + number(s2) + "\n";
  }

  private static void writeManualComparison(Path path, ZhangBoFatigueEvaluationResult base,
      List<ZhangBoFatigueOperationRecord> propagated) throws IOException {
    List<ZhangBoFatigueOperationRecord> expected = base.getOperations();
    StringBuilder out = new StringBuilder("job,stage,programStart,repropStart,startAbsError,"
        + "programEnd,repropEnd,endAbsError,pass\n");
    for (int index = 0; index < expected.size(); index++) {
      ZhangBoFatigueOperationRecord a = expected.get(index);
      ZhangBoFatigueOperationRecord b = propagated.get(index);
      double startError = Math.abs(a.start - b.start);
      double endError = Math.abs(a.end - b.end);
      out.append(a.job).append(',').append(a.stage).append(',').append(number(a.start))
          .append(',').append(number(b.start)).append(',').append(number(startError)).append(',')
          .append(number(a.end)).append(',').append(number(b.end)).append(',')
          .append(number(endError)).append(',').append(startError <= 1e-9 && endError <= 1e-9)
          .append('\n');
    }
    Files.write(path, out.toString().getBytes(StandardCharsets.UTF_8));
  }

  private static void writeGantt(Path path, String title,
      List<ZhangBoFatigueOperationRecord> operations) throws IOException {
    List<String> resources = new ArrayList<>();
    double makespan = 0.0;
    for (ZhangBoFatigueOperationRecord op : operations) {
      String resource = "F" + (op.factory + 1) + "-S" + (op.stage + 1) + "-M" + (op.machine + 1);
      if (!resources.contains(resource)) resources.add(resource);
      makespan = Math.max(makespan, op.end);
    }
    Collections.sort(resources);
    int width = 1200;
    int left = 150;
    int rowHeight = 44;
    int height = 90 + rowHeight * resources.size();
    double scale = (width - left - 30.0) / Math.max(1.0, makespan);
    StringBuilder svg = new StringBuilder();
    svg.append("<svg xmlns=\"http://www.w3.org/2000/svg\" width=\"").append(width)
        .append("\" height=\"").append(height).append("\" viewBox=\"0 0 ")
        .append(width).append(' ').append(height).append("\">\n")
        .append("<rect width=\"100%\" height=\"100%\" fill=\"white\"/>")
        .append("<text x=\"20\" y=\"30\" font-family=\"sans-serif\" font-size=\"18\">")
        .append(escape(title)).append("</text>\n");
    for (int row = 0; row < resources.size(); row++) {
      int y = 55 + row * rowHeight;
      svg.append("<text x=\"10\" y=\"").append(y + 22)
          .append("\" font-family=\"sans-serif\" font-size=\"12\">")
          .append(resources.get(row)).append("</text>")
          .append("<line x1=\"").append(left).append("\" y1=\"").append(y + 30)
          .append("\" x2=\"").append(width - 20).append("\" y2=\"").append(y + 30)
          .append("\" stroke=\"#ddd\"/>\n");
    }
    String[] colors = {"#4C78A8", "#F58518", "#54A24B", "#E45756", "#72B7B2",
        "#B279A2", "#FF9DA6", "#9D755D", "#BAB0AC", "#EDC948"};
    for (ZhangBoFatigueOperationRecord op : operations) {
      String resource = "F" + (op.factory + 1) + "-S" + (op.stage + 1) + "-M" + (op.machine + 1);
      int row = resources.indexOf(resource);
      double x = left + op.start * scale;
      double w = Math.max(1.0, op.actualDuration * scale);
      int y = 55 + row * rowHeight;
      svg.append("<rect x=\"").append(number(x)).append("\" y=\"").append(y)
          .append("\" width=\"").append(number(w)).append("\" height=\"28\" fill=\"")
          .append(colors[op.job % colors.length]).append("\" stroke=\"#222\"/>")
          .append("<text x=\"").append(number(x + 3)).append("\" y=\"").append(y + 18)
          .append("\" font-family=\"sans-serif\" font-size=\"10\" fill=\"white\">J")
          .append(op.job + 1).append("/W").append(op.worker + 1).append("</text>\n");
    }
    svg.append("</svg>\n");
    Files.write(path, svg.toString().getBytes(StandardCharsets.UTF_8));
  }

  private static String escape(String value) {
    return value.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
  }

  private static String number(double value) {
    return String.format(Locale.ROOT, "%.12f", value);
  }

  private static void requireEmpty(Path output) throws IOException {
    if (!Files.exists(output)) return;
    try (Stream<Path> children = Files.list(output)) {
      if (children.findAny().isPresent()) {
        throw new IllegalStateException("Refusing to overwrite non-empty shift evidence: " + output);
      }
    }
  }

  private static void writeEvidenceHashes(Path root) throws Exception {
    List<Path> files;
    try (Stream<Path> paths = Files.walk(root)) {
      files = paths.filter(Files::isRegularFile)
          .filter(path -> !path.getFileName().toString().equals("evidence-sha256.tsv"))
          .sorted().collect(Collectors.toList());
    }
    StringBuilder out = new StringBuilder("sha256\tbytes\trelativePath\n");
    for (Path file : files) {
      out.append(sha256(file)).append('\t').append(Files.size(file)).append('\t')
          .append(root.relativize(file).toString().replace('\\', '/')).append('\n');
    }
    Files.write(root.resolve("evidence-sha256.tsv"), out.toString().getBytes(StandardCharsets.UTF_8));
  }

  private static String sha256(Path file) throws IOException {
    try {
      MessageDigest digest = MessageDigest.getInstance("SHA-256");
      digest.update(Files.readAllBytes(file));
      StringBuilder out = new StringBuilder();
      for (byte value : digest.digest()) out.append(String.format("%02x", value & 0xff));
      return out.toString();
    } catch (NoSuchAlgorithmException exception) {
      throw new IllegalStateException(exception);
    }
  }

  private static final class Arguments {
    private final Path projectRoot;
    private final Path output;
    private Arguments(Path projectRoot, Path output) {
      this.projectRoot = projectRoot;
      this.output = output;
    }
    private static Arguments parse(String[] args) {
      Path project = Paths.get(".");
      Path output = Paths.get("paper_evidence/I1/10_common_gap_shift_validation");
      for (int index = 0; index < args.length; index++) {
        if ("--project-root".equals(args[index]) && index + 1 < args.length) {
          project = Paths.get(args[++index]);
        } else if ("--output".equals(args[index]) && index + 1 < args.length) {
          output = Paths.get(args[++index]);
        } else {
          throw new IllegalArgumentException("Usage: --project-root <path> --output <path>");
        }
      }
      return new Arguments(project, output);
    }
  }
}
