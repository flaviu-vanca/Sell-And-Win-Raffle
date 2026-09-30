package raffle.utils;

import java.util.ArrayList;
import java.util.List;

/**
 * Minimal CSV helpers shared by the readers and writers.
 * <p>
 * Fields are always written quoted with embedded quotes doubled ({@code "} becomes {@code ""}), so names or
 * descriptions containing quotes or commas survive a write/read round trip. The parser also accepts the
 * older files this application produced (unquoted numbers, empty {@code 1,,,} rows).
 */
public final class CsvUtil {

   private CsvUtil() {
   }

   public static String quote(String value) {
      String text = value == null ? "" : value;
      return "\"" + text.replace("\"", "\"\"") + "\"";
   }// end of quote method

   public static String[] parseLine(String line) {
      List<String> tokens = new ArrayList<>();
      StringBuilder sb = new StringBuilder();
      boolean inQuotes = false;

      for (int i = 0; i < line.length(); i++) {
         char c = line.charAt(i);
         if (inQuotes) {
            if (c == '"') {
               if (i + 1 < line.length() && line.charAt(i + 1) == '"') {
                  sb.append('"');// escaped quote inside a quoted field
                  i++;
               } else {
                  inQuotes = false;
               }// end of if-else block
            } else {
               sb.append(c);
            }// end of if-else block
         } else if (c == '"') {
            inQuotes = true;
         } else if (c == ',') {
            tokens.add(sb.toString());
            sb.setLength(0);
         } else {
            sb.append(c);
         }// end of if-else block
      }// end of for loop
      tokens.add(sb.toString());

      return tokens.toArray(new String[0]);
   }// end of parseLine method

}// end of CsvUtil class
