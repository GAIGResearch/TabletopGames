package games.descent2e.actions.monsterfeats;

import core.AbstractGameState;
import core.actions.AbstractAction;
import games.descent2e.DescentGameState;
import games.descent2e.actions.AttributeTest;
import games.descent2e.actions.TriggerAttributeTest;
import games.descent2e.components.Figure;
import games.descent2e.components.Monster;
import utilities.Vector2D;

import java.util.*;

import static games.descent2e.DescentHelper.*;

public class Throw extends TriggerAttributeTest {

    protected final int distance = 3; // How far the Monster can throw the target
    public Throw(int attackingFigure, int target) {
        super(attackingFigure, List.of(target));
    }

    public Throw(int attackingFigure, List<Integer> targets) {
        super(attackingFigure, targets);
    }

    @Override
    public boolean canExecute(DescentGameState dgs) {
        Figure f = (Figure) dgs.getComponentById(getAttackingFigure());
        if (!(f instanceof Monster) || !(((Monster) f).hasAction(MonsterAbilities.MonsterAbility.THROW))) return false;

        if (getTargets().isEmpty()) return false;
        Figure target = (Figure) dgs.getComponentById(getTargets().get(0));
        if (target == null) return false;
        return checkAdjacent(dgs, f, target);

    }

    @Override
    public List<AbstractAction> _computeAvailableActions(AbstractGameState state) {
        int target = currentTarget();
        int attacker = getAttackingFigure();

        // If we haven't made the test yet, get the test first
        int result = getResult();
        if (result == -1) return List.of(new ThrowTest(target, Figure.Attribute.Might, attacker));

        List<AbstractAction> retVal = new ArrayList<>();

        // Throw requires the target Hero to have failed its test to proceed
        if (result == 0) {
            DescentGameState dgs = (DescentGameState) state;
            Vector2D startPos = ((Figure) dgs.getComponentById(target)).getPosition();
            List<Vector2D> spaces = getForcedMovePositions(dgs, startPos, distance);
            for (Vector2D pos: spaces) {
                ThrowMove throwMove = new ThrowMove(target, attackingFigure, startPos, pos);
                if (throwMove.canExecute(dgs))
                    retVal.add(throwMove);
            }
        }

        return retVal;
    }

    @Override
    public void _afterAction(AbstractGameState state, AbstractAction action) {
        if (action instanceof AttributeTest) {
            setResult(((AttributeTest) action).getResult());
        }
        if (action instanceof ForcedMove)
        {
            setFinished(true);
        }
    }

    @Override
    public int getCurrentPlayer(AbstractGameState state) {
        // Throw has two parts - the initial test (the target Hero) and the forced movement on a failure (the Monster)
        int id = getResult() == 0 ? getAttackingFigure() : currentTarget();
        Figure f = (Figure) state.getComponentById(id);
        return f.getOwnerId();
    }

    @Override
    public TriggerAttributeTest copy() {
        Throw retVal = new Throw(getAttackingFigure(), getTargets());
        copyComponentTo(retVal);
        return retVal;
    }

    @Override
    public String toString() {
        return super.toString().replace("Call Attribute Test", "Throw");
    }

    @Override
    public String getString(AbstractGameState gameState) {
        return super.getString(gameState).replace("Call Attribute Test", "Throw");
    }
}
