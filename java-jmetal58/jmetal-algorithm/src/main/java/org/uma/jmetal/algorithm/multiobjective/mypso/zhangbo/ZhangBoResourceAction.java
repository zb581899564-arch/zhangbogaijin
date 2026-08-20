package org.uma.jmetal.algorithm.multiobjective.mypso.zhangbo;

import java.io.Serializable;

/** A complete, legal first-stage resource target for one job. */
public final class ZhangBoResourceAction implements Serializable {
  private static final long serialVersionUID = 1L;

  public enum Kind { FMW, MW, M, W }
  public enum Source { INERTIA, PBEST, GBEST, BOTH, EXPLORE }

  private final int job;
  private final Kind kind;
  private final Source source;
  private final int factory;
  private final int machine;
  private final int worker;

  public ZhangBoResourceAction(
      int job, Kind kind, Source source, int factory, int machine, int worker) {
    if (job < 0 || kind == null || source == null) throw new IllegalArgumentException("Invalid action");
    this.job = job;
    this.kind = kind;
    this.source = source;
    this.factory = factory;
    this.machine = machine;
    this.worker = worker;
  }

  public int getJob() { return job; }
  public Kind getKind() { return kind; }
  public Source getSource() { return source; }
  public int getFactory() { return factory; }
  public int getMachine() { return machine; }
  public int getWorker() { return worker; }

  public int priority() {
    if (kind == Kind.FMW) return 3;
    if (kind == Kind.MW) return 2;
    return 1;
  }

  public ZhangBoResourceAction withSource(Source value) {
    return new ZhangBoResourceAction(job, kind, value, factory, machine, worker);
  }

  public boolean hasSameTarget(ZhangBoResourceAction other) {
    return other != null && factory == other.factory && machine == other.machine && worker == other.worker;
  }

  public String toCanonicalText() {
    return job + ":" + kind + ":" + source + ":" + factory + ":" + machine + ":" + worker;
  }
}
