package games.catan.actions;

import core.AbstractGameState;
import core.actions.AbstractAction;
import games.catan.CatanGameState;

import java.util.Arrays;
import java.util.Objects;

public class AcceptTrade extends AbstractAction {
    public final int offeringPlayer;
    public final int receivingPlayer;
    public final int[] resourcesRequested;
    public final int[] resourcesOffered;

    public AcceptTrade(int offeringPlayer, int receivingPlayer, int[] resourcesRequested, int[] resourcesOffered) {
        this.offeringPlayer = offeringPlayer;
        this.receivingPlayer = receivingPlayer;
        this.resourcesRequested = resourcesRequested;
        this.resourcesOffered = resourcesOffered;
    }

    @Override
    public boolean execute(AbstractGameState gs) {
        if (CatanGameState.swapResources((CatanGameState) gs, receivingPlayer, offeringPlayer, resourcesRequested, resourcesOffered)) {
            return true;
        } else {
            throw new AssertionError("A partner did not have sufficient resources");
        }
    }

    @Override
    public AbstractAction copy() {
        return this;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj instanceof AcceptTrade) {
            AcceptTrade otherAction = (AcceptTrade) obj;
            return otherAction.offeringPlayer == offeringPlayer
                    && otherAction.receivingPlayer == receivingPlayer
                    && Arrays.equals(otherAction.resourcesRequested, resourcesRequested)
                    && Arrays.equals(otherAction.resourcesOffered, resourcesOffered);
        }
        return false;
    }

    @Override
    public int hashCode() {
        int retValue = Objects.hash(offeringPlayer, receivingPlayer);
        return retValue + 41 * Arrays.hashCode(resourcesOffered) + 163 * Arrays.hashCode(resourcesRequested);
    }

    @Override
    public String getString(AbstractGameState gameState) {
        return toString();
    }

    @Override
    public String toString() {
        return String.format("Player %s accepts trade offered by %d : %s for %s ", receivingPlayer, offeringPlayer,
                OfferPlayerTrade.resourceArrayToString(resourcesRequested), OfferPlayerTrade.resourceArrayToString(resourcesOffered));
    }
}
