package games.tictactoe;

import core.AbstractGameState;
import core.AbstractForwardModel;
import core.AbstractGame;
import core.AbstractPlayer;
import players.HumanConsolePlayer;
import players.RandomPlayer;

import java.util.*;

public class TicTacToeGame extends AbstractGame {

    public TicTacToeGame(List<AbstractPlayer> agents, AbstractForwardModel model, AbstractGameState gameState)
    {
        super(agents, model, gameState);
    }

    public static void main(String[] args){
        ArrayList<AbstractPlayer> agents = new ArrayList<>();
        agents.add(new RandomPlayer());
        agents.add(new HumanConsolePlayer());

        AbstractForwardModel forwardModel = new TicTacToeForwardModel();
        AbstractGameState gameState = new TicTacToeGameState(new TicTacToeGameParameters(), forwardModel, agents.size());
        AbstractGame game = new TicTacToeGame(agents, forwardModel, gameState);
        game.run(null);
    }
}
