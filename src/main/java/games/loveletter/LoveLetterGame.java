package games.loveletter;

import core.AbstractPlayer;
import core.AbstractGame;
import players.RandomPlayer;
import utilities.Utils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;


public class LoveLetterGame extends AbstractGame {

    public LoveLetterGame(List<AbstractPlayer> agents, LoveLetterForwardModel forwardModel, LoveLetterGameState gameState) {
        super(agents, forwardModel, gameState);
    }

    public static void main(String[] args){

        // create list of players
        ArrayList<AbstractPlayer> agents = new ArrayList<>();
        agents.add(new RandomPlayer());
        agents.add(new RandomPlayer());
        agents.add(new RandomPlayer());
        agents.add(new RandomPlayer());

        for (int i=0; i<1; i++) {
            // setup game
            LoveLetterParameters params = new LoveLetterParameters();
            LoveLetterForwardModel forwardModel = new LoveLetterForwardModel();
            LoveLetterGameState tmp_gameState = new LoveLetterGameState(params, forwardModel, agents.size());
            AbstractGame game = new LoveLetterGame(agents, forwardModel, tmp_gameState);

            // run game
            game.run(null);

            // evaluate result
            LoveLetterGameState finalGameState = (LoveLetterGameState) game.getGameState();
            finalGameState.print((LoveLetterTurnOrder) finalGameState.getTurnOrder());
            System.out.println(Arrays.toString(finalGameState.getPlayerResults()));
            Utils.GameResult[] playerResults = finalGameState.getPlayerResults();
            for (int j = 0; j < finalGameState.getNPlayers(); j++){
                if (playerResults[j] == Utils.GameResult.GAME_WIN)
                    System.out.println("Player " + j + " won");
            }
        }
    }

}
