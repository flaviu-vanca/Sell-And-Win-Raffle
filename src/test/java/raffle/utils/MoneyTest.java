package raffle.utils;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.Locale;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MoneyTest {

   @AfterEach
   void restoreLanguage() {
      Messages.setLocale(Locale.ENGLISH);
   }

   @Test
   void englishPutsTheSymbolFirst() {
      Messages.setLocale(Locale.ENGLISH);
      assertEquals("€15.00", Money.format(15.0, "EUR"));
   }

   @Test
   void romanianUsesACommaAndTheSymbolAfterwards() {
      Messages.setLocale(Locale.forLanguageTag("ro"));
      String text = Money.format(1234.5, "EUR");

      assertTrue(text.startsWith("1.234,50"), text);
      assertTrue(text.endsWith("€"), text);
   }

   @Test
   void anotherCurrencyCanBeChosen() {
      Messages.setLocale(Locale.forLanguageTag("ro"));
      assertTrue(Money.format(10, "ron").endsWith("lei"), Money.format(10, "ron"));
      Messages.setLocale(Locale.ENGLISH);
      assertTrue(Money.format(10, "USD").startsWith("$"), Money.format(10, "USD"));
   }

   @Test
   void anUnknownCurrencyCodeFallsBackToEuro() {
      Messages.setLocale(Locale.ENGLISH);
      assertEquals("€15.00", Money.format(15.0, "not-a-currency"));
   }
}
