#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""Collect every FC experiment run into one machine-readable CSV."""
import csv, io, math, os, re, sys

BASE = r'E:\学习\李明哲-毕业材料\张博改进\docs\evidence\V35-P26\experiments'

def load_front(path):
    with io.open(path, encoding='utf-8') as f:
        rows = list(csv.DictReader(f))
    return [[float(r['Cmax']), float(r['TEC']), float(r['TWC'])] for r in rows]

def nondominated(points):
    dominated = set()
    for i, p in enumerate(points):
        if i in dominated: continue
        for j, q in enumerate(points):
            if i == j or j in dominated: continue
            if all(p[k] <= q[k] + 1e-12 for k in range(3)) and any(p[k] < q[k] - 1e-12 for k in range(3)):
                dominated.add(j)
    return [p for i, p in enumerate(points) if i not in dominated]

def normalize(values, reference):
    mn = [min(p[k] for p in reference) for k in range(3)]
    mx = [max(p[k] for p in reference) for k in range(3)]
    return [[min(1.0, max(0.0, (p[k]-mn[k])/max(1e-12, mx[k]-mn[k]))) for k in range(3)] for p in values]

def hypervolume(points):
    rx, ry, rz = 1.1, 1.1, 1.1
    pts = sorted(points, key=lambda p: p[0])
    volume, active, index = 0.0, [], 0
    while index < len(pts):
        x = max(0.0, min(rx, pts[index][0]))
        while index < len(pts) and pts[index][0] <= x + 1e-12:
            active.append(pts[index]); index += 1
        next_x = max(x, min(rx, pts[index][0])) if index < len(pts) else rx
        volume += max(0.0, next_x - x) * union_yz(active, ry, rz)
    return max(0.0, volume)

def union_yz(points, ry, rz):
    pts = sorted(points, key=lambda p: p[1])
    area, min_z, index = 0.0, rz, 0
    while index < len(pts):
        y = max(0.0, min(ry, pts[index][1]))
        while index < len(pts) and pts[index][1] <= y + 1e-12:
            min_z = min(min_z, max(0.0, min(rz, pts[index][2]))); index += 1
        next_y = max(y, min(ry, pts[index][1])) if index < len(pts) else ry
        area += max(0.0, next_y - y) * max(0.0, rz - min_z)
    return area

def igd(a, r):
    return sum(min(math.dist(p, q) for q in a) for p in r) / len(r)

def spacing(points):
    if len(points) < 2: return 0.0
    pts = sorted(points)
    ds = [min(math.dist(pts[i], pts[j]) for j in range(len(pts)) if i != j) for i in range(len(pts))]
    mean = sum(ds)/len(ds)
    return math.sqrt(sum((d-mean)**2 for d in ds)/len(ds))

def parse_summary(path):
    out = {}
    try:
        txt = io.open(path, encoding='utf-8').read()
    except Exception:
        return out
    for key in ['seed','maxFEs','fullEvaluations','formalOuterCycles','formalQgRounds',
                'qgSelections','qgTdUpdates','qpActions','qpTransitions','cfvfOffspring',
                'cfvfRepairs','archiveInsertions','caTaLiteTest','caTaLiteApply',
                'caTaLiteFE','formalLocalFE','formalLocalFraction','frontSize','runNanos',
                'fm3StructurePreviews','proxyStructurePreviews','pddrEvents']:
        m = re.search(r'^' + key + r'=(\S+)', txt, re.M)
        if m: out[key] = m.group(1)
    return out

# groups: (name, root-dir, instance-dirs map, arms, seeds, budget)
# layouts:
#   'nested': root/instance/arm/seed-*/   (remote experiment batches)
#   'direct': root/arm/seed-*/            (local experiment batches)
#   'flat':   root/instance/{front.csv, mechanism-summary.txt}  (audit runs, no arm/seed)
def collect_runs(tag, root, instance, arm, seed, layout='nested'):
    if layout == 'flat':
        d = os.path.join(root, instance)
    else:
        if layout == 'nested':
            d = os.path.join(root, instance, arm, 'seed-' + seed)
        else:
            d = os.path.join(root, arm, 'seed-' + seed)
    front_path = os.path.join(d, 'front.csv')
    mech_path = os.path.join(d, 'mechanism-summary.txt')
    if not os.path.exists(front_path): return None
    summary = parse_summary(mech_path)
    return {
        'tag': tag, 'instance': instance, 'arm': arm, 'seed': seed,
        'front': load_front(front_path), 'summary': summary,
        'run_nanos': summary.get('runNanos', ''),
    }

def main():
    seeds = ['20260822','20260823','20260824']
    specs = []
    # FC-1 audit (local): flat layout, per-instance front + mechanism summary
    specs.append(('fc1-audit', os.path.join(BASE, 'fc1-semantic-audit'),
                  ['20_2_3_1','100_2_3_1'], ['audit'], ['-'], '50000', 'flat'))
    # FC-2 local 50k core (20-job) + scale (10/50/100): direct layout
    specs.append(('fc2-50k-local', os.path.join(BASE, 'fc2-core-20_2_3_1'),
                  ['20_2_3_1'], ['legacy','pacing'], seeds, '50000', 'direct'))
    for inst in ['10_2_3_1','50_2_3_1','100_2_3_1']:
        specs.append(('fc2-50k-local', os.path.join(BASE, 'fc2-scale-' + inst),
                      [inst], ['legacy','pacing'], seeds, '50000', 'direct'))
    # FC-2 local 500k
    specs.append(('fc2-500k-local', os.path.join(BASE, 'fc2-500k-20_2_3_1'),
                  ['20_2_3_1'], ['legacy','pacing'], seeds, '500000', 'direct'))
    # FC-2 remote 500k
    specs.append(('fc2-500k-remote', os.path.join(BASE, 'remote-fc-20260817', 'fc2-500k'),
                  ['10_2_3_1','20_2_3_1','50_2_3_1','100_2_3_1'],
                  ['legacy','pacing'], seeds, '500000', 'nested'))
    # FC-3 legacy base (remote)
    specs.append(('fc3-ab-legacy', os.path.join(BASE, 'remote-fc-20260817', 'fc3-ab'),
                  ['20_2_3_1'], ['standard','cheap'], seeds, '50000', 'nested'))
    # FC-3 pacing base (remote)
    specs.append(('fc3-pacing-base', os.path.join(BASE, 'remote-fc-20260817', 'fc3-pacing'),
                  ['20_2_3_1'], ['base','cheap'], seeds, '50000', 'nested'))
    # FC-4 rho (remote, may be partial)
    specs.append(('fc4-rho', os.path.join(BASE, 'remote-fc-20260817', 'fc4-rho'),
                  ['20_2_3_1','100_2_3_1'], ['rho0.1','rho0.2','rho0.3'], seeds, '500000', 'nested'))

    all_runs = []
    for tag, root, instances, arms, seed_list, budget, layout in specs:
        for inst in instances:
            pooled = []
            run_map = {}
            for arm in arms:
                for seed in seed_list:
                    run = collect_runs(tag, root, inst, arm, seed, layout)
                    if run is None: continue
                    run_map[(arm, seed)] = run
                    pooled += run['front']
            if not pooled: continue
            ref_raw = nondominated(pooled)
            ref_n = nondominated(normalize(ref_raw, ref_raw))
            for (arm, seed), run in sorted(run_map.items()):
                raw = nondominated(run['front'])
                an = nondominated(normalize(raw, ref_raw))
                s = run['summary']
                row = {
                    'tag': tag, 'instance': inst, 'arm': arm, 'seed': seed,
                    'budget': budget,
                    'fullEvaluations': s.get('fullEvaluations',''),
                    'frontSize': len(raw),
                    'HV': round(hypervolume(an), 6),
                    'IGD': round(igd(an, ref_n), 6),
                    'SP': round(spacing(raw), 6),
                    'minCmax': round(min(x[0] for x in raw), 2),
                    'minTEC': round(min(x[1] for x in raw), 2),
                    'minTWC': round(min(x[2] for x in raw), 2),
                    'outerCycles': s.get('formalOuterCycles',''),
                    'localFraction': s.get('formalLocalFraction',''),
                    'localFE': s.get('formalLocalFE',''),
                    'qgTdUpdates': s.get('qgTdUpdates',''),
                    'qpTransitions': s.get('qpTransitions',''),
                    'caTaLiteTest': s.get('caTaLiteTest',''),
                    'caTaLiteApply': s.get('caTaLiteApply',''),
                    'cfvfOffspring': s.get('cfvfOffspring',''),
                    'fm3Previews': s.get('fm3StructurePreviews',''),
                    'proxyPreviews': s.get('proxyStructurePreviews',''),
                    'runNanos': s.get('runNanos',''),
                    'runSeconds': (float(s['runNanos'])/1e9) if s.get('runNanos') else '',
                }
                all_runs.append(row)

    out = os.path.join(BASE, 'fc-all-runs.csv')
    fields = ['tag','instance','arm','seed','budget','fullEvaluations','frontSize',
              'HV','IGD','SP','minCmax','minTEC','minTWC','outerCycles','localFraction',
              'localFE','qgTdUpdates','qpTransitions','caTaLiteTest','caTaLiteApply',
              'cfvfOffspring','fm3Previews','proxyPreviews','runNanos','runSeconds']
    with io.open(out, 'w', encoding='utf-8', newline='') as f:
        w = csv.DictWriter(f, fieldnames=fields, extrasaction='ignore')
        w.writeheader()
        for row in all_runs: w.writerow(row)
    print('rows written:', len(all_runs), '->', out)

if __name__ == '__main__':
    main()
