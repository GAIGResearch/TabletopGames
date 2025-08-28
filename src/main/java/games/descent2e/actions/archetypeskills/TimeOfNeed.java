package games.descent2e.actions.archetypeskills;

import core.AbstractGameState;
import games.descent2e.DescentGameState;
import games.descent2e.DescentTypes;
import games.descent2e.actions.DescentAction;
import games.descent2e.actions.GetMovementPoints;
import games.descent2e.actions.Triggers;
import games.descent2e.actions.monsterfeats.MonsterAbilities;
import games.descent2e.components.Figure;
import games.descent2e.components.Hero;
import games.descent2e.components.Monster;

import java.util.Objects;

public class TimeOfNeed extends GetMovementPoints {

    int cardID = -1;

    public TimeOfNeed(int cardID) {
        super();
        this.cardID = cardID;
    }

    @Override
    public String getString(AbstractGameState gameState) {
        return toString();
    }
    @Override
    public String toString() { return "Time of Need: Gain 2 Movement Points and Recover 2 Fatigue"; }

    @Override
    public boolean execute(DescentGameState gs) {
        Figure f = gs.getActingFigure();
        int move = f.getAttributeValue(Figure.Attribute.MovePoints);
        f.setAttribute(Figure.Attribute.MovePoints, move + 2);
        f.getAttribute(Figure.Attribute.Fatigue).decrement();
        f.getAttribute(Figure.Attribute.Fatigue).decrement();
        f.getNActionsExecuted().increment();
        f.addActionTaken(toString());
        return true;
    }

    @Override
    public DescentAction copy() {
        return new TimeOfNeed(cardID);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), cardID);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof TimeOfNeed other)) return false;
        if (!super.equals(obj)) return false;
        return cardID == other.cardID;
    }

    @Override
    public boolean canExecute(DescentGameState dgs) {
        Figure f = dgs.getActingFigure();

        if (!(f instanceof Hero)) return false;

        return !f.hasCondition(DescentTypes.DescentCondition.Immobilize) && !f.getNActionsExecuted().isMaximum() && !f.getAttribute(Figure.Attribute.Fatigue).isMinimum();
    }
}
