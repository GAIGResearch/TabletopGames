package games.root.actions;

import core.AbstractGameState;
import core.actions.AbstractAction;
import games.root.RootGameState;
import games.root.RootParameters;
import games.root.components.Item;

import java.util.Objects;

public class VagabondDiscardItem extends AbstractAction {
    public final int playerID;
    public final Item.ItemType item;
    public final int itemID;

    public VagabondDiscardItem(int playerID, Item.ItemType item, int itemID){
        this.playerID = playerID;
        this.item = item;
        this.itemID = itemID;
    }

    @Override
    public boolean execute(AbstractGameState gs) {
        RootGameState currentState = (RootGameState) gs;
        if (currentState.getCurrentPlayer() == playerID && currentState.getPlayerFaction(playerID) == RootParameters.Factions.Vagabond){
            switch (item){
                case tea:
                    for (Item teaItem: currentState.getTeas()){
                        if (teaItem.getComponentID() == itemID){
                            teaItem.damaged = false;
                            teaItem.refreshed = true;
                            currentState.getCraftableItems().add(teaItem);
                            currentState.getTeas().remove(teaItem);
                            break;
                        }
                    }
                    break;
                case bag:
                    for (Item bagItem: currentState.getBags()){
                        if (bagItem.getComponentID() == itemID){
                            bagItem.refreshed = true;
                            bagItem.damaged = false;
                            currentState.getCraftableItems().add(bagItem);
                            currentState.getBags().remove(bagItem);
                            break;
                        }
                    }
                    break;
                case coin:
                    for (Item coinItem: currentState.getCoins()){
                        if (coinItem.getComponentID() == itemID){
                            coinItem.refreshed = true;
                            coinItem.damaged = false;
                            currentState.getCraftableItems().add(coinItem);
                            currentState.getCoins().remove(coinItem);
                            break;
                        }
                    }
                    break;
                default:
                    for (Item satchelItem: currentState.getSatchel()){
                        if (satchelItem.getComponentID() == itemID){
                            satchelItem.damaged = false;
                            satchelItem.refreshed = true;
                            currentState.getCraftableItems().add(satchelItem);
                            currentState.getSatchel().remove(satchelItem);
                            break;
                        }
                    }
                    break;
            }
            return true;
        }
        return false;
    }

    @Override
    public AbstractAction copy() {
        return new VagabondDiscardItem(playerID, item, itemID);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        VagabondDiscardItem that = (VagabondDiscardItem) o;
        return playerID == that.playerID && itemID == that.itemID && item == that.item;
    }

    @Override
    public int hashCode() {
        return Objects.hash(playerID, item, itemID);
    }

    @Override
    public String toString() {
        return "p" + playerID  + " discards " + item.toString();
    }

    @Override
    public String getString(AbstractGameState gameState) {
        RootGameState gs = (RootGameState) gameState;
        return gs.getPlayerFaction(playerID).toString() + " discards " + item.toString();
    }
}
