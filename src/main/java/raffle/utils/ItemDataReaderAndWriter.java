package raffle.utils;

import raffle.models.Item;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class ItemDataReaderAndWriter {

   static final String HEADER = "Image,Title,Description,Available Tickets,Ticket Price";

   public static List<Item> readItemsFromFile(Path filePath) throws IOException {
      List<Item> items = new ArrayList<>();
      List<String> lines = Files.readAllLines(filePath);

      // Skip the header line
      for (int i = 1; i < lines.size(); i++) {
         String line = lines.get(i);
         if (line.isBlank()) {
            continue;
         }// end of if block
         String[] fields = CsvUtil.parseLine(line);
         if (fields.length >= 5) {
            String image = normalizeImage(fields[0]);
            String title = fields[1];
            String description = fields[2];
            int tickets = Integer.parseInt(fields[3].trim());
            double price = Double.parseDouble(fields[4].trim());

            items.add(new Item(image, title, description, tickets, price));
         }// end of if statement
      }// end of for loop
      return items;
   }// end of readItemsFromFile method

   // Older versions wrote the text "null" when no default image had been chosen
   private static String normalizeImage(String value) {
      String image = value.trim();
      return image.equalsIgnoreCase("null") ? "" : image;
   }// end of normalizeImage method

   // Write the items to the CSV file
   public static void writeItemsToCSV(List<Item> items, Path filePath) throws IOException {
      List<String> lines = new ArrayList<>();
      lines.add(HEADER);
      items.stream()
           .sorted(Comparator.comparing(Item::getTitle))
           .forEach(item -> lines.add(String.join(",",
                                                  CsvUtil.quote(item.getImage()),
                                                  CsvUtil.quote(item.getTitle()),
                                                  CsvUtil.quote(item.getDescription()),
                                                  String.valueOf(item.getTickets()),
                                                  String.valueOf(item.getPrice()))));
      SafeFiles.writeLinesAtomically(filePath, lines);
   }// end of writeItemsToCSV method

}// end of ItemDataReaderAndWriter class
