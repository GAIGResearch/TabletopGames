package games.battlelore;

import core.AbstractGameState;
import core.AbstractParameters;
import core.components.Component;
import core.components.GridBoard;
import core.components.Token;
import core.turnorders.TurnOrder;
import games.GameType;

import java.util.List;

public class BattleloreGameState extends AbstractGameState{
    /**
     * Constructor. Initialises some generic game state variables.
     *
     * @param gameParameters - game parameters.
     * @param turnOrder      - turn order for this game.
     * @param gameType
     */

    GridBoard<Token> hexBoard;

    public BattleloreGameState(AbstractParameters gameParameters, TurnOrder turnOrder, GameType gameType) {
        super(gameParameters, turnOrder, GameType.Battlelore);
    }

    @Override
    protected List<Component> _getAllComponents() {
        return null;
    }

    @Override
    protected AbstractGameState _copy(int playerId) {
        return null;
    }

    @Override
    protected double _getHeuristicScore(int playerId) {
        /**
         * This provides the current score in game turns. This will only be relevant for games that have the concept
         * of victory points, etc.
         * If a game does not support this directly, then just return 0.0
         *
         * @param playerId
         * @return - double, score of current state
         */
        return 0;
    }

    @Override
    public double getGameScore(int playerId) {
        return 0;
    }

    @Override
    protected void _reset() {

    }

    @Override
    protected boolean _equals(Object o) {
        return false;
    }
}
