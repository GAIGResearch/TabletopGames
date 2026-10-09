package games.euchre.actions;

import core.AbstractGameState;
import core.actions.AbstractAction;
import core.components.FrenchCard;
import games.euchre.EuchreGameState;
import games.euchre.EuchreParameters;

import java.util.Objects;

/**
 * Choose the suit as trumps, making the current player the maker, and optionally go alone.
 */
public class CallTrump extends AbstractAction {

    public final FrenchCard.Suite suit;
    public final boolean alone;

    public CallTrump(FrenchCard.Suite suit, boolean alone) {
        this.suit = suit;
        this.alone = alone;
    }

    @Override
    public boolean execute(AbstractGameState gs) {
        EuchreGameState state = (EuchreGameState) gs;
        // in the first round the suit is the up-card's, and the dealer takes the up-card, unless the dealer is
        // sitting out and EuchreParameters.sittingOutDealerPicksUp is false
        boolean firstRound = state.isFirstBiddingRound();
        state.setTrumps(suit, state.getCurrentPlayer(), alone);
        boolean dealerPlays = state.getSittingOut() != state.getDealer();
        if (firstRound && (dealerPlays || ((EuchreParameters) state.getGameParameters()).sittingOutDealerPicksUp))
            state.getPlayerHand(state.getDealer()).add(state.getKitty().draw());
        return true;
    }

    @Override
    public CallTrump copy() {
        return this; // immutable
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof CallTrump that && suit == that.suit && alone == that.alone;
    }

    @Override
    public int hashCode() {
        return Objects.hash(suit.ordinal(), alone) + 562307;
    }

    @Override
    public String getString(AbstractGameState gameState) {
        return toString();
    }

    @Override
    public String toString() {
        return "Call " + suit + (alone ? " alone" : "");
    }
}
