package raffle.models;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ItemTest {

   @Test
   void exposesTheValuesItWasCreatedWith() {
      Item item = new Item("bike.png", "Bike", "Mountain bike", 20, 15.0);

      assertEquals("bike.png", item.getImage());
      assertEquals("Bike", item.getTitle());
      assertEquals("Mountain bike", item.getDescription());
      assertEquals(20, item.getTickets());
      assertEquals(15.0, item.getPrice());
   }

   @Test
   void ticketsAndImageCanBeUpdated() {
      Item item = new Item("", "Bike", "Mountain bike", 20, 15.0);
      item.setTickets(17);
      item.setImage("new.png");

      assertEquals(17, item.ticketsProperty().get());
      assertEquals("new.png", item.imageProperty().get());
   }
}
