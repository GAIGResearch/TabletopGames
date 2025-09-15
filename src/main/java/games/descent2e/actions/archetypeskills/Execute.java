package games.descent2e.actions.archetypeskills;

import core.AbstractGameState;
import core.actions.AbstractAction;
import games.descent2e.DescentGameState;
import games.descent2e.actions.DescentAction;
import games.descent2e.actions.Triggers;
import games.descent2e.actions.attack.EndCurrentPhase;
import games.descent2e.actions.attack.MeleeAttack;
import games.descent2e.components.DescentCard;
import games.descent2e.components.Figure;
import org.sparkproject.guava.collect.Iterables;
import utilities.Pair;

import java.util.Objects;

public class Execute extends DescentAction {

    int userID = -1;
    int cardID = -1;
    int fatigueCost = -1;

    public Execute(int userID, int cardID, int fatigueCost) {
        super(Triggers.ROLL_OWN_DICE);
        this.userID = userID;
        this.cardID = cardID;
        this.fatigueCost = fatigueCost;
    }

    @Override
    public boolean execute(DescentGameState gs) {

        MeleeAttack attack = (MeleeAttack) gs.currentActionInProgress();
        assert attack != null;
        attack.addDamage(fatigueCost);

        Figure f = (Figure) gs.getComponentById(userID);
        f.incrementAttribute(Figure.Attribute.Fatigue, fatigueCost);

        DescentCard card = (DescentCard) gs.getComponentById(cardID);
        f.exhaustCard(card);

        return false;
    }

    @Override
    public Execute copy() {
        return new Execute(userID, cardID, fatigueCost);
    }

    @Override
    public String getString(AbstractGameState gameState) {
        return "Execute: Add " + fatigueCost + " Damage";
    }

    @Override
    public String toString() {
        return String.format("Execute: Add %d Damage", fatigueCost);
    }

    @Override
    public boolean canExecute(DescentGameState dgs) {
        // We can only interrupt an Attack, and have not already used this card this round
        if (!dgs.isActionInProgress()) return false;
        if (!(dgs.currentActionInProgress() instanceof MeleeAttack)) return false;

        // No point in wasting Fatigue if we missed
        if (dgs.getAttackDicePool().getRange() < 1) return false;

        if (dgs.getActingFigure().getComponentID() != userID) return false;
        Figure f = (Figure) dgs.getComponentById(userID);
        if (f == null) return false;

        Pair<Integer, AbstractAction> lastAction = Iterables.getLast(dgs.getHistory());
        if (lastAction.a == f.getOwnerId() && lastAction.b instanceof EndCurrentPhase)
            return false;

        DescentCard card = (DescentCard) dgs.getComponentById(cardID);
        if (card == null) return false;
        if (f.isExhausted(card)) return false;

        return f.getAttributeValue(Figure.Attribute.Fatigue) + fatigueCost <= f.getAttributeMax(Figure.Attribute.Fatigue);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Execute that)) return false;
        if (!super.equals(o)) return false;
        return userID == that.userID && cardID == that.cardID && fatigueCost == that.fatigueCost;
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), userID, cardID, fatigueCost);
    }
}
