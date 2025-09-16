package games.descent2e.actions.archetypeskills;

import core.AbstractGameState;
import core.actions.AbstractAction;
import games.descent2e.DescentGameState;
import games.descent2e.actions.AttributeTest;
import games.descent2e.actions.TriggerAttributeTest;
import games.descent2e.actions.monsterfeats.*;
import games.descent2e.components.DescentCard;
import games.descent2e.components.Figure;
import games.descent2e.components.Monster;
import utilities.Vector2D;

import java.util.ArrayList;
import java.util.List;

import static games.descent2e.DescentHelper.*;

public class Cripple extends TriggerAttributeTest {

    int cardID = -1;
    public Cripple(int attackingFigure, int target, int cardID) {
        super(attackingFigure, List.of(target));
        this.cardID = cardID;
    }

    public Cripple(int attackingFigure, List<Integer> targets, int cardID) {
        super(attackingFigure, targets);
        this.cardID = cardID;
    }

    @Override
    public boolean execute(DescentGameState state) {
        Figure f = (Figure) state.getComponentById(attackingFigure);
        f.incrementAttribute(Figure.Attribute.Fatigue, 2);
        DescentCard card = (DescentCard) state.getComponentById(cardID);
        f.exhaustCard(card);
        f.addActionTaken(toString());
        state.setActionInProgress(this);
        return true;
    }

    @Override
    public boolean canExecute(DescentGameState dgs) {
        Figure f = (Figure) dgs.getComponentById(getAttackingFigure());
        DescentCard card = (DescentCard) dgs.getComponentById(cardID);
        if (f == null || card == null) return false;
        if (f.isExhausted(card)) return false;

        if (f.getAttributeValue(Figure.Attribute.Fatigue) + 2 > f.getAttribute(Figure.Attribute.Fatigue).getMaximum())
            return false;

        if (getTargets().isEmpty()) return false;
        Figure target = (Figure) dgs.getComponentById(getTargets().get(0));
        if (target == null) return false;
        return checkAdjacent(dgs, f, target);
    }

    @Override
    public List<AbstractAction> _computeAvailableActions(AbstractGameState state) {
        int target = currentTarget();
        int attacker = getAttackingFigure();
        return List.of(new CrippleTest(attacker, Figure.Attribute.Might, target));
    }

    @Override
    public Cripple copy() {
        Cripple retVal = new Cripple(getAttackingFigure(), getTargets(), cardID);
        copyComponentTo(retVal);
        return retVal;
    }

    @Override
    public String toString() {
        return super.toString().replace("Call Attribute Test", "Cripple");
    }

    @Override
    public String getString(AbstractGameState gameState) {
        return super.getString(gameState).replace("Call Attribute Test", "Cripple");
    }
}
