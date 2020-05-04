package games.explodingkittens;

import core.Game;
import players.RandomPlayer;
import players.AbstractPlayer;

import java.util.*;

public class ExplodingKittensGame extends Game {

    public ExplodingKittensGame(List<AbstractPlayer> agents) {
        super(agents);
        ExplodingKittenParameters params = new ExplodingKittenParameters();
        forwardModel = new ExplodingKittensForwardModel();
        gameState = new ExplodingKittensGameState(params, forwardModel, agents.size());
        ((ExplodingKittensGameState)gameState).setComponents(params);
    }

    public static void main(String[] args){
        ArrayList<AbstractPlayer> agents = new ArrayList<>();
        agents.add(new RandomPlayer(0));
        agents.add(new RandomPlayer(1));
        agents.add(new RandomPlayer(2));
        agents.add(new RandomPlayer(3));

        for (int i=0; i<1000; i++) {
            Game game = new ExplodingKittensGame(agents);
            game.run(null);
            ExplodingKittensGameState gameState = (ExplodingKittensGameState) game.getGameState();

            gameState.print((ExplodingKittenTurnOrder) gameState.getTurnOrder());
            // ((IPrintable) gameState.getObservation(null)).PrintToConsole();
            System.out.println(Arrays.toString(gameState.getPlayerResults()));

            for (int j = 0; j < gameState.getNPlayers(); j++){
                if (gameState.isPlayerAlive(j))
                    System.out.println("Player " + j + " won");
            }
        }
    }
}
