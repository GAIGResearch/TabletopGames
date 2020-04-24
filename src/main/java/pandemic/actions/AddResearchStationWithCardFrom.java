package pandemic.actions;

import actions.Action;
import components.BoardNode;
import components.Card;
import components.Deck;
import content.PropertyBoolean;
import core.GameState;
import pandemic.PandemicGameState;

import static pandemic.Constants.*;


public class AddResearchStationWithCardFrom extends AddResearchStation implements Action {

    private String fromCity;
    private Card card;

    public AddResearchStationWithCardFrom(String from, String to, Card c) {
        super(to);
        this.fromCity = from;
        this.card = c;
    }

    @Override
    public boolean execute(GameState gs) {
        boolean success = super.execute(gs);

        // Discard the card played
        Deck playerHand = (Deck)gs.getAreas().get(gs.getActingPlayer()).getComponent(playerHandHash);
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
