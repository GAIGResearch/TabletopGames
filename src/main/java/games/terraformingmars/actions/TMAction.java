package games.terraformingmars.actions;

import core.AbstractGameState;
import core.actions.AbstractAction;
import games.terraformingmars.TMGameState;
import games.terraformingmars.TMTurnOrder;
import games.terraformingmars.rules.Requirement;

import java.util.Objects;

public class TMAction extends AbstractAction {
    public Requirement<TMGameState> requirement;
    final boolean free;
    public final boolean pass;
    public boolean played;

    public TMAction(boolean free) {
        this.free = free;
        this.pass = false;
    }

    public TMAction() {
        this.free = false;
        this.pass = true;
    }

    public TMAction(boolean free, Requirement requirement) {
        this.free= free;
        this.pass = false;
        this.requirement = requirement;
    }

    @Override
    public boolean execute(AbstractGameState gs) {
        if (!free) {
            ((TMTurnOrder)gs.getTurnOrder()).registerActionTaken((TMGameState) gs, this);
        }
        played = true;
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
        return free == tmAction.free && pass == tmAction.pass && played == tmAction.played && Objects.equals(requirement, tmAction.requirement);
    }

    @Override
    public int hashCode() {
        return Objects.hash(requirement, free, pass, played);
    }

    @Override
    public String getString(AbstractGameState gameState) {
        return "Pass";
    }

    @Override
    public String toString() {
        return "Pass";
    }

    public void setPlayed(boolean played) {
        this.played = played;
    }

    public boolean isPlayed() {
        return played;
    }
}
