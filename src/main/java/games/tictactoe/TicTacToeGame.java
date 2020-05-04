package games.tictactoe;

import core.actions.IAction;
import core.GUI;
import core.Game;
import core.observations.IPrintable;
import core.observations.IObservation;
import players.AbstractPlayer;
import players.HumanConsolePlayer;
import players.RandomPlayer;

import java.util.*;

public class TicTacToeGame extends Game {

    public TicTacToeGame(List<AbstractPlayer> agents)
    {
        super(agents);
        forwardModel = new TicTacToeForwardModel();
        gameState = new TicTacToeGameState(new TicTacToeGameParameters(), forwardModel, agents.size());
    }

    public static void main(String[] args){
        ArrayList<AbstractPlayer> agents = new ArrayList<>();
        agents.add(new RandomPlayer(0));
        agents.add(new HumanConsolePlayer(1));

        Game game = new TicTacToeGame(agents);
        game.run(null);
    }
}
