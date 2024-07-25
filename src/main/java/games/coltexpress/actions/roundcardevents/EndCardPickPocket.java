package games.coltexpress.actions.roundcardevents;

import core.AbstractGameState;
import core.actions.AbstractAction;
import games.coltexpress.ColtExpressGameState;
import games.coltexpress.ColtExpressTypes;
import games.coltexpress.components.Compartment;
import games.coltexpress.components.Loot;

import java.util.LinkedList;

public class EndCardPickPocket extends RoundEvent {

    @Override
    public boolean execute(AbstractGameState gs) {
        ColtExpressGameState gameState = (ColtExpressGameState) gs;

        LinkedList<Compartment> train = gameState.getTrainCompartments();
        for (Compartment currentCompartment : train) {
            if (currentCompartment.playersInsideCompartment.size() == 1) {
                LinkedList<Loot> purses = new LinkedList<>();
                for (Loot loot : currentCompartment.lootInside.getComponents()) {
                    if (loot.getLootType() == ColtExpressTypes.LootType.Purse)
                        purses.add(loot);
                }
                if (purses.size() > 0) {
                    for (Integer playerID : currentCompartment.playersInsideCompartment)
                        gameState.addLoot(playerID, purses.get(gameState.getRnd().nextInt(purses.size())));
                }
            }

            if (currentCompartment.playersOnTopOfCompartment.size() == 1) {
                LinkedList<Loot> purses = new LinkedList<>();
                for (Loot loot : currentCompartment.lootOnTop.getComponents()) {
                    if (loot.getLootType() == ColtExpressTypes.LootType.Purse)
                        purses.add(loot);
                }
                if (purses.size() > 0) {
                    for (Integer playerID : currentCompartment.playersOnTopOfCompartment)
                        gameState.addLoot(playerID, purses.get(gameState.getRnd().nextInt(purses.size())));
                }
            }
        }
        return true;
    }

    @Override
    public AbstractAction copy() {
        return new EndCardPickPocket();
    }

    @Override
    public boolean equals(Object obj) {
        return obj instanceof EndCardPickPocket;
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
        return "Pick Pocket";
    }

    @Override
    public String getEventText() {
        return "Any bandit alone in or on a car can pick up a purse if there is one.";
    }
}
