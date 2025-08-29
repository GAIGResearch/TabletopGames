package games.descent2e.actions.archetypeskills;

import core.AbstractGameState;
import core.components.Component;
import games.descent2e.DescentGameState;
import games.descent2e.DescentHelper;
import games.descent2e.actions.DescentAction;
import games.descent2e.actions.Triggers;
import games.descent2e.components.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import static games.descent2e.DescentHelper.getFigureIndex;
import static games.descent2e.DescentHelper.hasLineOfSight;

public class RadiantLight extends Heal {

    List<Integer> targets;

    public RadiantLight(List<Integer> targets, int cardID) {
        super(targets.get(0), -1, true, cardID);
        this.targets = new ArrayList<>(targets);
    }

    @Override
    public String getString(AbstractGameState gameState) {

        String targetName = gameState.getComponentById(targets.get(0)).getComponentName().replace("Hero: ", "");

        String string = "Radiant Light: Heal Heroes and Damage Monsters for 1 Red Power Die to " + targetName;
        for (int i = 1; i < targets.size(); i++)
            string += " and " + gameState.getComponentById(targets.get(i)).getComponentName().replace("Hero: ", "");
        if (healthRecovered > 0)
            string += " (" + healthRecovered + " Health)";
        return string;
    }

    @Override
    public String toString() {
        String string = "Radiant Light: Heal Heroes and Damage Monsters for 1 Red Power Die to " + targets.get(0);
        for (int i = 1; i < targets.size(); i++)
            string += " and " + targets.get(i);
        return string + " (" + cardID + ")";
    }

    @Override
    public boolean execute(DescentGameState dgs) {

        Figure f = dgs.getActingFigure();

        f.getNActionsExecuted().increment();
        f.getAttribute(Figure.Attribute.Fatigue).increment(3);

        Component card = dgs.getComponentById(cardID);
        if (card != null)
        {
            f.exhaustCard((DescentCard) card);
        }

        // Health recovery: Roll 1 Red Die
        DicePool.heal.roll(dgs.getRnd());

        healthRecovered = DicePool.heal.getDamage() + 50;

        for (int targetID : targets)
        {
            Figure target = (Figure) dgs.getComponentById(targetID);

            // Heal the heroes, damage the monsters
            if (target instanceof Hero)
            {
                target.incrementAttribute(Figure.Attribute.Health, healthRecovered);
                if (((Hero) target).isDefeated())
                    ((Hero) target).setDefeated(dgs, false);
            }

            if (target instanceof Monster)
            {
                target.decrementAttribute(Figure.Attribute.Health, healthRecovered);
                if (target.getAttribute(Figure.Attribute.Health).isMinimum()) {
                    // Death
                    DescentHelper.figureDeath(dgs, target);
                    // Add to the list of defeated figures this turn
                    dgs.addDefeatedFigure(target, getFigureIndex(dgs, target), f, getFigureIndex(dgs, f));
                }
            }
        }

        f.addActionTaken(getString(dgs));

        return true;
    }

    @Override
    public boolean canExecute(DescentGameState dgs) {

        if (!super.canExecute(dgs)) return false;

        if (targets.isEmpty()) return false;

        Figure f = dgs.getActingFigure();

        // This costs 3 Fatigue to use
        if (f.getAttributeValue(Figure.Attribute.Fatigue) + 3 > f.getAttributeMax(Figure.Attribute.Fatigue))
            return false;

        boolean hasInjuredAlly = false;
        boolean hasMonster = false;

        for (int targetID : targets)
        {
            Figure target = (Figure) dgs.getComponentById(targetID);
            if (target == null) return false;

            // Only need to check once for both
            if (!hasInjuredAlly)
                if ((target instanceof Hero) && !target.getAttribute(Figure.Attribute.Health).isMaximum())
                    hasInjuredAlly = true;
            if (!hasMonster)
                if (target instanceof Monster)
                    hasMonster = true;

            // We can always target ourselves
            if (targetID == f.getComponentID()) continue;

            // All targets must have line on sight to use
            if (!hasLineOfSight(dgs, f.getPosition(), target.getPosition()))
                return false;
        }

        return hasInjuredAlly || hasMonster;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;
        RadiantLight heal = (RadiantLight) o;
        return Objects.equals(targets, heal.targets);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), targets);
    }

    @Override
    public RadiantLight copy()
    {
        RadiantLight heal = new RadiantLight(targets, cardID);
        heal.healthRecovered = healthRecovered;
        return heal;
    }
}
