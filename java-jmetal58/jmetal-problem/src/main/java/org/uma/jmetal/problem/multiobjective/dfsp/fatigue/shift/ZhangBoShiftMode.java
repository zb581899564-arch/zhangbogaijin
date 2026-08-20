package org.uma.jmetal.problem.multiobjective.dfsp.fatigue.shift;

/** Explicit decoder refinement mode. */
public enum ZhangBoShiftMode {
  NONE(false, false),
  LEFT_ONLY(true, false),
  RIGHT_ONLY(false, true),
  LEFT_RIGHT(true, true);

  private final boolean left;
  private final boolean right;

  ZhangBoShiftMode(boolean left, boolean right) {
    this.left = left;
    this.right = right;
  }

  public boolean usesLeftShift() { return left; }
  public boolean usesRightShift() { return right; }
}
