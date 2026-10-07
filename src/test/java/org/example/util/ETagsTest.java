package org.example.util;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

import jakarta.ws.rs.ClientErrorException;

class ETagsTest {

  @Test
  void missingOrBlankIfMatchIsPreconditionRequired() {
    assertEquals(428, assertThrows(ClientErrorException.class, () -> ETags.require(null, 3)).getResponse().getStatus());
    assertEquals(428, assertThrows(ClientErrorException.class, () -> ETags.require("  ", 3)).getResponse().getStatus());
  }

  @Test
  void wildcardMatchesAnyVersion() {
    assertDoesNotThrow(() -> ETags.require("*", 3));
  }

  @Test
  void acceptsQuotedWeakAndListedValues() {
    assertDoesNotThrow(() -> ETags.require("\"3\"", 3));
    assertDoesNotThrow(() -> ETags.require("3", 3));
    assertDoesNotThrow(() -> ETags.require("W/\"3\"", 3));
    assertDoesNotThrow(() -> ETags.require("\"1\", \"3\"", 3));
  }

  @Test
  void mismatchIsPreconditionFailed() {
    var error = assertThrows(ClientErrorException.class, () -> ETags.require("\"2\"", 3));
    assertEquals(412, error.getResponse().getStatus());
    assertThrows(ClientErrorException.class, () -> ETags.require("abc", 3));
  }
}
