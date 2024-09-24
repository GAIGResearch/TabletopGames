package games.root_final.actions;

import core.AbstractGameState;
import core.actions.AbstractAction;
import games.root_final.RootGameState;
import games.root_final.RootParameters;
import games.root_final.components.Item;

import java.util.Objects;

public class VagabondDiscardItem extends AbstractAction {
    public final int playerID;
    public Item item;

    public VagabondDiscardItem(int playerID, Item item){
        this.playerID = playerID;
        this.item = item;
    }

    @Override
    public boolean execute(AbstractGameState gs) {
        RootGameState currentState = (RootGameState) gs;
        if (currentState.getCurrentPlayer() == playerID && currentState.getPlayerFaction(playerID) == RootParameters.Factions.Vagabond){
            switch (item.itemType){
                case tea:
                    for (Item teaItem: currentState.getTeas()){
                        if (teaItem.getComponentID() == item.getComponentID()){
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
                        if (bagItem.getComponentID() == item.getComponentID()){
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
                        if (coinItem.getComponentID() == item.getComponentID()){
                            coinItem.refreshed = true;
                            coinItem.damaged = false;
                            currentState.getCraftableItems().add(coinItem);
                            currentState.getCoins().remove(coinItem);
                            break;
                        }
                    }
                    break;
                default:
                    for (Item sachelItem: currentState.getSachel()){
                        if (sachelItem.getComponentID() == item.getComponentID()){
                            sachelItem.damaged = false;
                            sachelItem.refreshed = true;
                            currentState.getCraftableItems().add(sachelItem);
                            currentState.getSachel().remove(sachelItem);
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
        return new VagabondDiscardItem(playerID, item);
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this){return true;}
        if (obj instanceof VagabondDiscardItem vd){
            return playerID == vd.playerID && item.getComponentID() == vd.item.getComponentID();
        }
        return false;
    }

    @Override
    public int hashCode() {
        return Objects.hash("VagabondDiscardItem", playerID, item.hashCode());
    }

    @Override
    public String getString(AbstractGameState gameState) {
        RootGameState gs = (RootGameState) gameState;
        return gs.getPlayerFaction(playerID).toString() + " discards " + item.itemType.toString();
    }
}
