package pandemic.actions;

import actions.Action;
import components.Card;
import components.Deck;
import components.IDeck;
import core.GameState;

import static pandemic.Constants.playerHandHash;


public class MovePlayerWithCard extends MovePlayer implements Action {

    private Card card;

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
            IDeck discardDeck = gs.findDeck("Player Deck Discard");
            result = discardDeck.add(card);
        }

        return result;
    }


    @Override
    public boolean equals(Object other)
    {
        if (this == other) return true;
        if(other instanceof MovePlayerWithCard)
        {
            MovePlayerWithCard otherAction = (MovePlayerWithCard) other;
            return card.equals(otherAction.card);

        }else return false;
    }
}
