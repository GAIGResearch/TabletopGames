package pandemic.actions;

import actions.IAction;
import components.Card;
import components.Deck;
import core.AbstractGameState;
import pandemic.PandemicGameState;
import turnorder.TurnOrder;

import static pandemic.Constants.playerHandHash;


public class AddResearchStationWithCard extends AddResearchStation implements IAction {

    private Card card;

    public AddResearchStationWithCard(String city, Card c) {
        super(city);
        this.card = c;
    }

    @Override
    public boolean Execute(AbstractGameState gs, TurnOrder turnOrder) {
        super.Execute(gs, turnOrder);
        PandemicGameState pgs = (PandemicGameState)gs;

        // Discard the card played
        Deck playerHand = (Deck)pgs.getComponent(playerHandHash, ((PandemicGameState) gs).getActingPlayer());
        playerHand.discard(card);

        return false;
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
}
