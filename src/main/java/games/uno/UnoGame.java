package games.uno;

import core.Game;
import players.AbstractPlayer;
import players.HumanConsolePlayer;

import java.util.*;

public class UnoGame extends Game {

    public UnoGame(List<AbstractPlayer> agents)
    {
        super(agents);
        forwardModel = new UnoForwardModel();
        gameState = new UnoGameState(new UnoGameParameters(), forwardModel, agents.size());
    }

    public static void main(String[] args){
        ArrayList<AbstractPlayer> agents = new ArrayList<>();
        agents.add(new HumanConsolePlayer(0));
        agents.add(new HumanConsolePlayer(1));

        UnoGame game = new UnoGame(agents);
        game.run(null);
    }

}
