package games.terraformingmars.rules.requirements;

import games.terraformingmars.TMTypes;
import games.terraformingmars.actions.TMAction;

import java.util.Objects;

public class ActionTypeRequirement implements Requirement<TMAction> {

    final TMTypes.ActionType actionType;
    final TMTypes.StandardProject project;

    public ActionTypeRequirement(TMTypes.ActionType actionType, TMTypes.StandardProject sp) {
        this.actionType = actionType;
        this.project = sp;
    }

    public ActionTypeRequirement copy() {
        return this;
    }

    @Override
    public boolean testCondition(TMAction o) {
        return o.actionType == actionType && (o.standardProject == null || (project == null || o.standardProject == project));
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
}
