package games.loveletter.actions;

import core.AbstractGameState;
import core.actions.AbstractAction;
import core.components.Deck;
import core.interfaces.IPrintable;
import games.loveletter.LoveLetterGameState;
import games.loveletter.cards.LoveLetterCard;

import java.util.Objects;

/**
 * The targeted player discards its current and draws a new one.
 * In case the discarded card is a princess, the targeted player is removed from the game.
 */
public class PrinceAction extends core.actions.DrawCard implements IPrintable {

    private final int opponentID;
    private boolean playerDiscardedPrincess;

    public PrinceAction(int deckFrom, int deckTo, int fromIndex, int opponentID) {
        super(deckFrom, deckTo, fromIndex);
        this.opponentID = opponentID;
        this.playerDiscardedPrincess = false;
    }

    @Override
    public boolean execute(AbstractGameState gs) {
        LoveLetterGameState llgs = (LoveLetterGameState)gs;
        Deck<LoveLetterCard> opponentDeck = llgs.getPlayerHandCards().get(opponentID);
        Deck<LoveLetterCard> opponentDiscardPile = llgs.getPlayerDiscardCards().get(opponentID);
        Deck<LoveLetterCard> drawPile = llgs.getDrawPile();
        playerDiscardedPrincess = false;

        if (((LoveLetterGameState) gs).isNotProtected(opponentID)){
            LoveLetterCard card = opponentDeck.draw();

            // if the discarded card is a princess, the targeted player loses the game
            if (card.cardType == LoveLetterCard.CardType.Princess) {
                ((LoveLetterGameState) gs).killPlayer(opponentID);
                playerDiscardedPrincess = true;
            }else
            {
                // draw a new card from the draw pile.
                // in case the draw pile is empty the targeted player receives the reserve card
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
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof PrinceAction)) return false;
        if (!super.equals(o)) return false;
        PrinceAction that = (PrinceAction) o;
        return opponentID == that.opponentID;
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), opponentID);
    }

    @Override
    public String toString(){
        String str = "Prince - player "+ opponentID + " discards its card and draws a new one.";
        if (playerDiscardedPrincess) str += " Player "+ opponentID + " discarded Princess. ";
        return str;
    }

    @Override
    public String getString(AbstractGameState gameState) {
        String str = "Prince (player " + opponentID + " discards card, draws card).";
        if (playerDiscardedPrincess) str += " Player "+ opponentID + " discarded Princess. ";
        //return "Prince (player " + opponentID + " discards card, draws card)";
        return str;
    }

    @Override
    public void printToConsole(AbstractGameState gameState) {
        System.out.println(toString());
    }

    @Override
    public AbstractAction copy() {
        PrinceAction pa = new PrinceAction(deckFrom, deckTo, fromIndex, opponentID);
        pa.setDiscardedPrincess(playerDiscardedPrincess);
        return pa;
    }

    public void setDiscardedPrincess(boolean discPrincess)
    {
        this.playerDiscardedPrincess = discPrincess;
    }

    public int getOpponentID() {
        return opponentID;
    }
}
