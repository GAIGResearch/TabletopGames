package games.pandemic.actions;

import core.actions.AbstractAction;
import core.actions.DrawCard;
import core.components.Card;
import core.components.Deck;
import core.AbstractGameState;
import games.pandemic.PandemicGameState;

import java.util.Objects;

import static games.pandemic.PandemicConstants.*;
import static core.CoreConstants.playerHandHash;


@SuppressWarnings("unchecked")
public class AddResearchStationWithCardFrom extends AddResearchStationFrom {

    private final int deckFrom, deckTo, cardIdx;
    private int cardId;
    private boolean executed;

    public AddResearchStationWithCardFrom(String from, String to, int deckFrom, int deckTo, int cardIdx) {
        super(from, to);
        this.cardIdx = cardIdx;
        this.deckFrom = deckFrom;
        this.deckTo = deckTo;
    }

    @Override
    public boolean execute(AbstractGameState gs) {
        executed = true;
        Deck<Card> from = (Deck<Card>) gs.getComponentById(deckFrom);
        cardId = from.getComponents().get(cardIdx).getComponentID();
        return super.execute(gs) & new DrawCard(deckFrom, deckTo, cardIdx).execute(gs);
    }

    public Card getCard(AbstractGameState gs) {
        if (!executed) {
            if (cardIdx == -1) return null;
            Deck<Card> from = (Deck<Card>) gs.getComponentById(deckFrom);
            return from.getComponents().get(cardIdx);
        }
        return (Card) gs.getComponentById(cardId);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof AddResearchStationWithCardFrom)) return false;
        if (!super.equals(o)) return false;
        AddResearchStationWithCardFrom that = (AddResearchStationWithCardFrom) o;
        return cardIdx == that.cardIdx;
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), cardIdx);
    }

    @Override
    public AbstractAction copy() {
        return new AddResearchStationWithCardFrom(this.fromCity, this.city, this.deckFrom, this.deckTo, this.cardIdx);
    }

    @Override
    public String toString() {
        return "Add Research Station in " + city + " from " + fromCity + " with card";
    }
}
