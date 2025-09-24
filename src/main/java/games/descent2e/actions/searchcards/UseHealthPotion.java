package games.descent2e.actions.searchcards;

import core.AbstractGameState;
import core.actions.AbstractAction;
import core.components.BoardNode;
import core.components.Card;
import core.components.Deck;
import core.components.GridBoard;
import core.interfaces.IExtendedSequence;
import core.properties.PropertyBoolean;
import core.properties.PropertyInt;
import core.properties.PropertyString;
import games.descent2e.DescentGameState;
import games.descent2e.DescentHelper;
import games.descent2e.actions.DescentAction;
import games.descent2e.actions.Triggers;
import games.descent2e.components.DescentCard;
import games.descent2e.components.Figure;
import games.descent2e.components.Hero;
import utilities.Vector2D;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import static core.CoreConstants.playersHash;
import static utilities.Utils.getNeighbourhood;

public class UseHealthPotion extends DescentAction {
    int toHealID;
    int itemID;
    private final String name = "Health Potion";

    public UseHealthPotion(int toHealID, int itemID) {
        super(Triggers.ACTION_POINT_SPEND);
        this.toHealID = toHealID;
        this.itemID = itemID;
    }

    @Override
    public String getString(AbstractGameState gameState) {
        return "Use Health Potion on " + gameState.getComponentById(toHealID).getComponentName().replace("Hero: ", "");
    }

    @Override
    public String toString() {
        return "Use Health Potion on " + toHealID;
    }

    @Override
    public boolean execute(DescentGameState dgs) {
        Hero hero = (Hero) dgs.getComponentById(toHealID);
        hero.setAttributeToMax(Figure.Attribute.Health);

        Hero user = (Hero) dgs.getActingFigure();
        user.getNActionsExecuted().increment();
        user.addActionTaken(toString());

        DescentCard card = (DescentCard) dgs.getComponentById(itemID);
        card.setProperty(new PropertyBoolean("used", true));
        return true;
    }

    @Override
    public UseHealthPotion copy() {
        return new UseHealthPotion(toHealID, itemID);
    }

    @Override
    public boolean canExecute(DescentGameState dgs) {
        Hero target = (Hero) dgs.getComponentById(toHealID);
        if (target == null || target.getAttribute(Figure.Attribute.Health).isMaximum()) return false;
        Hero user = (Hero) dgs.getActingFigure();
        if (user == null) return false;
        if (user.getNActionsExecuted().isMaximum()) return false;

        // If not self, check adjacency
        if (target != user)
            if(!DescentHelper.checkAdjacent(dgs, user, target))
                return false;

        Deck<DescentCard> heroInventory = user.getInventory();
        DescentCard card = (DescentCard) dgs.getComponentById(itemID);
        if (card == null) return false;
        if (heroInventory.contains(card))
            if (((PropertyString) card.getProperty("name")).value.equals(name))
                return (((PropertyBoolean) card.getProperty("used")).value.equals(false));
        return false;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof UseHealthPotion other)) return false;
        return toHealID == other.toHealID && itemID == other.itemID;
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), toHealID, itemID, name);
    }
}
