package games.scopa;

import core.actions.AbstractAction;
import core.components.TarotCard;
import org.junit.Before;
import org.junit.Test;

import java.util.List;
import java.util.Set;

import static core.components.TarotCard.*;
import static games.scopa.ScopaTestUtils.*;
import static org.junit.Assert.*;

/**
 * Which captures a card can make (ScopaUtils.captures) and the legal actions built from them.
 * Capture values: Ace-7 face, Knave 8, Cavalier 9, King 10.
 */
public class ScopaCaptureRulesTest {

    ScopaForwardModel fm;
    ScopaGameState state;

    // values 3, 7, 4, 6, 2, 8
    static final List<TarotCard> EXAMPLE_TABLE = List.of(cup(3), sword(7), baton(4), coin(6), cup(2), sword(KNAVE));

    @Before
    public void setup() {
        fm = new ScopaForwardModel();
        state = newState(5, fm);
    }

    // ---- ScopaUtils.captures ----

    @Test
    public void aKingCapturesEachPairOfTheExampleTableSummingToTen() {
        // pairs summing to 10 from {3, 7, 4, 6, 2, 8}: 3+7, 4+6, 2+8 (and no other: 3+6=9, 7+2=9, 4+8=12 ...)
        // triples: the smallest is 2+3+4 = 9, next 2+3+6 = 11, so none is 10; 4+ cards are at least 2+3+4+6 = 15
        assertEquals(sets(of(cup(3), sword(7)), of(baton(4), coin(6)), of(cup(2), sword(KNAVE))),
                asSets(ScopaUtils.captures(cup(KING), EXAMPLE_TABLE)));
    }

    @Test
    public void aCavalierCapturesPairsAndATripleOfTheExampleTable() {
        // 9 from {3, 7, 4, 6, 2, 8}: pairs 3+6, 7+2 (4+5, 8+1 impossible); triples: only 2+3+4 = 9 (every other
        // triple is larger); 4+ cards at least 15
        assertEquals(sets(of(cup(3), coin(6)), of(sword(7), cup(2)), of(cup(2), cup(3), baton(4))),
                asSets(ScopaUtils.captures(baton(CAVALIER), EXAMPLE_TABLE)));
    }

    @Test
    public void aCaptureMayTakeThreeCards() {
        // 7 from {1, 2, 4, 10}: pairs 1+2=3, 1+4=5, 2+4=6, any with 10 > 7; triple 1+2+4 = 7
        assertEquals(sets(of(sword(1), cup(2), baton(4))),
                asSets(ScopaUtils.captures(coin(7), List.of(sword(1), cup(2), baton(4), coin(KING)))));
    }

    @Test
    public void courtCardsCaptureByTheirValue() {
        // Knave 8 from {5, 3, 2, 6}: 5+3, 2+6; triples at least 2+3+5 = 10
        assertEquals(sets(of(sword(5), coin(3)), of(baton(2), cup(6))),
                asSets(ScopaUtils.captures(cup(KNAVE), List.of(sword(5), coin(3), baton(2), cup(6)))));
        // King 10 from {9, 1, 4}: 9+1 = 10; 1+4 = 5, 9+4 = 13, 9+1+4 = 14
        assertEquals(sets(of(cup(CAVALIER), coin(1))),
                asSets(ScopaUtils.captures(sword(KING), List.of(cup(CAVALIER), coin(1), baton(4)))));
        // a court card on the table counts its value in a sum: Cavalier 9 from {8, 1, 5}: 8+1; 8+5 = 13, 1+5 = 6,
        // 8+1+5 = 14
        assertEquals(sets(of(sword(KNAVE), baton(1))),
                asSets(ScopaUtils.captures(coin(CAVALIER), List.of(sword(KNAVE), baton(1), cup(5)))));
    }

    @Test
    public void cardsOfEqualValueGiveSeparateCaptures() {
        // 6 from {3, 3, 2, 4}: 3+3 and 2+4; triples at least 2+3+3 = 8
        assertEquals(sets(of(sword(3), coin(3)), of(baton(2), cup(4))),
                asSets(ScopaUtils.captures(cup(6), List.of(sword(3), coin(3), baton(2), cup(4)))));
        // 6 from three 3s: each of the three pairs is a separate capture (3+3+3 = 9 is too much)
        assertEquals(sets(of(sword(3), coin(3)), of(sword(3), cup(3)), of(coin(3), cup(3))),
                asSets(ScopaUtils.captures(baton(6), List.of(sword(3), coin(3), cup(3)))));
    }

    @Test
    public void aSameRankCardTakesPrecedenceOverEverySum() {
        // 7 of Cups: the 7 of Swords matches; 3+4 = 7 is a sum but is not offered
        assertEquals(sets(of(sword(7))),
                asSets(ScopaUtils.captures(cup(7), List.of(sword(7), baton(3), coin(4)))));
        // the same for a court card: Knave takes the Knave, not 5+3
        assertEquals(sets(of(sword(KNAVE))),
                asSets(ScopaUtils.captures(coin(KNAVE), List.of(sword(KNAVE), cup(5), baton(3)))));
    }

    @Test
    public void twoSameRankCardsAreTwoSingleCaptures() {
        // 4 of Swords with the 4 of Cups and 4 of Coins: capture one or the other, never both; Ace+3 not offered
        assertEquals(sets(of(cup(4)), of(coin(4))),
                asSets(ScopaUtils.captures(sword(4), List.of(cup(4), coin(4), sword(1), baton(3)))));
    }

    @Test
    public void aCardWithNoMatchOrSumCapturesNothing() {
        // Ace (1): no Ace on the table, and no two cards sum to 1
        assertTrue(ScopaUtils.captures(coin(1), EXAMPLE_TABLE).isEmpty());
        // 5 with {7, 9, 10}: every pair is over 5
        assertTrue(ScopaUtils.captures(coin(5), List.of(sword(7), cup(CAVALIER), baton(KING))).isEmpty());
        assertTrue(ScopaUtils.captures(coin(KING), List.of()).isEmpty());
    }

    // ---- legal actions ----

    @Test
    public void eachCaptureIsOneActionAndOnlyANonCapturingCardGoesToTheTable() {
        arrange(state, 0, of(cup(KING), coin(1), baton(CAVALIER)), of(sword(5), sword(6), baton(2)),
                EXAMPLE_TABLE.toArray(new TarotCard[0]));
        // King: 3+7, 4+6, 2+Knave; Ace: no capture, to the table; Cavalier: 3+6, 7+2, 2+3+4. No King or Cavalier
        // to the table (capture is compulsory). 3 + 1 + 3 = 7 actions.
        Set<AbstractAction> expected = Set.of(
                capture(cup(KING), cup(3), sword(7)),
                capture(cup(KING), baton(4), coin(6)),
                capture(cup(KING), cup(2), sword(KNAVE)),
                toTable(coin(1)),
                capture(baton(CAVALIER), cup(3), coin(6)),
                capture(baton(CAVALIER), sword(7), cup(2)),
                capture(baton(CAVALIER), cup(2), cup(3), baton(4)));
        assertEquals(expected, actionSet(fm, state));
        assertEquals(7, fm.computeAvailableActions(state).size());
    }

    @Test
    public void actionsFollowRankPrecedenceAndOfferEachSameRankCard() {
        arrange(state, 0, of(cup(7), sword(4), coin(5)), of(sword(5), sword(6), baton(2)),
                sword(7), baton(3), cup(4), coin(4));
        // 7 of Cups: takes the 7 of Swords only (3 + 4 = 7 twice, not offered)
        // 4 of Swords: the 4 of Cups or the 4 of Coins
        // 5 of Coins: no 5; sums from {7, 3, 4, 4}: 3+4 = 7, 4+4 = 8 ... none is 5 -> to the table
        assertEquals(Set.of(
                        capture(cup(7), sword(7)),
                        capture(sword(4), cup(4)),
                        capture(sword(4), coin(4)),
                        toTable(coin(5))),
                actionSet(fm, state));
    }

    @Test
    public void withAnEmptyTableEveryCardGoesToTheTable() {
        arrange(state, 1, of(cup(7), sword(4), coin(5)), of(sword(5), sword(6), baton(2)));
        // player 1 to act: their three cards, each to the table
        assertEquals(Set.of(toTable(sword(5)), toTable(sword(6)), toTable(baton(2))), actionSet(fm, state));
    }
}
