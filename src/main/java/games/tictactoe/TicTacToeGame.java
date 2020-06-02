package games.tictactoe;

import core.Game;
import core.AbstractPlayer;
import evaluation.Run;
import players.OSLA;
import players.RandomPlayer;

import java.util.*;

public class TicTacToeGame extends Game {

    public TicTacToeGame(List<AbstractPlayer> agents, TicTacToeGameParameters params)
    {
        super(Run.GameType.TicTacToe, agents, new TicTacToeForwardModel(), new TicTacToeGameState(params, agents.size()));
    }

    public static void main(String[] args){
        ArrayList<AbstractPlayer> agents = new ArrayList<>();
        agents.add(new OSLA());
        agents.add(new RandomPlayer());

        TicTacToeGameParameters params = new TicTacToeGameParameters(System.currentTimeMillis());
        Game game = new TicTacToeGame(agents, params);
        game.run(null);
    }
}
