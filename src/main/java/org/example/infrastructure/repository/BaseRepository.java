package org.example.infrastructure.repository;

import static org.jooq.impl.DSL.noCondition;
import static org.jooq.impl.DSL.not;
import static org.jooq.impl.DSL.trueCondition;

import java.util.Arrays;
import java.util.Collection;
import java.util.Map;
import java.util.Objects;

import org.example.util.NumberOperators;
import org.example.util.SortOrder;
import org.example.util.TemporalOperators;
import org.jooq.Condition;
import org.jooq.Field;
import org.jooq.SortField;

import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Id;

@Dependent
public class BaseRepository<T> {

  @Inject
  private EntityManager entityManager;

  public T save(T entity) {
    return save(entity, false);
  }

  /**
   * Com forceVersionIncrement, alterações em coleções "mappedBy" (que não incrementam a versão do dono)
   * também invalidam gravações concorrentes sobre a mesma versão.
   */
  public T save(T entity, boolean forceVersionIncrement) {
    if (hasId(entity)) {
      entity = entityManager.merge(entity);

      if (forceVersionIncrement) {
        entityManager.lock(entity, jakarta.persistence.LockModeType.OPTIMISTIC_FORCE_INCREMENT);
      }
    } else {
      entityManager.persist(entity);
    }

    entityManager.flush();

    return entity;
  }

  public void remove(T entity) {
    if (entityManager.contains(entity)) {
      entityManager.remove(entity);
    } else {
      entityManager.remove(entityManager.merge(entity));
    }

    entityManager.flush();
  }

  public Condition buildWhere(Object filter, Map<String, Field<?>> fields) {
    if (filter == null) {
      return noCondition();
    }

    Objects.requireNonNull(fields, "fields é obrigatório");

    Condition condition = trueCondition();

    for (var field : filter.getClass().getDeclaredFields()) {
      field.setAccessible(true);

      try {
        var operatorObject = field.get(filter);

        if (operatorObject != null) {
          condition = condition.and(processOperator(field.getName(), operatorObject, fields));
        }
      } catch (IllegalAccessException e) {
        throw new RuntimeException("Error accessing filter field", e);
      }
    }

    return condition;
  }

  /**
   * Monta o ORDER BY respeitando a ordem pedida (campo "order" do record de sort, quando existir) e, por fim,
   * o id como desempate para que offset/limit sejam estáveis.
   */
  public SortField<?>[] buildSort(Object sort, Map<String, Field<?>> fields) {
    if (sort == null) {
      return new SortField[] {};
    }

    Objects.requireNonNull(fields, "fields é obrigatório");

    var byName = new java.util.LinkedHashMap<String, SortField<?>>();
    java.util.List<?> requestedOrder = java.util.List.of();

    for (var field : sort.getClass().getDeclaredFields()) {
      if (field.isSynthetic()) {
        continue;
      }

      field.setAccessible(true);

      try {
        var value = field.get(sort);

        if (field.getName().equals("order")) {
          if (value instanceof java.util.List<?> list) {
            requestedOrder = list;
          }

          continue;
        }

        if (value instanceof SortOrder sortOrder) {
          @SuppressWarnings("unchecked")
          var dbField = (Field<Object>) fields.get(field.getName());

          if (dbField == null) {
            throw new IllegalArgumentException("Field não encontrado: " + field.getName());
          }

          byName.put(field.getName(), sortOrder == SortOrder.ASC ? dbField.asc() : dbField.desc());
        }
      } catch (IllegalAccessException e) {
        throw new RuntimeException("Error accessing sort field", e);
      }
    }

    var ordered = new java.util.ArrayList<SortField<?>>();

    for (var name : requestedOrder) {
      var sortField = byName.remove(String.valueOf(name));

      if (sortField != null) {
        ordered.add(sortField);
      }
    }

    ordered.addAll(byName.values());

    var idField = fields.get("id");

    if (idField != null && ordered.stream().noneMatch(f -> f.getName().equals(idField.getName()))) {
      ordered.add(idField.asc());
    }

    return ordered.toArray(SortField<?>[]::new);
  }

  private boolean hasId(T entity) {
    for (var field : entity.getClass().getDeclaredFields()) {
      if (field.isAnnotationPresent(Id.class)) {
        field.setAccessible(true);

        try {
          if (field.get(entity) != null) {
            return true;
          }
        } catch (IllegalAccessException ex) {
          throw new RuntimeException(ex);
        }
      }
    }

    return false;
  }

  private Condition processOperator(String fieldName, Object operators, Map<String, Field<?>> fields) {
    Condition condition = trueCondition();

    for (var operatorField : operators.getClass().getDeclaredFields()) {
      operatorField.setAccessible(true);

      try {
        var value = operatorField.get(operators);

        if (value != null) {
          var operatorName = operatorField.getName();
          condition = condition.and(buildConditionForOperator(fieldName, operatorName, value, fields));
        }
      } catch (IllegalAccessException e) {
        throw new RuntimeException("Error accessing operator field", e);
      }
    }

    return condition;
  }

  private Object coerce(Field<Object> dbField, Object value) {
    if (value instanceof Collection<?> values) {
      return values.stream().map(v -> coerce(dbField, v)).toList();
    }

    Class<?> type = dbField.getType();

    if (value instanceof java.math.BigDecimal number
        && (type == Integer.class || type == Long.class || type == Short.class)) {
      try {
        var exact = number.toBigIntegerExact();
        return type == Integer.class ? (Object) Integer.valueOf(exact.intValueExact())
            : type == Long.class ? (Object) Long.valueOf(exact.longValueExact())
                : (Object) Short.valueOf(exact.shortValueExact());
      } catch (ArithmeticException ex) {
        throw new IllegalArgumentException("Valor inválido para o campo " + dbField.getName() + ": " + number);
      }
    }

    return value;
  }

  private Object[] rangeOf(Object value) {
    if (value instanceof NumberOperators.NumberRange r) {
      return new Object[] { r.getStart(), r.getEnd() };
    }

    if (value instanceof TemporalOperators.TemporalRange r) {
      return new Object[] { r.getStart(), r.getEnd() };
    }

    throw new IllegalArgumentException("Intervalo não suportado: " + value.getClass().getName());
  }

  private Condition buildConditionForOperator(String fieldName, String operator, Object value,
      Map<String, Field<?>> fields) {
    @SuppressWarnings("unchecked")
    var dbField = (Field<Object>) fields.get(fieldName);

    if (dbField == null) {
      throw new IllegalArgumentException("Field não encontrado: " + fieldName);
    }

    value = coerce(dbField, value);

    switch (operator) {
    case "eq":
      return dbField.eq(value);
    case "ne":
      return dbField.ne(value);
    case "gt":
      return dbField.gt(value);
    case "ge":
      return dbField.ge(value);
    case "lt":
      return dbField.lt(value);
    case "le":
      return dbField.le(value);
    case "between":
      var range = rangeOf(value);
      return dbField.between(coerce(dbField, range[0]), coerce(dbField, range[1]));
    case "notBetween":
      range = rangeOf(value);
      return dbField.notBetween(coerce(dbField, range[0]), coerce(dbField, range[1]));
    case "in":
      return dbField.in((Collection<?>) value);
    case "notIn":
      return dbField.notIn((Collection<?>) value);
    case "startsWith":
      return dbField.startsWith(value);
    case "notStartsWith":
      return not(dbField.startsWith(value));
    case "endsWith":
      return dbField.endsWith(value);
    case "notEndsWith":
      return not(dbField.endsWith(value));
    case "contains":
      return dbField.contains(value);
    case "notContains":
      return dbField.notContains(value);
    default:
      throw new UnsupportedOperationException("Unsupported operator: " + operator);
    }
  }
}
