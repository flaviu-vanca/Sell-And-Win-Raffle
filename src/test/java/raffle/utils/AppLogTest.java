package raffle.utils;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AppLogTest {

   @TempDir
   Path dir;

   private final PrintStream originalErr = System.err;
   private final ByteArrayOutputStream console = new ByteArrayOutputStream();

   private void useFakeConsole() {
      System.setErr(new PrintStream(console, true, StandardCharsets.UTF_8));
   }

   @AfterEach
   void restoreErr() {
      AppLog.uninstall();// the file must be closed before the temporary folder can be deleted (Windows)
      System.setErr(originalErr);
   }

   @Test
   void whatIsWrittenToTheErrorOutputAlsoGoesToTheFile() throws IOException {
      useFakeConsole();
      Path file = dir.resolve("logs").resolve("app.log");

      AppLog.install(file);
      new IllegalStateException("something went wrong ăș").printStackTrace();

      String logged = Files.readString(file);
      assertTrue(logged.contains("something went wrong ăș"), logged);
      assertTrue(logged.contains("IllegalStateException"));
      assertTrue(console.toString(StandardCharsets.UTF_8).contains("something went wrong"), "the console still gets it too");
   }

   @Test
   void everyStartIsMarkedWithTheTimeAndTheComputer() throws IOException {
      useFakeConsole();
      Path file = dir.resolve("app.log");

      AppLog.install(file);

      String logged = Files.readString(file);
      assertTrue(logged.startsWith("=== "), logged);
      assertTrue(logged.contains("Java " + System.getProperty("java.version")));
   }

   @Test
   void laterStartsAddToTheSameFile() throws IOException {
      useFakeConsole();
      Path file = dir.resolve("app.log");
      AppLog.install(file);
      System.err.println("first run");

      AppLog.uninstall();// the first run ends
      useFakeConsole();
      AppLog.install(file);
      System.err.println("second run");

      String logged = Files.readString(file);
      assertTrue(logged.indexOf("first run") < logged.indexOf("second run"));
      assertEquals(2, logged.split("=== ", -1).length - 1);
   }

   @Test
   void aBigLogIsMovedAsideInsteadOfGrowingForever() throws IOException {
      useFakeConsole();
      Path file = dir.resolve("app.log");
      Files.write(file, new byte[(int) AppLog.MAX_BYTES + 10]);

      AppLog.install(file);

      assertTrue(Files.exists(dir.resolve("app.log.old")));
      assertTrue(Files.size(file) < 1000, "a fresh file");
   }

   @Test
   void aLogThatCannotBeOpenedNeverStopsTheApplication() throws IOException {
      useFakeConsole();
      Path blocker = Files.writeString(dir.resolve("not-a-folder"), "x");

      AppLog.install(blocker.resolve("app.log"));// the parent is a file: cannot be created

      assertTrue(console.toString(StandardCharsets.UTF_8).contains("Could not open the log file"));
      assertFalse(Files.isDirectory(blocker));
   }

   @Test
   void uninstallingGivesTheErrorOutputBackAndClosesTheFile() throws IOException {
      useFakeConsole();
      PrintStream beforeInstall = System.err;
      Path file = dir.resolve("app.log");
      AppLog.install(file);

      AppLog.uninstall();
      System.err.println("after");

      assertTrue(System.err == beforeInstall);
      assertFalse(Files.readString(file).contains("after"), "no longer written to the file");
      Files.delete(file);// a closed file can be deleted on every system
   }
}
