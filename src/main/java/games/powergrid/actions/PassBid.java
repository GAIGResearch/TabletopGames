package games.powergrid.actions;

import java.util.Objects;

import core.AbstractGameState;
import core.actions.AbstractAction;
import games.powergrid.PowerGridGameState;

public class PassBid extends AbstractAction {
    private final int playerId;

    public PassBid(int playerId) {
        this.playerId = playerId;
    }

    @Override
    public boolean execute(AbstractGameState gs) {
        PowerGridGameState pggs = (PowerGridGameState) gs;
        pggs.passOnAuction(playerId);         // <-- drop from rotation
        System.out.println("Player " + playerId + " Passed");
        return true;
    }

    @Override
    public AbstractAction copy() { return new PassBid(playerId); }
    @Override
    public boolean equals(Object obj) { return obj instanceof PassBid && ((PassBid) obj).playerId == playerId; }
    @Override
    public int hashCode() { return Objects.hash(playerId); }
    @Override
    public String getString(AbstractGameState gameState) { return "P" + playerId + " passes"; }
}

