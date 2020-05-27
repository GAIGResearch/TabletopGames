package games.carcassonne;

import core.AbstractGameState;
import core.AbstractForwardModel;
import core.AbstractGame;
import core.AbstractPlayer;

import java.util.List;

public class CarcassonneGame extends AbstractGame {
    public CarcassonneGame(List<AbstractPlayer> players, AbstractForwardModel model, AbstractGameState gameState) {
        super(players, model, gameState);
    }
}
