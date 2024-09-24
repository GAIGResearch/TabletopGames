package games.root_final.actions;

import core.AbstractGameState;
import core.actions.AbstractAction;
import games.root_final.RootGameState;
import games.root_final.RootParameters;
import games.root_final.components.Item;

import java.util.Objects;

public class ExhaustItem extends AbstractAction {
    public final int playerID;
    public Item item;

    public ExhaustItem(int playerID, Item item){
        this.playerID = playerID;
        this.item = item;
    }

    @Override
    public boolean execute(AbstractGameState gs) {
        RootGameState currentState = (RootGameState) gs;
        if (currentState.getCurrentPlayer() == playerID && currentState.getPlayerFaction(playerID) == RootParameters.Factions.Vagabond){
            switch (item.itemType){
                case bag:
                    for (Item bag: currentState.getBags()){
                        if (bag.equals(item) && bag.refreshed && !bag.damaged){
                            bag.refreshed = false;
                            return true;
                        }
                    }
                    break;
                case tea:
                    for (Item tea: currentState.getTeas()){
                        if (tea.equals(item) && tea.refreshed && !tea.damaged){
                            tea.refreshed = false;
                            return true;
                        }
                    }
                    break;
                case coin:
                    for (Item coin: currentState.getCoins()){
                        if (coin.equals(item) && coin.refreshed && !coin.damaged){
                            coin.refreshed = false;
                            return true;
                        }
                    }
                    break;
                default:
                    for (Item item: currentState.getSachel()){
                        if (item.equals(this.item) && item.refreshed && !item.damaged){
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
    public AbstractAction copy() {
        return new ExhaustItem(playerID, item);
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this){return true;}
        if (obj instanceof ExhaustItem ei){
            return playerID == ei.playerID && item.getComponentID() == ei.item.getComponentID();
        }
        return false;
    }

    @Override
    public int hashCode() {
        return Objects.hash("Exhaust", playerID, item.hashCode());
    }

    @Override
    public String getString(AbstractGameState gameState) {
        RootGameState gs = (RootGameState) gameState;
        return gs.getPlayerFaction(playerID).toString() + " exhausts " + item.itemType.toString();
    }
}
