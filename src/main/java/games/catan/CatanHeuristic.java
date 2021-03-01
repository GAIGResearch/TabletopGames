package games.catan;

import core.AbstractGameState;
import core.components.Card;
import core.components.Deck;
import core.interfaces.IStateHeuristic;
import utilities.Utils;

import java.util.List;

import static core.CoreConstants.playerHandHash;

public class CatanHeuristic implements IStateHeuristic {

    double FACTOR_PLAYER_SCORE = 0.9;
    double FACTOR_OPPONENTS_SCORE = 1.0/3.0;
    double FACTOR_PLAYER_RESOURCES = 0.1;


    @Override
    public double evaluateState(AbstractGameState gs, int playerId) {
        /**
         * TODO factors to evaluate:
         * cities
         * towns
         * ports
         * opponents resource estimates
         * development card count
         */
        CatanGameState cgs = (CatanGameState)gs;
        Utils.GameResult gameStatus = cgs.getGameStatus();

        if(gameStatus == Utils.GameResult.LOSE)
            return -1;
        if(gameStatus == Utils.GameResult.WIN)
            return 1;

        double stateValue = 0;

        int[] scores = cgs.getScores();
        for (int i = 0; i < scores.length; i++){
            if (i == playerId){
                stateValue+=FACTOR_PLAYER_SCORE*(scores[i]*0.1);
            }
            else {
                stateValue+=FACTOR_OPPONENTS_SCORE*(scores[i]*0.1);
            }
        }

        List<Card> playerHand = ((Deck<Card>)cgs.getComponent(playerHandHash,playerId)).getComponents();
        stateValue += FACTOR_PLAYER_RESOURCES * (Math.min(playerHand.size(),7) * (1.0/7.0));

        return stateValue;
    }
}
