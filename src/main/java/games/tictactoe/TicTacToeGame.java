package games.tictactoe;

import core.actions.IAction;
import core.GUI;
import core.Game;
import core.observations.IPrintable;
import core.observations.Observation;
import players.AbstractPlayer;
import players.HumanConsolePlayer;
import players.RandomPlayer;

import java.util.*;

public class TicTacToeGame extends Game {

    public TicTacToeGame(List<AbstractPlayer> agents)
    {
        gameState = new TicTacToeGameState(new TicTacToeGameParameters(), agents.size());
        forwardModel = new TicTacToeForwardModel();
    }

    @Override
    public void run(GUI gui) {
        while (!isEnded()){
            AbstractPlayer currentPlayer = players.get(gameState.getTurnOrder().getCurrentPlayer());
            int idx = currentPlayer.playerID;
            List<IAction> actions = Collections.unmodifiableList(gameState.getActions(idx));
            Observation observation = gameState.getObservation(idx);
            ((IPrintable) observation).PrintToConsole();
            int actionIdx = currentPlayer.getAction(observation, actions);
            forwardModel.next(gameState, actions.get(actionIdx));
            System.out.println();
        }

        ((IPrintable) gameState.getObservation(-1)).PrintToConsole();
        System.out.println(Arrays.toString(gameState.getPlayerResults()));
    }

    @Override
    public boolean isEnded() {
        return gameState.isTerminal();
    }

    @Override
    public HashSet<Integer> winners() {
        return null;
    }

    public static void main(String[] args){
        ArrayList<AbstractPlayer> agents = new ArrayList<>();
        agents.add(new RandomPlayer(0));
        agents.add(new HumanConsolePlayer(1));

        Game game = new TicTacToeGame(agents);
        game.run(null);
    }
}
