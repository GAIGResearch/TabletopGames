package games.descent2e.actions.tokens;

import core.AbstractGameState;
import core.actions.AbstractAction;
import core.components.Card;
import core.components.Deck;
import games.descent2e.DescentGameState;
import games.descent2e.actions.DescentAction;
import games.descent2e.actions.Triggers;
import games.descent2e.components.Hero;
import games.descent2e.components.tokens.DToken;

import java.util.Random;

/**
 * Draw random search card and add to player
 */
public class SearchAction extends TokenAction {
    public SearchAction() {
        super(-1, Triggers.ACTION_POINT_SPEND);
    }

    @Override
    public SearchAction copy() {
        return null;
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
            boolean added = ((Hero) gs.getActingFigure()).getOtherEquipment().add(searchCards.pick(new Random(gs.getGameParameters().getRandomSeed())));
            if (added) {
                ((DToken) gs.getComponentById(tokenID)).setPosition(null);  // Take off the map
            }
            return added;
        }
        return false;
    }
}
