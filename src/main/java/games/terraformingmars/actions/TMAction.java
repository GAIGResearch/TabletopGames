package games.terraformingmars.actions;

import core.AbstractGameState;
import core.actions.AbstractAction;
import games.terraformingmars.TMGameState;
import games.terraformingmars.TMTurnOrder;

import java.util.Objects;

public class TMAction extends AbstractAction {
    final boolean free;

    public final boolean pass;

    public TMAction(boolean free) {
        this.free = free;
        this.pass = false;
    }

    public TMAction() {
        this.free = false;
        this.pass = true;
    }

    @Override
    public boolean execute(AbstractGameState gs) {
        if (!free) {
            ((TMTurnOrder)gs.getTurnOrder()).registerActionTaken((TMGameState) gs, this);
        }
        return true;
    }

    @Override
    public AbstractAction copy() {
        return this;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof TMAction)) return false;
        TMAction tmAction = (TMAction) o;
        return free == tmAction.free &&
                pass == tmAction.pass;
    }

    @Override
    public int hashCode() {
        return Objects.hash(free, pass);
    }

    @Override
    public String getString(AbstractGameState gameState) {
        return "Pass";
    }

    @Override
    public String toString() {
        return "Pass";
    }
}
