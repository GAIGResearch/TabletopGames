package games.uno;
import core.AbstractGameState;
import core.interfaces.IStateHeuristic;
import utilities.Utils;

public class UnoHeuristic implements IStateHeuristic {

    double FACTOR_PLAYER = 0.5;
    double FACTOR_OPPONENT = -0.2;
    double FACTOR_N_CARDS = -0.3;

    @Override
    public double evaluateState(AbstractGameState gs, int playerId) {
        UnoGameState ugs = (UnoGameState) gs;
        UnoGameParameters ugp = ((UnoGameParameters)ugs.getGameParameters());
        Utils.GameResult gameStatus = gs.getGameStatus();

        if (gameStatus == Utils.GameResult.LOSE)
            return -1;
        if (gameStatus == Utils.GameResult.WIN)
            return 1;

        int nColors = ugp.colors.length;
        int deckSize = ugp.nDrawCards * ugp.specialDrawCards.length * nColors
                + ugp.nReverseCards * nColors
                + ugp.nSkipCards * nColors
                + ugp.nNumberCards * nColors
                + ugp.nWildCards * ugp.specialWildDrawCards.length
                ;

        double F_OPPONENT = FACTOR_OPPONENT/(ugs.getNPlayers()-1);
        double rawScore = 0;
        for (int i = 0; i < ugs.getNPlayers(); i++) {
            double s = 1.0*ugs.calculatePlayerPoints(i)/(ugp.nWinPoints*2);
            if (i == playerId) {
                rawScore += s * FACTOR_PLAYER;
            } else {
                rawScore += s * F_OPPONENT;
            }
        }
        rawScore += FACTOR_N_CARDS * ugs.getPlayerDecks().get(playerId).getSize()/deckSize;

//        System.out.println(rawScore);
        return rawScore;
    }
}