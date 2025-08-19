package games.descent2e.actions.monsterfeats;

import core.AbstractGameState;
import games.descent2e.DescentGameState;
import games.descent2e.actions.herofeats.AttackAllAdjacent;
import games.descent2e.components.Figure;

import java.util.List;

import static games.descent2e.DescentHelper.checkAdjacent;

public class Ignite extends Fire {

    public Ignite(int attackingFigure, List<Integer> defendingFigures) {
        super(attackingFigure, defendingFigures);
    }

    @Override
    public boolean canExecute(DescentGameState dgs) {

        // Lord Merick cannot perform this attack if the -1 HP recoil would defeat him
        Figure merick = (Figure) dgs.getComponentById(attackingFigure);
        if (merick.getAttribute(Figure.Attribute.Health).getValue() <= 1) return false;
        return super.canExecute(dgs);
    }

    @Override
    public String getString(AbstractGameState gameState) {
        return "Ignite: Attack all adjacent figures" + result;
    }

    @Override
    public String toString() {
        return "Ignite: Attack all adjacent figures";
    }
}
