package games.descent2e.actions.monsterfeats;

import core.AbstractGameState;
import core.actions.AbstractAction;
import games.descent2e.DescentGameState;
import games.descent2e.actions.AttributeTest;
import games.descent2e.actions.TriggerAttributeTest;
import games.descent2e.components.Figure;
import games.descent2e.components.Monster;
import utilities.Vector2D;

import java.util.ArrayList;
import java.util.List;

import static games.descent2e.DescentHelper.*;

public class Promotion extends TriggerAttributeTest {

    public Promotion(int attackingFigure, int target) {
        super(attackingFigure, List.of(target));
    }

    public Promotion(int attackingFigure, List<Integer> targets) {
        super(attackingFigure, targets);
    }

    @Override
    public boolean canExecute(DescentGameState dgs) {
        Figure f = (Figure) dgs.getComponentById(getAttackingFigure());
        if (!(f instanceof Monster) || !(((Monster) f).hasAction(MonsterAbilities.MonsterAbility.PROMOTION))) return false;

        if (getTargets().isEmpty()) return false;
        Figure target = (Figure) dgs.getComponentById(getTargets().get(0));
        if (target == null) return false;
        if (!(target instanceof Monster)) return false;
        return target.getName().toLowerCase().contains("minion") && checkAdjacent(dgs, f, target);
    }

    @Override
    public List<AbstractAction> _computeAvailableActions(AbstractGameState state) {
        int target = currentTarget();
        int attacker = getAttackingFigure();

        // If we haven't made the test yet, get the test first
        int result = getResult();
        if (result == -1) return List.of(new PromotionTest(attacker, Figure.Attribute.Willpower, target));

        List<AbstractAction> retVal = new ArrayList<>();

        // Promotion requires that Splig to have passed his test (result == 1) to proceed
        if (result == 1) {
            PromotionPromote promote = new PromotionPromote(target, attacker);
            if (promote.canExecute((DescentGameState) state)) {
                retVal.add(promote);
            }
        }

        return retVal;
    }

    @Override
    public void _afterAction(AbstractGameState state, AbstractAction action) {
        if (action instanceof AttributeTest) {
            setResult(((AttributeTest) action).getResult());
            // If Splig failed his test, we finish here
            if (getResult() == 0) setFinished(true);
        }
        if (action instanceof PromotionPromote)
        {
            setFinished(true);
        }
    }

    @Override
    public int getCurrentPlayer(AbstractGameState state) {
        // As Promotion only tests Splig, this should always return the Overlord
        Figure splig = (Figure) state.getComponentById(attackingFigure);
        return splig.getOwnerId();
    }

    @Override
    public TriggerAttributeTest copy() {
        Promotion retVal = new Promotion(getAttackingFigure(), getTargets());
        copyComponentTo(retVal);
        return retVal;
    }

    @Override
    public String toString() {
        return super.toString().replace("Call Attribute Test", "Promotion");
    }

    @Override
    public String getString(AbstractGameState gameState) {
        return super.getString(gameState).replace("Call Attribute Test", "Promotion");
    }
}
