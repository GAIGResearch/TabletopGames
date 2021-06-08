package games.terraformingmars.rules.requirements;

import games.terraformingmars.TMGameState;
import games.terraformingmars.TMTypes;
import games.terraformingmars.actions.TMAction;

import java.awt.*;
import java.util.Objects;

public class ActionTypeRequirement implements Requirement<TMAction> {

    public final TMTypes.ActionType actionType;
    public final TMTypes.StandardProject project;

    public ActionTypeRequirement(TMTypes.ActionType actionType, TMTypes.StandardProject sp) {
        this.actionType = actionType;
        this.project = sp;
    }

    public ActionTypeRequirement copy() {
        return this;
    }

    @Override
    public boolean testCondition(TMAction o) {
        return o.actionType == actionType
                && (project == null && o.standardProject == null
                    || o.standardProject == project);
    }

    @Override
    public boolean isMax() {
        return false;
    }

    @Override
    public boolean appliesWhenAnyPlayer() {
        return false;
    }

    @Override
    public String getDisplayText(TMGameState gs) {
        return null;
    }

    @Override
    public String getReasonForFailure(TMGameState gs) {
        return null;
    }

    @Override
    public Image[] getDisplayImages() {
        return null;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ActionTypeRequirement)) return false;
        ActionTypeRequirement that = (ActionTypeRequirement) o;
        return actionType == that.actionType && project == that.project;
    }

    @Override
    public int hashCode() {
        return Objects.hash(actionType, project);
    }

    @Override
    public String toString() {
        return "Action Type";
    }
}
