package games.connect4;

import core.Game;
import core.AbstractPlayer;
import games.GameType;
import players.simple.OSLAPlayer;
import players.simple.RandomPlayer;

import java.util.*;

public class Connect4Game extends Game {

    public Connect4Game(Connect4GameParameters params) {
        super(GameType.Connect4, new Connect4ForwardModel(), new Connect4GameState(params, 2));
    }

    public Connect4Game(List<AbstractPlayer> agents, Connect4GameParameters params) {
        super(GameType.Connect4, agents, new Connect4ForwardModel(), new Connect4GameState(params, agents.size()));
    }

    public static void main(String[] args){
        ArrayList<AbstractPlayer> agents = new ArrayList<>();
        agents.add(new OSLAPlayer());
        agents.add(new RandomPlayer());

        Connect4GameParameters params = new Connect4GameParameters(System.currentTimeMillis());
        Game game = new games.connect4.Connect4Game(agents, params);
        game.run();
    }
}
