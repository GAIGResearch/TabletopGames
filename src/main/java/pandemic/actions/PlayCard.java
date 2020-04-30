package pandemic.actions;

import actions.Action;
import components.Card;
import content.PropertyString;
import core.GameState;
import pandemic.Constants;
import utilities.Hash;

public class PlayCard implements Action {

    private Card card;

    public PlayCard(Card c) {
        this.card = c;
    }

    // TODO: generify potentially as (conditions -> effect), where effect is another action (or set of actions?)
    @Override
    public boolean execute(GameState gs) {
        // TODO: execute effect of card

        PropertyString country = (PropertyString) card.getProperty(Hash.GetInstance().hash("country"));
        if (country != null) {
            // Tried to play a city card.
            int activePlayer = gs.getActingPlayer().a;
            PropertyString name = (PropertyString) card.getProperty(Hash.GetInstance().hash("name"));
            PropertyString currentLocation = (PropertyString) ((Card) gs.getAreas().get(activePlayer).getComponent(Constants.playerCardHash)).getProperty(Constants.playerLocationHash);

            if (name.equals(currentLocation)) {
                // Trying to build a research station here.
                new AddResearchStation(name.value).execute(gs);  // TODO: where to take it from
                // TODO discard card
            } else {
                // Trying to move to different location.
                new MovePlayer(activePlayer, name.value).execute(gs);
                // TODO discard card
                return true;
            }
        } else {
            PropertyString effect = (PropertyString) card.getProperty(Hash.GetInstance().hash("effect"));
            if (effect != null) {
                // This is an event card!
                // TODO: resolve effect and discard card
                return true;
            }
        }

        return false;
    }


    @Override
    public boolean equals(Object other)
    {
        if (this == other) return true;
        if(other instanceof PlayCard)
        {
            PlayCard otherAction = (PlayCard) other;
            return card.equals(otherAction.card);

        }else return false;
    }
}
