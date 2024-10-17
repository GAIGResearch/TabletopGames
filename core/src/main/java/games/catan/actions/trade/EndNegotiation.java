package games.catan.actions.trade;

import core.AbstractGameState;
import core.actions.AbstractAction;
import games.catan.CatanGameState;

import java.util.Objects;

public class EndNegotiation extends AbstractAction {
    // This is used as an indication that we are stopping this round of negotiation
    public final int playerID;
    public final int offeringPlayerID;

    public EndNegotiation(int playerID, int offeringPlayerID) {
        this.playerID = playerID;
        this.offeringPlayerID = offeringPlayerID;
    }

    @Override
    public String toString() {
        return "p" + playerID + " rejects trade";
    }

    @Override
    public boolean execute(AbstractGameState gs) {
        ((CatanGameState) gs).setTradeOffer(null);
        gs.setTurnOwner(offeringPlayerID);
        ((CatanGameState) gs).negotiationStepsCount = 0;
        ((CatanGameState) gs).nTradesThisTurn++;
        return true;
    }

    @Override
    public EndNegotiation copy() {
        return this;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof EndNegotiation)) return false;
        EndNegotiation that = (EndNegotiation) o;
        return playerID == that.playerID && offeringPlayerID == that.offeringPlayerID;
    }

    @Override
    public int hashCode() {
        return Objects.hash(playerID, offeringPlayerID);
    }

    @Override
    public String getString(AbstractGameState gameState) {
        return toString();
    }
}
