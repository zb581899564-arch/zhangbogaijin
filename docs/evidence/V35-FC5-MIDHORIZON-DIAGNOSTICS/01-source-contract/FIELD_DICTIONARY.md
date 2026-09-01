# FIELD_DICTIONARY — V35-FC5-MIDHORIZON 扩展字典

继承 FC5_FIELD_DICTIONARY + 新增：

## Checkpoint
`instance,seed,arm,nominalCheckpointFE,actualSnapshotFE,overshootFE,generation,formalOuterCycle,qRound,frontType,solutionFingerprint,Cmax,TEC,TWC`

## PDDR Full Ledger
`instance,seed,arm,cycle,FE,poolIndex,stableFingerprint,Cmax,TEC,TWC,source,strictNondominated,exactObjectiveDuplicate,PDDRScore,PDDRRank,selected,selectedSlot,selectedSemanticRole` + 轮汇总 `poolSize,uniqueObjectiveCount,strictNdCount,cutoffRank=100,cutoffScore,scoreAtRank99/100/101` + 方向代表 `rankMarginToCutoff,scoreMarginToCutoff,nextCycleSurvived,teacherUsed,improvingOffspringObserved`

## Teacher Use Events
`eventId,generation,FE,requestingRole,teacherKind,teacherSource,cacheType,teacherFingerprint,teacherObjectives,action,state,mask,childFingerprint,childEvaluated,childObjectives,directionImproved,strictDominanceImproved,enteredPddrPool,pddrSelected,enteredArchive`

## CA-TA Contribution
`generation,FE,parentFingerprint,candidateFingerprint,context,bottleneck,macroNeighborhood,TEST/APPLY,candidateConstructed,candidateEvaluated,objectives,acceptedLocally,enteredPddrPool,pddrSelected,enteredArchive,survivedNextCycle,directionalGainCmax/TEC/TWC`

主键与去重键见 telemetry-schema-registry.csv
