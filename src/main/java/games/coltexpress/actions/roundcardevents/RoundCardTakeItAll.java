package games.coltexpress.actions.roundcardevents;

import core.AbstractGameState;
import core.actions.AbstractAction;
import games.coltexpress.ColtExpressGameState;
import games.coltexpress.ColtExpressParameters;
import games.coltexpress.ColtExpressTypes;
import games.coltexpress.components.Compartment;
import games.coltexpress.components.Loot;

import java.util.LinkedList;

public class RoundCardTakeItAll extends RoundEvent {

    @Override
    public boolean execute(AbstractGameState gs) {
        LinkedList<Compartment> train = ((ColtExpressGameState)gs).getTrainCompartments();
        int reward = ((ColtExpressParameters)gs.getGameParameters()).nCardTakeItAllReward;
        for (Compartment c : train) {
            if (c.containsMarshal) {
                ColtExpressTypes.LootType strongBox = ColtExpressTypes.LootType.Strongbox;
                c.lootInside.add(new Loot(strongBox, reward));
                break;
            }
        }
        return true;
    }

    @Override
    public AbstractAction copy() {
        return new RoundCardTakeItAll();
    }

    @Override
    public boolean equals(Object obj) {
        return obj instanceof RoundCardTakeItAll;
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
        return "Take It All";
    }

    @Override
    public String getEventText() {
        return "The Marshall drops a second strongbox.";
    }
}
