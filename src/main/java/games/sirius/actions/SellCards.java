package games.sirius.actions;

import core.AbstractGameState;
import core.actions.AbstractAction;
import core.components.Deck;
import games.sirius.*;

import java.util.*;

public class SellCards extends AbstractAction {

    SiriusConstants.SiriusCardType salesType;
    int[] saleValues;

    public SellCards(List<SiriusCard> cardsToSell) {
        // we check that all cards are of the same type
        if (cardsToSell.isEmpty())
            throw new IllegalArgumentException("Must specify at least one card to sell");
        salesType = cardsToSell.get(0).cardType;
        if (cardsToSell.stream().anyMatch(c -> c.cardType != salesType))
            throw new IllegalArgumentException("All cards must have the same type to sell");
        saleValues = cardsToSell.stream().mapToInt(c -> c.value).sorted().toArray();
    }

    @Override
    public boolean execute(AbstractGameState gs) {
        SiriusGameState state = (SiriusGameState) gs;
        // We sell each card in turn
        Deck<SiriusCard> hand = state.getPlayerHand(gs.getCurrentPlayer());
        for (int saleValue : saleValues) {
            Optional<SiriusCard> cardToSell = getMatchingCard(hand, saleValue);
            if (!cardToSell.isPresent())
                throw new AssertionError("Card not found : " + salesType + " " + saleValue);

            cardToSell.ifPresent(state::sellCard);
        }
        return true;
    }

    private Optional<SiriusCard> getMatchingCard(Deck<SiriusCard> hand, int val) {
        return hand.stream().filter(c -> c.cardType == salesType && c.value == val).findFirst();
    }

    @Override
    public AbstractAction copy() {
        return this; //immutable
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof SellCards) {
            SellCards other = (SellCards) obj;
            return other.salesType == salesType && Arrays.equals(other.saleValues, saleValues);
        }
        return false;
    }

    @Override
    public int hashCode() {
        return Objects.hash(salesType.ordinal()) + 31 * Arrays.hashCode(saleValues) + 82;
    }

    @Override
    public String toString() {
        return String.format("Sell %s : %s", salesType, Arrays.toString(saleValues));
    }
    @Override
    public String getString(AbstractGameState gameState) {
        return toString();
    }
}
