package games.catan;

import core.AbstractGameState;
import core.interfaces.IStateHeuristic;
import utilities.Utils;

public class CatanHeuristic implements IStateHeuristic {

    double FACTOR_PLAYER_SCORE = 1;
    double FACTOR_OPPONENTS_SCORE = -0.33;


    @Override
    public double evaluateState(AbstractGameState gs, int playerId) {
        /**
         * TODO factors to evaluate further down the line:
         * player resources
         * cities
         * towns
         * ports
         * opponents resource estimates
         */
        CatanGameState cgs = (CatanGameState)gs;
        Utils.GameResult gameStatus = cgs.getGameStatus();

        double stateValue = 0; //todo condense this down into a single line return statement once testing is done

        if(gameStatus == Utils.GameResult.LOSE)
            stateValue = -1;
        if(gameStatus == Utils.GameResult.WIN)
            stateValue = 1;

        int[] scores = cgs.getScores();
        for (int i = 0; i < scores.length; i++){
            if (i == playerId){
                stateValue+=FACTOR_PLAYER_SCORE*scores[i]*0.1;
            }
            else {
                stateValue+=FACTOR_OPPONENTS_SCORE*scores[i]*0.1;
            }
        }

        return stateValue;
    }
}
