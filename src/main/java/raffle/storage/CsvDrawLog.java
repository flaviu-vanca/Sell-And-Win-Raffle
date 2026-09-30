package raffle.storage;

import raffle.models.DrawEntry;
import raffle.models.Player;
import raffle.utils.CsvUtil;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/** Append-only CSV log of every draw, kept as the audit trail that shows who won what and when. */
final class CsvDrawLog {

   static final String HEADER = "Time,Item,Ticket,Winner,Phone Number";

   private final Path file;

   CsvDrawLog(Path file) {
      this.file = file;
   }

   void record(String itemTitle, Player winner, Instant time) throws IOException {
      Files.createDirectories(file.toAbsolutePath().getParent());
      StringBuilder text = new StringBuilder();
      if (! Files.exists(file)) {
         text.append(HEADER).append(System.lineSeparator());
      }// end of if block
      text.append(String.join(",",
                              time.toString(),
                              CsvUtil.quote(itemTitle),
                              String.valueOf(winner.getId()),
                              CsvUtil.quote(winner.getName()),
                              CsvUtil.quote(winner.getPhoneNumber())))
          .append(System.lineSeparator());
      Files.writeString(file, text, StandardCharsets.UTF_8, StandardOpenOption.CREATE, StandardOpenOption.APPEND);
   }// end of record method

   List<DrawEntry> readAll() throws IOException {
      List<DrawEntry> entries = new ArrayList<>();
      if (! Files.exists(file)) {
         return entries;
      }// end of if block
      List<String> lines = Files.readAllLines(file, StandardCharsets.UTF_8);
      for (int i = 1; i < lines.size(); i++) {
         if (lines.get(i).isBlank()) {
            continue;
         }// end of if block
         String[] fields = CsvUtil.parseLine(lines.get(i));
         if (fields.length >= 5) {
            try {
               entries.add(new DrawEntry(Instant.parse(fields[0]), fields[1], Integer.parseInt(fields[2].trim()), fields[3], fields[4]));
            } catch (RuntimeException e) {
               throw new IOException("Malformed line " + (i + 1) + " in " + file.getFileName() + ": " + e.getMessage(), e);
            }// end of try-catch block
         }// end of if block
      }// end of for loop
      return entries;
   }// end of readAll method

}// end of CsvDrawLog class
