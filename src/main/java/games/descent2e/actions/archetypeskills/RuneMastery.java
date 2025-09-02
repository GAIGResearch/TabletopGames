package games.descent2e.actions.archetypeskills;

import core.AbstractGameState;
import core.components.Component;
import core.interfaces.IExtendedSequence;
import games.descent2e.DescentGameState;
import games.descent2e.actions.DescentAction;
import games.descent2e.actions.Triggers;
import games.descent2e.actions.attack.MeleeAttack;
import games.descent2e.components.DescentCard;
import games.descent2e.components.Figure;

import java.util.Objects;

public class RuneMastery extends DescentAction {

    int cardID;

    public RuneMastery(int cardID) {
        super(Triggers.ROLL_OWN_DICE);
        this.cardID = cardID;
    }

    @Override
    public boolean execute(DescentGameState dgs) {

        MeleeAttack action = (MeleeAttack) dgs.currentActionInProgress();
        if (action != null)
            action.addExtraSurge(1);

        Figure f = dgs.getActingFigure();
        Component card = dgs.getComponentById(cardID);

        if (card != null)
            f.exhaustCard((DescentCard) card);

        f.addActionTaken(getString(dgs));

        return true;
    }

    @Override
    public RuneMastery copy() {
        return new RuneMastery(cardID);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;
        RuneMastery that = (RuneMastery) o;
        return cardID == that.cardID;
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), cardID);
    }

    @Override
    public String getString(AbstractGameState gameState) {
        return toString();
    }

    @Override
    public String toString() {
        return "Rune Mastery: Add +1 Surge to current attack";
    }

    @Override
    public boolean canExecute(DescentGameState dgs) {

        // We can only use this if it interrupts an Attack action
        if (dgs.isActionInProgress()) {
            IExtendedSequence action = dgs.currentActionInProgress();
            // Ranged Attacks are instances of Melee Attacks, so both types are covered
            if (action instanceof MeleeAttack melee) {
                if (melee.getSkip()) return false;
                Figure f = dgs.getActingFigure();
                DescentCard card = (DescentCard) dgs.getComponentById(cardID);
                return !f.isExhausted(card);
            }
        }
        return false;
    }
}
