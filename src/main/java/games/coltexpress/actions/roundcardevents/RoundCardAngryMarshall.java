package games.coltexpress.actions.roundcardevents;

import core.AbstractGameState;
import core.actions.AbstractAction;
import games.coltexpress.ColtExpressGameState;
import games.coltexpress.components.Compartment;

import java.util.LinkedList;

public class RoundCardAngryMarshall extends RoundEvent {

    @Override
    public boolean execute(AbstractGameState gs) {
        ColtExpressGameState gameState = (ColtExpressGameState) gs;

        LinkedList<Compartment> train = gameState.getTrainCompartments();
        for (int i = 0; i < train.size(); i++){
            Compartment c = train.get(i);
            if (c.containsMarshal){
                for (Integer playerID : c.playersOnTopOfCompartment)
                    gameState.addNeutralBullet(playerID);
                if (i > 0){
                    c.containsMarshal = false;
                    train.get(i-1).containsMarshal = true;
                }
            }
        }
        return true;
    }

    @Override
    public AbstractAction copy() {
        return new RoundCardAngryMarshall();
    }

    @Override
    public boolean equals(Object obj) {
        return obj instanceof RoundCardAngryMarshall;
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
        return "Angry Marshall";
    }

    @Override
    public String getEventText() {
        return "The Marshall shoots all bandits on the roof of his car and then moves one car toward the caboose.";
    }
}
