package games.sirius.actions;

import core.AbstractGameState;
import core.actions.AbstractAction;
import games.sirius.SiriusCard;
import games.sirius.SiriusConstants;
import games.sirius.SiriusGameState;
import games.sirius.SiriusTurnOrder;

import static games.sirius.SiriusConstants.SiriusCardType.FAVOUR;

public class FavourForRank extends AbstractAction {

    public final int newRank;

    public FavourForRank(int newRank) {
        this.newRank = newRank;
    }

    @Override
    public boolean execute(AbstractGameState gs) {
        SiriusGameState state = (SiriusGameState) gs;
        SiriusTurnOrder sto = (SiriusTurnOrder) state.getTurnOrder();
        int player = state.getCurrentPlayer();
        sto.setRank(player, newRank);
        SiriusCard favourCard = state.getPlayerHand(player).stream()
                .filter(c -> c.cardType == FAVOUR).findFirst()
                .orElseThrow(() -> new AssertionError("No Favour card available"));
        state.getPlayerHand(player).remove(favourCard);
        return true;
    }

    @Override
    public AbstractAction copy() {
        return this; // immutable
    }

    @Override
    public boolean equals(Object obj) {
        return obj instanceof FavourForRank && ((FavourForRank) obj).newRank == newRank;
    }

    @Override
    public int hashCode() {
        return 303 + 98324 * newRank;
    }

    @Override
    public String toString() {
        return "Change rank to " + newRank;
    }

    @Override
    public String getString(AbstractGameState gameState) {
        return toString();
    }
}
