package raffle.models;

import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;

public class Item {
   private final SimpleStringProperty image;
   private final SimpleStringProperty title;
   private final SimpleStringProperty description;  // Add this line
   private final SimpleIntegerProperty tickets;
   private final SimpleDoubleProperty price;

   public Item(String image, String title, String description, int tickets, double price) {
      this.image = new SimpleStringProperty(image);
      this.title = new SimpleStringProperty(title);
      this.description = new SimpleStringProperty(description);  // Add this line
      this.tickets = new SimpleIntegerProperty(tickets);
      this.price = new SimpleDoubleProperty(price);
   }

   public String getImage() {
      return image.get();
   }

   public SimpleStringProperty imageProperty() {
      return image;
   }

   public void setImage(String image) {
      this.image.set(image);
   }

   public String getTitle() {
      return title.get();
   }

   public SimpleStringProperty titleProperty() {
      return title;
   }

   public String getDescription() {  // Add this method
      return description.get();
   }

   public int getTickets() {
      return tickets.get();
   }

   public SimpleIntegerProperty ticketsProperty() {
      return tickets;
   }

   public void setTickets(int tickets) {
      this.tickets.set(tickets);
   }

   public double getPrice() {
      return price.get();
   }

   public SimpleDoubleProperty priceProperty() {
      return price;
   }

}// end of Item class