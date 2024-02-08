package core.forwardModels;

import core.AbstractGameState;
import core.actions.AbstractAction;
import core.interfaces.IPlayerDecorator;

import java.util.List;

public class ActionClassDecorator implements IPlayerDecorator {

    Class<?>[] actionClasses;
    boolean decisionPlayerOnly;

    public ActionClassDecorator(String actionClass) {
        this(actionClass, false);
    }

    public ActionClassDecorator(String actionClass, boolean decisionPlayerOnly) {
    // current restriction with instantiation from JSON is that we do not support List or Array parameters
        this.actionClasses = new Class<?>[1];
        this.decisionPlayerOnly = decisionPlayerOnly;
        try {
            this.actionClasses[0] = Class.forName(actionClass);
        } catch (ClassNotFoundException e) {
            throw new AssertionError("Action class not found: " + actionClass);
        }
    }

    @Override
    public List<AbstractAction> actionFilter(AbstractGameState state, List<AbstractAction> possibleActions) {
        // we filter the list of possible actions by removing any that match on one of the restricted classes
        List<AbstractAction> retValue = possibleActions.stream().filter(a -> {
            for (Class<?> c : actionClasses) {
                if (c.isInstance(a)) return false;
            }
            return true;
        }).toList();
        // if this filters out *all* actions, then we are at an impasse...and we ignore the filters
        // so that the player can at least take a valid action from the state
        if (retValue.isEmpty())
            return possibleActions;
        return retValue;
    }

    @Override
    public boolean decisionPlayerOnly() {
        return decisionPlayerOnly;
    }
}
