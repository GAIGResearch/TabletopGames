package games.coltexpress.actions.roundcardevents;

import core.AbstractGameState;
import core.actions.AbstractAction;
import games.coltexpress.ColtExpressGameState;
import games.coltexpress.ColtExpressParameters;
import games.coltexpress.components.Compartment;
import games.coltexpress.components.Loot;
import utilities.Utils;

import static games.coltexpress.ColtExpressTypes.LootType.Purse;

public class EndCardHostage extends RoundEvent {

    @Override
    public boolean execute(AbstractGameState gs) {
        ColtExpressGameState gameState = (ColtExpressGameState) gs;
        Compartment locomotive = gameState.getTrainCompartments().get(gameState.getNPlayers());
        int reward = ((ColtExpressParameters)gs.getGameParameters()).nCardHostageReward;
        for (Integer playerID : locomotive.playersOnTopOfCompartment){
            gameState.addLoot(playerID, new Loot(Purse, reward));
        }
        for (Integer playerID : locomotive.playersInsideCompartment){
            gameState.addLoot(playerID, new Loot(Purse, reward));
        }
        return true;
    }

    @Override
    public AbstractAction copy() {
        return new EndCardHostage();
    }

    @Override
    public boolean equals(Object obj) {
        return obj instanceof EndCardHostage;
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
        return "Hostage";
    }

    @Override
    public String getEventText() {
        return "All bandits in or on the locomotive collect $250 ransom.";
    }
}
