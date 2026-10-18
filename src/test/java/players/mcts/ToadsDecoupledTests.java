package players.mcts;

import core.AbstractPlayer;
import core.Game;
import games.GameType;
import games.toads.ToadGameState;
import org.junit.Test;
import players.PlayerConstants;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.*;

/**
 * MCTS on War of the Toads, where after the Attacker opens a Battle with their face-up card, the Attacker
 * chooses their hidden card at the same time as the Defender chooses both theirs. Both the decoupled and the
 * sequential search must play whole games without error.
 */
public class ToadsDecoupledTests {

    private static Game game(long seed, boolean decoupled) {
        List<AbstractPlayer> players = new ArrayList<>();
        for (int p = 0; p < 2; p++) {
            MCTSParams params = new MCTSParams();
            params.setRandomSeed(seed + p);
            params.budgetType = PlayerConstants.BUDGET_ITERATIONS;
            params.budget = 50;
            params.rolloutLength = 20;
            params.opponentTreePolicy = MCTSEnums.OpponentTreePolicy.OneTree;
            params.information = MCTSEnums.Information.Information_Set;
            params.decoupled = decoupled;
            TestMCTSPlayer player = new TestMCTSPlayer(params);
            player.rolloutTest = false;
            players.add(player);
        }
        Game g = GameType.WarOfTheToads.createGameInstance(2, seed);
        g.reset(players);
        return g;
    }

    @Test
    public void decoupledSearchPlaysWholeGames() {
        for (long seed = 1; seed <= 3; seed++) {
            Game g = game(seed, true);
            g.run();
            assertFalse(((ToadGameState) g.getGameState()).isNotTerminal());
        }
    }

    @Test
    public void sequentialSearchPlaysWholeGames() {
        for (long seed = 1; seed <= 3; seed++) {
            Game g = game(seed, false);
            g.run();
            assertFalse(((ToadGameState) g.getGameState()).isNotTerminal());
        }
    }
}
