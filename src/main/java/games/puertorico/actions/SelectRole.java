package games.puertorico.actions;

import core.AbstractGameState;
import core.actions.AbstractAction;
import games.puertorico.*;
import games.puertorico.roles.PuertoRicoRole;

public class SelectRole extends AbstractAction {

    public final PuertoRicoConstants.Role role;

    public SelectRole(PuertoRicoConstants.Role role) {
        this.role = role;
    }

    @Override
    public boolean execute(AbstractGameState gs) {
        PuertoRicoGameState prgs = (PuertoRicoGameState) gs;
        int moneyOnRole = prgs.getAvailableRoles().getOrDefault(role, -1);
        if (moneyOnRole == -1) {
            throw new AssertionError("Role not available: " + role);
        }
        prgs.changeDoubloons(prgs.getCurrentPlayer(), moneyOnRole);
        prgs.setCurrentRole(role);
        // We may then have some initial set up to do at the start of a phase
        PuertoRicoRole<?> roleAction = role.getAction(prgs);
        roleAction.startNewPhase(prgs);
        return true;
    }

    @Override
    public AbstractAction copy() {
        return this;
    }

    @Override
    public boolean equals(Object obj) {
        return obj instanceof SelectRole && ((SelectRole) obj).role == this.role;
    }

    @Override
    public int hashCode() {
        return 67 * this.role.ordinal();
    }

    @Override
    public String toString() {
        return "SelectRole: " + this.role;
    }

    @Override
    public String getString(AbstractGameState gameState) {
        return toString();
    }
}
