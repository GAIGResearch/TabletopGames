package games.coltexpress.actions;

import core.AbstractGameState;
import core.actions.DrawCard;
import games.coltexpress.ColtExpressGameState;
import games.coltexpress.components.Compartment;

public class MoveMarshalAction extends DrawCard {

    private final int sourceCompartment;
    private final int targetCompartment;

    public MoveMarshalAction(int plannedActions, int playerDeck,
                              int sourceCompartment, int targetCompartment){
        super(plannedActions, playerDeck);
        this.sourceCompartment = sourceCompartment;
        this.targetCompartment = targetCompartment;
    }

    @Override
    public boolean execute(AbstractGameState gs) {
        super.execute(gs);

        Compartment source = (Compartment) gs.getComponentById(sourceCompartment);
        Compartment target = (Compartment) gs.getComponentById(targetCompartment);

        source.containsMarshal = false;
        target.containsMarshal = true;
        for (Integer playerID : target.playersInsideCompartment){
            target.playersOnTopOfCompartment.add(playerID);
            ((ColtExpressGameState) gs).addNeutralBullet(playerID);
        }
        target.playersInsideCompartment.clear();
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
        return "MoveMarshal to compartment " + targetCompartment;
    }
}
