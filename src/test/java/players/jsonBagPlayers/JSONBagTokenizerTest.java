package players.jsonBagPlayers;

import core.AbstractGameState;
import games.GameType;
import org.apache.commons.lang3.NotImplementedException;
import org.junit.Test;

import java.util.*;

import static org.junit.Assert.*;

/**
 * Unit tests for {@link Tokenizer}: the JSON-Bag representation of a game trajectory.
 * <p>
 * The defaults of the convenience overloads differ between the String and Object entry points
 * ({@code tokenize(Object)} tokenizes with ordered=true, {@code tokenize(String)} with ordered=false),
 * so most tests call the explicit overload to pin down the behaviour under test.
 */
public class JSONBagTokenizerTest {

    private static Map<String, Integer> tokenize(String json, boolean ordered, boolean numSuffixFrequency,
                                                 boolean filterPlayer, Set<String> filterSet, boolean whitelist) {
        return Tokenizer.tokenize(json, "", ordered, "both", numSuffixFrequency, filterPlayer,
                false, false, filterSet, whitelist);
    }

    @Test
    public void flatObjectProducesKeyValueTokenAndNumericFrequency() {
        Map<String, Integer> bag = Tokenizer.tokenize("{\"a\":1}");
        assertEquals(1, bag.get(".a.1").intValue());
        // the numeric value also contributes its base prefix, counted with the value as frequency
        assertEquals(1, bag.get(".a").intValue());
    }

    @Test
    public void numericFrequencyUsesTheNumericValueAsCount() {
        Map<String, Integer> bag = Tokenizer.tokenize("{\"a\":3}");
        assertEquals(3, bag.get(".a").intValue());
        assertEquals(1, bag.get(".a.3").intValue());
    }

    @Test
    public void numericFrequencyAccumulatesAcrossArrayItems() {
        Map<String, Integer> bag = Tokenizer.tokenize("{\"xs\":[1,2]}");
        assertEquals(3, bag.get(".xs").intValue());
        assertEquals(1, bag.get(".xs.1").intValue());
        assertEquals(1, bag.get(".xs.2").intValue());
    }

    @Test
    public void numericFrequencyIsNotAppliedToNonIntegers() {
        Map<String, Integer> bag = Tokenizer.tokenize("{\"a\":1.5}");
        assertTrue("value token expected", bag.containsKey(".a.1.5"));
        assertFalse("non-integer values must not contribute a base-prefix frequency", bag.containsKey(".a"));
    }

    @Test
    public void numericFrequencyIsNotAppliedToZeroOrNegativeValues() {
        assertFalse(Tokenizer.tokenize("{\"a\":0}").containsKey(".a"));
        assertFalse(Tokenizer.tokenize("{\"a\":-2}").containsKey(".a"));
        assertTrue(Tokenizer.tokenize("{\"a\":-2}").containsKey(".a.-2"));
    }

    @Test
    public void numericFrequencyCanBeDisabled() {
        Map<String, Integer> bag = tokenize("{\"a\":3}", false, false, false, null, false);
        assertEquals(1, bag.get(".a.3").intValue());
        assertFalse(bag.containsKey(".a"));
    }

    @Test
    public void orderedModeAddsBothIndexedAndUnindexedArrayTokens() {
        Map<String, Integer> bag = tokenize("{\"xs\":[7,8]}", true, false, false, null, false);
        assertTrue(bag.containsKey(".xs[0].7"));
        assertTrue(bag.containsKey(".xs[1].8"));
        assertTrue(bag.containsKey(".xs.7"));
        assertTrue(bag.containsKey(".xs.8"));
    }

    @Test
    public void unorderedModeIgnoresArrayPositions() {
        Map<String, Integer> bag = tokenize("{\"xs\":[7,8]}", false, false, false, null, false);
        assertFalse(bag.keySet().stream().anyMatch(k -> k.contains("[")));
        assertEquals(1, bag.get(".xs.7").intValue());
        assertEquals(1, bag.get(".xs.8").intValue());
    }

    @Test
    public void orderedModeEmitsOnlyIndexedArrayTokens() {
        Map<String, Integer> bag = Tokenizer.tokenize("{\"xs\":[7,8]}", "", true, "ordered",
                false, false, false, false, null, false);
        assertTrue(bag.containsKey(".xs[0].7"));
        assertTrue(bag.containsKey(".xs[1].8"));
        assertFalse(bag.containsKey(".xs.7"));
    }

    @Test
    public void nestedObjectsCarryTheParentKeyAsPrefix() {
        Map<String, Integer> bag = tokenize("{\"a\":{\"b\":2}}", false, false, false, null, false);
        assertEquals(1, bag.get(".a.b.2").intValue());
    }

    @Test
    public void nullAndMissingValuesAreTokenizedAsNull() {
        Map<String, Integer> bag = tokenize("{\"a\":null}", false, false, false, null, false);
        assertEquals(1, bag.get(".a.null").intValue());
    }

    @Test
    public void filterPlayerSkipsObjectsOwnedByOtherPlayers() {
        String json = "{\"player\":1,\"v\":5}";
        assertTrue(tokenize(json, false, false, true, null, false).isEmpty());
        assertFalse(tokenize(json, false, false, false, null, false).isEmpty());

        // player 0 is the tokenizing player, so its objects are kept
        String playerZero = "{\"player\":0,\"v\":5}";
        assertFalse(tokenize(playerZero, false, false, true, null, false).isEmpty());
    }

    @Test
    public void whitelistFilterKeepsOnlyListedTokens() {
        Map<String, Integer> bag = tokenize("{\"a\":1,\"b\":2}", false, false, false,
                new HashSet<>(Collections.singletonList(".a.1")), true);
        assertEquals(Collections.singleton(".a.1"), bag.keySet());
    }

    @Test
    public void blacklistFilterRemovesListedTokens() {
        Set<String> blacklist = new HashSet<>(Arrays.asList(".a.1", ".a"));
        Map<String, Integer> bag = tokenize("{\"a\":1,\"b\":2}", false, true, false, blacklist, false);
        assertFalse(bag.containsKey(".a.1"));
        assertFalse(bag.containsKey(".a"));
        assertTrue(bag.containsKey(".b.2"));
    }

    @Test(expected = NotImplementedException.class)
    public void charModeIsExplicitlyUnsupported() {
        Tokenizer.tokenize("{\"a\":1}", "", false, "char", false, false, false, false, null, false);
    }

    @Test
    public void invalidJsonIsTokenizedAsAPrimitiveString() {
        Map<String, Integer> bag = Tokenizer.tokenize("not json at all");
        assertEquals(1, bag.get(".not json at all").intValue());
    }

    @Test
    public void mergeSumsFrequenciesOfSharedKeys() {
        Map<String, Integer> dest = new HashMap<>();
        dest.put("a", 2);
        dest.put("b", 1);
        Map<String, Integer> src = new HashMap<>();
        src.put("a", 3);
        src.put("c", 4);

        Tokenizer.merge(dest, src);

        assertEquals(5, dest.get("a").intValue());
        assertEquals(1, dest.get("b").intValue());
        assertEquals(4, dest.get("c").intValue());
    }

    @Test
    public void filterRetainsWhitelistAndRemovesBlacklistEntries() {
        Map<String, Integer> bag = new HashMap<>();
        bag.put("a", 1);
        bag.put("b", 2);

        Map<String, Integer> whitelisted = new HashMap<>(bag);
        Tokenizer.filter(whitelisted, new HashSet<>(Collections.singletonList("a")), true);
        assertEquals(Collections.singleton("a"), whitelisted.keySet());

        Map<String, Integer> blacklisted = new HashMap<>(bag);
        Tokenizer.filter(blacklisted, new HashSet<>(Collections.singletonList("a")), false);
        assertEquals(Collections.singleton("b"), blacklisted.keySet());
    }

    @Test
    public void filterIsANoOpForAnEmptyFilterSet() {
        Map<String, Integer> bag = new HashMap<>();
        bag.put("a", 1);
        Tokenizer.filter(bag, Collections.emptySet(), true);
        assertEquals(1, bag.size());
    }

    @Test
    public void gameStateIsTokenizedThroughItsContainer() {
        // exercises GameStateContainerFactory + Gson reflection over the container fields
        AbstractGameState gs = GameType.DotsAndBoxes.createGameInstance(2, 1234L).getGameState();
        Map<String, Integer> bag = Tokenizer.tokenize(gs);

        assertFalse("a game state must produce tokens", bag.isEmpty());
        assertTrue("container fields must be serialised", bag.keySet().stream().anyMatch(k -> k.contains("gameType")));
    }

    @Test
    public void tokensAreDistinctForDifferentGameStates() {
        AbstractGameState gs = GameType.DotsAndBoxes.createGameInstance(2, 1234L).getGameState();
        Map<String, Integer> bag = Tokenizer.tokenize(gs);
        Map<String, Integer> merged = new HashMap<>(bag);
        Tokenizer.merge(merged, bag);

        assertEquals(bag.size(), merged.size());
        bag.forEach((token, count) -> assertEquals(2 * count, merged.get(token).intValue()));
    }
}
