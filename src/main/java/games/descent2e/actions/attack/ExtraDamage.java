package games.descent2e.actions.attack;

import core.AbstractGameState;
import core.interfaces.IExtendedSequence;
import games.descent2e.DescentGameState;
import games.descent2e.DescentHelper;
import games.descent2e.actions.DescentAction;
import games.descent2e.actions.Triggers;
import games.descent2e.components.Figure;
import org.apache.spark.sql.sources.In;

import java.util.List;
import java.util.Objects;

import static games.descent2e.DescentHelper.checkAllSpaces;
import static games.descent2e.DescentHelper.getFigureIndex;

public class ExtraDamage extends DescentAction {

    int attackerID;
    int damage;
    int distance;
    List<Integer> targets;

    public static String distant = "Distant";
    public static String adjacent = "Adjacent";
    public String name;

    public ExtraDamage(int attackerID, int targetID) {
        super(Triggers.END_ATTACK);
        this.attackerID = attackerID;
        this.damage = 1;
        this.distance = 1;
        this.targets = List.of(targetID);
    }

    public ExtraDamage(int attackerID, List<Integer> targets) {
        super(Triggers.END_ATTACK);
        this.attackerID = attackerID;
        this.damage = 1;
        this.distance = 1;
        this.targets = List.copyOf(targets);
    }

    public ExtraDamage(int attackerID, int targetID, int damage) {
        super(Triggers.END_ATTACK);
        this.attackerID = attackerID;
        this.damage = damage;
        this.distance = 1;
        this.targets = List.of(targetID);
    }

    public ExtraDamage(int attackerID, int targetID, int damage, int distance) {
        super(Triggers.END_ATTACK);
        this.attackerID = attackerID;
        this.damage = damage;
        this.distance = distance;
        this.targets = List.of(targetID);
    }

    public ExtraDamage(int attackerID, List<Integer> targets, int damage, int distance) {
        super(Triggers.END_ATTACK);
        this.attackerID = attackerID;
        this.damage = damage;
        this.distance = distance;
        this.targets = List.copyOf(targets);
    }

    @Override
    public boolean execute(DescentGameState dgs) {

        Figure f = (Figure) dgs.getComponentById(attackerID);

        for (int target : targets) {
            Figure t = (Figure) dgs.getComponentById(target);
            t.getAttribute(Figure.Attribute.Health).decrement(damage);

            if (t.getAttribute(Figure.Attribute.Health).isMinimum()) {
                int index1 = getFigureIndex(dgs, t);
                int index2 = getFigureIndex(dgs, f);

                // Death
                DescentHelper.figureDeath(dgs, t);
                // Add to the list of defeated figures this turn
                dgs.addDefeatedFigure(t, index1, f, index2);
            }
        }

        dgs.removeInterruptAttack(name);

        f.addActionTaken(toString());

        return true;
    }

    @Override
    public ExtraDamage copy() {
        ExtraDamage retVal = new ExtraDamage(attackerID, targets, damage, distance);
        retVal.setName(name);
        return retVal;
    }

    @Override
    public boolean equals(Object other) {
        if (other instanceof ExtraDamage that) {
            return attackerID == that.attackerID && Objects.equals(targets, that.targets) &&
                    damage == that.damage && distance == that.distance && name.equals(that.name);
        }
        return false;
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), attackerID, targets, damage, distance, name);
    }

    @Override
    public String getString(AbstractGameState gameState) {
        String string = "Apply " + damage + " damage to " + gameState.getComponentById(targets.get(0)).toString();
        for (int i = 1; i < targets.size(); i++) {
            string += " and " + gameState.getComponentById(targets.get(i)).toString();
        }
        return string;
    }

    @Override
    public String toString() {
        String string = "Apply " + damage + " damage to " + targets.get(0);
        for (int i = 1; i < targets.size(); i++) {
            string += " and " + targets.get(i);
        }
        return string;
    }

    @Override
    public boolean canExecute(DescentGameState dgs) {

        Figure f = (Figure) dgs.getComponentById(attackerID);
        if (f == null) return false;
        if (targets.isEmpty()) return false;

        IExtendedSequence action = dgs.currentActionInProgress();
        if (!(action instanceof MeleeAttack melee)) return false;
        if (attackerID != melee.attackingFigure) return false;
        Figure originalTarget = (Figure) dgs.getComponentById(melee.defendingFigure);
        if (originalTarget == null) return false;

        for (int target : targets) {
            Figure t = (Figure) dgs.getComponentById(target);
            if (t == null) return false;
            if (!(checkAllSpaces(dgs, originalTarget, t, distance, false)))
                return false;
        }
        return true;
    }

    public void setName(String name) {
        this.name = name;
    }
}
