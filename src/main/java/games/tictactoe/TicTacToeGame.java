package games.tictactoe;

import core.Game;
import core.AbstractPlayer;
import games.GameType;
import players.simple.OSLAPlayer;
import players.simple.RandomPlayer;

import java.util.*;

public class TicTacToeGame extends Game {

    public TicTacToeGame(TicTacToeGameParameters params) {
        super(GameType.TicTacToe, new TicTacToeForwardModel(), new TicTacToeGameState(params, 2));
    }

    public TicTacToeGame(List<AbstractPlayer> agents, TicTacToeGameParameters params) {
        super(GameType.TicTacToe, agents, new TicTacToeForwardModel(), new TicTacToeGameState(params, agents.size()));
    }

    public static void main(String[] args){
        ArrayList<AbstractPlayer> agents = new ArrayList<>();
        agents.add(new OSLAPlayer());
        agents.add(new RandomPlayer());

        TicTacToeGameParameters params = new TicTacToeGameParameters();
        Game game = new TicTacToeGame(agents, params);
        game.run();
    }
}
