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
import games.descent2e.components.tokens.DToken;
import utilities.Vector2D;

import java.util.List;
import java.util.Random;

import static utilities.Utils.getNeighbourhood;

/**
 * Draw random search card and add to player
 */
public class SearchAction extends TokenAction {
    public SearchAction() {
        super(-1, Triggers.ACTION_POINT_SPEND);
    }

    @Override
    public SearchAction copy() {
        return this;
    }

    @Override
    public boolean canExecute(DescentGameState gs) {
        // Can only execute if player adjacent to search token
        DToken acolyte = (DToken) gs.getComponentById(tokenID);
        Hero hero = gs.getHeroes().get(acolyte.getOwnerId());
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
        return super.equals(o) && o instanceof TokenAction;
    }

    @Override
    public String getString(AbstractGameState gameState) {
        return null;
    }

    @Override
    public boolean execute(DescentGameState gs) {
        Deck<Card> searchCards = gs.getSearchCards();
        if (searchCards != null) {
            Hero hero = (Hero) gs.getActingFigure();
            boolean added = hero.getOtherEquipment().add(new DescentCard(searchCards.pick(new Random(gs.getGameParameters().getRandomSeed()))));
            if (added) {
                ((DToken) gs.getComponentById(tokenID)).setPosition(null);  // Take off the map
                hero.getNActionsExecuted().increment();
            }
            return added;
        }
        return false;
    }
}
