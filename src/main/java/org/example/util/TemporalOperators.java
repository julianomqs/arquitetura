package org.example.util;

import java.time.temporal.Temporal;
import java.util.List;

import lombok.Builder;
import lombok.Getter;

@Builder
@Getter
public class TemporalOperators {

  @Builder
  @Getter
  public static class TemporalRange {
    private Temporal start;
    private Temporal end;
  }

  private Temporal eq;
  private Temporal ne;
  private TemporalRange between;
  private TemporalRange notBetween;
  private Temporal gt;
  private Temporal ge;
  private Temporal lt;
  private Temporal le;
  private List<Temporal> in;
  private List<Temporal> notIn;
}
