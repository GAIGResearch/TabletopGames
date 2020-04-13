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

    String fromCity;
    Card card;

    public AddResearchStationWithCardFrom(String from, String to, Card c) {
        super(to);
        this.fromCity = from;
        this.card = c;
    }

    @Override
    public boolean execute(GameState gs) {
        boolean success = super.execute(gs);

        // Discard the card played
        Deck playerHand = (Deck)gs.getAreas().get(gs.getActivePlayer()).getComponent(playerHandHash);
        playerHand.discard(card);

        // Remove research station from "fromCity" location
        BoardNode bn = ((PandemicGameState)gs).world.getNode(nameHash, fromCity);
        if (bn != null) {
            bn.setProperty(researchStationHash, new PropertyBoolean(false));
        }

        return success;
    }
}
