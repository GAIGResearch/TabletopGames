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

}
