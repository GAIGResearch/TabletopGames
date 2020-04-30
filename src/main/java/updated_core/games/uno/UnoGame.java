package updated_core.games.uno;

import updated_core.ForwardModel;
import updated_core.Game;
import updated_core.actions.IAction;
import updated_core.actions.IPrintable;
import updated_core.games.tictactoe.TicTacToeForwardModel;
import updated_core.games.tictactoe.TicTacToeGame;
import updated_core.games.tictactoe.TicTacToeGameParameters;
import updated_core.games.tictactoe.TicTacToeGameState;
import updated_core.observations.Observation;
import updated_core.players.AbstractPlayer;
import updated_core.players.HumanConsolePlayer;
import updated_core.players.RandomAIPlayer;
import updated_core.turn_order.AlternatingTurnOrder;
import updated_core.turn_order.TurnOrder;

import java.util.*;

public class UnoGame extends Game {

    TurnOrder turnOrder;
    ForwardModel forwardModel = new UnoForwardModel();

    public UnoGame(List<AbstractPlayer> agents)
    {
        turnOrder = new AlternatingTurnOrder(agents);
        gameState = new UnoGameState(new UnoGameParameters(2));
    }

    @Override
    public void run() {
        while (!isEnded()){
            AbstractPlayer currentPlayer = turnOrder.getCurrentPlayer(gameState);
            List<IAction> actions = Collections.unmodifiableList(gameState.getActions(currentPlayer));
            Observation observation = gameState.getObservation(currentPlayer);
            //((IPrintable) observation).PrintToConsole();
            int actionIdx = currentPlayer.getAction(observation, actions);

            forwardModel.next(gameState, turnOrder, actions.get(actionIdx));
            turnOrder.endPlayerTurn(gameState);
            System.out.println();
        }

        //((IPrintable) gameState.getObservation(null)).PrintToConsole();
        //System.out.println(Arrays.toString(gameState.getPlayerResults()));
    }

    @Override
    public boolean isEnded() {
        return false;
    }

    @Override
    public HashSet<Integer> winners() {
        return null;
    }


    public static void main(String[] args){
        ArrayList<AbstractPlayer> agents = new ArrayList<>();
        agents.add(new HumanConsolePlayer(0));
        agents.add(new HumanConsolePlayer(1));

        UnoGame game = new UnoGame(agents);
        game.run();
    }

}
