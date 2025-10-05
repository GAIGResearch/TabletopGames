package games.powergrid.actions;

import java.util.Objects;


import core.AbstractGameState;
import core.actions.AbstractAction;
import games.powergrid.PowerGridGameState;

/**
 * Action representing that a player passes for the remainder of the current round/phase.
 * <p>
 * When executed, this removes the player from the round order so they will no longer
 * be prompted for actions during this phase.
 *
 * <p><b>Side effects:</b> Mutates {@link PowerGridGameState} by updating the round order.
 *
 * @see PowerGridGameState#removeFromRound(int)
 */

public class PassAction extends AbstractAction {
    private final int playerId;

    public PassAction(int playerId) {
        this.playerId = playerId;
    }

    @Override
    public boolean execute(AbstractGameState gs) {
        PowerGridGameState pggs = (PowerGridGameState) gs;
        pggs.removeFromRound(playerId);
        return true;
    }

    @Override
    public AbstractAction copy() { return new PassAction(playerId); }
    
    @Override
    public boolean equals(Object obj) { return obj instanceof PassAction && ((PassAction) obj).playerId == playerId; }
    
    @Override
    public int hashCode() { return Objects.hash(playerId); }
    
    @Override
    public String getString(AbstractGameState gameState) { return "Has elected to pass this Round"; }
    
    public int getPlayerId() {
    	return this.playerId;
    }
}

