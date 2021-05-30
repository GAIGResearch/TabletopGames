package games.battlelore;

import core.AbstractForwardModel;
import core.AbstractGameState;
import core.AbstractPlayer;
import core.Game;
import games.GameType;

import java.util.List;

public class BattleloreGame extends Game
{
    public BattleloreGame(GameType type, List<AbstractPlayer> players, AbstractForwardModel realModel, AbstractGameState gameState)
    {
        super(type, players, realModel, gameState);
    }
}
