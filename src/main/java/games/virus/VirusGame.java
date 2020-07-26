package games.virus;

import core.*;
import games.GameType;
import players.HumanConsolePlayer;
import players.OSLA;
import players.RandomPlayer;

import java.util.ArrayList;
import java.util.List;

// Official Rules
// https://tranjisgames.com/wp-content/uploads/2017/02/VIRUS-RULES-eng.pdf
public class VirusGame extends Game {
    public VirusGame(List<AbstractPlayer> agents, AbstractParameters gameParameters) {
        super(GameType.Virus, agents, new VirusForwardModel(), new VirusGameState(gameParameters, agents.size()));
    }

    public static void main(String[] args) {
        ArrayList<AbstractPlayer> agents = new ArrayList<>();
        //agents.add(new OSLA());
        //agents.add(new OSLA());
        //agents.add(new OSLA());
        //agents.add(new OSLA());
        //agents.add(new HumanConsolePlayer());
        //agents.add(new HumanConsolePlayer());
        agents.add(new RandomPlayer());
        agents.add(new RandomPlayer());

        AbstractParameters gameParameters = new VirusGameParameters(System.currentTimeMillis());
        Game game           = new VirusGame(agents, gameParameters);

        game.run(null);
    }
}
