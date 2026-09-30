package raffle.services;

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

/** Append-only log of every draw, kept as the audit trail that shows who won what and when. */
public final class DrawHistory {

   public record Entry(Instant time, String itemTitle, int ticketId, String winnerName, String phoneNumber) {
   }

   static final String HEADER = "Time,Item,Ticket,Winner,Phone Number";

   private final Path file;

   public DrawHistory(Path file) {
      this.file = file;
   }

   public void record(String itemTitle, Player winner, Instant time) throws IOException {
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

   public List<Entry> readAll() throws IOException {
      List<Entry> entries = new ArrayList<>();
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
            entries.add(new Entry(Instant.parse(fields[0]), fields[1], Integer.parseInt(fields[2].trim()), fields[3], fields[4]));
         }// end of if block
      }// end of for loop
      return entries;
   }// end of readAll method

   /** Ticket ids that already won a draw for this item. */
   public List<Integer> winningTicketIds(String itemTitle) throws IOException {
      return readAll().stream()
                      .filter(entry -> entry.itemTitle().equals(itemTitle))
                      .map(Entry::ticketId)
                      .toList();
   }// end of winningTicketIds method

}// end of DrawHistory class
