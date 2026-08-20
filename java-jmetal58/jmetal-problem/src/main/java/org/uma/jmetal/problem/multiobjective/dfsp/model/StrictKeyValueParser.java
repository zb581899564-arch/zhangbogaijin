package org.uma.jmetal.problem.multiobjective.dfsp.model;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;

final class StrictKeyValueParser {
  private StrictKeyValueParser() {
  }

  static Map<String, String> parse(InputStream stream, String sourceName) throws IOException {
    if (stream == null) {
      throw new IllegalArgumentException("Resource not found: " + sourceName);
    }
    return parse(new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8)), sourceName);
  }

  static Map<String, String> parse(String text, String sourceName) {
    try {
      return parse(new BufferedReader(new StringReader(text)), sourceName);
    } catch (IOException impossible) {
      throw new IllegalStateException(impossible);
    }
  }

  private static Map<String, String> parse(BufferedReader reader, String sourceName)
      throws IOException {
    Map<String, String> values = new LinkedHashMap<>();
    String line;
    int lineNumber = 0;
    while ((line = reader.readLine()) != null) {
      lineNumber++;
      String trimmed = line.trim();
      if (trimmed.isEmpty() || trimmed.startsWith("#")) {
        continue;
      }
      int separator = trimmed.indexOf('=');
      if (separator <= 0) {
        throw new IllegalArgumentException(
            sourceName + " line " + lineNumber + " must use key=value");
      }
      String key = trimmed.substring(0, separator).trim();
      String value = trimmed.substring(separator + 1).trim();
      if (key.isEmpty() || value.isEmpty()) {
        throw new IllegalArgumentException(
            sourceName + " line " + lineNumber + " contains an empty key or value");
      }
      if (values.containsKey(key)) {
        throw new IllegalArgumentException(
            sourceName + " line " + lineNumber + " duplicates key " + key);
      }
      values.put(key, value);
    }
    return values;
  }
}
