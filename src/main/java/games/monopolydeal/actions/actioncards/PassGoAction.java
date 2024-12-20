package games.monopolydeal.actions.actioncards;

import core.AbstractGameState;
import core.actions.AbstractAction;
import core.components.Component;
import games.monopolydeal.MonopolyDealGameState;
import games.monopolydeal.cards.CardType;
import games.monopolydeal.cards.MonopolyDealCard;

/**
 * <p>A simple action card which draws 2 cards into a player's hand when played.</p>
 */
public class PassGoAction extends AbstractAction implements IActionCard {
    @Override
    public boolean execute(AbstractGameState gs) {
        MonopolyDealGameState MDGS = (MonopolyDealGameState) gs;
        MDGS.drawCard(MDGS.getCurrentPlayer(),2);
        MDGS.discardCard(CardType.PassGo,MDGS.getCurrentPlayer());
        MDGS.useAction(1);
        return true;
    }
    @Override
    public PassGoAction copy() {
        return this;
    }
    @Override
    public boolean equals(Object obj) {
        return obj instanceof PassGoAction;
    }
    @Override
    public int hashCode() {
        return 23412030;
    }
    @Override
    public String toString() {
        return "PassGo Action";
    }
    @Override
    public String getString(AbstractGameState gameState) {
        return toString();
    }

    public int getTarget(MonopolyDealGameState gs) {
        return gs.getTurnOwner();
    }
}
