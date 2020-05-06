package games.pandemic.actions;

import core.actions.IAction;
import core.components.Card;
import core.components.Deck;
import core.AbstractGameState;
import games.pandemic.PandemicGameState;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Objects;

import static games.pandemic.PandemicConstants.playerDeckDiscardHash;
import static games.pandemic.PandemicConstants.playerHandHash;

@SuppressWarnings("unchecked")
public class RearrangeCardsWithCard implements IAction {
    private Deck<Card> deckFrom;
    private int[] newCardOrder;
    private Card card;  // card used to perform this action

    public RearrangeCardsWithCard(Deck<Card> deckFrom, int[] order, Card c) {
        this.deckFrom = deckFrom;
        this.newCardOrder = order;
        this.card = c;
    }

    @Override
    public boolean execute(AbstractGameState gs) {
        PandemicGameState pgs = (PandemicGameState)gs;
        Card[] cards = new Card[newCardOrder.length];
        for (int value : newCardOrder) {
            cards[value] = deckFrom.draw();
        }
        Deck<Card> draws = new Deck<>("Temp Draws from: " + deckFrom.getID());
        draws.setElements(new ArrayList<>(Arrays.asList(cards)));
        boolean result = deckFrom.add(draws);

        if (result) {
            ((Deck<Card>) pgs.getComponentActingPlayer(playerHandHash)).remove(card);
            Deck<Card> discardDeck = (Deck<Card>) pgs.getComponent(playerDeckDiscardHash);
            result = discardDeck.add(card);
        }
        return result;
    }

    public Card getCard() {
        return card;
    }

    public Deck<Card> getDeckFrom() {
        return deckFrom;
    }

    public int[] getNewCardOrder() {
        return newCardOrder;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        RearrangeCardsWithCard that = (RearrangeCardsWithCard) o;
        return Objects.equals(deckFrom, that.deckFrom) &&
                Arrays.equals(newCardOrder, that.newCardOrder) &&
                Objects.equals(card, that.card);
    }

    @Override
    public int hashCode() {
        int result = Objects.hash(deckFrom, card);
        result = 31 * result + Arrays.hashCode(newCardOrder);
        return result;
    }

    @Override
    public String toString() {
        return "RearrangeCardsWithCard{" +
                "deck=" + deckFrom.getID() +
                ", newCardOrder=" + Arrays.toString(newCardOrder) +
                ", card=" + card.toString() +
                '}';
    }
}
