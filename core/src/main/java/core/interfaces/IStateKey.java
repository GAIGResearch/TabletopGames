package core.interfaces;

import core.AbstractGameState;

public interface IStateKey {

    default Object getKey(AbstractGameState state) {
        return getKey(state, state.getCurrentPlayer());
    }

    // And to support situations where we would like the key to be different for different players
    Object getKey(AbstractGameState state, int playerId);

}
