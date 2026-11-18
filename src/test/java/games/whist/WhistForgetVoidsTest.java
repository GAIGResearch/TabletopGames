package games.whist;

import core.AbstractPlayer;
import core.Game;
import core.actions.AbstractAction;
import core.components.FrenchCard;
import games.GameType;
import games.tricktaking.ForgetVoidsDecorator;
import games.tricktaking.PlayCard;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.junit.Before;
import org.junit.Test;
import players.mcts.MCTSParams;
import players.mcts.MCTSPlayer;
import players.simple.RandomPlayer;
import utilities.JSONUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.Set;

import static core.components.FrenchCard.Suite.Hearts;
import static games.tricktaking.TrickTakingTestUtils.*;
import static games.whist.WhistTestUtils.*;
import static org.junit.Assert.*;

/**
 * An agent set to forget known voids (ForgetVoidsDecorator): its observation, and every copy made from it, neither
 * records nor uses voids, while the game state keeps them.
 */
public class WhistForgetVoidsTest {

    WhistGameState state;
    WhistForwardModel fm;

    /**
     * As WhistCopyTest: player 1 holds the 13 diamonds; spades trumps, the Ace of Spades turned up in dealer 3's hand.
     * Hearts are led and player 1 discards a diamond, so is known void in hearts; player 0 then leads the 2 of Clubs,
     * and player 1 is to play.
     */
    @Before
    public void setup() {
        state = newState(42);
        fm = new WhistForwardModel();
        arrangeHands(state,
                cards("AH", "KH", "QH", "JH", "10H", "2C", "3C", "4C", "5C", "6C", "7C", "8C", "9C"),
                cards("AD", "KD", "QD", "JD", "10D", "9D", "8D", "7D", "6D", "5D", "4D", "3D", "2D"),
                cards("9H", "8H", "7H", "6H", "10C", "JC", "QC", "KC", "AC", "2S", "3S", "4S", "5S"),
                cards("5H", "4H", "3H", "2H", "6S", "7S", "8S", "9S", "10S", "JS", "QS", "KS", "AS"));
        setTrumps(state, "AS");
        playCards(state, fm, "AH", "2D", "9H", "5H", "2C");
        assertEquals(Set.of(Hearts), state.getKnownVoids().get(1));
        assertEquals(1, state.getCurrentPlayer());
    }

    private static boolean holdsAHeart(WhistGameState s, int p) {
        for (FrenchCard c : s.getPlayerHand(p).getComponents())
            if (c.suite == Hearts) return true;
        return false;
    }

    @Test
    public void forgettingClearsTheVoids() {
        WhistGameState obs = (WhistGameState) state.copy(0);
        obs.forgetVoids(0);
        assertFalse(obs.getKnownVoids().isRemembering());
        assertTrue(obs.getKnownVoids().get(1).isEmpty());
    }

    @Test
    public void onceForgottenNoVoidIsRecorded() {
        WhistGameState obs = (WhistGameState) state.copy();
        obs.getKnownVoids().forget();
        fm.next(obs, new PlayCard<>(card("3D")));     // player 1, holding no clubs, discards a diamond
        assertTrue(obs.getKnownVoids().get(1).isEmpty());
        // whereas the game state records the club void
        fm.next(state, new PlayCard<>(card("3D")));
        assertEquals(Set.of(Hearts, FrenchCard.Suite.Clubs), state.getKnownVoids().get(1));
    }

    @Test
    public void aNewDealDoesNotBringTheVoidsBack() {
        WhistGameState obs = (WhistGameState) state.copy();
        obs.getKnownVoids().forget();
        obs.getKnownVoids().clear();
        assertFalse(obs.getKnownVoids().isRemembering());
    }

    @Test
    public void copiesOfAForgettingStateStayForgetful() {
        WhistGameState obs = (WhistGameState) state.copy(0);
        obs.forgetVoids(0);
        assertFalse(((WhistGameState) obs.copy()).getKnownVoids().isRemembering());
        assertFalse(((WhistGameState) obs.copy(2)).getKnownVoids().isRemembering());
    }

    @Test
    public void withoutVoidsACopyMayGiveAPlayerASuitTheyAreKnownVoidIn() {
        WhistGameState obs = (WhistGameState) state.copy(0);
        obs.forgetVoids(0);
        boolean heart = holdsAHeart(obs, 1);
        for (int i = 0; i < 50 && !heart; i++)
            heart = holdsAHeart((WhistGameState) obs.copy(0), 1);
        assertTrue("player 1 is given a heart in some copy", heart);
    }

    @Test
    public void forgettingRedeterminisesOnlyWhatThePlayerCannotSee() {
        WhistGameState obs = (WhistGameState) state.copy(0);
        obs.forgetVoids(0);
        assertEquals(cardsOf(state.getPlayerHand(0)), cardsOf(obs.getPlayerHand(0)));
        assertEquals(cardsOf(state.getCurrentTrick()), cardsOf(obs.getCurrentTrick()));
        assertEquals(cardsOf(state.getDiscardPile()), cardsOf(obs.getDiscardPile()));
        assertTrue("the dealer keeps the turned-up card", obs.getPlayerHand(3).contains(card("AS")));
        for (int p = 1; p < 4; p++)
            assertEquals(12, obs.getPlayerHand(p).getSize());
        assertAllCardsPresent(obs);
    }

    @Test
    public void forgettingTwiceDoesNothingMore() {
        WhistGameState obs = (WhistGameState) state.copy(0);
        obs.forgetVoids(0);
        WhistGameState before = (WhistGameState) obs.copy();
        obs.forgetVoids(0);
        assertEquals(before, obs);
    }

    @Test
    public void theGameStateKeepsItsVoids() {
        WhistGameState before = (WhistGameState) state.copy();
        WhistGameState obs = (WhistGameState) state.copy(0);
        obs.forgetVoids(0);
        assertEquals(before, state);
        assertTrue(state.getKnownVoids().isRemembering());
        assertNotEquals("forgetting is part of the state", state.getKnownVoids(), obs.getKnownVoids());
    }

    // ---- the decorator ----

    @Test
    public void theDecoratorForgetsTheVoidsAndChangesNoActions() {
        WhistGameState obs = (WhistGameState) state.copy(1);
        List<AbstractAction> actions = fm.computeAvailableActions(obs);
        assertSame(actions, new ForgetVoidsDecorator().actionFilter(obs, actions));
        assertFalse(obs.getKnownVoids().isRemembering());
        assertEquals(cardsOf(state.getPlayerHand(1)), cardsOf(obs.getPlayerHand(1)));
    }

    @Test
    public void theDecoratorDoesNothingInAGameWithoutVoids() {
        Game game = GameType.GoFish.createGameInstance(3, 1);
        game.reset(List.of(new RandomPlayer(), new RandomPlayer(), new RandomPlayer()));
        List<AbstractAction> actions = game.getForwardModel().computeAvailableActions(game.getGameState());
        assertSame(actions, new ForgetVoidsDecorator().actionFilter(game.getGameState(), actions));
    }

    @Test(expected = IllegalStateException.class)
    public void theDecoratorFailsInATrickTakingGameThatCannotForgetYet() {
        Game game = GameType.Hearts.createGameInstance(4, 1);
        game.reset(List.of(new RandomPlayer(), new RandomPlayer(), new RandomPlayer(), new RandomPlayer()));
        new ForgetVoidsDecorator().actionFilter(game.getGameState(), new ArrayList<>());
    }

    @Test
    public void theDecoratorIsBuiltFromJSON() throws Exception {
        JSONObject json = (JSONObject) new JSONParser().parse("{\"class\" : \"games.tricktaking.ForgetVoidsDecorator\"}");
        Object decorator = JSONUtils.loadClassFromJSON(json);
        assertTrue(decorator instanceof ForgetVoidsDecorator);
    }

    @Test
    public void gamesWithSomeSearchAgentsForgettingVoidsRunToTheEnd() {
        for (long seed = 1; seed <= 2; seed++) {
            Game game = GameType.Whist.createGameInstance(4, seed, valetParams());
            List<AbstractPlayer> players = new ArrayList<>();
            for (int p = 0; p < 4; p++) {
                MCTSParams params = new MCTSParams();
                params.setParameterValue("budget", 100);
                params.setRandomSeed((int) seed + p);
                MCTSPlayer player = new MCTSPlayer(params);
                if (p % 2 == 0)
                    player.addDecorator(new ForgetVoidsDecorator());
                players.add(player);
            }
            game.reset(players);
            game.run();
            WhistGameState end = (WhistGameState) game.getGameState();
            assertFalse("game " + seed + " ends", end.isNotTerminal());
            assertTrue("the game state remembers voids", end.getKnownVoids().isRemembering());
        }
    }
}
