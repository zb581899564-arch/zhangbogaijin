package org.uma.jmetal.algorithm.multiobjective.mypso.v35;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/** Pre-registered dormant arms; only the dedicated archive runner accepts them. */
public enum V35ArchiveExperimentProfile {
  ND0_FULL_ARCHIVE_CONTROL(V35ArchiveViewConfiguration.unboundedFull()),
  ND1_TEACHER_VIEW_K50(V35ArchiveViewConfiguration.teacherView(50)),
  ND2_TEACHER_VIEW_K25(V35ArchiveViewConfiguration.teacherView(25)),
  ND3_ACTIVE_ARCHIVE_K200(V35ArchiveViewConfiguration.activeArchive(200)),
  ND4_ACTIVE_ARCHIVE_K100(V35ArchiveViewConfiguration.activeArchive(100));

  private final V35ArchiveViewConfiguration configuration;

  V35ArchiveExperimentProfile(V35ArchiveViewConfiguration configuration) {
    this.configuration = configuration;
  }

  public V35ArchiveViewConfiguration getConfiguration() { return configuration; }
  public String canonicalText() {
    return "archiveExperimentArm=" + name() + '\n' + configuration.canonicalText();
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
  public boolean isControl() { return configuration.isControl(); }
}
