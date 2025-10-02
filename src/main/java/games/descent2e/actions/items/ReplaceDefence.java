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

public class ReplaceDefence extends DescentAction {

    protected int figureID;
    protected Figure.Attribute attribute;
    protected int cardID;
    public ReplaceDefence(int figureID, int cardID, Figure.Attribute attribute) {
        super(Triggers.ROLL_OWN_DICE);
        this.figureID = figureID;
        this.cardID = cardID;
        this.attribute = attribute;
    }

    @Override
    public String getString(AbstractGameState gameState) {
        Figure f = (Figure) gameState.getComponentById(figureID);
        return gameState.getComponentById(cardID).getProperty("name") + ": Set defence result to " + f.getAttribute(attribute).getValue() + " (" + attribute.toString() + ")";
    }

    public String toString() {
        return cardID + ": Set defence result to " + attribute.toString() + " score";
    }

    @Override
    public boolean execute(DescentGameState dgs) {
        Figure f = (Figure) dgs.getComponentById(figureID);
        //System.out.println("Exhausting shield!");
        f.exhaustCard((DescentCard) dgs.getComponentById(cardID));
        ((MeleeAttack) Objects.requireNonNull(dgs.currentActionInProgress())).swapDefence(f.getAttributeValue(attribute));
        f.addActionTaken(toString());
        return true;
    }

    @Override
    public DescentAction copy() {
        return new ReplaceDefence(figureID, cardID, attribute);
    }

    @Override
    public boolean canExecute(DescentGameState dgs) {
        Figure f = (Figure) dgs.getComponentById(figureID);
        DescentCard card = (DescentCard) dgs.getComponentById(cardID);
        if (f.isExhausted(card)) return false;
        if (f instanceof Hero)
        {
            if (!((Hero) f).getAllEquipment().contains(card)) return false;
        }

        return canUse(dgs);
    }

    protected boolean canUse(DescentGameState dgs)
    {
        IExtendedSequence currentAction = dgs.currentActionInProgress();
        if (!(currentAction instanceof MeleeAttack melee)) return false;
        if (!melee.getSkip() && melee.getDefendingFigure() == figureID && melee.getPhase() == MeleeAttack.AttackPhase.POST_DEFENCE_ROLL) {

            // If we already rolled higher than our attribute score, don't replace the result with a lower number
            int defence = dgs.getDefenceDicePool().getShields();
            int score = ((Figure) dgs.getComponentById(figureID)).getAttributeValue(attribute);
            if (defence > score) return false;

            // If the defender already has enough defence to block the attack, there's no point exhausting this either
            int damage = dgs.getAttackDicePool().getDamage() + melee.getExtraDamage();
            defence += melee.getExtraDefence() - melee.getPierce();
            return damage > defence;
        }
        return false;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;
        ReplaceDefence swap = (ReplaceDefence) o;
        return figureID == swap.figureID && attribute == swap.attribute && cardID == swap.cardID;
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), figureID, attribute, cardID);
    }
}
