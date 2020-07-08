package games.loveletter;
import core.AbstractGameState;
import core.interfaces.IStateHeuristic;
import games.loveletter.cards.LoveLetterCard;
import utilities.Utils;

import java.util.Random;

public class LoveLetterHeuristic implements IStateHeuristic {

    double COUNTESS_PLAY_THRESHOLD = 0.1;
    double FACTOR_CARDS = 0.3;
    double FACTOR_AFFECTION = 0.7;

    // Simple sum of card values

    @Override
    public double evaluateState(AbstractGameState gs, int playerId) {
        LoveLetterGameState llgs = (LoveLetterGameState) gs;
        LoveLetterParameters llp = (LoveLetterParameters) gs.getGameParameters();
        Utils.GameResult gameStatus = gs.getGameStatus();

        if (gameStatus == Utils.GameResult.LOSE)
            return -1;
        if (gameStatus == Utils.GameResult.WIN)
            return 1;

        double cardValues = 0;

        Random r = new Random(llgs.getGameParameters().getRandomSeed());
        for (LoveLetterCard card: llgs.getPlayerHandCards().get(playerId).getComponents()) {
            if (card.cardType == LoveLetterCard.CardType.Countess) {
                if (r.nextDouble() > COUNTESS_PLAY_THRESHOLD) {
                    cardValues += LoveLetterCard.CardType.Countess.getValue();
                }
            } else {
                cardValues += card.cardType.getValue();
            }
        }

        double maxCardValue = 1+llgs.getPlayerHandCards().get(playerId).getSize() * LoveLetterCard.CardType.getMaxCardValue();
        double nRequiredTokens = (llgs.getNPlayers()-1 < llp.nTokensWin.length ? llp.nTokensWin[llgs.getNPlayers()-1] :
                llp.nTokensWin[llp.nTokensWin.length-1]);
        if (nRequiredTokens < llgs.affectionTokens[playerId]) nRequiredTokens = llgs.affectionTokens[playerId];

        return FACTOR_CARDS * (cardValues/maxCardValue) + FACTOR_AFFECTION * (llgs.affectionTokens[playerId]/nRequiredTokens);
    }

}