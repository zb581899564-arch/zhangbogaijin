# V35 Final Source Freeze

状态：`FINAL_SOURCE_FREEZE=ACCEPTED`

## 冻结身份与可回查哈希

| 项目 | 值 |
|---|---|
| Source boundary commit | `7ea194f96bd98c0d7047a91e6d4a2169e815d200` |
| Java artifact source commit | `64b724e4ad1ebb9e6d836a1f091882a1afb3b030` |
| Shared-start manifest commit | `88b358ca452d4080ccf5a0902c0fc3098b4e5d8a` |
| Base repository commit | `e034faafce5f9458a324a03a4aea7ef7098e698d` |
| Release tag | `v35-final-doe1-frozen` (points to the metadata commit containing this file) |
| Isolated clean clone | `E:\学习\李明哲-毕业材料\_isolated-v35-final-doe1-freeze-20260823` |
| Source/config bundle SHA-256 | `ac92eda152348ce11861ec5c2f223e6a9c7643afd50cbaa5d48189d1fc41f0fd` (2,174 allowlisted files) |
| A4 anchor configuration SHA-256 | `cff6bbca0a8357ae848e625710c0ba39a1c9419becd84ef4e95f8bb6f88db09e` |
| Fat jar SHA-256 | `8dad8f40266feeaa4cdb9b47dbe4e342d9064847df32c7f2933149b9b6bad8b9` |
| DOE-1 final parameter-freeze evidence SHA-256 | `56bcd275e84ee9e55608f5274a70b93175194f31eca378673438325ba54eb789` |

`frozen-config.txt` is the exact A4 anchor configuration for seed `20260822`,
population `100` and `MaxFEs=500000`. The formal campaign supplies each
physical-run seed only through its separately frozen shared-start manifest;
all other declared semantic fields remain anchored here.

## Frozen semantic boundary

```text
A4-Pacing; FM3; DEGENERATE_SINGLE_FAMILY; SEQUENCE_INDEPENDENT
ShiftMode=NONE; GLOBAL_ORIGINAL PDDR; CA-TA-Lite -> inherited LS
dual-Q BLOCK_FROZEN warmup=0.10 P=5 G=5; Qg/Qp; DSCR; CFVF; PA_i
rho=0; directionalTeacherPool=false; population=100
subSwarmMixture=[G1_CMAX,G4_BALANCED,G2_TEC,G3_TWC]=[20,40,20,20]
MaxFEs=500000; objectives=[0,1,6]
```

The anchor records `diagnosisMode=FULL_MASK_AUDIT`, `tauAbs=1.0`,
`tauGap=1.0`, and `shadowAudit=false`. The following rejected paths are not
part of this release: `ORDER_SWAP`, `REGION_AWARE`, `BP_RESERVED_LEGACY`,
`rho>0`, active Shift/FCLS/FCRS, PF-SDST, sequence-dependent setup,
directional teacher pool, directional environmental selection,
crowding-distance selection, and a fourth objective.

## Clean source and initial-population method

The shared worktree was intentionally not committed: it contained unrelated
historical evidence, output trees, `node_modules`, and concurrent WIP. A new
isolated clone began at the base commit. Only the reviewed allowlist was
copied; the original 35 code/runner/test/utility files remained byte-identical
across the pre-copy and post-copy audit. `FINAL_ALLOWLIST.md` records the
subsequent Stage-2 additions.

The source snapshot includes the formal shared-start bridge and its
`45 x 20 = 900` frozen four-vector populations.
`ZhangBoV35FormalComparisonRunner` version
`v35-formal-comparison-gate-v2` requires a hash-bound snapshot for the exact
`(instance, seed)` run and reads it through
`ZhangBoV35FormalInitialPopulationFreezeRunner.readSnapshot()`. It rejects
direct population regeneration and verifies both V35 and P8 population hashes.

`scripts/v35_final_master_campaign.py` is the only Stage-2 campaign renderer.
It is separate from the older anonymous scheduler and binds every physical
RunKey to exactly `Arm + Instance + Seed + MaxFEs + JarSHA + ConfigSHA`. Before
launch it verifies the jar, frozen configuration, snapshot, and per-run
provenance hashes; its retry attempts are isolated and its completion marker is
atomic. It is not an algorithm mechanism.

The production decoder rejects CR bytes in strict UTF-8 runtime inputs. The
isolated clone therefore locks the canonical runtime inputs and formal snapshot
trees to LF through `.gitattributes`; all copied runtime inputs were byte-checked
against the authorized worktree.

## Build and verification

| Check | Result |
|---|---|
| Preferred local OpenJDK 11.0.27 | not installed locally |
| Build JDK | `E:\javavava`, Java `17.0.12` |
| Maven target | `source=1.8`, `target=1.8` |
| Formal runner classfile | major version `52` (Java 8) |
| Java 11 compatibility | classfile-level compatible; direct local Java 11 launch was not possible |
| Isolated fat-jar build | PASS |
| A0--A4 profile test | 2/2 PASS |
| A0--A4 smoke test | 2/2 PASS |
| Formal-runner snapshot gate test | 3/3 PASS |
| 45 x 20 shared-start bridge test | 1/1 PASS; materialization/verification used no decoder evaluation |
| Stage-2 master renderer test | 3/3 PASS |

The artifact jar was built from `64b724e...`; commits `cf55cd8...` and
`7ea194f...` add only Python campaign infrastructure and line-ending rules, so
they do not alter the jar payload. `jar.sha256` is the required deployment
identity, rather than the dirty main-worktree `target` artifact.

## Known production risk, not an algorithm change

The first Track-C candidate preflight requested `20,000` FE for A4 on
`20_2_3_1`, seed `20260828`, and safely completed at `15,258` decoder/FE calls.
It had no illegal, duplicate, or non-finite solutions and mechanisms triggered,
but it did **not** meet an exact-requested-FE gate because the existing safe-tail
boundary avoids creating partial Q/local batches. This freeze records that
risk; it does not change safe-tail semantics or misrepresent the result as an
exact 20k run. Any formal campaign acceptance must apply its declared FE gate
to the actual recorded count.

## Acceptance scope

This accepts the clean source/config/jar/initial-population freeze only. It
does not claim that raw formal runs, a reference front, performance conclusions,
or statistical conclusions already exist.
