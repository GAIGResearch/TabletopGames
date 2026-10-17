package games.crazyeights.actions;

import core.AbstractGameState;
import core.actions.AbstractAction;
import core.components.FrenchCard;
import games.crazyeights.CZEGameState;

/**
 * The dealer nominates the suit to match when the starter card is an Eight (only when
 * CZEParameters.dealerNominatesStarterSuit is true).
 */
public class NominateSuit extends AbstractAction {

    public final FrenchCard.Suite suit;

    public NominateSuit(FrenchCard.Suite suit) {
        this.suit = suit;
    }

    @Override
    public boolean execute(AbstractGameState gs) {
        ((CZEGameState) gs).setCurrentSuit(suit);
        return true;
    }

    @Override
    public NominateSuit copy() {
        return this; // immutable
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof NominateSuit that && suit == that.suit;
    }

    @Override
    public int hashCode() {
        return suit.ordinal() + 382904;
    }

    @Override
    public String getString(AbstractGameState gameState) {
        return toString();
    }

    @Override
    public String toString() {
        return "Nominate " + suit + " for the starter Eight";
    }
}
