package games.ventlife.ai;

import core.AbstractForwardModel;
import core.Game;
import core.actions.AbstractAction;
import games.GameType;
import games.ventlife.VentlifeGameState;
import org.junit.Test;
import players.simple.RandomPlayer;

import java.util.List;
import java.util.Random;

import static org.junit.Assert.assertEquals;

public class VentlifeScorerTest {

    @Test
    public void scoresMatchGameScoreThroughRandomGames() {
        Random rnd = new Random(42);
        for (int nPlayers = 2; nPlayers <= 4; nPlayers++) {
            Game game = GameType.Ventlife.createGameInstance(nPlayers, 7);
            for (int g = 0; g < 40; g++) {
                game.reset(List.<core.AbstractPlayer>of(new RandomPlayer(), new RandomPlayer(), new RandomPlayer(), new RandomPlayer())
                        .subList(0, nPlayers), g);
                VentlifeGameState state = (VentlifeGameState) game.getGameState();
                AbstractForwardModel fm = game.getForwardModel();
                while (state.isNotTerminal()) {
                    int[] fast = VentlifeScorer.scores(state);
                    for (int p = 0; p < nPlayers; p++)
                        assertEquals("game " + g + " tick " + state.getGameTick(), state.getGameScore(p), fast[p], 0.0);
                    List<AbstractAction> actions = fm.computeAvailableActions(state);
                    fm.next(state, actions.get(rnd.nextInt(actions.size())));
                }
            }
        }
    }
}
