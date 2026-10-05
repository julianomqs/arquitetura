package org.example;

import org.jooq.DSLContext;
import org.jooq.SQLDialect;
import org.jooq.impl.DSL;

import io.agroal.api.AgroalDataSource;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.context.RequestScoped;
import jakarta.enterprise.inject.Produces;
import jakarta.inject.Inject;

@ApplicationScoped
public class DSLContextProducer {

  @Inject
  AgroalDataSource defaultDataSource;

  @Produces
  @RequestScoped
  public DSLContext getDSLContext() {
    return DSL.using(defaultDataSource, SQLDialect.MYSQL);
  }
}
