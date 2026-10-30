package games.explodingkittens;

import core.CoreConstants;
import games.explodingkittens.actions.NopeableAction;
import games.explodingkittens.actions.PlayEKCard;
import games.explodingkittens.cards.ExplodingKittensCard;
import org.json.simple.JSONObject;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import utilities.JSONUtils;

import java.io.File;
import java.io.IOException;

import static games.explodingkittens.cards.ExplodingKittensCard.CardType.NOPE;
import static games.explodingkittens.cards.ExplodingKittensCard.CardType.SKIP;
import static org.junit.Assert.*;

/**
 * Round-trip serialization tests for {@link ExplodingKittensGameState}, mirroring
 * {@code games.backgammon.JSONTest} and {@code games.connect4.JSONTest}. Exploding Kittens is the
 * first game serialized that uses Extended Action Sequences, so the second and third tests
 * specifically exercise an {@link core.interfaces.IExtendedSequence} on the actionsInProgress stack.
 */
public class JSONTest {

    // deleted after each test, whether it passes or fails
    @Rule
    public TemporaryFolder tmp = new TemporaryFolder();

    private ExplodingKittensGameState freshState(int nPlayers) {
        ExplodingKittensParameters params = new ExplodingKittensParameters();
        ExplodingKittensGameState state = new ExplodingKittensGameState(params, nPlayers);
        ExplodingKittensForwardModel fm = new ExplodingKittensForwardModel();
        fm.setup(state);
        return state;
    }

    @Test
    public void testJSON() {
        ExplodingKittensGameState state = freshState(4);
        state.setFirstPlayer(1);
        state.setTurnOwner(2);
        state.setGameStatus(CoreConstants.GameResult.GAME_ONGOING);

        JSONObject json = state.toJSON();

        assertTrue(json.containsKey("abstractGameState"));
        JSONObject abstractGS = (JSONObject) json.get("abstractGameState");
        assertEquals(1L, ((Number) abstractGS.get("firstPlayer")).longValue());
        assertEquals(2L, ((Number) abstractGS.get("turnOwner")).longValue());

        ExplodingKittensGameState loaded = JSONUtils.loadClassFromJSON(json);

        // Full equality: decks (with componentIDs, order and visibility), counters and the
        // (here empty) actionsInProgress stack must all round-trip.
        assertEquals(state, loaded);
        assertEquals(state.getDrawPile(), loaded.getDrawPile());
        assertEquals(state.getDiscardPile(), loaded.getDiscardPile());
        for (int p = 0; p < state.getNPlayers(); p++)
            assertEquals(state.getPlayerHand(p), loaded.getPlayerHand(p));
    }

    @Test
    public void testJSONWithActionInProgress() {
        ExplodingKittensGameState state = freshState(4);
        state.setTurnOwner(0);

        // Ensure player 1 holds a NOPE so the NopeableAction finds an interrupter (and so stays
        // open on the stack rather than completing immediately).
        state.getPlayerHand(1).add(new ExplodingKittensCard(NOPE));
        NopeableAction nopeable = new NopeableAction(0, new PlayEKCard(SKIP), state);
        state.setActionInProgress(nopeable);
        assertTrue(state.isActionInProgress());

        JSONObject json = state.toJSON();
        ExplodingKittensGameState loaded = JSONUtils.loadClassFromJSON(json);

        assertEquals(state, loaded);
        assertTrue(loaded.isActionInProgress());
        assertEquals(state.currentActionInProgress(), loaded.currentActionInProgress());
        // currentPlayer is delegated to the sequence, so this confirms the interrupter was restored
        assertEquals(state.getCurrentPlayer(), loaded.getCurrentPlayer());
    }

    @Test
    public void testJSONLoadFromFile() throws IOException {
        ExplodingKittensGameState state = freshState(3);
        state.setTurnOwner(0);
        state.getPlayerHand(1).add(new ExplodingKittensCard(NOPE));
        state.setActionInProgress(new NopeableAction(0, new PlayEKCard(SKIP), state));

        JSONObject json = state.toJSON();
        File file = tmp.newFile("ekTestFile.json");
        JSONUtils.writeJSON(json, file.getPath());

        ExplodingKittensGameState loaded = JSONUtils.loadClassFromFile(file.getPath());

        assertEquals(state, loaded);
    }
}
