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

import static core.CoreConstants.playersHash;
import static utilities.Utils.getNeighbourhood;

public class UseStaminaPotion extends DescentAction {
    int toRestoreID;
    private final String name = "Stamina Potion";

    public UseStaminaPotion(int toRestoreID) {
        super(Triggers.ANYTIME);
        this.toRestoreID = toRestoreID;
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
        // Heal hero
        Hero hero = (Hero) dgs.getComponentById(toRestoreID);
        hero.setAttributeToMin(Figure.Attribute.Fatigue);

        Hero user = (Hero) dgs.getActingFigure();
        user.addActionTaken(toString());
        for (Card c : user.getInventory().getComponents()) {
            if (((PropertyString) c.getProperty("name")).value.equals(name)) {
                if (((PropertyBoolean) c.getProperty("used")).value.equals(false))
                {
                    c.setProperty(new PropertyBoolean("used", true));
                    break;
                }
            }
        }
        return true;
    }

    @Override
    public UseStaminaPotion copy() {
        return new UseStaminaPotion(toRestoreID);
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
        for (Card c : heroInventory.getComponents()) {
            if (((PropertyString) c.getProperty("name")).value.equals(name)) {
                if (((PropertyBoolean) c.getProperty("used")).value.equals(false))
                    return true;
            }
        }
        return false;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof UseStaminaPotion other)) return false;
        return toRestoreID == other.toRestoreID;
    }

    @Override
    public int hashCode() {
        return toRestoreID - 9867;
    }
}
