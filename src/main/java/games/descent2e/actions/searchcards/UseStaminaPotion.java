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
import games.descent2e.components.cards.SearchCard;
import utilities.Vector2D;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import static core.CoreConstants.playersHash;
import static utilities.Utils.getNeighbourhood;

public class UseStaminaPotion extends DescentAction {
    int toRestoreID;
    int itemID;
    private final String name = "Stamina Potion";

    public UseStaminaPotion(int toRestoreID, int itemID) {
        super(Triggers.ANYTIME);
        this.toRestoreID = toRestoreID;
        this.itemID = itemID;
    }

    @Override
    public String getString(AbstractGameState gameState) {
        return "Use Stamina Potion on " + gameState.getComponentById(toRestoreID).getComponentName().replace("Hero: ", "");
    }

    @Override
    public String toString() {
        return "Use Stamina Potion on " + toRestoreID;
    }

    @Override
    public boolean execute(DescentGameState dgs) {
        Hero hero = (Hero) dgs.getComponentById(toRestoreID);
        hero.setAttributeToMin(Figure.Attribute.Fatigue);

        Hero user = (Hero) dgs.getActingFigure();
        user.addActionTaken(toString());

        DescentCard card = (DescentCard) dgs.getComponentById(itemID);
        card.setProperty(new PropertyBoolean("used", true));
        return true;
    }

    @Override
    public UseStaminaPotion copy() {
        return new UseStaminaPotion(toRestoreID, itemID);
    }

    @Override
    public boolean canExecute(DescentGameState dgs) {
        Hero target = (Hero) dgs.getComponentById(toRestoreID);
        if (target == null || target.getAttribute(Figure.Attribute.Fatigue).isMinimum()) return false;
        Hero user = (Hero) dgs.getActingFigure();
        if (user == null) return false;

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
        if (!(obj instanceof UseStaminaPotion other)) return false;
        return toRestoreID == other.toRestoreID && itemID == other.itemID;
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), toRestoreID, itemID, name);
    }
}
