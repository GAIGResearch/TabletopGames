package games.uno;

import core.*;
import players.HumanConsolePlayer;

import java.util.*;

public class UnoGame extends AbstractGame {

    public UnoGame(List<AbstractPlayer> agents, AbstractGameParameters gameParameters) {
        super(agents, new UnoForwardModel(), new UnoGameState(gameParameters, new UnoTurnOrder(agents.size())));
    }

    public static void main(String[] args) {
        ArrayList<AbstractPlayer> agents = new ArrayList<>();
        agents.add(new HumanConsolePlayer());
        agents.add(new HumanConsolePlayer());
        agents.add(new HumanConsolePlayer());
        agents.add(new HumanConsolePlayer());

        AbstractGameParameters    gameParameters = new UnoGameParameters();
        AbstractGame              game           = new UnoGame(agents, gameParameters);

        game.run(null);
    }
}

