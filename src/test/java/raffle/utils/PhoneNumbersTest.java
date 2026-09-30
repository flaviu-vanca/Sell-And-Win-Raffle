package raffle.utils;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PhoneNumbersTest {

   @Test
   void tenDigitNumbersThatDoNotFitInAnIntAreValid() {
      // Integer.parseInt rejected these: the old validation only worked for numbers starting with 0
      assertTrue(PhoneNumbers.isValid("7123456789"));
      assertTrue(PhoneNumbers.isValid("5551234567"));
   }

   @Test
   void leadingZeroAndInternationalPrefixAreValid() {
      assertTrue(PhoneNumbers.isValid("0712345678"));
      assertTrue(PhoneNumbers.isValid("+40712345678"));
   }

   @Test
   void separatorsAreIgnored() {
      assertTrue(PhoneNumbers.isValid("0712 345 678"));
      assertEquals("0712345678", PhoneNumbers.normalize("0712-345-678"));
      assertEquals("+40712345678", PhoneNumbers.normalize(" +40 (712) 345.678 "));
   }

   @Test
   void rejectsLettersTooShortAndTooLong() {
      assertFalse(PhoneNumbers.isValid("07123abc78"));
      assertFalse(PhoneNumbers.isValid("123456"));
      assertFalse(PhoneNumbers.isValid("1234567890123456"));
      assertFalse(PhoneNumbers.isValid(""));
      assertFalse(PhoneNumbers.isValid(null));
   }
}
