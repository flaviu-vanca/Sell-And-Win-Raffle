package raffle.utils;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

class CsvUtilTest {

   @Test
   void quoteDoublesEmbeddedQuotes() {
      assertEquals("\"say \"\"hi\"\"\"", CsvUtil.quote("say \"hi\""));
   }

   @Test
   void quoteTreatsNullAsEmpty() {
      assertEquals("\"\"", CsvUtil.quote(null));
   }

   @Test
   void parsesPlainAndQuotedFields() {
      assertArrayEquals(new String[]{"1", "Ion Popescu", "0712345678", "3"}, CsvUtil.parseLine("1,\"Ion Popescu\",\"0712345678\",3"));
   }

   @Test
   void commaInsideQuotesIsNotASeparator() {
      assertArrayEquals(new String[]{"a", "x, y", "b"}, CsvUtil.parseLine("a,\"x, y\",b"));
   }

   @Test
   void emptyFieldsAreKept() {
      assertArrayEquals(new String[]{"1", "", "", ""}, CsvUtil.parseLine("1,,,"));
      assertArrayEquals(new String[]{"1", "", ""}, CsvUtil.parseLine("1,\"\",\"\""));
   }

   @Test
   void escapedQuoteRoundTrips() {
      String original = "O\"Brien, Jr.";
      assertArrayEquals(new String[]{original}, CsvUtil.parseLine(CsvUtil.quote(original)));
   }
}
