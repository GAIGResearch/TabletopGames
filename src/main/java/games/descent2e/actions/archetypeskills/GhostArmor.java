package games.descent2e.actions.archetypeskills;

import core.AbstractGameState;
import games.descent2e.DescentGameState;
import games.descent2e.actions.DescentAction;
import games.descent2e.actions.attack.MeleeAttack;
import games.descent2e.actions.items.Shield;
import games.descent2e.components.DescentCard;
import games.descent2e.components.Figure;
import games.descent2e.components.Hero;
import java.util.Objects;

public class GhostArmor extends Shield {

    // Ghost Armor can be used as many times as we'd like, so long as we have the Fatigue for it
    public GhostArmor(int figureID, int cardID) {
        super(figureID, cardID, 1);
    }

    @Override
    public String getString(AbstractGameState gameState) {
        return "Ghost Armor: +1 Shield";
    }

    public String toString() {
        return "Ghost Armor: +1 Shield";
    }

    @Override
    public boolean execute(DescentGameState dgs) {
        Figure f = (Figure) dgs.getComponentById(figureID);
        f.getAttribute(Figure.Attribute.Fatigue).increment();
        ((MeleeAttack) Objects.requireNonNull(dgs.currentActionInProgress())).addDefence(value);
        f.addActionTaken(toString());
        return true;
    }

    @Override
    public DescentAction copy() {
        return new GhostArmor(figureID, cardID);
    }

    @Override
    public boolean canExecute(DescentGameState dgs) {
        Figure f = (Figure) dgs.getComponentById(figureID);
        if (f.getAttribute(Figure.Attribute.Fatigue).isMaximum()) return false;

        if (!(f instanceof Hero)) return false;

        DescentCard skill = (DescentCard) dgs.getComponentById(cardID);
        if (!(((Hero) f).getSkills().contains(skill))) return false;

        return canUse(dgs);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;
        GhostArmor shield = (GhostArmor) o;
        return figureID == shield.figureID && value == shield.value && cardID == shield.cardID;
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode());
    }
}
