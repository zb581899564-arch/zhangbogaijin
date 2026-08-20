#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""FC-TIME-0 汇总：从 fc-time0/ 提取三臂 runNanos，输出 R1/R2/R 与时间门判定。"""
import csv, io, os, statistics, sys

BASE = r'E:\学习\李明哲-毕业材料\张博改进\docs\evidence\V35-P26\fc-time0'

def nanos_of(arm, run_index):
    d = os.path.join(BASE, arm, 'run%d' % run_index)
    mech = os.path.join(d, 'mechanism-summary.txt')
    if os.path.exists(mech):
        for line in io.open(mech, encoding='utf-8'):
            if line.startswith('runNanos='):
                return float(line.split('=')[1].strip())
    # QGS path: nested runs/seed-*/ALG/run-record.csv
    for root, _, files in os.walk(d):
        if 'run-record.csv' in files:
            path = os.path.join(root, 'run-record.csv')
            rows = list(csv.DictReader(io.open(path, encoding='utf-8')))
            if rows:
                return float(rows[-1]['runNanos'])
    return None

def median_seconds(arm):
    values = []
    for i in (1, 2, 3):
        n = nanos_of(arm, i)
        if n is not None:
            values.append(n / 1e9)
    if len(values) != 3:
        print('WARN: %s has %d formal runs' % (arm, len(values)))
    return statistics.median(values) if values else None

def main():
    qgs = median_seconds('qgs')
    legacy = median_seconds('legacy')
    pacing = median_seconds('pacing')
    print('=== FC-TIME-0 同机串行 500k 正式计时（中位，3 次正式运行）===')
    print('QGS   (李明哲基线): %.1f s' % qgs)
    print('Legacy (旧版机制栈): %.1f s' % legacy)
    print('Pacing(新版 A4+pacing 优化后): %.1f s' % pacing)
    if qgs and legacy and pacing:
        r1 = legacy / qgs
        r2 = pacing / legacy
        r = pacing / qgs
        print()
        print('R1 = Legacy/QGS   = %.2fx' % r1)
        print('R2 = Pacing/Legacy= %.2fx' % r2)
        print('R  = Pacing/QGS   = %.2fx' % r)
        print()
        print('时间门：红线 >10x；可接受 5-8x；理想 3-5x')
        if r > 10:
            print('判定: >10x 红线 —— 不启动 45x20，继续瘦身（FC-TIME-3 候选）')
        elif r > 8:
            print('判定: 8-10x —— 超过可接受上沿，继续瘦身')
        elif r >= 5:
            print('判定: 5-8x 可接受区间')
        else:
            print('判定: <5x 理想区间')

if __name__ == '__main__':
    main()
