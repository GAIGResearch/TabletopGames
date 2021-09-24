package games.loveletter.actions;

import core.AbstractGameState;
import core.actions.AbstractAction;
import core.interfaces.IExtendedSequence;
import core.interfaces.IPrintable;
import games.loveletter.LoveLetterGameState;
import games.loveletter.cards.LoveLetterCard;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * At the beginning of each round the player draws a card and loses its protection status.
 */
public class DrawCard extends AbstractAction implements IPrintable, IExtendedSequence {
    boolean cardDrawn;
    int cardIdxChosen;
    int playerID;

    public DrawCard(int playerID) {
        this.playerID = playerID;
        this.cardIdxChosen = -1;
    }

    public DrawCard(int playerID, int cardIdx) {
        this.cardIdxChosen = cardIdx;
        this.playerID = playerID;
    }

    @Override
    public boolean execute(AbstractGameState gs) {
        LoveLetterGameState llgs = (LoveLetterGameState)gs;
        // Player is no longer protected
        llgs.setProtection(playerID, false);
        if (this.cardIdxChosen == -1) {
            gs.setActionInProgress(this);
            return true;
        } else {
            // draw card with index for player
            LoveLetterCard card = llgs.getDrawPile().pick(cardIdxChosen);
            if (card != null) {
                llgs.getPlayerHandCards().get(playerID).add(card);
                llgs.setGamePhase(LoveLetterGameState.LoveLetterGamePhase.Draw);
                cardDrawn = true;
                return true;
            }
            return false;
        }
    }

    @Override
    public String getString(AbstractGameState gameState) {
        return "Draw card";
    }

    @Override
    public String toString() {
        return "Draw a card and remove protection status.";
    }

    @Override
    public void printToConsole(AbstractGameState gameState) {
        System.out.println(this);
    }

    @Override
    public List<AbstractAction> _computeAvailableActions(AbstractGameState state) {
        List<AbstractAction> drawCardActions = new ArrayList<>();
        for (int i = 0; i < ((LoveLetterGameState)state).getDrawPile().getSize(); i++) {
            drawCardActions.add(new DrawCard(playerID, i));
        }
        return drawCardActions;
    }

    @Override
    public int getCurrentPlayer(AbstractGameState state) {
        return -1; // Ask Game Master what card to draw
    }

    @Override
    public void registerActionTaken(AbstractGameState state, AbstractAction action) {
        cardDrawn = true;
    }

    @Override
    public boolean executionComplete(AbstractGameState state) {
        return cardDrawn;
    }

    @Override
    public DrawCard copy() {
        DrawCard dc = new DrawCard(playerID, cardIdxChosen);
        dc.cardDrawn = cardDrawn;
        return dc;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        DrawCard drawCard = (DrawCard) o;
        return cardDrawn == drawCard.cardDrawn && cardIdxChosen == drawCard.cardIdxChosen && playerID == drawCard.playerID;
    }

    @Override
    public int hashCode() {
        return Objects.hash(cardDrawn, cardIdxChosen, playerID);
    }
}
