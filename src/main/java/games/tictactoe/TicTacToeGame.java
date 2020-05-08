package games.tictactoe;

import core.AbstractGameState;
import core.ForwardModel;
import core.Game;
import core.AbstractPlayer;
import players.HumanConsolePlayer;
import players.RandomPlayer;

import java.util.*;

public class TicTacToeGame extends Game {

    public TicTacToeGame(List<AbstractPlayer> agents, ForwardModel model, AbstractGameState gameState)
    {
        super(agents, model, gameState);
    }

    public static void main(String[] args){
        ArrayList<AbstractPlayer> agents = new ArrayList<>();
        agents.add(new RandomPlayer());
        agents.add(new HumanConsolePlayer());

        ForwardModel forwardModel = new TicTacToeForwardModel();
        AbstractGameState gameState = new TicTacToeGameState(new TicTacToeGameParameters(), forwardModel, agents.size());
        Game game = new TicTacToeGame(agents, forwardModel, gameState);
        game.run(null);
    }
}
