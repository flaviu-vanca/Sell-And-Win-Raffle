package raffle.reports;

import raffle.models.Item;
import raffle.models.Player;
import raffle.reports.SalesReport.BuyerLine;
import raffle.reports.SalesReport.ItemReport;
import raffle.services.ItemSales;
import raffle.services.SalesSummary;
import raffle.storage.RaffleRepository;

import java.io.IOException;
import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;

/** Works out what the printed documents show, from the stored data. */
public final class ReportService {

   private final RaffleRepository repository;

   public ReportService(RaffleRepository repository) {
      this.repository = repository;
   }

   public SalesReport salesReport(Instant now) throws IOException {
      List<ItemReport> items = new ArrayList<>();
      List<ItemSales> figures = new ArrayList<>();
      for (Item item : repository.items()) {
         List<Player> ledger = repository.ledger(item.getTitle());
         ItemSales sales = ItemSales.of(ledger, item.getPrice());
         items.add(new ItemReport(item, sales, buyers(ledger, sales.priceCents())));
         figures.add(sales);
      }// end of for loop
      return new SalesReport(now, items, SalesSummary.of(figures), repository.draws());
   }// end of salesReport method

   /**
    * The receipt for the purchase that includes the given ticket: all the tickets that buyer got in the same sale
    * (a ledger that does not record the time of sale counts all of the buyer's tickets as one purchase).
    *
    * @return empty when the ticket does not exist or is not sold
    */
   public Optional<Receipt> receipt(String itemTitle, int ticketId) throws IOException {
      Optional<Item> item = repository.item(itemTitle);
      if (item.isEmpty()) {
         return Optional.empty();
      }// end of if block
      List<Player> ledger = repository.ledger(itemTitle);
      Optional<Player> chosen = ledger.stream().filter(ticket -> ticket.getId() == ticketId && ticket.isSold()).findFirst();
      if (chosen.isEmpty()) {
         return Optional.empty();
      }// end of if block

      Player first = chosen.get();
      List<Player> purchase = ledger.stream()
                                    .filter(ticket -> ticket.isSold() && ticket.buyerKey().equals(first.buyerKey())
                                                      && ticket.getSoldAt().equals(first.getSoldAt()))
                                    .sorted(Comparator.comparingInt(Player::getId))
                                    .toList();
      long itemPriceCents = Math.round(item.get().getPrice() * 100);
      long total = purchase.stream().mapToLong(ticket -> priceOf(ticket, itemPriceCents)).sum();
      int paid = (int) purchase.stream().filter(Player::isPaid).count();
      return Optional.of(new Receipt(itemTitle, item.get().getDescription(), first.getName(), first.getPhoneNumber(),
                                     purchase.stream().map(Player::getId).toList(), priceOf(purchase.getFirst(), itemPriceCents),
                                     total, paid, parse(first.getSoldAt())));
   }// end of receipt method

   /** A certificate for each of the given tickets that is sold, in the order given. */
   public List<Certificate> certificates(String itemTitle, List<Integer> ticketIds, Instant time) throws IOException {
      List<Player> ledger = repository.ledger(itemTitle);
      List<Certificate> certificates = new ArrayList<>();
      for (int id : ticketIds) {
         ledger.stream().filter(ticket -> ticket.getId() == id && ticket.isSold()).findFirst()
               .ifPresent(ticket -> certificates.add(new Certificate(itemTitle, ticket.getName(), id, time)));
      }// end of for loop
      return certificates;
   }// end of certificates method

   // One line per buyer (same name ignoring case, same phone), in name order
   static List<BuyerLine> buyers(List<Player> ledger, long itemPriceCents) {
      Map<String, List<Player>> groups = new TreeMap<>();
      for (Player ticket : ledger) {
         if (ticket.isSold()) {
            groups.computeIfAbsent(ticket.getName().strip().toLowerCase(Locale.ROOT) + "|" + ticket.getPhoneNumber().strip(),
                                   key -> new ArrayList<>()).add(ticket);
         }// end of if block
      }// end of for loop

      List<BuyerLine> lines = new ArrayList<>();
      for (List<Player> tickets : groups.values()) {
         tickets.sort(Comparator.comparingInt(Player::getId));
         long paid = 0;
         long owed = 0;
         for (Player ticket : tickets) {
            if (ticket.isPaid()) {
               paid += priceOf(ticket, itemPriceCents);
            } else {
               owed += priceOf(ticket, itemPriceCents);
            }// end of if-else block
         }// end of for loop
         lines.add(new BuyerLine(tickets.getFirst().getName(), tickets.getFirst().getPhoneNumber(),
                                 tickets.stream().map(Player::getId).toList(), paid, owed));
      }// end of for loop
      lines.sort(Comparator.comparing((BuyerLine line) -> line.name().toLowerCase(Locale.ROOT)).thenComparing(BuyerLine::phone));
      return lines;
   }// end of buyers method

   // A ticket sold before prices were recorded counts at the price of the item
   private static long priceOf(Player ticket, long itemPriceCents) {
      return ticket.getPriceCents() > 0 ? ticket.getPriceCents() : itemPriceCents;
   }// end of priceOf method

   private static Instant parse(String text) {
      try {
         return text.isBlank() ? null : Instant.parse(text);
      } catch (DateTimeParseException e) {
         return null;
      }// end of try-catch block
   }// end of parse method

}// end of ReportService class
