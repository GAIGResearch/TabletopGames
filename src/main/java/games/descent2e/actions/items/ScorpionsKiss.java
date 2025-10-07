package games.descent2e.actions.items;

import core.AbstractGameState;
import core.actions.AbstractAction;
import core.interfaces.IExtendedSequence;
import games.descent2e.DescentGameState;
import games.descent2e.DescentHelper;
import games.descent2e.DescentTypes;
import games.descent2e.actions.AttributeTest;
import games.descent2e.actions.TriggerAttributeTest;
import games.descent2e.actions.attack.MeleeAttack;
import games.descent2e.components.Figure;

import java.util.ArrayList;
import java.util.List;

import static games.descent2e.DescentHelper.hasLineOfSight;

public class ScorpionsKiss extends TriggerAttributeTest {

    public static String name = "Scorpion's Kiss";
    public ScorpionsKiss(int attackingFigure, int target) {
        super(attackingFigure, List.of(target));
    }

    public ScorpionsKiss(int attackingFigure, List<Integer> targets) {
        super(attackingFigure, targets);
    }

    @Override
    public boolean canExecute(DescentGameState dgs) {
        Figure f = (Figure) dgs.getComponentById(getAttackingFigure());
        if (f == null) return false;
        if (!f.hasBonus(DescentTypes.SkillBonus.ScorpionsKiss)) return false;
        if (f.getNActionsExecuted().isMaximum()) return false;

        if (getTargets().isEmpty()) return false;
        Figure target = (Figure) dgs.getComponentById(getTargets().get(0));
        if (target == null) return false;

        // If the target has both conditions, disallow them as a legal target
        if (_computeAvailableActions(dgs).isEmpty()) return false;

        return hasLineOfSight(dgs, f.getPosition(), target.getPosition());
    }

    @Override
    public List<AbstractAction> _computeAvailableActions(AbstractGameState state) {
        int target = currentTarget();
        int attacker = getAttackingFigure();

        List<AbstractAction> retVal = new ArrayList<>();
        ScorpionsKissTest poison = new ScorpionsKissTest(target, Figure.Attribute.Awareness, attacker, DescentTypes.DescentCondition.Poison);
        ScorpionsKissTest immobilize = new ScorpionsKissTest(target, Figure.Attribute.Awareness, attacker, DescentTypes.DescentCondition.Immobilize);

        if (poison.canExecute((DescentGameState) state))
            retVal.add(poison);
        if (immobilize.canExecute((DescentGameState) state))
            retVal.add(immobilize);

        return retVal;
    }

    @Override
    public void _afterAction(AbstractGameState state, AbstractAction action) {
        if (action instanceof AttributeTest) {
            // Regardless of the result, we end the action here
            setFinished(true);
        }
    }

    @Override
    public int getCurrentPlayer(AbstractGameState state) {
        // As Seduce only tests Lady Eliza, this should always return the Overlord
        Figure lieutenant = (Figure) state.getComponentById(attackingFigure);
        return lieutenant.getOwnerId();
    }

    @Override
    public TriggerAttributeTest copy() {
        ScorpionsKiss retVal = new ScorpionsKiss(getAttackingFigure(), getTargets());
        copyComponentTo(retVal);
        return retVal;
    }

    @Override
    public String toString() {
        return super.toString().replace("Call Attribute Test", name);
    }

    @Override
    public String getString(AbstractGameState gameState) {
        return super.getString(gameState).replace("Call Attribute Test", name);
    }
}
