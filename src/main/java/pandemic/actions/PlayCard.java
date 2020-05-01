package pandemic.actions;

import actions.IAction;
import components.Card;
import content.PropertyString;
import core.AbstractGameState;
import pandemic.Constants;
import pandemic.PandemicGameState;
import turnorder.TurnOrder;
import utilities.Hash;

public class PlayCard implements IAction {

    private Card card;

    public PlayCard(Card c) {
        this.card = c;
    }

    // TODO: generify potentially as (conditions -> effect), where effect is another action (or set of actions?)
    @Override
    public boolean Execute(AbstractGameState gs, TurnOrder turnOrder) {
        // TODO: execute effect of card
        PandemicGameState pgs = (PandemicGameState)gs;

        PropertyString country = (PropertyString) card.getProperty(Hash.GetInstance().hash("country"));
        if (country != null) {
            // Tried to play a city card.
            int activePlayer = ((PandemicGameState) gs).getActingPlayer();
            PropertyString name = (PropertyString) card.getProperty(Hash.GetInstance().hash("name"));
            PropertyString currentLocation = (PropertyString) ((Card) pgs.getComponent(Constants.playerCardHash, activePlayer)).getProperty(Constants.playerLocationHash);

            if (name.equals(currentLocation)) {
                // Trying to build a research station here.
                new AddResearchStation(name.value).Execute(gs, null);  // TODO: where to take it from
                // TODO discard card
            } else {
                // Trying to move to different location.
                new MovePlayer(activePlayer, name.value).Execute(gs, null);
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
