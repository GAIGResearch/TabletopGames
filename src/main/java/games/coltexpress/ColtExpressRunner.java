package games.coltexpress;

import core.AbstractPlayer;
import core.ForwardModel;
import core.Game;

import utilities.Utils;

import java.util.List;


public class ColtExpressRunner {

    public Utils.GameResult[] runGame(List<AbstractPlayer> agents) {
        ColtExpressParameters params = new ColtExpressParameters();
        ForwardModel forwardModel = new ColtExpressForwardModel();
        ColtExpressGameState gameState = new ColtExpressGameState(params, forwardModel, agents.size());

        Game game = new ColtExpressGame(agents, forwardModel, gameState);
        game.run(null);
        return game.getGameState().getPlayerResults();
    }
}
