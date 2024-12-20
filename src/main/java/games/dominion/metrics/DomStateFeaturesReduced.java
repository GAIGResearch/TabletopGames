package games.dominion.metrics;

import core.AbstractGameState;
import core.CoreConstants;
import core.interfaces.IStateFeatureVector;
import games.dominion.DominionConstants;
import games.dominion.DominionGameState;
import games.dominion.cards.CardType;
import games.dominion.cards.DominionCard;

public class DomStateFeaturesReduced implements IStateFeatureVector {

    String[] names = new String[]{"victoryPoints", "treasureValue", "actionCards", "treasureInHand",
            "actionsLeft", "buysLeft", "actionCardsInHand",
            "provinceCount", "duchyCount", "estateCount", "totalCards"};

    @Override
    public double[] featureVector(AbstractGameState gs, int playerId) {
        DominionGameState state = (DominionGameState) gs;

        double[] retValue = new double[names.length];

        // victoryPoints - simply the current score
        retValue[0] = state.getGameScore(playerId);

        // treasureValue - total treasure in hand
        retValue[1] = state.getTotal(playerId, DominionCard::treasureValue);

        // actionCards - percentage of deck made of action cards
        retValue[2] = state.getTotal(playerId, c -> c.isActionCard() ? 1 : 0) /
                (double) state.getTotalCards(playerId);

        // treasureInHand - total treasure in hand
        retValue[3] = state.getTotal(playerId, DominionConstants.DeckType.HAND, DominionCard::treasureValue);

        // actionCardsInHand
        retValue[4] = state.getTotal(playerId, DominionConstants.DeckType.HAND, c -> c.isActionCard() ? 1 : 0);

        // actionsLeft
        if (state.getCurrentPlayer() == playerId)
            retValue[5] = state.actionsLeft();

        // buysLeft
        if (state.getCurrentPlayer() == playerId)
            retValue[6] = state.buysLeft() / 5.0;

        retValue[7] = state.getTotal(playerId, c -> c.cardType() == CardType.PROVINCE ? 1 : 0);

        retValue[8] = state.getTotal(playerId, c -> c.cardType() == CardType.DUCHY ? 1 : 0);

        retValue[9] = state.getTotal(playerId, c -> c.cardType() == CardType.ESTATE ? 1 : 0);

        retValue[10] = state.getTotalCards(playerId);

        return retValue;
    }

    @Override
    public String[] names() {
        return names;
    }

}
