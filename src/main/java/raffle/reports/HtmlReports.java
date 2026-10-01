package raffle.reports;

import raffle.models.DrawEntry;
import raffle.reports.SalesReport.BuyerLine;
import raffle.reports.SalesReport.ItemReport;
import raffle.reports.SalesReport.OwedLine;
import raffle.services.ItemSales;
import raffle.services.SalesSummary;
import raffle.utils.Messages;
import raffle.utils.Money;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.List;
import java.util.stream.Collectors;

/**
 * The printed documents as complete HTML pages: the sales report, a receipt and the winners' certificates. A page
 * opens in any browser, which can print it or save it as a PDF, so no extra software is needed. Text follows the
 * active language. Everything typed by the operator is escaped.
 */
public final class HtmlReports {

   private static final String STYLE = """
           :root { --gold: #9c7945; --ink: #1d2733; --muted: #6b5a3a; --paper: #f4f1ea; }
           * { box-sizing: border-box; }
           body { font-family: "Segoe UI", "Helvetica Neue", "DejaVu Sans", Arial, sans-serif; color: var(--ink); margin: 0; padding: 24px; background: #fff; }
           h1 { margin: 0 0 4px; font-size: 26px; color: var(--gold); }
           h2 { margin: 26px 0 6px; font-size: 18px; border-bottom: 2px solid var(--gold); padding-bottom: 3px; }
           .sub { color: var(--muted); font-size: 13px; }
           table { border-collapse: collapse; width: 100%; margin: 8px 0 14px; font-size: 13px; }
           th { text-align: left; background: var(--paper); border-bottom: 2px solid var(--gold); padding: 6px 8px; }
           td { border-bottom: 1px solid #ddd; padding: 5px 8px; vertical-align: top; }
           .num { text-align: right; white-space: nowrap; }
           tr.total td { font-weight: bold; border-top: 2px solid var(--gold); border-bottom: 0; }
           .paid { color: #1e8e3e; font-weight: 600; }
           .owes { color: #c2571a; font-weight: 600; }
           .empty { color: var(--muted); font-style: italic; }
           .noprint { background: #fff8e1; border: 1px solid #e0c97a; padding: 8px 12px; margin-bottom: 16px; font-size: 13px; }
           @media print { .noprint { display: none; } body { padding: 0; } h2 { page-break-after: avoid; } tr { page-break-inside: avoid; } }
           """;

   private HtmlReports() {
   }

   // ---- sales report ----

   public static String salesReport(SalesReport report, String organizer) {
      StringBuilder html = new StringBuilder();
      begin(html, Messages.get("report.title"), "@page { size: A4; margin: 14mm; }");
      html.append("<div class=\"noprint\">").append(Html.escape(Messages.get("report.hint.print"))).append("</div>\n");
      html.append("<h1>").append(Html.escape(Messages.get("report.title"))).append("</h1>\n");
      html.append("<div class=\"sub\">");
      if (! organizer.isBlank()) {
         html.append(Html.escape(organizer)).append(" · ");
      }// end of if block
      html.append(Html.escape(Messages.get("report.generated", time(report.generatedAt())))).append("</div>\n");

      summary(html, report);
      for (ItemReport item : report.items()) {
         buyers(html, item);
      }// end of for loop
      owed(html, report);
      winners(html, report.draws());

      html.append("</body></html>\n");
      return html.toString();
   }// end of salesReport method

   private static void summary(StringBuilder html, SalesReport report) {
      html.append("<h2>").append(Html.escape(Messages.get("report.summary"))).append("</h2>\n<table>\n<tr>")
          .append(th("report.col.item", false)).append(th("report.col.price", true)).append(th("report.col.sold", true))
          .append(th("report.col.collected", true)).append(th("report.col.owed", true)).append(th("report.col.potential", true))
          .append("</tr>\n");
      for (ItemReport item : report.items()) {
         ItemSales sales = item.sales();
         html.append("<tr><td>").append(Html.escape(item.item().getTitle())).append("</td>")
             .append(num(money(sales.priceCents()))).append(num(sales.soldTickets() + " / " + sales.totalTickets()))
             .append(num(money(sales.collectedCents()))).append(num(money(sales.outstandingCents())))
             .append(num(money(sales.potentialCents()))).append("</tr>\n");
      }// end of for loop
      SalesSummary total = report.summary();
      html.append("<tr class=\"total\"><td>").append(Html.escape(Messages.get("report.total"))).append("</td><td></td>")
          .append(num(total.soldTickets() + " / " + total.totalTickets())).append(num(money(total.collectedCents())))
          .append(num(money(total.outstandingCents()))).append(num(money(total.potentialCents()))).append("</tr>\n</table>\n");
   }// end of summary method

   private static void buyers(StringBuilder html, ItemReport item) {
      html.append("<h2>").append(Html.escape(Messages.get("report.buyers", item.item().getTitle()))).append("</h2>\n");
      if (item.buyers().isEmpty()) {
         html.append("<p class=\"empty\">").append(Html.escape(Messages.get("report.noBuyers"))).append("</p>\n");
         return;
      }// end of if block
      html.append("<table>\n<tr>").append(th("report.col.name", false)).append(th("report.col.phone", false))
          .append(th("report.col.tickets", false)).append(th("report.col.count", true)).append(th("report.col.amount", true))
          .append(th("report.col.status", false)).append("</tr>\n");
      for (BuyerLine buyer : item.buyers()) {
         html.append("<tr><td>").append(Html.escape(buyer.name())).append("</td><td>").append(Html.escape(buyer.phone()))
             .append("</td><td>").append(Html.escape(numbers(buyer.ticketIds()))).append("</td>")
             .append(num(String.valueOf(buyer.ticketIds().size()))).append(num(money(buyer.totalCents()))).append("<td>")
             .append(status(buyer)).append("</td></tr>\n");
      }// end of for loop
      html.append("</table>\n");
   }// end of buyers method

   private static String status(BuyerLine buyer) {
      return buyer.owedCents() > 0
              ? "<span class=\"owes\">" + Html.escape(Messages.get("report.status.owes", money(buyer.owedCents()))) + "</span>"
              : "<span class=\"paid\">" + Html.escape(Messages.get("report.status.paid")) + "</span>";
   }// end of status method

   private static void owed(StringBuilder html, SalesReport report) {
      html.append("<h2>").append(Html.escape(Messages.get("report.owed.title"))).append("</h2>\n");
      List<OwedLine> owed = report.owedLines();
      if (owed.isEmpty()) {
         html.append("<p class=\"empty\">").append(Html.escape(Messages.get("report.owed.none"))).append("</p>\n");
         return;
      }// end of if block
      html.append("<table>\n<tr>").append(th("report.col.name", false)).append(th("report.col.phone", false))
          .append(th("report.col.item", false)).append(th("report.col.count", true)).append(th("report.col.owed", true)).append("</tr>\n");
      long total = 0;
      for (OwedLine line : owed) {
         BuyerLine buyer = line.buyer();
         total += buyer.owedCents();
         html.append("<tr><td>").append(Html.escape(buyer.name())).append("</td><td>").append(Html.escape(buyer.phone()))
             .append("</td><td>").append(Html.escape(line.itemTitle())).append("</td>")
             .append(num(String.valueOf(buyer.ticketIds().size()))).append("<td class=\"num owes\">").append(money(buyer.owedCents()))
             .append("</td></tr>\n");
      }// end of for loop
      html.append("<tr class=\"total\"><td>").append(Html.escape(Messages.get("report.total")))
          .append("</td><td></td><td></td><td></td>").append("<td class=\"num\">").append(money(total)).append("</td></tr>\n</table>\n");
   }// end of owed method

   private static void winners(StringBuilder html, List<DrawEntry> draws) {
      html.append("<h2>").append(Html.escape(Messages.get("report.winners.title"))).append("</h2>\n");
      if (draws.isEmpty()) {
         html.append("<p class=\"empty\">").append(Html.escape(Messages.get("report.winners.none"))).append("</p>\n");
         return;
      }// end of if block
      html.append("<table>\n<tr>").append(th("report.col.time", false)).append(th("report.col.item", false))
          .append(th("report.col.ticket", true)).append(th("report.col.winner", false)).append("</tr>\n");
      for (DrawEntry draw : draws) {
         html.append("<tr><td>").append(Html.escape(time(draw.time()))).append("</td><td>").append(Html.escape(draw.itemTitle()))
             .append("</td>").append(num(String.valueOf(draw.ticketId()))).append("<td>").append(Html.escape(draw.winnerName()))
             .append("</td></tr>\n");
      }// end of for loop
      html.append("</table>\n");
   }// end of winners method

   // ---- receipt ----

   public static String receipt(Receipt receipt, String organizer) {
      StringBuilder html = new StringBuilder();
      begin(html, Messages.get("receipt.title"), """
              @page { size: A5; margin: 12mm; }
              .receipt { max-width: 420px; margin: 0 auto; border: 2px solid var(--gold); border-radius: 10px; padding: 22px 26px; }
              .receipt h1 { text-align: center; }
              .receipt .sub { text-align: center; margin-bottom: 14px; }
              .receipt table { font-size: 15px; }
              .receipt th { background: none; border-bottom: 1px dotted #aaa; width: 42%; font-weight: normal; color: var(--muted); }
              .receipt td { border-bottom: 1px dotted #aaa; }
              .receipt tr.total td, .receipt tr.total th { font-size: 18px; border-top: 2px solid var(--gold); color: var(--ink); font-weight: bold; }
              .receipt .thanks { text-align: center; margin-top: 16px; color: var(--muted); }
              """);
      html.append("<div class=\"noprint\">").append(Html.escape(Messages.get("report.hint.print"))).append("</div>\n");
      html.append("<div class=\"receipt\">\n<h1>").append(Html.escape(Messages.get("receipt.title"))).append("</h1>\n<div class=\"sub\">");
      if (! organizer.isBlank()) {
         html.append(Html.escape(organizer));
      }// end of if block
      html.append("</div>\n<table>\n");
      line(html, "receipt.item", Html.escape(receipt.itemTitle()));
      line(html, "receipt.buyer", Html.escape(receipt.buyerName()));
      line(html, "receipt.phone", Html.escape(receipt.phone()));
      line(html, "receipt.tickets", Html.escape(numbers(receipt.ticketIds())));
      line(html, "receipt.count", String.valueOf(receipt.ticketIds().size()));
      line(html, "receipt.price", money(receipt.priceCents()));
      html.append("<tr class=\"total\"><th>").append(Html.escape(Messages.get("receipt.total"))).append("</th><td>")
          .append(money(receipt.totalCents())).append("</td></tr>\n");
      line(html, "receipt.status", receipt.fullyPaid()
              ? "<span class=\"paid\">" + Html.escape(Messages.get("receipt.paid")) + "</span>"
              : "<span class=\"owes\">" + Html.escape(receipt.paidTickets() == 0
                                                       ? Messages.get("receipt.unpaid")
                                                       : Messages.get("receipt.partial", String.valueOf(receipt.paidTickets()),
                                                                      String.valueOf(receipt.ticketIds().size()))) + "</span>");
      if (receipt.soldAt() != null) {
         line(html, "receipt.date", Html.escape(time(receipt.soldAt())));
      }// end of if block
      html.append("</table>\n<div class=\"thanks\">").append(Html.escape(Messages.get("receipt.footer"))).append("</div>\n</div>\n</body></html>\n");
      return html.toString();
   }// end of receipt method

   private static void line(StringBuilder html, String labelKey, String valueHtml) {
      html.append("<tr><th>").append(Html.escape(Messages.get(labelKey))).append("</th><td>").append(valueHtml).append("</td></tr>\n");
   }// end of line method

   // ---- certificates ----

   public static String certificates(List<Certificate> certificates, String organizer) {
      StringBuilder html = new StringBuilder();
      begin(html, Messages.get("certificate.title"), """
              @page { size: A4 landscape; margin: 0; }
              body { padding: 16px; background: #eee; }
              .certificate { width: 100%; max-width: 1123px; aspect-ratio: 297 / 210; margin: 0 auto 24px; background: #fff;
                             border: 12px double var(--gold); display: flex; flex-direction: column; align-items: center;
                             justify-content: center; text-align: center; padding: 30px 60px; }
              .certificate .org { font-size: 15pt; letter-spacing: 3px; text-transform: uppercase; color: var(--muted); min-height: 1.4em; }
              .certificate h1 { font-size: 40pt; margin: 14px 0 6px; letter-spacing: 2px; }
              .certificate .congrats { font-size: 20pt; color: var(--muted); margin-bottom: 20px; }
              .certificate .winner { font-size: 38pt; font-weight: bold; margin: 8px 0; }
              .certificate .won { font-size: 16pt; color: var(--muted); margin: 10px 0 4px; }
              .certificate .prize { font-size: 30pt; font-weight: bold; color: var(--gold); margin: 4px 0; }
              .certificate .ticket { font-size: 16pt; margin-top: 14px; }
              .certificate .date { font-size: 13pt; color: var(--muted); margin-top: 30px; }
              @media print { body { padding: 0; background: #fff; }
                             .certificate { width: 297mm; height: 210mm; max-width: none; margin: 0; page-break-after: always; border-width: 14px; } }
              """);
      html.append("<div class=\"noprint\">").append(Html.escape(Messages.get("report.hint.print"))).append("</div>\n");
      for (Certificate certificate : certificates) {
         html.append("<section class=\"certificate\">\n<div class=\"org\">").append(Html.escape(organizer)).append("</div>\n<h1>")
             .append(Html.escape(Messages.get("certificate.title"))).append("</h1>\n<div class=\"congrats\">")
             .append(Html.escape(Messages.get("certificate.congrats"))).append("</div>\n<div class=\"winner\">")
             .append(Html.escape(certificate.winnerName())).append("</div>\n<div class=\"won\">")
             .append(Html.escape(Messages.get("certificate.hasWon"))).append("</div>\n<div class=\"prize\">")
             .append(Html.escape(certificate.itemTitle())).append("</div>\n<div class=\"ticket\">")
             .append(Html.escape(Messages.get("certificate.ticket", String.valueOf(certificate.ticketId())))).append("</div>\n<div class=\"date\">")
             .append(Html.escape(date(certificate.time()))).append("</div>\n</section>\n");
      }// end of for loop
      html.append("</body></html>\n");
      return html.toString();
   }// end of certificates method

   // ---- shared ----

   private static void begin(StringBuilder html, String title, String extraStyle) {
      html.append("<!DOCTYPE html>\n<html lang=\"").append(Messages.locale().getLanguage()).append("\">\n<head>\n<meta charset=\"utf-8\">\n")
          .append("<title>").append(Html.escape(title)).append("</title>\n<style>\n").append(STYLE).append(extraStyle)
          .append("\n</style>\n</head>\n<body>\n");
   }// end of begin method

   private static String th(String key, boolean number) {
      return "<th" + (number ? " class=\"num\"" : "") + ">" + Html.escape(Messages.get(key)) + "</th>";
   }// end of th method

   private static String num(String text) {
      return "<td class=\"num\">" + Html.escape(text) + "</td>";
   }// end of num method

   private static String money(long cents) {
      return Money.format(cents / 100.0);
   }// end of money method

   private static String numbers(List<Integer> ids) {
      return ids.stream().map(String::valueOf).collect(Collectors.joining(", "));
   }// end of numbers method

   private static String time(Instant instant) {
      return DateTimeFormatter.ofLocalizedDateTime(FormatStyle.MEDIUM).withLocale(Messages.locale())
                              .withZone(ZoneId.systemDefault()).format(instant);
   }// end of time method

   private static String date(Instant instant) {
      return DateTimeFormatter.ofLocalizedDate(FormatStyle.LONG).withLocale(Messages.locale())
                              .withZone(ZoneId.systemDefault()).format(instant);
   }// end of date method

}// end of HtmlReports class
