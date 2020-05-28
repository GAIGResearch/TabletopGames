package games.virus;

import core.*;
import players.HumanConsolePlayer;

import java.util.ArrayList;
import java.util.List;

public class VirusGame extends Game {
    public VirusGame(List<AbstractPlayer> agents, ForwardModel model, AbstractGameState gameState) {
        super(agents, model, gameState);
    }

    public static void main(String[] args) {
        ArrayList<AbstractPlayer> agents = new ArrayList<>();
        agents.add(new HumanConsolePlayer());
        agents.add(new HumanConsolePlayer());
        agents.add(new HumanConsolePlayer());
        agents.add(new HumanConsolePlayer());

        ForwardModel      forwardModel   = new VirusForwardModel();
        GameParameters    gameParameters = new VirusGameParameters();
        AbstractGameState gameState      = new VirusGameState(gameParameters, forwardModel, agents.size());
        Game              game           = new VirusGame(agents, forwardModel, gameState);

        game.run(null);
    }
}
