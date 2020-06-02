package games.carcassonne;

import core.*;

import java.util.List;

public class CarcassonneGame extends Game {
    public CarcassonneGame(List<AbstractPlayer> players, AbstractForwardModel model, AbstractGameState gameState) {
        super(GameType.Carcassonne, players, model, gameState);
    }
}
