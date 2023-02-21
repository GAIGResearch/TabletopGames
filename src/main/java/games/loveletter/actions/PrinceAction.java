package games.loveletter.actions;

import core.AbstractGameState;
import core.components.Deck;
import core.interfaces.IPrintable;
import games.loveletter.LoveLetterGameState;
import games.loveletter.cards.LoveLetterCard;

import java.util.Objects;

/**
 * The targeted player discards its current and draws a new one.
 * In case the discarded card is a princess, the targeted player is removed from the game.
 */
public class PrinceAction extends PlayCard implements IPrintable {

    private final int opponentID;
    private LoveLetterCard.CardType cardDiscarded;

    public PrinceAction(int fromIndex, int playerID, int opponentID) {
        super(fromIndex, playerID);
        this.opponentID = opponentID;
    }

    @Override
    public boolean execute(AbstractGameState gs) {
        LoveLetterGameState llgs = (LoveLetterGameState)gs;
        Deck<LoveLetterCard> opponentDeck = llgs.getPlayerHandCards().get(opponentID);
        Deck<LoveLetterCard> opponentDiscardPile = llgs.getPlayerDiscardCards().get(opponentID);
        Deck<LoveLetterCard> drawPile = llgs.getDrawPile();

        LoveLetterCard card = opponentDeck.draw();
        opponentDiscardPile.add(card);

        // if the discarded card is a princess, the targeted player loses the game
        cardDiscarded = card.cardType;
        if (card.cardType == LoveLetterCard.CardType.Princess) {
            ((LoveLetterGameState) gs).killPlayer(opponentID);
            if (llgs.getCoreGameParameters().recordEventHistory) {
                llgs.recordHistory("Player " + opponentID + " discards Princess and loses!");
            }
        } else
        {
            if (llgs.getCoreGameParameters().recordEventHistory) {
                llgs.recordHistory("Player " + opponentID + " discards " + card.cardType);
            }

            // draw a new card from the draw pile.
            // in case the draw pile is empty the targeted player receives the reserve card
            LoveLetterCard cardDrawn = drawPile.draw();
            if (cardDrawn == null)
                cardDrawn = ((LoveLetterGameState)gs).getReserveCard();
            opponentDeck.add(cardDrawn);
        }

        return super.execute(gs);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof PrinceAction)) return false;
        if (!super.equals(o)) return false;
        PrinceAction that = (PrinceAction) o;
        return playerID == that.playerID && opponentID == that.opponentID && cardDiscarded == that.cardDiscarded;
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), playerID, opponentID, cardDiscarded);
    }

    @Override
    public String toString(){
        return "Prince (" + opponentID + " discards " + (cardDiscarded != null? cardDiscarded : "card") + " and draws a new card)";
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
        PrinceAction pa = new PrinceAction(fromIndex, playerID, opponentID);
        pa.cardDiscarded = cardDiscarded;
        return pa;
    }

    public int getOpponentID() {
        return opponentID;
    }
}
