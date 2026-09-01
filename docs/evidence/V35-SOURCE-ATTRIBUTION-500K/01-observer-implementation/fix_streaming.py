# -*- coding: utf-8 -*-
"""Fixes the observer to TRUE streaming: flushed rows go to temp files on disk
(acceptance correction 2026-09-01).  Memory holds only bounded buffers."""
import io

P = "src/org/uma/jmetal/algorithm/multiobjective/mypso/v35/V35SourceAttributionObserver.java"
t = io.open(P, encoding="utf-8").read()
NL = chr(92) + "n"  # backslash-n for Java string escape

# 1) fields
old1 = (
    "  private static final StringBuilder flushedEventLedger = new StringBuilder(\n"
    "      eventHeader());\n"
    "  private static long flushCount = 0L;")
new1 = (
    "  // TRUE STREAMING (acceptance correction 2026-09-01): flushed rows go to a\n"
    "  // temp file on disk, NOT back into memory.  Memory holds only the bounded\n"
    "  // unflushed buffer (maxRowsBeforeFlush x worstCaseBytesPerRowResident).\n"
    "  private static java.io.BufferedWriter ledgerWriter = null;\n"
    "  private static java.io.File ledgerTempFile = null;\n"
    "  private static long flushCount = 0L;")
assert t.count(old1) == 1, "f1: %d" % t.count(old1)
t = t.replace(old1, new1)

# 2) pddr buffer -> writer
old2 = "  private static final StringBuilder pddrBuffer = new StringBuilder(pddrHeader());"
new2 = (
    "  private static java.io.BufferedWriter pddrWriter = null;\n"
    "  private static java.io.File pddrTempFile = null;")
assert t.count(old2) == 1
t = t.replace(old2, new2)

# 3) disarm
old3 = (
    "    flushedEventLedger.setLength(0); flushedEventLedger.append(eventHeader());\n"
    "    flushCount = 0L; pddrBuffer.setLength(0); pddrBuffer.append(pddrHeader());\n"
    "    pddrRounds = 0L;")
new3 = (
    "    closeLedgerWriters();\n"
    "    flushCount = 0L;\n"
    "    pddrRounds = 0L;")
assert t.count(old3) == 1
t = t.replace(old3, new3)

# 4) flushEventBuffer: write to file
old4 = (
    "    flushedEventLedger.append(eventBuffer);\n"
    "    eventBuffer.setLength(0);")
new4 = (
    "    try {\n"
    "      if (ledgerWriter == null) {\n"
    "        ledgerTempFile = java.io.File.createTempFile(\"v35-source-ledger-\", \".csv\");\n"
    "        ledgerTempFile.deleteOnExit();\n"
    "        ledgerWriter = new java.io.BufferedWriter(\n"
    "            new java.io.OutputStreamWriter(\n"
    "                new java.io.FileOutputStream(ledgerTempFile),\n"
    "                java.nio.charset.StandardCharsets.UTF_8), 1 << 16);\n"
    "        ledgerWriter.write(eventHeader());\n"
    "      }\n"
    "      ledgerWriter.write(eventBuffer.toString());\n"
    "      ledgerWriter.flush();\n"
    "    } catch (java.io.IOException error) {\n"
    "      fail(\"ledgerWriteError:\" + error.toString());\n"
    "    }\n"
    "    eventBuffer.setLength(0);")
assert t.count(old4) == 1
t = t.replace(old4, new4)

# 5) PDDR append -> writer.write (multi-line StringBuilder chain, exact from file)
old5 = (
    "        pddrBuffer.append(outerCycle).append(',').append(fe).append(',')\n"
    "            .append(index + 1).append(',').append(poolSourceNames.get(index))\n"
    "            .append(',').append(fp).append(\",true,\").append(sel).append(',')\n"
    "            .append(rank).append(',')\n"
    "            .append(Double.isNaN(score) ? \"NOT_EXPORTED_AT_POOL_LEVEL\" : score)\n"
    "            .append('" + NL + "');")
new5 = (
    "        if (pddrWriter != null) {\n"
    "          pddrWriter.write(outerCycle + \",\" + fe + \",\" + (index + 1) + \",\"\n"
    "              + poolSourceNames.get(index) + \",\" + fp + \",true,\" + sel + \",\"\n"
    "              + rank + \",\"\n"
    "              + (Double.isNaN(score) ? \"NOT_EXPORTED_AT_POOL_LEVEL\" : score));\n"
    "          pddrWriter.write(\"" + NL + "\");\n"
    "        }")
assert t.count(old5) == 1, "f5: %d" % t.count(old5)
t = t.replace(old5, new5)

# 6) getters return file paths
old6 = (
    "  public static String getEvaluationLedgerCsv() {\n"
    "    return flushedEventLedger.toString() + eventBuffer;\n"
    "  }\n"
    "  public static String getPddrLedgerCsv() { return pddrBuffer.toString(); }")
new6 = (
    "  public static String getEvaluationLedgerCsv() {\n"
    "    return \"STREAMED_TO_FILE:\" + (ledgerTempFile == null ? \"NONE\"\n"
    "        : ledgerTempFile.getAbsolutePath());\n"
    "  }\n"
    "  public static String getPddrLedgerCsv() {\n"
    "    return \"STREAMED_TO_FILE:\" + (pddrTempFile == null ? \"NONE\"\n"
    "        : pddrTempFile.getAbsolutePath());\n"
    "  }\n"
    "\n"
    "  /** Opens the PDDR streaming writer (called by the runner at run start). */\n"
    "  public static void openLedgerFiles(java.io.File parentDir) {\n"
    "    if (!armed) return;\n"
    "    try {\n"
    "      pddrTempFile = java.io.File.createTempFile(\"v35-pddr-ledger-\", \".csv\",\n"
    "          parentDir);\n"
    "      pddrTempFile.deleteOnExit();\n"
    "      pddrWriter = new java.io.BufferedWriter(\n"
    "          new java.io.OutputStreamWriter(\n"
    "              new java.io.FileOutputStream(pddrTempFile),\n"
    "              java.nio.charset.StandardCharsets.UTF_8), 1 << 16);\n"
    "      pddrWriter.write(pddrHeader());\n"
    "    } catch (java.io.IOException error) {\n"
    "      fail(\"pddrOpenError:\" + error.toString());\n"
    "    }\n"
    "  }\n"
    "\n"
    "  /** Closes both streaming writers (called before reading files). */\n"
    "  public static void closeLedgerWriters() {\n"
    "    try {\n"
    "      if (ledgerWriter != null) { ledgerWriter.flush(); ledgerWriter.close(); ledgerWriter = null; }\n"
    "    } catch (java.io.IOException error) { fail(\"ledgerCloseError:\" + error.toString()); }\n"
    "    try {\n"
    "      if (pddrWriter != null) { pddrWriter.flush(); pddrWriter.close(); pddrWriter = null; }\n"
    "    } catch (java.io.IOException error) { fail(\"pddrCloseError:\" + error.toString()); }\n"
    "  }\n"
    "\n"
    "  public static java.io.File getLedgerTempFile() { return ledgerTempFile; }\n"
    "  public static java.io.File getPddrTempFile() { return pddrTempFile; }")
assert t.count(old6) == 1
t = t.replace(old6, new6)

io.open(P, "w", encoding="utf-8", newline="\n").write(t)
print("Observer streaming fix applied (6 edits)")
