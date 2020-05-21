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
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static utilities.CoreConstants.VERBOSE;

public class ColtExpressGame extends Game {

    public ColtExpressGame(List<AbstractPlayer> agents, ForwardModel forwardModel, ColtExpressGameState gameState) {
        super(agents, forwardModel, gameState);
    }

    public static void main(String[] args){
        ArrayList<AbstractPlayer> agents = new ArrayList<>();
        agents.add(new RandomPlayer());
        agents.add(new RandomPlayer());
        agents.add(new RandomPlayer());
        agents.add(new RandomPlayer());

        for (int i=0; i<1; i++) {
            ColtExpressParameters params = new ColtExpressParameters();
            ForwardModel forwardModel = new ColtExpressForwardModel();
            ColtExpressGameState tmp_gameState = new ColtExpressGameState(params, forwardModel, agents.size());

            Game game = new ColtExpressGame(agents, forwardModel, tmp_gameState);
            game.run(null);
            ColtExpressGameState gameState = (ColtExpressGameState) game.getGameState();

            gameState.printToConsole();
            // ((IPrintable) gameState.getObservation(null)).PrintToConsole();
            System.out.println(Arrays.toString(gameState.getPlayerResults()));

            Utils.GameResult[] playerResults = gameState.getPlayerResults();
            for (int j = 0; j < gameState.getNPlayers(); j++){
                if (playerResults[j] == Utils.GameResult.GAME_WIN)
                    System.out.println("Player " + j + " won");
            }
        }
    }
}
