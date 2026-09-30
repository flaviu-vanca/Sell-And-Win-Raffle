package raffle.services;

/**
 * Something the operator typed or chose is not acceptable. It carries the key of the message to show (and its
 * arguments) instead of the text, so the service stays free of the language and the screens.
 */
public final class ValidationException extends Exception {

   private final String messageKey;
   private final Object[] arguments;

   public ValidationException(String messageKey, Object... arguments) {
      super(messageKey);
      this.messageKey = messageKey;
      this.arguments = arguments;
   }

   public String messageKey() {
      return messageKey;
   }// end of messageKey method

   public Object[] arguments() {
      return arguments.clone();
   }// end of arguments method

}// end of ValidationException class
