package games.descent2e.actions.monsterfeats;

import core.AbstractGameState;
import core.actions.AbstractAction;
import core.components.BoardNode;
import core.properties.PropertyVector2D;
import games.descent2e.DescentGameState;
import games.descent2e.actions.attack.TriggerAttributeTest;
import games.descent2e.components.Figure;
import games.descent2e.components.Monster;
import utilities.Vector2D;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import static games.descent2e.DescentHelper.*;

public class Dominion extends TriggerAttributeTest {

    protected final int distance = 2; // How far Zachareth can move the target
    public Dominion(int attackingFigure, int target) {
        super(attackingFigure, target);
    }

    @Override
    public String getString(AbstractGameState gameState) {

        String attackerName = ((Figure) gameState.getComponentById(attackingFigure)).getName().replace("Hero: ", "");;
        String defenderName = ((Figure) gameState.getComponentById(defendingFigure)).getName().replace("Hero: ", "");

        return "Dominion by " + attackerName + " on " + defenderName;
    }

    @Override
    public String toString() {
        return String.format("Dominion by %d on %d", attackingFigure, defendingFigure);
    }

    @Override
    public boolean execute(DescentGameState state) {
        super.execute(state);
        Figure zachareth = (Figure) state.getComponentById(attackingFigure);
        zachareth.getNActionsExecuted().increment();
        zachareth.addActionTaken(toString());

        return true;
    }

    @Override
    public List<AbstractAction> _computeAvailableActions(AbstractGameState state) {
        if (phase.interrupt == null)
            throw new AssertionError("Should not be reachable");
        DescentGameState dgs = (DescentGameState) state;
        Monster zachareth = (Monster) dgs.getComponentById(attackingFigure);
        List<AbstractAction> retVal = new ArrayList<>();

        // System.out.println(((Figure) dgs.getComponentById(defendingFigure)).getName());

        DominionTest dominionTest = new DominionTest(attackingFigure, Figure.Attribute.Willpower, defendingFigure, zachareth.getNActionsExecuted().getValue());
        if (dominionTest.canExecute(dgs)) {
            retVal.add(dominionTest);
        }

        Figure target = (Figure) dgs.getComponentById(defendingFigure);
        if (target.isOffMap()) {
            Set<BoardNode> targetSpaces = getNeighboursInRange(dgs, target.getPosition(), distance);

            for (BoardNode space : targetSpaces) {
                if (space == null) continue;
                Vector2D pos = ((PropertyVector2D) space.getProperty("coordinates")).values;
                DominionMove dominionMove = new DominionMove(defendingFigure, attackingFigure, pos);
                if (dominionMove.canExecute(dgs))
                    retVal.add(dominionMove);
            }
        }

        if (retVal.isEmpty())
        {
            List<AbstractAction> superRetVal = super._computeAvailableActions(state);
            if (superRetVal != null && !superRetVal.isEmpty())
            {
                retVal.addAll(superRetVal);
            }
        }

        return retVal;
    }

    @Override
    public Dominion copy() {
        Dominion retVal = new Dominion(attackingFigure, defendingFigure);
        super.copyComponentTo(retVal);
        return retVal;
    }

    @Override
    public boolean canExecute(DescentGameState dgs) {
        Figure f = (Figure) dgs.getComponentById(attackingFigure);
        if (!(f instanceof Monster)) return false;
        if (!((Monster) f).hasAction(MonsterAbilities.MonsterAbility.DOMINION)) return false;
        Figure target = (Figure) dgs.getComponentById(defendingFigure);
        if (target == null) return false;
        return hasLineOfSight(dgs, f.getPosition(), target.getPosition());
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        return super.equals(o);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode());
    }
}
