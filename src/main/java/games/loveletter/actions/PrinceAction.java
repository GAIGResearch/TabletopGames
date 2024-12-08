package games.loveletter.actions;

import core.AbstractGameState;
import core.components.Deck;
import core.interfaces.IPrintable;
import games.loveletter.LoveLetterGameState;
import games.loveletter.cards.CardType;
import games.loveletter.cards.LoveLetterCard;

/**
 * The targeted player discards its current and draws a new one.
 * In case the discarded card is a princess, the targeted player is removed from the game.
 */
public class PrinceAction extends PlayCard implements IPrintable {

    public PrinceAction(int cardIdx, int playerID, int opponentID, boolean canExecuteEffect, boolean discard) {
        super(CardType.Prince, cardIdx, playerID, opponentID, null, null, canExecuteEffect, discard);
    }

    @Override
    protected boolean _execute(LoveLetterGameState llgs) {
        Deck<LoveLetterCard> opponentDeck = llgs.getPlayerHandCards().get(targetPlayer);
        Deck<LoveLetterCard> opponentDiscardPile = llgs.getPlayerDiscardCards().get(targetPlayer);
        Deck<LoveLetterCard> drawPile = llgs.getDrawPile();

        LoveLetterCard card = opponentDeck.draw();
        opponentDiscardPile.add(card);

        // if the discarded card is a princess, the targeted player loses the game
        targetCardType = card.cardType;
        if (targetCardType == CardType.Princess) {
            llgs.killPlayer(playerID, targetPlayer, cardType);
            if (llgs.getCoreGameParameters().recordEventHistory) {
                llgs.recordHistory("Player " + targetPlayer + " discards Princess and loses!");
            }
        } else {
            if (llgs.getCoreGameParameters().recordEventHistory) {
                llgs.recordHistory("Player " + targetPlayer + " discards " + card.cardType);
            }

            // draw a new card from the draw pile.
            // in case the draw pile is empty the targeted player receives the reserve card
            LoveLetterCard cardDrawn = drawPile.draw();
            if (cardDrawn == null)
                cardDrawn = llgs.getRemovedCard();
            opponentDeck.add(cardDrawn);
        }

        return true;
    }

    @Override
    public boolean equals(Object o) {
        return super.equals(o) && o instanceof PrinceAction;
    }

    @Override
    public PrinceAction copy() {
        PrinceAction pa = new PrinceAction(cardIdx, playerID, targetPlayer, canExecuteEffect, discard);
        pa.targetCardType = targetCardType;
        return pa;
    }

    @Override
    public String getString(AbstractGameState gameState) {
        return "Prince: p" + targetPlayer;
    }

}
