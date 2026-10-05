package org.example;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Scanner;

public class MigrationGenerator {

  public static void main(String[] args) {
    var scanner = new Scanner(System.in);
    System.out.print("Enter migration name: ");
    var migrationName = scanner.nextLine();
    scanner.close();

    var timestamp = new SimpleDateFormat("yyyyMMddHHmmssSSS").format(new Date());
    var fileName = "V" + timestamp + "__" + migrationName + ".sql";
    var filePath = Paths.get("src/main/resources/db/migration", fileName);

    try {
      Files.createDirectories(filePath.getParent());
      Files.createFile(filePath);
      System.out.println("Created file: " + filePath.toAbsolutePath());

      var content = "-- Migration script for " + migrationName + "\n-- Add your SQL statements here\n";
      Files.writeString(filePath, content);
    } catch (IOException e) {
      e.printStackTrace();
    }
  }
}
