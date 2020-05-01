package pandemic.actions;

import actions.IAction;
import components.BoardNode;
import components.Card;
import components.Deck;
import content.PropertyBoolean;
import core.AbstractGameState;
import pandemic.PandemicGameState;
import turnorder.TurnOrder;

import static pandemic.Constants.*;


public class AddResearchStationWithCardFrom extends AddResearchStation implements IAction {

    private String fromCity;
    private Card card;

    public AddResearchStationWithCardFrom(String from, String to, Card c) {
        super(to);
        this.fromCity = from;
        this.card = c;
    }

    @Override
    public boolean Execute(AbstractGameState gs, TurnOrder turnOrder) {
        boolean success = super.Execute(gs, turnOrder);
        PandemicGameState pgs = (PandemicGameState)gs;

        // Discard the card played
        Deck playerHand = (Deck)pgs.getComponent(playerHandHash, ((PandemicGameState) gs).getActingPlayer());
        playerHand.discard(card);

        // Remove research station from "fromCity" location
        BoardNode bn = ((PandemicGameState)gs).world.getNode(nameHash, fromCity);
        if (bn != null) {
            bn.setProperty(researchStationHash, new PropertyBoolean(false));
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
