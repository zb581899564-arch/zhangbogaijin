package org.uma.jmetal.algorithm.multiobjective.mypso.zhangbo;

import java.io.Serializable;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.AbstractList;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/** Bounded formal-run event evidence with a deterministic rolling SHA-256. */
public final class ZhangBoEventLog extends AbstractList<String> implements Serializable {
  private static final long serialVersionUID = 1L;
  public static final String FULL_CAPTURE_PROPERTY = "zhangbo.events.fullCapture";
  public static final String CAPACITY_PROPERTY = "zhangbo.events.capacity";
  private static final int DEFAULT_CAPACITY = 4096;

  private final boolean fullCapture;
  private final int capacity;
  private final ArrayDeque<String> retained;
  private long totalCount;
  private byte[] rollingHash = new byte[32];

  public ZhangBoEventLog() {
    this(Boolean.getBoolean(FULL_CAPTURE_PROPERTY),
        Integer.getInteger(CAPACITY_PROPERTY, DEFAULT_CAPACITY));
  }

  ZhangBoEventLog(boolean fullCapture, int capacity) {
    if (capacity < 1) throw new IllegalArgumentException("event capacity must be positive");
    this.fullCapture = fullCapture;
    this.capacity = capacity;
    this.retained = new ArrayDeque<>(Math.min(capacity, DEFAULT_CAPACITY));
  }

  @Override public boolean add(String value) {
    if (value == null) throw new IllegalArgumentException("event must not be null");
    rollingHash = hash(rollingHash, value);
    totalCount++;
    if (!fullCapture && retained.size() == capacity) retained.removeFirst();
    retained.addLast(value);
    modCount++;
    return true;
  }

  @Override public String get(int index) {
    if (index < 0 || index >= retained.size()) throw new IndexOutOfBoundsException();
    int current = 0;
    for (String value : retained) {
      if (current++ == index) return value;
    }
    throw new IndexOutOfBoundsException();
  }

  @Override public int size() { return retained.size(); }

  @Override public Iterator<String> iterator() { return retained.iterator(); }

  @Override public void clear() {
    retained.clear();
    totalCount = 0L;
    rollingHash = new byte[32];
    modCount++;
  }

  public long getTotalCount() { return totalCount; }
  public boolean isFullCapture() { return fullCapture; }
  public int getCapacity() { return capacity; }
  public List<String> snapshot() { return new ArrayList<>(retained); }
  public String rollingSha256() { return hex(rollingHash); }

  private static byte[] hash(byte[] previous, String value) {
    try {
      MessageDigest digest = MessageDigest.getInstance("SHA-256");
      digest.update(previous);
      digest.update((byte) '\n');
      digest.update(value.getBytes(StandardCharsets.UTF_8));
      return digest.digest();
    } catch (NoSuchAlgorithmException exception) {
      throw new IllegalStateException(exception);
    }
  }

  private static String hex(byte[] value) {
    StringBuilder result = new StringBuilder(value.length * 2);
    for (byte item : value) result.append(String.format("%02x", item & 0xff));
    return result.toString();
  }
}
