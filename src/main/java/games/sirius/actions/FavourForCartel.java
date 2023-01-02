package games.sirius.actions;

import core.AbstractGameState;
import core.actions.AbstractAction;
import core.components.Deck;
import games.sirius.*;

import static games.sirius.SiriusConstants.SiriusCardType.FAVOUR;

public class FavourForCartel extends AbstractAction {

    public final int cartelLocation;

    public FavourForCartel(int moon) {
        cartelLocation = moon;
    }

    @Override
    public boolean execute(AbstractGameState gs) {
        SiriusGameState state = (SiriusGameState) gs;
        int player = state.getCurrentPlayer();
        Moon moon = state.getMoon(cartelLocation);
        if (moon.getMoonType() == SiriusConstants.MoonType.TRADING)
            throw new IllegalArgumentException("Cannot have a Cartel on Sirius");
        moon.setCartelOwner(state.getCurrentPlayer());
        Deck<SiriusCard> hand = state.getPlayerHand(player);
        for (int i = 0; i < hand.getSize(); i++) {
            if (hand.get(i).cardType == FAVOUR) {
                hand.remove(i);
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
        return obj instanceof FavourForCartel && ((FavourForCartel) obj).cartelLocation == cartelLocation;
    }

    @Override
    public int hashCode() {
        return 30983 + 31 * cartelLocation;
    }

    @Override
    public String toString() {
        return "Favour to gain cartel at " + cartelLocation;
    }

    @Override
    public String getString(AbstractGameState gameState) {
        SiriusGameState state = (SiriusGameState) gameState;
        return "Gain Cartel at " + state.getMoon(cartelLocation);
    }
}
