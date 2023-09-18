package core.actions;

import core.AbstractGameState;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class MacroAction extends AbstractAction {
    final List<AbstractAction> actions;  // atomic actions that when applied in sequence make this macro action
    final List<AbstractGameState> states;  // sets of states we've gone through.
    final int playerID;
    int nextToBeApplied;

    public MacroAction(int playerID, List<AbstractAction> actions, List<AbstractGameState> stateHashes) {
        this.playerID = playerID;
        this.actions = actions;
        this.states = stateHashes;
        nextToBeApplied = 0;
    }

    @Override
    public boolean execute(AbstractGameState gs) {
        if (states.get(nextToBeApplied).hashCode() == gs.hashCode()) {
            actions.get(nextToBeApplied).execute(gs);
            nextToBeApplied++;
            return true;
        }
        return false;
    }

    public Integer getFinalStateHash() {
        return states.get(states.size() - 1).hashCode();
    }

    public List<AbstractAction> getActions() {
        return actions;
    }

    public List<AbstractGameState> getStates() {
        return states;
    }

    public AbstractAction getNext() {
        return actions.get(nextToBeApplied);
    }

    public int getPlayerID() {
        return playerID;
    }

    public boolean executionComplete() {
        return nextToBeApplied == actions.size();
    }

    public MacroAction copy() {
        List<AbstractAction> actionsCopy = new ArrayList<>();
        for (AbstractAction action : actions) {
            actionsCopy.add(action.copy());
        }
        MacroAction copy = new MacroAction(playerID, actionsCopy, new ArrayList<>(states));
        copy.nextToBeApplied = nextToBeApplied;
        return copy;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof MacroAction)) return false;
        MacroAction that = (MacroAction) o;
        return playerID == that.playerID && nextToBeApplied == that.nextToBeApplied && Objects.equals(actions, that.actions);
    }

    @Override
    public int hashCode() {
        return Objects.hash(actions, playerID, nextToBeApplied);
    }

    @Override
    public String getString(AbstractGameState gameState) {
        return toString();
    }

    @Override
    public String toString() {
        return "Macro";
    }
}
