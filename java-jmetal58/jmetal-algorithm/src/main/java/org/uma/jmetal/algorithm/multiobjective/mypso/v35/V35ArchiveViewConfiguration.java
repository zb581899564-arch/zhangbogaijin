package org.uma.jmetal.algorithm.multiobjective.mypso.v35;

import java.io.Serializable;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/** Immutable, separately hashed configuration for dormant archive experiments. */
public final class V35ArchiveViewConfiguration implements Serializable {
  private static final long serialVersionUID = 1L;
  public static final String VERSION = "v35-archive-experiment-v1";

  private final V35ArchiveMode mode;
  private final int teacherViewCapacity;
  private final int activeArchiveCapacity;

  private V35ArchiveViewConfiguration(V35ArchiveMode mode, int teacherViewCapacity,
      int activeArchiveCapacity) {
    if (mode == null) throw new IllegalArgumentException("mode");
    if (mode == V35ArchiveMode.UNBOUNDED_FULL
        && (teacherViewCapacity != 0 || activeArchiveCapacity != 0)) {
      throw new IllegalArgumentException("unbounded mode cannot declare a capacity");
    }
    if (mode == V35ArchiveMode.BOUNDED_TEACHER_VIEW
        && (teacherViewCapacity < 3 || activeArchiveCapacity != 0)) {
      throw new IllegalArgumentException("teacher-view mode requires capacity >= 3 only");
    }
    if (mode == V35ArchiveMode.BOUNDED_ACTIVE_ARCHIVE
        && (activeArchiveCapacity < 3 || teacherViewCapacity != 0)) {
      throw new IllegalArgumentException("active-archive mode requires capacity >= 3 only");
    }
    this.mode = mode;
    this.teacherViewCapacity = teacherViewCapacity;
    this.activeArchiveCapacity = activeArchiveCapacity;
  }

  public static V35ArchiveViewConfiguration unboundedFull() {
    return new V35ArchiveViewConfiguration(V35ArchiveMode.UNBOUNDED_FULL, 0, 0);
  }

  public static V35ArchiveViewConfiguration teacherView(int capacity) {
    return new V35ArchiveViewConfiguration(
        V35ArchiveMode.BOUNDED_TEACHER_VIEW, capacity, 0);
  }

  public static V35ArchiveViewConfiguration activeArchive(int capacity) {
    return new V35ArchiveViewConfiguration(
        V35ArchiveMode.BOUNDED_ACTIVE_ARCHIVE, 0, capacity);
  }

  public V35ArchiveMode getMode() { return mode; }
  public int getTeacherViewCapacity() { return teacherViewCapacity; }
  public int getActiveArchiveCapacity() { return activeArchiveCapacity; }
  public boolean isControl() { return mode == V35ArchiveMode.UNBOUNDED_FULL; }
  public boolean usesTeacherView() { return mode == V35ArchiveMode.BOUNDED_TEACHER_VIEW; }
  public boolean boundsActiveArchive() {
    return mode == V35ArchiveMode.BOUNDED_ACTIVE_ARCHIVE;
  }

  public String canonicalText() {
    return "archiveExperimentVersion=" + VERSION + '\n'
        + "archiveMode=" + mode + '\n'
        + "teacherViewCapacity=" + teacherViewCapacity + '\n'
        + "activeArchiveCapacity=" + activeArchiveCapacity + '\n'
        + "objectiveSlots=0,1,6\n"
        + "selection=maximin-extremes-stable-fingerprint\n"
        + "pddrSelectionMode=UNCHANGED_GLOBAL_ORIGINAL\n";
  }

  public String configurationHash() {
    try {
      byte[] digest = MessageDigest.getInstance("SHA-256")
          .digest(canonicalText().getBytes(StandardCharsets.UTF_8));
      StringBuilder result = new StringBuilder();
      for (byte value : digest) result.append(String.format("%02x", value & 0xff));
      return result.toString();
    } catch (NoSuchAlgorithmException error) {
      throw new IllegalStateException("SHA-256 unavailable", error);
    }
  }
}
