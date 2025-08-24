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
import java.util.Objects;

import static games.descent2e.DescentHelper.*;

public class Dominion extends TriggerAttributeTest {

    protected final int distance = 2; // How far Zachareth can move the target
    boolean partTwo = false;
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
        // If we haven't made the test yet, get the test first
        // Which test we call for depends upon whether this is the first test of the action (Zachareth testing himself)
        // or the second test (Zachareth testing the target)
        int result = getResult();
        if (result == -1) return partTwo ? List.of(new DominionTest(target, Figure.Attribute.Willpower, attacker, true)) : List.of(new DominionTest(target, Figure.Attribute.Willpower, attacker));

        List<AbstractAction> retVal = new ArrayList<>();

        // Dominion requires that Baron Zachareth to have passed his test (result == 1) to proceed
        if (result == 1) {
            DescentGameState dgs = (DescentGameState) state;
            Vector2D startPos = ((Figure) dgs.getComponentById(target)).getPosition();
            List<Vector2D> spaces = getForcedMovePositions(dgs, startPos, distance);
            for (Vector2D pos: spaces) {
                DominionMove dominionMove = new DominionMove(target, attackingFigure, startPos, pos, distance);
                if (dominionMove.canExecute(dgs))
                    retVal.add(dominionMove);
            }
        }

        return retVal;
    }

    @Override
    public void _afterAction(AbstractGameState state, AbstractAction action) {
        if (action instanceof AttributeTest) {
            setResult(((AttributeTest) action).getResult());
            // If Zachareth failed his test (result == 0), we finish here
            // We also finish after the second test, regardless of result
            if (getResult() == 0 || partTwo) setFinished(true);
        }
        if (action instanceof ForcedMove)
        {
            partTwo = true; // After the first move, we are now in the second part of the action
            resetResult();
        }
    }

    @Override
    public int getCurrentPlayer(AbstractGameState state) {
        // Dominion has two parts - the initial test and forced movement (Baron Zachareth) and the second test (the target Hero)
        int id = partTwo ? currentTarget() : getAttackingFigure();
        Figure f = (Figure) state.getComponentById(id);
        return f.getOwnerId();
    }

    @Override
    public TriggerAttributeTest copy() {
        Dominion retVal = new Dominion(getAttackingFigure(), getTargets());
        copyComponentTo(retVal);
        return retVal;
    }

    @Override
    public void copyComponentTo(TriggerAttributeTest retVal) {
        super.copyComponentTo(retVal);
        ((Dominion) retVal).partTwo = this.partTwo;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof Dominion dominion) {
            return super.equals(dominion) && this.partTwo == dominion.partTwo;
        } else {
            return false;
        }
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), partTwo);
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
