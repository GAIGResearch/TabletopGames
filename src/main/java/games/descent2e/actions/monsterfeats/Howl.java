package games.descent2e.actions.monsterfeats;

import core.AbstractGameState;
import core.actions.AbstractAction;
import games.descent2e.DescentGameState;
import games.descent2e.actions.TriggerAttributeTest;
import games.descent2e.components.Figure;
import games.descent2e.components.Monster;

import java.util.List;

public class Howl extends TriggerAttributeTest {
    public Howl(int attackingFigure, List<Integer> targets) {
        super(attackingFigure, targets);
    }

    @Override
    public boolean canExecute(DescentGameState dgs) {
        if (getTargets().isEmpty()) return false;
        Figure f = (Figure) dgs.getComponentById(getAttackingFigure());
        return f instanceof Monster && (((Monster) f).hasAction(MonsterAbilities.MonsterAbility.HOWL));
    }

    @Override
    public List<AbstractAction> _computeAvailableActions(AbstractGameState state) {
        int target = currentTarget();
        int attacker = getAttackingFigure();
        return List.of(new HowlTest(target, Figure.Attribute.Willpower, attacker));
    }

    @Override
    public TriggerAttributeTest copy() {
        Howl retVal = new Howl(getAttackingFigure(), getTargets());
        copyComponentTo(retVal);
        return retVal;
    }

    @Override
    public String toString() {
        return super.toString().replace("Call Attribute Test", "Howl");
    }

    @Override
    public String getString(AbstractGameState gameState) {
        return super.getString(gameState).replace("Call Attribute Test", "Howl");
    }
}