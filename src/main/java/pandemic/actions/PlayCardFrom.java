package pandemic.actions;

import actions.IAction;
import components.Card;
import content.PropertyString;
import core.AbstractGameState;
import pandemic.PandemicGameState;
import turnorder.TurnOrder;
import utilities.Hash;

import pandemic.Constants;

public class PlayCardFrom implements IAction {

    private Card card;
    private String destination;

    public PlayCardFrom(Card c, String destination) {
        this.card = c;
        this.destination = destination;
    }

    @Override
    public boolean Execute(AbstractGameState gs, TurnOrder turnOrder) {
        // TODO: execute effect of card
        PandemicGameState pgs = (PandemicGameState)gs;

        PropertyString country = (PropertyString) card.getProperty(Hash.GetInstance().hash("country"));
        if (country != null) {
            int activePlayer = ((PandemicGameState) gs).getActingPlayer();

            // Tried to play a city card. Moving from this city to the destination, only if current location matches
            // the card played.

            PropertyString name = (PropertyString) card.getProperty(Hash.GetInstance().hash("name"));
            PropertyString currentLocation = (PropertyString) ((Card) pgs.getComponent(Constants.playerCardHash, activePlayer)).getProperty(Constants.playerLocationHash);

            if (name.equals(currentLocation)) {
                new MovePlayer(activePlayer, destination).Execute(gs, null);
                // TODO discard card
                return true;
            }
        }

        return false;
    }


    @Override
    public boolean equals(Object other)
    {
        if (this == other) return true;
        if(other instanceof PlayCardFrom)
        {
            PlayCardFrom otherAction = (PlayCardFrom) other;
            return card.equals(otherAction.card) && destination.equals(otherAction.destination);

        }else return false;
    }
}
