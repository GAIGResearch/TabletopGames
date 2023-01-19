package games.dicemonastery;

import core.AbstractGameState;
import core.AbstractPlayer;
import core.Game;
import register.GameType;

import java.util.List;

public class DiceMonasteryGame extends Game {

    public DiceMonasteryGame(List<AbstractPlayer> agents, DiceMonasteryParams params) {
        super(GameType.DiceMonastery, agents, new DiceMonasteryForwardModel(), new DiceMonasteryGameState(params, agents.size()));
    }

    public DiceMonasteryGame(DiceMonasteryForwardModel realModel, AbstractGameState state) {
        super(GameType.DiceMonastery, realModel, state);
    }

}
