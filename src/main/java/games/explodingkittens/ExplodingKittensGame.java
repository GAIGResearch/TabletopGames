package games.explodingkittens;

import core.actions.IAction;
import core.GUI;
import core.Game;
import players.RandomPlayer;
import core.observations.IPrintable;
import core.observations.IObservation;
import players.AbstractPlayer;

import java.util.*;

public class ExplodingKittensGame extends Game {

    public ExplodingKittensGame(List<AbstractPlayer> agents) {
        ExplodingKittenParameters params = new ExplodingKittenParameters();
        gameState = new ExplodingKittensPartialObservableGameState(params, agents.size());
        forwardModel = new ExplodingKittensForwardModel();
    }

    @Override
    public void run(GUI gui) {
        while (!isEnded()){
            //((ExplodingKittensGameState) gameState).print((ExplodingKittenTurnOrder) turnOrder);
            AbstractPlayer currentPlayer = players.get(gameState.getTurnOrder().getCurrentPlayer());
            int idx = currentPlayer.playerID;
            List<IAction> actions = Collections.unmodifiableList(gameState.getActions(idx));
            IObservation observation = gameState.getObservation(idx);
            if (observation != null)
                ((IPrintable) observation).PrintToConsole();
            int actionIdx = currentPlayer.getAction(observation, actions);
            forwardModel.next(gameState, actions.get(actionIdx));
            System.out.println();
        }

        ((ExplodingKittensGameState) gameState).print((ExplodingKittenTurnOrder) gameState.getTurnOrder());
        // ((IPrintable) gameState.getObservation(null)).PrintToConsole();
        System.out.println(Arrays.toString(gameState.getPlayerResults()));

        System.out.println("Game Over");
        for (int i = 0; i < gameState.getNPlayers(); i++){
            if (((ExplodingKittensGameState) gameState).isPlayerAlive[i])
                System.out.println("Player " + i + " won");
        }
    }

    @Override
    public boolean isEnded() {
        return ((ExplodingKittensGameState) gameState).isGameOver();
    }

    public static void main(String[] args){
        ArrayList<AbstractPlayer> agents = new ArrayList<>();
        agents.add(new RandomPlayer(0));
        agents.add(new RandomPlayer(1));
        agents.add(new RandomPlayer(2));
        agents.add(new RandomPlayer(3));

        Game game = new ExplodingKittensGame(agents);
        game.run(null);
    }
}
