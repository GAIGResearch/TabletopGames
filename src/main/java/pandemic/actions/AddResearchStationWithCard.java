package pandemic.actions;

import actions.Action;
import components.Card;
import components.Deck;
import core.GameState;

import static pandemic.Constants.playerHandHash;


public class AddResearchStationWithCard extends AddResearchStation implements Action {

    Card card;

    public AddResearchStationWithCard(String city, Card c) {
        super(city);
        this.card = c;
    }

    @Override
    public boolean execute(GameState gs) {
        super.execute(gs);

        // Discard the card played
        Deck playerHand = (Deck)gs.getAreas().get(gs.getActingPlayer()).getComponent(playerHandHash);
        playerHand.discard(card);

        return false;
    }
}
