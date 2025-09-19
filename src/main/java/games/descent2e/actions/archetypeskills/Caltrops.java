package games.descent2e.actions.archetypeskills;

import com.google.common.collect.Iterables;
import core.AbstractGameState;
import core.actions.AbstractAction;
import games.descent2e.DescentGameState;
import games.descent2e.DescentTypes;
import games.descent2e.actions.Move;
import games.descent2e.actions.TriggerAttributeTest;
import games.descent2e.actions.Triggers;
import games.descent2e.components.DescentCard;
import games.descent2e.components.Figure;
import utilities.Pair;

import java.util.ArrayList;
import java.util.List;

import static games.descent2e.DescentHelper.checkAdjacent;

public class Caltrops extends TriggerAttributeTest {

    int cardID = -1;

    public Caltrops(int attackingFigure, int cardID) {
        super(attackingFigure, new ArrayList<>(), Triggers.MOVE_INTO_SPACE);
        this.cardID = cardID;
    }

    public Caltrops(int attackingFigure, List<Integer> targets, int cardID) {
        super(attackingFigure, targets, Triggers.MOVE_INTO_SPACE);
        this.cardID = cardID;
    }

    public void addTarget(int target) {
        targets = List.of(target);
    }

    @Override
    public boolean execute(DescentGameState state) {
        Figure f = (Figure) state.getComponentById(attackingFigure);
        f.getAttribute(Figure.Attribute.Fatigue).increment();
        DescentCard card = (DescentCard) state.getComponentById(cardID);
        f.exhaustCard(card);
        f.addActionTaken(toString());
        state.setActionInProgress(this);
        return true;
    }

    @Override
    public boolean canExecute(DescentGameState dgs) {
        if (dgs.getHistory().isEmpty()) return false;

        Figure f = (Figure) dgs.getComponentById(getAttackingFigure());
        DescentCard card = (DescentCard) dgs.getComponentById(cardID);
        if (f == null || card == null) return false;
        if (f.isExhausted(card)) return false;
        if (!f.hasBonus(DescentTypes.SkillBonus.Caltrops)) return false;

        if (f.getAttribute(Figure.Attribute.Fatigue).isMaximum())
            return false;

        if (getTargets().isEmpty()) return false;
        Figure target = (Figure) dgs.getComponentById(getTargets().get(0));
        if (target == null) return false;

        Pair<Integer, AbstractAction> lastAction = Iterables.getLast(dgs.getHistory());
        if (lastAction.a != target.getOwnerId() || !(lastAction.b instanceof Move))
            return false;

        return checkAdjacent(dgs, f, target);
    }

    @Override
    public List<AbstractAction> _computeAvailableActions(AbstractGameState state) {
        int target = currentTarget();
        int attacker = getAttackingFigure();
        return List.of(new CaltropsTest(attacker, Figure.Attribute.Awareness, target));
    }

    @Override
    public Caltrops copy() {
        Caltrops retVal = new Caltrops(getAttackingFigure(), getTargets(), cardID);
        copyComponentTo(retVal);
        return retVal;
    }

    @Override
    public String toString() {
        return super.toString().replace("Call Attribute Test", "Caltrops");
    }

    @Override
    public String getString(AbstractGameState gameState) {
        return super.getString(gameState).replace("Call Attribute Test", "Caltrops");
    }
}
