package raffle.utils;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import raffle.models.Item;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ItemDataReaderAndWriterTest {

   @TempDir
   Path dir;

   @Test
   void roundTripKeepsCommasQuotesAndWindowsPaths() throws IOException {
      Path file = dir.resolve("data.csv");
      String image = "C:\\Users\\Ana\\Sell & Win Raffle\\Bike\\bike, red.png";
      List<Item> items = List.of(new Item(image, "Bike", "Red \"mountain\" bike, 21 speeds", 17, 15.5));

      ItemDataReaderAndWriter.writeItemsToCSV(items, file);
      Item read = ItemDataReaderAndWriter.readItemsFromFile(file).getFirst();

      assertEquals(image, read.getImage());
      assertEquals("Bike", read.getTitle());
      assertEquals("Red \"mountain\" bike, 21 speeds", read.getDescription());
      assertEquals(17, read.getTickets());
      assertEquals(15.5, read.getPrice());
   }

   @Test
   void theTextNullWrittenByOlderVersionsMeansNoImage() throws IOException {
      Path file = dir.resolve("data.csv");
      Files.write(file, List.of("Image, Title, Description, Available Tickets, Ticket Price",
                                "null,\"Bicicleta\",\"Bicicleta de munte\",17,15.0"));

      Item read = ItemDataReaderAndWriter.readItemsFromFile(file).getFirst();

      assertEquals("", read.getImage());
      assertEquals("Bicicleta", read.getTitle());
   }

   @Test
   void itemsAreWrittenSortedByTitleWithoutReorderingTheCallersList() throws IOException {
      Path file = dir.resolve("data.csv");
      List<Item> items = List.of(new Item("", "Zebra", "z", 1, 1.0), new Item("", "Apple", "a", 1, 1.0));

      ItemDataReaderAndWriter.writeItemsToCSV(items, file);

      assertEquals("Zebra", items.getFirst().getTitle());
      assertEquals("Apple", ItemDataReaderAndWriter.readItemsFromFile(file).getFirst().getTitle());
   }
}
