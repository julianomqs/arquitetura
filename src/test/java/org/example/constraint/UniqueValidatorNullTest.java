package org.example.constraint;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;

import org.junit.jupiter.api.Test;

class UniqueValidatorNullTest {
  @Test
  void ignoresNullElements() {
    var validator = new UniqueValidator();
    validator.initialize(new Unique() {
      public Class<? extends java.lang.annotation.Annotation> annotationType() {
        return Unique.class;
      }

      public String message() {
        return "";
      }

      public Class<?>[] groups() {
        return new Class<?>[0];
      }

      @SuppressWarnings("unchecked")
      public Class<? extends jakarta.validation.Payload>[] payload() {
        return new Class[0];
      }

      public String[] value() {
        return new String[] { "x" };
      }
    });

    assertTrue(validator.isValid(Arrays.asList(null, null), null));
  }
}
