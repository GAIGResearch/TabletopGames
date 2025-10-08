package games.descent2e.actions.items;

import core.AbstractGameState;
import core.properties.PropertyString;
import games.descent2e.DescentGameState;
import games.descent2e.DescentHelper;
import games.descent2e.actions.DescentAction;
import games.descent2e.actions.Triggers;
import games.descent2e.components.DescentCard;
import games.descent2e.components.Figure;
import games.descent2e.components.Hero;

import java.util.List;
import java.util.Objects;

public class StaffOfLight extends DescentAction {

    static final int range = 3;

    public StaffOfLight() {
        super(Triggers.ACTION_POINT_SPEND);
    }

    @Override
    public boolean execute(DescentGameState dgs) {

        Figure f = dgs.getActingFigure();
        f.getNActionsExecuted().increment();
        f.addActionTaken(toString());

        for (Hero h : dgs.getHeroes()) {
            if (DescentHelper.inRange(f.getPosition(), h.getPosition(), range)) {
                h.getAttribute(Figure.Attribute.Health).increment();
                h.getAttribute(Figure.Attribute.Fatigue).decrement();
            }
        }
        return true;
    }

    @Override
    public DescentAction copy() {
        return new StaffOfLight();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        return o instanceof StaffOfLight;
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), 999999);
    }

    @Override
    public String getString(AbstractGameState gameState) {
        return toString();
    }

    @Override
    public String toString() {
        return "Staff Of Light: All Heroes within 3 spaces recover 1 Health and 1 Fatigue";
    }

    @Override
    public boolean canExecute(DescentGameState dgs) {
        Figure f = dgs.getActingFigure();
        if (f.getNActionsExecuted().isMaximum()) return false;

        // First, check that we do actually have the Staff of Light equipped
        boolean hasStaff = false;

        if (f instanceof Hero hero) {
            List<DescentCard> weapons = hero.getHandEquipment().getComponents();
            for (DescentCard weapon : weapons) {
                // Obtain the action, or passive, property of the Item
                PropertyString action = (PropertyString) weapon.getProperty("action");
                if (action == null) continue;
                if (action.value.contains("Effect:StaffOfLight")) {
                    hasStaff = true;
                    break;
                }
            }
        }
        if (!hasStaff) return false;

        // Check that there is at least one Hero in range who either is not at full Health or has Fatigue
        // There's no point using this action if it wouldn't help out anyone
        for (Hero h : dgs.getHeroes()) {
            if (!DescentHelper.inRange(f.getPosition(), h.getPosition(), range)) continue;
            if (!h.getAttribute(Figure.Attribute.Health).isMaximum()) return true;
            if (!h.getAttribute(Figure.Attribute.Fatigue).isMinimum()) return true;
        }

        return false;
    }
}
