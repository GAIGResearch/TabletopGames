package games.root.actions;

import core.AbstractGameState;
import core.actions.AbstractAction;
import games.root.RootGameState;
import games.root.RootParameters;
import games.root.components.Item;

import java.util.Objects;

public class ExhaustItem extends AbstractAction {
    public final int playerID;
    public final Item.ItemType item;

    public ExhaustItem(int playerID,  Item.ItemType item){
        this.playerID = playerID;
        this.item = item;
    }

    @Override
    public boolean execute(AbstractGameState gs) {
        RootGameState currentState = (RootGameState) gs;
        if (currentState.getCurrentPlayer() == playerID && currentState.getPlayerFaction(playerID) == RootParameters.Factions.Vagabond){
            switch (item){
                case bag:
                    for (Item bag: currentState.getBags()){
                        if (bag.itemType.equals(item) && bag.refreshed && !bag.damaged){
                            bag.refreshed = false;
                            return true;
                        }
                    }
                    break;
                case tea:
                    for (Item tea: currentState.getTeas()){
                        if (tea.itemType.equals(item) && tea.refreshed && !tea.damaged){
                            tea.refreshed = false;
                            return true;
                        }
                    }
                    break;
                case coin:
                    for (Item coin: currentState.getCoins()){
                        if (coin.itemType.equals(item) && coin.refreshed && !coin.damaged){
                            coin.refreshed = false;
                            return true;
                        }
                    }
                    break;
                default:
                    for (Item item: currentState.getSatchel()){
                        if (item.itemType.equals(this.item) && item.refreshed && !item.damaged){
                            item.refreshed = false;
                            return true;
                        }
                    }
                    break;
            }
        }
        return false;
    }

    @Override
    public ExhaustItem copy() {
        return this;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ExhaustItem that = (ExhaustItem) o;
        return playerID == that.playerID && item == that.item;
    }

    @Override
    public int hashCode() {
        return Objects.hash(playerID, item);
    }

    @Override
    public String toString() {
        return "p" + playerID + " exhausts " + item.toString();
    }

    @Override
    public String getString(AbstractGameState gameState) {
        RootGameState gs = (RootGameState) gameState;
        return gs.getPlayerFaction(playerID).toString() + " exhausts " + item.toString();
    }
}
