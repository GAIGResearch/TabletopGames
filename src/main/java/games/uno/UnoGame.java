package games.uno;

import core.*;
import players.HumanConsolePlayer;
import java.util.*;

public class UnoGame extends Game {

    public UnoGame(List<AbstractPlayer> agents, ForwardModel model, AbstractGameState gameState) {
        super(agents, model, gameState);
    }

    public static void main(String[] args) {
        ArrayList<AbstractPlayer> agents = new ArrayList<>();
        agents.add(new HumanConsolePlayer());
        agents.add(new HumanConsolePlayer());
        agents.add(new HumanConsolePlayer());
        agents.add(new HumanConsolePlayer());

        ForwardModel      forwardModel   = new UnoForwardModel();
        GameParameters    gameParameters = new UnoGameParameters();
        AbstractGameState gameState      = new UnoGameState(gameParameters, forwardModel, agents.size());
        Game              game           = new UnoGame(agents, forwardModel, gameState);

        game.run(null);
    }
}

