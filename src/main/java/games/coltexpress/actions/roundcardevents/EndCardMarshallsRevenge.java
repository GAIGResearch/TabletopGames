package games.coltexpress.actions.roundcardevents;

import core.AbstractGameState;
import core.actions.AbstractAction;
import core.components.Deck;
import core.components.PartialObservableDeck;
import games.coltexpress.ColtExpressGameState;
import games.coltexpress.ColtExpressTypes;
import games.coltexpress.components.Compartment;
import games.coltexpress.components.Loot;
import utilities.Utils;

import java.util.LinkedList;

public class EndCardMarshallsRevenge extends RoundEvent {

    @Override
    public boolean execute(AbstractGameState gs) {
        ColtExpressGameState gameState = (ColtExpressGameState) gs;

        LinkedList<Compartment> train = gameState.getTrainCompartments();
        for (Compartment c : train) {
            if (c.containsMarshal) {
                for (Integer playerID : c.playersOnTopOfCompartment) {
                    Deck<Loot> playerLoot = gameState.getLoot(playerID);
                    Loot leastValuablePurse = null;
                    for (Loot loot : playerLoot.getComponents()) {
                        if (loot.getLootType() == ColtExpressTypes.LootType.Purse &&
                                (leastValuablePurse == null || leastValuablePurse.getValue() < loot.getValue()))
                            leastValuablePurse = loot;
                    }
                    if (leastValuablePurse != null)
                        playerLoot.remove(leastValuablePurse);
                }
                break;
            }
        }
        return true;
    }

    @Override
    public AbstractAction copy() {
        return new EndCardMarshallsRevenge();
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
    public String getString(AbstractGameState gameState) {
        return toString();
    }

    @Override
    public String toString() {
        return "Marshall's Revenge";
    }

    @Override
    public String getEventText() {
        return "All bandits on the roof of the Marshall's car drop their least valuable purse.";
    }
}
