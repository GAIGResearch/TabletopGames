package games.coltexpress;

import core.AbstractPlayer;
import core.ForwardModel;
import core.GUI;
import core.Game;
import core.actions.IAction;
import core.observations.IObservation;
import core.observations.IPrintable;
import players.RandomPlayer;
import utilities.Utils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static utilities.CoreConstants.VERBOSE;

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
