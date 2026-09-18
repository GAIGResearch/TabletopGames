package games.agram;

import core.AbstractForwardModel;
import core.Game;
import core.actions.AbstractAction;
import core.components.FrenchCard;
import games.agram.actions.PlayCard;
import org.junit.Test;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

import static core.CoreConstants.GameResult.LOSE_GAME;
import static core.CoreConstants.GameResult.WIN_GAME;
import static games.agram.AgramTestUtils.*;
import static org.junit.Assert.*;

public class AgramGameFlowTest {

    @Test
    public void winningAnEarlierTrickDoesNotWinTheGameOnlyTheLastTrickDoes() {
        Game game = newGame(3, 5);
        AgramGameState state = (AgramGameState) game.getGameState();
        AbstractForwardModel fm = game.getForwardModel();
        // two cards each, so the game is two tricks long
        giveHand(state, 0, "AH", "3C");
        giveHand(state, 1, "4H", "AC");
        giveHand(state, 2, "5H", "10C");

        // trick 1: player 0's Ace of Hearts wins
        playCards(state, fm, "AH", "4H", "5H");
        assertTrue(state.isNotTerminal());
        assertEquals(0, state.getCurrentPlayer());
        for (int p = 0; p < 3; p++)
            assertEquals("no score until the game is over", 0.0, state.getGameScore(p), 0.0);

        // trick 2: player 0 must lead the Three of Clubs; player 1's Ace of Clubs wins the last trick
        assertEquals(List.of(play("3C")), fm.computeAvailableActions(state));
        fm.next(state, play("3C"));
        assertEquals(List.of(play("AC")), fm.computeAvailableActions(state));
        fm.next(state, play("AC"));
        assertTrue("the game is not over until the last card is played", state.isNotTerminal());
        fm.next(state, play("10C"));

        assertFalse(state.isNotTerminal());
        assertEquals(LOSE_GAME, state.getPlayerResults()[0]);
        assertEquals(WIN_GAME, state.getPlayerResults()[1]);
        assertEquals(LOSE_GAME, state.getPlayerResults()[2]);
        assertEquals(0.0, state.getGameScore(0), 0.0);
        assertEquals(1.0, state.getGameScore(1), 0.0);
        assertEquals(0.0, state.getGameScore(2), 0.0);
        assertEquals(6, state.getDiscardPile().getSize());
        assertEquals(0, state.getCurrentTrick().getSize());
        assertAllCardsPresent(state);
    }

    /**
     * Plays a random game and returns the number of actions after which it ended; fails if it has not ended
     * within the cap.
     */
    private int actionsToEndOfRandomGame(AgramGameState state, AbstractForwardModel fm, long seed) {
        Random rnd = new Random(seed);
        int actions = 0;
        while (state.isNotTerminal() && actions < 200) {
            playRandomAction(state, fm, rnd);
            actions++;
        }
        assertFalse("game did not end within 200 actions", state.isNotTerminal());
        return actions;
    }

    @Test
    public void aDefaultGameEndsAfterSixTricks() {
        Game game = newGame(3, 11);
        AgramGameState state = (AgramGameState) game.getGameState();
        // 6 tricks x 3 players
        assertEquals(18, actionsToEndOfRandomGame(state, game.getForwardModel(), 11));
        assertEquals(18, state.getDiscardPile().getSize());
        assertEquals("undealt cards are never used", 17, state.getDrawDeck().getSize());
    }

    @Test
    public void withFiveCardsPerPlayerTheGameEndsAfterFiveTricks() {
        AgramParameters params = new AgramParameters();
        params.setParameterValue("nCardsPerPlayer", 5);
        Game game = newGame(3, 11, params);
        AgramGameState state = (AgramGameState) game.getGameState();
        // 5 tricks x 3 players
        assertEquals(15, actionsToEndOfRandomGame(state, game.getForwardModel(), 11));
        assertEquals(15, state.getDiscardPile().getSize());
        assertEquals(20, state.getDrawDeck().getSize());
    }

    /**
     * The legal cards from the rules: all of the hand when leading or void in the suit led, else that suit only.
     */
    private static Set<AbstractAction> expectedActions(List<FrenchCard> hand, List<FrenchCard> trickSoFar) {
        Set<AbstractAction> actions = new HashSet<>();
        boolean canFollow = !trickSoFar.isEmpty() && hand.stream().anyMatch(c -> c.suite == trickSoFar.get(0).suite);
        for (FrenchCard c : hand)
            if (!canFollow || c.suite == trickSoFar.get(0).suite)
                actions.add(new PlayCard(c));
        return actions;
    }

    @Test
    public void randomGamesForTwoToFivePlayersFollowTheRulesToTheEnd() {
        int tricksChecked = 0;
        for (int nPlayers = 2; nPlayers <= 5; nPlayers++) {
            for (long seed = 1; seed <= 5; seed++) {
                Game game = newGame(nPlayers, seed);
                AgramGameState state = (AgramGameState) game.getGameState();
                AbstractForwardModel fm = game.getForwardModel();
                Random rnd = new Random(seed);
                String label = nPlayers + " players, seed " + seed;

                List<FrenchCard> trickCards = new ArrayList<>();
                List<Integer> trickPlayers = new ArrayList<>();
                int lastTrickWinner = -1;
                int steps = 0;
                while (state.isNotTerminal() && steps++ < 100) {
                    int player = state.getCurrentPlayer();
                    List<AbstractAction> actions = fm.computeAvailableActions(state);
                    assertEquals(label, expectedActions(state.getPlayerHands().get(player).getComponents(), trickCards),
                            new HashSet<>(actions));
                    PlayCard chosen = (PlayCard) actions.get(rnd.nextInt(actions.size()));
                    fm.next(state, chosen);
                    trickCards.add(chosen.card);
                    trickPlayers.add(player);
                    assertAllCardsPresent(state);

                    if (trickCards.size() == nPlayers) {
                        lastTrickWinner = expectedTrickWinner(trickCards, trickPlayers);
                        if (state.isNotTerminal())
                            assertEquals(label + ": trick winner leads", lastTrickWinner, state.getCurrentPlayer());
                        assertEquals(label, 0, state.getCurrentTrick().getSize());
                        trickCards.clear();
                        trickPlayers.clear();
                        tricksChecked++;
                    } else {
                        assertEquals(label + ": next player in turn", (player + 1) % nPlayers, state.getCurrentPlayer());
                    }
                }
                assertFalse(label + ": game did not end within 100 actions", state.isNotTerminal());
                assertEquals(label + ": 6 cards each played in 6 tricks", 6 * nPlayers, steps);
                for (int p = 0; p < nPlayers; p++) {
                    assertEquals(label + ", player " + p, p == lastTrickWinner ? WIN_GAME : LOSE_GAME,
                            state.getPlayerResults()[p]);
                    assertEquals(label + ", player " + p, p == lastTrickWinner ? 1.0 : 0.0, state.getGameScore(p), 0.0);
                }
            }
        }
        assertTrue("no trick was completed", tricksChecked > 0);
    }
}
