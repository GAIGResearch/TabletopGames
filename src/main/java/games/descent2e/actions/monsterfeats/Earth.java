package games.descent2e.actions.monsterfeats;

import core.AbstractGameState;
import core.actions.AbstractAction;
import games.descent2e.DescentGameState;
import games.descent2e.DescentTypes;
import games.descent2e.actions.TriggerAttributeTest;
import games.descent2e.components.Figure;
import games.descent2e.components.Monster;

import java.util.List;

public class Earth extends TriggerAttributeTest {

    public Earth(int attackingFigure, List<Integer> targets) {
        super(attackingFigure, targets);
    }

    @Override
    public boolean canExecute(DescentGameState dgs) {

        List<Integer> targets = getTargets();
        if (targets.isEmpty()) return false;

        boolean canImmobilize = false;

        // Check that at least one Hero is not immobilized
        // Otherwise, why would you use this action when everyone is already immobilized?
        for (Integer heroId : targets) {
            Figure hero = (Figure) dgs.getComponentById(heroId);
            if (!hero.hasCondition(DescentTypes.DescentCondition.Immobilize)) {
                canImmobilize = true;
                break;
            }
        }

        if (!canImmobilize) return false;

        Figure f = (Figure) dgs.getComponentById(attackingFigure);
        return (f instanceof Monster) && (((Monster) f).hasAction(MonsterAbilities.MonsterAbility.EARTH));
    }

    @Override
    public List<AbstractAction> _computeAvailableActions(AbstractGameState state) {
        int target = currentTarget();
        int attacker = getAttackingFigure();
        return List.of(new EarthTest(target, Figure.Attribute.Awareness, attacker));
    }

    @Override
    public TriggerAttributeTest copy() {
        Earth retVal = new Earth(getAttackingFigure(), getTargets());
        copyComponentTo(retVal);
        return retVal;
    }

    @Override
    public String toString() {
        return super.toString().replace("Call Attribute Test", "Earth");
    }

    @Override
    public String getString(AbstractGameState gameState) {
        return super.getString(gameState).replace("Call Attribute Test", "Earth");
    }
}
