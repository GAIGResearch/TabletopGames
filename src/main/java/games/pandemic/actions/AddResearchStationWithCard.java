package games.pandemic.actions;

import core.actions.AbstractAction;
import core.actions.DrawCard;
import core.components.Card;
import core.components.Deck;
import core.AbstractGameState;
import games.pandemic.PandemicGameState;

import java.util.Objects;

import static games.pandemic.PandemicConstants.playerDeckDiscardHash;
import static core.CoreConstants.playerHandHash;


public class AddResearchStationWithCard extends AddResearchStation {

    private final int deckFrom, deckTo, cardIdx;
    private int cardId;
    private boolean executed;

    public AddResearchStationWithCard(String city, int deckFrom, int deckTo, int cardIdx) {
        super(city);
        this.cardIdx = cardIdx;
        this.deckFrom = deckFrom;
        this.deckTo = deckTo;
    }

    public boolean execute(AbstractGameState gs) {
        executed = true;
        Deck<Card> deck = (Deck<Card>) gs.getComponentById(deckFrom);
        cardId = deck.getComponents().get(cardIdx).getComponentID();
        return super.execute(gs) & new DrawCard(deckFrom, deckTo, cardIdx).execute(gs);
    }

    public Card getCard(AbstractGameState gs) {
        if (!executed) {
            if (cardIdx == -1) return null;
            Deck<Card> deck = (Deck<Card>) gs.getComponentById(deckFrom);
            return deck.getComponents().get(cardIdx);
        }
        return (Card) gs.getComponentById(cardId);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof AddResearchStationWithCard)) return false;
        if (!super.equals(o)) return false;
        AddResearchStationWithCard that = (AddResearchStationWithCard) o;
        return cardIdx == that.cardIdx;
    }

    @Override
    public String toString() {
        return "Add Research Station in " + city + " with card";
    }

    @Override
    public AbstractAction copy() {
        return new AddResearchStationWithCard(this.city, deckFrom, deckTo, this.cardIdx);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), cardIdx);
    }
}
