package games.crazyeights;

import core.AbstractForwardModel;
import core.CoreConstants.GameResult;
import core.Game;
import core.actions.AbstractAction;
import core.components.FrenchCard;
import games.crazyeights.actions.Pass;
import games.crazyeights.actions.PlayCard;
import org.junit.Test;

import java.util.List;
import java.util.Random;

import static core.CoreConstants.GameResult.*;
import static core.components.FrenchCard.Suite.Hearts;
import static games.crazyeights.CZETestUtils.*;
import static org.junit.Assert.*;

/**
 * Penalty points and ordinal positions at the end of the game.
 * <p>
 * After a player goes out, players are ranked by penalty points (lowest first, equal penalties share a position).
 * After a blocked game they are ranked by cards in hand (fewest first, equal counts share), penalties ignored.
 */
public class CZEScoringTest {

    private static CZEGameState newState(CZEParameters params, int nPlayers) {
        params.setRandomSeed(42);
        CZEGameState state = new CZEGameState(params, nPlayers);
        new CZEForwardModel().setup(state);
        return state;
    }

    private static int[] ordinals(CZEGameState state) {
        int[] result = new int[state.getNPlayers()];
        for (int p = 0; p < result.length; p++)
            result[p] = state.getOrdinalPosition(p);
        return result;
    }

    // ---------------------------------------------------------------- unit: penalties (F2)

    @Test
    public void penaltyIsFaceValueForSpotCardsTenForPicturesOneForAnAceAndFiftyForAnEight() {
        CZEGameState state = newState(new CZEParameters(), 3);

        giveHand(state, 0);
        assertEquals(0, state.handPenalty(0));
        giveHand(state, 0, card("2H"));
        assertEquals(2, state.handPenalty(0));
        giveHand(state, 0, card("9C"));
        assertEquals(9, state.handPenalty(0));
        giveHand(state, 0, card("10D"));
        assertEquals(10, state.handPenalty(0));
        giveHand(state, 0, card("JC"));
        assertEquals(10, state.handPenalty(0));
        giveHand(state, 0, card("QS"));
        assertEquals(10, state.handPenalty(0));
        giveHand(state, 0, card("KH"));
        assertEquals(10, state.handPenalty(0));
        giveHand(state, 0, card("AS"));
        assertEquals(1, state.handPenalty(0));    // not 14
        giveHand(state, 0, card("8D"));
        assertEquals(50, state.handPenalty(0));   // not 8
        // mixed hand: 1 + 50 + 10 + 3 + 10
        giveHand(state, 0, card("AS"), card("8D"), card("10D"), card("3C"), card("QH"));
        assertEquals(74, state.handPenalty(0));
        // each player's own hand only: player 1 holds 7 + 4
        giveHand(state, 1, card("7S"), card("4H"));
        assertEquals(11, state.handPenalty(1));
    }

    @Test
    public void penaltiesForEightsAndAcesAreReadFromTheParameters() {
        CZEParameters params = new CZEParameters();
        CZEGameState state = newState(params, 3);
        giveHand(state, 0, card("8D"), card("AS"), card("5C"));
        assertEquals(50 + 1 + 5, state.handPenalty(0));

        params.setParameterValue("eightPenalty", 20);
        assertEquals(20 + 1 + 5, state.handPenalty(0));

        params.setParameterValue("acePenalty", 15);
        assertEquals(20 + 15 + 5, state.handPenalty(0));
    }

    // ---------------------------------------------------------------- unit: ordinal positions

    @Test
    public void afterGoingOutPlayersAreRankedByLowestPenaltyWithEqualPenaltiesSharingAPosition() {
        CZEGameState state = newState(new CZEParameters(), 4);
        CZEForwardModel fm = new CZEForwardModel();
        setTopDiscard(state, card("5H"));
        giveHand(state, 0, card("9H"));                            // goes out
        giveHand(state, 1, card("8D"));                            // 1 card, 50
        giveHand(state, 2, card("2C"), card("3S"));                // 2 cards, 5
        giveHand(state, 3, card("2D"), card("2S"), card("AC"));    // 3 cards, 5 (Ace = 1)
        assertEquals(0, state.getCurrentPlayer());

        fm.next(state, new PlayCard(card("9H"), Hearts));

        assertFalse(state.isNotTerminal());
        // by penalty, not by cards in hand: P0 (0) 1st, P2 and P3 (5) share 2nd, P1 (50) 4th
        assertArrayEquals(new int[]{1, 4, 2, 2}, ordinals(state));
        assertArrayEquals(new GameResult[]{WIN_GAME, LOSE_GAME, LOSE_GAME, LOSE_GAME}, state.getPlayerResults());
    }

    @Test
    public void afterABlockedGamePlayersAreRankedByCardsInHandIgnoringPenalty() {
        CZEGameState state = newState(new CZEParameters(), 4);
        CZEForwardModel fm = new CZEForwardModel();
        setTopDiscard(state, card("5H"));
        giveHand(state, 0, card("2C"), card("3S"));   // 2 cards, 5
        giveHand(state, 1, card("KD"));               // 1 card, 10
        giveHand(state, 2, card("QS"), card("JC"));   // 2 cards, 20
        leaveNothingToDraw(state, 3);                 // every other card
        assertEquals(List.of(new Pass()), fm.computeAvailableActions(state));
        state.setConsecutivePasses(3);                // the other three players have passed (see CZEBlockedGameTest)

        fm.next(state, new Pass());

        assertFalse(state.isNotTerminal());
        assertArrayEquals(new GameResult[]{LOSE_GAME, WIN_GAME, LOSE_GAME, LOSE_GAME}, state.getPlayerResults());
        // P1 (1 card) 1st, P0 and P2 (2 cards) share 2nd although P0 has fewer penalty points, P3 4th
        assertArrayEquals(new int[]{2, 1, 2, 4}, ordinals(state));
    }

    // ---------------------------------------------------------------- integration

    @Test
    public void blockedGameWithTwoRealPassesRanksEveryPlayerOnFewestCardsFirst() {
        Game game = newGame(3, 11);
        CZEGameState state = (CZEGameState) game.getGameState();
        AbstractForwardModel fm = game.getForwardModel();
        setTopDiscard(state, card("5H"));
        giveHand(state, 0, card("2C"), card("3S"));   // 2 cards, 5
        giveHand(state, 1, card("KD"), card("QS"));   // 2 cards, 20
        leaveNothingToDraw(state, 2);                 // every other card, including all the Eights
        state.setConsecutivePasses(1);                // player 2 passed last (cannot really happen, see CZEBlockedGameTest)

        assertEquals(List.of(new Pass()), fm.computeAvailableActions(state));
        fm.next(state, new Pass());
        assertTrue(state.isNotTerminal());
        assertEquals(List.of(new Pass()), fm.computeAvailableActions(state));
        fm.next(state, new Pass());

        assertFalse(state.isNotTerminal());
        assertArrayEquals(new GameResult[]{WIN_GAME, WIN_GAME, LOSE_GAME}, state.getPlayerResults());
        // tied on fewest cards, so both 1st, whatever their penalties
        assertArrayEquals(new int[]{1, 1, 3}, ordinals(state));
    }

    /**
     * Penalty points written out independently of CZEGameState.handPenalty.
     */
    private static int penalty(List<FrenchCard> hand) {
        int total = 0;
        for (FrenchCard c : hand) {
            total += switch (c.type) {
                case Ace -> 1;
                case Jack, Queen, King -> 10;
                case Number -> c.number == 8 ? 50 : c.number;
            };
        }
        return total;
    }

    @Test
    public void randomGamesRankTheWinnerFirstAndOthersByLowestPenalty() {
        int checkedLosers = 0;
        for (int nPlayers : new int[]{2, 3, 5}) {
            for (long seed = 1; seed <= 3; seed++) {
                String label = nPlayers + " players, seed " + seed;
                Game game = newGame(nPlayers, seed);
                CZEGameState state = (CZEGameState) game.getGameState();
                AbstractForwardModel fm = game.getForwardModel();
                Random rnd = new Random(seed);

                int steps = 0;
                while (state.isNotTerminal() && steps++ < 5000) {
                    List<AbstractAction> actions = fm.computeAvailableActions(state);
                    fm.next(state, actions.get(rnd.nextInt(actions.size())));
                }
                assertFalse(label + ": did not end within 5000 actions", state.isNotTerminal());
                // a blocked game cannot arise from real play with one deck, so someone went out
                assertTrue(label + ": expected a normal win", state.getConsecutivePasses() < nPlayers);

                int[] penalties = new int[nPlayers];
                for (int p = 0; p < nPlayers; p++)
                    penalties[p] = penalty(state.getPlayerHands().get(p).getComponents());
                for (int p = 0; p < nPlayers; p++) {
                    int expected = 1;
                    for (int q = 0; q < nPlayers; q++)
                        if (penalties[q] < penalties[p]) expected++;
                    assertEquals(label + ", ordinal of player " + p, expected, state.getOrdinalPosition(p));
                    GameResult result = state.getPlayerResults()[p];
                    assertEquals(label + ", result of player " + p, expected == 1 ? WIN_GAME : LOSE_GAME, result);
                    if (result == LOSE_GAME) checkedLosers++;
                }
            }
        }
        assertTrue(checkedLosers > 0);
    }
}
