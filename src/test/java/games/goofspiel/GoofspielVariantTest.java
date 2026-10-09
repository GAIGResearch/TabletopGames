package games.goofspiel;

import core.actions.AbstractAction;
import games.goofspiel.actions.Bid;
import org.junit.Test;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static core.CoreConstants.GameResult.*;
import static core.components.FrenchCard.Suite.*;
import static games.goofspiel.GoofspielParameters.TieRule.*;
import static games.goofspiel.GoofspielTestUtils.*;
import static org.junit.Assert.*;

/**
 * The aceHigh and cardsPerSuit variants, and parameters surviving a copy. aceHigh: the Ace ranks above the King
 * as a bid and scores 14 as a prize. cardsPerSuit = n: hands and prize deck hold the n lowest-valued ranks
 * (A..n ace low, 2..n+1 ace high) and the game lasts n rounds.
 */
public class GoofspielVariantTest {

    GoofspielForwardModel fm = new GoofspielForwardModel();

    // ---- aceHigh ----

    @Test
    public void withAceHighTheAceBidBeatsTheKing() {
        GoofspielGameState state = newState(2, 51, params("aceHigh", true));
        arrangePrizes(state, "5D", "2D");
        // Ace 14 > King 13: player 0 wins
        playRound(fm, state, "AC", "KS");
        assertEquals(cards("5D"), multiset(state.getWonPrizes(0)));
        assertEquals(0, state.getWonPrizes(1).getSize());
    }

    @Test
    public void withAceLowTheKingBeatsTheAceBid() {
        GoofspielGameState state = newState(2, 51, params("aceHigh", false));
        arrangePrizes(state, "5D", "2D");
        // Ace 1 < King 13: player 1 wins
        playRound(fm, state, "AC", "KS");
        assertEquals(0, state.getWonPrizes(0).getSize());
        assertEquals(cards("5D"), multiset(state.getWonPrizes(1)));
    }

    @Test
    public void withAceHighAWonAceScoresFourteen() {
        GoofspielGameState state = newState(2, 52, params("aceHigh", true));
        arrangePrizes(state, "AD", "7D", "2D");
        playRound(fm, state, "KC", "QS");   // 13 > 12: player 0 wins the Ace
        playRound(fm, state, "JC", "10S");  // 11 > 10: player 0 wins the 7
        // 14 + 7 = 21
        assertEquals(21.0, state.getGameScore(0), 0.0);
    }

    @Test
    public void withAceLowAWonAceScoresOne() {
        GoofspielGameState state = newState(2, 52, params("aceHigh", false));
        arrangePrizes(state, "AD", "7D", "2D");
        playRound(fm, state, "KC", "QS");
        playRound(fm, state, "JC", "10S");
        // 1 + 7 = 8
        assertEquals(8.0, state.getGameScore(0), 0.0);
    }

    @Test
    public void withAceHighAndThirteenCardsTheSuitsRunTwoToAce() {
        GoofspielGameState state = newState(3, 53, params("aceHigh", true));
        assertEquals(multiset(suit(Clubs, 13, true)), multiset(state.getHand(0)));
        assertEquals(multiset(suit(Hearts, 13, true)), multiset(state.getHand(2)));
        // 12 face down + 1 on offer = the 13 Diamonds 2..A
        assertEquals(12, state.getPrizeDeck().getSize());
        assertEquals(1, state.getPrizesOnOffer().getSize());
        assertAllCardsPresent(state);
        GoofspielParameters params = (GoofspielParameters) state.getGameParameters();
        // values 2..14 in every hand, the Ace 14
        List<Integer> handValues = state.getHand(1).getComponents().stream().map(params::cardValue).sorted().toList();
        assertEquals(List.of(2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14), handValues);
    }

    // ---- cardsPerSuit ----

    @Test
    public void withFiveCardsPerSuitTheSuitsAreAceToFive() {
        GoofspielGameState state = newState(2, 54, params("cardsPerSuit", 5));
        assertEquals(cards("AC", "2C", "3C", "4C", "5C"), multiset(state.getHand(0)));
        assertEquals(cards("AS", "2S", "3S", "4S", "5S"), multiset(state.getHand(1)));
        // 5 Diamonds A..5: 4 face down + 1 on offer
        assertEquals(4, state.getPrizeDeck().getSize());
        assertEquals(1, state.getPrizesOnOffer().getSize());
        assertAllCardsPresent(state);
        // one Bid per card in hand
        Set<AbstractAction> expected = new HashSet<>();
        for (String r : List.of("A", "2", "3", "4", "5")) expected.add(new Bid(1, card(r + "S")));
        assertEquals(expected, new HashSet<>(fm.computeAvailableActions(state, null, 1)));
    }

    @Test
    public void withFiveCardsPerSuitAndAceHighTheSuitsAreTwoToSixWithNoAce() {
        GoofspielGameState state = newState(2, 55, params("cardsPerSuit", 5, "aceHigh", true));
        assertEquals(cards("2C", "3C", "4C", "5C", "6C"), multiset(state.getHand(0)));
        assertEquals(cards("2S", "3S", "4S", "5S", "6S"), multiset(state.getHand(1)));
        assertEquals(4, state.getPrizeDeck().getSize());
        assertEquals(1, state.getPrizesOnOffer().getSize());
        assertAllCardsPresent(state);
    }

    @Test
    public void withFiveCardsPerSuitTheGameEndsAfterFiveRounds() {
        GoofspielGameState state = newState(2, 56, params("cardsPerSuit", 5));
        arrangePrizes(state, "5D", "4D", "3D", "2D", "AD");
        // P0 bids A..5, P1 bids 2,3,4,5,A: P1 wins rounds 1-4 (prizes 5,4,3,2), P0 wins round 5 (5 > 1, the Ace)
        String[] p0 = {"A", "2", "3", "4", "5"};
        String[] p1 = {"2", "3", "4", "5", "A"};
        for (int r = 0; r < 5; r++) {
            assertTrue("the game ended after " + r + " rounds", state.isNotTerminal());
            playRound(fm, state, p0[r] + "C", p1[r] + "S");
        }
        assertFalse(state.isNotTerminal());
        assertEquals(0, state.getPrizeDeck().getSize());
        assertEquals(0, state.getPrizesOnOffer().getSize());
        assertEquals(5, state.getPlayedBids(0).getSize());
        // P0: 1; P1: 5 + 4 + 3 + 2 = 14
        assertEquals(1.0, state.getGameScore(0), 0.0);
        assertEquals(14.0, state.getGameScore(1), 0.0);
        assertEquals(LOSE_GAME, state.getPlayerResults()[0]);
        assertEquals(WIN_GAME, state.getPlayerResults()[1]);
        assertAllCardsPresent(state);
    }

    @Test
    public void withFiveCardsPerSuitAndAceHighTheSixIsTheTopBidAndPrize() {
        GoofspielGameState state = newState(2, 57, params("cardsPerSuit", 5, "aceHigh", true));
        arrangePrizes(state, "6D", "2D", "3D", "4D", "5D");
        // round 1: 6 > 5, P0 wins the 6 of Diamonds (P1 is left with 2, 3, 4, 6)
        // rounds 2-5: P0 2,3,4,5 vs P1 3,4,6,2: P1 wins 2 (3 > 2), 3 (4 > 3), 4 (6 > 4); P0 wins 5 (5 > 2)
        playRound(fm, state, "6C", "5S");
        playRound(fm, state, "2C", "3S");
        playRound(fm, state, "3C", "4S");
        playRound(fm, state, "4C", "6S");
        playRound(fm, state, "5C", "2S");
        assertFalse(state.isNotTerminal());
        assertEquals(cards("6D", "5D"), multiset(state.getWonPrizes(0)));
        assertEquals(cards("2D", "3D", "4D"), multiset(state.getWonPrizes(1)));
        // P0: 6 + 5 = 11; P1: 2 + 3 + 4 = 9
        assertEquals(11.0, state.getGameScore(0), 0.0);
        assertEquals(9.0, state.getGameScore(1), 0.0);
        assertEquals(WIN_GAME, state.getPlayerResults()[0]);
        assertAllCardsPresent(state);
    }

    // ---- parameters and copies ----

    @Test
    public void parametersSurviveACopyOfTheParametersAndOfTheState() {
        GoofspielParameters params = params("tieRule", DISCARD, "aceHigh", true, "cardsPerSuit", 7);
        GoofspielParameters copy = (GoofspielParameters) params.copy();
        assertEquals(DISCARD, copy.getParameterValue("tieRule"));
        assertEquals(true, copy.getParameterValue("aceHigh"));
        assertEquals(7, copy.getParameterValue("cardsPerSuit"));
        assertEquals(DISCARD, copy.tieRule);
        assertTrue(copy.aceHigh);
        assertEquals(7, copy.cardsPerSuit);

        // a state copy keeps the parameters, and plays by them: 7 cards 2..8, a tie discards
        GoofspielGameState state = newState(2, 58, params);
        GoofspielGameState stateCopy = (GoofspielGameState) state.copy();
        GoofspielParameters copied = (GoofspielParameters) stateCopy.getGameParameters();
        assertEquals(DISCARD, copied.getParameterValue("tieRule"));
        assertEquals(true, copied.getParameterValue("aceHigh"));
        assertEquals(7, copied.getParameterValue("cardsPerSuit"));
        assertEquals(cards("2C", "3C", "4C", "5C", "6C", "7C", "8C"), multiset(stateCopy.getHand(0)));
        arrangePrizes(stateCopy, "8D", "2D");
        playRound(fm, stateCopy, "8C", "8S");
        assertEquals(cards("8D"), multiset(stateCopy.getDiscardedPrizes()));
        assertEquals(cards("2D"), multiset(stateCopy.getPrizesOnOffer()));
    }
}
