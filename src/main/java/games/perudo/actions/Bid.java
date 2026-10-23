package games.perudo.actions;

import core.AbstractGameState;
import core.actions.AbstractAction;
import games.perudo.PerudoGameState;

import static games.perudo.PerudoParameters.PACO;

/**
 * Bid that at least quantity dice on the table show face.
 */
public class Bid extends AbstractAction {

    public final int quantity;
    public final int face;

    public Bid(int quantity, int face) {
        this.quantity = quantity;
        this.face = face;
    }

    @Override
    public boolean execute(AbstractGameState gs) {
        PerudoGameState state = (PerudoGameState) gs;
        state.setBid(quantity, face, state.getCurrentPlayer());
        return true;
    }

    @Override
    public Bid copy() {
        return this; // immutable
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Bid that)) return false;
        return quantity == that.quantity && face == that.face;
    }

    @Override
    public int hashCode() {
        return 31 * quantity + face + 581207;
    }

    @Override
    public String getString(AbstractGameState gameState) {
        return toString();
    }

    @Override
    public String toString() {
        return "Bid " + describe(quantity, face);
    }

    /**
     * @return a bid as shown to players: "5 x 3", or "3 Pacos"
     */
    public static String describe(int quantity, int face) {
        return face == PACO ? quantity + (quantity == 1 ? " Paco" : " Pacos") : quantity + " x " + face;
    }
}
