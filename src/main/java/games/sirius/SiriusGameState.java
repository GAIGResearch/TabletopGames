package games.sirius;

import core.*;
import core.components.*;
import core.turnorders.TurnOrder;
import games.GameType;

import java.util.*;

public class SiriusGameState extends AbstractGameState {
    public SiriusGameState(AbstractParameters gameParameters, GameType gameType) {
        super(gameParameters, gameType);
        rnd = new Random(gameParameters.getRandomSeed());
    }

    Deck<Card> ammoniaDeck;
    List<Moon> moons;
    Random rnd;

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
    protected void _reset() {
        ammoniaDeck = new Deck<>("ammoniaDeck", -1, CoreConstants.VisibilityMode.VISIBLE_TO_OWNER);
        moons = new ArrayList<>();
    }

    @Override
    protected boolean _equals(Object o) {
        return false;
    }
}
