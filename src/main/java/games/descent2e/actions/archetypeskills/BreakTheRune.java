package games.descent2e.actions.archetypeskills;

import core.AbstractGameState;
import games.descent2e.DescentGameState;
import games.descent2e.actions.attack.MultiAttack;
import games.descent2e.components.Figure;
import games.descent2e.components.Monster;

import java.util.List;
import java.util.Objects;

public class BreakTheRune extends MultiAttack {
    public BreakTheRune(int attackingFigure, List<Integer> defendingFigures) {
        super(attackingFigure, defendingFigures);
        this.minRange = Integer.MIN_VALUE;
    }

    public boolean execute (DescentGameState state)
    {
        Figure f = (Figure) state.getComponentById(attackingFigure);
        f.incrementAttribute(Figure.Attribute.Fatigue, 4);
        super.execute(state);
        return true;
    }

    @Override
    public BreakTheRune copy() {
        BreakTheRune retValue = new BreakTheRune(attackingFigure, defendingFigures);
        copyComponentTo(retValue);
        return retValue;
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode());
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        return super.equals(o);
    }

    @Override
    public String getString(AbstractGameState gameState)
    {
        Figure attacker = (Figure) gameState.getComponentById(attackingFigure);

        attackerName = attacker.getComponentName().replace("Hero: ", "");

        String string = String.format("Break the Rune by " + attackerName + " on ");

        for (int i = 0; i < defendingFigures.size(); i++) {
            Figure defender = (Figure) gameState.getComponentById(defendingFigures.get(i));
            defenderName = defender.getComponentName().replace("Hero: ", "");
            string += defenderName;

            if (i < defendingFigures.size() - 1) {
                string += " and ";
            }
        }

        string += "; " + result;

        return string;
    }

    @Override
    public String toString()
    {
        String string = super.toString();
        return string.replace("Multi Attack by", "Break the Rune by");
    }

    @Override
    public boolean canExecute(DescentGameState dgs)
    {
        // This costs 4 Fatigue to execute
        Figure f = (Figure) dgs.getComponentById(attackingFigure);
        if (f.getAttributeValue(Figure.Attribute.Fatigue) + 4 > f.getAttributeMax(Figure.Attribute.Fatigue)) return false;
        // We only enable this if there are at least two Monsters
        // Otherwise, why not just make a regular Ranged Attack and avoid risking friendly fire?
        int hasMonster = 0;
        int minimum = 2;

        for (int target : defendingFigures)
        {
            if (dgs.getComponentById(target) instanceof Monster)
                hasMonster++;
        }

        if (hasMonster < minimum) return false;

        // Otherwise treat it like any other Multi Attack
        return super.canExecute(dgs);
    }
}
