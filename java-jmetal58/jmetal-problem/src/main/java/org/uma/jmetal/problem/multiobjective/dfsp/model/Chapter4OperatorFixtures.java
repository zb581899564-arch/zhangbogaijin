package org.uma.jmetal.problem.multiobjective.dfsp.model;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Structured, non-executing fixtures transcribed from ESWA Figs. 5-6. */
public final class Chapter4OperatorFixtures {
  public static final String RESOURCE = "/dfsp/chapter4/eswa-2026-operator-fixtures.properties";

  public enum Fig6CaseId {
    FA_CROSSOVER("faCrossover"),
    FA_MUTATION("faMutation"),
    MA_CROSSOVER("maCrossover"),
    MA_MUTATION("maMutation"),
    WA_CROSSOVER("waCrossover"),
    WA_MUTATION("waMutation");

    private final String key;

    Fig6CaseId(String key) {
      this.key = key;
    }
  }

  public static final class Fig5Fixture {
    private final List<Integer> x;
    private final List<Integer> y;
    private final List<SwapPair> exchangeSequence;
    private final double r1;
    private final int selectedCount;
    private final SwapPair selectedPair;
    private final List<Integer> expected;

    private Fig5Fixture(Map<String, String> values) {
      x = immutableIntegers(values.get("fig5.x"), "fig5.x");
      y = immutableIntegers(values.get("fig5.y"), "fig5.y");
      exchangeSequence = parsePairs(values.get("fig5.exchangeSequence"));
      r1 = Double.parseDouble(values.get("fig5.r1"));
      selectedCount = Integer.parseInt(values.get("fig5.selectedCount"));
      selectedPair = parsePair(values.get("fig5.selectedPair"), "fig5.selectedPair");
      expected = immutableIntegers(values.get("fig5.expected"), "fig5.expected");
    }

    public List<Integer> getX() { return new ArrayList<>(x); }
    public List<Integer> getY() { return new ArrayList<>(y); }
    public List<SwapPair> getExchangeSequence() { return new ArrayList<>(exchangeSequence); }
    public double getR1() { return r1; }
    public int getSelectedCount() { return selectedCount; }
    public SwapPair getSelectedPair() { return selectedPair; }
    public List<Integer> getExpected() { return new ArrayList<>(expected); }
  }

  public static final class SwapPair {
    private final int firstPosition;
    private final int secondPosition;

    public SwapPair(int firstPosition, int secondPosition) {
      this.firstPosition = firstPosition;
      this.secondPosition = secondPosition;
    }

    public int getFirstPosition() { return firstPosition; }
    public int getSecondPosition() { return secondPosition; }

    @Override
    public boolean equals(Object other) {
      if (!(other instanceof SwapPair)) {
        return false;
      }
      SwapPair that = (SwapPair) other;
      return firstPosition == that.firstPosition && secondPosition == that.secondPosition;
    }

    @Override
    public int hashCode() {
      return 31 * firstPosition + secondPosition;
    }

    @Override
    public String toString() {
      return "(" + firstPosition + ',' + secondPosition + ')';
    }
  }

  public static final class Fig6Case {
    private final Fig6CaseId id;
    private final String operation;
    private final String changedVector;
    private final List<Integer> selectedPositions;
    private final List<Integer> parentVector;
    private final List<Integer> expectedFa;
    private final List<Integer> expectedMa;
    private final List<Integer> expectedWa;
    private final String events;

    private Fig6Case(Map<String, String> values, Fig6CaseId id) {
      this.id = id;
      String prefix = "fig6." + id.key + '.';
      operation = values.get(prefix + "operation");
      changedVector = values.get(prefix + "changedVector");
      selectedPositions = immutableIntegers(
          values.get(prefix + "selectedPositions"), prefix + "selectedPositions");
      String parent = values.get(prefix + "parentVector");
      parentVector = "none".equals(parent)
          ? Collections.<Integer>emptyList()
          : immutableIntegers(parent, prefix + "parentVector");
      expectedFa = immutableIntegers(values.get(prefix + "expected.FA"), prefix + "expected.FA");
      expectedMa = immutableIntegers(values.get(prefix + "expected.MA"), prefix + "expected.MA");
      expectedWa = immutableIntegers(values.get(prefix + "expected.WA"), prefix + "expected.WA");
      events = values.get(prefix + "events");
    }

    public Fig6CaseId getId() { return id; }
    public String getOperation() { return operation; }
    public String getChangedVector() { return changedVector; }
    public List<Integer> getSelectedPositions() { return new ArrayList<>(selectedPositions); }
    public List<Integer> getParentVector() { return new ArrayList<>(parentVector); }
    public List<Integer> getExpectedFa() { return new ArrayList<>(expectedFa); }
    public List<Integer> getExpectedMa() { return new ArrayList<>(expectedMa); }
    public List<Integer> getExpectedWa() { return new ArrayList<>(expectedWa); }
    public String getEvents() { return events; }
  }

  private final int indexBase;
  private final Fig5Fixture fig5;
  private final List<Integer> fig6BaseFa;
  private final List<Integer> fig6BaseMa;
  private final List<Integer> fig6BaseWa;
  private final int fig6Factory2Stage1WorkerCount;
  private final Map<Fig6CaseId, Fig6Case> fig6Cases;

  private Chapter4OperatorFixtures(Map<String, String> values) {
    requireKeys(values);
    if (!"1".equals(values.get("schemaVersion"))) {
      throw new IllegalArgumentException("Unsupported operator fixture schemaVersion");
    }
    indexBase = Integer.parseInt(values.get("indexBase"));
    if (indexBase != 1) {
      throw new IllegalArgumentException("Paper operator fixtures must use indexBase=1");
    }
    fig5 = new Fig5Fixture(values);
    fig6BaseFa = immutableIntegers(values.get("fig6.base.FA"), "fig6.base.FA");
    fig6BaseMa = immutableIntegers(values.get("fig6.base.MA"), "fig6.base.MA");
    fig6BaseWa = immutableIntegers(values.get("fig6.base.WA"), "fig6.base.WA");
    fig6Factory2Stage1WorkerCount =
        Integer.parseInt(values.get("fig6.factory2.stage1.workerCount"));
    fig6Cases = new EnumMap<>(Fig6CaseId.class);
    for (Fig6CaseId id : Fig6CaseId.values()) {
      fig6Cases.put(id, new Fig6Case(values, id));
    }
  }

  public static Chapter4OperatorFixtures load() {
    try (InputStream stream = Chapter4OperatorFixtures.class.getResourceAsStream(RESOURCE)) {
      return new Chapter4OperatorFixtures(StrictKeyValueParser.parse(stream, RESOURCE));
    } catch (IOException exception) {
      throw new IllegalStateException("Cannot load " + RESOURCE, exception);
    }
  }

  public int getIndexBase() { return indexBase; }
  public Fig5Fixture getFig5() { return fig5; }
  public List<Integer> getFig6BaseFa() { return new ArrayList<>(fig6BaseFa); }
  public List<Integer> getFig6BaseMa() { return new ArrayList<>(fig6BaseMa); }
  public List<Integer> getFig6BaseWa() { return new ArrayList<>(fig6BaseWa); }
  public int getFig6Factory2Stage1WorkerCount() { return fig6Factory2Stage1WorkerCount; }
  public Fig6Case getFig6Case(Fig6CaseId id) { return fig6Cases.get(id); }

  private static List<SwapPair> parsePairs(String text) {
    String[] tokens = text.split(";", -1);
    List<SwapPair> result = new ArrayList<>(tokens.length);
    for (String token : tokens) {
      result.add(parsePair(token, "fig5.exchangeSequence"));
    }
    return Collections.unmodifiableList(result);
  }

  private static SwapPair parsePair(String text, String field) {
    List<Integer> pair = immutableIntegers(text, field);
    if (pair.size() != 2) {
      throw new IllegalArgumentException(field + " must contain exactly two positions");
    }
    return new SwapPair(pair.get(0), pair.get(1));
  }

  private static List<Integer> immutableIntegers(String text, String field) {
    String[] tokens = text.split(",", -1);
    List<Integer> result = new ArrayList<>(tokens.length);
    for (int index = 0; index < tokens.length; index++) {
      try {
        result.add(Integer.parseInt(tokens[index].trim()));
      } catch (NumberFormatException exception) {
        throw new IllegalArgumentException(
            field + " position " + index + " is not an integer: " + tokens[index], exception);
      }
    }
    return Collections.unmodifiableList(result);
  }

  private static void requireKeys(Map<String, String> values) {
    Set<String> required = new HashSet<>(Arrays.asList(
        "schemaVersion", "indexBase", "fig5.x", "fig5.y", "fig5.exchangeSequence",
        "fig5.r1", "fig5.selectedCount", "fig5.selectedPair", "fig5.expected",
        "fig6.base.FA", "fig6.base.MA", "fig6.base.WA",
        "fig6.factory2.stage1.workerCount"));
    for (Fig6CaseId id : Fig6CaseId.values()) {
      String prefix = "fig6." + id.key + '.';
      required.add(prefix + "operation");
      required.add(prefix + "changedVector");
      required.add(prefix + "selectedPositions");
      required.add(prefix + "parentVector");
      required.add(prefix + "expected.FA");
      required.add(prefix + "expected.MA");
      required.add(prefix + "expected.WA");
      required.add(prefix + "events");
    }
    if (!values.keySet().equals(required)) {
      Set<String> missing = new HashSet<>(required);
      missing.removeAll(values.keySet());
      Set<String> unknown = new HashSet<>(values.keySet());
      unknown.removeAll(required);
      throw new IllegalArgumentException(
          "Operator fixture keys mismatch; missing=" + missing + ", unknown=" + unknown);
    }
  }
}
