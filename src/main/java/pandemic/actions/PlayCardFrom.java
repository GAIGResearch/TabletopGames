package pandemic.actions;

import actions.Action;
import components.Card;
import content.PropertyString;
import core.GameState;
import utilities.Hash;

import pandemic.Constants;

public class PlayCardFrom implements Action {

    private Card card;
    private String destination;

    public PlayCardFrom(Card c, String destination) {
        this.card = c;
        this.destination = destination;
    }

    @Override
    public boolean execute(GameState gs) {
        // TODO: execute effect of card

        PropertyString country = (PropertyString) card.getProperty(Hash.GetInstance().hash("country"));
        if (country != null) {
            int activePlayer = gs.getActingPlayer();

            // Tried to play a city card. Moving from this city to the destination, only if current location matches
            // the card played.

            PropertyString name = (PropertyString) card.getProperty(Hash.GetInstance().hash("name"));
            PropertyString currentLocation = (PropertyString) ((Card) gs.getAreas().get(activePlayer).getComponent(Constants.playerCardHash)).getProperty(Constants.playerLocationHash);

            if (name.equals(currentLocation)) {
                new MovePlayer(activePlayer, destination).execute(gs);
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
