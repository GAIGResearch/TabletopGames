package games.seasaltpaper;

import core.AbstractGameState;
import core.AbstractForwardModel;
import core.Game;
import core.actions.AbstractAction;
import games.GameType;
import org.junit.Test;
import players.jsonBagPlayers.Tokenizer;

import java.util.List;
import java.util.Map;

import static org.junit.Assert.*;

/**
 * SeaSaltPaper builds its draw pile by iterating the card definitions in {@code cardsInit}, which is a HashMap
 * keyed by enum pairs. Enum hash codes are identity hashes, so the old iteration order - and with it the shuffled
 * deal - depended on what else the process had done rather than on the game seed. The definitions are now walked
 * in a canonical order; this test pins that the same seed deals the same game regardless of what ran before.
 */
public class SeaSaltPaperSeedDeterminismTest {

    private static AbstractGameState seaSaltPaper(long seed) {
        return GameType.SeaSaltPaper.createGameInstance(2, seed,
                GameType.SeaSaltPaper.createParameters(seed)).getGameState();
    }

    @Test
    public void theSameSeedDealsTheSameGameWhateverRanBefore() {
        Map<String, Integer> before = Tokenizer.tokenize(seaSaltPaper(101L));

        // work the JVM has to do in between: play another game, and serialise a state to JSON
        Game other = GameType.Wonders7.createGameInstance(3, 7L,
                GameType.Wonders7.createParameters(7L));
        AbstractGameState otherState = other.getGameState();
        AbstractForwardModel forwardModel = other.getForwardModel();
        for (int i = 0; i < 5; i++) {
            List<AbstractAction> actions = forwardModel.computeAvailableActions(otherState);
            if (actions.isEmpty()) break;
            forwardModel.next(otherState, actions.get(0).copy());
        }
        new com.google.gson.Gson().toJson(
                evaluation.containers.GameStateContainerFactory.createContainer(otherState));

        Map<String, Integer> after = Tokenizer.tokenize(seaSaltPaper(101L));

        assertEquals("the same seed must deal the same game", before, after);
    }

    @Test
    public void differentSeedsDealDifferentGames() {
        assertNotEquals(Tokenizer.tokenize(seaSaltPaper(101L)), Tokenizer.tokenize(seaSaltPaper(202L)));
    }
}
