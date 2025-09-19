package games.descent2e.actions;

import core.AbstractGameState;
import core.actions.AbstractAction;
import core.interfaces.IExtendedSequence;
import games.descent2e.DescentGameState;
import games.descent2e.components.Figure;
import org.apache.spark.sql.streaming.Trigger;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/** Helper class for any monster ability that triggers an attribute test for a set of target characters.
 *
 * The implementing classes just need to:
 * - implement the _computeAvailableActions method to return the actions for the current target (i.e. the test to be executed)
 * - For this tracking to work the action must be a subclass of AttributeTest (else you also need to override _afterAction()
 * - implement the _copy() method.
 *
 * - See Howl for an example
 **/
public class TriggerAttributeTest extends DescentAction implements IExtendedSequence {

    protected List<Integer> targets;
    protected final int attackingFigure;
    protected int currentIndex;
    protected int result;
    protected boolean finished = false;

    public TriggerAttributeTest(int attackingFigure, List<Integer> targets) {
        super(Triggers.ACTION_POINT_SPEND);
        this.attackingFigure = attackingFigure;
        this.targets = targets;
        currentIndex = 0;
        result = -1;
    }

    public TriggerAttributeTest(int attackingFigure, List<Integer> targets, Triggers trigger) {
        super(trigger);
        this.attackingFigure = attackingFigure;
        this.targets = targets;
        currentIndex = 0;
        result = -1;
    }

    @Override
    public boolean execute(DescentGameState state) {
        Figure f = (Figure) state.getComponentById(attackingFigure);
        f.getNActionsExecuted().increment();
        f.addActionTaken(toString());
        state.setActionInProgress(this);
        return true;
    }

    @Override
    public int getCurrentPlayer(AbstractGameState state) {
        // We find the player for the current Figure
        Figure h = (Figure) state.getComponentById(targets.get(currentIndex));
        return h.getOwnerId();
    }

    @Override
    public void _afterAction(AbstractGameState state, AbstractAction action) {
        if (action instanceof AttributeTest) {
            setResult(((AttributeTest) action).getResult());
            currentIndex++;
            if (currentIndex == targets.size()) setFinished(true);
        }
    }

    @Override
    public boolean executionComplete(AbstractGameState state) {
        return finished;
    }

    @Override
    public TriggerAttributeTest copy() {
        List<Triggers> triggers = new ArrayList<>(getTriggers());
        TriggerAttributeTest retVal = new TriggerAttributeTest(attackingFigure, getTargets(), triggers.get(0));
        copyComponentTo(retVal);
        return retVal;
    }

    public void copyComponentTo(TriggerAttributeTest retVal) {
        retVal.currentIndex = currentIndex;
        retVal.result = result;
        retVal.finished = finished;
    }

    @Override
    public boolean canExecute(DescentGameState dgs) {
        return true;
    }

    public List<AbstractAction> _computeAvailableActions(AbstractGameState state) {
        // This is where the call for the Attribute Tests would go
        return new ArrayList<>();
    }

    public List<Integer> getTargets() {
        return List.copyOf(targets);
    }

    public int currentTarget() {
        if (currentIndex < targets.size()) {
            return targets.get(currentIndex);
        } else {
            return -1; // No current target
        }
    }
    public int getAttackingFigure() {
        return attackingFigure;
    }

    public int getCurrentIndex() {
        return currentIndex;
    }
    public void setResult(boolean result) {
        this.result = result ? 1 : 0; // Store as 1 for true, 0 for false, -1 for unresolved
    }
    public int getResult() {
        return result;
    }

    public void resetResult()
    {
        this.result = -1; // Reset to unresolved
    }

    public boolean isFinished() {
        return finished;
    }
    public void setFinished(boolean finished) {
        this.finished = finished;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;
        TriggerAttributeTest triggerAction = (TriggerAttributeTest) o;
        return attackingFigure == triggerAction.attackingFigure &&
                currentIndex == triggerAction.currentIndex &&
                result == triggerAction.result &&
                finished == triggerAction.finished &&
                Objects.equals(targets, triggerAction.targets);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), targets, attackingFigure, currentIndex, result, finished, getClass().getSimpleName());
    }

    @Override
    public String getString(AbstractGameState gameState) {
        String attackerName = ((Figure) gameState.getComponentById(attackingFigure)).getName().replace("Hero: ", "");;
        String string = "Call Attribute Test by "+ attackerName + " on ";

        for (int i = 0; i < targets.size(); i++) {
            Figure defender = (Figure) gameState.getComponentById(targets.get(i));
            String defenderName = defender.getComponentName().replace("Hero: ", "");
            string += defenderName;

            if (i < targets.size() - 1) {
                string += " and ";
            }
        }
        return string;
    }

    @Override
    public String toString() {
        String string = "Call Attribute Test by " + attackingFigure + " on ";
        for (int i = 0; i < targets.size(); i++) {
            string += targets.get(i);
            if (i < targets.size() - 1) {
                string += " and ";
            }
        }
        return string;
    }
}