package raffle.utils;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.Locale;

import static org.junit.jupiter.api.Assertions.assertEquals;

class MessagesTest {

   @AfterEach
   void restoreDefault() {
      Messages.setLocale(Locale.ENGLISH);
   }

   @Test
   void englishTextComesFromTheDefaultBundle() {
      Messages.setLocale(Locale.ENGLISH);
      assertEquals("*** Winner ***", Messages.get("draw.winner"));
   }

   @Test
   void romanianBundleIsSelectedByLocale() {
      Messages.setLocale(Locale.forLanguageTag("ro"));
      assertEquals("*** Câștigător ***", Messages.get("draw.winner"));
   }

   @Test
   void unknownLanguageFallsBackToEnglish() {
      Messages.setLocale(Locale.forLanguageTag("fr"));
      assertEquals("*** Winner ***", Messages.get("draw.winner"));
   }

   @Test
   void argumentsAreSubstituted() {
      Messages.setLocale(Locale.ENGLISH);
      assertEquals("3 of 20 tickets sold", Messages.get("draw.soldCount", "3", "20"));
      Messages.setLocale(Locale.forLanguageTag("ro"));
      assertEquals("3 din 20 bilete vândute", Messages.get("draw.soldCount", "3", "20"));
   }

   @Test
   void missingKeyIsReturnedInsteadOfCrashing() {
      assertEquals("no.such.key", Messages.get("no.such.key"));
   }

   @Test
   void unsupportedLanguageIsEnglishEvenWhenTheJvmDefaultIsRomanian() {
      Locale original = Locale.getDefault();
      try {
         Locale.setDefault(Locale.forLanguageTag("ro"));
         Messages.setLocale(Locale.FRENCH);
         assertEquals("*** Winner ***", Messages.get("draw.winner"));
         assertEquals(Locale.ENGLISH, Messages.locale());
      } finally {
         Locale.setDefault(original);
      }
   }

   @Test
   void otherIsTheLanguageTheButtonSwitchesTo() {
      Messages.setLocale(Locale.ENGLISH);
      assertEquals("ro", Messages.other().getLanguage());
      Messages.setLocale(Locale.forLanguageTag("ro"));
      assertEquals("en", Messages.other().getLanguage());
   }

   @Test
   void bundleIsExposedForFxml() {
      Messages.setLocale(Locale.forLanguageTag("ro"));
      assertEquals("Extragere", Messages.bundle().getString("main.btn.draw"));
   }
}
