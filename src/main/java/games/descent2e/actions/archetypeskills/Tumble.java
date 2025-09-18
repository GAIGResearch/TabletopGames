package games.descent2e.actions.archetypeskills;

import core.AbstractGameState;
import games.descent2e.DescentGameState;
import games.descent2e.actions.DescentAction;
import games.descent2e.actions.Triggers;
import games.descent2e.components.DescentCard;
import games.descent2e.components.Figure;

import java.util.Objects;

public class Tumble extends DescentAction {

    int userID = -1;
    int cardID = -1;
    public Tumble(int userID, int cardID) {
        super(Triggers.ACTION_POINT_SPEND);
        this.userID = userID;
        this.cardID = cardID;
    }

    @Override
    public boolean execute(DescentGameState gs) {
        Figure f = (Figure) gs.getComponentById(userID);
        f.getAttribute(Figure.Attribute.Fatigue).increment();
        DescentCard card = (DescentCard) gs.getComponentById(cardID);
        if (card != null)
            f.exhaustCard(card);
        f.setCanIgnoreEnemies(true);
        f.addActionTaken(toString());

        return true;
    }

    @Override
    public Tumble copy() {
        return new Tumble(userID, cardID);
    }

    @Override
    public boolean equals(Object o) {
        if (o instanceof Tumble t) {
            return userID == t.userID && cardID == t.cardID;
        } else {
            return false;
        }
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), userID, cardID);
    }

    @Override
    public String getString(AbstractGameState gameState) {
        return toString();
    }

    @Override
    public String toString() {
        return "Tumble: Spend 1 Fatigue to move through enemy spaces";
    }

    @Override
    public boolean canExecute(DescentGameState dgs) {
        Figure f = (Figure) dgs.getComponentById(userID);
        if (f == null) return false;
        if (f.getAttribute(Figure.Attribute.Fatigue).isMaximum()) return false;
        DescentCard card = (DescentCard) dgs.getComponentById(cardID);
        if (card == null) return false;
        if (f.isExhausted(card)) return false;

        // Only use this if we have chosen to move this turn
        // Otherwise we are wasting Fatigue to move through enemies when we are not moving
        if (f.getAttributeValue(Figure.Attribute.MovePoints) <= 1) return false;
        return !f.canIgnoreEnemies();
    }
}
