package games.cantstop;

import core.*;
import register.GameType;
import players.human.*;
import players.simple.*;

import java.util.ArrayList;
import java.util.List;

public class CantStopGame extends Game {
    public CantStopGame(List<AbstractPlayer> players, AbstractParameters gameParameters) {
        super(GameType.CantStop, players, new CantStopForwardModel(), new CantStopGameState(gameParameters, players.size()));
    }

    public static void main(String[] args) {
        ActionController ac = new ActionController();

        ArrayList<AbstractPlayer> agents = new ArrayList<>();
        agents.add(new RandomPlayer());
        agents.add(new RandomPlayer());
        agents.add(new OSLAPlayer());
        agents.add(new HumanConsolePlayer());

        runOne(GameType.CantStop, null, agents, System.currentTimeMillis() + 1000,
                false, null, null, 0);
    }

}
