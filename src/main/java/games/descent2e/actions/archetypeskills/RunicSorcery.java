package games.descent2e.actions.archetypeskills;

import core.AbstractGameState;
import games.descent2e.DescentGameState;
import games.descent2e.DescentTypes;
import games.descent2e.actions.attack.RangedAttack;
import games.descent2e.components.Figure;

public class RunicSorcery extends RangedAttack {

    public RunicSorcery(int attackingFigure, int defendingFigure) {
        super(attackingFigure, defendingFigure);
    }

    public RunicSorcery(int attackingFigure, int defendingFigure, boolean isMelee, boolean hasReach) {
        super(attackingFigure, defendingFigure);
        this.isMelee = isMelee;
        this.hasReach = hasReach;
    }

    @Override
    public boolean execute(DescentGameState dgs) {
        Figure f = (Figure) dgs.getComponentById(attackingFigure);
        f.getAttribute(Figure.Attribute.Fatigue).increment();
        setSubdue(true);
        super.execute(dgs);
        return true;
    }

    @Override
    public boolean canExecute(DescentGameState dgs)
    {
        Figure f = (Figure) dgs.getComponentById(attackingFigure);
        if (f.getAttribute(Figure.Attribute.Fatigue).isMaximum()) return false;

        Figure target = (Figure) dgs.getComponentById(defendingFigure);
        boolean status = false;

        for (DescentTypes.DescentCondition condition : DescentTypes.DescentCondition.values())
        {
            if (!target.hasCondition(condition)) {
                status = true;
                break;
            }
        }

        if (!status) return false;

        return super.canExecute(dgs);

    }

    @Override
    public RunicSorcery copy() {
        RunicSorcery retVal = new RunicSorcery(attackingFigure, defendingFigure, isMelee, hasReach);
        copyComponentTo(retVal);
        return retVal;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof RunicSorcery) {
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
        return super.shortString(gameState).replace("Ranged Attack by", "Runic Sorcery by");
    }

    @Override
    public String longString(AbstractGameState gameState) {
        return super.longString(gameState).replace("Ranged Attack by", "Runic Sorcery by");
    }

    @Override
    public String toString() {
        return super.toString().replace("Ranged Attack by", "Runic Sorcery by");
    }
}
