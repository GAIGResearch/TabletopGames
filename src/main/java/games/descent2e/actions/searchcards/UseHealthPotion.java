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

import static core.CoreConstants.playersHash;
import static utilities.Utils.getNeighbourhood;

public class UseHealthPotion extends DescentAction {
    int toHealID;
    private final String name = "Health Potion";

    public UseHealthPotion(int toHealID) {
        super(Triggers.ACTION_POINT_SPEND);
        this.toHealID = toHealID;
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
        // Heal hero
        Hero hero = (Hero) dgs.getComponentById(toHealID);
        hero.setAttributeToMax(Figure.Attribute.Health);

        Hero user = (Hero) dgs.getActingFigure();
        user.getNActionsExecuted().increment();
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
    public UseHealthPotion copy() {
        return new UseHealthPotion(toHealID);
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
        if (!(obj instanceof UseHealthPotion other)) return false;
        return toHealID == other.toHealID;
    }

    @Override
    public int hashCode() {
        return toHealID - 789793 + 31;
    }
}
