package games.goofspiel;

import org.junit.Before;
import org.junit.Test;

import java.util.List;

import static games.goofspiel.GoofspielTestUtils.*;
import static org.junit.Assert.*;

/**
 * Resolving a round once every player has bid: the unique highest bid takes every prize on offer; a tie for
 * the highest bid carries the prizes over to the next round (default tie rule CARRY_OVER). Score = sum of the
 * values of the prizes won.
 */
public class GoofspielResolutionTest {

    GoofspielGameState state;
    GoofspielForwardModel fm;

    @Before
    public void setup() {
        state = newState(2, 5);
        fm = new GoofspielForwardModel();
    }

    @Test
    public void highestBidWinsThePrizeAndTheNextPrizeIsTurnedUp() {
        arrangePrizes(state, "7D", "QD", "3D");
        playRound(fm, state, "9C", "4S");

        // 9 > 4: player 0 takes the 7 of Diamonds
        assertEquals(cards("7D"), multiset(state.getWonPrizes(0)));
        assertEquals(0, state.getWonPrizes(1).getSize());
        // both bids are revealed, the bid decks are empty
        assertEquals(List.of(card("9C")), state.getPlayedBids(0).getComponents());
        assertEquals(List.of(card("4S")), state.getPlayedBids(1).getComponents());
        assertEquals(0, state.getBid(0).getSize());
        assertEquals(0, state.getBid(1).getSize());
        assertEquals(12, state.getHand(0).getSize());
        // the Queen comes up; 12 - 1 = 11 left face down
        assertEquals(cards("QD"), multiset(state.getPrizesOnOffer()));
        assertEquals(11, state.getPrizeDeck().getSize());
        // a new round, player 0 first, everyone to bid again
        assertEquals(1, state.getRoundCounter());
        assertEquals(0, state.getCurrentPlayer());
        assertEquals(List.of(0, 1), state.getCurrentSimultaneousPlayers());
        assertTrue(state.isNotTerminal());
        assertAllCardsPresent(state);

        // second round: 2 < J, so player 1 takes the Queen
        playRound(fm, state, "2C", "JS");
        assertEquals(cards("7D"), multiset(state.getWonPrizes(0)));
        assertEquals(cards("QD"), multiset(state.getWonPrizes(1)));
        // played bids: most recent on top
        assertEquals(List.of(card("2C"), card("9C")), state.getPlayedBids(0).getComponents());
        assertEquals(cards("3D"), multiset(state.getPrizesOnOffer()));
        assertEquals(10, state.getPrizeDeck().getSize());
        assertEquals(2, state.getRoundCounter());
    }

    @Test
    public void tiedBidsCarryThePrizeOverUntilSomeoneWinsThemAll() {
        arrangePrizes(state, "5D", "8D", "JD", "2D");
        playRound(fm, state, "6C", "6S");
        // tie: nobody wins; the 5 stays on offer with the 8
        assertEquals(0, state.getWonPrizes(0).getSize());
        assertEquals(0, state.getWonPrizes(1).getSize());
        assertEquals(cards("5D", "8D"), multiset(state.getPrizesOnOffer()));
        assertEquals(11, state.getPrizeDeck().getSize());
        assertEquals(List.of(card("6C")), state.getPlayedBids(0).getComponents());
        assertEquals(List.of(card("6S")), state.getPlayedBids(1).getComponents());
        assertEquals(1, state.getRoundCounter());
        assertEquals(List.of(0, 1), state.getCurrentSimultaneousPlayers());

        playRound(fm, state, "3C", "3S");
        // a second tie: three prizes on offer
        assertEquals(cards("5D", "8D", "JD"), multiset(state.getPrizesOnOffer()));
        assertEquals(10, state.getPrizeDeck().getSize());

        playRound(fm, state, "10C", "2S");
        // 10 > 2: player 0 takes all three, 5 + 8 + 11 = 24
        assertEquals(cards("5D", "8D", "JD"), multiset(state.getWonPrizes(0)));
        assertEquals(24.0, state.getGameScore(0), 0.0);
        assertEquals(0.0, state.getGameScore(1), 0.0);
        assertEquals(cards("2D"), multiset(state.getPrizesOnOffer()));
        assertEquals(9, state.getPrizeDeck().getSize());
        assertEquals(0, state.getDiscardedPrizes().getSize());
        assertAllCardsPresent(state);
    }

    @Test
    public void withThreePlayersATieForTheHighestBidCarriesOver() {
        GoofspielGameState three = newState(3, 5);
        arrangePrizes(three, "9D", "4D");
        // 5, 5, 3: players 0 and 1 tie for the highest bid
        playRound(fm, three, "5C", "5S", "3H");
        for (int p = 0; p < 3; p++)
            assertEquals(0, three.getWonPrizes(p).getSize());
        assertEquals(cards("9D", "4D"), multiset(three.getPrizesOnOffer()));

        // 3, 7, 7: players 1 and 2 tie for the highest; player 0's lower bid does not break the tie
        playRound(fm, three, "3C", "7S", "7H");
        for (int p = 0; p < 3; p++)
            assertEquals(0, three.getWonPrizes(p).getSize());
        assertEquals(3, three.getPrizesOnOffer().getSize());
        assertAllCardsPresent(three);
    }

    @Test
    public void withThreePlayersATieBelowTheHighestBidDoesNotMatter() {
        GoofspielGameState three = newState(3, 5);
        arrangePrizes(three, "9D", "4D");
        // 5, 3, 3: player 0 is the unique highest
        playRound(fm, three, "5C", "3S", "3H");
        assertEquals(cards("9D"), multiset(three.getWonPrizes(0)));
        assertEquals(cards("4D"), multiset(three.getPrizesOnOffer()));

        // 2, 2, K: player 2 is the unique highest
        playRound(fm, three, "2C", "2S", "KH");
        assertEquals(cards("4D"), multiset(three.getWonPrizes(2)));
        assertEquals(0, three.getWonPrizes(1).getSize());
        assertEquals(1, three.getPrizesOnOffer().getSize());
        assertAllCardsPresent(three);
    }

    @Test
    public void scoreIsTheSumOfTheValuesOfThePrizesWon() {
        // move K, A and 7 of Diamonds to player 0's won prizes, the Jack and Queen to player 1's
        arrangePrizes(state, "KD", "AD", "7D", "JD", "QD");
        for (int i = 0; i < 5; i++) {
            var prize = state.getPrizesOnOffer().draw();
            (i < 3 ? state.getWonPrizes(0) : state.getWonPrizes(1)).add(prize);
            state.getPrizesOnOffer().add(state.getPrizeDeck().draw());
        }
        // K 13 + A 1 + 7 = 21; J 11 + Q 12 = 23
        assertEquals(21.0, state.getGameScore(0), 0.0);
        assertEquals(23.0, state.getGameScore(1), 0.0);
        assertAllCardsPresent(state);
    }
}
