package raffle.reports;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import raffle.models.Item;
import raffle.models.Player;
import raffle.reports.SalesReport.BuyerLine;
import raffle.reports.SalesReport.OwedLine;
import raffle.storage.CsvRaffleRepository;
import raffle.storage.RaffleRepository;

import java.io.IOException;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ReportServiceTest {

   private static final Instant NOW = Instant.parse("2026-09-30T20:00:00Z");
   private static final String FIRST = "2026-09-30T18:00:00Z";
   private static final String SECOND = "2026-09-30T19:00:00Z";

   @TempDir
   Path root;

   RaffleRepository repository;
   ReportService service;

   @BeforeEach
   void setUp() throws IOException {
      repository = new CsvRaffleRepository(root);
      service = new ReportService(repository);
   }

   private static List<Player> tickets(int count) {
      return new ArrayList<>(IntStream.rangeClosed(1, count).mapToObj(id -> new Player(id, "", "", 0)).toList());
   }

   private void addBike(List<Player> sold) throws IOException {
      List<Player> ledger = tickets(6);
      sold.forEach(ticket -> ledger.set(ticket.getId() - 1, ticket));
      repository.addItem(new Item("", "Bike", "Red bike", 6, 10.0), ledger);
   }

   @Test
   void theReportListsEachBuyerOnceWithWhatIsPaidAndOwed() throws IOException {
      addBike(List.of(new Player(1, "Ion Popescu", "0712345678", 2, true, FIRST, 1000),
                      new Player(2, "ion popescu", "0712345678", 2, false, FIRST, 1000),
                      new Player(4, "Ana Pop", "0700000001", 1, false, SECOND, 1000)));

      SalesReport report = service.salesReport(NOW);

      assertEquals(1, report.items().size());
      List<BuyerLine> buyers = report.items().getFirst().buyers();
      assertEquals(2, buyers.size());
      assertEquals("Ana Pop", buyers.get(0).name());
      assertEquals(1000, buyers.get(0).owedCents());
      assertEquals("Ion Popescu", buyers.get(1).name(), "the same person with a different case is one buyer");
      assertEquals(List.of(1, 2), buyers.get(1).ticketIds());
      assertEquals(1000, buyers.get(1).paidCents());
      assertEquals(1000, buyers.get(1).owedCents());
      assertEquals(3000, buyers.stream().mapToLong(BuyerLine::totalCents).sum());
   }

   @Test
   void tooTheWholeRaffleHasItsFigures() throws IOException {
      addBike(List.of(new Player(1, "Ion", "0712345678", 1, true, FIRST, 1000)));
      repository.addItem(new Item("", "Phone", "Black", 2, 99.5), tickets(2));

      SalesReport report = service.salesReport(NOW);

      assertEquals(2, report.summary().items());
      assertEquals(8, report.summary().totalTickets());
      assertEquals(1, report.summary().soldTickets());
      assertEquals(1000, report.summary().collectedCents());
      assertEquals(NOW, report.generatedAt());
   }

   @Test
   void everyTicketCountsAtItsOwnPriceAndOldOnesAtTheItemsPrice() throws IOException {
      addBike(List.of(new Player(1, "Ion", "0712345678", 2, false, FIRST, 800),
                      new Player(2, "Ion", "0712345678", 2, false, FIRST, 0)));// price not recorded: the item's 10.00

      BuyerLine ion = service.salesReport(NOW).items().getFirst().buyers().getFirst();

      assertEquals(1800, ion.owedCents());
   }

   @Test
   void theBuyersWhoStillOweComeBiggestDebtFirst() throws IOException {
      addBike(List.of(new Player(1, "Ana", "0700000001", 1, false, FIRST, 1000),
                      new Player(2, "Bob", "0700000002", 2, false, FIRST, 1000),
                      new Player(3, "Bob", "0700000002", 2, false, FIRST, 1000),
                      new Player(4, "Cora", "0700000003", 1, true, FIRST, 1000)));

      List<OwedLine> owed = service.salesReport(NOW).owedLines();

      assertEquals(List.of("Bob", "Ana"), owed.stream().map(line -> line.buyer().name()).toList());
      assertEquals("Bike", owed.getFirst().itemTitle());
   }

   @Test
   void theDrawsAreInTheReport() throws IOException {
      addBike(List.of(new Player(1, "Ion", "0712345678", 1, true, FIRST, 1000)));
      repository.recordDraw("Bike", new Player(1, "Ion", "0712345678", 1), NOW);

      assertEquals(1, service.salesReport(NOW).draws().size());
   }

   @Test
   void aReceiptCoversOnePurchaseNotEverythingTheBuyerEverBought() throws IOException {
      addBike(List.of(new Player(1, "Ion", "0712345678", 3, true, FIRST, 1000),
                      new Player(2, "Ion", "0712345678", 3, true, FIRST, 1000),
                      new Player(5, "Ion", "0712345678", 3, false, SECOND, 1200)));

      Receipt first = service.receipt("Bike", 2).orElseThrow();
      Receipt second = service.receipt("Bike", 5).orElseThrow();

      assertEquals(List.of(1, 2), first.ticketIds());
      assertEquals(2000, first.totalCents());
      assertEquals(1000, first.priceCents());
      assertTrue(first.fullyPaid());
      assertEquals(Instant.parse(FIRST), first.soldAt());
      assertEquals(List.of(5), second.ticketIds());
      assertEquals(1200, second.totalCents());
      assertFalse(second.fullyPaid());
      assertEquals("Red bike", second.itemDescription());
   }

   @Test
   void aPartlyPaidPurchaseSaysHowManyArePaid() throws IOException {
      addBike(List.of(new Player(1, "Ion", "0712345678", 2, true, FIRST, 1000), new Player(2, "Ion", "0712345678", 2, false, FIRST, 1000)));

      Receipt receipt = service.receipt("Bike", 1).orElseThrow();

      assertEquals(1, receipt.paidTickets());
      assertFalse(receipt.fullyPaid());
   }

   @Test
   void aLedgerWithoutTimesTreatsAllOfTheBuyersTicketsAsOnePurchase() throws IOException {
      addBike(List.of(new Player(1, "Ion", "0712345678", 2, true, "", 0), new Player(3, "Ion", "0712345678", 2, true, "", 0)));

      Receipt receipt = service.receipt("Bike", 1).orElseThrow();

      assertEquals(List.of(1, 3), receipt.ticketIds());
      assertEquals(2000, receipt.totalCents(), "no recorded price: the item's 10.00");
      assertNull(receipt.soldAt());
   }

   @Test
   void thereIsNoReceiptForATicketNobodyBoughtOrAnItemThatDoesNotExist() throws IOException {
      addBike(List.of(new Player(1, "Ion", "0712345678", 1, true, FIRST, 1000)));

      assertTrue(service.receipt("Bike", 2).isEmpty());
      assertTrue(service.receipt("Bike", 99).isEmpty());
      assertTrue(service.receipt("Nothing", 1).isEmpty());
   }

   @Test
   void certificatesAreMadeForSoldTicketsInTheOrderGiven() throws IOException {
      addBike(List.of(new Player(1, "Ion", "0712345678", 1, true, FIRST, 1000), new Player(4, "Ana", "0700000001", 1, true, FIRST, 1000)));

      List<Certificate> certificates = service.certificates("Bike", List.of(4, 2, 1), NOW);

      assertEquals(List.of("Ana", "Ion"), certificates.stream().map(Certificate::winnerName).toList(), "ticket 2 is unsold: no certificate");
      assertEquals(4, certificates.getFirst().ticketId());
      assertEquals("Bike", certificates.getFirst().itemTitle());
      assertEquals(NOW, certificates.getFirst().time());
   }
}
