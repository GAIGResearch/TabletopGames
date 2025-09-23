package games.powergrid.actions;

import java.util.Objects;

import core.AbstractGameState;
import core.actions.AbstractAction;
import games.powergrid.PowerGridGameState;

public class PassAction extends AbstractAction {
    private final int playerId;

    public PassAction(int playerId) {
        this.playerId = playerId;
    }

    @Override
    public boolean execute(AbstractGameState gs) {
        PowerGridGameState pggs = (PowerGridGameState) gs;
        pggs.removeFromRound(playerId);
        System.out.println("Player " + playerId + " has elected to pass");
        return true;
    }

    @Override
    public AbstractAction copy() { return new PassAction(playerId); }
    @Override
    public boolean equals(Object obj) { return obj instanceof PassAction && ((PassAction) obj).playerId == playerId; }
    @Override
    public int hashCode() { return Objects.hash(playerId); }
    @Override
    public String getString(AbstractGameState gameState) { return "P" + playerId + " passes"; }
}

