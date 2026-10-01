package raffle.utils;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Instant;

/**
 * Keeps what the application writes to its error output (the stack traces of things that went wrong) in a file as
 * well. The packaged application has no console, so without this a failure on someone else's computer would leave
 * nothing to look at. The file is {@code logs/app.log}; it is moved to {@code app.log.old} when it passes 1 MB.
 */
public final class AppLog {

   static final long MAX_BYTES = 1_000_000;

   // What System.err was before the log took over, and the open log file (the file stays open while the application runs)
   private static PrintStream replaced;
   private static OutputStream logFile;

   private AppLog() {
   }

   /** Starts writing the error output to the file too. Logging is a convenience: a problem here never stops the application. */
   public static synchronized void install(Path file) {
      uninstall();
      PrintStream original = System.err;
      try {
         Files.createDirectories(file.toAbsolutePath().getParent());
         if (Files.exists(file) && Files.size(file) > MAX_BYTES) {
            Files.move(file, file.resolveSibling(file.getFileName() + ".old"), StandardCopyOption.REPLACE_EXISTING);
         }// end of if block
         OutputStream log = new FileOutputStream(file.toFile(), true);
         replaced = original;
         logFile = log;
         System.setErr(new PrintStream(new Tee(original, log), true, StandardCharsets.UTF_8));
         System.err.println("=== " + Instant.now() + " | Java " + System.getProperty("java.version") + " | "
                            + System.getProperty("os.name") + " | " + System.getProperty("user.home"));
      } catch (IOException | RuntimeException e) {
         original.println("Could not open the log file " + file + ": " + e.getMessage());
      }// end of try-catch block
   }// end of install method

   /** Gives the error output back and closes the file. The application never needs this; tests do (Windows cannot delete an open file). */
   public static synchronized void uninstall() {
      if (logFile != null) {
         System.err.flush();
         System.setErr(replaced);
         try {
            logFile.close();
         } catch (IOException ignored) {
            // nothing more can be done
         }// end of try-catch block
         logFile = null;
         replaced = null;
      }// end of if block
   }// end of uninstall method

   // Writes to the console (when there is one) and to the file; a failing half never breaks the other
   private static final class Tee extends OutputStream {

      private final OutputStream first;
      private final OutputStream second;

      Tee(OutputStream first, OutputStream second) {
         this.first = first;
         this.second = second;
      }

      @Override
      public void write(int b) {
         try {
            first.write(b);
         } catch (IOException ignored) {
            // no console to write to
         }// end of try-catch block
         try {
            second.write(b);
         } catch (IOException ignored) {
            // the file is full or gone
         }// end of try-catch block
      }// end of write method

      @Override
      public void write(byte[] bytes, int offset, int length) {
         try {
            first.write(bytes, offset, length);
         } catch (IOException ignored) {
            // no console to write to
         }// end of try-catch block
         try {
            second.write(bytes, offset, length);
         } catch (IOException ignored) {
            // the file is full or gone
         }// end of try-catch block
      }// end of write method

      @Override
      public void flush() {
         try {
            first.flush();
         } catch (IOException ignored) {
            // no console to write to
         }// end of try-catch block
         try {
            second.flush();
         } catch (IOException ignored) {
            // the file is full or gone
         }// end of try-catch block
      }// end of flush method

   }// end of Tee class

}// end of AppLog class
