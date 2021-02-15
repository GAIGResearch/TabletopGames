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

    private int cardIdx;
    private int cardId;
    private boolean executed;

    public AddResearchStationWithCard(String city, int cardIdx) {
        super(city);
        this.cardIdx = cardIdx;
    }

    public boolean _execute(AbstractGameState gs) {
        executed = true;
        Deck<Card> playerHand = (Deck<Card>) ((PandemicGameState)gs).getComponentActingPlayer(playerHandHash);
        Deck<Card> discardPile = (Deck<Card>) ((PandemicGameState)gs).getComponent(playerDeckDiscardHash);
        cardId = playerHand.getComponents().get(cardIdx).getComponentID();
        return super._execute(gs) & new DrawCard(playerHand.getComponentID(), discardPile.getComponentID(), cardIdx)._execute(gs);
    }

    public Card getCard(AbstractGameState gs) {
        if (!executed) {
            Deck<Card> deck = (Deck<Card>) ((PandemicGameState)gs).getComponentActingPlayer(playerHandHash);
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
        return "AddResearchStationWithCard{" +
                "toCity='" + city + '\'' +
                ", cardIdx=" + cardIdx +
                '}';
    }

    @Override
    public AbstractAction copy() {
        return new AddResearchStationWithCard(this.city, this.cardIdx);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), cardIdx);
    }
}
