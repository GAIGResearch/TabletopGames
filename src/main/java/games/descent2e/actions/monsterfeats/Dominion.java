package games.descent2e.actions.monsterfeats;

import core.AbstractGameState;
import core.actions.AbstractAction;
import games.descent2e.DescentGameState;
import games.descent2e.actions.TriggerAttributeTest;
import games.descent2e.components.Figure;
import games.descent2e.components.Monster;

import java.util.List;

import static games.descent2e.DescentHelper.*;

public class Dominion extends TriggerAttributeTest {

    protected final int distance = 2; // How far Zachareth can move the target
    public Dominion(int attackingFigure, int target) {
        super(attackingFigure, List.of(target));
    }

    public Dominion(int attackingFigure, List<Integer> targets) {
        super(attackingFigure, targets);
    }

    @Override
    public boolean canExecute(DescentGameState dgs) {
        Figure f = (Figure) dgs.getComponentById(getAttackingFigure());
        if (!(f instanceof Monster) || !(((Monster) f).hasAction(MonsterAbilities.MonsterAbility.DOMINION))) return false;

        if (getTargets().isEmpty()) return false;
        Figure target = (Figure) dgs.getComponentById(getTargets().get(0));
        if (target == null) return false;
        return hasLineOfSight(dgs, f.getPosition(), target.getPosition());
    }

    @Override
    public List<AbstractAction> _computeAvailableActions(AbstractGameState state) {
        int target = currentTarget();
        int attacker = getAttackingFigure();
        return List.of(new DominionTest(target, Figure.Attribute.Might, attacker));
    }

    @Override
    public TriggerAttributeTest copy() {
        Dominion retVal = new Dominion(getAttackingFigure(), getTargets());
        copyComponentTo(retVal);
        return retVal;
    }

    @Override
    public String toString() {
        return super.toString().replace("Call Attribute Test", "Dominion");
    }

    @Override
    public String getString(AbstractGameState gameState) {
        return super.getString(gameState).replace("Call Attribute Test", "Dominion");
    }
}
