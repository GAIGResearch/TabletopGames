package games.findmurderer.actions;

import core.AbstractGameState;
import core.actions.AbstractAction;
import games.findmurderer.MurderGameState;
import utilities.Vector2D;

import java.util.Objects;

/* Detective action to focus vision around a cell in the grid */

public class LookAt extends AbstractAction {
    public final Vector2D target;

    public LookAt (Vector2D target){
        this.target = target;
    }

    @Override
    public boolean execute(AbstractGameState gs) {
        ((MurderGameState)gs).setDetectiveFocus(target);
        return true;
    }

    @Override
    public LookAt copy() {
        return this;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof LookAt)) return false;
        LookAt lookAt = (LookAt) o;
        return Objects.equals(target, lookAt.target);
    }

    @Override
    public int hashCode() {
        return Objects.hash(target);
    }

    @Override
    public String getString(AbstractGameState gameState) {
        return toString();
    }

    @Override
    public String toString() {
        return "Look at " + target;
    }
}
