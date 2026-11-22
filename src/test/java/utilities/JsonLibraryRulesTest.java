package utilities;

import org.junit.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static org.junit.Assert.assertTrue;

/**
 * Jackson, through {@link Json}, is TAG's JSON library (JSONSimple remains for config and parameter files).
 * Gson still arrives in the jar through other libraries, so these rules are checked on the sources rather
 * than left to the dependency list.
 */
public class JsonLibraryRulesTest {

    /** Compares Gson's serialisation with Jackson's, so it is the one place Gson may still be used. */
    private static final Set<String> GSON_ALLOWED = Set.of(
            "src/test/java/players/jsonBagPlayers/TokenizerSerialiserEquivalenceTest.java",
            "src/test/java/utilities/JsonLibraryRulesTest.java");

    private static final Set<String> NEW_MAPPER_ALLOWED = Set.of(
            "src/main/java/utilities/Json.java",
            "src/test/java/utilities/JsonLibraryRulesTest.java");

    private static List<Path> sources() throws IOException {
        try (Stream<Path> files = Files.walk(Paths.get("src"))) {
            return files.filter(p -> p.toString().endsWith(".java")).toList();
        }
    }

    private static List<String> offenders(Pattern pattern, Set<String> allowed) throws IOException {
        List<String> found = new java.util.ArrayList<>();
        for (Path p : sources()) {
            String name = p.toString().replace('\\', '/');
            if (!allowed.contains(name) && pattern.matcher(Files.readString(p)).find()) found.add(name);
        }
        return found;
    }

    @Test
    public void gsonIsNotUsed() throws IOException {
        List<String> found = offenders(Pattern.compile("com\\.google\\.gson"), GSON_ALLOWED);
        assertTrue("Use utilities.Json (Jackson) instead of Gson in: " + found, found.isEmpty());
    }

    @Test
    public void noObjectMapperIsCreatedOutsideJson() throws IOException {
        List<String> found = offenders(Pattern.compile("new\\s+([\\w.]+\\.)?ObjectMapper\\s*\\("), NEW_MAPPER_ALLOWED);
        assertTrue("Use utilities.Json.mapper() instead of new ObjectMapper() in: " + found, found.isEmpty());
    }
}
