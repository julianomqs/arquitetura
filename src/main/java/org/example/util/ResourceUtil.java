package org.example.util;

import java.lang.reflect.ParameterizedType;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import java.time.temporal.Temporal;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class ResourceUtil {

  @SuppressWarnings("unchecked")
  public static <T> T buildFilter(Class<T> filterClass, Map<String, Object> filterFields) {
    Objects.requireNonNull(filterClass, "filterClass não pode ser null.");
    Objects.requireNonNull(filterFields, "filterFields não pode ser null.");

    try {
      var builderMethod = ReflectionUtil.getMethod(filterClass, "builder");
      var builderInstance = builderMethod.invoke(null);

      for (var entry : filterFields.entrySet()) {
        var fieldName = entry.getKey();
        var value = entry.getValue();

        if (value == null) {
          continue;
        }

        if (value instanceof String valueStr) {
          var parts = valueStr.split("\\|", -1);

          if (parts.length > 2) {
            throw new IllegalArgumentException("Formato de filtro inválido. Apenas um '|' é permitido.");
          }

          var criteria = parts.length == 2 ? parts[0].trim() : "eq";
          var criteriaValue = parts.length == 2 ? parts[1].trim() : valueStr;

          switch (criteria) {
          case "eq", "ne", "gt", "ge", "lt", "le", "startsWith", "notStartsWith", "endsWith", "notEndsWith",
              "contains", "notContains" -> applySimpleFilter(builderInstance, fieldName, criteria, criteriaValue);

          case "between", "notBetween" -> {
            var range = criteriaValue.split(",");

            if (range.length != 2) {
              throw new IllegalArgumentException(
                  "Filtros 'between' e 'notBetween' devem ter exatamente dois valores separados por vírgula.");
            }

            applyRangeFilter(builderInstance, fieldName, criteria, range[0].trim(), range[1].trim());
          }
          case "in", "notIn" -> {
            var values = java.util.Arrays.stream(criteriaValue.split(","))
                .map(String::trim)
                .filter(v -> !v.isEmpty())
                .toArray(String[]::new);

            if (values.length == 0) {
              throw new IllegalArgumentException("Filtros 'in' e 'notIn' devem conter pelo menos um valor.");
            }

            applyListFilter(builderInstance, fieldName, criteria, List.of(values));
          }

          default -> throw new IllegalArgumentException("Critério de filtro inválido: " + criteria);
          }
        } else {
          applySimpleFilter(builderInstance, fieldName, "eq", value);
        }
      }

      var buildMethod = ReflectionUtil.getMethod(builderInstance.getClass(), "build");

      return (T) buildMethod.invoke(builderInstance);
    } catch (IllegalArgumentException ex) {
      throw ex;
    } catch (Exception ex) {
      throw new RuntimeException(ex);
    }
  }

  @SuppressWarnings("unchecked")
  public static <T> T buildSort(Class<T> sortClass, String sortParameter) {
    Objects.requireNonNull(sortClass, "sortClass não pode ser null.");

    if (sortParameter == null || sortParameter.isBlank()) {
      return null;
    }

    var sortFields = sortParameter.split(",");

    try {
      var builderMethod = ReflectionUtil.getMethod(sortClass, "builder");
      var builderInstance = builderMethod.invoke(null);

      var requestedOrder = new java.util.LinkedHashSet<String>();

      for (var sortField : sortFields) {
        var parts = sortField.split("\\|");

        if (parts.length != 2) {
          throw new IllegalArgumentException(
              "Formato de ordenação inválido. Esperado: campo|ordem (ex.: dateTime|DESC)");
        }

        var fieldName = parts[0];
        var sortOrder = parts[1].toUpperCase();

        var method = ReflectionUtil.getMethod(builderInstance.getClass(), fieldName);
        if (method == null || method.getParameterCount() != 1
            || method.getParameterTypes()[0] != SortOrder.class) {
          throw new IllegalArgumentException("Campo de ordenação inválido: " + fieldName);
        }
        SortOrder orderValue;
        try {
          orderValue = SortOrder.valueOf(sortOrder);
        } catch (IllegalArgumentException ex) {
          throw new IllegalArgumentException("Valor de ordenação inválido: " + parts[1] + ". Use ASC ou DESC.");
        }

        method.invoke(builderInstance, orderValue);
        requestedOrder.add(fieldName);
      }

      var orderMethod = ReflectionUtil.getMethod(builderInstance.getClass(), "order");

      if (orderMethod != null) {
        orderMethod.invoke(builderInstance, new java.util.ArrayList<>(requestedOrder));
      }

      var buildMethod = ReflectionUtil.getMethod(builderInstance.getClass(), "build");

      return (T) buildMethod.invoke(builderInstance);
    } catch (IllegalArgumentException ex) {
      throw new IllegalArgumentException(ex.getMessage(), ex);
    } catch (Exception ex) {
      throw new RuntimeException(ex);
    }
  }

  private static void applySimpleFilter(Object builderInstance, String fieldName, String criteria, Object value)
      throws Exception {
    var fieldMethod = requireFilterField(builderInstance, fieldName);
    var operatorType = fieldMethod.getParameterTypes()[0];

    var operatorBuilderMethod = ReflectionUtil.getMethod(operatorType, "builder");
    var operatorBuilder = operatorBuilderMethod.invoke(null);

    var criteriaMethod = requireCriteria(operatorBuilder, criteria, fieldName);

    var parameterType = criteriaMethod.getParameterTypes()[0];

    var parsedValue = value instanceof String ? parseValue((String) value, parameterType) : value;

    criteriaMethod.invoke(operatorBuilder, parsedValue);

    var operatorInstance = ReflectionUtil.getMethod(operatorBuilder.getClass(), "build").invoke(operatorBuilder);

    fieldMethod.invoke(builderInstance, operatorInstance);
  }

  private static void applyRangeFilter(Object builderInstance, String fieldName, String criteria, String start,
      String end) throws Exception {
    var fieldMethod = requireFilterField(builderInstance, fieldName);
    var operatorType = fieldMethod.getParameterTypes()[0];

    var operatorBuilderMethod = ReflectionUtil.getMethod(operatorType, "builder");
    var operatorBuilder = operatorBuilderMethod.invoke(null);

    var rangeType = requireCriteria(operatorBuilder, criteria, fieldName).getParameterTypes()[0];
    var rangeBuilderMethod = ReflectionUtil.getMethod(rangeType, "builder");
    var rangeBuilder = rangeBuilderMethod.invoke(null);

    var startMethod = ReflectionUtil.getMethod(rangeBuilder.getClass(), "start");
    var endMethod = ReflectionUtil.getMethod(rangeBuilder.getClass(), "end");

    startMethod.invoke(rangeBuilder, parseValue(start, startMethod.getParameterTypes()[0]));
    endMethod.invoke(rangeBuilder, parseValue(end, endMethod.getParameterTypes()[0]));

    var range = ReflectionUtil.getMethod(rangeBuilder.getClass(), "build").invoke(rangeBuilder);

    var criteriaMethod = requireCriteria(operatorBuilder, criteria, fieldName);
    criteriaMethod.invoke(operatorBuilder, range);

    var operatorInstance = ReflectionUtil.getMethod(operatorBuilder.getClass(), "build").invoke(operatorBuilder);

    fieldMethod.invoke(builderInstance, operatorInstance);
  }

  private static void applyListFilter(Object builderInstance, String fieldName, String criteria, List<?> values)
      throws Exception {
    var fieldMethod = requireFilterField(builderInstance, fieldName);
    var operatorType = fieldMethod.getParameterTypes()[0];

    var operatorBuilderMethod = ReflectionUtil.getMethod(operatorType, "builder");
    var operatorBuilder = operatorBuilderMethod.invoke(null);

    var criteriaMethod = requireCriteria(operatorBuilder, criteria, fieldName);
    var genericType = criteriaMethod.getGenericParameterTypes()[0];
    if (!(genericType instanceof ParameterizedType parameterizedType)
        || !(parameterizedType.getActualTypeArguments()[0] instanceof Class<?> elementType)) {
      throw new IllegalArgumentException("Tipo de lista não suportado para o filtro " + fieldName);
    }

    var parsedValues = values.stream()
        .map(value -> parseValue(value.toString(), elementType))
        .toList();

    criteriaMethod.invoke(operatorBuilder, parsedValues);

    var operatorInstance = ReflectionUtil.getMethod(operatorBuilder.getClass(), "build").invoke(operatorBuilder);

    fieldMethod.invoke(builderInstance, operatorInstance);
  }

  private static java.lang.reflect.Method requireFilterField(Object builderInstance, String fieldName) {
    var method = ReflectionUtil.getMethod(builderInstance.getClass(), fieldName);

    if (method == null) {
      throw new IllegalArgumentException("Campo de filtro inválido: " + fieldName);
    }

    return method;
  }

  private static java.lang.reflect.Method requireCriteria(Object operatorBuilder, String criteria,
      String fieldName) {
    var method = ReflectionUtil.getMethod(operatorBuilder.getClass(), criteria);

    if (method == null) {
      throw new IllegalArgumentException(
          "Critério '" + criteria + "' não suportado para o campo " + fieldName);
    }

    return method;
  }

  private static Object parseValue(String value, Class<?> targetType) {
    if (targetType == String.class) {
      return value;
    } else if (targetType == Number.class) {
      return new BigDecimal(value);
    } else if (targetType == Boolean.class || targetType == boolean.class) {
      return Boolean.parseBoolean(value);
    } else if (targetType == Temporal.class) {
      return parseDate(value);
    } else if (targetType == List.class) {
      throw new IllegalArgumentException("Parsing de listas não suportado diretamente.");
    }

    throw new IllegalArgumentException("Tipo de destino não suportado: " + targetType.getName());
  }

  private static Temporal parseDate(String value) {
    try {
      return LocalDate.parse(value);
    } catch (DateTimeParseException ignore) {
    }

    try {
      return LocalDateTime.parse(value);
    } catch (DateTimeParseException ignore) {
    }


    throw new IllegalArgumentException("Formato de data inválido: " + value);
  }
}
