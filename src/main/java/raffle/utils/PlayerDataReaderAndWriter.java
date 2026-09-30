package raffle.utils;

import raffle.models.Player;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class PlayerDataReaderAndWriter {

   static final String HEADER = "ID,Name,Phone Number,Number of Tickets";

   public static List<Player> readPlayersFromFile(Path filePath) throws IOException {
      List<Player> players = new ArrayList<>();
      List<String> lines = Files.readAllLines(filePath);

      for (int i = 1; i < lines.size(); i++) {// line 0 is the header
         String line = lines.get(i);
         if (line.isBlank()) {
            continue;
         }// end of if block
         String[] fields = CsvUtil.parseLine(line);
         if (fields.length >= 4) {
            int id = Integer.parseInt(fields[0].trim());
            String name = fields[1];
            String phoneNumber = fields[2];
            String ticketsText = fields[3].trim();
            int numberOfTickets = ticketsText.isEmpty() ? 0 : Integer.parseInt(ticketsText);

            players.add(new Player(id, name, phoneNumber, numberOfTickets));
         }// end of if statement
      }// end of for loop
      return players;
   }// end of readPlayersFromFile method

   public static void writePlayersToCSV(List<Player> players, Path filePath) throws IOException {
      List<String> lines = new ArrayList<>();
      lines.add(HEADER);
      players.stream()
             .sorted(Comparator.comparingInt(Player::getId))
             .forEach(player -> lines.add(String.join(",",
                                                      String.valueOf(player.getId()),
                                                      CsvUtil.quote(player.getName()),
                                                      CsvUtil.quote(player.getPhoneNumber()),
                                                      String.valueOf(player.getNumberOfTickets()))));
      SafeFiles.writeLinesAtomically(filePath, lines);
   }// end of writePlayersToCSV method

}// end of PlayerDataReaderAndWriter class
