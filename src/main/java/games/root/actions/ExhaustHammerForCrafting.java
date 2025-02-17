package games.root.actions;

import core.AbstractGameState;
import core.actions.AbstractAction;
import games.root.RootGameState;
import games.root.RootParameters;
import games.root.components.Item;

import java.util.Objects;

public class ExhaustHammerForCrafting extends AbstractAction{
    public final int playerID;
    public final RootParameters.ClearingTypes activate;
    public final RootParameters.ClearingTypes actual;

    public ExhaustHammerForCrafting(int playerID, RootParameters.ClearingTypes activate, RootParameters.ClearingTypes actual){
        this.playerID = playerID;
        this.activate = activate;
        this.actual = actual;
    }

    @Override
    public boolean execute(AbstractGameState gs) {
        RootGameState state = (RootGameState) gs;
        if (gs.getCurrentPlayer() == playerID){

            for (Item item: state.getSatchel()){
                if (item.itemType == Item.ItemType.hammer && !item.damaged && item.refreshed){
                    item.refreshed = false;
                    return true;
                }
            }
        }
        return false;
    }

    @Override
    public ExhaustHammerForCrafting copy() {
        return this;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ExhaustHammerForCrafting that = (ExhaustHammerForCrafting) o;
        return playerID == that.playerID && activate == that.activate && actual == that.actual;
    }

    @Override
    public int hashCode() {
        return Objects.hash(playerID, activate, actual);
    }

    @Override
    public String toString() {
        return "p" + playerID + " exhausts hammer " + activate.toString() + " to satisfy " + actual.toString() + "requirement";
    }

    @Override
    public String getString(AbstractGameState gameState) {
        RootGameState gs = (RootGameState) gameState;
        return gs.getPlayerFaction(playerID).toString() + " exhausts hammer " + activate.toString() + " to satisfy " + actual.toString() + "requirement";
    }
}
