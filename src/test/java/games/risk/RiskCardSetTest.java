package games.risk;

import games.risk.components.RiskCard;
import org.junit.Test;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static games.risk.RiskTestUtils.card;
import static games.risk.RiskTestUtils.wild;
import static games.risk.WorldMap.*;
import static org.junit.Assert.*;

/**
 * Which 3 cards make a set (RiskUtils.isSet), the distinct sets in a hand (RiskUtils.sets) and the value of the
 * k-th set traded (RiskParameters.tradeValue).
 * Symbols used: Infantry - Alaska, Venezuela, Peru, Argentina; Cavalry - Greenland, Alberta, Yakutsk, India;
 * Artillery - Northwest Territory, Western Europe.
 */
public class RiskCardSetTest {

    @Test
    public void threeOfOneSymbolIsASet() {
        assertTrue(RiskUtils.isSet(List.of(card(ALASKA), card(VENEZUELA), card(PERU))));         // 3 Infantry
        assertTrue(RiskUtils.isSet(List.of(card(GREENLAND), card(ALBERTA), card(INDIA))));       // 3 Cavalry
        assertTrue(RiskUtils.isSet(List.of(card(NORTHWEST_TERRITORY), card(WESTERN_EUROPE),
                card(INDONESIA))));                                                               // 3 Artillery
    }

    @Test
    public void oneOfEachSymbolIsASet() {
        assertTrue(RiskUtils.isSet(List.of(card(ALASKA), card(GREENLAND), card(NORTHWEST_TERRITORY))));
        assertTrue(RiskUtils.isSet(List.of(card(INDIA), card(PERU), card(WESTERN_EUROPE)))); // any order
    }

    @Test
    public void anyTwoWithAWildCardIsASet() {
        assertTrue(RiskUtils.isSet(List.of(card(ALASKA), card(PERU), wild())));            // 2 Infantry + wild
        assertTrue(RiskUtils.isSet(List.of(card(ALASKA), card(GREENLAND), wild())));       // 2 different + wild
        assertTrue(RiskUtils.isSet(List.of(wild(), card(GREENLAND), card(WESTERN_EUROPE))));
        assertTrue(RiskUtils.isSet(List.of(wild(), wild(), card(ALASKA))));                // 2 wilds + any 1
    }

    @Test
    public void theRulebookSampleSetsAreSets() {
        // pdf p.7: Yakutsk, Greenland, India (3 Cavalry); Argentina, Western Europe, India (1 of each);
        // a wild card with Western Europe and India
        assertTrue(RiskUtils.isSet(List.of(card(YAKUTSK), card(GREENLAND), card(INDIA))));
        assertTrue(RiskUtils.isSet(List.of(card(ARGENTINA), card(WESTERN_EUROPE), card(INDIA))));
        assertTrue(RiskUtils.isSet(List.of(wild(), card(WESTERN_EUROPE), card(INDIA))));
    }

    @Test
    public void twoOfOneSymbolAndOneOfAnotherIsNotASet() {
        assertFalse(RiskUtils.isSet(List.of(card(ALASKA), card(PERU), card(GREENLAND))));            // I I C
        assertFalse(RiskUtils.isSet(List.of(card(NORTHWEST_TERRITORY), card(WESTERN_EUROPE), card(INDIA)))); // A A C
        assertFalse(RiskUtils.isSet(List.of(card(YAKUTSK), card(INDIA), card(ARGENTINA))));          // C C I
    }

    @Test
    public void handWithNoSetHasNoSets() {
        assertEquals(List.of(), RiskUtils.sets(List.of(card(ALASKA), card(PERU))));
        assertEquals(List.of(), RiskUtils.sets(List.of(card(ALASKA), card(PERU), card(GREENLAND), card(ALBERTA))));
    }

    @Test
    public void setsListsEveryDistinctSetInCardOrder() {
        // Alaska (I, 0), Northwest Territory (A, 1), Greenland (C, 2), wild: one of each, and each pair + the wild
        List<RiskCard> hand = List.of(wild(), card(GREENLAND), card(ALASKA), card(NORTHWEST_TERRITORY));
        List<List<RiskCard>> sets = RiskUtils.sets(hand);
        Set<List<RiskCard>> expected = Set.of(
                List.of(card(ALASKA), card(NORTHWEST_TERRITORY), card(GREENLAND)),
                List.of(card(ALASKA), card(NORTHWEST_TERRITORY), wild()),
                List.of(card(ALASKA), card(GREENLAND), wild()),
                List.of(card(NORTHWEST_TERRITORY), card(GREENLAND), wild()));
        assertEquals(4, sets.size());
        assertEquals(expected, new HashSet<>(sets));
    }

    @Test
    public void setsUsingDifferentWildCardsAreTheSameSet() {
        // Alaska, Peru (both Infantry), 2 wilds: {Alaska, Peru, wild} once (not once per wild),
        // {Alaska, wild, wild}, {Peru, wild, wild}
        List<RiskCard> hand = List.of(card(ALASKA), wild(), card(PERU), wild());
        List<List<RiskCard>> sets = RiskUtils.sets(hand);
        Set<List<RiskCard>> expected = Set.of(
                List.of(card(ALASKA), card(PERU), wild()),
                List.of(card(ALASKA), wild(), wild()),
                List.of(card(PERU), wild(), wild()));
        assertEquals(3, sets.size());
        assertEquals(expected, new HashSet<>(sets));
    }

    @Test
    public void fiveCardsWithoutAWildGiveTheOneOfEachSets() {
        // Alaska, Peru (I), Greenland, Alberta (C), Northwest Territory (A): no 3 of a symbol;
        // one of each = 2 x 2 x 1 = 4 sets
        List<RiskCard> hand = List.of(card(PERU), card(ALBERTA), card(ALASKA), card(NORTHWEST_TERRITORY),
                card(GREENLAND));
        Set<List<RiskCard>> expected = Set.of(
                List.of(card(ALASKA), card(NORTHWEST_TERRITORY), card(GREENLAND)),
                List.of(card(ALASKA), card(NORTHWEST_TERRITORY), card(ALBERTA)),
                List.of(card(NORTHWEST_TERRITORY), card(GREENLAND), card(PERU)),
                List.of(card(NORTHWEST_TERRITORY), card(ALBERTA), card(PERU)));
        List<List<RiskCard>> sets = RiskUtils.sets(hand);
        assertEquals(4, sets.size());
        assertEquals(expected, new HashSet<>(sets));
    }

    @Test
    public void defaultTradeValuesForTheFirstEightSets() {
        // 4, 6, 8, 10, 12, 15 from the list; then 15 + 5 = 20 (pdf: the seventh), 20 + 5 = 25 (the eighth)
        RiskParameters params = new RiskParameters();
        int[] expected = {4, 6, 8, 10, 12, 15, 20, 25};
        for (int k = 1; k <= 8; k++)
            assertEquals("set " + k, expected[k - 1], params.tradeValue(k));
    }

    @Test
    public void tradeValuesListSetsTheFirstValues() {
        // "3,7" with increment 2: 3, 7, then 7 + 2 = 9, 9 + 2 = 11
        RiskParameters params = new RiskParameters();
        params.setParameterValue("tradeValues", "3,7");
        params.setParameterValue("tradeValueIncrement", 2);
        assertEquals(3, params.tradeValue(1));
        assertEquals(7, params.tradeValue(2));
        assertEquals(9, params.tradeValue(3));
        assertEquals(11, params.tradeValue(4));
    }

    @Test
    public void linearTradeValuesRiseByOneForEachSet() {
        // expert rule (pdf p.15): the k-th set is worth tradeValues[0] + (k - 1) = 4 + (k - 1): 4, 5, 6, ... 11
        RiskParameters params = new RiskParameters();
        params.setParameterValue("linearTradeValues", true);
        int[] expected = {4, 5, 6, 7, 8, 9, 10, 11};
        for (int k = 1; k <= 8; k++)
            assertEquals("set " + k, expected[k - 1], params.tradeValue(k));
    }

    @Test
    public void linearTradeValuesStartFromTheFirstListValue() {
        // "3,7" with linear values: 3 + (k - 1) = 3, 4, 5, 6 (the 7 and the increment 2 are not used)
        RiskParameters params = new RiskParameters();
        params.setParameterValue("linearTradeValues", true);
        params.setParameterValue("tradeValues", "3,7");
        params.setParameterValue("tradeValueIncrement", 2);
        assertEquals(3, params.tradeValue(1));
        assertEquals(4, params.tradeValue(2));
        assertEquals(5, params.tradeValue(3));
        assertEquals(6, params.tradeValue(4));
    }

    @Test
    public void tradeValueIncrementAppliesAfterTheList() {
        // default list, increment 3: sixth 15, seventh 15 + 3 = 18, eighth 21
        RiskParameters params = new RiskParameters();
        params.setParameterValue("tradeValueIncrement", 3);
        assertEquals(15, params.tradeValue(6));
        assertEquals(18, params.tradeValue(7));
        assertEquals(21, params.tradeValue(8));
    }
}
