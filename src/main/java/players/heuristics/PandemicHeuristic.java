package players.heuristics;
import core.AbstractGameState;
import core.components.Deck;
import games.pandemic.PandemicConstants;
import games.pandemic.PandemicData;
import games.pandemic.PandemicGameState;
import utilities.Utils;

public class PandemicHeuristic extends StateHeuristic {
    private BoardStats rootBoardStats;

    public PandemicHeuristic(AbstractGameState root) {
        rootBoardStats = new BoardStats((PandemicGameState)root);
    }

    @Override
    public double evaluateState(AbstractGameState gs) {
        Utils.GameResult gamestatus = gs.getGameStatus();

        // Compute a score relative to the root's state.
        BoardStats lastBoardState = new BoardStats((PandemicGameState)gs);
        double rawScore = rootBoardStats.score(lastBoardState);

        if(gamestatus == Utils.GameResult.GAME_LOSE)
            rawScore = -1;

        if(gamestatus == Utils.GameResult.GAME_WIN)
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
            PandemicData data = gs.getData();
            // iterate over the game state and get the value for the variables
//            int counterValue = gs.findCounter("Disease counter").getValue();
            nOutbreaks = data.findCounter("Outbreaks").getValue();
            nCardsInPile = data.findDeck("Player Deck").getCards().size();
            nCardsInHand = ((Deck)gs.getComponent(PandemicConstants.playerDeckHash)).getSize();

            // get disease cubes
            for (int i = 0; i < 4; i++){
                nDiseaseCubes += data.findCounter("Disease cube " + PandemicConstants.colors[i]).getValue();
                if (data.findCounter("Disease " + PandemicConstants.colors[i]).getValue() > 0)
                    nCuresDiscovered += 1;
            }

            nResearchStations = data.findCounter("Research Stations").getValue();


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