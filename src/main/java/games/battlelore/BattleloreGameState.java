package games.battlelore;

import core.AbstractGameState;
import core.AbstractParameters;
import core.components.Component;
import core.components.GridBoard;
import core.components.Token;
import core.turnorders.AlternatingTurnOrder;
import core.turnorders.TurnOrder;
import games.GameType;
import games.dominion.DominionParameters;
import games.dominion.DominionTurnOrder;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Random;

public class BattleloreGameState extends AbstractGameState
{
    /**
     * Constructor. Initialises some generic game state variables.
     *
     * @param gameParameters - game parameters.
     * @param turnOrder      - turn order for this game.
     * @param gameType
     */

    GridBoard<Token> hexBoard;

    public BattleloreGameState(AbstractParameters gameParameters, int nPlayers)
    {
        super(gameParameters, new AlternatingTurnOrder(nPlayers) ,GameType.Battlelore);
    }

    @Override
    protected List<Component> _getAllComponents()
    {
        return new ArrayList<Component>()
        {{
            add(hexBoard);
        }};
    }

    @Override
    protected AbstractGameState _copy(int playerId)
    {
        BattleloreGameState state = new BattleloreGameState(gameParameters.copy(), 2);
        state.hexBoard = hexBoard.copy();
        return state;
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
        //TODO_ERTUGRUL: Add the heuristic
        return 0;
    }

    @Override
    public double getGameScore(int playerId) {
        return 0;
    }

    @Override
    protected void _reset() {
        hexBoard = null;
    }

    @Override
    protected boolean _equals(Object o)
    {
        if (this== o)
        {
            return true;
        }

        if (!(o instanceof BattleloreGameState))
        {
            return false;
        }

        if (!super.equals(o))
        {
            return false;
        }

        BattleloreGameState other = (BattleloreGameState) o;
        return Objects.equals(hexBoard, other.hexBoard);
    }

}
