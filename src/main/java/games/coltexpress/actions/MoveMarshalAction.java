package games.coltexpress.actions;

import core.AbstractGameState;
import core.components.PartialObservableDeck;
import games.coltexpress.ColtExpressGameState;
import games.coltexpress.cards.ColtExpressCard;
import games.coltexpress.components.Compartment;

public class MoveMarshalAction extends ColtExpressExecuteCardAction {

    private final Compartment sourceCompartment;
    private final Compartment targetCompartment;

    public MoveMarshalAction(ColtExpressCard card, PartialObservableDeck<ColtExpressCard> plannedActions,
                              PartialObservableDeck<ColtExpressCard> playerDeck,
                              Compartment sourceCompartment, Compartment targetCompartment){
        super(card, plannedActions, playerDeck);
        this.sourceCompartment = sourceCompartment;
        this.targetCompartment = targetCompartment;
    }

    @Override
    public boolean execute(AbstractGameState gs) {
        super.execute(gs);

        sourceCompartment.containsMarshal = false;
        targetCompartment.containsMarshal = true;
        for (Integer playerID : targetCompartment.playersInsideCompartment){
            targetCompartment.playersOnTopOfCompartment.add(playerID);
            ((ColtExpressGameState) gs).addNeutralBullet(playerID);
        }
        targetCompartment.playersInsideCompartment.clear();
        return false;
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
        return "MoveMarshal to compartment " + targetCompartment.id;
    }
}
