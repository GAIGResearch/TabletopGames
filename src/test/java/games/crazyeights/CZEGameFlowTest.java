package games.crazyeights;

import core.AbstractForwardModel;
import core.Game;
import core.CoreConstants.GameResult;
import core.actions.AbstractAction;
import core.components.Deck;
import games.crazyeights.actions.DrawCard;
import games.crazyeights.actions.Pass;
import games.crazyeights.actions.PlayCard;
import org.junit.Test;

import java.util.List;
import java.util.Random;

import static core.CoreConstants.GameResult.LOSE_GAME;
import static core.CoreConstants.GameResult.WIN_GAME;
import static core.components.FrenchCard.Suite.*;
import static games.crazyeights.CZETestUtils.*;
import static org.junit.Assert.*;

public class CZEGameFlowTest {

    @Test
    public void scriptedHandFollowsTheMatchingRulesToAWin() {
        Game game = newGame(3, 1);
        CZEGameState state = (CZEGameState) game.getGameState();
        AbstractForwardModel fm = game.getForwardModel();
        setTopDiscard(state, card("5H"));
        giveHand(state, 0, card("9H"), card("3S"));
        giveHand(state, 1, card("KS"), card("4D"));
        giveHand(state, 2, card("8C"), card("7D"), card("JD"));

        // P0: only the Nine of Hearts matches the Five of Hearts
        assertEquals(0, state.getCurrentPlayer());
        assertEquals(List.of(new PlayCard(card("9H"), Hearts)), fm.computeAvailableActions(state));
        fm.next(state, new PlayCard(card("9H"), Hearts));

        // P1: neither a Heart nor a Nine, so must draw
        assertEquals(1, state.getCurrentPlayer());
        assertEquals(List.of(new DrawCard()), fm.computeAvailableActions(state));
        fm.next(state, new DrawCard());
        assertEquals(3, state.getPlayerHands().get(1).getSize());

        // P2: plays the Eight and nominates Spades
        assertEquals(2, state.getCurrentPlayer());
        fm.next(state, new PlayCard(card("8C"), Spades));
        assertEquals(Spades, state.getCurrentSuit());

        // P0: the Three of Spades is their last card
        assertEquals(0, state.getCurrentPlayer());
        assertEquals(List.of(new PlayCard(card("3S"), Spades)), fm.computeAvailableActions(state));
        fm.next(state, new PlayCard(card("3S"), Spades));

        assertFalse(state.isNotTerminal());
        assertEquals(WIN_GAME, state.getPlayerResults()[0]);
        assertEquals(LOSE_GAME, state.getPlayerResults()[1]);
        assertEquals(LOSE_GAME, state.getPlayerResults()[2]);
        assertAllCardsPresent(state);
    }

    @Test
    public void passesAreCountedUntilACardIsPlayedAndTheDiscardUnderTheTopCanBeDrawn() {
        Game game = newGame(3, 2);
        CZEGameState state = (CZEGameState) game.getGameState();
        AbstractForwardModel fm = game.getForwardModel();
        setTopDiscard(state, card("5H"));
        giveHand(state, 0, card("9C"), card("KS"));
        giveHand(state, 1, card("10C"), card("QS"));
        leaveNothingToDraw(state, 2);   // player 2 holds every other card, including the Eight of Hearts

        // P0 and P1: no Heart, Five or Eight, and nothing to draw
        assertEquals(0, state.getCurrentPlayer());
        assertEquals(List.of(new Pass()), fm.computeAvailableActions(state));
        fm.next(state, new Pass());
        assertEquals(1, state.getConsecutivePasses());
        assertEquals(List.of(new Pass()), fm.computeAvailableActions(state));
        fm.next(state, new Pass());
        assertEquals(2, state.getConsecutivePasses());
        assertTrue(state.isNotTerminal());

        // P2: plays the Eight of Hearts nominating Diamonds, resetting the count
        assertEquals(2, state.getCurrentPlayer());
        fm.next(state, new PlayCard(card("8H"), Diamonds));
        assertEquals(0, state.getConsecutivePasses());

        // P0: stock is empty but the Five of Hearts is under the top card, so it can be drawn
        assertEquals(List.of(new DrawCard()), fm.computeAvailableActions(state));
        fm.next(state, new DrawCard());
        assertTrue(state.getPlayerHands().get(0).contains(card("5H")));
        assertEquals(List.of(card("8H")), state.getDiscardPile().getComponents());
        assertEquals(Diamonds, state.getCurrentSuit());
        assertEquals(0, state.getDrawDeck().getSize());

        // P1: nothing to play or draw again
        assertEquals(List.of(new Pass()), fm.computeAvailableActions(state));
        fm.next(state, new Pass());
        assertEquals(1, state.getConsecutivePasses());
        assertTrue(state.isNotTerminal());
        assertAllCardsPresent(state);
    }

    @Test
    public void randomGamesEndWithTheFewestCardsWinningAndEveryCardAccountedFor() {
        // Up to 8 players the stock runs out, so the discards must be reshuffled for the game to finish
        for (int nPlayers : new int[]{2, 4, 6, 8}) {
            for (long seed = 1; seed <= 5; seed++) {
                String label = nPlayers + " players, seed " + seed;
                Game game = newGame(nPlayers, seed);
                CZEGameState state = (CZEGameState) game.getGameState();
                AbstractForwardModel fm = game.getForwardModel();
                Random rnd = new Random(seed);

                int steps = 0;
                while (state.isNotTerminal() && steps < 5000) {
                    List<AbstractAction> actions = fm.computeAvailableActions(state);
                    fm.next(state, actions.get(rnd.nextInt(actions.size())));
                    assertAllCardsPresent(state);
                    steps++;
                }
                assertFalse(label + ": did not end within 5000 actions", state.isNotTerminal());

                // Going out (0 cards) or a blocked game: every player on the fewest cards wins, all others lose
                int fewest = state.getPlayerHands().stream().mapToInt(Deck::getSize).min().orElseThrow();
                for (int p = 0; p < nPlayers; p++) {
                    GameResult expected = state.getPlayerHands().get(p).getSize() == fewest ? WIN_GAME : LOSE_GAME;
                    assertEquals(label + ", player " + p, expected, state.getPlayerResults()[p]);
                }
            }
        }
    }
}
