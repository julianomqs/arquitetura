package org.example;

import io.github.cdimascio.dotenv.Dotenv;
import org.jooq.codegen.GenerationTool;
import org.jooq.meta.jaxb.Configuration;
import org.jooq.meta.jaxb.Database;
import org.jooq.meta.jaxb.Generate;
import org.jooq.meta.jaxb.Generator;
import org.jooq.meta.jaxb.Jdbc;
import org.jooq.meta.jaxb.Target;

public class JooqTablesGenerator {

  public static void main(String[] args) {
    Dotenv dotenv = Dotenv.load();

    var generate = new Generate()
        .withJavaTimeTypes(true)
        .withRecords(false);

    var config = new Configuration()
        .withJdbc(new Jdbc()
            .withDriver("com.mysql.jdbc.Driver")
            .withUrl(dotenv.get("DB_URL"))
            .withUser(dotenv.get("DB_USER"))
            .withPassword(dotenv.get("DB_PASSWORD")))
        .withGenerator(new Generator()
            .withDatabase(new Database()
                .withName("org.jooq.meta.mysql.MySQLDatabase")
                .withIncludes(".*")
                .withInputSchema("invoice")
                .withOutputSchemaToDefault(true)
                .withUnsignedTypes(false))
            .withGenerate(generate)
            .withTarget(new Target()
                .withPackageName("org.example.jooq")
                .withDirectory("src/main/java")));

    try {
      GenerationTool.generate(config);
    } catch (Exception e) {
      e.printStackTrace();
    }
  }
}
