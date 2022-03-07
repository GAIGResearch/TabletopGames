package games.loveletter;

import core.*;
import games.GameType;
import players.simple.OSLAPlayer;
import players.simple.RandomPlayer;

import java.util.ArrayList;
import java.util.List;


public class LoveLetterGame extends Game {

    public LoveLetterGame(List<AbstractPlayer> agents, LoveLetterParameters params) {
        super(GameType.LoveLetter, agents, new LoveLetterForwardModel(), new LoveLetterGameState(params, agents.size()));
    }

    public LoveLetterGame(AbstractForwardModel forwardModel, AbstractGameState gameState) {
        super(GameType.LoveLetter, forwardModel, gameState);
    }

    public static void main(String[] args){

        // create list of players
        ArrayList<AbstractPlayer> agents = new ArrayList<>();
        agents.add(new RandomPlayer());
        agents.add(new RandomPlayer());
        agents.add(new RandomPlayer());
        agents.add(new OSLAPlayer());

        for (int i=0; i<1; i++) {
            // setup game
            LoveLetterParameters params = new LoveLetterParameters(System.currentTimeMillis());
            Game game = new LoveLetterGame(agents, params);

            // run game
            game.run();
        }
    }

}
