package games.uno;

import core.actions.IAction;
import core.GUI;
import core.Game;
import core.observations.Observation;
import players.AbstractPlayer;
import players.HumanConsolePlayer;

import java.util.*;

public class UnoGame extends Game {

    public UnoGame(List<AbstractPlayer> agents)
    {
        gameState = new UnoGameState(new UnoGameParameters(), agents.size());
        forwardModel = new UnoForwardModel();
    }

    @Override
    public void run(GUI gui) {
        while (!isEnded()){
            AbstractPlayer currentPlayer = players.get(gameState.getTurnOrder().getCurrentPlayer());
            int idx = currentPlayer.playerID;
            List<IAction> actions = Collections.unmodifiableList(gameState.getActions(idx));
            Observation observation = gameState.getObservation(idx);
            //((IPrintable) observation).PrintToConsole();
            int actionIdx = currentPlayer.getAction(observation, actions);

            forwardModel.next(gameState, actions.get(actionIdx));
            gameState.getTurnOrder().endPlayerTurn(gameState);
            System.out.println();
        }

        //((IPrintable) gameState.getObservation(-1)).PrintToConsole();
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
        game.run(null);
    }

}
