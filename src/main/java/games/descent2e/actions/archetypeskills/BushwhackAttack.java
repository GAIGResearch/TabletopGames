package games.descent2e.actions.archetypeskills;

import core.AbstractGameState;
import games.descent2e.actions.attack.FreeAttack;
import games.descent2e.actions.monsterfeats.Air;
import games.descent2e.components.DescentCard;
import games.descent2e.components.Figure;
import games.descent2e.components.Monster;

import java.util.List;
import java.util.Objects;

import static games.descent2e.DescentHelper.checkAllSpaces;

public class BushwhackAttack extends FreeAttack {

    int cardID = -1;

    public BushwhackAttack(int attackingFigure, int defendingFigure, boolean isMelee, boolean reach, int cardID) {
        super(attackingFigure, defendingFigure, isMelee, reach);
        this.cardID = cardID;
    }

    @Override
    public boolean execute(games.descent2e.DescentGameState state) {
        Figure f = (Figure) state.getComponentById(attackingFigure);
        f.getAttribute(Figure.Attribute.Fatigue).increment();
        boolean oldExtraAttack = f.hasUsedExtraAction();
        DescentCard card = (DescentCard) state.getComponentById(cardID);
        if (card != null)
            f.exhaustCard(card);

        super.execute(state);

        f.setUsedExtraAction(oldExtraAttack);
        return true;
    }

    @Override
    public boolean canExecute(games.descent2e.DescentGameState dgs) {
        Figure f = dgs.getActingFigure();
        if (f == null) return false;
        if (f.getAttribute(Figure.Attribute.Fatigue).isMaximum()) return false;

        DescentCard card = (DescentCard) dgs.getComponentById(cardID);
        if (card == null) return false;
        if (f.isExhausted(card)) return false;

        Figure target = (Figure) dgs.getComponentById(defendingFigure);
        if (target == null) return false;

        if (Air.checkAir(dgs, f, target)) {
            // If the target has the Air Immunity passive and we are not adjacent, we cannot attack them
            return false;
        }

        if (!checkAllSpaces(dgs, f, target, getRange(), true)) return false;

        for (List<Monster> monsters : dgs.getMonsters())
        {
            for (Monster monster : monsters)
            {
                // We can only use Bushwhack if we only have one monster in our line of sight
                if (monster.getComponentID() == defendingFigure) continue;
                if (checkAllSpaces(dgs, f, monster, Integer.MAX_VALUE, true))
                    return false;
            }
        }
        return true;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof BushwhackAttack other) {
            if (other.cardID == this.cardID)
                return super.equals(obj);
        }
        return false;
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), cardID);
    }

    @Override
    public String getString(AbstractGameState gameState) {
        return super.getString(gameState).replace("Free ", "Bushwhack ");
    }

    @Override
    public String toString() {
        return super.toString().replace("Free ", "Bushwhack ");
    }

    public BushwhackAttack copy() {
        BushwhackAttack retValue = new BushwhackAttack(attackingFigure, defendingFigure, isMelee, hasReach, cardID);
        copyComponentTo(retValue);
        return retValue;
    }
}
