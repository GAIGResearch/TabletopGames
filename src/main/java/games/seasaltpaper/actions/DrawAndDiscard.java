package games.seasaltpaper.actions;

import core.AbstractGameState;
import core.actions.AbstractAction;
import core.interfaces.IExtendedSequence;
import games.seasaltpaper.SeaSaltPaperGameState;
import games.seasaltpaper.SeaSaltPaperParameters;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;


// Sequence of Draw 2 from draw pile then discard 1 to one of the discard piles
public class DrawAndDiscard extends AbstractAction implements IExtendedSequence {

    enum Step {
        DRAW,
        DISCARD,
        DONE
    }

    final int playerId, howManyDraw, howManyDiscard;

    Step currentStep = Step.DRAW;
    int[] drawnCardsId;

    public DrawAndDiscard(int playerId, int howManyDraw, int howManyDiscard)
    {
        this.playerId = playerId;
        this.howManyDraw = howManyDraw;
        this.howManyDiscard = howManyDiscard;
    }

    @Override
    public boolean execute(AbstractGameState gs) {
        SeaSaltPaperGameState sspgs = (SeaSaltPaperGameState) gs;
        int n = Math.min(sspgs.getDrawPile().getSize(), howManyDraw);
        DrawMultiple drawMultiple = new DrawMultiple(n, playerId);
        if (!(drawMultiple.execute(gs))) {
            return false;
        }
        drawnCardsId = drawMultiple.getDrawnCardsId();
        if (drawnCardsId != null && drawnCardsId.length == 1) { // No discard if only one card drawn
            currentStep = Step.DONE;
            return true;
        }
        currentStep = Step.DISCARD;
        gs.setActionInProgress(this);
        return true;
    }

    @Override
    public List<AbstractAction> _computeAvailableActions(AbstractGameState state) {
        List<AbstractAction> actions = new ArrayList<>();
        SeaSaltPaperGameState sspgs = (SeaSaltPaperGameState) state;
        if (currentStep == Step.DISCARD)
        {
            boolean emptyDiscardPile = false;
            List<Integer> discardPiles = new ArrayList<>();
            // TODO Make this more general for any number of discard piles
            if (sspgs.getDiscardPile2().getSize() == 0 && sspgs.getDiscardPile1().getSize() == 0) {
                discardPiles.add(sspgs.getDiscardPile1().getComponentID());
            }
            else if (sspgs.getDiscardPile1().getSize() == 0) {
                discardPiles.add(sspgs.getDiscardPile1().getComponentID());
            }
            else if (sspgs.getDiscardPile2().getSize() == 0)
            {
                discardPiles.add(sspgs.getDiscardPile2().getComponentID());
            }
            else
            {
                discardPiles.add(sspgs.getDiscardPile1().getComponentID());
                discardPiles.add(sspgs.getDiscardPile2().getComponentID());
            }

            // Actions for discarding each card into each pile
            for (int cardID : drawnCardsId) {
                if (cardID == -1) {
//                    assert false;
                    continue;
                }
                for (int pileID: discardPiles) {
                    actions.add(new Discard(cardID, pileID, playerId));
                }
            }
        }
        return actions;
    }

    @Override
    public int getCurrentPlayer(AbstractGameState state) {
        return playerId;
    }

    @Override
    public void _afterAction(AbstractGameState state, AbstractAction action) {
        // Discard howManyDiscard cards
        if (howManyDiscard == 0) {
            currentStep = Step.DONE;
        }
        else if (currentStep == Step.DISCARD) {
            Discard d = (Discard) action;
            int discardCount = 0;
            for (int i = 0; i < drawnCardsId.length; i++) {
                if (drawnCardsId[i] == d.discardCardId) {
                    drawnCardsId[i] = -1;
                }
                if (drawnCardsId[i] == -1) {
                    discardCount += 1;
                }
            }
            if (discardCount == howManyDiscard || discardCount == drawnCardsId.length) {
                currentStep = Step.DONE;
            }
        }
    }

    @Override
    public boolean executionComplete(AbstractGameState state) { return currentStep == Step.DONE; }

    @Override
    public DrawAndDiscard copy() {
        DrawAndDiscard c = new DrawAndDiscard(playerId, howManyDraw, howManyDiscard);
        if (drawnCardsId != null) {
            c.drawnCardsId = drawnCardsId.clone();
        }
        c.currentStep = currentStep;
        return c;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        DrawAndDiscard that = (DrawAndDiscard) o;
        return playerId == that.playerId && Arrays.equals(drawnCardsId, that.drawnCardsId) && currentStep == that.currentStep && howManyDraw == that.howManyDraw && howManyDiscard == that.howManyDiscard;
    }

    @Override
    public int hashCode() {
        int result = Objects.hash(playerId, currentStep, howManyDraw, howManyDiscard);
        result = 31 * result + Arrays.hashCode(drawnCardsId);
        return result;
    }

    @Override
    public String getString(AbstractGameState gameState) {
        return toString();
    }

    @Override
    public String toString() {
        return "Draw " + howManyDraw + " from deck and discard " + howManyDiscard;
    }
}
