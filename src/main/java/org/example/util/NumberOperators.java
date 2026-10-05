package org.example.util;

import java.util.List;

import lombok.Builder;
import lombok.Getter;

@Builder
@Getter
public class NumberOperators {

  @Builder
  @Getter
  public static class NumberRange {
    private Number start;
    private Number end;
  }

  private Number eq;
  private Number ne;
  private NumberRange between;
  private NumberRange notBetween;
  private Number gt;
  private Number ge;
  private Number lt;
  private Number le;
  private List<Number> in;
  private List<Number> notIn;
}
