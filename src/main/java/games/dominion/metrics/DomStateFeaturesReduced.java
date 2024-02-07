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
        CoreConstants.GameResult playerResult = state.getPlayerResults()[playerId];

        double[] retValue = new double[names.length];

        // victoryPoints - simply the current score divided by 100 and number of players
        retValue[0] = Math.min(state.getGameScore(playerId) / 100.0, 1.0);

        // treasureValue - total treasure in hand divided by 200
        retValue[1] = Math.min(state.getTotal(playerId, DominionCard::treasureValue) / 200.0, 1.0);

        // actionCards - percentage of deck made of action cards
        retValue[2] = state.getTotal(playerId, c -> c.isActionCard() ? 1 : 0) /
                (double) state.getTotalCards(playerId);

        // treasureInHand - total treasure in hand divided by 20
        retValue[3] = Math.min(state.getTotal(playerId, DominionConstants.DeckType.HAND, DominionCard::treasureValue) / 20.0, 1.0);

        // actionCardsInHand - number / 5 of actionCards In Hand
        retValue[4] = Math.min(state.getTotal(playerId, DominionConstants.DeckType.HAND, c -> c.isActionCard() ? 1 : 0) / 5.0, 1.0);

        // actionsLeft / 5.
        if (state.getCurrentPlayer() == playerId)
            retValue[5] = Math.min(state.actionsLeft() / 5.0, 1.0);

        // buysLeft / 5
        if (state.getCurrentPlayer() == playerId)
            retValue[6] = Math.min(state.buysLeft() / 5.0, 1.0);

        retValue[7] = state.getTotal(playerId, c -> c.cardType() == CardType.PROVINCE ? 1 : 0) / 12.0;

        retValue[8] = state.getTotal(playerId, c -> c.cardType() == CardType.DUCHY ? 1 : 0) / 12.0;

        retValue[9] = state.getTotal(playerId, c -> c.cardType() == CardType.ESTATE ? 1 : 0) / 12.0;

        retValue[10] = state.getTotalCards(playerId) / 40.0;

        return retValue;
    }

    @Override
    public String[] names() {
        return names;
    }

}
