package games.DiceMonastery;

import core.*;
import games.GameType;

import java.util.*;

public class DiceMonasteryGame extends Game {

    public DiceMonasteryGame(DiceMonasteryForwardModel realModel, AbstractGameState state) {
        super(GameType.DiceMonastery, realModel, state);
    }
}
