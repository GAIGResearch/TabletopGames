package games.sushigo;

import core.AbstractForwardModel;
import core.AbstractGameState;
import core.Game;
import games.GameType;

public class SGGame extends Game {
    public SGGame(GameType type, AbstractForwardModel model, AbstractGameState gameState) {
        super(type, model, gameState);
    }
}
