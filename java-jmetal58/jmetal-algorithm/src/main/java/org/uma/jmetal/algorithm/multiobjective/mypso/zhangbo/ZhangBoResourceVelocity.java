package org.uma.jmetal.algorithm.multiobjective.mypso.zhangbo;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Immutable resource-action memory carried by a particle copy. */
public final class ZhangBoResourceVelocity implements Serializable {
  private static final long serialVersionUID = 1L;
  public static final ZhangBoResourceVelocity EMPTY = new ZhangBoResourceVelocity(Collections.<ZhangBoResourceAction>emptyList());
  private final List<ZhangBoResourceAction> actions;

  public ZhangBoResourceVelocity(List<ZhangBoResourceAction> actions) {
    if (actions == null) throw new IllegalArgumentException("actions");
    this.actions = Collections.unmodifiableList(new ArrayList<>(actions));
  }

  public List<ZhangBoResourceAction> getActions() { return actions; }
}
