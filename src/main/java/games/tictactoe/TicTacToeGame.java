package games.tictactoe;

import core.AbstractForwardModel;
import core.AbstractGame;
import core.AbstractPlayer;
import players.HumanConsolePlayer;
import players.RandomPlayer;

import java.util.*;

public class TicTacToeGame extends AbstractGame {

    public TicTacToeGame(List<AbstractPlayer> agents, TicTacToeGameParameters params)
    {
        super(agents, new ArrayList<AbstractForwardModel>() {{
            for (int i = 0; i < agents.size(); i++) {
                add(new TicTacToeForwardModel(System.currentTimeMillis()));
            }
        }}, new TicTacToeForwardModel(params.getGameSeed()), new TicTacToeGameState(params, agents.size()));
    }

    public static void main(String[] args){
        ArrayList<AbstractPlayer> agents = new ArrayList<>();
        agents.add(new RandomPlayer());
        agents.add(new HumanConsolePlayer());

        TicTacToeGameParameters params = new TicTacToeGameParameters();
        AbstractGame game = new TicTacToeGame(agents, params);
        game.run(null);
    }
}
