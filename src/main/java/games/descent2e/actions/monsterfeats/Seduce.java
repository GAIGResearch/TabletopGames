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

public class Seduce extends TriggerAttributeTest {

    protected final int distance = 1; // How far Eliza can move the target
    public Seduce(int attackingFigure, int target) {
        super(attackingFigure, List.of(target));
    }

    public Seduce(int attackingFigure, List<Integer> targets) {
        super(attackingFigure, targets);
    }

    @Override
    public boolean canExecute(DescentGameState dgs) {
        Figure f = (Figure) dgs.getComponentById(getAttackingFigure());
        if (!(f instanceof Monster) || !(((Monster) f).hasAction(MonsterAbilities.MonsterAbility.SEDUCE))) return false;

        if (getTargets().isEmpty()) return false;
        Figure target = (Figure) dgs.getComponentById(getTargets().get(0));
        if (target == null) return false;
        return inRange(f.getPosition(), target.getPosition(), 3) && !target.isOffMap();
    }

    @Override
    public List<AbstractAction> _computeAvailableActions(AbstractGameState state) {
        int target = currentTarget();
        int attacker = getAttackingFigure();

        // If we haven't made the test yet, get the test first
        int result = getResult();
        if (result == -1) return List.of(new SeduceTest(attacker, Figure.Attribute.Willpower, target));

        List<AbstractAction> retVal = new ArrayList<>();

        // Seduce requires that Lady Eliza to have passed her test (result == 1) to proceed
        if (result == 1) {
            DescentGameState dgs = (DescentGameState) state;
            Vector2D startPos = ((Figure) dgs.getComponentById(target)).getPosition();
            List<Vector2D> spaces = getForcedMovePositions(dgs, startPos, distance);
            for (Vector2D pos: spaces) {
                SeduceMove seduceMove = new SeduceMove(target, attackingFigure, startPos, pos);
                if (seduceMove.canExecute(dgs))
                    retVal.add(seduceMove);
            }
        }

        return retVal;
    }

    @Override
    public void _afterAction(AbstractGameState state, AbstractAction action) {
        if (action instanceof AttributeTest) {
            setResult(((AttributeTest) action).getResult());
            // If Eliza failed her test, we finish here
            if (getResult() == 0) setFinished(true);
        }
        if (action instanceof ForcedMove)
        {
            setFinished(true);
        }
    }

    @Override
    public TriggerAttributeTest copy() {
        Seduce retVal = new Seduce(getAttackingFigure(), getTargets());
        copyComponentTo(retVal);
        return retVal;
    }

    @Override
    public String toString() {
        return super.toString().replace("Call Attribute Test", "Seduce");
    }

    @Override
    public String getString(AbstractGameState gameState) {
        return super.getString(gameState).replace("Call Attribute Test", "Seduce");
    }
}
