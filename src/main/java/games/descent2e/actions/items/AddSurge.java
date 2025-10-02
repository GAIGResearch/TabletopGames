package games.descent2e.actions.items;

import core.AbstractGameState;
import core.components.Component;
import core.interfaces.IExtendedSequence;
import games.descent2e.DescentGameState;
import games.descent2e.actions.DescentAction;
import games.descent2e.actions.Triggers;
import games.descent2e.actions.attack.MeleeAttack;
import games.descent2e.components.DescentCard;
import games.descent2e.components.Figure;
import games.descent2e.components.Hero;

import java.util.ArrayList;
import java.util.Objects;

public class AddSurge extends DescentAction {

    int userID;
    int cardID;
    int increase;

    public AddSurge(int userID, int cardID) {
        super(Triggers.ROLL_OWN_DICE);
        this.userID = userID;
        this.cardID = cardID;
        this.increase = 1;
    }

    public AddSurge(int userID, int cardID, int increase) {
        super(Triggers.ROLL_OWN_DICE);
        this.userID = userID;
        this.cardID = cardID;
        this.increase = increase;
    }

    public AddSurge(Triggers triggers, int userID, int cardID, int increase) {
        super(triggers);
        this.userID = userID;
        this.cardID = cardID;
        this.increase = increase;
    }

    @Override
    public boolean execute(DescentGameState dgs) {

        MeleeAttack action = (MeleeAttack) dgs.currentActionInProgress();
        if (action != null)
            action.addExtraSurge(increase);

        Figure f = (Figure) dgs.getComponentById(userID);
        DescentCard card = (DescentCard) dgs.getComponentById(cardID);

        if (card != null)
            f.exhaustCard(card);

        f.addActionTaken(getString(dgs));

        return true;
    }

    @Override
    public AddSurge copy() {
        return new AddSurge(getTriggers().iterator().next(), userID, cardID, increase);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;
        AddSurge that = (AddSurge) o;
        return userID == that.userID && cardID == that.cardID && increase == that.increase;
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), userID, cardID, increase);
    }

    @Override
    public String getString(AbstractGameState gameState) {
        String name = gameState.getComponentById(cardID).toString();
        return name + ": " + toString();
    }

    @Override
    public String toString() {
        if (increase > 0) return "Add " + increase + " Surge to current attack";
        else return "Remove " + increase + " Surge from current attack";
    }

    @Override
    public boolean canExecute(DescentGameState dgs) {

        // We can only use this if it interrupts an Attack action
        if (dgs.isActionInProgress()) {
            IExtendedSequence action = dgs.currentActionInProgress();
            // Ranged Attacks are instances of Melee Attacks, so both types are covered
            if (action instanceof MeleeAttack melee) {
                if (melee.getSkip()) return false;
                if (increase < 0)
                    if (dgs.getAttackDicePool().getSurge() + melee.getExtraSurge() <= 0)
                        return false;
                Figure f = (Figure) dgs.getComponentById(userID);
                if (f == null) return false;
                if (!(f instanceof Hero hero)) return false;
                DescentCard card = (DescentCard) dgs.getComponentById(cardID);
                return hero.getAllEquipment().contains(card) && !hero.isExhausted(card);
            }
        }
        return false;
    }
}
