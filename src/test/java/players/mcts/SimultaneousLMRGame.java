package players.mcts;

import core.AbstractGameState;
import core.AbstractParameters;

import java.util.List;

/**
 * The LMR fixture with both players moving at once, on every turn.
 * <p>
 * Everything else is inherited: no state, three actions each, the forward model never ends the
 * game. What changes is only what the state reports about who is deciding, which is the one thing
 * a decoupled search asks. Every node of a decoupled tree over this game is therefore a
 * multi-actor node with nine joint actions, which makes the decoupled path testable
 * deterministically without SushiGo's reveal-and-rotate machinery.
 * <p>
 * A search with {@code decoupled = false} over this game must be indistinguishable from one over
 * plain LMRGame - that is the sequential-play contract in players/mcts/DecoupledUCT.md, and
 * {@link DecoupledUCTTests} checks it against the sequential golden digest.
 */
class SimultaneousLMRGame extends LMRGame {

    SimultaneousLMRGame(AbstractParameters gameParameters) {
        super(gameParameters);
    }

    @Override
    public List<Integer> getCurrentSimultaneousPlayers() {
        return List.of(0, 1);
    }

    @Override
    protected AbstractGameState _copy(int playerId) {
        return new SimultaneousLMRGame(gameParameters);
    }

    @Override
    protected boolean _equals(Object o) {
        return o instanceof SimultaneousLMRGame;
    }
}
