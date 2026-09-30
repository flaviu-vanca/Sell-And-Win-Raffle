package raffle.utils;

import java.io.IOException;
import java.io.Writer;
import java.nio.channels.Channels;
import java.nio.channels.FileChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.util.List;

/**
 * Crash-safe file writing.
 * <p>
 * The old code truncated the target and then wrote to it, so a crash or power loss half way through lost the
 * whole ledger. Here the new content goes to a temporary file first, is flushed to disk, and only then replaces
 * the target in a single rename. The previous version is kept next to it as {@code <name>.bak}.
 */
public final class SafeFiles {

   private SafeFiles() {
   }

   public static void writeLinesAtomically(Path target, List<String> lines) throws IOException {
      Path absolute = target.toAbsolutePath();
      Path directory = absolute.getParent();
      Files.createDirectories(directory);

      Path temp = directory.resolve(absolute.getFileName() + ".tmp");
      try (FileChannel channel = FileChannel.open(temp, StandardOpenOption.CREATE, StandardOpenOption.WRITE, StandardOpenOption.TRUNCATE_EXISTING);
           Writer writer = Channels.newWriter(channel, StandardCharsets.UTF_8)) {
         for (String line : lines) {
            writer.write(line);
            writer.write(System.lineSeparator());
         }// end of for loop
         writer.flush();
         channel.force(true);
      } catch (IOException e) {
         Files.deleteIfExists(temp);
         throw e;
      }// end of try-catch block

      if (Files.exists(absolute)) {
         Files.copy(absolute, directory.resolve(absolute.getFileName() + ".bak"), StandardCopyOption.REPLACE_EXISTING);
      }// end of if block

      try {
         Files.move(temp, absolute, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
      } catch (AtomicMoveNotSupportedException e) {
         Files.move(temp, absolute, StandardCopyOption.REPLACE_EXISTING);
      }// end of try-catch block
   }// end of writeLinesAtomically method

}// end of SafeFiles class
