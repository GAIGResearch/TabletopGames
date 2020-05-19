package games.carcassonne;

import core.AbstractGameState;
import core.ForwardModel;
import core.Game;
import core.AbstractPlayer;

import java.util.List;

public class CarcassonneGame extends Game {
    public CarcassonneGame(List<AbstractPlayer> players, ForwardModel model, AbstractGameState gameState) {
        super(players, model, gameState);
    }
}
