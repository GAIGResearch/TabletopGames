package games.descent2e.actions.items;

import core.AbstractGameState;
import core.interfaces.IExtendedSequence;
import games.descent2e.DescentGameState;
import games.descent2e.actions.DescentAction;
import games.descent2e.actions.Triggers;
import games.descent2e.actions.attack.MeleeAttack;
import games.descent2e.components.DescentCard;
import games.descent2e.components.Figure;
import games.descent2e.components.Hero;

import java.util.Objects;

public class Shield extends DescentAction {

    int figureID;
    int value = 1;
    int cardID;
    public Shield(int figureID, int cardID, int value) {
        super(Triggers.ROLL_OWN_DICE);
        this.figureID = figureID;
        this.cardID = cardID;
        this.value = value;
    }

    @Override
    public String getString(AbstractGameState gameState) {
        return "Exhaust " + gameState.getComponentById(cardID).getProperty("name") + " for +" + value + " shield to defense roll";
    }

    public String toString() {
        return "Exhaust card " + cardID;
    }

    @Override
    public boolean execute(DescentGameState dgs) {
        Figure f = (Figure) dgs.getComponentById(figureID);
        //System.out.println("Exhausting shield!");
        f.exhaustCard((DescentCard) dgs.getComponentById(cardID));
        ((MeleeAttack) Objects.requireNonNull(dgs.currentActionInProgress())).addDefence(value);
        f.addActionTaken(toString());
        return true;
    }

    @Override
    public DescentAction copy() {
        return new Shield(figureID, cardID, value);
    }

    @Override
    public boolean canExecute(DescentGameState dgs) {
        Figure f = (Figure) dgs.getComponentById(figureID);
        DescentCard card = (DescentCard) dgs.getComponentById(cardID);
        if (f.isExhausted(card)) return false;
        if (f instanceof Hero)
        {
            if (!((Hero) f).getHandEquipment().contains(card)) return false;
        }
        IExtendedSequence currentAction = dgs.currentActionInProgress();
        if (!(currentAction instanceof MeleeAttack)) return false;
        MeleeAttack melee = (MeleeAttack) currentAction;
        if (!melee.getSkip() && melee.getDefendingFigure() == figureID && melee.getPhase() == MeleeAttack.AttackPhase.POST_DEFENCE_ROLL) {

            // If the defender already has enough defence to block the attack, there's no point in exhausting the shield
            int damage = dgs.getAttackDicePool().getDamage() + melee.getExtraDamage();
            int defence = dgs.getDefenceDicePool().getShields() + melee.getExtraDefence() - melee.getPierce();
            return damage > defence;
        }
        return false;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;
        Shield shield = (Shield) o;
        return figureID == shield.figureID && value == shield.value && cardID == shield.cardID;
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), figureID, value, cardID);
    }
}
