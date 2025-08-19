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

public class Seduce extends TriggerAttributeTest {

    protected final int distance = 1; // How far Eliza can move the target
    public Seduce(int attackingFigure, int target) {
        super(attackingFigure, target);
    }

    @Override
    public String getString(AbstractGameState gameState) {

        String attackerName = ((Figure) gameState.getComponentById(attackingFigure)).getName().replace("Hero: ", "");;
        String defenderName = ((Figure) gameState.getComponentById(defendingFigure)).getName().replace("Hero: ", "");

        return "Seduce by " + attackerName + " on " + defenderName;
    }

    @Override
    public String toString() {
        return String.format("Seduce by %d on %d", attackingFigure, defendingFigure);
    }

    @Override
    public boolean execute(DescentGameState state) {
        super.execute(state);
        Figure eliza = (Figure) state.getComponentById(attackingFigure);
        eliza.getNActionsExecuted().increment();
        eliza.setHasAttacked(true);
        eliza.addActionTaken(toString());

        return true;
    }

    @Override
    public List<AbstractAction> _computeAvailableActions(AbstractGameState state) {
        if (phase.interrupt == null)
            throw new AssertionError("Should not be reachable");
        DescentGameState dgs = (DescentGameState) state;
        Monster eliza = (Monster) dgs.getComponentById(attackingFigure);
        List<AbstractAction> retVal = new ArrayList<>();

        // System.out.println(((Figure) dgs.getComponentById(defendingFigure)).getName());

        SeduceTest seduceTest = new SeduceTest(attackingFigure, Figure.Attribute.Willpower, defendingFigure, eliza.getNActionsExecuted().getValue());
        if (seduceTest.canExecute(dgs)) {
            retVal.add(seduceTest);
        }

        Figure target = (Figure) dgs.getComponentById(defendingFigure);
        if (target.isOffMap()) {
            Set<BoardNode> targetSpaces = getNeighboursInRange(dgs, target.getPosition(), distance);

            for (BoardNode space : targetSpaces) {
                if (space == null) continue;
                Vector2D pos = ((PropertyVector2D) space.getProperty("coordinates")).values;
                SeduceMove seduceMove = new SeduceMove(defendingFigure, attackingFigure, pos);
                if (seduceMove.canExecute(dgs))
                    retVal.add(seduceMove);
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
    public Seduce copy() {
        Seduce retVal = new Seduce(attackingFigure, defendingFigure);
        super.copyComponentTo(retVal);
        return retVal;
    }

    @Override
    public boolean canExecute(DescentGameState dgs) {
        Figure f = (Figure) dgs.getComponentById(attackingFigure);
        if (!(f instanceof Monster)) return false;
        if (!((Monster) f).hasAction(MonsterAbilities.MonsterAbility.SEDUCE)) return false;
        Figure target = (Figure) dgs.getComponentById(defendingFigure);
        if (target == null) return false;
        return inRange(f.getPosition(), target.getPosition(), 3) && !target.isOffMap();
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
