package games.loveletter;

import core.AbstractGameState;
import core.CoreConstants;
import core.components.PartialObservableDeck;
import core.interfaces.IStateHeuristic;
import evaluation.optimisation.TunableParameters;
import games.loveletter.cards.CardType;
import games.loveletter.cards.LoveLetterCard;
import utilities.Utils;

import java.util.HashSet;
import java.util.Set;
import java.util.stream.IntStream;

import static games.loveletter.cards.CardType.*;

public class LoveLetterHeuristic extends TunableParameters implements IStateHeuristic {

    double FACTOR_CARDS = 0.3;
    double FACTOR_AFFECTION = 0.7;
    double FACTOR_COUNTESS = 0.0;
    double FACTOR_BARON = 0.0;
    double FACTOR_GUARD = 0.0;
    double FACTOR_HANDMAID = 0.0;
    double FACTOR_KING = 0.0;
    double FACTOR_PRIEST = 0.0;
    double FACTOR_PRINCE = 0.0;
    double FACTOR_PRINCESS = 0.0;
    double FACTOR_HIDDEN = 0.0;
    double FACTOR_ADVANTAGE = 0.0;

    public LoveLetterHeuristic() {
        addTunableParameter("FACTOR_CARDS", 0.3);
        addTunableParameter("FACTOR_AFFECTION", 0.7);
        addTunableParameter("FACTOR_COUNTESS", 0.0);
        addTunableParameter("FACTOR_BARON", 0.0);
        addTunableParameter("FACTOR_GUARD", 0.0);
        addTunableParameter("FACTOR_HANDMAID", 0.0);
        addTunableParameter("FACTOR_KING", 0.0);
        addTunableParameter("FACTOR_PRIEST", 0.0);
        addTunableParameter("FACTOR_PRINCE", 0.0);
        addTunableParameter("FACTOR_PRINCESS", 0.0);
        addTunableParameter("FACTOR_HIDDEN", 0.0);
        addTunableParameter("FACTOR_ADVANTAGE", 0.0);
    }

    @Override
    public void _reset() {
        FACTOR_CARDS = (double) getParameterValue("FACTOR_CARDS");
        FACTOR_AFFECTION = (double) getParameterValue("FACTOR_AFFECTION");
        FACTOR_ADVANTAGE = (double) getParameterValue("FACTOR_ADVANTAGE");
        FACTOR_BARON = (double) getParameterValue("FACTOR_BARON");
        FACTOR_GUARD = (double) getParameterValue("FACTOR_GUARD");
        FACTOR_COUNTESS = (double) getParameterValue("FACTOR_COUNTESS");
        FACTOR_HANDMAID = (double) getParameterValue("FACTOR_HANDMAID");
        FACTOR_KING = (double) getParameterValue("FACTOR_KING");
        FACTOR_PRIEST = (double) getParameterValue("FACTOR_PRIEST");
        FACTOR_PRINCE = (double) getParameterValue("FACTOR_PRINCE");
        FACTOR_PRINCESS = (double) getParameterValue("FACTOR_PRINCESS");
    }

    @Override
    public double evaluateState(AbstractGameState gs, int playerId) {
        LoveLetterGameState llgs = (LoveLetterGameState) gs;
        LoveLetterParameters llp = (LoveLetterParameters) gs.getGameParameters();
        CoreConstants.GameResult playerResult = gs.getPlayerResults()[playerId];

        if (!gs.isNotTerminal()) {
            return playerResult.value;
        }
        double cardValues = 0;

        Set<CardType> cardTypes = new HashSet<>();
        for (LoveLetterCard card : llgs.getPlayerHandCards().get(playerId).getComponents()) {
            cardValues += card.cardType.getValue();
            cardTypes.add(card.cardType);
        }

        double maxCardValue = 1 + llgs.getPlayerHandCards().get(playerId).getSize() * getMaxCardValue();
        double nRequiredTokens = (llgs.getNPlayers() == 2 ? llp.nTokensWin2 : llgs.getNPlayers() == 3 ? llp.nTokensWin3 : llp.nTokensWin4);
        if (nRequiredTokens < llgs.affectionTokens[playerId]) nRequiredTokens = llgs.affectionTokens[playerId];

        double retValue = FACTOR_CARDS * (cardValues / maxCardValue) + FACTOR_AFFECTION * (llgs.affectionTokens[playerId] / nRequiredTokens);

        if (FACTOR_HIDDEN != 0.0) {
            int visibleCards = 0;
            for (int player = 0; player < llgs.getNPlayers(); player++) {
                if (player != playerId) {
                    PartialObservableDeck<LoveLetterCard> deck = llgs.getPlayerHandCards().get(player);
                    visibleCards += (int) IntStream.range(0, deck.getSize()).filter(i -> deck.getVisibilityForPlayer(i, playerId)).count();
                }
            }
            retValue += visibleCards * FACTOR_HIDDEN / (llgs.getNPlayers() - 1.0);
        }

        if (FACTOR_ADVANTAGE != 0.0) {
            int maxOtherScore = IntStream.range(0, llgs.getNPlayers())
                    .filter(p -> p != playerId)
                    .map(p -> (int) llgs.getGameScore(p)).max().orElseThrow(() -> new AssertionError("??"));
            retValue += FACTOR_ADVANTAGE * ((llgs.affectionTokens[playerId] - maxOtherScore) / nRequiredTokens);
        }

        if (cardTypes.contains(Countess)) retValue += FACTOR_COUNTESS;
        if (cardTypes.contains(King)) retValue += FACTOR_KING;
        if (cardTypes.contains(Baron)) retValue += FACTOR_BARON;
        if (cardTypes.contains(Handmaid)) retValue += FACTOR_HANDMAID;
        if (cardTypes.contains(Guard)) retValue += FACTOR_GUARD;
        if (cardTypes.contains(Priest)) retValue += FACTOR_PRIEST;
        if (cardTypes.contains(Prince)) retValue += FACTOR_PRINCE;
        if (cardTypes.contains(Princess)) retValue += FACTOR_PRINCESS;

        return Utils.clamp(retValue, -1.0, 1.0);
    }

    /**
     * Return a copy of this game parameters object, with the same parameters as in the original.
     *
     * @return - new game parameters object.
     */
    @Override
    protected LoveLetterHeuristic _copy() {
        LoveLetterHeuristic retValue = new LoveLetterHeuristic();
        retValue.FACTOR_CARDS = FACTOR_CARDS;
        retValue.FACTOR_AFFECTION = FACTOR_AFFECTION;
        retValue.FACTOR_PRINCESS = FACTOR_PRINCESS;
        retValue.FACTOR_PRINCE = FACTOR_PRINCE;
        retValue.FACTOR_GUARD = FACTOR_GUARD;
        retValue.FACTOR_BARON = FACTOR_BARON;
        retValue.FACTOR_PRIEST = FACTOR_PRIEST;
        retValue.FACTOR_KING = FACTOR_KING;
        retValue.FACTOR_HANDMAID = FACTOR_HANDMAID;
        retValue.FACTOR_COUNTESS = FACTOR_COUNTESS;
        retValue.FACTOR_HIDDEN = FACTOR_HIDDEN;
        retValue.FACTOR_ADVANTAGE = FACTOR_ADVANTAGE;
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
            return other.FACTOR_HIDDEN == FACTOR_HIDDEN &&
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