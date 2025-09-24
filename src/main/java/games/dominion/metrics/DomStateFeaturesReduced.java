package games.dominion.metrics;

import core.AbstractGameState;
import core.interfaces.IStateFeatureVector;
import games.dominion.*;
import games.dominion.cards.CardType;
import games.dominion.cards.DominionCard;

public class DomStateFeaturesReduced implements IStateFeatureVector {

    String[] names = new String[]{"victoryPoints", "treasureValue", "actionCards", "treasureInHand",
            "actionCardsInHand", "actionsLeft", "buysLeft",
            "provinceCount", "duchyCount", "estateCount", "totalCards", "actionSize",
            "currentLead", "provincesLeft", "emptyDecks"};
    DominionForwardModel fm = new DominionForwardModel();

    @Override
    public double[] doubleVector(AbstractGameState gs, int playerId) {
        DominionGameState state = (DominionGameState) gs;

        double[] retValue = new double[names.length];

        // victoryPoints - simply the current score
        retValue[0] = state.getGameScore(playerId);

        // treasureValue - mean treasure value per card
        retValue[1] = (double) state.getTotal(playerId, DominionCard::treasureValue) / state.getTotalCards(playerId);

        // actionCards - percentage of deck made of action cards
        retValue[2] = state.getTotal(playerId, c -> c.isActionCard() ? 1 : 0) /
                (double) state.getTotalCards(playerId);

        // treasureInHand - total treasure in hand
        retValue[3] = state.getTotal(playerId, DominionConstants.DeckType.HAND, DominionCard::treasureValue);

        // actionCardsInHand
        retValue[4] = state.getTotal(playerId, DominionConstants.DeckType.HAND, c -> c.isActionCard() ? 1 : 0);

        // actionsLeft
        if (state.getCurrentPlayer() == playerId)
            retValue[5] = state.getActionsLeft();

        // buysLeft
        if (state.getCurrentPlayer() == playerId)
            retValue[6] = state.getBuysLeft();

        retValue[7] = state.getTotal(playerId, c -> c.cardType() == CardType.PROVINCE ? 1 : 0);

        retValue[8] = state.getTotal(playerId, c -> c.cardType() == CardType.DUCHY ? 1 : 0);

        retValue[9] = state.getTotal(playerId, c -> c.cardType() == CardType.ESTATE ? 1 : 0);

        retValue[10] = state.getTotalCards(playerId);

        retValue[11] = fm.computeAvailableActions(state).size();

        double bestScoreOfOtherPlayers = 0.0;
        for (int i = 0; i < state.getNPlayers(); i++) {
            if (i != playerId) {
                double score = state.getGameScore(i);
                if (score > bestScoreOfOtherPlayers) {
                    bestScoreOfOtherPlayers = score;
                }
            }
        }
        retValue[12] = retValue[0] - bestScoreOfOtherPlayers;

        retValue[13] = state.cardsOfType(CardType.PROVINCE, -1, DominionConstants.DeckType.SUPPLY);

        retValue[14] = state.getEmptyDeckCount();

        return retValue;
    }

    @Override
    public String[] names() {
        return names;
    }

}
