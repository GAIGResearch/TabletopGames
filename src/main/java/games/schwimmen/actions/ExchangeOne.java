package games.schwimmen.actions;

import core.AbstractGameState;
import core.actions.AbstractAction;
import core.components.Deck;
import core.components.FrenchCard;
import core.components.PartialObservableDeck;
import games.schwimmen.SchwimmenGameState;
import games.schwimmen.SchwimmenUtils;

import java.util.Objects;

/**
 * The current player exchanges one card of their hand for one card on the table.
 */
public class ExchangeOne extends AbstractAction {

    public final FrenchCard handCard;
    public final FrenchCard tableCard;

    public ExchangeOne(FrenchCard handCard, FrenchCard tableCard) {
        this.handCard = handCard;
        this.tableCard = tableCard;
    }

    @Override
    public boolean execute(AbstractGameState gs) {
        SchwimmenGameState state = (SchwimmenGameState) gs;
        PartialObservableDeck<FrenchCard> hand = state.getPlayerHand(state.getCurrentPlayer());
        Deck<FrenchCard> table = state.getTable();
        int handIndex = hand.getComponents().indexOf(handCard);
        int tableIndex = table.getComponents().indexOf(tableCard);
        hand.remove(handIndex);
        // every player saw the card taken from the table
        hand.add(tableCard, handIndex, SchwimmenUtils.visibleToAll(state.getNPlayers()));
        table.setComponent(tableIndex, handCard);
        return true;
    }

    @Override
    public ExchangeOne copy() {
        return this;
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof ExchangeOne other && other.handCard.equals(handCard) && other.tableCard.equals(tableCard);
    }

    @Override
    public int hashCode() {
        return Objects.hash(handCard, tableCard) + 734107;
    }

    @Override
    public String getString(AbstractGameState gameState) {
        return toString();
    }

    @Override
    public String toString() {
        return "Exchange " + handCard + " for " + tableCard;
    }
}
