package games.loveletter.actions;

import core.AbstractGameState;
import core.components.Deck;
import core.components.PartialObservableDeck;
import core.observations.IPrintable;
import games.loveletter.LoveLetterGameState;
import games.loveletter.cards.LoveLetterCard;

public class PrinceAction extends DrawCard implements IPrintable {

    private final int opponentID;

    public PrinceAction(int deckFrom, int deckTo, int fromIndex, int opponentID) {
        super(deckFrom, deckTo, fromIndex);
        this.opponentID = opponentID;
    }

    @Override
    public boolean execute(AbstractGameState gs) {
        LoveLetterGameState llgs = (LoveLetterGameState)gs;
        int playerID = gs.getTurnOrder().getCurrentPlayer(gs);
        Deck<LoveLetterCard> playerDeck = llgs.getPlayerHandCards().get(playerID);
        PartialObservableDeck<LoveLetterCard> opponentDeck = llgs.getPlayerHandCards().get(opponentID);
        Deck<LoveLetterCard> opponentDiscardPile = llgs.getPlayerDiscardCards().get(opponentID);
        Deck<LoveLetterCard> drawPile = llgs.getDrawPile();

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

        return super.execute(gs);
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
