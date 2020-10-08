package games.dominion;

import core.*;
import games.GameType;
import players.mcts.MCTSPlayer;
import players.simple.*;

import java.util.*;

public class DominionGame extends Game {

    public DominionGame(List<AbstractPlayer> players, DominionParameters params) {
        super(GameType.Dominion, players, new DominionForwardModel(), new DominionGameState(params, players.size()));
    }


    public static void main(String[] args){

        // create list of players
        ArrayList<AbstractPlayer> agents = new ArrayList<>();
        agents.add(new RandomPlayer());
        agents.add(new OSLAPlayer());
        agents.add(new RandomPlayer());
        agents.add(new OSLAPlayer());

        for (int i=0; i<1; i++) {
            // setup game
            DominionParameters params = new DominionParameters(System.currentTimeMillis());
            Game game = new DominionGame(agents, params);

            // run game
            game.run(null);
        }
    }
}
