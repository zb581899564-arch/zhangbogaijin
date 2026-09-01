# -*- coding: utf-8 -*-
"""Applies V4 Source-Attribution patches to the shadowed ZhangBoMOHPSOQ.java
(V3's 11 patches are already present in the copied source; this adds the
V4 context/teacher/PA patches and redirects the ledger hook)."""
import io, os

HERE = os.path.dirname(os.path.abspath(__file__))
P = os.path.join(HERE, "src", "org", "uma", "jmetal", "algorithm",
                 "multiobjective", "mypso", "ZhangBoMOHPSOQ.java")
t = io.open(P, encoding="utf-8").read()
OBS = "org.uma.jmetal.algorithm.multiobjective.mypso.v35.V35SourceAttributionObserver"
patches = []

# V4-1: Qg selection context (in selectQgLeader, after pendingQgSelections.put)
patches.append((
    "        pendingQgSelections.put(group, selection);",
    "        pendingQgSelections.put(group, selection);\n"
    "        // V35-SOURCE-ATTRIBUTION-PATCH: Qg teacher/action round context (pure observation).\n"
    "        " + OBS + ".onQgSelection(group == null ? \"UNASSIGNED\" : group.name(),\n"
    "                selection.getAction(), selection.getLeader());"))

# V4-2: Qp selection context marker (children list passed; PA detail comes from the shadowed controller)
patches.append((
    "            if (pendingQpSelections.put(selection.getBranchId(), selection) != null) {",
    "            // V35-SOURCE-ATTRIBUTION-PATCH: Qp round context marker (pure observation).\n"
    "            " + OBS + ".onQpSelections(null);\n"
    "            if (pendingQpSelections.put(selection.getBranchId(), selection) != null) {"))

# V4-3: context at Q-round
patches.append((
    "                formalQRoundIndex = round;",
    "                formalQRoundIndex = round;\n"
    "                // V35-SOURCE-ATTRIBUTION-PATCH: round context (pure observation).\n"
    "                " + OBS + ".context(generationNumber(), formalBaselineOuterCycles, formalQRoundIndex);"))

# V4-4: context reset at cycle end
patches.append((
    "            formalQRoundIndex = -1;",
    "            formalQRoundIndex = -1;\n"
    "            // V35-SOURCE-ATTRIBUTION-PATCH: cycle context reset (pure observation).\n"
    "            " + OBS + ".context(generationNumber(), formalBaselineOuterCycles, formalQRoundIndex);"))

# V4-5: context at cycle start
patches.append((
    "            formalBaselineOuterCycles++;",
    "            formalBaselineOuterCycles++;\n"
    "            // V35-SOURCE-ATTRIBUTION-PATCH: cycle context (pure observation).\n"
    "            " + OBS + ".context(generationNumber(), formalBaselineOuterCycles, formalQRoundIndex);"))

# V4-6: PDDR hook redirect from V35SourceLedgerHook to the attribution observer
old_pddr = """        if (org.uma.jmetal.algorithm.multiobjective.mypso.v35.V35SourceLedgerHook.isArmed()) {"""
new_pddr = """        if (org.uma.jmetal.algorithm.multiobjective.mypso.v35.V35SourceAttributionObserver.isArmed()) {"""
assert t.count(old_pddr) == 1
t = t.replace(old_pddr, new_pddr)
old_call = """            org.uma.jmetal.algorithm.multiobjective.mypso.v35.V35SourceLedgerHook.onPddrRound(
                    ledgerPoolSolutions, ledgerPoolSourceNames, selected,
                    fullEvaluationCount, (int) formalBaselineOuterCycles + 1);"""
new_call = """            org.uma.jmetal.algorithm.multiobjective.mypso.v35.V35SourceAttributionObserver.onPddrRound(
                    ledgerPoolSolutions, ledgerPoolSourceNames, selected,
                    fullEvaluationCount, (int) formalBaselineOuterCycles + 1);"""
assert t.count(old_call) == 1
t = t.replace(old_call, new_call)

for i, (old, new) in enumerate(patches, 1):
    n = t.count(old)
    assert n == 1, "V4 patch %d matched %d times" % (i, n)
    t = t.replace(old, new)

io.open(P, "w", encoding="utf-8", newline="\n").write(t)
print("ZhangBoMOHPSOQ V4 patches applied: %d (on top of V3's 11)" % len(patches))
