package games.carcassonne;

import core.AbstractGameState;
import core.AbstractForwardModel;
import core.Game;
import core.AbstractPlayer;
import evaluation.Run;

import java.util.List;

public class CarcassonneGame extends Game {
    public CarcassonneGame(List<AbstractPlayer> players, AbstractForwardModel model, AbstractGameState gameState) {
        super(Run.GameType.Carcassonne, players, model, gameState);
    }
}
