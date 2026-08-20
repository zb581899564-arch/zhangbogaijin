package org.uma.jmetal.problem.multiobjective.dfsp.model;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Canonical UTF-8 line codec for the four-vector solution. */
public final class DhhfspEncodingCodec {
  public static final String SCHEMA_VERSION = "1";
  private static final Set<String> REQUIRED_KEYS = new HashSet<>(Arrays.asList(
      "schemaVersion", "semanticTag", "indexBase", "JS", "FA", "MA", "WA"));

  private DhhfspEncodingCodec() {
  }

  public static String serialize(DhhfspFourVectorSolution solution, int indexBase) {
    requireIndexBase(indexBase);
    StringBuilder result = new StringBuilder();
    result.append("schemaVersion=").append(SCHEMA_VERSION).append('\n');
    result.append("semanticTag=").append(solution.getSemanticTag()).append('\n');
    result.append("indexBase=").append(indexBase).append('\n');
    appendVector(result, "JS", solution.getJobSequence(), indexBase);
    appendVector(result, "FA", solution.getFactoryAssignments(), indexBase);
    appendVector(result, "MA", solution.getMachineAssignments(), indexBase);
    appendVector(result, "WA", solution.getWorkerAssignments(), indexBase);
    return result.toString();
  }

  public static DhhfspFourVectorSolution deserialize(String text) {
    Map<String, String> values = StrictKeyValueParser.parse(text, "four-vector encoding");
    if (!values.keySet().equals(REQUIRED_KEYS)) {
      Set<String> missing = new HashSet<>(REQUIRED_KEYS);
      missing.removeAll(values.keySet());
      Set<String> unknown = new HashSet<>(values.keySet());
      unknown.removeAll(REQUIRED_KEYS);
      throw new IllegalArgumentException(
          "four-vector encoding keys mismatch; missing=" + missing + ", unknown=" + unknown);
    }
    if (!SCHEMA_VERSION.equals(values.get("schemaVersion"))) {
      throw new IllegalArgumentException(
          "Unsupported schemaVersion: " + values.get("schemaVersion"));
    }
    int indexBase = parseInteger(values.get("indexBase"), "indexBase");
    requireIndexBase(indexBase);
    return new DhhfspFourVectorSolution(
        parseVector(values.get("JS"), "JS", indexBase),
        parseVector(values.get("FA"), "FA", indexBase),
        parseVector(values.get("MA"), "MA", indexBase),
        parseVector(values.get("WA"), "WA", indexBase),
        values.get("semanticTag"));
  }

  private static void appendVector(
      StringBuilder target, String name, List<Integer> values, int indexBase) {
    target.append(name).append('=');
    for (int index = 0; index < values.size(); index++) {
      Integer value = values.get(index);
      if (value == null) {
        throw new IllegalArgumentException(name + " position " + index + " contains null");
      }
      if (index > 0) {
        target.append(',');
      }
      target.append(value + indexBase);
    }
    target.append('\n');
  }

  private static List<Integer> parseVector(String text, String name, int indexBase) {
    String[] tokens = text.split(",", -1);
    List<Integer> values = new ArrayList<>(tokens.length);
    for (int index = 0; index < tokens.length; index++) {
      if (tokens[index].trim().isEmpty()) {
        throw new IllegalArgumentException(name + " position " + index + " is empty");
      }
      values.add(parseInteger(tokens[index].trim(), name + " position " + index) - indexBase);
    }
    return values;
  }

  private static int parseInteger(String text, String field) {
    try {
      return Integer.parseInt(text);
    } catch (NumberFormatException exception) {
      throw new IllegalArgumentException(field + " is not an integer: " + text, exception);
    }
  }

  private static void requireIndexBase(int indexBase) {
    if (indexBase != 0 && indexBase != 1) {
      throw new IllegalArgumentException("indexBase must be 0 or 1: " + indexBase);
    }
  }
}
