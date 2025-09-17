package games.descent2e.actions.archetypeskills;

import core.AbstractGameState;
import games.descent2e.DescentGameState;
import games.descent2e.DescentTypes;
import games.descent2e.actions.attack.FreeAttack;
import games.descent2e.actions.attack.MeleeAttack;
import games.descent2e.actions.monsterfeats.Air;
import games.descent2e.components.DescentCard;
import games.descent2e.components.Figure;
import games.descent2e.components.Hero;

import java.util.Objects;

import static games.descent2e.DescentHelper.checkAdjacent;
import static games.descent2e.DescentHelper.checkAllSpaces;

public class CounterAttack extends FreeAttack {

    public static int cardID = -1;

    public CounterAttack(int attackingFigure, int defendingFigure) {
        super(attackingFigure, defendingFigure, true, false);
    }

    @Override
    public boolean execute(DescentGameState state) {
        Figure attacker = (Figure) state.getComponentById(attackingFigure);
        boolean oldExtra = attacker.hasUsedExtraAction();
        attacker.getAttribute(Figure.Attribute.Fatigue).increment();
        attacker.setUsedExtraAction(false);
        super.execute(state);
        attacker.setUsedExtraAction(oldExtra);

        DescentCard card = (DescentCard) state.getComponentById(cardID);
        if (card != null) {
            attacker.exhaustCard(card);
        }
        attacker.addActionTaken(toString());

        return true;
    }

    @Override
    public boolean canExecute(DescentGameState dgs) {
        if (!dgs.isActionInProgress()) return false;
        if (!(dgs.currentActionInProgress() instanceof MeleeAttack)) return false;

        Figure f = (Figure) dgs.getComponentById(attackingFigure);
        if (f == null) return false;
        if (!f.hasBonus(DescentTypes.SkillBonus.CounterAttack)) return false;
        if (f instanceof Hero hero)
            if (hero.isDefeated()) return false;

        if (f.getAttribute(Figure.Attribute.Fatigue).isMaximum()) return false;

        DescentCard card = (DescentCard) dgs.getComponentById(cardID);
        if (card == null) return false;
        if (f.isExhausted(card)) return false;

        Figure target = (Figure) dgs.getComponentById(defendingFigure);
        if (target == null) return false;

        return checkAdjacent(dgs, f, target);
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof CounterAttack other) {
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
        return super.getString(gameState).replace("Free Melee Attack", "Counter Attack");}

    @Override
    public String toString() {
        return super.toString().replace("Free Melee Attack", "Counter Attack");
    }

    public CounterAttack copy() {
        CounterAttack retValue = new CounterAttack(attackingFigure, defendingFigure);
        copyComponentTo(retValue);
        return retValue;
    }
    public void copyComponentTo(CounterAttack target) {
        super.copyComponentTo(target);
    }

    public static void setCardID(int id) {
        cardID = id;
    }

    public static int getCardID() {
        return cardID;
    }
}
