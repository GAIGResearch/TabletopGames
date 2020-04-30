package updated_core.games.tictactoe;

import updated_core.ForwardModel;
import updated_core.Game;
import updated_core.actions.IAction;
import updated_core.actions.IPrintable;
import updated_core.observations.Observation;
import updated_core.players.AbstractPlayer;
import updated_core.players.HumanConsolePlayer;
import updated_core.players.RandomAIPlayer;
import updated_core.turn_order.AlternatingTurnOrder;
import updated_core.turn_order.TurnOrder;

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
    public void run() {
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
        agents.add(new RandomAIPlayer(0));
        agents.add(new HumanConsolePlayer(1));

        Game game = new TicTacToeGame(agents);
        game.run();
    }
}
