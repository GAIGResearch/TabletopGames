package players.heuristics;
import core.AbstractGameState;
import core.CoreConstants;
import core.components.Counter;
import core.components.Deck;
import core.interfaces.IStateHeuristic;
import games.pandemic.PandemicConstants;
import games.pandemic.PandemicGameState;
import utilities.Hash;

public class PandemicDiffHeuristic implements IStateHeuristic {
    private BoardStats rootBoardStats;

    public PandemicDiffHeuristic(AbstractGameState root) {
        rootBoardStats = new BoardStats((PandemicGameState)root);
    }

    @Override
    public double evaluateState(AbstractGameState gs, int playerId) {
        CoreConstants.GameResult gamestatus = gs.getGameStatus();

        // Compute a score relative to the root's state.
        BoardStats lastBoardState = new BoardStats((PandemicGameState)gs);
        double rawScore = rootBoardStats.score(lastBoardState);

        if(gamestatus == CoreConstants.GameResult.LOSE_GAME)
            rawScore = -1;

        if(gamestatus == CoreConstants.GameResult.WIN_GAME)
            rawScore = 1;

        return rawScore;
    }


    public static class BoardStats
    {
        int nCuresDiscovered;
        int nDiseaseCubes;
        int nCardsInPile;
        int nCardsInHand;
        int nOutbreaks;
        int nResearchStations;

        double FACTOR_CURES = 0.3;
        double FACTOR_CUBES = 0.2;
        double FACTOR_CARDS_IN_PILE = 0.15;
        double FACTOR_CARDS_IN_HAND = 0.15;
        double FACTOR_OUTBREAKS = -0.2;
        double FACTOR_RS = 0.2;

        BoardStats(PandemicGameState gs) {
            nOutbreaks = ((Counter)gs.getComponent(PandemicConstants.outbreaksHash)).getValue();
            nCardsInPile = ((Deck)gs.getComponent(PandemicConstants.playerDeckHash)).getSize();
            nCardsInHand = ((Deck)gs.getComponentActingPlayer(CoreConstants.playerHandHash)).getSize();
            nResearchStations = ((Counter)gs.getComponent(PandemicConstants.researchStationHash)).getValue();

            for (int i = 0; i < 4; i++){
                nDiseaseCubes += ((Counter)gs.getComponent(Hash.GetInstance().hash("Disease Cube " + PandemicConstants.colors[i]))).getValue();
                if (((Counter)gs.getComponent(Hash.GetInstance().hash("Disease Cube " + PandemicConstants.colors[i]))).getValue() > 0)
                    nCuresDiscovered += 1;
            }

        }

        /**
         * Computes score for a game, in relation to the initial state at the root.
         * @param futureState the stats of the board at the end of the rollout.
         * @return a score [0, 1]
         */
        double score(BoardStats futureState)
        {
            int diffCures = futureState.nCuresDiscovered - this.nCuresDiscovered;
            int diffCardsInHand = futureState.nCardsInHand - this.nCardsInHand;
            int diffCubes = futureState.nDiseaseCubes - this.nDiseaseCubes;
            int diffCardsInPile = futureState.nCardsInPile - this.nCardsInPile;
            int diffOutbreaks = futureState.nOutbreaks - this.nOutbreaks;
            int diffResearchStations = futureState.nResearchStations - this.nResearchStations;

            double score = diffCures * FACTOR_CURES + diffCardsInHand * FACTOR_CARDS_IN_HAND + diffCubes * FACTOR_CUBES +
                    diffCardsInPile * FACTOR_CARDS_IN_PILE + diffOutbreaks * FACTOR_OUTBREAKS + diffResearchStations * FACTOR_RS;

//            System.out.println("OSLA evaluated = " + score);

            return score;
        }
    }
}