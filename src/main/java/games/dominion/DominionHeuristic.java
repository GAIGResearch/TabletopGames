package games.dominion;

import core.AbstractGameState;
import core.CoreConstants;
import core.interfaces.IStateHeuristic;
import evaluation.optimisation.TunableParameters;
import games.dominion.cards.CardType;
import games.dominion.cards.DominionCard;
import utilities.Utils;

import static games.dominion.DominionConstants.*;

public class DominionHeuristic extends TunableParameters implements IStateHeuristic {

    double victoryPoints = 0.5;
    double treasureValue = 0.4;
    double actionCards = 0.1;
    double treasureInHand = 0.3;
    double actionCardsInHand = 0.3;
    double actionsLeft = 0.1;
    double buysLeft = 0.1;
    double provinceCount = 0.1;
    double duchyCount = 0.0;
    double estateCount = -0.1;
    double totalCards = 0.0;

    public DominionHeuristic() {
        addTunableParameter("victoryPoints", 0.5);
        addTunableParameter("treasureValue", 0.4);
        addTunableParameter("actionCards", 0.1);
        addTunableParameter("treasureInHand", 0.3);
        addTunableParameter("actionsLeft", 0.3);
        addTunableParameter("buysLeft", 0.1);
        addTunableParameter("actionCardsInHand", 0.1);
        addTunableParameter("provinceCount", 0.2);
        addTunableParameter("duchyCount", 0.0);
        addTunableParameter("estateCount", -0.1);
        addTunableParameter("totalCards", 0.0);
    }

    @Override
    public void _reset() {
        victoryPoints = (double) getParameterValue("victoryPoints");
        treasureValue = (double) getParameterValue("treasureValue");
        actionCards = (double) getParameterValue("actionCards");
        treasureInHand = (double) getParameterValue("treasureInHand");
        actionCardsInHand = (double) getParameterValue("actionCardsInHand");
        actionsLeft = (double) getParameterValue("actionsLeft");
        buysLeft = (double) getParameterValue("buysLeft");
        provinceCount = (double) getParameterValue("provinceCount");
        duchyCount = (double) getParameterValue("duchyCount");
        estateCount = (double) getParameterValue("estateCount");
        totalCards = (double) getParameterValue("totalCards");
    }


    /**
     * Returns a score for the state that should be maximised by the player (the bigger, the better).
     * Ideally bounded between [-1, 1].
     *
     * @param gs       - game state to evaluate and score.
     * @param playerId
     * @return - value of given state.
     */
    @Override
    public double evaluateState(AbstractGameState gs, int playerId) {
        DominionGameState state = (DominionGameState) gs;
        CoreConstants.GameResult playerResult = state.getPlayerResults()[playerId];

        if (playerResult == CoreConstants.GameResult.LOSE_GAME)
            return -1;
        if (playerResult == CoreConstants.GameResult.WIN_GAME)
            return 1;

        // We have several factors to consider (all maxed to 1.0)
        double retValue = 0.0;

        // victoryPoints - simply the current score divided by 100 and number of players
        if (victoryPoints != 0.0)
            retValue += victoryPoints * Math.min(state.getGameScore(playerId) / 100.0, 1.0);

        // treasureValue - total treasure in hand divided by 200
        if (treasureValue != 0.0)
            retValue += treasureValue * Math.min(state.getTotal(playerId, DominionCard::treasureValue) / 200.0, 1.0);

        // actionCards - percentage of deck made of action cards
        if (actionCards != 0.0)
            retValue += actionCards * state.getTotal(playerId, c -> c.isActionCard() ? 1 : 0) /
                    (double) state.getTotalCards(playerId);

        // treasureInHand - total treasure in hand divided by 20
        if (treasureInHand != 0.0)
            retValue += treasureInHand * Math.min(state.getTotal(playerId, DeckType.HAND, DominionCard::treasureValue) / 20.0, 1.0);

        // actionCardsInHand - number / 5 of actionCards In Hand
        if (actionCardsInHand != 0.0)
            retValue += actionCardsInHand * Math.min(state.getTotal(playerId, DeckType.HAND, c -> c.isActionCard() ? 1 : 0) / 5.0, 1.0);

        // actionsLeft / 5.
        if (actionsLeft != 0.0)
            if (state.getCurrentPlayer() == playerId)
                retValue += actionsLeft * Math.min(state.getActionsLeft() / 5.0, 1.0);

        // buysLeft / 5
        if (buysLeft != 0.0)
            if (state.getCurrentPlayer() == playerId)
                retValue += buysLeft * Math.min(state.getBuysLeft() / 5.0, 1.0);

        if (provinceCount != 0.0)
            retValue += provinceCount * state.getTotal(playerId, c -> c.cardType() == CardType.PROVINCE ? 1 : 0) / 12.0;

        if (duchyCount != 0.0)
            retValue += duchyCount * state.getTotal(playerId, c -> c.cardType() == CardType.DUCHY ? 1 : 0) / 12.0;

        if (estateCount != 0.0)
            retValue += estateCount * state.getTotal(playerId, c -> c.cardType() == CardType.ESTATE ? 1 : 0) / 12.0;

        if (totalCards != 0.0)
            retValue += totalCards * state.getTotalCards(playerId) / 40.0;

        return Utils.clamp(retValue, -1.0, 1.0);
    }


    @Override
    protected DominionHeuristic _copy() {
        DominionHeuristic retValue = new DominionHeuristic();
        retValue.victoryPoints = victoryPoints;
        retValue.treasureValue = treasureValue;
        retValue.actionCards = actionCards;
        retValue.treasureInHand = treasureInHand;
        retValue.actionsLeft = actionsLeft;
        retValue.buysLeft = buysLeft;
        retValue.actionCardsInHand = actionCardsInHand;
        retValue.provinceCount = provinceCount;
        retValue.duchyCount = duchyCount;
        retValue.estateCount = estateCount;
        retValue.totalCards = totalCards;
        return retValue;
    }

    @Override
    protected boolean _equals(Object o) {
        if (o instanceof DominionHeuristic) {
            DominionHeuristic other = (DominionHeuristic) o;
            return other.victoryPoints == victoryPoints && other.buysLeft == buysLeft && other.actionCards == actionCards &&
                    other.treasureInHand == treasureInHand && other.treasureValue == other.treasureValue &&
                    other.actionCardsInHand == actionCardsInHand && other.actionsLeft == actionsLeft &&
                    other.estateCount == estateCount && other.duchyCount == duchyCount && other.provinceCount == provinceCount &&
                    other.totalCards == totalCards;
        }
        return false;
    }

    /**
     * @return Returns Tuned Parameters corresponding to the current settings
     * (will use all defaults if setParameterValue has not been called at all)
     */
    @Override
    public DominionHeuristic instantiate() {
        return _copy();
    }

}
