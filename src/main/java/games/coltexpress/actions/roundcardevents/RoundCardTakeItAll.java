package games.coltexpress.actions.roundcardevents;

import core.AbstractGameState;
import core.actions.AbstractAction;
import games.coltexpress.ColtExpressGameState;
import games.coltexpress.ColtExpressParameters;
import games.coltexpress.components.Compartment;
import games.coltexpress.components.Loot;

import java.util.LinkedList;

public class RoundCardTakeItAll extends AbstractAction {

    @Override
    public boolean execute(AbstractGameState gs) {
        LinkedList<Compartment> train = ((ColtExpressGameState)gs).getTrainCompartments();
        for (Compartment c : train) {
            if (c.containsMarshal) {
                ColtExpressParameters.LootType strongBox = ColtExpressParameters.LootType.Strongbox;
                c.lootInside.add(new Loot(strongBox, strongBox.getDefaultValue()));
                break;
            }
        }
        return true;
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
    public String toString() {
        return "Take It All";
    }
}
