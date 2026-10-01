package raffle.reports;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import raffle.models.DrawEntry;
import raffle.models.Item;
import raffle.models.Player;
import raffle.reports.SalesReport.BuyerLine;
import raffle.reports.SalesReport.ItemReport;
import raffle.services.ItemSales;
import raffle.services.SalesSummary;
import raffle.utils.Messages;

import java.io.IOException;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.Properties;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class HtmlReportsTest {

   private static final Instant NOW = Instant.parse("2026-09-30T20:00:00Z");

   @BeforeEach
   void english() {
      Messages.setLocale(Locale.ENGLISH);
   }

   @AfterEach
   void restore() {
      Messages.setLocale(Locale.ENGLISH);
   }

   private static SalesReport report(String itemTitle, String buyerName) {
      Item item = new Item("", itemTitle, "Red", 4, 10.0);
      List<Player> ledger = List.of(new Player(1, buyerName, "0712345678", 2, true, "", 1000),
                                    new Player(2, buyerName, "0712345678", 2, false, "", 1000),
                                    new Player(3, "", "", 0), new Player(4, "", "", 0));
      ItemSales sales = ItemSales.of(ledger, 10.0);
      BuyerLine buyer = new BuyerLine(buyerName, "0712345678", List.of(1, 2), 1000, 1000);
      return new SalesReport(NOW, List.of(new ItemReport(item, sales, List.of(buyer))), SalesSummary.of(List.of(sales)),
                             List.of(new DrawEntry(NOW, itemTitle, 1, buyerName, "0712345678")));
   }

   @Test
   void theReportShowsTheFiguresTheBuyersAndWhoOwes() {
      String html = HtmlReports.salesReport(report("Bike", "Ion Popescu"), "");

      assertTrue(html.startsWith("<!DOCTYPE html>"));
      assertTrue(html.contains("<title>Sales report</title>"));
      assertTrue(html.contains("Ion Popescu"));
      assertTrue(html.contains("0712345678"));
      assertTrue(html.contains("1, 2"), "the ticket numbers");
      assertTrue(html.contains("€10.00"), "collected, and owed");
      assertTrue(html.contains("€40.00"), "what selling everything would bring");
      assertTrue(html.contains("Owes €10.00"));
      assertTrue(html.contains("Payments still to collect"));
      assertTrue(html.contains("Winners"));
   }

   @Test
   void anythingTypedByTheOperatorIsEscaped() {
      String html = HtmlReports.salesReport(report("Bike <b>& \"co\"</b>", "<script>alert(1)</script>"), "<i>Club</i>");

      assertFalse(html.contains("<script>alert"), "a name must never become markup");
      assertTrue(html.contains("&lt;script&gt;alert(1)&lt;/script&gt;"));
      assertTrue(html.contains("Bike &lt;b&gt;&amp; &quot;co&quot;&lt;/b&gt;"));
      assertTrue(html.contains("&lt;i&gt;Club&lt;/i&gt;"));
   }

   @Test
   void theLanguageOfTheApplicationIsUsed() {
      Messages.setLocale(Locale.forLanguageTag("ro"));

      String html = HtmlReports.salesReport(report("Bicicleta", "Ion"), "");

      assertTrue(html.contains("<html lang=\"ro\">"));
      assertTrue(html.contains("Raport de vânzări"));
      assertTrue(html.contains("Datorează 10,00"), "amounts in the Romanian format");
      assertTrue(html.contains("Plăți de încasat"));
   }

   @Test
   void aReportWithNothingInItSaysSo() {
      SalesReport empty = new SalesReport(NOW, List.of(), SalesSummary.of(List.of()), List.of());

      String html = HtmlReports.salesReport(empty, "");

      assertTrue(html.contains("Nobody owes anything."));
      assertTrue(html.contains("No draws yet."));
   }

   @Test
   void theOrganizerIsShownWhenThereIsOne() {
      assertTrue(HtmlReports.salesReport(report("Bike", "Ion"), "Pop Club").contains("Pop Club · Generated"));
      assertFalse(HtmlReports.salesReport(report("Bike", "Ion"), "").contains(" · Generated"));
   }

   @Test
   void aReceiptShowsThePurchase() {
      Receipt receipt = new Receipt("Bike", "Red", "Ion Popescu", "0712345678", List.of(2, 4), 1000, 2000, 2, Instant.parse("2026-09-30T18:00:00Z"));

      String html = HtmlReports.receipt(receipt, "Pop Club");

      assertTrue(html.contains("<title>Receipt</title>"));
      assertTrue(html.contains("Pop Club"));
      assertTrue(html.contains("Ion Popescu"));
      assertTrue(html.contains("2, 4"));
      assertTrue(html.contains("€20.00"));
      assertTrue(html.contains(">Paid<"));
   }

   @Test
   void aReceiptSaysWhenNotEverythingIsPaid() {
      Receipt none = new Receipt("Bike", "Red", "Ion", "0712345678", List.of(2, 4), 1000, 2000, 0, null);
      Receipt some = new Receipt("Bike", "Red", "Ion", "0712345678", List.of(2, 4), 1000, 2000, 1, null);

      assertTrue(HtmlReports.receipt(none, "").contains("Not paid yet"));
      assertTrue(HtmlReports.receipt(some, "").contains("1 of 2 tickets paid"));
      assertFalse(HtmlReports.receipt(none, "").contains("Date</th>"), "no time of sale is recorded: no date line");
   }

   @Test
   void everyWinnerGetsACertificateOnAPageOfItsOwn() {
      List<Certificate> certificates = List.of(new Certificate("Bike", "Ion Popescu", 17, NOW), new Certificate("Bike", "Ana <Pop>", 4, NOW));

      String html = HtmlReports.certificates(certificates, "Pop Club");

      assertEquals(2, html.split("<section class=\"certificate\">", -1).length - 1);
      assertTrue(html.contains("Ion Popescu"));
      assertTrue(html.contains("Ana &lt;Pop&gt;"));
      assertTrue(html.contains("Ticket number 17"));
      assertTrue(html.contains("Congratulations!"));
      assertFalse(html.contains("0712345678"), "no phone numbers on a certificate");
   }

   @Test
   void everyTextOfTheDocumentsIsTranslatedInBothLanguages() throws IOException {
      Properties english = load("messages.properties");
      Properties romanian = load("messages_ro.properties");
      for (String key : english.stringPropertyNames()) {
         if (key.startsWith("report.") || key.startsWith("receipt.") || key.startsWith("certificate.")) {
            assertTrue(romanian.containsKey(key), "not translated: " + key);
         }
      }

      Receipt receipt = new Receipt("Bike", "Red", "Ion", "0712345678", List.of(2), 1000, 1000, 0, NOW);
      for (Locale locale : List.of(Locale.ENGLISH, Locale.forLanguageTag("ro"))) {
         Messages.setLocale(locale);
         String all = HtmlReports.salesReport(report("Bike", "Ion"), "") + HtmlReports.receipt(receipt, "")
                      + HtmlReports.certificates(List.of(new Certificate("Bike", "Ion", 2, NOW)), "");
         for (String key : english.stringPropertyNames()) {
            if (key.startsWith("report.") || key.startsWith("receipt.") || key.startsWith("certificate.")) {
               assertFalse(all.contains(key), "a raw key shows in the " + locale + " documents: " + key);
            }
         }
      }
   }

   private static Properties load(String file) throws IOException {
      Properties properties = new Properties();
      try (Reader reader = Files.newBufferedReader(Path.of("src/main/resources/i18n").resolve(file), StandardCharsets.UTF_8)) {
         properties.load(reader);
      }
      return properties;
   }
}
