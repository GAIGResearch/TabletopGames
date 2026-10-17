package games.goofspiel;

import games.goofspiel.GoofspielParameters.TieRule;
import org.junit.Test;

import java.util.List;

import static games.goofspiel.GoofspielParameters.TieRule.*;
import static games.goofspiel.GoofspielTestUtils.*;
import static org.junit.Assert.*;

/**
 * The variant tie rules. DISCARD: a tie for the highest bid discards every prize on offer. HIGHEST_UNIQUE: bids
 * made by more than one player are disqualified, the highest remaining bid wins, and with no unique bid the prizes
 * are discarded. Neither rule ever carries prizes over. (CARRY_OVER is in GoofspielResolutionTest.)
 */
public class GoofspielTieRuleTest {

    GoofspielForwardModel fm = new GoofspielForwardModel();

    private GoofspielGameState stateWith(TieRule tieRule, int nPlayers, long seed) {
        return newState(nPlayers, seed, params("tieRule", tieRule));
    }

    // ---- bidWinner, the pure decision ----

    @Test
    public void highestUniqueDisqualifiesTiedBidsAndTheHighestRemainingBidWins() {
        // 7, 7, 4: the 7s are disqualified, the 4 is the only unique bid -> player 2
        assertEquals(2, GoofspielForwardModel.bidWinner(new int[]{7, 7, 4}, HIGHEST_UNIQUE));
        // 9, 7, 7: the 9 is unique and highest -> player 0
        assertEquals(0, GoofspielForwardModel.bidWinner(new int[]{9, 7, 7}, HIGHEST_UNIQUE));
        // 2, 6, 6, 5, 3: unique bids are 2, 5, 3; the highest of them, 5, is player 3's
        assertEquals(3, GoofspielForwardModel.bidWinner(new int[]{2, 6, 6, 5, 3}, HIGHEST_UNIQUE));
        // 4, 9, 7, 7: unique bids are 4 and 9 -> the 9, player 1 (not the lowest unique bid)
        assertEquals(1, GoofspielForwardModel.bidWinner(new int[]{4, 9, 7, 7}, HIGHEST_UNIQUE));
    }

    @Test
    public void highestUniqueHasNoWinnerWhenEveryBidIsMatched() {
        // 7, 7, 4, 4: both values made twice -> nobody
        assertEquals(-1, GoofspielForwardModel.bidWinner(new int[]{7, 7, 4, 4}, HIGHEST_UNIQUE));
        // 2 players, 5 = 5 -> nobody
        assertEquals(-1, GoofspielForwardModel.bidWinner(new int[]{5, 5}, HIGHEST_UNIQUE));
        // 8, 8, 8: three the same -> nobody
        assertEquals(-1, GoofspielForwardModel.bidWinner(new int[]{8, 8, 8}, HIGHEST_UNIQUE));
    }

    @Test
    public void discardHasTheSameWinnerAsCarryOver() {
        // the unique highest bid wins; a tie at the top means nobody, even with a unique bid below it
        assertEquals(0, GoofspielForwardModel.bidWinner(new int[]{9, 4}, DISCARD));
        assertEquals(-1, GoofspielForwardModel.bidWinner(new int[]{5, 5}, DISCARD));
        assertEquals(-1, GoofspielForwardModel.bidWinner(new int[]{7, 7, 4}, DISCARD));
        assertEquals(0, GoofspielForwardModel.bidWinner(new int[]{5, 3, 3}, DISCARD));
    }

    // ---- DISCARD through whole rounds ----

    @Test
    public void underDiscardATieDiscardsThePrizeAndTheNextRoundOffersOnlyTheNextPrize() {
        GoofspielGameState state = stateWith(DISCARD, 2, 41);
        arrangePrizes(state, "7D", "QD", "3D");
        playRound(fm, state, "5C", "5S");

        // 5 = 5: the 7 of Diamonds is discarded, nobody wins it
        assertEquals(cards("7D"), multiset(state.getDiscardedPrizes()));
        assertEquals(0, state.getWonPrizes(0).getSize());
        assertEquals(0, state.getWonPrizes(1).getSize());
        // only the Queen on offer; 12 - 1 = 11 left face down
        assertEquals(cards("QD"), multiset(state.getPrizesOnOffer()));
        assertEquals(11, state.getPrizeDeck().getSize());
        assertEquals(1, state.getRoundCounter());
        assertEquals(List.of(0, 1), state.getCurrentSimultaneousPlayers());
        assertAllCardsPresent(state);

        // 9 > 4: player 0 wins the Queen alone and scores 12 (not 7 + 12)
        playRound(fm, state, "9C", "4S");
        assertEquals(cards("QD"), multiset(state.getWonPrizes(0)));
        assertEquals(12.0, state.getGameScore(0), 0.0);
        assertEquals(cards("7D"), multiset(state.getDiscardedPrizes()));
        assertEquals(cards("3D"), multiset(state.getPrizesOnOffer()));
        assertAllCardsPresent(state);
    }

    @Test
    public void underDiscardTheUniqueHighestBidWinsAndATieBelowItDoesNotMatter() {
        GoofspielGameState state = stateWith(DISCARD, 3, 42);
        arrangePrizes(state, "10D", "2D");
        // 5 > 3 = 3: player 0 wins the 10
        playRound(fm, state, "5C", "3S", "3H");
        assertEquals(cards("10D"), multiset(state.getWonPrizes(0)));
        assertEquals(0, state.getDiscardedPrizes().getSize());
        assertEquals(cards("2D"), multiset(state.getPrizesOnOffer()));
    }

    @Test
    public void underDiscardATieForTheTopWithAUniqueBidBelowDiscardsThePrize() {
        GoofspielGameState state = stateWith(DISCARD, 3, 43);
        arrangePrizes(state, "10D", "2D");
        // 7 = 7 > 4: tie at the top, so the 10 is discarded (the 4 does not win under DISCARD)
        playRound(fm, state, "7C", "7S", "4H");
        assertEquals(cards("10D"), multiset(state.getDiscardedPrizes()));
        for (int p = 0; p < 3; p++) assertEquals(0, state.getWonPrizes(p).getSize());
        assertEquals(cards("2D"), multiset(state.getPrizesOnOffer()));
    }

    // ---- HIGHEST_UNIQUE through whole rounds ----

    @Test
    public void underHighestUniqueTheOnlyUniqueBidWinsBelowATiedTop() {
        GoofspielGameState state = stateWith(HIGHEST_UNIQUE, 3, 44);
        arrangePrizes(state, "10D", "2D");
        // 7, 7, 4: the 7s are disqualified; player 2 wins the 10 with the 4
        playRound(fm, state, "7C", "7S", "4H");
        assertEquals(cards("10D"), multiset(state.getWonPrizes(2)));
        assertEquals(0, state.getWonPrizes(0).getSize());
        assertEquals(0, state.getWonPrizes(1).getSize());
        assertEquals(0, state.getDiscardedPrizes().getSize());
        assertEquals(List.of(card("4H")), state.getPlayedBids(2).getComponents());
        assertEquals(cards("2D"), multiset(state.getPrizesOnOffer()));
        assertEquals(10.0, state.getGameScore(2), 0.0);
        assertAllCardsPresent(state);
    }

    @Test
    public void underHighestUniqueAUniqueTopBidWinsAsUsual() {
        GoofspielGameState state = stateWith(HIGHEST_UNIQUE, 3, 45);
        arrangePrizes(state, "10D", "2D");
        // 9, 7, 7: the 9 is unique -> player 0
        playRound(fm, state, "9C", "7S", "7H");
        assertEquals(cards("10D"), multiset(state.getWonPrizes(0)));
        assertEquals(0, state.getDiscardedPrizes().getSize());
    }

    @Test
    public void underHighestUniqueWithNoUniqueBidThePrizeIsDiscardedNotCarried() {
        GoofspielGameState state = stateWith(HIGHEST_UNIQUE, 4, 46);
        arrangePrizes(state, "10D", "JD", "2D");
        // 7, 7, 4, 4 (player 3 holds Clubs again): no unique bid, the 10 is discarded
        playRound(fm, state, "7C", "7S", "4H", "4C");
        assertEquals(cards("10D"), multiset(state.getDiscardedPrizes()));
        for (int p = 0; p < 4; p++) assertEquals(0, state.getWonPrizes(p).getSize());
        // only the Jack on offer; 12 - 1 = 11 face down
        assertEquals(cards("JD"), multiset(state.getPrizesOnOffer()));
        assertEquals(11, state.getPrizeDeck().getSize());
        assertAllCardsPresent(state);

        // next round 9, 8, 3, 2: player 0 wins the Jack alone (11, not 10 + 11)
        playRound(fm, state, "9C", "8S", "3H", "2C");
        assertEquals(cards("JD"), multiset(state.getWonPrizes(0)));
        assertEquals(11.0, state.getGameScore(0), 0.0);
        assertEquals(cards("2D"), multiset(state.getPrizesOnOffer()));
    }

    @Test
    public void underHighestUniqueATwoPlayerTieDiscardsThePrize() {
        GoofspielGameState state = stateWith(HIGHEST_UNIQUE, 2, 47);
        arrangePrizes(state, "KD", "3D");
        playRound(fm, state, "6C", "6S");
        // 6 = 6: no unique bid, the King goes to nobody
        assertEquals(cards("KD"), multiset(state.getDiscardedPrizes()));
        assertEquals(cards("3D"), multiset(state.getPrizesOnOffer()));
        assertEquals(0, state.getWonPrizes(0).getSize());
        assertEquals(0, state.getWonPrizes(1).getSize());
    }

    @Test
    public void underHighestUniqueAUniqueLowerBidStillWinsInTheLastRound() {
        // 5 cards per suit, 3 players; prizes come up A, 2, 3, 4, 5
        GoofspielGameState state = newState(3, 48, params("tieRule", HIGHEST_UNIQUE, "cardsPerSuit", 5));
        arrangePrizes(state, "AD", "2D", "3D", "4D", "5D");
        // rounds 1-4: player 0 bids 2..5 against 1..4 from both others -> player 0's bid is unique and highest
        // (in round 1 the others' Aces tie with each other, which only disqualifies them)
        playRound(fm, state, "2C", "AS", "AH");
        playRound(fm, state, "3C", "2S", "2H");
        playRound(fm, state, "4C", "3S", "3H");
        playRound(fm, state, "5C", "4S", "4H");
        assertEquals(cards("AD", "2D", "3D", "4D"), multiset(state.getWonPrizes(0)));
        // last round: player 0 has the Ace left, the others the 5s: 5 = 5 are out, the Ace (1) is unique
        playRound(fm, state, "AC", "5S", "5H");
        assertFalse(state.isNotTerminal());
        assertEquals(cards("AD", "2D", "3D", "4D", "5D"), multiset(state.getWonPrizes(0)));
        assertEquals(0, state.getDiscardedPrizes().getSize());
        // 1 + 2 + 3 + 4 + 5 = 15
        assertEquals(15.0, state.getGameScore(0), 0.0);
        assertAllCardsPresent(state);
    }

    @Test
    public void underDiscardATieInTheLastRoundDiscardsAndTheGameEndsAfterFiveRounds() {
        GoofspielGameState state = newState(2, 49, params("tieRule", DISCARD, "cardsPerSuit", 5));
        arrangePrizes(state, "AD", "2D", "3D", "4D", "5D");
        // rounds 1-4: 1 = 1 (discard A), 3 > 2 (P0 wins 2), 2 < 4 (P1 wins 3), 4 > 3 (P0 wins 4)
        playRound(fm, state, "AC", "AS");
        playRound(fm, state, "3C", "2S");
        playRound(fm, state, "2C", "4S");
        playRound(fm, state, "4C", "3S");
        assertTrue(state.isNotTerminal());
        assertEquals(4, state.getRoundCounter());
        assertEquals(cards("5D"), multiset(state.getPrizesOnOffer()));
        assertEquals(cards("AD"), multiset(state.getDiscardedPrizes()));
        // round 5: both have only the 5 left: 5 = 5, the 5 of Diamonds is discarded and the game ends
        playRound(fm, state, "5C", "5S");
        assertFalse(state.isNotTerminal());
        assertEquals(cards("AD", "5D"), multiset(state.getDiscardedPrizes()));
        assertEquals(0, state.getPrizesOnOffer().getSize());
        // P0: 2 + 4 = 6; P1: 3
        assertEquals(6.0, state.getGameScore(0), 0.0);
        assertEquals(3.0, state.getGameScore(1), 0.0);
        assertEquals(core.CoreConstants.GameResult.WIN_GAME, state.getPlayerResults()[0]);
        assertAllCardsPresent(state);
    }
}
