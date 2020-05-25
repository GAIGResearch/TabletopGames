package games.coltexpress;

import core.AbstractPlayer;
import core.ForwardModel;
import core.Game;
import players.RandomPlayer;
import utilities.Utils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class ColtExpressGame extends Game {

    public ColtExpressGame(List<AbstractPlayer> agents, ForwardModel forwardModel, ColtExpressGameState gameState) {
        super(agents, forwardModel, gameState);
    }

    @Override
    public void run(GUI gui) {
        while (!gameState.isTerminal()){
            //System.out.println();
            if (VERBOSE) System.out.println("Round: " + gameState.getTurnOrder().getRoundCounter());
            //((IPrintable) gameState).printToConsole();
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
        }

        gameState.endGame();

        if (VERBOSE)
            System.out.println("Game Over");
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

            //gameState.printToConsole();
            // ((IPrintable) gameState.getObservation(null)).PrintToConsole();
            //System.out.println(Arrays.toString(gameState.getPlayerResults()));

            Utils.GameResult[] playerResults = gameState.getPlayerResults();
            for (int j = 0; j < gameState.getNPlayers(); j++){
                if (playerResults[j] == Utils.GameResult.GAME_WIN)
                    System.out.println("Player " + j + " won");
            }
        }
    }
}
