package games.coltexpress;

import core.AbstractPlayer;
import core.AbstractForwardModel;
import core.AbstractGame;

import utilities.Utils;

import java.util.List;


public class ColtExpressRunner {

    public Utils.GameResult[] runGame(List<AbstractPlayer> agents) {
        ColtExpressParameters params = new ColtExpressParameters();
        AbstractForwardModel forwardModel = new ColtExpressForwardModel();
        ColtExpressGameState gameState = new ColtExpressGameState(params, forwardModel, agents.size());

        AbstractGame game = new ColtExpressGame(agents, forwardModel, gameState);
        game.run(null);
        return game.getGameState().getPlayerResults();
    }
}
