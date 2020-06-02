package games.loveletter;

import core.AbstractForwardModel;
import core.AbstractGameState;
import core.AbstractPlayer;
import core.Game;
import evaluation.Run;
import players.OSLA;
import players.RandomPlayer;

import java.util.ArrayList;
import java.util.List;


public class LoveLetterGame extends Game {

    public LoveLetterGame(List<AbstractPlayer> agents, LoveLetterParameters params) {
        super(Run.GameType.LoveLetter, agents, new LoveLetterForwardModel(), new LoveLetterGameState(params, agents.size()));
    }

    public LoveLetterGame(AbstractForwardModel forwardModel, AbstractGameState gameState) {
        super(Run.GameType.LoveLetter, forwardModel, gameState);
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
            LoveLetterParameters params = new LoveLetterParameters(System.currentTimeMillis());
            Game game = new LoveLetterGame(agents, params);

            // run game
            game.run(null);
        }
    }

}
