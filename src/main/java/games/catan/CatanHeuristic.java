package games.catan;

import core.AbstractGameState;
import core.components.Card;
import core.components.Deck;
import core.interfaces.IStateHeuristic;
import games.catan.components.Settlement;
import utilities.Utils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.OptionalInt;

import static core.CoreConstants.playerHandHash;

public class CatanHeuristic implements IStateHeuristic {

    double FACTOR_PLAYER_SCORE = 100;
    double FACTOR_PLAYER_RESOURCES = 10;
    double FACTOR_PLAYER_DEVELOPMENT_CARDS = 10;
    double FACTOR_PLAYER_CITIES = 50;
    double FACTOR_PLAYER_SETTLEMENTS = 40;
    double FACTOR_PLAYER_PORTS = 20;
    double FACTOR_PLAYER_KNIGHTS = 15;
    double FACTOR_OPPONENTS_SCORE = -1.0/3.0;


    @Override
    public double evaluateState(AbstractGameState gs, int playerId) {
        /**
         * TODO factors to evaluate:
         * largest army
         * longest road
         * development card advanced
         * opponents resource estimates
         */
        CatanGameState cgs = (CatanGameState)gs;
        Utils.GameResult gameStatus = cgs.getGameStatus();

        if(gameStatus == Utils.GameResult.LOSE)
            return -1;
        if(gameStatus == Utils.GameResult.WIN)
            return 1;

        double stateValue = 0;
        // scores evaluation
        int[] scores = cgs.getScores();
        for (int i = 0; i < scores.length; i++){
            if (i == playerId){
                stateValue+=FACTOR_PLAYER_SCORE*(scores[i]*0.1);
            }
            else {
                stateValue+=FACTOR_OPPONENTS_SCORE*(scores[i]*0.1);
            }
        }
        // player resource evaluation
        List<Card> playerHand = ((Deck<Card>)cgs.getComponent(playerHandHash,playerId)).getComponents();
        stateValue += FACTOR_PLAYER_RESOURCES * (Math.min(playerHand.size(),7) * (1.0/7.0));
        // player development card evaluation - very simple at the moment
        Deck<Card> playerDevDeck = (Deck<Card>)cgs.getComponentActingPlayer(CatanConstants.developmentDeckHash);
        stateValue += FACTOR_PLAYER_DEVELOPMENT_CARDS * (Math.min(playerDevDeck.getComponents().size(),10) * 0.1);
        // player settlement and city evaluation
        ArrayList<Settlement> settlements = cgs.getPlayersSettlements(playerId);
        int settlementCount = 0, cityCount = 0;
        for (int i = 0; i < settlements.size(); i++){
            if (settlements.get(i).getType()==1){
                settlementCount++;
            } else {
                cityCount++;
            }
        }
        stateValue+= FACTOR_PLAYER_SETTLEMENTS * (settlementCount * 0.2);
        stateValue+= FACTOR_PLAYER_CITIES * (cityCount * 0.25);
        // Player port control evaluation
        int[] playerExchangeRate = cgs.getExchangeRates(); // {4, 4, 4, 4, 4}
        int portControlCount = 0;
        for (int i = 0; i < playerExchangeRate.length; i++){
            if (playerExchangeRate[i] < 4) {
                portControlCount++;
            }
        }
        stateValue+= FACTOR_PLAYER_PORTS * (portControlCount * 0.2);
        // Player knight count evalution
        int[] knights = cgs.getKnights();
        int playerKnightsCount = knights[playerId];
        int highestKnightsCount = Arrays.stream(knights).max().getAsInt();
        stateValue += FACTOR_PLAYER_KNIGHTS * (((double) ((100 / highestKnightsCount^2) * playerKnightsCount^2))/100) ;

        return stateValue;
    }
}
