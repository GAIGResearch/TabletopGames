package games.scopa;

import core.components.TarotCard;
import org.junit.Before;
import org.junit.Test;

import java.util.List;

import static core.components.TarotCard.*;
import static core.components.TarotCard.Suit.*;
import static games.scopa.ScopaTestUtils.*;
import static org.junit.Assert.*;

/**
 * The deal's score: ScopaUtils.primiera and coins, and getDealScore's categories (scopas, most cards, most Coins,
 * the 7 of Coins, the higher primiera), each judged on captured piles arranged directly. Cards not in a pile stay in
 * the hands, table or draw deck, so the 40 are conserved.
 * Primiera values: 7 = 21, 6 = 18, Ace = 16, 5 = 15, 4 = 14, 3 = 13, 2 = 12, courts = 10.
 */
public class ScopaScoringTest {

    ScopaForwardModel fm;
    ScopaGameState state;

    @Before
    public void setup() {
        fm = new ScopaForwardModel();
        state = newState(41, fm);
    }

    private void assertDealScores(int p0, int p1) {
        assertEquals("player 0 deal score", p0, state.getDealScore(0));
        assertEquals("player 1 deal score", p1, state.getDealScore(1));
        // nothing banked in the first deal: the game score is the deal score
        assertEquals("player 0 game score", p0, state.getGameScore(0), 0.0);
        assertEquals("player 1 game score", p1, state.getGameScore(1), 0.0);
        assertAllCardsPresent(state);
    }

    // ---- ScopaUtils.primiera and coins ----

    @Test
    public void thePrimieraOfTheFourSevensIsEightyFour() {
        assertEquals(84, ScopaUtils.primiera(List.of(sword(7), baton(7), cup(7), coin(7))));   // 4 * 21
    }

    @Test
    public void thePrimieraAddsTheBestCardOfEachSuit() {
        // 7 Coins 21 + 6 Cups 18 + Ace Swords 16 + King Batons 10 = 65
        assertEquals(65, ScopaUtils.primiera(List.of(coin(7), cup(6), sword(1), baton(KING))));
    }

    @Test
    public void onlyTheBestCardOfASuitCountsTowardsThePrimiera() {
        // Coins: 7 (21) beats 6 (18) and 5 (15); Swords Ace 16 beats King 10; Batons 2 12; Cups 3 13 beats Knave 10
        // 21 + 16 + 12 + 13 = 62
        assertEquals(62, ScopaUtils.primiera(List.of(coin(5), coin(7), coin(6), sword(KING), sword(1), baton(2),
                cup(KNAVE), cup(3))));
    }

    @Test
    public void aPileMissingASuitHasNoPrimiera() {
        // no Cups
        assertEquals(0, ScopaUtils.primiera(List.of(coin(7), sword(7), baton(7), coin(6), sword(6))));
        assertEquals(0, ScopaUtils.primiera(List.of()));
    }

    @Test
    public void coinsCountsTheCoinsInAPile() {
        assertEquals(3, ScopaUtils.coins(List.of(coin(1), sword(1), coin(7), cup(KING), coin(KING))));
        assertEquals(0, ScopaUtils.coins(List.of(sword(7), baton(7))));
        assertEquals(0, ScopaUtils.coins(List.of()));
    }

    // ---- getDealScore, one category at a time ----

    @Test
    public void emptyPilesScoreNothing() {
        // after the deal, before any capture: every category is tied at 0
        assertDealScores(0, 0);
    }

    @Test
    public void thePlayerWithMoreCardsScoresAPoint() {
        // cards 5 v 4 -> P1 1; no Coins either side; no 7 of Coins; no primiera (both lack suits)
        setPiles(state, suit(Batons, 1, 2, 3, 4), suit(Swords, 1, 2, 3, 4, 5));
        assertDealScores(0, 1);
    }

    @Test
    public void equalNumbersOfCardsScoreNothing() {
        // cards 4 v 4 tie; no Coins; no primiera
        setPiles(state, suit(Swords, 1, 2, 3, 4), suit(Batons, 1, 2, 3, 4));
        assertDealScores(0, 0);
    }

    @Test
    public void thePlayerWithMoreCoinsScoresAPoint() {
        // P0 Coins 1-6 (6 cards), P1 Coins Knave, Cavalier, King + Swords 1-3 (6 cards):
        // cards 6 v 6 tie; Coins 6 v 3 -> P0 1; the 7 of Coins is in nobody's pile; no primiera (both lack suits)
        setPiles(state, suit(Coins, 1, 2, 3, 4, 5, 6), cat(suit(Coins, KNAVE, CAVALIER, KING), suit(Swords, 1, 2, 3)));
        assertDealScores(1, 0);
    }

    @Test
    public void equalNumbersOfCoinsScoreNothing() {
        // cards 3 v 3; Coins 2 v 2; no 7 of Coins; no primiera
        setPiles(state, of(coin(1), coin(2), sword(1)), of(coin(3), coin(4), baton(1)));
        assertDealScores(0, 0);
    }

    @Test
    public void theSevenOfCoinsScoresAPointForPlayerOne() {
        // cards 1 v 1; Coins 1 v 1; the 7 of Coins -> P1 1; no primiera
        setPiles(state, of(coin(1)), of(coin(7)));
        assertDealScores(0, 1);
    }

    @Test
    public void theSevenOfCoinsScoresAPointForPlayerZero() {
        // the same piles swapped: the 7 of Coins -> P0 1
        setPiles(state, of(coin(7)), of(coin(1)));
        assertDealScores(1, 0);
    }

    @Test
    public void theHigherPrimieraScoresAPoint() {
        // P0 Swords 7, Batons 7, Cups 7, Coins 6: 21 + 21 + 21 + 18 = 81
        // P1 Swords 6, Batons 6, Cups 6, Coins 5: 18 + 18 + 18 + 15 = 69
        // cards 4 v 4; Coins 1 v 1; no 7 of Coins -> only primiera: P0 1
        setPiles(state, of(sword(7), baton(7), cup(7), coin(6)), of(sword(6), baton(6), cup(6), coin(5)));
        assertDealScores(1, 0);
    }

    @Test
    public void theHigherPrimieraScoresForPlayerOneAndCountsOnlyTheBestCardPerSuit() {
        // P0 Swords 7, Ace, 5, 4, Batons 2, Cups 2, Coins 2: best per suit 21 + 12 + 12 + 12 = 57
        // P1 Swords 6, Batons 6, Knave, Cavalier, Cups 6, Knave, Coins Ace: best per suit 18 + 18 + 18 + 16 = 70
        //    -> P1 1
        // (summing every card instead: P0 57 + 16 + 15 + 14 = 102, P1 70 + 10 + 10 + 10 = 100 -> P0)
        // cards 7 v 7 tie; Coins 1 v 1 tie; no 7 of Coins
        setPiles(state, of(sword(7), sword(1), sword(5), sword(4), baton(2), cup(2), coin(2)),
                of(sword(6), baton(6), cup(6), coin(1), baton(KNAVE), baton(CAVALIER), cup(KNAVE)));
        assertDealScores(0, 1);
    }

    @Test
    public void equalPrimieraScoresNothing() {
        // P0 Swords 7, Batons 6, Cups Ace, Coins 5: 21 + 18 + 16 + 15 = 70
        // P1 Swords 6, Batons 7, Cups 5, Coins Ace: 18 + 21 + 15 + 16 = 70 -> tie
        // cards 4 v 4; Coins 1 v 1; no 7 of Coins
        setPiles(state, of(sword(7), baton(6), cup(1), coin(5)), of(sword(6), baton(7), cup(5), coin(1)));
        assertDealScores(0, 0);
    }

    @Test
    public void neitherPileWithAllFourSuitsScoresNoPrimiera() {
        // P0 Swords 7, Batons 7, Cups 7 (no Coins): 0; P1 Swords 6, Batons 6, Cups 6 (no Coins): 0 -> tie at 0
        // cards 3 v 3; Coins 0 v 0
        setPiles(state, of(sword(7), baton(7), cup(7)), of(sword(6), baton(6), cup(6)));
        assertDealScores(0, 0);
    }

    @Test
    public void aPileMissingASuitLosesThePrimieraToAWeakerCompletePile() {
        // P0 Swords 2, Batons 2, Cups 2, Coins 2, 3: 12 + 12 + 12 + 13 = 49
        // P1 Swords 7, 6, Batons 7, Coins 6, 5 - no Cups: 0 (summing its best cards would give 21 + 21 + 18 = 60)
        // cards 5 v 5; Coins 2 v 2; no 7 of Coins -> primiera P0 1
        setPiles(state, of(sword(2), baton(2), cup(2), coin(2), coin(3)),
                of(sword(7), sword(6), baton(7), coin(6), coin(5)));
        assertDealScores(1, 0);
    }

    @Test
    public void scopasAddToTheCategoryPoints() {
        // cards 5 v 4 -> P0 1; nothing else; scopas 0 and 2 -> P0 0 + 1 = 1, P1 2 + 0 = 2
        setPiles(state, suit(Swords, 1, 2, 3, 4, 5), suit(Batons, 1, 2, 3, 4));
        state.scopas[1] = 2;
        assertDealScores(1, 2);
    }

    @Test
    public void theWorkedExampleScoresFourToThree() {
        // All 40 cards split between the piles.
        // P0 (22): Coins 7, Ace, 2, Knave, Cavalier, King (6); Swords Ace-4, 6; Batons Ace-4, 6; Cups Ace-6
        // P1 (18): Coins 3-6 (4); Swords 5, 7, Knave, Cavalier, King; Batons the same; Cups 7, Knave, Cavalier, King
        TarotCard[] p0 = cat(suit(Coins, 7, 1, 2, KNAVE, CAVALIER, KING), suit(Swords, 1, 2, 3, 4, 6),
                suit(Batons, 1, 2, 3, 4, 6), suit(Cups, 1, 2, 3, 4, 5, 6));
        TarotCard[] p1 = cat(suit(Coins, 3, 4, 5, 6), suit(Swords, 5, 7, KNAVE, CAVALIER, KING),
                suit(Batons, 5, 7, KNAVE, CAVALIER, KING), suit(Cups, 7, KNAVE, CAVALIER, KING));
        assertEquals(22, p0.length);
        assertEquals(18, p1.length);
        arrange(state, 0, of(), of());   // empty hands and table: all 40 go to the piles
        setPiles(state, p0, p1);
        assertEquals(0, state.getDrawDeck().getSize());
        state.scopas[0] = 1;
        state.scopas[1] = 2;
        // primiera: P0 Coins 7 21 + Swords 6 18 + Batons 6 18 + Cups 6 18 = 75
        //           P1 Coins 6 18 + Swords 7 21 + Batons 7 21 + Cups 7 21 = 81 -> P1
        // P0: scopa 1 + cards (22 > 18) 1 + Coins (6 > 4) 1 + 7 of Coins 1 = 4
        // P1: scopas 2 + primiera 1 = 3
        assertDealScores(4, 3);
    }

    @Test
    public void theGameScoreAddsTheBankedScoreToTheDealScore() {
        // cards 5 v 4 -> P0 1; banked 3 and 5 -> 3 + 1 = 4, 5 + 0 = 5
        setPiles(state, suit(Swords, 1, 2, 3, 4, 5), suit(Batons, 1, 2, 3, 4));
        state.bankedScores[0] = 3;
        state.bankedScores[1] = 5;
        assertEquals(1, state.getDealScore(0));
        assertEquals(0, state.getDealScore(1));
        assertEquals(4.0, state.getGameScore(0), 0.0);
        assertEquals(5.0, state.getGameScore(1), 0.0);
    }
}
