package games.descent2e.actions.searchcards;

import core.AbstractGameState;
import core.actions.AbstractAction;
import core.components.*;
import core.interfaces.IExtendedSequence;
import core.properties.PropertyBoolean;
import core.properties.PropertyInt;
import core.properties.PropertyString;
import games.descent2e.DescentGameState;
import games.descent2e.DescentHelper;
import games.descent2e.DescentTypes;
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

public class UseCurseDoll extends DescentAction {

    int toCureID;
    int itemID;
    DescentTypes.DescentCondition conditionToCure;
    private final String name = "Curse Doll";

    public UseCurseDoll(int toCureID, int itemID, DescentTypes.DescentCondition conditionToCure) {
        super(Triggers.ACTION_POINT_SPEND);
        this.toCureID = toCureID;
        this.itemID = itemID;
        this.conditionToCure = conditionToCure;
    }

    @Override
    public String getString(AbstractGameState gameState) {
        return "Use Curse Doll on " + gameState.getComponentById(toCureID).getComponentName().replace("Hero: ", "") + " to cure " + conditionToCure;
    }

    @Override
    public String toString() {
        return "Use Curse Doll on " + toCureID + " to cure " + conditionToCure;
    }

    @Override
    public boolean execute(DescentGameState dgs) {
        Hero hero = (Hero) dgs.getComponentById(toCureID);
        hero.getConditions().remove(conditionToCure);

        Hero user = (Hero) dgs.getActingFigure();
        user.getNActionsExecuted().increment();
        user.addActionTaken(toString());

        DescentCard card = (DescentCard) dgs.getComponentById(itemID);
        card.setProperty(new PropertyBoolean("used", true));
        return true;
    }

    @Override
    public UseCurseDoll copy() {
        return new UseCurseDoll(toCureID, itemID, conditionToCure);
    }

    @Override
    public boolean canExecute(DescentGameState dgs) {
        Hero target = (Hero) dgs.getComponentById(toCureID);
        if (target == null || !target.hasCondition(conditionToCure)) return false;
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
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof UseCurseDoll that)) return false;
        return toCureID == that.toCureID && itemID == that.itemID && conditionToCure == that.conditionToCure;
    }

    @Override
    public int hashCode() {
        return Objects.hash(toCureID, itemID, conditionToCure, name);
    }
}
