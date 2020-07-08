package games.uno;

import core.*;
import games.GameType;
import players.OSLA;

import java.util.*;

public class UnoGame extends Game {

    public UnoGame(List<AbstractPlayer> agents, AbstractParameters gameParameters) {
        super(GameType.Uno, agents, new UnoForwardModel(), new UnoGameState(gameParameters, agents.size()));
    }

    public static void main(String[] args) {
        ArrayList<AbstractPlayer> agents = new ArrayList<>();
//        agents.add(new HumanConsolePlayer());
//        agents.add(new HumanConsolePlayer());
//        agents.add(new HumanConsolePlayer());
//        agents.add(new HumanConsolePlayer());
        agents.add(new OSLA());
        agents.add(new OSLA());
        agents.add(new OSLA());
        agents.add(new OSLA());
        agents.add(new OSLA());

        AbstractParameters gameParameters = new UnoGameParameters(System.currentTimeMillis());
        Game game           = new UnoGame(agents, gameParameters);

        game.run(null);
    }
}

