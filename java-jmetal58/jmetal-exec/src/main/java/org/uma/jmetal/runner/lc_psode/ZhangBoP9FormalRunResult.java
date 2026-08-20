package org.uma.jmetal.runner.lc_psode;

import org.uma.jmetal.algorithm.multiobjective.mypso.p8.P8RunRecord;
import org.uma.jmetal.problem.multiobjective.dfsp.fatigue.ZhangBoDecoderTimingSnapshot;

/** One formal P9 run plus auditable mechanism counters. */
final class ZhangBoP9FormalRunResult {
  final P8RunRecord record;
  final long cfvfOffspring;
  final long pddrEvents;
  final long baselineUpdateEvents;
  final long fixedNeighborhoodEvents;
  final long archiveInsertions;
  final long lineageEvents;
  final long qgSelections;
  final long qgTdUpdates;
  final long qpActions;
  final long qpTransitions;
  final long qpSwitches;
  final long dualQEvents;
  final long caTaEvents;
  final long caTaTestCalls;
  final long caTaApplyCalls;
  final long formalOuterCycles;
  final long formalQgRounds;
  final long formalCriticalFactorySwaps;
  final long formalCriticalFactoryInserts;
  final long formalO1O9Evaluations;
  final String qgTableHash;
  final String qpTableHash;
  final String mechanismVectorHash;
  final String mechanismSummary;
  final long algorithmRunNanos;
  final long experimentWallNanos;
  final ZhangBoDecoderTimingSnapshot decoderTiming;

  ZhangBoP9FormalRunResult(P8RunRecord record, long cfvfOffspring, long pddrEvents,
      long baselineUpdateEvents, long fixedNeighborhoodEvents, long archiveInsertions,
      long lineageEvents, long qgSelections, long qgTdUpdates, long qpActions,
      long qpTransitions, long qpSwitches, long dualQEvents, long caTaEvents,
      long caTaTestCalls, long caTaApplyCalls,
      long formalOuterCycles, long formalQgRounds,
      long formalCriticalFactorySwaps, long formalCriticalFactoryInserts,
      long formalO1O9Evaluations, String qgTableHash, String qpTableHash,
      String mechanismVectorHash, String mechanismSummary,
      long algorithmRunNanos, long experimentWallNanos,
      ZhangBoDecoderTimingSnapshot decoderTiming) {
    this.record = record;
    this.cfvfOffspring = cfvfOffspring;
    this.pddrEvents = pddrEvents;
    this.baselineUpdateEvents = baselineUpdateEvents;
    this.fixedNeighborhoodEvents = fixedNeighborhoodEvents;
    this.archiveInsertions = archiveInsertions;
    this.lineageEvents = lineageEvents;
    this.qgSelections = qgSelections;
    this.qgTdUpdates = qgTdUpdates;
    this.qpActions = qpActions;
    this.qpTransitions = qpTransitions;
    this.qpSwitches = qpSwitches;
    this.dualQEvents = dualQEvents;
    this.caTaEvents = caTaEvents;
    this.caTaTestCalls = caTaTestCalls;
    this.caTaApplyCalls = caTaApplyCalls;
    this.formalOuterCycles = formalOuterCycles;
    this.formalQgRounds = formalQgRounds;
    this.formalCriticalFactorySwaps = formalCriticalFactorySwaps;
    this.formalCriticalFactoryInserts = formalCriticalFactoryInserts;
    this.formalO1O9Evaluations = formalO1O9Evaluations;
    this.qgTableHash = qgTableHash;
    this.qpTableHash = qpTableHash;
    this.mechanismVectorHash = mechanismVectorHash;
    this.mechanismSummary = mechanismSummary;
    this.algorithmRunNanos = algorithmRunNanos;
    this.experimentWallNanos = experimentWallNanos;
    this.decoderTiming = decoderTiming;
  }
}
