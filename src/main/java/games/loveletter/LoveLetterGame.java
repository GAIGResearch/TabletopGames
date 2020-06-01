package games.loveletter;

import core.AbstractForwardModel;
import core.AbstractGameState;
import core.AbstractPlayer;
import core.AbstractGame;
import players.OSLA;
import players.RandomPlayer;
import utilities.Utils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;


public class LoveLetterGame extends AbstractGame {

    public LoveLetterGame(List<AbstractPlayer> agents, LoveLetterParameters params) {
        super(agents, new LoveLetterForwardModel(), new LoveLetterGameState(params, agents.size()));
    }

    public LoveLetterGame(AbstractForwardModel forwardModel, AbstractGameState gameState) {
        super(forwardModel, gameState);
    }

    public static void main(String[] args){

        // create list of players
        ArrayList<AbstractPlayer> agents = new ArrayList<>();
        agents.add(new RandomPlayer());
        agents.add(new RandomPlayer());
        agents.add(new RandomPlayer());
        agents.add(new OSLA());

        for (int i=0; i<1; i++) {
            // setup game
            LoveLetterParameters params = new LoveLetterParameters();
            AbstractGame game = new LoveLetterGame(agents, params);

            // run game
            game.run(null);
        }
    }

}
