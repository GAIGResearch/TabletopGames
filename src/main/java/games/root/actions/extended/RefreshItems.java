package games.root.actions.extended;

import core.AbstractGameState;
import core.actions.AbstractAction;
import core.interfaces.IExtendedSequence;
import games.root.RootGameState;
import games.root.actions.PassSubGamePhase;
import games.root.actions.Refresh;
import games.root.components.Item;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class RefreshItems extends AbstractAction implements IExtendedSequence {
    public final int playerID;

    boolean done = false;
    int refreshedItems = 0;

    public RefreshItems(int playerID) {
        this.playerID = playerID;
    }

    @Override
    public boolean execute(AbstractGameState gs) {
        RootGameState state = (RootGameState) gs;
        state.setActionInProgress(this);
        return true;
    }

    @Override
    public List<AbstractAction> _computeAvailableActions(AbstractGameState state) {
        RootGameState currentState = (RootGameState) state;
        List<AbstractAction> actions = new ArrayList<>();
        //for each team refresh item -> else pass
        if (refreshedItems < 3 + (currentState.getNumberOfTeas() * 2)) {
            for (Item item : currentState.getSatchel()) {
                if (!item.refreshed) {
                    Refresh action = new Refresh(playerID, item.itemType, item.getComponentID());
                    actions.add(action);
                }
            }
        }
        //if there is nothing to refresh
        if (actions.isEmpty()) {
            PassSubGamePhase pass = new PassSubGamePhase(playerID);
            actions.add(pass);
        }
        return actions;
    }

    @Override
    public int getCurrentPlayer(AbstractGameState state) {
        return playerID;
    }

    @Override
    public void _afterAction(AbstractGameState state, AbstractAction action) {
        RootGameState currentState = (RootGameState) state;
        if(action instanceof Refresh) {
            refreshedItems++;
            if (refreshedItems == 3 + (currentState.getNumberOfTeas() * 2)) {
                done = true;
            }
        } else if (action instanceof PassSubGamePhase) {
            done = true;
        }
    }

    @Override
    public boolean executionComplete(AbstractGameState state) {
        return done;
    }

    @Override
    public RefreshItems copy() {
        RefreshItems copy = new RefreshItems(playerID);
        copy.refreshedItems = refreshedItems;
        copy.done = done;
        return copy;
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof RefreshItems that)) return false;
        return playerID == that.playerID && done == that.done && refreshedItems == that.refreshedItems;
    }

    @Override
    public int hashCode() {
        return Objects.hash(playerID, done, refreshedItems);
    }

    @Override
    public String toString() {
        return "p" + playerID + " refreshes items";
    }

    @Override
    public String getString(AbstractGameState gameState) {
        RootGameState gs = (RootGameState) gameState;
        return gs.getPlayerFaction(playerID).toString()  + " refreshes items";
    }
}
