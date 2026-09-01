# -*- coding: utf-8 -*-
"""Applies the 11 source-ledger patches to the shadowed ZhangBoMOHPSOQ.java.
Every replacement must match exactly once; otherwise aborts without writing."""
import io, os

p = os.path.join(os.path.dirname(os.path.abspath(__file__)),
                 "src", "org", "uma", "jmetal", "algorithm", "multiobjective", "mypso",
                 "ZhangBoMOHPSOQ.java")
t = io.open(p, encoding="utf-8").read()

Q = 'V35EvaluationSourceContext.Source.'
patches = [
# P1 batch loop 1 (unevaluated)
("""                if (v35PassiveArchive != null) {
                    for (PermutationSolution<Integer> solution : unevaluated) {
                        v35PassiveArchive.observe(solution);
                    }
                }""",
 """                if (v35PassiveArchive != null) {
                    for (PermutationSolution<Integer> solution : unevaluated) {
                        // V35-SOURCE-LEDGER-PATCH: source readable inside the window.
                        v35PassiveArchive.observeWithSource(solution, V35EvaluationSourceContext.current());
                    }
                }"""),
# P2 batch loop 2 (swarm, original odd indentation preserved)
("""                if (v35PassiveArchive != null) {
                    for (PermutationSolution<Integer> solution : swarm) {
                v35PassiveArchive.observe(solution);
            }
        }""",
 """                if (v35PassiveArchive != null) {
                    for (PermutationSolution<Integer> solution : swarm) {
                // V35-SOURCE-LEDGER-PATCH: source readable inside the window.
                v35PassiveArchive.observeWithSource(solution, V35EvaluationSourceContext.current());
            }
        }"""),
# P3 global offspring single site
("""            fc52RecordEvaluated(candidate, fullEvaluationCount,
                    V35EvaluationSourceContext.Source.GLOBAL_CFVF);
            observePassiveArchive(candidate);""",
 """            fc52RecordEvaluated(candidate, fullEvaluationCount,
                    V35EvaluationSourceContext.Source.GLOBAL_CFVF);
            // V35-SOURCE-LEDGER-PATCH
            observePassiveArchive(candidate, V35EvaluationSourceContext.Source.GLOBAL_CFVF);"""),
# P4 legacy CA-TA
("""                observePassiveArchive(local);
                    zhangBoCaTaController.record(context, id, accepted, gain,""",
 """                // V35-SOURCE-LEDGER-PATCH
                observePassiveArchive(local, decision.isTestPhase()
                        ? V35EvaluationSourceContext.Source.CATA_TEST
                        : V35EvaluationSourceContext.Source.CATA_APPLY);
                    zhangBoCaTaController.record(context, id, accepted, gain,"""),
# P5 CA-TA-Lite
("""                observePassiveArchive(local);
                v35CaTaLiteController.record(context, action, accepted, gain,""",
 """                // V35-SOURCE-LEDGER-PATCH
                observePassiveArchive(local, decision.isTest()
                        ? V35EvaluationSourceContext.Source.CATA_TEST
                        : V35EvaluationSourceContext.Source.CATA_APPLY);
                v35CaTaLiteController.record(context, action, accepted, gain,"""),
# P6 fixed VNS
("""                observePassiveArchive(local);
                zhangBoCaTaEvents.add("generation=" + generationNumber() + ",slot="
                        + parent.slot + ",fixedId=" + id""",
 """                // V35-SOURCE-LEDGER-PATCH
                observePassiveArchive(local, V35EvaluationSourceContext.Source.INTRA_FACTORY_VNS);
                zhangBoCaTaEvents.add("generation=" + generationNumber() + ",slot="
                        + parent.slot + ",fixedId=" + id"""),
# P7 critical swap
("""                observePassiveArchive(candidate);
                zhangBoCaTaEvents.add("formalLocal:outer=" + completedOuterGenerations
                        + ",slot=" + parent.slot + ",op=CRITICAL_FACTORY_SWAP,accepted="
                        + accepted + ",fe=" + fullEvaluationCount);""",
 """                // V35-SOURCE-LEDGER-PATCH
                observePassiveArchive(candidate, V35EvaluationSourceContext.Source.INTER_FACTORY_LS);
                zhangBoCaTaEvents.add("formalLocal:outer=" + completedOuterGenerations
                        + ",slot=" + parent.slot + ",op=CRITICAL_FACTORY_SWAP,accepted="
                        + accepted + ",fe=" + fullEvaluationCount);"""),
# P8 critical insert
("""                observePassiveArchive(candidate);
                zhangBoCaTaEvents.add("formalLocal:outer=" + completedOuterGenerations
                        + ",slot=" + parent.slot + ",op=CRITICAL_FACTORY_INSERT,accepted="
                        + accepted + ",fe=" + fullEvaluationCount);""",
 """                // V35-SOURCE-LEDGER-PATCH
                observePassiveArchive(candidate, V35EvaluationSourceContext.Source.INTER_FACTORY_LS);
                zhangBoCaTaEvents.add("formalLocal:outer=" + completedOuterGenerations
                        + ",slot=" + parent.slot + ",op=CRITICAL_FACTORY_INSERT,accepted="
                        + accepted + ",fe=" + fullEvaluationCount);"""),
# P9 O1-O9
("""                    observePassiveArchive(candidate);
                    zhangBoCaTaEvents.add("formalLocal:outer=" + completedOuterGenerations
                            + ",slot=" + parent.slot + ",pass=" + pass + ",op=" + id""",
 """                    // V35-SOURCE-LEDGER-PATCH
                    observePassiveArchive(candidate, V35EvaluationSourceContext.Source.INTRA_FACTORY_VNS);
                    zhangBoCaTaEvents.add("formalLocal:outer=" + completedOuterGenerations
                            + ",slot=" + parent.slot + ",pass=" + pass + ",op=" + id"""),
# P10 observePassiveArchive signature + body
("""    private void observePassiveArchive(PermutationSolution<Integer> evaluated) {
        if (v35PassiveArchive != null) v35PassiveArchive.observe(evaluated);
    }""",
 """    // V35-SOURCE-LEDGER-PATCH: explicit source parameter (pure label, no behavior change).
    private void observePassiveArchive(PermutationSolution<Integer> evaluated,
            V35EvaluationSourceContext.Source source) {
        if (v35PassiveArchive != null) v35PassiveArchive.observeWithSource(evaluated, source);
    }"""),
# P11 PDDR hook after cmaxAudit.observePddrSelection
("""        if (cmaxAudit != null) {
            cmaxAudit.observePddrSelection(selected, generationNumber());
        }""",
 """        if (cmaxAudit != null) {
            cmaxAudit.observePddrSelection(selected, generationNumber());
        }
        // V35-SOURCE-LEDGER-PATCH: pure observation of merge-pool composition and
        // PDDR selection (no-op unless armed; no behavior change, no RNG, no FE).
        if (org.uma.jmetal.algorithm.multiobjective.mypso.v35.V35SourceLedgerHook.isArmed()) {
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
            org.uma.jmetal.algorithm.multiobjective.mypso.v35.V35SourceLedgerHook.onPddrRound(
                    ledgerPoolSolutions, ledgerPoolSourceNames, selected,
                    fullEvaluationCount, (int) formalBaselineOuterCycles + 1);
        }"""),
]

for i, (old, new) in enumerate(patches, 1):
    n = t.count(old)
    assert n == 1, "patch %d matched %d times" % (i, n)
    t = t.replace(old, new)

io.open(p, "w", encoding="utf-8", newline="\n").write(t)
print("ZhangBoMOHPSOQ patched: %d/%d sites, all unique" % (len(patches), len(patches)))
