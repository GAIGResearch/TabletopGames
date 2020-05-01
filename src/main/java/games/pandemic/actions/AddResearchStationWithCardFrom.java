package games.pandemic.actions;

import core.actions.IAction;
import core.components.BoardNode;
import core.components.Card;
import core.components.Deck;
import core.content.PropertyBoolean;
import core.AbstractGameState;
import games.pandemic.PandemicGameState;

import static games.pandemic.Constants.*;


@SuppressWarnings("unchecked")
public class AddResearchStationWithCardFrom extends AddResearchStation implements IAction {

    private String fromCity;
    private Card card;

    public AddResearchStationWithCardFrom(String from, String to, Card c) {
        super(to);
        this.fromCity = from;
        this.card = c;
    }

    @Override
    public boolean execute(AbstractGameState gs) {
        boolean success = super.execute(gs);
        PandemicGameState pgs = (PandemicGameState)gs;

        if (success) {
            // Remove research station from "fromCity" location
            BoardNode bn = ((PandemicGameState) gs).world.getNode(nameHash, fromCity);
            if (bn != null) {
                bn.setProperty(researchStationHash, new PropertyBoolean(false));

                // Discard the card played
                Deck<Card> playerHand = (Deck<Card>) pgs.getComponent(playerHandHash, pgs.getActingPlayerID());
                playerHand.discard(card);
                Deck<Card> discardDeck = (Deck<Card>) pgs.getComponent(playerDeckDiscardHash);
                success = discardDeck.add(card);
            } else {
                success = false;
            }
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
}
