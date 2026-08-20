package org.uma.jmetal.problem.multiobjective.dfsp.fatigue;

import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;

/** REPORT-mode UTF-8 decoder shared by the persisted scenario codecs. */
final class StrictUtf8 {
  private StrictUtf8() { }

  static String decode(byte[] bytes, String source) {
    CharsetDecoder decoder = StandardCharsets.UTF_8.newDecoder()
        .onMalformedInput(CodingErrorAction.REPORT)
        .onUnmappableCharacter(CodingErrorAction.REPORT);
    try {
      CharBuffer decoded = decoder.decode(ByteBuffer.wrap(bytes));
      return decoded.toString();
    } catch (CharacterCodingException exception) {
      throw new IllegalArgumentException("Malformed UTF-8: " + source, exception);
    }
  }
}
