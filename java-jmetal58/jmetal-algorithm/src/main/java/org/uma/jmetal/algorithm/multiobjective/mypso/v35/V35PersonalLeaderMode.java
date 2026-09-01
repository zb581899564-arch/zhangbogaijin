package org.uma.jmetal.algorithm.multiobjective.mypso.v35;

/**
 * Explicit personal-leader boundary used only by the V35 causal diagnostics.
 *
 * <p>{@link #AUTHOR_HISTORY} is the inherited non-archive path.
 * {@link #ARCHIVE_DIRECTIONAL} selects the deterministic directional best from
 * a lineage archive without constructing or training a Qp table.
 * {@link #QP_FOUR_ACTIONS} is the regular four-action Qp path.</p>
 */
public enum V35PersonalLeaderMode {
  AUTHOR_HISTORY,
  ARCHIVE_DIRECTIONAL,
  QP_FOUR_ACTIONS
}
