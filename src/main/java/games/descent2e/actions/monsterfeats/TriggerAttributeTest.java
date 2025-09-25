package games.descent2e.actions.monsterfeats;

import core.AbstractGameState;
import core.actions.AbstractAction;
import core.interfaces.IExtendedSequence;
import games.descent2e.DescentGameState;
import games.descent2e.actions.AttributeTest;
import games.descent2e.actions.DescentAction;
import games.descent2e.actions.Triggers;
import games.descent2e.components.Figure;
import games.descent2e.components.Monster;

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
public abstract class TriggerAttributeTest extends DescentAction implements IExtendedSequence {

    final List<Integer> heroes;
    final int attackingFigure;
    int currentIndex;

    public TriggerAttributeTest(int attackingFigure, List<Integer> targets) {
        super(Triggers.ACTION_POINT_SPEND);
        this.attackingFigure = attackingFigure;
        this.heroes = targets;
        currentIndex = 0;
    }

    @Override
    public String toString() {
        return "Howl";
    }

    @Override
    public boolean execute(DescentGameState state) {
        Figure monster = (Figure) state.getComponentById(attackingFigure);
        monster.getNActionsExecuted().increment();
        monster.addActionTaken(toString());
        state.setActionInProgress(this);
        return true;
    }

    @Override
    public int getCurrentPlayer(AbstractGameState state) {
        // We find the player for the next hero
        Figure h = (Figure) state.getComponentById(heroes.get(currentIndex));
        return h.getOwnerId();
    }

    @Override
    public void _afterAction(AbstractGameState state, AbstractAction action) {
        if (action instanceof AttributeTest) {
            currentIndex++;
        }
    }

    @Override
    public boolean executionComplete(AbstractGameState state) {
        return currentIndex == heroes.size();
    }

    @Override
    public TriggerAttributeTest copy() {
        TriggerAttributeTest retValue = _copy();
        retValue.currentIndex = currentIndex;
        return retValue;
    }

    abstract TriggerAttributeTest _copy();

    @Override
    public boolean canExecute(DescentGameState dgs) {
        Figure f = (Figure) dgs.getComponentById(attackingFigure);
        return f instanceof Monster && (((Monster) f).hasAction(MonsterAbilities.MonsterAbility.HOWL));
    }

    public List<Integer> getTargets() {
        return List.copyOf(heroes);
    }

    public int currentTarget() {
        if (currentIndex < heroes.size()) {
            return heroes.get(currentIndex);
        } else {
            return -1; // No current target
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;
        TriggerAttributeTest triggerAction = (TriggerAttributeTest) o;
        return attackingFigure == triggerAction.attackingFigure && currentIndex == triggerAction.currentIndex &&
                Objects.equals(heroes, triggerAction.heroes);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), heroes, attackingFigure, currentIndex, getClass().getSimpleName());
    }

    @Override
    public String getString(AbstractGameState gameState) {
        return getClass().getSimpleName() + " by " + ((Figure) gameState.getComponentById(attackingFigure)).getName() +
                " at " + heroes.stream().map(id -> ((Figure) gameState.getComponentById(id)).getName()).toList();
    }
}
