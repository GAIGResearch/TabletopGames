package games.descent2e.actions.monsterfeats;

import core.AbstractGameState;
import core.actions.AbstractAction;
import games.descent2e.DescentGameState;
import games.descent2e.actions.TriggerAttributeTest;
import games.descent2e.components.Figure;
import games.descent2e.components.Monster;

import java.util.List;

import static games.descent2e.DescentHelper.checkAdjacent;
import static games.descent2e.DescentTypes.DescentCondition.Immobilize;

public class Grab extends TriggerAttributeTest {

    public Grab(int attackingFigure, int target) {
        super(attackingFigure, List.of(target));
    }

    public Grab(int attackingFigure, List<Integer> targets) {
        super(attackingFigure, targets);
    }

    @Override
    public boolean canExecute(DescentGameState dgs) {
        Figure f = (Figure) dgs.getComponentById(getAttackingFigure());
        if (!(f instanceof Monster) || !(((Monster) f).hasAction(MonsterAbilities.MonsterAbility.GRAB))) return false;

        if (getTargets().isEmpty()) return false;
        Figure target = (Figure) dgs.getComponentById(getTargets().get(0));

        // As identical conditions do not stack, there is no point attempting to grab a figure that is already immobilized
        if (target == null || target.hasCondition(Immobilize)) {
            return false;
        }

        return checkAdjacent(dgs, f, target);
    }

    @Override
    public List<AbstractAction> _computeAvailableActions(AbstractGameState state) {
        int target = currentTarget();
        int attacker = getAttackingFigure();
        return List.of(new GrabTest(target, Figure.Attribute.Might, attacker));
    }

    @Override
    public TriggerAttributeTest copy() {
        Grab retVal = new Grab(getAttackingFigure(), getTargets());
        copyComponentTo(retVal);
        return retVal;
    }

    @Override
    public String toString() {
        return super.toString().replace("Call Attribute Test", "Grab");
    }

    @Override
    public String getString(AbstractGameState gameState) {
        return super.getString(gameState).replace("Call Attribute Test", "Grab");
    }
}
