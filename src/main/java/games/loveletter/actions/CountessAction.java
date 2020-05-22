package games.loveletter.actions;

import core.AbstractGameState;
import core.actions.IAction;
import core.components.Card;
import core.components.Deck;
import core.observations.IPrintable;
import games.explodingkittens.actions.PlayCard;
import games.loveletter.cards.LoveLetterCard;

public class CountessAction extends PlayCard<LoveLetterCard> implements IAction, IPrintable {

    public CountessAction(LoveLetterCard card, Deck<LoveLetterCard> playerHand, Deck<LoveLetterCard> discardPile){
        super(card, playerHand, discardPile);
    }

    @Override
    public boolean execute(AbstractGameState gs) {
        super.execute(gs);
        return false;
    }

    @Override
    public Card getCard() {
        return null;
    }


    @Override
    public String toString(){
        return "Countess - needs to be discarded if the player also holds King or Prince";
    }
    @Override
    public void printToConsole() {
        System.out.println(toString());
    }
}
