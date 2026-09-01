package org.uma.jmetal.algorithm.multiobjective.mypso.v35;

/**
 * Dormant archive experiment modes.  None of these values is part of the
 * frozen production configuration; production runs never install an archive
 * experiment runtime and therefore retain the historical unbounded archive.
 */
public enum V35ArchiveMode {
  UNBOUNDED_FULL,
  BOUNDED_TEACHER_VIEW,
  BOUNDED_ACTIVE_ARCHIVE
}
