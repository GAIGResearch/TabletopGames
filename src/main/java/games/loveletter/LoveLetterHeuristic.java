package games.loveletter;
import core.AbstractGameState;
import core.interfaces.IStateHeuristic;
import evaluation.TunableParameters;
import games.loveletter.cards.LoveLetterCard;
import utilities.Utils;

import java.util.Random;

public class LoveLetterHeuristic extends TunableParameters implements IStateHeuristic {

    double COUNTESS_PLAY_THRESHOLD = 0.1;
    double FACTOR_CARDS = 0.3;
    double FACTOR_AFFECTION = 0.7;

    public LoveLetterHeuristic() {
        addTunableParameter("COUNTESS_PLAY_THRESHOLD", 0.1);
        addTunableParameter("FACTOR_CARDS", 0.3);
        addTunableParameter("FACTOR_AFFECTION", 0.7);
    }

    @Override
    public void _reset() {
        COUNTESS_PLAY_THRESHOLD = (double) getParameterValue("COUNTESS_PLAY_THRESHOLD");
        FACTOR_CARDS = (double) getParameterValue("FACTOR_CARDS");
        FACTOR_AFFECTION = (double) getParameterValue("FACTOR_AFFECTION");
    }

    @Override
    public double evaluateState(AbstractGameState gs, int playerId) {
        LoveLetterGameState llgs = (LoveLetterGameState) gs;
        LoveLetterParameters llp = (LoveLetterParameters) gs.getGameParameters();
        Utils.GameResult playerResult = gs.getPlayerResults()[playerId];

        if (!gs.isNotTerminal()) {
            return playerResult.value;
        }
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
        double nRequiredTokens = (llgs.getNPlayers() == 2? llp.nTokensWin2 : llgs.getNPlayers() == 3? llp.nTokensWin3 : llp.nTokensWin4);
        if (nRequiredTokens < llgs.affectionTokens[playerId]) nRequiredTokens = llgs.affectionTokens[playerId];

        return FACTOR_CARDS * (cardValues/maxCardValue) + FACTOR_AFFECTION * (llgs.affectionTokens[playerId]/nRequiredTokens);
    }

    /**
     * Return a copy of this game parameters object, with the same parameters as in the original.
     *
     * @return - new game parameters object.
     */
    @Override
    protected LoveLetterHeuristic _copy() {
        LoveLetterHeuristic retValue = new LoveLetterHeuristic();
        retValue.COUNTESS_PLAY_THRESHOLD = COUNTESS_PLAY_THRESHOLD;
        retValue.FACTOR_CARDS = FACTOR_CARDS;
        retValue.FACTOR_AFFECTION = FACTOR_AFFECTION;
        return retValue;
    }

    /**
     * Checks if the given object is the same as the current.
     *
     * @param o - other object to test equals for.
     * @return true if the two objects are equal, false otherwise
     */
    @Override
    protected boolean _equals(Object o) {
        if (o instanceof LoveLetterHeuristic) {
            LoveLetterHeuristic other = (LoveLetterHeuristic) o;
            return other.COUNTESS_PLAY_THRESHOLD == COUNTESS_PLAY_THRESHOLD &&
                    other.FACTOR_AFFECTION == FACTOR_AFFECTION && other.FACTOR_CARDS == FACTOR_CARDS;
        }
        return false;
    }

    /**
     * @return Returns Tuned Parameters corresponding to the current settings
     * (will use all defaults if setParameterValue has not been called at all)
     */
    @Override
    public LoveLetterHeuristic instantiate() {
        return this._copy();
    }

}