package games.virus;

import core.*;
import players.HumanConsolePlayer;

import java.util.ArrayList;
import java.util.List;

public class VirusGame extends AbstractGame {
    public VirusGame(List<AbstractPlayer> agents, AbstractForwardModel model, AbstractGameState gameState) {
        super(agents, model, gameState);
    }

    public static void main(String[] args) {
        ArrayList<AbstractPlayer> agents = new ArrayList<>();
        agents.add(new HumanConsolePlayer());
        agents.add(new HumanConsolePlayer());
        agents.add(new HumanConsolePlayer());
        agents.add(new HumanConsolePlayer());

        AbstractForwardModel      forwardModel   = new VirusForwardModel();
        AbstractGameParameters    gameParameters = new VirusGameParameters();
        AbstractGameState         gameState      = new VirusGameState(gameParameters, forwardModel, agents.size());
        AbstractGame              game           = new VirusGame(agents, forwardModel, gameState);

        game.run(null);
    }
}
