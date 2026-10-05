package org.example.constraint;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.annotation.Annotation;
import java.util.List;
import java.lang.reflect.Proxy;

import jakarta.validation.ConstraintValidatorContext;
import jakarta.validation.Payload;
import org.junit.jupiter.api.Test;

class UniqueValidatorTest {
  record Item(Integer id, Integer product) {}

  @Test
  void acceptsNullOrEmptyList() {
    var validator = validator("product");
    var context = new ContextDouble();
    assertTrue(validator.isValid(null, context.context));
    assertTrue(validator.isValid(List.of(), context.context));
  }

  @Test
  void rejectsRepeatedConfiguredValues() {
    var validator = validator("product");
    var context = new ContextDouble();

    assertFalse(validator.isValid(List.of(new Item(1, 42), new Item(2, 42)), context.context));
    assertTrue(context.defaultViolationDisabled);
    assertEquals("Duplicado com base nos campos: product", context.template);
  }

  @Test
  void supportsCompositeUniqueness() {
    assertTrue(validator("id", "product").isValid(
        List.of(new Item(1, 10), new Item(1, 11)), new ContextDouble().context));
  }

  private UniqueValidator validator(String... fields) {
    var validator = new UniqueValidator();
    validator.initialize(new Unique() {
      @Override public String[] value() { return fields; }
      @Override public String message() { return "Valores duplicados encontrados."; }
      @Override public Class<?>[] groups() { return new Class<?>[0]; }
      @Override public Class<? extends Payload>[] payload() { return new Class[0]; }
      @Override public Class<? extends Annotation> annotationType() { return Unique.class; }
    });
    return validator;
  }

  private static class ContextDouble {
    private String template;
    private boolean defaultViolationDisabled;
    private ConstraintValidatorContext context;

    ContextDouble() {
      var builder = (ConstraintValidatorContext.ConstraintViolationBuilder) Proxy.newProxyInstance(
          ConstraintValidatorContext.ConstraintViolationBuilder.class.getClassLoader(),
          new Class<?>[] { ConstraintValidatorContext.ConstraintViolationBuilder.class },
          (proxy, method, args) -> {
            if (method.getName().equals("addConstraintViolation")) return context;
            return null;
          });
      context = (ConstraintValidatorContext) Proxy.newProxyInstance(
          ConstraintValidatorContext.class.getClassLoader(), new Class<?>[] { ConstraintValidatorContext.class },
          (proxy, method, args) -> {
            if (method.getName().equals("buildConstraintViolationWithTemplate")) {
              template = (String) args[0];
              return builder;
            }
            if (method.getName().equals("disableDefaultConstraintViolation")) {
              defaultViolationDisabled = true;
            }
            return null;
          });
    }
  }
}
