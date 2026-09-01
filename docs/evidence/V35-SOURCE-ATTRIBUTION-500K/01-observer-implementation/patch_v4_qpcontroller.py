# -*- coding: utf-8 -*-
"""Patches the shadowed ZhangBoQpController with the personal-archive
observation hook (Phase A0 schema: enteredPersonalArchive / Qp teacher)."""
import io, os

HERE = os.path.dirname(os.path.abspath(__file__))
P = os.path.join(HERE, "src", "org", "uma", "jmetal", "algorithm",
                 "multiobjective", "mypso", "zhangbo", "ZhangBoQpController.java")
t = io.open(P, encoding="utf-8").read()

old = """      child.setAttribute(ZhangBoQpLineageState.class,
          new ZhangBoQpLineageState(nextPbest.getFingerprint()));"""
new = """      child.setAttribute(ZhangBoQpLineageState.class,
          new ZhangBoQpLineageState(nextPbest.getFingerprint()));
      // V35-SOURCE-ATTRIBUTION-PATCH: personal-archive observation (pure
      // observation; no-op unless armed; no RNG/FE/behavior change).
      org.uma.jmetal.algorithm.multiobjective.mypso.v35.V35SourceAttributionObserver
          .onPersonalArchiveUpdate(childEntry.getFingerprint(),
              nextPbest.getFingerprint(), update.isInsertedEntrySurvived(),
              selection.getAction() == null ? -1 : selection.getAction().ordinal());"""
assert t.count(old) == 1, "QpController anchor matched %d" % t.count(old)
t = t.replace(old, new)
io.open(P, "w", encoding="utf-8", newline="\n").write(t)
print("ZhangBoQpController patched (personal-archive hook)")
