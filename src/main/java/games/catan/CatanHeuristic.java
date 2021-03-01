package games.catan;

import core.AbstractGameState;
import core.components.Card;
import core.components.Deck;
import core.interfaces.IStateHeuristic;
import utilities.Utils;

import java.util.List;

import static core.CoreConstants.playerHandHash;

public class CatanHeuristic implements IStateHeuristic {

    double FACTOR_PLAYER_SCORE = 0.7;
    double FACTOR_OPPONENTS_SCORE = -1.0/3.0;
    double FACTOR_PLAYER_RESOURCES = 0.1;
    double FACTOR_PLAYER_DEVELOPMENT_CARDS = 0.2;


    @Override
    public double evaluateState(AbstractGameState gs, int playerId) {
        /**
         * TODO factors to evaluate:
         * cities
         * towns
         * ports
         * knight count
         * longest road closeness
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


        return stateValue;
    }
}
