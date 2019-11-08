package actions;

import components.Card;
import content.PropertyString;
import core.GameState;
import utilities.Hash;

import static actions.MovePlayer.playerLocationHash;
import static pandemic.PandemicGameState.playerCardHash;

public class PlayCardFrom implements Action {

    Card card;
    String destination;

    public PlayCardFrom(Card c, String destination) {
        this.card = c;
        this.destination = destination;
    }

    @Override
    public boolean execute(GameState gs) {
        // TODO: execute effect of card

        PropertyString country = (PropertyString) card.getProperty(Hash.GetInstance().hash("country"));
        if (country != null) {
            int activePlayer = gs.getActivePlayer();

            // Tried to play a city card. Moving from this city to the destination, only if current location matches
            // the card played.

            PropertyString name = (PropertyString) card.getProperty(Hash.GetInstance().hash("name"));
            PropertyString currentLocation = (PropertyString) ((Card) gs.getAreas().get(activePlayer).getComponent(playerCardHash)).getProperty(playerLocationHash);

            if (name.equals(currentLocation)) {
                new MovePlayer(activePlayer, destination).execute(gs);
                // TODO discard card
                return true;
            }
        }

        return false;
    }
}
