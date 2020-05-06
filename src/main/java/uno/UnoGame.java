package uno;

import actions.IAction;
import core.ForwardModel;
import core.GUI;
import core.Game;
import observations.Observation;
import players.AbstractPlayer;
import players.HumanConsolePlayer;
import turnorder.AlternatingTurnOrder;
import turnorder.TurnOrder;

import java.util.*;

public class UnoGame extends Game {

    TurnOrder turnOrder;
    ForwardModel forwardModel = new UnoForwardModel();

    public UnoGame(List<AbstractPlayer> agents) {
        int nPlayers = agents.size();

        turnOrder = new AlternatingTurnOrder(agents);                               // The turn order can change
        gameState = new UnoGameState(new UnoGameParameters(nPlayers));
    }

    @Override
    public void run(GUI gui) {
        while (!isEnded()) {
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

    // The game is ended if there is a player without cards
    @Override
    public boolean isEnded() {
        return ((UnoGameState) gameState).isEnded();
    }

    @Override
    public HashSet<Integer> winners() {
        // TODO
        return null;
    }

    public static void main(String[] args) {
        ArrayList<AbstractPlayer> agents = new ArrayList<>();
        agents.add(new HumanConsolePlayer(0));
        agents.add(new HumanConsolePlayer(1));
        agents.add(new HumanConsolePlayer(2));
        agents.add(new HumanConsolePlayer(3));

        UnoGame game = new UnoGame(agents);
        game.run(null);
    }
}