package players.heuristics;
import core.AbstractGameState;
import core.CoreConstants;
import core.components.Counter;
import core.components.Deck;
import games.pandemic.PandemicConstants;
import games.pandemic.PandemicGameState;
import utilities.Hash;
import utilities.Utils;

public class PandemicHeuristic extends StateHeuristic {
    AbstractGameState pgs;

    public PandemicHeuristic(AbstractGameState gs) {
        this.pgs = gs;
    }

    @Override
    public double evaluateState(AbstractGameState gs) {
        Utils.GameResult gamestatus = gs.getGameStatus();

        // Compute a score relative to the root's state.
        BoardStats boardStats = new BoardStats((PandemicGameState)gs);
        double rawScore = boardStats.score();

        if(gamestatus == Utils.GameResult.LOSE)
            rawScore = -1;

        if(gamestatus == Utils.GameResult.WIN)
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
         * @return a score [0, 1]
         */
        double score()
        {
            int diffCures = this.nCuresDiscovered;
            int diffCardsInHand = this.nCardsInHand;
            int diffCubes = this.nDiseaseCubes;
            int diffCardsInPile = this.nCardsInPile;
            int diffOutbreaks = this.nOutbreaks;
            int diffResearchStations = this.nResearchStations;

            double score = diffCures * FACTOR_CURES + diffCardsInHand * FACTOR_CARDS_IN_HAND + diffCubes * FACTOR_CUBES +
                    diffCardsInPile * FACTOR_CARDS_IN_PILE + diffOutbreaks * FACTOR_OUTBREAKS + diffResearchStations * FACTOR_RS;

//            System.out.println("OSLA evaluated = " + score);

            return score;
        }
    }
}