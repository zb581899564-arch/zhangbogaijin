# -*- coding: utf-8 -*-
"""Runner streaming adaptation (phase 2 of the streaming fix)."""
import io

P = "src/org/uma/jmetal/runner/lc_psode/V35ObserverGateRunner.java"
t = io.open(P, encoding="utf-8").read()

# 1) arm: open pddr ledger files
old1 = ("        V35SourceAttributionObserver.attach(problem);")
new1 = ("        V35SourceAttributionObserver.attach(problem);\n"
        "        V35SourceAttributionObserver.openLedgerFiles(\n"
        "            value.output.toAbsolutePath().normalize().getParent().toFile());")
assert t.count(old1) == 1, "r1: %d" % t.count(old1)
t = t.replace(old1, new1)

# 2) before disarm: close writers + copy files + count from disk
old2 = "      ledgerRows = countDataRows(evaluationLedger);"
new2 = (
    "      V35SourceAttributionObserver.closeLedgerWriters();\n"
    "      copyLedgerFiles(partial);\n"
    "      ledgerRows = countLedgerFileRows(partial);")
assert t.count(old2) == 1, "r2: %d" % t.count(old2)
t = t.replace(old2, new2)

# 3) remove string-based writeSourceLedgers call
old3 = "        writeSourceLedgers(partial, evaluationLedger, pddrLedger);\n"
assert t.count(old3) == 1, "r3: %d" % t.count(old3)
t = t.replace(old3, "")

# 4) remove countDataRows
old4 = (
    "  private static long countDataRows(String csv) {\n"
    "    if (csv == null || csv.isEmpty()) return 0L;\n"
    "    long rows = 0L;\n"
    "    for (String line : csv.split(\"" + chr(92) + chr(110) + "\")) {\n"
    "      if (!line.isEmpty() && !line.startsWith(\"actualFE,\")\n"
    "          && !line.startsWith(\"cycle,\")) {\n"
    "        rows++;\n"
    "      }\n"
    "    }\n"
    "    return rows;\n"
    "  }\n"
    "\n")
assert t.count(old4) == 1, "r4: %d" % t.count(old4)
t = t.replace(old4, "")

# 5) add helpers before writeBudget
anchor = "  private static void writeBudget("
addition = (
    "  /** Copies observer streaming temp files into the run output dir. */\n"
    "  private static void copyLedgerFiles(Path partial) throws IOException {\n"
    "    java.io.File ledger = V35SourceAttributionObserver.getLedgerTempFile();\n"
    "    java.io.File pddr = V35SourceAttributionObserver.getPddrTempFile();\n"
    "    if (ledger != null && ledger.exists()) {\n"
    "      Files.copy(ledger.toPath(), partial.resolve(\"source-ledger.csv\"),\n"
    "          java.nio.file.StandardCopyOption.REPLACE_EXISTING);\n"
    "    }\n"
    "    if (pddr != null && pddr.exists()) {\n"
    "      Files.copy(pddr.toPath(), partial.resolve(\"pddr-round-ledger.csv\"),\n"
    "          java.nio.file.StandardCopyOption.REPLACE_EXISTING);\n"
    "    }\n"
    "  }\n"
    "\n"
    "  /** Counts data rows in the on-disk ledger (excludes headers). */\n"
    "  private static long countLedgerFileRows(Path partial) {\n"
    "    java.io.File ledger = partial.resolve(\"source-ledger.csv\").toFile();\n"
    "    if (!ledger.exists()) return 0L;\n"
    "    long rows = 0L;\n"
    "    try (java.io.BufferedReader reader = new java.io.BufferedReader(\n"
    "        new java.io.InputStreamReader(new java.io.FileInputStream(ledger),\n"
    "            StandardCharsets.UTF_8))) {\n"
    "      String line;\n"
    "      while ((line = reader.readLine()) != null) {\n"
    "        if (!line.isEmpty() && !line.startsWith(\"actualFE,\")\n"
    "            && !line.startsWith(\"cycle,\")) {\n"
    "          rows++;\n"
    "        }\n"
    "      }\n"
    "    } catch (java.io.IOException error) {\n"
    "      return 0L;\n"
    "    }\n"
    "    return rows;\n"
    "  }\n"
    "\n"
    "  private static void writeBudget(")
assert t.count(anchor) == 1, "r5: %d" % t.count(anchor)
t = t.replace(anchor, addition, 1)

io.open(P, "w", encoding="utf-8", newline="\n").write(t)
print("Runner streaming adaptation applied (5 edits)")
