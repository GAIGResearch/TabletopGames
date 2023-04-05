package games.catan.actions.trade;

import core.AbstractGameState;
import core.actions.AbstractAction;
import games.catan.CatanGameState;
import games.catan.CatanParameters;

import java.util.Objects;

public class AcceptTrade extends AbstractAction {
    public final int playerID;
    public final int offeringPlayer;
    public final int otherPlayer;
    public final CatanParameters.Resource resourceRequested, resourceOffered;
    public final int nRequested, nOffered;

    public AcceptTrade(int playerID, CatanParameters.Resource resourceOffered, int nOffered,
                       CatanParameters.Resource resourceRequested, int nRequested,
                       int offeringPlayerID, int otherPlayerID) {
        this.playerID = playerID;
        this.offeringPlayer = offeringPlayerID;
        this.otherPlayer = otherPlayerID;
        this.resourceRequested = resourceRequested;
        this.nOffered = nOffered;
        this.resourceOffered = resourceOffered;
        this.nRequested = nRequested;
    }

    @Override
    public boolean execute(AbstractGameState gs) {
        if (((CatanGameState)gs).swapResources(otherPlayer, offeringPlayer, resourceRequested, nRequested) &&
                ((CatanGameState)gs).swapResources(offeringPlayer, otherPlayer, resourceOffered, nOffered)) {
            ((CatanGameState) gs).setTradeOffer(null);
            gs.setTurnOwner(offeringPlayer);
            ((CatanGameState) gs).negotiationStepsCount = 0;
            ((CatanGameState) gs).nTradesThisTurn++;
            return true;
        } else {
            throw new AssertionError("A partner did not have sufficient resources");
        }
    }

    @Override
    public AcceptTrade copy() {
        return this;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof AcceptTrade)) return false;
        AcceptTrade that = (AcceptTrade) o;
        return playerID == that.playerID && offeringPlayer == that.offeringPlayer && otherPlayer == that.otherPlayer && nRequested == that.nRequested && nOffered == that.nOffered && resourceRequested == that.resourceRequested && resourceOffered == that.resourceOffered;
    }

    @Override
    public int hashCode() {
        return Objects.hash(playerID, offeringPlayer, otherPlayer, resourceRequested, resourceOffered, nRequested, nOffered);
    }

    @Override
    public String getString(AbstractGameState gameState) {
        return toString();
    }

    @Override
    public String toString() {
        return String.format("p%d accepts trade (p%d to p%d : %d %s for %d %s)", playerID, offeringPlayer, otherPlayer,
                nOffered, resourceOffered, nRequested, resourceRequested);
    }
}
