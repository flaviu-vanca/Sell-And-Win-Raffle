package raffle.reports;

/** Small helpers for writing HTML by hand. */
final class Html {

   private Html() {
   }

   /** Makes text safe to put inside HTML: names and titles typed by the operator must never become markup. */
   static String escape(String text) {
      if (text == null) {
         return "";
      }// end of if block
      StringBuilder escaped = new StringBuilder(text.length() + 16);
      for (char c : text.toCharArray()) {
         switch (c) {
            case '&' -> escaped.append("&amp;");
            case '<' -> escaped.append("&lt;");
            case '>' -> escaped.append("&gt;");
            case '"' -> escaped.append("&quot;");
            case '\'' -> escaped.append("&#39;");
            default -> escaped.append(c);
         }// end of switch
      }// end of for loop
      return escaped.toString();
   }// end of escape method

}// end of Html class
