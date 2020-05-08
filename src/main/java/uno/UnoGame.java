package uno;

import actions.IAction;
import core.ForwardModel;
import core.GUI;
import core.Game;
import observations.IPrintable;
import observations.Observation;
import players.AbstractPlayer;
import players.HumanConsolePlayer;
import turnorder.AlternatingTurnOrder;
import turnorder.TurnOrder;
import uno.actions.PlayCard;

import java.util.*;

public class UnoGame extends Game {

    private TurnOrder turnOrder;
    private ForwardModel forwardModel = new UnoForwardModel();

    public UnoGame(List<AbstractPlayer> agents) {
        int nPlayers = agents.size();

        turnOrder = new AlternatingTurnOrder(agents);
        UnoGameParameters gameParameters = new UnoGameParameters(nPlayers);
        gameState = new UnoGameState(gameParameters, turnOrder);
    }

    @Override
    public void run(GUI gui) {
        while (!isEnded()) {
            System.out.println("### " + turnOrder.getTurnCounter());
            AbstractPlayer currentPlayer = turnOrder.getCurrentPlayer(gameState);
            List<IAction> actions = Collections.unmodifiableList(gameState.getActions(currentPlayer));
            Observation observation = gameState.getObservation(currentPlayer);
            int actionIdx = currentPlayer.getAction(observation, actions);
            forwardModel.next(gameState, turnOrder, actions.get(actionIdx));
            turnOrder.endPlayerTurn(gameState);
            System.out.println();
        }

        System.out.println("**************************************************");
        System.out.println("The winner is the player: " + ((UnoGameState) gameState).getWinnerID());
        System.out.println("**************************************************");
    }

    // The game is ended if there is a player without cards
    @Override
    public boolean isEnded() {
        return ((UnoGameState) gameState).isEnded();
    }

    @Override
    public HashSet<Integer> winners() {
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