package games.descent2e.actions.items;

import core.AbstractGameState;
import core.actions.AbstractAction;
import core.interfaces.IExtendedSequence;
import games.descent2e.DescentGameState;
import games.descent2e.DescentTypes;
import games.descent2e.actions.AttributeTest;
import games.descent2e.actions.TriggerAttributeTest;
import games.descent2e.actions.attack.MeleeAttack;
import games.descent2e.actions.monsterfeats.ForcedMove;
import games.descent2e.actions.monsterfeats.MonsterAbilities;
import games.descent2e.actions.monsterfeats.SeduceMove;
import games.descent2e.actions.monsterfeats.SeduceTest;
import games.descent2e.components.Figure;
import games.descent2e.components.Monster;
import utilities.Vector2D;

import java.util.ArrayList;
import java.util.List;

import static games.descent2e.DescentHelper.getForcedMovePositions;
import static games.descent2e.DescentHelper.inRange;

public class ZoreksFavor extends TriggerAttributeTest {

    public static String name = "Shield of Zorek's Favor";
    public ZoreksFavor(int attackingFigure, int target) {
        super(attackingFigure, List.of(target));
    }

    public ZoreksFavor(int attackingFigure, List<Integer> targets) {
        super(attackingFigure, targets);
    }

    @Override
    public boolean canExecute(DescentGameState dgs) {
        Figure f = (Figure) dgs.getComponentById(getAttackingFigure());
        if (f == null) return false;
        if (!f.hasBonus(DescentTypes.SkillBonus.ZoreksFavor)) return false;
        if (f.getAttribute(Figure.Attribute.Health).isMinimum()) return false;

        if (getTargets().isEmpty()) return false;
        Figure target = (Figure) dgs.getComponentById(getTargets().get(0));
        if (target == null) return false;

        IExtendedSequence current = dgs.currentActionInProgress();
        if (current == null) return false;
        if (!(current instanceof MeleeAttack melee)) return false;
        return melee.getDefendingFigure() == getAttackingFigure();
    }

    @Override
    public List<AbstractAction> _computeAvailableActions(AbstractGameState state) {
        int target = currentTarget();
        int attacker = getAttackingFigure();

        return List.of(new ZoreksFavorTest(attacker, Figure.Attribute.Might, target));
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
        ZoreksFavor retVal = new ZoreksFavor(getAttackingFigure(), getTargets());
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
