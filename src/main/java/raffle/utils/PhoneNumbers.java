package raffle.utils;

import java.util.regex.Pattern;

/**
 * Phone number handling. Numbers are kept as text: a 10 digit number does not fit in an {@code int}, and a
 * leading zero or {@code +} is part of the number.
 */
public final class PhoneNumbers {

   private static final Pattern SEPARATORS = Pattern.compile("[\\s.\\-()]");
   private static final Pattern VALID = Pattern.compile("\\+?\\d{7,15}");

   private PhoneNumbers() {
   }

   /** Removes spaces, dots, dashes and brackets so the same number always compares equal. */
   public static String normalize(String raw) {
      return raw == null ? "" : SEPARATORS.matcher(raw.trim()).replaceAll("");
   }// end of normalize method

   /** 7 to 15 digits, optionally starting with {@code +} (the E.164 maximum is 15 digits). */
   public static boolean isValid(String raw) {
      return VALID.matcher(normalize(raw)).matches();
   }// end of isValid method

}// end of PhoneNumbers class
