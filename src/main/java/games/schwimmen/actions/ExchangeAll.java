package games.schwimmen.actions;

import core.AbstractGameState;
import core.actions.AbstractAction;
import core.components.Deck;
import core.components.FrenchCard;
import core.components.PartialObservableDeck;
import games.schwimmen.SchwimmenGameState;
import games.schwimmen.SchwimmenUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * The current player exchanges their whole hand with the table.
 */
public class ExchangeAll extends AbstractAction {

    @Override
    public boolean execute(AbstractGameState gs) {
        SchwimmenGameState state = (SchwimmenGameState) gs;
        PartialObservableDeck<FrenchCard> hand = state.getPlayerHand(state.getCurrentPlayer());
        Deck<FrenchCard> table = state.getTable();
        List<FrenchCard> oldHand = new ArrayList<>(hand.getComponents());
        hand.clear();
        // every player saw the cards taken from the table
        boolean[] visibleToAll = SchwimmenUtils.visibleToAll(state.getNPlayers());
        for (FrenchCard card : table.getComponents())
            hand.add(card, hand.getSize(), visibleToAll);
        table.clear();
        for (FrenchCard card : oldHand)
            table.addToBottom(card);
        return true;
    }

    @Override
    public ExchangeAll copy() {
        return this;
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof ExchangeAll;
    }

    @Override
    public int hashCode() {
        return 734111;
    }

    @Override
    public String getString(AbstractGameState gameState) {
        return "Exchange all";
    }

    @Override
    public String toString() {
        return "Exchange all";
    }
}
