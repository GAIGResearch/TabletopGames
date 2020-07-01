package games.loveletter;
import core.AbstractGameState;
import core.interfaces.IStateHeuristic;
import games.loveletter.cards.LoveLetterCard;
import utilities.Utils;

import java.util.Random;

public class LoveLetterHeuristic implements IStateHeuristic {

    double COUNTESS_PLAY_THRESHOLD = 0.1;

    // Simple sum of card values
    
    @Override
    public double evaluateState(AbstractGameState gs, int playerId) {
        LoveLetterGameState llgs = (LoveLetterGameState) gs;
        Utils.GameResult gameStatus = gs.getGameStatus();

        if (gameStatus == Utils.GameResult.LOSE)
            return -1;
        if (gameStatus == Utils.GameResult.WIN)
            return 1;

        double cardValues = 0;

        Random r = new Random(llgs.getGameParameters().getGameSeed());
        for (LoveLetterCard card: llgs.getPlayerHandCards().get(playerId).getComponents()) {
            if (card.cardType == LoveLetterCard.CardType.Countess) {
                if (r.nextDouble() > COUNTESS_PLAY_THRESHOLD) {
                    cardValues += LoveLetterCard.CardType.Countess.getValue();
                }
            } else {
                cardValues += card.cardType.getValue();
            }
        }

        double maxCardValue = llgs.getPlayerHandCards().get(playerId).getSize() * LoveLetterCard.CardType.getMaxCardValue();

        return cardValues/maxCardValue;
    }

}