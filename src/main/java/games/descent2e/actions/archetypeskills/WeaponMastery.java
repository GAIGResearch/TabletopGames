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

public class WeaponMastery extends DescentAction {

    int userID = -1;
    int cardID = -1;

    public WeaponMastery(int userID, int cardID) {
        super(Triggers.ROLL_OWN_DICE);
        this.userID = userID;
        this.cardID = cardID;
    }

    @Override
    public boolean execute(DescentGameState gs) {

        MeleeAttack attack = (MeleeAttack) gs.currentActionInProgress();
        assert attack != null;
        attack.addExtraSurge(1);

        Figure f = (Figure) gs.getComponentById(userID);
        DescentCard card = (DescentCard) gs.getComponentById(cardID);
        f.exhaustCard(card);

        return false;
    }

    @Override
    public WeaponMastery copy() {
        return new WeaponMastery(userID, cardID);
    }

    @Override
    public String getString(AbstractGameState gameState) {
        return toString();
    }

    @Override
    public String toString() {
        return "Weapon Mastery: Add 1 Surge";
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
        return !f.isExhausted(card);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof WeaponMastery that)) return false;
        if (!super.equals(o)) return false;
        return userID == that.userID && cardID == that.cardID;
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), userID, cardID);
    }
}
