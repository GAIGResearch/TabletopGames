package games.descent2e.actions.tokens;

import core.AbstractGameState;
import core.components.Card;
import core.components.Deck;
import core.components.GridBoard;
import games.descent2e.DescentGameState;
import games.descent2e.DescentTypes;
import games.descent2e.actions.Triggers;
import games.descent2e.components.Hero;
import games.descent2e.components.DescentCard;
import games.descent2e.components.cards.SearchCard;
import games.descent2e.components.cards.ShopCard;
import games.descent2e.components.tokens.DToken;
import utilities.Vector2D;

import java.util.List;

import static utilities.Utils.getNeighbourhood;

/**
 * Draw random search card and add to player
 */
public class SearchAction extends TokenAction<SearchAction> {
    protected boolean freeSearch = false;
    String item;
    public SearchAction() {
        super(-1, Triggers.ACTION_POINT_SPEND);
    }

    @Override
    public SearchAction _copy() {
        SearchAction search = new SearchAction();
        copyComponentsTo(search);
        return search;
    }

    public void copyComponentsTo(SearchAction action) {
        action.freeSearch = freeSearch;
        action.item = item;
    }

    @Override
    public boolean canExecute(DescentGameState gs) {
        // Can only execute if player adjacent to search token
        Hero hero = (Hero) gs.getActingFigure();
        if (!freeSearch && hero.getNActionsExecuted().isMaximum()) return false;
        Vector2D loc = hero.getPosition();
        GridBoard board = gs.getMasterBoard();
        List<Vector2D> neighbours = getNeighbourhood(loc.getX(), loc.getY(), board.getWidth(), board.getHeight(), true);
        for (DToken token: gs.getTokens()) {
            if (token.getDescentTokenType() == DescentTypes.DescentToken.Search && neighbours.contains(token.getPosition())) {
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean equals(Object o) {
        return super.equals(o) && o instanceof SearchAction
                && ((SearchAction) o).freeSearch == freeSearch && ((SearchAction) o).item == item;
    }

    @Override
    public String getString(AbstractGameState gameState) {
        return toString();
    }

    @Override
    public String toString() {
        return "Search";
    }

    @Override
    public boolean execute(DescentGameState gs) {

        Hero f = (Hero) gs.getActingFigure();

        if (!freeSearch)
            f.getNActionsExecuted().increment();

        Deck<Card> searchCards = gs.getSearchCards();
        DToken searchToken = (DToken) gs.getComponentById(tokenID);
        if (searchCards != null) {
            SearchCard card = (SearchCard) searchCards.draw();
            if (card.getComponentName().equals("Treasure Chest")) {
                // TODO - when shop cards are added
                boolean added = getShopItem(gs, f);
                if (added) {
                    searchToken.setPosition(null);  // Take off the map
                }
                return added;
            }
            else if (!card.getComponentName().equals("Nothing")) {
                boolean added = f.getInventory().add(new DescentCard(card));
                if (added) {
                    searchToken.setPosition(null);  // Take off the map
                }
                return added;
            }
            searchToken.setPosition(null);  // Take off the map
            return true;
        }
        return false;
    }

    public String getItemID() {
        return item;
    }

    public void setItemID(DescentGameState dgs) {
        // Set to the next search card to be drawn
        this.item = dgs.getSearchCards().get(0).getComponentName();
    }

    public static boolean getShopItem(DescentGameState dgs, Hero f)
    {
        Deck<Card> shopCards;
        if (dgs.getCurrentQuest().getAct() == 1)
            shopCards = dgs.getAct1ShopCards();
        else
            shopCards = dgs.getAct2ShopCards();

        if (shopCards.getSize() == 0)
            return false;
        ShopCard card = (ShopCard) shopCards.draw();
        return f.getInventory().add(new DescentCard(card));
    }
}
