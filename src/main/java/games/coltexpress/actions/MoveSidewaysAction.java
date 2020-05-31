package games.coltexpress.actions;

import core.AbstractGameState;
import core.components.PartialObservableDeck;
import games.coltexpress.ColtExpressGameState;
import games.coltexpress.cards.ColtExpressCard;
import games.coltexpress.components.Compartment;

public class MoveSidewaysAction extends ColtExpressExecuteCardAction {

    private final Compartment targetArea;
    private final Compartment sourceArea;

    public MoveSidewaysAction(ColtExpressCard card, PartialObservableDeck<ColtExpressCard> plannedActions,
                              PartialObservableDeck<ColtExpressCard> playerDeck,
                              Compartment sourceArea, Compartment targetArea){
        super(card, plannedActions, playerDeck);
        this.targetArea = targetArea;
        this.sourceArea = sourceArea;
    }

    @Override
    public boolean execute(AbstractGameState gs) {
        super.execute(gs);
        if (sourceArea.playersInsideCompartment.contains(card.playerID)){
            sourceArea.playersInsideCompartment.remove(card.playerID);
            if (targetArea.containsMarshal){
                ((ColtExpressGameState) gs).addNeutralBullet(card.playerID);
                targetArea.playersOnTopOfCompartment.add(card.playerID);
            }
            else
                targetArea.playersInsideCompartment.add(card.playerID);
        }
        else{
            sourceArea.playersOnTopOfCompartment.remove(card.playerID);
            targetArea.playersOnTopOfCompartment.add(card.playerID);
        }

        return true;
    }

    @Override
    public boolean equals(Object obj) {
        throw new UnsupportedOperationException();
        //return false;
    }

    @Override
    public int hashCode() {
        throw new UnsupportedOperationException();
    }

    public String toString(){
        return "MoveSideways; player " + card.playerID;
    }
}
