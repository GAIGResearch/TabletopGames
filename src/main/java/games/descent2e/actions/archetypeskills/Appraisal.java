package games.descent2e.actions.archetypeskills;

import com.google.common.collect.Iterables;
import core.AbstractGameState;
import core.actions.AbstractAction;
import core.components.Deck;
import games.descent2e.DescentGameState;
import games.descent2e.actions.DescentAction;
import games.descent2e.actions.Triggers;
import games.descent2e.actions.tokens.SearchAction;
import games.descent2e.components.DescentCard;
import games.descent2e.components.Figure;
import games.descent2e.components.Hero;
import games.descent2e.components.cards.SearchCard;
import utilities.Pair;

import java.util.Objects;

import static games.descent2e.actions.tokens.SearchAction.getShopItem;
import static games.descent2e.actions.tokens.SearchAction.setItemAbilities;

public class Appraisal extends DescentAction {

    String item;
    String nextItem;
    public Appraisal(String item, String nextItem) {
        super(Triggers.ANYTIME);
        this.item = item;
        this.nextItem = nextItem;
    }

    @Override
    public boolean execute(DescentGameState dgs) {
        Hero hero = (Hero) dgs.getActingFigure();

        if (!item.equals("Nothing"))
        {
            Deck<DescentCard> equipment = hero.getOtherEquipment();
            for (DescentCard equip : equipment)
            {
                if (equip.getComponentName().equals(item))
                {
                    equipment.remove(equip);
                    break;
                }
            }
        }

        SearchCard card = (SearchCard) dgs.getSearchCards().draw();
        if (card.getComponentName().equals("Treasure Chest")) {
            return getShopItem(dgs, hero);
        }
        else if (!card.getComponentName().equals("Nothing")) {
            DescentCard c = new DescentCard(card);
            boolean added = hero.getInventory().add(c);
            setItemAbilities(dgs, hero, c);
            return added;
        }
        return true;
    }

    @Override
    public Appraisal copy() {
        return new Appraisal(item, nextItem);
    }

    @Override
    public boolean equals(Object o) {
        if (o instanceof Appraisal a) {
            return item.equals(a.item) && nextItem.equals(a.nextItem);
        } else {
            return false;
        }
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), item, nextItem);
    }

    @Override
    public String getString(AbstractGameState gameState) {
        return toString();
    }

    @Override
    public String toString() {
        return "Appraisal: Discard " + item + " to draw a new Search card";
    }

    @Override
    public boolean canExecute(DescentGameState dgs) {
        Figure f = dgs.getActingFigure();
        if (f == null) return false;
        if (!(f instanceof Hero hero)) return false;

        // Can't draw a new Search card if we're out of Search cards
        if (dgs.getSearchCards().getSize() == 0) return false;

        // Why would you throw away the best Search item in the game?
        // This isn't a legality blockade, just a sanity check
        // Stops the bots from throwing away good stuff
        if (item.equals("Treasure Chest")) return false;

        if (dgs.getHistory().isEmpty()) return false;

        Pair<Integer, AbstractAction> lastAction = Iterables.getLast(dgs.getHistory());
        if (!lastAction.a.equals(f.getOwnerId())) return false;
        if (!(lastAction.b instanceof SearchAction search)) return false;
        if (!Objects.equals(search.getItemID(), item)) return false;

        if (item.equals("Nothing")) return true;

        Deck<DescentCard> equipment = hero.getOtherEquipment();
        if (equipment == null) return false;
        if (equipment.getSize() == 0) return false;
        // Check we actually have the item to discard
        for (DescentCard equip : equipment)
        {
            if (equip.getComponentName().equals(item))
                return true;
        }
        return false;
    }
}
