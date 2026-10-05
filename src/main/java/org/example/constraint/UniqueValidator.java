package org.example.constraint;

import java.util.HashSet;
import java.util.List;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class UniqueValidator implements ConstraintValidator<Unique, List<?>> {

  private String[] fields;

  @Override
  public void initialize(Unique annotation) {
    this.fields = annotation.value();
  }

  @Override
  public boolean isValid(List<?> items, ConstraintValidatorContext context) {
    if (items == null || items.isEmpty()) {
      return true;
    }

    try {
      var seen = new HashSet<String>();

      for (var item : items) {
        if (item == null) {
          continue;
        }

        var key = new StringBuilder();

        for (var fieldName : fields) {
          var field = item.getClass().getDeclaredField(fieldName);
          field.setAccessible(true);

          var value = field.get(item);
          key.append(value).append("|");
        }

        if (!seen.add(key.toString())) {
          context.disableDefaultConstraintViolation();
          context.buildConstraintViolationWithTemplate("Duplicado com base nos campos: " + String.join(", ", fields))
              .addConstraintViolation();

          return false;
        }
      }
    } catch (Exception ex) {
      throw new RuntimeException("Erro durante a validação de duplicidade", ex);
    }

    return true;
  }
}
