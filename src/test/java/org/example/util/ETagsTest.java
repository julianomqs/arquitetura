package org.example.util;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

import jakarta.ws.rs.ClientErrorException;

class ETagsTest {

  @Test
  void withoutIfMatchThereIsNoCheck() {
    assertDoesNotThrow(() -> ETags.check(null, 3));
    assertDoesNotThrow(() -> ETags.check("  ", 3));
    assertDoesNotThrow(() -> ETags.check("*", 3));
  }

  @Test
  void acceptsQuotedWeakAndListedValues() {
    assertDoesNotThrow(() -> ETags.check("\"3\"", 3));
    assertDoesNotThrow(() -> ETags.check("3", 3));
    assertDoesNotThrow(() -> ETags.check("W/\"3\"", 3));
    assertDoesNotThrow(() -> ETags.check("\"1\", \"3\"", 3));
  }

  @Test
  void mismatchIsPreconditionFailed() {
    var error = assertThrows(ClientErrorException.class, () -> ETags.check("\"2\"", 3));
    assertEquals(412, error.getResponse().getStatus());
    assertThrows(ClientErrorException.class, () -> ETags.check("abc", 3));
  }
}
