package games.descent2e.actions.archetypeskills;

import core.AbstractGameState;
import games.descent2e.DescentGameState;
import games.descent2e.DescentTypes;
import games.descent2e.actions.attack.BlastAttack;
import games.descent2e.actions.attack.RangedAttack;
import games.descent2e.components.Figure;

public class ExplodingRune extends RangedAttack {

    public ExplodingRune(int attackingFigure, int defendingFigure) {
        super(attackingFigure, defendingFigure);
    }

    public ExplodingRune(int attackingFigure, int defendingFigure, boolean isMelee, boolean hasReach) {
        super(attackingFigure, defendingFigure);
        this.isMelee = isMelee;
        this.hasReach = hasReach;
    }

    @Override
    public boolean execute(DescentGameState dgs) {
        Figure f = (Figure) dgs.getComponentById(attackingFigure);
        f.getAttribute(Figure.Attribute.Fatigue).increment();
        super.execute(dgs);
        return true;
    }

    @Override
    public boolean canExecute(DescentGameState dgs)
    {
        Figure f = (Figure) dgs.getComponentById(attackingFigure);
        if (f.getAttribute(Figure.Attribute.Fatigue).isMaximum()) return false;
        return super.canExecute(dgs);
    }

    @Override
    public void removeInterruptAttacks(DescentGameState dgs) {
        // Make sure that, when we reset Interrupt Attacks, we add in this skill's Blast ability
        super.removeInterruptAttacks(dgs);
        addInterruptAttack(dgs, BlastAttack.name);
    }

    @Override
    public ExplodingRune copy() {
        ExplodingRune retVal = new ExplodingRune(attackingFigure, defendingFigure, isMelee, hasReach);
        copyComponentTo(retVal);
        return retVal;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof ExplodingRune) {
            return super.equals(obj);
        }
        return false;
    }

    @Override
    public int hashCode() {
        return super.hashCode();
    }

    @Override
    public String shortString(AbstractGameState gameState) {
        return super.shortString(gameState).replace("Ranged Attack by", "Exploding Rune by");
    }

    @Override
    public String longString(AbstractGameState gameState) {
        return super.longString(gameState).replace("Ranged Attack by", "Exploding Rune by");
    }

    @Override
    public String toString() {
        return super.toString().replace("Ranged Attack by", "Exploding Rune by");
    }
}
