package games.coltexpress;

import core.GUI;
import core.Game;
import core.actions.IAction;
import core.observations.IObservation;
import core.observations.IPrintable;
import players.AbstractPlayer;
import players.RandomPlayer;
import utilities.Utils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static games.pandemic.PandemicConstants.VERBOSE;

public class ColtExpressGame extends Game {

    public ColtExpressGame(List<AbstractPlayer> agents) {
        super(agents);
        ColtExpressParameters params = new ColtExpressParameters();
        forwardModel = new ColtExpressForwardModel();
        gameState = new ColtExpressGameState(params, forwardModel, agents.size());
    }

    @Override
    public void run(GUI gui) {
        while (!gameState.isTerminal()){
            System.out.println();
            System.out.println();
            if (VERBOSE) System.out.println("Round: " + gameState.getTurnOrder().getRoundCounter());

            // Get player to ask for actions next
            int activePlayer = gameState.getTurnOrder().getCurrentPlayer(gameState);
            // Get actions for the player
            List<IAction> actions = Collections.unmodifiableList(gameState.getActions(true));
            IObservation observation = gameState.getObservation(activePlayer);
            if (observation != null && VERBOSE) {
                ((IPrintable) observation).printToConsole();
            }

            IAction action = actions.size() > 0 ? actions.get(players.get(activePlayer).getAction(observation, actions)) : null;
            forwardModel.next(gameState, action);
            break;
        }

        gameState.endGame();

        System.out.println("Game Over");
    }

    public static void main(String[] args){
        ArrayList<AbstractPlayer> agents = new ArrayList<>();
        agents.add(new RandomPlayer(0));
        agents.add(new RandomPlayer(1));
        agents.add(new RandomPlayer(2));
        agents.add(new RandomPlayer(3));

        for (int i=0; i<1; i++) {
            Game game = new ColtExpressGame(agents);
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
