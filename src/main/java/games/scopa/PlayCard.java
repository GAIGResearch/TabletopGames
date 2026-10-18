package games.scopa;

import core.AbstractGameState;
import core.actions.AbstractAction;
import core.components.Deck;
import core.components.TarotCard;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

/**
 * The current player plays a card from their hand, capturing the given cards from the table with it; with no cards to
 * capture, the card is added to the table.
 */
public class PlayCard extends AbstractAction {

    // the order in which captured cards are held, so that the same capture given in any order is the same action
    static final Comparator<TarotCard> ORDER = Comparator.comparing((TarotCard c) -> c.suit).thenComparingInt(c -> c.number);

    public final TarotCard card;
    public final List<TarotCard> captured;

    public PlayCard(TarotCard card, List<TarotCard> captured) {
        this.card = card;
        List<TarotCard> sorted = new ArrayList<>(captured);
        sorted.sort(ORDER);
        this.captured = List.copyOf(sorted);
    }

    /**
     * Plays the card to the table, capturing nothing.
     */
    public PlayCard(TarotCard card) {
        this(card, List.of());
    }

    public boolean isCapture() {
        return !captured.isEmpty();
    }

    @Override
    public boolean execute(AbstractGameState gs) {
        ScopaGameState state = (ScopaGameState) gs;
        int player = state.getCurrentPlayer();
        state.playerHands.get(player).remove(card);  // throws if the player does not hold the card
        if (!isCapture()) {
            state.table.add(card);
            return true;
        }
        Deck<TarotCard> pile = state.capturedCards.get(player);
        for (TarotCard c : captured) {
            state.table.remove(c);  // throws if the card is not on the table
            pile.add(c);
        }
        pile.add(card);
        state.lastCapturer = player;
        // sweeping the table is a scopa, except with the last card of the deal
        if (state.table.getSize() == 0 && !state.isDealOver())
            state.scopas[player]++;
        return true;
    }

    @Override
    public PlayCard copy() {
        return this; // immutable
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof PlayCard that && card.equals(that.card) && captured.equals(that.captured);
    }

    @Override
    public int hashCode() {
        return Objects.hash(card, captured) + 518203;
    }

    @Override
    public String getString(AbstractGameState gameState) {
        return toString();
    }

    @Override
    public String toString() {
        if (captured.isEmpty())
            return "Play " + card.getComponentName() + " to the table";
        StringBuilder sb = new StringBuilder("Play " + card.getComponentName() + ", capture ");
        for (int i = 0; i < captured.size(); i++) {
            if (i > 0) sb.append(" + ");
            sb.append(captured.get(i).getComponentName());
        }
        return sb.toString();
    }
}
