package games.loveletter.actions;

import core.AbstractGameState;
import core.actions.IAction;
import core.components.Card;
import core.components.Deck;
import core.components.IDeck;
import core.observations.IPrintable;
import games.explodingkittens.actions.PlayCard;
import games.loveletter.LoveLetterGameState;
import games.loveletter.cards.LoveLetterCard;

public class PrinceAction extends PlayCard<LoveLetterCard> implements IAction, IPrintable {

    private final Deck<LoveLetterCard> opponentDeck;
    private final Deck<LoveLetterCard> drawPile;
    private final int opponentID;
    private final Deck<LoveLetterCard> opponentDiscardPile;

    public PrinceAction(LoveLetterCard card, IDeck<LoveLetterCard> playerHand, IDeck<LoveLetterCard> discardPile,
                       Deck<LoveLetterCard> opponentDeck, int opponentID, Deck<LoveLetterCard> drawPile,
                       Deck<LoveLetterCard> opponentDiscardPile){
        super(card, playerHand, discardPile);
        this.opponentDeck = opponentDeck;
        this.drawPile = drawPile;
        this.opponentID = opponentID;
        this.opponentDiscardPile = opponentDiscardPile;
    }

    @Override
    public boolean execute(AbstractGameState gs) {
        super.execute(gs);
        if (((LoveLetterGameState) gs).isNotProtected(opponentID)){
            LoveLetterCard card = opponentDeck.draw();

            if (card.cardType == LoveLetterCard.CardType.Princess)
                ((LoveLetterGameState)gs).killPlayer(opponentID);
            else
            {
                opponentDiscardPile.add(card);
                LoveLetterCard cardDrawn = drawPile.draw();
                if (cardDrawn == null)
                    cardDrawn = ((LoveLetterGameState)gs).getReserveCard();
                opponentDeck.add(cardDrawn);
            }

        }

        return false;
    }

    @Override
    public Card getCard() {
        return null;
    }

    @Override
    public String toString(){
        return "Prince - player "+ opponentID + " discards its card and draws a new one";
    }

    @Override
    public void printToConsole() {
        System.out.println(toString());
    }
}
