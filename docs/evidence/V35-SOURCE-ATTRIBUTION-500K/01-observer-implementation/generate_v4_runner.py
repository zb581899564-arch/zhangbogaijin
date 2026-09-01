# -*- coding: utf-8 -*-
"""Generates V35ObserverGateRunner.java from V3 runner (one-shot script)."""
import io

SRC = r"E:\学习\李明哲-毕业材料\张博改进\docs\evidence\V35-GAP-SOURCE-CONTRIBUTION-DIAGNOSTICS-V1\01-implementation\src\org\uma\jmetal\runner\lc_psode\V35SourceDiagnosticRunner.java"
OUT = r"E:\学习\李明哲-毕业材料\张博改进\docs\evidence\V35-SOURCE-ATTRIBUTION-500K\01-observer-implementation\src\org\uma\jmetal\runner\lc_psode\V35ObserverGateRunner.java"

t = io.open(SRC, encoding="utf-8").read()

repl = [
    ("public final class V35SourceDiagnosticRunner {",
     "public final class V35ObserverGateRunner {"),
    ('public static final String VERSION = "v35-source-diagnostic-runner-v3";',
     'public static final String VERSION = "v35-source-attribution-observer-runner-v4";'),
    ("private V35SourceDiagnosticRunner() { }",
     "private V35ObserverGateRunner() { }"),
    ("jmetal-algorithm-5.8-V35-SOURCE-DIAGNOSTICS-V3.jar",
     "jmetal-algorithm-5.8-V35-SOURCE-ATTRIBUTION-OBSERVER-V4.jar"),
    ('String runId = "GAPLSRC-"', 'String runId = "SAOBS-"'),
    ("import org.uma.jmetal.algorithm.multiobjective.mypso.v35.V35SourceLedgerHook;",
     "import org.uma.jmetal.algorithm.multiobjective.mypso.v35.V35SourceAttributionObserver;\n"
     "import org.uma.jmetal.algorithm.multiobjective.mypso.v35.V35SourceAttributionObserver.RunnerMemorySampler;"),
    ("V35_SOURCE_DIAGNOSTIC_COMPLETED", "V35_OBSERVER_GATE_COMPLETED"),
]
for old, new in repl:
    if old not in t:
        raise RuntimeError("NOT FOUND: %s" % old[:60])
    t = t.replace(old, new)

# Hook replacements
hook_pairs = [
    ("V35SourceLedgerHook.arm()",
     "V35SourceAttributionObserver.arm(runId, value.instance,\n"
     "            String.valueOf(value.seed), label.cliAlias());\n"
     "        V35SourceAttributionObserver.attach(problem)"),
    ("V35SourceLedgerHook.getEvaluationLedgerCsv()",
     "V35SourceAttributionObserver.getEvaluationLedgerCsv()"),
    ("V35SourceLedgerHook.getPddrLedgerCsv()",
     "V35SourceAttributionObserver.getPddrLedgerCsv()"),
    ("V35SourceLedgerHook.getPddrRoundCount()",
     "V35SourceAttributionObserver.getPddrRounds()"),
    ("V35SourceLedgerHook.getErrorCount()",
     "V35SourceAttributionObserver.getErrorCount()"),
    ("V35SourceLedgerHook.getLastError()",
     "V35SourceAttributionObserver.getLastError()"),
    ("V35SourceLedgerHook.getUnsetSourceRows()",
     "V35SourceAttributionObserver.getUnknownSourceEvents()"),
    ("V35SourceLedgerHook.disarm()",
     "V35SourceAttributionObserver.disarm()"),
]
for old, new in hook_pairs:
    t = t.replace(old, new)

# Completeness gate
old_gate = 'value(record.getMechanismSummary(), "formalOuterCycles"));'
new_gate = old_gate + "\n" + \
    "        gateCompleteness(failures,\n" + \
    "            V35SourceAttributionObserver.getDroppedEvents(),\n" + \
    "            V35SourceAttributionObserver.getUnknownSourceEvents(),\n" + \
    "            V35SourceAttributionObserver.getInvalidObjectiveRows(),\n" + \
    "            V35SourceAttributionObserver.getDuplicateCandidateEventRows(),\n" + \
    "            V35SourceAttributionObserver.getBoundedCapacityViolations(),\n" + \
    "            V35SourceAttributionObserver.getB0Captured(),\n" + \
    "            V35SourceAttributionObserver.getCheckpointFronts().size());"
t = t.replace(old_gate, new_gate, 1)

# Memory summary call
old_led = "writeSourceLedgers(partial, evaluationLedger, pddrLedger);"
new_led = old_led + "\n" + "        writeMemorySummary(partial, sampler, wallNanos);"
t = t.replace(old_led, new_led, 1)

# Sampler lifecycle
old_try = "    try {"
new_try = ("    RunnerMemorySampler sampler = new RunnerMemorySampler();\n"
           "    try {\n      sampler.start();")
t = t.replace(old_try, new_try, 1)

old_disarm = "V35SourceAttributionObserver.disarm();"
t = t.replace(old_disarm, "sampler.stop();\n      " + old_disarm)

# Methods + inner class
anchor = "  private static void writeBudget("
addition = (
    "  /** Observer completeness gate (task 15). */\n"
    "  private static void gateCompleteness(List<String> failures,\n"
    "      long droppedEvents, long unknownSourceEvents, long invalidObjectiveRows,\n"
    "      long duplicateCandidateEventRows, long boundedCapacityViolations,\n"
    "      long b0Captured, int checkpointCount) {\n"
    "    if (droppedEvents != 0L) failures.add(\"droppedEvents=\" + droppedEvents);\n"
    "    if (unknownSourceEvents != 0L) failures.add(\"unknownSourceEvents=\" + unknownSourceEvents);\n"
    "    if (invalidObjectiveRows != 0L) failures.add(\"invalidObjectiveRows=\" + invalidObjectiveRows);\n"
    "    if (duplicateCandidateEventRows != 0L) failures.add(\"duplicateCandidateEventRows=\" + duplicateCandidateEventRows);\n"
    "    if (boundedCapacityViolations != 0L) failures.add(\"boundedCapacityViolations=\" + boundedCapacityViolations);\n"
    "    if (b0Captured == 0L) failures.add(\"B0Missing\");\n"
    "    if (checkpointCount < 1) failures.add(\"noCheckpointsCaptured\");\n"
    "  }\n"
    "\n"
    "  /** Memory/GC summary writer. */\n"
    "  private static void writeMemorySummary(Path dir, RunnerMemorySampler sampler,\n"
    "      long wallNanos) throws IOException {\n"
    "    String NL = \"\\n\";\n"
    "    StringBuilder text = new StringBuilder();\n"
    "    text.append(\"heapUsedPeak=\").append(sampler.heapUsedPeak()).append(NL);\n"
    "    text.append(\"heapCommittedPeak=\").append(sampler.heapCommittedPeak()).append(NL);\n"
    "    text.append(\"gcCollectionCount=\").append(sampler.gcCollectionCount()).append(NL);\n"
    "    text.append(\"gcCollectionTime=\").append(sampler.gcCollectionTime()).append(NL);\n"
    "    text.append(\"runWallClockNanos=\").append(wallNanos).append(NL);\n"
    "    text.append(\"samples=\").append(sampler.sampleCount()).append(NL);\n"
    "    Files.write(dir.resolve(\"memory-summary.properties\"),\n"
    "        text.toString().getBytes(StandardCharsets.UTF_8));\n"
    "  }\n"
    "\n"
    "  /** Memory/GC sampler (identical on OFF and ON). */\n"
    "  static final class RunnerMemorySampler implements Runnable {\n"
    "    private final java.lang.management.MemoryMXBean memory =\n"
    "        java.lang.management.ManagementFactory.getMemoryMXBean();\n"
    "    private final List<java.lang.management.GarbageCollectorMXBean> gcs =\n"
    "        java.lang.management.ManagementFactory.getGarbageCollectorMXBeans();\n"
    "    private volatile boolean running = true;\n"
    "    private volatile long heapUsedPeak = 0L;\n"
    "    private volatile long heapCommittedPeak = 0L;\n"
    "    private volatile long samples = 0L;\n"
    "    private Thread worker;\n"
    "    void start() {\n"
    "      worker = new Thread(this, \"observer-memory-sampler\");\n"
    "      worker.setDaemon(true);\n"
    "      worker.start();\n"
    "    }\n"
    "    void stop() {\n"
    "      running = false;\n"
    "      try { worker.join(2000L); } catch (InterruptedException ignored) { }\n"
    "      sample();\n"
    "    }\n"
    "    public void run() {\n"
    "      while (running) {\n"
    "        sample();\n"
    "        try { Thread.sleep(100L); } catch (InterruptedException ignored) { return; }\n"
    "      }\n"
    "    }\n"
    "    private void sample() {\n"
    "      java.lang.management.MemoryUsage usage = memory.getHeapMemoryUsage();\n"
    "      heapUsedPeak = Math.max(heapUsedPeak, usage.getUsed());\n"
    "      heapCommittedPeak = Math.max(heapCommittedPeak, usage.getCommitted());\n"
    "      samples++;\n"
    "    }\n"
    "    long heapUsedPeak() { return heapUsedPeak; }\n"
    "    long heapCommittedPeak() { return heapCommittedPeak; }\n"
    "    long sampleCount() { return samples; }\n"
    "    long gcCollectionCount() {\n"
    "      long total = 0L;\n"
    "      for (java.lang.management.GarbageCollectorMXBean bean : gcs) total += bean.getCollectionCount();\n"
    "      return total;\n"
    "    }\n"
    "    long gcCollectionTime() {\n"
    "      long total = 0L;\n"
    "      for (java.lang.management.GarbageCollectorMXBean bean : gcs) total += bean.getCollectionTime();\n"
    "      return total;\n"
    "    }\n"
    "  }\n"
    "\n"
    "  private static void writeBudget(")
t = t.replace(anchor, addition, 1)

io.open(OUT, "w", encoding="utf-8", newline="\n").write(t)
print("V35ObserverGateRunner written:", len(t.splitlines()), "lines")
