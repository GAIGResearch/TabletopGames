package games.coltexpress.actions.roundcardevents;

import core.AbstractGameState;
import core.actions.AbstractAction;
import games.coltexpress.ColtExpressGameState;
import games.coltexpress.components.Compartment;

import java.util.LinkedList;

public class RoundCardSwivelArm extends RoundEvent {

    @Override
    public boolean execute(AbstractGameState gs) {
        ColtExpressGameState cegs = (ColtExpressGameState) gs;

        LinkedList<Compartment> train = ((ColtExpressGameState)gs).getTrainCompartments();
        Compartment caboose = train.get(0);
        for (int i = 1; i < train.size(); i++) {
            Compartment compartment = train.get(i);
            caboose.playersOnTopOfCompartment.addAll(compartment.playersOnTopOfCompartment);
            compartment.playersOnTopOfCompartment.clear();
        }

        return true;
    }

    @Override
    public AbstractAction copy() {
        return new RoundCardSwivelArm();
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
    public String getString(AbstractGameState gameState) {
        return toString();
    }

    @Override
    public String toString() {
        return "Swivel Arm";
    }

    @Override
    public String getEventText() {
        return "All bandits on the roof of the train are swept to the caboose.";
    }
}
