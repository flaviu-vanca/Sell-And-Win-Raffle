package raffle.utils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

/**
 * Timestamped copies of the data folders. All data is small CSV, so a full snapshot per start-up is cheap and
 * gives the operator something to go back to.
 */
public final class BackupService {

   static final int KEEP_SNAPSHOTS = 20;
   private static final DateTimeFormatter STAMP = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss");

   private BackupService() {
   }

   /**
    * Copies {@code data/} and {@code records/} below {@code root} into {@code root/backups/<timestamp>/}.
    *
    * @return the snapshot folder, or empty when there was nothing to back up
    */
   public static Optional<Path> snapshot(Path root) throws IOException {
      Path backups = root.resolve("backups");
      List<Path> sources = List.of(root.resolve("data"), root.resolve("records"));
      if (sources.stream().noneMatch(Files::isDirectory)) {
         return Optional.empty();
      }// end of if block

      Path target = backups.resolve(STAMP.format(LocalDateTime.now()));
      for (Path source : sources) {
         if (Files.isDirectory(source)) {
            copyFlat(source, target.resolve(source.getFileName()));
         }// end of if block
      }// end of for loop

      prune(backups);
      return Optional.of(target);
   }// end of snapshot method

   /**
    * Keeps a file that is about to be removed from the application (for example the ticket ledger of a
    * deleted item) in {@code root/backups/deleted/} instead of destroying it.
    */
   public static Path archive(Path root, Path file) throws IOException {
      Path deleted = root.resolve("backups").resolve("deleted");
      Files.createDirectories(deleted);
      Path target = deleted.resolve(STAMP.format(LocalDateTime.now()) + "-" + file.getFileName());
      return Files.move(file, target, StandardCopyOption.REPLACE_EXISTING);
   }// end of archive method

   private static void copyFlat(Path sourceDir, Path targetDir) throws IOException {
      Files.createDirectories(targetDir);
      try (Stream<Path> files = Files.list(sourceDir)) {
         for (Path file : files.filter(Files::isRegularFile).toList()) {
            Files.copy(file, targetDir.resolve(file.getFileName()), StandardCopyOption.REPLACE_EXISTING);
         }// end of for loop
      }// end of try-with-resources
   }// end of copyFlat method

   private static void prune(Path backups) throws IOException {
      try (Stream<Path> children = Files.list(backups)) {
         List<Path> snapshots = new ArrayList<>(children.filter(Files::isDirectory)
                                                        .filter(path -> ! path.getFileName().toString().equals("deleted"))
                                                        .sorted(Comparator.comparing(Path::getFileName))
                                                        .toList());
         while (snapshots.size() > KEEP_SNAPSHOTS) {
            deleteRecursively(snapshots.removeFirst());
         }// end of while loop
      }// end of try-with-resources
   }// end of prune method

   private static void deleteRecursively(Path directory) throws IOException {
      try (Stream<Path> walk = Files.walk(directory)) {
         for (Path path : walk.sorted(Comparator.reverseOrder()).toList()) {
            Files.deleteIfExists(path);
         }// end of for loop
      }// end of try-with-resources
   }// end of deleteRecursively method

}// end of BackupService class
