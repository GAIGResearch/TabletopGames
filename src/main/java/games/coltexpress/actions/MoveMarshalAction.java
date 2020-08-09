package games.coltexpress.actions;

import core.AbstractGameState;
import core.actions.AbstractAction;
import core.actions.DrawCard;
import games.coltexpress.ColtExpressGameState;
import games.coltexpress.components.Compartment;

import java.util.Objects;

public class MoveMarshalAction extends DrawCard {

    private final int sourceCompartment;
    private final int targetCompartment;

    public MoveMarshalAction(int plannedActions, int playerDeck, int cardIdx,
                              int sourceCompartment, int targetCompartment){
        super(plannedActions, playerDeck, cardIdx);

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
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof MoveMarshalAction)) return false;
        if (!super.equals(o)) return false;
        MoveMarshalAction that = (MoveMarshalAction) o;
        return sourceCompartment == that.sourceCompartment &&
                targetCompartment == that.targetCompartment;
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), sourceCompartment, targetCompartment);
    }

    public String toString(){
        return "MoveMarshal to compartment " + targetCompartment;
    }

    @Override
    public String getString(AbstractGameState gameState) {
        Compartment target = (Compartment) gameState.getComponentById(targetCompartment);
        int idx = target.getCompartmentID();
        return "Move marshal to comp=" + idx;
    }

    @Override
    public AbstractAction copy() {
        return new MoveMarshalAction(deckFrom, deckTo, fromIndex, sourceCompartment, targetCompartment);
    }
}
