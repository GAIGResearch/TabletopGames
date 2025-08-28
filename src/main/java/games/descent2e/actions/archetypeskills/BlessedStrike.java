package games.descent2e.actions.archetypeskills;

import core.AbstractGameState;
import core.actions.AbstractAction;
import core.interfaces.IExtendedSequence;
import games.descent2e.DescentGameState;
import games.descent2e.DescentTypes;
import games.descent2e.actions.DescentAction;
import games.descent2e.actions.Triggers;
import games.descent2e.actions.attack.FreeAttack;
import games.descent2e.actions.attack.MeleeAttack;
import games.descent2e.components.Figure;

import java.util.ArrayList;
import java.util.List;

import static games.descent2e.DescentHelper.*;

public class BlessedStrike extends DescentAction implements IExtendedSequence {
    int userID;
    int allyID;
    boolean hasReach;
    boolean healing = false;
    boolean complete = false;
    boolean oldExtraAttack = false;
    public BlessedStrike(int userID, int allyID, boolean hasReach) {
        super(Triggers.ACTION_POINT_SPEND);
        this.userID = userID;
        this.allyID = allyID;
        this.hasReach = hasReach;
    }

    public boolean execute(DescentGameState dgs) {

        dgs.setActionInProgress(this);
        Figure user = (Figure) dgs.getComponentById(userID);
        user.getNActionsExecuted().increment();
        user.getAttribute(Figure.Attribute.Fatigue).increment();
        user.addActionTaken(toString());
        oldExtraAttack = user.hasUsedExtraAction();
        user.setUsedExtraAction(false);

        return true;
    }

    public boolean canExecute(DescentGameState dgs) {
        Figure user = (Figure) dgs.getComponentById(userID);
        Figure ally = (Figure) dgs.getComponentById(allyID);

        if (user == null || ally == null) return false;

        if (user.getNActionsExecuted().isMaximum()) return false;
        if (user.getAttribute(Figure.Attribute.Fatigue).isMaximum()) return false;

        // Either ally must be able to heal to be used
        // or the user if no ally is chosen
        if (ally.getAttribute(Figure.Attribute.Health).isMaximum())
            return false;

        // Must be adjacent to the chosen ally
        if ((userID != allyID) && !checkAdjacent(dgs, user, ally)) return false;

        // Must have a target to attack
        List<Integer> targets = getMeleeTargets(dgs, user, hasReach);
        return !targets.isEmpty();
    }

    @Override
    public List<AbstractAction> _computeAvailableActions(AbstractGameState state) {

        List<AbstractAction> actions = new ArrayList<>();
        DescentGameState dgs = (DescentGameState) state;
        Figure user = (Figure) dgs.getComponentById(userID);

        List<Integer> targets = getMeleeTargets(dgs, user, hasReach);

        for (Integer targetID : targets)
        {
            FreeAttack attack = new FreeAttack(userID, targetID, true, hasReach);
            if (attack.canExecute(dgs))
                actions.add(attack);
        }

        return actions;
    }

    @Override
    public int getCurrentPlayer(AbstractGameState state) {
        return state.getComponentById(userID).getOwnerId();
    }

    @Override
    public void _afterAction(AbstractGameState state, AbstractAction action) {
        if (action instanceof MeleeAttack)
        {
            complete = true;
            Figure user = (Figure) state.getComponentById(userID);
            user.setUsedExtraAction(oldExtraAttack);
            MeleeAttack attack = (MeleeAttack) action;
            if (attack.toStringWithResult().contains("Missed"))
            {
                return;
            }
            if (attack.getDamage() > 0)
            {
                healing = true;
            }
        }

        if (healing)
        {
            Figure user = (Figure) state.getComponentById(userID);
            user.incrementAttribute(Figure.Attribute.Health, 2);
            if (allyID != userID)
            {
                Figure ally = (Figure) state.getComponentById(allyID);
                ally.incrementAttribute(Figure.Attribute.Health, 2);
            }
        }
    }

    @Override
    public boolean executionComplete(AbstractGameState state) {
        return complete;
    }

    @Override
    public BlessedStrike copy() {
        BlessedStrike retValue = new BlessedStrike(userID, allyID, hasReach);
        retValue.complete = complete;
        retValue.healing = healing;
        retValue.oldExtraAttack = oldExtraAttack;
        return retValue;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof BlessedStrike that)) return false;
        if (!super.equals(o)) return false;
        return userID == that.userID && allyID == that.allyID && hasReach == that.hasReach && healing == that.healing && oldExtraAttack == that.oldExtraAttack && complete == that.complete;
    }

    @Override
    public int hashCode() {
        return java.util.Objects.hash(super.hashCode(), userID, allyID, hasReach, healing, oldExtraAttack, complete);
    }

    @Override
    public String toString() {
        if (allyID == userID) return String.format("Blessed Strike to heal %d", userID);
        return String.format("Blessed Strike to heal %d and %d", userID, allyID);
    }

    @Override
    public String getString(AbstractGameState gameState) {
        Figure user = (Figure) gameState.getComponentById(userID);

        String userName = user.getComponentName().replace("Hero: ", "");
        String string = "Blessed Strike: Melee Attack, +2 Health to " + userName;
        String healString = healing ? " (Healed)" : complete ? " (Did Not Heal)" : " if damaging";
        if (allyID == userID) return string + healString;

        Figure ally = (Figure) gameState.getComponentById(allyID);
        String allyName = ally.getComponentName().replace("Hero: ", "");
        return string + " and " + allyName + healString;
    }
}
