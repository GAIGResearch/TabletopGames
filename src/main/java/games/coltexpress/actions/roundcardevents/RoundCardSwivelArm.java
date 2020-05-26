package games.coltexpress.actions.roundcardevents;

import core.AbstractGameState;
import core.actions.AbstractAction;
import games.coltexpress.ColtExpressGameState;
import games.coltexpress.components.Compartment;

import java.util.LinkedList;

public class RoundCardSwivelArm extends AbstractAction {

    @Override
    public boolean execute(AbstractGameState gs) {
        LinkedList<Compartment> train = ((ColtExpressGameState)gs).getTrainCompartments();
        Compartment caboose = train.get(0);
        for (Compartment compartment : train) {
            caboose.playersOnTopOfCompartment.addAll(compartment.playersOnTopOfCompartment);
            compartment.playersOnTopOfCompartment.clear();
        }
        return true;
    }

    @Override
    public boolean equals(Object obj) {
        return obj instanceof RoundCardSwivelArm;
    }

    @Override
    public int hashCode() {
        return 0;
    }

    @Override
    public String toString() {
        return "Swivel Arm";
    }
}
