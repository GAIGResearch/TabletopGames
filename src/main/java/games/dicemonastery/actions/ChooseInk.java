package games.dicemonastery.actions;

import core.AbstractGameState;
import core.actions.AbstractAction;
import games.dicemonastery.DiceMonasteryConstants.Resource;

public class ChooseInk extends AbstractAction {

    public final Resource ink;

    public ChooseInk(Resource ink) {
        this.ink = ink;
        if (!ink.isInk)
            throw new AssertionError(ink + " is not an ink");
    }

    @Override
    public boolean execute(AbstractGameState gs) {
        return true;
    }

    @Override
    public AbstractAction copy() {
        return this; // immutable
    }

    @Override
    public boolean equals(Object obj) {
        return obj instanceof ChooseInk && ((ChooseInk) obj).ink == ink;
    }

    @Override
    public int hashCode() {
        return 3892 + ink.ordinal();
    }

    @Override
    public String toString() {
        return "Chooses to use " + ink;
    }

    @Override
    public String getString(AbstractGameState gameState) {
        return toString();
    }
}
