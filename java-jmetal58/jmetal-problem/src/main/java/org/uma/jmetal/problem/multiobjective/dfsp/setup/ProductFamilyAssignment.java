package org.uma.jmetal.problem.multiobjective.dfsp.setup;

import java.io.Serializable;
import java.util.Arrays;

/** Immutable job-to-family assignment. */
public final class ProductFamilyAssignment implements Serializable {
  private static final long serialVersionUID = 1L;
  private final int[] familyOfJob;
  private final int familyCount;

  public ProductFamilyAssignment(int[] familyOfJob, int familyCount) {
    if (familyOfJob == null || familyOfJob.length == 0 || familyCount <= 0) {
      throw new IllegalArgumentException("family assignment dimensions must be positive");
    }
    this.familyOfJob = familyOfJob.clone();
    this.familyCount = familyCount;
    for (int job = 0; job < this.familyOfJob.length; job++) {
      int family = this.familyOfJob[job];
      if (family < 0 || family >= familyCount) {
        throw new IllegalArgumentException("familyOfJob[" + job + "]=" + family
            + " outside [0," + familyCount + ")");
      }
    }
  }

  public int getFamilyCount() { return familyCount; }
  public int getNumberOfJobs() { return familyOfJob.length; }
  public int getFamilyOfJob(int job) {
    if (job < 0 || job >= familyOfJob.length) throw new IndexOutOfBoundsException("job=" + job);
    return familyOfJob[job];
  }
  public int[] getFamilyOfJob() { return familyOfJob.clone(); }
  @Override public String toString() { return Arrays.toString(familyOfJob); }
}
