package games.powergrid.actions;

import core.AbstractGameState;
import core.actions.AbstractAction;
import games.powergrid.PowerGridGameState;

import java.util.Objects;

public class IncreaseBid extends AbstractAction {

    private final int playerId;

    public IncreaseBid(int playerId) {
        this.playerId = playerId;
    }

    @Override
    public boolean execute(AbstractGameState gs) {
        PowerGridGameState pggs = (PowerGridGameState) gs;

        int currentBid = pggs.getCurrentBid();
        int newBid = currentBid + 1;

        // Check if the player can afford the new bid
        if (pggs.getPlayersMoney(playerId) >= newBid) {
            pggs.setCurrentBid(newBid, playerId);  
            System.out.printf("Player %d increases bid to %d%n", playerId, newBid);
            return true;
        }

        System.out.printf("Player %d cannot afford to increase bid to %d%n", playerId, newBid);
        return false;
    }

    @Override
    public AbstractAction copy() {
        return new IncreaseBid(playerId);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof IncreaseBid)) return false;
        IncreaseBid other = (IncreaseBid) obj;
        return this.playerId == other.playerId;
    }

    @Override
    public int hashCode() {
        return Objects.hash(playerId);
    }

    @Override
    public String getString(AbstractGameState gameState) {
        PowerGridGameState pggs = (PowerGridGameState) gameState;
        return String.format("P%d increases bid to %d", playerId, pggs.getCurrentBid() + 1);
    }
}
