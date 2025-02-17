package games.root.actions.extended;

import core.AbstractGameState;
import core.actions.AbstractAction;
import core.interfaces.IExtendedSequence;
import games.root.RootGameState;
import games.root.RootParameters;
import games.root.actions.PassGamePhase;
import games.root.actions.VagabondRepair;
import games.root.components.Item;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class VagabondHideout extends AbstractAction implements IExtendedSequence {
    public final int playerID;

    int repaired = 0;
    boolean done = false;

    public VagabondHideout(int playerID){
        this.playerID = playerID;
    }
    @Override
    public boolean execute(AbstractGameState gs) {
        RootGameState currentState = (RootGameState) gs;
        if (currentState.getCurrentPlayer() == playerID && currentState.getPlayerFaction(playerID) == RootParameters.Factions.Vagabond){
            for (Item item: currentState.getSatchel()){
                if (item.itemType == Item.ItemType.torch && !item.damaged && item.refreshed){
                    item.refreshed = false;
                    currentState.increaseActionsPlayed();
                    currentState.setActionInProgress(this);
                    return true;
                }
            }
        }
        return false;
    }

    @Override
    public List<AbstractAction> _computeAvailableActions(AbstractGameState state) {
        RootGameState rootGameState = (RootGameState) state;
        List<AbstractAction> actions = new ArrayList<>();
        if (repaired < 3){
            for (Item itemToRepair : rootGameState.getSatchel()){
                if (itemToRepair.damaged){
                    VagabondRepair action = new VagabondRepair(rootGameState.getCurrentPlayer(), itemToRepair.itemType, true);
                    if (!actions.contains(action)) actions.add(action);
                }
            }
            for (Item itemToRepair : rootGameState.getCoins()){
                if (itemToRepair.damaged){
                    VagabondRepair action = new VagabondRepair(rootGameState.getCurrentPlayer(), itemToRepair.itemType, true);
                    if (!actions.contains(action)) actions.add(action);
                }
            }
            for (Item itemToRepair : rootGameState.getBags()){
                if (itemToRepair.damaged){
                    VagabondRepair action = new VagabondRepair(rootGameState.getCurrentPlayer(), itemToRepair.itemType, true);
                    if (!actions.contains(action)) actions.add(action);
                }
            }
            for (Item itemToRepair : rootGameState.getTeas()){
                if (itemToRepair.damaged){
                    VagabondRepair action = new VagabondRepair(rootGameState.getCurrentPlayer(), itemToRepair.itemType, true);
                    if (!actions.contains(action)) actions.add(action);
                }
            }
        }else {
            actions.add(new PassGamePhase(playerID));
        }
        if (actions.isEmpty()){
            actions.add(new PassGamePhase(playerID));
        }
        return actions;
    }

    @Override
    public int getCurrentPlayer(AbstractGameState state) {
        return playerID;
    }

    @Override
    public void _afterAction(AbstractGameState state, AbstractAction action) {
        if (action instanceof VagabondRepair){
            repaired++;
        } else if (action instanceof PassGamePhase) {
            done = true;
        }

    }

    @Override
    public boolean executionComplete(AbstractGameState state) {
        return done;
    }

    @Override
    public VagabondHideout copy() {
        VagabondHideout copy = new VagabondHideout(playerID);
        copy.done = done;
        copy.repaired = repaired;
        return copy;
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof VagabondHideout that)) return false;
        return playerID == that.playerID && repaired == that.repaired && done == that.done;
    }

    @Override
    public int hashCode() {
        return Objects.hash(playerID, repaired, done);
    }

    @Override
    public String toString() {
        return "p" + playerID + " plays hideout";
    }

    @Override
    public String getString(AbstractGameState gameState) {
        RootGameState gs = (RootGameState) gameState;
        return gs.getPlayerFaction(playerID).toString() + " plays hideout";
    }
}
