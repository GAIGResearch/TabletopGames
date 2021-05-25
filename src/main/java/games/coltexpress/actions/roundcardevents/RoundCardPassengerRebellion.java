package games.coltexpress.actions.roundcardevents;

import core.AbstractGameState;
import core.actions.AbstractAction;
import games.coltexpress.ColtExpressGameState;
import games.coltexpress.components.Compartment;

import java.util.LinkedList;

public class RoundCardPassengerRebellion extends RoundEvent {

    @Override
    public boolean execute(AbstractGameState gs) {
        ColtExpressGameState gameState = (ColtExpressGameState) gs;
        LinkedList<Compartment> train = gameState.getTrainCompartments();
        for (Compartment c : train) {
            for (Integer playerID : c.playersInsideCompartment) {
                gameState.addNeutralBullet(playerID);
            }
        }
        return true;
    }

    @Override
    public AbstractAction copy() {
        return new RoundCardPassengerRebellion();
    }

    @Override
    public boolean equals(Object obj) {
        return obj instanceof RoundCardPassengerRebellion;
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
        return "Passenger Rebellion";
    }

    @Override
    public String getEventText() {
        return "All bandits in the train receive one Neutral Bullet card.";
    }
}
