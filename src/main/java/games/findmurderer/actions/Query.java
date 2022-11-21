package games.findmurderer.actions;

/* Detective question to reveal information about a person: who they were adjacent to previously */

import core.AbstractGameState;
import core.actions.AbstractAction;
import games.findmurderer.MurderGameState;

import java.util.Objects;

public class Query extends AbstractAction {
    public final int targetID;

    public Query(int targetID) {
        this.targetID = targetID;
    }

    @Override
    public boolean execute(AbstractGameState gs) {
        MurderGameState mgs = (MurderGameState) gs;
        mgs.getDetectiveInformation().put(targetID, gs.getTurnOrder().getRoundCounter());
        return true;
    }

    @Override
    public Query copy() {
        return this;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Query)) return false;
        Query query = (Query) o;
        return targetID == query.targetID;
    }

    @Override
    public int hashCode() {
        return Objects.hash(targetID);
    }

    @Override
    public String getString(AbstractGameState gameState) {
        return toString();
    }

    @Override
    public String toString() {
        return "Query " + targetID;
    }
}
