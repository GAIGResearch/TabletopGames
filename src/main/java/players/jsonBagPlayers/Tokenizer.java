package players.jsonBagPlayers;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.IntNode;
import com.fasterxml.jackson.databind.node.TextNode;
import core.AbstractGameState;
import core.AbstractGameStateContainer;
import evaluation.containers.GameStateContainerFactory;
import org.apache.commons.lang3.NotImplementedException;
import utilities.Json;

import java.io.FileReader;
import java.io.IOException;
import java.util.*;

public class Tokenizer {

    /**
     * Tokenizes a JSON string using the default configuration.
     * Numeric atomic values are also counted under their base prefix when possible.
     */
    public static Map<String, Integer> tokenize(String json) {
        return tokenize(json, "", false, "both", true, false, false, false, null, false);
    }

    /**
     * Tokenizes a JSON string with optional token filtering.
     * Numeric atomic values are also counted under their base prefix when possible.
     */
    public static Map<String, Integer> tokenize(String json, Set<String> filterSet, boolean whitelist) {
        return tokenize(json, "", false, "both", true, false, false, false, filterSet, whitelist);
    }

    /**
     * Tokenizes an arbitrary object using the default configuration.
     * Numeric atomic values are also counted under their base prefix when possible.
     */
    public static Map<String, Integer> tokenize(Object obj) {
        return tokenize(obj, "", true, "both", true, false, false, false, null, false);
    }

    /**
     * Tokenizes an arbitrary object with optional token filtering.
     * Numeric atomic values are also counted under their base prefix when possible.
     */
    public static Map<String, Integer> tokenize(Object obj, Set<String> filterSet, boolean whitelist) {
        return tokenize(obj, "", true, "both", true, false, false, false, filterSet, whitelist);
    }

    public static Map<String, Integer> tokenize(AbstractGameState gs) {
        AbstractGameStateContainer gsContainer = GameStateContainerFactory.createContainer(gs);
        return Tokenizer.tokenize(gsContainer);
    }
    public static Map<String, Integer> tokenize(AbstractGameState gs, Set<String> filterSet, boolean whitelist) {
        AbstractGameStateContainer gsContainer = GameStateContainerFactory.createContainer(gs);
        return Tokenizer.tokenize(gsContainer, filterSet, whitelist);
    }

    /**
     * Tokenizes an arbitrary object with full control over prefixing, ordering, and filtering.
     *
     * @param obj the object to tokenize
     * @param prefix prefix applied to generated tokens
     * @param ordered whether ordered array positions should be included
     * @param mode tokenization mode: both, ordered, unordered, or char
     * @param numSuffixFrequency when true, numeric atomic values also contribute the base prefix token as a frequency
     * @param filterPlayer whether to skip objects with player > 0
     * @param binning whether to bin numeric x/y values
     * @param pairXY whether to combine x/y into a single token
     * @param filterSet whitelist or blacklist token set
     * @param whitelist true for whitelist filtering, false for blacklist filtering
     * @return token frequency map
     */
    public static Map<String, Integer> tokenize(Object obj, String prefix, boolean ordered,
                                                String mode, boolean numSuffixFrequency, boolean filterPlayer, boolean binning, boolean pairXY,
                                                Set<String> filterSet, boolean whitelist) {
        JsonNode el = Json.toTree(obj);
        return tokenize(el, prefix, ordered, mode, numSuffixFrequency, filterPlayer, binning, pairXY, filterSet, whitelist);
    }

    /**
     * Tokenizes an arbitrary object with the default full-option behavior.
     */
    public static Map<String, Integer> tokenize(Object obj, String prefix, boolean ordered,
                                                String mode, boolean filterPlayer, boolean binning, boolean pairXY,
                                                Set<String> filterSet, boolean whitelist) {
        return tokenize(obj, prefix, ordered, mode, false, filterPlayer, binning, pairXY, filterSet, whitelist);
    }

    /**
     * Tokenizes a JSON string with full control over prefixing, ordering, and filtering.
     * See {@link #tokenize(JsonNode, String, boolean, String, boolean, boolean, boolean, boolean, Set, boolean)}
     * for the tokenization behavior.
     */
    public static Map<String, Integer> tokenize(String json, String prefix, boolean ordered,
                                                String mode, boolean numSuffixFrequency, boolean filterPlayer, boolean binning, boolean pairXY,
                                                Set<String> filterSet, boolean whitelist) {
        JsonNode root;
        try {
            root = Json.mapper().readTree(json);
        } catch (JsonProcessingException e) {
            // treat as primitive string if not valid JSON
            root = TextNode.valueOf(json);
        }
        return tokenize(root, prefix, ordered, mode, numSuffixFrequency, filterPlayer, binning, pairXY, filterSet, whitelist);
    }

    /**
     * Tokenizes a JSON string using the default full-option behavior.
     */
    public static Map<String, Integer> tokenize(String json, String prefix, boolean ordered,
                                                String mode, boolean filterPlayer, boolean binning, boolean pairXY,
                                                Set<String> filterSet, boolean whitelist) {
        return tokenize(json, prefix, ordered, mode, true, filterPlayer, binning, pairXY, filterSet, whitelist);
    }

    /**
     * Tokenizes a JSON element into a token frequency map.
     *
     * <p>Arrays can be tokenized with ordered and/or unordered prefixes, objects are walked
     * recursively, and atomic numeric values can optionally contribute their base prefix as a
     * frequency count when {@code numSuffixFrequency} is enabled.</p>
     *
     * @param collection the JSON element to tokenize
     * @param prefix prefix applied to generated tokens
     * @param ordered whether ordered array positions should be included (e.g., set this to {@code false} when tokenizing a list of JSON objects to ignore their ordering)
     * @param mode tokenization mode: {@code both}, {@code ordered}, {@code unordered}, or {@code char}
     * @param numSuffixFrequency when true, numeric atomic values also add the base prefix token using the numeric value as its count
     * @param filterPlayer whether to skip objects with {@code player > 0}
     * @param binning whether to bin numeric {@code x}/{@code y} values
     * @param pairXY whether to combine {@code x}/{@code y} into a single token
     * @param filterSet whitelist or blacklist token set
     * @param whitelist true for whitelist filtering, false for blacklist filtering
     * @return token frequency map
     */
    public static Map<String, Integer> tokenize(JsonNode collection, String prefix, boolean ordered,
                                                String mode, boolean numSuffixFrequency, boolean filterPlayer, boolean binning, boolean pairXY,
                                                Set<String> filterSet, boolean whitelist) {
        Map<String, Integer> freq = new HashMap<>();
        if ("char".equals(mode)) {
//            addToken(freq, collection.toString());
//            return freq;
            throw new NotImplementedException("Tokenize mode 'char' is not supported");
        }

        if (collection == null || collection.isNull() || collection.isMissingNode()) return freq;

        if (collection.isArray()) {
            for (int i = 0; i < collection.size(); i++) {
                String orderedPrefix = prefix + "[" + i + "]";
                JsonNode item = collection.get(i);
                if (isAtomic(item)) {
                    String s = primitiveToString(item);
                    if (ordered && !"unordered".equals(mode)) {
                        addToken(freq, orderedPrefix + "." + s, filterSet, whitelist);
                        addNumericFrequencyToken(freq, orderedPrefix, item, numSuffixFrequency, filterSet, whitelist);
                    }
                    if (!"ordered".equals(mode) || !ordered) {
                        addToken(freq, prefix + "." + s, filterSet, whitelist);
                        addNumericFrequencyToken(freq, prefix, item, numSuffixFrequency, filterSet, whitelist);
                    }
                } else {
                    if (ordered && !"unordered".equals(mode)) {
                        merge(freq, tokenize(item, orderedPrefix, ordered, mode, numSuffixFrequency, filterPlayer, binning, pairXY, filterSet, whitelist));
                    }
                    if (!"ordered".equals(mode) || !ordered) {
                        // Set subsequent call ordered = True if mode is not unordered
                        merge(freq, tokenize(item, prefix, !"unordered".equals(mode), mode, numSuffixFrequency, filterPlayer, binning, pairXY, filterSet, whitelist));
                    }
                }
            }
        } else if (collection.isObject()) {
            // filter player (if present and > 0)
            JsonNode player = collection.get("player");
            if (filterPlayer && player != null && player.isValueNode() && !player.isNull()) {
                int p = player.asInt();
                if (p > 0) return freq;
            }

            int pairX = -99, pairY = -99;
            for (Map.Entry<String, JsonNode> e : collection.properties()) {
                String key = e.getKey();
                String keyPrefix = prefix + "." + key;
                JsonNode value = e.getValue();
                if (isAtomic(value)) {
                    // binning numerical value
                    if (binning && ("x".equals(key) || "y".equals(key))) {
                        int n = 2;
                        double dv = value.asDouble();
                        int b = ((int) Math.floor(dv / n)) * n;
                        value = IntNode.valueOf(b);
                    }
                    if ("x".equals(key)) {
                        pairX = value.asInt();
                        if (pairXY) continue;
                    }
                    if ("y".equals(key)) {
                        pairY = value.asInt();
                        if (pairXY) continue;
                    }
                    addToken(freq, keyPrefix + "." + primitiveToString(value), filterSet, whitelist);
                    addNumericFrequencyToken(freq, keyPrefix, value, numSuffixFrequency, filterSet, whitelist);
                } else {
                    merge(freq, tokenize(value, keyPrefix, ordered, mode, numSuffixFrequency, filterPlayer, binning, pairXY, filterSet, whitelist));
                }
            }
            if (pairXY && pairX >= 0) {
                addToken(freq, prefix + ".x." + pairX + ".y." + pairY, filterSet, whitelist);
            }
        } else { // primitive
            addToken(freq, prefix + "." + primitiveToString(collection), filterSet, whitelist);
        }

        return freq;
    }

    private static boolean isValidToken(String token, Set<String> filterSet, boolean whitelist) {
        if (token == null) return false;
        if (filterSet == null || filterSet.isEmpty()) return true;
        if (!whitelist) {   // Blacklist filtering
            // TODO MAKE THIS BLACKLIST FILTER WITH REGEX INSTEAD (as to also include substrings filtering)
            return !filterSet.contains(token);
        }
        else {  // Whitelist filtering
            return filterSet.contains(token);
        }
    }

    private static boolean isAtomic(JsonNode el) {
        return el == null || el.isValueNode() || el.isMissingNode();
    }

    private static String primitiveToString(JsonNode el) {
        if (el == null || el.isNull() || el.isMissingNode()) return "null";
        if (el.isContainerNode()) return el.toString();
        if (el.isTextual()) return el.textValue();
        // the boxed number's own toString, so 3 stays "3" and 3.0 stays "3.0"
        if (el.isNumber()) return el.numberValue().toString();
        if (el.isBoolean()) return Boolean.toString(el.booleanValue());
        return el.asText();
    }

    public static void addToken(Map<String, Integer> map, String token) {
        if (token == null) return;
        Integer v = map.get(token);
        map.put(token, v == null ? 1 : v + 1);
    }

    // With filterList
    public static void addToken(Map<String, Integer> map, String token, Set<String> filterSet, boolean whitelist) {
        if (!isValidToken(token, filterSet, whitelist)) return;
        Integer v = map.get(token);
        map.put(token, v == null ? 1 : v + 1);
    }

    private static void addNumericFrequencyToken(Map<String, Integer> map, String tokenPrefix, JsonNode item,
                                                 boolean numSuffixFrequency, Set<String> filterSet, boolean whitelist) {
        if (!numSuffixFrequency || tokenPrefix == null || !isPositiveInteger(item)) return;
        int frequency = item.asInt();
        if (!isValidToken(tokenPrefix, filterSet, whitelist)) return;
        map.merge(tokenPrefix, frequency, Integer::sum);
    }

    private static boolean isPositiveInteger(JsonNode el) {
        if (el == null || !el.isNumber()) return false;
        Number number = el.numberValue();
        if (number instanceof Integer || number instanceof Long || number instanceof Short || number instanceof Byte) {
            return number.longValue() > 0;
        }
        double d = number.doubleValue();
        return d > 0 && d == Math.rint(d);
    }

    public static void merge(Map<String, Integer> dest, Map<String, Integer> src) {
        for (Map.Entry<String, Integer> e : src.entrySet()) {
//            dest.put(e.getKey(), dest.getOrDefault(e.getKey(), 0) + e.getValue());
            dest.merge(e.getKey(), e.getValue(), Integer::sum);
        }
    }

    public static void filter(Map<String, Integer> freq, Set<String> filterSet, boolean whitelist) {
        if (filterSet == null || filterSet.isEmpty()) return;
        if (whitelist) {
            freq.keySet().retainAll(filterSet);
        } else {
            filterSet.forEach(freq.keySet()::remove);
        }
    }

    /**
     * Loads n_prototypes of type Map<String, Integer> from JSON files.
     * Files are expected to be named "prototype{i}.json" where i ranges from 0 to n_prototypes-1.
     *
     * @param path         - the directory path containing the prototype JSON files
     * @param n_prototypes - the number of prototypes to load
     * @return a list of Map<String, Integer> loaded from the JSON files
     */
    public static List<Map<String, Integer>> loadPrototypes(String path, int n_prototypes, boolean full) {
        List<Map<String, Integer>> prototypes = new ArrayList<>();
        TypeReference<Map<String, Integer>> mapType = new TypeReference<>() {};
        String fullString = full ? "full_" : "";
        for (int i = 0; i < n_prototypes; i++) {
            String filePath = path + "/" + fullString + "prototype" + i + ".json";
            try (FileReader reader = new FileReader(filePath)) {
                Map<String, Integer> prototype = Json.mapper().readValue(reader, mapType);
                if (prototype != null) {
                    prototypes.add(prototype);
                } else {
                    System.err.println("Warning: prototype" + i + ".json was empty or invalid");
                    prototypes.add(new HashMap<>());
                }
            } catch (IOException e) {
                System.err.println("Error loading prototype" + i + ".json: " + e.getMessage());
                prototypes.add(new HashMap<>());
            }
        }
        return prototypes;
    }

    /**
     * Loads a list of strings from a JSON file.
     * The JSON file should contain an array of strings, e.g., ["item1", "item2", "item3"]
     *
     * @param filePath - the path to the JSON file
     * @return a list of strings loaded from the JSON file, or an empty list if loading fails
     */
    public static List<String> loadStringList(String filePath) {
        try (FileReader reader = new FileReader(filePath)) {
            List<String> result = Json.mapper().readValue(reader, new TypeReference<List<String>>() {});
            return result != null ? result : new ArrayList<>();
        } catch (IOException e) {
            System.err.println("Error loading string list from " + filePath + ": " + e.getMessage());
            return new ArrayList<>();
        }
    }
    
}
