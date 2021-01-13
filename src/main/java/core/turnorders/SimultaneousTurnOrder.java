package core.turnorders;

/**
 * This implements a simple simultaneous turn order, for example in Diamant.
 *
 * The difference to AlternatingTurnOrder is that when a state is copied from the
 * perspective of a player (i.e. when they are about to make a decision), any decisions made, but not yet revealed, are
 * deleted.
 * This will then require the algorithm to make appropriate decisions about what the unrevealed actions are.
 *
 * This copying/deletion is performed in the GameState.copy(). This TurnOrder is a marker so that players
 * can take appropriate action. This is used for example in OSLA Player.
 * (It would be equally possible to add a marker interface to the Game...this feels slightly better as
 * simultaneous moves is
 */
public class SimultaneousTurnOrder extends TurnOrder {
    public SimultaneousTurnOrder(int nPlayers) {
        super(nPlayers);
    }

    @Override
    protected void _reset() {
        // no additional actions required
    }

    @Override
    protected SimultaneousTurnOrder _copy() {
        return new SimultaneousTurnOrder(nPlayers);
    }

}
