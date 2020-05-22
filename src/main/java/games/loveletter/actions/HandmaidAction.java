package games.loveletter.actions;

import core.AbstractGameState;
import core.actions.IAction;
import core.components.Card;
import core.components.Deck;
import core.observations.IPrintable;
import games.explodingkittens.actions.PlayCard;
import games.loveletter.LoveLetterGameState;
import games.loveletter.cards.LoveLetterCard;

public class HandmaidAction  extends PlayCard<LoveLetterCard> implements IAction, IPrintable {

    private final int playerID;

    public HandmaidAction(LoveLetterCard card, Deck<LoveLetterCard> playerHand, Deck<LoveLetterCard> discardPile,
                          int ownPlayerID){
        super(card, playerHand, discardPile);
        this.playerID = ownPlayerID;
    }

    @Override
    public boolean execute(AbstractGameState gs) {
        super.execute(gs);

        ((LoveLetterGameState)gs).setProtection(playerID, true);
        return true;
    }

    @Override
    public Card getCard() {
        return null;
    }

    @Override
    public String toString(){
        return "Handmaid - get protection status";
    }

    @Override
    public void printToConsole() {
        System.out.println(toString());
    }
}
