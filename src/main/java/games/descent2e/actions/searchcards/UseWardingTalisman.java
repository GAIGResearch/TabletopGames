package games.descent2e.actions.searchcards;

import core.AbstractGameState;
import core.components.Deck;
import core.properties.PropertyBoolean;
import core.properties.PropertyString;
import games.descent2e.DescentGameState;
import games.descent2e.actions.DescentAction;
import games.descent2e.actions.Triggers;
import games.descent2e.actions.attack.MeleeAttack;
import games.descent2e.actions.items.Shield;
import games.descent2e.components.DescentCard;
import games.descent2e.components.Figure;
import games.descent2e.components.Hero;

import java.util.Objects;

public class UseWardingTalisman extends Shield {

    private final String name = "Warding Talisman";

    public UseWardingTalisman(int figureID, int itemID) {
        super(figureID, itemID, 2);
    }

    @Override
    public String getString(AbstractGameState gameState) {
        return "Use " + name + " for +" + value + " shield to defense roll";
    }

    @Override
    public String toString() {
        return "Use " + name;
    }

    @Override
    public boolean execute(DescentGameState dgs) {
        Figure f = (Figure) dgs.getComponentById(figureID);
        DescentCard card = (DescentCard) dgs.getComponentById(cardID);
        card.setProperty(new PropertyBoolean("used", true));
        ((MeleeAttack) Objects.requireNonNull(dgs.currentActionInProgress())).addDefence(value);
        f.addActionTaken(toString());
        return true;
    }

    @Override
    public DescentAction copy() {
        return this;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof UseWardingTalisman that)) return false;
        return super.equals(o);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), name);
    }

    @Override
    public boolean canExecute(DescentGameState dgs) {
        Hero f = (Hero) dgs.getComponentById(figureID);
        DescentCard card = (DescentCard) dgs.getComponentById(cardID);
        if (card == null) return false;

        Deck<DescentCard> heroInventory = f.getInventory();
        if (heroInventory.contains(card))
            if (((PropertyString) card.getProperty("name")).value.equals(name))
                if (((PropertyBoolean) card.getProperty("used")).value.equals(false))
                    return canUse(dgs);
        return false;
    }
}
