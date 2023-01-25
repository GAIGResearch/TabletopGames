package games.sirius.actions;

import core.AbstractGameState;
import core.actions.AbstractAction;
import core.components.Deck;
import games.sirius.*;

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
        Deck<SiriusCard> hand = state.getPlayerHand(player);
        for (int i = 0; i < hand.getSize(); i++) {
            if (hand.get(i).cardType == FAVOUR) {
                hand.remove(i);
                state.setActionTaken("Favour", player);
                return true;
            }
        }
        throw new AssertionError("No Favour card available");
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
