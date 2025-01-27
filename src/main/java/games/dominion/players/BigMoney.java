package games.dominion.players;

import core.AbstractGameState;
import core.AbstractPlayer;
import core.actions.AbstractAction;
import games.dominion.DominionGameState;
import games.dominion.actions.BuyCard;
import games.dominion.actions.EndPhase;
import games.dominion.cards.CardType;

import java.util.Collections;
import java.util.List;
import java.util.Map;

public class BigMoney extends AbstractPlayer {

    public BigMoney() {
        super(null, "BigMoney");
    }

    /**
     * Generate a valid action to play in the game. Valid actions can be found by accessing
     * AbstractGameState.getActions()
     *
     * @param gameState observation of the current game state
     */
    @Override
    public AbstractAction _getAction(AbstractGameState gameState, List<AbstractAction> possibleActions) {
        DominionGameState state = (DominionGameState) gameState;
        DominionGameState.DominionGamePhase phase = (DominionGameState.DominionGamePhase) state.getGamePhase();
        int player = gameState.getCurrentPlayer();
        int cash = state.getAvailableSpend(player);
        int provinces = state.getCardsIncludedInGame().getOrDefault(CardType.PROVINCE, 0);

        if (phase != DominionGameState.DominionGamePhase.Buy)
            return new EndPhase(phase);
        List<AbstractAction> actions = getForwardModel().computeAvailableActions(gameState, getParameters().actionSpace);

        switch (cash) {
            case 0:
            case 1:
                return new EndPhase(phase);
            case 2:
                if (provinces < 4 && actions.contains(new BuyCard(CardType.ESTATE, player)))
                    return new BuyCard(CardType.ESTATE, player);
                return new EndPhase(phase);
            case 3:
            case 4:
                return new BuyCard(CardType.SILVER, player);
            case 5:
                if (provinces < 6 && actions.contains(new BuyCard(CardType.DUCHY, player)))
                    return new BuyCard(CardType.DUCHY, player);
                else
                    return new BuyCard(CardType.SILVER, player);
            case 6:
            case 7:
                return new BuyCard(CardType.GOLD, player);
            default:
                return new BuyCard(CardType.PROVINCE, player);


        }
    }

    @Override
    public String toString() {
        return "BigMoney";
    }

    @Override
    public BigMoney copy() {
        return this;
    }

    @Override
    public Map<AbstractAction, Map<String, Object>> getDecisionStats() {
        return Collections.emptyMap();
    }
}
