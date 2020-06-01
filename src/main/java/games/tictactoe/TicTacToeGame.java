package games.tictactoe;

import core.AbstractGame;
import core.AbstractPlayer;
import players.HumanConsolePlayer;
import players.OSLA;
import players.RandomPlayer;

import java.util.*;

public class TicTacToeGame extends AbstractGame {

    public TicTacToeGame(List<AbstractPlayer> agents, TicTacToeGameParameters params)
    {
        super(agents, new TicTacToeForwardModel(params.getGameSeed()), new TicTacToeGameState(params, agents.size()));
    }

    public static void main(String[] args){
        ArrayList<AbstractPlayer> agents = new ArrayList<>();
        agents.add(new OSLA());
        agents.add(new RandomPlayer());

        TicTacToeGameParameters params = new TicTacToeGameParameters();
        AbstractGame game = new TicTacToeGame(agents, params);
        game.run(null);
    }
}
