package games.explodingkittens;

import core.AbstractGameState;
import core.CoreConstants;
import core.interfaces.IStateHeuristic;
import evaluation.optimisation.TunableParameters;
import games.explodingkittens.actions.IsNopeable;
import games.explodingkittens.cards.ExplodingKittensCard;

public class ExplodingKittensHeuristic extends TunableParameters implements IStateHeuristic {

    double explodingValue = -1;
    double defuseValue = 1;
    double regularValue = -0.01;
    double seeFutureValue = -0.3;  // Play it
    double nopeValue = -0.5;  // Play it
    double attackValue = -0.4;  // Play it
    double skipValue = 0.2;
    double favorValue = -0.1;
    double shuffleValue = -0.2;

    public ExplodingKittensHeuristic() {
        addTunableParameter("explodingValue", -1.0);
        addTunableParameter("defuseValue", 1.0);
        addTunableParameter("regularValue", -0.01);
        addTunableParameter("seeFutureValue", -0.3);
        addTunableParameter("nopeValue", -0.5);
        addTunableParameter("attackValue", -0.4);
        addTunableParameter("skipValue", 0.2);
        addTunableParameter("favorValue", -0.1);
        addTunableParameter("shuffleValue", -0.2);
    }

    @Override
    public void _reset() {
        explodingValue = (double) getParameterValue("explodingValue");
        defuseValue = (double) getParameterValue("defuseValue");
        regularValue = (double) getParameterValue("regularValue");
        seeFutureValue = (double) getParameterValue("seeFutureValue");
        nopeValue = (double) getParameterValue("nopeValue");
        attackValue = (double) getParameterValue("attackValue");
        skipValue = (double) getParameterValue("skipValue");
        favorValue = (double) getParameterValue("favorValue");
        shuffleValue = (double) getParameterValue("shuffleValue");
    }

    @Override
    public double evaluateState(AbstractGameState gs, int playerId) {
        ExplodingKittensGameState ekgs = (ExplodingKittensGameState)gs;
        CoreConstants.GameResult playerResult = ekgs.getPlayerResults()[playerId];

        if (playerResult == CoreConstants.GameResult.LOSE_GAME)
            return -1;
        if (playerResult == CoreConstants.GameResult.WIN_GAME)
            return 1;

        double cardValues = 0.0;
        for (ExplodingKittensCard card : ekgs.playerHandCards.get(playerId).getComponents()) {
            cardValues += getCardValue(ekgs, card);
        }

        return cardValues / (ekgs.playerHandCards.get(playerId).getSize() + 1);
    }

    // TODO: check state more
    double getCardValue(ExplodingKittensGameState ekgs, ExplodingKittensCard card) {
        switch (card.cardType) {
            case EXPLODING_KITTEN:
                return explodingValue;
            case DEFUSE:
                return defuseValue;
            case NOPE:
                if (ekgs.actionStack.size() > 0 && ekgs.actionStack.get(0) instanceof IsNopeable) {
                    return nopeValue;
                } else return 0;  // Neutral
            case ATTACK:
                return attackValue;
            case SKIP:
                return skipValue;
            case FAVOR:
                return favorValue;
            case SHUFFLE:
                return shuffleValue;
            case SEETHEFUTURE:
                return seeFutureValue;  // TODO: higher if future not already known, otherwise low
            default:
                return regularValue;
        }
    }

    /**
     * Return a copy of this game parameters object, with the same parameters as in the original.
     *
     * @return - new game parameters object.
     */
    @Override
    protected ExplodingKittensHeuristic _copy() {
        ExplodingKittensHeuristic retValue = new ExplodingKittensHeuristic();
        retValue.explodingValue = explodingValue;
        retValue.defuseValue = defuseValue;
        retValue.regularValue = regularValue;
        retValue.seeFutureValue = seeFutureValue;
        retValue.nopeValue = nopeValue;
        retValue.attackValue = attackValue;
        retValue.skipValue = skipValue;
        retValue.favorValue = favorValue;
        retValue.shuffleValue = shuffleValue;
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
        if (o instanceof ExplodingKittensHeuristic) {
            ExplodingKittensHeuristic other = (ExplodingKittensHeuristic) o;
            return other.explodingValue == explodingValue && other.defuseValue == defuseValue &&
                    other.regularValue == regularValue && other.seeFutureValue == seeFutureValue &&
                    other.nopeValue == nopeValue && other.attackValue == attackValue &&
                    other.skipValue == skipValue && other.favorValue == favorValue &&
                    other.shuffleValue == shuffleValue;
        }
        return false;
    }

    /**
     * @return Returns Tuned Parameters corresponding to the current settings
     * (will use all defaults if setParameterValue has not been called at all)
     */
    @Override
    public ExplodingKittensHeuristic instantiate() {
        return this._copy();
    }


}