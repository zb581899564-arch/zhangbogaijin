package org.uma.jmetal.algorithm.multiobjective.mypso.zhangbo;

import java.io.Serializable;
import org.uma.jmetal.problem.multiobjective.dfsp.fatigue.ZhangBoFatigueInstanceData;
import org.uma.jmetal.problem.multiobjective.dfsp.fatigue.ZhangBoFatigueParameters;

/**
 * Algorithm-side contract for the data required by CFVF and local search.
 *
 * <p>The canonical problem adapter can implement this contract without
 * coupling the algorithm to the legacy {@code ZhangBoEDHHFSPW} class.  The
 * current checkout still supplies a legacy bridge in
 * {@link ZhangBoProblemContexts}; no problem-module source is changed here.</p>
 */
public interface ZhangBoProblemContext extends Serializable {
  ZhangBoFatigueInstanceData getFatigueInstanceData();

  ZhangBoFatigueParameters getFatigueParameters();
}
