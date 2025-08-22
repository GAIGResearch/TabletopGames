package games.descent2e.actions.monsterfeats;

import core.AbstractGameState;
import core.actions.AbstractAction;
import core.actions.DoNothing;
import games.descent2e.DescentGameState;
import games.descent2e.actions.AttributeTest;
import games.descent2e.actions.Move;
import games.descent2e.actions.TriggerAttributeTest;
import games.descent2e.components.Figure;
import games.descent2e.components.Monster;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import static games.descent2e.DescentHelper.*;

public class Overpower extends TriggerAttributeTest {

    boolean canTest = false;
    int currentTarget = -1;
    int testIndex = 0;
    int testMax = 3;
    int range;
    public Overpower(int attackingFigure, List<Integer> targets, int range) {
        super(attackingFigure, targets);
        this.range = range;
    }

    @Override
    public boolean execute(DescentGameState dgs) {
        Figure alric = (Figure) dgs.getComponentById(getAttackingFigure());
        testMax = alric.getAttributeMax(Figure.Attribute.MovePoints);
        // As this can be interrupted by other Move actions, we take however many points he had originally and go from there
        alric.setAttribute(Figure.Attribute.MovePoints, range);

        // If Alric starts adjacent to at least one Hero, we can immediately call a test
        List<Integer> adj = getAdjacentTargets(dgs, alric, false);
        canTest = !adj.isEmpty();

        return super.execute(dgs);
    }

    @Override
    public boolean canExecute(DescentGameState dgs) {
        Figure f = (Figure) dgs.getComponentById(getAttackingFigure());
        if (!(f instanceof Monster) || !(((Monster) f).hasAction(MonsterAbilities.MonsterAbility.OVERPOWER))) return false;

        if (getTargets().isEmpty()) return false;

        // Overpower works on targets adjacent to Sir Alric
        // So we need to check that, at the end of his movement, he would at least have one Hero he could be adjacent to
        boolean inRange = false;

        for (int targetId : getTargets()) {
            Figure target = (Figure) dgs.getComponentById(targetId);
            if (target == null) return false;
            if (inRange) continue;
            if (!target.isOffMap() && inRange(f.getPosition(), target.getPosition(), range)) {
                inRange = true;
            }
        }
        return inRange;
    }

    @Override
    public List<AbstractAction> _computeAvailableActions(AbstractGameState state) {

        int attacker = getAttackingFigure();
        Figure alric = (Figure) state.getComponentById(attacker);

        int currentTarget = getTarget();
        if (currentTarget != -1)
        {
            Figure target = (Figure) state.getComponentById(currentTarget);
            return List.of(new OverpowerMove(currentTarget, attacker, target.getPosition(), alric.getPosition()));
        }

        List<AbstractAction> retVal = new ArrayList<>();

        DescentGameState dgs = (DescentGameState) state;

        if (canTest)
        {
            List<Integer> adj = getAdjacentTargets(dgs, alric, false);
            for (int target : adj) {
                Figure f = (Figure) state.getComponentById(target);
                if (f == null || f.isOffMap()) continue;
                if (!targets.contains(target)) continue;    // Makes sure we can only test against Heroes we can swap with

                OverpowerTest overpowerTest = new OverpowerTest(attacker, Figure.Attribute.Might, target);
                retVal.add(overpowerTest);
            }
            if (!retVal.isEmpty())
                // AttributeTestSkip disables the test for that movement, as we don't have to test every time we end up adjacent to a Hero
                retVal.add(new AttributeTestSkip());

            return retVal;
        }

        List<AbstractAction> movement = moveActions(dgs, alric);
        retVal.addAll(movement);

        // If Alric has attempted at least one test, he can end Overpower prematurely
        if (testIndex > 0) retVal.add(new OverpowerStop());

        if(retVal.isEmpty())
            retVal.add(new DoNothing());

        return retVal;
    }

    @Override
    public void _afterAction(AbstractGameState state, AbstractAction action) {
        Figure f = (Figure) state.getComponentById(getAttackingFigure());

        if (action instanceof Move) {
            // If Alric is adjacent to at least one Hero, we can initiate a test
            canTest = false;
            List<Integer> adj = getAdjacentTargets((DescentGameState) state, f, false);
            for (int target : adj)
            {
                // Check to make sure we haven't killed the target in our previous actions
                Figure t = (Figure) state.getComponentById(target);
                if (t == null || t.isOffMap()) continue;
                if (!getTargets().contains(target)) continue; // We can only test against Heroes we can swap with
                // If we have a valid target, we can test
                canTest = true;
                break;
            }
        }

        if (action instanceof OverpowerTest)
        {
            setResult(((AttributeTest) action).getResult());
            canTest = false; // We can only test once per movement, so we disable it to prevent swap chaining

            // On a success, we set our victim to the test's target
            if(getResult() == 1) setTarget(((OverpowerTest) action).getTargetFigure());
            else {
                resetTarget();  // Reset the target if we failed the test
                testIndex++;    // Increment here only on a failed test
            }
        }

        // DoNothing covers both AttributeTestSkip and OverpowerStop
        if (action instanceof ForcedMove || action instanceof DoNothing)
        {
            if (action instanceof ForcedMove) testIndex++; // Increment here only on a successful test
            resetResult();
            resetTarget();
            canTest = false;
        }

        // If Alric wants to stop early, end Overpower here
        // We still retain the movement if we so desire, we just can no longer make tests
        if (action instanceof OverpowerStop)
        {
            setFinished(true);
        }

        // We only finish if there are no more valid targets for tests,
        // and either we have made the maximum number of tests or we have run out of movement
        if (!canTest && (currentTarget == -1) &&
                ((testIndex >= testMax) || f.getAttribute(Figure.Attribute.MovePoints).isMinimum()))
        {
            setFinished(true);
        }
    }

    @Override
    public int getCurrentPlayer(AbstractGameState state) {
        // As Overpower only tests Sir Alric, this should always return the Overlord
        Figure alric = (Figure) state.getComponentById(attackingFigure);
        return alric.getOwnerId();
    }

    @Override
    public TriggerAttributeTest copy() {
        Overpower retVal = new Overpower(getAttackingFigure(), getTargets(), range);
        copyComponentTo(retVal);
        return retVal;
    }

    @Override
    public void copyComponentTo(TriggerAttributeTest retVal) {
        super.copyComponentTo(retVal);
        ((Overpower) retVal).canTest = this.canTest;
        ((Overpower) retVal).currentTarget = this.currentTarget;
        ((Overpower) retVal).testIndex = this.testIndex;
        ((Overpower) retVal).testMax = this.testMax;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof Overpower overpower) {
            return super.equals(overpower) &&
                    this.canTest == overpower.canTest &&
                    this.currentTarget == overpower.currentTarget &&
                    this.testIndex == overpower.testIndex &&
                    this.range == overpower.range &&
                    this.testMax == overpower.testMax;
        } else {
            return false;
        }
    }

    public void setTarget(int target)
    {
        this.currentTarget = target;
    }

    public void resetTarget() {
        this.currentTarget = -1;
    }

    public int getTarget() {
        return currentTarget;
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), canTest, currentTarget, testIndex, range, testMax);
    }

    @Override
    public String toString() {
        return "Overpower: Move and can make (Might) Tests to force swap with adjacent Heroes";
    }

    @Override
    public String getString(AbstractGameState gameState) {
        String alricName = ((Figure) gameState.getComponentById(attackingFigure)).getName().replace("Hero: ", "");
        return "Overpower: " + alricName + " can move and make (Might) Tests to swap with adjacent Heroes";
    }
}
