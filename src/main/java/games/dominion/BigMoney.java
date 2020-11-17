package games.dominion;

import core.AbstractGameState;
import core.AbstractPlayer;
import core.actions.AbstractAction;
import games.dominion.actions.BuyCard;
import games.dominion.actions.EndPhase;
import games.dominion.cards.CardType;

import java.util.*;

public class BigMoney extends AbstractPlayer {

    /**
     * Generate a valid action to play in the game. Valid actions can be found by accessing
     * AbstractGameState.getActions()
     *
     * @param gameState observation of the current game state
     */
    @Override
    public AbstractAction getAction(AbstractGameState gameState) {
        DominionGameState state = (DominionGameState) gameState;
        List<AbstractAction> actions = state.getActions();
        int cash = state.availableSpend(getPlayerID());
        int provinces = state.cardsAvailable.getOrDefault(CardType.PROVINCE, 0);

        if (state.getGamePhase() != DominionGameState.DominionGamePhase.Buy)
            return new EndPhase();

        switch (cash) {
            case 0:
            case 1:
                return new EndPhase();
            case 2:
                if (provinces < 4 && actions.contains(new BuyCard(CardType.ESTATE, getPlayerID())))
                    return new BuyCard(CardType.ESTATE, getPlayerID());
                return new EndPhase();
            case 3:
            case 4:
                return new BuyCard(CardType.SILVER, getPlayerID());
            case 5:
                if (provinces < 6 && actions.contains(new BuyCard(CardType.DUCHY, getPlayerID())))
                    return new BuyCard(CardType.DUCHY, getPlayerID());
                else
                    return new BuyCard(CardType.SILVER, getPlayerID());
            case 6:
            case 7:
                return new BuyCard(CardType.GOLD, getPlayerID());
            default:
                return new BuyCard(CardType.PROVINCE, getPlayerID());


        }
    }

    @Override
    public String toString() {
        return "BigMoney";
    }
}
