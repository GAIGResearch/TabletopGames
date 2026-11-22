package players.jsonBagPlayers;

import org.junit.Test;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.*;

import static org.junit.Assert.assertEquals;

/**
 * Pins the {@link Tokenizer}'s output on fixed JSON documents, for every combination of options, so a
 * change to the tree walk (such as the move from Gson to Jackson) cannot silently change the tokens.
 * The digests were recorded with the Gson tokenizer.
 * <p>
 * Number literals are written as Java writes them ({@code Double.toString}); literals that Java never
 * writes, such as {@code 1.50} or {@code 1e5}, are normalised by the parser and are not pinned here.
 * <p>
 * To regenerate after an intended change, run with {@code -Dgolden.print=true} and paste the output.
 */
public class TokenizerGoldenTest {

    static final String[] DOCUMENTS = {
            // flat values of every primitive type
            "{\"i\":3,\"neg\":-2,\"zero\":0,\"d\":1.5,\"whole\":3.0,\"tiny\":1.0E-5,\"big\":1.0E7,\"long\":9007199254740993," +
                    "\"s\":\"text\",\"t\":true,\"f\":false,\"n\":null,\"u\":\"\\u00e9\\u4e2d\"}",
            // arrays of primitives, nested arrays and empty containers
            "{\"xs\":[7,8,7],\"ss\":[\"a\",\"b\"],\"grid\":[[1,0],[0,2]],\"mixed\":[1,\"a\",null,true,2.5],\"e\":[],\"o\":{}}",
            // arrays of objects, as decks of cards
            "{\"deck\":[{\"name\":\"Copper\",\"cost\":0},{\"name\":\"Silver\",\"cost\":3},{\"name\":\"Copper\",\"cost\":0}]," +
                    "\"hands\":[[{\"name\":\"Gold\",\"cost\":6}],[]]}",
            // per-player objects, for player filtering
            "{\"players\":[{\"player\":0,\"score\":5},{\"player\":1,\"score\":7},{\"player\":2,\"pieces\":[1,2]}]," +
                    "\"owner\":{\"player\":-1,\"v\":1}}",
            // coordinates, for binning and x/y pairing
            "{\"pieces\":[{\"x\":3,\"y\":5,\"c\":\"red\"},{\"x\":0,\"y\":1},{\"x\":7.0,\"y\":2.0}],\"pos\":{\"x\":4,\"y\":9}}",
            // a top-level array and a bare primitive
            "[{\"a\":1},{\"a\":2},3,\"z\"]",
            // not JSON: tokenized as a string
            "not json at all",
    };

    record Options(boolean ordered, String mode, boolean numSuffixFrequency, boolean filterPlayer,
                   boolean binning, boolean pairXY) {
    }

    static final Options[] OPTIONS = {
            new Options(false, "both", true, false, false, false),
            new Options(true, "both", true, false, false, false),
            new Options(true, "ordered", true, false, false, false),
            new Options(true, "unordered", true, false, false, false),
            new Options(false, "ordered", true, false, false, false),
            new Options(false, "both", false, false, false, false),
            new Options(true, "both", false, false, false, false),
            new Options(false, "both", true, true, false, false),
            new Options(true, "both", true, true, false, false),
            new Options(false, "both", true, false, true, false),
            new Options(false, "both", true, false, false, true),
            new Options(true, "both", false, true, true, true),
    };

    static final String[] EXPECTED = {
            "67d3be773b6c2abc", "67d3be773b6c2abc", "67d3be773b6c2abc", "67d3be773b6c2abc", "67d3be773b6c2abc", "8046a04d6a00b7cf", "8046a04d6a00b7cf", "67d3be773b6c2abc", "67d3be773b6c2abc", "67d3be773b6c2abc", "67d3be773b6c2abc", "8046a04d6a00b7cf",
            "6bdc38f80d4bf81c", "1ace80c914eb0d13", "7d61d1b5f4cc37e5", "e9fbe19995178aef", "a601134bb0b74b54", "4410ce21e84b7ca8", "864541cdc4c51d86", "6bdc38f80d4bf81c", "1ace80c914eb0d13", "6bdc38f80d4bf81c", "6bdc38f80d4bf81c", "864541cdc4c51d86",
            "5ec23a76cd7c4b79", "846dc8be7f69679f", "96f9b10fd8583421", "f8177bfd06ae0c85", "5521e8cf32c2a667", "55a3a51aa9cfb2cd", "b664344561822fbb", "5ec23a76cd7c4b79", "846dc8be7f69679f", "5ec23a76cd7c4b79", "5ec23a76cd7c4b79", "b664344561822fbb",
            "a8233f42761df31e", "0211a39e49fa554d", "1f8030facab8566c", "8ad193f3735f3266", "3a43c92b71968a2c", "f27456c26bf53d0e", "e43e2dad18c4f62a", "e1716aad4993804a", "fbb86070c3404977", "a8233f42761df31e", "a8233f42761df31e", "2374ac81dd53290f",
            "dad578ca22915b70", "1e6d195c0fb97a5a", "486cf0b88573c3bb", "dad578ca22915b70", "dad578ca22915b70", "08f739d365237f23", "38db7c064a1c11b8", "dad578ca22915b70", "1e6d195c0fb97a5a", "5529976f34cf251f", "d932c06541ad9803", "6b32fda04d173ce5",
            "383d0fbffe8efd63", "b5c387ff704b7123", "d811e821e3b28483", "383d0fbffe8efd63", "383d0fbffe8efd63", "ccf326894d81447c", "2f5a16c300c96c3f", "383d0fbffe8efd63", "b5c387ff704b7123", "383d0fbffe8efd63", "383d0fbffe8efd63", "2f5a16c300c96c3f",
            "f0ade13be21c50d3", "f0ade13be21c50d3", "f0ade13be21c50d3", "f0ade13be21c50d3", "f0ade13be21c50d3", "f0ade13be21c50d3", "f0ade13be21c50d3", "f0ade13be21c50d3", "f0ade13be21c50d3", "f0ade13be21c50d3", "f0ade13be21c50d3", "f0ade13be21c50d3",
    };

    static String digest(Map<String, Integer> bag) throws Exception {
        StringBuilder sb = new StringBuilder();
        new TreeMap<>(bag).forEach((k, v) -> sb.append(k).append('=').append(v).append('\n'));
        byte[] hash = MessageDigest.getInstance("SHA-256").digest(sb.toString().getBytes(StandardCharsets.UTF_8));
        return HexFormat.of().formatHex(hash, 0, 8);
    }

    static List<String> actualDigests() throws Exception {
        List<String> result = new ArrayList<>();
        for (String doc : DOCUMENTS) {
            for (Options o : OPTIONS) {
                result.add(digest(Tokenizer.tokenize(doc, "", o.ordered(), o.mode(), o.numSuffixFrequency(),
                        o.filterPlayer(), o.binning(), o.pairXY(), null, false)));
            }
        }
        return result;
    }

    @Test
    public void tokensMatchRecordedDigests() throws Exception {
        List<String> actual = actualDigests();
        if (Boolean.getBoolean("golden.print")) {
            for (int i = 0; i < actual.size(); i += OPTIONS.length)
                System.out.println("            " + String.join(", ",
                        actual.subList(i, i + OPTIONS.length).stream().map(s -> "\"" + s + "\"").toList()) + ",");
        }
        assertEquals(DOCUMENTS.length * OPTIONS.length, EXPECTED.length);
        for (int i = 0; i < actual.size(); i++) {
            int doc = i / OPTIONS.length, opt = i % OPTIONS.length;
            assertEquals("document " + doc + ", " + OPTIONS[opt], EXPECTED[i], actual.get(i));
        }
    }
}
