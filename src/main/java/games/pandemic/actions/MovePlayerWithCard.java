package games.pandemic.actions;

import core.actions.IAction;
import core.components.Card;
import core.components.Deck;
import core.AbstractGameState;
import games.pandemic.PandemicGameState;

import static games.pandemic.PandemicConstants.playerDeckDiscardHash;
import static games.pandemic.PandemicConstants.playerHandHash;


@SuppressWarnings("unchecked")
public class MovePlayerWithCard extends MovePlayer implements IAction {

    private Card card;

    public MovePlayerWithCard(int playerIdx, String city, Card c) {
        super(playerIdx, city);
        this.card = c;
    }

    @Override
    public boolean execute(AbstractGameState gs) {
        boolean result = super.execute(gs);
        PandemicGameState pgs = (PandemicGameState)gs;

        if (result) {
            // Discard the card played
            Deck<Card> playerHand = (Deck<Card>) pgs.getComponentActingPlayer(playerHandHash);
            playerHand.remove(card);
            Deck<Card> discardDeck = (Deck<Card>) pgs.getComponent(playerDeckDiscardHash);
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
