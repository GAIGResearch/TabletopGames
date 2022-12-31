package games.sirius.actions;

import core.AbstractGameState;
import core.actions.AbstractAction;
import core.components.Deck;
import games.sirius.*;

import java.util.*;
import java.util.stream.Collectors;

import static games.sirius.SiriusConstants.SiriusCardType.CONTRABAND;
import static java.util.stream.Collectors.toList;

public class SellCards extends AbstractAction {

    public final SiriusConstants.SiriusCardType salesType;
    int[] saleValues;

    public SellCards(List<SiriusCard> cardsToSell) {
        // we check that all cards are of the same type
        if (cardsToSell.isEmpty())
            throw new IllegalArgumentException("Must specify at least one card to sell");
        salesType = cardsToSell.get(0).cardType;
        if (cardsToSell.stream().anyMatch(c -> c.cardType != salesType))
            throw new IllegalArgumentException("All cards must have the same type to sell");
        int glowingContrabandCards = (int) cardsToSell.stream().filter(c -> c.cardType == CONTRABAND && c.value == 0).count();
        int glowingContrabandSets = glowingContrabandCards / 3;
        int glowingContrabandSurplus = glowingContrabandCards % 3;
        // We now remove the surplus from cards to Sell
        if (glowingContrabandSurplus > 0) {
            List<SiriusCard> cardsToRemove = cardsToSell.stream()
                    .filter(c -> c.cardType == CONTRABAND && c.value == 0)
                    .limit(glowingContrabandSurplus)
                    .collect(toList());
            cardsToSell.removeAll(cardsToRemove);
        }
        saleValues = cardsToSell.stream().mapToInt(c -> c.value).sorted().toArray();
        // then check for glowing contraband - for each set of 3 we have a value of 10
        for (int gc = 0; gc < glowingContrabandSets; gc++) {
            for (int i = 0; i < saleValues.length; i++)
                if (saleValues[i] == 0) {
                    saleValues[i] = 10;
                    break;
                }
        }
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

            cardToSell.ifPresent(c -> state.sellCard(c, saleValue));
        }
        return true;
    }

    public int getTotalValue() {
        return Arrays.stream(saleValues).sum();
    }

    public int getTotalCards() {return saleValues.length;}

    private Optional<SiriusCard> getMatchingCard(Deck<SiriusCard> hand, int v) {
        int val = v == 10 ? 0 : v; // to cater for Glowing Contraband
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
