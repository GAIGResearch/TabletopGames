package games.coltexpress.actions.roundcardevents;

import core.AbstractGameState;
import core.actions.AbstractAction;
import games.coltexpress.ColtExpressGameState;
import games.coltexpress.components.Compartment;
import games.coltexpress.components.Loot;
import utilities.Utils;

import static games.coltexpress.ColtExpressParameters.LootType.Purse;

public class EndCardHostage extends AbstractAction {

    @Override
    public boolean execute(AbstractGameState gs) {
        ColtExpressGameState gameState = (ColtExpressGameState) gs;
        Compartment locomotive = gameState.getTrainCompartments().get(gameState.getNPlayers());
        for (Integer playerID : locomotive.playersOnTopOfCompartment){
            gameState.addLoot(playerID, new Loot(Purse, Purse.getDefaultValue()));
        }
        for (Integer playerID : locomotive.playersInsideCompartment){
            gameState.addLoot(playerID, new Loot(Purse, Purse.getDefaultValue()));
        }
        gameState.setGameStatus(Utils.GameResult.GAME_END);
        gameState.endGame();
        return true;
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
    public String toString() {
        return "Hostage";
    }
}
