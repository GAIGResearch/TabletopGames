package games.sushigo;

import core.AbstractGameState;
import core.AbstractParameters;
import core.components.Component;
import core.turnorders.TurnOrder;

import java.util.ArrayList;
import java.util.List;

public class SGGameState extends AbstractGameState {
    /**
     * Constructor. Initialises some generic game state variables.
     *
     * @param gameParameters - game parameters.
     * @param turnOrder      - turn order for this game.
     */
    public SGGameState(AbstractParameters gameParameters, TurnOrder turnOrder) {
        super(gameParameters, turnOrder);
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
        return 0;
    }

    @Override
    public double getGameScore(int playerId) {
        return 0;
    }

    @Override
    protected ArrayList<Integer> _getUnknownComponentsIds(int playerId) {
        return null;
    }

    @Override
    protected void _reset() {

    }

    @Override
    protected boolean _equals(Object o) {
        return false;
    }
}
