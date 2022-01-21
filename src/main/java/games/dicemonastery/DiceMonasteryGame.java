package games.dicemonastery;

import core.AbstractGameState;
import core.AbstractPlayer;
import core.Game;
import games.GameType;
import players.human.ActionController;
import players.human.HumanGUIPlayer;
import players.mcts.MCTSParams;
import players.mcts.MCTSPlayer;
import players.rmhc.RMHCPlayer;
import players.simple.RandomPlayer;

import java.util.ArrayList;
import java.util.List;

import static games.GameType.DiceMonastery;

public class DiceMonasteryGame extends Game {

    public DiceMonasteryGame(List<AbstractPlayer> agents, DiceMonasteryParams params) {
        super(GameType.DiceMonastery, agents, new DiceMonasteryForwardModel(), new DiceMonasteryGameState(params, agents.size()));
    }

    public DiceMonasteryGame(DiceMonasteryForwardModel realModel, AbstractGameState state) {
        super(GameType.DiceMonastery, realModel, state);
    }


    public static void main(String[] args) {
        /* 1. Action controller for GUI interactions. If set to null, running without visuals. */
        ActionController ac = new ActionController(); //null;

        /* 2. Game seed */
        long seed = System.currentTimeMillis(); //0;

        /* 3. Set up players for the game */
        ArrayList<AbstractPlayer> players = new ArrayList<>();

        MCTSParams params1 = new MCTSParams();

        players.add(new RandomPlayer());
        players.add(new RMHCPlayer());
        players.add(new MCTSPlayer(params1));
        players.add(new HumanGUIPlayer(ac));
//        players.add(new HumanConsolePlayer());

        /* 4. Run! */
        runOne(DiceMonastery, null, players, seed, ac, false, null);

    }
}
