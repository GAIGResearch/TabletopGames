package games.dicemonastery.actions;

import core.AbstractGameState;
import core.actions.AbstractAction;
import games.dicemonastery.DiceMonasteryGameState;

public class SummerBid extends AbstractAction {

    public final int beer;
    public final int mead;

    public SummerBid(int beer, int mead) {
        this.beer = beer;
        this.mead = mead;
    }

    @Override
    public boolean execute(AbstractGameState gs) {
        DiceMonasteryGameState state = (DiceMonasteryGameState) gs;
        // We move all beer and mead cubes from Storeroom to Hand
        // then move the ones selected for the bid into CommittedBid
        return state.reserveBid(beer, mead);
    }

    @Override
    public AbstractAction copy() {
        // immutable
        return this;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof SummerBid) {
            SummerBid other = (SummerBid) obj;
            return other.mead == mead && other.beer == beer;
        }
        return false;
    }

    @Override
    public int hashCode() {
        return beer * 59 + mead * 3 - 74398421;
    }

    @Override
    public String getString(AbstractGameState gameState) {
        return toString();
    }

    @Override
    public String toString() {
        return String.format("Bids %d beer and %d mead", beer, mead);
    }
}
