package games.pandemic.actions;

import core.actions.IAction;
import core.components.Card;
import core.components.Deck;
import core.AbstractGameState;
import games.pandemic.PandemicGameState;

import java.util.Objects;

import static games.pandemic.PandemicConstants.*;
import static utilities.CoreConstants.playerHandHash;


@SuppressWarnings("unchecked")
public class AddResearchStationWithCardFrom extends AddResearchStationFrom implements IAction {

    private String fromCity;
    private Card card;

    public AddResearchStationWithCardFrom(String from, String to, Card c) {
        super(from, to);
        this.fromCity = from;
        this.card = c;
    }

    @Override
    public boolean execute(AbstractGameState gs) {
        boolean success = super.execute(gs);
        PandemicGameState pgs = (PandemicGameState)gs;

        if (success) {
            // Discard the card played
            Deck<Card> playerHand = (Deck<Card>) pgs.getComponentActingPlayer(playerHandHash);
            playerHand.remove(card);
            Deck<Card> discardDeck = (Deck<Card>) pgs.getComponent(playerDeckDiscardHash);
            success = discardDeck.add(card);
        }

        return success;
    }


    @Override
    public boolean equals(Object other)
    {
        if (this == other) return true;
        if(other instanceof AddResearchStationWithCardFrom)
        {
            AddResearchStationWithCardFrom otherAction = (AddResearchStationWithCardFrom) other;
            return fromCity.equals(otherAction.fromCity) &&  card.equals(otherAction.card);

        }else return false;
    }

    @Override
    public String toString() {
        return "AddResearchStationWithCardFrom{" +
                "card=" + card.toString() +
                ", fromCity='" + fromCity + '\'' +
                ", toCity='" + city + '\'' +
                '}';
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), fromCity, card);
    }
}
