package games.seasaltpaper.actions;

import core.AbstractGameState;
import core.actions.AbstractAction;
import core.components.Deck;
import core.interfaces.IExtendedSequence;
import games.seasaltpaper.SeaSaltPaperGameState;
import games.seasaltpaper.cards.SeaSaltPaperCard;

import java.util.ArrayList;
import java.util.List;



// Sequence of Draw 2 from draw pile then discard 1 to one of the discard piles
public class DrawAndDiscard extends AbstractAction implements IExtendedSequence {

    static final int NUMBER_OF_CARDS_DRAWN = 2; // TODO move this to parameter class
    enum Step {
        DRAW,
        DISCARD,
        DONE;
    }

    final int playerId;
    int[] drawnCardsId;
    Step currentStep = Step.DRAW;

    public DrawAndDiscard(int playerId)
    {
        this.playerId = playerId;
        drawnCardsId = new int[NUMBER_OF_CARDS_DRAWN];
    }

    @Override
    public boolean execute(AbstractGameState gs) {
        DrawMultiple drawMultiple = new DrawMultiple(NUMBER_OF_CARDS_DRAWN, playerId);
        if (!(drawMultiple.execute(gs))) {
            return false;
        }
        drawnCardsId = drawMultiple.getDrawnCardsId();
        currentStep = Step.DISCARD;
        gs.setActionInProgress(this);
        return true;
    }

    @Override
    public List<AbstractAction> _computeAvailableActions(AbstractGameState state) {
        List<AbstractAction> actions = new ArrayList<>();
        SeaSaltPaperGameState sspgs = (SeaSaltPaperGameState) state;
        if (currentStep == Step.DRAW) { // TODO move this to execute()
            //actions.add(new DrawMultiple(NUMBER_OF_CARDS_DRAWN, playerId));
        }
        else if (currentStep == Step.DISCARD)
        {
            boolean emptyDiscardPile = false;
            List<Integer> discardPiles = new ArrayList<>();
            // TODO Make this more general for any number of discard piles
            if (sspgs.getDiscardPile2().getSize() == 0 && sspgs.getDiscardPile1().getSize() == 0) {
                System.out.println("BOTH DISCARD PILES ARE EMPTY!!!!");
                discardPiles.add(sspgs.getDiscardPile1().getComponentID());
//                discardPiles.add(sspgs.getDiscardPile2().getComponentID());
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
        if (currentStep == Step.DISCARD) {
            currentStep = Step.DONE;
        }
    }

    @Override
    public boolean executionComplete(AbstractGameState state) { return currentStep == Step.DONE; }

    @Override
    public DrawAndDiscard copy() {
        return this;
    }

    @Override
    public boolean equals(Object obj) {
        return this == obj;
    }

    @Override
    public int hashCode() {
        return 0;
    }

    @Override
    public String getString(AbstractGameState gameState) {
        return "Draw 2 from the DrawPile then discard 1 to one of the discard pile.";
    }
}
