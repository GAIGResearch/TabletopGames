package games.pandemic.actions;

import core.AbstractGameState;
import core.actions.AbstractAction;
import core.components.Card;
import core.components.Deck;
import games.pandemic.PandemicGameState;

import java.util.Objects;

import static games.pandemic.PandemicConstants.playerDeckDiscardHash;
import static core.CoreConstants.playerHandHash;

@SuppressWarnings("unchecked")
public class RemoveCardWithCard extends AbstractAction {
    private Deck<Card> deck;
    private Card card; // card to be discarded from player hand after this action is executed
    private int removeCard;

    public RemoveCardWithCard(Deck<Card> deck, int discardCard, Card card) {
        this.deck = deck;
        this.removeCard = discardCard;
        this.card = card;
    }


    @Override
    public boolean execute(AbstractGameState gs) {
        PandemicGameState pgs = (PandemicGameState)gs;
        deck.remove(removeCard); // card removed from the game

        // Discard other card from player hand
        Deck<Card> discardDeck = (Deck<Card>) pgs.getComponent(playerDeckDiscardHash);
        Deck<Card> playerHand = (Deck<Card>) pgs.getComponentActingPlayer(playerHandHash);
        playerHand.remove(card);
        discardDeck.add(card);
        return true;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        RemoveCardWithCard that = (RemoveCardWithCard) o;
        return removeCard == that.removeCard &&
                Objects.equals(deck, that.deck) &&
                Objects.equals(card, that.card);
    }

    @Override
    public int hashCode() {
        return Objects.hash(deck, card, removeCard);
    }

    public Card getCard() {
        return card;
    }

    public Deck<Card> getDeck() {
        return deck;
    }

    public int getRemoveCard() {
        return removeCard;
    }

    @Override
    public String toString() {
        return "RemoveCardWithCard{" +
                "deck=" + deck.getComponentName() +
                ", card=" + card.toString() +
                ", removeCard=" + removeCard +
                '}';
    }
}
