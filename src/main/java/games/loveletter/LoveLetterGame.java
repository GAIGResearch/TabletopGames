package games.loveletter;

import core.AbstractPlayer;
import core.Game;
import players.RandomPlayer;
import utilities.Utils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class LoveLetterGame extends Game {


    public LoveLetterGame(List<AbstractPlayer> agents, LoveLetterForwardModel forwardModel, LoveLetterGameState gameState) {
        super(agents, forwardModel, gameState);
    }

    public static void main(String[] args){
        ArrayList<AbstractPlayer> agents = new ArrayList<>();
        agents.add(new RandomPlayer());
        agents.add(new RandomPlayer());
        agents.add(new RandomPlayer());
        agents.add(new RandomPlayer());

        for (int i=0; i<1; i++) {
            LoveLetterParameters params = new LoveLetterParameters();
            LoveLetterForwardModel forwardModel = new LoveLetterForwardModel();
            LoveLetterGameState tmp_gameState = new LoveLetterGameState(params, forwardModel, agents.size());

            Game game = new LoveLetterGame(agents, forwardModel, tmp_gameState);
            game.run(null);
            LoveLetterGameState gameState = (LoveLetterGameState) game.getGameState();

            gameState.print((LoveLetterTurnOrder) gameState.getTurnOrder());
            // ((IPrintable) gameState.getObservation(null)).PrintToConsole();
            System.out.println(Arrays.toString(gameState.getPlayerResults()));

            Utils.GameResult[] playerResults = gameState.getPlayerResults();
            for (int j = 0; j < gameState.getNPlayers(); j++){
                if (playerResults[j] == Utils.GameResult.GAME_WIN)
                    System.out.println("Player " + j + " won");
            }
        }
    }
}
