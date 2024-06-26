package games.coltexpress.actions.roundcardevents;

import core.AbstractGameState;
import core.actions.AbstractAction;
import games.coltexpress.ColtExpressGameState;
import games.coltexpress.components.Compartment;

import java.util.LinkedList;

public class RoundCardBraking extends RoundEvent {

    @Override
    public boolean execute(AbstractGameState gs) {
        LinkedList<Compartment> train = ((ColtExpressGameState)gs).getTrainCompartments();
        Compartment targetCompartment = train.get(train.size()-1);
        Compartment sourceCompartment;
        for (int i = train.size()-2; i >= 0; i--){
            sourceCompartment = train.get(i);
            targetCompartment.playersOnTopOfCompartment.addAll(sourceCompartment.playersOnTopOfCompartment);
            sourceCompartment.playersOnTopOfCompartment.clear();
            targetCompartment = sourceCompartment;
        }
        return true;
    }

    @Override
    public AbstractAction copy() {
        return new RoundCardBraking();
    }

    @Override
    public boolean equals(Object obj) {
        return obj instanceof RoundCardBraking;
    }

    @Override
    public int hashCode() {
        return 0;
    }

    @Override
    public String getString(AbstractGameState gameState) {
        return toString();
    }

    @Override
    public String toString() {
        return null;
    }

    @Override
    public String getEventText() {
        return "All bandits on the roof of the train move one car toward the locomotive.";
    }
}
