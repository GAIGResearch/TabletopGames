package games.catan;

import core.AbstractGameState;
import core.AbstractParameters;
import core.CoreConstants;
import core.components.BoardNodeWithEdges;
import core.components.Counter;
import core.interfaces.IStateHeuristic;
import evaluation.optimisation.TunableParameters;
import games.catan.components.Building;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import static games.catan.components.Building.Type.Settlement;

public class CatanHeuristic extends TunableParameters implements IStateHeuristic {
    double playerScore = 0.4;
    double playerResources = 0.05;
    double playerDevelopmentCards = 0.05;
    double playerCities = 0.25;
    double playerSettlements = 0.15;
    double playerPorts = 0.1;
    double opponentsScore = -1.0;

    public CatanHeuristic() {
        addTunableParameter("playerScore", 0.4);
        addTunableParameter("playerResources", 0.05);
        addTunableParameter("playerDevelopmentCards", 0.05);
        addTunableParameter("playerCities", 0.25);
        addTunableParameter("playerSettlements", 0.15);
        addTunableParameter("playerPorts", 0.1);
        addTunableParameter("opponentsScore", -1.0);
        _reset();
    }

    @Override
    public void _reset() {
        playerScore = (double) getParameterValue("playerScore");
        playerResources = (double) getParameterValue("playerResources");
        playerDevelopmentCards = (double) getParameterValue("playerDevelopmentCards");
        playerCities = (double) getParameterValue("playerCities");
        playerSettlements = (double) getParameterValue("playerSettlements");
        playerPorts = (double) getParameterValue("playerPorts");
        opponentsScore = (double) getParameterValue("opponentsScore");
    }

    @Override
    public double evaluateState(AbstractGameState gs, int playerId) {
        CatanGameState state = (CatanGameState) gs;
        CoreConstants.GameResult playerResult = state.getPlayerResults()[playerId];

        if (playerResult == CoreConstants.GameResult.LOSE_GAME)
            return -1;
        if (playerResult == CoreConstants.GameResult.WIN_GAME)
            return 1;

        double stateValue = 0.0;

        // value each player’s score then divide by total required to win, for opponents divide by number of opponents
        if(playerScore != 0.0 || opponentsScore != 0.0){
            int[] scores = state.getScores();
            for(int i = 0; i < scores.length; i++){
                if (i != playerId){
                    stateValue += opponentsScore * ((((scores[i] + state.getVictoryPoints()[i]))
                            / (double)((CatanParameters)state.getGameParameters()).points_to_win)/(double) state.getNPlayers());
                } else {
                    stateValue += playerScore * (((scores[i]))
                            / (double)((CatanParameters)state.getGameParameters()).points_to_win);
                }
            }
        }


        // value total resources, caps out at 7
        if(playerResources != 0.0){
            stateValue += playerResources * Math.min(((double) state.getNResourcesInHand(playerId))/7.0,1.0);
        }

        // value development card count
        if(playerDevelopmentCards != 0){
            stateValue += playerDevelopmentCards * Math.min(((double) state.playerDevCards.get(playerId).getSize())/3.0,1.0);
        }

        // value player cities and settlements
        if(playerCities != 0.0 || playerSettlements != 0.0){
            int settlementCount = 0, cityCount = 0;
            ArrayList<BoardNodeWithEdges> settlements = state.getPlayersSettlements(playerId);
            for (BoardNodeWithEdges settlement : settlements) {
                if(((Building)settlement).getBuildingType() == Settlement)
                    settlementCount++;
                else
                    cityCount++;
            }
            stateValue += playerCities * (cityCount/4.0) + playerSettlements * (settlementCount/5.0);
        }

        // value player ports
        if (playerPorts != 0.0){
            Map<CatanParameters.Resource, Counter> playerExchangeRates = state.getExchangeRates(playerId);
            for(Map.Entry<CatanParameters.Resource, Counter> e: playerExchangeRates.entrySet()){
                    if(e.getValue().getValue() < ((CatanParameters)state.getGameParameters()).default_exchange_rate - 1)
                        stateValue += playerPorts * 0.2;
                    else if (e.getValue().getValue() < ((CatanParameters)state.getGameParameters()).default_exchange_rate)
                        stateValue += playerPorts * 0.1;
            }
        }

        return stateValue;
    }

    @Override
    protected AbstractParameters _copy() {
        CatanHeuristic retValue = new CatanHeuristic();
        retValue.playerScore = playerScore;
        retValue.playerResources = playerResources;
        retValue.playerDevelopmentCards = playerDevelopmentCards;
        retValue.playerCities = playerCities;
        retValue.playerSettlements = playerSettlements;
        retValue.playerPorts = playerPorts;
        retValue.opponentsScore = opponentsScore;
        return retValue;
    }

    @Override
    protected boolean _equals(Object o) {
        if (o instanceof CatanHeuristic) {
            CatanHeuristic other = (CatanHeuristic) o;
            return other.playerScore == playerScore
                    && other.playerResources == playerResources
                    && other.playerDevelopmentCards == playerDevelopmentCards
                    && other.playerCities == playerCities
                    && other.playerSettlements == playerSettlements
                    && other.playerPorts == playerPorts
                    && other.opponentsScore == opponentsScore;
        }
        return false;
    }

    @Override
    public Object instantiate() {
        return _copy();
    }
}
