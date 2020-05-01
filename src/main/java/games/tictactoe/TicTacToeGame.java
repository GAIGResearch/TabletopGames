package games.tictactoe;

import actions.IAction;
import core.ForwardModel;
import core.GUI;
import core.Game;
import observations.IPrintable;
import observations.Observation;
import players.AbstractPlayer;
import players.HumanConsolePlayer;
import players.RandomPlayer;
import turnorder.AlternatingTurnOrder;
import turnorder.TurnOrder;

import java.util.*;

public class TicTacToeGame extends Game {

    TurnOrder turnOrder;
    ForwardModel forwardModel = new TicTacToeForwardModel();

    public TicTacToeGame(List<AbstractPlayer> agents)
    {
        turnOrder = new AlternatingTurnOrder(agents);
        gameState = new TicTacToeGameState(new TicTacToeGameParameters(2));
    }

    @Override
    public void run(GUI gui) {
        while (!isEnded()){
            AbstractPlayer currentPlayer = turnOrder.getCurrentPlayer(gameState);
            List<IAction> actions = Collections.unmodifiableList(gameState.getActions(currentPlayer));
            Observation observation = gameState.getObservation(currentPlayer);
            ((IPrintable) observation).PrintToConsole();
            int actionIdx = currentPlayer.getAction(observation, actions);
            forwardModel.next(gameState, turnOrder, actions.get(actionIdx));
            System.out.println();
        }

        ((IPrintable) gameState.getObservation(null)).PrintToConsole();
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
