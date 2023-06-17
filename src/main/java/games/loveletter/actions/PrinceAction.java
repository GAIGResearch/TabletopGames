package games.loveletter.actions;

import core.AbstractGameState;
import core.CoreConstants;
import core.components.Deck;
import core.interfaces.IPrintable;
import games.loveletter.LoveLetterGameState;
import games.loveletter.cards.LoveLetterCard;

import java.util.ArrayList;
import java.util.List;

/**
 * The targeted player discards its current and draws a new one.
 * In case the discarded card is a princess, the targeted player is removed from the game.
 */
public class PrinceAction extends PlayCard implements IPrintable {

    private transient LoveLetterCard.CardType cardDiscarded;
    // Not part of state; purely used for logging purposes

    public PrinceAction(int playerID, int opponentID, boolean canExecuteEffect) {
        super(LoveLetterCard.CardType.Prince, playerID, opponentID, null, null, canExecuteEffect);
    }

    @Override
    protected boolean _execute(LoveLetterGameState llgs) {
        Deck<LoveLetterCard> opponentDeck = llgs.getPlayerHandCards().get(targetPlayer);
        Deck<LoveLetterCard> opponentDiscardPile = llgs.getPlayerDiscardCards().get(targetPlayer);
        Deck<LoveLetterCard> drawPile = llgs.getDrawPile();

        LoveLetterCard card = opponentDeck.draw();
        opponentDiscardPile.add(card);

        // if the discarded card is a princess, the targeted player loses the game
        cardDiscarded = card.cardType;
        if (cardDiscarded == LoveLetterCard.CardType.Princess) {
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
        if (this == o) return true;
        if (!(o instanceof PrinceAction)) return false;
        return super.equals(o);
    }

    @Override
    public String _toString(){
        return "Prince (" + targetPlayer + " discards " + (cardDiscarded != null? cardDiscarded : "card") + " and draws a new card)";
    }

    @Override
    public String getString(AbstractGameState gameState) {
        return toString();
    }

    @Override
    public void printToConsole(AbstractGameState gameState) {
        System.out.println(this);
    }

    @Override
    public PrinceAction copy() {
        PrinceAction pa = new PrinceAction(playerID, targetPlayer, canExecuteEffect);
        pa.cardDiscarded = cardDiscarded;
        return pa;
    }

    public static List<? extends PlayCard> generateActions(LoveLetterGameState gs, int playerID) {
        List<PlayCard> cardActions = new ArrayList<>();
        for (int targetPlayer = 0; targetPlayer < gs.getNPlayers(); targetPlayer++) {
            if (gs.getPlayerResults()[targetPlayer] == CoreConstants.GameResult.LOSE_ROUND || gs.isProtected(targetPlayer))
                continue;
            cardActions.add(new PrinceAction(playerID, targetPlayer, true));
        }
        if (cardActions.size() == 0) cardActions.add(new PrinceAction(playerID, -1, false));
        return cardActions;
    }
}
