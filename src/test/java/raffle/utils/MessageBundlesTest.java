package raffle.utils;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Guards the translations: a missing key would show up as raw text on screen (or break an FXML screen). */
class MessageBundlesTest {

   private static final Path SOURCES = Path.of("src/main/java");
   private static final Path FXML = Path.of("src/main/resources/fxml_files");
   private static final Path BUNDLES = Path.of("src/main/resources/i18n");
   private static final Pattern JAVA_KEY = Pattern.compile("Messages\\.get\\(\"([^\"]+)\"");
   private static final Pattern FXML_KEY = Pattern.compile("=\"%([A-Za-z0-9_.]+)\"");
   private static final Pattern PLACEHOLDER = Pattern.compile("\\{(\\d+)}");

   private static Map<String, String> load(String file) throws IOException {
      Properties properties = new Properties();
      try (Reader reader = Files.newBufferedReader(BUNDLES.resolve(file), StandardCharsets.UTF_8)) {
         properties.load(reader);
      }
      Map<String, String> map = new TreeMap<>();
      properties.forEach((key, value) -> map.put((String) key, (String) value));
      return map;
   }

   @Test
   void englishAndRomanianDefineTheSameKeys() throws IOException {
      Set<String> english = load("messages.properties").keySet();
      Set<String> romanian = load("messages_ro.properties").keySet();

      Set<String> missingInRomanian = new TreeSet<>(english);
      missingInRomanian.removeAll(romanian);
      Set<String> missingInEnglish = new TreeSet<>(romanian);
      missingInEnglish.removeAll(english);

      assertTrue(missingInRomanian.isEmpty(), "not translated to Romanian: " + missingInRomanian);
      assertTrue(missingInEnglish.isEmpty(), "missing from the English default: " + missingInEnglish);
   }

   @Test
   void translationsAreNotEmptyAndKeepTheSamePlaceholders() throws IOException {
      Map<String, String> english = load("messages.properties");
      Map<String, String> romanian = load("messages_ro.properties");

      for (Map.Entry<String, String> entry : english.entrySet()) {
         String translated = romanian.get(entry.getKey());
         assertFalse(translated == null || translated.isBlank(), "empty Romanian text for " + entry.getKey());
         assertEquals(placeholders(entry.getValue()), placeholders(translated), "placeholders differ for " + entry.getKey());
      }
   }

   @Test
   void everyKeyUsedInJavaOrFxmlExists() throws IOException {
      Set<String> defined = load("messages.properties").keySet();
      Set<String> used = new TreeSet<>();
      collect(SOURCES, ".java", JAVA_KEY, used);
      collect(FXML, ".fxml", FXML_KEY, used);

      assertFalse(used.isEmpty(), "no keys found: the test is scanning the wrong folders");
      used.removeAll(defined);
      assertTrue(used.isEmpty(), "used but not defined: " + used);
   }

   private static Set<String> placeholders(String text) {
      Set<String> found = new TreeSet<>();
      Matcher matcher = PLACEHOLDER.matcher(text);
      while (matcher.find()) {
         found.add(matcher.group(1));
      }
      return found;
   }

   private static void collect(Path root, String extension, Pattern pattern, Set<String> into) throws IOException {
      try (Stream<Path> files = Files.walk(root)) {
         for (Path file : files.filter(path -> path.toString().endsWith(extension)).toList()) {
            Matcher matcher = pattern.matcher(Files.readString(file, StandardCharsets.UTF_8));
            while (matcher.find()) {
               into.add(matcher.group(1));
            }
         }
      }
   }
}
