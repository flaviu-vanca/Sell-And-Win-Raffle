package raffle.utils;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BackupServiceTest {

   @TempDir
   Path root;

   @Test
   void nothingToBackUpOnAFreshInstall() throws IOException {
      assertEquals(Optional.empty(), BackupService.snapshot(root));
      assertFalse(Files.exists(root.resolve("backups")));
   }

   @Test
   void snapshotCopiesCatalogAndLedgers() throws IOException {
      Files.createDirectories(root.resolve("data"));
      Files.createDirectories(root.resolve("records"));
      Files.writeString(root.resolve("data").resolve("data.csv"), "catalog");
      Files.writeString(root.resolve("records").resolve("Bike.csv"), "ledger");

      Path snapshot = BackupService.snapshot(root).orElseThrow();

      assertEquals("catalog", Files.readString(snapshot.resolve("data").resolve("data.csv")));
      assertEquals("ledger", Files.readString(snapshot.resolve("records").resolve("Bike.csv")));
   }

   @Test
   void onlyTheNewestSnapshotsAreKept() throws IOException {
      Files.createDirectories(root.resolve("data"));
      Files.writeString(root.resolve("data").resolve("data.csv"), "catalog");
      Path backups = root.resolve("backups");
      for (int i = 1; i <= BackupService.KEEP_SNAPSHOTS + 5; i++) {
         Files.createDirectories(backups.resolve(String.format("2020010100%04d", i)));
      }
      Files.createDirectories(backups.resolve("deleted"));

      Path fresh = BackupService.snapshot(root).orElseThrow();

      try (Stream<Path> children = Files.list(backups)) {
         long snapshots = children.filter(Files::isDirectory).filter(p -> ! p.getFileName().toString().equals("deleted")).count();
         assertEquals(BackupService.KEEP_SNAPSHOTS, snapshots);
      }
      assertTrue(Files.exists(fresh), "the new snapshot must survive pruning");
      assertTrue(Files.exists(backups.resolve("deleted")), "the archive of deleted items is never pruned");
   }

   @Test
   void archiveMovesAFileInsteadOfDestroyingIt() throws IOException {
      Files.createDirectories(root.resolve("records"));
      Path ledger = root.resolve("records").resolve("Bike.csv");
      Files.writeString(ledger, "sales");

      Path archived = BackupService.archive(root, ledger);

      assertFalse(Files.exists(ledger));
      assertEquals("sales", Files.readString(archived));
      assertTrue(archived.getFileName().toString().endsWith("-Bike.csv"));
   }
}
