package raffle.models;

import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleLongProperty;
import javafx.beans.property.SimpleStringProperty;

public class Player {
   private final SimpleIntegerProperty id;
   private final SimpleStringProperty name;
   private final SimpleStringProperty phoneNumber;
   private final SimpleIntegerProperty numberOfTickets;
   private final SimpleBooleanProperty paid;
   private final SimpleStringProperty soldAt;
   private final SimpleLongProperty priceCents;

   /** A ticket row without payment details: a sold ticket counts as paid, which is what older ledgers meant. */
   public Player(int id, String name, String phoneNumber, int numberOfTickets) {
      this(id, name, phoneNumber, numberOfTickets, name != null && ! name.isBlank(), "");
   }

   /**
    * @param paid   whether the buyer has paid for this ticket (meaningless for unsold rows)
    * @param soldAt when the ticket was sold, as an ISO-8601 instant, or empty when unknown
    */
   public Player(int id, String name, String phoneNumber, int numberOfTickets, boolean paid, String soldAt) {
      this(id, name, phoneNumber, numberOfTickets, paid, soldAt, 0);
   }

   /**
    * @param priceCents what the ticket was sold for, in cents; 0 when it is not recorded (unsold tickets, and
    *                   tickets sold by versions that did not record it), which means "the price of the item"
    */
   public Player(int id, String name, String phoneNumber, int numberOfTickets, boolean paid, String soldAt, long priceCents) {
      this.id = new SimpleIntegerProperty(id);
      this.name = new SimpleStringProperty(name);
      this.phoneNumber = new SimpleStringProperty(phoneNumber);
      this.numberOfTickets = new SimpleIntegerProperty(numberOfTickets);
      this.paid = new SimpleBooleanProperty(paid);
      this.soldAt = new SimpleStringProperty(soldAt == null ? "" : soldAt);
      this.priceCents = new SimpleLongProperty(priceCents);
   }

   public int getId() {
      return id.get();
   }

   public SimpleIntegerProperty idProperty() {
      return id;
   }

   public String getName() {
      return name.get();
   }

   public SimpleStringProperty nameProperty() {
      return name;
   }

   public String getPhoneNumber() {
      return phoneNumber.get();
   }

   public SimpleStringProperty phoneNumberProperty() {
      return phoneNumber;
   }

   public int getNumberOfTickets() {
      return numberOfTickets.get();
   }

   public SimpleIntegerProperty numberOfTicketsProperty() {
      return numberOfTickets;
   }

   public boolean isPaid() {
      return paid.get();
   }

   public SimpleBooleanProperty paidProperty() {
      return paid;
   }

   public String getSoldAt() {
      return soldAt.get();
   }

   public SimpleStringProperty soldAtProperty() {
      return soldAt;
   }

   public long getPriceCents() {
      return priceCents.get();
   }

   public SimpleLongProperty priceCentsProperty() {
      return priceCents;
   }

   /** A ticket row is sold when a buyer name is set; unsold rows are kept in the ledger with empty fields. */
   public boolean isSold() {
      String buyer = name.get();
      return buyer != null && ! buyer.isBlank();
   }

   /**
    * Identifies the person behind a ticket: the ledger stores one row per ticket, so a buyer with three
    * tickets appears three times with the same name and phone number.
    */
   public String buyerKey() {
      return name.get().strip().toLowerCase(java.util.Locale.ROOT) + "|" + phoneNumber.get().strip();
   }

   public void setName(String name) {
      this.name.set(name);
   }

   public void setPhoneNumber(String phoneNumber) {
      this.phoneNumber.set(phoneNumber);
   }

   public void setNumberOfTickets(int numberOfTickets) {
      this.numberOfTickets.set(numberOfTickets);
   }

   public void setPaid(boolean paid) {
      this.paid.set(paid);
   }

   public void setPriceCents(long priceCents) {
      this.priceCents.set(priceCents);
   }

   public void setSoldAt(String soldAt) {
      this.soldAt.set(soldAt == null ? "" : soldAt);
   }

   /** Makes the row an unsold ticket again (used when a record is removed). */
   public void clear() {
      setName("");
      setPhoneNumber("");
      setNumberOfTickets(0);
      setPaid(false);
      setSoldAt("");
      setPriceCents(0);
   }
}
