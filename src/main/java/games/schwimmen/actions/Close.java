package games.schwimmen.actions;

import core.AbstractGameState;
import core.actions.AbstractAction;

/**
 * At the end of their turn, the current player closes the deal or does not.
 */
public class Close extends AbstractAction {

    public final boolean close;

    public Close(boolean close) {
        this.close = close;
    }

    @Override
    public boolean execute(AbstractGameState gs) {
        // the forward model records the close
        return true;
    }

    @Override
    public Close copy() {
        return this;
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof Close other && other.close == close;
    }

    @Override
    public int hashCode() {
        return close ? 734117 : 734119;
    }

    @Override
    public String getString(AbstractGameState gameState) {
        return toString();
    }

    @Override
    public String toString() {
        return close ? "Close" : "Do not close";
    }
}
