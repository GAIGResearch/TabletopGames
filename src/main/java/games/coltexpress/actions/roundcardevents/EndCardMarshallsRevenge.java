package games.coltexpress.actions.roundcardevents;

import core.AbstractGameState;
import core.actions.AbstractAction;
import core.components.PartialObservableDeck;
import games.coltexpress.ColtExpressGameState;
import games.coltexpress.ColtExpressParameters;
import games.coltexpress.components.Compartment;
import games.coltexpress.components.Loot;

import java.util.LinkedList;

public class EndCardMarshallsRevenge extends AbstractAction {

    @Override
    public boolean execute(AbstractGameState gs) {
        ColtExpressGameState gameState = (ColtExpressGameState) gs;

        LinkedList<Compartment> train = gameState.getTrainCompartments();
        for (int i = 0; i < train.size(); i++){
            Compartment c = train.get(i);
            if (c.containsMarshal){
                for (Integer playerID : c.playersOnTopOfCompartment){
                    PartialObservableDeck<Loot> playerLoot = gameState.getLoot(playerID);
                    Loot lestValueablePurse = null;
                    for (Loot loot : playerLoot.getComponents()) {
                        if (loot.getLootType() == ColtExpressParameters.LootType.Purse &&
                                (lestValueablePurse == null || lestValueablePurse.getValue() < loot.getValue()))
                            lestValueablePurse = loot;
                    }
                    if (lestValueablePurse != null)
                        playerLoot.remove(lestValueablePurse);
                }
                break;
            }
        }

        gameState.endGame();
        return true;
    }

    @Override
    public boolean equals(Object obj) {
        return obj instanceof EndCardMarshallsRevenge;
    }

    @Override
    public int hashCode() {
        return 0;
    }

    @Override
    public String toString() {
        return "Marshall's Revenge";
    }
}
