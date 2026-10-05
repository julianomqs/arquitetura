package org.example.util;

import java.util.List;

import lombok.Builder;
import lombok.Getter;

@Builder
@Getter
public class StringOperators {

  private String eq;
  private String ne;
  private String startsWith;
  private String notStartsWith;
  private String endsWith;
  private String notEndsWith;
  private String contains;
  private String notContains;
  private List<String> in;
  private List<String> notIn;
}
