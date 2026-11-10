package players.jsonBagPlayers;


import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class TokenizerTest {
    public static void main(String[] args) throws IOException {
        String jsonString = new String(Files.readAllBytes(Paths.get("src/main/java/players/jsonBagPlayers/test.txt")));;
        Set<String> filterSet = new HashSet<>(Tokenizer.loadStringList("src/main/java/players/jsonBagPlayers/filterTest.json"));
        filterSet = new HashSet<>();
        Map<String, Integer> jsonBag;

//        jsonBag = Tokenizer.tokenize(jsonString);
        jsonBag = Tokenizer.tokenize(jsonString, filterSet, true);
//        Tokenizer.filter(jsonBag, filterSet, true);
        System.out.println(jsonBag.size());
        jsonBag.forEach((key, value) -> System.out.println(key + " : " + value));
    }
}
