package org.uma.jmetal.algorithm.multiobjective.mypso.v35;

/**
 * FC-5.2：评估来源上下文标记（纯观察旁路）。
 *
 * <p>只回答一个问题：当前正在执行的 full FM3 评估属于哪个搜索算子。
 * 算法侧在每次正式评估入口前 {@link #begin(Source)}，finally 中 {@link #end()}；
 * 问题侧（{@code ZhangBoCanonicalProductionProblem}）在 FM3 落盘后读取当前 tag，
 * 用于区分 GLOBAL_CFVF / INTER_FACTORY_LS / INTRA_FACTORY_VNS / CATA_TEST /
 * CATA_APPLY / INITIAL_POPULATION / FINAL_EVALUATE / SHADOW（shadow 诊断采样）。</p>
 *
 * <p>纪律：不调随机数、不改变 FE、不改变控制流与候选；ThreadLocal 由 try/finally
 * 保证不串到下一次评估；shadow 评估（{@code v35ShadowProblem}）显式标记 SHADOW，
 * 使 FC-5.1 的 best-ever 口径能被 FC-5.2 复核（shadow 评估不消耗正式 FE）。</p>
 */
public final class V35EvaluationSourceContext {

  /** 与用户 FC-5.2 规范一致的来源清单（FINAL_EVALUATE 为 Q 轮后补评，SHADOW 为诊断旁路）。 */
  public enum Source {
    INITIAL_POPULATION,
    GLOBAL_CFVF,
    INTER_FACTORY_LS,
    INTRA_FACTORY_VNS,
    CATA_TEST,
    CATA_APPLY,
    FINAL_EVALUATE,
    SHADOW,
    OTHER
  }

  private static volatile boolean enabled = false;
  private static final ThreadLocal<Source> CURRENT = new ThreadLocal<>();

  private V35EvaluationSourceContext() {
  }

  public static void setEnabled(boolean value) {
    enabled = value;
    if (!value) {
      CURRENT.remove();
    }
  }

  public static boolean isEnabled() {
    return enabled;
  }

  /** 进入一个正式评估入口（try/finally 配对，纯观察）。 */
  public static void begin(Source source) {
    if (!enabled) {
      return;
    }
    CURRENT.set(source);
  }

  /** 退出评估入口，清除上下文（防止串到下一次评估）。 */
  public static void end() {
    if (!enabled) {
      return;
    }
    CURRENT.remove();
  }

  /** 当前评估来源；未启用或不在评估入口内返回 null（问题侧按 null 视为正式之外）。 */
  public static Source current() {
    if (!enabled) {
      return null;
    }
    return CURRENT.get();
  }

  public static String currentName() {
    Source source = current();
    return source == null ? "NONE" : source.name();
  }
}
