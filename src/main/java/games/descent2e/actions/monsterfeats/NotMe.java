package games.descent2e.actions.monsterfeats;

import core.AbstractGameState;
import core.actions.AbstractAction;
import games.descent2e.DescentGameState;
import games.descent2e.actions.AttributeTest;
import games.descent2e.actions.TriggerAttributeTest;
import games.descent2e.actions.Triggers;
import games.descent2e.components.Figure;
import games.descent2e.components.Monster;
import scala.Int;
import spire.algebra.Trig;
import utilities.Vector2D;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import static games.descent2e.DescentHelper.*;

public class NotMe extends TriggerAttributeTest {

    public static boolean hasSwapped = false;
    private int victim = -1;

    public NotMe(int attackingFigure) {
        super(attackingFigure, new ArrayList<>(), Triggers.START_ATTACK);
    }

    @Override
    public boolean canExecute(DescentGameState dgs) {

        // We can't swap if we've immediately just swapped
        if (hasSwapped) return false;

        Figure f = (Figure) dgs.getComponentById(getAttackingFigure());
        if (!(f instanceof Monster) || !(((Monster) f).hasPassive(MonsterAbilities.MonsterPassive.NOTME))) return false;

        // Splig must be adjacent to at least one Monster in order for this to activate
        List<Integer> chumps = getAdjacentTargets(dgs, f, true);
        if (chumps.isEmpty()) return false;

        for (int chump : chumps)
        {
            // Splig may be a coward, but he's not stupid enough to target himself
            if (chump == getAttackingFigure()) continue;

            Figure swap = (Figure) dgs.getComponentById(chump);
            if (swap == null) continue;
            if (swap.isOffMap()) continue;
            if (swap instanceof Monster) return true;
        }
        return false;
    }

    @Override
    public List<AbstractAction> _computeAvailableActions(AbstractGameState state) {

        int attacker = getAttackingFigure();
        // If we haven't made the test yet, get the test first
        int result = getResult();
        if (result == -1) return List.of(new NotMeTest(attacker, Figure.Attribute.Awareness));

        List<AbstractAction> retVal = new ArrayList<>();

        // Not Me! requires that Splig to have passed his test (result == 1) to proceed
        if (result == 1) {
            DescentGameState dgs = (DescentGameState) state;
            Figure splig = (Figure) dgs.getComponentById(attacker);
            List<Integer> chumps = getAdjacentTargets(dgs, splig, true);

            for (int chump : chumps)
            {
                // Splig may be a coward, but he's not stupid enough to target himself
                if (chump == getAttackingFigure()) continue;

                Figure swap = (Figure) dgs.getComponentById(chump);
                if (swap == null) continue;
                if (swap.isOffMap()) continue;
                if (swap instanceof Monster)
                {
                    NotMeSwap swapping = new NotMeSwap(chump, attacker);
                    if (swapping.canExecute(dgs))
                        retVal.add(swapping);
                }
            }

            if (!retVal.isEmpty())
            {
                NotMeSwap swapping = new NotMeSwap(attacker, attacker);
                if (swapping.canExecute(dgs))
                    retVal.add(swapping);
            }

        }

        return retVal;
    }

    @Override
    public void _afterAction(AbstractGameState state, AbstractAction action) {
        if (action instanceof AttributeTest) {
            setResult(((AttributeTest) action).getResult());
            // If Splig failed his test, we finish here
            if (getResult() == 0) {
                setFinished(true);
                hasSwapped = true;
                setVictim(attackingFigure);
            }
        }
        if (action instanceof NotMeSwap)
        {
            hasSwapped = true;
            setVictim(((NotMeSwap) action).getTarget());
            setFinished(true);
        }
    }

    @Override
    public int hashCode()
    {
        return Objects.hash(super.hashCode(), hasSwapped, victim);
    }

    @Override
    public int getCurrentPlayer(AbstractGameState state) {
        // As Not Me! only tests Splig, this should always return the Overlord
        Figure splig = (Figure) state.getComponentById(attackingFigure);
        return splig.getOwnerId();
    }

    @Override
    public TriggerAttributeTest copy() {
        NotMe retVal = new NotMe(getAttackingFigure());
        copyComponentTo(retVal);
        return retVal;
    }

    @Override
    public void copyComponentTo(TriggerAttributeTest retVal)
    {
        super.copyComponentTo(retVal);
        ((NotMe) retVal).victim = victim;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;
        NotMe swap = (NotMe) o;
        return swap.victim == victim;
    }

    @Override
    public String toString() {
        return "Not Me! by " + attackingFigure;
    }

    @Override
    public String getString(AbstractGameState gameState) {
        return "Not Me! by " + gameState.getComponentById(attackingFigure).getComponentName().replace("Hero: ", "");
    }

    public static void setSwapped(boolean swap)
    {
        hasSwapped = swap;
    }

    public static boolean getSwapped()
    {
        return hasSwapped;
    }

    public int getVictim()
    {
        return victim;
    }

    public void setVictim(int victim)
    {
        this.victim = victim;
    }
}
