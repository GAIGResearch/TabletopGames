package players.mcts;

import core.AbstractGameState;
import core.AbstractParameters;
import core.components.Component;
import games.GameType;

import java.util.ArrayList;
import java.util.List;

public class LMRGame extends AbstractGameState {

    public LMRGame(AbstractParameters gameParameters) {
        super(gameParameters, 2);
    }

    // A Simple Game State for testing MCTS - this has no state at all, and 3 possible actions
    @Override
    protected GameType _getGameType() {
        return GameType.LoveLetter; // Not really
    }

    @Override
    protected List<Component> _getAllComponents() {
        return new ArrayList<>();
    }

    @Override
    protected AbstractGameState _copy(int playerId) {
        return new LMRGame(gameParameters);
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
    protected boolean _equals(Object o) {
        return o instanceof LMRGame;
    }
}

class LMTParameters extends AbstractParameters {

    public LMTParameters(int seed) {
        super();
        setRandomSeed(seed);
    }
    @Override
    protected AbstractParameters _copy() {
        return this;
    }

    @Override
    public boolean _equals(Object o) {
        return o instanceof LMTParameters;
    }

    @Override
    public int hashCode() {
        return 0;
    }
}