package games.crazyeights.actions;

import core.AbstractGameState;
import core.actions.AbstractAction;
import core.components.FrenchCard;
import games.crazyeights.CZEGameState;

import java.util.Objects;

/**
 * The dealer nominates the suit to match when the starter card is an Eight (Phase D, only when
 * CZEParameters.dealerNominatesStarterSuit is true).
 */
public class NominateSuit extends AbstractAction {

    public final int player;
    public final FrenchCard.Suite suit;

    public NominateSuit(int player, FrenchCard.Suite suit) {
        this.player = player;
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
        return o instanceof NominateSuit that && player == that.player && suit == that.suit;
    }

    @Override
    public int hashCode() {
        return Objects.hash(player, suit, "NominateSuit");
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
