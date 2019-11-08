package actions;

import components.Card;
import content.PropertyString;
import core.GameState;
import utilities.Hash;

import static actions.MovePlayer.playerLocationHash;
import static pandemic.PandemicGameState.playerCardHash;

public class PlayCard implements Action {

    Card card;

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
            int activePlayer = gs.getActivePlayer();
            PropertyString name = (PropertyString) card.getProperty(Hash.GetInstance().hash("name"));
            PropertyString currentLocation = (PropertyString) ((Card) gs.getAreas().get(activePlayer).getComponent(playerCardHash)).getProperty(playerLocationHash);

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
}
