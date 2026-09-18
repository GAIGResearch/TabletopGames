package players.mcts;

import core.AbstractGameState;
import core.AbstractParameters;

import java.util.List;

/**
 * The LMR fixture with three players, of whom only players 0 and 1 ever decide (together, on every
 * turn). Player 2 is a spectator with a reward entry but no decisions, which is the shape of a
 * Diamant node once the searching player has left the cave while others continue. Used to check
 * what a multi-actor backup does with the entry of a player who is not in the acting set.
 */
class ThreePlayerSimultaneousLMRGame extends LMRGame {

    ThreePlayerSimultaneousLMRGame(AbstractParameters gameParameters) {
        super(gameParameters, 3);
    }

    @Override
    public List<Integer> getCurrentSimultaneousPlayers() {
        return List.of(0, 1);
    }

    @Override
    protected AbstractGameState _copy(int playerId) {
        return new ThreePlayerSimultaneousLMRGame(gameParameters);
    }

    @Override
    protected boolean _equals(Object o) {
        return o instanceof ThreePlayerSimultaneousLMRGame;
    }
}
