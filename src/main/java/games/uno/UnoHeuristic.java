package games.uno;

import core.AbstractGameState;
import core.CoreConstants;
import core.interfaces.IStateHeuristic;
import evaluation.optimisation.TunableParameters;

public class UnoHeuristic extends TunableParameters implements IStateHeuristic {

    double FACTOR_PLAYER = 0.5;
    double FACTOR_OPPONENT = -0.2;
    double FACTOR_N_CARDS = -0.3;

    public UnoHeuristic() {
        addTunableParameter("FACTOR_PLAYER", 0.5);
        addTunableParameter("FACTOR_OPPONENT", -0.2);
        addTunableParameter("FACTOR_N_CARDS", -0.3);
    }

    @Override
    public void _reset() {
        FACTOR_PLAYER = (double) getParameterValue("FACTOR_PLAYER");
        FACTOR_OPPONENT = (double) getParameterValue("FACTOR_OPPONENT");
        FACTOR_N_CARDS = (double) getParameterValue("FACTOR_N_CARDS");
    }

    @Override
    public double evaluateState(AbstractGameState gs, int playerId) {
        UnoGameState ugs = (UnoGameState) gs;
        UnoGameParameters ugp = ((UnoGameParameters) ugs.getGameParameters());
        CoreConstants.GameResult playerResult = gs.getPlayerResults()[playerId];

        if (playerResult == CoreConstants.GameResult.LOSE_GAME)
            return -1;
        if (playerResult == CoreConstants.GameResult.WIN_GAME)
            return 1;

        int nColors = ugp.colors.length;
        int deckSize = ugp.nDrawCards * ugp.specialDrawCards.length * nColors
                + ugp.nReverseCards * nColors
                + ugp.nSkipCards * nColors
                + ugp.nNumberCards * nColors
                + ugp.nWildCards * ugp.specialWildDrawCards.length;

        double F_OPPONENT = FACTOR_OPPONENT / (ugs.getNPlayers() - 1);
        double rawScore = 0;
        for (int i = 0; i < ugs.getNPlayers(); i++) {
            double s = 1.0 * ugs.calculatePlayerPoints(i, true) / (ugp.nWinPoints * 2);
            if (i == playerId) {
                rawScore += s * FACTOR_PLAYER;
            } else {
                rawScore += s * F_OPPONENT;
            }
        }
        rawScore += FACTOR_N_CARDS * ugs.getPlayerDecks().get(playerId).getSize() / deckSize;

//        System.out.println(rawScore);
        return rawScore;
    }

    /**
     * Return a copy of this game parameters object, with the same parameters as in the original.
     *
     * @return - new game parameters object.
     */
    @Override
    protected UnoHeuristic _copy() {
        UnoHeuristic retValue = new UnoHeuristic();
        retValue.FACTOR_OPPONENT = FACTOR_OPPONENT;
        retValue.FACTOR_N_CARDS = FACTOR_N_CARDS;
        retValue.FACTOR_PLAYER = FACTOR_PLAYER;
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
        if (o instanceof UnoHeuristic) {
            UnoHeuristic other = (UnoHeuristic) o;
            return other.FACTOR_OPPONENT == FACTOR_OPPONENT && other.FACTOR_PLAYER == FACTOR_PLAYER &&
                    other.FACTOR_N_CARDS == FACTOR_N_CARDS;
        }
        return false;
    }

    /**
     * @return Returns Tuned Parameters corresponding to the current settings
     * (will use all defaults if setParameterValue has not been called at all)
     */
    @Override
    public UnoHeuristic instantiate() {
        return _copy();
    }

}