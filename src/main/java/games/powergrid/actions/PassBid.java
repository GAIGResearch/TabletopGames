package games.powergrid.actions;

import core.AbstractGameState;
import core.actions.AbstractAction;
import games.powergrid.PowerGridGameState;

import java.util.Objects;

public class PassBid extends AbstractAction {
    private final int playerId;

    public PassBid(int playerId) {
        this.playerId = playerId;
    }

    @Override
    public boolean execute(AbstractGameState gs) {
        PowerGridGameState pggs = (PowerGridGameState) gs;

        // Call the helper that marks the player as passed
        pggs.passBid(playerId);

        System.out.printf("Player %d passes the bid.%n", playerId);

        return true;  // successful action
    }

    @Override
    public AbstractAction copy() {
        return new PassBid(playerId);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof PassBid)) return false;
        PassBid other = (PassBid) obj;
        return this.playerId == other.playerId;
    }

    @Override
    public int hashCode() {
        return Objects.hash(playerId);
    }

    @Override
    public String getString(AbstractGameState gameState) {
        return String.format("Has elected to not bid");
    }
}
