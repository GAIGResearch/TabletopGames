package pandemic.actions;

import actions.Action;
import components.Card;
import components.Deck;
import core.GameState;

import static pandemic.Constants.playerHandHash;


public class MovePlayerWithCard extends MovePlayer implements Action {

    Card card;

    public MovePlayerWithCard(int playerIdx, String city, Card c) {
        super(playerIdx, city);
        this.card = c;
    }

    @Override
    public boolean execute(GameState gs) {
        boolean result = super.execute(gs);

        if (result) {
            // Discard the card played
            Deck playerHand = (Deck) gs.getAreas().get(gs.getActingPlayer()).getComponent(playerHandHash);
            playerHand.discard(card);
            Deck discardDeck = gs.findDeck("Player Deck Discard");
            result = discardDeck.add(card);
        }

        return result;
    }
}
