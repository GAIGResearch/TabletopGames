package games.loveletter.actions;

import core.AbstractGameState;
import core.CoreConstants;
import core.components.Deck;
import core.interfaces.IPrintable;
import games.loveletter.LoveLetterGameState;
import games.loveletter.cards.LoveLetterCard;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * The targeted player discards its current and draws a new one.
 * In case the discarded card is a princess, the targeted player is removed from the game.
 */
public class PrinceAction extends PlayCard implements IPrintable {

    private LoveLetterCard.CardType cardDiscarded;

    public PrinceAction(int playerID, int opponentID) {
        super(LoveLetterCard.CardType.Prince, playerID, opponentID, null, null);
    }

    @Override
    public boolean execute(AbstractGameState gs) {
        LoveLetterGameState llgs = (LoveLetterGameState)gs;
        Deck<LoveLetterCard> opponentDeck = llgs.getPlayerHandCards().get(targetPlayer);
        Deck<LoveLetterCard> opponentDiscardPile = llgs.getPlayerDiscardCards().get(targetPlayer);
        Deck<LoveLetterCard> drawPile = llgs.getDrawPile();

        LoveLetterCard card = opponentDeck.draw();
        opponentDiscardPile.add(card);

        // if the discarded card is a princess, the targeted player loses the game
        cardDiscarded = card.cardType;
        if (card.cardType == LoveLetterCard.CardType.Princess) {
            ((LoveLetterGameState) gs).killPlayer(targetPlayer);
            if (llgs.getCoreGameParameters().recordEventHistory) {
                llgs.recordHistory("Player " + targetPlayer + " discards Princess and loses!");
            }
        } else
        {
            if (llgs.getCoreGameParameters().recordEventHistory) {
                llgs.recordHistory("Player " + targetPlayer + " discards " + card.cardType);
            }

            // draw a new card from the draw pile.
            // in case the draw pile is empty the targeted player receives the reserve card
            LoveLetterCard cardDrawn = drawPile.draw();
            if (cardDrawn == null)
                cardDrawn = ((LoveLetterGameState)gs).getRemovedCard();
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
        return cardDiscarded == that.cardDiscarded;
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), cardDiscarded);
    }

    @Override
    public String toString(){
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
        PrinceAction pa = new PrinceAction(playerID, targetPlayer);
        pa.cardDiscarded = cardDiscarded;
        return pa;
    }

    public static List<? extends PlayCard> generateActions(LoveLetterGameState gs, int playerID) {
        List<PlayCard> cardActions = new ArrayList<>();
        for (int targetPlayer = 0; targetPlayer < gs.getNPlayers(); targetPlayer++) {
            if (gs.getPlayerResults()[targetPlayer] == CoreConstants.GameResult.LOSE || gs.isProtected(targetPlayer))
                continue;
            cardActions.add(new PrinceAction(playerID, targetPlayer));
        }
        return cardActions;
    }
}
