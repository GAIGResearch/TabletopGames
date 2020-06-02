package games.virus;

import core.*;
import games.GameType;
import players.OSLA;

import java.util.ArrayList;
import java.util.List;

public class VirusGame extends Game {
    public VirusGame(List<AbstractPlayer> agents, AbstractGameParameters gameParameters) {
        super(GameType.Virus, agents, new VirusForwardModel(), new VirusGameState(gameParameters, agents.size()));
    }

    public static void main(String[] args) {
        ArrayList<AbstractPlayer> agents = new ArrayList<>();
        agents.add(new OSLA());
        agents.add(new OSLA());
        agents.add(new OSLA());
        agents.add(new OSLA());
//        agents.add(new HumanConsolePlayer());
//        agents.add(new HumanConsolePlayer());
//        agents.add(new HumanConsolePlayer());

        AbstractGameParameters    gameParameters = new VirusGameParameters(System.currentTimeMillis());
        Game game           = new VirusGame(agents, gameParameters);

        game.run(null);
    }
}
