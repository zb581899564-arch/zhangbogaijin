package org.uma.jmetal.problem.multiobjective.dfsp.fatigue;

import java.util.ArrayList;
import java.util.List;

/**
 * Optional pass-through audit for the P25E paper-author representation
 * repairs. A session must be installed explicitly before a run and cleared
 * afterwards. Every method is a no-op when no session is installed, so the
 * audit never changes search behaviour, random streams, FE counts or result
 * hashes; it only appends to an in-memory event list.
 */
public final class V35P25ERepairAudit {
  private static final ThreadLocal<Session> CURRENT = new ThreadLocal<>();

  private V35P25ERepairAudit() { }

  /** One deterministic repair action: position {@code position} of {@code vector}
   * was changed from {@code oldValue} to {@code newValue} before evaluation
   * number {@code evaluationIndex} (0-based count of already-evaluated
   * candidates). */
  public static final class Event {
    public final String vector;
    public final int position;
    public final int oldValue;
    public final int newValue;
    public final int evaluationIndex;

    Event(String vector, int position, int oldValue, int newValue, int evaluationIndex) {
      this.vector = vector;
      this.position = position;
      this.oldValue = oldValue;
      this.newValue = newValue;
      this.evaluationIndex = evaluationIndex;
    }

    @Override public String toString() {
      return vector + ',' + position + ',' + oldValue + ',' + newValue + ',' + evaluationIndex;
    }
  }

  public static void install() {
    if (CURRENT.get() != null) {
      throw new IllegalStateException("P25E repair audit already installed");
    }
    CURRENT.set(new Session());
  }

  public static void clear() {
    if (CURRENT.get() == null) {
      throw new IllegalStateException("P25E repair audit is not installed");
    }
    CURRENT.remove();
  }

  public static boolean isInstalled() { return CURRENT.get() != null; }

  public static void record(String vector, int position, int oldValue, int newValue,
      int evaluationIndex) {
    Session session = CURRENT.get();
    if (session == null) return; // default path: zero-overhead no-op
    session.events.add(new Event(vector, position, oldValue, newValue, evaluationIndex));
  }

  public static List<Event> events() {
    Session session = CURRENT.get();
    if (session == null) throw new IllegalStateException("P25E repair audit is not installed");
    return session.events;
  }

  private static final class Session {
    private final List<Event> events = new ArrayList<>();
  }
}
