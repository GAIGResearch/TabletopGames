package games.uno;

import core.*;
import players.HumanConsolePlayer;

import java.util.*;

public class UnoGame extends AbstractGame {

    public UnoGame(List<AbstractPlayer> agents, AbstractForwardModel model, AbstractGameState gameState) {
        super(agents, model, gameState);
    }

    public static void main(String[] args) {
        ArrayList<AbstractPlayer> agents = new ArrayList<>();
        agents.add(new HumanConsolePlayer());
        agents.add(new HumanConsolePlayer());
        agents.add(new HumanConsolePlayer());
        agents.add(new HumanConsolePlayer());

        AbstractForwardModel      forwardModel   = new UnoForwardModel();
        AbstractGameParameters    gameParameters = new UnoGameParameters();
        AbstractGameState         gameState      = new UnoGameState(gameParameters, forwardModel, agents.size());
        AbstractGame              game           = new UnoGame(agents, forwardModel, gameState);

        game.run(null);
    }
}

