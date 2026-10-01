package raffle.reports;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ReportFilesTest {

   @TempDir
   Path dir;

   @Test
   void aDocumentIsSavedInTheReportsFolderAsUtf8() throws IOException {
      Path file = ReportFiles.write(dir.resolve("reports"), "sales-report", "<p>Câștigător ăîșț</p>");

      assertEquals(dir.resolve("reports"), file.getParent());
      assertTrue(file.getFileName().toString().endsWith("-sales-report.html"));
      assertEquals("<p>Câștigător ăîșț</p>", Files.readString(file));
   }

   @Test
   void twoDocumentsMadeInTheSameSecondDoNotOverwriteEachOther() throws IOException {
      Path first = ReportFiles.write(dir, "receipt", "one");
      Path second = ReportFiles.write(dir, "receipt", "two");

      assertNotEquals(first, second);
      assertEquals("one", Files.readString(first));
      assertEquals("two", Files.readString(second));
   }
}
