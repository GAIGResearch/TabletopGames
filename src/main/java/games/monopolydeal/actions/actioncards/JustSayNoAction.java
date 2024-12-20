package games.monopolydeal.actions.actioncards;

import core.AbstractGameState;
import core.actions.AbstractAction;
import core.components.Component;
import games.monopolydeal.MonopolyDealGameState;
import games.monopolydeal.cards.CardType;
import games.monopolydeal.cards.MonopolyDealCard;

/**
 * <p>A simple action which discards a JustSayNo card from a player's hand. This is used as a reaction for denying the effects for other action cards.</p>
 */
public class JustSayNoAction extends AbstractAction implements IActionCard {
    @Override
    public boolean execute(AbstractGameState gs) {
        MonopolyDealGameState MDGS = (MonopolyDealGameState) gs;
        MDGS.discardCard(CardType.JustSayNo,MDGS.getCurrentPlayer());
        return true;
    }
    @Override
    public JustSayNoAction copy() {
        return this;
    }
    @Override
    public boolean equals(Object obj) {
        return obj instanceof JustSayNoAction;
    }
    @Override
    public int hashCode() {
        return 1232389;
    }
    @Override
    public String toString() {
        return "JustSayNo Action";
    }
    @Override
    public String getString(AbstractGameState gameState) {
        return toString();
    }

    public int getTarget(MonopolyDealGameState gs) {
        return gs.getTurnOwner();
    }
}
