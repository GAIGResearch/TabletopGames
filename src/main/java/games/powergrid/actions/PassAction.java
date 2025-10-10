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

    @Override
    public boolean execute(AbstractGameState gs) {
        PowerGridGameState s = (PowerGridGameState) gs;
        int me = s.getCurrentPlayer();
        s.removeFromRound(me);
        return true;
    }

    @Override
    public AbstractAction copy() { 
        return new PassAction(); 
    }

    @Override
    public boolean equals(Object obj) { 
        return obj instanceof PassAction; 
    }

    @Override
    public int hashCode() { 
        return 0xA55A1;  // any small constant; valid hex
    }

    @Override
    public String getString(AbstractGameState gameState) { 
        return "Pass Round"; 
    }
}

