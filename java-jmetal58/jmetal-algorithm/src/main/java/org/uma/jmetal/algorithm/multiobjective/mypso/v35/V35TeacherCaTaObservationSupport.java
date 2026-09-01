package org.uma.jmetal.algorithm.multiobjective.mypso.v35;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Locale;
import org.uma.jmetal.algorithm.multiobjective.mypso.zhangbo.ZhangBoQgController;
import org.uma.jmetal.solution.PermutationSolution;

/**
 * Small observation-only support package shared by the Agent-C teacher and
 * CA-TA ledgers.
 *
 * <p>The status names are deliberately emitted as data, rather than being
 * represented by an empty cell or by {@code NOT_OBSERVED}.  This keeps an
 * unobservable field distinguishable from a genuine negative observation and
 * from a right-censored event.</p>
 */
public final class V35TeacherCaTaObservationSupport {
  private V35TeacherCaTaObservationSupport() { }

  /** Explicit lifecycle/field evidence states. */
  public enum Status {
    OBSERVED,
    RIGHT_CENSORED_RUN_END,
    NOT_APPLICABLE_BY_ARM,
    NOT_APPLICABLE_BY_Q_SYSTEM,
    NOT_SELECTED,
    NOT_ENTERED_ARCHIVE,
    NOT_ENTERED_MERGE_POOL,
    UNOBSERVABLE_INVALID_Q_SYSTEM,
    UNOBSERVABLE_CALLER_DID_NOT_SUPPLY_Q_STATE,
    UNOBSERVABLE_CALLER_DID_NOT_SUPPLY_Q_ACTION,
    UNOBSERVABLE_CALLER_DID_NOT_SUPPLY_REQUESTER_SLOT,
    UNOBSERVABLE_CALLER_DID_NOT_SUPPLY_REQUESTER_ROLE,
    UNOBSERVABLE_CALLER_DID_NOT_SUPPLY_CANDIDATE_VIEW,
    UNOBSERVABLE_CALLER_DID_NOT_SUPPLY_DIRECTIONAL_SCORES,
    UNOBSERVABLE_CALLER_DID_NOT_SUPPLY_TEACHER_SOURCE,
    UNOBSERVABLE_CALLER_DID_NOT_SUPPLY_CACHE_TYPE,
    UNOBSERVABLE_CALLER_DID_NOT_SUPPLY_OFFSPRING_LINK,
    UNOBSERVABLE_CALLER_DID_NOT_SUPPLY_OFFSPRING_IMPROVEMENT,
    UNOBSERVABLE_CALLER_DID_NOT_SUPPLY_PARENT,
    UNOBSERVABLE_INVALID_TEACHER_SOURCE,
    UNOBSERVABLE_INVALID_CACHE_TYPE,
    UNOBSERVABLE_NO_GENERATION_HOOK,
    UNOBSERVABLE_NO_EVALUATION_HOOK,
    UNOBSERVABLE_NO_LOCAL_ACCEPTANCE_HOOK,
    UNOBSERVABLE_NO_MERGE_POOL_HOOK,
    UNOBSERVABLE_NO_PDDR_ROUND_POOL,
    UNOBSERVABLE_NO_PERSONAL_ARCHIVE_HOOK,
    UNOBSERVABLE_NO_GLOBAL_ARCHIVE_HOOK,
    UNOBSERVABLE_NO_NEXT_GENERATION_HOOK,
    UNOBSERVABLE_NO_LATER_TEACHER_HOOK,
    UNOBSERVABLE_NO_LATER_OFFSPRING_HOOK,
    UNOBSERVABLE_NO_TEACHER_USE_EVENTS,
    UNOBSERVABLE_NO_MATCHING_SCOPE_EVENT,
    UNOBSERVABLE_PDDR_EVENT_ID_NOT_FOUND,
    UNOBSERVABLE_EVENT_ID_NOT_FOUND,
    UNOBSERVABLE_DUPLICATE_BACKFILL,
    UNOBSERVABLE_CANDIDATE_ID_MISMATCH
  }

  /** Archive side used by the CA-TA lifecycle callbacks. */
  public enum ArchiveKind {
    PERSONAL,
    GLOBAL
  }

  /** Stable, non-evidentiary event key. It is never used as a fingerprint. */
  public static String eventId(String prefix, long sequence) {
    if (prefix == null || prefix.length() == 0 || sequence < 1L) {
      throw new IllegalArgumentException("invalid observation event id");
    }
    return prefix + String.format(Locale.ROOT, "%08d", sequence);
  }

  /**
   * Returns the real four-vector fingerprint used by the ZhangBo code path.
   * No digest is applied here: a digest is an identity key, not the evidence
   * requested by the diagnostic schema.
   */
  public static String fingerprint(PermutationSolution<Integer> solution) {
    if (solution == null) return "";
    return ZhangBoQgController.fingerprint(solution);
  }

  /** The legacy Telemetry bridge currently transports this key as SHA-256. */
  public static String transportFingerprint(String rawFingerprint) {
    if (rawFingerprint == null) return "";
    try {
      byte[] digest = MessageDigest.getInstance("SHA-256")
          .digest(rawFingerprint.getBytes(StandardCharsets.UTF_8));
      StringBuilder result = new StringBuilder(digest.length * 2);
      for (byte value : digest) {
        result.append(String.format(Locale.ROOT, "%02x", value & 0xff));
      }
      return result.toString();
    } catch (NoSuchAlgorithmException exception) {
      throw new IllegalStateException("SHA-256 unavailable", exception);
    }
  }

  /** RFC-4180 compatible CSV cell; simple enum/numeric cells remain readable. */
  public static String csv(Object value) {
    if (value == null) return "";
    String text = String.valueOf(value);
    if (text.indexOf(',') < 0 && text.indexOf('"') < 0
        && text.indexOf('\n') < 0 && text.indexOf('\r') < 0) {
      return text;
    }
    return '"' + text.replace("\"", "\"\"") + '"';
  }

  public static String number(Double value) {
    if (value == null || !Double.isFinite(value)) return "";
    return String.format(Locale.ROOT, "%.12f", value);
  }

  public static String objectives(PermutationSolution<Integer> solution) {
    if (solution == null) return "";
    return number(solution.getObjective(0)) + ";"
        + number(solution.getObjective(1)) + ";"
        + number(solution.getObjective(6));
  }

  public static boolean isFinite(Double value) {
    return value != null && Double.isFinite(value);
  }
}
