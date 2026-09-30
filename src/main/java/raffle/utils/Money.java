package raffle.utils;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.NumberFormat;
import java.util.Currency;
import java.util.Locale;

/**
 * Formats amounts of money for the active language, for example {@code €15.00} in English and
 * {@code 15,00 €} in Romanian. The currency is the {@code currency} setting (an ISO code such as EUR or RON).
 */
public final class Money {

   public static final String DEFAULT_CURRENCY = "EUR";

   private Money() {
   }

   public static String format(double amount) {
      return format(amount, AppSettings.get("currency").orElse(DEFAULT_CURRENCY));
   }// end of format method

   static String format(double amount, String currencyCode) {
      NumberFormat format = NumberFormat.getCurrencyInstance(Messages.locale());
      Currency currency;
      try {
         currency = Currency.getInstance(currencyCode.trim().toUpperCase(Locale.ROOT));
      } catch (IllegalArgumentException e) {
         currency = Currency.getInstance(DEFAULT_CURRENCY);// an unknown code in the settings file
      }// end of try-catch block
      format.setCurrency(currency);

      // The JDK shows "EUR" and "RON" in Romanian; people expect the euro sign and "lei"
      if (format instanceof DecimalFormat decimalFormat) {
         DecimalFormatSymbols symbols = decimalFormat.getDecimalFormatSymbols();
         if (currency.getCurrencyCode().equals("EUR")) {
            symbols.setCurrencySymbol("€");
         } else if (currency.getCurrencyCode().equals("RON") && Messages.locale().getLanguage().equals("ro")) {
            symbols.setCurrencySymbol("lei");
         }// end of if-else block
         decimalFormat.setDecimalFormatSymbols(symbols);
      }// end of if block
      return format.format(amount);
   }// end of format method

}// end of Money class
