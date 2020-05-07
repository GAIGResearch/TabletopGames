package games.uno;

import core.AbstractGameState;
import core.ForwardModel;
import core.Game;
import core.AbstractPlayer;
import players.HumanConsolePlayer;

import java.util.*;

public class UnoGame extends Game {

    public UnoGame(List<AbstractPlayer> agents, ForwardModel model, AbstractGameState gameState)
    {
        super(agents, model, gameState);
    }

    public static void main(String[] args){
        ArrayList<AbstractPlayer> agents = new ArrayList<>();
        agents.add(new HumanConsolePlayer());
        agents.add(new HumanConsolePlayer());

        ForwardModel forwardModel = new UnoForwardModel();
        AbstractGameState gameState = new UnoGameState(new UnoGameParameters(), forwardModel, agents.size());
        UnoGame game = new UnoGame(agents, forwardModel, gameState);
        game.run(null);
    }

}
