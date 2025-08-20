package games.descent2e.actions.monsterfeats;

import core.AbstractGameState;
import games.descent2e.actions.attack.RangedAttack;

public class ShadowBolt extends RangedAttack {
    public ShadowBolt(int attackingFigure, int defendingFigure) {
        super(attackingFigure, defendingFigure);
    }

    @Override
    public String shortString(AbstractGameState gameState) {
        return super.shortString(gameState).replace("Ranged Attack", "Shadow Bolt");
    }

    @Override
    public String longString(AbstractGameState gameState) {
        return super.longString(gameState).replace("Ranged Attack", "Shadow Bolt");
    }

    @Override
    public String toString() {
        return String.format("Shadow Bolt by %d on %d", attackingFigure, defendingFigure);
    }
}
