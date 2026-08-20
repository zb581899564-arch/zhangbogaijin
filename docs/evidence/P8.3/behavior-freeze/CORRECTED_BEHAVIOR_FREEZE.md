# P8.3 corrected behavior freeze

- Algorithm semantics: `cata-apply-v2`
- Instance: I1 (`10_2_2_1` bridge)
- Seed: `20260808`
- Population: `10`
- Budget: `5000 FE` (completed at `4999 FE` without a partial generation)
- CA-TA Test calls: `2237`
- CA-TA Apply calls: `1102`
- CA-TA full evaluations: `3339`
- Initial population SHA-256: `4f9c0fabf5b9effa62576186503d026887a071af27c2685b07f6f96064ea35e3`
- Qg table SHA-256: `39B262E950AFC623AF998DC0215488CFD6CFC625811E1F414A84AB6D92938B32`
- Qp table SHA-256: `E6FCD73636E8FCA4E07CBF448E9416BE4E73C6F420E1BFA5CBD212FE236FB279`

The tracked I1 lineage contains 279 Apply decisions. Every Apply decision is followed by exactly one evaluated local candidate; the violation count is zero. Test decisions still evaluate every currently valid neighborhood once. The final freeze is byte-identical to the preliminary corrected run for the action, CA-TA, Q-table, PDDR, lineage, and final-population evidence listed below.

This is the behavioral baseline for all subsequent *pure performance* changes. A performance change is accepted only if the initial population, action trace, evaluation trace, final front, and FE count remain unchanged.

## SHA-256

```text
7cf8ab72967f05f16e9902e4326e718190dced3d15268cf69ccfd6923149b408  05_one_particle_evolution/cfvf_tracked_lineage_events.log
dfcfe54d1abca92e3a47eb1cbdcc02db0e399e7098130ff7cbf1ad774df66451  05_one_particle_evolution/dual_q_events.log
dca16aadaa0325d0ea03ef351e1f2bc45a7d67fc4f127d5a7b8164ff7c0ab7f0  05_one_particle_evolution/initial_population.csv
eced64fe39c2f8064d9a37762b45ba8e287aada35e355a1a6bde643c706a7221  05_one_particle_evolution/qg_events.log
c0fb9ab541d82815bdfc5a8675eb6fa1cfbd09e78a7ecb6a7fb6d1e4c85ab0ba  05_one_particle_evolution/qp_tracked_lineage_events.log
1456418d5ef6c233f580331151e6368ece0f4ef2dbced28577d1bc9ee837ddba  05_one_particle_evolution/trace_summary.properties
8c2b92fe119476a472a2479ec25bae4fdfb6a18c126b36cef2e96518b0c894eb  06_local_search/cata_statistics.txt
a6e25062499ff456ac1766fa14521a0def4ac04689cb05162a813272c611bae3  06_local_search/cata_tracked_lineage_events.log
4f56b6aa3e844ed18e0a334c279da46f9ae968ef75ef10930eb26f9fda9fac5b  07_environment_selection/final_population.csv
2273da0dc06925b962bfa8c7df128d889349f141f4060d17918fd9c652c16138  07_environment_selection/lineage_events.log
790be67b370e219dacdb10fdccfeba2c6274735298cb2eded10ab1c1bb1c0d0c  07_environment_selection/lineage_final_state.txt
42878cd13c580e7ee753e78d16a7c9e8224e781e7f6505803412982ec7288a2d  07_environment_selection/pddr_events.log
```

The `trace_summary.properties` file remains the authoritative counter and Q-table summary.
