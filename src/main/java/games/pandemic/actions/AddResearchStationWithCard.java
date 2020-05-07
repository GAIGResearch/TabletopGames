package games.pandemic.actions;

import core.actions.IAction;
import core.components.Card;
import core.components.Deck;
import core.AbstractGameState;
import games.pandemic.PandemicGameState;

import static games.pandemic.PandemicConstants.playerDeckDiscardHash;


import static utilities.CoreConstants.playerHandHash;


@SuppressWarnings("unchecked")
public class AddResearchStationWithCard extends AddResearchStation implements IAction {

    private Card card;

    public AddResearchStationWithCard(String city, Card c) {
        super(city);
        this.card = c;
    }

    public boolean execute(AbstractGameState gs) {
        boolean result = super.execute(gs);
        PandemicGameState pgs = (PandemicGameState)gs;

        if (result) {
            // Discard the card played
            Deck<Card> playerHand = (Deck<Card>) pgs.getComponentActingPlayer(playerHandHash);
            playerHand.remove(card);
            Deck<Card> discardDeck = (Deck<Card>) pgs.getComponent(playerDeckDiscardHash);
            result = discardDeck.add(card);
        }

        return result;
    }

    @Override
    public boolean equals(Object other)
    {
        if (this == other) return true;
        if(other instanceof AddResearchStationWithCard)
        {
            AddResearchStationWithCard otherAction = (AddResearchStationWithCard) other;
            return card.equals(otherAction.card);

        }else return false;
    }

    public Card getCard() {
        return card;
    }

    @Override
    public String toString() {
        return "AddResearchStationWithCard{" +
                "card=" + card.toString() +
                ", toCity='" + city + '\'' +
                '}';
    }
}
