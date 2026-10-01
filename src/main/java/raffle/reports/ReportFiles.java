package raffle.reports;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/** Saves a printed document in the {@code reports} folder, each under a new name made of the time and the kind. */
public final class ReportFiles {

   private static final DateTimeFormatter STAMP = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss");

   private ReportFiles() {
   }

   /**
    * @param kind a short ASCII name such as {@code sales-report}, part of the file name
    * @return the file that was written
    */
   public static Path write(Path reportsDir, String kind, String html) throws IOException {
      Files.createDirectories(reportsDir);
      Path file = reportsDir.resolve(STAMP.format(LocalDateTime.now()) + "-" + kind + ".html");
      for (int copy = 2; Files.exists(file); copy++) {
         file = reportsDir.resolve(STAMP.format(LocalDateTime.now()) + "-" + kind + "-" + copy + ".html");
      }// end of for loop
      return Files.writeString(file, html, StandardCharsets.UTF_8);
   }// end of write method

}// end of ReportFiles class
